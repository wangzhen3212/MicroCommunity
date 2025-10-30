/**
 * 合同查询命令类
 * 
 * 该类负责处理合同查询相关的业务逻辑，包括查询合同列表、合同属性信息等
 * 通过注解@Java110Cmd声明服务代码为"/contract/queryContract"
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.store.cmd.contract;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.contract.ContractDto;
import com.java110.dto.contract.ContractAttrDto;
import com.java110.intf.common.IContractApplyUserInnerServiceSMO;
import com.java110.intf.common.IContractChangeUserInnerServiceSMO;
import com.java110.intf.store.IContractAttrInnerServiceSMO;
import com.java110.intf.store.IContractInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "/contract/queryContract")
public class QueryContractCmd extends Cmd {

    /**
     * 合同内部服务接口
     */
    @Autowired
    private IContractInnerServiceSMO contractInnerServiceSMOImpl;

    /**
     * 合同属性内部服务接口
     */
    @Autowired
    private IContractAttrInnerServiceSMO contractAttrInnerServiceSMOImpl;

    /**
     * 合同申请用户内部服务接口
     */
    @Autowired
    private IContractApplyUserInnerServiceSMO contractApplyUserInnerServiceSMOImpl;

    /**
     * 合同变更用户内部服务接口
     */
    @Autowired
    private IContractChangeUserInnerServiceSMO contractChangeUserInnerServiceSMO;

    /**
     * 参数验证方法
     * 
     * 主要验证分页参数的正确性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 调用父类的分页信息验证方法
        super.validatePageInfo(reqJson);
    }

    /**
     * 执行合同查询命令
     * 
     * 根据查询条件查询合同信息，包括合同基本信息和属性信息
     * 支持到期合同的特殊查询逻辑
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当命令执行过程中出现错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 从请求头中获取店铺ID
        String storeId = cmdDataFlowContext.getReqHeaders().get("store-id");

        // 将请求JSON转换为ContractDto对象
        ContractDto contractDto = BeanConvertUtil.covertBean(reqJson, ContractDto.class);
        contractDto.setStoreId(storeId);
        
        // 处理到期合同的查询逻辑
        if ("1".equals(reqJson.getString("expiration"))) {
            // 设置排除的状态：投诉状态和失败状态
            contractDto.setNoStates(new String[]{ContractDto.STATE_COMPLAINT, ContractDto.STATE_FAIL});
            // 设置结束时间为当前时间，用于查询已到期的合同
            contractDto.setEndTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        }
        
        // 查询合同总数
        int count = contractInnerServiceSMOImpl.queryContractsCount(contractDto);

        List<ContractDto> contractDtos = null;
        if (count > 0) {
            // 查询合同列表数据
            contractDtos = contractInnerServiceSMOImpl.queryContracts(contractDto);
            // 刷新合同属性信息
            refreshAttr(contractDtos);
        } else {
            // 如果没有数据，返回空列表
            contractDtos = new ArrayList<>();
        }

        // 计算总页数并构建返回结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) contractDto.getRow()), count, contractDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应实体到数据流上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 刷新合同属性信息
     * 
     * 为合同列表中的每个合同对象设置对应的属性信息
     * 
     * @param contractDtos 合同数据传输对象列表
     */
    private void refreshAttr(List<ContractDto> contractDtos) {
        // 收集所有合同ID
        List<String> contractIds = new ArrayList<>();
        for (ContractDto contractDto : contractDtos) {
            contractIds.add(contractDto.getContractId());
        }

        // 如果合同ID列表为空，直接返回
        if (contractIds.size() < 1) {
            return;
        }

        // 创建合同属性查询条件
        ContractAttrDto contractAttrDto = new ContractAttrDto();
        // 设置要查询的合同ID数组
        contractAttrDto.setContractIds(contractIds.toArray(new String[contractIds.size()]));
        // 批量查询合同属性信息
        List<ContractAttrDto> contractAttrDtos = contractAttrInnerServiceSMOImpl.queryContractAttrs(contractAttrDto);

        List<ContractAttrDto> attrs = null;
        // 遍历所有合同，为每个合同设置对应的属性列表
        for (ContractDto contractDto : contractDtos) {
            attrs = new ArrayList<>();
            // 遍历所有属性，找到属于当前合同的属性
            for (ContractAttrDto tmpContractAttrDto : contractAttrDtos) {
                if (contractDto.getContractId().equals(tmpContractAttrDto.getContractId())) {
                    attrs.add(tmpContractAttrDto);
                }
            }
            // 将属性列表设置到合同对象中
            contractDto.setAttrs(attrs);
        }
    }
}