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
package com.java110.acct.cmd.couponShopPool;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.acct.ICouponShopPoolV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.coupon.CouponShopPoolDto;

import java.util.List;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;


/**
 * 类表述：查询
 * 服务编码：couponShopPool.listCouponShopPool
 * 请求路劲：/app/couponShopPool.ListCouponShopPool
 * add by 吴学文 at 2021-11-24 00:11:45 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 优惠券商铺池查询命令类
 * 
 * 该类负责处理优惠券商铺池列表查询请求，实现分页查询优惠券商铺池信息的功能
 * 通过调用优惠券商铺池服务接口获取数据，并返回标准格式的响应结果
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2021-11-24
 */
@Java110Cmd(serviceCode = "couponShopPool.listCouponShopPool")
public class ListCouponShopPoolCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListCouponShopPoolCmd.class);
    
    /**
     * 优惠券商铺池服务接口
     * 用于调用优惠券商铺池相关的数据操作
     */
    @Autowired
    private ICouponShopPoolV1InnerServiceSMO couponShopPoolV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，主要验证分页参数
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于处理请求和响应
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息验证方法，确保分页参数正确
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 
     * 执行优惠券商铺池查询业务逻辑，包括数据查询、结果封装和响应设置
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 将请求JSON对象转换为优惠券商铺池数据传输对象
        CouponShopPoolDto couponShopPoolDto = BeanConvertUtil.covertBean(reqJson, CouponShopPoolDto.class);

        // 查询优惠券商铺池总记录数
        int count = couponShopPoolV1InnerServiceSMOImpl.queryCouponShopPoolsCount(couponShopPoolDto);

        List<CouponShopPoolDto> couponShopPoolDtos = null;

        // 根据查询结果数量决定返回数据
        if (count > 0) {
            // 如果存在记录，查询具体的优惠券商铺池列表数据
            couponShopPoolDtos = couponShopPoolV1InnerServiceSMOImpl.queryCouponShopPools(couponShopPoolDto);
        } else {
            // 如果没有记录，返回空列表
            couponShopPoolDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 构建返回结果对象，包含总页数、总记录数和数据列表
        ResultVo resultVo = new ResultVo(totalPage, count, couponShopPoolDtos);

        // 创建HTTP响应实体，设置响应状态为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到数据流上下文中
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}