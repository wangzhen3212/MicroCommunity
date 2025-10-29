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
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.user.UserDto;
import com.java110.intf.fee.IPayFeeQrcodeV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.payFee.PayFeeQrcodePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 类表述：保存支付二维码信息命令类
 * 服务编码：payFeeQrcode.savePayFeeQrcode
 * 请求路劲：/app/payFeeQrcode.SavePayFeeQrcode
 * add by 吴学文 at 2023-09-04 23:24:48 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * 功能描述：处理支付二维码信息的保存操作，包括参数验证、数据转换和持久化存储
 */
@Java110Cmd(serviceCode = "payFeeQrcode.savePayFeeQrcode")
public class SavePayFeeQrcodeCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SavePayFeeQrcodeCmd.class);

    /**
     * ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 支付二维码服务接口
     */
    @Autowired
    private IPayFeeQrcodeV1InnerServiceSMO payFeeQrcodeV1InnerServiceSMOImpl;

    /**
     * 用户服务接口
     */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     *
     * @param event CMD事件对象
     * @param cmdDataFlowContext 数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填参数是否存在
        Assert.hasKeyAndValue(reqJson, "qrcodeName", "请求报文中未包含qrcodeName");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.hasKeyAndValue(reqJson, "queryWay", "请求报文中未包含queryWay");
        Assert.hasKeyAndValue(reqJson, "smsValidate", "请求报文中未包含smsValidate");
        Assert.hasKeyAndValue(reqJson, "customFee", "请求报文中未包含customFee");
        Assert.hasKeyAndValue(reqJson, "preFee", "请求报文中未包含preFee");
        Assert.hasKeyAndValue(reqJson, "feeType", "请求报文中未包含feeType");
        Assert.hasKeyAndValue(reqJson, "content", "请求报文中未包含content");
    }

    /**
     * 执行保存支付二维码命令
     * 处理支付二维码信息的保存逻辑，包括数据转换、ID生成和持久化操作
     *
     * @param event CMD事件对象
     * @param cmdDataFlowContext 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当保存失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 从上下文中获取当前用户ID
        String userId = CmdContextUtils.getUserId(cmdDataFlowContext);

        // 查询用户信息
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);
        // 验证用户存在且唯一
        Assert.listOnlyOne(userDtos, "员工不存在");

        // 将请求JSON转换为支付二维码PO对象
        PayFeeQrcodePo payFeeQrcodePo = BeanConvertUtil.covertBean(reqJson, PayFeeQrcodePo.class);
        // 生成支付二维码ID
        payFeeQrcodePo.setPfqId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        // 设置创建人员信息
        payFeeQrcodePo.setCreateStaffId(userId);
        payFeeQrcodePo.setCreateStaffName(userDtos.get(0).getName());
        
        // 调用服务保存支付二维码信息
        int flag = payFeeQrcodeV1InnerServiceSMOImpl.savePayFeeQrcode(payFeeQrcodePo);

        // 检查保存结果
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}