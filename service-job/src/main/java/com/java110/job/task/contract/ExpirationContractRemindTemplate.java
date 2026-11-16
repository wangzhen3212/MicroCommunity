package com.java110.job.task.contract;

import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.task.TaskDto;
import com.java110.intf.store.IContractInnerServiceSMO;
import com.java110.job.quartz.TaskSystemQuartz;
import com.java110.utils.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 合同到期提醒：每日扫描生效中合同，到期前 30 / 15 / 7 天写入提醒日志
 */
@Component
public class ExpirationContractRemindTemplate extends TaskSystemQuartz {

    private static final String[] REMIND_DAYS = {"30", "15", "7"};

    private static final String PREFIX_LOG_ID = "97";

    @Autowired
    private IContractInnerServiceSMO contractInnerServiceSMOImpl;

    @Override
    protected void process(TaskDto taskDto) throws Exception {
        for (String days : REMIND_DAYS) {
            try {
                pushRemind(days);
            } catch (Exception e) {
                logger.error("合同到期提醒推送失败，提醒天数：" + days, e);
            }
        }
    }

    /**
     * 推送指定提醒天数的合同
     *
     * @param days 到期前天数
     */
    private void pushRemind(String days) {
        Map<String, Object> info = new HashMap<>();
        info.put("days", days);
        info.put("page", -1);
        List<Map> reminds = contractInnerServiceSMOImpl.queryContractReminds(info);
        if (reminds == null || reminds.isEmpty()) {
            return;
        }
        for (Map remind : reminds) {
            if (remind.get("daysLeft") == null || !days.equals(remind.get("daysLeft").toString())) {
                continue;
            }
            String contractId = remind.get("contractId").toString();
            if (isRemindedToday(contractId, days)) {
                continue;
            }
            Map<String, Object> logInfo = new HashMap<>();
            logInfo.put("logId", GenerateCodeFactory.getGeneratorId(PREFIX_LOG_ID));
            logInfo.put("contractId", contractId);
            logInfo.put("remindType", days);
            logInfo.put("channel", "inner");
            logInfo.put("sendTime", DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
            logInfo.put("content", buildContent(remind, days));
            logInfo.put("storeId", remind.get("storeId"));
            contractInnerServiceSMOImpl.saveContractRemindLog(logInfo);
        }
    }

    /**
     * 当天是否已推送过同类型提醒，避免重复
     */
    private boolean isRemindedToday(String contractId, String days) {
        Map<String, Object> info = new HashMap<>();
        info.put("contractId", contractId);
        List<Map> logs = contractInnerServiceSMOImpl.queryContractRemindLogs(info);
        if (logs == null || logs.isEmpty()) {
            return false;
        }
        String today = DateUtil.getNow("yyyy-MM-dd");
        for (Map log : logs) {
            if (log.get("sendTime") == null || log.get("remindType") == null) {
                continue;
            }
            if (days.equals(log.get("remindType").toString())
                    && log.get("sendTime").toString().startsWith(today)) {
                return true;
            }
        }
        return false;
    }

    private String buildContent(Map remind, String days) {
        String content = "合同[" + remind.get("contractCode") + "]"
                + (remind.get("contractName") == null ? "" : "（" + remind.get("contractName") + "）")
                + "将于" + remind.get("endTime") + "到期，剩余" + days + "天，请及时办理续签或终止手续。";
        return content.length() > 500 ? content.substring(0, 500) : content;
    }
}
