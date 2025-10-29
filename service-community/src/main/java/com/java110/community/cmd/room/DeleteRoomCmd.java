package com.java110.community.cmd.room;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.Environment;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.fee.PayFeeDto;
import com.java110.dto.owner.OwnerRoomRelDto;
import com.java110.dto.unit.UnitDto;
import com.java110.intf.community.*;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.intf.fee.IPayFeeDetailMonthInnerServiceSMO;
import com.java110.intf.fee.IPayFeeDetailV1InnerServiceSMO;
import com.java110.intf.fee.IPayFeeV1InnerServiceSMO;
import com.java110.intf.report.IReportOweFeeInnerServiceSMO;
import com.java110.intf.user.IOwnerRoomRelV1InnerServiceSMO;
import com.java110.po.fee.PayFeeDetailPo;
import com.java110.po.fee.PayFeePo;
import com.java110.po.owner.OwnerRoomRelPo;
import com.java110.po.payFee.PayFeeDetailMonthPo;
import com.java110.po.reportFee.ReportOweFeePo;
import com.java110.po.room.RoomPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 删除房屋命令类
 * 
 * 该命令类用于处理删除房屋的业务逻辑，包括：
 * 1. 删除房屋基本信息
 * 2. 解绑房屋与业主的关系
 * 3. 删除房屋相关的费用信息
 * 4. 删除房屋相关的缴费记录和欠费记录
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023
 */
@Java110CmdDoc(title = "删除房屋",
        description = "对应后台 删除房屋功能，这里需要先解绑业主和房屋关系 才能做删除",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/room.deleteRoom",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "room.deleteRoom",
        seq = 15
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "roomId", length = 30, remark = "单元ID"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

@Java110ExampleDoc(
        reqBody = "{\n" +
                "\t\"roomId\": \"123123123123\",\n" +
                "\t\"communityId\": \"2022121921870161\",\n" +
                "}",
        resBody = "{\"code\":0,\"msg\":\"成功\"}"
)
@Java110Cmd(serviceCode = "room.deleteRoom")
public class DeleteRoomCmd extends Cmd {

    @Autowired
    private IUnitV1InnerServiceSMO unitV1InnerServiceSMOImpl;
    @Autowired
    private IUnitInnerServiceSMO unitInnerServiceSMOImpl;
    @Autowired
    private IFloorV1InnerServiceSMO floorV1InnerServiceSMOImpl;

    @Autowired
    private IRoomV1InnerServiceSMO roomV1InnerServiceSMOImpl;

    @Autowired
    private IRoomAttrV1InnerServiceSMO roomAttrV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerRoomRelV1InnerServiceSMO ownerRoomRelV1InnerServiceSMOImpl;

    @Autowired
    private IPayFeeV1InnerServiceSMO payFeeV1InnerServiceSMOImpl;

    @Autowired
    private IPayFeeDetailV1InnerServiceSMO payFeeDetailV1InnerServiceSMOImpl;

    @Autowired
    private IReportOweFeeInnerServiceSMO reportOweFeeInnerServiceSMOImpl;

    @Autowired
    private IPayFeeDetailMonthInnerServiceSMO payFeeDetailMonthInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整和有效，包括：
     * 1. 检查必要参数是否存在
     * 2. 验证单元ID是否属于指定小区
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 检查当前环境是否为开发环境
        Environment.isDevEnv();
        
        // 验证必要参数是否存在
        Assert.jsonObjectHaveKey(reqJson, "roomId", "请求报文中未包含roomId节点");
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求报文中未包含communityId节点");
        Assert.jsonObjectHaveKey(reqJson, "unitId", "请求报文中未包含unitId节点");

        // 构建单元查询条件
        UnitDto unitDto = new UnitDto();
        unitDto.setCommunityId(reqJson.getString("communityId"));
        unitDto.setUnitId(reqJson.getString("unitId"));
        
        // 校验小区楼ID和小区是否有对应关系
        List<UnitDto> units = unitInnerServiceSMOImpl.queryUnitsByCommunityId(unitDto);

        // 验证单元是否存在
        if (units == null || units.size() < 1) {
            throw new IllegalArgumentException("传入单元ID不是该小区的单元");
        }
    }

    /**
     * 执行删除房屋命令
     * 
     * 主要执行以下操作：
     * 1. 删除房屋基本信息
     * 2. 解绑业主与房屋的关系
     * 3. 删除房屋相关的费用信息
     * 4. 设置响应结果
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 构建业务数据对象
        JSONObject businessUnit = new JSONObject();
        businessUnit.putAll(reqJson);
        
        // 将JSON对象转换为RoomPo对象
        RoomPo roomPo = BeanConvertUtil.covertBean(businessUnit, RoomPo.class);
        
        // 执行房屋删除操作
        int flag = roomV1InnerServiceSMOImpl.deleteRoom(roomPo);
        if (flag < 1) {
            throw new CmdException("删除房屋失败");
        }

        // 解绑业主与房屋的关系
        unbindOwnerRoomRel(roomPo);

        // 删除房屋下的费用和缴费记录
        deleteRoomFee(roomPo);

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }

    /**
     * 自动解绑业主和房屋关系
     * 
     * 查询房屋下所有的业主关系并逐一删除
     * 
     * @param roomPo 房屋信息对象
     * @throws CmdException 当房屋ID为空时抛出异常
     */
    private void unbindOwnerRoomRel(RoomPo roomPo) {
        // 验证房屋ID不能为空
        if (StringUtil.isEmpty(roomPo.getRoomId())) {
            throw new CmdException("房屋ID不能为空");
        }

        // 构建业主房屋关系查询条件
        OwnerRoomRelDto ownerRoomRelDto = new OwnerRoomRelDto();
        ownerRoomRelDto.setRoomId(roomPo.getRoomId());
        
        // 查询房屋下的所有业主关系
        List<OwnerRoomRelDto> ownerRoomRelDtos = ownerRoomRelV1InnerServiceSMOImpl.queryOwnerRoomRels(ownerRoomRelDto);

        // 如果没有业主关系，直接返回
        if (ownerRoomRelDtos == null || ownerRoomRelDtos.size() < 1) {
            return;
        }
        
        // 逐一删除业主房屋关系
        OwnerRoomRelPo ownerRoomRelPo = null;
        for (OwnerRoomRelDto tmpOwnerRoomRelDto : ownerRoomRelDtos) {
            ownerRoomRelPo = new OwnerRoomRelPo();
            ownerRoomRelPo.setRelId(tmpOwnerRoomRelDto.getRelId());
            ownerRoomRelV1InnerServiceSMOImpl.deleteOwnerRoomRel(ownerRoomRelPo);
        }
    }

    /**
     * 删除房屋费用
     * 
     * 删除房屋相关的所有费用信息，包括：
     * 1. 费用基本信息
     * 2. 缴费记录
     * 3. 欠费记录
     * 4. 离散月数据
     * 
     * @param roomPo 房屋信息对象
     * @throws CmdException 当房屋ID为空时抛出异常
     */
    private void deleteRoomFee(RoomPo roomPo) {
        // 验证房屋ID不能为空
        if (StringUtil.isEmpty(roomPo.getRoomId())) {
            throw new CmdException("房屋ID不能为空");
        }
        
        // 构建费用查询条件
        PayFeeDto feeDto = new PayFeeDto();
        feeDto.setPayerObjId(roomPo.getRoomId());           // 设置缴费对象ID为房屋ID
        feeDto.setPayerObjType(FeeDto.PAYER_OBJ_TYPE_ROOM); // 设置缴费对象类型为房屋
        feeDto.setCommunityId(roomPo.getCommunityId());     // 设置小区ID
        
        // 查询房屋下的所有费用
        List<PayFeeDto> feeDtos = payFeeV1InnerServiceSMOImpl.queryPayFees(feeDto);

        // 如果没有费用信息，直接返回
        if (feeDtos == null || feeDtos.size() < 1) {
            return;
        }

        // 逐一删除费用信息
        for (PayFeeDto payFeeDto : feeDtos) {
            doDeleteFee(payFeeDto);
        }
    }

    /**
     * 执行删除费用操作
     * 
     * 删除单个费用的所有相关信息，包括：
     * 1. 缴费记录
     * 2. 费用基本信息
     * 3. 欠费记录
     * 4. 离散月数据
     * 
     * @param payFeeDto 费用信息数据传输对象
     */
    private void doDeleteFee(PayFeeDto payFeeDto) {
        // 删除缴费记录
        PayFeeDetailPo payFeeDetailPo = new PayFeeDetailPo();
        payFeeDetailPo.setFeeId(payFeeDto.getFeeId());              // 设置费用ID
        payFeeDetailPo.setCommunityId(payFeeDto.getCommunityId());  // 设置小区ID
        payFeeDetailV1InnerServiceSMOImpl.deletePayFeeDetailNew(payFeeDetailPo);

        // 删除费用基本信息
        PayFeePo payFeePo = new PayFeePo();
        payFeePo.setFeeId(payFeeDto.getFeeId());                    // 设置费用ID
        payFeePo.setCommunityId(payFeeDto.getCommunityId());        // 设置小区ID
        payFeeV1InnerServiceSMOImpl.deletePayFee(payFeePo);

        // 删除欠费记录
        ReportOweFeePo reportOweFeePo = new ReportOweFeePo();
        reportOweFeePo.setFeeId(payFeeDto.getFeeId());              // 设置费用ID
        reportOweFeePo.setCommunityId(payFeeDto.getCommunityId());  // 设置小区ID
        reportOweFeeInnerServiceSMOImpl.deleteReportOweFee(reportOweFeePo);

        // 删除离散月数据
        PayFeeDetailMonthPo payFeeDetailMonthPo = new PayFeeDetailMonthPo();
        payFeeDetailMonthPo.setFeeId(payFeeDto.getFeeId());         // 设置费用ID
        payFeeDetailMonthPo.setCommunityId(payFeeDto.getCommunityId()); // 设置小区ID
        payFeeDetailMonthInnerServiceSMOImpl.deletePayFeeDetailMonth(payFeeDetailMonthPo);
    }
}