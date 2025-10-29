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
 * 类表述：保存员工考核信息命令类
 * 服务编码：examineStaff.saveExamineStaff
 * 请求路劲：/app/examineStaff.SaveExamineStaff
 * add by 吴学文 at 2023-03-07 15:57:29 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * 功能描述：该类用于处理员工考核信息的保存操作，包括考核基本信息、考核项目和考核说明的保存
 */
@Java110Cmd(serviceCode = "examine.saveExamineStaff")
public class SaveExamineStaffCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveExamineStaffCmd.class);

    /**
     * ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 员工考核信息服务接口
     */
    @Autowired
    private IExamineStaffV1InnerServiceSMO examineStaffV1InnerServiceSMOImpl;

    /**
     * 员工考核项目服务接口
     */
    @Autowired
    private IExamineStaffProjectV1InnerServiceSMO examineStaffProjectV1InnerServiceSMOImpl;

    /**
     * 用户信息服务接口
     */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 员工考核说明服务接口
     */
    @Autowired
    private IExamineStaffIntroductionV1InnerServiceSMO examineStaffIntroductionV1InnerServiceSMOImpl;

    /**
     * 照片服务接口
     */
    @Autowired
    private IPhotoSMO photoSMOImpl;

    /**
     * 参数验证方法
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     * 
     * 功能描述：验证请求参数是否完整，包括必填字段和考核项目的存在性
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填字段
        Assert.hasKeyAndValue(reqJson, "staffId", "请求报文中未包含staffId");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.hasKeyAndValue(reqJson, "introduction", "请求报文中未包含communityId");

        // 验证考核项目是否存在
        if (!reqJson.containsKey("projectIds")) {
            throw new CmdException("没包含考核项目");
        }

        JSONArray projectIds = reqJson.getJSONArray("projectIds");

        // 验证考核项目是否为空
        if (projectIds == null || projectIds.size() < 1) {
            throw new CmdException("没包含考核项目");
        }
    }

    /**
     * 执行保存员工考核信息命令
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     * 
     * 功能描述：保存员工考核信息，包括考核基本信息、考核项目和考核说明，支持事务处理
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 查询员工信息
        UserDto userDto = new UserDto();
        userDto.setUserId(reqJson.getString("staffId"));
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);

        // 验证员工是否存在且唯一
        Assert.listOnlyOne(userDtos, "员工不存在");

        // 保存考核基本信息
        ExamineStaffPo examineStaffPo = BeanConvertUtil.covertBean(reqJson, ExamineStaffPo.class);
        examineStaffPo.setEsId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        examineStaffPo.setStaffName(userDtos.get(0).getName());
        int flag = examineStaffV1InnerServiceSMOImpl.saveExamineStaff(examineStaffPo);

        // 检查保存是否成功
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 保存考核项目信息
        JSONArray projectIds = reqJson.getJSONArray("projectIds");

        ExamineStaffProjectPo tmpExamineStaffProjectPo = null;
        // 遍历所有考核项目并保存
        for (int projectIndex = 0; projectIndex < projectIds.size(); projectIndex++) {
            tmpExamineStaffProjectPo = new ExamineStaffProjectPo();
            tmpExamineStaffProjectPo.setEsId(examineStaffPo.getEsId());
            tmpExamineStaffProjectPo.setCommunityId(examineStaffPo.getCommunityId());
            tmpExamineStaffProjectPo.setProjectId(projectIds.getString(projectIndex));
            tmpExamineStaffProjectPo.setEspId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
            examineStaffProjectV1InnerServiceSMOImpl.saveExamineStaffProject(tmpExamineStaffProjectPo);
        }

        // 保存考核说明信息（如果存在）
        if (reqJson.containsKey("introduction") && !StringUtil.isEmpty(reqJson.getString("introduction"))) {
            ExamineStaffIntroductionPo examineStaffIntroductionPo = new ExamineStaffIntroductionPo();
            examineStaffIntroductionPo.setEsiId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
            examineStaffIntroductionPo.setStaffId(reqJson.getString("staffId"));
            examineStaffIntroductionPo.setIntroduction(reqJson.getString("introduction"));
            examineStaffIntroductionPo.setCommunityId(reqJson.getString("communityId"));
            examineStaffIntroductionV1InnerServiceSMOImpl.saveExamineStaffIntroduction(examineStaffIntroductionPo);
        }

        // 保存员工头像照片
        photoSMOImpl.savePhoto(reqJson.getString("headerImg"),
                examineStaffPo.getEsId(),
                reqJson.getString("communityId"));

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}