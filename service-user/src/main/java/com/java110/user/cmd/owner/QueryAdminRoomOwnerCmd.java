package com.java110.user.cmd.owner;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.owner.OwnerDto;
import com.java110.intf.user.IOwnerInnerServiceSMO;
import com.java110.user.bmo.owner.IQueryOwnerStatisticsBMO;
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
 * 管理员查询房屋业主信息命令类
 * 该命令用于管理员查询指定房屋的业主信息，包括业主基本信息和统计信息
 * 服务编码：owner.queryAdminRoomOwner
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "owner.queryAdminRoomOwner")
public class QueryAdminRoomOwnerCmd extends Cmd {

    /**
     * 业主内部服务接口，用于查询业主基本信息
     */
    @Autowired
    private IOwnerInnerServiceSMO ownerInnerServiceSMOImpl;

    /**
     * 业主统计信息查询业务接口，用于查询业主的统计信息
     */
    @Autowired
    private IQueryOwnerStatisticsBMO queryOwnerStatisticsBMOImpl;

    /**
     * 参数验证方法
     * 验证管理员权限、分页信息和必要参数
     *
     * @param event   命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置上下文信息
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException   当命令执行异常时抛出
     * @throws ParseException 当参数解析异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证管理员权限
        super.validateAdmin(context);
        // 验证分页信息
        super.validatePageInfo(reqJson);
        // 验证必须包含房屋编号参数
        Assert.hasKeyAndValue(reqJson, "roomId", "未包含房屋编号");
    }

    /**
     * 命令执行方法
     * 根据房屋信息查询业主列表，并获取业主的统计信息
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException   当命令执行异常时抛出
     * @throws ParseException 当参数解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从上下文中获取当前用户ID
        String userId = CmdContextUtils.getUserId(context);

        // TODO: 根据房屋查询时，先用房屋信息查询业主ID
        // 当前实现可能需要优化，先通过房屋查询业主ID，再查询业主详细信息

        // 将请求JSON转换为OwnerDto对象
        OwnerDto ownerDto = BeanConvertUtil.covertBean(reqJson, OwnerDto.class);

        // 获取每页显示行数
        int row = reqJson.getInteger("row");
        
        // 查询符合条件的业主总记录数
        int total = ownerInnerServiceSMOImpl.queryOwnersCount(ownerDto);
        
        // 业主信息列表
        List<OwnerDto> ownerDtos = null;
        
        // 如果有符合条件的记录，则查询详细信息
        if (total > 0) {
            // 查询业主基本信息列表
            ownerDtos = ownerInnerServiceSMOImpl.queryOwners(ownerDto);
            // 查询业主统计信息并合并到业主基本信息中
            ownerDtos = queryOwnerStatisticsBMOImpl.query(ownerDtos);
        } else {
            // 如果没有记录，返回空列表
            ownerDtos = new ArrayList<>();
        }
        
        // 计算总页数：总记录数 / 每页行数，向上取整
        // 创建响应实体，包含分页信息和业主数据
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(
            (int) Math.ceil((double) total / (double) row), // 总页数
            total,                                          // 总记录数
            ownerDtos                                       // 业主数据列表
        );
        
        // 设置响应实体到上下文
        context.setResponseEntity(responseEntity);
    }
}