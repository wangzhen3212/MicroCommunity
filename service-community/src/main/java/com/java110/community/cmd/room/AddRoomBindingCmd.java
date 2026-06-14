/**
 * 房屋绑定添加命令类
 * 
 * 该类用于处理房屋绑定添加的业务逻辑，包括楼栋、单元和房屋的创建与绑定。
 * 支持新增楼栋、单元和房屋，同时处理房屋属性的保存。
 * 通过注解@Java110Cmd声明服务编码为"room.addRoomBinding"。
 * 
 * @author AI Assistant
 * @version 1.0
 * @since 2024
 */
package com.java110.community.cmd.room;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.floor.FloorDto;
import com.java110.dto.room.RoomDto;
import com.java110.dto.unit.UnitDto;
import com.java110.intf.community.IFloorV1InnerServiceSMO;
import com.java110.intf.community.IRoomAttrV1InnerServiceSMO;
import com.java110.intf.community.IRoomV1InnerServiceSMO;
import com.java110.intf.community.IUnitV1InnerServiceSMO;
import com.java110.po.floor.FloorPo;
import com.java110.po.room.RoomAttrPo;
import com.java110.po.room.RoomPo;
import com.java110.po.unit.UnitPo;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Java110Cmd(serviceCode = "room.addRoomBinding")
public class AddRoomBindingCmd extends Cmd {

    @Autowired
    private IUnitV1InnerServiceSMO unitV1InnerServiceSMOImpl;

    @Autowired
    private IFloorV1InnerServiceSMO floorV1InnerServiceSMOImpl;

    @Autowired
    private IRoomV1InnerServiceSMO roomV1InnerServiceSMOImpl;

    @Autowired
    private IRoomAttrV1InnerServiceSMO roomAttrV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数中是否包含必要的房屋信息字段，并对房屋视图数据进行属性值判断
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 从请求数据中获取数据数组
        JSONArray infos = reqJson.getJSONArray("data");

        // 验证房屋视图数据中必须包含的字段
        Assert.hasKeyByFlowData(infos, "addRoomView", "roomNum", "必填，请填写房屋编号");
        Assert.hasKeyByFlowData(infos, "addRoomView", "communityId", "必填，请填写房屋小区信息");
        Assert.hasKeyByFlowData(infos, "addRoomView", "layer", "必填，请填写房屋楼层");
        Assert.hasKeyByFlowData(infos, "addRoomView", "section", "必填，请填写房屋楼层");
        Assert.hasKeyByFlowData(infos, "addRoomView", "apartment", "必填，请选择房屋户型");
        Assert.hasKeyByFlowData(infos, "addRoomView", "builtUpArea", "必填，请填写房屋建筑面积");
        Assert.hasKeyByFlowData(infos, "addRoomView", "feeCoefficient", "必填，请填写房屋每平米单价");
        Assert.hasKeyByFlowData(infos, "addRoomView", "state", "必填，请选择房屋状态");

        // 从数据数组中查找房屋视图数据
        JSONObject addRoomView = null;
        for (int roomIndex = 0; roomIndex < infos.size(); roomIndex++) {
            JSONObject _info = infos.getJSONObject(roomIndex);
            if (_info.containsKey("addRoomView") && _info.getString("flowComponent").equals("addRoomView")) {
                addRoomView = _info;
                break;
            }
        }

        // 如果未找到房屋视图数据，直接返回
        if (addRoomView == null) {
            return;
        }

        // 判断房屋视图数据的属性值是否合法
        Assert.judgeAttrValue(addRoomView);
    }

    /**
     * 执行房屋绑定添加命令
     * 
     * 处理楼栋、单元和房屋的创建与绑定逻辑，包括数据验证、重复性检查和保存操作
     * 使用事务注解确保数据一致性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当命令执行失败时抛出
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 从请求数据中获取各个组件的数据
        JSONArray infos = reqJson.getJSONArray("data");
        JSONObject viewFloorInfo = getObj(infos, "viewFloorInfo");      // 楼栋视图信息
        JSONObject viewUnitInfo = getObj(infos, "viewUnitInfo");        // 单元视图信息
        JSONObject addRoomView = getObj(infos, "addRoomView");          // 房屋视图信息
        
        // 初始化变量
        String floorId = "";    // 楼栋ID
        String unitId = "";     // 单元ID
        String roomNum = "";    // 房屋编号
        int flag = 0;           // 操作结果标志

        // 检查是否选择了现有的楼栋、单元和房屋
        if (viewFloorInfo.containsKey("floorId") && StringUtil.isEmpty(viewFloorInfo.getString("floorId"))
                && viewUnitInfo.containsKey("unitId") && StringUtil.isEmpty(viewUnitInfo.getString("unitId"))
                && addRoomView.containsKey("roomNum") && StringUtil.isEmpty(addRoomView.getString("roomNum"))) {
            floorId = viewFloorInfo.getString("floorId");
            unitId = viewUnitInfo.getString("unitId");
            roomNum = addRoomView.getString("roomNum");
            
            // 查询楼栋下的单元信息
            UnitDto unitDto = new UnitDto();
            unitDto.setFloorId(floorId);
            unitDto.setUnitId(unitId);
            List<UnitDto> unitDtos = unitV1InnerServiceSMOImpl.queryUnits(unitDto);
            Assert.listOnlyOne(unitDtos, "查询单元信息错误！");
            
            // 检查房屋是否已存在
            RoomDto roomDto = new RoomDto();
            roomDto.setUnitId(unitDtos.get(0).getUnitId());
            roomDto.setRoomNum(roomNum);
            List<RoomDto> roomDtos = roomV1InnerServiceSMOImpl.queryRooms(roomDto);
            if (roomDtos != null && roomDtos.size() > 0) {
                throw new IllegalArgumentException("该房屋已经存在！");
            }
        }

        // 处理楼栋信息 - 如果前端没有提供楼栋ID，则创建新楼栋
        if (!hasKey(viewFloorInfo, "floorId")) {
            // 获取楼栋编号和小区ID
            String floorNum = viewFloorInfo.getString("floorNum");
            String communityId = viewFloorInfo.getString("communityId");
            
            // 检查楼栋编号是否重复
            FloorDto floorDto = new FloorDto();
            floorDto.setFloorNum(floorNum);
            floorDto.setCommunityId(communityId);
            int floorCount = floorV1InnerServiceSMOImpl.queryFloorsCount(floorDto);
            if (floorCount > 0) {
                throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "楼栋已经存在");
            }
            
            // 生成楼栋ID并保存楼栋信息
            viewFloorInfo.put("floorId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_floorId));
            viewFloorInfo.put("userId", reqJson.getString("userId"));
            FloorPo floorPo = BeanConvertUtil.covertBean(viewFloorInfo, FloorPo.class);
            flag = floorV1InnerServiceSMOImpl.saveFloor(floorPo);
            if (flag < 1) {
                throw new CmdException("保存楼栋失败");
            }
        }

        // 处理单元信息 - 如果前端没有提供单元ID，则创建新单元
        if (!hasKey(viewUnitInfo, "unitId")) {
            // 如果选择了现有楼栋，检查该楼栋下单元是否重复
            if (viewFloorInfo.containsKey("floorId") && !StringUtil.isEmpty(viewFloorInfo.getString("floorId"))) {
                String floorId1 = viewFloorInfo.getString("floorId");
                String communityId = viewUnitInfo.getString("communityId");
                String unitNum = viewUnitInfo.getString("unitNum");
                
                UnitDto unitDto = new UnitDto();
                unitDto.setFloorId(floorId1);
                unitDto.setCommunityId(communityId);
                unitDto.setUnitNum(unitNum);
                int unitCount = unitV1InnerServiceSMOImpl.queryUnitsCount(unitDto);
                if (unitCount > 0) {
                    throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "单元已经存在");
                }
            }
            
            // 生成单元ID并保存单元信息
            viewUnitInfo.put("unitId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_unitId));
            viewUnitInfo.put("userId", reqJson.getString("userId"));
            viewUnitInfo.put("floorId", viewFloorInfo.getString("floorId"));

            UnitPo unitPo = BeanConvertUtil.covertBean(viewUnitInfo, UnitPo.class);
            flag = unitV1InnerServiceSMOImpl.saveUnit(unitPo);
            if (flag < 1) {
                throw new CmdException("保存单元失败");
            }
        }

        // 处理房屋信息 - 如果前端没有提供房屋ID，则创建新房屋
        if (!hasKey(addRoomView, "roomId")) {
            // 如果选择了现有单元，检查该单元下房屋是否重复
            if (viewUnitInfo.containsKey("unitId") && !StringUtil.isEmpty(viewUnitInfo.getString("unitId"))) {
                String unitId1 = viewUnitInfo.getString("unitId");
                RoomDto roomDto = new RoomDto();
                roomDto.setUnitId(unitId1);
                roomDto.setRoomNum(addRoomView.getString("roomNum"));
                roomDto.setCommunityId(addRoomView.getString("communityId"));
                int roomCount = roomV1InnerServiceSMOImpl.queryRoomsCount(roomDto);
                if (roomCount > 0) {
                    throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "房屋已经存在");
                }
            }
            
            // 生成房屋ID并设置房屋基本信息
            addRoomView.put("roomId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_roomId));
            addRoomView.put("userId", reqJson.getString("userId"));
            addRoomView.put("unitId", viewUnitInfo.getString("unitId"));
            addRoomView.put("roomType", RoomDto.ROOM_TYPE_ROOM);
            
            // 转换并保存房屋信息
            RoomPo roomPo = BeanConvertUtil.covertBean(addRoomView, RoomPo.class);
            
            // 根据户型设置房屋分区信息
            if (addRoomView.containsKey("apartment1") && addRoomView.getString("apartment1").equals("10")) {
                roomPo.setSection("1");
            } else if (addRoomView.containsKey("apartment1") && addRoomView.getString("apartment1").equals("20")) {
                roomPo.setSection("2");
            } else if (addRoomView.containsKey("apartment1") && addRoomView.getString("apartment1").equals("30")) {
                roomPo.setSection("3");
            } else if (addRoomView.containsKey("apartment1") && addRoomView.getString("apartment1").equals("40")) {
                roomPo.setSection("4");
            } else if (addRoomView.containsKey("apartment1") && addRoomView.getString("apartment1").equals("50")) {
                roomPo.setSection("5");
            } else if (addRoomView.containsKey("apartment1") && addRoomView.getString("apartment1").equals("60")) {
                roomPo.setSection("6");
            } else if (addRoomView.containsKey("apartment1") && addRoomView.getString("apartment1").equals("70")) {
                roomPo.setSection("7");
            } else if (addRoomView.containsKey("apartment1") && addRoomView.getString("apartment1").equals("80")) {
                roomPo.setSection("8");
            }
            
            flag = roomV1InnerServiceSMOImpl.saveRoom(roomPo);
            if (flag < 1) {
                throw new CmdException("保存房屋失败");
            }
            
            // 处理房屋属性信息
            dealRoomAttr(addRoomView, cmdDataFlowContext);
        }

        // 构建返回结果
        JSONObject paramOutObj = new JSONObject();
        paramOutObj.put("floorId", viewFloorInfo.getString("floorId"));
        paramOutObj.put("unitId", viewUnitInfo.getString("unitId"));
        paramOutObj.put("roomId", addRoomView.getString("roomId"));
        ResponseEntity<String> responseEntity = null;

        responseEntity = ResultVo.createResponseEntity(paramOutObj);

        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 处理房屋属性信息
     * 
     * 保存房屋的扩展属性信息
     * 
     * @param addRoomView 房屋视图数据
     * @param cmdDataFlowContext 命令数据流上下文
     */
    private void dealRoomAttr(JSONObject addRoomView, ICmdDataFlowContext cmdDataFlowContext) {
        // 检查是否包含属性数据
        if (!addRoomView.containsKey("attrs")) {
            return;
        }

        JSONArray attrs = addRoomView.getJSONArray("attrs");
        if (attrs.size() < 1) {
            return;
        }

        // 遍历属性数组，保存每个属性
        JSONObject attr = null;
        int flag = 0;
        for (int attrIndex = 0; attrIndex < attrs.size(); attrIndex++) {
            attr = attrs.getJSONObject(attrIndex);
            attr.put("roomId", addRoomView.getString("roomId"));
            
            // 创建房屋属性对象并保存
            RoomAttrPo roomAttrPo = new RoomAttrPo();
            roomAttrPo.setAttrId(GenerateCodeFactory.getAttrId());
            roomAttrPo.setRoomId(attr.getString("roomId"));
            roomAttrPo.setSpecCd(attr.getString("specCd"));
            roomAttrPo.setValue(attr.getString("value"));

            flag = roomAttrV1InnerServiceSMOImpl.saveRoomAttr(roomAttrPo);
            if (flag < 1) {
                throw new CmdException("保存单元失败");
            }
        }
    }

    /**
     * 检查JSON对象中是否包含指定键且值有效
     * 
     * 判断键是否存在，且值不为空且不以"-"开头
     * 
     * @param info JSON对象
     * @param key 要检查的键
     * @return 如果键存在且值有效返回true，否则返回false
     */
    private boolean hasKey(JSONObject info, String key) {
        if (!info.containsKey(key)
                || StringUtil.isEmpty(info.getString(key))
                || info.getString(key).startsWith("-")) {
            return false;
        }
        return true;
    }

    /**
     * 从JSON数组中根据流程组件名称获取对应的JSON对象
     * 
     * 遍历JSON数组，查找指定流程组件名称对应的数据对象
     * 
     * @param infos JSON数组
     * @param flowComponent 流程组件名称
     * @return 对应的JSON对象
     * @throws IllegalArgumentException 当未找到指定组件时抛出
     */
    private JSONObject getObj(JSONArray infos, String flowComponent) {
        JSONObject serviceInfo = null;

        // 遍历数组查找指定组件
        for (int infoIndex = 0; infoIndex < infos.size(); infoIndex++) {
            Assert.hasKeyAndValue(infos.getJSONObject(infoIndex), "flowComponent", "未包含服务流程组件名称");

            if (flowComponent.equals(infos.getJSONObject(infoIndex).getString("flowComponent"))) {
                serviceInfo = infos.getJSONObject(infoIndex);
                Assert.notNull(serviceInfo, "未包含服务信息");
                return serviceInfo;
            }
        }

        throw new IllegalArgumentException("未找到组件编码为【" + flowComponent + "】数据");
    }
}