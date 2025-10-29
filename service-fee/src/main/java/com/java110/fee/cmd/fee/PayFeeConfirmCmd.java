/**
 * 费用支付确认命令类
 * 
 * 该类负责处理费用支付确认相关的业务逻辑，包括：
 * 1. 处理现金账户和积分账户的扣款
 * 2. 处理优惠券的使用和状态更新
 * 3. 添加费用折扣信息
 * 4. 更新相关业务状态（停车费、报修单、租金延期等）
 * 5. 生成费用明细记录
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.account.AccountDto;
import com.java110.dto.app.AppDto;
import com.java110.dto.coupon.CouponUserDto;
import com.java110.dto.fee.FeeAttrDto;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.dto.fee.FeeDetailDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.fee.ComputeDiscountDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.parking.ParkingSpaceApplyDto;
import com.java110.dto.repair.RepairDto;
import com.java110.dto.repair.RepairUserDto;
import com.java110.fee.bmo.fee.IFeeBMO;
import com.java110.fee.bmo.fee.IFinishFeeNotify;
import com.java110.intf.acct.IAccountInnerServiceSMO;
import com.java110.intf.acct.ICouponUserDetailV1InnerServiceSMO;
import com.java110.intf.acct.ICouponUserV1InnerServiceSMO;
import com.java110.intf.community.*;
import com.java110.intf.fee.*;
import com.java110.intf.user.IOwnerCarInnerServiceSMO;
import com.java110.po.account.AccountDetailPo;
import com.java110.po.room.ApplyRoomDiscountPo;
import com.java110.po.car.OwnerCarPo;
import com.java110.po.coupon.CouponUserPo;
import com.java110.po.coupon.CouponUserDetailPo;
import com.java110.po.fee.FeeAccountDetailPo;
import com.java110.po.owner.RepairPoolPo;
import com.java110.po.owner.RepairUserPo;
import com.java110.po.parking.ParkingSpaceApplyPo;
import com.java110.po.payFee.PayFeeDetailDiscountPo;
import com.java110.utils.cache.CommonCache;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.*;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Java110Cmd(serviceCode = "fee.payFeeConfirm")
public class PayFeeConfirmCmd extends Cmd {

    @Autowired
    private IFeeBMO feeBMOImpl;

    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    @Autowired
    private IFeeDiscountInnerServiceSMO feeDiscountInnerServiceSMOImpl;

    @Autowired
    private IFeeAttrInnerServiceSMO feeAttrInnerServiceSMOImpl;

    @Autowired
    private IRepairUserInnerServiceSMO repairUserInnerServiceSMO;

    @Autowired
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl;

    @Autowired
    private IOwnerCarNewV1InnerServiceSMO ownerCarNewV1InnerServiceSMOImpl;

    @Autowired
    private IApplyRoomDiscountInnerServiceSMO applyRoomDiscountInnerServiceSMOImpl;

    @Autowired
    private IFeeDetailInnerServiceSMO iFeeDetailInnerServiceSMO;
    @Autowired
    private IPayFeeDetailDiscountInnerServiceSMO payFeeDetailDiscountInnerServiceSMOImpl;
    @Autowired
    private IPayFeeDetailDiscountNewV1InnerServiceSMO payFeeDetailDiscountNewV1InnerServiceSMOImpl;
    @Autowired
    private IRepairPoolV1InnerServiceSMO repairPoolNewV1InnerServiceSMOImpl;
    @Autowired
    private IRepairUserV1InnerServiceSMO repairUserNewV1InnerServiceSMOImpl;
    @Autowired
    private ICouponUserV1InnerServiceSMO couponUserV1InnerServiceSMOImpl;
    @Autowired
    private ICouponUserDetailV1InnerServiceSMO couponUserDetailV1InnerServiceSMOImpl;
    @Autowired
    private IParkingSpaceApplyV1InnerServiceSMO parkingSpaceApplyV1InnerServiceSMOImpl;
    @Autowired
    private IAccountInnerServiceSMO accountInnerServiceSMOImpl;

    @Autowired
    private IFeeAccountDetailServiceSMO feeAccountDetailServiceSMOImpl;

    @Autowired
    private IFinishFeeNotify finishFeeNotifyImpl;

    //默认序列
    protected static final int DEFAULT_SEQ = 1;

    /**
     * 参数验证方法
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中是否包含订单ID
        Assert.hasKeyAndValue(reqJson, "oId", "是否包含订单信息");
    }

    /**
     * 执行费用支付确认命令
     * 
     * 主要业务流程：
     * 1. 从缓存中获取预支付信息
     * 2. 设置支付渠道相关参数
     * 3. 处理账户扣款
     * 4. 处理优惠折扣
     * 5. 更新优惠券状态
     * 6. 添加费用明细
     * 7. 更新相关业务状态
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 从缓存中获取预支付信息并移除
        String paramIn = CommonCache.getAndRemoveValue("payFeePre" + reqJson.getString("oId"));
        JSONObject paramObj = JSONObject.parseObject(paramIn);

        String appId = paramObj.getString("appId");

        // 根据应用ID设置不同的支付渠道参数
        if (AppDto.WECHAT_MINA_OWNER_APP_ID.equals(appId)) {  //微信小程序支付
            paramObj.put("primeRate", "6");
            paramObj.put("remark", "线上小程序支付");
        } else if (AppDto.WECHAT_OWNER_APP_ID.equals(appId)) {  //微信公众号支付
            paramObj.put("primeRate", "5");
            paramObj.put("remark", "线上公众号支付");
        } else {
            paramObj.put("primeRate", "6");
            paramObj.put("remark", "线上小程序支付");
        }
        // 生成明细ID
        paramObj.put("detailId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_detailId));

        //处理现金账户
        dealAccount(paramObj);

        //处理优惠折扣
        addDiscount(paramObj);

        //修改已使用优惠卷信息
        modifyCouponUser(paramObj);

        //添加单元信息
        feeBMOImpl.addFeePreDetail(paramObj);
        feeBMOImpl.modifyPreFee(paramObj);

        //todo 为停车费单独处理 - 更新车辆结束时间
        finishFeeNotifyImpl.updateCarEndTime(paramObj.getString("feeId"), paramObj.getString("communityId"));

        //todo 修改报修单 - 更新报修单状态
        finishFeeNotifyImpl.updateRepair(paramObj.getString("feeId"), paramObj.getString("communityId"), paramObj.getString("receivedAmount"));

        //todo 租金延期房屋结束时间 - 更新房屋租赁结束时间
        finishFeeNotifyImpl.updateRoomEndTime(paramObj.getString("feeId"), paramObj.getString("communityId"));

        //查询 pay_fee_detail 是否缴费
        FeeDetailDto feeDetailDto = new FeeDetailDto();
        feeDetailDto.setDetailId(paramObj.getString("detailId"));
        List<FeeDetailDto> feeDetailDtoList = iFeeDetailInnerServiceSMO.queryBusinessFeeDetails(feeDetailDto);
        if (feeDetailDtoList != null && feeDetailDtoList.size() == 1) {
            //获取bId
            String bId = feeDetailDtoList.get(0).getbId();
            //获取优惠信息
            JSONArray computeDiscountDtos = paramObj.getJSONArray("computeDiscountDtos");
            ComputeDiscountDto computeDiscountDto = null;
            if (!ListUtil.isNull(computeDiscountDtos)) {
                for (int accountIndex = 0; accountIndex < computeDiscountDtos.size(); accountIndex++) {
                    computeDiscountDto = BeanConvertUtil.covertBean(computeDiscountDtos.getJSONObject(accountIndex), ComputeDiscountDto.class);
                    if (!StringUtil.isEmpty(computeDiscountDto.getArdId())) {
                        // 更新空置房优惠信息
                        ApplyRoomDiscountPo applyRoomDiscountPo = new ApplyRoomDiscountPo();
                        //将业务id更新到空置房优惠里面
                        applyRoomDiscountPo.setbId(bId);
                        applyRoomDiscountPo.setArdId(computeDiscountDto.getArdId());
                        int flag = applyRoomDiscountInnerServiceSMOImpl.updateApplyRoomDiscount(applyRoomDiscountPo);
                        if (flag < 1) {
                            throw new CmdException("更新微信派单池信息失败");
                        }
                    }
                }
            }
        }
        
        //回调判断 车位申请表是否有数据，有数据则刷新申请表状态为 3003 完成状态即可
        //判断车辆是否已经有申请单
        ParkingSpaceApplyDto parkingSpaceApplyDto = new ParkingSpaceApplyDto();
        parkingSpaceApplyDto.setFeeId(paramObj.getString("feeId"));
        parkingSpaceApplyDto.setState("2002");//审核中
        List<ParkingSpaceApplyDto> parkingSpaceApplyDtos = parkingSpaceApplyV1InnerServiceSMOImpl.queryParkingSpaceApplys(parkingSpaceApplyDto);
        if (!ListUtil.isNull(parkingSpaceApplyDtos)) {
            // 更新车位申请表状态为完成
            ParkingSpaceApplyPo parkingSpaceApplyPo = new ParkingSpaceApplyPo();
            parkingSpaceApplyPo.setApplyId(parkingSpaceApplyDtos.get(0).getApplyId());
            parkingSpaceApplyPo.setState("3003");
            int flag = parkingSpaceApplyV1InnerServiceSMOImpl.updateParkingSpaceApply(parkingSpaceApplyPo);
            if (flag < 1) {
                throw new CmdException("更新车位申请表状态失败");
            }
        }
        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }

    /**
     * 处理账户扣款逻辑
     * 
     * 处理现金账户和积分账户的扣款操作：
     * 1. 现金账户扣款
     * 2. 积分账户扣款
     * 3. 生成账户扣款记录
     * 4. 更新支付备注信息
     * 
     * @param paramObj 支付参数对象
     * @throws CmdException 命令执行异常
     */
    private void dealAccount(JSONObject paramObj) {
        // 获取选择的用户账户列表
        JSONArray accountDtos = paramObj.getJSONArray("selectUserAccount");
        if(ListUtil.isNull(accountDtos)){
            return;
        }
        AccountDto accountDto = null;
        String accountRemark = ""; // 现金账户扣款备注
        String pointRemark = "";   // 积分账户扣款备注
        
        for (int accountIndex = 0; accountIndex < accountDtos.size(); accountIndex++) {
            accountDto = BeanConvertUtil.covertBean(accountDtos.getJSONObject(accountIndex), AccountDto.class);
            
            if (!StringUtil.isEmpty(accountDto.getAcctType()) && accountDto.getAcctType().equals("2003")) { 
                //2003 现金账户 2004 积分账户
                //获取现金账户抵扣金额
                BigDecimal cashMoney = new BigDecimal(0.0);
                if (!StringUtil.isEmpty(paramObj.getString("cashMoney"))) {
                    String cashMoney1 = paramObj.getString("cashMoney");
                    cashMoney = new BigDecimal(cashMoney1);
                    if (cashMoney.compareTo(BigDecimal.ZERO) > 0) {
                        accountRemark = "现金账户抵扣" + String.format("%.2f", cashMoney) + "元";
                    }
                }
                int i = cashMoney.compareTo(BigDecimal.ZERO);
                if (i > 0) {
                    BigDecimal amount = new BigDecimal(Double.parseDouble(accountDto.getAmount())); //现金账户余额
                    // 执行现金账户扣款
                    AccountDetailPo accountDetailPo = new AccountDetailPo();
                    accountDetailPo.setAcctId(accountDto.getAcctId());
                    accountDetailPo.setObjId(accountDto.getObjId());
                    accountDetailPo.setObjType(accountDto.getObjType());
                    accountDetailPo.setAmount(String.valueOf(cashMoney));
                    int flag = accountInnerServiceSMOImpl.withholdAccount(accountDetailPo);
                    if (flag < 1) {
                        throw new CmdException("现金账户扣款失败！");
                    }
                    // 现金账户扣款记录，生成抵扣明细记录
                    FeeAccountDetailPo feeAccountDetailPo = new FeeAccountDetailPo();
                    feeAccountDetailPo.setFadId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_fadId));
                    feeAccountDetailPo.setDetailId(paramObj.getString("detailId"));
                    feeAccountDetailPo.setCommunityId(paramObj.getString("communityId"));
                    feeAccountDetailPo.setState("1002"); //1001 无抵扣 1002 现金账户抵扣 1003 积分账户抵扣 1004 优惠券抵扣
                    feeAccountDetailPo.setAmount(String.valueOf(cashMoney)); //现金抵扣金额
                    feeAccountDetailServiceSMOImpl.saveFeeAccountDetail(feeAccountDetailPo);
                }

            } else if (!StringUtil.isEmpty(accountDto.getAcctType()) && accountDto.getAcctType().equals("2004")) { 
                //积分账户
                //获取积分账户抵扣的积分数
                BigDecimal pointsMoney = new BigDecimal(0.0);
                BigDecimal pointsMoneyNow = new BigDecimal(0.0);
                if (!StringUtil.isEmpty(paramObj.getString("pointsMoney"))) {
                    String pointsMoney1 = paramObj.getString("pointsMoney");
                    pointsMoney = new BigDecimal(pointsMoney1);
                    String pointsMoneyNow1 = paramObj.getString("pointsMoneyNow");
                    pointsMoneyNow = new BigDecimal(pointsMoneyNow1);
                    if (pointsMoneyNow.compareTo(BigDecimal.ZERO) > 0) {
                        pointRemark = "积分账户抵扣" + String.format("%.2f", pointsMoneyNow) + "元";
                    }

                }
                int i = pointsMoney.compareTo(BigDecimal.ZERO);
                if (i > 0) {
                    BigDecimal amount = new BigDecimal(Double.parseDouble(accountDto.getAmount())); //积分账户积分数
                    // 执行积分账户扣款
                    AccountDetailPo accountDetailPo = new AccountDetailPo();
                    accountDetailPo.setAcctId(accountDto.getAcctId());
                    accountDetailPo.setObjId(accountDto.getObjId());
                    accountDetailPo.setObjType(accountDto.getObjType());
                    accountDetailPo.setAmount(String.valueOf(pointsMoney));
                    int flag = accountInnerServiceSMOImpl.withholdAccount(accountDetailPo);
                    if (flag < 1) {
                        throw new CmdException("积分账户扣款失败！");
                    }

                    // 积分账户扣款记录，生成抵扣明细记录
                    FeeAccountDetailPo feeAccountDetailPo = new FeeAccountDetailPo();
                    feeAccountDetailPo.setFadId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_fadId));
                    feeAccountDetailPo.setDetailId(paramObj.getString("detailId"));
                    feeAccountDetailPo.setCommunityId(paramObj.getString("communityId"));
                    feeAccountDetailPo.setState("1003"); //1001 无抵扣 1002 现金账户抵扣 1003 积分账户抵扣 1004 优惠券抵扣

                    feeAccountDetailPo.setAmount(String.valueOf(paramObj.getString("pointsMoneyNow"))); //现金抵扣金额
                    feeAccountDetailServiceSMOImpl.saveFeeAccountDetail(feeAccountDetailPo);
                }
            }
        }
        // 更新支付备注信息，包含账户扣款信息
        if (!StringUtil.isEmpty(accountRemark) && !StringUtil.isEmpty(pointRemark)) {
            paramObj.put("remark", paramObj.getString("remark") + "，" + accountRemark + "，" + pointRemark);
        } else {
            paramObj.put("remark", paramObj.getString("remark") + "，" + accountRemark + pointRemark);
        }

    }

    /**
     * 修改优惠券使用状态
     * 
     * 处理优惠券的使用逻辑：
     * 1. 验证优惠券是否可用
     * 2. 更新优惠券状态为已使用
     * 3. 记录优惠券使用明细
     * 4. 更新支付备注信息
     * 
     * @param paramObj 支付参数对象
     * @throws CmdException 命令执行异常
     */
    private void modifyCouponUser(JSONObject paramObj) {
        // 检查是否有优惠券金额且金额大于0
        if (!paramObj.containsKey("couponPrice") || paramObj.getDouble("couponPrice") <= 0) {
            return;
        }
        
        CouponUserDto couponUserDto = null;
        JSONArray couponUserDtos = paramObj.getJSONArray("couponUserDtos");
        if(ListUtil.isNull(couponUserDtos)){
            return ;
        }
        CouponUserDto couponUser = null;
        
        for (int accountIndex = 0; accountIndex < couponUserDtos.size(); accountIndex++) {
            couponUser = BeanConvertUtil.covertBean(couponUserDtos.getJSONObject(accountIndex), CouponUserDto.class);
            
            // 验证优惠券是否可用
            couponUserDto = new CouponUserDto();
            couponUserDto.setCouponId(couponUser.getCouponId());
            couponUserDto.setState(CouponUserDto.COUPON_STATE_RUN);
            List<CouponUserDto> couponUserDtos1 = couponUserV1InnerServiceSMOImpl.queryCouponUsers(couponUserDto);
            if (couponUserDtos1 == null || couponUserDtos1.size() < 1) {
                throw new CmdException("优惠券被使用");
            }
            
            // 更新优惠券状态为已停止使用
            CouponUserPo couponUserPo = new CouponUserPo();
            couponUserPo.setState(CouponUserDto.COUPON_STATE_STOP);
            couponUserPo.setCouponId(couponUser.getCouponId());
            int fage = couponUserV1InnerServiceSMOImpl.updateCouponUser(couponUserPo);
            if (fage < 1) {
                throw new CmdException("更新优惠卷信息失败");
            }
            
            // 记录优惠券使用明细
            CouponUserDetailPo couponUserDetailPo = new CouponUserDetailPo();
            couponUserDetailPo.setUoId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_usId));
            couponUserDetailPo.setCouponId(couponUser.getCouponId());
            couponUserDetailPo.setUserId(couponUser.getUserId());
            couponUserDetailPo.setCouponName(couponUser.getCouponName());
            couponUserDetailPo.setUserName(couponUser.getUserName());
            couponUserDetailPo.setObjId(paramObj.getString("feeId"));
            couponUserDetailPo.setObjType(paramObj.getString("feeTypeCd"));
            couponUserDetailPo.setOrderId(paramObj.getString("oId"));
            fage = couponUserDetailV1InnerServiceSMOImpl.saveCouponUserDetail(couponUserDetailPo);
            if (fage < 1) {
                throw new CmdException("新增优惠卷使用记录信息失败");
            }
        }

        // 更新支付备注信息，包含优惠券抵扣信息
        paramObj.put("remark", paramObj.getString("remark") + "-优惠劵抵扣" + paramObj.getDouble("couponPrice") + "元");

    }

    /**
     * 添加折扣信息
     * 
     * 处理费用折扣逻辑：
     * 1. 检查是否有折扣金额
     * 2. 遍历折扣列表
     * 3. 为每个有效折扣生成折扣记录
     * 
     * @param paramObj 支付参数对象
     * @throws CmdException 命令执行异常
     */
    private void addDiscount(JSONObject paramObj) {
        // 检查是否有折扣金额且金额大于0
        if (!paramObj.containsKey("discountPrice") || paramObj.getDouble("discountPrice") <= 0) {
            return;
        }
        
        JSONArray computeDiscountDtos = paramObj.getJSONArray("computeDiscountDtos");
        if(ListUtil.isNull(computeDiscountDtos)){
            return;
        }
        ComputeDiscountDto computeDiscountDto = null;
        
        for (int accountIndex = 0; accountIndex < computeDiscountDtos.size(); accountIndex++) {
            computeDiscountDto = BeanConvertUtil.covertBean(computeDiscountDtos.getJSONObject(accountIndex), ComputeDiscountDto.class);
            // 跳过折扣金额为0的记录
            if (computeDiscountDto.getDiscountPrice() <= 0) {
                continue;
            }
            
            // 构建折扣业务对象
            JSONObject businessFee = new JSONObject();
            businessFee.put("discountPrice", computeDiscountDto.getDiscountPrice());
            businessFee.put("discountId", computeDiscountDto.getDiscountId());
            businessFee.put("detailId", paramObj.getString("detailId"));
            businessFee.put("communityId", paramObj.getString("communityId"));
            businessFee.put("feeId", paramObj.getString("feeId"));

            // 保存折扣明细记录
            PayFeeDetailDiscountPo payFeeDetailDiscount = BeanConvertUtil.covertBean(businessFee, PayFeeDetailDiscountPo.class);
            payFeeDetailDiscount.setbId("-1");
            payFeeDetailDiscount.setDetailDiscountId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_dataId));
            int fage = payFeeDetailDiscountNewV1InnerServiceSMOImpl.savePayFeeDetailDiscountNew(payFeeDetailDiscount);

            if (fage < 1) {
                throw new CmdException("更新费用信息失败");
            }
        }
    }


}