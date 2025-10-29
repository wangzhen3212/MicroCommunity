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
package com.java110.user.cmd.ownerCommittee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.user.IOwnerCommitteeContractV1InnerServiceSMO;
import com.java110.intf.user.IOwnerCommitteeV1InnerServiceSMO;
import com.java110.po.owner.OwnerCommitteePo;
import com.java110.po.owner.OwnerCommitteeContractPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 删除业委会信息命令类
 * 
 * 该类负责处理删除业委会信息的业务逻辑，包括删除业委会基本信息及其关联的合同信息
 * 服务编码：ownerCommittee.deleteOwnerCommittee
 * 请求路径：/app/ownerCommittee.DeleteOwnerCommittee
 * 
 * @author 吴学文 at 2023-03-06 11:57:04 mail: 928255095@qq.com
 * @open source address: https://gitee.com/wuxw7/MicroCommunity
 * @官网：http://www.homecommunity.cn
 * @温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "ownerCommittee.deleteOwnerCommittee")
public class DeleteOwnerCommitteeCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteOwnerCommitteeCmd.class);

    /**
     * 业委会信息服务接口
     */
    @Autowired
    private IOwnerCommitteeV1InnerServiceSMO ownerCommitteeV1InnerServiceSMOImpl;

    /**
     * 业委会合同信息服务接口
     */
    @Autowired
    private IOwnerCommitteeContractV1InnerServiceSMO ownerCommitteeContractV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段：ocId和communityId
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含ocId字段
        Assert.hasKeyAndValue(reqJson, "ocId", "ocId不能为空");
        // 验证请求参数中必须包含communityId字段
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行删除业委会信息命令
     * 
     * 该方法负责执行删除业委会信息的核心业务逻辑，包括：
     * 1. 删除业委会基本信息
     * 2. 删除业委会关联的合同信息
     * 3. 设置响应结果
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON数据转换为业委会PO对象
        OwnerCommitteePo ownerCommitteePo = BeanConvertUtil.covertBean(reqJson, OwnerCommitteePo.class);
        
        // 调用服务删除业委会基本信息，返回受影响的行数
        int flag = ownerCommitteeV1InnerServiceSMOImpl.deleteOwnerCommittee(ownerCommitteePo);

        // 检查删除操作是否成功，如果受影响行数小于1表示删除失败
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 创建业委会合同PO对象，用于删除关联的合同信息
        OwnerCommitteeContractPo ownerCommitteeContractPo = null;
        ownerCommitteeContractPo = new OwnerCommitteeContractPo();
        // 设置合同对象中的业委会ID，用于关联删除
        ownerCommitteeContractPo.setOcId(ownerCommitteePo.getOcId());
        // 调用服务删除业委会关联的合同信息
        ownerCommitteeContractV1InnerServiceSMOImpl.deleteOwnerCommitteeContract(ownerCommitteeContractPo);

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}