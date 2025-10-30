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
import com.java110.dto.coupon.CouponRuleCppsDto;
import com.java110.intf.acct.ICouponRuleCppsV1InnerServiceSMO;
import com.java110.po.coupon.CouponRuleCppsPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 类表述：保存
 * 服务编码：couponRuleCpps.saveCouponRuleCpps
 * 请求路劲：/app/couponRuleCpps.SaveCouponRuleCpps
 * add by 吴学文 at 2022-11-22 01:37:00 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 优惠券规则关联关系保存命令类
 * 
 * 该类负责处理优惠券规则与优惠券产品规格的关联关系保存操作，
 * 包括参数验证、业务逻辑处理和数据库保存等功能
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-11-22
 */
@Java110Cmd(serviceCode = "couponRule.saveCouponRuleCpps")
public class SaveCouponRuleCppsCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveCouponRuleCppsCmd.class);

    /**
     * ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 优惠券规则关联关系服务接口
     */
    @Autowired
    private ICouponRuleCppsV1InnerServiceSMO couponRuleCppsV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整，并检查优惠券规则是否已经关联过指定的优惠券产品规格
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败或业务规则不满足时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必需参数是否存在
        Assert.hasKeyAndValue(reqJson, "ruleId", "请求报文中未包含ruleId");
        Assert.hasKeyAndValue(reqJson, "cppId", "请求报文中未包含cppId");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.hasKeyAndValue(reqJson, "quantity", "请求报文中未包含quantity");

        // 查询规则是否已经关联过优惠券产品规格
        CouponRuleCppsDto couponRuleCppsDto = new CouponRuleCppsDto();
        couponRuleCppsDto.setRuleId(reqJson.getString("ruleId")); // 设置规则ID
        couponRuleCppsDto.setCppId(reqJson.getString("cppId"));   // 设置优惠券产品规格ID
        
        // 查询已存在的关联关系数量
        int count = couponRuleCppsV1InnerServiceSMOImpl.queryCouponRuleCppssCount(couponRuleCppsDto);

        // 如果已存在关联关系，则抛出异常
        if(count > 0){
            throw new CmdException("该优惠券已经关联过");
        }
    }

    /**
     * 执行保存命令方法
     * 
     * 将请求参数转换为实体对象，生成唯一ID，并保存到数据库中
     * 使用事务注解确保数据一致性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON对象转换为实体对象
        CouponRuleCppsPo couponRuleCppsPo = BeanConvertUtil.covertBean(reqJson, CouponRuleCppsPo.class);
        
        // 生成唯一ID并设置到实体对象中
        couponRuleCppsPo.setCrcId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 执行保存操作
        int flag = couponRuleCppsV1InnerServiceSMOImpl.saveCouponRuleCpps(couponRuleCppsPo);

        // 检查保存是否成功
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}