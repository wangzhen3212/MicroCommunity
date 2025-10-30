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
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.store.IResourceStoreUseRecordV1InnerServiceSMO;
import com.java110.po.resource.ResourceStoreUseRecordPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 类表述：保存
 * 服务编码：resourceStoreUseRecord.saveResourceStoreUseRecord
 * 请求路劲：/app/resourceStoreUseRecord.SaveResourceStoreUseRecord
 * add by 吴学文 at 2022-08-08 14:39:58 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 资源仓库使用记录保存命令类
 * 该类负责处理资源仓库使用记录的保存操作，包括参数验证和数据持久化
 * 通过注解标识服务编码，与前端请求路径对应
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-08-08
 */
@Java110Cmd(serviceCode = "resourceStore.saveResourceStoreUseRecord")
public class SaveResourceStoreUseRecordCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveResourceStoreUseRecordCmd.class);

    /**
     * ID生成前缀，用于生成资源使用记录ID
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 资源仓库使用记录服务接口
     */
    @Autowired
    private IResourceStoreUseRecordV1InnerServiceSMO resourceStoreUseRecordV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求报文中的必填字段是否完整
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证资源ID是否存在
        Assert.hasKeyAndValue(reqJson, "resId", "请求报文中未包含resId");
        // 验证使用数量是否存在
        Assert.hasKeyAndValue(reqJson, "quantity", "请求报文中未包含quantity");
        // 验证单价是否存在
        Assert.hasKeyAndValue(reqJson, "unitPrice", "请求报文中未包含unitPrice");
        // 验证创建用户ID是否存在
        Assert.hasKeyAndValue(reqJson, "createUserId", "请求报文中未包含createUserId");
        // 验证创建用户姓名是否存在
        Assert.hasKeyAndValue(reqJson, "createUserName", "请求报文中未包含createUserName");
        // 验证仓库ID是否存在
        Assert.hasKeyAndValue(reqJson, "storeId", "请求报文中未包含storeId");
    }

    /**
     * 执行保存资源仓库使用记录命令
     * 将请求参数转换为实体对象并保存到数据库
     * 使用事务注解确保数据一致性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为资源仓库使用记录实体对象
        ResourceStoreUseRecordPo resourceStoreUseRecordPo = BeanConvertUtil.covertBean(reqJson, ResourceStoreUseRecordPo.class);
        // 生成资源使用记录ID
        resourceStoreUseRecordPo.setRsurId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务层保存数据到数据库
        int flag = resourceStoreUseRecordV1InnerServiceSMOImpl.saveResourceStoreUseRecord(resourceStoreUseRecordPo);

        // 检查保存结果，如果影响行数小于1则认为保存失败
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}