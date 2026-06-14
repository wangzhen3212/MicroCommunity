package com.java110.community.cmd.inspection;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.inspection.InspectionTaskDetailDto;
import com.java110.intf.community.IInspectionTaskDetailV1InnerServiceSMO;
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
 * 查询巡检任务明细命令类
 * 负责处理管理员查询巡检任务明细的请求，包括参数验证和业务逻辑处理
 * 服务代码：inspection.queryAdminInspectionTaskDetail
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "inspection.queryAdminInspectionTaskDetail")
public class QueryAdminInspectionTaskDetailCmd extends Cmd {

    /**
     * 巡检任务明细服务接口，用于查询巡检任务明细数据
     */
    @Autowired
    private IInspectionTaskDetailV1InnerServiceSMO inspectionTaskDetailV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 验证分页参数、管理员权限和必要的业务参数
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     * @throws ParseException 当参数解析失败时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证分页参数（页码和每页记录数）
        super.validatePageInfo(reqJson);
        // 验证管理员权限
        super.validateAdmin(context);
        // 验证请求中必须包含任务ID参数
        Assert.hasKeyAndValue(reqJson, "taskId", "未包含任务ID");
    }

    /**
     * 执行查询巡检任务明细命令
     * 根据查询条件获取巡检任务明细列表，并返回分页结果
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现错误时抛出
     * @throws ParseException 当数据解析过程中出现错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求JSON转换为巡检任务明细数据传输对象
        InspectionTaskDetailDto inspectionTaskDetailDto = BeanConvertUtil.covertBean(reqJson, InspectionTaskDetailDto.class);
        
        // 查询符合条件的巡检任务明细总数
        int count = inspectionTaskDetailV1InnerServiceSMOImpl.queryInspectionTaskDetailsCount(inspectionTaskDetailDto);
        
        List<InspectionTaskDetailDto> inspectionTaskDetails = null;
        
        // 如果存在数据，则查询明细列表
        if (count > 0) {
            // 查询巡检任务明细列表，并将结果转换为DTO列表
            inspectionTaskDetails = BeanConvertUtil.covertBeanList(
                    inspectionTaskDetailV1InnerServiceSMOImpl.queryInspectionTaskDetails(inspectionTaskDetailDto),
                    InspectionTaskDetailDto.class);
        } else {
            // 如果没有数据，返回空列表
            inspectionTaskDetails = new ArrayList<>();
        }
        
        // 计算总页数：总记录数 / 每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 构建返回结果对象，包含分页信息和数据列表
        ResultVo resultVo = new ResultVo(totalPage, count, inspectionTaskDetails);

        // 创建HTTP响应实体，设置响应状态为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到上下文中，返回给调用方
        context.setResponseEntity(responseEntity);
    }
}