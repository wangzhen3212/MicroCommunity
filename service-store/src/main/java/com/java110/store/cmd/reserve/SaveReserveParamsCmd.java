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
package com.java110.store.cmd.reserve;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.store.IReserveParamsOpenTimeV1InnerServiceSMO;
import com.java110.intf.store.IReserveParamsV1InnerServiceSMO;
import com.java110.po.reserve.ReserveParamsPo;
import com.java110.po.reserve.ReserveParamsOpenTimePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 类表述：保存预约参数配置信息
 * 服务编码：reserveParams.saveReserveParams
 * 请求路劲：/app/reserveParams.SaveReserveParams
 * add by 吴学文 at 2022-12-05 18:04:04 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "reserve.saveReserveParams")
public class SaveReserveParamsCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveReserveParamsCmd.class);

    /**
     * ID生成前缀，用于生成预约参数和开放时间的唯一标识
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 预约参数服务接口
     */
    @Autowired
    private IReserveParamsV1InnerServiceSMO reserveParamsV1InnerServiceSMOImpl;

    /**
     * 预约参数开放时间服务接口
     */
    @Autowired
    private IReserveParamsOpenTimeV1InnerServiceSMO reserveParamsOpenTimeV1InnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 检查请求报文中是否包含必要的参数
     *
     * @param event            命令事件
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson          请求JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.hasKeyAndValue(reqJson, "name", "请求报文中未包含name");
        Assert.hasKeyAndValue(reqJson, "paramWay", "请求报文中未包含paramWay");
        Assert.hasKeyAndValue(reqJson, "paramWayText", "请求报文中未包含paramWayText");
        Assert.hasKeyAndValue(reqJson, "startTime", "请求报文中未包含startTime");
        Assert.hasKeyAndValue(reqJson, "maxQuantity", "请求报文中未包含maxQuantity");
        Assert.hasKeyAndValue(reqJson, "hoursMaxQuantity", "请求报文中未包含hoursMaxQuantity");
    }

    /**
     * 执行保存预约参数命令
     * 保存预约参数配置信息，并为每个小时生成默认的开放时间配置
     *
     * @param event            命令事件
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson          请求JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 将请求JSON转换为预约参数PO对象
        ReserveParamsPo reserveParamsPo = BeanConvertUtil.covertBean(reqJson, ReserveParamsPo.class);
        // 生成预约参数ID
        reserveParamsPo.setParamsId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        // 保存预约参数到数据库
        int flag = reserveParamsV1InnerServiceSMOImpl.saveReserveParams(reserveParamsPo);

        // 检查保存是否成功
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 创建24小时的开放时间配置列表
        List<ReserveParamsOpenTimePo> reserveParamsOpenTimePos = new ArrayList<>();
        ReserveParamsOpenTimePo reserveParamsOpenTimePo = null;
        
        // 为每个小时生成默认的开放时间配置
        for (int hours = 0; hours < 24; hours++) {
            reserveParamsOpenTimePo = new ReserveParamsOpenTimePo();
            // 设置小区ID
            reserveParamsOpenTimePo.setCommunityId(reqJson.getString("communityId"));
            // 设置关联的预约参数ID
            reserveParamsOpenTimePo.setParamsId(reserveParamsPo.getParamsId());
            // 默认设置为不开放
            reserveParamsOpenTimePo.setIsOpen("N");
            // 生成时间配置ID
            reserveParamsOpenTimePo.setTimeId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
            // 设置小时数
            reserveParamsOpenTimePo.setHours(hours + "");
            // 添加到列表
            reserveParamsOpenTimePos.add(reserveParamsOpenTimePo);
        }
        
        // 批量保存开放时间配置到数据库
        flag = reserveParamsOpenTimeV1InnerServiceSMOImpl.saveReserveParamsOpenTimes(reserveParamsOpenTimePos);
        
        // 检查保存是否成功
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}