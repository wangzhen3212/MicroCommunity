/**
 * 业主退房命令处理类
 * 
 * 功能描述：处理业主与房屋解绑的业务逻辑，包括验证房屋是否存在未完成费用，
 * 以及执行业主退房操作（删除业主房屋关系，更新房屋状态为空闲）
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.user.cmd.owner;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.Environment;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.room.RoomDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.fee.PayFeeDto;
import com.java110.dto.owner.OwnerRoomRelDto;
import com.java110.intf.user.IOwnerRoomRelV1InnerServiceSMO;
import com.java110.intf.community.IRoomV1InnerServiceSMO;
import com.java110.intf.fee.IPayFeeV1InnerServiceSMO;
import com.java110.po.owner.OwnerRoomRelPo;
import com.java110.po.room.RoomPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Java110Cmd(serviceCode = "owner.ownerExitRoom")
public class OwnerExitRoomCmd extends Cmd {

    @Autowired
    private IPayFeeV1InnerServiceSMO payFeeV1InnerServiceSMOImpl;

    @Autowired
    private IRoomV1InnerServiceSMO roomV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerRoomRelV1InnerServiceSMO ownerRoomRelV1InnerServiceSMOImpl;

    /**
     * 验证请求参数和业务规则
     * 
     * 功能描述：验证请求参数完整性，检查所选房屋是否存在未完成的费用，
     * 如果存在费用则不允许退房操作
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 检查当前环境是否为开发环境
        Environment.isDevEnv();
        
        // 验证请求报文中必须包含业主ID
        Assert.jsonObjectHaveKey(reqJson, "ownerId", "请求报文中未包含业主");

        // 获取选择的房屋列表
        JSONArray selectRooms = reqJson.getJSONArray("selectRooms");

        // 验证是否选择了房屋
        if(selectRooms == null || selectRooms.size() < 1){
            throw new CmdException("未选择房屋");
        }

        PayFeeDto payFeeDto = null;
        List<PayFeeDto> payFeeDtos = null;
        
        // 遍历所有选择的房屋，检查是否存在未完成的费用
        for(int selectIndex = 0; selectIndex < selectRooms.size(); selectIndex++){
            // 创建费用查询对象
            payFeeDto = new PayFeeDto();
            payFeeDto.setPayerObjId(selectRooms.getString(selectIndex)); // 设置缴费对象ID（房屋ID）
            payFeeDto.setPayerObjType(FeeDto.PAYER_OBJ_TYPE_ROOM); // 设置缴费对象类型为房屋
            payFeeDto.setState(FeeDto.STATE_DOING); // 设置费用状态为进行中（未完成）
            
            // 查询该房屋是否存在未完成的费用
            payFeeDtos = payFeeV1InnerServiceSMOImpl.queryPayFees(payFeeDto);
            
            // 如果存在未完成的费用，则抛出异常
            if(payFeeDtos != null && payFeeDtos.size()>0){
                // 查询房屋信息用于生成错误消息
                RoomDto roomDto = new RoomDto();
                roomDto.setRoomId(selectRooms.getString(selectIndex));
                List<RoomDto> roomDtos = roomV1InnerServiceSMOImpl.queryRooms(roomDto);
                
                String roomName = "";
                if(roomDtos != null && roomDtos.size()> 0){
                    // 生成完整的房屋名称：楼层-单元-房号
                    roomName = roomDtos.get(0).getFloorNum()+"-"+roomDtos.get(0).getUnitNum()+"-"+roomDtos.get(0).getRoomNum();
                }
                // 抛出包含房屋名称的异常信息
                throw new CmdException(roomName+"存在费用");
            }
        }
    }

    /**
     * 执行业主退房操作
     * 
     * 功能描述：删除业主与房屋的关联关系，并将房屋状态更新为空闲状态
     * 该方法使用事务注解确保操作的原子性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 获取请求中的房屋列表
        JSONArray rooms = reqJson.getJSONArray("selectRooms");
        OwnerRoomRelPo ownerRoomRelPo = null;
        int flag = 0; // 操作结果标志
        OwnerRoomRelDto ownerRoomRelDto = null;
        List<OwnerRoomRelDto> ownerRoomRelDtos = null;
        
        // 遍历所有需要退房的房屋
        for (int roomIndex = 0; roomIndex < rooms.size(); roomIndex++) {
            // 查询业主与房屋的关联关系
            ownerRoomRelDto = new OwnerRoomRelDto();
            ownerRoomRelDto.setOwnerId(reqJson.getString("ownerId")); // 设置业主ID
            ownerRoomRelDto.setRoomId(rooms.getString(roomIndex)); // 设置房屋ID
            ownerRoomRelDtos = ownerRoomRelV1InnerServiceSMOImpl.queryOwnerRoomRels(ownerRoomRelDto);
            
            // 如果存在关联关系，则删除所有关联记录
            if(ownerRoomRelDtos != null && ownerRoomRelDtos.size()>0){
                for(OwnerRoomRelDto tmpOwnerRoomRelDto : ownerRoomRelDtos) {
                    ownerRoomRelPo = new OwnerRoomRelPo();
                    ownerRoomRelPo.setRelId(tmpOwnerRoomRelDto.getRelId()); // 设置关联关系ID
                    // 删除业主房屋关联关系
                    flag = ownerRoomRelV1InnerServiceSMOImpl.deleteOwnerRoomRel(ownerRoomRelPo);
                    if (flag < 1) {
                        throw new CmdException("删除业主房屋失败");
                    }
                }
            }

            // 更新房屋状态为空闲
            RoomPo roomPo = new RoomPo();
            roomPo.setRoomId(rooms.getString(roomIndex)); // 设置房屋ID
            roomPo.setCommunityId(reqJson.getString("communityId")); // 设置小区ID
            roomPo.setState(RoomDto.STATE_FREE); // 设置房屋状态为空闲
            flag = roomV1InnerServiceSMOImpl.updateRoom(roomPo);
            if (flag < 1) {
                throw new CmdException("操作业主失败");
            }
        }
    }
}