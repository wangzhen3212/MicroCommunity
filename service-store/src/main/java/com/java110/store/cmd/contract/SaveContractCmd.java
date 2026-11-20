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
import com.java110.dto.contract.ContractDto;
import com.java110.intf.store.IContractInnerServiceSMO;
import com.java110.po.contract.ContractFilePo;
import com.java110.po.contract.ContractPo;
import com.java110.store.bmo.contract.ISaveContractBMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.NumberUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * 新增合同（草稿 state=10，正式提交走两级审批）
 */
@Java110CmdDoc(title = "新增合同",
        description = "新增合同，state=10 保存草稿，其它状态提交法务校对/领导审核",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/contract.saveContract",
        resource = "contractDoc",
        author = "wuxw",
        serviceCode = "contract.saveContract",
        seq = 1
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "contractName", length = 100, remark = "合同名称"),
        @Java110ParamDoc(name = "contractCode", length = 50, remark = "合同编号，为空时自动生成"),
        @Java110ParamDoc(name = "templateId", length = 30, remark = "合同模板ID"),
        @Java110ParamDoc(name = "category", length = 12, remark = "合同分类 1房屋出租 2采购 3维保"),
        @Java110ParamDoc(name = "partyA", length = 200, remark = "甲方"),
        @Java110ParamDoc(name = "partyB", length = 200, remark = "乙方"),
        @Java110ParamDoc(name = "amount", length = 20, remark = "合同金额"),
        @Java110ParamDoc(name = "startTime", length = 20, remark = "生效日期"),
        @Java110ParamDoc(name = "endTime", length = 20, remark = "到期日期"),
        @Java110ParamDoc(name = "state", length = 4, remark = "合同状态，10 草稿，其它为提交审批"),
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
        reqBody = "{\"contractName\":\"2026年办公室租赁合同\",\"category\":\"1\",\"partyA\":\"XX物业\",\"partyB\":\"张三\",\"amount\":\"12000\",\"startTime\":\"2026-01-01\",\"endTime\":\"2026-12-31\",\"state\":\"10\"}",
        resBody = "{'code':0,'msg':'保存成功'}"
)
@Java110Cmd(serviceCode = "contract.saveContract")
public class SaveContractCmd extends Cmd {

    @Autowired
    private ISaveContractBMO saveContractBMOImpl;

    @Autowired
    private IContractInnerServiceSMO contractInnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Assert.hasKeyAndValue(reqJson, "contractName", "请求报文中未包含合同名称");
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
            boolean draft = ContractDto.STATE_DRAFT.equals(reqJson.getString("state"));
            if (!draft) {
                validateSubmit(reqJson);
            }
            if (StringUtil.isEmpty(reqJson.getString("contractCode"))) {
                reqJson.put("contractCode", generateContractCode(storeId, reqJson.getString("category")));
            }
            reqJson.put("userId", userId);

            ContractPo contractPo = BeanConvertUtil.covertBean(reqJson, ContractPo.class);
            contractPo.setStoreId(storeId);
            contractPo.setCommunityId(reqJson.getString("communityId"));
            contractPo.setStartUserId(userId);
            if (StringUtil.isEmpty(contractPo.getContractParentId())) {
                contractPo.setContractParentId("-1");
            }
            if (StringUtil.isEmpty(contractPo.getSigningTime())) {
                contractPo.setSigningTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
            }
            contractPo.setContractFilePo(buildContractFiles(reqJson));
            context.setResponseEntity(saveContractBMOImpl.save(contractPo, reqJson));
        } catch (Exception e) {
            context.setResponseEntity(ResultVo.error("保存合同失败：" + e.getMessage()));
        }
    }

    /**
     * 正式提交时的必填校验（草稿允许缺省）
     */
    private void validateSubmit(JSONObject reqJson) throws CmdException {
        Assert.hasKeyAndValue(reqJson, "partyA", "请求报文中未包含甲方");
        Assert.hasKeyAndValue(reqJson, "partyB", "请求报文中未包含乙方");
        Assert.hasKeyAndValue(reqJson, "startTime", "请求报文中未包含生效日期");
        Assert.hasKeyAndValue(reqJson, "endTime", "请求报文中未包含到期日期");
        String amount = reqJson.getString("amount");
        if (StringUtil.isEmpty(amount) || !NumberUtil.isDouble(amount) || NumberUtil.getDouble(amount) <= 0) {
            throw new CmdException("合同金额必须大于 0");
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

    /**
     * 生成合同编号：HC-{yyyy}-{ZL|CG|WB}-{4位序号}
     */
    private String generateContractCode(String storeId, String category) {
        String prefix = categoryPrefix(category);
        String codeHead = "HC-" + DateUtil.getNow("yyyy") + "-" + prefix + "-";

        ContractDto contractDto = new ContractDto();
        contractDto.setStoreId(storeId);
        contractDto.setContractCodeLike(codeHead);
        int count = contractInnerServiceSMOImpl.queryContractsCount(contractDto);
        return codeHead + String.format("%04d", count + 1);
    }

    private String categoryPrefix(String category) {
        if (ContractDto.CATEGORY_RENT.equals(category)) {
            return "ZL";
        }
        if (ContractDto.CATEGORY_MAINTAIN.equals(category)) {
            return "WB";
        }
        return "CG";
    }
}
