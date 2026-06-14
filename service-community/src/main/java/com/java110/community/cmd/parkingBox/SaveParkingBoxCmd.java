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
 * 类表述：保存停车场岗亭信息的命令类
 * 服务编码：parkingBox.saveParkingBox
 * 请求路劲：/app/parkingBox.SaveParkingBox
 * 功能描述：该类负责处理停车场岗亭的保存请求，包括岗亭基本信息保存和岗亭与停车场的关联关系建立
 * add by 吴学文 at 2021-10-18 00:10:40 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "parkingBox.saveParkingBox")
public class SaveParkingBoxCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveParkingBoxCmd.class);

    /**
     * ID生成的前缀
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
     * 停车场岗亭区域关联服务接口
     */
    @Autowired
    private IParkingBoxAreaV1InnerServiceSMO parkingBoxAreaV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 功能描述：验证请求参数是否完整，确保必要的参数都存在
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填参数是否存在
        Assert.hasKeyAndValue(reqJson, "boxName", "请求报文中未包含boxName");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.hasKeyAndValue(reqJson, "tempCarIn", "请求报文中未包含tempCarIn");
        Assert.hasKeyAndValue(reqJson, "fee", "请求报文中未包含fee");
        Assert.hasKeyAndValue(reqJson, "blueCarIn", "请求报文中未包含blueCarIn");
        Assert.hasKeyAndValue(reqJson, "yelowCarIn", "请求报文中未包含yelowCarIn");
    }

    /**
     * 执行命令方法
     * 功能描述：保存停车场岗亭信息，包括岗亭基本信息和岗亭与停车场的关联关系
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为停车场岗亭PO对象
        ParkingBoxPo parkingBoxPo = BeanConvertUtil.covertBean(reqJson, ParkingBoxPo.class);
        // 生成岗亭ID
        parkingBoxPo.setBoxId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        // 保存岗亭基本信息
        int flag = parkingBoxV1InnerServiceSMOImpl.saveParkingBox(parkingBoxPo);
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }
        
        // 如果请求中不包含停车场ID，则只保存岗亭基本信息后返回
        if (!reqJson.containsKey("paId") || StringUtil.isEmpty(reqJson.getString("paId"))) {
            cmdDataFlowContext.setResponseEntity(ResultVo.success());
            return;
        }
        
        // 验证停车场是否存在
        ParkingAreaDto parkingAreaDto = new ParkingAreaDto();
        parkingAreaDto.setPaId(reqJson.getString("paId"));
        parkingAreaDto.setCommunityId(reqJson.getString("communityId"));
        List<ParkingAreaDto> parkingAreaDtos = parkingAreaInnerServiceSMOImpl.queryParkingAreas(parkingAreaDto);
        // 确保停车场存在且唯一
        Assert.listOnlyOne(parkingAreaDtos, "停车场不存在");
        
        // 创建岗亭与停车场的关联关系
        ParkingBoxAreaPo parkingBoxAreaPo = new ParkingBoxAreaPo();
        // 生成关联关系ID
        parkingBoxAreaPo.setBaId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        // 设置岗亭ID
        parkingBoxAreaPo.setBoxId(parkingBoxPo.getBoxId());
        // 设置停车场ID
        parkingBoxAreaPo.setPaId(parkingAreaDtos.get(0).getPaId());
        // 设置小区ID
        parkingBoxAreaPo.setCommunityId(parkingAreaDtos.get(0).getCommunityId());
        // 设置为默认区域
        parkingBoxAreaPo.setDefaultArea(ParkingBoxAreaDto.DEFAULT_AREA_TRUE);
        // 保存岗亭与停车场的关联关系
        flag = parkingBoxAreaV1InnerServiceSMOImpl.saveParkingBoxArea(parkingBoxAreaPo);
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }
        
        // 返回成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}