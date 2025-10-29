/**
 * 应用端删除成员车辆命令类
 * 
 * 该命令用于处理移动端应用删除成员车辆的业务逻辑，包括权限验证和车辆删除操作。
 * 主要功能包括：
 * 1. 验证用户权限和车辆信息
 * 2. 执行车辆删除操作
 * 3. 支持事务处理确保数据一致性
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.user.cmd.owner;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.owner.OwnerAppUserDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.intf.user.*;
import com.java110.po.car.OwnerCarPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

@Java110Cmd(serviceCode = "owner.appDeleteMemberCar")
public class AppDeleteMemberCarCmd extends Cmd {

    @Autowired
    private IOwnerAppUserInnerServiceSMO ownerAppUserInnerServiceSMOImpl;

    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl;

    @Autowired
    private IOwnerCarV1InnerServiceSMO ownerCarV1InnerServiceSMOImpl;

    /**
     * 验证请求参数和业务逻辑
     * 
     * 该方法主要完成以下验证：
     * 1. 验证用户是否绑定业主身份
     * 2. 验证请求参数完整性
     * 3. 验证车辆信息是否存在
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求头和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当验证失败时抛出命令异常
     * @throws ParseException 当解析数据时出现错误抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从请求头中获取用户ID
        String userId = context.getReqHeaders().get("user-id");

        // 构建业主应用用户查询条件
        OwnerAppUserDto ownerAppUserDto = new OwnerAppUserDto();
        ownerAppUserDto.setUserId(userId);
        ownerAppUserDto.setCommunityId(reqJson.getString("communityId"));
        // 查询用户绑定的业主信息
        List<OwnerAppUserDto> ownerAppUserDtos = ownerAppUserInnerServiceSMOImpl.queryOwnerAppUsers(ownerAppUserDto);

        // 验证用户是否绑定了业主
        if (ListUtil.isNull(ownerAppUserDtos)) {
            throw new CmdException("未绑定业主");
        }

        // 遍历查询结果，查找有效的成员ID
        String memberId = "";
        for (OwnerAppUserDto tmpOwnerAppUserDto : ownerAppUserDtos) {
            // 跳过无效的成员ID（-1表示无效）
            if ("-1".equals(tmpOwnerAppUserDto.getMemberId())) {
                continue;
            }
            memberId = tmpOwnerAppUserDto.getMemberId();
        }

        // 验证是否找到有效的成员ID
        if (StringUtil.isEmpty(memberId)) {
            throw new CmdException("未绑定业主");
        }

        // 根据成员ID查询业主详细信息
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setCommunityId(reqJson.getString("communityId"));
        ownerDto.setMemberId(memberId);
        List<OwnerDto> ownerDtos = ownerV1InnerServiceSMOImpl.queryOwners(ownerDto);

        // 验证业主信息存在且唯一
        Assert.listOnlyOne(ownerDtos, "业主不存在");

        // 将业主ID设置到请求参数中，供后续使用
        reqJson.put("ownerId", ownerDtos.get(0).getOwnerId());
        
        // 验证请求参数完整性
        Assert.jsonObjectHaveKey(reqJson, "carMemberId", "请求报文中未包含carMemberId");
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求报文中未包含communityId");

        // 构建车辆查询条件
        OwnerCarDto ownerCarDto = new OwnerCarDto();
        ownerCarDto.setMemberId(reqJson.getString("carMemberId"));
        ownerCarDto.setCommunityId(reqJson.getString("communityId"));
        ownerCarDto.setOwnerId(reqJson.getString("ownerId"));
        // 设置查询的车辆类型：主要车辆和成员车辆（排除临时车辆）
        ownerCarDto.setCarTypeCds(new String[]{OwnerCarDto.CAR_TYPE_PRIMARY, OwnerCarDto.CAR_TYPE_MEMBER});
        // 查询符合条件的车辆信息
        List<OwnerCarDto> ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);

        // 验证车辆信息存在且唯一
        Assert.listOnlyOne(ownerCarDtos, "当前未找到需要删除车辆");
    }

    /**
     * 执行删除成员车辆操作
     * 
     * 该方法在事务中执行车辆删除操作，确保数据一致性。
     * 如果删除失败，会抛出异常并回滚事务。
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出命令异常
     * @throws ParseException 当解析数据时出现错误抛出解析异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 构建车辆删除对象
        OwnerCarPo ownerCarPo = new OwnerCarPo();
        ownerCarPo.setCommunityId(reqJson.getString("communityId"));
        ownerCarPo.setMemberId(reqJson.getString("carMemberId"));
        
        // 执行车辆删除操作
        int flag = ownerCarV1InnerServiceSMOImpl.deleteOwnerCar(ownerCarPo);
        
        // 验证删除操作是否成功
        if (flag < 1) {
            throw new IllegalArgumentException("删除车辆出错");
        }
    }
}