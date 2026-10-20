package com.java110.report.cmd.dataReport;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.parking.ParkingAreaDto;
import com.java110.dto.report.QueryStatisticsDto;
import com.java110.intf.community.IParkingAreaV1InnerServiceSMO;
import com.java110.report.statistics.IFeeStatistics;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * 查询收费情况分析
 * 停车费按停车场查询
 */
@Java110Cmd(serviceCode = "dataReport.queryParkingFeeStatistics")
public class QueryParkingFeeStatisticsCmd extends Cmd {

    @Autowired
    private IFeeStatistics feeStatisticsImpl;

    @Autowired
    private IParkingAreaV1InnerServiceSMO parkingAreaV1InnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
        Assert.hasKeyAndValue(reqJson, "startDate", "未包含开始时间");
        Assert.hasKeyAndValue(reqJson, "endDate", "未包含结束时间");

        String startDate = reqJson.getString("startDate");
        String endDate = reqJson.getString("endDate");
        if (!StringUtil.isEmpty(startDate) && !startDate.contains(":")) {
            startDate += " 00:00:00";
            reqJson.put("startDate", startDate);
        }
        if (!StringUtil.isEmpty(endDate) && !endDate.contains(":")) {
            endDate += " 23:59:59";
            reqJson.put("endDate", endDate);
        }
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        ParkingAreaDto parkingAreaDto = new ParkingAreaDto();
        parkingAreaDto.setCommunityId(reqJson.getString("communityId"));
        String paIds = reqJson.getString("paIds");
        if (!StringUtil.isEmpty(paIds)) {
            parkingAreaDto.setPaIds(paIds.split(","));
        }
        List<ParkingAreaDto> parkingAreaDtos = parkingAreaV1InnerServiceSMOImpl.queryParkingAreas(parkingAreaDto);

        JSONArray data = new JSONArray();
        //todo 没有停车场时直接返回空数据
        if (parkingAreaDtos == null || parkingAreaDtos.size() < 1) {
            context.setResponseEntity(ResultVo.createResponseEntity(data));
            return;
        }

        String monthFastDate = DateUtil.getFormatTimeStringB(DateUtil.getFirstDate(reqJson.getString("startDate")));
        String monthLastDate = DateUtil.getFormatTimeStringB(DateUtil.getNextMonthFirstDate(reqJson.getString("startDate")));
        String startDate = reqJson.getString("startDate");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(DateUtil.getDateFromStringB(startDate));
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        String endDate = DateUtil.getFormatTimeStringB(calendar.getTime());

        //todo 根据停车场ID循环查询
        for (ParkingAreaDto tmpParkingAreaDto : parkingAreaDtos) {
            doGetData(tmpParkingAreaDto.getPaId(), data, reqJson, startDate, endDate, monthFastDate, monthLastDate);
        }
        context.setResponseEntity(ResultVo.createResponseEntity(data));
    }

    /**
     * 查询停车场数据
     *
     * @param paId
     * @param datas
     */
    private void doGetData(String paId, JSONArray datas, JSONObject reqJson, String startDate, String endDate,
                           String monthFastDate, String monthLastDate) {
        QueryStatisticsDto queryStatisticsDto = new QueryStatisticsDto();
        queryStatisticsDto.setCommunityId(reqJson.getString("communityId"));
        queryStatisticsDto.setPaId(paId);
        queryStatisticsDto.setFeeTypeCd(reqJson.getString("feeTypeCd"));
        queryStatisticsDto.setStartDate(reqJson.getString("startDate"));
        queryStatisticsDto.setEndDate(reqJson.getString("endDate"));
        queryStatisticsDto.setTodayStartDate(startDate);
        queryStatisticsDto.setTodayEndDate(endDate);
        queryStatisticsDto.setHisDate(monthFastDate);
        queryStatisticsDto.setMonthEndDate(monthLastDate);

        List<Map> infos = feeStatisticsImpl.getParkingAreaFeeStatistics(queryStatisticsDto);
        if (infos == null || infos.size() < 1) {
            return;
        }
        for (Map info : infos) {
            datas.add(info);
        }
    }
}
