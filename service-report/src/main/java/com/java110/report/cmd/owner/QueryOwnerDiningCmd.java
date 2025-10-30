package com.java110.report.cmd.owner;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.owner.OwnerDto;
import com.java110.intf.report.IReportOrderStatisticsInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 业主家庭成员就餐明细查询命令类
 * 负责处理业主家庭成员就餐明细的查询请求，包括参数验证和数据查询
 * 服务编码：owner.queryOwnerDining
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "owner.queryOwnerDining")
public class QueryOwnerDiningCmd extends Cmd {

    /**
     * 报表订单统计内部服务接口
     * 用于查询业主就餐相关统计数据
     */
    @Autowired
    private IReportOrderStatisticsInnerServiceSMO reportOrderStatisticsInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性和完整性，并对日期参数进行格式化处理
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现异常时抛出
     * @throws ParseException 当日期解析出现异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证必须包含小区ID参数
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");

        // 检查是否包含开始日期和结束日期参数
        if(reqJson.containsKey("startDate") && reqJson.containsKey("endDate")) {
            String startDate = reqJson.getString("startDate");
            String endDate = reqJson.getString("endDate");
            
            // 如果开始日期不为空且不包含时间部分，则添加起始时间
            if (!StringUtil.isEmpty(startDate) && !startDate.contains(":")) {
                startDate += " 00:00:00";
                reqJson.put("startDate", startDate);
            }
            
            // 如果结束日期不为空且不包含时间部分，则添加结束时间
            if (!StringUtil.isEmpty(endDate) && !endDate.contains(":")) {
                endDate += " 23:59:59";
                reqJson.put("endDate", endDate);
            }
        }
    }

    /**
     * 命令执行方法
     * 处理业主就餐明细查询业务逻辑，包括数据查询和结果返回
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现异常时抛出
     * @throws ParseException 当日期解析出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 获取分页行数参数
        int row = reqJson.getInteger("row");
        // 将请求参数转换为OwnerDto对象
        OwnerDto ownerDto = BeanConvertUtil.covertBean(reqJson, OwnerDto.class);

        // 查询符合条件的总记录数
        int total = reportOrderStatisticsInnerServiceSMOImpl.getOwnerDiningCount(ownerDto);
        
        List<Map> infos = null;
        // 根据查询结果决定是否查询详细数据
        if (total > 0) {
            // 查询业主就餐明细数据
            infos = reportOrderStatisticsInnerServiceSMOImpl.getOwnerDinings(ownerDto);
        } else {
            // 如果没有数据，返回空列表
            infos = new ArrayList<>();
        }

        // 计算总页数并创建响应实体
        // 使用Math.ceil确保向上取整，正确处理余数情况
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity((int) Math.ceil((double) total / (double) row), total, infos);
        // 设置响应实体到上下文
        context.setResponseEntity(responseEntity);
    }
}