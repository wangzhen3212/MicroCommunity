/**
 * 优惠券计算命令类
 * 
 * 功能：根据费用信息计算适用的优惠券规则，主要用于缴费时计算可用的优惠券
 * 通过查询费用配置对应的优惠规则，并获取规则下的优惠产品信息
 * 
 * 主要业务流程：
 * 1. 验证请求参数完整性
 * 2. 查询费用基本信息
 * 3. 根据费用配置查询适用的优惠规则
 * 4. 根据优惠规则查询对应的优惠产品
 * 5. 返回优惠产品列表给前端
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2024
 */
package com.java110.acct.cmd.coupon;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.coupon.CouponRuleCppsDto;
import com.java110.dto.coupon.CouponRuleFeeDto;
import com.java110.dto.fee.FeeDto;
import com.java110.intf.acct.ICouponRuleCppsV1InnerServiceSMO;
import com.java110.intf.acct.ICouponRuleFeeV1InnerServiceSMO;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.DateUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * API文档注解 - 优惠券计算接口
 * 提供缴费时计算可用优惠券的功能
 */
@Java110CmdDoc(title = "根据费用计算优惠券",
        description = "缴费时计算费用 是否有优惠券",
        httpMethod = "get",
        url = "http://{ip}:{port}/app/coupon.computePayFeeCoupon",
        resource = "acctDoc",
        author = "吴学文",
        serviceCode = "coupon.computePayFeeCoupon"
)

/**
 * 请求参数文档注解
 * 定义接口需要的三个核心参数
 */
@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "feeId", length = 30, remark = "费用ID"),
        @Java110ParamDoc(name = "cycle", length = 30, remark = "缴费周期"),
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
})

/**
 * 响应参数文档注解
 * 定义接口返回的数据结构
 */
@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "data", type = "Array", remark = "有效数据"),
                @Java110ParamDoc(parentNodeName = "data",name = "userId", type = "String", remark = "用户ID"),
                @Java110ParamDoc(parentNodeName = "data",name = "token", type = "String", remark = "临时票据"),
        }
)

/**
 * 请求响应示例文档注解
 * 提供接口调用示例
 */
@Java110ExampleDoc(
        reqBody="{'feeId':'123123','cycle':'1','communityId':'123123'}",
        resBody="{'code':0,'msg':'成功','data':{'userId':'123123','token':'123213'}}"
)

/**
 * 命令注解 - 注册服务代码
 */
@Java110Cmd(serviceCode = "coupon.computePayFeeCoupon")
public class ComputePayFeeCouponCmd extends Cmd {

    /**
     * 优惠规则费用服务接口 - 用于查询费用对应的优惠规则
     */
    @Autowired
    private ICouponRuleFeeV1InnerServiceSMO couponRuleFeeV1InnerServiceSMOImpl;

    /**
     * 优惠规则产品服务接口 - 用于查询规则对应的优惠产品
     */
    @Autowired
    private ICouponRuleCppsV1InnerServiceSMO couponRuleCppsV1InnerServiceSMOImpl;

    /**
     * 费用服务接口 - 用于查询费用基本信息
     */
    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否包含必要的字段，确保后续业务逻辑能够正常执行
     * 主要验证费用ID、小区ID和缴费周期三个关键参数
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证费用ID参数是否存在
        Assert.hasKeyAndValue(reqJson,"feeId","未包含费用");
        // 验证小区ID参数是否存在
        Assert.hasKeyAndValue(reqJson,"communityId","未包含小区");
        // 验证缴费周期参数是否存在
        Assert.hasKeyAndValue(reqJson,"cycles","未包含缴费周期");
    }

    /**
     * 执行优惠券计算命令
     * 
     * 根据费用信息查询适用的优惠券规则，并返回优惠产品信息
     * 主要流程：
     * 1. 查询费用基本信息
     * 2. 根据费用配置查询优惠规则
     * 3. 根据优惠规则查询优惠产品
     * 4. 返回优惠产品列表
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 命令执行异常
     * @throws ParseException 数据解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 构建费用查询条件
        FeeDto feeDto = new FeeDto();
        feeDto.setFeeId(reqJson.getString("feeId"));
        // 查询费用信息
        List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);

        // 验证费用存在且唯一
        Assert.listOnlyOne(feeDtos,"费用不存在");

        // 构建优惠规则查询条件
        CouponRuleFeeDto couponRuleFeeDto = new CouponRuleFeeDto();
        // 设置费用配置ID - 用于关联优惠规则
        couponRuleFeeDto.setFeeConfigId(feeDtos.get(0).getConfigId());
        // 设置当前时间，用于判断优惠规则是否在有效期内
        couponRuleFeeDto.setCurTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        // 设置小区ID - 限定优惠规则适用范围
        couponRuleFeeDto.setCommunityId(reqJson.getString("communityId"));
        // 设置缴费周期 - 用于匹配周期相关的优惠规则
        couponRuleFeeDto.setCycle(reqJson.getString("cycles"));
        // 查询适用的优惠规则
        List<CouponRuleFeeDto> couponRuleFeeDtos = couponRuleFeeV1InnerServiceSMOImpl.queryCouponRuleFees(couponRuleFeeDto);

        // 如果没有找到适用的优惠规则，返回空数组
        if(couponRuleFeeDtos == null || couponRuleFeeDtos.size()<1){
            context.setResponseEntity(ResultVo.createResponseEntity(new JSONArray()));
            return ;
        }

        // 收集所有适用的规则ID
        List<String> ruleIds = new ArrayList<>();
        for(CouponRuleFeeDto tmpCouponRuleFeeDto: couponRuleFeeDtos){
            ruleIds.add(tmpCouponRuleFeeDto.getRuleId());
        }

        // 构建优惠产品查询条件
        CouponRuleCppsDto couponRuleCppsDto = new CouponRuleCppsDto();
        // 设置规则ID数组 - 查询这些规则对应的优惠产品
        couponRuleCppsDto.setRuleIds(ruleIds.toArray(new String[ruleIds.size()]));
        // 查询优惠产品信息
        List<CouponRuleCppsDto> couponRuleCppsDtos = couponRuleCppsV1InnerServiceSMOImpl.queryCouponRuleCppss(couponRuleCppsDto);

        // 如果没有找到优惠产品，返回空数组
        if(couponRuleCppsDtos == null || couponRuleCppsDtos.size() < 1){
            context.setResponseEntity(ResultVo.createResponseEntity(new JSONArray()));
            return ;
        }
        
        // 返回查询到的优惠产品列表
        context.setResponseEntity(ResultVo.createResponseEntity(couponRuleCppsDtos));
    }
}