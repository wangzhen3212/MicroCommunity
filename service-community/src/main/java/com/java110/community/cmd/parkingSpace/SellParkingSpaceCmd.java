package com.java110.community.cmd.parkingSpace;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.intf.community.IParkingSpaceInnerServiceSMO;
import com.java110.intf.community.IParkingSpaceV1InnerServiceSMO;
import com.java110.intf.fee.IFeeConfigInnerServiceSMO;
import com.java110.intf.user.IOwnerCarAttrInnerServiceSMO;
import com.java110.intf.user.IOwnerCarV1InnerServiceSMO;
import com.java110.po.car.OwnerCarPo;
import com.java110.po.owner.OwnerCarAttrPo;
import com.java110.po.parking.ParkingSpacePo;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

/**
 * 停车位出售/出租命令处理类
 * 负责处理停车位的出售和出租业务逻辑，包括参数验证、车位状态修改、车辆信息保存等操作
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "parkingSpace.sellParkingSpace")
public class SellParkingSpaceCmd extends Cmd {

    /** 费用配置服务 */
    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    /** 停车位查询服务 */
    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;
    
    /** 停车位操作服务 */
    @Autowired
    private IParkingSpaceV1InnerServiceSMO parkingSpaceV1InnerServiceSMOImpl;

    /** 车主车辆操作服务 */
    @Autowired
    private IOwnerCarV1InnerServiceSMO ownerCarV1InnerServiceSMOImpl;

    /** 车主车辆属性服务 */
    @Autowired
    private IOwnerCarAttrInnerServiceSMO ownerCarAttrInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     *
     * @param event 命令事件对象
     * @param context 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必需参数是否存在
        Assert.jsonObjectHaveKey(reqJson, "communityId", "未包含小区ID");
        Assert.jsonObjectHaveKey(reqJson, "ownerId", "请求报文中未包含ownerId");
        Assert.jsonObjectHaveKey(reqJson, "carNum", "请求报文中未包含carNum");
        Assert.jsonObjectHaveKey(reqJson, "carBrand", "请求报文中未包含carBrand");
        Assert.jsonObjectHaveKey(reqJson, "carType", "请求报文中未包含carType");
        Assert.jsonObjectHaveKey(reqJson, "carColor", "未包含carColor");
        Assert.jsonObjectHaveKey(reqJson, "psId", "未包含psId");
        Assert.jsonObjectHaveKey(reqJson, "storeId", "未包含storeId");
        //Assert.jsonObjectHaveKey(reqJson, "receivedAmount", "未包含receivedAmount");
        Assert.jsonObjectHaveKey(reqJson, "sellOrHire", "未包含sellOrHire");

        // 验证必需参数是否为空
        Assert.hasLength(reqJson.getString("communityId"), "小区ID不能为空");
        Assert.hasLength(reqJson.getString("ownerId"), "ownerId不能为空");
        Assert.hasLength(reqJson.getString("psId"), "psId不能为空");
        //Assert.isMoney(reqJson.getString("receivedAmount"), "不是有效的实收金额");

        // 验证出售/出租类型参数是否有效
        if (!"H".equals(reqJson.getString("sellOrHire"))
                && !"S".equals(reqJson.getString("sellOrHire"))) {
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "请求报文中sellOrFire值错误 ，出售为S 出租为H");
        }
    }

    /**
     * 命令执行方法
     * 处理停车位出售/出租的核心业务逻辑
     *
     * @param event 命令事件对象
     * @param context 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当业务处理失败时抛出异常
     * @throws ParseException 当数据解析失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 生成费用ID
        String feeId = GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_feeId);
        reqJson.put("feeId", feeId);

        // 执行停车位出售/出租操作
        sellParkingSpace(reqJson);

        // 设置车辆号码类型（出售S/出租H）
        reqJson.put("carNumType", reqJson.getString("sellOrHire"));

        // 修改停车位状态
        modifySellParkingSpaceState(reqJson);
    }

    /**
     * 修改停车位状态信息
     * 根据出售/出租操作更新停车位的状态
     *
     * @param paramInJson 接口调用方传入的参数JSON对象
     */
    public void modifySellParkingSpaceState(JSONObject paramInJson) {
        // 构建停车位查询条件
        ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
        parkingSpaceDto.setCommunityId(paramInJson.getString("communityId"));
        parkingSpaceDto.setPsId(paramInJson.getString("psId"));
        
        // 查询停车位信息
        List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);

        // 验证查询结果
        if (parkingSpaceDtos == null || parkingSpaceDtos.size() != 1) {
            // 如果未查询到或查询到多个停车位，直接返回不处理
            //throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "未查询到停车位信息" + JSONObject.toJSONString(parkingSpaceDto));
            return;
        }

        // 获取查询到的停车位信息
        parkingSpaceDto = parkingSpaceDtos.get(0);

        // 构建停车位更新对象
        JSONObject businessParkingSpace = new JSONObject();
        businessParkingSpace.putAll(BeanConvertUtil.beanCovertMap(parkingSpaceDto));
        // 设置新的停车位状态
        businessParkingSpace.put("state", paramInJson.getString("carNumType"));
        
        // 转换为PO对象并执行更新
        ParkingSpacePo parkingSpacePo = BeanConvertUtil.covertBean(businessParkingSpace, ParkingSpacePo.class);
        int flag = parkingSpaceV1InnerServiceSMOImpl.updateParkingSpace(parkingSpacePo);
        if (flag < 1) {
            throw new CmdException("修改车位失败");
        }
    }

    /**
     * 出售/出租停车位
     * 保存车辆信息到车主车辆表
     *
     * @param paramInJson 接口调用方传入的参数JSON对象
     */
    public void sellParkingSpace(JSONObject paramInJson) {
        // 构建车主车辆业务对象
        JSONObject businessOwnerCar = new JSONObject();
        businessOwnerCar.putAll(paramInJson);
        
        // 生成车辆成员ID
        businessOwnerCar.put("memberId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_carId));
        
        // 处理车辆ID，如果不存在或为负数则使用生成的ID
        if (!paramInJson.containsKey("carId") || paramInJson.getString("carId").startsWith("-")) {
            businessOwnerCar.put("carId", businessOwnerCar.getString("memberId"));
        }
        
        // 转换为PO对象
        OwnerCarPo ownerCarPo = BeanConvertUtil.covertBean(businessOwnerCar, OwnerCarPo.class);
        // 设置车辆状态为正常
        ownerCarPo.setState(OwnerCarDto.STATE_NORMAL);

        // 如果没有指定车辆类型，则设置为主要车辆
        if (!paramInJson.containsKey("carTypeCd") || StringUtil.isEmpty(paramInJson.getString("carTypeCd"))) {
            ownerCarPo.setCarTypeCd(OwnerCarDto.CAR_TYPE_PRIMARY);
        }
        
        // 处理车辆属性信息
        dealOwnerCarAttr(paramInJson, ownerCarPo);
        
        // 保存车辆信息
        int flag = ownerCarV1InnerServiceSMOImpl.saveOwnerCar(ownerCarPo);
        if (flag < 1) {
            throw new CmdException("保存车辆失败");
        }
    }

    /**
     * 处理车主车辆属性
     * 保存车辆的扩展属性信息
     *
     * @param paramInJson 接口调用方传入的参数JSON对象
     * @param ownerCarPo 车主车辆PO对象
     */
    private void dealOwnerCarAttr(JSONObject paramInJson, OwnerCarPo ownerCarPo) {
        // 检查是否存在属性数据
        if (!paramInJson.containsKey("attrs")) {
            return;
        }

        // 获取属性数组
        JSONArray attrs = paramInJson.getJSONArray("attrs");
        if (attrs.size() < 1) {
            return;
        }
        
        // 遍历并保存每个属性
        JSONObject attr = null;
        int flag = 0;
        for (int attrIndex = 0; attrIndex < attrs.size(); attrIndex++) {
            attr = attrs.getJSONObject(attrIndex);
            OwnerCarAttrPo ownerCarAttrPo = new OwnerCarAttrPo();
            // 生成属性ID
            ownerCarAttrPo.setAttrId(GenerateCodeFactory.getAttrId());
            // 设置小区ID
            ownerCarAttrPo.setCommunityId(ownerCarPo.getCommunityId());
            // 设置车辆ID
            ownerCarAttrPo.setCarId(ownerCarPo.getCarId());
            // 设置规格代码和值
            ownerCarAttrPo.setSpecCd(attr.getString("specCd"));
            ownerCarAttrPo.setValue(attr.getString("value"));
            
            // 保存车辆属性
            flag = ownerCarAttrInnerServiceSMOImpl.saveOwnerCarAttr(ownerCarAttrPo);
            if (flag < 1) {
                throw new CmdException("保存车辆属性失败");
            }
        }
    }
}