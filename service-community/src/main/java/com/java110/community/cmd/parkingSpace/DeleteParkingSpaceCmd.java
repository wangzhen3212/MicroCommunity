/**
 * 删除车位命令类
 * 
 * 该类负责处理删除车位相关的业务逻辑，包括参数验证和车位删除操作。
 * 特别处理子母车位的删除逻辑，确保在删除前检查车位状态和相关约束条件。
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
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.intf.community.IParkingSpaceV1InnerServiceSMO;
import com.java110.po.parking.ParkingSpacePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "parkingSpace.deleteParkingSpace")
public class DeleteParkingSpaceCmd extends Cmd {

    @Autowired
    private IParkingSpaceV1InnerServiceSMO parkingSpaceV1InnerServiceSMOImpl;

    /**
     * 验证删除车位请求参数
     * 
     * 该方法用于验证删除车位请求的合法性，包括：
     * 1. 检查必要参数是否存在
     * 2. 验证车位状态是否为空闲
     * 3. 对于子母车位，检查相关车位的状态
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 检查请求报文中是否包含必要的参数
        Assert.jsonObjectHaveKey(reqJson, "psId", "请求报文中未包含psId");
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求报文中未包含communityId");
        
        // 验证车位状态必须为空闲状态才能删除
        if (!ParkingSpaceDto.STATE_FREE.equals(reqJson.getString("state"))) {
            throw new CmdException("车位不是空闲，不能做删除");
        }
        
        // 检查是否为子母车位
        ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
        parkingSpaceDto.setPsId(reqJson.getString("psId"));
        parkingSpaceDto.setTypeCd(ParkingSpaceDto.TYPE_CD_SON_MOTHER);
        parkingSpaceDto.setParkingType("2"); // 1：普通车位  2：子母车位  3：豪华车位
        parkingSpaceDto.setCommunityId(reqJson.getString("communityId"));
        List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceV1InnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);
        
        // 如果不是子母车位，直接返回
        if (parkingSpaceDtos == null || parkingSpaceDtos.size() < 1) {
            return;
        }
        
        // 构建子母车位的编号列表
        List<String> nums = new ArrayList<>();
        nums.add(parkingSpaceDtos.get(0).getNum());
        if (parkingSpaceDtos.get(0).getNum().endsWith(ParkingSpaceDto.NUM_MOTHER)) {
            // 如果是母车位，添加对应的子车位编号
            nums.add(parkingSpaceDtos.get(0).getNum().replace(ParkingSpaceDto.NUM_MOTHER, ""));
        } else {
            // 如果是子车位，添加对应的母车位编号
            nums.add(parkingSpaceDtos.get(0).getNum() + ParkingSpaceDto.NUM_MOTHER);
        }
        
        // 查询子母车位的状态
        parkingSpaceDto = new ParkingSpaceDto();
        parkingSpaceDto.setNums(nums.toArray(new String[nums.size()]));
        parkingSpaceDto.setCommunityId(reqJson.getString("communityId"));
        parkingSpaceDto.setStates(new String[]{ParkingSpaceDto.STATE_HIRE, ParkingSpaceDto.STATE_SELL});
        parkingSpaceDto.setParkingType("2"); // 1：普通车位  2：子母车位  3：豪华车位
        
        // 检查子母车位中是否有已出租或已售出的车位
        int flag = parkingSpaceV1InnerServiceSMOImpl.queryParkingSpacesCount(parkingSpaceDto);
        if (flag > 0) {
            throw new CmdException("子母车位非空闲");
        }
        
        // 将子母车位编号列表存入请求参数中，供后续处理使用
        reqJson.put("sonMotherNums", nums);
    }

    /**
     * 执行删除车位操作
     * 
     * 根据车位类型执行不同的删除逻辑：
     * 1. 普通车位：直接删除
     * 2. 子母车位：删除所有相关的子母车位
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     * @throws ParseException 当解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 如果不是子母车位，直接删除单个车位
        if (!reqJson.containsKey("sonMotherNums")) {
            deleteParkingSpace(reqJson);
            return;
        }
        
        // 处理子母车位的删除逻辑
        List<String> nums = reqJson.getObject("sonMotherNums", List.class);
        ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
        parkingSpaceDto.setNums(nums.toArray(new String[nums.size()]));
        parkingSpaceDto.setCommunityId(reqJson.getString("communityId"));
        
        // 查询所有相关的子母车位
        List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceV1InnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);
        JSONObject paramIn = null;
        
        // 遍历所有子母车位并逐个删除
        for (ParkingSpaceDto tmpParkingSpaceDto : parkingSpaceDtos) {
            paramIn = new JSONObject();
            paramIn.put("psId", tmpParkingSpaceDto.getPsId());
            deleteParkingSpace(reqJson);
        }
    }

    /**
     * 删除车位具体实现方法
     * 
     * 该方法执行实际的删除车位操作，将车位信息转换为PO对象并调用服务层进行删除。
     * 
     * @param paramInJson 接口调用方传入的参数，包含要删除的车位ID
     * @throws CmdException 当删除操作失败时抛出异常
     */
    public void deleteParkingSpace(JSONObject paramInJson) {
        // 构建业务参数
        JSONObject businessParkingSpace = new JSONObject();
        businessParkingSpace.put("psId", paramInJson.getString("psId"));
        
        // 将JSON对象转换为PO对象
        ParkingSpacePo parkingSpacePo = BeanConvertUtil.covertBean(businessParkingSpace, ParkingSpacePo.class);
        
        // 调用服务层删除车位
        int flag = parkingSpaceV1InnerServiceSMOImpl.deleteParkingSpace(parkingSpacePo);

        // 检查删除操作是否成功
        if (flag < 1) {
            throw new CmdException("删除车位失败");
        }
    }
}