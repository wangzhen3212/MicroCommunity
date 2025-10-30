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
package com.java110.acct.cmd.couponProperty;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.acct.ICouponPropertyPoolConfigV1InnerServiceSMO;
import com.java110.po.coupon.CouponPropertyPoolConfigPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 类表述：保存优惠券属性池配置命令类
 * 服务编码：couponPropertyPoolConfig.saveCouponPropertyPoolConfig
 * 请求路径：/app/couponPropertyPoolConfig.SaveCouponPropertyPoolConfig
 * 功能描述：处理保存优惠券属性池配置的请求，包括参数验证和业务逻辑处理
 * add by 吴学文 at 2022-11-19 23:41:54 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "couponProperty.saveCouponPropertyPoolConfig")
public class SaveCouponPropertyPoolConfigCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveCouponPropertyPoolConfigCmd.class);

    /**
     * 配置ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 优惠券属性池配置服务接口
     */
    @Autowired
    private ICouponPropertyPoolConfigV1InnerServiceSMO couponPropertyPoolConfigV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 功能描述：验证请求参数是否完整，检查必要字段是否存在
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求报文中必须包含couponId字段
        Assert.hasKeyAndValue(reqJson, "couponId", "请求报文中未包含couponId");
        // 验证请求报文中必须包含columnKey字段
        Assert.hasKeyAndValue(reqJson, "columnKey", "请求报文中未包含columnKey");
        // 验证请求报文中必须包含columnValue字段
        Assert.hasKeyAndValue(reqJson, "columnValue", "请求报文中未包含columnValue");
    }

    /**
     * 执行命令方法
     * 功能描述：处理保存优惠券属性池配置的业务逻辑，包括数据转换、ID生成和数据持久化
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为优惠券属性池配置POJO对象
        CouponPropertyPoolConfigPo couponPropertyPoolConfigPo = BeanConvertUtil.covertBean(reqJson, CouponPropertyPoolConfigPo.class);
        // 生成配置ID，使用预设的前缀
        couponPropertyPoolConfigPo.setConfigId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务层保存配置数据
        int flag = couponPropertyPoolConfigV1InnerServiceSMOImpl.saveCouponPropertyPoolConfig(couponPropertyPoolConfigPo);

        // 检查保存结果，如果影响行数小于1则认为保存失败
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}