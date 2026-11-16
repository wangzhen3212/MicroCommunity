package com.java110.store.cmd.contract;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.store.IContractInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 合同台账统计：总数、分类计数、生效数、待审数、总金额、覆盖房屋数
 */
@Java110Cmd(serviceCode = "contract.listContractStatistics")
public class ListContractStatisticsCmd extends Cmd {

    @Autowired
    private IContractInnerServiceSMO contractInnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String storeId = CmdContextUtils.getStoreId(context);

        Map<String, Object> info = new HashMap<>();
        info.put("storeId", storeId);
        if (reqJson.containsKey("communityId")) {
            info.put("communityId", reqJson.getString("communityId"));
        }
        List<Map> statistics = contractInnerServiceSMOImpl.queryContractStatistics(info);
        Map<String, Object> data = new HashMap<>();
        if (statistics != null && !statistics.isEmpty()) {
            data = statistics.get(0);
        }
        context.setResponseEntity(ResultVo.createResponseEntity(data));
    }
}
