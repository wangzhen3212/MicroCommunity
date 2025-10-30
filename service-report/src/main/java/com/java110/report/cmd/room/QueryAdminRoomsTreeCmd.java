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
import com.java110.utils.util.ListUtil;
import com.java110.vo.api.ApiRoomDataVo;
import com.java110.vo.api.ApiRoomVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * 查询管理员房间树形结构命令类
 * 
 * 该类负责处理管理员查询房间树形结构的请求，包括参数验证和业务逻辑处理
 * 通过服务码"room.queryAdminRoomsTree"标识，用于管理系统中的房间信息展示
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "room.queryAdminRoomsTree")
public class QueryAdminRoomsTreeCmd extends Cmd {

    /**
     * 社区报表内部服务接口，用于获取房间相关数据
     */
    @Autowired
    private IReportCommunityInnerServiceSMO reportCommunityInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求中必须包含的字段，并进行分页信息和管理员权限的验证
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求中必须包含unitId字段
        Assert.jsonObjectHaveKey(reqJson, "unitId", "请求中未包含单元信息");
        // 验证分页信息
        super.validatePageInfo(reqJson);
        // 验证管理员权限
        super.validateAdmin(cmdDataFlowContext);
    }

    /**
     * 命令执行方法
     * 
     * 处理查询管理员房间树形结构的业务逻辑，包括数据查询、格式转换和响应构建
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为RoomDto对象
        RoomDto roomDto = BeanConvertUtil.covertBean(reqJson, RoomDto.class);
        // 创建API响应对象
        ApiRoomVo apiRoomVo = new ApiRoomVo();

        // 房间数据列表
        List<RoomDto> roomDtoList = null;

        // 调用服务层方法查询房间树形结构数据
        roomDtoList = reportCommunityInnerServiceSMOImpl.queryRoomsTree(roomDto);
        // 初始化总数为0
        apiRoomVo.setTotal(0);
        // 如果查询结果不为空，设置实际总数
        if (ListUtil.isNotNull(roomDtoList)) {
            apiRoomVo.setTotal(roomDtoList.size());
        }

        // 将RoomDto列表转换为ApiRoomDataVo列表
        apiRoomVo.setRooms(BeanConvertUtil.covertBeanList(roomDtoList, ApiRoomDataVo.class));
        // 获取每页显示行数
        int row = reqJson.getInteger("row");
        // 计算总页数：总记录数/每页行数，向上取整
        apiRoomVo.setRecords((int) Math.ceil((double) apiRoomVo.getTotal() / (double) row));

        // 构建HTTP响应实体，将API响应对象转换为JSON字符串
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiRoomVo), HttpStatus.OK);
        // 设置响应实体到数据流上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}