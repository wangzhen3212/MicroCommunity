/**
 * 设备数量查询命令类
 * 
 * 该命令类用于查询系统中各类设备相关的统计数量，包括应用数、接口数、服务注册数、
 * 定时任务数、配置数等，主要用于系统管理员查看系统运行状态和资源使用情况。
 * 
 * 服务代码：admin.queryDevCount
 * 需要开发者权限才能执行该命令
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
import com.java110.dto.community.CommunityDto;
import com.java110.dto.complaint.ComplaintDto;
import com.java110.dto.fee.FeeDetailDto;
import com.java110.dto.inspection.InspectionTaskDto;
import com.java110.dto.mapping.MappingDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.repair.RepairDto;
import com.java110.dto.room.RoomDto;
import com.java110.dto.service.RouteDto;
import com.java110.dto.service.ServiceDto;
import com.java110.dto.shop.ShopDto;
import com.java110.dto.store.StoreDto;
import com.java110.dto.task.TaskDto;
import com.java110.intf.community.*;
import com.java110.intf.dev.ITaskV1InnerServiceSMO;
import com.java110.intf.fee.IFeeDetailInnerServiceSMO;
import com.java110.intf.mall.IShopInnerServiceSMO;
import com.java110.intf.store.IComplaintInnerServiceSMO;
import com.java110.intf.store.IStoreInnerServiceSMO;
import com.java110.intf.user.IOwnerCarV1InnerServiceSMO;
import com.java110.intf.user.IOwnerV1InnerServiceSMO;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.DateUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Java110Cmd(serviceCode = "admin.queryDevCount")
public class QueryDevCountCmd extends Cmd {

    /**
     * 店铺信息服务接口，用于验证用户权限
     */
    @Autowired
    private IStoreInnerServiceSMO storeInnerServiceSMOImpl;

    /**
     * 应用信息服务接口，用于查询应用数量
     */
    @Autowired
    private IAppInnerServiceSMO appInnerServiceSMOImpl;

    /**
     * 路由信息服务接口，用于查询服务注册数量
     */
    @Autowired
    private IRouteInnerServiceSMO routeInnerServiceSMOImpl;

    /**
     * 服务信息服务接口，用于查询接口数量
     */
    @Autowired
    private IServiceInnerServiceSMO serviceInnerServiceSMOImpl;

    /**
     * 定时任务信息服务接口，用于查询任务数量
     */
    @Autowired
    private ITaskV1InnerServiceSMO taskV1InnerServiceSMOImpl;

    /**
     * 配置信息服务接口，用于查询配置数量
     */
    @Autowired
    private IMappingInnerServiceSMO mappingInnerServiceSMOImpl;

    /**
     * 命令验证方法
     * 
     * 验证当前用户是否具有开发者权限，只有开发者账户才能执行该命令
     * 
     * @param event 命令事件对象，包含命令执行的相关信息
     * @param context 命令数据流上下文，包含请求和响应的数据流信息
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当用户权限验证失败时抛出命令异常
     * @throws ParseException 当数据解析异常时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从上下文中获取当前店铺ID
        String storeId = CmdContextUtils.getStoreId(context);

        // 构建店铺查询条件
        StoreDto storeDto = new StoreDto();
        storeDto.setStoreId(storeId);
        // 设置店铺类型为开发者类型
        storeDto.setStoreTypeCd(StoreDto.STORE_TYPE_DEV);
        
        // 查询符合条件的店铺数量
        int count = storeInnerServiceSMOImpl.getStoreCount(storeDto);
        
        // 如果店铺数量小于1，说明当前用户不是开发者，抛出权限异常
        if (count < 1) {
            throw new CmdException("非法操作，请用系统开发者账户操作");
        }
    }

    /**
     * 命令执行方法
     * 
     * 查询系统中各类设备相关的统计数量，包括：
     * - 应用数
     * - 接口数
     * - 服务注册数
     * - 定时任务总数
     * - 启用定时任务数
     * - 配置数
     * 
     * 将查询结果封装成列表返回给前端
     * 
     * @param event 命令事件对象，包含命令执行的相关信息
     * @param context 命令数据流上下文，包含请求和响应的数据流信息
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当命令执行过程中发生错误时抛出命令异常
     * @throws ParseException 当数据解析异常时抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 初始化数据列表，用于存储所有统计结果
        List<Map> datas = new ArrayList<>();
        
        // 查询应用数量
        AppDto appDto = new AppDto();
        int appCount = appInnerServiceSMOImpl.queryAppsCount(appDto);
        setDatas(datas, "应用数", appCount);

        // 查询接口数量
        ServiceDto serviceDto = new ServiceDto();
        int storeCount = serviceInnerServiceSMOImpl.queryServicesCount(serviceDto);
        setDatas(datas, "接口数", storeCount);

        // 查询服务注册数量
        RouteDto routeDto = new RouteDto();
        int routeCount =  routeInnerServiceSMOImpl.queryRoutesCount(routeDto);
        setDatas(datas, "服务注册数", routeCount);

        // 查询定时任务总数
        TaskDto taskDto = new TaskDto();
        int taskCount = taskV1InnerServiceSMOImpl.queryTasksCount(taskDto);
        setDatas(datas, "任务总数", taskCount);

        // 查询启用状态的定时任务数量（状态码002表示启用）
        taskDto = new TaskDto();
        taskDto.setState("002");
        taskCount = taskV1InnerServiceSMOImpl.queryTasksCount(taskDto);
        setDatas(datas, "启用任务数", taskCount);

        // 查询配置数量
        MappingDto mappingDto = new MappingDto();
        int mappingCount = mappingInnerServiceSMOImpl.queryMappingsCount(mappingDto);
        setDatas(datas, "配置数", mappingCount);

        // 将统计结果设置到响应中
        context.setResponseEntity(ResultVo.createResponseEntity(datas));

    }

    /**
     * 设置统计数据到结果列表
     * 
     * 将统计项的名称和值封装成Map对象，并添加到数据列表中
     * 
     * @param datas 数据列表，用于存储所有统计结果
     * @param name 统计项的名称，如"应用数"、"接口数"等
     * @param value 统计项的具体数值
     */
    private void setDatas(List<Map> datas, String name, int value) {
        // 创建统计项的信息Map
        Map info = new HashMap();
        // 设置统计项名称
        info.put("name", name);
        // 设置统计项数值
        info.put("value", value);
        // 将统计项添加到数据列表
        datas.add(info);
    }
}