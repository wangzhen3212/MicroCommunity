package com.java110.fee.cmd.carMonth;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.IotDataDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.intf.job.IIotInnerServiceSMO;
import com.java110.intf.user.IOwnerCarInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

/**
 * 查询停车月卡命令类
 * 该命令类用于处理移动端查询停车月卡信息的请求，包括验证请求参数和执行业务逻辑
 * 通过调用车辆信息服务和物联网服务来获取月卡信息
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode carMonth.queryAppCarMonthCard
 */
@Java110Cmd(serviceCode = "carMonth.queryAppCarMonthCard")
public class QueryAppCarMonthCardCmd extends Cmd {

    /**
     * 物联网服务接口，用于调用物联网相关业务
     */
    @Autowired
    private IIotInnerServiceSMO iotInnerServiceSMOImpl;

    /**
     * 车主车辆信息服务接口，用于查询车辆相关信息
     */
    @Autowired
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl;

    /**
     * 验证请求参数方法
     * 对传入的请求参数进行校验，确保必要的参数存在且有效
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于处理请求和响应
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     * @throws ParseException 当参数解析失败时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 调用父类的分页信息验证方法
        super.validatePageInfo(reqJson);
        // 验证请求中必须包含小区ID参数
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID");
        // 验证请求中必须包含车辆ID参数
        Assert.hasKeyAndValue(reqJson, "carId", "未包含carId");
    }

    /**
     * 执行命令方法
     * 处理查询停车月卡的核心业务逻辑，包括查询车辆信息和调用物联网服务
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于处理请求和响应
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当业务逻辑执行失败时抛出命令异常
     * @throws ParseException 当数据解析失败时抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 创建车辆信息查询对象
        OwnerCarDto ownerCarDto = new OwnerCarDto();
        // 设置车辆ID，从请求参数中获取
        ownerCarDto.setMemberId(reqJson.getString("carId"));
        // 设置小区ID，从请求参数中获取
        ownerCarDto.setCommunityId(reqJson.getString("communityId"));
        
        // 调用车辆信息服务查询车辆信息
        List<OwnerCarDto> ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);

        // 检查查询结果是否为空，如果为空则抛出异常
        if (ListUtil.isNull(ownerCarDtos)) {
            throw new CmdException("车辆不存在");
        }

        // 将查询到的车辆区域编号添加到请求参数中，供后续物联网服务使用
        reqJson.put("paNum", ownerCarDtos.get(0).getAreaNum());

        // 创建物联网数据传输对象，设置服务标识和请求参数
        IotDataDto iotDataDto = new IotDataDto("queryCarMonthCardBmoImpl", reqJson);
        // 调用物联网服务查询月卡信息
        ResultVo data = iotInnerServiceSMOImpl.postIotData(iotDataDto);

        // 将查询结果设置到响应上下文中
        context.setResponseEntity(ResultVo.createResponseEntity(data));
    }
}