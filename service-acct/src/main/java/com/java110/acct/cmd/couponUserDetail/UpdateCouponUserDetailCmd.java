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
package com.java110.acct.cmd.couponUserDetail;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.acct.ICouponUserDetailV1InnerServiceSMO;
import com.java110.po.coupon.CouponUserDetailPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;


/**
 * 类表述：更新
 * 服务编码：couponUserDetail.updateCouponUserDetail
 * 请求路劲：/app/couponUserDetail.UpdateCouponUserDetail
 * add by 吴学文 at 2021-11-24 00:20:57 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 优惠券用户详情更新命令类
 * 
 * 该类负责处理优惠券用户详情的更新操作，包括参数验证和业务逻辑执行。
 * 通过注解@Java110Cmd标识服务编码，与前端请求路径对应。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2021-11-24
 */
@Java110Cmd(serviceCode = "couponUserDetail.updateCouponUserDetail")
public class UpdateCouponUserDetailCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(UpdateCouponUserDetailCmd.class);

    /**
     * 优惠券用户详情服务接口
     * 用于执行优惠券用户详情的数据库操作
     */
    @Autowired
    private ICouponUserDetailV1InnerServiceSMO couponUserDetailV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整，确保必要的字段都存在且不为空。
     * 如果缺少必要参数，会抛出相应的异常。
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必要参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "uoId", "uoId不能为空");
        Assert.hasKeyAndValue(reqJson, "couponId", "请求报文中未包含couponId");
        Assert.hasKeyAndValue(reqJson, "couponName", "请求报文中未包含couponName");
        Assert.hasKeyAndValue(reqJson, "userId", "请求报文中未包含userId");
        Assert.hasKeyAndValue(reqJson, "userName", "请求报文中未包含userName");
        Assert.hasKeyAndValue(reqJson, "objType", "请求报文中未包含objType");
        Assert.hasKeyAndValue(reqJson, "objId", "请求报文中未包含objId");
    }

    /**
     * 命令执行方法
     * 
     * 执行优惠券用户详情的更新操作，包括数据转换、业务逻辑处理和结果返回。
     * 使用@Java110Transactional注解确保操作的原子性。
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON对象转换为优惠券用户详情POJO对象
        CouponUserDetailPo couponUserDetailPo = BeanConvertUtil.covertBean(reqJson, CouponUserDetailPo.class);
        
        // 调用服务接口执行更新操作，返回受影响的行数
        int flag = couponUserDetailV1InnerServiceSMOImpl.updateCouponUserDetail(couponUserDetailPo);

        // 检查更新操作是否成功，如果受影响行数小于1则认为更新失败
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}