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
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.acct.ICouponShopPoolV1InnerServiceSMO;
import com.java110.po.coupon.CouponShopPoolPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;

/**
 * 类表述：删除优惠券商铺池命令类
 * 服务编码：couponShopPool.deleteCouponShopPool
 * 请求路劲：/app/couponShopPool.DeleteCouponShopPool
 * add by 吴学文 at 2021-11-24 00:11:45 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * 类功能说明：处理删除优惠券商铺池的Cmd命令，负责验证请求参数并执行删除操作
 * 主要职责：
 * 1. 验证删除优惠券商铺池的必要参数
 * 2. 执行优惠券商铺池的删除操作
 * 3. 处理删除结果并返回响应
 */
@Java110Cmd(serviceCode = "couponShopPool.deleteCouponShopPool")
public class DeleteCouponShopPoolCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteCouponShopPoolCmd.class);

    /**
     * 优惠券商铺池服务接口
     */
    @Autowired
    private ICouponShopPoolV1InnerServiceSMO couponShopPoolV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否包含删除优惠券商铺池所需的必填字段
     *
     * @param event Cmd事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含spId字段
        Assert.hasKeyAndValue(reqJson, "spId", "spId不能为空");
        // 验证请求参数中必须包含shopId字段
        Assert.hasKeyAndValue(reqJson, "shopId", "shopId不能为空");
    }

    /**
     * 执行删除优惠券商铺池命令
     * 将请求参数转换为PO对象并调用服务层进行删除操作
     *
     * @param event Cmd事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为优惠券商铺池PO对象
        CouponShopPoolPo couponShopPoolPo = BeanConvertUtil.covertBean(reqJson, CouponShopPoolPo.class);
        
        // 调用服务层接口执行删除操作，返回影响的行数
        int flag = couponShopPoolV1InnerServiceSMOImpl.deleteCouponShopPool(couponShopPoolPo);

        // 检查删除操作是否成功，如果影响行数小于1则认为删除失败
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 设置成功的响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}