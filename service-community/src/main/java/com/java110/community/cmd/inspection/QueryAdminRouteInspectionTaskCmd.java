package com.java110.community.cmd.inspection;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.inspection.InspectionTaskDto;
import com.java110.intf.community.IInspectionTaskV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 查询巡检点对应的巡检任务命令类
 * 
 * 该命令类用于处理查询巡检路线对应的巡检任务请求，主要功能包括：
 * 1. 验证请求参数的有效性
 * 2. 查询指定巡检路线的巡检任务列表
 * 3. 返回分页格式的查询结果
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "inspection.queryAdminRouteInspectionTask")
public class QueryAdminRouteInspectionTaskCmd extends Cmd {

    /**
     * 巡检任务服务接口，用于调用巡检任务相关的业务方法
     */
    @Autowired
    private IInspectionTaskV1InnerServiceSMO inspectionTaskV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 该方法用于验证请求参数的合法性，包括：
     * 1. 验证分页参数
     * 2. 验证管理员权限
     * 3. 验证必要的业务参数
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现业务异常时抛出
     * @throws ParseException 当参数解析异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证分页参数，确保分页信息完整
        super.validatePageInfo(reqJson);
        // 验证管理员权限，确保当前用户具有管理员权限
        super.validateAdmin(context);
        // 验证巡检路线ID参数，确保请求中包含必要的巡检路线信息
        Assert.hasKeyAndValue(reqJson, "inspectionRouteId", "未包含巡检点");
    }

    /**
     * 命令执行方法
     * 
     * 该方法负责执行主要的业务逻辑：
     * 1. 将请求参数转换为业务对象
     * 2. 查询巡检任务总数
     * 3. 根据分页信息查询巡检任务列表
     * 4. 构建并返回分页响应结果
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现业务异常时抛出
     * @throws ParseException 当参数解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求JSON参数转换为巡检任务数据传输对象
        InspectionTaskDto inspectionTaskDto = BeanConvertUtil.covertBean(reqJson, InspectionTaskDto.class);

        // 查询指定巡检路线的巡检任务总数，用于分页计算
        int count = inspectionTaskV1InnerServiceSMOImpl.queryRouteInspectionTasksCount(inspectionTaskDto);

        List<InspectionTaskDto> inspectionTaskDtos = null;

        // 根据查询结果数量决定如何处理数据
        if (count > 0) {
            // 如果存在巡检任务，则查询具体的巡检任务列表并转换为DTO对象
            inspectionTaskDtos = BeanConvertUtil.covertBeanList(
                inspectionTaskV1InnerServiceSMOImpl.queryRouteInspectionTasks(inspectionTaskDto), 
                InspectionTaskDto.class
            );
        } else {
            // 如果没有查询到巡检任务，返回空列表
            inspectionTaskDtos = new ArrayList<>();
        }

        // 构建分页结果对象，计算总页数
        // 使用Math.ceil确保向上取整，正确处理不满一页的情况
        ResultVo resultVo = new ResultVo(
            (int) Math.ceil((double) count / (double) reqJson.getInteger("row")), // 总页数
            count, // 总记录数
            inspectionTaskDtos // 当前页数据列表
        );

        // 创建HTTP响应实体，设置响应状态为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到上下文中，供框架后续处理
        context.setResponseEntity(responseEntity);
    }
}