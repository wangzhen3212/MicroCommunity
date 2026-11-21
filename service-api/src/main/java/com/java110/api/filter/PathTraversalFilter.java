package com.java110.api.filter;

import com.java110.core.log.LoggerFactory;
import org.slf4j.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.regex.Pattern;

/**
 * 路径穿越防护过滤器
 * <p>
 * 对 uri 与 queryString 做两次 URL 解码后匹配 ../、..\、%2e%2e、\0 等穿越特征，命中直接返回 400，
 * 防止利用 ../ 与编码穿越读取服务器文件，请求 body 不参与匹配避免误伤富文本
 */
public class PathTraversalFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(PathTraversalFilter.class);

    private static final String SAFETY_LOG_FLAG = "SafetyFilter";

    private static final Pattern TRAVERSAL_PATTERN = Pattern.compile("\\.\\.[/\\\\]|%2e%2e|\u0000");

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;

        String uri = request.getRequestURI();
        String queryString = request.getQueryString();

        if (hasTraversal(uri) || hasTraversal(queryString)) {
            logger.warn("{} 拦截路径穿越请求，uri={}，query={}，ip={}", SAFETY_LOG_FLAG, uri, queryString, request.getRemoteAddr());
            writeError(response);
            return;
        }

        chain.doFilter(req, res);
    }

    @Override
    public void destroy() {
    }

    private boolean hasTraversal(String value) {
        if (value == null || value.length() == 0) {
            return false;
        }
        String decoded = decode(decode(value));
        return TRAVERSAL_PATTERN.matcher(decoded.toLowerCase()).find();
    }

    private String decode(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    private void writeError(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("text/plain;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        out.write("{\"code\":400,\"msg\":\"非法请求\"}");
        out.flush();
    }
}
