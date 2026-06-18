/**
 * 查询楼层和单元信息命令类
 * 
 * 该命令类负责处理查询楼层及其关联单元信息的业务逻辑，包括数据权限验证和结果返回。
 * 主要功能包括验证请求参数、检查用户数据权限、查询楼层单元数据并返回格式化结果。
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.community.cmd.floor;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.unit.UnitDto;
import com.java110.dto.data.DataPrivilegeStaffDto;
import com.java110.intf.community.IDataPrivilegeUnitV1InnerServiceSMO;
import com.java110.intf.community.IFloorInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.ApiUnitVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Java110Cmd(serviceCode = "floor.queryFloorAndUnits")
public class QueryFloorAndUnitsCmd extends Cmd {

    @Autowired
    private IFloorInnerServiceSMO floorInnerServiceSMOImpl;

    @Autowired
    private IDataPrivilegeUnitV1InnerServiceSMO dataPrivilegeUnitV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求中是否包含必要的参数，目前主要验证communityId参数是否存在。
     * 注释掉的代码为预留的楼层ID验证逻辑，可根据需要启用。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于访问请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws IllegalArgumentException 当请求参数不符合要求时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求中必须包含communityId参数
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求中未包含communityId信息");
        // 以下为预留的楼层ID验证逻辑，可根据业务需要启用
        //Assert.jsonObjectHaveKey(reqJson, "floorId", "请求中未包含floorId信息");
        //校验小区楼ID和小区是否有对应关系
//        int total = floorInnerServiceSMOImpl.queryFloorsCount(BeanConvertUtil.covertBean(reqJson, FloorDto.class));
//
//        if (total < 1) {
//            throw new IllegalArgumentException("传入小区楼ID不是该小区的楼");
//        }
    }

    /**
     * 执行查询楼层和单元信息命令
     * 
     * 主要业务流程：
     * 1. 转换请求参数为UnitDto对象
     * 2. 检查用户数据权限，获取有权限访问的单元ID列表
     * 3. 根据权限过滤条件查询楼层和单元信息
     * 4. 转换查询结果为API响应格式并设置响应实体
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为UnitDto对象，用于后续查询
        UnitDto unitDto = BeanConvertUtil.covertBean(reqJson, UnitDto.class);
        unitDto.setUserId(""); // 清空用户ID，避免影响查询结果

        // 查询员工是否有配置授权单元 - 数据权限验证
        String staffId = cmdDataFlowContext.getReqHeaders().get("user-id");
        DataPrivilegeStaffDto dataPrivilegeStaffDto = new DataPrivilegeStaffDto();
        dataPrivilegeStaffDto.setCommunityId(reqJson.getString("communityId")); // 设置小区ID
        dataPrivilegeStaffDto.setStaffId(staffId); // 设置员工ID
        
        // 根据员工权限获取可访问的单元ID数组
        String[] unitIds = dataPrivilegeUnitV1InnerServiceSMOImpl.queryDataPrivilegeUnitsByStaff(dataPrivilegeStaffDto);

        // 如果员工有特定的单元访问权限，则设置权限过滤条件
        if(unitIds != null && unitIds.length>0){
            unitDto.setUnitIds(unitIds); // 将权限单元ID设置到查询条件中
        }

        // 查询楼层和单元信息列表
        List<UnitDto> unitDtoList = floorInnerServiceSMOImpl.queryFloorAndUnits(unitDto);

        // 将UnitDto列表转换为API响应VO对象列表
        List<ApiUnitVo> apiUnitVos = BeanConvertUtil.covertBeanList(unitDtoList, ApiUnitVo.class);

        // 创建HTTP响应实体，返回JSON格式的查询结果
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiUnitVos), HttpStatus.OK);
        cmdDataFlowContext.setResponseEntity(responseEntity); // 设置响应到上下文
    }
}