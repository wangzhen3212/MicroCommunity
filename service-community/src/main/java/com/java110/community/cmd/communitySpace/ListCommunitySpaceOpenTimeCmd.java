/**
 * 场地可预约时间查询命令类
 * 
 * 该命令类用于处理查询场地可预约时间的业务逻辑，支持根据场地ID、小区ID和预约日期
 * 查询系统中可用的预约时间段信息。主要用于社区场地预约系统的前端接口调用。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022
 */
package com.java110.community.cmd.communitySpace;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.log.LoggerFactory;
import com.java110.doc.annotation.*;
import com.java110.dto.community.CommunitySpaceOpenTimeDto;
import com.java110.intf.community.ICommunitySpaceOpenTimeV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.List;

@Java110CmdDoc(title = "查询可预约时间场地",
        description = "查询系统中的查询可预约时间场地",
        httpMethod = "get",
        url = "http://{ip}:{port}/app/communitySpace.listCommunitySpaceOpenTime",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "communitySpace.listCommunitySpaceOpenTime"
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "spaceId", length = 30, remark = "场地ID"),
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "appointmentTime", length = 30, remark = "预约日期 YYYY-MM-DD"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "data", type = "Array", remark = "有效数据"),
                @Java110ParamDoc(parentNodeName = "data", name = "hours", type = "String", remark = "小时"),
        }
)

@Java110ExampleDoc(
        reqBody = "http://{ip}:{port}/app/communitySpace.listCommunitySpaceOpenTime?spaceId=123&appointmentTime=2022-01-01&communityId=2022081539020475",
        resBody = "{\"code\":0,\"data\":[{\"hours\":1,\"hours\":2}],\"msg\":\"成功\",\"page\":0,\"records\":1,\"rows\":0,\"total\":2}"
)
@Java110Cmd(serviceCode = "communitySpace.listCommunitySpaceOpenTime")
public class ListCommunitySpaceOpenTimeCmd extends Cmd{

    // 日志记录器
    private static Logger logger = LoggerFactory.getLogger(ListCommunitySpacePersonCmd.class);

    // 场地可预约时间服务接口
    @Autowired
    private ICommunitySpaceOpenTimeV1InnerServiceSMO communitySpaceOpenTimeV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整，确保必要的参数都存在且不为空
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证预约时间参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson,"appointmentTime","预约时间");
        // 验证小区ID参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson,"communityId","小区ID");
    }

    /**
     * 命令执行方法
     * 
     * 根据请求参数查询场地可预约时间信息，并返回查询结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 命令执行异常
     * @throws ParseException 日期解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 创建场地可预约时间数据传输对象
        CommunitySpaceOpenTimeDto communitySpaceOpenTimeDto = new CommunitySpaceOpenTimeDto();
        
        // 设置查询条件：场地ID
        communitySpaceOpenTimeDto.setSpaceId(reqJson.getString("spaceId"));
        // 设置查询条件：场馆ID
        communitySpaceOpenTimeDto.setVenueId(reqJson.getString("venueId"));
        // 设置查询条件：预约时间
        communitySpaceOpenTimeDto.setAppointmentTime(reqJson.getString("appointmentTime"));
        
        // 调用服务层接口查询场地可预约时间列表
        List<CommunitySpaceOpenTimeDto> communitySpaceOpenTimeDtos = communitySpaceOpenTimeV1InnerServiceSMOImpl.queryCommunitySpaceOpenTimes(communitySpaceOpenTimeDto);

        // 构建返回结果对象，包含查询到的数据列表
        ResultVo resultVo = new ResultVo(1, communitySpaceOpenTimeDtos.size(), communitySpaceOpenTimeDtos);

        // 创建HTTP响应实体，设置状态码为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到上下文中，供框架后续处理
        context.setResponseEntity(responseEntity);
    }
}