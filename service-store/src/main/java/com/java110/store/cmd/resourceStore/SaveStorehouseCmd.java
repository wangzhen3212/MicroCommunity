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
package com.java110.store.cmd.resourceStore;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.store.StorehouseDto;
import com.java110.intf.store.IStorehouseInnerServiceSMO;
import com.java110.intf.store.IStorehouseV1InnerServiceSMO;
import com.java110.po.store.StorehousePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 类表述：保存仓库信息命令类
 * 服务编码：storehouse.saveStorehouse
 * 请求路劲：/app/storehouse.SaveStorehouse
 * add by 吴学文 at 2022-08-08 15:47:06 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * 功能描述：处理仓库信息保存的CMD命令类，负责接收前端请求并保存仓库信息到数据库
 * 包括参数验证、业务逻辑处理和事务管理
 */
@Java110Cmd(serviceCode = "resourceStore.saveStorehouse")
public class SaveStorehouseCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveStorehouseCmd.class);

    /**
     * 仓库ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 仓库V1版本服务接口
     */
    @Autowired
    private IStorehouseV1InnerServiceSMO storehouseV1InnerServiceSMOImpl;

    /**
     * 仓库服务接口
     */
    @Autowired
    private IStorehouseInnerServiceSMO storehouseInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性和完整性，包括必填字段检查、重复性检查和业务规则验证
     *
     * @param event CMD事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置上下文信息
     * @param reqJson 请求参数的JSON对象
     * @throws IllegalArgumentException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填字段是否存在
        Assert.hasKeyAndValue(reqJson, "shName", "请求报文中未包含shName");
        Assert.hasKeyAndValue(reqJson, "purchaseSwitch", "请求报文中未包含purchaseSwitch");
        Assert.hasKeyAndValue(reqJson, "useSwitch", "请求报文中未包含useSwitch");
        Assert.hasKeyAndValue(reqJson, "purchaseSwitch", "请求报文中未包含purchaseSwitch");
        Assert.hasKeyAndValue(reqJson, "useSwitch", "请求报文中未包含useSwitch");
        Assert.hasKeyAndValue(reqJson, "allocationSwitch", "请求报文中未包含allocationSwitch");
        
        // 从上下文中获取店铺ID并设置到请求参数中
        String storeId = CmdContextUtils.getStoreId(cmdDataFlowContext);
        reqJson.put("storeId", storeId);

        // 检查仓库名称是否重复
        StorehouseDto storehouseDto = new StorehouseDto();
        storehouseDto.setShName(reqJson.getString("shName"));
        storehouseDto.setStoreId(reqJson.getString("storeId"));
        int flag = storehouseInnerServiceSMOImpl.queryStorehousesCount(storehouseDto);
        if (flag > 0) {
            throw new IllegalArgumentException("已存在仓库");
        }

        // 根据采购开关状态验证采购流程ID
        if ("ON".equals(reqJson.getString("purchaseSwitch"))) {
            Assert.hasKeyAndValue(reqJson, "purchaseRafId", "请求报文中未包含采购流程");
        }

        // 根据领用开关状态验证领用流程ID
        if ("ON".equals(reqJson.getString("useSwitch"))) {
            Assert.hasKeyAndValue(reqJson, "useRafId", "请求报文中未包含领用流程");
        }

        // 根据调拨开关状态验证调拨流程ID
        if ("ON".equals(reqJson.getString("allocationSwitch"))) {
            Assert.hasKeyAndValue(reqJson, "allocationRafId", "请求报文中未包含调拨流程");
        }
    }

    /**
     * 执行保存仓库信息的核心方法
     * 将验证通过的请求数据转换为PO对象并保存到数据库，支持事务管理
     *
     * @param event CMD事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置上下文信息
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON对象转换为仓库PO对象
        StorehousePo storehousePo = BeanConvertUtil.covertBean(reqJson, StorehousePo.class);
        // 生成仓库ID
        storehousePo.setShId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));

        // 调用服务保存仓库信息
        int flag = storehouseV1InnerServiceSMOImpl.saveStorehouse(storehousePo);

        // 检查保存结果
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}