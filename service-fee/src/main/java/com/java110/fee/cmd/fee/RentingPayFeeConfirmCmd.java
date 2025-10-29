package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.community.CommunityMemberDto;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.renting.RentingPoolDto;
import com.java110.intf.community.ICommunityInnerServiceSMO;
import com.java110.intf.fee.IFeeAttrInnerServiceSMO;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.intf.fee.IPayFeeDetailV1InnerServiceSMO;
import com.java110.intf.fee.IPayFeeV1InnerServiceSMO;
import com.java110.intf.order.IOrderInnerServiceSMO;
import com.java110.intf.user.IRentingPoolInnerServiceSMO;
import com.java110.po.fee.PayFeeDetailPo;
import com.java110.po.fee.PayFeePo;
import com.java110.po.renting.RentingPoolPo;
import com.java110.utils.constant.CommonConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;

/**
 * 租赁费用确认命令类
 * 处理租赁费用支付确认业务，包括物业、代理商和运营方的费用分成计算和记录
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "fee.rentingPayFeeConfirm")
public class RentingPayFeeConfirmCmd extends Cmd {

    @Autowired
    private IFeeAttrInnerServiceSMO feeAttrInnerServiceSMOImpl;

    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    @Autowired
    private IOrderInnerServiceSMO orderInnerServiceSMOImpl;

    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    @Autowired
    private IRentingPoolInnerServiceSMO rentingPoolInnerServiceSMOImpl;

    @Autowired
    private IPayFeeV1InnerServiceSMO payFeeV1InnerServiceSMOImpl;

    @Autowired
    private IPayFeeDetailV1InnerServiceSMO payFeeDetailV1InnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 检查请求报文中是否包含必要的订单信息
     *
     * @param event   命令事件对象
     * @param context 数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求报文中是否包含订单ID
        Assert.jsonObjectHaveKey(reqJson, "oId", "请求报文中未包含订单信息");
        // 验证订单ID不能为空
        Assert.hasLength(reqJson.getString("oId"), "订单信息不能为空");
    }

    /**
     * 执行租赁费用确认命令
     * 处理费用分成逻辑，包括物业、代理商和运营方的费用计算和记录
     *
     * @param event    命令事件对象
     * @param context  数据流上下文
     * @param paramObj 请求参数对象
     * @throws CmdException    当业务处理失败时抛出异常
     * @throws ParseException  当日期解析失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject paramObj) throws CmdException, ParseException {
        // 从参数中获取订单ID、金额和费用名称
        String oId = paramObj.getString("oId");
        String money = paramObj.getString("money");
        String feeName = paramObj.getString("feeName");
        
        // 将订单ID设置到请求头中
        context.getReqHeaders().put(CommonConstant.O_ID, oId);
        
        // 将参数对象转换为租赁池DTO
        RentingPoolDto rentingPoolDto = BeanConvertUtil.covertBean(paramObj, RentingPoolDto.class);
        int flag = 0; // 操作结果标志

        JSONArray businesses = new JSONArray();
        BigDecimal serviceDec = null; // 服务费率
        BigDecimal payMoney = null; // 支付金额
        double receivableAmount = 0.0; // 应收金额
        PayFeeDetailPo payFeeDetailPo = null; // 费用明细对象
        CommunityMemberDto communityMemberDto = null; // 社区成员DTO
        List<CommunityMemberDto> communityMemberDtos = null; // 社区成员列表
        PayFeePo payFeePo = null; // 费用对象

        // 物业收取费用处理
        double propertyRate = Double.parseDouble(rentingPoolDto.getPropertySeparateRate());
        if (propertyRate > 0) {
            // 计算物业应收金额
            serviceDec = new BigDecimal(propertyRate);
            payMoney = new BigDecimal(money);
            receivableAmount = serviceDec.multiply(payMoney).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
            
            // 创建费用明细记录
            payFeeDetailPo = new PayFeeDetailPo();
            payFeeDetailPo.setDetailId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_detailId));
            payFeeDetailPo.setEndTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
            payFeeDetailPo.setStartTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
            payFeeDetailPo.setFeeId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_feeId));
            payFeeDetailPo.setPrimeRate("1.0"); // 设置基础费率
            payFeeDetailPo.setCycles("1"); // 设置周期数
            payFeeDetailPo.setReceivableAmount(receivableAmount + ""); // 应收金额
            payFeeDetailPo.setReceivedAmount(receivableAmount + ""); // 实收金额
            payFeeDetailPo.setCommunityId(rentingPoolDto.getCommunityId()); // 社区ID
            payFeeDetailPo.setPayOrderId(payFeeDetailPo.getDetailId()); // 支付订单ID
            payFeeDetailPo.setCashierId("-1"); // 收银员ID（系统）
            payFeeDetailPo.setCashierName("系统收银"); // 收银员名称
            payFeeDetailPo.setState("1400"); // 费用状态
            
            // 保存费用明细记录
            flag = payFeeDetailV1InnerServiceSMOImpl.savePayFeeDetailNew(payFeeDetailPo);
            if (flag < 1) {
                throw new CmdException("添加费用异常");
            }

            // 查询物业信息
            communityMemberDto = new CommunityMemberDto();
            communityMemberDto.setCommunityId(rentingPoolDto.getCommunityId());
            communityMemberDto.setMemberTypeCd("390001200002"); // 物业类型编码
            communityMemberDto.setAuditStatusCd(CommunityMemberDto.AUDIT_STATUS_NORMAL); // 正常审核状态
            communityMemberDtos = communityInnerServiceSMOImpl.getCommunityMembers(communityMemberDto);

            // 验证只能查询到一个物业信息
            Assert.listOnlyOne(communityMemberDtos, "物业信息查询有误");

            // 创建物业费用记录
            String propertyId = communityMemberDtos.get(0).getMemberId();
            payFeePo = new PayFeePo();
            payFeePo.setFeeId(payFeeDetailPo.getFeeId()); // 费用ID
            payFeePo.setFeeFlag(FeeDto.FEE_FLAG_ONCE); // 一次性费用标志
            payFeePo.setIncomeObjId(propertyId); // 收入对象ID（物业）
            payFeePo.setCommunityId(rentingPoolDto.getCommunityId()); // 社区ID
            payFeePo.setUserId("-1"); // 用户ID（系统）
            payFeePo.setPayerObjId(rentingPoolDto.getRentingId()); // 付款对象ID（租赁）
            payFeePo.setEndTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A)); // 结束时间
            payFeePo.setFeeTypeCd(FeeConfigDto.FEE_TYPE_CD_SYSTEM); // 系统费用类型
            payFeePo.setAmount(receivableAmount + ""); // 费用金额
            payFeePo.setState(FeeDto.STATE_FINISH); // 已完成状态
            payFeePo.setStartTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A)); // 开始时间
            payFeePo.setPayerObjType(FeeDto.PAYER_OBJ_TYPE_RENTING); // 付款对象类型（租赁）
            payFeePo.setConfigId(FeeConfigDto.CONFIG_ID_RENTING); // 租赁配置ID
            
            // 保存费用记录
            flag = payFeeV1InnerServiceSMOImpl.savePayFee(payFeePo);
            if (flag < 1) {
                throw new CmdException("添加费用异常");
            }
        }

        // 代理商分成处理
        double proxyRate = Double.parseDouble(rentingPoolDto.getProxySeparateRate());
        if (proxyRate > 0) {
            // 计算代理商应收金额
            serviceDec = new BigDecimal(proxyRate);
            payMoney = new BigDecimal(money);
            receivableAmount = serviceDec.multiply(payMoney).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
            
            // 创建代理商费用明细记录
            payFeeDetailPo = new PayFeeDetailPo();
            payFeeDetailPo.setDetailId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_detailId));
            payFeeDetailPo.setEndTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
            payFeeDetailPo.setStartTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
            payFeeDetailPo.setFeeId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_feeId));
            payFeeDetailPo.setPrimeRate("1.0");
            payFeeDetailPo.setCycles("1");
            payFeeDetailPo.setReceivableAmount(receivableAmount + "");
            payFeeDetailPo.setReceivedAmount(receivableAmount + "");
            payFeeDetailPo.setCommunityId(rentingPoolDto.getCommunityId());
            payFeeDetailPo.setPayOrderId(payFeeDetailPo.getDetailId());
            payFeeDetailPo.setCashierId("-1");
            payFeeDetailPo.setCashierName("系统收银");
            
            // 保存代理商费用明细记录
            flag = payFeeDetailV1InnerServiceSMOImpl.savePayFeeDetailNew(payFeeDetailPo);
            if (flag < 1) {
                throw new CmdException("添加费用异常");
            }

            // 查询代理商信息
            communityMemberDto = new CommunityMemberDto();
            communityMemberDto.setCommunityId(rentingPoolDto.getCommunityId());
            communityMemberDto.setMemberTypeCd("390001200003"); // 代理商类型编码
            communityMemberDto.setAuditStatusCd(CommunityMemberDto.AUDIT_STATUS_NORMAL);
            communityMemberDtos = communityInnerServiceSMOImpl.getCommunityMembers(communityMemberDto);

            // 验证只能查询到一个代理商信息
            Assert.listOnlyOne(communityMemberDtos, "代理商信息查询有误");

            // 创建代理商费用记录
            String proxyId = communityMemberDtos.get(0).getMemberId();
            payFeePo = new PayFeePo();
            payFeePo.setFeeId(payFeeDetailPo.getFeeId());
            payFeePo.setFeeFlag(FeeDto.FEE_FLAG_ONCE);
            payFeePo.setIncomeObjId(proxyId); // 收入对象ID（代理商）
            payFeePo.setCommunityId(rentingPoolDto.getCommunityId());
            payFeePo.setUserId("-1");
            payFeePo.setPayerObjId(rentingPoolDto.getRentingId());
            payFeePo.setEndTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
            payFeePo.setFeeTypeCd(FeeConfigDto.FEE_TYPE_CD_SYSTEM);
            payFeePo.setAmount(receivableAmount + "");
            payFeePo.setState(FeeDto.STATE_FINISH);
            payFeePo.setStartTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
            payFeePo.setPayerObjType(FeeDto.PAYER_OBJ_TYPE_RENTING);
            payFeePo.setConfigId(FeeConfigDto.CONFIG_ID_RENTING);
            
            // 保存代理商费用记录
            flag = payFeeV1InnerServiceSMOImpl.savePayFee(payFeePo);
            if (flag < 1) {
                throw new CmdException("添加费用异常");
            }
        }

        // 运营分成处理
        double adminRate = Double.parseDouble(rentingPoolDto.getAdminSeparateRate());
        if (adminRate > 0) {
            // 计算运营应收金额
            serviceDec = new BigDecimal(adminRate);
            payMoney = new BigDecimal(money);
            receivableAmount = serviceDec.multiply(payMoney).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
            
            // 创建运营费用明细记录
            payFeeDetailPo = new PayFeeDetailPo();
            payFeeDetailPo.setDetailId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_detailId));
            payFeeDetailPo.setEndTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
            payFeeDetailPo.setStartTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
            payFeeDetailPo.setFeeId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_feeId));
            payFeeDetailPo.setPrimeRate("1.0");
            payFeeDetailPo.setCycles("1");
            payFeeDetailPo.setReceivableAmount(receivableAmount + "");
            payFeeDetailPo.setReceivedAmount(receivableAmount + "");
            payFeeDetailPo.setCommunityId(rentingPoolDto.getCommunityId());
            payFeeDetailPo.setPayOrderId(payFeeDetailPo.getDetailId());
            payFeeDetailPo.setCashierId("-1");
            payFeeDetailPo.setCashierName("系统收银");
            
            // 保存运营费用明细记录
            flag = payFeeDetailV1InnerServiceSMOImpl.savePayFeeDetailNew(payFeeDetailPo);
            if (flag < 1) {
                throw new CmdException("添加费用异常");
            }

            // 查询运营信息
            communityMemberDto = new CommunityMemberDto();
            communityMemberDto.setCommunityId(rentingPoolDto.getCommunityId());
            communityMemberDto.setMemberTypeCd("390001200000"); // 运营类型编码
            communityMemberDto.setAuditStatusCd(CommunityMemberDto.AUDIT_STATUS_NORMAL);
            communityMemberDtos = communityInnerServiceSMOImpl.getCommunityMembers(communityMemberDto);

            // 验证只能查询到一个运营信息
            Assert.listOnlyOne(communityMemberDtos, "代理商信息查询有误");

            // 创建运营费用记录
            String adminId = communityMemberDtos.get(0).getMemberId();
            payFeePo = new PayFeePo();
            payFeePo.setFeeId(payFeeDetailPo.getFeeId());
            payFeePo.setFeeFlag(FeeDto.FEE_FLAG_ONCE);
            payFeePo.setIncomeObjId(adminId); // 收入对象ID（运营）
            payFeePo.setCommunityId(rentingPoolDto.getCommunityId());
            payFeePo.setUserId("-1");
            payFeePo.setPayerObjId(rentingPoolDto.getRentingId());
            payFeePo.setEndTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
            payFeePo.setFeeTypeCd(FeeConfigDto.FEE_TYPE_CD_SYSTEM);
            payFeePo.setAmount(receivableAmount + "");
            payFeePo.setState(FeeDto.STATE_FINISH);
            payFeePo.setStartTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
            payFeePo.setPayerObjType(FeeDto.PAYER_OBJ_TYPE_RENTING);
            payFeePo.setConfigId(FeeConfigDto.CONFIG_ID_RENTING);
            
            // 保存运营费用记录
            flag = payFeeV1InnerServiceSMOImpl.savePayFee(payFeePo);
            if (flag < 1) {
                throw new CmdException("添加费用异常");
            }
        }

        // 更新租赁池状态
        RentingPoolPo rentingPoolPo = new RentingPoolPo();
        rentingPoolPo.setRentingId(rentingPoolDto.getRentingId()); // 租赁ID
        rentingPoolPo.setCommunityId(rentingPoolDto.getCommunityId()); // 社区ID
        // 根据当前状态设置新的状态：待支付转为业主待支付，其他转为申请同意
        rentingPoolPo.setState(RentingPoolDto.STATE_TO_PAY.equals(rentingPoolDto.getState()) ? 
                              RentingPoolDto.STATE_OWNER_TO_PAY : RentingPoolDto.STATE_APPLY_AGREE);
        rentingPoolInnerServiceSMOImpl.updateRentingPool(rentingPoolPo);
    }
}