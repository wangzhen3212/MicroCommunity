package com.java110.community.cmd.inspection;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.inspection.InspectionPlanDto;
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
 * 该类用于处理查询指定巡检点下巡检任务的业务逻辑，包括参数验证和数据查询
 * 通过服务码 inspection.queryPointInspectionTask 被调用
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "inspection.queryPointInspectionTask")
public class QueryPointInspectionTaskCmd extends Cmd {

    /**
     * 巡检任务服务接口，用于查询巡检任务相关数据
     */
    @Autowired
    private IInspectionTaskV1InnerServiceSMO inspectionTaskV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整，包括分页信息和必要字段
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现业务异常时抛出
     * @throws ParseException 当参数解析异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证分页参数
        super.validatePageInfo(reqJson);

        // 验证必须包含巡检点ID
        Assert.hasKeyAndValue(reqJson, "inspectionId", "未包含巡检点");
        // 验证必须包含小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
    }

    /**
     * 命令执行方法
     * 
     * 根据巡检点ID查询对应的巡检任务列表，支持分页查询
     * 返回包含总页数、总记录数和任务列表的响应结果
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于设置响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现业务异常时抛出
     * @throws ParseException 当参数解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求JSON转换为巡检任务DTO对象
        InspectionTaskDto inspectionTaskDto = BeanConvertUtil.covertBean(reqJson, InspectionTaskDto.class);

        // 查询巡检任务总记录数
        int count = inspectionTaskV1InnerServiceSMOImpl.queryPointInspectionTasksCount(inspectionTaskDto);

        List<InspectionTaskDto> inspectionTaskDtos = null;

        // 根据查询结果数量决定是否查询详细数据
        if (count > 0) {
            // 查询巡检任务详细列表并转换为DTO对象
            inspectionTaskDtos = BeanConvertUtil.covertBeanList(inspectionTaskV1InnerServiceSMOImpl.queryPointInspectionTasks(inspectionTaskDto), InspectionTaskDto.class);
        } else {
            // 如果没有记录，返回空列表
            inspectionTaskDtos = new ArrayList<>();
        }
        
        // 计算总页数：总记录数 / 每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 构建返回结果对象
        ResultVo resultVo = new ResultVo(totalPage, count, inspectionTaskDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应数据到上下文
        context.setResponseEntity(responseEntity);
    }
}