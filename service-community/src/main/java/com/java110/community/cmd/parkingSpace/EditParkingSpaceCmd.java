/**
 * 停车位编辑命令类
 * 
 * 该类负责处理停车位信息的编辑操作，包括参数验证和业务逻辑执行。
 * 通过注解@Java110Cmd声明服务代码为"parkingSpace.editParkingSpace"。
 * 继承自Cmd基类，实现validate和doCmd方法。
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.community.cmd.parkingSpace;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.intf.community.IParkingSpaceInnerServiceSMO;
import com.java110.intf.community.IParkingSpaceV1InnerServiceSMO;
import com.java110.po.parking.ParkingSpacePo;
import com.java110.utils.constant.BusinessTypeConstant;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

@Java110Cmd(serviceCode = "parkingSpace.editParkingSpace")
public class EditParkingSpaceCmd extends Cmd {

    /**
     * 停车位内部服务接口
     */
    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;

    /**
     * 停车位V1版本内部服务接口
     */
    @Autowired
    private IParkingSpaceV1InnerServiceSMO parkingSpaceV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证编辑停车位请求的必需参数，包括小区ID、停车位ID、停车场ID、车位编号和面积等。
     * 同时验证停车位ID的有效性和车位类型修改的合法性。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置上下文数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败或业务逻辑异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必需参数是否存在
        Assert.jsonObjectHaveKey(reqJson, "communityId", "未包含小区ID");
        Assert.jsonObjectHaveKey(reqJson, "psId", "未包含停车位ID");
        Assert.jsonObjectHaveKey(reqJson, "paId", "未包含停车场信息");
        Assert.jsonObjectHaveKey(reqJson, "num", "请求报文中未包含num");
        Assert.jsonObjectHaveKey(reqJson, "area", "请求报文中未包含area");

        // 验证停车位ID不能为空
        Assert.hasLength(reqJson.getString("psId"), "停车位ID不能为空");

        // 验证停车位ID不能以"-"开头，确保是已有ID
        if (reqJson.getString("psId").startsWith("-")) {
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "停车位ID必须为已有ID");
        }

        // 查询停车位信息，验证停车位是否存在
        ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
        parkingSpaceDto.setCommunityId(reqJson.getString("communityId"));
        parkingSpaceDto.setPsId(reqJson.getString("psId"));
        List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);

        // 验证查询结果，必须存在且唯一
        if (parkingSpaceDtos == null || parkingSpaceDtos.size() != 1) {
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "未查询到停车位信息" + JSONObject.toJSONString(parkingSpaceDto));
        }

        // 如果不修改车位类型，直接返回
        if (!reqJson.containsKey("parkingType")) {
            return;
        }

        // 如果车位类型没有变化，直接返回
        if (parkingSpaceDtos.get(0).getParkingType().equals(reqJson.getString("parkingType"))) {
            return;
        }

        // 验证子母车位的修改限制：子母车位不能修改为其他类型，其他类型也不能修改为子母车位
        if (ParkingSpaceDto.TYPE_CD_SON_MOTHER.equals(reqJson.getString("parkingType"))
                || ParkingSpaceDto.TYPE_CD_SON_MOTHER.equals(parkingSpaceDtos.get(0).getParkingType())
        ) {
            throw  new CmdException("子母车位不能修改为其他车位，其他车位也不能修改为子母车位！");
        }
    }

    /**
     * 执行停车位编辑命令
     * 
     * 根据请求参数更新停车位信息，包括车位编号、面积等属性。
     * 先查询现有停车位信息，然后更新到数据库。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置上下文数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当业务执行失败时抛出
     * @throws ParseException 当数据解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 查询要编辑的停车位信息
        ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
        parkingSpaceDto.setCommunityId(reqJson.getString("communityId"));
        parkingSpaceDto.setPsId(reqJson.getString("psId"));
        List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);

        // 验证停车位信息是否存在且唯一
        if (parkingSpaceDtos == null || parkingSpaceDtos.size() != 1) {
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "未查询到停车位信息" + JSONObject.toJSONString(parkingSpaceDto));
        }

        // 获取现有的停车位信息
        parkingSpaceDto = parkingSpaceDtos.get(0);

        // 创建业务对象并设置更新数据
        JSONObject businessParkingSpace = new JSONObject();
        businessParkingSpace.putAll(reqJson);
        // 保持原有状态不变
        businessParkingSpace.put("state", parkingSpaceDto.getState());
        
        // 将JSON对象转换为停车位PO对象
        ParkingSpacePo parkingSpacePo = BeanConvertUtil.covertBean(businessParkingSpace, ParkingSpacePo.class);
        
        // 调用V1版本服务更新停车位信息
        // parkingSpaceInnerServiceSMOImpl.updateParkingSpace(parkingSpacePo);
        int flag = parkingSpaceV1InnerServiceSMOImpl.updateParkingSpace(parkingSpacePo);

        // 检查更新结果，失败时抛出异常
        if (flag < 1) {
            throw new CmdException("修改车位失败");
        }
    }
}