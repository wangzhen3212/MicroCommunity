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
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.file.FileRelDto;
import com.java110.intf.acct.IInvoiceApplyV1InnerServiceSMO;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.invoice.InvoiceApplyDto;

import java.util.List;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询
 * 服务编码：invoiceApply.listInvoiceApply
 * 请求路劲：/app/invoiceApply.ListInvoiceApply
 * add by 吴学文 at 2023-10-08 16:26:34 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 发票申请列表查询命令类
 * 
 * 该类负责处理发票申请列表查询请求，包括参数验证、数据查询和结果返回等功能。
 * 主要功能包括：
 * 1. 验证查询参数的有效性
 * 2. 查询发票申请列表数据
 * 3. 查询关联的电子发票文件
 * 4. 返回分页查询结果
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023-10-08
 */
@Java110Cmd(serviceCode = "invoice.listInvoiceApply")
public class ListInvoiceApplyCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListInvoiceApplyCmd.class);
    
    /**
     * 发票申请服务接口
     */
    @Autowired
    private IInvoiceApplyV1InnerServiceSMO invoiceApplyV1InnerServiceSMOImpl;

    /**
     * 文件关联服务接口
     */
    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，包括分页参数和必填字段的验证
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证小区ID不能为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行命令方法
     * 
     * 处理发票申请列表查询的主要业务逻辑，包括：
     * 1. 转换请求参数为DTO对象
     * 2. 查询发票申请总数
     * 3. 查询发票申请列表数据
     * 4. 查询电子发票文件信息
     * 5. 构建并返回响应结果
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为发票申请DTO对象
        InvoiceApplyDto invoiceApplyDto = BeanConvertUtil.covertBean(reqJson, InvoiceApplyDto.class);

        // 查询发票申请总数
        int count = invoiceApplyV1InnerServiceSMOImpl.queryInvoiceApplysCount(invoiceApplyDto);

        List<InvoiceApplyDto> invoiceApplyDtos = null;

        // 根据查询结果数量决定返回数据
        if (count > 0) {
            // 查询发票申请列表数据
            invoiceApplyDtos = invoiceApplyV1InnerServiceSMOImpl.queryInvoiceApplys(invoiceApplyDto);
        } else {
            // 无数据时返回空列表
            invoiceApplyDtos = new ArrayList<>();
        }

        // 查询电子发票文件信息
        queryInvoicePhoto(invoiceApplyDtos);

        // 计算总页数并构建结果对象
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, invoiceApplyDtos);

        // 构建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应数据到上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 查询电子发票文件方法
     * 
     * 根据发票申请ID查询关联的电子发票文件信息，并将文件路径设置到发票申请DTO中
     * 注意：该方法仅在查询单条记录时生效
     * 
     * @param invoiceApplyDtos 发票申请数据列表
     */
    private void queryInvoicePhoto(List<InvoiceApplyDto> invoiceApplyDtos) {
        // 仅当查询结果为单条记录时才查询电子发票
        if (invoiceApplyDtos == null || invoiceApplyDtos.size() != 1) {
            return;
        }

        // 构建文件查询条件
        FileRelDto fileRelDto = new FileRelDto();
        // 设置查询对象ID为申请ID
        fileRelDto.setObjId(invoiceApplyDtos.get(0).getApplyId());
        // 设置关联类型为电子发票
        fileRelDto.setRelTypeCd(FileRelDto.REL_TYPE_CE_INVOICE);
        // 查询文件关联信息
        List<FileRelDto> fileRelDtos = fileRelInnerServiceSMOImpl.queryFileRels(fileRelDto);

        // 如果存在关联文件，将文件路径设置到发票申请DTO中
        if (fileRelDtos != null && !fileRelDtos.isEmpty()) {
            List<String> urls = new ArrayList<>();
            // 遍历所有文件关联记录，收集文件路径
            for (FileRelDto fileRel : fileRelDtos) {
                urls.add(fileRel.getFileRealName());
            }
            // 将文件路径列表设置到发票申请DTO中
            invoiceApplyDtos.get(0).setUrls(urls);
        }
    }
}