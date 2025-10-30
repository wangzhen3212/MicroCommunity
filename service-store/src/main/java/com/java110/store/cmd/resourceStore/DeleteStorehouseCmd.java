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
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.community.IResourceStoreServiceSMO;
import com.java110.intf.store.IStorehouseV1InnerServiceSMO;
import com.java110.po.purchase.ResourceStorePo;
import com.java110.po.store.StorehousePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

/**
 * 类表述：删除仓库命令类
 * 服务编码：storehouse.deleteStorehouse
 * 请求路劲：/app/storehouse.DeleteStorehouse
 * 功能描述：该命令类用于处理删除仓库的请求，在删除前会检查仓库中是否包含物品，
 *          如果仓库为空则允许删除，否则返回错误提示
 * add by 吴学文 at 2022-08-08 15:47:06 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "resourceStore.deleteStorehouse")
public class DeleteStorehouseCmd extends Cmd {
    private static Logger logger = LoggerFactory.getLogger(DeleteStorehouseCmd.class);

    /**
     * 仓库服务接口，用于执行仓库的删除操作
     */
    @Autowired
    private IStorehouseV1InnerServiceSMO storehouseV1InnerServiceSMOImpl;

    /**
     * 资源库存服务接口，用于查询仓库中的物品数量
     */
    @Autowired
    private IResourceStoreServiceSMO resourceStoreServiceSMOImpl;

    /**
     * 验证请求参数
     * 功能描述：验证删除仓库请求的必需参数是否完整
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求中必须包含shId参数
        Assert.hasKeyAndValue(reqJson, "shId", "shId不能为空");
    }

    /**
     * 执行删除仓库命令
     * 功能描述：删除指定的仓库，但在删除前会检查仓库中是否包含物品，
     *          如果仓库不为空则不允许删除并返回错误信息
     * @param event 命令事件对象
     * @param context 数据流上下文，用于设置响应结果
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 从请求中获取仓库ID
        String shId = reqJson.getString("shId");
        
        // 创建资源库存查询对象，用于检查仓库中是否包含物品
        ResourceStorePo resourceStorePo = new ResourceStorePo();
        resourceStorePo.setShId(shId);
        
        // 查询指定仓库中的物品数量
        int page = resourceStoreServiceSMOImpl.getResourceStoresCount(resourceStorePo);
        
        // 如果仓库中没有物品，则执行删除操作
        if (page < 1) {
            // 将请求JSON转换为仓库对象
            StorehousePo storehousePo = BeanConvertUtil.covertBean(reqJson, StorehousePo.class);
            
            // 调用服务删除仓库
            int flag = storehouseV1InnerServiceSMOImpl.deleteStorehouse(storehousePo);
            
            // 检查删除操作是否成功
            if (flag < 1) {
                throw new CmdException("删除数据失败");
            }
        } else {
            // 如果仓库中包含物品，返回业务验证错误
            ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_BUSINESS_VERIFICATION, "该仓库包含物品，不能直接删除！");
            context.setResponseEntity(responseEntity);
            return;
        }
        
        // 设置删除成功的响应
        context.setResponseEntity(ResultVo.success());
    }
}