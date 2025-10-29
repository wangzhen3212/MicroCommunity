/**
 * 收费收据查询命令类
 * 
 * 功能：管理员查询收费收据信息的命令处理类，支持按条件查询收据列表，
 *       并根据收费对象类型（房屋/车位）补充相关信息
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.fee.cmd.receipt;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.fee.FeeReceiptDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.owner.OwnerRoomRelDto;
import com.java110.dto.room.RoomDto;
import com.java110.dto.store.StoreUserDto;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.fee.IFeeReceiptInnerServiceSMO;
import com.java110.intf.user.IOwnerCarInnerServiceSMO;
import com.java110.intf.user.IOwnerRoomRelInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "receipt.queryAdminFeeReceipt")
public class QueryAdminFeeReceiptCmd extends Cmd {

    @Autowired
    private IFeeReceiptInnerServiceSMO feeReceiptInnerServiceSMOImpl;

    @Autowired
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl;

    @Autowired
    private IOwnerRoomRelInnerServiceSMO ownerRoomRelInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 功能：验证管理员权限和分页参数
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证管理员权限
        super.validateAdmin(context);
        // 验证分页参数
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 
     * 功能：处理收费收据查询请求，包括参数处理、数据查询、结果封装等
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求参数转换为FeeReceiptDto对象
        FeeReceiptDto feeReceiptDto = BeanConvertUtil.covertBean(reqJson, FeeReceiptDto.class);

        // 处理查询开始时间，添加时间范围
        if (!StringUtil.isEmpty(feeReceiptDto.getQstartTime())) {
            feeReceiptDto.setQstartTime(feeReceiptDto.getQstartTime() + " 00:00:00");
        }
        // 处理查询结束时间，添加时间范围
        if (!StringUtil.isEmpty(feeReceiptDto.getQendTime())) {
            feeReceiptDto.setQendTime(feeReceiptDto.getQendTime() + " 23:59:59");
        }

        // 查询收据总数
        int count = feeReceiptInnerServiceSMOImpl.queryFeeReceiptsCount(feeReceiptDto);

        List<FeeReceiptDto> feeReceiptDtos = null;
        if (count > 0) {
            // 查询收据列表
            feeReceiptDtos = feeReceiptInnerServiceSMOImpl.queryFeeReceipts(feeReceiptDto);
            // 设置店铺名称
            for (FeeReceiptDto feeReceipt : feeReceiptDtos) {
                feeReceipt.setStoreName(feeReceiptDto.getStoreName());
            }
            // 补充房屋信息
            freshRoomInfo(feeReceiptDtos);
        } else {
            // 无数据时返回空列表
            feeReceiptDtos = new ArrayList<>();
        }

        // 计算总页数并封装返回结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) feeReceiptDto.getRow()), count, feeReceiptDtos);

        // 设置HTTP响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        context.setResponseEntity(responseEntity);
    }

    /**
     * 刷新房屋信息方法
     * 
     * 功能：根据收费对象类型，为收据列表补充房屋或车位相关信息
     * 
     * @param feeReceiptDtos 收据数据列表
     */
    private void freshRoomInfo(List<FeeReceiptDto> feeReceiptDtos) {
        // 遍历所有收据记录
        for (FeeReceiptDto feeReceiptDto : feeReceiptDtos) {
            // 如果收费对象类型是房屋，直接设置房屋名称，车位号设为"-"
            if (FeeDto.PAYER_OBJ_TYPE_ROOM.equals(feeReceiptDto.getObjType())) {
                feeReceiptDto.setRoomName(feeReceiptDto.getObjName());
                feeReceiptDto.setCarNum("-");
                continue;
            }

            // 处理车位类型的收费对象
            doFreshRoomInfo(feeReceiptDto);
        }
    }

    /**
     * 处理车位信息的房屋信息刷新
     * 
     * 功能：根据车位信息查询对应的房屋信息，并设置到收据对象中
     * 
     * @param feeReceiptDto 收据数据对象
     */
    private void doFreshRoomInfo(FeeReceiptDto feeReceiptDto) {
        // 设置车位号
        feeReceiptDto.setCarNum(feeReceiptDto.getObjName());
        
        // 查询车位信息
        OwnerCarDto ownerCarDto = new OwnerCarDto();
        ownerCarDto.setCarId(feeReceiptDto.getObjId());
        ownerCarDto.setCommunityId(feeReceiptDto.getCommunityId());
        List<OwnerCarDto> ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);

        // 如果未找到车位信息，设置房屋名称为"-"
        if (ListUtil.isNull(ownerCarDtos)) {
            feeReceiptDto.setRoomName("-");
            return;
        }
        
        // 根据车主ID查询房屋关系
        OwnerRoomRelDto ownerRoomRelDto = new OwnerRoomRelDto();
        ownerRoomRelDto.setOwnerId(ownerCarDtos.get(0).getOwnerId());

        List<OwnerRoomRelDto> ownerRoomRelDtos = ownerRoomRelInnerServiceSMOImpl.queryOwnerRoomRels(ownerRoomRelDto);
        
        // 如果未找到房屋关系，设置房屋名称为"-"
        if (ListUtil.isNull(ownerRoomRelDtos)) {
            feeReceiptDto.setRoomName("-");
            return;
        }

        // 收集所有房间ID
        List<String> roomIds = new ArrayList<>();
        for (OwnerRoomRelDto tOwnerRoomRelDto : ownerRoomRelDtos) {
            roomIds.add(tOwnerRoomRelDto.getRoomId());
        }

        // 查询房间详细信息
        RoomDto roomDto = new RoomDto();
        roomDto.setCommunityId(feeReceiptDto.getCommunityId());
        roomDto.setRoomIds(roomIds.toArray(new String[roomIds.size()]));
        List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);
        
        // 拼接房屋名称（格式：楼层-单元-房号）
        String roomName = "";
        for (RoomDto tRoomDto : roomDtos) {
            roomName += (tRoomDto.getFloorNum() + "-" + tRoomDto.getUnitNum() + "-" + tRoomDto.getRoomNum() + "/");
        }

        // 去除末尾的斜杠
        roomName = roomName.endsWith("/") ? roomName.substring(0, roomName.length() - 1) : roomName;
        // 设置房屋名称
        feeReceiptDto.setRoomName(roomName);
    }
}