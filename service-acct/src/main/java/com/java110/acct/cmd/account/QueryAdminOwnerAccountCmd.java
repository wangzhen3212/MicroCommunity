package com.java110.acct.cmd.account;

import com.alibaba.fastjson.JSONObject;
import com.java110.acct.bmo.account.IGetAccountBMO;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
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
import com.java110.intf.user.IStaffCommunityV1InnerServiceSMO;
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
 * 查询账户命令类
 * 用于处理管理员查询业主账户信息的请求，支持通过业主ID、费用ID等多种方式查询账户信息
 * 该类继承自Cmd基类，实现了命令模式的验证和执行逻辑
 */
@Java110Cmd(serviceCode = "account.queryAdminOwnerAccount")
public class QueryAdminOwnerAccountCmd extends Cmd {

    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    @Autowired
    private IOwnerRoomRelInnerServiceSMO ownerRoomRelInnerServiceSMOImpl;

    @Autowired
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl;

    @Autowired
    private IContractInnerServiceSMO contractInnerServiceSMOImpl;

    @Autowired
    private IOwnerGetDataCheck ownerGetDataCheckImpl;

    @Autowired
    private IGetAccountBMO getAccountBMOImpl;

    @Autowired
    private IStaffCommunityV1InnerServiceSMO staffCommunityV1InnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 验证分页信息和管理员权限
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException  命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证管理员权限
        super.validateAdmin(context);
    }

    /**
     * 执行查询账户命令
     * 根据请求参数构建查询条件，调用BMO层查询业主账户信息
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException  命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求JSON转换为AccountDto对象
        AccountDto accountDto = BeanConvertUtil.covertBean(reqJson, AccountDto.class);

        // 获取当前用户ID
        String staffId = CmdContextUtils.getUserId(context);

        // 查询用户管理的社区ID列表
        List<String> communityIds = staffCommunityV1InnerServiceSMOImpl.queryStaffCommunityIds(staffId);

        // 如果用户有管理的社区，设置到查询条件中
        if (!ListUtil.isNull(communityIds)) {
            accountDto.setCommunityIds(communityIds.toArray(new String[communityIds.size()]));
        }

        // 处理费用ID参数，获取对应的业主ID
        hasFeeId(reqJson, accountDto);
        
        // 处理直接传递的业主ID参数
        String ownerId = reqJson.getString("ownerId");
        if (!StringUtil.isEmpty(ownerId)) {
            accountDto.setObjId(ownerId);
        }

        // 设置查询条件：对象类型为个人，分区ID为社区ID
        accountDto.setObjType(AccountDto.OBJ_TYPE_PERSON);
        accountDto.setPartId(reqJson.getString("communityId"));

        // 处理账户类型参数
        String acctTypes = reqJson.getString("acctTypes");
        if (!StringUtil.isNullOrNone(acctTypes)) {
            accountDto.setAcctTypes(acctTypes.split(","));
        }

        // TODO: 业主账户安全性校验（待实现）
        // ownerGetDataCheckImpl.checkOwnerAccount(appId, userId, BeanConvertUtil.beanCovertJson(accountDto));

        // 构建业主查询条件
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setOwnerId(accountDto.getObjId());
        ownerDto.setCommunityId(reqJson.getString("communityId"));
        ownerDto.setLink(reqJson.getString("link"));
        ownerDto.setIdCard(reqJson.getString("idCard"));
        
        // 调用BMO层查询业主账户信息
        ResponseEntity<String> responseEntity = getAccountBMOImpl.queryOwnerAccount(accountDto, ownerDto);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 判断参数中是否有feeId，并根据feeId获取对应的业主ID
     * 支持房屋、车位、合同等多种付费对象类型
     *
     * @param reqJson    请求的JSON数据
     * @param accountDto 账户数据传输对象
     */
    private void hasFeeId(JSONObject reqJson, AccountDto accountDto) {
        // 检查请求中是否包含feeId参数
        if (!reqJson.containsKey("feeId")) {
            return;
        }
        
        String feeId = reqJson.getString("feeId");

        // 如果feeId为空，直接返回
        if (StringUtil.isEmpty(feeId)) {
            return;
        }
        
        String ownerId = "";
        
        // 根据feeId查询费用信息
        FeeDto feeDto = new FeeDto();
        feeDto.setFeeId(feeId);
        List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);
        
        // 验证查询结果唯一性
        Assert.listOnlyOne(feeDtos, "查询费用信息错误！");
        
        // 获取付费对象类型和ID
        String payerObjType = feeDtos.get(0).getPayerObjType();  // 3333 房屋 6666 是车位
        String payerObjId = feeDtos.get(0).getPayerObjId();
        
        // 根据不同的付费对象类型处理
        if (FeeDto.PAYER_OBJ_TYPE_ROOM.equals(payerObjType)) { 
            // 房屋类型：查询房屋与业主的关系
            OwnerRoomRelDto ownerRoomRelDto = new OwnerRoomRelDto();
            ownerRoomRelDto.setRoomId(payerObjId);
            List<OwnerRoomRelDto> ownerRoomRelDtos = ownerRoomRelInnerServiceSMOImpl.queryOwnerRoomRels(ownerRoomRelDto);
            
            if (ListUtil.isNull(ownerRoomRelDtos)) {
                throw new CmdException("房屋未绑定业主关系");
            }
            ownerId = ownerRoomRelDtos.get(0).getOwnerId();
            
        } else if (FeeDto.PAYER_OBJ_TYPE_CAR.equals(payerObjType)) {
            // 车位类型：查询车位与业主的关系
            OwnerCarDto ownerCarDto = new OwnerCarDto();
            ownerCarDto.setCarId(payerObjId);
            List<OwnerCarDto> ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);
            ownerId = ownerCarDtos.get(0).getOwnerId();
            
        } else if (FeeDto.PAYER_OBJ_TYPE_CONTRACT.equals(payerObjType)) {
            // 合同类型：查询合同信息
            ContractDto contractDto = new ContractDto();
            contractDto.setContractId(payerObjId);
            List<ContractDto> contractDtos = contractInnerServiceSMOImpl.queryContracts(contractDto);
            ownerId = contractDtos.get(0).getObjId();
            
        } else {
            // 其他类型：设置默认值
            ownerId = "-1";
        }
        
        // 将获取到的业主ID设置到账户DTO中
        accountDto.setObjId(ownerId);
    }
}