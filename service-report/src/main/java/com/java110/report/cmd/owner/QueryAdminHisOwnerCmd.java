package com.java110.report.cmd.owner;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
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
 * 该类负责处理业主历史记录查询请求，通过调用报表服务获取业主变更信息，
 * 并返回分页格式的查询结果。主要用于管理系统中的业主信息变更追踪功能。
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "owner.queryAdminHisOwner")
public class QueryAdminHisOwnerCmd extends Cmd {

    /**
     * 报表社区内部服务接口，用于查询业主历史记录
     */
    @Autowired
    private IReportCommunityInnerServiceSMO reportCommunityInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证用户权限和分页参数的有效性，确保请求的合法性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现业务异常时抛出
     * @throws ParseException 当参数解析失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证管理员权限
        super.validateAdmin(context);
        // 验证分页参数
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 
     * 处理业主历史记录查询请求，获取业主变更记录并返回分页结果
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现业务异常时抛出
     * @throws ParseException 当参数解析失败时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 获取每页显示行数
        int row = reqJson.getInteger("row");
        // 将请求参数转换为OwnerDto对象
        OwnerDto ownerDto = BeanConvertUtil.covertBean(reqJson, OwnerDto.class);

        // 查询业主历史记录总数
        int total = reportCommunityInnerServiceSMOImpl.queryHisOwnerCount(ownerDto);
        
        List<OwnerDto> ownerDtos = null;
        // 根据查询结果总数判断是否需要查询详细数据
        if (total > 0) {
            // 查询业主历史记录详情
            ownerDtos = reportCommunityInnerServiceSMOImpl.queryHisOwners(ownerDto);
        } else {
            // 如果没有数据，返回空列表
            ownerDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页行数，向上取整
        int totalPage = (int) Math.ceil((double) total / (double) row);
        // 创建响应实体，包含分页信息和数据
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(totalPage, total, ownerDtos);
        // 设置响应结果到上下文
        context.setResponseEntity(responseEntity);
    }
}