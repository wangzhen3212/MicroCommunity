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
package com.java110.acct.cmd.couponDetail;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.acct.ICouponDetailV1InnerServiceSMO;
import com.java110.po.coupon.CouponDetailPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;

/**
 * 优惠券详情删除命令类
 * 
 * 该类负责处理优惠券详情的删除操作，实现了删除优惠券详情的业务逻辑。
 * 通过注解标识服务编码，与前端请求路径对应，提供参数验证和删除执行功能。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2021-11-24
 */
@Java110Cmd(serviceCode = "couponDetail.deleteCouponDetail")
public class DeleteCouponDetailCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteCouponDetailCmd.class);

    /**
     * 优惠券详情服务接口
     * 用于调用优惠券详情的数据库操作
     */
    @Autowired
    private ICouponDetailV1InnerServiceSMO couponDetailV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的detailId字段，确保删除操作有明确的标识。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含detailId字段，确保删除操作有明确的标识
        Assert.hasKeyAndValue(reqJson, "detailId", "detailId不能为空");
    }

    /**
     * 执行删除命令方法
     * 
     * 将请求参数转换为优惠券详情PO对象，调用服务层执行删除操作，
     * 并根据删除结果设置响应信息。
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为优惠券详情PO对象，便于后续数据库操作
        CouponDetailPo couponDetailPo = BeanConvertUtil.covertBean(reqJson, CouponDetailPo.class);
        
        // 调用服务层执行删除操作，返回影响的行数
        int flag = couponDetailV1InnerServiceSMOImpl.deleteCouponDetail(couponDetailPo);

        // 检查删除操作是否成功，如果影响行数小于1则认为删除失败
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 设置成功的响应结果，返回操作成功信息
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}