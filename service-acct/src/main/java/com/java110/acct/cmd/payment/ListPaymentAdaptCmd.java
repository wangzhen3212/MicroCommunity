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
import com.java110.intf.acct.IPaymentAdaptV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.payment.PaymentAdaptDto;
import java.util.List;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询支付适配器列表命令类
 * 服务编码：paymentAdapt.listPaymentAdapt
 * 请求路劲：/app/paymentAdapt.ListPaymentAdapt
 * 功能描述：该命令类用于处理支付适配器列表查询请求，负责查询支付适配器信息并返回分页结果
 * add by 吴学文 at 2023-10-26 10:50:22 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "payment.listPaymentAdapt")
public class ListPaymentAdaptCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(ListPaymentAdaptCmd.class);
    
    /**
     * 支付适配器服务接口
     */
    @Autowired
    private IPaymentAdaptV1InnerServiceSMO paymentAdaptV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 功能描述：验证请求参数的有效性，主要验证分页参数
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息验证方法，验证分页参数
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 功能描述：执行支付适配器列表查询业务逻辑，包括数据查询、分页处理和响应构建
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为支付适配器数据传输对象
        PaymentAdaptDto paymentAdaptDto = BeanConvertUtil.covertBean(reqJson, PaymentAdaptDto.class);

        // 查询支付适配器总记录数
        int count = paymentAdaptV1InnerServiceSMOImpl.queryPaymentAdaptsCount(paymentAdaptDto);

        // 声明支付适配器列表
        List<PaymentAdaptDto> paymentAdaptDtos = null;

        // 根据查询结果数量判断是否查询数据
        if (count > 0) {
            // 查询数量大于0时，执行支付适配器列表查询
            paymentAdaptDtos = paymentAdaptV1InnerServiceSMOImpl.queryPaymentAdapts(paymentAdaptDto);
        } else {
            // 查询数量为0时，返回空列表
            paymentAdaptDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页显示行数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 构建返回结果对象，包含总页数、总记录数和数据列表
        ResultVo resultVo = new ResultVo(totalPage, count, paymentAdaptDtos);

        // 构建HTTP响应实体，设置响应状态为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到命令数据流上下文中
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}