package com.java110.dev.cmd.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.app.AppDto;
import com.java110.dto.service.RouteDto;
import com.java110.dto.service.ServiceDto;
import com.java110.intf.community.IAppInnerServiceSMO;
import com.java110.intf.community.IRouteInnerServiceSMO;
import com.java110.intf.community.IServiceInnerServiceSMO;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 服务绑定命令类
 * 用于处理应用、服务和路由之间的绑定关系
 * 
 * @Java110Cmd 注解声明这是一个命令类，serviceCode指定服务编码
 */
@Java110Cmd(serviceCode = "service.bindingService")
public class BindingServiceCmd extends Cmd {

    // 应用服务接口
    @Autowired
    private IAppInnerServiceSMO appInnerServiceSMOImpl;

    // 服务信息接口
    @Autowired
    private IServiceInnerServiceSMO serviceInnerServiceSMOImpl;

    // 路由服务接口
    @Autowired
    private IRouteInnerServiceSMO routeInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     * 
     * @param event 命令事件
     * @param context 数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 从请求中获取数据数组
        JSONArray infos = reqJson.getJSONArray("data");

        // 验证路由视图中的必填字段
        Assert.hasKeyByFlowData(infos, "addRouteView", "orderTypeCd", "必填，请填写订单类型");
        Assert.hasKeyByFlowData(infos, "addRouteView", "invokeLimitTimes", "必填，请填写调用次数");
        Assert.hasKeyByFlowData(infos, "addRouteView", "invokeModel", "可填，请填写消息队列，订单在异步调用时使用");

        // 验证数据数组长度必须为3（应用、服务、路由扩展信息）
        if(infos == null || infos.size() !=3){
            throw new IllegalArgumentException("请求参数错误，为包含 应用，服务或扩展信息");
        }
    }

    /**
     * 命令执行方法
     * 处理服务绑定逻辑，包括应用、服务和路由信息的保存
     * 
     * @param event 命令事件
     * @param context 数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 从请求中获取数据数组
        JSONArray infos = reqJson.getJSONArray("data");

        // 分别获取应用、服务和路由扩展信息
        JSONObject viewAppInfo = getObj(infos, "App");
        JSONObject viewServiceInfo = getObj(infos, "Service");
        JSONObject addRouteView = getObj(infos, "addRouteView");

        // 验证各组件信息不能为空
        Assert.notNull(viewAppInfo, "未包含应用信息");
        Assert.notNull(viewServiceInfo, "未包含服务信息");
        Assert.notNull(addRouteView, "未包含扩展信息");

        // 处理应用信息：如果应用ID不存在，则保存应用信息并获取应用ID
        if(!hasKey(viewAppInfo, "appId")){
            viewAppInfo.put("appId", saveAppInfo(reqJson, viewAppInfo));
        }

        // 处理服务信息：如果服务ID不存在，则保存服务信息并获取服务ID
        if(!hasKey(viewServiceInfo, "serviceId")){
            viewServiceInfo.put("serviceId", saveServiceInfo(reqJson, viewServiceInfo));
        }

        // 处理路由信息：将路由扩展信息转换为路由DTO并设置应用ID和服务ID
        RouteDto routeDto = BeanConvertUtil.covertBean(addRouteView, RouteDto.class);
        routeDto.setAppId(viewAppInfo.getString("appId"));
        routeDto.setServiceId(viewServiceInfo.getString("serviceId"));

        // 保存路由信息
        int count = routeInnerServiceSMOImpl.saveRoute(routeDto);

        // 验证保存结果
        if (count < 1) {
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "保存应用数据失败");
        }

        // 设置响应结果
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(routeDto), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 保存应用信息
     * 
     * @param reqJson 请求报文信息
     * @param appInfo 应用组件信息
     * @return 应用ID
     */
    private String saveAppInfo(JSONObject reqJson, JSONObject appInfo){
        // 将JSON对象转换为应用DTO
        AppDto appDto = BeanConvertUtil.covertBean(appInfo, AppDto.class);

        // 生成应用ID
        appDto.setAppId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_id));

        // 保存应用信息
        int count = appInnerServiceSMOImpl.saveApp(appDto);

        // 验证保存结果
        if (count < 1) {
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "保存应用数据失败");
        }

        return appDto.getAppId();
    }

    /**
     * 保存服务信息
     * 
     * @param reqJson 请求报文信息
     * @param serviceInfo 服务组件信息
     * @return 服务ID
     */
    private String saveServiceInfo(JSONObject reqJson, JSONObject serviceInfo){
        // 将JSON对象转换为服务DTO
        ServiceDto serviceDto = BeanConvertUtil.covertBean(serviceInfo, ServiceDto.class);
        
        // 设置服务默认属性
        serviceDto.setProvideAppId("8000418002"); // 提供应用ID
        serviceDto.setBusinessTypeCd("API");      // 业务类型编码
        serviceDto.setSeq("1");                   // 序列号

        // 生成服务ID
        serviceDto.setServiceId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_service_id));

        // 保存服务信息
        int count = serviceInnerServiceSMOImpl.saveService(serviceDto);

        // 验证保存结果
        if (count < 1) {
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "保存服务数据失败");
        }
        return serviceDto.getServiceId();
    }

    /**
     * 从数据数组中根据流程组件名称获取对应的JSON对象
     * 
     * @param infos 数据数组
     * @param flowComponent 流程组件名称
     * @return 对应的JSON对象
     */
    private JSONObject getObj(JSONArray infos , String flowComponent){
        JSONObject serviceInfo = null;

        // 遍历数据数组查找指定组件
        for(int infoIndex = 0 ; infoIndex < infos.size(); infoIndex ++){
            // 验证每个对象都包含flowComponent字段
            Assert.hasKeyAndValue(infos.getJSONObject(infoIndex), "flowComponent", "未包含服务流程组件名称");

            // 匹配指定组件名称
            if(flowComponent.equals(infos.getJSONObject(infoIndex).getString("flowComponent"))){
                serviceInfo = infos.getJSONObject(infoIndex);
                Assert.notNull(serviceInfo, "未包含服务信息");
                return serviceInfo;
            }
        }

        // 如果未找到指定组件，抛出异常
        throw new IllegalArgumentException("未找到组件编码为【" + flowComponent + "】数据");
    }

    /**
     * 检查JSON对象中是否包含指定键且值不为空
     * 
     * @param info JSON对象
     * @param key 要检查的键
     * @return 如果键存在且值不为空且不以"-"开头，返回true；否则返回false
     */
    private boolean hasKey(JSONObject info, String key){
        if(!info.containsKey(key)
                || StringUtil.isEmpty(info.getString(key))
                || info.getString(key).startsWith("-")){
            return false;
        }
        return true;
    }
}