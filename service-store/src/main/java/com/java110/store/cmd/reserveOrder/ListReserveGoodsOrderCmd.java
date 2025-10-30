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
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.reserve.ReserveGoodsOrderTimeDto;
import com.java110.intf.store.IReserveGoodsOrderTimeV1InnerServiceSMO;
import com.java110.intf.store.IReserveGoodsOrderV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.reserve.ReserveGoodsOrderDto;

import java.util.List;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询
 * 服务编码：reserveGoodsOrder.listReserveGoodsOrder
 * 请求路劲：/app/reserveGoodsOrder.ListReserveGoodsOrder
 * add by 吴学文 at 2022-12-06 10:58:18 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 预约商品订单查询命令类
 * 负责处理预约商品订单列表查询请求，包括分页查询和订单时间信息的关联查询
 * 服务编码：reserveOrder.listReserveGoodsOrder
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-12-06
 */
@Java110Cmd(serviceCode = "reserveOrder.listReserveGoodsOrder")
public class ListReserveGoodsOrderCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListReserveGoodsOrderCmd.class);
    
    /**
     * 预约商品订单服务接口
     */
    @Autowired
    private IReserveGoodsOrderV1InnerServiceSMO reserveGoodsOrderV1InnerServiceSMOImpl;

    /**
     * 预约商品订单时间服务接口
     */
    @Autowired
    private IReserveGoodsOrderTimeV1InnerServiceSMO reserveGoodsOrderTimeV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 验证请求参数是否包含必要的信息，包括分页信息和小区ID
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页信息
        super.validatePageInfo(reqJson);
        // 验证小区ID是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
    }

    /**
     * 执行命令方法
     * 处理预约商品订单查询请求，包括分页查询和关联查询订单时间信息
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为预约商品订单DTO对象
        ReserveGoodsOrderDto reserveGoodsOrderDto = BeanConvertUtil.covertBean(reqJson, ReserveGoodsOrderDto.class);

        // 查询预约商品订单总数
        int count = reserveGoodsOrderV1InnerServiceSMOImpl.queryReserveGoodsOrdersCount(reserveGoodsOrderDto);

        List<ReserveGoodsOrderDto> reserveGoodsOrderDtos = null;

        if (count > 0) {
            // 查询预约商品订单列表
            reserveGoodsOrderDtos = reserveGoodsOrderV1InnerServiceSMOImpl.queryReserveGoodsOrders(reserveGoodsOrderDto);
            // 为每个订单关联查询时间信息
            freshTimes(reserveGoodsOrderDtos);
        } else {
            // 如果没有数据，返回空列表
            reserveGoodsOrderDtos = new ArrayList<>();
        }

        // 计算总页数并构建返回结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, reserveGoodsOrderDtos);

        // 设置响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 为预约商品订单列表刷入时间信息
     * 根据商品ID查询对应的预约时间信息，并设置到每个订单对象中
     *
     * @param reserveGoodsOrderDtos 预约商品订单列表
     */
    private void freshTimes(List<ReserveGoodsOrderDto> reserveGoodsOrderDtos) {
        // 检查订单列表是否为空
        if (reserveGoodsOrderDtos == null || reserveGoodsOrderDtos.size() < 1) {
            return;
        }
        
        // 收集所有商品ID
        List<String> goodsIds = new ArrayList<>();
        for (ReserveGoodsOrderDto reserveGoodsOrderDto : reserveGoodsOrderDtos) {
            goodsIds.add(reserveGoodsOrderDto.getGoodsId());
        }

        // 构建查询条件，查询所有相关商品的时间信息
        ReserveGoodsOrderTimeDto reserveGoodsOrderTimeDto = new ReserveGoodsOrderTimeDto();
        reserveGoodsOrderTimeDto.setGoodsIds(goodsIds.toArray(new String[goodsIds.size()]));
        List<ReserveGoodsOrderTimeDto> reserveGoodsOrderTimeDtos
                = reserveGoodsOrderTimeV1InnerServiceSMOImpl.queryReserveGoodsOrderTimes(reserveGoodsOrderTimeDto);
        
        List<ReserveGoodsOrderTimeDto> times = null;
        // 为每个订单设置对应的时间信息
        for (ReserveGoodsOrderDto reserveGoodsOrderDto : reserveGoodsOrderDtos) {
            times = new ArrayList<>();
            // 遍历时间信息，找到与当前订单商品ID匹配的时间记录
            for (ReserveGoodsOrderTimeDto tmpReserveGoodsOrderTimeDto : reserveGoodsOrderTimeDtos) {
                if (reserveGoodsOrderDto.getGoodsId().equals(tmpReserveGoodsOrderTimeDto.getGoodsId())) {
                    times.add(tmpReserveGoodsOrderTimeDto);
                }
            }
            // 将时间信息设置到订单对象中
            reserveGoodsOrderDto.setTimes(times);
        }
    }
}