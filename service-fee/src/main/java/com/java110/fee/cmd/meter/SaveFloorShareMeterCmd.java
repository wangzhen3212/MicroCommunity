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
package com.java110.fee.cmd.meter;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.dto.floor.FloorDto;
import com.java110.dto.floorShareMeter.FloorShareMeterDto;
import com.java110.intf.community.IFloorInnerServiceSMO;
import com.java110.intf.fee.IFeeConfigInnerServiceSMO;
import com.java110.intf.fee.IFloorShareMeterV1InnerServiceSMO;
import com.java110.po.floorShareMeter.FloorShareMeterPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 楼栋公摊仪表保存命令类
 * 
 * 该类用于处理楼栋公摊仪表的保存操作，包括参数验证、数据转换和持久化存储。
 * 服务编码：floorShareMeter.saveFloorShareMeter
 * 请求路径：/app/floorShareMeter.SaveFloorShareMeter
 * 
 * @author 吴学文 at 2025-03-25 15:01:11 mail: 928255095@qq.com
 * @open source address: https://gitee.com/wuxw7/MicroCommunity
 * @官网：http://www.homecommunity.cn
 * @温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "meter.saveFloorShareMeter")
public class SaveFloorShareMeterCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveFloorShareMeterCmd.class);

    /**
     * ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 楼栋公摊仪表服务接口
     */
    @Autowired
    private IFloorShareMeterV1InnerServiceSMO floorShareMeterV1InnerServiceSMOImpl;

    /**
     * 楼栋服务接口
     */
    @Autowired
    private IFloorInnerServiceSMO floorInnerServiceSMOImpl;

    /**
     * 费用配置服务接口
     */
    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整和有效，确保必要的参数都存在
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填参数是否存在
        Assert.hasKeyAndValue(reqJson, "floorId", "请求报文中未包含floorId");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.hasKeyAndValue(reqJson, "meterType", "请求报文中未包含meterType");
        Assert.hasKeyAndValue(reqJson, "shareType", "请求报文中未包含shareType");
        Assert.hasKeyAndValue(reqJson, "configId", "请求报文中未包含费用项");
        Assert.hasKeyAndValue(reqJson, "sharePrice", "请求报文中未包含公摊单价");
        
        // 调用父类的属性验证方法
        super.validateProperty(cmdDataFlowContext);
    }

    /**
     * 执行保存楼栋公摊仪表命令
     * 
     * 处理楼栋公摊仪表的保存逻辑，包括数据验证、转换和持久化操作
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 验证费用配置信息是否存在
        FeeConfigDto feeConfigDto = new FeeConfigDto();
        feeConfigDto.setConfigId(reqJson.getString("configId"));
        List<FeeConfigDto> feeConfigDtos = feeConfigInnerServiceSMOImpl.queryFeeConfigs(feeConfigDto);
        Assert.listOnlyOne(feeConfigDtos, "未包含费用项");

        // 验证楼栋信息是否存在
        FloorDto floorDto = new FloorDto();
        floorDto.setFloorId(reqJson.getString("floorId"));
        floorDto.setCommunityId(reqJson.getString("communityId"));
        List<FloorDto> floorDtos = floorInnerServiceSMOImpl.queryFloors(floorDto);
        Assert.listOnlyOne(floorDtos, "楼栋不存在");

        // 将请求JSON数据转换为楼栋公摊仪表持久化对象
        FloorShareMeterPo floorShareMeterPo = BeanConvertUtil.covertBean(reqJson, FloorShareMeterPo.class);
        // 生成主键ID
        floorShareMeterPo.setFsmId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        // 设置初始读数为0
        floorShareMeterPo.setCurDegree("0");
        // 设置当前抄表时间为当前时间
        floorShareMeterPo.setCurReadingTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        // 设置楼栋编号
        floorShareMeterPo.setFloorNum(floorDtos.get(0).getFloorNum());
        // 设置费用项名称
        floorShareMeterPo.setConfigName(feeConfigDtos.get(0).getFeeName());
        
        // 调用服务保存楼栋公摊仪表数据
        int flag = floorShareMeterV1InnerServiceSMOImpl.saveFloorShareMeter(floorShareMeterPo);

        // 检查保存结果，如果失败则抛出异常
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}