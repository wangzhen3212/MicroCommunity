package com.java110.store.cmd.propertyIndex;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.complaint.ComplaintDto;
import com.java110.intf.store.IComplaintInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 物业首页投诉统计查询命令类
 * 
 * 该类用于处理物业首页的投诉相关统计查询请求，包括查询全部投诉数量、待处理投诉数量和已完成投诉数量
 * 通过调用投诉内部服务接口获取统计数据并返回给前端
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode propertyIndex.queryComplaintIndex
 */
@Java110Cmd(serviceCode = "propertyIndex.queryComplaintIndex")
public class QueryComplaintIndexCmd extends Cmd {

    /**
     * 投诉内部服务接口，用于查询投诉相关数据
     */
    @Autowired
    private IComplaintInnerServiceSMO complaintInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数中是否包含必要的小区信息
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含communityId字段
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区信息");
    }

    /**
     * 命令执行方法
     * 
     * 查询指定小区的投诉统计信息，包括：
     * 1. 全部投诉数量
     * 2. 待处理投诉数量
     * 3. 已完成投诉数量
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现错误时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 创建返回结果对象
        JSONObject paramOut = new JSONObject();

        // 查询全部投诉数量
        ComplaintDto complaintDto = new ComplaintDto();
        complaintDto.setCommunityId(reqJson.getString("communityId"));
        int allCount = complaintInnerServiceSMOImpl.queryComplaintsCount(complaintDto);
        paramOut.put("allComplaintCount", allCount);

        // 查询待处理投诉数量
        complaintDto = new ComplaintDto();
        complaintDto.setCommunityId(reqJson.getString("communityId"));
        // 设置投诉状态为待处理状态
        complaintDto.setState(ComplaintDto.STATE_WAIT);
        int waitCount = complaintInnerServiceSMOImpl.queryComplaintsCount(complaintDto);
        paramOut.put("waitComplaintCount", waitCount);

        // 查询已完成投诉数量
        complaintDto = new ComplaintDto();
        complaintDto.setCommunityId(reqJson.getString("communityId"));
        // 设置投诉状态为已完成状态
        complaintDto.setState(ComplaintDto.STATE_FINISH);
        int finishCount = complaintInnerServiceSMOImpl.queryComplaintsCount(complaintDto);
        paramOut.put("finishComplaintCount", finishCount);
        
        // 设置响应结果
        context.setResponseEntity(ResultVo.createResponseEntity(paramOut));
    }
}