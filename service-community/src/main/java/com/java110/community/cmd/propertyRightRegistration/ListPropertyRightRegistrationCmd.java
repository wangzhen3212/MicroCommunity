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
package com.java110.community.cmd.propertyRightRegistration;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.floor.FloorDto;
import com.java110.dto.room.RoomDto;
import com.java110.dto.unit.UnitDto;
import com.java110.intf.community.IFloorInnerServiceSMO;
import com.java110.intf.community.IPropertyRightRegistrationV1InnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.community.IUnitInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.room.PropertyRightRegistrationDto;

import java.util.List;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;


/**
 * 类表述：查询
 * 服务编码：propertyRightRegistration.listPropertyRightRegistration
 * 请求路劲：/app/propertyRightRegistration.ListPropertyRightRegistration
 * add by 吴学文 at 2021-10-09 10:34:14 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 产权登记查询命令类
 * 负责处理产权登记信息的查询请求，支持按房屋编号查询和分页查询
 * 通过解析房屋编号（楼栋-单元-房屋）获取对应的房屋ID，然后查询该房屋的产权登记信息
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2021-10-09
 */
@Java110Cmd(serviceCode = "propertyRightRegistration.listPropertyRightRegistration")
public class ListPropertyRightRegistrationCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListPropertyRightRegistrationCmd.class);

    /**
     * 产权登记服务接口
     */
    @Autowired
    private IPropertyRightRegistrationV1InnerServiceSMO propertyRightRegistrationV1InnerServiceSMOImpl;

    /**
     * 楼栋服务接口
     */
    @Autowired
    private IFloorInnerServiceSMO floorInnerServiceSMOImpl;

    /**
     * 单元服务接口
     */
    @Autowired
    private IUnitInnerServiceSMO unitInnerServiceSMOImpl;

    /**
     * 房屋服务接口
     */
    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性，主要验证分页参数
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        super.validatePageInfo(reqJson);
    }

    /**
     * 执行命令方法
     * 处理产权登记查询请求，支持按房屋编号查询和分页查询
     * 如果请求中包含房屋编号，会解析编号并查询对应的房屋ID
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 处理房屋编号查询逻辑
        if (reqJson.containsKey("allNum") && !StringUtil.isEmpty(reqJson.getString("allNum"))) {
            // 获取房屋编号(楼栋-单元-房屋)
            String allNum = reqJson.getString("allNum");
            String[] split = allNum.split("-");
            
            // 验证房屋编号格式是否正确（必须是楼栋-单元-房屋的三段式）
            if (split.length != 3) {
                ResultVo resultVo = new ResultVo(ResultVo.CODE_ERROR, "房屋编号格式错误！");
                ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
                cmdDataFlowContext.setResponseEntity(responseEntity);
                return;
            }
            
            String floorNum = split[0]; // 楼栋号
            String unitNum = split[1]; // 单元号
            String roomNum = split[2]; // 房间号
            
            // 根据楼栋号和小区id查询楼栋信息
            FloorDto floorDto = new FloorDto();
            floorDto.setFloorNum(floorNum);
            floorDto.setCommunityId(reqJson.getString("communityId"));
            List<FloorDto> floorDtos = floorInnerServiceSMOImpl.queryFloors(floorDto);
            Assert.listOnlyOne(floorDtos, "查询楼栋错误！");
            
            // 根据楼栋id和单元号查询单元信息
            UnitDto unitDto = new UnitDto();
            unitDto.setFloorId(floorDtos.get(0).getFloorId());
            unitDto.setUnitNum(unitNum);
            List<UnitDto> unitDtos = unitInnerServiceSMOImpl.queryUnits(unitDto);
            Assert.listOnlyOne(unitDtos, "查询小区错误！");
            
            // 根据单元id和房间号查询房间信息
            RoomDto roomDto = new RoomDto();
            roomDto.setUnitId(unitDtos.get(0).getUnitId());
            roomDto.setRoomNum(roomNum);
            List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);
            Assert.listOnlyOne(roomDtos, "查询房屋错误！");
            
            // 获取房屋id并设置到请求参数中
            String roomId = roomDtos.get(0).getRoomId();
            reqJson.put("roomId", roomId);
        }

        // 将请求参数转换为产权登记DTO对象
        PropertyRightRegistrationDto propertyRightRegistrationDto = BeanConvertUtil.covertBean(reqJson, PropertyRightRegistrationDto.class);

        // 查询产权登记记录总数
        int count = propertyRightRegistrationV1InnerServiceSMOImpl.queryPropertyRightRegistrationsCount(propertyRightRegistrationDto);

        List<PropertyRightRegistrationDto> propertyRightRegistrationDtos = null;

        // 根据查询结果数量决定返回的数据
        if (count > 0) {
            // 查询产权登记详情列表
            propertyRightRegistrationDtos = propertyRightRegistrationV1InnerServiceSMOImpl.queryPropertyRightRegistrations(propertyRightRegistrationDto);
        } else {
            // 如果没有数据，返回空列表
            propertyRightRegistrationDtos = new ArrayList<>();
        }

        // 计算总页数并构建返回结果
        // 使用Math.ceil确保向上取整，正确处理最后一页的数据
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, propertyRightRegistrationDtos);

        // 设置HTTP响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}