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
package com.java110.community.cmd.parkingBox;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.parking.ParkingAreaDto;
import com.java110.dto.parking.ParkingBoxAreaDto;
import com.java110.intf.community.IParkingAreaInnerServiceSMO;
import com.java110.intf.community.IParkingBoxAreaV1InnerServiceSMO;
import com.java110.intf.community.IParkingBoxV1InnerServiceSMO;
import com.java110.po.parking.ParkingBoxPo;
import com.java110.po.parking.ParkingBoxAreaPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 停车场岗亭更新命令类
 * 
 * 该类负责处理停车场岗亭信息的更新操作，包括岗亭基本信息更新和岗亭与停车区域的关联关系维护
 * 服务编码：parkingBox.updateParkingBox
 * 请求路径：/app/parkingBox.UpdateParkingBox
 * 
 * @author 吴学文 at 2021-10-18 00:10:40 mail: 928255095@qq.com
 * @open source address: https://gitee.com/wuxw7/MicroCommunity
 * @官网：http://www.homecommunity.cn
 * @温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "parkingBox.updateParkingBox")
public class UpdateParkingBoxCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdateParkingBoxCmd.class);

    /**
     * ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 停车场岗亭服务接口
     */
    @Autowired
    private IParkingBoxV1InnerServiceSMO parkingBoxV1InnerServiceSMOImpl;

    /**
     * 停车场区域服务接口
     */
    @Autowired
    private IParkingAreaInnerServiceSMO parkingAreaInnerServiceSMOImpl;

    /**
     * 岗亭区域关联服务接口
     */
    @Autowired
    private IParkingBoxAreaV1InnerServiceSMO parkingBoxAreaV1InnerServiceSMOImpl;

    /**
     * 参数校验方法
     * 
     * 验证请求参数中是否包含必要的字段
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含boxId字段
        Assert.hasKeyAndValue(reqJson, "boxId", "boxId不能为空");
        // 验证请求参数中必须包含communityId字段
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行岗亭更新命令
     * 
     * 该方法负责更新停车场岗亭信息，并维护岗亭与停车区域的关联关系
     * 如果请求中包含停车区域信息，还会更新岗亭与区域的关联关系
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为岗亭PO对象
        ParkingBoxPo parkingBoxPo = BeanConvertUtil.covertBean(reqJson, ParkingBoxPo.class);
        // 调用服务更新岗亭基本信息
        int flag = parkingBoxV1InnerServiceSMOImpl.updateParkingBox(parkingBoxPo);
        // 检查更新是否成功
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }
        
        // 如果请求中不包含停车区域ID或停车区域ID为空，则直接返回成功
        if (!reqJson.containsKey("paId") || StringUtil.isEmpty(reqJson.getString("paId"))) {
            cmdDataFlowContext.setResponseEntity(ResultVo.success());
            return;
        }
        
        // 验证停车场区域是否存在
        ParkingAreaDto parkingAreaDto = new ParkingAreaDto();
        parkingAreaDto.setPaId(reqJson.getString("paId"));
        parkingAreaDto.setCommunityId(reqJson.getString("communityId"));
        List<ParkingAreaDto> parkingAreaDtos = parkingAreaInnerServiceSMOImpl.queryParkingAreas(parkingAreaDto);
        // 确保查询结果只有一个停车场区域
        Assert.listOnlyOne(parkingAreaDtos, "停车场不存在");
        
        // 查询岗亭与停车区域的关联关系
        ParkingBoxAreaDto parkingBoxAreaDto = new ParkingBoxAreaDto();
        parkingBoxAreaDto.setBoxId(reqJson.getString("boxId"));
        parkingBoxAreaDto.setPaId(parkingAreaDtos.get(0).getPaId());
        parkingBoxAreaDto.setCommunityId(parkingAreaDtos.get(0).getCommunityId());
        List<ParkingBoxAreaDto> parkingBoxAreaDtos = parkingBoxAreaV1InnerServiceSMOImpl.queryParkingBoxAreas(parkingBoxAreaDto);
        
        // 准备岗亭区域关联PO对象
        ParkingBoxAreaPo parkingBoxAreaPo = new ParkingBoxAreaPo();
        parkingBoxAreaPo.setBoxId(parkingBoxPo.getBoxId());
        parkingBoxAreaPo.setPaId(parkingAreaDtos.get(0).getPaId());
        parkingBoxAreaPo.setCommunityId(parkingAreaDtos.get(0).getCommunityId());
        
        // 根据关联关系是否存在，决定是新增还是更新
        if (parkingBoxAreaDtos == null || parkingBoxAreaDtos.size() < 1) {
            // 生成新的关联关系ID
            parkingBoxAreaPo.setBaId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
            // 设置为默认区域
            parkingBoxAreaPo.setDefaultArea(ParkingBoxAreaDto.DEFAULT_AREA_TRUE);
            // 保存新的岗亭区域关联关系
            flag = parkingBoxAreaV1InnerServiceSMOImpl.saveParkingBoxArea(parkingBoxAreaPo);
        } else {
            // 使用现有的关联关系ID
            parkingBoxAreaPo.setBaId(parkingBoxAreaDtos.get(0).getBaId());
            // 更新现有的岗亭区域关联关系
            flag = parkingBoxAreaV1InnerServiceSMOImpl.updateParkingBoxArea(parkingBoxAreaPo);
        }
        
        // 检查关联关系操作是否成功
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }
        
        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}