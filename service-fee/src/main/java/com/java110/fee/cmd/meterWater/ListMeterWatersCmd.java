/*
 * Copyright 2017-2020 吴学文 and java110 team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.java110.fee.cmd.meterWater;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.room.RoomDto;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.intf.community.IParkingSpaceInnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.fee.IMeterWaterInnerServiceSMO;
import com.java110.intf.fee.IMeterWaterV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.meter.MeterWaterDto;

import java.util.List;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：查询水表读数记录的命令类
 * 服务编码：meterWater.listMeterWater
 * 请求路径：/app/meterWater.ListMeterWater
 * 功能描述：该命令类用于处理查询水表读数记录的请求，支持按房间号或车位号查询，并返回分页结果
 * @author 吴学文 at 2022-07-21 09:17:10 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "meterWater.listMeterWaters")
public class ListMeterWatersCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListMeterWatersCmd.class);

    @Autowired
    private IMeterWaterV1InnerServiceSMO meterWaterV1InnerServiceSMOImpl;

    @Autowired
    private IMeterWaterInnerServiceSMO meterWaterInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 功能描述：验证请求参数的有效性，包括分页信息和必要参数
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页信息
        super.validatePageInfo(reqJson);
        // 验证小区ID是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID");
    }

    /**
     * 命令执行方法
     * 功能描述：执行查询水表读数记录的主要业务逻辑，包括参数处理、数据查询和结果返回
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为MeterWaterDto对象
        MeterWaterDto meterWaterDto = BeanConvertUtil.covertBean(reqJson, MeterWaterDto.class);
        ResultVo resultVo = null;
        
        // 刷新费用参数，如果刷新失败则返回空结果
        if (!freshFeeDtoParam(meterWaterDto, reqJson)) {
            ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(1, 0, new JSONArray());
            context.setResponseEntity(responseEntity);
            return;
        }

        // 查询水表记录总数
        int count = meterWaterInnerServiceSMOImpl.queryMeterWatersCount(meterWaterDto);

        List<MeterWaterDto> meterWaterDtos = null;

        if (count > 0) {
            // 如果有数据，则查询具体的水表记录列表
            meterWaterDtos = meterWaterInnerServiceSMOImpl.queryMeterWaters(meterWaterDto);
        } else {
            // 如果没有数据，返回空列表
            meterWaterDtos = new ArrayList<>();
        }

        // 构造返回结果，包含分页信息
        resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, meterWaterDtos);

        // 设置响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        context.setResponseEntity(responseEntity);
    }

    /**
     * 刷新费用参数方法
     * 功能描述：根据房间号或车位号刷新查询参数，将房间号或车位号转换为对应的对象ID
     * @param meterWaterDto 水表数据传输对象
     * @param reqJson 请求的JSON对象
     * @return boolean 参数刷新是否成功
     */
    private boolean freshFeeDtoParam(MeterWaterDto meterWaterDto, JSONObject reqJson) {

        // 检查请求中是否包含房间号参数
        if (!reqJson.containsKey("roomNum")) {
            return true;
        }

        String roomNum = reqJson.getString("roomNum");

        // 如果房间号为空，直接返回true
        if (StringUtil.isEmpty(roomNum)) {
            return true;
        }

        // 房间号必须包含"-"分隔符
        if (!roomNum.contains("-")) {
            return false;
        }

        // 如果对象类型为空，默认设置为房间类型
        if (StringUtil.isEmpty(meterWaterDto.getObjType())) {
            meterWaterDto.setObjType(MeterWaterDto.PAYER_OBJ_TYPE_ROOM);
        }

        // 根据对象类型处理不同的业务逻辑
        if (MeterWaterDto.PAYER_OBJ_TYPE_ROOM.equals(meterWaterDto.getObjType())) {
            // 房间号格式：楼栋号-单元号-房间号
            String[] nums = roomNum.split("-", 3);
            if (nums.length != 3) {
                return false;
            }
            RoomDto roomDto = new RoomDto();
            roomDto.setFloorNum(nums[0]);    // 设置楼栋号
            roomDto.setUnitNum(nums[1]);     // 设置单元号
            roomDto.setRoomNum(nums[2]);     // 设置房间号
            roomDto.setCommunityId(meterWaterDto.getCommunityId()); // 设置小区ID
            List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);

            // 如果查询不到对应的房间，返回false
            if (roomDtos == null || roomDtos.size() < 1) {
                return false;
            }
            // 设置房间ID到水表DTO中
            meterWaterDto.setObjId(roomDtos.get(0).getRoomId());

        } else {
            // 车位号格式：区域号-车位号
            String[] nums = roomNum.split("-", 2);
            if (nums.length != 2) {
                return false;
            }
            ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
            parkingSpaceDto.setAreaNum(nums[0]);    // 设置区域号
            parkingSpaceDto.setNum(nums[1]);        // 设置车位号
            parkingSpaceDto.setCommunityId(meterWaterDto.getCommunityId()); // 设置小区ID
            List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);

            // 如果查询不到对应的车位，返回false
            if (parkingSpaceDtos == null || parkingSpaceDtos.size() < 1) {
                return false;
            }
            // 设置车位ID到水表DTO中
            meterWaterDto.setObjId(parkingSpaceDtos.get(0).getPsId());
        }

        return true;
    }
}