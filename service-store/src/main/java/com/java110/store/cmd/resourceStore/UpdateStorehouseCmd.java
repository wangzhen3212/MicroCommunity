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
import com.java110.intf.store.IStorehouseV1InnerServiceSMO;
import com.java110.po.store.StorehousePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：更新仓库信息命令类
 * 服务编码：storehouse.updateStorehouse
 * 请求路劲：/app/storehouse.UpdateStorehouse
 * 功能描述：该命令类用于处理仓库信息的更新操作，包括参数验证和业务逻辑处理
 * add by 吴学文 at 2022-08-08 15:47:06 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "resourceStore.updateStorehouse")
public class UpdateStorehouseCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(UpdateStorehouseCmd.class);

    /**
     * 仓库服务接口
     */
    @Autowired
    private IStorehouseV1InnerServiceSMO storehouseV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 功能描述：验证更新仓库信息请求参数的完整性和有效性
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填参数：仓库ID和仓库名称
        Assert.hasKeyAndValue(reqJson, "shId", "shId不能为空");
        Assert.hasKeyAndValue(reqJson, "shName", "请求报文中未包含shName");
        
        // 从上下文中获取店铺ID并设置到请求参数中
        String storeId = CmdContextUtils.getStoreId(cmdDataFlowContext);
        reqJson.put("storeId", storeId);
        
        // 根据开关状态验证相关流程ID参数
        // 采购开关开启时，必须提供采购流程ID
        if ("ON".equals(reqJson.getString("purchaseSwitch"))) {
            Assert.hasKeyAndValue(reqJson, "purchaseRafId", "请求报文中未包含采购流程");
        }
        // 使用开关开启时，必须提供使用流程ID
        if ("ON".equals(reqJson.getString("useSwitch"))) {
            Assert.hasKeyAndValue(reqJson, "useRafId", "请求报文中未包含领用流程");
        }
        // 调拨开关开启时，必须提供调拨流程ID
        if ("ON".equals(reqJson.getString("allocationSwitch"))) {
            Assert.hasKeyAndValue(reqJson, "allocationRafId", "请求报文中未包含调拨流程");
        }
    }

    /**
     * 命令执行方法
     * 功能描述：执行仓库信息的更新操作，包括数据转换和业务处理
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当更新操作失败时抛出命令异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON对象转换为仓库POJO对象
        StorehousePo storehousePo = BeanConvertUtil.covertBean(reqJson, StorehousePo.class);
        
        // 处理仓库描述字段，如果为空则设置为默认值"-1"
        if (StringUtil.isEmpty(storehousePo.getShDesc())) {
            storehousePo.setShDesc("-1");
        }
        
        // 调用服务层更新仓库信息
        int flag = storehouseV1InnerServiceSMOImpl.updateStorehouse(storehousePo);
        
        // 检查更新结果，如果更新失败则抛出异常
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }
        
        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}