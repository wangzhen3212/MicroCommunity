package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.Java110CmdDoc;
import com.java110.doc.annotation.Java110ExampleDoc;
import com.java110.doc.annotation.Java110ParamDoc;
import com.java110.doc.annotation.Java110ParamsDoc;
import com.java110.doc.annotation.Java110ResponseDoc;
import com.java110.fee.bmo.fee.IPreGenerateFeeBMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * 查询预生成账单期次
 */
@Java110CmdDoc(title = "查询预生成账单期次",
        description = "自费用规则账期终点起按缴费周期试算最多12期，受费用规则最大时间约束",
        httpMethod = "get",
        url = "http://{ip}:{port}/app/fee.queryPreGenerateFeePeriods",
        resource = "feeDoc",
        author = "吴学文",
        serviceCode = "fee.queryPreGenerateFeePeriods",
        seq = 1
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "ruleId", length = 30, remark = "费用规则ID"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "data", type = "Object", length = -1, remark = "数据"),
                @Java110ParamDoc(parentNodeName = "data", name = "feeFlag", length = 12, remark = "费用标识 2006012 一次性 1003006 周期性"),
                @Java110ParamDoc(parentNodeName = "data", name = "feeName", length = 64, remark = "费用项名称"),
                @Java110ParamDoc(parentNodeName = "data", name = "paymentCycle", length = 12, remark = "缴费周期(月)"),
                @Java110ParamDoc(parentNodeName = "data", name = "endTime", length = 30, remark = "费用规则当前账期终点"),
                @Java110ParamDoc(parentNodeName = "data", name = "periods", type = "Array", length = -1, remark = "期次集合"),
                @Java110ParamDoc(parentNodeName = "data", name = "seq", type = "int", length = 11, remark = "期号，从1开始"),
                @Java110ParamDoc(parentNodeName = "data", name = "startTime", length = 30, remark = "账期开始时间"),
        }
)

@Java110ExampleDoc(
        reqBody = "http://ip:port/app/fee.queryPreGenerateFeePeriods?communityId=2022121921870161&ruleId=1120221219257000001",
        resBody = "{\"code\":0,\"msg\":\"成功\",\"data\":{\"feeFlag\":\"1003006\",\"feeName\":\"物业费\",\"paymentCycle\":\"1\",\"endTime\":\"2026-11-01 00:00:00\",\"periods\":[{\"seq\":1,\"startTime\":\"2026-11-01 00:00:00\",\"endTime\":\"2026-11-30 23:59:59\"}]}}"
)
@Java110Cmd(serviceCode = "fee.queryPreGenerateFeePeriods")
public class QueryPreGenerateFeePeriodsCmd extends Cmd {

    @Autowired
    private IPreGenerateFeeBMO preGenerateFeeBMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID");
        Assert.hasKeyAndValue(reqJson, "ruleId", "未包含费用规则ID");
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Map<String, Object> data = preGenerateFeeBMOImpl.queryPeriods(
                reqJson.getString("communityId"),
                reqJson.getString("ruleId"));
        context.setResponseEntity(ResultVo.createResponseEntity(data));
    }
}
