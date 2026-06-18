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
package com.java110.acct.cmd.supplierCoupon;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.intf.acct.ISupplierCouponV1InnerServiceSMO;
import com.java110.po.supplier.SupplierCouponPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Java110CmdDoc(title = "删除供应商优惠券",
        description = "用于外系统删除供应商优惠券",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/supplierCoupon.deleteSupplierCoupon",
        resource = "acctDoc",
        author = "吴学文",
        serviceCode = "supplierCoupon.deleteSupplierCoupona"
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "couponId", length = 30, remark = "优惠券ID"),
        @Java110ParamDoc(name = "supplierId", length = 30, remark = "供应商ID"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

@Java110ExampleDoc(
        reqBody="{\"couponId\":\"123123\",\"supplierId\":\"123123\"}",
        resBody="{'code':0,'msg':'成功'}"
)
/**
 * 删除供应商优惠券命令类
 * 
 * 该类负责处理删除供应商优惠券的业务逻辑，包括参数验证和删除操作。
 * 通过注解配置了API文档信息，支持外系统调用删除供应商优惠券功能。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-11-17
 */
@Java110Cmd(serviceCode = "supplierCoupon.deleteSupplierCoupon")
public class DeleteSupplierCouponCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteSupplierCouponCmd.class);

    /**
     * 供应商优惠券服务接口
     */
    @Autowired
    private ISupplierCouponV1InnerServiceSMO supplierCouponV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段，确保删除操作有足够的条件信息
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含couponId字段
        Assert.hasKeyAndValue(reqJson, "couponId", "couponId不能为空");
        // 验证请求参数中必须包含supplierId字段
        Assert.hasKeyAndValue(reqJson, "supplierId", "supplierId不能为空");
    }

    /**
     * 执行删除供应商优惠券命令
     * 
     * 将请求参数转换为供应商优惠券对象，调用服务层进行删除操作，
     * 并根据删除结果设置响应信息
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为供应商优惠券PO对象
        SupplierCouponPo supplierCouponPo = BeanConvertUtil.covertBean(reqJson, SupplierCouponPo.class);
        
        // 调用服务层删除供应商优惠券，返回影响的行数
        int flag = supplierCouponV1InnerServiceSMOImpl.deleteSupplierCoupon(supplierCouponPo);

        // 检查删除操作是否成功，如果影响行数小于1表示删除失败
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 设置成功的响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}