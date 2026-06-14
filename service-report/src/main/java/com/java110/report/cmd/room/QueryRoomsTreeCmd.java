/**
 * 房屋查询命令类 - 查询房屋树形结构数据
 * 
 * 该类负责处理房屋树形结构查询请求，通过注解@Java110Cmd(serviceCode = "room.queryRoomsTree")标识服务代码，
 * 继承自Cmd基类，实现房屋数据的树形结构查询和返回功能。
 * 主要功能包括请求参数验证、房屋数据查询、分页处理和响应结果封装。
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
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

@Java110Cmd(serviceCode = "room.queryRoomsTree")
public class QueryRoomsTreeCmd extends Cmd {

    /**
     * 报表社区内部服务接口，用于查询房屋树形结构数据
     */
    @Autowired
    private IReportCommunityInnerServiceSMO reportCommunityInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求报文中的必需参数是否存在且格式正确，包括：
     * - communityId：小区ID，必须存在且不为空
     * - unitId：单元ID，必须存在
     * - page：页码，必须存在且为数字
     * - row：每页行数，必须存在且为数字
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于处理数据流
     * @param reqJson 请求JSON对象，包含前端传递的参数
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证communityId参数是否存在
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求中未包含communityId信息");
        // 验证unitId参数是否存在
        Assert.jsonObjectHaveKey(reqJson, "unitId", "请求中未包含单元信息");
        // 验证page参数是否存在
        Assert.jsonObjectHaveKey(reqJson, "page", "请求报文中未包含page节点");
        // 验证row参数是否存在
        Assert.jsonObjectHaveKey(reqJson, "row", "请求报文中未包含row节点");
        // 验证page参数是否为数字
        Assert.isInteger(reqJson.getString("page"), "page不是数字");
        // 验证row参数是否为数字
        Assert.isInteger(reqJson.getString("row"), "row不是数字");
        // 验证communityId参数是否为空字符串
        Assert.hasLength(reqJson.getString("communityId"), "小区ID不能为空");
        // 调用父类的属性验证方法
        super.validateProperty(cmdDataFlowContext);
    }

    /**
     * 执行房屋树形结构查询命令
     * 
     * 主要业务流程：
     * 1. 将请求参数转换为RoomDto对象
     * 2. 调用服务层查询房屋树形结构数据
     * 3. 设置返回数据的统计信息
     * 4. 转换数据格式并计算分页信息
     * 5. 封装响应结果并设置到上下文
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为RoomDto对象，便于后续查询
        RoomDto roomDto = BeanConvertUtil.covertBean(reqJson, RoomDto.class);
        // 创建返回给前端的房屋数据VO对象
        ApiRoomVo apiRoomVo = new ApiRoomVo();

        // 房屋数据列表，初始化为null
        List<RoomDto> roomDtoList = null;

        // 调用服务层接口，查询房屋树形结构数据
        roomDtoList = reportCommunityInnerServiceSMOImpl.queryRoomsTree(roomDto);
        // 初始化总记录数为0
        apiRoomVo.setTotal(0);
        // 判断查询结果列表是否不为空
        if (ListUtil.isNotNull(roomDtoList)) {
            // 设置总记录数为查询结果列表的大小
            apiRoomVo.setTotal(roomDtoList.size());
        }

        // 将RoomDto列表转换为ApiRoomDataVo列表，便于前端展示
        apiRoomVo.setRooms(BeanConvertUtil.covertBeanList(roomDtoList, ApiRoomDataVo.class));
        // 从请求参数中获取每页显示的行数
        int row = reqJson.getInteger("row");
        // 计算总页数：总记录数/每页行数，向上取整
        apiRoomVo.setRecords((int) Math.ceil((double) apiRoomVo.getTotal() / (double) row));

        // 创建HTTP响应实体，将ApiRoomVo对象转换为JSON字符串，状态码为200
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiRoomVo), HttpStatus.OK);
        // 将响应实体设置到数据流上下文中，返回给前端
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}