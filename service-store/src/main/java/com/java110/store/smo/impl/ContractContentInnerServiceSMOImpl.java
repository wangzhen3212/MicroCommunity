package com.java110.store.smo.impl;

import com.java110.core.base.smo.BaseServiceSMO;
import com.java110.dto.PageDto;
import com.java110.dto.contract.ContractContentDto;
import com.java110.intf.store.IContractContentInnerServiceSMO;
import com.java110.po.contract.ContractContentPo;
import com.java110.store.dao.IContractContentServiceDao;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 合同正文内部服务实现（正文独立表，写入前做标签白名单清洗）
 */
@RestController
public class ContractContentInnerServiceSMOImpl extends BaseServiceSMO implements IContractContentInnerServiceSMO {

    private static Logger logger = LoggerFactory.getLogger(ContractContentInnerServiceSMOImpl.class);

    /**
     * 允许的标签白名单（与前端一致）
     */
    private static final Set<String> ALLOW_TAGS = new HashSet<>(Arrays.asList(
            "p", "br", "strong", "b", "em", "i", "u", "s", "h1", "h2", "h3", "h4", "h5",
            "ul", "ol", "li", "table", "thead", "tbody", "tr", "td", "th", "span", "div", "a",
            "sub", "sup", "img", "font", "blockquote", "hr"));

    private static final Pattern TAG_PATTERN = Pattern.compile("<\\s*/?\\s*([a-zA-Z0-9]+)([^>]*)>");

    private static final Pattern DANGER_BLOCK = Pattern.compile(
            "<\\s*(script|style|iframe|object|embed|link|meta)[^>]*>.*?<\\s*/\\s*\\1\\s*>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern DANGER_SINGLE = Pattern.compile(
            "<\\s*/?\\s*(script|style|iframe|object|embed|link|meta)[^>]*>",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern EVENT_ATTR = Pattern.compile(
            "\\son[a-zA-Z]+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern JS_URL = Pattern.compile(
            "(href|src)\\s*=\\s*(\"|')\\s*javascript:[^\"']*(\"|')", Pattern.CASE_INSENSITIVE);

    @Autowired
    private IContractContentServiceDao contractContentServiceDaoImpl;

    @Override
    public int saveContractContent(@RequestBody ContractContentPo contractContentPo) {
        contractContentPo.setContent(sanitize(contractContentPo.getContent()));
        return contractContentServiceDaoImpl.saveContractContentInfo(BeanConvertUtil.beanCovertMap(contractContentPo));
    }

    @Override
    public int updateContractContent(@RequestBody ContractContentPo contractContentPo) {
        contractContentPo.setContent(sanitize(contractContentPo.getContent()));
        return contractContentServiceDaoImpl.updateContractContentInfo(BeanConvertUtil.beanCovertMap(contractContentPo));
    }

    @Override
    public List<ContractContentDto> queryContractContents(@RequestBody ContractContentDto contractContentDto) {
        int page = contractContentDto.getPage();
        if (page != PageDto.DEFAULT_PAGE) {
            contractContentDto.setPage((page - 1) * contractContentDto.getRow());
        }
        return BeanConvertUtil.covertBeanList(
                contractContentServiceDaoImpl.getContractContentInfo(BeanConvertUtil.beanCovertMap(contractContentDto)),
                ContractContentDto.class);
    }

    /**
     * 按白名单清洗正文 HTML，防止 XSS
     *
     * @param html 原始正文
     * @return 清洗后的正文
     */
    public static String sanitize(String html) {
        if (StringUtil.isEmpty(html)) {
            return html;
        }
        String result = DANGER_BLOCK.matcher(html).replaceAll("");
        result = DANGER_SINGLE.matcher(result).replaceAll("");
        result = EVENT_ATTR.matcher(result).replaceAll("");
        result = JS_URL.matcher(result).replaceAll("$1=\"\"");

        Matcher matcher = TAG_PATTERN.matcher(result);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String tagName = matcher.group(1).toLowerCase();
            String replacement = "";
            if (ALLOW_TAGS.contains(tagName)) {
                replacement = matcher.group();
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    public IContractContentServiceDao getContractContentServiceDaoImpl() {
        return contractContentServiceDaoImpl;
    }

    public void setContractContentServiceDaoImpl(IContractContentServiceDao contractContentServiceDaoImpl) {
        this.contractContentServiceDaoImpl = contractContentServiceDaoImpl;
    }
}
