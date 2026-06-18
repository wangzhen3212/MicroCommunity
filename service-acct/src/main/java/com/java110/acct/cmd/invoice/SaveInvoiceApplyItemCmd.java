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
package com.java110.acct.cmd.invoice;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.acct.IInvoiceApplyItemV1InnerServiceSMO;
import com.java110.po.invoice.InvoiceApplyItemPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：保存发票申请项命令类
 * 服务编码：invoiceApplyItem.saveInvoiceApplyItem
 * 请求路劲：/app/invoiceApplyItem.SaveInvoiceApplyItem
 * add by 吴学文 at 2023-10-08 18:31:57 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * 功能描述：处理发票申请项保存操作的命令类，负责验证请求参数并保存发票申请项数据
 * 主要职责：
 * 1. 验证请求参数的完整性和有效性
 * 2. 将请求数据转换为实体对象
 * 3. 生成唯一标识并保存数据
 * 4. 处理保存结果并返回响应
 */
@Java110Cmd(serviceCode = "invoice.saveInvoiceApplyItem")
public class SaveInvoiceApplyItemCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveInvoiceApplyItemCmd.class);

    /**
     * 申请ID前缀，用于生成唯一标识
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 发票申请项服务接口，用于数据持久化操作
     */
    @Autowired
    private IInvoiceApplyItemV1InnerServiceSMO invoiceApplyItemV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求报文中是否包含必要的参数
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证申请ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "applyId", "请求报文中未包含applyId");
        // 验证项目类型参数是否存在
        Assert.hasKeyAndValue(reqJson, "itemType", "请求报文中未包含itemType");
        // 验证项目名称参数是否存在
        Assert.hasKeyAndValue(reqJson, "itemName", "请求报文中未包含itemName");
        // 验证项目对象ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "itemObjId", "请求报文中未包含itemObjId");
        // 验证项目金额参数是否存在
        Assert.hasKeyAndValue(reqJson, "itemAmount", "请求报文中未包含itemAmount");
        // 验证社区ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
    }

    /**
     * 执行保存发票申请项命令
     * 将请求数据转换为实体对象，生成唯一标识并保存到数据库
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为发票申请项实体对象
        InvoiceApplyItemPo invoiceApplyItemPo = BeanConvertUtil.covertBean(reqJson, InvoiceApplyItemPo.class);
        
        // 生成唯一的申请ID
        invoiceApplyItemPo.setApplyId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务层保存发票申请项数据
        int flag = invoiceApplyItemV1InnerServiceSMOImpl.saveInvoiceApplyItem(invoiceApplyItemPo);

        // 检查保存结果，如果影响行数小于1则保存失败
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}