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
package com.java110.acct.cmd.payment;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.acct.IPaymentPoolConfigV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.payment.PaymentPoolConfigDto;
import java.util.List;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询
 * 服务编码：paymentPoolConfig.listPaymentPoolConfig
 * 请求路劲：/app/paymentPoolConfig.ListPaymentPoolConfig
 * add by 吴学文 at 2023-10-25 12:02:30 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 支付池配置查询命令类
 * 
 * 该类用于处理支付池配置列表查询请求，负责验证请求参数并执行查询操作
 * 通过调用支付池配置服务接口获取数据，并返回分页查询结果
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023-10-25
 */
@Java110Cmd(serviceCode = "payment.listPaymentPoolConfig")
public class ListPaymentPoolConfigCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListPaymentPoolConfigCmd.class);
    
    /**
     * 支付池配置服务接口
     */
    @Autowired
    private IPaymentPoolConfigV1InnerServiceSMO paymentPoolConfigV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，包括分页参数和必填字段
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证必填参数communityId是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
    }

    /**
     * 执行命令方法
     * 
     * 处理支付池配置查询业务逻辑，包括数据查询、结果封装和响应设置
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当业务处理过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为支付池配置数据传输对象
        PaymentPoolConfigDto paymentPoolConfigDto = BeanConvertUtil.covertBean(reqJson, PaymentPoolConfigDto.class);

        // 查询支付池配置总记录数
        int count = paymentPoolConfigV1InnerServiceSMOImpl.queryPaymentPoolConfigsCount(paymentPoolConfigDto);

        List<PaymentPoolConfigDto> paymentPoolConfigDtos = null;

        // 根据查询结果数量决定返回数据
        if (count > 0) {
            // 查询支付池配置列表数据
            paymentPoolConfigDtos = paymentPoolConfigV1InnerServiceSMOImpl.queryPaymentPoolConfigs(paymentPoolConfigDto);
        } else {
            // 无数据时返回空列表
            paymentPoolConfigDtos = new ArrayList<>();
        }

        // 计算总页数并封装返回结果
        // 使用Math.ceil向上取整确保分页正确
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, paymentPoolConfigDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应结果到数据流上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}