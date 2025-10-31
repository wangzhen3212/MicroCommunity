package com.java110.dev.cmd.serviceProvide;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.service.ServiceProvideDto;
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
 * 保存服务提供命令类
 * 处理服务提供信息的保存操作
 * 
 * @Java110Cmd 注解定义服务编码为 "serviceProvide.saveServiceProvide"
 */
@Java110Cmd(serviceCode = "serviceProvide.saveServiceProvide")
public class SaveServiceProvideCmd extends Cmd {

    // 注入服务内部服务接口
    @Autowired
    private IServiceInnerServiceSMO serviceInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     *
     * @param event   命令事件
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 从请求JSON中获取数据数组
        JSONArray infos = reqJson.getJSONArray("data");

        // 验证必需字段是否存在
        Assert.hasKeyByFlowData(infos, "Service", "name", "必填，请填写服务名称");
        Assert.hasKeyByFlowData(infos, "Service", "serviceCode", "必填，请填写服务编码");
        Assert.hasKeyByFlowData(infos, "devServiceProvideView", "queryModel", "必填，请选择是否显示菜单");
        Assert.hasKeyByFlowData(infos, "devServiceProvideView", "params", "必填，请填写参数");
    }

    /**
     * 命令执行方法
     * 处理服务提供信息的保存逻辑
     *
     * @param event   命令事件
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 从请求JSON中获取数据数组
        JSONArray infos = reqJson.getJSONArray("data");

        // 从数据数组中获取不同组件的数据
        JSONObject viewServiceInfo = getObj(infos, "Service");
        JSONObject devServiceProvideView = getObj(infos, "devServiceProvideView");
        JSONObject serviceProvideRemarkView = getObj(infos, "serviceProvideRemarkView");

        // 将JSON数据转换为ServiceProvideDto对象
        ServiceProvideDto serviceProvideDto = BeanConvertUtil.covertBean(viewServiceInfo, ServiceProvideDto.class);
        // 合并开发服务提供视图数据
        serviceProvideDto = BeanConvertUtil.covertBean(devServiceProvideView, serviceProvideDto);
        // 合并服务提供备注视图数据
        serviceProvideDto = BeanConvertUtil.covertBean(serviceProvideRemarkView, serviceProvideDto);

        // 调用服务保存数据
        int count = serviceInnerServiceSMOImpl.saveServiceProvide(serviceProvideDto);
        
        // 检查保存结果
        if (count < 1) {
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "保存数据失败");
        }

        // 设置响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(viewServiceInfo.toJSONString(), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 检查JSON对象中是否包含指定键且值不为空
     *
     * @param info JSON对象
     * @param key  要检查的键
     * @return 如果键存在且值不为空返回true，否则返回false
     */
    private boolean hasKey(JSONObject info, String key) {
        if (!info.containsKey(key)
                || StringUtil.isEmpty(info.getString(key))
                || info.getString(key).startsWith("-")) {
            return false;
        }
        return true;
    }

    /**
     * 从JSON数组中根据流程组件名称获取对应的JSON对象
     *
     * @param infos         JSON数组
     * @param flowComponent 流程组件名称
     * @return 对应的JSON对象
     * @throws IllegalArgumentException 如果未找到指定组件的数据
     */
    private JSONObject getObj(JSONArray infos, String flowComponent) {
        JSONObject serviceInfo = null;

        // 遍历JSON数组查找指定组件的数据
        for (int infoIndex = 0; infoIndex < infos.size(); infoIndex++) {
            // 验证每个对象是否包含flowComponent字段
            Assert.hasKeyAndValue(infos.getJSONObject(infoIndex), "flowComponent", "未包含服务流程组件名称");

            // 检查是否匹配目标组件
            if (flowComponent.equals(infos.getJSONObject(infoIndex).getString("flowComponent"))) {
                serviceInfo = infos.getJSONObject(infoIndex);
                Assert.notNull(serviceInfo, "未包含服务信息");
                return serviceInfo;
            }
        }

        // 如果未找到指定组件的数据，抛出异常
        throw new IllegalArgumentException("未找到组件编码为【" + flowComponent + "】数据");
    }
}