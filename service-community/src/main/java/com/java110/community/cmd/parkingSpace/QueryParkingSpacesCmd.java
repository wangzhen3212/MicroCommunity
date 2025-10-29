/**
 * 停车位查询命令类
 * 
 * 该类负责处理停车位查询相关的业务逻辑，支持按条件分页查询停车位信息，
 * 同时支持根据车牌号查询对应的停车位信息
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
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
import com.java110.utils.util.StringUtil;
import com.java110.vo.api.ApiParkingSpaceDataVo;
import com.java110.vo.api.ApiParkingSpaceVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "parkingSpace.queryParkingSpaces")
public class QueryParkingSpacesCmd extends Cmd {

    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;

    @Autowired
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求中是否包含必要的参数，包括分页参数和社区ID
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 验证请求中必须包含page参数
        Assert.jsonObjectHaveKey(reqJson, "page", "请求中未包含page信息");
        // 验证请求中必须包含row参数
        Assert.jsonObjectHaveKey(reqJson, "row", "请求中未包含row信息");
        // 验证请求中必须包含communityId参数
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求中未包含communityId信息");
        // 验证page参数是否为有效数字
        Assert.isInteger(reqJson.getString("page"), "不是有效数字");
        // 验证row参数是否为有效数字
        Assert.isInteger(reqJson.getString("row"), "不是有效数字");
    }

    /**
     * 执行停车位查询命令
     * 
     * 根据请求参数查询停车位信息，支持按车牌号查询和普通条件查询两种方式
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 如果请求中包含车牌号且不为空，则按车牌号查询停车位
        if (reqJson.containsKey("carNum") && !StringUtil.isEmpty(reqJson.getString("carNum"))) {
            queryParkingSpaceByCarNum(reqJson, cmdDataFlowContext);
            return;
        }
        
        // 获取每页显示条数
        int row = reqJson.getInteger("row");
        // 创建返回VO对象
        ApiParkingSpaceVo apiParkingSpaceVo = new ApiParkingSpaceVo();
        
        // 将请求参数转换为停车位DTO对象
        ParkingSpaceDto parkingSpaceDto = BeanConvertUtil.covertBean(reqJson, ParkingSpaceDto.class);
        
        // 查询总记录数
        int total = parkingSpaceInnerServiceSMOImpl.queryParkingSpacesCount(parkingSpaceDto);
        apiParkingSpaceVo.setTotal(total);
        
        // 如果存在记录，则查询详细数据
        if (total > 0) {
            List<ParkingSpaceDto> parkingSpaceDtoList = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(BeanConvertUtil.covertBean(reqJson, ParkingSpaceDto.class));
            // 将DTO列表转换为VO列表
            apiParkingSpaceVo.setParkingSpaces(BeanConvertUtil.covertBeanList(parkingSpaceDtoList, ApiParkingSpaceDataVo.class));
        }
        
        // 计算总页数
        apiParkingSpaceVo.setRecords((int) Math.ceil((double) total / (double) row));
        
        // 构建响应实体并设置到上下文
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiParkingSpaceVo), HttpStatus.OK);
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 根据车牌号查询停车位信息
     * 
     * 通过车牌号查询对应的业主车辆信息，再根据车辆关联的停车位ID查询停车位详情
     * 
     * @param reqJson 请求报文数据对象
     * @param cmdDataFlowContext 命令数据流上下文对象
     */
    private void queryParkingSpaceByCarNum(JSONObject reqJson, ICmdDataFlowContext cmdDataFlowContext) {
        ApiParkingSpaceVo apiParkingSpaceVo = new ApiParkingSpaceVo();
        // 获取每页显示条数
        int row = reqJson.getInteger("row");
        
        // 将请求参数转换为业主车辆DTO对象
        OwnerCarDto ownerCarDto = BeanConvertUtil.covertBean(reqJson, OwnerCarDto.class);
        // 根据车牌号查询业主车辆信息
        List<OwnerCarDto> ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);
        // 设置总记录数
        apiParkingSpaceVo.setTotal(ownerCarDtos.size());
        
        // 如果查询到车辆信息，则查询对应的停车位信息
        if (ownerCarDtos.size() > 0) {
            ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
            // 获取所有停车位ID
            parkingSpaceDto.setPsIds(getPsIds(ownerCarDtos));
            // 根据停车位ID列表查询停车位详情
            List<ParkingSpaceDto> parkingSpaceDtoList = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);
            // 将停车位DTO列表转换为VO列表
            apiParkingSpaceVo.setParkingSpaces(BeanConvertUtil.covertBeanList(parkingSpaceDtoList, ApiParkingSpaceDataVo.class));
        }
        
        // 计算总页数
        apiParkingSpaceVo.setRecords((int) Math.ceil((double) ownerCarDtos.size() / (double) row));
        
        // 构建响应实体并设置到上下文
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiParkingSpaceVo), HttpStatus.OK);
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 从业主车辆信息中提取停车位ID数组
     * 
     * 遍历业主车辆列表，收集所有关联的停车位ID
     * 
     * @param ownerCarDtos 业主车辆信息列表
     * @return 停车位ID数组
     */
    private String[] getPsIds(List<OwnerCarDto> ownerCarDtos) {
        List<String> psIds = new ArrayList<String>();
        // 遍历业主车辆列表，提取停车位ID
        for (OwnerCarDto ownerCarDto : ownerCarDtos) {
            psIds.add(ownerCarDto.getPsId());
        }
        // 将List转换为数组返回
        return psIds.toArray(new String[psIds.size()]);
    }

    /**
     * 请求数据处理方法
     * 
     * 对请求参数进行预处理，主要处理状态参数的转换
     * 
     * @param reqJson 请求数据对象
     */
    private void refreshReqJson(JSONObject reqJson) {
        // 如果请求中不包含state参数，直接返回
        if (!reqJson.containsKey("state")) {
            return;
        }
        
        // 如果state参数值为"SH"，则转换为包含"S"和"H"的数组
        if ("SH".equals(reqJson.getString("state"))) {
            reqJson.put("states", new String[]{"S", "H"});
            // 移除原始的state参数
            reqJson.remove("state");
        }
    }
}