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

/**
 * 查询小区费用统计命令类
 * 用于管理员查询各小区的费用统计详情，包括收费笔数和收款金额等信息
 * 
 * @Java110Cmd 注解指定服务代码为"admin.queryCommunityFee"
 */
@Java110Cmd(serviceCode = "admin.queryCommunityFee")
public class QueryCommunityFeeCmd extends Cmd {

    @Autowired
    private IStoreInnerServiceSMO storeInnerServiceSMOImpl;

    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    @Autowired
    private IBaseDataStatistics baseDataStatisticsImpl;

    /**
     * 验证请求参数的有效性
     * 检查操作权限和日期参数，设置默认时间范围
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException   当命令执行异常时抛出
     * @throws ParseException 当日期解析异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从上下文中获取店铺ID
        String storeId = CmdContextUtils.getStoreId(context);

        // 验证操作权限，只有管理员账户可以操作
        StoreDto storeDto = new StoreDto();
        storeDto.setStoreId(storeId);
        storeDto.setStoreTypeCd(StoreDto.STORE_TYPE_ADMIN);
        int count = storeInnerServiceSMOImpl.getStoreCount(storeDto);
        if (count < 1) {
            throw new CmdException("非法操作，请用系统管理员账户操作");
        }

        // 检查并设置默认时间范围
        if (!reqJson.containsKey("startTime")) {
            // 如果没有开始时间，设置为当天日期
            String startTime = DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_B);
            String endTime = startTime + " 23:59:59";
            reqJson.put("startTime", startTime);
            reqJson.put("endTime", endTime);
        }

        // 如果开始时间为空，同样设置默认时间范围
        String startTime = reqJson.getString("startTime");
        if (StringUtil.isEmpty(startTime)) {
            startTime = DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_B);
            String endTime = startTime + " 23:59:59";
            reqJson.put("startTime", startTime);
            reqJson.put("endTime", endTime);
        }
    }

    /**
     * 执行查询小区费用统计的主要业务逻辑
     * 获取所有小区信息并查询对应的费用统计详情
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException   当命令执行异常时抛出
     * @throws ParseException 当日期解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 查询所有小区信息
        CommunityDto communityDto = new CommunityDto();
        communityDto.setPage(1);
        communityDto.setRow(100);
        List<CommunityDto> communityDtos = communityInnerServiceSMOImpl.queryCommunitys(communityDto);
        List<Map> datas = null;

        // 如果没有查询到小区信息，返回默认数据
        if (communityDtos == null || communityDtos.isEmpty()) {
            datas = new ArrayList<>();
            Map data = new HashMap();
            data.put("communityId", "-1");
            data.put("communityName", "未添加小区");
            data.put("count", 0);
            data.put("receivedAmount", 0);

            datas.add(data);
            context.setResponseEntity(ResultVo.createResponseEntity(datas));
            return;
        }

        // 提取所有小区ID
        List<String> communityIds = new ArrayList<>();
        for (CommunityDto tmpCommunityDto : communityDtos) {
            communityIds.add(tmpCommunityDto.getCommunityId());
        }

        // 将小区ID数组添加到请求参数中
        reqJson.put("communityIds", communityIds.toArray(new String[communityIds.size()]));
        // 查询小区费用统计详情
        datas = baseDataStatisticsImpl.getCommunityFeeDetailCount(reqJson);

        // 如果没有查询到费用统计数据，构造默认数据
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
                data.put("receivedAmount", 0);
                datas = new ArrayList<>();
                datas.add(data);
            }
            context.setResponseEntity(ResultVo.createResponseEntity(datas));
            return;
        }

        // 为费用统计数据添加小区名称
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