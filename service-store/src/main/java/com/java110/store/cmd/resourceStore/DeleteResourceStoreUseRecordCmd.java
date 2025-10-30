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
import com.java110.intf.store.IResourceStoreUseRecordV1InnerServiceSMO;
import com.java110.po.resource.ResourceStoreUseRecordPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 资源库使用记录删除命令类
 * 
 * 该类负责处理删除资源库使用记录的请求，包括参数验证和业务逻辑执行
 * 服务编码：resourceStore.deleteResourceStoreUseRecord
 * 请求路径：/app/resourceStoreUseRecord.DeleteResourceStoreUseRecord
 * 
 * @author 吴学文 at 2022-08-08 14:39:58 mail: 928255095@qq.com
 * @open source address: https://gitee.com/wuxw7/MicroCommunity
 * @official website: http://www.homecommunity.cn
 * 
 * @温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "resourceStore.deleteResourceStoreUseRecord")
public class DeleteResourceStoreUseRecordCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteResourceStoreUseRecordCmd.class);

    /**
     * 资源库使用记录服务接口
     */
    @Autowired
    private IResourceStoreUseRecordV1InnerServiceSMO resourceStoreUseRecordV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必需的字段
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws IllegalArgumentException 当必需参数缺失时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含rsurId字段
        Assert.hasKeyAndValue(reqJson, "rsurId", "rsurId不能为空");
    }

    /**
     * 命令执行方法
     * 
     * 执行删除资源库使用记录的业务逻辑，包括数据转换和服务调用
     * 使用事务注解确保操作的原子性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为实体对象
        ResourceStoreUseRecordPo resourceStoreUseRecordPo = BeanConvertUtil.covertBean(reqJson, ResourceStoreUseRecordPo.class);
        
        // 调用服务接口执行删除操作
        int flag = resourceStoreUseRecordV1InnerServiceSMOImpl.deleteResourceStoreUseRecord(resourceStoreUseRecordPo);

        // 检查删除操作是否成功
        if (flag < 1) {
            // 删除失败时抛出异常
            throw new CmdException("删除数据失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}