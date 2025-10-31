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
 * 类表述：更新映射域信息
 * 服务编码：mappingDomain.updateMappingDomain
 * 请求路径：/app/mappingDomain.UpdateMappingDomain
 * 
 * 功能描述：该命令类用于处理映射域信息的更新操作，包括参数验证和业务逻辑处理
 * 
 * @author 吴学文 at 2023-01-29 14:09:04 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "mapping.updateMappingDomain")
public class UpdateMappingDomainCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(UpdateMappingDomainCmd.class);

    /**
     * 映射域服务接口
     * 用于调用映射域相关的业务逻辑操作
     */
    @Autowired
    private IMappingDomainV1InnerServiceSMO mappingDomainV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 功能说明：验证请求参数是否完整和有效
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param cmdDataFlowContext 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * 
     * @throws IllegalArgumentException 当必要参数缺失时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含id字段
        Assert.hasKeyAndValue(reqJson, "id", "id不能为空");
        
        // 可以在此处添加其他参数验证逻辑
        // 例如：验证字段格式、长度、取值范围等
    }

    /**
     * 命令执行方法
     * 
     * 功能说明：执行映射域信息的更新操作
     * 使用@Java110Transactional注解确保操作的事务性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * 
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 记录调试日志
        logger.debug("开始更新映射域信息，请求参数：{}", reqJson.toJSONString());

        // 将JSON对象转换为映射域POJO对象
        MappingDomainPo mappingDomainPo = BeanConvertUtil.covertBean(reqJson, MappingDomainPo.class);
        
        // 调用服务层接口执行更新操作
        int flag = mappingDomainV1InnerServiceSMOImpl.updateMappingDomain(mappingDomainPo);

        // 检查更新结果
        if (flag < 1) {
            // 记录错误日志
            logger.error("更新映射域信息失败，影响行数：{}", flag);
            throw new CmdException("更新数据失败");
        }

        // 记录成功日志
        logger.info("映射域信息更新成功，ID：{}", mappingDomainPo.getId());
        
        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}