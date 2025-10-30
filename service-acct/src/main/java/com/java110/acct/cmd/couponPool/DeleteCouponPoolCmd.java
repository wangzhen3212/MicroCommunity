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
 * 类表述：删除
 * 服务编码：couponPool.deleteCouponPool
 * 请求路劲：/app/couponPool.DeleteCouponPool
 * add by 吴学文 at 2021-11-23 23:48:04 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 优惠券池删除命令类
 * 
 * 该类负责处理删除优惠券池的业务逻辑，继承自Cmd基类，通过注解标识服务编码，
 * 实现请求参数的验证和优惠券池数据的删除操作
 * 
 * @author 吴学文
 * @version 1.0
 * @date 2021-11-23
 */
@Java110Cmd(serviceCode = "couponPool.deleteCouponPool")
public class DeleteCouponPoolCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteCouponPoolCmd.class);

    /**
     * 优惠券池服务接口
     * 用于调用优惠券池相关的数据操作
     */
    @Autowired
    private ICouponPoolV1InnerServiceSMO couponPoolV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的poolId字段，确保删除操作有明确的标识
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含poolId字段
        Assert.hasKeyAndValue(reqJson, "poolId", "poolId不能为空");
    }

    /**
     * 执行删除优惠券池命令
     * 
     * 将请求参数转换为优惠券池对象，调用服务层删除方法，并根据删除结果设置响应
     * 该方法使用事务注解确保操作的原子性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为优惠券池POJO对象
        CouponPoolPo couponPoolPo = BeanConvertUtil.covertBean(reqJson, CouponPoolPo.class);
        
        // 调用服务层删除方法，返回受影响的行数
        int flag = couponPoolV1InnerServiceSMOImpl.deleteCouponPool(couponPoolPo);

        // 检查删除操作是否成功，如果受影响行数小于1则认为删除失败
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 设置成功的响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}