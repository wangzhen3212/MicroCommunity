package com.java110.report.cmd.owner;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.owner.OwnerDto;
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
 * 查询业主变更记录命令类
 * 
 * 该类继承自Cmd基类，用于处理查询业主历史变更记录的请求
 * 通过注解@Java110Cmd声明服务代码为"owner.queryHisOwner"
 * 主要功能包括参数验证和执行查询业务逻辑
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "owner.queryHisOwner")
public class QueryHisOwnerCmd extends Cmd {

    /**
     * 报表社区内部服务接口，用于查询业主变更记录相关数据
     */
    @Autowired
    private IReportCommunityInnerServiceSMO reportCommunityInnerServiceSMOImpl;

    /**
     * 验证请求参数方法
     * 
     * 该方法用于验证请求参数是否完整和有效
     * 主要验证小区ID是否存在，并调用父类的属性验证方法
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行出现异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求参数中必须包含小区ID
        Assert.hasKeyAndValue(reqJson,"communityId","未包含小区");
        // 调用父类的属性验证方法进行额外验证
        super.validateProperty(context);
    }

    /**
     * 执行命令方法
     * 
     * 该方法负责处理查询业主变更记录的核心业务逻辑
     * 包括分页查询业主变更记录、统计总数并返回响应结果
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行出现异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 获取分页行数参数
        int row = reqJson.getInteger("row");
        // 将请求JSON对象转换为OwnerDto对象
        OwnerDto ownerDto = BeanConvertUtil.covertBean(reqJson, OwnerDto.class);

        // 查询业主变更记录总数
        int total = reportCommunityInnerServiceSMOImpl.queryHisOwnerCount(ownerDto);
        
        List<OwnerDto> ownerDtos = null;
        
        // 根据查询结果总数决定是否查询详细数据
        if (total > 0) {
            // 总数大于0时，查询业主变更记录列表
            ownerDtos = reportCommunityInnerServiceSMOImpl.queryHisOwners(ownerDto);
        } else {
            // 总数为0时，返回空列表
            ownerDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页行数，向上取整
        // 创建包含分页信息和数据的响应实体
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity((int) Math.ceil((double) total / (double) row), total, ownerDtos);
        // 将响应实体设置到上下文中
        context.setResponseEntity(responseEntity);
    }
}