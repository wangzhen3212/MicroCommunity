/**
 * 停车位费用查询命令类
 * 
 * 该命令类用于根据停车位ID查询相关的费用信息，包括停车位基本信息和关联的车辆信息
 * 通过注解@Java110Cmd声明服务代码为"fee.queryFeeByParkingSpace"
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.intf.community.IParkingSpaceInnerServiceSMO;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.intf.user.IOwnerCarInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.ApiParkingSpaceFeeVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Java110Cmd(serviceCode = "fee.queryFeeByParkingSpace")
public class QueryFeeByParkingSpaceCmd extends Cmd {

    /**
     * 费用内部服务接口，用于查询费用相关信息
     */
    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    /**
     * 停车位内部服务接口，用于查询停车位信息
     */
    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;

    /**
     * 业主车辆内部服务接口，用于查询车辆信息
     */
    @Autowired
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求JSON中是否包含必要的参数，确保后续业务逻辑能够正常执行
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数JSON对象，包含前端传递的所有参数
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求中必须包含小区ID参数
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求中未包含communityId信息");
        // 验证请求中必须包含停车位ID参数
        Assert.jsonObjectHaveKey(reqJson, "psId", "请求中未包含psId信息");
    }

    /**
     * 命令执行方法
     * 
     * 主要业务逻辑处理方法，依次查询费用信息、停车位信息和车辆信息，
     * 并将所有信息整合后返回给前端
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当业务逻辑执行异常时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为费用DTO对象，用于查询费用信息
        FeeDto feeDtoParamIn = BeanConvertUtil.covertBean(reqJson, FeeDto.class);
        // 设置缴费对象ID为停车位ID
        feeDtoParamIn.setPayerObjId(reqJson.getString("psId"));

        // 根据参数查询费用信息列表
        List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDtoParamIn);
        ResponseEntity<String> responseEntity = null;
        
        // 如果未查询到费用信息，返回空JSON对象
        if (feeDtos == null || feeDtos.size() == 0) {
            responseEntity = new ResponseEntity<String>("{}", HttpStatus.OK);
            context.setResponseEntity(responseEntity);
            return;
        }

        // 获取第一个费用信息（假设每个停车位只有一个主要费用）
        FeeDto feeDto = feeDtos.get(0);

        // 将费用DTO转换为API返回VO对象
        ApiParkingSpaceFeeVo apiFeeVo = BeanConvertUtil.covertBean(feeDto, ApiParkingSpaceFeeVo.class);

        // 查询停车位信息
        ParkingSpaceDto parkingSpaceDto = BeanConvertUtil.covertBean(reqJson, ParkingSpaceDto.class);
        List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);

        // 验证只能查询到一条停车位记录
        Assert.listOnlyOne(parkingSpaceDtos, "未查询到或查询多条 车位信息");

        // 获取唯一的停车位信息
        parkingSpaceDto = parkingSpaceDtos.get(0);

        // 将停车位信息合并到返回VO对象中
        BeanConvertUtil.covertBean(parkingSpaceDto, apiFeeVo);

        // 查询车辆信息
        OwnerCarDto ownerCarDto = BeanConvertUtil.covertBean(reqJson, OwnerCarDto.class);

        List<OwnerCarDto> ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);
        // 验证只能查询到一条车辆记录
        Assert.listOnlyOne(ownerCarDtos, "未查询到或查询多条 车辆信息");
        ownerCarDto = ownerCarDtos.get(0);

        // 将车辆信息合并到返回VO对象中
        BeanConvertUtil.covertBean(ownerCarDto, apiFeeVo);

        // 构建成功响应，将整合后的VO对象转换为JSON字符串返回
        responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiFeeVo), HttpStatus.OK);

        // 设置响应实体到上下文
        context.setResponseEntity(responseEntity);
    }
}