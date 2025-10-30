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
package com.java110.store.cmd.assetInventoryDetail;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.store.IAssetInventoryDetailV1InnerServiceSMO;
import com.java110.po.assetInventory.AssetInventoryDetailPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 资产盘点明细删除命令类
 * 负责处理资产盘点明细的删除操作，包括参数验证和业务逻辑执行
 * 通过注解标识服务编码，与前端请求路径对应
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-09-23
 */
@Java110Cmd(serviceCode = "assetInventoryDetail.deleteAssetInventoryDetail")
public class DeleteAssetInventoryDetailCmd extends Cmd {
    
    /**
     * 日志记录器
     * 用于记录类执行过程中的日志信息
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteAssetInventoryDetailCmd.class);

    /**
     * 资产盘点明细服务接口
     * 用于执行资产盘点明细的数据库操作，包括删除、查询等
     * 通过Spring自动注入实现类
     */
    @Autowired
    private IAssetInventoryDetailV1InnerServiceSMO assetInventoryDetailV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数中是否包含必要的字段，确保删除操作的完整性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象，包含要删除的资产盘点明细信息
     * @throws IllegalArgumentException 当必要参数缺失时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含id字段，用于标识要删除的资产盘点明细记录
        Assert.hasKeyAndValue(reqJson, "id", "id不能为空");
        // 验证请求参数中必须包含communityId字段，用于标识所属小区
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 命令执行方法
     * 执行资产盘点明细的删除操作，包括数据转换和业务逻辑处理
     * 使用事务注解确保操作的原子性，要么全部成功，要么全部回滚
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于设置响应结果
     * @param reqJson 请求参数的JSON对象，包含要删除的资产盘点明细信息
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为资产盘点明细持久化对象，便于后续数据库操作
        AssetInventoryDetailPo assetInventoryDetailPo = BeanConvertUtil.covertBean(reqJson, AssetInventoryDetailPo.class);
        
        // 调用服务层执行删除操作，返回影响的行数
        int flag = assetInventoryDetailV1InnerServiceSMOImpl.deleteAssetInventoryDetail(assetInventoryDetailPo);

        // 检查删除操作是否成功，影响行数小于1表示删除失败
        if (flag < 1) {
            // 删除失败时抛出命令异常，事务会自动回滚
            throw new CmdException("删除数据失败");
        }

        // 设置响应结果为成功，返回操作成功的响应信息
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}