/*
 * Copyright 2017-2020 吴学文 and java110 team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.java110.user.cmd.examine;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.core.smo.IPhotoSMO;
import com.java110.dto.user.UserDto;
import com.java110.intf.user.IExamineStaffIntroductionV1InnerServiceSMO;
import com.java110.intf.user.IExamineStaffProjectV1InnerServiceSMO;
import com.java110.intf.user.IExamineStaffV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.examine.ExamineStaffPo;
import com.java110.po.examine.ExamineStaffIntroductionPo;
import com.java110.po.examine.ExamineStaffProjectPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


/**
 * 类表述：更新员工考核信息命令类
 * 服务编码：examineStaff.updateExamineStaff
 * 请求路劲：/app/examineStaff.UpdateExamineStaff
 * 功能描述：该命令类用于处理员工考核信息的更新操作，包括更新考核基本信息、考核项目和考核说明等
 * add by 吴学文 at 2023-03-07 15:57:29 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "examine.updateExamineStaff")
public class UpdateExamineStaffCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdateExamineStaffCmd.class);

    // 员工考核信息服务接口
    @Autowired
    private IExamineStaffV1InnerServiceSMO examineStaffV1InnerServiceSMOImpl;

    // 员工考核项目服务接口
    @Autowired
    private IExamineStaffProjectV1InnerServiceSMO examineStaffProjectV1InnerServiceSMOImpl;

    // 员工考核说明服务接口
    @Autowired
    private IExamineStaffIntroductionV1InnerServiceSMO examineStaffIntroductionV1InnerServiceSMOImpl;

    // 照片服务接口
    @Autowired
    private IPhotoSMO photoSMOImpl;

    // 用户信息服务接口
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 功能描述：验证请求参数的有效性，确保必要的参数都存在且有效
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填参数是否存在
        Assert.hasKeyAndValue(reqJson, "esId", "esId不能为空");
        Assert.hasKeyAndValue(reqJson, "staffId", "staffId不能为空");
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");

        // 验证是否包含考核项目
        if (!reqJson.containsKey("projectIds")) {
            throw new CmdException("没包含考核项目");
        }

        JSONArray projectIds = reqJson.getJSONArray("projectIds");

        // 验证考核项目数组是否为空
        if (projectIds == null || projectIds.size() < 1) {
            throw new CmdException("没包含考核项目");
        }
    }

    /**
     * 执行命令方法
     * 功能描述：处理员工考核信息的更新操作，包括更新考核基本信息、重新关联考核项目和更新考核说明
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当业务处理失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 根据员工ID查询员工信息
        UserDto userDto = new UserDto();
        userDto.setUserId(reqJson.getString("staffId"));
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);

        // 验证员工是否存在且唯一
        Assert.listOnlyOne(userDtos, "员工不存在");

        // 将请求参数转换为考核员工PO对象
        ExamineStaffPo examineStaffPo = BeanConvertUtil.covertBean(reqJson, ExamineStaffPo.class);
        // 设置员工姓名
        examineStaffPo.setStaffName(userDtos.get(0).getName());
        // 更新考核员工基本信息
        int flag = examineStaffV1InnerServiceSMOImpl.updateExamineStaff(examineStaffPo);

        // 检查更新操作是否成功
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        // 删除原有的考核项目关联
        ExamineStaffProjectPo tmpExamineStaffProjectPo = null;
        tmpExamineStaffProjectPo = new ExamineStaffProjectPo();
        tmpExamineStaffProjectPo.setEsId(examineStaffPo.getEsId());
        tmpExamineStaffProjectPo.setCommunityId(examineStaffPo.getCommunityId());
        examineStaffProjectV1InnerServiceSMOImpl.deleteExamineStaffProject(tmpExamineStaffProjectPo);

        // 重新关联新的考核项目
        JSONArray projectIds = reqJson.getJSONArray("projectIds");

        for (int projectIndex = 0; projectIndex < projectIds.size(); projectIndex++) {
            tmpExamineStaffProjectPo = new ExamineStaffProjectPo();
            tmpExamineStaffProjectPo.setEsId(examineStaffPo.getEsId());
            tmpExamineStaffProjectPo.setCommunityId(examineStaffPo.getCommunityId());
            tmpExamineStaffProjectPo.setProjectId(projectIds.getString(projectIndex));
            // 生成考核项目关联ID
            tmpExamineStaffProjectPo.setEspId(GenerateCodeFactory.getGeneratorId("11"));
            // 保存考核项目关联
            examineStaffProjectV1InnerServiceSMOImpl.saveExamineStaffProject(tmpExamineStaffProjectPo);
        }

        // 更新考核说明信息（如果存在）
        if (reqJson.containsKey("introduction") && !StringUtil.isEmpty(reqJson.getString("introduction"))) {
            ExamineStaffIntroductionPo examineStaffIntroductionPo = new ExamineStaffIntroductionPo();
            examineStaffIntroductionPo.setStaffId(reqJson.getString("staffId"));
            examineStaffIntroductionPo.setIntroduction(reqJson.getString("introduction"));
            examineStaffIntroductionPo.setCommunityId(reqJson.getString("communityId"));
            examineStaffIntroductionV1InnerServiceSMOImpl.updateExamineStaffIntroduction(examineStaffIntroductionPo);
        }
        
        // 保存头像照片（如果存在）
        if(reqJson.containsKey("headerImg")) {
            photoSMOImpl.savePhoto(reqJson.getString("headerImg"),
                    examineStaffPo.getEsId(),
                    reqJson.getString("communityId"));
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}