package com.java110.acct.cmd.parkingCoupon;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.parking.ParkingCouponCarDto;
import com.java110.dto.parking.ParkingCouponShopDto;
import com.java110.dto.shop.ShopDto;
import com.java110.dto.shop.StoreShopDto;
import com.java110.dto.store.StoreUserDto;
import com.java110.intf.acct.IParkingCouponCarV1InnerServiceSMO;
import com.java110.intf.acct.IParkingCouponShopV1InnerServiceSMO;
import com.java110.intf.mall.IShopInnerServiceSMO;
import com.java110.intf.shore.IShoreShopV1InnerServiceSMO;
import com.java110.intf.store.IStoreShopCommunityV1InnerServiceSMO;
import com.java110.intf.store.IStoreShopV1InnerServiceSMO;
import com.java110.intf.store.IStoreUserV1InnerServiceSMO;
import com.java110.po.parking.ParkingCouponCarPo;
import com.java110.po.parking.ParkingCouponShopPo;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.exception.CmdException;
import com.java110.utils.lock.DistributedLock;
import com.java110.utils.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.List;

/**
 * 特来电充电结束通知处理命令类
 * 功能：处理特来电充电结束通知，验证商户停车券信息，发放停车券给指定车牌号
 * 用途：接收特来电加密报文，解密后处理停车券发放逻辑，返回处理结果给特来电系统
 * 
 * 接口路径示例：
 * /app/ext/parkingCoupon.teldSendParkingCouponCar/992020051967020024
 * /app/ext/login.getTokenForTeld/992020051967020024
 */
@Java110Cmd(serviceCode = "notification_charge_end_order_info")
public class TeldSendParkingCouponCarCmd extends Cmd {

    @Autowired
    private IStoreUserV1InnerServiceSMO storeUserV1InnerServiceSMOImpl;

    @Autowired
    private IStoreShopV1InnerServiceSMO storeShopV1InnerServiceSMOImpl;

    @Autowired
    private IParkingCouponShopV1InnerServiceSMO parkingCouponShopV1InnerServiceSMOImpl;

    @Autowired
    private IParkingCouponCarV1InnerServiceSMO parkingCouponCarV1InnerServiceSMOImpl;

    /** ID生成前缀 */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 请求参数验证方法
     * 验证请求中是否包含必要的加密报文数据
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求中必须包含Data字段（加密报文）
        Assert.hasKeyAndValue(reqJson, "Data", "未包含加密报文");
    }

    /**
     * 命令执行主方法
     * 处理特来电充电结束通知，解密报文，发放停车券，返回处理结果
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从缓存中获取特来电加解密相关配置
        String signKey = MappingCache.getValue("TELD_DOMAIN", "SIGN_KEY");
        String aesKey = MappingCache.getValue("TELD_DOMAIN", "AES_KEY");
        String aesIv = MappingCache.getValue("TELD_DOMAIN", "AES_IV");

        // 解密特来电传输的加密数据
        String data = TeldUtil.Decrypt(reqJson.getString("Data"), aesKey, aesIv);

        // 验证解密后的数据是否为空
        if (StringUtil.isEmpty(data)) {
            throw new CmdException("未包含报文");
        }

        // 解析解密后的JSON数据
        JSONObject dataObj = JSONObject.parseObject(data);

        // 获取充电订单序列号
        String StartChargeSeq = dataObj.getString("StartChargeSeq");
        
        // 初始化返回参数默认值（成功状态）
        int ConfirmResult = 0;      // 确认结果：0-成功，1-失败
        int PlateAutResult = 1;     // 车牌识别结果：1-识别成功，2-识别失败
        int PlateAutFailReason = 0; // 车牌识别失败原因：0-无，6-其他原因
        
        try {
            // 执行停车券发放逻辑
            doSend(context, dataObj);
        } catch (Exception e) {
            e.printStackTrace();
            // 处理异常时设置失败状态
            ConfirmResult = 1;      // 确认失败
            PlateAutResult = 2;     // 车牌识别失败
            PlateAutFailReason = 6; // 失败原因：其他
        }

        // 构建返回给特来电的响应参数
        JSONObject paramOut = new JSONObject();
        paramOut.put("StartChargeSeq", StartChargeSeq);
        paramOut.put("ConfirmResult", ConfirmResult);
        paramOut.put("PlateAutResult", PlateAutResult);
        paramOut.put("PlateAutFailReason", PlateAutFailReason);
        
        String paramStr = "";
        try {
            // 生成加密的返回参数
            paramStr = TeldUtil.generateReturnParam(paramOut.toJSONString(), aesKey, aesIv, signKey, reqJson.getString("OperatorID"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 设置HTTP响应
        context.setResponseEntity(new ResponseEntity(paramStr, HttpStatus.OK));
    }

    /**
     * 执行停车券发放逻辑
     * 验证商户信息，检查停车券库存，发放停车券给指定车牌号
     *
     * @param context 命令数据流上下文
     * @param dataObj 解密后的数据对象
     */
    private void doSend(ICmdDataFlowContext context, JSONObject dataObj) {
        // 从请求头中获取用户ID
        String userId = context.getReqHeaders().get("user-id");

        // 根据用户ID查询商户信息
        StoreUserDto storeUserDto = new StoreUserDto();
        storeUserDto.setUserId(userId);
        List<StoreUserDto> storeUserDtos = storeUserV1InnerServiceSMOImpl.queryStoreUsers(storeUserDto);
        // 验证商户存在且唯一
        Assert.listOnlyOne(storeUserDtos, "商户不存在");

        // 根据商户ID查询店铺信息
        StoreShopDto storeShopDto = new StoreShopDto();
        storeShopDto.setStoreId(storeUserDtos.get(0).getStoreId());
        List<ShopDto> storeShops = storeShopV1InnerServiceSMOImpl.queryStoreShops(storeShopDto);
        // 验证商户店铺存在
        if (storeShops == null || storeShops.size() < 1) {
            Assert.listOnlyOne(storeUserDtos, "商户店铺不存在");
        }

        // 查询商户的停车券库存信息
        ParkingCouponShopDto parkingCouponShopDto = new ParkingCouponShopDto();
        parkingCouponShopDto.setHasQuantity("Y"); // 只查询有库存的记录
        parkingCouponShopDto.setShopId(storeShops.get(0).getShopId());
        List<ParkingCouponShopDto> parkingCouponShopDtos = parkingCouponShopV1InnerServiceSMOImpl.queryParkingCouponShops(parkingCouponShopDto);

        // 检查停车券库存
        if (parkingCouponShopDtos == null || parkingCouponShopDtos.size() < 1) {
            throw new CmdException("停车券已用完");
        }

        // 获取车牌号码
        String carNum = dataObj.getString("PlateNum");

        int quantity = 0; // 停车券数量
        int flag = 0;     // 操作标志

        // 使用分布式锁确保库存操作的原子性
        String requestId = DistributedLock.getLockUUID();
        String key = this.getClass().getSimpleName() + parkingCouponShopDtos.get(0).getCouponShopId();
        try {
            // 获取分布式锁
            DistributedLock.waitGetDistributedLock(key, requestId);
            
            // 重新查询停车券库存信息（防止并发问题）
            parkingCouponShopDto = new ParkingCouponShopDto();
            parkingCouponShopDto.setCouponShopId(parkingCouponShopDtos.get(0).getCouponShopId());
            parkingCouponShopDto.setShopId(storeShops.get(0).getShopId());
            parkingCouponShopDtos = parkingCouponShopV1InnerServiceSMOImpl.queryParkingCouponShops(parkingCouponShopDto);
            
            // 获取当前库存数量
            quantity = Integer.parseInt(parkingCouponShopDtos.get(0).getQuantity());
            // 检查库存是否充足
            if (quantity < 1) {
                throw new CmdException("停车劵不足，请购买");
            }
            
            // 更新停车券库存（减1）
            ParkingCouponShopPo parkingCouponShopPo = new ParkingCouponShopPo();
            parkingCouponShopPo.setCouponShopId(parkingCouponShopDtos.get(0).getCouponShopId());
            parkingCouponShopPo.setQuantity((quantity - 1) + "");
            flag = parkingCouponShopV1InnerServiceSMOImpl.updateParkingCouponShop(parkingCouponShopPo);
            
            // 检查库存更新是否成功
            if (flag < 1) {
                throw new CmdException("优惠券递减失败");
            }
        } finally {
            // 释放分布式锁
            DistributedLock.releaseDistributedLock(requestId, key);
        }

        // 构建停车券发放记录
        String StartChargeSeq = dataObj.getString("StartChargeSeq");
        JSONObject remarkObj = new JSONObject();
        remarkObj.put("StartChargeSeq", StartChargeSeq); // 记录充电订单号

        // 创建停车券车辆关联记录
        ParkingCouponCarPo parkingCouponCarPo = new ParkingCouponCarPo();
        parkingCouponCarPo.setCarNum(carNum);                                    // 车牌号
        parkingCouponCarPo.setGiveWay("1001");                                   // 发放方式
        parkingCouponCarPo.setCouponShopId(parkingCouponShopDtos.get(0).getCouponShopId()); // 停车券店铺ID
        parkingCouponCarPo.setShopId(parkingCouponShopDtos.get(0).getShopId());             // 店铺ID
        parkingCouponCarPo.setPccId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));    // 生成主键ID
        parkingCouponCarPo.setCouponId(parkingCouponShopDtos.get(0).getCouponId());         // 优惠券ID
        parkingCouponCarPo.setCommunityId(parkingCouponShopDtos.get(0).getCommunityId());   // 小区ID
        parkingCouponCarPo.setStartTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));   // 开始时间（当前时间）
        parkingCouponCarPo.setEndTime(DateUtil.getAddDayString(DateUtil.getCurrentDate(), DateUtil.DATE_FORMATE_STRING_A, 1)); // 结束时间（当前时间+1天）
        parkingCouponCarPo.setPaId(parkingCouponShopDtos.get(0).getPaId());                 // 停车区域ID
        parkingCouponCarPo.setState(ParkingCouponCarDto.STATE_WAIT);                        // 状态：待使用
        parkingCouponCarPo.setTypeCd(parkingCouponShopDtos.get(0).getTypeCd());             // 类型编码
        parkingCouponCarPo.setValue(parkingCouponShopDtos.get(0).getValue());               // 优惠券面值
        parkingCouponCarPo.setRemark(remarkObj.toJSONString());                             // 备注信息（包含充电订单号）

        // 保存停车券发放记录
        flag = parkingCouponCarV1InnerServiceSMOImpl.saveParkingCouponCar(parkingCouponCarPo);

        // 检查保存是否成功
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }
    }
}