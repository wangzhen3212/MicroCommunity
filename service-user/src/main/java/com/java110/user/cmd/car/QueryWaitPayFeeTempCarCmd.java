package com.java110.user.cmd.car;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.machine.CarInoutDetailDto;
import com.java110.dto.machine.CarInoutDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.owner.OwnerCarOpenUserDto;
import com.java110.intf.common.ICarInoutDetailInnerServiceSMO;
import com.java110.intf.user.IOwnerCarOpenUserV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 查询待缴费临时车命令类
 * 
 * 该命令类用于处理查询待缴费临时车的业务逻辑，主要功能包括：
 * 1. 根据openId查询用户绑定的车辆信息
 * 2. 根据设备ID查询入场失败的临时车辆信息
 * 3. 返回待缴费的临时车辆列表
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "car.queryWaitPayFeeTempCar")
public class QueryWaitPayFeeTempCarCmd extends Cmd {

    /**
     * 车主开放用户服务接口
     */
    @Autowired
    private IOwnerCarOpenUserV1InnerServiceSMO ownerCarOpenUserV1InnerServiceSMOImpl;

    /**
     * 车辆进出记录详情服务接口
     */
    @Autowired
    private ICarInoutDetailInnerServiceSMO carInoutDetailInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的openId字段
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含openId字段
        Assert.hasKeyAndValue(reqJson, "openId", "未包含开放用户");
    }

    /**
     * 命令执行方法
     * 
     * 根据请求参数查询待缴费的临时车辆信息，执行逻辑：
     * 1. 如果没有提供设备ID，则根据openId查询用户绑定的车辆
     * 2. 如果提供了设备ID，则查询该设备上入场失败的临时车辆
     * 3. 返回查询到的车辆信息列表
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当数据解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 初始化车辆信息列表
        List<OwnerCarDto> ownerCarDtos = new ArrayList<>();
        OwnerCarDto ownerCarDto = null;
        
        // 检查请求参数中是否包含machineId字段
        if (!StringUtil.jsonHasKayAndValue(reqJson, "machineId")) {
            // 如果没有设备ID，则根据openId查询用户绑定的车辆
            OwnerCarOpenUserDto ownerCarOpenUserDto = new OwnerCarOpenUserDto();
            ownerCarOpenUserDto.setOpenId(reqJson.getString("openId"));
            
            // 调用服务查询用户绑定的车辆开放用户信息
            List<OwnerCarOpenUserDto> ownerCarOpenUserDtos = ownerCarOpenUserV1InnerServiceSMOImpl.queryOwnerCarOpenUsers(ownerCarOpenUserDto);
            
            // 遍历查询结果，构建车辆信息列表
            for (OwnerCarOpenUserDto tmpOwnerCarOpenUserDto : ownerCarOpenUserDtos) {
                ownerCarDto = new OwnerCarDto();
                ownerCarDto.setCarNum(tmpOwnerCarOpenUserDto.getCarNum());
                ownerCarDtos.add(ownerCarDto);
            }
            
            // 设置响应结果并返回
            context.setResponseEntity(ResultVo.createResponseEntity(ownerCarDtos));
            return;
        }

        // 如果有设备ID，则查询该设备上的车辆进出记录
        CarInoutDetailDto carInoutDetailDto = new CarInoutDetailDto();
        // 设置查询条件：设备ID
        carInoutDetailDto.setMachineId(reqJson.getString("machineId"));
        // 设置查询条件：状态为入场失败
        carInoutDetailDto.setState(CarInoutDto.STATE_IN_FAIL);
        // 设置查询条件：进出类型为出场
        carInoutDetailDto.setCarInout(CarInoutDetailDto.CAR_INOUT_OUT);
        // 设置分页参数：第一页，每页1条记录
        carInoutDetailDto.setPage(1);
        carInoutDetailDto.setRow(1);
        
        // 调用服务查询车辆进出详情
        List<CarInoutDetailDto> carInoutDetailDtos = carInoutDetailInnerServiceSMOImpl.queryCarInoutDetails(carInoutDetailDto);

        // 如果没有查询到结果，返回空列表
        if (carInoutDetailDtos == null || carInoutDetailDtos.size() < 1) {
            context.setResponseEntity(ResultVo.createResponseEntity(ownerCarDtos));
            return;
        }

        // 遍历查询结果，构建车辆信息列表
        for (CarInoutDetailDto tmpCarInoutDetailDto : carInoutDetailDtos) {
            ownerCarDto = new OwnerCarDto();
            ownerCarDto.setCarNum(tmpCarInoutDetailDto.getCarNum());
            ownerCarDtos.add(ownerCarDto);
        }
        
        // 设置响应结果
        context.setResponseEntity(ResultVo.createResponseEntity(ownerCarDtos));

    }
}