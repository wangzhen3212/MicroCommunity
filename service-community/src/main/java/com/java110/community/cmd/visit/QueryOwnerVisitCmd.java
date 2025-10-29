/**
 * 访客查询命令类
 * 
 * 该类用于处理访客查询相关的业务逻辑，主要功能包括：
 * 1. 验证查询请求参数
 * 2. 根据用户ID查询关联的房屋信息
 * 3. 调用物联网服务获取访客记录
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.community.cmd.visit;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.IotDataDto;
import com.java110.dto.owner.OwnerAppUserDto;
import com.java110.intf.job.IIotInnerServiceSMO;
import com.java110.intf.user.IOwnerAppUserV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

@Java110Cmd(serviceCode = "visit.queryOwnerVisit")
public class QueryOwnerVisitCmd extends Cmd {

    /**
     * 物联网服务接口，用于调用物联网相关功能
     */
    @Autowired
    private IIotInnerServiceSMO iotInnerServiceSMOImpl;

    /**
     * 业主应用用户服务接口，用于查询业主房屋关联信息
     */
    @Autowired
    private IOwnerAppUserV1InnerServiceSMO ownerAppUserV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数中是否包含必要的小区ID信息
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置上下文信息
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     * @throws ParseException 当参数解析失败时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求参数中必须包含小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
    }

    /**
     * 执行访客查询命令
     * 
     * 主要执行流程：
     * 1. 获取当前用户ID
     * 2. 查询用户关联的房屋信息
     * 3. 验证用户是否已认证房屋
     * 4. 调用物联网服务获取访客记录
     * 5. 设置响应结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当业务逻辑执行失败时抛出命令异常
     * @throws ParseException 当数据解析失败时抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从上下文中获取当前用户ID
        String userId = CmdContextUtils.getUserId(context);

        // 构建业主应用用户查询条件
        OwnerAppUserDto ownerAppUserDto = new OwnerAppUserDto();
        ownerAppUserDto.setUserId(userId); // 设置用户ID
        ownerAppUserDto.setCommunityId(reqJson.getString("communityId")); // 设置小区ID
        
        // 查询用户关联的房屋信息
        List<OwnerAppUserDto> ownerAppUserDtos = ownerAppUserV1InnerServiceSMOImpl.queryOwnerAppUsers(ownerAppUserDto);
        
        // 验证用户是否已认证房屋，如果未认证则抛出异常
        if (ListUtil.isNull(ownerAppUserDtos)) {
            throw new CmdException("未认证房屋");
        }

        // 将业主ID添加到请求参数中，用于后续查询
        reqJson.put("ownerId", ownerAppUserDtos.get(0).getMemberId());

        // 构建物联网数据请求对象并调用物联网服务获取访客记录
        IotDataDto iotDataDto = new IotDataDto("listVisitBmoImpl", reqJson);
        ResultVo resultVo = iotInnerServiceSMOImpl.postIotData(iotDataDto);

        // 设置响应结果到上下文
        context.setResponseEntity(ResultVo.createResponseEntity(resultVo));
    }
}