/**
 * 管理员查询小区报修统计命令类
 * 
 * 该命令类用于处理管理员查询小区报修统计数据的请求，包括权限验证和数据统计功能。
 * 主要功能包括：
 * 1. 验证管理员权限
 * 2. 处理查询时间范围
 * 3. 获取小区列表并统计各小区的报修数量
 * 4. 返回格式化的统计结果
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.report.cmd.admin;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.store.StoreDto;
import com.java110.intf.community.ICommunityInnerServiceSMO;
import com.java110.intf.store.IStoreInnerServiceSMO;
import com.java110.report.statistics.IBaseDataStatistics;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Java110Cmd(serviceCode = "admin.queryCommunityRepair")
public class QueryCommunityRepairCmd extends Cmd {

    @Autowired
    private IStoreInnerServiceSMO storeInnerServiceSMOImpl;

    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    @Autowired
    private IBaseDataStatistics baseDataStatisticsImpl;

    /**
     * 请求参数验证方法
     * 
     * 该方法主要完成以下验证逻辑：
     * 1. 验证当前用户是否为系统管理员
     * 2. 处理查询时间范围，如果没有提供时间范围则设置默认值为当天
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置上下文信息
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当权限验证失败时抛出命令异常
     * @throws ParseException 当时间解析异常时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 从上下文中获取店铺ID
        String storeId = CmdContextUtils.getStoreId(context);

        // 验证当前用户是否为系统管理员
        StoreDto storeDto = new StoreDto();
        storeDto.setStoreId(storeId);
        storeDto.setStoreTypeCd(StoreDto.STORE_TYPE_ADMIN);
        int count = storeInnerServiceSMOImpl.getStoreCount(storeDto);
        if (count < 1) {
            throw new CmdException("非法操作，请用系统管理员账户操作");
        }

        // 处理查询时间范围，如果没有提供开始时间则设置默认值为当天
        if (!reqJson.containsKey("startTime")) {
            String startTime = DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_B);
            String endTime = startTime + " 23:59:59";
            reqJson.put("startTime", startTime);
            reqJson.put("endTime", endTime);
        }

        // 如果开始时间为空，同样设置默认值为当天
        String startTime = reqJson.getString("startTime");
        if (StringUtil.isEmpty(startTime)) {
            startTime = DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_B);
            String endTime = startTime + " 23:59:59";
            reqJson.put("startTime", startTime);
            reqJson.put("endTime", endTime);
        }
    }

    /**
     * 执行查询小区报修统计命令
     * 
     * 该方法主要完成以下功能：
     * 1. 获取所有小区列表
     * 2. 统计每个小区的报修数量
     * 3. 处理统计结果并返回
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行异常时抛出
     * @throws ParseException 当时间解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 查询所有小区信息
        CommunityDto communityDto = new CommunityDto();
        communityDto.setPage(1);
        communityDto.setRow(100);
        List<CommunityDto> communityDtos = communityInnerServiceSMOImpl.queryCommunitys(communityDto);
        List<Map> datas = null;
        
        // 如果没有查询到小区数据，返回默认的空数据
        if (communityDtos == null || communityDtos.isEmpty()) {
            datas = new ArrayList<>();
            Map data = new HashMap();
            data.put("communityId", "-1");
            data.put("communityName", "未添加小区");
            data.put("count", 0);

            datas.add(data);
            context.setResponseEntity(ResultVo.createResponseEntity(datas));
            return;
        }

        // 提取所有小区ID
        List<String> communityIds = new ArrayList<>();
        for (CommunityDto tmpCommunityDto : communityDtos) {
            communityIds.add(tmpCommunityDto.getCommunityId());
        }

        // 将小区ID数组添加到请求参数中，用于后续统计查询
        reqJson.put("communityIds", communityIds.toArray(new String[communityIds.size()]));
        // 调用统计服务获取小区报修数量
        datas = baseDataStatisticsImpl.getCommunityRepairCount(reqJson);

        // 如果没有统计到数据，为前10个小区创建默认的统计结果
        if (datas == null || datas.isEmpty()) {
            datas = new ArrayList<>();
            for (int communityIndex = 0; communityIndex < communityDtos.size(); communityIndex++) {
                // 只处理前10个小区，避免数据量过大
                if (communityIndex > 9) {
                    continue;
                }
                Map data = new HashMap();
                data.put("communityId", communityDtos.get(communityIndex).getCommunityId());
                data.put("communityName", communityDtos.get(communityIndex).getName());
                data.put("count", 0);
                datas = new ArrayList<>();
                datas.add(data);
            }
            context.setResponseEntity(ResultVo.createResponseEntity(datas));
            return;
        }

        // 为统计结果添加小区名称信息
        for (Map data : datas) {
            for (CommunityDto tmpCommunityDto : communityDtos) {
                if (data.get("communityId").equals(tmpCommunityDto.getCommunityId())) {
                    data.put("communityName", tmpCommunityDto.getName());
                }
            }
        }

        // 设置响应结果
        context.setResponseEntity(ResultVo.createResponseEntity(datas));
    }
}