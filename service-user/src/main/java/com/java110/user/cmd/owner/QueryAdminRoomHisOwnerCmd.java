package com.java110.user.cmd.owner;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.owner.OwnerDto;
import com.java110.intf.user.IOwnerInnerServiceSMO;
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
 * 查询管理房间历史业主命令类
 * 
 * 该命令类用于处理查询指定房间历史业主信息的请求，包括参数验证和业务逻辑处理
 * 通过服务码"owner.queryAdminRoomHisOwner"标识，仅管理员可访问
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "owner.queryAdminRoomHisOwner")
public class QueryAdminRoomHisOwnerCmd extends Cmd {

    /**
     * 业主内部服务接口，用于查询业主相关信息
     */
    @Autowired
    private IOwnerInnerServiceSMO ownerInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数的有效性，包括分页信息验证、管理员权限验证和必要参数检查
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现错误时抛出
     * @throws ParseException 当参数解析失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证分页参数（页码、每页记录数）
        super.validatePageInfo(reqJson);
        // 验证当前用户是否具有管理员权限
        super.validateAdmin(context);
        // 验证请求中必须包含房屋编号参数
        Assert.hasKeyAndValue(reqJson, "roomId", "未包含房屋编号");
    }

    /**
     * 命令执行方法
     * 
     * 处理查询房间历史业主信息的业务逻辑，包括数据查询、结果封装和响应设置
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现错误时抛出
     * @throws ParseException 当参数解析失败时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求JSON对象转换为OwnerDto对象，便于后续查询
        OwnerDto ownerDto = BeanConvertUtil.covertBean(reqJson, OwnerDto.class);
        
        // 查询指定房间的历史业主记录总数
        int count = ownerInnerServiceSMOImpl.queryOwnerLogsCountByRoom(ownerDto);

        List<OwnerDto> ownerDtos = null;
        // 根据查询结果数量决定是否查询详细数据
        if (count > 0) {
            // 查询指定房间的历史业主详细信息列表
            ownerDtos = ownerInnerServiceSMOImpl.queryOwnerLogsByRoom(ownerDto);
        } else {
            // 如果没有记录，返回空列表
            ownerDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数 / 每页记录数，向上取整
        // 创建结果对象，包含分页信息和数据列表
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) ownerDto.getRow()), count, ownerDtos);

        // 创建HTTP响应实体，将结果对象转换为JSON字符串，状态码为200
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到上下文中，供框架后续处理
        context.setResponseEntity(responseEntity);
    }
}