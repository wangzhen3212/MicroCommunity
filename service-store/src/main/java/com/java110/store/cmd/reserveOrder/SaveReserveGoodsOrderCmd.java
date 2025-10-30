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
package com.java110.store.cmd.reserveOrder;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.reserve.ReserveGoodsDto;
import com.java110.dto.reserve.ReserveGoodsOrderDto;
import com.java110.dto.reserve.ReserveGoodsOrderTimeDto;
import com.java110.dto.reserve.ReserveParamsDto;
import com.java110.intf.store.IReserveGoodsOrderTimeV1InnerServiceSMO;
import com.java110.intf.store.IReserveGoodsOrderV1InnerServiceSMO;
import com.java110.intf.store.IReserveGoodsV1InnerServiceSMO;
import com.java110.po.reserve.ReserveGoodsOrderPo;
import com.java110.po.reserve.ReserveGoodsOrderTimePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * 预约商品订单保存命令类
 * 
 * 该类负责处理预约商品订单的保存业务逻辑，包括参数验证、预约时间校验、订单信息保存等功能。
 * 服务编码：reserveGoodsOrder.saveReserveGoodsOrder
 * 请求路径：/app/reserveGoodsOrder.SaveReserveGoodsOrder
 * 
 * @author 吴学文 at 2022-12-06 10:58:18 mail: 928255095@qq.com
 * @open source address: https://gitee.com/wuxw7/MicroCommunity
 * @官网：http://www.homecommunity.cn
 * @温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "reserveOrder.saveReserveGoodsOrder")
public class SaveReserveGoodsOrderCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveReserveGoodsOrderCmd.class);

    /** 订单ID前缀 */
    public static final String CODE_PREFIX_ID = "10";

    @Autowired
    private IReserveGoodsOrderV1InnerServiceSMO reserveGoodsOrderV1InnerServiceSMOImpl;

    @Autowired
    private IReserveGoodsOrderTimeV1InnerServiceSMO reserveGoodsOrderTimeV1InnerServiceSMOImpl;

    @Autowired
    private IReserveGoodsV1InnerServiceSMO reserveGoodsV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，包括必填字段检查、商品存在性验证、预约时间校验等
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填字段
        Assert.hasKeyAndValue(reqJson, "goodsId", "请求报文中未包含goodsId");
        Assert.hasKeyAndValue(reqJson, "type", "请求报文中未包含type");
        Assert.hasKeyAndValue(reqJson, "personName", "请求报文中未包含personName");
        Assert.hasKeyAndValue(reqJson, "personTel", "请求报文中未包含personTel");
        Assert.hasKeyAndValue(reqJson, "appointmentTime", "请求报文中未包含appointmentTime");
        Assert.hasKeyAndValue(reqJson, "receivedAmount", "请求报文中未包含receivedAmount");
        Assert.hasKeyAndValue(reqJson, "payWay", "请求报文中未包含payWay");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");

        // 校验商品是否存在
        ReserveGoodsDto reserveGoodsDto = new ReserveGoodsDto();
        reserveGoodsDto.setGoodsId(reqJson.getString("goodsId"));
        List<ReserveGoodsDto> reserveGoodsDtos = reserveGoodsV1InnerServiceSMOImpl.queryReserveGoodss(reserveGoodsDto);
        
        // 根据类型验证商品存在性
        if ("1001".equals(reqJson.getString("type"))) {
            Assert.listOnlyOne(reserveGoodsDtos, "就餐不存在");
        } else {
            Assert.listOnlyOne(reserveGoodsDtos, "服务不存在");
        }

        // 校验预约时间是否有效
        checkAppointmentTime(reqJson, reserveGoodsDtos.get(0));

        // 检查是否包含时间段信息
        if (!reqJson.containsKey("times")) {
            cmdDataFlowContext.setResponseEntity(ResultVo.success());
            return;
        }

        JSONArray openTimes = reqJson.getJSONArray("times");

        // 检查时间段数组是否为空
        if (openTimes == null || openTimes.size() < 1) {
            cmdDataFlowContext.setResponseEntity(ResultVo.success());
            return;
        }
        
        // 验证每个时间段是否可预约
        ReserveGoodsOrderTimeDto reserveGoodsOrderTimeDto = null;
        int flag = 0;
        int quantity = 0;
        for (int timeIndex = 0; timeIndex < openTimes.size(); timeIndex++) {
            reserveGoodsOrderTimeDto = new ReserveGoodsOrderTimeDto();
            reserveGoodsOrderTimeDto.setCommunityId(reqJson.getString("communityId"));
            reserveGoodsOrderTimeDto.setAppointmentTime(reqJson.getString("appointmentTime"));
            reserveGoodsOrderTimeDto.setHours(openTimes.getJSONObject(timeIndex).getString("hours"));
            reserveGoodsOrderTimeDto.setGoodsId(reqJson.getString("goodsId"));
            
            // 检查该时间段是否已被预约
            flag = reserveGoodsOrderTimeV1InnerServiceSMOImpl.queryReserveGoodsOrderTimesCount(reserveGoodsOrderTimeDto);
            if (flag > 0) {
                throw new CmdException(reqJson.getString("appointmentTime") + "," + openTimes.getJSONObject(timeIndex).getString("hours") + "已经被预约");
            }

            // 检查预约数量是否超过限制
            quantity = Integer.parseInt(openTimes.getJSONObject(timeIndex).getString("quantity"));
            if (quantity > Integer.parseInt(reserveGoodsDtos.get(0).getHoursMaxQuantity())) {
                throw new CmdException("预约数量超过设定数量");
            }
        }
    }

    /**
     * 检查预约时间是否有效
     * 
     * 根据商品的预约方式（按天或按周）验证指定的预约时间是否在允许的预约范围内
     * 
     * @param reqJson 请求参数JSON对象
     * @param reserveGoodsDto 预约商品信息DTO
     * @throws CmdException 当预约时间无效时抛出异常
     */
    private void checkAppointmentTime(JSONObject reqJson, ReserveGoodsDto reserveGoodsDto) {
        Calendar calendar = Calendar.getInstance();
        // 将字符串格式的预约时间转换为Date对象
        calendar.setTime(DateUtil.getDateFromStringB(reqJson.getString("appointmentTime")));
        int day;
        // 获取商品允许预约的时间配置
        String[] days = reserveGoodsDto.getParamWayText().split(",");

        // 根据预约方式验证时间
        if (ReserveParamsDto.PARAM_WAY_DAY.equals(reserveGoodsDto.getParamWay())) {
            // 按天预约：检查指定日期是否在允许的日期列表中
            day = calendar.get(Calendar.DAY_OF_MONTH);
            if (!Arrays.asList(days).contains(day + "")) {
                throw new CmdException(reqJson.getString("appointmentTime") + "不能预约");
            }
        } else {
            // 按周预约：检查指定星期几是否在允许的星期列表中
            day = calendar.get(Calendar.DAY_OF_WEEK);
            boolean isFirstSunday = (calendar.getFirstDayOfWeek() == Calendar.SUNDAY);
            // 获取周几
            // 若一周第一天为星期天，则-1
            if (isFirstSunday) {
                day = day - 1;
                if (day == 0) {
                    day = 7;
                }
            }
            if (!Arrays.asList(days).contains(day + "")) {
                throw new CmdException(reqJson.getString("appointmentTime") + "不能预约");
            }
        }
    }

    /**
     * 执行保存预约订单命令
     * 
     * 保存预约订单主信息和时间段信息，使用事务保证数据一致性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当保存失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 转换请求参数为订单PO对象
        ReserveGoodsOrderPo reserveGoodsOrderPo = BeanConvertUtil.covertBean(reqJson, ReserveGoodsOrderPo.class);
        // 生成订单ID
        reserveGoodsOrderPo.setOrderId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        // 设置外部订单ID，默认为-1
        if (StringUtil.isEmpty(reserveGoodsOrderPo.getExtOrderId())) {
            reserveGoodsOrderPo.setExtOrderId("-1");
        }

        // 设置订单状态为待确认
        reserveGoodsOrderPo.setState(ReserveGoodsOrderDto.STATE_W);

        // 保存订单主信息
        int flag = reserveGoodsOrderV1InnerServiceSMOImpl.saveReserveGoodsOrder(reserveGoodsOrderPo);

        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 保存订单时间段信息
        JSONArray openTimes = reqJson.getJSONArray("times");

        ReserveGoodsOrderTimePo reserveGoodsOrderTimePo = null;
        for (int timeIndex = 0; timeIndex < openTimes.size(); timeIndex++) {
            reserveGoodsOrderTimePo = new ReserveGoodsOrderTimePo();
            reserveGoodsOrderTimePo.setCommunityId(reserveGoodsOrderPo.getCommunityId());
            reserveGoodsOrderTimePo.setGoodsId(reserveGoodsOrderPo.getGoodsId());
            reserveGoodsOrderTimePo.setOrderId(reserveGoodsOrderPo.getOrderId());
            reserveGoodsOrderTimePo.setHours(openTimes.getJSONObject(timeIndex).getString("hours"));
            reserveGoodsOrderTimePo.setQuantity(openTimes.getJSONObject(timeIndex).getString("quantity"));
            reserveGoodsOrderTimePo.setTimeId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
            // 设置时间段状态为待确认
            reserveGoodsOrderTimePo.setState(ReserveGoodsOrderTimeDto.STATE_WAIT_CONFIRM);
            reserveGoodsOrderTimeV1InnerServiceSMOImpl.saveReserveGoodsOrderTime(reserveGoodsOrderTimePo);
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}