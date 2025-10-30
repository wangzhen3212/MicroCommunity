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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.coupon.CouponPropertyPoolDto;
import com.java110.intf.acct.ICouponPropertyPoolConfigV1InnerServiceSMO;
import com.java110.intf.acct.ICouponPropertyPoolV1InnerServiceSMO;
import com.java110.intf.community.ICommunityV1InnerServiceSMO;
import com.java110.po.coupon.CouponPropertyPoolPo;
import com.java110.po.coupon.CouponPropertyPoolConfigPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 类表述：保存
 * 服务编码：couponProperty.saveCouponPropertyPool
 * 请求路劲：/app/couponProperty.SaveCouponPropertyPool
 * add by 吴学文 at 2022-11-19 23:00:42 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 优惠券资产池保存命令类
 * 负责处理优惠券资产池的新增操作，包括优惠券基本信息保存和配置信息保存
 * 服务编码：couponProperty.saveCouponPropertyPool
 * 请求路径：/app/couponProperty.SaveCouponPropertyPool
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-11-19
 */
@Java110Cmd(serviceCode = "couponProperty.saveCouponPropertyPool")
public class SaveCouponPropertyPoolCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveCouponPropertyPoolCmd.class);

    /** ID生成前缀 */
    public static final String CODE_PREFIX_ID = "10";

    /** 优惠券资产池服务接口 */
    @Autowired
    private ICouponPropertyPoolV1InnerServiceSMO couponPropertyPoolV1InnerServiceSMOImpl;

    /** 优惠券资产池配置服务接口 */
    @Autowired
    private ICouponPropertyPoolConfigV1InnerServiceSMO couponPropertyPoolConfigV1InnerServiceSMOImpl;

    /** 小区服务接口 */
    @Autowired
    private ICommunityV1InnerServiceSMO communityV1InnerServiceSMOImpl;

    /**
     * 参数校验方法
     * 验证请求参数是否完整和有效
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数校验失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 校验必填字段
        Assert.hasKeyAndValue(reqJson, "couponName", "请求报文中未包含couponName");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.hasKeyAndValue(reqJson, "toType", "请求报文中未包含toType");
        Assert.hasKeyAndValue(reqJson, "stock", "请求报文中未包含stock");
        Assert.hasKeyAndValue(reqJson, "validityDay", "请求报文中未包含validityDay");

        // 校验用途配置数组
        if(!reqJson.containsKey("toTypes")){
            throw new CmdException("未包含用途");
        }

        JSONArray toTypes = reqJson.getJSONArray("toTypes");

        if(toTypes == null || toTypes.size()< 1){
            throw new CmdException("未包含用途");
        }

        // 遍历用途配置，校验每个配置项的值
        JSONObject typeObj = null;
        for(int typeIndex = 0;typeIndex < toTypes.size(); typeIndex++){
            typeObj = toTypes.getJSONObject(typeIndex);
            // 检查是否包含columnValue字段
            if(!typeObj.containsKey("columnValue")){
                throw new CmdException(typeObj.getString("name")+"未填写值");
            }

            // 检查columnValue字段值是否为空
            if(StringUtil.isEmpty(typeObj.getString("columnValue"))){
                throw new CmdException(typeObj.getString("name")+"未填写值");
            }
        }

    }

    /**
     * 执行保存优惠券资产池命令
     * 保存优惠券基本信息及其配置信息
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 查询小区信息
        CommunityDto communityDto = new CommunityDto();
        communityDto.setCommunityId(reqJson.getString("communityId"));
        List<CommunityDto> communityDtos = communityV1InnerServiceSMOImpl.queryCommunitys(communityDto);

        // 校验小区是否存在且唯一
        Assert.listOnlyOne(communityDtos,"小区不存在");

        // 转换请求参数为优惠券资产池PO对象
        CouponPropertyPoolPo couponPropertyPoolPo = BeanConvertUtil.covertBean(reqJson, CouponPropertyPoolPo.class);
        // 生成主键ID
        couponPropertyPoolPo.setCppId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        // 设置小区名称
        couponPropertyPoolPo.setCommunityName(communityDtos.get(0).getName());
        // 设置来源类型为自定义
        couponPropertyPoolPo.setFromType(CouponPropertyPoolDto.FROM_TYPE_CUSTOM);
        // 设置来源ID
        couponPropertyPoolPo.setFromId(couponPropertyPoolPo.getCppId());
        // 设置状态为有效
        couponPropertyPoolPo.setState(CouponPropertyPoolDto.STATE_Y);
        // 保存优惠券资产池基本信息
        int flag = couponPropertyPoolV1InnerServiceSMOImpl.saveCouponPropertyPool(couponPropertyPoolPo);

        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 保存优惠券配置信息
        JSONArray toTypes = reqJson.getJSONArray("toTypes");

        JSONObject typeObj = null;
        CouponPropertyPoolConfigPo couponPropertyPoolConfigPo = null;
        // 遍历用途配置数组，保存每个配置项
        for(int typeIndex = 0;typeIndex < toTypes.size(); typeIndex++){
            typeObj = toTypes.getJSONObject(typeIndex);
            couponPropertyPoolConfigPo = new CouponPropertyPoolConfigPo();
            // 设置配置键
            couponPropertyPoolConfigPo.setColumnKey(typeObj.getString("columnKey"));
            // 设置配置值
            couponPropertyPoolConfigPo.setColumnValue(typeObj.getString("columnValue"));
            // 生成配置ID
            couponPropertyPoolConfigPo.setConfigId(GenerateCodeFactory.getGeneratorId("11"));
            // 设置关联的优惠券ID
            couponPropertyPoolConfigPo.setCouponId(couponPropertyPoolPo.getCppId());
            // 保存配置信息
            couponPropertyPoolConfigV1InnerServiceSMOImpl.saveCouponPropertyPoolConfig(couponPropertyPoolConfigPo);
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}