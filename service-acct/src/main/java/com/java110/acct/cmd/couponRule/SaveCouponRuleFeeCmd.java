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
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.coupon.CouponRuleFeeDto;
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
 * 类表述：保存优惠券规则费用关联信息
 * 服务编码：couponRuleFee.saveCouponRuleFee
 * 请求路劲：/app/couponRuleFee.SaveCouponRuleFee
 * add by 吴学文 at 2022-11-22 01:44:48 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 优惠券规则费用关联保存命令类
 * 负责处理优惠券规则与费用项的关联关系保存操作
 * 包括参数验证、业务逻辑处理和数据库保存等功能
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-11-22
 */
@Java110Cmd(serviceCode = "couponRule.saveCouponRuleFee")
public class SaveCouponRuleFeeCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveCouponRuleFeeCmd.class);

    /**
     * ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 优惠券规则费用关联服务接口
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
     * 验证请求参数的有效性和完整性，确保业务逻辑执行的正确性
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填参数是否存在
        Assert.hasKeyAndValue(reqJson, "ruleId", "请求报文中未包含ruleId");
        Assert.hasKeyAndValue(reqJson, "feeConfigId", "请求报文中未包含feeConfigId");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.hasKeyAndValue(reqJson, "payStartTime", "请求报文中未包含payStartTime");
        Assert.hasKeyAndValue(reqJson, "payEndTime", "请求报文中未包含payEndTime");
        Assert.hasKeyAndValue(reqJson, "payMonth", "请求报文中未包含payMonth");

        // 检查费用项是否已经关联到该规则
        CouponRuleFeeDto couponRuleFeeDto = new CouponRuleFeeDto();
        couponRuleFeeDto.setRuleId(reqJson.getString("ruleId"));
        couponRuleFeeDto.setFeeConfigId(reqJson.getString("feeConfigId"));
        long count = couponRuleFeeV1InnerServiceSMOImpl.queryCouponRuleFeesCount(couponRuleFeeDto);

        // 如果已经存在关联关系，则抛出异常
        if(count > 0){
            throw new CmdException("费用项已经关联");
        }
    }

    /**
     * 执行保存优惠券规则费用关联信息
     * 包含事务管理，确保数据的一致性
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 查询费用配置信息
        FeeConfigDto feeConfigDto = new FeeConfigDto();
        feeConfigDto.setConfigId(reqJson.getString("feeConfigId"));
        List<FeeConfigDto> feeConfigDtos = feeConfigInnerServiceSMOImpl.queryFeeConfigs(feeConfigDto);

        // 验证费用项是否存在且唯一
        Assert.listOnlyOne(feeConfigDtos,"费用项不存在");

        // 将请求参数转换为持久化对象
        CouponRuleFeePo couponRuleFeePo = BeanConvertUtil.covertBean(reqJson, CouponRuleFeePo.class);
        // 生成主键ID
        couponRuleFeePo.setCrfId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        // 设置费用项名称
        couponRuleFeePo.setFeeConfigName(feeConfigDtos.get(0).getFeeName());
        
        // 保存数据到数据库
        int flag = couponRuleFeeV1InnerServiceSMOImpl.saveCouponRuleFee(couponRuleFeePo);

        // 检查保存结果
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}