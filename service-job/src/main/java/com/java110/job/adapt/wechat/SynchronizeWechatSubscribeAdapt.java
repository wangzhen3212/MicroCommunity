package com.java110.job.adapt.wechat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.core.factory.WechatFactory;
import com.java110.core.log.LoggerFactory;
import com.java110.dto.system.Business;
import com.java110.dto.wechat.SmallWeChatDto;
import com.java110.dto.wechat.WechatSubscribeDto;
import com.java110.intf.store.ISmallWeChatInnerServiceSMO;
import com.java110.intf.user.IWechatSubscribeV1InnerServiceSMO;
import com.java110.job.adapt.DatabusAdaptImpl;
import com.java110.po.wechat.WechatSubscribePo;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.WechatConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(value = "synchronizeWechatSubscribeAdapt")
public class SynchronizeWechatSubscribeAdapt extends DatabusAdaptImpl {
    private static Logger logger = LoggerFactory.getLogger(SynchronizeWechatSubscribeAdapt.class);

    @Autowired
    private IWechatSubscribeV1InnerServiceSMO wechatSubscribeV1InnerServiceSMOImpl;

    @Autowired
    private ISmallWeChatInnerServiceSMO smallWeChatInnerServiceSMOImpl;

    @Autowired
    private RestTemplate outRestTemplate;

    private static final String GET_USER = "https://api.weixin.qq.com/cgi-bin/user/get?access_token=ACCESS_TOKEN";

    private static final String GET_BATCH_USER = "https://api.weixin.qq.com/cgi-bin/user/info/batchget?access_token=ACCESS_TOKEN";

    @Override
    public void execute(Business business, List<Business> businesses) {
        JSONObject data = business.getData();
        String communityId = data.getString("communityId");
        if (StringUtil.isEmpty(communityId)) {
            throw new CmdException("未包含小区");
        }

        SmallWeChatDto smallWeChatDto = new SmallWeChatDto();
        smallWeChatDto.setObjId(communityId);
        smallWeChatDto.setWeChatType(SmallWeChatDto.WECHAT_TYPE_PUBLIC);
        List<SmallWeChatDto> smallWeChatDtos = smallWeChatInnerServiceSMOImpl.querySmallWeChats(smallWeChatDto);
        if (ListUtil.isNull(smallWeChatDtos)) {
            throw new CmdException("未配置公众号");
        }

        String accessToken = WechatFactory.getAccessToken(smallWeChatDtos.get(0).getAppId(), smallWeChatDtos.get(0).getAppSecret());

        WechatSubscribeDto queryDto = new WechatSubscribeDto();
        queryDto.setAppId(smallWeChatDtos.get(0).getAppId());
        queryDto.setStatusCd("0");
        List<WechatSubscribeDto> existingSubscribes = wechatSubscribeV1InnerServiceSMOImpl.queryWechatSubscribes(queryDto);

        Map<String, WechatSubscribeDto> existingMap = new HashMap<>();
        for (WechatSubscribeDto dto : existingSubscribes) {
            existingMap.put(dto.getOpenId(), dto);
        }

        getAllOpenId(accessToken, "", smallWeChatDtos.get(0), existingMap);

        for (WechatSubscribeDto existing : existingSubscribes) {
            if (!existingMap.containsKey(existing.getOpenId())) {
                WechatSubscribePo deletePo = new WechatSubscribePo();
                deletePo.setSubId(existing.getSubId());
                deletePo.setAppId(existing.getAppId());
                wechatSubscribeV1InnerServiceSMOImpl.deleteWechatSubscribe(deletePo);
            }
        }
    }

    /**
     * 拉取微信关注用户列表并同步到数据库
     * 流程：
     * 1. 调用 /cgi-bin/user/get 拉取 OpenID 列表（一次最多 10000 个）
     * 2. 按 100 个一批调用 /cgi-bin/user/info/batchget 拉取用户详情，每批只请求一次
     * 3. 一次 batchget 返回的 user_info_list 同时填充该批次所有 PO（不分 insert/update）
     * 4. next_openid 为空表示列表结束，否则继续拉取下一页
     *
     * @param accessToken 微信 access_token
     * @param nextOpenid  上一页返回的 next_openid，首次传入空字符串
     * @param weChatDto   公众号配置
     * @param existingMap DB 中已存在的订阅记录（key=openId），方法内会移除已处理的 openId
     */
    private void getAllOpenId(String accessToken, String nextOpenid, SmallWeChatDto weChatDto, Map<String, WechatSubscribeDto> existingMap) {
        // 1. 请求微信关注用户列表（一次最多 10000 个 OpenID）
        String url = GET_USER.replace("ACCESS_TOKEN", accessToken);
        if (!StringUtil.isEmpty(nextOpenid)) {
            url += ("&next_openid=" + nextOpenid);
        }
        ResponseEntity<String> paramOut = outRestTemplate.getForEntity(url, String.class);
        logger.info("获取关注用户列表返回:{}", paramOut);
        if (paramOut.getStatusCode() != HttpStatus.OK) {
            throw new IllegalArgumentException(paramOut.getBody());
        }
        JSONObject paramOutObj = JSONObject.parseObject(paramOut.getBody());
        if (paramOutObj.containsKey("errcode")) {
            throw new IllegalArgumentException(paramOut.getBody());
        }
        if (!paramOutObj.containsKey("data")) {
            return;
        }
        JSONObject dataObj = paramOutObj.getJSONObject("data");
        JSONArray openids = dataObj.getJSONArray("openid");
        // openids 为空表示列表已结束，停止递归
        if (ListUtil.isNull(openids)) {
            return;
        }
        String nextOpenidFromResponse = paramOutObj.getString("next_openid");

        // 本页同步时刻（同一页内所有 PO 共用，便于按页追溯同步时间）
        Date syncTime = new Date();

        // 2. 按 100 个一批调用 batchget 拉取用户详情，每批只请求一次
        List<WechatSubscribePo> insertPos = new ArrayList<>();
        List<WechatSubscribePo> updatePos = new ArrayList<>();
        JSONArray user_list = new JSONArray();
        // 当前批次的所有 PO（按 OpenID 顺序，与 user_list 一一对应，不分 insert/update）
        List<WechatSubscribePo> currentBatchPos = new ArrayList<>();

        for (int openIndex = 0; openIndex < openids.size(); openIndex++) {
            String openId = openids.getString(openIndex);

            WechatSubscribePo wechatSubscribePo = new WechatSubscribePo();
            wechatSubscribePo.setAppId(weChatDto.getAppId());
            wechatSubscribePo.setOpenId(openId);
            wechatSubscribePo.setOpenType(WechatSubscribeDto.OPEN_TYPE_WECHAT);
            wechatSubscribePo.setCreateTime(syncTime);

            if (existingMap.containsKey(openId)) {
                // 已存在：保留原 subId 与 user_id 绑定，走 UPDATE
                WechatSubscribeDto existing = existingMap.remove(openId);
                wechatSubscribePo.setSubId(existing.getSubId());
                wechatSubscribePo.setUserId(existing.getUserId());
                updatePos.add(wechatSubscribePo);
            } else {
                // 新关注：生成 subId，走 INSERT
                wechatSubscribePo.setSubId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_file_id));
                insertPos.add(wechatSubscribePo);
            }

            currentBatchPos.add(wechatSubscribePo);

            JSONObject user = new JSONObject();
            user.put("openid", openId);
            user.put("lang", "zh_CN");
            user_list.add(user);

            // 凑满 100 个调用一次 batchget，然后立即保存该批次
            if (user_list.size() >= 100) {
                fetchAndFillUserDetail(user_list, currentBatchPos, accessToken);
                saveBatch(insertPos, updatePos);
                insertPos = new ArrayList<>();
                updatePos = new ArrayList<>();
                user_list = new JSONArray();
                currentBatchPos = new ArrayList<>();
            }
        }

        // 处理剩余不足 100 个的批次
        if (!user_list.isEmpty()) {
            fetchAndFillUserDetail(user_list, currentBatchPos, accessToken);
            saveBatch(insertPos, updatePos);
        }

        // 3. next_openid 为空表示列表结束，否则继续拉取下一页
        if (!StringUtil.isEmpty(nextOpenidFromResponse)) {
            getAllOpenId(accessToken, nextOpenidFromResponse, weChatDto, existingMap);
        }
    }

    /**
     * 保存一批 insert/update PO 到数据库
     *
     * @param insertPos 新增列表
     * @param updatePos 更新列表
     */
    private void saveBatch(List<WechatSubscribePo> insertPos, List<WechatSubscribePo> updatePos) {
        if (!ListUtil.isNull(insertPos)) {
            wechatSubscribeV1InnerServiceSMOImpl.saveWechatSubscribes(insertPos);
        }
        if (!ListUtil.isNull(updatePos)) {
            for (WechatSubscribePo po : updatePos) {
                wechatSubscribeV1InnerServiceSMOImpl.updateWechatSubscribe(po);
            }
        }
    }

    /**
     * 调用微信 /cgi-bin/user/info/batchget 接口拉取一批用户详情（最多 100 个 OpenID）
     * 一次请求同时填充该批次所有 PO 的 unionId、remark、tagidList
     *
     * @param userList    本批次 OpenID 请求列表（与 batchPos 一一对应）
     * @param batchPos    本批次的 PO 列表（包含 insert 和 update 的 PO）
     * @param accessToken 微信 access_token
     */
    private void fetchAndFillUserDetail(JSONArray userList, List<WechatSubscribePo> batchPos, String accessToken) {
        if (ListUtil.isNull(userList) || ListUtil.isNull(batchPos)) {
            return;
        }

        String url = GET_BATCH_USER.replace("ACCESS_TOKEN", accessToken);

        JSONObject paramIn = new JSONObject();
        paramIn.put("user_list", userList);

        logger.info("批量获取用户详情请求:{}", paramIn.toJSONString());

        ResponseEntity<String> responseEntity = outRestTemplate.postForEntity(url, paramIn.toJSONString(), String.class);
        logger.info("批量获取用户详情返回:{}", responseEntity);

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            throw new IllegalArgumentException(responseEntity.getBody());
        }
        JSONObject paramOutObj = JSONObject.parseObject(responseEntity.getBody());
        if (paramOutObj.containsKey("errcode")) {
            throw new IllegalArgumentException(paramOutObj.getString("errmsg"));
        }

        JSONArray user_info_list = paramOutObj.getJSONArray("user_info_list");
        if (ListUtil.isNull(user_info_list)) {
            return;
        }

        // 将返回的用户详情按 openid 填充到对应 PO（一次请求同时处理 insert 和 update 的 PO）
        for (int userIndex = 0; userIndex < user_info_list.size(); userIndex++) {
            JSONObject user = user_info_list.getJSONObject(userIndex);
            String openid = user.getString("openid");
            String unionid = user.getString("unionid");
            String remark = user.getString("remark");
            JSONArray tagidListArr = user.getJSONArray("tagid_list");
            String tagidList = (tagidListArr == null) ? "" : tagidListArr.toJSONString();

            for (WechatSubscribePo po : batchPos) {
                if (openid.equals(po.getOpenId())) {
                    po.setUnionId(StringUtil.isEmpty(unionid) ? "-1" : unionid);
                    // 写入微信返回的备注与标签ID列表
                    po.setRemark(remark == null ? "" : remark);
                    po.setTagidList(tagidList);
                    break;
                }
            }
        }
    }


}
