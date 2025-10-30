package com.java110.store.cmd.contract;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.contract.ContractDto;
import com.java110.dto.contract.ContractAttrDto;
import com.java110.dto.contract.ContractRoomDto;
import com.java110.intf.common.IContractApplyUserInnerServiceSMO;
import com.java110.intf.common.IContractChangeUserInnerServiceSMO;
import com.java110.intf.store.IContractAttrInnerServiceSMO;
import com.java110.intf.store.IContractInnerServiceSMO;
import com.java110.intf.store.IContractRoomInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 根据房屋ID查询合同信息命令类
 * 
 * 该类是一个Spring命令组件，用于处理根据房屋ID查询相关合同信息的业务逻辑。
 * 通过房屋ID查询关联的合同信息，并返回包含合同基本信息和扩展属性的完整数据。
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode contract.queryContractByRoomId
 */
@Java110Cmd(serviceCode = "contract.queryContractByRoomId")
public class QueryContractByRoomIdCmd extends Cmd {

    /** 合同内部服务接口 */
    @Autowired
    private IContractInnerServiceSMO contractInnerServiceSMOImpl;

    /** 合同属性内部服务接口 */
    @Autowired
    private IContractAttrInnerServiceSMO contractAttrInnerServiceSMOImpl;

    /** 合同申请用户内部服务接口 */
    @Autowired
    private IContractApplyUserInnerServiceSMO contractApplyUserInnerServiceSMOImpl;

    /** 合同变更用户内部服务接口 */
    @Autowired
    private IContractChangeUserInnerServiceSMO contractChangeUserInnerServiceSMO;

    /** 合同房屋关联内部服务接口 */
    @Autowired
    private IContractRoomInnerServiceSMO contractRoomInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否合法，包括分页信息和必填参数检查
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 调用父类方法验证分页信息
        super.validatePageInfo(reqJson);
        // 验证请求参数中必须包含roomId字段
        Assert.hasKeyAndValue(reqJson, "roomId", "未包含房屋信息");
    }

    /**
     * 命令执行方法
     * 
     * 根据房屋ID查询关联的合同信息，包括合同基本信息和扩展属性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 创建合同房屋关联查询对象
        ContractRoomDto contractRoomDto = new ContractRoomDto();
        // 设置房屋ID查询条件
        contractRoomDto.setRoomId(reqJson.getString("roomId"));
        // 根据房屋ID查询合同房屋关联信息
        List<ContractRoomDto> roomDtos = contractRoomInnerServiceSMOImpl.queryContractRooms(contractRoomDto);
        
        // 如果没有查询到关联的合同信息，直接返回空结果
        if(roomDtos == null || roomDtos.size()<1){
            return ;
        }

        // 提取所有关联的合同ID
        List<String> contractIds = new ArrayList<>();
        for(ContractRoomDto tmpContractRoomDto : roomDtos){
            contractIds.add(tmpContractRoomDto.getContractId());
        }

        // 将请求参数转换为合同查询对象
        ContractDto contractDto = BeanConvertUtil.covertBean(reqJson, ContractDto.class);
        // 设置合同ID查询条件
        contractDto.setContractIds(contractIds.toArray(new String[contractIds.size()]));

        // 查询符合条件的合同总数
        int count = contractInnerServiceSMOImpl.queryContractsCount(contractDto);

        List<ContractDto> contractDtos = null;
        // 如果存在合同记录，则查询合同详细信息
        if (count > 0) {
            contractDtos = contractInnerServiceSMOImpl.queryContracts(contractDto);
            // 刷新合同的扩展属性信息
            refreshAttr(contractDtos);
        } else {
            // 没有查询到合同信息，返回空列表
            contractDtos = new ArrayList<>();
        }

        // 构造返回结果对象，包含分页信息和数据列表
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) contractDto.getRow()), count, contractDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应结果到数据流上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 刷新合同属性信息
     * 
     * 为合同列表中的每个合同对象加载其扩展属性信息
     * 
     * @param contractDtos 合同数据对象列表
     */
    private void refreshAttr(List<ContractDto> contractDtos) {
        // 提取所有合同ID用于批量查询属性
        List<String> contractIds = new ArrayList<>();
        for (ContractDto contractDto : contractDtos) {
            contractIds.add(contractDto.getContractId());
        }

        // 如果没有合同ID，直接返回
        if (contractIds.size() < 1) {
            return;
        }

        // 创建合同属性查询对象
        ContractAttrDto contractAttrDto = new ContractAttrDto();
        // 设置合同ID查询条件
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