package com.java110.store.cmd.contract;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.store.IContractInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 到期提醒清单（剩余天数升序，附带提醒推送记录）
 */
@Java110Cmd(serviceCode = "contract.listContractReminds")
public class ListContractRemindsCmd extends Cmd {

    private static final String DEFAULT_DAYS = "30";

    @Autowired
    private IContractInnerServiceSMO contractInnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        super.validatePageInfo(reqJson);
        Assert.hasKeyAndValue(reqJson, "communityId", "必填，请填写小区信息");
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String storeId = CmdContextUtils.getStoreId(context);
        String communityId = reqJson.getString("communityId");

        Map<String, Object> info = new HashMap<>();
        info.put("storeId", storeId);
        info.put("communityId", communityId);
        info.put("days", StringUtil.isEmpty(reqJson.getString("days")) ? DEFAULT_DAYS : reqJson.getString("days"));
        if (!StringUtil.isEmpty(reqJson.getString("category"))) {
            info.put("category", reqJson.getString("category"));
        }
        if (!StringUtil.isEmpty(reqJson.getString("searchText"))) {
            info.put("searchText", reqJson.getString("searchText"));
        }
        if (!StringUtil.isEmpty(reqJson.getString("includeExpired"))) {
            info.put("includeExpired", reqJson.getString("includeExpired"));
        }
        info.put("page", reqJson.getInteger("page"));
        info.put("row", reqJson.getInteger("row"));

        int count = contractInnerServiceSMOImpl.queryContractRemindsCount(info);

        List<Map> reminds = new ArrayList<>();
        if (count > 0) {
            reminds = contractInnerServiceSMOImpl.queryContractReminds(info);
            refreshRemindLogs(reminds, communityId);
        }

        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")),
                count, reminds);
        context.setResponseEntity(ResultVo.createResponseEntity(resultVo));
    }

    /**
     * 批量查询提醒记录并挂到清单行上
     */
    private void refreshRemindLogs(List<Map> reminds, String communityId) {
        String[] contractIds = new String[reminds.size()];
        for (int i = 0; i < reminds.size(); i++) {
            contractIds[i] = reminds.get(i).get("contractId").toString();
        }
        Map<String, Object> info = new HashMap<>();
        info.put("contractIds", contractIds);
        info.put("communityId", communityId);
        List<Map> logs = contractInnerServiceSMOImpl.queryContractRemindLogs(info);
        for (Map remind : reminds) {
            List<Map> contractLogs = new ArrayList<>();
            if (logs != null) {
                for (Map log : logs) {
                    if (remind.get("contractId").equals(log.get("contractId"))) {
                        contractLogs.add(log);
                    }
                }
            }
            remind.put("logs", contractLogs);
        }
    }
}
