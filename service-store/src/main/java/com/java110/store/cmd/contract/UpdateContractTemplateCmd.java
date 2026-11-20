package com.java110.store.cmd.contract;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.store.IContractTypeTemplateInnerServiceSMO;
import com.java110.po.contract.ContractTypeTemplatePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 更新合同模板（名称/分类/版本/描述/正文）
 */
@Java110Cmd(serviceCode = "contract.updateContractTemplate")
public class UpdateContractTemplateCmd extends Cmd {

    @Autowired
    private IContractTypeTemplateInnerServiceSMO contractTypeTemplateInnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Assert.hasKeyAndValue(reqJson, "templateId", "请求报文中未包含模板ID");
        Assert.hasKeyAndValue(reqJson, "communityId", "必填，请填写小区信息");
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String storeId = CmdContextUtils.getStoreId(context);
        ContractTypeTemplatePo templatePo = BeanConvertUtil.covertBean(reqJson, ContractTypeTemplatePo.class);
        templatePo.setStoreId(storeId);
        templatePo.setCommunityId(reqJson.getString("communityId"));
        templatePo.setStatusCd("0");
        int flag = contractTypeTemplateInnerServiceSMOImpl.updateContractTypeTemplate(templatePo);
        if (flag < 1) {
            throw new CmdException("更新合同模板失败");
        }
        context.setResponseEntity(ResultVo.success());
    }
}
