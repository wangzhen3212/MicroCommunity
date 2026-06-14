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
package com.java110.community.cmd.communityLocation;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.floor.FloorDto;
import com.java110.dto.unit.UnitDto;
import com.java110.dto.parking.ParkingAreaDto;
import com.java110.dto.parking.ParkingBoxDto;
import com.java110.intf.community.*;
import com.java110.po.community.CommunityLocationPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 类表述：更新
 * 服务编码：communityLocation.updateCommunityLocation
 * 请求路劲：/app/communityLocation.UpdateCommunityLocation
 * add by 吴学文 at 2022-07-17 00:10:38 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 社区位置更新命令类
 * 
 * 该类负责处理社区位置信息的更新操作，支持不同类型的社区位置更新，
 * 包括单元、楼栋、停车场、岗亭等位置类型的更新处理
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-07-17
 */
@Java110Cmd(serviceCode = "communityLocation.updateCommunityLocation")
public class UpdateCommunityLocationCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdateCommunityLocationCmd.class);

    @Autowired
    private ICommunityLocationV1InnerServiceSMO communityLocationV1InnerServiceSMOImpl;

    @Autowired
    private IFloorInnerServiceSMO floorInnerServiceSMOImpl;

    @Autowired
    private IUnitInnerServiceSMO unitInnerServiceSMOImpl;

    @Autowired
    private IParkingAreaInnerServiceSMO parkingAreaInnerServiceSMOImpl;

    @Autowired
    private IParkingBoxV1InnerServiceSMO parkingBoxV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证更新社区位置信息所需的必要参数是否完整
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证locationId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "locationId", "locationId不能为空");
        // 验证communityId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行社区位置更新命令
     * 
     * 根据不同的位置类型处理相应的位置信息更新逻辑，
     * 包括单元、楼栋、停车场、岗亭等类型的位置更新
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为社区位置PO对象
        CommunityLocationPo communityLocationPo = BeanConvertUtil.covertBean(reqJson, CommunityLocationPo.class);
        
        // 处理单元类型的位置更新（2000代表单元类型）
        if (!StringUtil.isEmpty(communityLocationPo.getLocationType()) && communityLocationPo.getLocationType().equals("2000")) { //1000 小区；2000 单元；3000 房屋；4000 岗亭；5000 部门；6000 楼栋；7000 停车场
            // 查询单元信息
            UnitDto unitDto = new UnitDto();
            unitDto.setUnitId(reqJson.getString("unitId"));
            List<UnitDto> unitDtos = unitInnerServiceSMOImpl.queryUnits(unitDto);
            Assert.listOnlyOne(unitDtos, "查询单元错误！");
            
            // 查询楼栋信息
            FloorDto floorDto = new FloorDto();
            floorDto.setFloorId(reqJson.getString("floorId"));
            List<FloorDto> floorDtos = floorInnerServiceSMOImpl.queryFloors(floorDto);
            Assert.listOnlyOne(floorDtos, "查询房屋错误！");
            
            // 设置位置对象ID和名称
            communityLocationPo.setLocationObjId(unitDtos.get(0).getUnitId());
            communityLocationPo.setLocationName(floorDtos.get(0).getFloorNum() + "号楼" + unitDtos.get(0).getUnitNum() + "单元");
            communityLocationPo.setLocationObjName(communityLocationPo.getLocationName());
        }
        
        // 处理楼栋类型的位置更新（6000代表楼栋类型）
        if (!StringUtil.isEmpty(communityLocationPo.getLocationType()) && communityLocationPo.getLocationType().equals("6000")) { //6000 楼栋
            communityLocationPo.setLocationObjId(reqJson.getString("floorId"));
            
            // 查询楼栋信息
            FloorDto floorDto = new FloorDto();
            floorDto.setFloorId(reqJson.getString("floorId"));
            List<FloorDto> floorDtos = floorInnerServiceSMOImpl.queryFloors(floorDto);
            Assert.listOnlyOne(floorDtos, "查询楼栋错误！");
            
            // 设置位置名称
            communityLocationPo.setLocationName(floorDtos.get(0).getFloorNum() + "号楼");
            communityLocationPo.setLocationObjName(communityLocationPo.getLocationName());
        }
        
        // 处理停车场类型的位置更新（7000代表停车场类型）
        if (!StringUtil.isEmpty(communityLocationPo.getLocationType()) && communityLocationPo.getLocationType().equals("7000")) { //7000 停车场
            // 查询停车场区域信息
            ParkingAreaDto parkingAreaDto = new ParkingAreaDto();
            parkingAreaDto.setPaId(reqJson.getString("paId"));
            List<ParkingAreaDto> parkingAreaDtos = parkingAreaInnerServiceSMOImpl.queryParkingAreas(parkingAreaDto);
            Assert.listOnlyOne(parkingAreaDtos, "查询停车场错误！");
            
            // 设置位置对象ID和名称
            communityLocationPo.setLocationObjId(reqJson.getString("paId"));
            communityLocationPo.setLocationName(parkingAreaDtos.get(0).getNum() + "停车场");
            communityLocationPo.setLocationObjName(communityLocationPo.getLocationName());
        }
        
        // 处理岗亭类型的位置更新（4000代表岗亭类型）
        if (!StringUtil.isEmpty(communityLocationPo.getLocationType()) && communityLocationPo.getLocationType().equals("4000")) { //4000 岗亭
            // 查询岗亭信息
            ParkingBoxDto parkingBoxDto = new ParkingBoxDto();
            parkingBoxDto.setBoxId(reqJson.getString("boxId"));
            List<ParkingBoxDto> parkingBoxDtos = parkingBoxV1InnerServiceSMOImpl.queryParkingBoxs(parkingBoxDto);
            
            // 设置位置对象ID和名称
            communityLocationPo.setLocationObjId(reqJson.getString("boxId"));
            communityLocationPo.setLocationName(parkingBoxDtos.get(0).getBoxName());
            communityLocationPo.setLocationObjName(communityLocationPo.getLocationName());
        }
        
        // 执行社区位置更新操作
        int flag = communityLocationV1InnerServiceSMOImpl.updateCommunityLocation(communityLocationPo);

        // 检查更新操作是否成功
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}