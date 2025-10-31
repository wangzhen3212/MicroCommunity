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
import com.java110.core.factory.GenerateCodeFactory;
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
 * 类表述：保存映射域信息
 * 服务编码：mappingDomain.saveMappingDomain
 * 请求路径：/app/mappingDomain.SaveMappingDomain
 * add by 吴学文 at 2023-01-29 14:09:04 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "mapping.saveMappingDomain")
public class SaveMappingDomainCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveMappingDomainCmd.class);

    /**
     * ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 映射域服务接口
     */
    @Autowired
    private IMappingDomainV1InnerServiceSMO mappingDomainV1InnerServiceSMOImpl;

    /**
     * 参数校验方法
     * 
     * @param event 命令事件
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * 
     * 功能说明：
     * 1. 校验请求参数中必须包含domain字段
     * 2. 校验请求参数中必须包含seq字段
     * 3. 如果参数缺失，会抛出相应的异常信息
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 校验domain字段不能为空
        Assert.hasKeyAndValue(reqJson, "domain", "请求报文中未包含domain");
        // 校验seq字段不能为空
        Assert.hasKeyAndValue(reqJson, "seq", "请求报文中未包含seq");
    }

    /**
     * 业务处理方法
     * 
     * @param event 命令事件
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * 
     * 功能说明：
     * 1. 将请求参数转换为映射域实体对象
     * 2. 生成唯一ID
     * 3. 调用服务保存映射域信息
     * 4. 处理保存结果，失败时抛出异常
     * 5. 设置响应结果
     * 
     * 注解说明：
     * @Java110Transactional 表示该方法需要事务管理
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON参数转换为映射域实体对象
        MappingDomainPo mappingDomainPo = BeanConvertUtil.covertBean(reqJson, MappingDomainPo.class);
        
        // 生成唯一ID
        mappingDomainPo.setId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务保存映射域信息
        int flag = mappingDomainV1InnerServiceSMOImpl.saveMappingDomain(mappingDomainPo);

        // 检查保存结果，如果影响行数小于1则认为保存失败
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}