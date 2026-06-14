/**
 * 车辆添加停车位命令处理类
 * 
 * 功能：处理车辆与停车位的绑定关系，包括验证车辆和车位状态、更新车辆和车位信息等操作
 * 主要用于实现车辆续租停车位的业务逻辑
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode owner.carAddParkingSpace
 */
package com.java110.user.cmd.owner;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.intf.community.IParkingSpaceInnerServiceSMO;
import com.java110.intf.community.IParkingSpaceV1InnerServiceSMO;
import com.java110.intf.user.IOwnerCarInnerServiceSMO;
import com.java110.intf.user.IOwnerCarV1InnerServiceSMO;
import com.java110.po.car.OwnerCarPo;
import com.java110.po.parking.ParkingSpacePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Java110Cmd(serviceCode = "owner.carAddParkingSpace")
public class CarAddParkingSpaceCmd extends Cmd {

    @Autowired
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl;

    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;

    @Autowired
    private IParkingSpaceV1InnerServiceSMO parkingSpaceV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerCarV1InnerServiceSMO ownerCarV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 功能：验证请求参数的有效性，包括必填字段检查、车辆信息查询和状态验证
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 数据流上下文，用于获取和设置上下文数据
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必填字段是否存在
        Assert.jsonObjectHaveKey(reqJson, "communityId", "未包含小区ID");
        Assert.jsonObjectHaveKey(reqJson, "carId", "请求报文中未包含carId");
        Assert.jsonObjectHaveKey(reqJson, "startTime", "请求报文中未包含startTime");
        Assert.jsonObjectHaveKey(reqJson, "endTime", "请求报文中未包含startTime");
        Assert.jsonObjectHaveKey(reqJson, "psId", "请求报文中未包含psId");
        Assert.hasLength(reqJson.getString("communityId"), "小区ID不能为空");

        // 查询车辆信息
        OwnerCarDto ownerCarDto = new OwnerCarDto();
        ownerCarDto.setCarId(reqJson.getString("carId"));
        ownerCarDto.setCommunityId(reqJson.getString("communityId"));
        List<OwnerCarDto> ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);
        
        // 验证车辆信息是否存在且唯一
        Assert.listOnlyOne(ownerCarDtos, "未找到车辆信息");

        // 检查车辆状态，如果车辆已有有效车位则不允许续租
        String state = ownerCarDtos.get(0).getState();
        if (!StringUtil.isEmpty(state) && !state.equals(OwnerCarDto.STATE_FINISH)) {
            throw new IllegalArgumentException("已有车位无需续租");
        }
    }

    /**
     * 命令执行方法
     * 
     * 功能：执行车辆与停车位绑定的核心业务逻辑，包括车位状态验证、车辆信息更新和车位信息更新
     * 
     * @param event 命令事件对象
     * @param context 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当业务逻辑执行失败时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为车辆PO对象
        OwnerCarPo ownerCarPo = BeanConvertUtil.covertBean(reqJson, OwnerCarPo.class);
        
        // 查询停车位信息
        ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
        parkingSpaceDto.setPsId(ownerCarPo.getPsId());
        List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);
        
        // 验证车位信息是否存在且唯一
        Assert.listOnlyOne(parkingSpaceDtos, "查询车位信息错误！");
        
        // 检查车位状态，只有空闲状态的车位才能被续租
        String state = parkingSpaceDtos.get(0).getState();
        if (!StringUtil.isEmpty(state) && !state.equals("F")) { // 如果车位状态不是空闲的，就不能续租
            throw new IllegalArgumentException("车位已被使用，无法继续续租！");
        }
        
        // 更新车辆状态为正常使用状态
        ownerCarPo.setState(OwnerCarDto.STATE_NORMAL);
        int flag = ownerCarV1InnerServiceSMOImpl.updateOwnerCar(ownerCarPo);
        if (flag < 1) {
            throw new IllegalArgumentException("修改车辆出错");
        }
        
        // 设置车位状态为出租状态
        reqJson.put("carNumType", ParkingSpaceDto.STATE_HIRE);
        
        // 重新查询车位信息用于更新
        parkingSpaceDto = new ParkingSpaceDto();
        parkingSpaceDto.setCommunityId(reqJson.getString("communityId"));
        parkingSpaceDto.setPsId(reqJson.getString("psId"));
        parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);

        // 验证车位信息是否存在
        if (parkingSpaceDtos == null || parkingSpaceDtos.size() != 1) {
            //throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "未查询到停车位信息" + JSONObject.toJSONString(parkingSpaceDto));
            return;
        }

        // 获取车位详细信息
        parkingSpaceDto = parkingSpaceDtos.get(0);

        // 构建车位业务对象
        JSONObject businessParkingSpace = new JSONObject();
        businessParkingSpace.putAll(BeanConvertUtil.beanCovertMap(parkingSpaceDto));
        businessParkingSpace.put("state", reqJson.getString("carNumType"));
        
        // 更新车位状态为出租状态
        ParkingSpacePo parkingSpacePo = BeanConvertUtil.covertBean(businessParkingSpace, ParkingSpacePo.class);
        flag = parkingSpaceV1InnerServiceSMOImpl.updateParkingSpace(parkingSpacePo);
        if (flag < 1) {
            throw new IllegalArgumentException("修改车辆出错");
        }
    }
}