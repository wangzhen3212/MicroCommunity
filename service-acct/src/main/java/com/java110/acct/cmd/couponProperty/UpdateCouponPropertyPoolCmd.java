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
import com.java110.intf.acct.ICouponPropertyPoolConfigV1InnerServiceSMO;
import com.java110.intf.acct.ICouponPropertyPoolV1InnerServiceSMO;
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


/**
 * 类表述：更新
 * 服务编码：couponProperty.updateCouponPropertyPool
 * 请求路劲：/app/couponProperty.UpdateCouponPropertyPool
 * add by 吴学文 at 2022-11-19 23:00:42 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 优惠券属性池更新命令类
 * 负责处理优惠券属性池的更新操作，包括验证请求参数和执行更新逻辑
 * 该类继承自Cmd基类，通过注解标识服务编码，用于处理特定的业务请求
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-11-19
 */
@Java110Cmd(serviceCode = "couponProperty.updateCouponPropertyPool")
public class UpdateCouponPropertyPoolCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdateCouponPropertyPoolCmd.class);

    /**
     * 优惠券属性池服务接口
     */
    @Autowired
    private ICouponPropertyPoolV1InnerServiceSMO couponPropertyPoolV1InnerServiceSMOImpl;

    /**
     * 优惠券属性池配置服务接口
     */
    @Autowired
    private ICouponPropertyPoolConfigV1InnerServiceSMO couponPropertyPoolConfigV1InnerServiceSMOImpl;

    /**
     * 验证请求参数的有效性
     * 检查必要的参数是否存在且格式正确，确保请求数据的完整性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置数据流信息
     * @param reqJson 请求的JSON对象，包含所有请求参数
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证cppId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "cppId", "cppId不能为空");

        // 检查是否包含用途配置信息
        if(!reqJson.containsKey("toTypes")){
            throw new CmdException("未包含用途");
        }

        // 获取用途配置数组
        JSONArray toTypes = reqJson.getJSONArray("toTypes");

        // 验证用途配置数组不为空且至少包含一个元素
        if(toTypes == null || toTypes.size()< 1){
            throw new CmdException("未包含用途");
        }

        JSONObject typeObj = null;
        // 遍历用途配置数组，验证每个配置项的完整性
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
     * 执行优惠券属性池更新操作
     * 更新优惠券属性池基本信息，并重新配置相关的用途信息
     * 采用事务处理确保数据一致性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON对象
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 将请求JSON转换为优惠券属性池PO对象
        CouponPropertyPoolPo couponPropertyPoolPo = BeanConvertUtil.covertBean(reqJson, CouponPropertyPoolPo.class);
        // 执行优惠券属性池更新操作
        int flag = couponPropertyPoolV1InnerServiceSMOImpl.updateCouponPropertyPool(couponPropertyPoolPo);

        // 检查更新操作是否成功
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }
        
        CouponPropertyPoolConfigPo couponPropertyPoolConfigPo = null;
        // 创建优惠券属性池配置对象，用于删除原有配置
        couponPropertyPoolConfigPo = new CouponPropertyPoolConfigPo();
        // 设置优惠券ID，用于标识要删除的配置记录
        couponPropertyPoolConfigPo.setCouponId(couponPropertyPoolPo.getCppId());

        // 删除该优惠券原有的所有配置信息
        couponPropertyPoolConfigV1InnerServiceSMOImpl.deleteCouponPropertyPoolConfig(couponPropertyPoolConfigPo);

        // 获取新的用途配置数组
        JSONArray toTypes = reqJson.getJSONArray("toTypes");

        JSONObject typeObj = null;

        // 遍历新的用途配置数组，逐条添加配置信息
        for(int typeIndex = 0;typeIndex < toTypes.size(); typeIndex++){
            typeObj = toTypes.getJSONObject(typeIndex);
            // 创建新的配置对象
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