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
package com.java110.acct.cmd.couponUser;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.acct.ICouponUserV1InnerServiceSMO;
import com.java110.po.coupon.CouponUserPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;

/**
 * 类表述：保存用户优惠券信息命令类
 * 服务编码：couponUser.saveCouponUser
 * 请求路劲：/app/couponUser.SaveCouponUser
 * 功能描述：该命令类用于处理用户优惠券的保存操作，包括参数验证和业务逻辑处理
 * add by 吴学文 at 2021-11-24 00:19:34 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "couponUser.saveCouponUser")
public class SaveCouponUserCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveCouponUserCmd.class);

    /**
     * 优惠券ID前缀，用于生成优惠券唯一标识
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 优惠券用户服务接口，用于操作优惠券用户相关数据
     */
    @Autowired
    private ICouponUserV1InnerServiceSMO couponUserV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 功能描述：验证请求参数是否完整，确保必要的参数都存在
     * @param event 命令事件对象，包含事件相关信息
     * @param cmdDataFlowContext 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求报文中必须包含poolId参数
        Assert.hasKeyAndValue(reqJson, "poolId", "请求报文中未包含poolId");
        // 验证请求报文中必须包含couponName参数
        Assert.hasKeyAndValue(reqJson, "couponName", "请求报文中未包含couponName");
        // 验证请求报文中必须包含actualPrice参数
        Assert.hasKeyAndValue(reqJson, "actualPrice", "请求报文中未包含actualPrice");
        // 验证请求报文中必须包含validityDay参数
        Assert.hasKeyAndValue(reqJson, "validityDay", "请求报文中未包含validityDay");
        // 验证请求报文中必须包含userId参数
        Assert.hasKeyAndValue(reqJson, "userId", "请求报文中未包含userId");
        // 验证请求报文中必须包含userName参数
        Assert.hasKeyAndValue(reqJson, "userName", "请求报文中未包含userName");
        // 验证请求报文中必须包含tel参数
        Assert.hasKeyAndValue(reqJson, "tel", "请求报文中未包含tel");
        // 验证请求报文中必须包含state参数
        Assert.hasKeyAndValue(reqJson, "state", "请求报文中未包含state");
    }

    /**
     * 执行保存用户优惠券命令
     * 功能描述：将请求参数转换为优惠券用户对象，生成优惠券ID并保存到数据库
     * @param event 命令事件对象，包含事件相关信息
     * @param cmdDataFlowContext 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为优惠券用户POJO对象
        CouponUserPo couponUserPo = BeanConvertUtil.covertBean(reqJson, CouponUserPo.class);
        // 生成优惠券唯一ID，使用前缀+自增序列的方式
        couponUserPo.setCouponId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        // 调用服务层保存优惠券用户信息
        int flag = couponUserV1InnerServiceSMOImpl.saveCouponUser(couponUserPo);

        // 检查保存操作是否成功，flag小于1表示保存失败
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}