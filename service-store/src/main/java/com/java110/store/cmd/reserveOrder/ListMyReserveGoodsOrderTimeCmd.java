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
import com.java110.dto.reserve.ReserveGoodsOrderDto;
import com.java110.dto.reserve.ReserveGoodsOrderTimeDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.store.IReserveGoodsOrderTimeV1InnerServiceSMO;
import com.java110.intf.store.IReserveGoodsOrderV1InnerServiceSMO;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;


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
 * 预约商品订单时间查询命令类
 * 
 * 该类用于处理查询用户预约商品订单时间的业务逻辑，主要功能包括：
 * 1. 验证用户身份和请求参数
 * 2. 查询用户的预约时间记录
 * 3. 返回分页查询结果
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-12-06
 */
@Java110Cmd(serviceCode = "reserveOrder.listMyReserveGoodsOrderTime")
public class ListMyReserveGoodsOrderTimeCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListMyReserveGoodsOrderTimeCmd.class);
    
    /**
     * 预约商品订单服务接口
     */
    @Autowired
    private IReserveGoodsOrderV1InnerServiceSMO reserveGoodsOrderV1InnerServiceSMOImpl;

    /**
     * 用户信息服务接口
     */
    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;

    /**
     * 预约商品订单时间服务接口
     */
    @Autowired
    private IReserveGoodsOrderTimeV1InnerServiceSMO reserveGoodsOrderTimeV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，主要验证分页参数
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息验证方法
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 
     * 处理查询用户预约商品订单时间的核心业务逻辑，包括：
     * 1. 获取当前用户信息
     * 2. 查询用户的预约时间记录
     * 3. 构建分页响应结果
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行过程中出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 从请求头中获取用户ID
        String userId = cmdDataFlowContext.getReqHeaders().get("user-id");

        // 构建用户查询条件
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        // 查询用户信息
        List<UserDto> userDtos = userInnerServiceSMOImpl.getUsers(userDto);

        // 验证用户存在且唯一
        Assert.listOnlyOne(userDtos,"用户不存在");

        // 将请求参数转换为预约时间查询对象
        ReserveGoodsOrderTimeDto reserveGoodsOrderTimeDto = BeanConvertUtil.covertBean(reqJson, ReserveGoodsOrderTimeDto.class);
        // 设置查询条件中的用户手机号
        reserveGoodsOrderTimeDto.setPersonTel(userDtos.get(0).getTel());

        // 查询符合条件的记录总数
        int count = reserveGoodsOrderTimeV1InnerServiceSMOImpl.queryReserveGoodsOrderTimesCount(reserveGoodsOrderTimeDto);

        List<ReserveGoodsOrderTimeDto> reserveGoodsOrderTimeDtos = null;

        if (count > 0) {
            // 如果有记录，则查询具体的数据
            reserveGoodsOrderTimeDtos = reserveGoodsOrderTimeV1InnerServiceSMOImpl.queryReserveGoodsOrderTimes(reserveGoodsOrderTimeDto);
        } else {
            // 如果没有记录，返回空列表
            reserveGoodsOrderTimeDtos = new ArrayList<>();
        }

        // 构建分页响应结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, reserveGoodsOrderTimeDtos);

        // 设置HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 刷新预约时间信息方法
     * 
     * 为预约商品订单列表中的每个订单填充对应的时间信息
     * 
     * @param reserveGoodsOrderDtos 预约商品订单数据列表
     */
    private void freshTimes(List<ReserveGoodsOrderDto> reserveGoodsOrderDtos) {
        // 如果订单列表为空，直接返回
        if (reserveGoodsOrderDtos == null || reserveGoodsOrderDtos.size() < 1) {
            return;
        }
        
        // 收集所有商品的ID
        List<String> goodsIds = new ArrayList<>();
        for (ReserveGoodsOrderDto reserveGoodsOrderDto : reserveGoodsOrderDtos) {
            goodsIds.add(reserveGoodsOrderDto.getGoodsId());
        }

        // 构建时间查询条件
        ReserveGoodsOrderTimeDto reserveGoodsOrderTimeDto = new ReserveGoodsOrderTimeDto();
        reserveGoodsOrderTimeDto.setGoodsIds(goodsIds.toArray(new String[goodsIds.size()]));
        // 批量查询所有相关商品的时间信息
        List<ReserveGoodsOrderTimeDto> reserveGoodsOrderTimeDtos
                = reserveGoodsOrderTimeV1InnerServiceSMOImpl.queryReserveGoodsOrderTimes(reserveGoodsOrderTimeDto);
        
        List<ReserveGoodsOrderTimeDto> times = null;
        // 为每个订单设置对应的时间信息
        for (ReserveGoodsOrderDto reserveGoodsOrderDto : reserveGoodsOrderDtos) {
            times = new ArrayList<>();
            // 筛选属于当前商品的时间信息
            for (ReserveGoodsOrderTimeDto tmpReserveGoodsOrderTimeDto : reserveGoodsOrderTimeDtos) {
                if (reserveGoodsOrderDto.getGoodsId().equals(tmpReserveGoodsOrderTimeDto.getGoodsId())) {
                    times.add(tmpReserveGoodsOrderTimeDto);
                }
            }
            // 设置时间信息到订单对象
            reserveGoodsOrderDto.setTimes(times);
        }
    }
}