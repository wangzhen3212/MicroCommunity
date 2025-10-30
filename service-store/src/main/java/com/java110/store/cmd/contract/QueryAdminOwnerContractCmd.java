package com.java110.store.cmd.contract;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.contract.ContractAttrDto;
import com.java110.dto.contract.ContractDto;
import com.java110.intf.common.IContractApplyUserInnerServiceSMO;
import com.java110.intf.common.IContractChangeUserInnerServiceSMO;
import com.java110.intf.store.IContractAttrInnerServiceSMO;
import com.java110.intf.store.IContractInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询管理员业主合同命令类
 * 该命令用于查询管理员权限下的业主合同信息，包括合同基本信息和相关属性
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "contract.queryAdminOwnerContract")
public class QueryAdminOwnerContractCmd extends Cmd {

    @Autowired
    private IContractInnerServiceSMO contractInnerServiceSMOImpl;

    @Autowired
    private IContractAttrInnerServiceSMO contractAttrInnerServiceSMOImpl;

    @Autowired
    private IContractApplyUserInnerServiceSMO contractApplyUserInnerServiceSMOImpl;

    @Autowired
    private IContractChangeUserInnerServiceSMO contractChangeUserInnerServiceSMO;

    /**
     * 验证请求参数
     * 验证分页信息、管理员权限和必要的业主ID参数
     *
     * @param event             命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson           请求的JSON数据
     * @throws CmdException 当验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证管理员权限
        super.validateAdmin(cmdDataFlowContext);
        // 验证业主ID参数是否存在
        Assert.hasKeyAndValue(reqJson,"ownerId","未包含业主ID");
    }

    /**
     * 执行查询管理员业主合同命令
     * 根据业主ID查询合同信息，包括合同基本信息和属性信息，并返回分页结果
     *
     * @param event             命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson           请求的JSON数据
     * @throws CmdException 当命令执行失败时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为ContractDto对象
        ContractDto contractDto = BeanConvertUtil.covertBean(reqJson, ContractDto.class);
        // 设置对象ID为业主ID
        contractDto.setObjId(reqJson.getString("ownerId"));

        // 查询合同总数
        int count = contractInnerServiceSMOImpl.queryContractsCount(contractDto);

        List<ContractDto> contractDtos = null;
        if (count > 0) {
            // 如果有合同数据，查询合同列表
            contractDtos = contractInnerServiceSMOImpl.queryContracts(contractDto);
            // 刷新合同属性信息
            refreshAttr(contractDtos);
        } else {
            // 如果没有合同数据，返回空列表
            contractDtos = new ArrayList<>();
        }

        // 计算总页数并创建结果对象
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) contractDto.getRow()), count, contractDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应实体到数据流上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 刷新合同属性信息
     * 为合同列表中的每个合同查询并设置对应的属性信息
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
        if (ListUtil.isNull(contractIds)) {
            return;
        }

        // 创建合同属性查询条件
        ContractAttrDto contractAttrDto = new ContractAttrDto();
        contractAttrDto.setContractIds(contractIds.toArray(new String[contractIds.size()]));
        // 批量查询合同属性
        List<ContractAttrDto> contractAttrDtos = contractAttrInnerServiceSMOImpl.queryContractAttrs(contractAttrDto);

        List<ContractAttrDto> attrs = null;
        // 为每个合同设置对应的属性列表
        for (ContractDto contractDto : contractDtos) {
            attrs = new ArrayList<>();
            // 遍历所有属性，找到属于当前合同的属性
            for (ContractAttrDto tmpContractAttrDto : contractAttrDtos) {
                if (contractDto.getContractId().equals(tmpContractAttrDto.getContractId())) {
                    attrs.add(tmpContractAttrDto);
                }
            }
            // 设置合同属性列表
            contractDto.setAttrs(attrs);
        }
    }
}