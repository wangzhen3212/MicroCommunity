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
import com.java110.dto.community.CommunitySpacePersonDto;
import com.java110.dto.community.CommunitySpacePersonTimeDto;
import com.java110.dto.wechat.OnlinePayDto;
import com.java110.dto.reserve.ReserveGoodsOrderDto;
import com.java110.intf.acct.IOnlinePayV1InnerServiceSMO;
import com.java110.intf.store.IReserveGoodsOrderTimeV1InnerServiceSMO;
import com.java110.intf.store.IReserveGoodsOrderV1InnerServiceSMO;
import com.java110.po.wechat.OnlinePayPo;
import com.java110.po.reserve.ReserveGoodsOrderPo;
import com.java110.po.reserve.ReserveGoodsOrderTimePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 类表述：删除预约商品订单命令类
 * 服务编码：reserveOrder.deleteReserveGoodsPerson
 * 请求路径：/app/reserveOrder.deleteReserveGoodsPerson
 * 功能描述：该类负责处理预约商品订单的删除操作，包括订单状态更新、退款处理和相关时间记录的更新
 * 主要流程：验证请求参数 -> 查询有效订单 -> 处理退款 -> 更新订单状态 -> 更新预约时间状态
 * add by 吴学文 at 2022-09-30 11:36:52 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "reserveOrder.deleteReserveGoodsPerson")
public class DeleteReserveGoodsPersonCmd extends Cmd {
    private static Logger logger = LoggerFactory.getLogger(DeleteReserveGoodsPersonCmd.class);

    @Autowired
    private IReserveGoodsOrderV1InnerServiceSMO reserveGoodsOrderV1InnerServiceSMOImpl;

    @Autowired
    private IOnlinePayV1InnerServiceSMO onlinePayV1InnerServiceSMOImpl;

    @Autowired
    private IReserveGoodsOrderTimeV1InnerServiceSMO reserveGoodsOrderTimeV1InnerServiceSMOImpl;

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
        // 验证订单ID不能为空
        Assert.hasKeyAndValue(reqJson, "orderId", "orderId不能为空");
        // 验证小区ID不能为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行删除预约商品订单命令
     * 主要流程：查询有效订单 -> 处理退款 -> 更新订单状态 -> 更新预约时间状态
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 查询有效的预约商品订单
        ReserveGoodsOrderDto reserveGoodsOrderDto = new ReserveGoodsOrderDto();
        reserveGoodsOrderDto.setOrderId(reqJson.getString("orderId"));
        // 设置订单状态为有效状态
        reserveGoodsOrderDto.setState(CommunitySpacePersonDto.STATE_S);
        List<ReserveGoodsOrderDto> reserveGoodsOrderDtos = reserveGoodsOrderV1InnerServiceSMOImpl.queryReserveGoodsOrders(reserveGoodsOrderDto);

        // 验证只能找到一个有效订单
        Assert.listOnlyOne(reserveGoodsOrderDtos, "未找到可以取消订单");

        // 处理线上支付退款
        returnOnlinePayMoney(reserveGoodsOrderDtos);

        // 如果存在外部订单ID且不为-1，则查询关联的所有订单
        if(!StringUtil.isEmpty(reserveGoodsOrderDtos.get(0).getExtOrderId()) &&
                !"-1".equals(reserveGoodsOrderDtos.get(0).getExtOrderId())){
            reserveGoodsOrderDto = new ReserveGoodsOrderDto();
            reserveGoodsOrderDto.setExtOrderId(reserveGoodsOrderDtos.get(0).getExtOrderId());
            reserveGoodsOrderDto.setState(CommunitySpacePersonDto.STATE_S);
            reserveGoodsOrderDtos = reserveGoodsOrderV1InnerServiceSMOImpl.queryReserveGoodsOrders(reserveGoodsOrderDto);
        }

        // 遍历所有需要取消的订单，更新订单状态和预约时间状态
        for (ReserveGoodsOrderDto reserveGoodsOrderDto1 : reserveGoodsOrderDtos) {
            // 更新订单状态为取消状态
            ReserveGoodsOrderPo reserveGoodsOrderPo = new ReserveGoodsOrderPo();
            reserveGoodsOrderPo.setOrderId(reserveGoodsOrderDto1.getOrderId());
            reserveGoodsOrderPo.setState(CommunitySpacePersonDto.STATE_CL);
            int flag = reserveGoodsOrderV1InnerServiceSMOImpl.updateReserveGoodsOrder(reserveGoodsOrderPo);

            // 检查更新是否成功
            if (flag < 1) {
                throw new CmdException("删除数据失败");
            }

            // 更新预约时间状态为取消状态
            ReserveGoodsOrderTimePo reserveGoodsOrderTimePo = new ReserveGoodsOrderTimePo();
            reserveGoodsOrderTimePo.setOrderId(reserveGoodsOrderDto1.getOrderId());
            reserveGoodsOrderTimePo.setState(CommunitySpacePersonTimeDto.STATE_CL);
            reserveGoodsOrderTimeV1InnerServiceSMOImpl.updateReserveGoodsOrderTime(reserveGoodsOrderTimePo);
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }

    /**
     * 处理线上支付退款
     * 查询支付记录并更新退款状态
     * 
     * @param reserveGoodsOrderDtos 预约商品订单数据列表
     */
    private void returnOnlinePayMoney(List<ReserveGoodsOrderDto> reserveGoodsOrderDtos) {
        // 根据外部订单ID查询支付记录
        OnlinePayDto onlinePayDto = new OnlinePayDto();
        onlinePayDto.setOrderId(reserveGoodsOrderDtos.get(0).getExtOrderId());
        List<OnlinePayDto> onlinePayDtos = onlinePayV1InnerServiceSMOImpl.queryOnlinePays(onlinePayDto);
        
        // 如果没有支付记录，直接返回
        if (onlinePayDtos == null || onlinePayDtos.size() < 1) {
            return;
        }

        // 更新支付记录状态为等待退款
        OnlinePayPo onlinePayPo = new OnlinePayPo();
        onlinePayPo.setOrderId(onlinePayDtos.get(0).getOrderId());
        onlinePayPo.setPayId(onlinePayDtos.get(0).getPayId());
        onlinePayPo.setState(OnlinePayDto.STATE_WT);
        // 设置退款金额为原支付金额
        onlinePayPo.setRefundFee(onlinePayDtos.get(0).getTotalFee());
        onlinePayV1InnerServiceSMOImpl.updateOnlinePay(onlinePayPo);
    }
}