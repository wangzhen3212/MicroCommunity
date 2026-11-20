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
import com.java110.dto.contract.ContractFeeComputeDto;
import com.java110.dto.fee.FeeDto;
import com.java110.fee.bmo.fee.IComputeContractFeeBMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 合同费用试算（只读，不产生费用/账单）
 */
@Java110CmdDoc(title = "合同费用试算",
        description = "按费用项计费公式统一计算合同标的费用，支持房屋(3333)与商铺(4444)，只读不落库",
        httpMethod = "get",
        url = "http://{ip}:{port}/app/fee.computeContractFeeAmount",
        resource = "feeDoc",
        author = "吴学文",
        serviceCode = "fee.computeContractFeeAmount",
        seq = 1
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "configId", length = 30, remark = "费用项ID，mode=feeConfig 时必填"),
        @Java110ParamDoc(name = "payerObjType", length = 4, remark = "标的类型 3333 房屋 4444 商铺"),
        @Java110ParamDoc(name = "payerObjIds", length = 1000, remark = "标的ID，逗号分隔支持多个"),
        @Java110ParamDoc(name = "months", type = "int", length = 11, remark = "合同月数，由前端按起止日期换算后传入"),
        @Java110ParamDoc(name = "startTime", length = 30, remark = "合同生效日期 yyyy-MM-dd"),
        @Java110ParamDoc(name = "endTime", length = 30, remark = "合同到期日期 yyyy-MM-dd"),
        @Java110ParamDoc(name = "mode", length = 12, remark = "feeConfig 按费用项公式（默认）/ rent 按租金公式汇总"),
        @Java110ParamDoc(name = "cycle", type = "int", length = 11, remark = "期数，不传由后端按月数与缴费周期折算"),
        @Java110ParamDoc(name = "amount", length = 20, remark = "手工金额，仅手填类公式使用"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "data", type = "Object", length = -1, remark = "数据"),
                @Java110ParamDoc(parentNodeName = "data", name = "payerObjType", length = 4, remark = "标的类型"),
                @Java110ParamDoc(parentNodeName = "data", name = "feeName", length = 64, remark = "费用项名称"),
                @Java110ParamDoc(parentNodeName = "data", name = "computingFormula", length = 12, remark = "计费公式"),
                @Java110ParamDoc(parentNodeName = "data", name = "months", type = "int", length = 11, remark = "计算月数"),
                @Java110ParamDoc(parentNodeName = "data", name = "cycle", type = "int", length = 11, remark = "计算期数"),
                @Java110ParamDoc(parentNodeName = "data", name = "unitPrice", length = 20, remark = "单价（取自费用项配置）"),
                @Java110ParamDoc(parentNodeName = "data", name = "additionalAmount", length = 20, remark = "附加费（取自费用项配置）"),
                @Java110ParamDoc(parentNodeName = "data", name = "baseAmount", length = 20, remark = "结算基数合计"),
                @Java110ParamDoc(parentNodeName = "data", name = "amount", length = 20, remark = "试算金额合计"),
                @Java110ParamDoc(parentNodeName = "data", name = "remark", length = 500, remark = "计算说明"),
                @Java110ParamDoc(parentNodeName = "data", name = "details", type = "Array", length = -1, remark = "每个标的的试算明细"),
        }
)

@Java110ExampleDoc(
        reqBody = "http://ip:port/app/fee.computeContractFeeAmount?communityId=2022121921870161&configId=9220221219257000001&payerObjType=3333&payerObjIds=752022082030880010&months=12&startTime=2026-11-20&endTime=2027-11-19",
        resBody = "{\"code\":0,\"msg\":\"成功\",\"data\":{\"payerObjType\":\"3333\",\"feeName\":\"A区物业费\",\"computingFormula\":\"1001\",\"months\":12,\"cycle\":12,\"unitPrice\":\"1.30\",\"additionalAmount\":\"0.00\",\"baseAmount\":\"100.00\",\"amount\":\"1560.00\",\"remark\":\"建筑面积：100.00\\n单价：1.30\\n附加费：0.00\",\"details\":[{\"payerObjType\":\"3333\",\"objId\":\"752022082030880010\",\"objName\":\"1-1-1001\",\"builtUpArea\":\"100.00\",\"areaValue\":\"90.00\",\"rentValue\":\"100.00\",\"amount\":\"1560.00\",\"remark\":\"建筑面积：100.00\\n单价：1.30\\n附加费：0.00\"}]}}"
)
@Java110Cmd(serviceCode = "fee.computeContractFeeAmount")
public class ComputeContractFeeAmountCmd extends Cmd {

    private static final String MODE_FEE_CONFIG = "feeConfig";
    private static final String MODE_RENT = "rent";

    @Autowired
    private IComputeContractFeeBMO computeContractFeeBMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID");
        Assert.hasKeyAndValue(reqJson, "payerObjType", "未包含标的类型");
        Assert.hasKeyAndValue(reqJson, "payerObjIds", "未包含标的ID");
        Assert.hasKeyAndValue(reqJson, "startTime", "未包含合同生效日期");
        Assert.hasKeyAndValue(reqJson, "endTime", "未包含合同到期日期");

        String payerObjType = reqJson.getString("payerObjType");
        if (!FeeDto.PAYER_OBJ_TYPE_ROOM.equals(payerObjType) && !FeeDto.PAYER_OBJ_TYPE_SHOP.equals(payerObjType)) {
            throw new CmdException("不支持的缴费对象类型：" + payerObjType);
        }

        String mode = StringUtil.isEmpty(reqJson.getString("mode")) ? MODE_FEE_CONFIG : reqJson.getString("mode");
        if (MODE_FEE_CONFIG.equals(mode)) {
            Assert.hasKeyAndValue(reqJson, "configId", "未包含费用项ID");
        } else if (!MODE_RENT.equals(mode)) {
            throw new CmdException("不支持的试算模式：" + mode);
        }

        if (reqJson.containsKey("months") && !StringUtil.isEmpty(reqJson.getString("months"))) {
            Integer months = null;
            try {
                months = reqJson.getInteger("months");
            } catch (Exception e) {
                throw new CmdException("合同月数无效，请先选择合同到期日期");
            }
            if (months == null || months <= 0) {
                throw new CmdException("合同月数无效，请先选择合同到期日期");
            }
        }
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        ContractFeeComputeDto data = computeContractFeeBMOImpl.computeContractFeeAmount(reqJson);
        context.setResponseEntity(ResultVo.createResponseEntity(data));
    }
}
