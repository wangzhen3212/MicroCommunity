package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.Java110CmdDoc;
import com.java110.doc.annotation.Java110ExampleDoc;
import com.java110.doc.annotation.Java110ParamDoc;
import com.java110.doc.annotation.Java110ParamsDoc;
import com.java110.doc.annotation.Java110ResponseDoc;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.fee.PreGenerateFeePeriodDto;
import com.java110.fee.bmo.fee.IPreGenerateFeeBMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 预生成费用账单
 */
@Java110CmdDoc(title = "预生成费用账单",
        description = "按勾选的连续期次生成周期性费用账单，一次性费用直接生成一张账单",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/fee.preGenerateFee",
        resource = "feeDoc",
        author = "吴学文",
        serviceCode = "fee.preGenerateFee",
        seq = 1
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "ruleId", length = 30, remark = "费用规则ID"),
        @Java110ParamDoc(name = "periods", type = "Array", length = -1, remark = "需出账的期次，一次性费用可为空，必须从第1期开始连续"),
        @Java110ParamDoc(parentNodeName = "periods", name = "startTime", length = 30, remark = "账期开始时间"),
        @Java110ParamDoc(parentNodeName = "periods", name = "endTime", length = 30, remark = "账期结束时间"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "data", type = "int", length = 11, remark = "生成的账单条数"),
        }
)

@Java110ExampleDoc(
        reqBody = "{\"communityId\":\"2022121921870161\",\"ruleId\":\"1120221219257000001\",\"periods\":[{\"startTime\":\"2026-11-01 00:00:00\",\"endTime\":\"2026-11-30 23:59:59\"}]}",
        resBody = "{\"code\":0,\"msg\":\"成功\",\"data\":1}"
)
@Java110Cmd(serviceCode = "fee.preGenerateFee")
public class PreGenerateFeeCmd extends Cmd {

    @Autowired
    private IPreGenerateFeeBMO preGenerateFeeBMOImpl;

    @Override
    @SuppressWarnings("unchecked")
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID");
        Assert.hasKeyAndValue(reqJson, "ruleId", "未包含费用规则ID");

        String communityId = reqJson.getString("communityId");
        String ruleId = reqJson.getString("ruleId");

        Map<String, Object> data = preGenerateFeeBMOImpl.queryPeriods(communityId, ruleId);

        //todo 一次性费用直接出账，无需校验期次
        if (FeeDto.FEE_FLAG_ONCE.equals(data.get("feeFlag"))) {
            return;
        }

        List<PreGenerateFeePeriodDto> allPeriods = null;
        Object periodsObj = data.get("periods");
        if (periodsObj != null) {
            allPeriods = (List<PreGenerateFeePeriodDto>) periodsObj;
        }

        preGenerateFeeBMOImpl.validatePeriods(allPeriods, convertPeriods(reqJson), ruleId);
    }

    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        int count = preGenerateFeeBMOImpl.preGenerate(
                reqJson.getString("communityId"),
                reqJson.getString("ruleId"),
                convertPeriods(reqJson));
        context.setResponseEntity(ResultVo.createResponseEntity(count));
    }

    /**
     * 解析前端提交的期次
     *
     * @param reqJson 请求报文
     * @return 期次集合
     */
    private List<PreGenerateFeePeriodDto> convertPeriods(JSONObject reqJson) {
        List<PreGenerateFeePeriodDto> periods = new ArrayList<>();
        JSONArray jsonArray = reqJson.getJSONArray("periods");
        if (jsonArray == null || jsonArray.isEmpty()) {
            return periods;
        }

        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject tmpJson = jsonArray.getJSONObject(i);
            if (tmpJson == null) {
                continue;
            }
            PreGenerateFeePeriodDto period = new PreGenerateFeePeriodDto();
            period.setSeq(tmpJson.getIntValue("seq"));
            period.setStartTime(tmpJson.getString("startTime"));
            period.setEndTime(tmpJson.getString("endTime"));
            if (StringUtil.isEmpty(period.getStartTime()) || StringUtil.isEmpty(period.getEndTime())) {
                continue;
            }
            periods.add(period);
        }
        return periods;
    }
}
