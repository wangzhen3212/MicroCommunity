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
package com.java110.acct.cmd.couponShopPoolDetail;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.acct.ICouponShopPoolDetailV1InnerServiceSMO;
import com.java110.po.coupon.CouponShopPoolDetailPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;


/**
 * 类表述：更新
 * 服务编码：couponShopPoolDetail.updateCouponShopPoolDetail
 * 请求路劲：/app/couponShopPoolDetail.UpdateCouponShopPoolDetail
 * add by 吴学文 at 2021-11-24 00:17:18 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 优惠券商铺池详情更新命令类
 * 
 * 该类负责处理优惠券商铺池详情的更新操作，包括参数验证和业务逻辑执行
 * 通过注解标识服务编码，与前端请求路径对应
 * 
 * @author 吴学文
 * @version 1.0
 * @date 2021-11-24
 */
@Java110Cmd(serviceCode = "couponShopPoolDetail.updateCouponShopPoolDetail")
public class UpdateCouponShopPoolDetailCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(UpdateCouponShopPoolDetailCmd.class);

    /**
     * 优惠券商铺池详情服务接口
     */
    @Autowired
    private ICouponShopPoolDetailV1InnerServiceSMO couponShopPoolDetailV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整，确保更新操作所需的所有必要字段都存在
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证detailId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "detailId", "detailId不能为空");
        // 验证spId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "spId", "请求报文中未包含spId");
        // 验证poolId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "poolId", "请求报文中未包含poolId");
        // 验证shopId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "shopId", "请求报文中未包含shopId");
        // 验证couponName参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "couponName", "请求报文中未包含couponName");
        // 验证actualPrice参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "actualPrice", "请求报文中未包含actualPrice");
        // 验证sendCount参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "sendCount", "请求报文中未包含sendCount");
        // 验证validityDay参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "validityDay", "请求报文中未包含validityDay");
        // 验证userId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "userId", "请求报文中未包含userId");
        // 验证userName参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "userName", "请求报文中未包含userName");
        // 验证tel参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "tel", "请求报文中未包含tel");
    }

    /**
     * 执行更新命令方法
     * 
     * 将请求参数转换为实体对象，调用服务层进行数据更新操作
     * 使用事务注解确保操作的原子性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为优惠券商铺池详情实体对象
        CouponShopPoolDetailPo couponShopPoolDetailPo = BeanConvertUtil.covertBean(reqJson, CouponShopPoolDetailPo.class);
        
        // 调用服务层接口执行更新操作，返回影响的行数
        int flag = couponShopPoolDetailV1InnerServiceSMOImpl.updateCouponShopPoolDetail(couponShopPoolDetailPo);

        // 检查更新操作是否成功，如果影响行数小于1则认为更新失败
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}