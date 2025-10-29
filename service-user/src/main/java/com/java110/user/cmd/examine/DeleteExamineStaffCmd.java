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

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.user.IExamineStaffV1InnerServiceSMO;
import com.java110.intf.user.IExamineStaffProjectV1InnerServiceSMO;
import com.java110.po.examine.ExamineStaffPo;
import com.java110.po.examine.ExamineStaffProjectPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：删除审核人员命令类
 * 服务编码：examineStaff.deleteExamineStaff
 * 请求路径：/app/examineStaff.DeleteExamineStaff
 * 
 * 功能描述：该命令类用于处理删除审核人员及其相关项目信息的业务逻辑
 * 主要功能包括：
 * 1. 验证删除操作的必要参数
 * 2. 删除审核人员基本信息
 * 3. 删除审核人员关联的项目信息
 * 4. 返回操作结果
 * 
 * @author 吴学文 at 2023-03-07 15:57:29 mail: 928255095@qq.com
 * @open source address: https://gitee.com/wuxw7/MicroCommunity
 * @官网：http://www.homecommunity.cn
 * @温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "examine.deleteExamineStaff")
public class DeleteExamineStaffCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteExamineStaffCmd.class);

    /**
     * 审核人员服务接口
     */
    @Autowired
    private IExamineStaffV1InnerServiceSMO examineStaffV1InnerServiceSMOImpl;

    /**
     * 审核人员项目服务接口
     */
    @Autowired
    private IExamineStaffProjectV1InnerServiceSMO examineStaffProjectV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 功能描述：验证删除审核人员操作所需的必要参数是否完整
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含esId（审核人员ID）
        Assert.hasKeyAndValue(reqJson, "esId", "esId不能为空");
        // 验证请求参数中必须包含communityId（小区ID）
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 命令执行方法
     * 
     * 功能描述：执行删除审核人员及其相关项目信息的业务逻辑
     * 执行步骤：
     * 1. 将请求参数转换为审核人员PO对象
     * 2. 调用服务删除审核人员基本信息
     * 3. 检查删除结果，失败则抛出异常
     * 4. 删除审核人员关联的项目信息
     * 5. 设置成功响应结果
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为审核人员PO对象
        ExamineStaffPo examineStaffPo = BeanConvertUtil.covertBean(reqJson, ExamineStaffPo.class);
        
        // 调用审核人员服务删除审核人员基本信息
        int flag = examineStaffV1InnerServiceSMOImpl.deleteExamineStaff(examineStaffPo);

        // 检查删除操作是否成功，如果影响行数小于1则认为删除失败
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 创建审核人员项目PO对象，用于删除关联的项目信息
        ExamineStaffProjectPo examineStaffProjectPo = new ExamineStaffProjectPo();
        // 设置审核人员ID
        examineStaffProjectPo.setEsId(examineStaffPo.getEsId());
        // 设置小区ID
        examineStaffProjectPo.setCommunityId(examineStaffPo.getCommunityId());
        // 调用服务删除审核人员关联的项目信息
        examineStaffProjectV1InnerServiceSMOImpl.deleteExamineStaffProject(examineStaffProjectPo);

        // 设置成功响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}