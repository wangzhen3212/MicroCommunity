package com.java110.community.cmd.room;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.room.RoomDto;
import com.java110.dto.unit.UnitDto;
import com.java110.intf.community.IRoomV1InnerServiceSMO;
import com.java110.intf.community.IUnitInnerServiceSMO;
import com.java110.intf.community.IUnitV1InnerServiceSMO;
import com.java110.po.room.RoomPo;
import com.java110.po.unit.UnitPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

/**
 * 商铺保存命令类
 * 负责处理商铺信息的保存业务逻辑，包括商铺所属单元的创建和商铺信息的添加
 * 服务代码：room.saveShops
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "room.saveShops")
public class SaveShopsCmd extends Cmd {

    @Autowired
    private IUnitInnerServiceSMO unitInnerServiceSMOImpl;

    @Autowired
    private IUnitV1InnerServiceSMO unitV1InnerServiceSMOImpl;

    @Autowired
    private IRoomV1InnerServiceSMO roomV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和格式是否正确
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当参数验证失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必需参数是否存在
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求报文中未包含communityId节点");
        Assert.jsonObjectHaveKey(reqJson, "floorId", "请求报文中未包含楼栋信息");
        Assert.jsonObjectHaveKey(reqJson, "roomNum", "请求报文中未包含roomNum节点");
        Assert.jsonObjectHaveKey(reqJson, "layer", "请求报文中未包含layer节点");
        Assert.jsonObjectHaveKey(reqJson, "builtUpArea", "请求报文中未包含section节点");
        Assert.jsonObjectHaveKey(reqJson, "feeCoefficient", "请求报文中未包含apartment节点");

        // 验证数值格式
        Assert.isMoney(reqJson.getString("builtUpArea"), "建筑面积数据格式错误");
        Assert.isMoney(reqJson.getString("feeCoefficient"), "房屋单价数据格式错误");

        // 设置默认值
        if (!reqJson.containsKey("roomSubType")) {
            reqJson.put("roomSubType", RoomDto.ROOM_SUB_TYPE_WORK); // 默认设置为办公类型
        }

        if (!reqJson.containsKey("roomRent")) {
            reqJson.put("roomRent", "0"); // 默认租金为0
        }

        if (!reqJson.containsKey("roomArea")) {
            reqJson.put("roomRent", reqJson.getString("builtUpArea")); // 默认房间面积为建筑面积
        }
    }

    /**
     * 执行商铺保存命令
     * 主要逻辑：检查单元是否存在，不存在则创建，然后保存商铺信息
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException    当命令执行失败时抛出
     * @throws ParseException 当数据解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 构建单元查询条件
        UnitDto unitDto = new UnitDto();
        unitDto.setCommunityId(reqJson.getString("communityId"));
        unitDto.setFloorId(reqJson.getString("floorId"));
        unitDto.setUnitNum("0"); // 设置单元编号为0，表示商铺单元
        
        // 查询小区楼ID和小区是否有对应关系的单元
        List<UnitDto> units = unitInnerServiceSMOImpl.queryUnitsByCommunityId(unitDto);
        String unitId = "";
        
        // 如果单元不存在，则创建新单元
        if (units == null || units.size() < 1) {
            unitId = GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_unitId); // 生成单元ID
            JSONObject unit = new JSONObject();
            unit.put("floorId", reqJson.getString("floorId"));
            unit.put("layerCount", "31"); // 默认层数为31层
            unit.put("unitId", unitId);
            unit.put("unitNum", "0"); // 单元编号为0
            unit.put("lift", "0"); // 无电梯
            unit.put("remark", "系统创建"); // 备注信息
            unit.put("unitArea", "1.00"); // 单元面积
            addUnit(unit); // 调用添加单元方法
        } else {
            unitId = units.get(0).getUnitId(); // 使用已存在的单元ID
        }
        
        // 设置商铺相关参数
        reqJson.put("unitId", unitId);
        reqJson.put("section", "1"); // 默认分区为1
        reqJson.put("apartment", "10101"); // 默认户型
        reqJson.put("state", RoomDto.STATE_SHOP_FREE); // 商铺状态：空闲
        reqJson.put("roomType", RoomDto.ROOM_TYPE_SHOPS); // 房间类型：商铺
        
        addRoom(reqJson); // 调用添加商铺方法
    }

    /**
     * 添加单元信息
     * 将单元信息保存到数据库中
     *
     * @param paramInJson 包含单元信息的JSON对象
     * @throws CmdException 当保存单元失败时抛出
     */
    public void addUnit(JSONObject paramInJson) {
        // 构建业务单元对象
        JSONObject businessUnit = new JSONObject();
        businessUnit.put("floorId", paramInJson.getString("floorId"));
        businessUnit.put("layerCount", paramInJson.getString("layerCount"));
        businessUnit.put("unitId", !paramInJson.containsKey("unitId") ? GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_unitId)
                : paramInJson.getString("unitId")); // 如果未提供unitId则生成新的
        businessUnit.put("unitNum", paramInJson.getString("unitNum"));
        businessUnit.put("lift", paramInJson.getString("lift"));
        businessUnit.put("remark", paramInJson.getString("remark"));
        businessUnit.put("unitArea", paramInJson.getString("unitArea"));
        businessUnit.put("userId", "-1"); // 默认用户ID
        
        // 转换为PO对象并保存
        UnitPo unitPo = BeanConvertUtil.covertBean(businessUnit, UnitPo.class);
        int flag = unitV1InnerServiceSMOImpl.saveUnit(unitPo);
        
        // 检查保存结果
        if (flag < 1) {
            throw new CmdException("保存单元失败");
        }
    }

    /**
     * 添加商铺信息
     * 将商铺信息保存到数据库中
     *
     * @param paramInJson 包含商铺信息的JSON对象
     * @throws CmdException 当保存商铺失败时抛出
     */
    public void addRoom(JSONObject paramInJson) {
        // 构建业务单元对象，复制所有传入参数
        JSONObject businessUnit = new JSONObject();
        businessUnit.putAll(paramInJson);
        businessUnit.put("roomId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_roomId)); // 生成房间ID
        businessUnit.put("userId", "-1"); // 默认用户ID
        
        // 转换为PO对象并保存
        RoomPo roomPo = BeanConvertUtil.covertBean(businessUnit, RoomPo.class);
        int flag = roomV1InnerServiceSMOImpl.saveRoom(roomPo);
        
        // 检查保存结果
        if (flag < 1) {
            throw new CmdException("保存单元失败");
        }
    }
}