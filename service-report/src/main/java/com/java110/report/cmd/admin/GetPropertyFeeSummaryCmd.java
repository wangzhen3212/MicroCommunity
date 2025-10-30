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
 * 物业费台账查询命令类
 * 
 * 该类用于查询物业费汇总数据，包括物业费的月度统计、小区和物业公司信息等
 * 通过注解@Java110Cmd标识为命令类，服务码为admin.getPropertyFeeSummary
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "admin.getPropertyFeeSummary")
public class GetPropertyFeeSummaryCmd extends Cmd {

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
     * 验证用户权限和分页信息，确保只有系统管理员可以执行此操作
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证分页信息
        super.validatePageInfo(reqJson);
        
        // 获取当前用户的店铺ID
        String storeId = CmdContextUtils.getStoreId(context);

        // 构建店铺查询条件
        StoreDto storeDto = new StoreDto();
        storeDto.setStoreId(storeId);
        storeDto.setStoreTypeCd(StoreDto.STORE_TYPE_ADMIN);
        
        // 查询管理员店铺数量
        int count = storeInnerServiceSMOImpl.getStoreCount(storeDto);
        
        // 如果不是管理员，抛出异常
        if (count < 1) {
            throw new CmdException("非法操作，请用系统管理员账户操作");
        }
    }

    /**
     * 执行命令方法
     * 
     * 查询物业费汇总数据，包括数据统计、月度费用计算和小区物业信息处理
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 获取分页行数
        int row = reqJson.getIntValue("row");
        
        // 设置默认查询时间范围（当前年份）
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);

        // 设置默认开始时间（当年1月1日）
        if(!reqJson.containsKey("startTime") || StringUtil.isEmpty(reqJson.getString("startTime"))){
            reqJson.put("startTime", year + "-01-01");
        }
        
        // 设置默认结束时间（次年1月1日）
        if(!reqJson.containsKey("endTime") || StringUtil.isEmpty(reqJson.getString("endTime"))){
            reqJson.put("endTime", (year + 1) + "-01-01");
        }
        
        // 处理房屋名称查询条件
        if(reqJson.containsKey("roomName") && !StringUtil.isEmpty(reqJson.getString("roomName"))){
            // 房屋名称格式：楼栋号-单元号-房间号
            String[] roomNames = reqJson.getString("roomName").split("-",3);
            
            // 验证房屋名称格式是否正确
            if(roomNames.length != 3){
                throw new CmdException("房屋名称查询条件错误");
            }
            
            // 将房屋名称拆分为楼栋号、单元号、房间号
            reqJson.put("floorNum",roomNames[0]);
            reqJson.put("unitNum",roomNames[1]);
            reqJson.put("roomNum",roomNames[2]);
        }

        // 查询物业费汇总数据总数量
        int total = baseDataStatisticsInnerServiceSMOImpl.getPropertyFeeSummaryDataCount(reqJson);

        List<Map> infos = null;
        
        // 如果有数据，查询详细数据；否则返回空列表
        if (total > 0) {
            infos = baseDataStatisticsInnerServiceSMOImpl.getPropertyFeeSummaryData(reqJson);
        } else {
            infos = new ArrayList<>();
        }

        // 计算每月物业费情况
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
     * 为查询结果添加小区名称和物业公司名称信息
     * 
     * @param infos 物业费数据列表
     */
    private void computeCommunityAndProperty(List<Map> infos) {
        // 查询所有小区及其物业信息
        CommunityDto communityDto = new CommunityDto();
        List<CommunityDto> communityDtos = communityV1InnerServiceSMOImpl.queryCommunityAndPropertys(communityDto);

        // 如果没有小区数据，直接返回
        if (communityDtos == null || communityDtos.isEmpty()) {
            return;
        }

        // 查询所有物业公司信息
        StoreDto storeDto = new StoreDto();
        storeDto.setStoreTypeCd(StoreDto.STORE_TYPE_PROPERTY);
        List<StoreDto> storeDtos = storeV1InnerServiceSMOImpl.queryStores(storeDto);

        // 为每个小区设置物业公司名称
        for(CommunityDto tmpCommunityDto : communityDtos){
            for(StoreDto tmpStoreDto: storeDtos){
                // 根据店铺ID匹配物业公司
                if(tmpStoreDto.getStoreId().equals(tmpCommunityDto.getStoreId())){
                    tmpCommunityDto.setStoreName(tmpStoreDto.getName());
                }
            }
        }

        // 为每个物业费记录设置小区名称和物业公司信息
        for(Map info: infos){
            for(CommunityDto tmpCommunityDto : communityDtos){
                // 根据小区ID匹配小区信息
                if(info.get("communityId").equals(tmpCommunityDto.getCommunityId())){
                    info.put("communityName",tmpCommunityDto.getName());
                    info.put("storeId",tmpCommunityDto.getStoreId());
                    info.put("storeName",tmpCommunityDto.getStoreName());
                }
            }
        }
    }

    /**
     * 计算每月物业费情况
     * 
     * 为每个房屋记录计算指定时间范围内的月度物业费数据
     * 
     * @param infos 物业费数据列表
     * @param reqJson 请求参数JSON对象，包含时间范围
     */
    private void computeEveryMonthFee(List<Map> infos, JSONObject reqJson) {
        // 如果数据为空，直接返回
        if (infos == null || infos.isEmpty()) {
            return;
        }

        // 收集所有房屋ID
        List<String> roomIds = new ArrayList<>();
        for (Map info : infos) {
            roomIds.add(info.get("roomId").toString());
        }

        // 构建月度费用查询参数
        Map info = new HashMap();
        info.put("roomIds", roomIds.toArray(new String[roomIds.size()]));
        info.put("startTime", reqJson.getString("startTime"));
        info.put("endTime", reqJson.getString("endTime"));

        // 查询月度费用数据
        List<Map> monthDatas = baseDataStatisticsInnerServiceSMOImpl.computeEveryMonthFee(info);

        List<Map> months = null;
        
        // 为每个房屋记录匹配对应的月度费用数据
        for (Map roomInfo : infos) {
            months = new ArrayList<>();
            for (Map monthInfo : monthDatas) {
                // 跳过配置ID为空的情况
                if (roomInfo.get("configId") == null || monthInfo.get("configId") == null) {
                    continue;
                }
                
                // 根据房屋ID和配置ID匹配月度数据
                if (roomInfo.get("roomId").equals(monthInfo.get("roomId"))
                        && roomInfo.get("configId").equals(monthInfo.get("configId"))) {
                    months.add(monthInfo);
                }
            }

            // 将月度数据添加到房屋记录中
            roomInfo.put("monthData", months);
        }
    }
}