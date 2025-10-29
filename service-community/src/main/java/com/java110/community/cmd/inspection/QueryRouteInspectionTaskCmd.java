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
 * 该类继承自Cmd基类，用于处理查询巡检点关联的巡检任务请求
 * 主要功能包括参数验证和查询巡检任务数据
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode inspection.queryRouteInspectionTask
 */
@Java110Cmd(serviceCode = "inspection.queryRouteInspectionTask")
public class QueryRouteInspectionTaskCmd extends Cmd {

    /**
     * 巡检任务服务接口，用于调用巡检任务相关的业务方法
     */
    @Autowired
    private IInspectionTaskV1InnerServiceSMO inspectionTaskV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数是否完整和有效，包括分页参数、必填字段等
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现异常时抛出
     * @throws ParseException 当参数解析异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证属性参数
        super.validateProperty(context);
        // 验证巡检路线ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "inspectionRouteId", "未包含巡检点");
        // 验证小区ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
    }

    /**
     * 命令执行方法
     * 
     * 处理查询巡检点关联的巡检任务业务逻辑，包括数据查询、结果封装和响应设置
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现异常时抛出
     * @throws ParseException 当参数解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求JSON对象转换为巡检任务数据传输对象
        InspectionTaskDto inspectionTaskDto = BeanConvertUtil.covertBean(reqJson, InspectionTaskDto.class);

        // 查询巡检任务总数
        int count = inspectionTaskV1InnerServiceSMOImpl.queryRouteInspectionTasksCount(inspectionTaskDto);

        List<InspectionTaskDto> inspectionTaskDtos = null;

        // 根据查询结果数量判断是否查询详细数据
        if (count > 0) {
            // 查询巡检任务详细列表数据
            inspectionTaskDtos = BeanConvertUtil.covertBeanList(
                inspectionTaskV1InnerServiceSMOImpl.queryRouteInspectionTasks(inspectionTaskDto), 
                InspectionTaskDto.class
            );
        } else {
            // 如果没有数据，返回空列表
            inspectionTaskDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 封装返回结果对象
        ResultVo resultVo = new ResultVo(totalPage, count, inspectionTaskDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应数据到上下文
        context.setResponseEntity(responseEntity);
    }
}