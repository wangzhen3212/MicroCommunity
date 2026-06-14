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
package com.java110.fee.cmd.payFeeBatch;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.payFee.PayFeeBatchDto;
import com.java110.intf.fee.IPayFeeBatchV1InnerServiceSMO;
import com.java110.po.payFee.PayFeeBatchPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * 类表述：取消费用申请
 * 服务编码：payFeeBatch.applyDeletePayFeeBatchCmd
 * 请求路劲：/app/payFeeBatch.applyDeletePayFeeBatchCmd
 * add by 吴学文 at 2021-09-22 18:00:14 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 费用批次申请删除命令类
 * 
 * 该类用于处理费用批次删除申请的业务逻辑，将费用批次状态更新为待审核状态
 * 实现费用批次删除申请的提交功能
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2021-09-22
 */
@Java110Cmd(serviceCode = "payFeeBatch.applyDeletePayFeeBatchCmd")
public class ApplyDeletePayFeeBatchCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(ApplyDeletePayFeeBatchCmd.class);

    /**
     * 费用批次服务接口
     */
    @Autowired
    private IPayFeeBatchV1InnerServiceSMO payFeeBatchV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整，确保必要的参数都存在且不为空
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证批次ID参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "batchId", "batchId不能为空");
        // 验证小区ID参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
        // 验证备注参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "remark", "remark不能为空");
    }

    /**
     * 执行命令方法
     * 
     * 处理费用批次删除申请的核心业务逻辑，将费用批次状态更新为待审核状态
     * 使用事务保证数据一致性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当更新数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为费用批次PO对象
        PayFeeBatchPo payFeeBatchPo = BeanConvertUtil.covertBean(reqJson, PayFeeBatchPo.class);
        // 设置费用批次状态为申请状态（待审核）
        payFeeBatchPo.setState(PayFeeBatchDto.STATE_APPLY);
        // 设置状态消息为"待审核"
        payFeeBatchPo.setMsg("待审核");
        
        // 调用服务更新费用批次信息
        int flag = payFeeBatchV1InnerServiceSMOImpl.updatePayFeeBatch(payFeeBatchPo);

        // 检查更新操作是否成功，如果影响行数小于1则视为失败
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}