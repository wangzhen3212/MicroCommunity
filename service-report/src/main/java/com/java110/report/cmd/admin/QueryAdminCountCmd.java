/**
 * 管理员数据统计命令类
 * 
 * 功能：用于查询系统管理后台的各项统计数据，包括小区、物业、商家、商铺、房屋、业主、车辆等数量，
 * 以及今日的缴费、报修、巡检、投诉等业务数据统计。
 * 
 * 该命令通过@Java110Cmd注解标识服务代码为"admin.queryAdminCount"，
 * 只有系统管理员账户才能执行此命令获取统计数据。
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
import com.java110.dto.App;
import com.java110.dto.app.AppDto;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.complaint.ComplaintDto;
import com.java110.dto.fee.FeeDetailDto;
import com.java110.dto.inspection.InspectionTaskDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.repair.RepairDto;
import com.java110.dto.room.RoomDto;
import com.java110.dto.shop.ShopDto;
import com.java110.dto.store.StoreDto;
import com.java110.intf.community.*;
import com.java110.intf.fee.IFeeDetailInnerServiceSMO;
import com.java110.intf.fee.IPayFeeDetailV1InnerServiceSMO;
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

@Java110Cmd(serviceCode = "admin.queryAdminCount")
public class QueryAdminCountCmd extends Cmd {

    /** 商户服务接口 */
    @Autowired
    private IStoreInnerServiceSMO storeInnerServiceSMOImpl;

    /** 小区服务接口 */
    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    /** 商铺服务接口（非必需） */
    @Autowired(required = false)
    private IShopInnerServiceSMO shopInnerServiceSMOImpl;

    /** 费用明细服务接口 */
    @Autowired
    private IFeeDetailInnerServiceSMO feeDetailInnerServiceSMOImpl;

    /** 报修服务接口 */
    @Autowired
    private IRepairInnerServiceSMO repairInnerServiceSMOImpl;

    /** 巡检任务服务接口 */
    @Autowired
    private IInspectionTaskInnerServiceSMO inspectionTaskInnerServiceSMOImpl;

    /** 投诉服务接口 */
    @Autowired
    private IComplaintInnerServiceSMO complaintInnerServiceSMOImpl;

    /** 房屋服务接口 */
    @Autowired
    private IRoomV1InnerServiceSMO roomV1InnerServiceSMOImpl;

    /** 业主服务接口 */
    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;

    /** 业主车辆服务接口 */
    @Autowired
    private IOwnerCarV1InnerServiceSMO ownerCarV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 功能：验证当前操作用户是否为系统管理员，只有管理员才能执行统计查询
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求上下文信息
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当验证失败时抛出命令异常
     * @throws ParseException 当参数解析异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从上下文中获取当前商户ID
        String storeId = CmdContextUtils.getStoreId(context);

        // 构建商户查询条件，验证是否为管理员账户
        StoreDto storeDto = new StoreDto();
        storeDto.setStoreId(storeId);
        storeDto.setStoreTypeCd(StoreDto.STORE_TYPE_ADMIN);
        
        // 查询管理员账户数量
        int count = storeInnerServiceSMOImpl.getStoreCount(storeDto);
        
        // 如果不是管理员账户，抛出异常
        if (count < 1) {
            throw new CmdException("非法操作，请用系统管理员账户操作");
        }
    }

    /**
     * 执行命令方法
     * 
     * 功能：查询系统各项统计数据，包括基础数据统计和今日业务数据统计
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行异常时抛出
     * @throws ParseException 当数据解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 初始化数据列表，用于存储所有统计结果
        List<Map> datas = new ArrayList<>();
        
        // 统计小区个数
        CommunityDto communityDto = new CommunityDto();
        int communityCount = communityInnerServiceSMOImpl.queryCommunitysCount(communityDto);
        setDatas(datas, "小区数", communityCount);

        // 统计物业公司个数
        StoreDto storeDto = new StoreDto();
        storeDto.setStoreTypeCd(StoreDto.STORE_TYPE_PROPERTY);
        int storeCount = storeInnerServiceSMOImpl.getStoreCount(storeDto);
        setDatas(datas, "物业数", storeCount);

        // 统计商家个数
        storeDto = new StoreDto();
        storeDto.setStoreTypeCd(StoreDto.STORE_TYPE_MALL);
        storeCount = storeInnerServiceSMOImpl.getStoreCount(storeDto);
        setDatas(datas, "商家数", storeCount);

        // 统计商铺个数（需要检查是否启用商城功能）
        int shopCount = 0;
        if ("ON".equals(MappingCache.getValue("HAS_HC_MALL"))) {
            ShopDto shopDto = new ShopDto();
            shopCount = shopInnerServiceSMOImpl.queryShopsCount(shopDto);
        }
        setDatas(datas, "店铺数", shopCount);

        // 统计房屋数（住宅类型）
        RoomDto roomDto = new RoomDto();
        roomDto.setRoomType(RoomDto.ROOM_TYPE_ROOM);
        int roomCount = roomV1InnerServiceSMOImpl.queryRoomsCount(roomDto);
        setDatas(datas, "房屋数", roomCount);

        // 统计商铺数（商铺类型）
        roomDto = new RoomDto();
        roomDto.setRoomType(RoomDto.ROOM_TYPE_SHOPS);
        roomCount = roomV1InnerServiceSMOImpl.queryRoomsCount(roomDto);
        setDatas(datas, "商铺数", roomCount);

        // 统计业主数
        OwnerDto ownerDto = new OwnerDto();
        int ownerCount = ownerV1InnerServiceSMOImpl.queryOwnersCount(ownerDto);
        setDatas(datas, "业主数", ownerCount);

        // 统计车辆数（包含各种租赁类型的车辆）
        OwnerCarDto ownerCarDto = new OwnerCarDto();
        ownerCarDto.setLeaseTypes(new String[]{OwnerCarDto.LEASE_TYPE_INNER,
                OwnerCarDto.LEASE_TYPE_MONTH,
                OwnerCarDto.LEASE_TYPE_SALE,
                OwnerCarDto.LEASE_TYPE_NO_MONEY,
                OwnerCarDto.LEASE_TYPE_RESERVE
        });
        int carCount = ownerCarV1InnerServiceSMOImpl.queryOwnerCarsCount(ownerCarDto);
        setDatas(datas, "车辆数", carCount);

        // 设置今日时间范围：从当天00:00:00到23:59:59
        String startTime = DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_B);
        String endTime = startTime + " 23:59:59";
        
        // 统计今日缴费订单数
        FeeDetailDto feeDetailDto = new FeeDetailDto();
        feeDetailDto.setState(FeeDetailDto.STATE_NORMAL); // 只统计正常状态的缴费记录
        feeDetailDto.setStartTime(DateUtil.getDateFromStringB(startTime));
        feeDetailDto.setEndTime(DateUtil.getDateFromStringA(endTime));
        int feeCount = feeDetailInnerServiceSMOImpl.queryFeeDetailsCount(feeDetailDto);
        setDatas(datas, "今日缴费数", feeCount);

        // 统计今日报修订单数
        RepairDto repairDto = new RepairDto();
        repairDto.setStartTime(startTime);
        repairDto.setEndTime(endTime);
        int repairCount = repairInnerServiceSMOImpl.queryRepairsCount(repairDto);
        setDatas(datas, "今日报修数", repairCount);

        // 统计今日巡检订单数（包含进行中和已完成的任务）
        InspectionTaskDto inspectionTaskDto = new InspectionTaskDto();
        inspectionTaskDto.setStartTime(startTime);
        inspectionTaskDto.setEndTime(endTime);
        inspectionTaskDto.setStates(new String[]{InspectionTaskDto.STATE_DOING, InspectionTaskDto.STATE_FINISH});
        int inspectionCount = inspectionTaskInnerServiceSMOImpl.queryInspectionTasksCount(inspectionTaskDto);
        setDatas(datas, "今日巡检数", inspectionCount);

        // 统计今日投诉单数
        ComplaintDto complaintDto = new ComplaintDto();
        complaintDto.setStartTime(startTime);
        complaintDto.setEndTime(endTime);
        int complaintCount = complaintInnerServiceSMOImpl.queryComplaintsCount(complaintDto);
        setDatas(datas, "今日投诉数", complaintCount);

        // 将统计结果设置到响应中
        context.setResponseEntity(ResultVo.createResponseEntity(datas));
    }

    /**
     * 设置统计数据到结果列表
     * 
     * 功能：将统计项的名称和值封装成Map对象，并添加到数据列表中
     * 
     * @param datas 数据列表，用于存储所有统计结果
     * @param name 统计项名称
     * @param value 统计项数值
     */
    private void setDatas(List<Map> datas, String name, int value) {
        Map info = new HashMap();
        info.put("name", name);   // 统计项名称
        info.put("value", value); // 统计项数值
        datas.add(info);          // 添加到结果列表
    }
}