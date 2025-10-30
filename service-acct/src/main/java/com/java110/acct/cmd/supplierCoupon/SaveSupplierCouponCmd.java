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
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.doc.annotation.*;
import com.java110.dto.supplier.SupplierDto;
import com.java110.intf.acct.ISupplierCouponV1InnerServiceSMO;
import com.java110.intf.scm.ISupplierV1InnerServiceSMO;
import com.java110.po.supplier.SupplierCouponPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


@Java110CmdDoc(title = "添加供应商优惠券",
        description = "用于外系统添加供应商优惠券",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/supplierCoupon.saveSupplierCoupon",
        resource = "acctDoc",
        author = "吴学文",
        serviceCode = "supplierCoupon.saveSupplierCoupon"
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "supplierId", length = 30, remark = "供应商ID"),
        @Java110ParamDoc(name = "name", length = 64, remark = "名称"),
        @Java110ParamDoc(name = "businessKey", length = 64, remark = "优惠券业务ID，第三方ID"),
        @Java110ParamDoc(name = "valuePrice", length = 64, remark = "售价")
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

@Java110ExampleDoc(
        reqBody="{\"supplierId\":\"123123\",\"name\":\"2号楼\",\"businessKey\":\"123123\",\"valuePrice\":\"123123\"}",
        resBody="{'code':0,'msg':'成功'}"
)

/**
 * 保存供应商优惠券命令类
 * 该类负责处理添加供应商优惠券的业务逻辑，包括参数验证、数据转换和持久化操作
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-11-17
 */
@Java110Cmd(serviceCode = "supplierCoupon.saveSupplierCoupon")
public class SaveSupplierCouponCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveSupplierCouponCmd.class);

    /**
     * 优惠券ID前缀，用于生成唯一的优惠券ID
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 供应商优惠券服务接口，用于操作供应商优惠券数据
     */
    @Autowired
    private ISupplierCouponV1InnerServiceSMO supplierCouponV1InnerServiceSMOImpl;

    /**
     * 供应商服务接口，用于查询供应商信息
     */
    @Autowired
    private ISupplierV1InnerServiceSMO supplierV1InnerServiceSMOImpl;

    /**
     * 验证请求参数的有效性
     * 检查请求报文中是否包含必要的参数，确保后续业务逻辑能够正常执行
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证名称参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "name", "请求报文中未包含name");
        // 验证供应商ID参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "supplierId", "请求报文中未包含supplierId");
        // 验证业务键参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "businessKey", "请求报文中未包含businessKey");
        // 验证价值价格参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "valuePrice", "请求报文中未包含valuePrice");
    }

    /**
     * 执行保存供应商优惠券的核心业务逻辑
     * 包括供应商验证、数据转换、优惠券ID生成和数据持久化操作
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当保存数据失败时抛出命令异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 创建供应商查询对象并设置供应商ID
        SupplierDto supplierDto = new SupplierDto();
        supplierDto.setSupplierId(reqJson.getString("supplierId"));
        
        // 查询供应商信息，验证供应商是否存在
        List<SupplierDto> supplierDtos = supplierV1InnerServiceSMOImpl.querySuppliers(supplierDto);

        // 验证查询结果，确保只存在一个匹配的供应商
        Assert.listOnlyOne(supplierDtos,"请先添加供应商");

        // 将请求JSON对象转换为供应商优惠券持久化对象
        SupplierCouponPo supplierCouponPo = BeanConvertUtil.covertBean(reqJson, SupplierCouponPo.class);
        // 设置供应商名称
        supplierCouponPo.setSupplierName(supplierDtos.get(0).getSupplierName());
        // 生成唯一的优惠券ID
        supplierCouponPo.setCouponId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务保存供应商优惠券数据
        int flag = supplierCouponV1InnerServiceSMOImpl.saveSupplierCoupon(supplierCouponPo);

        // 检查保存操作是否成功，失败则抛出异常
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}