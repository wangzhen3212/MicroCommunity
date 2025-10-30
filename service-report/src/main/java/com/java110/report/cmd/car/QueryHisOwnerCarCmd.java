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
 * 
 * 该类负责处理车辆历史变更记录的查询请求，包括参数验证和数据查询功能
 * 通过服务码"car.queryHisOwnerCar"标识，用于查询小区内车辆的历史变更信息
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "car.queryHisOwnerCar")
public class QueryHisOwnerCarCmd extends Cmd {

    /**
     * 报表社区内部服务接口，用于查询车辆历史记录
     */
    @Autowired
    private IReportCommunityInnerServiceSMO reportCommunityInnerServiceSMOImpl;

    /**
     * 验证请求参数的有效性
     * 
     * 该方法用于验证传入的请求参数是否包含必要的字段，并进行基本的参数校验
     * 
     * @param event 命令事件对象，包含请求的相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当参数解析失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求参数中必须包含小区ID
        Assert.hasKeyAndValue(reqJson,"communityId","未包含小区");
        // 调用父类的属性验证方法
        super.validateProperty(context);
    }

    /**
     * 执行车辆历史记录查询命令
     * 
     * 该方法负责处理车辆历史记录的查询业务逻辑，包括分页查询和数据返回
     * 
     * @param event 命令事件对象，包含请求的相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当参数解析失败时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 获取分页参数：每页显示的行数
        int row = reqJson.getInteger("row");
        // 将请求参数转换为OwnerCarDto对象，便于后续查询
        OwnerCarDto ownerCarDto = BeanConvertUtil.covertBean(reqJson, OwnerCarDto.class);

        // 查询符合条件的车辆历史记录总数
        int total = reportCommunityInnerServiceSMOImpl.queryHisOwnerCarCount(ownerCarDto);
        
        // 定义车辆历史记录列表
        List<OwnerCarDto> ownerCarDtos = null;
        
        // 根据查询结果总数判断是否需要查询详细数据
        if (total > 0) {
            // 如果存在数据，则查询具体的车辆历史记录列表
            ownerCarDtos = reportCommunityInnerServiceSMOImpl.queryHisOwnerCars(ownerCarDto);
        } else {
            // 如果没有数据，返回空列表
            ownerCarDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页行数，向上取整
        // 创建响应实体，包含分页信息和数据列表
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity((int) Math.ceil((double) total / (double) row), total, ownerCarDtos);
        // 将响应实体设置到上下文中
        context.setResponseEntity(responseEntity);
    }
}