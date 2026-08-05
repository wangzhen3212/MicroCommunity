package com.java110.job.smo.impl;

import com.java110.core.base.smo.BaseServiceSMO;
import com.java110.dto.notice.NoticeStaffDto;
import com.java110.intf.job.IMsgNotifyInnerServiceSMO;
import com.java110.job.msgNotify.IMsgNotify;
import com.java110.job.msgNotify.MsgNotifyFactory;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.vo.ResultVo;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息通知 Feign 接口实现，供其他服务通过 Feign 调用推送能力
 */
@RestController
public class MsgNotifyInnerServiceSMOImpl extends BaseServiceSMO implements IMsgNotifyInnerServiceSMO {

    @Override
    public ResultVo sendStaffMsg(@RequestBody NoticeStaffDto noticeStaffDto) {
        IMsgNotify msgNotify = MsgNotifyFactory.getMsgNotify(MsgNotifyFactory.NOTIFY_WAY_WECHAT);
        // 如果未指定url，使用员工端巡检首页
        if (noticeStaffDto.getUrl() == null || noticeStaffDto.getUrl().isEmpty()) {
            String wechatUrl = MappingCache.getValue(MappingConstant.URL_DOMAIN, "STAFF_WECHAT_URL");
            if (wechatUrl == null) {
                wechatUrl = "";
            }
            if (wechatUrl.endsWith("#/")) {
                wechatUrl += "pages/inspection/inspection";
            } else if (wechatUrl.endsWith("/")) {
                wechatUrl += "#/pages/inspection/inspection";
            } else {
                wechatUrl += "/#/pages/inspection/inspection";
            }
            noticeStaffDto.setUrl(wechatUrl);
        }
        return msgNotify.sendStaffMsg(noticeStaffDto);
    }
}
