package com.java110.acct.payment.adapt.changk;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ChangKSignature {

    private static final Set<String> EXCLUDE_FIELDS = new HashSet<>(Arrays.asList(
            "key_sign", "channel_discount_info", "pay_trace", "pay_time"
    ));

    public static String generateSign(TreeMap<String, String> params, String accessToken) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String value = entry.getValue();
            if (value != null && !value.isEmpty()) {
                sb.append(entry.getKey()).append("=").append(value).append("&");
            }
        }
        sb.append("access_token=").append(accessToken);
        return DigestUtils.md5Hex(sb.toString()).toLowerCase();
    }

    public static boolean verifyNotifySign(JSONObject params, String accessToken) {
        TreeMap<String, String> sortedParams = new TreeMap<>();

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (EXCLUDE_FIELDS.contains(key) || value == null) {
                continue;
            }
            sortedParams.put(key, value.toString());
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        sb.append("access_token=").append(accessToken);

        String calculatedSign = DigestUtils.md5Hex(sb.toString()).toLowerCase();
        return calculatedSign.equalsIgnoreCase(params.getString("key_sign"));
    }
}
