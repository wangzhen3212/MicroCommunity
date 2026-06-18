/**
 * 单元查询命令类
 * 
 * 该命令类用于处理外系统查询单元信息的请求，支持按小区、楼栋等条件查询单元数据，
 * 并包含数据权限控制功能，确保用户只能查询有权限访问的单元信息
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2024
 */
package com.java110.community.cmd.unit;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.unit.UnitDto;
import com.java110.dto.data.DataPrivilegeStaffDto;
import com.java110.intf.community.IDataPrivilegeUnitV1InnerServiceSMO;
import com.java110.intf.community.IFloorInnerServiceSMO;
import com.java110.intf.community.IUnitInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.ApiUnitVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * API文档注解 - 查询单元接口
 * 提供外系统查询单元信息的RESTful接口
 */
@Java110CmdDoc(title = "查询单元",
        description = "用于外系统查询单元信息功能",
        httpMethod = "get",
        url = "http://{ip}:{port}/app/unit.queryUnits",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "unit.queryUnits",
        seq = 12
)

/**
 * 请求参数文档注解
 * 定义接口的输入参数规范
 */
@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "floorId", length = 30, remark = "楼栋ID"),
        @Java110ParamDoc(name = "page", type = "int",length = 11, remark = "页数"),
        @Java110ParamDoc(name = "row", type = "int",length = 11, remark = "行数"),
})

/**
 * 响应参数文档注解
 * 定义接口返回的数据结构
 */
@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "unitId", type = "String", length = 30,  remark = "单元ID"),
                @Java110ParamDoc(name = "unitNum",  type = "String", length = 250,  remark = "单元编号"),
                @Java110ParamDoc(name = "seq",  type = "int", length = 11,  remark = "排序"),
        }
)

/**
 * 接口示例文档注解
 * 提供请求和响应的示例
 */
@Java110ExampleDoc(
        reqBody="http://{ip}:{port}/app/unit.queryUnits?page=1&row=10&communityId=123123&floorId=123",
        resBody="{'unitId':'123123','unitNum':'123123','seq':1}"
)

/**
 * 命令注解 - 标识该命令对应的服务编码
 */
@Java110Cmd(serviceCode = "unit.queryUnits")
public class QueryUnitsCmd extends Cmd {
    
    /**
     * 单元内部服务接口 - 用于查询单元数据
     */
    @Autowired
    private IUnitInnerServiceSMO unitInnerServiceSMOImpl;

    /**
     * 楼栋内部服务接口 - 用于验证楼栋信息
     */
    @Autowired
    private IFloorInnerServiceSMO floorInnerServiceSMOImpl;

    /**
     * 数据权限单元服务接口 - 用于处理用户数据权限
     */
    @Autowired
    private IDataPrivilegeUnitV1InnerServiceSMO dataPrivilegeUnitV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 该方法用于验证请求参数的合法性，确保必要的参数存在且格式正确
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws IllegalArgumentException 当必要参数缺失或格式不正确时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求中必须包含小区ID参数
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求中未包含communityId信息");
        
        // 注释掉的楼栋ID验证 - 可根据业务需求决定是否启用
        //Assert.jsonObjectHaveKey(reqJson, "floorId", "请求中未包含floorId信息");
        
        // 注释掉的楼栋与小区关系验证逻辑
        // 校验传入的小区楼ID是否属于指定小区，防止越权查询
        //int total = floorInnerServiceSMOImpl.queryFloorsCount(BeanConvertUtil.covertBean(reqJson, FloorDto.class));
        //
        //if (total < 1) {
        //    throw new IllegalArgumentException("传入小区楼ID不是该小区的楼");
        //}
    }

    /**
     * 命令执行方法
     * 
     * 该方法处理实际的业务逻辑，包括数据权限控制、单元信息查询和结果返回
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为单元数据传输对象
        UnitDto unitDto = BeanConvertUtil.covertBean(reqJson, UnitDto.class);
        // 设置用户ID为空，避免用户信息干扰查询条件
        unitDto.setUserId("");

        // 从请求头中获取用户ID，用于数据权限控制
        String staffId = cmdDataFlowContext.getReqHeaders().get("user-id");
        
        // 创建数据权限查询对象
        DataPrivilegeStaffDto dataPrivilegeStaffDto = new DataPrivilegeStaffDto();
        dataPrivilegeStaffDto.setStaffId(staffId);
        
        // 查询该用户有权限访问的单元ID列表
        String[] unitIds = dataPrivilegeUnitV1InnerServiceSMOImpl.queryDataPrivilegeUnitsByStaff(dataPrivilegeStaffDto);

        // 如果用户有特定的单元权限，则设置权限过滤条件
        if(unitIds != null && unitIds.length>0){
            unitDto.setUnitIds(unitIds);
        }

        // 根据查询条件查询单元列表
        List<UnitDto> unitDtoList = unitInnerServiceSMOImpl.queryUnits(unitDto);

        // 将单元数据传输对象列表转换为API响应对象列表
        List<ApiUnitVo> apiUnitVos = BeanConvertUtil.covertBeanList(unitDtoList, ApiUnitVo.class);

        // 构建HTTP响应实体，返回JSON格式的查询结果
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiUnitVos), HttpStatus.OK);
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}