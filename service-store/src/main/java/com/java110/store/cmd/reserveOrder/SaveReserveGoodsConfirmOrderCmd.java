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

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.community.CommunitySpacePersonTimeDto;
import com.java110.dto.reserve.ReserveGoodsConfirmOrderDto;
import com.java110.dto.reserve.ReserveGoodsOrderDto;
import com.java110.dto.reserve.ReserveGoodsOrderTimeDto;
import com.java110.intf.store.IReserveGoodsConfirmOrderV1InnerServiceSMO;
import com.java110.intf.store.IReserveGoodsOrderTimeV1InnerServiceSMO;
import com.java110.intf.store.IReserveGoodsOrderV1InnerServiceSMO;
import com.java110.po.reserve.ReserveGoodsConfirmOrderPo;
import com.java110.po.reserve.ReserveGoodsOrderTimePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 类表述：保存预约商品确认订单命令类
 * 服务编码：reserveGoodsConfirmOrder.saveReserveGoodsConfirmOrder
 * 请求路劲：/app/reserveGoodsConfirmOrder.SaveReserveGoodsConfirmOrder
 * 功能描述：处理预约商品确认订单的保存操作，包括验证预约时间记录、更新预约时间状态和创建确认订单
 * add by 吴学文 at 2022-12-06 16:15:13 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "reserveOrder.saveReserveGoodsConfirmOrder")
public class SaveReserveGoodsConfirmOrderCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveReserveGoodsConfirmOrderCmd.class);

    /**
     * ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 预约商品确认订单服务接口
     */
    @Autowired
    private IReserveGoodsConfirmOrderV1InnerServiceSMO reserveGoodsConfirmOrderV1InnerServiceSMOImpl;

    /**
     * 预约商品订单时间服务接口
     */
    @Autowired
    private IReserveGoodsOrderTimeV1InnerServiceSMO reserveGoodsOrderTimeV1InnerServiceSMOImpl;

    /**
     * 预约商品订单服务接口
     */
    @Autowired
    private IReserveGoodsOrderV1InnerServiceSMO reserveGoodsOrderV1InnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求报文中必须包含timeId参数
        Assert.hasKeyAndValue(reqJson, "timeId", "请求报文中未包含timeId");
        // 验证请求报文中必须包含communityId参数
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
    }

    /**
     * 执行保存预约商品确认订单命令
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当业务处理失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 构建预约商品订单时间查询条件
        ReserveGoodsOrderTimeDto reserveGoodsOrderTimeDto = new ReserveGoodsOrderTimeDto();
        reserveGoodsOrderTimeDto.setTimeId(reqJson.getString("timeId"));
        reserveGoodsOrderTimeDto.setCommunityId(reqJson.getString("communityId"));
        // 设置查询状态为待确认状态
        reserveGoodsOrderTimeDto.setState(CommunitySpacePersonTimeDto.STATE_WAIT_CONFIRM);
        // 查询预约时间记录
        List<ReserveGoodsOrderTimeDto> reserveGoodsOrderTimeDtos = reserveGoodsOrderTimeV1InnerServiceSMOImpl.queryReserveGoodsOrderTimes(reserveGoodsOrderTimeDto);

        // 验证查询结果必须且只能有一条记录
        Assert.listOnlyOne(reserveGoodsOrderTimeDtos, "未包含预约记录");

        // 根据预约时间记录中的订单ID查询预约订单信息
        ReserveGoodsOrderDto reserveGoodsOrderDto = new ReserveGoodsOrderDto();
        reserveGoodsOrderDto.setOrderId(reserveGoodsOrderTimeDtos.get(0).getOrderId());
        List<ReserveGoodsOrderDto> reserveGoodsOrderDtos = reserveGoodsOrderV1InnerServiceSMOImpl.queryReserveGoodsOrders(reserveGoodsOrderDto);
        // 验证预约订单必须存在且唯一
        Assert.listOnlyOne(reserveGoodsOrderDtos, "预约订单不存在");

        // 将预约时间状态修改为已完成状态（核销中）
        ReserveGoodsOrderTimePo reserveGoodsOrderTimePo = new ReserveGoodsOrderTimePo();
        reserveGoodsOrderTimePo.setTimeId(reserveGoodsOrderTimeDtos.get(0).getTimeId());
        reserveGoodsOrderTimePo.setState(CommunitySpacePersonTimeDto.STATE_FINISH);
        int flag = reserveGoodsOrderTimeV1InnerServiceSMOImpl.updateReserveGoodsOrderTime(reserveGoodsOrderTimePo);
        // 验证更新操作是否成功
        if (flag < 1) {
            throw new CmdException("核销预约失败");
        }

        // 将请求参数转换为确认订单PO对象
        ReserveGoodsConfirmOrderPo reserveGoodsConfirmOrderPo = BeanConvertUtil.covertBean(reqJson, ReserveGoodsConfirmOrderPo.class);
        // 生成确认订单ID
        reserveGoodsConfirmOrderPo.setCoId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        // 设置订单ID
        reserveGoodsConfirmOrderPo.setOrderId(reserveGoodsOrderTimeDtos.get(0).getOrderId());
        // 设置订单类型
        reserveGoodsConfirmOrderPo.setType(reserveGoodsOrderDtos.get(0).getType());
        // 设置商品ID
        reserveGoodsConfirmOrderPo.setGoodsId(reserveGoodsOrderDtos.get(0).getGoodsId());
        // 保存确认订单信息
        flag = reserveGoodsConfirmOrderV1InnerServiceSMOImpl.saveReserveGoodsConfirmOrder(reserveGoodsConfirmOrderPo);

        // 验证保存操作是否成功
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 查询刚刚保存的确认订单信息
        ReserveGoodsConfirmOrderDto reserveGoodsConfirmOrderDto = new ReserveGoodsConfirmOrderDto();
        reserveGoodsConfirmOrderDto.setCoId(reserveGoodsConfirmOrderPo.getCoId());
        List<ReserveGoodsConfirmOrderDto> reserveGoodsConfirmOrderDtos = reserveGoodsConfirmOrderV1InnerServiceSMOImpl.queryReserveGoodsConfirmOrders(reserveGoodsConfirmOrderDto);

        // 设置响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.createResponseEntity(reserveGoodsConfirmOrderDtos));

    }
}