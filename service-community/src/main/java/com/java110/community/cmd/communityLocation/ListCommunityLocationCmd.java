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
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.floor.FloorDto;
import com.java110.dto.unit.UnitDto;
import com.java110.dto.community.CommunityLocationDto;
import com.java110.intf.community.*;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：查询社区位置信息列表
 * 服务编码：communityLocation.listCommunityLocation
 * 请求路劲：/app/communityLocation.ListCommunityLocation
 * add by 吴学文 at 2022-07-17 00:10:38 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "communityLocation.listCommunityLocations")
public class ListCommunityLocationCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListCommunityLocationCmd.class);

    @Autowired
    private ICommunityLocationInnerServiceSMO communityLocationInnerServiceSMOImpl;

    @Autowired
    private IUnitInnerServiceSMO unitInnerServiceSMOImpl;

    @Autowired
    private IFloorInnerServiceSMO floorInnerServiceSMOImpl;

    /**
     * 参数校验方法
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息校验
        super.validatePageInfo(reqJson);
        // 校验请求参数中必须包含小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含小区信息");
    }

    /**
     * 执行命令方法 - 查询社区位置信息列表
     * @param event 命令事件对象
     * @param context 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        // 将请求参数转换为CommunityLocationDto对象
        CommunityLocationDto communityLocationDto = BeanConvertUtil.covertBean(reqJson, CommunityLocationDto.class);

        // 查询社区位置信息总数
        int count = communityLocationInnerServiceSMOImpl.queryCommunityLocationsCount(communityLocationDto);

        List<CommunityLocationDto> communityLocationDtos = new ArrayList<>();

        // 如果有数据则进行详细查询和处理
        if (count > 0) {
            // 查询社区位置信息列表
            List<CommunityLocationDto> communityLocations = communityLocationInnerServiceSMOImpl.queryCommunityLocations(communityLocationDto);
            
            // 遍历每个位置信息，根据位置类型进行不同的处理
            for (CommunityLocationDto communityLocation : communityLocations) {
                // 处理单元类型的位置信息（2000表示单元）
                if (!StringUtil.isEmpty(communityLocation.getLocationType()) && communityLocation.getLocationType().equals("2000")
                        && !StringUtil.isEmpty(communityLocation.getLocationObjId()) && !communityLocation.getLocationObjId().equals("-1")) {
                    // 根据单元ID查询单元信息
                    UnitDto unitDto = new UnitDto();
                    unitDto.setUnitId(communityLocation.getLocationObjId());
                    List<UnitDto> unitDtos = unitInnerServiceSMOImpl.queryUnits(unitDto);
                    
                    // 如果查询到单元信息，则设置楼层ID和单元ID
                    if(!ListUtil.isNull(unitDtos)){
                        communityLocation.setFloorId(unitDtos.get(0).getFloorId());
                        communityLocation.setUnitId(unitDtos.get(0).getUnitId());
                        
                        // 根据楼层ID查询楼层信息
                        FloorDto floorDto = new FloorDto();
                        floorDto.setFloorId(unitDtos.get(0).getFloorId());
                        List<FloorDto> floorDtos = floorInnerServiceSMOImpl.queryFloors(floorDto);
                        
                        // 如果查询到楼层信息，则设置楼层相关信息
                        if(!ListUtil.isNull(floorDtos)){
                            // 可以在这里设置楼层相关信息
                        }
                    }
                } 
                // 处理楼栋类型的位置信息（6000表示楼栋）
                else if (!StringUtil.isEmpty(communityLocation.getLocationType()) && communityLocation.getLocationType().equals("6000")
                        && !StringUtil.isEmpty(communityLocation.getLocationObjId()) && !communityLocation.getLocationObjId().equals("-1")) {
                    // 设置楼层ID为位置对象ID
                    communityLocation.setFloorId(communityLocation.getLocationObjId());
                } 
                // 处理停车场类型的位置信息（7000表示停车场）
                else if (!StringUtil.isEmpty(communityLocation.getLocationType()) && communityLocation.getLocationType().equals("7000")
                        && !StringUtil.isEmpty(communityLocation.getLocationObjId()) && !communityLocation.getLocationObjId().equals("-1")) {
                    // 设置停车场ID为位置对象ID
                    communityLocation.setPaId(communityLocation.getLocationObjId());
                } 
                // 处理岗亭类型的位置信息（4000表示岗亭）
                else if (!StringUtil.isEmpty(communityLocation.getLocationType()) && communityLocation.getLocationType().equals("4000")
                        && !StringUtil.isEmpty(communityLocation.getLocationObjId()) && !communityLocation.getLocationObjId().equals("-1")) {
                    // 设置岗亭ID为位置对象ID
                    communityLocation.setBoxId(communityLocation.getLocationObjId());
                }
                
                // 将处理后的位置信息添加到结果列表
                communityLocationDtos.add(communityLocation);
            }
        } else {
            // 如果没有数据，返回空列表
            communityLocationDtos = new ArrayList<>();
        }

        // 构建分页结果对象
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, communityLocationDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应实体到上下文
        context.setResponseEntity(responseEntity);
    }
}