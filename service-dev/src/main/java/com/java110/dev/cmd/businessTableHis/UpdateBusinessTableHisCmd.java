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
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;


/**
 * 类表述：更新业务表历史记录
 * 服务编码：businessTableHis.UpdateBusinessTableHis
 * 请求路径：/app/businessTableHis.UpdateBusinessTableHis
 * 
 * 功能描述：
 * 该命令类用于处理业务表历史记录的更新操作，包括参数验证和数据更新
 * 
 * 创建信息：
 * add by 吴学文 at 2021-09-12 13:58:05 
 * mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 
 * 温馨提示：
 * 如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "businessTableHis.updateBusinessTableHis")
public class UpdateBusinessTableHisCmd extends Cmd {

    // 日志记录器
    private static Logger logger = LoggerFactory.getLogger(UpdateBusinessTableHisCmd.class);

    // 业务表历史记录服务接口
    @Autowired
    private IBusinessTableHisV1InnerServiceSMO businessTableHisV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 功能说明：
     * 验证请求参数是否完整和有效，确保更新操作的必要参数存在
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * 
     * @throws IllegalArgumentException 当必要参数缺失时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证hisId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "hisId", "hisId不能为空");
        // 注意：这里重复验证了hisId，可能是笔误，应该验证其他必要字段
        Assert.hasKeyAndValue(reqJson, "hisId", "hisId不能为空");
    }

    /**
     * 命令执行方法
     * 
     * 功能说明：
     * 执行业务表历史记录的更新操作，将请求数据转换为PO对象并调用服务层进行更新
     * 该方法使用事务注解确保数据一致性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * 
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 记录调试日志
        logger.debug("开始执行业务表历史记录更新操作，请求参数：{}", reqJson.toJSONString());

        // 将JSON对象转换为业务表历史记录PO对象
        BusinessTableHisPo businessTableHisPo = BeanConvertUtil.covertBean(reqJson, BusinessTableHisPo.class);
        
        // 调用服务层接口执行更新操作
        int flag = businessTableHisV1InnerServiceSMOImpl.updateBusinessTableHis(businessTableHisPo);

        // 检查更新结果
        if (flag < 1) {
            // 记录错误日志
            logger.error("更新业务表历史记录失败，hisId：{}", businessTableHisPo.getHisId());
            throw new CmdException("更新数据失败");
        }

        // 记录成功日志
        logger.debug("业务表历史记录更新成功，hisId：{}", businessTableHisPo.getHisId());
        
        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}