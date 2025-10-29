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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
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
 * 类表述：更新
 * 服务编码：ownerCommittee.updateOwnerCommittee
 * 请求路劲：/app/ownerCommittee.UpdateOwnerCommittee
 * add by 吴学文 at 2023-03-06 11:57:04 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 业委会信息更新命令类
 * 
 * 该类负责处理业委会信息的更新请求，包括业委会基本信息的更新和业委会合同信息的更新
 * 使用事务保证数据一致性，支持批量更新合同信息
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023-03-06
 */
@Java110Cmd(serviceCode = "ownerCommittee.updateOwnerCommittee")
public class UpdateOwnerCommitteeCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(UpdateOwnerCommitteeCmd.class);

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
     * 请求参数验证方法
     * 
     * 验证更新业委会信息所需的必要参数是否存在
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当必要参数缺失时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含ocId
        Assert.hasKeyAndValue(reqJson, "ocId", "ocId不能为空");
        // 验证请求参数中必须包含communityId
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行业委会信息更新命令
     * 
     * 该方法负责更新业委会基本信息，并处理相关的合同信息更新
     * 采用事务处理保证数据一致性，先更新业委会基本信息，再处理合同信息
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当更新失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为业委会PO对象
        OwnerCommitteePo ownerCommitteePo = BeanConvertUtil.covertBean(reqJson, OwnerCommitteePo.class);
        // 调用服务更新业委会基本信息
        int flag = ownerCommitteeV1InnerServiceSMOImpl.updateOwnerCommittee(ownerCommitteePo);

        // 检查更新是否成功，失败则抛出异常
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        // 获取请求中的合同信息数组
        JSONArray contracts = reqJson.getJSONArray("contracts");

        // 如果没有合同信息，直接返回成功
        if (contracts == null || contracts.size() < 1) {
            return;
        }
        
        OwnerCommitteeContractPo ownerCommitteeContractPo = null;

        // 删除该业委会原有的所有合同信息
        ownerCommitteeContractPo = new OwnerCommitteeContractPo();
        ownerCommitteeContractPo.setOcId(ownerCommitteePo.getOcId());
        ownerCommitteeContractV1InnerServiceSMOImpl.deleteOwnerCommitteeContract(ownerCommitteeContractPo);

        // 遍历并保存新的合同信息
        for (int contractIndex = 0; contractIndex < contracts.size(); contractIndex++) {
            // 将单个合同JSON转换为合同PO对象
            ownerCommitteeContractPo = BeanConvertUtil.covertBean(contracts.getJSONObject(contractIndex), OwnerCommitteeContractPo.class);
            // 生成合同ID
            ownerCommitteeContractPo.setContractId(GenerateCodeFactory.getGeneratorId("11"));
            // 设置业委会ID
            ownerCommitteeContractPo.setOcId(ownerCommitteePo.getOcId());
            // 设置小区ID
            ownerCommitteeContractPo.setCommunityId(ownerCommitteePo.getCommunityId());
            // 保存合同信息
            ownerCommitteeContractV1InnerServiceSMOImpl.saveOwnerCommitteeContract(ownerCommitteeContractPo);
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}