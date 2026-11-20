package com.java110.store.cmd.contract;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.doc.annotation.Java110CmdDoc;
import com.java110.doc.annotation.Java110ExampleDoc;
import com.java110.doc.annotation.Java110ParamDoc;
import com.java110.doc.annotation.Java110ParamsDoc;
import com.java110.doc.annotation.Java110ResponseDoc;
import com.java110.po.contract.ContractFilePo;
import com.java110.po.contract.ContractPo;
import com.java110.store.bmo.contract.IUpdateContractBMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * 修改合同（审批中的合同由 BMO 拦截，返回可读提示）
 */
@Java110CmdDoc(title = "修改合同",
        description = "修改合同基本信息、关联房屋、收款计划与正文",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/contract.updateContract",
        resource = "contractDoc",
        author = "wuxw",
        serviceCode = "contract.updateContract",
        seq = 1
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "contractId", length = 30, remark = "合同ID"),
        @Java110ParamDoc(name = "contractName", length = 100, remark = "合同名称"),
        @Java110ParamDoc(name = "contractCode", length = 50, remark = "合同编号"),
        @Java110ParamDoc(name = "partyA", length = 200, remark = "甲方"),
        @Java110ParamDoc(name = "partyB", length = 200, remark = "乙方"),
        @Java110ParamDoc(name = "amount", length = 20, remark = "合同金额"),
        @Java110ParamDoc(name = "rooms", type = "Array", length = 0, remark = "关联房屋/商铺"),
        @Java110ParamDoc(name = "plans", type = "Array", length = 0, remark = "收款计划期次"),
        @Java110ParamDoc(name = "content", length = 0, remark = "合同正文"),
        @Java110ParamDoc(name = "templateContent", length = 0, remark = "含变量占位符的合同正文原文"),
        @Java110ParamDoc(name = "variables", type = "Object", length = 0, remark = "正文变量值"),
        @Java110ParamDoc(name = "contractFilePo", type = "Array", length = 0, remark = "合同附件"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

@Java110ExampleDoc(
        reqBody = "{\"contractId\":\"102026112000000001\",\"contractName\":\"2026年办公室租赁合同\",\"partyA\":\"XX物业\",\"partyB\":\"张三\",\"amount\":\"12000\"}",
        resBody = "{'code':0,'msg':'保存成功'}"
)
@Java110Cmd(serviceCode = "contract.updateContract")
public class UpdateContractCmd extends Cmd {

    @Autowired
    private IUpdateContractBMO updateContractBMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Assert.hasKeyAndValue(reqJson, "contractId", "请求报文中未包含合同ID");
        Assert.hasKeyAndValue(reqJson, "communityId", "必填，请填写小区信息");
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String storeId = CmdContextUtils.getStoreId(context);
        String userId = CmdContextUtils.getUserId(context);
        try {
            if (StringUtil.isEmpty(storeId)) {
                throw new CmdException("未获取到商户信息，请重新登录后重试");
            }
            reqJson.put("userId", userId);

            ContractPo contractPo = BeanConvertUtil.covertBean(reqJson, ContractPo.class);
            contractPo.setContractId(reqJson.getString("contractId"));
            contractPo.setStoreId(storeId);
            contractPo.setCommunityId(reqJson.getString("communityId"));
            contractPo.setContractFilePo(buildContractFiles(reqJson));
            context.setResponseEntity(updateContractBMOImpl.update(contractPo, reqJson));
        } catch (Exception e) {
            context.setResponseEntity(ResultVo.error("修改合同失败：" + e.getMessage()));
        }
    }

    /**
     * 组装合同附件，附件列表缺失按空集合处理
     */
    private List<ContractFilePo> buildContractFiles(JSONObject reqJson) {
        List<ContractFilePo> contractFilePos = new ArrayList<>();
        JSONArray contractFiles = reqJson.getJSONArray("contractFilePo");
        if (contractFiles == null || contractFiles.isEmpty()) {
            return contractFilePos;
        }
        for (int conFileIndex = 0; conFileIndex < contractFiles.size(); conFileIndex++) {
            JSONObject resourceStore = contractFiles.getJSONObject(conFileIndex);
            ContractFilePo contractFilePo = BeanConvertUtil.covertBean(resourceStore, ContractFilePo.class);
            contractFilePo.setContractFileId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_contractFileId));
            contractFilePos.add(contractFilePo);
        }
        return contractFilePos;
    }
}
