/**
 * 管理员查询应用服务数据命令类
 * 
 * 该类用于处理管理员查询应用服务数据的请求，主要功能包括：
 * 1. 验证请求的合法性，确保只有系统管理员可以操作
 * 2. 查询应用服务数据并返回结果
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.report.cmd.admin;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.app.AppDto;
import com.java110.dto.store.StoreDto;
import com.java110.intf.community.IAppInnerServiceSMO;
import com.java110.intf.store.IStoreInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

@Java110Cmd(serviceCode = "admin.queryAppServiceData")
public class QueryAppServiceDataCmd extends Cmd {

    /**
     * 店铺内部服务接口，用于店铺相关操作
     */
    @Autowired
    private IStoreInnerServiceSMO storeInnerServiceSMOImpl;

    /**
     * 应用内部服务接口，用于应用相关操作
     */
    @Autowired
    private IAppInnerServiceSMO appInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 该方法用于验证请求的合法性，主要检查：
     * 1. 当前操作者是否为系统管理员
     * 2. 防止非法操作
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求上下文信息
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当验证失败时抛出命令异常
     * @throws ParseException 当解析异常时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 从上下文中获取店铺ID
        String storeId = CmdContextUtils.getStoreId(context);

        // 创建店铺数据传输对象并设置参数
        StoreDto storeDto = new StoreDto();
        storeDto.setStoreId(storeId);
        // 设置店铺类型为开发者类型
        storeDto.setStoreTypeCd(StoreDto.STORE_TYPE_DEV);
        
        // 查询符合条件的店铺数量
        int count = storeInnerServiceSMOImpl.getStoreCount(storeDto);
        
        // 如果店铺数量小于1，说明不是系统管理员，抛出异常
        if (count < 1) {
            throw new CmdException("非法操作，请用系统管理员账户操作");
        }

    }

    /**
     * 执行命令方法
     * 
     * 该方法用于执行查询应用服务数据的主要业务逻辑：
     * 1. 查询所有应用服务数据
     * 2. 将查询结果封装并设置到响应中
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于设置响应信息
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行失败时抛出命令异常
     * @throws ParseException 当解析异常时抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 创建应用数据传输对象
        AppDto appDto = new AppDto();
        // 查询应用服务数据列表
        List<AppDto> appDtos = appInnerServiceSMOImpl.queryAppsServiceData(appDto);

        // 将查询结果封装成响应实体并设置到上下文中
        context.setResponseEntity(ResultVo.createResponseEntity(appDtos));
    }
}