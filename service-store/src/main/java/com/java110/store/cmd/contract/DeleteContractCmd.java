package com.java110.store.cmd.contract;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.Java110CmdDoc;
import com.java110.doc.annotation.Java110ExampleDoc;
import com.java110.doc.annotation.Java110ParamDoc;
import com.java110.doc.annotation.Java110ParamsDoc;
import com.java110.doc.annotation.Java110ResponseDoc;
import com.java110.po.contract.ContractPo;
import com.java110.store.bmo.contract.IDeleteContractBMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 删除合同（合同存在未结束费用时返回可读提示）
 */
@Java110CmdDoc(title = "删除合同",
        description = "删除合同并回滚合同房屋与业主关系",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/contract.deleteContract",
        resource = "contractDoc",
        author = "wuxw",
        serviceCode = "contract.deleteContract",
        seq = 1
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "contractId", length = 30, remark = "合同ID"),
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

@Java110ExampleDoc(
        reqBody = "{\"contractId\":\"102026112000000001\",\"communityId\":\"2022092200930358\"}",
        resBody = "{'code':0,'msg':'保存成功'}"
)
@Java110Cmd(serviceCode = "contract.deleteContract")
public class DeleteContractCmd extends Cmd {

    @Autowired
    private IDeleteContractBMO deleteContractBMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Assert.hasKeyAndValue(reqJson, "contractId", "请求报文中未包含合同ID");
        Assert.hasKeyAndValue(reqJson, "communityId", "必填，请填写小区信息");
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String storeId = CmdContextUtils.getStoreId(context);
        try {
            if (StringUtil.isEmpty(storeId)) {
                throw new CmdException("未获取到商户信息，请重新登录后重试");
            }
            ContractPo contractPo = BeanConvertUtil.covertBean(reqJson, ContractPo.class);
            contractPo.setContractId(reqJson.getString("contractId"));
            contractPo.setStoreId(storeId);
            contractPo.setCommunityId(reqJson.getString("communityId"));
            context.setResponseEntity(deleteContractBMOImpl.delete(contractPo));
        } catch (Exception e) {
            context.setResponseEntity(ResultVo.error("删除合同失败：" + e.getMessage()));
        }
    }
}
