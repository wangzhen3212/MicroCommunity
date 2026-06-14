/**
 * 删除单元命令类
 * 
 * 该命令类用于处理删除单元信息的业务逻辑，包括参数验证和单元删除操作。
 * 通过注解定义了API文档信息，支持事务处理，确保数据一致性。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023
 */
package com.java110.community.cmd.unit;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.floor.FloorDto;
import com.java110.dto.room.RoomDto;
import com.java110.dto.unit.UnitDto;
import com.java110.intf.community.IFloorInnerServiceSMO;
import com.java110.intf.community.IRoomV1InnerServiceSMO;
import com.java110.intf.community.IUnitInnerServiceSMO;
import com.java110.intf.community.IUnitV1InnerServiceSMO;
import com.java110.po.unit.UnitPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * API文档注解 - 删除单元接口
 * 定义接口的基本信息、参数、响应和示例
 */
@Java110CmdDoc(title = "删除单元",
        description = "用于外系统删除单元信息功能",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/unit.deleteUnit",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "unit.deleteUnit",
        seq = 11
)

/**
 * 请求参数文档定义
 */
@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "unitId", length = 30, remark = "单元ID"),
})

/**
 * 响应参数文档定义
 */
@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

/**
 * 请求响应示例文档
 */
@Java110ExampleDoc(
        reqBody="{\"unitId\":\"123123\",\"communityId\":\"2022081539020475\"}",
        resBody="{'code':0,'msg':'成功'}"
)

/**
 * 命令注解，定义服务编码
 */
@Java110Cmd(serviceCode = "unit.deleteUnit")
public class DeleteUnitCmd extends Cmd {
    
    /** 楼栋服务接口 */
    @Autowired
    private IFloorInnerServiceSMO floorInnerServiceSMOImpl;

    /** 单元服务接口 */
    @Autowired
    private IUnitInnerServiceSMO unitInnerServiceSMOImpl;

    /** 单元V1版本服务接口 */
    @Autowired
    private IUnitV1InnerServiceSMO unitV1InnerServiceSMOImpl;

    /** 房屋V1版本服务接口 */
    @Autowired
    private IRoomV1InnerServiceSMO roomV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的完整性和业务逻辑的正确性，包括：
     * 1. 必要参数检查
     * 2. 小区楼ID与小区关系验证
     * 3. 单元ID与楼栋关系验证
     * 4. 单元下房屋存在性检查
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON数据
     * @throws IllegalArgumentException 当参数验证失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 检查必要参数是否存在
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求报文中未包含communityId节点");
        Assert.jsonObjectHaveKey(reqJson, "floorId", "请求报文中未包含floorId节点");
        Assert.jsonObjectHaveKey(reqJson, "unitId", "请求报文中未包含unitId节点");

        // 创建楼栋查询对象
        FloorDto floorDto = new FloorDto();
        floorDto.setCommunityId(reqJson.getString("communityId"));
        floorDto.setFloorId(reqJson.getString("floorId"));
        // 校验小区楼ID和小区是否有对应关系
        int total = floorInnerServiceSMOImpl.queryFloorsCount(floorDto);

        // 如果查询结果小于1，说明传入的小区楼ID不属于该小区
        if (total < 1) {
            throw new IllegalArgumentException("传入小区楼ID不是该小区的楼");
        }

        // 创建单元查询对象
        UnitDto unitDto = new UnitDto();
        unitDto.setFloorId(reqJson.getString("floorId"));
        unitDto.setUnitId(reqJson.getString("unitId"));
        // 校验小区楼ID和单元ID是否有关系
        total = unitInnerServiceSMOImpl.queryUnitsCount(unitDto);
        // 如果查询结果小于1，说明传入的单元不属于该楼栋
        if (total < 1) {
            throw new IllegalArgumentException("传入单元不是该小区的楼的单元");
        }

        // 创建房屋查询对象，检查单元下是否存在房屋
        RoomDto roomDto = new RoomDto();
        roomDto.setUnitId(reqJson.getString("unitId"));
        roomDto.setCommunityId(reqJson.getString("communityId"));
        // 查询单元下的房屋数量
        int count = roomV1InnerServiceSMOImpl.queryRoomsCount(roomDto);
        // 如果存在房屋，则不允许删除单元
        if(count > 0){
            throw new IllegalArgumentException("单元下存在房屋 请先删除房屋");
        }
    }

    /**
     * 执行删除单元命令
     * 
     * 在事务中执行单元删除操作，确保数据一致性。
     * 删除成功后返回成功响应，失败时抛出异常。
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON数据
     * @throws CmdException 当删除操作失败时抛出
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 构建业务单元对象
        JSONObject businessUnit = new JSONObject();
        businessUnit.put("unitId", reqJson.getString("unitId"));
        // 将JSON对象转换为单元PO对象
        UnitPo unitPo = BeanConvertUtil.covertBean(businessUnit, UnitPo.class);
        // 执行删除操作
        int flag = unitV1InnerServiceSMOImpl.deleteUnit(unitPo);

        // 检查删除操作是否成功
        if (flag < 1) {
            throw new CmdException("删除单元失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}