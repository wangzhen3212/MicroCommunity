package com.java110.community.cmd.ownerRepair;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.repair.RepairDto;
import com.java110.intf.community.IRepairInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 查询员工待办维修工单命令类
 * 
 * 该类用于处理员工待办维修工单的查询请求，通过接收小区ID和员工ID参数，
 * 查询该员工在当前小区下的待处理维修工单信息，并返回分页结果
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023
 */
@Java110CmdDoc(title = "员工待办维修单",
        description = "查询员工待办维修单",
        httpMethod = "get",
        url = "http://{ip}:{port}/app/ownerRepair.listUndoRepairsByStaff",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "ownerRepair.listUndoRepairsByStaff",
        seq = 17
)

@Java110ParamsDoc(
        params = {
                @Java110ParamDoc(name = "communityId", length = 30, remark = "必填，小区ID"),
                @Java110ParamDoc(name = "staffId", length = 30, remark = "必填，员工ID"),
        })

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "data", type = "Array", remark = "有效数据"),
                @Java110ParamDoc(parentNodeName = "data",name = "appointmentTime", type = "String", remark = "预约时间"),
                @Java110ParamDoc(parentNodeName = "data",name = "repairName", type = "String", remark = "报修名称"),
                @Java110ParamDoc(parentNodeName = "data",name = "repairId", type = "String", remark = "工单ID"),
                @Java110ParamDoc(parentNodeName = "data",name = "context", type = "String", remark = "报修内容"),
        }
)

@Java110ExampleDoc(
        reqBody="{'username':'wuxw','passwd':'admin'}",
        resBody="{\"code\":0,\"data\":[{\"appointmentTime\":\"2023-02-18 23:34:23\",\"bId\":\"202023021803780003\"," +
                "\"communityId\":\"2023013154290059\",\"context\":\"飞蛾打印机报修单测试\",\"page\":-1,\"preRuId\":\"832023021884560007\"," +
                "\"preStaffId\":\"302023013185270087\",\"preStaffName\":\"0131演示物业\",\"publicArea\":\"T\",\"records\":0,\"repairChannel\":\"T\"," +
                "\"repairId\":\"822023021809940002\",\"repairName\":\"飞蛾打印机报修单测试\",\"repairObjId\":\"2023013154290059\"," +
                "\"repairObjName\":\"0131培训小区\",\"repairObjType\":\"001\",\"repairType\":\"102023020172340620\",\"repairTypeName\":\"水电报修\"," +
                "\"repairWay\":\"200\",\"returnVisitFlag\":\"001\",\"returnVisitFlagName\":\"都不回访\",\"row\":0,\"ruId\":\"832023021876140009\"," +
                "\"startRuId\":\"832023021897810004\",\"state\":\"1100\",\"stateName\":\"接单\",\"statusCd\":\"0\",\"tel\":\"18909714455\"," +
                "\"total\":0}],\"msg\":\"成功\",\"page\":0,\"records\":1,\"rows\":0,\"total\":1}"
)
@Java110Cmd(serviceCode = "ownerRepair.listUndoRepairsByStaff")
public class ListUndoRepairsByStaffCmd extends Cmd {

    /**
     * 维修服务接口，用于查询维修工单相关信息
     */
    @Autowired
    private IRepairInnerServiceSMO repairInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整和有效，包括分页信息和必填字段
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 数据流上下文对象，用于处理请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证分页信息
        super.validatePageInfo(reqJson);
        // 验证小区ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "请求中未包含小区ID");
        // 验证员工ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "staffId", "请求中未包含员工信息");
    }

    /**
     * 执行查询员工待办维修工单命令
     * 
     * 根据小区ID和员工ID查询该员工的待办维修工单列表，支持分页查询
     * 
     * @param event 命令事件对象
     * @param context 数据流上下文对象，用于设置响应数据
     * @param reqJson 请求参数的JSON对象，包含查询条件和分页信息
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当数据解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求JSON转换为RepairDto对象
        RepairDto ownerRepairDto = BeanConvertUtil.covertBean(reqJson, RepairDto.class);
        
        // 查询员工待办维修工单总数
        int count = repairInnerServiceSMOImpl.queryStaffRepairsCount(ownerRepairDto);
        
        List<RepairDto> ownerRepairs = null;
        
        // 根据查询结果数量决定是否查询详细数据
        if (count > 0) {
            // 查询员工待办维修工单列表
            ownerRepairs = repairInnerServiceSMOImpl.queryStaffRepairs(ownerRepairDto);
        } else {
            // 如果没有数据，返回空列表
            ownerRepairs = new ArrayList<>();
        }
        
        // 计算总页数：总记录数 / 每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        
        // 创建响应实体，包含分页信息和数据
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(totalPage, count, ownerRepairs);
        
        // 设置响应数据到上下文
        context.setResponseEntity(responseEntity);
    }
}