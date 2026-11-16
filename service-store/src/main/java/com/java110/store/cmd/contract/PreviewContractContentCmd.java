package com.java110.store.cmd.contract;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.contract.ContractTypeTemplateDto;
import com.java110.intf.store.IContractTypeTemplateInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 合同正文变量渲染（模板 + 表单变量 + 房屋清单），仅返回渲染结果不入库
 */
@Java110Cmd(serviceCode = "contract.previewContractContent")
public class PreviewContractContentCmd extends Cmd {

    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*}}|\\$\\{\\s*([a-zA-Z0-9_]+)\\s*}");

    @Autowired
    private IContractTypeTemplateInnerServiceSMO contractTypeTemplateInnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String storeId = CmdContextUtils.getStoreId(context);
        String content = reqJson.getString("content");
        String templateId = reqJson.getString("templateId");

        if (StringUtil.isEmpty(content) && !StringUtil.isEmpty(templateId)) {
            content = queryTemplateContent(templateId, storeId);
        }
        if (StringUtil.isEmpty(content)) {
            throw new CmdException("合同正文为空，无法预览");
        }

        JSONObject variables = reqJson.getJSONObject("variables");
        Map<String, Object> data = new HashMap<>();
        data.put("content", render(content, variables));
        context.setResponseEntity(ResultVo.createResponseEntity(data));
    }

    private String queryTemplateContent(String templateId, String storeId) {
        ContractTypeTemplateDto templateDto = new ContractTypeTemplateDto();
        templateDto.setTemplateId(templateId);
        templateDto.setStoreId(storeId);
        templateDto.setStatusCd("0");
        List<ContractTypeTemplateDto> templateDtos = contractTypeTemplateInnerServiceSMOImpl.queryContractTypeTemplates(templateDto);
        if (templateDtos == null || templateDtos.isEmpty()) {
            return null;
        }
        return templateDtos.get(0).getContext();
    }

    private String render(String content, JSONObject variables) {
        if (variables == null || variables.isEmpty()) {
            return content;
        }
        Matcher matcher = VAR_PATTERN.matcher(content);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            Object value = variables.get(key);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(value == null ? "" : value.toString()));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
