/**
 * 删除楼栋命令类
 * 
 * 该命令用于处理删除楼栋信息的业务逻辑，包括参数验证、楼栋删除以及关联单元数据的清理。
 * 在执行删除操作前会校验楼栋下是否存在房屋或商铺，确保数据完整性。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023
 */
package com.java110.community.cmd.floor;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.room.RoomDto;
import com.java110.dto.unit.UnitDto;
import com.java110.intf.community.IFloorInnerServiceSMO;
import com.java110.intf.community.IFloorV1InnerServiceSMO;
import com.java110.intf.community.IRoomV1InnerServiceSMO;
import com.java110.intf.community.IUnitV1InnerServiceSMO;
import com.java110.po.floor.FloorPo;
import com.java110.po.unit.UnitPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;


@Java110CmdDoc(title = "删除楼栋",
        description = "用于外系统删除楼栋信息功能",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/floor.deleteFloor",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "floor.deleteFloor",
        seq = 7
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "floorId", length = 30, remark = "楼栋ID"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

@Java110ExampleDoc(
        reqBody = "{\"floorId\":\"123123\",\"communityId\":\"2022081539020475\"}",
        resBody = "{'code':0,'msg':'成功'}"
)

@Java110Cmd(serviceCode = "floor.deleteFloor")
public class DeleteFloorCmd extends Cmd {
    
    /** 楼栋内部服务接口 */
    @Autowired
    private IFloorInnerServiceSMO floorInnerServiceSMOImpl;

    /** 楼栋V1版本内部服务接口 */
    @Autowired
    private IFloorV1InnerServiceSMO floorV1InnerServiceSMOImpl;

    /** 单元V1版本内部服务接口 */
    @Autowired
    private IUnitV1InnerServiceSMO unitV1InnerServiceSMOImpl;

    /** 房间V1版本内部服务接口 */
    @Autowired
    private IRoomV1InnerServiceSMO roomV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整，并检查楼栋下是否存在房屋或商铺数据
     * 如果存在关联数据则抛出异常，阻止删除操作
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws IllegalArgumentException 当参数验证失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必要参数是否存在
        Assert.jsonObjectHaveKey(reqJson, "floorId", "请求报文中未包含floorId");
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求报文中未包含communityId");

        // 注释掉的单元验证代码 - 后续可能需要启用
//        UnitDto unitDto = new UnitDto();
//        unitDto.setFloorId(reqJson.getString("floorId"));
//        unitDto.setCommunityId(reqJson.getString("communityId"));
////        unitDto.setRoomUnit(UnitDto.ROOM_UNIT_Y);
//        int count = unitV1InnerServiceSMOImpl.queryUnitsCount(unitDto);
//        if (count > 0) {
//            throw new IllegalArgumentException("请先删除单元 再删除楼栋");
//        }

        // 校验楼栋下是否存在房屋或者商铺数据
        RoomDto roomDto = new RoomDto();
        roomDto.setFloorId(reqJson.getString("floorId"));  // 设置楼栋ID
        roomDto.setCommunityId(reqJson.getString("communityId"));  // 设置小区ID
        int count = roomV1InnerServiceSMOImpl.queryRoomsCount(roomDto);
        if (count > 0) {
            throw new IllegalArgumentException("请先删除楼栋下的房屋或者商铺");
        }
    }

    /**
     * 执行删除楼栋命令
     * 
     * 主要执行以下操作：
     * 1. 删除指定的楼栋信息
     * 2. 删除该楼栋下的所有单元信息
     * 3. 设置操作成功的响应结果
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当删除操作失败时抛出
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为楼栋PO对象
        FloorPo floorPo = BeanConvertUtil.covertBean(reqJson, FloorPo.class);
        // 执行楼栋删除操作
        int flag = floorV1InnerServiceSMOImpl.deleteFloor(floorPo);

        // 检查删除操作是否成功
        if (flag < 1) {
            throw new CmdException("删除楼栋失败");
        }

        // 删除楼栋下的所有单元信息
        UnitDto unitDto = new UnitDto();
        unitDto.setFloorId(reqJson.getString("floorId"));  // 设置要查询的楼栋ID
        unitDto.setCommunityId(reqJson.getString("communityId"));  // 设置小区ID
        // 查询楼栋下的所有单元
        List<UnitDto> unitDtos = unitV1InnerServiceSMOImpl.queryUnits(unitDto);

        // 如果没有单元数据，直接返回
        if (unitDtos == null || unitDtos.size() < 1) {
            return;
        }

        // 遍历并删除所有关联的单元
        UnitPo unitPo = null;
        for (UnitDto tmpUnitDto : unitDtos) {
            unitPo = new UnitPo();
            unitPo.setUnitId(tmpUnitDto.getUnitId());  // 设置单元ID
            unitV1InnerServiceSMOImpl.deleteUnit(unitPo);  // 执行单元删除
        }

        // 设置操作成功的响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}