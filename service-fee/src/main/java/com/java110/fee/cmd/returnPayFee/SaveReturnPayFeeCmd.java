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
package com.java110.fee.cmd.returnPayFee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.fee.FeeDetailDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.payFee.ReturnPayFeeDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.fee.IFeeDetailInnerServiceSMO;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.intf.fee.IPayFeeDetailV1InnerServiceSMO;
import com.java110.intf.fee.IReturnPayFeeV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.fee.PayFeeDetailPo;
import com.java110.po.payFee.ReturnPayFeePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 类表述：保存退费申请信息
 * 服务编码：returnPayFee.saveReturnPayFee
 * 请求路劲：/app/returnPayFee.SaveReturnPayFee
 * add by 吴学文 at 2022-02-21 12:20:03 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "returnPayFee.saveReturnPayFee")
public class SaveReturnPayFeeCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveReturnPayFeeCmd.class);

    /**
     * 退费ID前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 退费服务接口
     */
    @Autowired
    private IReturnPayFeeV1InnerServiceSMO returnPayFeeV1InnerServiceSMOImpl;

    /**
     * 费用服务接口
     */
    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    /**
     * 缴费明细服务接口
     */
    @Autowired
    private IPayFeeDetailV1InnerServiceSMO payFeeDetailNewV1InnerServiceSMOImpl;

    /**
     * 费用明细服务接口
     */
    @Autowired
    private IFeeDetailInnerServiceSMO feeDetailInnerServiceSMOImpl;

    /**
     * 用户服务接口
     */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     *
     * @param event              命令事件
     * @param cmdDataFlowContext 数据流上下文
     * @param reqJson            请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必要参数是否存在
        Assert.hasKeyAndValue(reqJson, "feeId", "请求报文中未包含feeId");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.hasKeyAndValue(reqJson, "detailId", "请求报文中未包含detailId");
        Assert.hasKeyAndValue(reqJson, "reason", "请求报文中未包含reason");

        // 验证缴费记录是否存在
        FeeDetailDto feeDetailDto = new FeeDetailDto();
        feeDetailDto.setCommunityId(reqJson.getString("communityId"));
        feeDetailDto.setDetailId(reqJson.getString("detailId"));
        feeDetailDto.setFeeId(reqJson.getString("feeId"));
        List<FeeDetailDto> feeDetailDtos = feeDetailInnerServiceSMOImpl.queryFeeDetails(feeDetailDto);

        // 确保只存在一条匹配的缴费记录
        Assert.listOnlyOne(feeDetailDtos, "不存在该缴费记录");
    }

    /**
     * 执行退费申请保存操作
     * 处理退费申请的业务逻辑，包括数据验证、退费记录创建和缴费明细更新
     *
     * @param event              命令事件
     * @param cmdDataFlowContext 数据流上下文
     * @param reqJson            请求参数JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 获取当前操作用户ID
        String userId = cmdDataFlowContext.getReqHeaders().get("user-id");

        // 验证用户是否存在
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);
        Assert.listOnlyOne(userDtos, "用户不存在");

        // 验证费用信息是否存在
        FeeDto feeDto = new FeeDto();
        feeDto.setCommunityId(reqJson.getString("communityId"));
        feeDto.setFeeId(reqJson.getString("feeId"));
        List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);
        Assert.listOnlyOne(feeDtos, "不存在该费用");

        // 验证费用明细信息
        FeeDetailDto feeDetailDto = new FeeDetailDto();
        feeDetailDto.setDetailId(reqJson.getString("detailId"));
        List<FeeDetailDto> feeDetailDtos = feeDetailInnerServiceSMOImpl.queryFeeDetails(feeDetailDto);
        Assert.listOnlyOne(feeDetailDtos, "未找到需要修改的活动 或多条数据");

        // 设置退费申请相关参数
        reqJson.put("configId", feeDtos.get(0).getConfigId());
        reqJson.put("feeTypeCd", feeDtos.get(0).getFeeTypeCd());

        // 创建退费申请对象并设置属性
        ReturnPayFeePo returnPayFeePo = BeanConvertUtil.covertBean(reqJson, ReturnPayFeePo.class);
        returnPayFeePo.setReturnFeeId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID)); // 生成退费ID
        returnPayFeePo.setState(ReturnPayFeeDto.STATE_WAIT); // 设置状态为待处理
        returnPayFeePo.setCycles((reqJson.getDouble("cycles") * -1) + ""); // 周期数取负值
        returnPayFeePo.setReceivableAmount((reqJson.getDouble("receivableAmount") * -1) + ""); // 应收金额取负值
        returnPayFeePo.setReceivedAmount((reqJson.getDouble("receivedAmount") * -1) + ""); // 实收金额取负值
        // 设置支付方式，使用原缴费记录的支付方式
        returnPayFeePo.setPrimeRate(feeDetailDtos.get(0).getPrimeRate());
        returnPayFeePo.setApplyPersonId(userId); // 设置申请人ID
        returnPayFeePo.setApplyPersonName(userDtos.get(0).getName()); // 设置申请人姓名

        // 保存退费申请记录
        int flag = returnPayFeeV1InnerServiceSMOImpl.saveReturnPayFee(returnPayFeePo);
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置缴费明细状态为已完成
        reqJson.put("state", "1000");

        // 更新缴费明细信息
        JSONObject businessReturnPayFee = new JSONObject();
        businessReturnPayFee.putAll(BeanConvertUtil.beanCovertMap(feeDetailDtos.get(0)));
        businessReturnPayFee.putAll(reqJson);
        PayFeeDetailPo payFeeDetailPo = BeanConvertUtil.covertBean(businessReturnPayFee, PayFeeDetailPo.class);

        flag = payFeeDetailNewV1InnerServiceSMOImpl.updatePayFeeDetailNew(payFeeDetailPo);
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 返回成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}