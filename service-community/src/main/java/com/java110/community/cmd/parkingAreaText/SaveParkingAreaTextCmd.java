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
package com.java110.community.cmd.parkingAreaText;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.parking.ParkingAreaTextDto;
import com.java110.intf.community.IParkingAreaTextV1InnerServiceSMO;
import com.java110.po.parking.ParkingAreaTextPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 类表述：保存
 * 服务编码：parkingAreaText.saveParkingAreaText
 * 请求路劲：/app/parkingAreaText.SaveParkingAreaText
 * add by 吴学文 at 2021-09-23 18:15:21 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 停车区域文本配置保存命令类
 * 负责处理停车区域文本配置的保存和更新操作，支持新增配置和更新现有配置
 * 通过服务编码 parkAreaText.saveParkingAreaText 对外提供服务
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2021-09-23
 */
@Java110Cmd(serviceCode = "parkingAreaText.saveParkingAreaText")
public class SaveParkingAreaTextCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveParkingAreaTextCmd.class);

    /**
     * ID生成前缀，用于生成文本配置ID
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 停车区域文本配置服务接口
     */
    @Autowired
    private IParkingAreaTextV1InnerServiceSMO parkingAreaTextV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整，确保必要的参数都存在
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证停车区域ID参数
        Assert.hasKeyAndValue(reqJson, "paId", "请求报文中未包含paId");
        // 验证小区ID参数
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        // 验证类型编码参数
        Assert.hasKeyAndValue(reqJson, "typeCd", "请求报文中未包含typeCd");
        // 验证文本1参数
        Assert.hasKeyAndValue(reqJson, "text1", "请求报文中未包含text1");
        // 验证文本2参数
        Assert.hasKeyAndValue(reqJson, "text2", "请求报文中未包含text2");
        // 验证文本3参数
        Assert.hasKeyAndValue(reqJson, "text3", "请求报文中未包含text3");
        // 验证文本4参数
        Assert.hasKeyAndValue(reqJson, "text4", "请求报文中未包含text4");
        // 验证语音参数
        Assert.hasKeyAndValue(reqJson, "voice", "请求报文中未包含voice");
    }

    /**
     * 执行保存停车区域文本配置命令
     * 根据停车区域ID、小区ID和类型编码判断是新增还是更新配置
     * 如果配置不存在则新增，存在则更新
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为停车区域文本配置PO对象
        ParkingAreaTextPo parkingAreaTextPo = BeanConvertUtil.covertBean(reqJson, ParkingAreaTextPo.class);
        
        // 构建查询条件DTO，用于检查是否已存在配置
        ParkingAreaTextDto parkingAreaTextDto = new ParkingAreaTextDto();
        parkingAreaTextDto.setPaId(reqJson.getString("paId"));
        parkingAreaTextDto.setCommunityId(reqJson.getString("communityId"));
        parkingAreaTextDto.setTypeCd(reqJson.getString("typeCd"));
        
        // 查询已存在的停车区域文本配置
        List<ParkingAreaTextDto> parkingAreaTextDtos = parkingAreaTextV1InnerServiceSMOImpl.queryParkingAreaTexts(parkingAreaTextDto);
        
        int flag = 0; // 操作结果标志
        
        // 根据查询结果决定是新增还是更新操作
        if (parkingAreaTextDtos == null || parkingAreaTextDtos.size() < 1) {
            // 没有找到现有配置，执行新增操作
            // 生成新的文本配置ID
            parkingAreaTextPo.setTextId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
            // 调用服务保存新的停车区域文本配置
            flag = parkingAreaTextV1InnerServiceSMOImpl.saveParkingAreaText(parkingAreaTextPo);
        } else {
            // 找到现有配置，执行更新操作
            // 使用现有配置的ID进行更新
            parkingAreaTextPo.setTextId(parkingAreaTextDtos.get(0).getTextId());
            // 调用服务更新停车区域文本配置
            flag = parkingAreaTextV1InnerServiceSMOImpl.updateParkingAreaText(parkingAreaTextPo);
        }
        
        // 检查操作是否成功
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}