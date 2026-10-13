package com.java110.community.cmd.communitySpace;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.log.LoggerFactory;
import com.java110.doc.annotation.*;
import com.java110.dto.community.CommunitySpaceDto;
import com.java110.dto.community.CommunitySpaceOpenTimeDto;
import com.java110.intf.community.ICommunitySpaceOpenTimeV1InnerServiceSMO;
import com.java110.intf.community.ICommunitySpaceV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Java110CmdDoc(title = "查询可预约时间场地",
        description = "查询系统中的查询可预约时间场地（场馆维度开放配置，传 spaceId 时按场地占用排除已预约时段）",
        httpMethod = "get",
        url = "http://{ip}:{port}/app/communitySpace.listCommunitySpaceOpenTime",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "communitySpace.listCommunitySpaceOpenTime"
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "spaceId", length = 30, remark = "场地ID（传该值时按场地排除已占用时段）"),
        @Java110ParamDoc(name = "venueId", length = 30, remark = "场馆ID（开放配置维度）"),
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "weekDay", length = 2, remark = "星期 1~7 周一~周日，不传时按 appointmentTime 推算"),
        @Java110ParamDoc(name = "appointmentTime", length = 30, remark = "预约日期 YYYY-MM-DD"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "data", type = "Array", remark = "有效数据"),
                @Java110ParamDoc(parentNodeName = "data", name = "hours", type = "String", remark = "小时"),
                @Java110ParamDoc(parentNodeName = "data", name = "isOpen", type = "String", remark = "是否开放 Y 开放 N 未开放"),
                @Java110ParamDoc(parentNodeName = "data", name = "weekDay", type = "String", remark = "星期"),
        }
)

@Java110ExampleDoc(
        reqBody = "http://{ip}:{port}/app/communitySpace.listCommunitySpaceOpenTime?venueId=123&appointmentTime=2022-01-01&spaceId=456&communityId=2022081539020475",
        resBody = "{\"code\":0,\"data\":[{\"hours\":\"8\",\"isOpen\":\"Y\"}],\"msg\":\"成功\",\"page\":0,\"records\":1,\"rows\":0,\"total\":2}"
)
@Java110Cmd(serviceCode = "communitySpace.listCommunitySpaceOpenTime")
public class ListCommunitySpaceOpenTimeCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListCommunitySpaceOpenTimeCmd.class);

    private static final String WEEK_DAY_ALL = "0";

    private static final String DATE_PATTERN = "yyyy-MM-dd";

    @Autowired
    private ICommunitySpaceOpenTimeV1InnerServiceSMO communitySpaceOpenTimeV1InnerServiceSMOImpl;

    @Autowired
    private ICommunitySpaceV1InnerServiceSMO communitySpaceV1InnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        Assert.hasKeyAndValue(reqJson, "communityId", "小区ID");
        if (StringUtil.isEmpty(reqJson.getString("spaceId")) && StringUtil.isEmpty(reqJson.getString("venueId"))) {
            throw new CmdException("请求报文中未包含 venueId 或 spaceId");
        }
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        String communityId = reqJson.getString("communityId");
        String venueId = reqJson.getString("venueId");
        String spaceId = reqJson.getString("spaceId");
        String appointmentTime = reqJson.getString("appointmentTime");

        //传场地时解析其所属场馆（旧调用方兼容）
        if (StringUtil.isEmpty(venueId) && !StringUtil.isEmpty(spaceId)) {
            venueId = queryVenueIdBySpace(spaceId, communityId);
        }

        String weekDay = reqJson.getString("weekDay");
        if (StringUtil.isEmpty(weekDay) && !StringUtil.isEmpty(appointmentTime)) {
            weekDay = getWeekDay(appointmentTime);
        }

        CommunitySpaceOpenTimeDto communitySpaceOpenTimeDto = new CommunitySpaceOpenTimeDto();
        communitySpaceOpenTimeDto.setCommunityId(communityId);
        communitySpaceOpenTimeDto.setVenueId(venueId);
        communitySpaceOpenTimeDto.setWeekDay(weekDay);
        if (!StringUtil.isEmpty(weekDay)) {
            communitySpaceOpenTimeDto.setWeekDays(new String[]{weekDay, WEEK_DAY_ALL});
        }
        //占用判定：按请求场地 + 日期排除已预约时段
        if (!StringUtil.isEmpty(appointmentTime) && !StringUtil.isEmpty(spaceId)) {
            communitySpaceOpenTimeDto.setSpaceId(spaceId);
            communitySpaceOpenTimeDto.setAppointmentTime(appointmentTime);
        }
        List<CommunitySpaceOpenTimeDto> communitySpaceOpenTimeDtos =
                communitySpaceOpenTimeV1InnerServiceSMOImpl.queryCommunitySpaceOpenTimes(communitySpaceOpenTimeDto);

        if (!StringUtil.isEmpty(weekDay)) {
            communitySpaceOpenTimeDtos = mergeWeekDayConfig(communitySpaceOpenTimeDtos, weekDay);
        }

        ResultVo resultVo = new ResultVo(1, communitySpaceOpenTimeDtos.size(), communitySpaceOpenTimeDtos);

        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        context.setResponseEntity(responseEntity);
    }

    /**
     * 查询场地所属场馆
     *
     * @param spaceId     场地ID
     * @param communityId 小区ID
     * @return 场馆ID
     */
    private String queryVenueIdBySpace(String spaceId, String communityId) {
        CommunitySpaceDto communitySpaceDto = new CommunitySpaceDto();
        communitySpaceDto.setSpaceId(spaceId);
        communitySpaceDto.setCommunityId(communityId);
        List<CommunitySpaceDto> communitySpaceDtos = communitySpaceV1InnerServiceSMOImpl.queryCommunitySpaces(communitySpaceDto);
        if (communitySpaceDtos == null || communitySpaceDtos.size() < 1) {
            return "";
        }
        return communitySpaceDtos.get(0).getVenueId();
    }

    /**
     * 星期差异化配置与全周通用配置合并：同一小时优先取星期配置
     *
     * @param allDtos 配置行
     * @param weekDay 星期
     * @return 合并后的配置行
     */
    private List<CommunitySpaceOpenTimeDto> mergeWeekDayConfig(List<CommunitySpaceOpenTimeDto> allDtos, String weekDay) {
        if (allDtos == null || allDtos.size() < 1) {
            return new ArrayList<>();
        }
        Map<String, CommunitySpaceOpenTimeDto> weekDayMap = new HashMap<>();
        Map<String, CommunitySpaceOpenTimeDto> commonMap = new HashMap<>();
        for (CommunitySpaceOpenTimeDto dto : allDtos) {
            if (weekDay.equals(dto.getWeekDay())) {
                weekDayMap.put(dto.getHours(), dto);
                continue;
            }
            commonMap.put(dto.getHours(), dto);
        }
        List<CommunitySpaceOpenTimeDto> resultDtos = new ArrayList<>();
        for (Map.Entry<String, CommunitySpaceOpenTimeDto> entry : commonMap.entrySet()) {
            if (weekDayMap.containsKey(entry.getKey())) {
                continue;
            }
            resultDtos.add(entry.getValue());
        }
        resultDtos.addAll(weekDayMap.values());
        resultDtos.sort((one, two) -> Integer.parseInt(one.getHours()) - Integer.parseInt(two.getHours()));
        return resultDtos;
    }

    /**
     * 日期转星期（1~7 周一~周日）
     *
     * @param date 日期 YYYY-MM-DD
     * @return 星期
     */
    private String getWeekDay(String date) {
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_PATTERN);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(simpleDateFormat.parse(date.substring(0, 10)));
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            return (dayOfWeek == Calendar.SUNDAY ? 7 : dayOfWeek - 1) + "";
        } catch (Exception e) {
            logger.error("解析预约日期失败", e);
            return "";
        }
    }
}
