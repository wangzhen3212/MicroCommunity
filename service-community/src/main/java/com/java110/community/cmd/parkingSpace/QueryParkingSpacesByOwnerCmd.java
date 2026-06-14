package com.java110.community.cmd.parkingSpace;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.intf.community.IParkingSpaceInnerServiceSMO;
import com.java110.intf.user.IOwnerCarInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.ApiParkingSpaceDataVo;
import com.java110.vo.api.ApiParkingSpaceVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 查询业主停车位信息命令类
 * 
 * 该命令类用于处理根据业主ID查询其关联的停车位信息的业务逻辑，
 * 包括验证请求参数、查询业主车辆信息、查询停车位信息并返回格式化结果
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode parkingSpace.queryParkingSpacesByOwner
 */
@Java110Cmd(serviceCode = "parkingSpace.queryParkingSpacesByOwner")
public class QueryParkingSpacesByOwnerCmd extends Cmd{

    /**
     * 停车位内部服务接口
     */
    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;

    /**
     * 业主车辆内部服务接口
     */
    @Autowired
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl;

    /**
     * 验证请求参数方法
     * 
     * 该方法用于验证请求参数是否完整，确保必要的参数存在
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求中必须包含communityId参数
        Assert.hasKeyAndValue(reqJson, "communityId", "请求中未包含communityId信息");
        // 验证请求中必须包含ownerId参数
        Assert.hasKeyAndValue(reqJson, "ownerId", "请求中未包含ownerId信息");
    }

    /**
     * 执行命令方法
     * 
     * 该方法负责主要的业务逻辑处理：
     * 1. 根据业主ID查询其拥有的车辆信息
     * 2. 如果没有车辆则返回空结果
     * 3. 遍历每辆车，查询对应的停车位信息
     * 4. 组装停车位和车辆信息返回给前端
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 命令执行异常
     * @throws ParseException 解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 创建停车位VO对象用于封装返回数据
        ApiParkingSpaceVo apiParkingSpaceVo = new ApiParkingSpaceVo();

        // 创建业主车辆DTO对象并设置业主ID
        OwnerCarDto ownerCarDto = new OwnerCarDto();
        ownerCarDto.setOwnerId(reqJson.getString("ownerId"));

        // 根据业主ID查询其拥有的车辆信息
        List<OwnerCarDto> ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);

        // 在没有停车位的情况下直接返回空数组
        ResponseEntity<String> responseEntity = null;
        if (ownerCarDtos == null || ownerCarDtos.size() == 0) {
            // 返回空的停车位数组
            responseEntity = new ResponseEntity<String>("{\"parkingSpaces\":[]}", HttpStatus.OK);
            context.setResponseEntity(responseEntity);
            return;
        }

        ParkingSpaceDto parkingSpaceDto = null;
        // 创建停车位数据VO列表用于存储最终结果
        List<ApiParkingSpaceDataVo> apiParkingSpaceDataVos = new ArrayList<>();
        
        // 遍历业主的每辆车，查询对应的停车位信息
        for (OwnerCarDto tmpOwnerCarDto : ownerCarDtos) {
            // 创建停车位查询条件
            parkingSpaceDto = new ParkingSpaceDto();
            parkingSpaceDto.setCommunityId(reqJson.getString("communityId"));
            parkingSpaceDto.setPsId(tmpOwnerCarDto.getPsId());
            
            // 根据停车位ID查询停车位信息
            List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);

            // 如果查询结果不为1条记录，则跳过该车辆（可能存在数据异常）
            if (parkingSpaceDtos == null || parkingSpaceDtos.size() != 1) {
                // 注释掉的异常抛出：throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "根据psId 查询存在多条停车位信息" + tmpOwnerCarDto.getPsId());
                continue; // 跳过当前车辆，继续处理下一辆
            }

            // 获取唯一的停车位信息
            parkingSpaceDto = parkingSpaceDtos.get(0);

            // 将业主车辆信息转换为停车位数据VO对象
            ApiParkingSpaceDataVo apiParkingSpaceDataVo = BeanConvertUtil.covertBean(tmpOwnerCarDto, ApiParkingSpaceDataVo.class);

            // 将停车位信息合并到VO对象中
            apiParkingSpaceDataVo = BeanConvertUtil.covertBean(parkingSpaceDto, apiParkingSpaceDataVo);
            
            // 设置车辆相关信息
            apiParkingSpaceDataVo.setCarNum(tmpOwnerCarDto.getCarNum());
            apiParkingSpaceDataVo.setCarType(tmpOwnerCarDto.getCarType());
            apiParkingSpaceDataVo.setCarTypeName(tmpOwnerCarDto.getCarTypeName());

            // 将完整的停车位数据添加到结果列表
            apiParkingSpaceDataVos.add(apiParkingSpaceDataVo);
        }

        // 设置返回VO对象的停车位列表
        apiParkingSpaceVo.setParkingSpaces(apiParkingSpaceDataVos);
        
        // 将结果转换为JSON字符串并设置响应
        responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiParkingSpaceVo), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }
}