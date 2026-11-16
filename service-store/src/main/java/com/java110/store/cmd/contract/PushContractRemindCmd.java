package com.java110.store.cmd.contract;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.contract.ContractDto;
import com.java110.intf.store.IContractInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 批量推送到期提醒（站内信 + 短信），推送结果写入提醒日志
 */
@Java110Cmd(serviceCode = "contract.pushContractRemind")
public class PushContractRemindCmd extends Cmd {

    private static final String PREFIX_LOG_ID = "97";
    private static final String REMIND_TYPE_MANUAL = "manual";
    private static final String CHANNEL_INNER = "inner";
    private static final String CHANNEL_SMS = "sms";

    @Autowired
    private IContractInnerServiceSMO contractInnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        if (!reqJson.containsKey("contractIds")) {
            throw new CmdException("请求报文中未包含需要提醒的合同");
        }
    }

    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String storeId = CmdContextUtils.getStoreId(context);
        JSONArray contractIds = reqJson.getJSONArray("contractIds");
        if (contractIds == null || contractIds.isEmpty()) {
            throw new CmdException("请求报文中未包含需要提醒的合同");
        }
        String remindType = StringUtil.isEmpty(reqJson.getString("remindType"))
                ? REMIND_TYPE_MANUAL : reqJson.getString("remindType");
        List<String> channels = parseChannels(reqJson.getString("channels"));

        int successCount = 0;
        List<Map<String, Object>> failList = new ArrayList<>();
        for (int i = 0; i < contractIds.size(); i++) {
            String contractId = contractIds.getString(i);
            try {
                saveRemindLog(contractId, storeId, remindType, channels);
                successCount++;
            } catch (Exception e) {
                Map<String, Object> fail = new HashMap<>();
                fail.put("contractId", contractId);
                fail.put("msg", e.getMessage());
                failList.add(fail);
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("successCount", successCount);
        data.put("failCount", failList.size());
        data.put("failList", failList);
        context.setResponseEntity(ResultVo.createResponseEntity(data));
    }

    /**
     * 按渠道逐个留痕（短信通道异常不影响站内信）
     */
    private void saveRemindLog(String contractId, String storeId, String remindType, List<String> channels) {
        ContractDto contractDto = new ContractDto();
        contractDto.setContractId(contractId);
        contractDto.setStoreId(storeId);
        contractDto.setStatusCd("0");
        List<ContractDto> contractDtos = contractInnerServiceSMOImpl.queryContracts(contractDto);
        if (contractDtos == null || contractDtos.isEmpty()) {
            throw new CmdException("未查询到合同[" + contractId + "]");
        }
        ContractDto contract = contractDtos.get(0);
        String content = buildContent(contract, remindType);
        for (String channel : channels) {
            Map<String, Object> info = new HashMap<>();
            info.put("logId", GenerateCodeFactory.getGeneratorId(PREFIX_LOG_ID));
            info.put("contractId", contractId);
            info.put("remindType", remindType);
            info.put("channel", channel);
            info.put("sendTime", DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
            info.put("receiveTel", CHANNEL_SMS.equals(channel) ? contract.getOperatorLink() : null);
            info.put("content", content);
            info.put("storeId", storeId);
            contractInnerServiceSMOImpl.saveContractRemindLog(info);
        }
    }

    private String buildContent(ContractDto contract, String remindType) {
        StringBuilder content = new StringBuilder();
        content.append("合同[")
                .append(contract.getContractCode())
                .append("]")
                .append(StringUtil.isEmpty(contract.getContractName()) ? "" : "（" + contract.getContractName() + "）")
                .append("将于")
                .append(contract.getEndTime())
                .append("到期");
        if (!REMIND_TYPE_MANUAL.equals(remindType)) {
            content.append("，剩余").append(remindType).append("天提醒");
        }
        content.append("，请及时办理续签或终止手续。");
        return content.length() > 500 ? content.substring(0, 500) : content.toString();
    }

    private List<String> parseChannels(String channels) {
        List<String> result = new ArrayList<>();
        if (StringUtil.isEmpty(channels)) {
            result.add(CHANNEL_INNER);
            return result;
        }
        String[] channelArray = channels.split(",");
        for (String channel : channelArray) {
            if (StringUtil.isEmpty(channel)) {
                continue;
            }
            String tmpChannel = channel.trim();
            if (!result.contains(tmpChannel)) {
                result.add(tmpChannel);
            }
        }
        if (result.isEmpty()) {
            result.add(CHANNEL_INNER);
        }
        return result;
    }
}
