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
package com.java110.dev.cmd.businessTableHis;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.dev.IBusinessTableHisV1InnerServiceSMO;
import com.java110.po.business.BusinessTableHisPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 类表述：删除业务表历史记录
 * 服务编码：businessTableHis.DeleteBusinessTableHis
 * 请求路径：/app/businessTableHis.DeleteBusinessTableHis
 * 
 * 功能说明：该命令类用于处理删除业务表历史记录的请求，包括参数验证和业务逻辑处理
 * 
 * 主要流程：
 * 1. 验证请求参数
 * 2. 转换请求数据为业务对象
 * 3. 调用服务层删除历史记录
 * 4. 处理删除结果并返回响应
 * 
 * @author 吴学文 at 2021-09-12 13:58:05 
 * mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "businessTableHis.deleteBusinessTableHis")
public class DeleteBusinessTableHisCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteBusinessTableHisCmd.class);

    /**
     * 业务表历史记录服务接口
     * 用于调用业务表历史记录的相关操作
     */
    @Autowired
    private IBusinessTableHisV1InnerServiceSMO businessTableHisV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 功能说明：验证请求参数是否完整和有效
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文，包含请求和响应信息
     * @param reqJson 请求参数的JSON对象
     * 
     * @throws IllegalArgumentException 当参数验证失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证hisId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "hisId", "hisId不能为空");
        // 重复验证确保参数完整性（这里可能是重复验证，实际可根据需求调整）
        Assert.hasKeyAndValue(reqJson, "hisId", "hisId不能为空");
        
        // 可以在这里添加更多的参数验证逻辑
        // 例如：验证hisId的格式、长度等
    }

    /**
     * 命令执行方法
     * 
     * 功能说明：执行删除业务表历史记录的核心业务逻辑
     * 使用@Java110Transactional注解确保事务性操作
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出命令异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        
        // 记录调试日志
        logger.debug("开始处理删除业务表历史记录请求，请求参数：{}", reqJson.toJSONString());

        // 将JSON请求参数转换为业务对象
        BusinessTableHisPo businessTableHisPo = BeanConvertUtil.covertBean(reqJson, BusinessTableHisPo.class);
        
        // 调用服务层接口删除业务表历史记录
        int flag = businessTableHisV1InnerServiceSMOImpl.deleteBusinessTableHis(businessTableHisPo);

        // 检查删除操作是否成功
        if (flag < 1) {
            // 记录错误日志
            logger.error("删除业务表历史记录失败，hisId：{}", reqJson.getString("hisId"));
            throw new CmdException("删除数据失败");
        }

        // 记录成功日志
        logger.info("删除业务表历史记录成功，hisId：{}", reqJson.getString("hisId"));
        
        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
        
        // 记录调试日志
        logger.debug("删除业务表历史记录请求处理完成");
    }
}