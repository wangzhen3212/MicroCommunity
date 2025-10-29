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
package com.java110.fee.cmd.tempCarFeeConfigAttr;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.fee.ITempCarFeeConfigAttrV1InnerServiceSMO;
import com.java110.po.tempCarFee.TempCarFeeConfigAttrPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：保存临时车费用配置属性命令类
 * 服务编码：tempCarFeeConfigAttr.saveTempCarFeeConfigAttr
 * 请求路劲：/app/tempCarFeeConfigAttr.SaveTempCarFeeConfigAttr
 * add by 吴学文 at 2022-06-27 13:39:52 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * 功能描述：处理临时车费用配置属性的保存操作，包括参数验证和业务逻辑处理
 * 主要职责：
 * 1. 验证请求参数完整性
 * 2. 将请求数据转换为持久化对象
 * 3. 生成唯一属性ID
 * 4. 调用服务层保存数据
 * 5. 处理保存结果并返回响应
 */
@Java110Cmd(serviceCode = "tempCarFeeConfigAttr.saveTempCarFeeConfigAttr")
public class SaveTempCarFeeConfigAttrCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveTempCarFeeConfigAttrCmd.class);

    /**
     * 属性ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 临时车费用配置属性服务接口
     */
    @Autowired
    private ITempCarFeeConfigAttrV1InnerServiceSMO tempCarFeeConfigAttrV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求报文中必须包含的字段
     *
     * @param event 命令事件对象，包含事件相关信息
     * @param cmdDataFlowContext 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求报文中必须包含configId字段
        Assert.hasKeyAndValue(reqJson, "configId", "请求报文中未包含configId");
        // 验证请求报文中必须包含communityId字段
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        // 验证请求报文中必须包含specCd字段
        Assert.hasKeyAndValue(reqJson, "specCd", "请求报文中未包含specCd");
        // 验证请求报文中必须包含value字段
        Assert.hasKeyAndValue(reqJson, "value", "请求报文中未包含value");
    }

    /**
     * 执行保存临时车费用配置属性命令
     * 将请求数据转换为持久化对象并保存到数据库
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为临时车费用配置属性持久化对象
        TempCarFeeConfigAttrPo tempCarFeeConfigAttrPo = BeanConvertUtil.covertBean(reqJson, TempCarFeeConfigAttrPo.class);
        
        // 生成唯一的属性ID
        tempCarFeeConfigAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务层保存数据，返回影响的行数
        int flag = tempCarFeeConfigAttrV1InnerServiceSMOImpl.saveTempCarFeeConfigAttr(tempCarFeeConfigAttrPo);

        // 检查保存结果，如果影响行数小于1表示保存失败
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}