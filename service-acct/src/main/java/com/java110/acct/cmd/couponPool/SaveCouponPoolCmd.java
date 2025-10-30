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
package com.java110.acct.cmd.couponPool;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.acct.ICouponPoolV1InnerServiceSMO;
import com.java110.po.coupon.CouponPoolPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;

/**
 * 类表述：保存优惠券池信息
 * 服务编码：couponPool.saveCouponPool
 * 请求路劲：/app/couponPool.SaveCouponPool
 * add by 吴学文 at 2021-11-23 23:48:04 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * 优惠券池保存命令类，负责处理优惠券池的新增操作
 * 该类继承自Cmd基类，实现了优惠券池数据的验证和保存功能
 */
@Java110Cmd(serviceCode = "couponPool.saveCouponPool")
public class SaveCouponPoolCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveCouponPoolCmd.class);

    /**
     * ID生成前缀，用于生成优惠券池ID
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 优惠券池服务接口，用于操作优惠券池数据
     */
    @Autowired
    private ICouponPoolV1InnerServiceSMO couponPoolV1InnerServiceSMOImpl;

    /**
     * 验证请求参数方法
     * 检查请求报文中是否包含必要的参数
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证优惠券类型参数
        Assert.hasKeyAndValue(reqJson, "couponType", "请求报文中未包含couponType");
        // 验证优惠券名称参数
        Assert.hasKeyAndValue(reqJson, "couponName", "请求报文中未包含couponName");
        // 验证实际价格参数
        Assert.hasKeyAndValue(reqJson, "actualPrice", "请求报文中未包含actualPrice");
        // 验证购买价格参数
        Assert.hasKeyAndValue(reqJson, "buyPrice", "请求报文中未包含buyPrice");
        // 验证优惠券库存参数
        Assert.hasKeyAndValue(reqJson, "couponStock", "请求报文中未包含couponStock");
        // 验证有效期天数参数
        Assert.hasKeyAndValue(reqJson, "validityDay", "请求报文中未包含validityDay");
        // 验证排序序号参数
        Assert.hasKeyAndValue(reqJson, "seq", "请求报文中未包含seq");
    }

    /**
     * 执行保存优惠券池命令
     * 将请求参数转换为优惠券池对象并保存到数据库
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为优惠券池POJO对象
        CouponPoolPo couponPoolPo = BeanConvertUtil.covertBean(reqJson, CouponPoolPo.class);
        // 设置优惠券池状态为运行中
        couponPoolPo.setState(CouponPoolPo.STATE_RUN);
        // 生成优惠券池ID
        couponPoolPo.setPoolId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务保存优惠券池数据
        int flag = couponPoolV1InnerServiceSMOImpl.saveCouponPool(couponPoolPo);

        // 检查保存结果，如果影响行数小于1则认为保存失败
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}