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
package com.java110.fee.cmd.payFeeQrcode;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.fee.IPayFeeQrcodeV1InnerServiceSMO;
import com.java110.utils.cache.UrlCache;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.payFee.PayFeeQrcodeDto;

import java.util.List;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询
 * 服务编码：payFeeQrcode.listPayFeeQrcode
 * 请求路劲：/app/payFeeQrcode.ListPayFeeQrcode
 * add by 吴学文 at 2023-09-04 23:24:48 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 支付二维码查询命令类
 * 负责处理支付二维码列表查询请求，包括参数验证、数据查询和二维码URL生成
 * 服务编码：payFeeQrcode.listPayFeeQrcode
 * 请求路径：/app/payFeeQrcode.ListPayFeeQrcode
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023-09-04
 */
@Java110Cmd(serviceCode = "payFeeQrcode.listPayFeeQrcode")
public class ListPayFeeQrcodeCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListPayFeeQrcodeCmd.class);
    
    /**
     * 支付二维码服务接口
     */
    @Autowired
    private IPayFeeQrcodeV1InnerServiceSMO payFeeQrcodeV1InnerServiceSMOImpl;

    /**
     * 支付二维码页面路径常量
     */
    private static final String payQrCode = "/#/pages/fee/payQrCode";

    /**
     * 参数验证方法
     * 验证请求参数的有效性，包括分页参数和必填字段
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于处理请求和响应
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证小区ID参数是否为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 命令执行方法
     * 处理支付二维码列表查询业务逻辑，包括数据查询、二维码URL计算和响应构建
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为DTO对象
        PayFeeQrcodeDto payFeeQrcodeDto = BeanConvertUtil.covertBean(reqJson, PayFeeQrcodeDto.class);

        // 查询支付二维码总数
        int count = payFeeQrcodeV1InnerServiceSMOImpl.queryPayFeeQrcodesCount(payFeeQrcodeDto);

        List<PayFeeQrcodeDto> payFeeQrcodeDtos = null;

        if (count > 0) {
            // 查询支付二维码列表数据
            payFeeQrcodeDtos = payFeeQrcodeV1InnerServiceSMOImpl.queryPayFeeQrcodes(payFeeQrcodeDto);
            // 计算二维码访问地址
            computeQrCodeUrl(payFeeQrcodeDtos);
        } else {
            // 如果没有数据，返回空列表
            payFeeQrcodeDtos = new ArrayList<>();
        }

        // 构建分页结果对象
        // 计算总页数：总记录数/每页记录数，向上取整
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, payFeeQrcodeDtos);

        // 构建HTTP响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应到上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 计算支付二维码访问URL
     * 为每个支付二维码DTO对象生成完整的二维码访问地址
     *
     * @param payFeeQrcodeDtos 支付二维码数据传输对象列表
     */
    private void computeQrCodeUrl(List<PayFeeQrcodeDto> payFeeQrcodeDtos) {
        // 检查参数有效性
        if (payFeeQrcodeDtos == null || payFeeQrcodeDtos.size() < 1) {
            return;
        }
        
        // 获取基础URL地址
        String ownerUrl = UrlCache.getOwnerUrl();

        String qrcodeUrl = "";
        // 遍历所有支付二维码，为每个生成完整的访问URL
        for (PayFeeQrcodeDto payFeeQrcodeDto : payFeeQrcodeDtos) {
            // 构建二维码URL：基础URL + 页面路径 + 小区ID参数 + 二维码ID参数
            qrcodeUrl = ownerUrl + payQrCode + "?communityId=" + payFeeQrcodeDto.getCommunityId() + "&pfqId=" + payFeeQrcodeDto.getPfqId();
            // 设置二维码URL到DTO对象
            payFeeQrcodeDto.setQrCodeUrl(qrcodeUrl);
        }
    }
}