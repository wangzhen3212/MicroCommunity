/**
 * 查询社区业主账户命令类
 * 
 * 该命令类负责处理查询社区业主账户信息的业务逻辑，支持根据业主ID、费用ID等多种条件查询账户信息。
 * 通过解析请求参数，构建查询条件，并调用相应的业务模块对象(BMO)获取账户数据。
 * 同时包含对费用ID参数的特殊处理逻辑，能够根据费用对象类型(房屋、车位、合同)获取对应的业主ID。
 * 
 * @author Java110
 * @version 1.0
 * @see Cmd
 */
package com.java110.acct.cmd.account;

import com.alibaba.fastjson.JSONObject;
import com.java110.acct.bmo.account.IGetAccountBMO;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.smo.IOwnerGetDataCheck;
import com.java110.dto.account.AccountDto;
import com.java110.dto.contract.ContractDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.owner.OwnerRoomRelDto;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.intf.store.IContractInnerServiceSMO;
import com.java110.intf.user.IOwnerCarInnerServiceSMO;
import com.java110.intf.user.IOwnerRoomRelInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.List;

/**
 * 查询账户
 */
@Java110Cmd(serviceCode = "account.queryCommunityOwnerAccount")
public class QueryCommunityOwnerAccountCmd extends Cmd {

    /** 费用内部服务接口 */
    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    /** 业主房屋关系内部服务接口 */
    @Autowired
    private IOwnerRoomRelInnerServiceSMO ownerRoomRelInnerServiceSMOImpl;

    /** 业主车辆内部服务接口 */
    @Autowired
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl;

    /** 合同内部服务接口 */
    @Autowired
    private IContractInnerServiceSMO contractInnerServiceSMOImpl;

    /** 业主数据获取检查接口 */
    @Autowired
    private IOwnerGetDataCheck ownerGetDataCheckImpl;

    /** 获取账户业务模块对象接口 */
    @Autowired
    private IGetAccountBMO getAccountBMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，确保包含必要的参数信息
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     * @throws ParseException 当参数解析失败时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证必须包含小区ID参数
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
        // 验证分页信息参数
        super.validatePageInfo(reqJson);
        // 验证属性参数
        super.validateProperty(context);
    }

    /**
     * 命令执行方法
     * 
     * 处理查询业主账户的主要业务逻辑，包括参数转换、条件构建和账户查询
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行过程中出现错误时抛出命令异常
     * @throws ParseException 当参数解析失败时抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 将请求参数转换为账户数据传输对象
        AccountDto accountDto = BeanConvertUtil.covertBean(reqJson, AccountDto.class);

        // 处理费用ID参数，设置对应的业主ID
        hasFeeId(reqJson, accountDto);
        
        // 如果请求中包含业主ID，则设置到账户DTO中
        String ownerId = reqJson.getString("ownerId");
        if (!StringUtil.isEmpty(ownerId)) {
            accountDto.setObjId(ownerId);
        }

        // 设置账户对象类型为个人类型
        accountDto.setObjType(AccountDto.OBJ_TYPE_PERSON);
        // 设置分区ID为小区ID
        accountDto.setPartId(reqJson.getString("communityId"));

        // 处理账户类型参数，支持多账户类型查询
        String acctTypes = reqJson.getString("acctTypes");
        if (!StringUtil.isNullOrNone(acctTypes)) {
            // 将逗号分隔的账户类型字符串转换为数组
            accountDto.setAcctTypes(acctTypes.split(","));
        }

        //todo 业主账户安全性校验 - 待实现的安全校验逻辑
        // ownerGetDataCheckImpl.checkOwnerAccount(appId, userId, BeanConvertUtil.beanCovertJson(accountDto));

        // 构建业主查询条件
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setOwnerId(accountDto.getObjId());
        ownerDto.setCommunityId(reqJson.getString("communityId"));
        ownerDto.setLink(reqJson.getString("link"));
        ownerDto.setIdCard(reqJson.getString("idCard"));
        
        // 调用业务模块对象查询业主账户信息
        ResponseEntity<String> responseEntity = getAccountBMOImpl.queryOwnerAccount(accountDto, ownerDto);
        
        // 设置响应结果到上下文
        context.setResponseEntity(responseEntity);
    }

    /**
     * 判断参数中是否包含费用ID并处理相关逻辑
     * 
     * 如果请求中包含费用ID，则根据费用对象类型(房屋、车位、合同)查询对应的业主ID，
     * 并将业主ID设置到账户数据传输对象中
     * 
     * @param reqJson 请求参数JSON对象
     * @param accountDto 账户数据传输对象，用于设置查询到的业主ID
     */
    private void hasFeeId(JSONObject reqJson, AccountDto accountDto) {

        // 检查请求参数中是否包含费用ID
        if (!reqJson.containsKey("feeId")) {
            return;
        }
        
        String feeId = reqJson.getString("feeId");

        // 如果费用ID为空，直接返回
        if (StringUtil.isEmpty(feeId)) {
            return;
        }
        
        String ownerId = "";
        
        // 构建费用查询条件
        FeeDto feeDto = new FeeDto();
        feeDto.setFeeId(feeId);
        
        // 查询费用信息
        List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);
        
        // 验证查询结果唯一性
        Assert.listOnlyOne(feeDtos, "查询费用信息错误！");
        
        // 获取付费对象类型(3333 房屋 6666 是车位)
        String payerObjType = feeDtos.get(0).getPayerObjType();
        // 获取付费对象ID
        String payerObjId = feeDtos.get(0).getPayerObjId();
        
        // 根据付费对象类型处理不同的业务逻辑
        if (FeeDto.PAYER_OBJ_TYPE_ROOM.equals(payerObjType)) { 
            // 房屋类型：查询房屋绑定的业主关系
            OwnerRoomRelDto ownerRoomRelDto = new OwnerRoomRelDto();
            ownerRoomRelDto.setRoomId(payerObjId);
            List<OwnerRoomRelDto> ownerRoomRelDtos = ownerRoomRelInnerServiceSMOImpl.queryOwnerRoomRels(ownerRoomRelDto);
            
            // 验证房屋是否绑定业主关系
            if (ListUtil.isNull(ownerRoomRelDtos)) {
                throw new CmdException("房屋未绑定业主关系");
            }
            
            // 获取业主ID
            ownerId = ownerRoomRelDtos.get(0).getOwnerId();
            
        } else if (FeeDto.PAYER_OBJ_TYPE_CAR.equals(payerObjType)) {
            // 车位类型：查询车辆绑定的业主关系
            OwnerCarDto ownerCarDto = new OwnerCarDto();
            ownerCarDto.setCarId(payerObjId);
            List<OwnerCarDto> ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);
            
            // 获取业主ID（注释掉的唯一性验证）
            // Assert.listOnlyOne(ownerCarDtos, "查询业主车辆关系表错误！");
            ownerId = ownerCarDtos.get(0).getOwnerId();
            
        } else if (FeeDto.PAYER_OBJ_TYPE_CONTRACT.equals(payerObjType)) {
            // 合同类型：查询合同信息
            ContractDto contractDto = new ContractDto();
            contractDto.setContractId(payerObjId);
            List<ContractDto> contractDtos = contractInnerServiceSMOImpl.queryContracts(contractDto);
            
            // 获取合同对象ID作为业主ID（注释掉的唯一性验证）
            // Assert.listOnlyOne(ownerCarDtos, "查询业主车辆关系表错误！");
            ownerId = contractDtos.get(0).getObjId();
            
        } else {
            // 其他类型：设置默认业主ID
            ownerId = "-1";
        }
        
        // 将查询到的业主ID设置到账户DTO中
        accountDto.setObjId(ownerId);
    }
}