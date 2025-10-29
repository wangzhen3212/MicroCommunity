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
 * 业主委员会信息保存命令类
 * 
 * 该类负责处理业主委员会信息的保存请求，包括业主委员会基本信息及其关联合同信息的保存。
 * 服务编码：ownerCommittee.saveOwnerCommittee
 * 请求路径：/app/ownerCommittee.SaveOwnerCommittee
 * 
 * {"ocId":"","name":"张三","sex":"B","link":"18909711425","idCard":"63212652452455","address":"谁是谁","position":"试试","post":"谁是谁","postDesc":"试试",
 * "appointTime":"谁是谁","curTime":"试试","state":"1000","remark":"谁是谁",
 * "contracts":[{"id":"6545bf9d-d1e8-4419-b744-df40d098f9ac","relName":"xxx","name":"xxx","link":"xxx","address":"xxx"}],
 * "communityId":"2023013154290059"}
 * 
 * 类表述：保存业主委员会信息
 * 服务编码：ownerCommittee.saveOwnerCommittee
 * 请求路劲：/app/ownerCommittee.SaveOwnerCommittee
 * add by 吴学文 at 2023-03-06 11:57:04 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "ownerCommittee.saveOwnerCommittee")
public class SaveOwnerCommitteeCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveOwnerCommitteeCmd.class);

    /**
     * ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 业主委员会服务接口
     */
    @Autowired
    private IOwnerCommitteeV1InnerServiceSMO ownerCommitteeV1InnerServiceSMOImpl;

    /**
     * 业主委员会合同服务接口
     */
    @Autowired
    private IOwnerCommitteeContractV1InnerServiceSMO ownerCommitteeContractV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求报文中是否包含必要的参数，确保数据的完整性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填字段是否存在
        Assert.hasKeyAndValue(reqJson, "name", "请求报文中未包含name");
        Assert.hasKeyAndValue(reqJson, "sex", "请求报文中未包含sex");
        Assert.hasKeyAndValue(reqJson, "link", "请求报文中未包含link");
        Assert.hasKeyAndValue(reqJson, "position", "请求报文中未包含position");
        Assert.hasKeyAndValue(reqJson, "post", "请求报文中未包含post");
        Assert.hasKeyAndValue(reqJson, "state", "请求报文中未包含state");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含小区信息");
    }

    /**
     * 执行保存业主委员会信息命令
     * 
     * 该方法负责保存业主委员会的基本信息及其关联的合同信息
     * 使用事务保证数据的一致性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为业主委员会PO对象
        OwnerCommitteePo ownerCommitteePo = BeanConvertUtil.covertBean(reqJson, OwnerCommitteePo.class);
        // 生成业主委员会ID
        ownerCommitteePo.setOcId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        // 保存业主委员会基本信息
        int flag = ownerCommitteeV1InnerServiceSMOImpl.saveOwnerCommittee(ownerCommitteePo);

        // 检查保存结果
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 检查请求中是否包含合同信息
        if (!reqJson.containsKey("contracts")) {
            // 没有合同信息，直接返回成功
            cmdDataFlowContext.setResponseEntity(ResultVo.success());
            return;
        }

        // 获取合同信息数组
        JSONArray contracts = reqJson.getJSONArray("contracts");

        // 检查合同信息是否为空
        if (contracts == null || contracts.size() < 1) {
            // 合同信息为空，直接返回成功
            cmdDataFlowContext.setResponseEntity(ResultVo.success());
            return;
        }
        
        OwnerCommitteeContractPo ownerCommitteeContractPo = null;

        // 遍历合同信息并保存
        for (int contractIndex = 0; contractIndex < contracts.size(); contractIndex++) {
            // 将合同JSON转换为合同PO对象
            ownerCommitteeContractPo = BeanConvertUtil.covertBean(contracts.getJSONObject(contractIndex), OwnerCommitteeContractPo.class);
            // 生成合同ID
            ownerCommitteeContractPo.setContractId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
            // 设置关联的业主委员会ID
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