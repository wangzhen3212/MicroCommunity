package com.java110.api.filter;

import com.java110.core.factory.AuthenticationFactory;
import com.java110.core.factory.DataTransactionFactory;
import com.java110.utils.cache.CommonCache;
import com.java110.utils.constant.CommonConstant;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.FilterException;
import com.java110.utils.util.StringUtil;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Map;

/**
 * Created by wuxw on 2018/5/2.
 */
public class JwtFilter implements Filter {

    private static Logger logger = LoggerFactory.getLogger(JwtFilter.class);

    private String[] excludedUris;

    @Override
    public void destroy() {
        // TODO Auto-generated method stub
    }


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        excludedUris = filterConfig.getInitParameter("excludedUri").split(",");
    }

    public void doFilter(final ServletRequest req, final ServletResponse res, final FilterChain chain)
            throws IOException, ServletException {

        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;

        String ip = getClientIp(request);
        if (isIpBlacklisted(ip)) {
            return;
        }

        String uri = request.getServletPath();
        if (isExcludedUri(uri)) {
            chain.doFilter(request, response);
            return;
        }
        String token = "";
        try {
            //获取token
            token = this.getToken(request);
            try {
                long tokenStartTime = new Date().getTime();
                Map<String, String> claims = AuthenticationFactory.verifyToken(token);
                logger.debug("校验token 耗时：{}", new Date().getTime() - tokenStartTime);
                request.setAttribute("claims", claims);

            } catch (Exception e) {
                //Invalid signature/claims
                logger.debug("解析token 失败 ：{}", e.getMessage());
                throw new FilterException(ResponseConstant.RESULT_CODE_NO_AUTHORITY_ERROR, "您还没有登录，请先登录");
            }

            chain.doFilter(req, res);
        } catch (FilterException e) {
            logger.debug("未授权访问：{}，URI：{}", e.getMessage(), uri);
            recordUnauthorizedAccess(request, uri);
            if ("POST".equals(request.getMethod())) {
                writeJson(response,
                        DataTransactionFactory.pageResponseJson(ResponseConstant.RESULT_CODE_NO_AUTHORITY_ERROR, e.getMessage(), null),
                        "UTF-8");
            } else {
                //response.sendRedirect("/flow/login");
                noLogin(request,response);
            }

        } catch (Exception e) {
            //response.sendRedirect("/flow/login");
            logger.error("业务处理失败", e);
            noLogin(request,response);
        }
    }

    public void noLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String type = request.getHeader("X-Requested-With");// XMLHttpRequest     HttpServletRequest -> request
        if ("XMLHttpRequest".equals(type)) {
            //是ajax请求
            // 异步请求下的重定向
            response.setHeader("CONTEXTPATH", "/#/views/user/login");//重定向目标地址
            response.setHeader("location", "/#/views/user/login");//重定向目标地址
            response.setStatus(401);
        } else {
            //非ajax请求，直接使用重定向
            response.sendRedirect("/#/views/user/login");
        }
    }

    /**
     * 获取TOKEN
     *
     * @param request
     * @return
     */
    private String getToken(HttpServletRequest request) throws FilterException {
        String token = "";
        if (request.getCookies() != null && request.getCookies().length > 0) {
            for (Cookie cookie : request.getCookies()) {
                if (CommonConstant.COOKIE_AUTH_TOKEN.equals(cookie.getName())) {
                    token = cookie.getValue();
                }
            }
        }

        String authorization = request.getHeader("Authorization");
//StringUtil.isEmpty(token) &&
        if(!StringUtil.isEmpty(authorization)){
            token = authorization.substring("Bearer ".length());
        }

        if (StringUtil.isNullOrNone(token)) {
            throw new FilterException(ResponseConstant.RESULT_CODE_NO_AUTHORITY_ERROR, "您还没有登录，请先登录");
        }
        return token;
    }

    private void writeJson(HttpServletResponse response, String data, String encoding) {
        //设置编码格式
        response.setContentType("text/plain;charset=" + encoding);
        response.setCharacterEncoding(encoding);

        PrintWriter out = null;
        try {
            out = response.getWriter();
            out.write(data);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void recordUnauthorizedAccess(HttpServletRequest request, String uri) {
        String ip = getClientIp(request);
        String key = "UNAUTH_ACCESS_" + ip;
        try {
            String countStr = CommonCache.getValue(key);
            int count = StringUtil.isEmpty(countStr) ? 0 : Integer.parseInt(countStr);
            count++;
            CommonCache.setValue(key, String.valueOf(count), 3600);
            if (count >= 100) {
                addIpToBlacklist(ip);
            }
        } catch (Exception e) {
            logger.debug("记录未授权访问失败：{}", e.getMessage());
        }
    }

    private boolean isIpBlacklisted(String ip) {
        try {
            String blacklisted = CommonCache.getValue("IP_BLACKLIST_" + ip);
            return "true".equals(blacklisted);
        } catch (Exception e) {
            return false;
        }
    }

    private void addIpToBlacklist(String ip) {
        try {
            CommonCache.setValue("IP_BLACKLIST_" + ip, "true", 7200);
            logger.warn("IP {} 因未授权访问超过100次，已加入黑名单", ip);
        } catch (Exception e) {
            logger.debug("加入IP黑名单失败：{}", e.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (!StringUtil.isEmpty(ip) && !"unknown".equalsIgnoreCase(ip)) {
            int index = ip.indexOf(',');
            if (index != -1) {
                ip = ip.substring(0, index);
            }
        }
        if (StringUtil.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StringUtil.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StringUtil.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StringUtil.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private boolean isExcludedUri(String uri) {
        if (excludedUris == null || excludedUris.length <= 0) {
            return false;
        }
        for (String ex : excludedUris) {
            uri = uri.trim();
            ex = ex.trim();
            if (uri.toLowerCase().matches(ex.toLowerCase().replace("*", ".*")))
                return true;
        }
        return false;
    }


}
