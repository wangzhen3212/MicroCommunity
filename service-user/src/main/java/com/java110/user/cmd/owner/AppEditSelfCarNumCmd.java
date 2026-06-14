package com.java110.user.cmd.owner;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.IotDataDto;
import com.java110.dto.owner.OwnerAppUserDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.intf.job.IIotInnerServiceSMO;
import com.java110.intf.user.IOwnerAppUserInnerServiceSMO;
import com.java110.intf.user.IOwnerCarInnerServiceSMO;
import com.java110.intf.user.IOwnerCarV1InnerServiceSMO;
import com.java110.intf.user.IOwnerV1InnerServiceSMO;
import com.java110.po.car.OwnerCarPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

/**
 * 业主自己修改车牌号命令类
 * 处理业主通过APP自助修改车辆车牌号的业务逻辑
 * 包括参数验证、权限校验、车辆状态检查和车牌号更新等操作
 */
@Java110Cmd(serviceCode = "owner.appEditSelfCarNum")
public class AppEditSelfCarNumCmd extends Cmd {

    @Autowired
    private IOwnerAppUserInnerServiceSMO ownerAppUserInnerServiceSMOImpl;

    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl;

    @Autowired
    private IIotInnerServiceSMO iotInnerServiceSMOImpl;

    @Autowired
    private IOwnerCarV1InnerServiceSMO ownerCarV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的完整性和业务逻辑的合法性
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException   命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证必需参数是否存在
        Assert.hasKeyAndValue(reqJson, "carId", "未包含carId");
        Assert.hasKeyAndValue(reqJson, "carNum", "未包含carNum");
        Assert.hasKeyAndValue(reqJson, "newCarNum", "未包含newCarNum");
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含communityId");

        // 从请求头获取用户ID
        String userId = context.getReqHeaders().get("user-id");

        // 查询用户与业主的绑定关系
        OwnerAppUserDto ownerAppUserDto = new OwnerAppUserDto();
        ownerAppUserDto.setUserId(userId);
        ownerAppUserDto.setCommunityId(reqJson.getString("communityId"));
        ownerAppUserDto.setMemberId(reqJson.getString("memberId"));
        List<OwnerAppUserDto> ownerAppUserDtos = ownerAppUserInnerServiceSMOImpl.queryOwnerAppUsers(ownerAppUserDto);

        // 检查是否绑定业主
        if (ListUtil.isNull(ownerAppUserDtos)) {
            throw new CmdException("未绑定业主");
        }

        // 遍历绑定关系，查找有效的业主成员ID
        String memberId = "";
        for (OwnerAppUserDto tmpOwnerAppUserDto : ownerAppUserDtos) {
            // 跳过无效的成员ID
            if ("-1".equals(tmpOwnerAppUserDto.getMemberId())) {
                continue;
            }
            memberId = tmpOwnerAppUserDto.getMemberId();
        }

        // 验证是否找到有效的业主成员ID
        if (StringUtil.isEmpty(memberId)) {
            throw new CmdException("未绑定业主");
        }

        // 根据成员ID查询业主信息
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setCommunityId(reqJson.getString("communityId"));
        ownerDto.setMemberId(memberId);
        List<OwnerDto> ownerDtos = ownerV1InnerServiceSMOImpl.queryOwners(ownerDto);

        // 验证业主信息存在且唯一
        Assert.listOnlyOne(ownerDtos, "业主不存在");

        // 将业主ID放入请求参数中，供后续使用
        reqJson.put("ownerId", ownerDtos.get(0).getOwnerId());
    }

    /**
     * 执行命令方法
     * 处理业主修改车牌号的核心业务逻辑，包括车辆验证、状态检查和数据更新
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException   命令异常
     * @throws ParseException 解析异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 构建车辆查询条件
        OwnerCarDto ownerCarDto = new OwnerCarDto();
        ownerCarDto.setCarId(reqJson.getString("carId"));
        ownerCarDto.setOwnerId(reqJson.getString("ownerId"));
        ownerCarDto.setCarNum(reqJson.getString("carNum"));

        // 查询车辆信息
        List<OwnerCarDto> ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);

        // 验证车辆是否存在
        if (ListUtil.isNull(ownerCarDtos)) {
            throw new CmdException("车辆不存在");
        }

        // 检查原车牌号是否在场内停车区域
        if (hasInParkingArea(ownerCarDtos.get(0).getCarNum(), ownerCarDtos.get(0).getAreaNum(), ownerCarDtos.get(0).getCommunityId())) {
            throw new CmdException(ownerCarDtos.get(0).getCarNum() + "车在场，请先出场");
        }
        
        // 检查新车牌号是否在场内停车区域
        if (hasInParkingArea(reqJson.getString("newCarNum"), ownerCarDtos.get(0).getAreaNum(), ownerCarDtos.get(0).getCommunityId())) {
            throw new CmdException(ownerCarDtos.get(0).getCarNum() + "车在场，请先出场");
        }

        // 构建车辆更新对象
        OwnerCarPo ownerCarPo = new OwnerCarPo();
        ownerCarPo.setMemberId(ownerCarDtos.get(0).getMemberId());
        ownerCarPo.setCarId(ownerCarDtos.get(0).getCarId());
        ownerCarPo.setCarNum(reqJson.getString("newCarNum"));
        ownerCarPo.setCommunityId(reqJson.getString("communityId"));

        // 执行车辆信息更新
        ownerCarV1InnerServiceSMOImpl.updateOwnerCar(ownerCarPo);
    }

    /**
     * 检查车辆是否在停车区域内
     * 通过调用物联网服务查询车辆在场状态
     *
     * @param carNum      车牌号
     * @param areaNum     区域编号
     * @param communityId 小区ID
     * @return boolean true-车辆在场内，false-车辆不在场内
     */
    private boolean hasInParkingArea(String carNum, String areaNum, String communityId) {
        // 构建物联网服务查询参数
        JSONObject paramIn = new JSONObject();
        paramIn.put("communityId", communityId);
        paramIn.put("page", 1);
        paramIn.put("row", 1);
        paramIn.put("paNum", areaNum);
        paramIn.put("carNum", carNum);

        // 调用物联网服务查询车辆在场状态
        ResultVo resultVo = iotInnerServiceSMOImpl.postIotData(new IotDataDto("listCarInParkingAreaBmoImpl", paramIn));

        // 如果服务调用失败，默认返回true（保守策略，避免误操作）
        if (resultVo.getCode() != ResultVo.CODE_OK) {
            return true;
        }

        // 获取查询结果数据
        JSONArray data = (JSONArray) resultVo.getData();

        // 根据查询结果判断车辆是否在场内
        if (ListUtil.isNull(data)) {
            return false; // 无数据表示车辆不在场内
        }

        return true; // 有数据表示车辆在场内
    }
}