package com.java110.user.cmd.user;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.SendSmsFactory;
import com.java110.dto.msg.SmsDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.room.RoomDto;
import com.java110.intf.common.ISmsInnerServiceSMO;
import com.java110.intf.community.IRoomV1InnerServiceSMO;
import com.java110.intf.user.IOwnerInnerServiceSMO;
import com.java110.intf.user.IOwnerV1InnerServiceSMO;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.utils.cache.CommonCache;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import com.java110.utils.util.ValidatorUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Date;
import java.util.List;

/**
 * 业主发送短信验证码命令类
 * 
 * 该类负责处理向业主发送短信验证码的业务逻辑，支持通过手机号或房屋信息查询业主并发送验证码
 * 主要功能包括参数验证、业主信息查询、验证码生成和发送等
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode user.ownerSendSms
 */
@Java110Cmd(serviceCode = "user.ownerSendSms")
public class OwnerSendSmsCmd extends Cmd {

    /** 公共域常量 */
    public static final String DOMAIN_COMMON = "DOMAIN.COMMON";

    /** 身份证开关常量 */
    public static final String ID_CARD_SWITCH = "ID_CARD_SWITCH";

    /** 业主内部服务接口 */
    @Autowired
    private IOwnerInnerServiceSMO ownerInnerServiceSMOImpl;

    /** 用户内部服务接口 */
    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;

    /** 短信内部服务接口 */
    @Autowired
    private ISmsInnerServiceSMO smsInnerServiceSMOImpl;

    /** 业主V1内部服务接口 */
    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;

    /** 房屋V1内部服务接口 */
    @Autowired
    private IRoomV1InnerServiceSMO roomV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，包括查询方式、小区信息、房屋信息或手机号等
     * 同时校验验证码发送频率，防止重复发送
     * 
     * @param event   命令事件对象
     * @param context 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        // 验证必填参数
        Assert.hasKeyAndValue(reqJson, "queryWay", "未包含查询方式");
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");

        // 根据查询方式验证不同的参数
        if ("room".equals(reqJson.getString("queryWay"))) {
            // 房屋查询方式需要房屋信息
            Assert.hasKeyAndValue(reqJson, "roomNum", "未包含房屋信息");
        } else {
            // 手机号查询方式需要手机号
            Assert.hasKeyAndValue(reqJson, "link", "未包含手机号");
            // 验证手机号格式
            if (!ValidatorUtil.isMobile(reqJson.getString("link"))) {
                throw new IllegalArgumentException("手机号格式错误");
            }
        }

        // 计算业主手机号
        String link = computeOwnerLink(reqJson);
        reqJson.put("tel", link);

        // 校验验证码发送频率，防止重复发送
        String smsCode = CommonCache.getValue(reqJson.getString("tel") + SendSmsFactory.VALIDATE_CODE);

        if (!StringUtil.isEmpty(smsCode) && smsCode.contains("-")) {
            // 解析缓存中的时间戳
            long oldTime = Long.parseLong(smsCode.substring(smsCode.indexOf("-"), smsCode.length()));
            long nowTime = new Date().getTime();
            // 检查是否在2分钟内重复发送
            if (nowTime - oldTime < 1000 * 60 * 2) {
                throw new IllegalArgumentException("请不要重复发送验证码");
            }
        }
    }

    /**
     * 计算业主手机号
     * 
     * 根据查询方式（手机号或房屋信息）获取业主的手机号码
     * 
     * @param reqJson 请求参数的JSON对象
     * @return String 业主的手机号码
     * @throws CmdException 当业主不存在时抛出异常
     */
    private String computeOwnerLink(JSONObject reqJson) {

        String link = "";
        // 手机号查询方式
        if ("phone".equals(reqJson.getString("queryWay"))) {
            OwnerDto ownerDto = new OwnerDto();
            ownerDto.setLink(reqJson.getString("link"));
            ownerDto.setCommunityId(reqJson.getString("communityId"));
            ownerDto.setOwnerTypeCd(OwnerDto.OWNER_TYPE_CD_OWNER);
            // 查询业主信息
            List<OwnerDto> ownerDtos = ownerV1InnerServiceSMOImpl.queryOwners(ownerDto);
            if (ownerDtos == null || ownerDtos.size() < 1) {
                throw new CmdException("业主不存在");
            }
            link = reqJson.getString("link");
            return link;
        }
        
        // 房屋信息查询方式
        String roomNumStr = reqJson.getString("roomNum");
        // 解析房屋编号（格式：楼栋-单元-房间）
        String[] roomNums = roomNumStr.split("-", 3);
        if (roomNums == null || roomNums.length != 3) {
            throw new CmdException("业主不存在");
        }

        // 根据房屋信息查询房屋
        RoomDto roomDto = new RoomDto();
        roomDto.setFloorNum(roomNums[0]);
        roomDto.setUnitNum(roomNums[1]);
        roomDto.setRoomNum(roomNums[2]);
        roomDto.setCommunityId(reqJson.getString("communityId"));
        List<RoomDto> roomDtos = roomV1InnerServiceSMOImpl.queryRooms(roomDto);

        // 验证房屋存在且唯一
        Assert.listOnlyOne(roomDtos, "房屋不存在");

        // 根据房屋ID查询业主信息
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setRoomId(roomDtos.get(0).getRoomId());
        ownerDto.setCommunityId(reqJson.getString("communityId"));
        ownerDto.setOwnerTypeCd(OwnerDto.OWNER_TYPE_CD_OWNER);
        List<OwnerDto> ownerDtos = ownerInnerServiceSMOImpl.queryOwners(ownerDto);
        if (ownerDtos == null || ownerDtos.size() < 1) {
            throw new CmdException("业主不存在");
        }

        // 返回业主手机号
        return ownerDtos.get(0).getLink();
    }

    /**
     * 执行发送短信命令
     * 
     * 生成验证码并发送给业主，支持真实发送和模拟发送两种模式
     * 
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当发送失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String tel = reqJson.getString("tel");
        String captchaType = reqJson.getString("captchaType");
        
        // 生成6位数字验证码
        String msgCode = SendSmsFactory.generateMessageCode(6);
        SmsDto smsDto = new SmsDto();
        smsDto.setTel(tel);
        smsDto.setCode(msgCode);
        
        // 检查短信发送开关是否开启
        if ("ON".equals(MappingCache.getValue(MappingConstant.SMS_DOMAIN, SendSmsFactory.SMS_SEND_SWITCH))) {
            // 真实发送短信
            smsDto = smsInnerServiceSMOImpl.send(smsDto);
            // 设置发送结果消息（显示手机尾号）
            smsDto.setMsg("向业主手机(尾号" + tel.substring(7, tel.length()) + ")下发验证码");
        } else {
            // 模拟发送模式：将验证码缓存到本地
            CommonCache.setValue(smsDto.getTel() + SendSmsFactory.VALIDATE_CODE, 
                smsDto.getCode().toLowerCase() + "-" + new Date().getTime(), 
                CommonCache.defaultExpireTime);
            smsDto.setSuccess(true);
            // 设置模拟发送的消息内容
            smsDto.setMsg("您的验证码为" + msgCode);
        }
        
        // 设置响应结果
        context.setResponseEntity(ResultVo.createResponseEntity(
            smsDto.isSuccess() ? ResultVo.CODE_OK : ResultVo.CODE_ERROR, 
            smsDto.getMsg()));
    }
}