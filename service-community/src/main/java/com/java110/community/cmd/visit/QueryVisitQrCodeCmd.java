/**
 * 访客二维码查询命令类
 * 
 * 该类负责处理访客二维码查询请求，通过物联网服务获取访客二维码URL
 * 主要用于社区访客管理系统的二维码生成和查询功能
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.community.cmd.visit;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.IotDataDto;
import com.java110.intf.job.IIotInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;

/**
 * 访客二维码查询命令实现类
 * 服务编码：visit.queryVisitQrCode
 */
@Java110Cmd(serviceCode = "visit.queryVisitQrCode")
public class QueryVisitQrCodeCmd extends Cmd {

    /**
     * 物联网内部服务接口，用于调用物联网相关功能
     */
    @Autowired
    private IIotInnerServiceSMO iotInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数中是否包含必要的小区ID信息
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     * @throws ParseException 当参数解析失败时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求参数中必须包含小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID");
    }

    /**
     * 命令执行方法
     * 
     * 调用物联网服务获取访客二维码URL，并将结果设置到响应中
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行失败时抛出命令异常
     * @throws ParseException 当参数解析失败时抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 创建物联网数据传输对象，指定业务模块和请求参数
        IotDataDto iotDataDto = new IotDataDto("getVisitQrCodeUrlBmoImpl", reqJson);
        
        // 调用物联网服务获取访客二维码URL结果
        ResultVo resultVo = iotInnerServiceSMOImpl.postIotData(iotDataDto);
        
        // 将处理结果设置到响应实体中返回给前端
        context.setResponseEntity(ResultVo.createResponseEntity(resultVo));
    }
}