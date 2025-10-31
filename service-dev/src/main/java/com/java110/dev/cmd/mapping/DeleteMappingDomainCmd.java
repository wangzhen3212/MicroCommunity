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
package com.java110.dev.cmd.mapping;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.dev.IMappingDomainV1InnerServiceSMO;
import com.java110.po.mapping.MappingDomainPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：删除映射域命令类
 * 服务编码：mappingDomain.deleteMappingDomain
 * 请求路径：/app/mappingDomain.DeleteMappingDomain
 * 
 * 功能描述：该命令类用于处理映射域数据的删除操作，包括参数验证和业务逻辑执行
 * 
 * 作者信息：
 * add by 吴学文 at 2023-01-29 14:09:04 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "mapping.deleteMappingDomain")
public class DeleteMappingDomainCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteMappingDomainCmd.class);

    /**
     * 映射域服务接口
     * 用于调用映射域相关的数据操作
     */
    @Autowired
    private IMappingDomainV1InnerServiceSMO mappingDomainV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 功能：验证删除映射域所需的必要参数
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * 
     * 验证逻辑：
     * 1. 检查请求参数中是否包含必填字段"id"
     * 2. 如果缺少必要参数，将抛出参数验证异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含id字段
        Assert.hasKeyAndValue(reqJson, "id", "id不能为空");
    }

    /**
     * 命令执行方法
     * 
     * 功能：执行映射域数据的删除操作
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当删除操作失败时抛出命令异常
     * 
     * 执行流程：
     * 1. 将请求参数转换为映射域PO对象
     * 2. 调用服务层删除映射域数据
     * 3. 检查删除操作结果
     * 4. 设置响应结果
     * 
     * 事务说明：该方法使用@Java110Transactional注解，确保删除操作的原子性
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为MappingDomainPo对象
        MappingDomainPo mappingDomainPo = BeanConvertUtil.covertBean(reqJson, MappingDomainPo.class);
        
        // 调用服务层执行删除操作，返回受影响的行数
        int flag = mappingDomainV1InnerServiceSMOImpl.deleteMappingDomain(mappingDomainPo);

        // 检查删除操作是否成功
        if (flag < 1) {
            // 如果删除失败，抛出命令异常
            throw new CmdException("删除数据失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}