/**
 * 费用管理查询命令类
 * 
 * 功能：处理管理员欠费明细查询请求，包括数据验证、欠费统计计算、社区信息补充等功能
 * 服务代码：fee.queryAdminOweFeeDetail
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.report.cmd.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.log.LoggerFactory;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.reportFee.ReportFeeMonthStatisticsDto;
import com.java110.intf.community.ICommunityV1InnerServiceSMO;
import com.java110.intf.report.IReportFeeMonthStatisticsInnerServiceSMO;
import com.java110.intf.user.IStaffCommunityV1InnerServiceSMO;
import com.java110.report.bmo.reportFeeMonthStatistics.impl.GetReportFeeMonthStatisticsBMOImpl;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "fee.queryAdminOweFeeDetail")
public class QueryAdminOweFeeDetailCmd extends Cmd {

    private static final Logger logger = LoggerFactory.getLogger(GetReportFeeMonthStatisticsBMOImpl.class);

    @Autowired
    private IReportFeeMonthStatisticsInnerServiceSMO reportFeeMonthStatisticsInnerServiceSMOImpl;

    @Autowired
    private ICommunityV1InnerServiceSMO communityV1InnerServiceSMOImpl;

    @Autowired
    private IStaffCommunityV1InnerServiceSMO staffCommunityV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 功能：验证管理员权限和分页信息
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证管理员权限
        super.validateAdmin(context);
        // 验证分页参数
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行主方法
     * 
     * 功能：查询管理员管辖范围内的欠费明细数据，包括欠费天数计算、社区名称补充等
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求参数转换为DTO对象
        ReportFeeMonthStatisticsDto reportFeeMonthStatisticsDto = BeanConvertUtil.covertBean(reqJson, ReportFeeMonthStatisticsDto.class);
        // 获取当前用户ID
        String staffId = CmdContextUtils.getUserId(context);

        // 查询当前员工管理的社区ID列表
        List<String> communityIds = staffCommunityV1InnerServiceSMOImpl.queryStaffCommunityIds(staffId);

        // 如果员工有管理的社区，设置查询条件中的社区ID
        if (!ListUtil.isNull(communityIds)) {
            reportFeeMonthStatisticsDto.setCommunityIds(communityIds.toArray(new String[communityIds.size()]));
        }
        
        // 查询欠费明细总数
        int count = reportFeeMonthStatisticsInnerServiceSMOImpl.queryOweFeeDetailCount(reportFeeMonthStatisticsDto);
        List<ReportFeeMonthStatisticsDto> reportFeeMonthStatisticsDtos = null;
        
        // 如果有数据，查询详细欠费信息
        if (count > 0) {
            reportFeeMonthStatisticsDtos = reportFeeMonthStatisticsInnerServiceSMOImpl.queryOweFeeDetail(reportFeeMonthStatisticsDto);
            // 查询主要的欠费统计信息
            ReportFeeMonthStatisticsDto tmpReportFeeMonthStatisticsDto = reportFeeMonthStatisticsInnerServiceSMOImpl.queryOweFeeDetailMajor(reportFeeMonthStatisticsDto);
            
            // 为每条欠费记录设置总欠费金额
            if (!ListUtil.isNull(reportFeeMonthStatisticsDtos)) {
                for (ReportFeeMonthStatisticsDto reportFeeMonthStatisticsDto1 : reportFeeMonthStatisticsDtos) {
                    // 注释掉的代码：设置应收金额和实收金额
//                    reportFeeMonthStatisticsDto1.setAllReceivableAmount(tmpReportFeeMonthStatisticsDto.getAllReceivableAmount());
//                    reportFeeMonthStatisticsDto1.setAllReceivedAmount(tmpReportFeeMonthStatisticsDto.getAllReceivedAmount());
                    // 设置总欠费金额
                    reportFeeMonthStatisticsDto1.setAllOweAmount(tmpReportFeeMonthStatisticsDto.getOweAmount());
                }
            }
            // 计算欠费天数
            freshReportOweDay(reportFeeMonthStatisticsDtos);
        } else {
            // 没有数据时返回空列表
            reportFeeMonthStatisticsDtos = new ArrayList<>();
        }

        // 补充社区名称信息
        refreshCommunityName(reportFeeMonthStatisticsDtos);

        // 构造返回结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reportFeeMonthStatisticsDto.getRow()), count, reportFeeMonthStatisticsDtos);

        // 设置响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 计算欠费天数
     * 
     * 功能：根据开始时间和结束时间计算每条欠费记录的欠费天数
     * 
     * @param reportFeeMonthStatisticsDtos 欠费统计DTO列表
     */
    private void freshReportOweDay(List<ReportFeeMonthStatisticsDto> reportFeeMonthStatisticsDtos) {
        int day = 0;
        // 遍历每条欠费记录计算欠费天数
        for (ReportFeeMonthStatisticsDto reportFeeMonthStatisticsDto : reportFeeMonthStatisticsDtos) {
            try {
                // 计算开始时间和结束时间之间的天数差
                day = DateUtil.daysBetween(DateUtil.getDateFromStringB(reportFeeMonthStatisticsDto.getEndTime()),
                        DateUtil.getDateFromStringB(reportFeeMonthStatisticsDto.getStartTime()));
                // 设置欠费天数
                reportFeeMonthStatisticsDto.setOweDay(day);
            } catch (Exception e) {
                // 计算失败时记录错误日志
                logger.error("计算欠费天数失败", e);
            }
        }
    }

    /**
     * 刷新社区名称
     * 
     * 功能：根据社区ID查询社区名称并设置到欠费记录中
     * 
     * @param reportFeeMonthStatisticsDtos 欠费统计DTO列表
     */
    private void refreshCommunityName(List<ReportFeeMonthStatisticsDto> reportFeeMonthStatisticsDtos) {
        // 如果列表为空，直接返回
        if(ListUtil.isNull(reportFeeMonthStatisticsDtos)){
            return;
        }

        // 收集所有社区ID
        List<String> communityIds = new ArrayList<>();
        for (ReportFeeMonthStatisticsDto reportFeeMonthStatisticsDto : reportFeeMonthStatisticsDtos) {
            communityIds.add(reportFeeMonthStatisticsDto.getCommunityId());
        }

        // 如果社区ID列表为空，直接返回
        if(ListUtil.isNull(communityIds)){
            return ;
        }
        
        // 查询社区信息
        CommunityDto communityDto = new CommunityDto();
        communityDto.setCommunityIds(communityIds.toArray(new String[communityIds.size()]));
        List<CommunityDto> communityDtos = communityV1InnerServiceSMOImpl.queryCommunitys(communityDto);
        
        // 如果查询结果为空，直接返回
        if(ListUtil.isNull(communityDtos)){
            return;
        }
        
        // 为每条欠费记录设置社区名称
        for (ReportFeeMonthStatisticsDto reportFeeMonthStatisticsDto : reportFeeMonthStatisticsDtos) {
            for (CommunityDto tCommunityDto : communityDtos) {
                // 匹配社区ID
                if (!reportFeeMonthStatisticsDto.getCommunityId().equals(tCommunityDto.getCommunityId())) {
                    continue;
                }
                // 设置社区名称
                reportFeeMonthStatisticsDto.setCommunityName(tCommunityDto.getName());
            }
        }
    }
}