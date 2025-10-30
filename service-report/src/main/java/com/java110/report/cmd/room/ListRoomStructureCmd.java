package com.java110.report.cmd.room;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.room.RoomDto;
import com.java110.intf.report.IReportCommunityInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 房屋结构图查询命令类
 * 
 * 该类负责处理房屋结构图查询请求，通过服务码"room.listRoomStructure"标识，
 * 主要功能包括参数验证和查询房屋结构信息
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "room.listRoomStructure")
public class ListRoomStructureCmd extends Cmd {

    /**
     * 报表社区内部服务接口，用于查询房屋结构信息
     */
    @Autowired
    private IReportCommunityInnerServiceSMO reportCommunityInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求JSON中是否包含必要的参数，确保后续业务逻辑能够正常执行
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于处理请求和响应
     * @param reqJson 请求参数JSON对象，包含前端传递的参数
     * @throws IllegalArgumentException 当必要参数缺失时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含unitId（单元ID）
        Assert.hasKeyAndValue(reqJson, "unitId", "未传入单元信息");
        // 验证请求参数中必须包含communityId（社区ID）
        Assert.hasKeyAndValue(reqJson, "communityId", "未传入房屋信息");
    }

    /**
     * 执行房屋结构图查询命令
     * 
     * 将请求参数转换为RoomDto对象，调用服务层查询房屋结构信息，
     * 并将查询结果设置到响应中返回给前端
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为RoomDto对象，便于后续业务处理
        RoomDto roomDto = BeanConvertUtil.covertBean(reqJson, RoomDto.class);
        
        // 调用服务层接口，查询房屋结构信息
        List<RoomDto> roomDtos = reportCommunityInnerServiceSMOImpl.queryRoomStructures(roomDto);

        // 将查询结果封装为统一响应格式，并设置到数据流上下文中
        cmdDataFlowContext.setResponseEntity(ResultVo.createResponseEntity(roomDtos));
    }
}