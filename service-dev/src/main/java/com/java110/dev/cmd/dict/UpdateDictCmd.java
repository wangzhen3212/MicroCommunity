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
package com.java110.dev.cmd.dict;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.dev.IDictV1InnerServiceSMO;
import com.java110.po.dict.DictPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：更新字典数据命令类
 * 服务编码：dict.updateDict
 * 请求路径：/app/dict.UpdateDict
 * 
 * 功能描述：
 * 该命令类用于处理字典数据的更新操作，包括参数验证、数据转换和业务逻辑处理。
 * 通过注解标识服务编码，实现字典数据的更新功能。
 * 
 * 主要流程：
 * 1. 验证请求参数完整性
 * 2. 转换请求数据为实体对象
 * 3. 调用服务层更新字典数据
 * 4. 处理更新结果并返回响应
 * 
 * @author 吴学文 at 2022-07-06 20:03:58 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "dict.updateDict")
public class UpdateDictCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(UpdateDictCmd.class);

    /**
     * 字典服务接口
     * 用于调用字典相关的业务逻辑操作
     */
    @Autowired
    private IDictV1InnerServiceSMO dictV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 功能说明：
     * 验证请求参数是否完整和有效，确保必要的参数存在且符合要求
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * 
     * 验证规则：
     * - id参数必须存在且不能为空，用于标识要更新的字典记录
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证id参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "id", "id不能为空");
        
        // 可以在此处添加其他参数验证逻辑
        // 例如：验证字典名称、字典编码等参数的格式和有效性
    }

    /**
     * 命令执行方法
     * 
     * 功能说明：
     * 执行字典数据的更新操作，包括数据转换、业务逻辑处理和结果返回
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当更新操作失败时抛出命令异常
     * 
     * 执行流程：
     * 1. 将JSON数据转换为字典实体对象
     * 2. 调用服务层更新字典数据
     * 3. 检查更新结果，失败时抛出异常
     * 4. 设置成功响应结果
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 记录调试日志，输出请求参数
        logger.debug("开始处理字典更新请求，请求参数：{}", reqJson.toJSONString());

        // 将JSON请求数据转换为字典实体对象
        DictPo dictPo = BeanConvertUtil.covertBean(reqJson, DictPo.class);
        
        // 调用字典服务接口执行更新操作
        int flag = dictV1InnerServiceSMOImpl.updateDict(dictPo);

        // 检查更新结果，如果影响行数小于1表示更新失败
        if (flag < 1) {
            logger.error("字典数据更新失败，影响行数：{}，字典ID：{}", flag, dictPo.getId());
            throw new CmdException("更新数据失败");
        }

        // 记录成功日志
        logger.info("字典数据更新成功，字典ID：{}", dictPo.getId());
        
        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
    
    /**
     * 获取字典服务接口实例
     * 
     * @return 字典服务接口实例
     */
    public IDictV1InnerServiceSMO getDictV1InnerServiceSMOImpl() {
        return dictV1InnerServiceSMOImpl;
    }

    /**
     * 设置字典服务接口实例
     * 
     * @param dictV1InnerServiceSMOImpl 字典服务接口实例
     */
    public void setDictV1InnerServiceSMOImpl(IDictV1InnerServiceSMO dictV1InnerServiceSMOImpl) {
        this.dictV1InnerServiceSMOImpl = dictV1InnerServiceSMOImpl;
    }
}