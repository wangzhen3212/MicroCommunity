package com.java110.user.cmd.owner;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.SendSmsFactory;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.payFee.PayFeeQrcodeDto;
import com.java110.dto.room.RoomDto;
import com.java110.intf.community.IRoomV1InnerServiceSMO;
import com.java110.intf.fee.IPayFeeQrcodeV1InnerServiceSMO;
import com.java110.intf.user.IOwnerInnerServiceSMO;
import com.java110.intf.user.IOwnerV1InnerServiceSMO;
import com.java110.utils.cache.CommonCache;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import com.java110.utils.util.ValidatorUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

/**
 * 二维码查询业主命令类
 * 功能：通过二维码信息查询业主信息，支持按房屋编号和手机号两种查询方式
 * 用途：用于物业系统中通过扫描二维码获取业主信息的业务场景
 */
@Java110Cmd(serviceCode = "owner.getQrcodeOwner")
public class GetQrcodeOwnerCmd extends Cmd {

    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;

    @Autowired
    private IRoomV1InnerServiceSMO roomV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerInnerServiceSMO ownerInnerServiceSMOImpl;

    @Autowired
    private IPayFeeQrcodeV1InnerServiceSMO payFeeQrcodeV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性，包括二维码验证、短信验证码验证等
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException   命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证必需参数是否存在
        Assert.hasKeyAndValue(reqJson, "queryWay", "未包含查询方式");
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
        Assert.hasKeyAndValue(reqJson, "pfqId", "未包含二维码信息");

        // 根据查询方式验证不同的必需参数
        if ("room".equals(reqJson.getString("queryWay"))) {
            // 按房屋查询需要房屋编号
            Assert.hasKeyAndValue(reqJson, "roomNum", "未包含房屋信息");
        } else {
            // 按手机号查询需要手机号
            Assert.hasKeyAndValue(reqJson, "link", "未包含手机号");
            // 验证手机号格式
            if (!ValidatorUtil.isMobile(reqJson.getString("link"))) {
                throw new IllegalArgumentException("手机号格式错误");
            }
        }

        // 验证二维码信息是否存在
        PayFeeQrcodeDto payFeeQrcodeDto = new PayFeeQrcodeDto();
        payFeeQrcodeDto.setPfqId(reqJson.getString("pfqId"));
        payFeeQrcodeDto.setCommunityId(reqJson.getString("communityId"));
        List<PayFeeQrcodeDto> payFeeQrcodeDtos = payFeeQrcodeV1InnerServiceSMOImpl.queryPayFeeQrcodes(payFeeQrcodeDto);

        // 确保二维码唯一存在
        Assert.listOnlyOne(payFeeQrcodeDtos, "二维码不存在");

        // 检查是否需要短信验证
        // todo 不校验验证码 - 临时注释，后续可能需要开启验证
        if (!"ON".equals(payFeeQrcodeDtos.get(0).getSmsValidate())) {
            return; // 不需要短信验证，直接返回
        }
        
        // 需要短信验证时验证验证码
        Assert.hasKeyAndValue(reqJson, "msgCode", "未包含验证码信息");

        // 计算业主信息并验证短信验证码
        OwnerDto ownerDto = computeOwner(reqJson);
        String smsCode = CommonCache.getValue(ownerDto.getLink() + SendSmsFactory.VALIDATE_CODE);

        // 处理验证码格式（可能包含时间戳）
        if (!StringUtil.isEmpty(smsCode) && smsCode.contains("-")) {
            smsCode = smsCode.substring(0, smsCode.indexOf("-"));
        }

        // 验证验证码是否正确
        if (!reqJson.getString("msgCode").equals(smsCode)) {
            throw new CmdException("验证码错误");
        }
    }

    /**
     * 执行命令方法
     * 处理业务逻辑，查询业主信息并返回结果
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException   命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 计算业主信息
        OwnerDto ownerDto = computeOwner(reqJson);
        String ownerName = ownerDto.getName();

        // 构建返回数据
        JSONObject data = new JSONObject();
        data.put("ownerId", ownerDto.getOwnerId());        // 业主ID
        data.put("roomId", ownerDto.getRoomId());          // 房间ID
        data.put("ownerName", StringUtil.maskName(ownerName)); // 脱敏后的业主姓名

        // 设置响应结果
        context.setResponseEntity(ResultVo.createResponseEntity(data));
    }

    /**
     * 计算业主信息方法
     * 根据查询方式（手机号或房屋编号）查询业主信息
     *
     * @param reqJson 请求参数JSON对象
     * @return OwnerDto 业主信息数据传输对象
     * @throws CmdException 当业主不存在时抛出异常
     */
    private OwnerDto computeOwner(JSONObject reqJson) {
        // 按手机号查询方式
        if ("phone".equals(reqJson.getString("queryWay"))) {
            OwnerDto ownerDto = new OwnerDto();
            ownerDto.setLink(reqJson.getString("link"));                   // 设置手机号
            ownerDto.setCommunityId(reqJson.getString("communityId"));     // 设置小区ID
            ownerDto.setOwnerTypeCd(OwnerDto.OWNER_TYPE_CD_OWNER);         // 设置业主类型
            List<OwnerDto> ownerDtos = ownerV1InnerServiceSMOImpl.queryOwners(ownerDto);
            
            // 验证业主是否存在
            if (ownerDtos == null || ownerDtos.size() < 1) {
                throw new CmdException("业主不存在");
            }
            return ownerDtos.get(0); // 返回第一个匹配的业主
        }
        
        // 按房屋编号查询方式
        String roomNumStr = reqJson.getString("roomNum");
        // 解析房屋编号格式：楼层-单元-房间号
        String[] roomNums = roomNumStr.split("-", 3);
        if (roomNums == null || roomNums.length != 3) {
            throw new CmdException("业主不存在");
        }

        // 构建房屋查询条件
        RoomDto roomDto = new RoomDto();
        roomDto.setFloorNum(roomNums[0]);                  // 设置楼层号
        roomDto.setUnitNum(roomNums[1]);                   // 设置单元号
        roomDto.setRoomNum(roomNums[2]);                   // 设置房间号
        roomDto.setCommunityId(reqJson.getString("communityId")); // 设置小区ID
        List<RoomDto> roomDtos = roomV1InnerServiceSMOImpl.queryRooms(roomDto);

        // 验证房屋是否存在且唯一
        Assert.listOnlyOne(roomDtos, "房屋不存在");

        // 根据房屋ID查询业主信息
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setRoomId(roomDtos.get(0).getRoomId());           // 设置房间ID
        ownerDto.setCommunityId(reqJson.getString("communityId")); // 设置小区ID
        ownerDto.setOwnerTypeCd(OwnerDto.OWNER_TYPE_CD_OWNER);     // 设置业主类型
        List<OwnerDto> ownerDtos = ownerInnerServiceSMOImpl.queryOwners(ownerDto);
        
        // 验证业主是否存在
        if (ownerDtos == null || ownerDtos.size() < 1) {
            throw new CmdException("业主不存在");
        }
        
        // 设置房间ID并返回业主信息
        ownerDtos.get(0).setRoomId(roomDtos.get(0).getRoomId());
        return ownerDtos.get(0);
    }
}