/**
 * 合同管理查询命令类
 * 
 * 该命令类用于处理管理员合同查询请求，支持分页查询合同信息，
 * 并关联查询合同的扩展属性信息
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
import com.java110.dto.contract.ContractAttrDto;
import com.java110.dto.contract.ContractDto;
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

@Java110Cmd(serviceCode = "contract.queryAdminContract")
public class QueryAdminContractCmd extends Cmd {

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
     * 请求参数验证方法
     * 
     * 验证分页参数和管理员权限
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证管理员权限
        super.validateAdmin(cmdDataFlowContext);
    }

    /**
     * 执行合同查询命令
     * 
     * 处理管理员合同查询请求，查询合同列表并返回分页结果
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为合同DTO对象
        ContractDto contractDto = BeanConvertUtil.covertBean(reqJson, ContractDto.class);
        // 设置店铺ID为空，查询所有店铺的合同
        contractDto.setStoreId("");
        
        // 查询合同总数
        int count = contractInnerServiceSMOImpl.queryContractsCount(contractDto);

        List<ContractDto> contractDtos = null;
        // 如果有数据，则查询合同列表
        if (count > 0) {
            contractDtos = contractInnerServiceSMOImpl.queryContracts(contractDto);
            // 刷新合同属性信息
            refreshAttr(contractDtos);
        } else {
            // 没有数据时返回空列表
            contractDtos = new ArrayList<>();
        }

        // 计算总页数并创建结果对象
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) contractDto.getRow()), count, contractDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应结果
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 刷新合同属性信息
     * 
     * 批量查询合同属性，并将属性信息设置到对应的合同对象中
     * 
     * @param contractDtos 合同数据对象列表
     */
    private void refreshAttr(List<ContractDto> contractDtos) {
        // 收集所有合同ID
        List<String> contractIds = new ArrayList<>();
        for (ContractDto contractDto : contractDtos) {
            contractIds.add(contractDto.getContractId());
        }

        // 如果没有合同ID，直接返回
        if (contractIds.size() < 1) {
            return;
        }

        // 创建合同属性查询条件
        ContractAttrDto contractAttrDto = new ContractAttrDto();
        // 设置要查询的合同ID数组
        contractAttrDto.setContractIds(contractIds.toArray(new String[contractIds.size()]));
        // 批量查询合同属性
        List<ContractAttrDto> contractAttrDtos = contractAttrInnerServiceSMOImpl.queryContractAttrs(contractAttrDto);

        List<ContractAttrDto> attrs = null;
        // 遍历所有合同，设置对应的属性信息
        for (ContractDto contractDto : contractDtos) {
            attrs = new ArrayList<>();
            // 查找该合同的所有属性
            for (ContractAttrDto tmpContractAttrDto : contractAttrDtos) {
                // 如果属性属于当前合同，则添加到属性列表
                if (contractDto.getContractId().equals(tmpContractAttrDto.getContractId())) {
                    attrs.add(tmpContractAttrDto);
                }
            }
            // 设置合同的属性列表
            contractDto.setAttrs(attrs);
        }
    }
}