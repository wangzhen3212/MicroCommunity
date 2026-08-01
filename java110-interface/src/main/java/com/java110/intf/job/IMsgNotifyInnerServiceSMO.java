package com.java110.intf.job;

import com.java110.config.feign.FeignConfiguration;
import com.java110.dto.notice.NoticeStaffDto;
import com.java110.vo.ResultVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * 消息通知 Feign 接口，供其他服务通过 Feign 调用 job-service 的推送能力
 */
@FeignClient(name = "job-service", configuration = {FeignConfiguration.class})
@RequestMapping("/msgNotifyApi")
public interface IMsgNotifyInnerServiceSMO {

    /**
     * 发送员工微信通知
     *
     * @param noticeStaffDto 通知参数
     * @return 推送结果
     */
    @RequestMapping(value = "/sendStaffMsg", method = RequestMethod.POST)
    ResultVo sendStaffMsg(@RequestBody NoticeStaffDto noticeStaffDto);
}
