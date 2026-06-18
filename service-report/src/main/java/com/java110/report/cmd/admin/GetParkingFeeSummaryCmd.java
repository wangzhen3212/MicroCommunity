package com.java110.report.cmd.admin;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.store.StoreDto;
import com.java110.intf.community.ICommunityV1InnerServiceSMO;
import com.java110.intf.report.IBaseDataStatisticsInnerServiceSMO;
import com.java110.intf.store.IStoreInnerServiceSMO;
import com.java110.intf.store.IStoreV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.*;

/**
 * 车位费台账查询命令类
 * 
 * 该类用于处理车位费台账的查询请求，包括数据验证、数据查询、月度费用计算等功能
 * 通过调用基础数据统计服务获取车位费汇总信息，并计算每月费用情况
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode admin.getParkingFeeSummary
 */
@Java110Cmd(serviceCode = "admin.getParkingFeeSummary")
public class GetParkingFeeSummaryCmd extends Cmd {

    @Autowired
    private IStoreInnerServiceSMO storeInnerServiceSMOImpl;

    @Autowired
    private IBaseDataStatisticsInnerServiceSMO baseDataStatisticsInnerServiceSMOImpl;

    @Autowired
    private ICommunityV1InnerServiceSMO communityV1InnerServiceSMOImpl;

    @Autowired
    private IStoreV1InnerServiceSMO storeV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证分页参数和用户权限，确保只有系统管理员可以执行此操作
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当验证失败时抛出命令异常
     * @throws ParseException 当参数解析失败时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证分页信息
        super.validatePageInfo(reqJson);
        
        // 获取当前用户的店铺ID
        String storeId = CmdContextUtils.getStoreId(context);

        // 验证用户是否为系统管理员
        StoreDto storeDto = new StoreDto();
        storeDto.setStoreId(storeId);
        storeDto.setStoreTypeCd(StoreDto.STORE_TYPE_ADMIN);
        int count = storeInnerServiceSMOImpl.getStoreCount(storeDto);
        if (count < 1) {
            throw new CmdException("非法操作，请用系统管理员账户操作");
        }
    }

    /**
     * 执行车位费台账查询命令
     * 
     * 处理车位费台账查询请求，包括时间范围设置、车位编号解析、数据查询和月度费用计算
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行失败时抛出命令异常
     * @throws ParseException 当参数解析失败时抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 获取分页行数
        int row = reqJson.getIntValue("row");
        
        // 设置默认时间范围（当前年份的1月1日到次年1月1日）
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);

        if (!reqJson.containsKey("startTime") || StringUtil.isEmpty(reqJson.getString("startTime"))) {
            reqJson.put("startTime", year + "-01-01");
        }
        if (!reqJson.containsKey("endTime") || StringUtil.isEmpty(reqJson.getString("endTime"))) {
            reqJson.put("endTime", (year + 1) + "-01-01");
        }
        
        // 解析车位编号（格式：车位区域-车位编号）
        if (reqJson.containsKey("psName") && !StringUtil.isEmpty(reqJson.getString("psName"))) {
            String[] psNames = reqJson.getString("psName").split("-", 2);
            if (psNames.length != 2) {
                throw new CmdException("车位编号错误");
            }
            reqJson.put("paNum", psNames[0]);  // 车位区域编号
            reqJson.put("psNum", psNames[1]);  // 车位编号
        }

        // 查询车位费汇总数据总数量
        int total = baseDataStatisticsInnerServiceSMOImpl.getParkingFeeSummaryDataCount(reqJson);

        List<Map> infos = null;
        if (total > 0) {
            // 查询车位费汇总数据
            infos = baseDataStatisticsInnerServiceSMOImpl.getParkingFeeSummaryData(reqJson);
        } else {
            infos = new ArrayList<>();
        }

        // 计算每月费用情况
        computeEveryMonthFee(infos, reqJson);

        // 计算小区和物业公司信息
        computeCommunityAndProperty(infos);

        // 构建响应结果
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity((int) Math.ceil((double) total / (double) row), total, infos);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 计算小区和物业公司信息
     * 
     * 为车位费数据添加小区名称和物业公司名称信息
     * 
     * @param infos 车位费数据列表
     */
    private void computeCommunityAndProperty(List<Map> infos) {
        // 查询所有小区及其物业信息
        CommunityDto communityDto = new CommunityDto();
        List<CommunityDto> communityDtos = communityV1InnerServiceSMOImpl.queryCommunityAndPropertys(communityDto);

        if (communityDtos == null || communityDtos.isEmpty()) {
            return;
        }

        // 查询所有物业公司信息
        StoreDto storeDto = new StoreDto();
        storeDto.setStoreTypeCd(StoreDto.STORE_TYPE_PROPERTY);
        List<StoreDto> storeDtos = storeV1InnerServiceSMOImpl.queryStores(storeDto);

        // 为小区数据设置物业公司名称
        for (CommunityDto tmpCommunityDto : communityDtos) {
            for (StoreDto tmpStoreDto : storeDtos) {
                if (tmpStoreDto.getStoreId().equals(tmpCommunityDto.getStoreId())) {
                    tmpCommunityDto.setStoreName(tmpStoreDto.getName());
                }
            }
        }

        // 为车位费数据设置小区名称和物业公司信息
        for (Map info : infos) {
            for (CommunityDto tmpCommunityDto : communityDtos) {
                if (info.get("communityId").equals(tmpCommunityDto.getCommunityId())) {
                    info.put("communityName", tmpCommunityDto.getName());  // 小区名称
                    info.put("storeId", tmpCommunityDto.getStoreId());     // 物业公司ID
                    info.put("storeName", tmpCommunityDto.getStoreName()); // 物业公司名称
                }
            }
        }
    }

    /**
     * 计算每月费用情况
     * 
     * 为每个车位计算指定时间范围内的每月费用明细
     * 
     * @param infos 车位费数据列表
     * @param reqJson 请求参数JSON对象，包含时间范围信息
     */
    private void computeEveryMonthFee(List<Map> infos, JSONObject reqJson) {
        if (infos == null || infos.isEmpty()) {
            return;
        }

        // 收集所有车位ID
        List<String> roomIds = new ArrayList<>();
        for (Map info : infos) {
            roomIds.add(info.get("carId").toString());
        }

        // 构建查询参数
        Map info = new HashMap();
        info.put("roomIds", roomIds.toArray(new String[roomIds.size()]));  // 车位ID数组
        info.put("startTime", reqJson.getString("startTime"));             // 开始时间
        info.put("endTime", reqJson.getString("endTime"));                 // 结束时间

        // 查询每月费用数据
        List<Map> monthDatas = baseDataStatisticsInnerServiceSMOImpl.computeEveryMonthFee(info);

        // 为每个车位设置月度费用数据
        List<Map> months = null;
        for (Map roomInfo : infos) {
            months = new ArrayList<>();
            for (Map monthInfo : monthDatas) {
                // 检查配置ID和车位ID是否匹配
                if (roomInfo.get("configId") == null || monthInfo.get("configId") == null) {
                    continue;
                }
                if (roomInfo.get("carId").equals(monthInfo.get("roomId"))
                        && roomInfo.get("configId").equals(monthInfo.get("configId"))) {
                    months.add(monthInfo);
                }
            }

            roomInfo.put("monthData", months);  // 设置月度费用数据
        }
    }
}