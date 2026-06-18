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
package com.java110.fee.cmd.receipt;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.fee.IAccountReceiptV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.account.AccountReceiptDto;

import java.util.List;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询
 * 服务编码：accountReceipt.listAccountReceipt
 * 请求路劲：/app/accountReceipt.ListAccountReceipt
 * add by 吴学文 at 2023-04-13 01:28:30 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 账户回单查询命令类
 * 负责处理账户回单列表查询请求，包括参数验证、数据查询和结果返回
 * 服务编码：receipt.listAccountReceipt
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023-04-13
 */
@Java110Cmd(serviceCode = "receipt.listAccountReceipt")
public class ListAccountReceiptCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListAccountReceiptCmd.class);
    
    /**
     * 账户回单服务接口
     */
    @Autowired
    private IAccountReceiptV1InnerServiceSMO accountReceiptV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性，包括分页信息和必要字段
     *
     * @param event 命令事件对象，包含事件相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页信息
        super.validatePageInfo(reqJson);
        // 验证属性信息
        super.validateProperty(cmdDataFlowContext);
        // 验证小区ID是否存在于请求参数中
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区信息");
    }

    /**
     * 命令执行方法
     * 处理账户回单查询业务逻辑，包括数据查询、结果封装和响应设置
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为账户回单数据传输对象
        AccountReceiptDto accountReceiptDto = BeanConvertUtil.covertBean(reqJson, AccountReceiptDto.class);

        // 如果请求中包含arIds参数且不为空，则设置到DTO中
        if(reqJson.containsKey("arIds") && !StringUtil.isEmpty(reqJson.getString("arIds"))){
            // 将逗号分隔的字符串转换为数组
            accountReceiptDto.setArIds(reqJson.getString("arIds").split(","));
        }

        // 查询账户回单总记录数
        int count = accountReceiptV1InnerServiceSMOImpl.queryAccountReceiptsCount(accountReceiptDto);

        List<AccountReceiptDto> accountReceiptDtos = null;

        // 根据查询结果数量决定返回的数据
        if (count > 0) {
            // 查询账户回单列表数据
            accountReceiptDtos = accountReceiptV1InnerServiceSMOImpl.queryAccountReceipts(accountReceiptDto);
        } else {
            // 如果没有数据，返回空列表
            accountReceiptDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 封装返回结果
        ResultVo resultVo = new ResultVo(totalPage, count, accountReceiptDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应实体到数据流上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}