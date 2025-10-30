/**
 * 缴费赠送积分计算命令类
 * 
 * 该类负责处理缴费时计算赠送积分的业务逻辑，根据缴费信息、小区配置和积分规则
 * 计算用户缴费后应获得的积分数额
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2024
 */
package com.java110.acct.cmd.integral;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.smo.IComputeFeeSMO;
import com.java110.doc.annotation.*;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.integral.IntegralRuleConfigDto;
import com.java110.dto.integral.IntegralRuleFeeDto;
import com.java110.intf.acct.IIntegralRuleConfigV1InnerServiceSMO;
import com.java110.intf.acct.IIntegralRuleFeeV1InnerServiceSMO;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.DateUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Java110CmdDoc(title = "缴费赠送积分",
        description = "缴费时计算费用是否有积分",
        httpMethod = "get",
        url = "http://{ip}:{port}/app/integral.computePayFeeIntegral",
        resource = "acctDoc",
        author = "吴学文",
        serviceCode = "integral.computePayFeeIntegral"
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "feeId", length = 30, remark = "费用ID"),
        @Java110ParamDoc(name = "cycle", length = 30, remark = "缴费周期"),
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "amount", length = 30, remark = "缴费金额"),
        @Java110ParamDoc(name = "area", length = 30, remark = "缴费面积"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "data", type = "int", remark = "赠送积分数"),
        }
)

@Java110ExampleDoc(
        reqBody="{'feeId':'123123','cycle':'1','communityId':'123123','amount':'123123','area':'123123'}",
        resBody="{'code':0,'msg':'成功','data':1000}"
)
@Java110Cmd(serviceCode = "integral.computePayFeeIntegral")
public class ComputePayFeeIntegralCmd extends Cmd {

    @Autowired
    private IIntegralRuleFeeV1InnerServiceSMO integralRuleFeeV1InnerServiceSMOImpl;

    @Autowired
    private IIntegralRuleConfigV1InnerServiceSMO integralRuleConfigV1InnerServiceSMOImpl;

    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    @Autowired
    private IComputeFeeSMO computeFeeSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整，确保必要的参数都存在
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必要参数是否存在
        Assert.hasKeyAndValue(reqJson,"feeId","未包含费用");
        Assert.hasKeyAndValue(reqJson,"communityId","未包含小区");
        Assert.hasKeyAndValue(reqJson,"cycles","未包含缴费周期");
        Assert.hasKeyAndValue(reqJson,"amount","未包含缴费周期");
        Assert.hasKeyAndValue(reqJson,"area","未包含缴费周期");
    }

    /**
     * 执行积分计算命令
     * 
     * 根据缴费信息查询相关积分规则，计算应赠送的积分数额
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 命令执行异常
     * @throws ParseException 数据解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 查询费用信息
        FeeDto feeDto = new FeeDto();
        feeDto.setFeeId(reqJson.getString("feeId"));
        List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);

        // 验证费用是否存在且唯一
        Assert.listOnlyOne(feeDtos,"费用不存在");

        // 构建积分规则费用查询条件
        IntegralRuleFeeDto integralRuleFeeDto = new IntegralRuleFeeDto();
        integralRuleFeeDto.setCurTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A)); // 设置当前时间
        integralRuleFeeDto.setCommunityId(reqJson.getString("communityId")); // 设置小区ID
        integralRuleFeeDto.setCycle(reqJson.getString("cycles")); // 设置缴费周期
        List<IntegralRuleFeeDto> integralRuleFeeDtos = integralRuleFeeV1InnerServiceSMOImpl.queryIntegralRuleFees(integralRuleFeeDto);

        // 如果没有找到对应的积分规则费用配置，返回0积分
        if(integralRuleFeeDtos == null || integralRuleFeeDtos.size()<1){
            context.setResponseEntity(ResultVo.createResponseEntity(0));
            return ;
        }

        // 收集所有适用的规则ID
        List<String> ruleIds = new ArrayList<>();
        for(IntegralRuleFeeDto tmpCouponRuleFeeDto: integralRuleFeeDtos){
            ruleIds.add(tmpCouponRuleFeeDto.getRuleId());
        }

        // 根据规则ID查询积分规则配置
        IntegralRuleConfigDto integralRuleConfigDto = new IntegralRuleConfigDto();
        integralRuleConfigDto.setRuleIds(ruleIds.toArray(new String[ruleIds.size()]));
        List<IntegralRuleConfigDto> integralRuleConfigDtos = integralRuleConfigV1InnerServiceSMOImpl.queryIntegralRuleConfigs(integralRuleConfigDto);

        // 如果没有找到积分规则配置，返回0积分
        if(integralRuleConfigDtos == null || integralRuleConfigDtos.size() < 1){
            context.setResponseEntity(ResultVo.createResponseEntity(0));
            return ;
        }

        // 计算赠送的积分数额
        long quantity = computeIntegralQuantity(integralRuleConfigDtos,reqJson);
        // 设置响应结果
        context.setResponseEntity(ResultVo.createResponseEntity(quantity));
    }

    /**
     * 计算积分数量
     * 
     * 根据积分规则配置和请求参数计算应赠送的积分数额
     * 当前实现返回0，实际业务逻辑需要根据具体规则计算
     * 
     * @param integralRuleConfigDtos 积分规则配置列表
     * @param reqJson 请求参数JSON对象
     * @return 计算得到的积分数额
     */
    private long computeIntegralQuantity(List<IntegralRuleConfigDto> integralRuleConfigDtos,JSONObject reqJson) {
        long quantity = 0;
        // TODO: 实现具体的积分计算逻辑
        // 注释掉的代码示例：
        // for(IntegralRuleConfigDto integralRuleConfigDto:integralRuleConfigDtos){
        //     quantity += computeFeeSMOImpl.computeOneIntegralQuantity(integralRuleConfigDto,reqJson);
        // }
        return quantity;
    }
}