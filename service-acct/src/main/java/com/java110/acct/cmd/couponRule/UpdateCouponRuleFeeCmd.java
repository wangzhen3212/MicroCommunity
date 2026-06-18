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
package com.java110.acct.cmd.couponRule;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.intf.acct.ICouponRuleFeeV1InnerServiceSMO;
import com.java110.intf.fee.IFeeConfigInnerServiceSMO;
import com.java110.po.coupon.CouponRuleFeePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;


/**
 * 类表述：更新
 * 服务编码：couponRuleFee.updateCouponRuleFee
 * 请求路劲：/app/couponRuleFee.UpdateCouponRuleFee
 * add by 吴学文 at 2022-11-22 01:44:48 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 优惠券规则费用更新命令类
 * 
 * 该类负责处理优惠券规则费用的更新操作，包括参数验证、费用配置查询和优惠券规则费用信息的更新
 * 通过注解@Java110Cmd指定服务编码，用于路由到对应的命令处理器
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-11-22
 */
@Java110Cmd(serviceCode = "couponRule.updateCouponRuleFee")
public class UpdateCouponRuleFeeCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(UpdateCouponRuleFeeCmd.class);

    /**
     * 优惠券规则费用服务接口
     */
    @Autowired
    private ICouponRuleFeeV1InnerServiceSMO couponRuleFeeV1InnerServiceSMOImpl;

    /**
     * 费用配置服务接口
     */
    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段，确保更新操作的数据完整性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含crfId字段
        Assert.hasKeyAndValue(reqJson, "crfId", "crfId不能为空");
        // 验证请求参数中必须包含communityId字段
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行优惠券规则费用更新命令
     * 
     * 该方法负责实际的业务逻辑处理，包括查询费用配置信息、更新优惠券规则费用数据
     * 使用@Java110Transactional注解确保操作的原子性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当费用配置不存在或更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 创建费用配置查询对象
        FeeConfigDto feeConfigDto = new FeeConfigDto();
        // 设置要查询的费用配置ID
        feeConfigDto.setConfigId(reqJson.getString("feeConfigId"));
        // 查询费用配置信息
        List<FeeConfigDto> feeConfigDtos = feeConfigInnerServiceSMOImpl.queryFeeConfigs(feeConfigDto);

        // 验证费用配置存在且唯一
        Assert.listOnlyOne(feeConfigDtos,"费用项不存在");

        // 将请求JSON转换为优惠券规则费用PO对象
        CouponRuleFeePo couponRuleFeePo = BeanConvertUtil.covertBean(reqJson, CouponRuleFeePo.class);
        // 设置费用配置名称到PO对象中
        couponRuleFeePo.setFeeConfigName(feeConfigDtos.get(0).getFeeName());

        // 调用服务更新优惠券规则费用信息
        int flag = couponRuleFeeV1InnerServiceSMOImpl.updateCouponRuleFee(couponRuleFeePo);

        // 检查更新操作是否成功
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}