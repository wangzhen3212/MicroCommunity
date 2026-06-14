package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.intf.community.IParkingSpaceInnerServiceSMO;
import com.java110.intf.user.IOwnerCarInnerServiceSMO;
import com.java110.intf.user.IOwnerInnerServiceSMO;
import com.java110.intf.user.IOwnerRoomRelInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.ApiParkingSpaceDataVo;
import com.java110.vo.api.ApiParkingSpaceVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 费用设置相关的停车位查询命令类
 * 功能：根据不同的查询条件查询停车位信息，主要用于费用设置场景
 * 支持按车位基本信息查询和按车牌号查询两种方式
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "fee.listParkingSpacesWhereFeeSet")
public class ListParkingSpacesWhereFeeSetCmd extends Cmd {

    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;

    @Autowired
    private IOwnerInnerServiceSMO ownerInnerServiceSMOImpl;

    @Autowired
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl;

    @Autowired
    private IOwnerRoomRelInnerServiceSMO ownerRoomRelInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 调用父类方法验证分页信息
        super.validatePageInfo(reqJson);
        // 验证小区ID是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID");
    }

    /**
     * 执行命令的主方法
     * 根据不同的查询条件执行相应的停车位查询逻辑
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        ApiParkingSpaceVo apiParkingSpaceVo = new ApiParkingSpaceVo();
        
        // 如果请求中包含车牌号，则按车牌号查询停车位信息
        if (reqJson.containsKey("carNum")) {
            queryParkingSpaceByCarInfo(apiParkingSpaceVo, reqJson, context);
            return;
        }

        // 按车位基本信息查询
        ParkingSpaceDto parkingSpaceDto = BeanConvertUtil.covertBean(reqJson, ParkingSpaceDto.class);
        // 设置关联查询车主信息标志
        parkingSpaceDto.setWithOwnerCar(true);
        
        // 查询总记录数
        int total = parkingSpaceInnerServiceSMOImpl.queryParkingSpacesCount(BeanConvertUtil.covertBean(reqJson, ParkingSpaceDto.class));
        apiParkingSpaceVo.setTotal(total);
        
        if (total > 0) {
            // 查询停车位列表数据
            List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);

            // 刷新停车位的业主信息
            refreshParkingSpaceOwners(reqJson.getString("communityId"), parkingSpaceDtos);

            // 转换数据格式
            apiParkingSpaceVo.setParkingSpaces(BeanConvertUtil.covertBeanList(parkingSpaceDtos, ApiParkingSpaceDataVo.class));
        }
        
        // 计算总页数
        int row = reqJson.getInteger("row");
        apiParkingSpaceVo.setRecords((int) Math.ceil((double) total / (double) row));

        // 设置响应结果
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiParkingSpaceVo), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 根据车牌号查询停车位信息
     * 通过车主车辆信息关联查询对应的停车位信息
     *
     * @param apiParkingSpaceVo 停车位VO对象，用于存储查询结果
     * @param reqJson 请求的JSON数据，包含查询条件
     * @param context 命令数据流上下文
     */
    private void queryParkingSpaceByCarInfo(ApiParkingSpaceVo apiParkingSpaceVo, JSONObject reqJson, ICmdDataFlowContext context) {
        // 转换请求参数为车辆DTO对象
        OwnerCarDto ownerCarDto = BeanConvertUtil.covertBean(reqJson, OwnerCarDto.class);
        
        // 查询车辆信息总数
        int total = ownerCarInnerServiceSMOImpl.queryOwnerCarsCount(ownerCarDto);
        apiParkingSpaceVo.setTotal(total);
        
        if (total > 0) {
            // 查询车辆详细信息列表
            List<OwnerCarDto> ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);
            List<ParkingSpaceDto> parkingSpaceDtos = null;

            // 根据车辆信息刷新对应的停车位信息
            parkingSpaceDtos = refreshCarParkingSpaces(reqJson.getString("communityId"), ownerCarDtos);

            // 转换数据格式
            apiParkingSpaceVo.setParkingSpaces(BeanConvertUtil.covertBeanList(parkingSpaceDtos, ApiParkingSpaceDataVo.class));
        }
        
        // 计算总页数
        int row = reqJson.getInteger("row");
        apiParkingSpaceVo.setRecords((int) Math.ceil((double) total / (double) row));

        // 设置响应结果
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiParkingSpaceVo), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 根据车辆信息刷新停车位信息
     * 通过车辆与停车位的关联关系，获取完整的停车位信息
     *
     * @param communityId 小区ID
     * @param ownerCarDtos 车主车辆信息列表
     * @return List<ParkingSpaceDto> 包含完整业主信息的停车位列表
     */
    private List<ParkingSpaceDto> refreshCarParkingSpaces(String communityId, List<OwnerCarDto> ownerCarDtos) {
        // 收集所有停车位ID
        List<String> psIds = new ArrayList<>();
        for (OwnerCarDto ownerCarDto : ownerCarDtos) {
            psIds.add(ownerCarDto.getPsId());
        }
        
        // 构建停车位查询条件
        ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
        parkingSpaceDto.setCommunityId(communityId);
        parkingSpaceDto.setPsIds(psIds.toArray(new String[psIds.size()]));
        
        // 查询停车位信息
        List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);

        // 将车辆信息中的业主信息设置到对应的停车位中
        for (ParkingSpaceDto tmpParkingSpaceDto : parkingSpaceDtos) {
            for (OwnerCarDto ownerCarDto : ownerCarDtos) {
                // 通过停车位ID匹配对应的车辆信息
                if (tmpParkingSpaceDto.getPsId().equals(ownerCarDto.getPsId())) {
                    // 设置业主相关信息到停车位对象中
                    tmpParkingSpaceDto.setOwnerId(ownerCarDto.getOwnerId());
                    tmpParkingSpaceDto.setOwnerName(ownerCarDto.getOwnerName());
                    tmpParkingSpaceDto.setIdCard(ownerCarDto.getIdCard());
                    tmpParkingSpaceDto.setLink(ownerCarDto.getLink());
                }
            }
        }

        return parkingSpaceDtos;
    }

    /**
     * 刷新停车位的业主信息
     * 通过停车位与车辆的关联关系，获取并设置每个停车位对应的业主信息
     *
     * @param communityId 小区ID
     * @param parkingSpaceDtos 停车位信息列表
     */
    private void refreshParkingSpaceOwners(String communityId, List<ParkingSpaceDto> parkingSpaceDtos) {
        // 收集所有停车位ID
        List<String> psIds = new ArrayList<>();
        for (ParkingSpaceDto parkingSpaceDto : parkingSpaceDtos) {
            psIds.add(parkingSpaceDto.getPsId());
        }
        
        // 构建车辆查询条件
        OwnerCarDto ownerCarDto = new OwnerCarDto();
        ownerCarDto.setCommunityId(communityId);
        ownerCarDto.setPsIds(psIds.toArray(new String[psIds.size()]));
        // 设置关联查询业主信息标志
        ownerCarDto.setWithOwner(true);
        
        // 查询车辆信息（包含业主信息）
        List<OwnerCarDto> ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);

        // 将车辆信息中的业主信息设置到对应的停车位中
        for (OwnerCarDto tmp : ownerCarDtos) {
            for (ParkingSpaceDto tmpParkingSpaceDto : parkingSpaceDtos) {
                // 通过停车位ID匹配对应的车辆信息
                if (tmpParkingSpaceDto.getPsId().equals(tmp.getPsId())) {
                    // 设置业主相关信息到停车位对象中
                    tmpParkingSpaceDto.setOwnerId(tmp.getOwnerId());
                    tmpParkingSpaceDto.setOwnerName(tmp.getOwnerName());
                    tmpParkingSpaceDto.setIdCard(tmp.getIdCard());
                    tmpParkingSpaceDto.setLink(tmp.getLink());
                    tmpParkingSpaceDto.setCarNum(tmp.getCarNum());
                }
            }
        }
    }
}