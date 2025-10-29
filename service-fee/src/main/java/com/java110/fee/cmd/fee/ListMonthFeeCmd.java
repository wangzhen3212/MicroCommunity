package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.CommunitySettingFactory;
import com.java110.core.log.LoggerFactory;
import com.java110.core.smo.IComputeFeeSMO;
import com.java110.dto.fee.FeeAttrDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.floor.FloorDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.owner.OwnerRoomRelDto;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.dto.payFee.PayFeeDetailMonthDto;
import com.java110.dto.room.RoomDto;
import com.java110.dto.unit.UnitDto;
import com.java110.intf.community.IFloorInnerServiceSMO;
import com.java110.intf.community.IParkingSpaceInnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.community.IUnitInnerServiceSMO;
import com.java110.intf.fee.IFeeAttrInnerServiceSMO;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.intf.fee.IPayFeeDetailMonthInnerServiceSMO;
import com.java110.intf.user.IOwnerCarInnerServiceSMO;
import com.java110.intf.user.IOwnerInnerServiceSMO;
import com.java110.intf.user.IOwnerRoomRelV1InnerServiceSMO;
import com.java110.po.fee.FeeAttrPo;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.*;
import com.java110.vo.ResultVo;
import com.java110.vo.api.fee.ApiFeeDataVo;
import com.java110.vo.api.fee.ApiFeeVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 查询月费用信息命令类
 * 该命令用于处理费用月明细的查询请求，支持按房屋编号、费用对象ID等多种条件查询
 * 主要功能包括：费用数据查询、房屋信息关联、费用属性关联、金额精度处理等
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "fee.listMonthFee")
public class ListMonthFeeCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListMonthFeeCmd.class);

    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;

    @Autowired
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl;

    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    @Autowired
    private IFeeAttrInnerServiceSMO feeAttrInnerServiceSMOImpl;

    @Autowired
    private IComputeFeeSMO computeFeeSMOImpl;

    @Autowired
    private IFloorInnerServiceSMO floorInnerServiceSMOImpl;

    @Autowired
    private IUnitInnerServiceSMO unitInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IOwnerRoomRelV1InnerServiceSMO ownerRoomRelV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerInnerServiceSMO ownerInnerServiceSMOImpl;

    @Autowired
    private IPayFeeDetailMonthInnerServiceSMO payFeeDetailMonthInnerServiceSMOImpl;

    // 域配置常量
    /** 公共域配置 */
    public static final String DOMAIN_COMMON = "DOMAIN.COMMON";
    /** 总费用价格配置键 */
    public static final String TOTAL_FEE_PRICE = "TOTAL_FEE_PRICE";
    /** 收款金额开关配置键 */
    public static final String RECEIVED_AMOUNT_SWITCH = "RECEIVED_AMOUNT_SWITCH";
    /** 线下支付费用开关配置键 */
    public static final String OFFLINE_PAY_FEE_SWITCH = "OFFLINE_PAY_FEE_SWITCH";

    /**
     * 参数验证方法
     * 验证请求参数是否包含必要的小区ID信息，并验证分页参数
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        super.validatePageInfo(reqJson);
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID");
    }

    /**
     * 执行命令方法
     * 处理月费用明细查询请求，包括数据查询、数据处理和结果返回
     *
     * @param event 命令事件对象
     * @param context 数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 根据房屋编号刷新缴费对象ID
        freshPayerObjIdByRoomNum(reqJson);

        // 将请求参数转换为查询DTO对象
        PayFeeDetailMonthDto payFeeDetailMonthDto = BeanConvertUtil.covertBean(reqJson, PayFeeDetailMonthDto.class);

        // 处理多个缴费对象ID的情况
        morePayerObjIds(reqJson, payFeeDetailMonthDto);

        // 查询符合条件的记录总数
        int count = payFeeDetailMonthInnerServiceSMOImpl.queryPagePayFeeDetailMonthsCount(payFeeDetailMonthDto);

        List<PayFeeDetailMonthDto> payFeeDetailMonthDtos = null;
        if (count > 0) {
            // 分页查询费用月明细数据
            payFeeDetailMonthDtos = payFeeDetailMonthInnerServiceSMOImpl.queryPagePayFeeDetailMonths(payFeeDetailMonthDto);
            // 根据配置处理金额小数点精度
            doScale(payFeeDetailMonthDtos);
            // 将费用属性信息刷入费用明细对象中，方便前端展示
            freshFeeAttrs(payFeeDetailMonthDtos);
        } else {
            payFeeDetailMonthDtos = new ArrayList<>();
        }

        // 构建响应结果并设置到上下文中
        context.setResponseEntity(ResultVo.createResponseEntity((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, payFeeDetailMonthDtos));
    }

    /**
     * 处理应收金额小数点精度
     * 根据费用配置的精度和小数位数对金额进行格式化处理
     *
     * @param payFeeDetailMonthDtos 费用月明细数据列表
     */
    private void doScale(List<PayFeeDetailMonthDto> payFeeDetailMonthDtos) {
        if (payFeeDetailMonthDtos == null || payFeeDetailMonthDtos.size() < 1) {
            return;
        }
        double amount = 0.0;
        for (PayFeeDetailMonthDto payFeeDetailMonthDto : payFeeDetailMonthDtos) {
            // 根据精度配置计算金额
            amount = MoneyUtil.computePriceScale(Double.parseDouble(payFeeDetailMonthDto.getReceivableAmount()),
                    payFeeDetailMonthDto.getScale(),
                    Integer.parseInt(payFeeDetailMonthDto.getDecimalPlace()));
            payFeeDetailMonthDto.setReceivableAmount(amount + "");
        }
    }

    /**
     * 处理多个缴费对象ID
     * 将请求中的多个缴费对象ID字符串转换为数组并设置到查询条件中
     *
     * @param reqJson 请求JSON对象
     * @param payFeeDetailMonthDto 费用月明细查询DTO
     */
    private void morePayerObjIds(JSONObject reqJson, PayFeeDetailMonthDto payFeeDetailMonthDto) {
        if (!reqJson.containsKey("payerObjIds") || StringUtil.isEmpty(reqJson.getString("payerObjIds"))) {
            return;
        }

        String payerObjIds = reqJson.getString("payerObjIds");
        // 将逗号分隔的字符串转换为数组
        payFeeDetailMonthDto.setObjIds(payerObjIds.split(","));
    }

    /**
     * 根据房屋编号刷新缴费对象ID
     * 通过房屋编号（格式：楼栋号-单元号-房间号）查询对应的房间ID并设置到请求参数中
     *
     * @param reqJson 请求JSON对象
     */
    private void freshPayerObjIdByRoomNum(JSONObject reqJson) {
        if (!reqJson.containsKey("roomNum") || StringUtil.isEmpty(reqJson.getString("roomNum"))) {
            return;
        }

        // 解析房屋编号格式：楼栋号-单元号-房间号
        String[] roomNums = reqJson.getString("roomNum").split("-", 3);
        if (roomNums == null || roomNums.length != 3) {
            throw new IllegalArgumentException("房屋编号格式错误！");
        }

        String floorNum = roomNums[0];
        String unitNum = roomNums[1];
        String roomNum = roomNums[2];
        
        // 根据楼栋号查询楼栋信息
        FloorDto floorDto = new FloorDto();
        floorDto.setFloorNum(floorNum);
        floorDto.setCommunityId(reqJson.getString("communityId"));
        List<FloorDto> floorDtos = floorInnerServiceSMOImpl.queryFloors(floorDto);
        if (floorDtos == null || floorDtos.size() < 1) {
            return;
        }
        
        // 遍历楼栋信息，查询单元信息
        for (FloorDto floor : floorDtos) {
            UnitDto unitDto = new UnitDto();
            unitDto.setFloorId(floor.getFloorId());
            unitDto.setUnitNum(unitNum);
            List<UnitDto> unitDtos = unitInnerServiceSMOImpl.queryUnits(unitDto);
            if (unitDtos == null || unitDtos.size() < 1) {
                continue;
            }
            
            // 遍历单元信息，查询房间信息
            for (UnitDto unit : unitDtos) {
                RoomDto roomDto = new RoomDto();
                roomDto.setUnitId(unit.getUnitId());
                roomDto.setRoomNum(roomNum);
                roomDto.setCommunityId(reqJson.getString("communityId"));
                List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);
                // 验证只能查询到一个房间
                Assert.listOnlyOne(roomDtos, "查询不到房屋！");
                // 将房间ID设置到请求参数中作为缴费对象ID
                reqJson.put("payerObjId", roomDtos.get(0).getRoomId());
            }
        }
    }

    /**
     * 刷新费用属性信息
     * 将费用相关的属性信息关联到费用明细对象中
     *
     * @param payFeeDetailMonthDtos 费用月明细数据列表
     */
    private void freshFeeAttrs(List<PayFeeDetailMonthDto> payFeeDetailMonthDtos) {
        // 收集所有费用ID
        List<String> feeIds = new ArrayList<>();
        for (PayFeeDetailMonthDto feeDto : payFeeDetailMonthDtos) {
            feeIds.add(feeDto.getFeeId());
        }

        // 批量查询费用属性
        FeeAttrDto feeAttrDto = new FeeAttrDto();
        feeAttrDto.setFeeIds(feeIds.toArray(new String[feeIds.size()]));
        List<FeeAttrDto> feeAttrDtos = feeAttrInnerServiceSMOImpl.queryFeeAttrs(feeAttrDto);

        if (feeAttrDtos == null || feeAttrDtos.size() < 1) {
            return;
        }

        // 将费用属性关联到对应的费用明细对象中
        List<FeeAttrDto> tmpFeeAttrDtos = null;
        for (PayFeeDetailMonthDto feeDto : payFeeDetailMonthDtos) {
            tmpFeeAttrDtos = new ArrayList<>();
            for (FeeAttrDto tmpFeeAttrDto : feeAttrDtos) {
                // 根据费用ID匹配属性
                if (!feeDto.getFeeId().equals(tmpFeeAttrDto.getFeeId())) {
                    continue;
                }
                tmpFeeAttrDtos.add(tmpFeeAttrDto);
            }
            feeDto.setFeeAttrs(tmpFeeAttrDtos);
        }
    }
}