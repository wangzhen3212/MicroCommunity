package com.java110.report.cmd.car;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.intf.report.IReportCommunityInnerServiceSMO;
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
 * 查询车辆变更记录命令类
 * 该命令用于查询管理员权限下的车辆历史变更记录信息
 * 实现了车辆历史数据的查询和分页返回功能
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode car.queryAdminHisOwnerCar
 */
@Java110Cmd(serviceCode = "car.queryAdminHisOwnerCar")
public class QueryAdminHisOwnerCarCmd extends Cmd {

    /**
     * 报表社区内部服务接口，用于查询车辆历史记录
     */
    @Autowired
    private IReportCommunityInnerServiceSMO reportCommunityInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性和管理员权限
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行出现异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证管理员权限
        super.validateAdmin(context);
    }

    /**
     * 命令执行方法
     * 查询车辆历史变更记录并返回分页结果
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行出现异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 获取每页显示行数
        int row = reqJson.getInteger("row");
        // 将请求JSON转换为OwnerCarDto对象
        OwnerCarDto ownerCarDto = BeanConvertUtil.covertBean(reqJson, OwnerCarDto.class);

        // 查询符合条件的车辆历史记录总数
        int total = reportCommunityInnerServiceSMOImpl.queryHisOwnerCarCount(ownerCarDto);
        
        List<OwnerCarDto> ownerCarDtos = null;
        
        // 如果存在记录，则查询详细数据
        if (total > 0) {
            ownerCarDtos = reportCommunityInnerServiceSMOImpl.queryHisOwnerCars(ownerCarDto);
        } else {
            // 如果没有记录，返回空列表
            ownerCarDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页行数，向上取整
        int totalPage = (int) Math.ceil((double) total / (double) row);
        // 创建响应实体，包含分页信息和数据
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(totalPage, total, ownerCarDtos);
        // 设置响应到上下文
        context.setResponseEntity(responseEntity);
    }
}