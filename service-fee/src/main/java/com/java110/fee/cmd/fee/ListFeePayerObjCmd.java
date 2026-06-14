package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.contract.ContractDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.fee.PayFeeDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.owner.OwnerRoomRelDto;
import com.java110.dto.room.RoomDto;
import com.java110.intf.community.IRoomV1InnerServiceSMO;
import com.java110.intf.fee.IPayFeeV1InnerServiceSMO;
import com.java110.intf.store.IContractInnerServiceSMO;
import com.java110.intf.user.IBuildingOwnerV1InnerServiceSMO;
import com.java110.intf.user.IOwnerCarV1InnerServiceSMO;
import com.java110.intf.user.IOwnerRoomRelV1InnerServiceSMO;
import com.java110.intf.user.IOwnerV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

/**
 * 查询费用作用对象信息命令类
 * 
 * 该类用于处理查询费用作用对象信息的请求，支持查询房屋、车辆、合同等不同类型的费用对象
 * 根据传入的对象类型和对象ID，查询对应的详细信息并返回给调用方
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023
 */
@Java110CmdDoc(title = "查询费用作用对象",
        description = "供三方系统查询的费用对象 比如 房屋 车辆 合同等",
        httpMethod = "get",
        url = "http://{ip}:{port}/app/fee.listFeePayerObj",
        resource = "feeDoc",
        author = "吴学文",
        serviceCode = "fee.listFeePayerObj",
        seq = 1
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "payerObjId", length = 30, remark = "对象ID"),
        @Java110ParamDoc(name = "payerObjType", length = 12, remark = "对象类型"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "data", type = "Object", length = -1, defaultValue = "成功", remark = "数据"),
                @Java110ParamDoc(parentNodeName = "data", name = "payerObjId", length = 30, remark = "费用对象ID"),
                @Java110ParamDoc(parentNodeName = "data", name = "payerObjName", length = 64, remark = "费用对象名称"),
                @Java110ParamDoc(parentNodeName = "data", name = "paNum", length = 11, remark = "停车场"),
                @Java110ParamDoc(parentNodeName = "data", name = "psNum", length = 30, remark = "车位"),
                @Java110ParamDoc(parentNodeName = "data", name = "ownerId", length = 30, remark = "业主Id"),
                @Java110ParamDoc(parentNodeName = "data", name = "ownerName", length = 64, remark = "业主名称"),
                @Java110ParamDoc(parentNodeName = "data", name = "tel", length = 11, remark = "手机号"),
        }
)

@Java110ExampleDoc(
        reqBody = "http://ip:port/app/fee.listFeePayerObj?payerObjId=1001&payerObjType=3333&communityId=2022121921870161",
        resBody = "{\n" +
                "\t\"data\": {\n" +
                "\t\t\"payerObjId\": \"张三\",\n" +
                "\t\t\"payerObjName\": \"\",\n" +
                "\t\t\"paNum\": \"18909718888\",\n" +
                "\t\t\"psNum\": \"772023012589770046\",\n" +
                "\t\t\"ownerId\": \"王王\",\n" +
                "\t\t\"ownerName\": \"1001\",\n" +
                "\t\t\"tel\": \"\",\n" +
                "\t},\n" +
                "\t\"page\": 0,\n" +
                "\t\"records\": 1,\n" +
                "\t\"rows\": 0,\n" +
                "\t\"total\": 1\n" +
                "}"
)
@Java110Cmd(serviceCode = "fee.listFeePayerObj")
public class ListFeePayerObjCmd extends Cmd {

    /** 费用服务接口 */
    @Autowired
    private IPayFeeV1InnerServiceSMO payFeeV1InnerServiceSMOImpl;

    /** 业主服务接口 */
    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;

    /** 房屋服务接口 */
    @Autowired
    private IRoomV1InnerServiceSMO roomV1InnerServiceSMOImpl;

    /** 业主房屋关系服务接口 */
    @Autowired
    private IOwnerRoomRelV1InnerServiceSMO ownerRoomRelV1InnerServiceSMOImpl;

    /** 业主车辆服务接口 */
    @Autowired
    private IOwnerCarV1InnerServiceSMO ownerCarV1InnerServiceSMOImpl;

    /** 合同服务接口 */
    @Autowired
    private IContractInnerServiceSMO contractInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整有效，包括小区ID、费用对象ID和费用对象类型
     * 如果请求中包含费用ID，会自动查询对应的费用对象信息
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证小区ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID");

        // 如果包含feeId参数，则根据feeId查询并设置payerObjId和payerObjType
        ifContainFeeId(reqJson);
        
        // 验证费用对象ID和类型参数是否存在
        Assert.hasKeyAndValue(reqJson, "payerObjId", "未包含payerObjId");
        Assert.hasKeyAndValue(reqJson, "payerObjType", "未包含payerObjType");
    }

    /**
     * 命令执行方法
     * 
     * 根据费用对象类型调用不同的处理方法，查询对应的费用对象详细信息
     * 支持房屋、车辆、合同三种类型的费用对象查询
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        JSONObject data = new JSONObject();
        String payerObjType = reqJson.getString("payerObjType");
        String payerObjId = reqJson.getString("payerObjId");
        
        // 根据费用对象类型分别处理
        ifRoom(payerObjId, payerObjType, reqJson, data);    // 处理房屋类型
        ifCar(payerObjId, payerObjType, reqJson, data);     // 处理车辆类型
        ifContract(payerObjId, payerObjType, reqJson, data); // 处理合同类型

        // 设置响应结果
        context.setResponseEntity(ResultVo.createResponseEntity(data));
    }

    /**
     * 处理房屋类型的费用对象
     * 
     * 查询房屋详细信息，包括房屋基本信息、业主信息等
     * 如果房屋存在关联的业主，还会查询业主的详细信息
     * 
     * @param payerObjId 费用对象ID
     * @param payerObjType 费用对象类型
     * @param reqJson 请求参数JSON对象
     * @param data 返回数据JSON对象
     */
    private void ifRoom(String payerObjId, String payerObjType, JSONObject reqJson, JSONObject data) {
        // 如果不是房屋类型，直接返回
        if (!FeeDto.PAYER_OBJ_TYPE_ROOM.equals(payerObjType)) {
            return;
        }
        
        // 查询房屋基本信息
        RoomDto roomDto = new RoomDto();
        roomDto.setRoomId(payerObjId);
        roomDto.setCommunityId(reqJson.getString("communityId"));
        List<RoomDto> roomDtos = roomV1InnerServiceSMOImpl.queryRooms(roomDto);

        // 验证房屋是否存在且唯一
        Assert.listOnlyOne(roomDtos, "房屋不存在");

        // 将房屋信息添加到返回数据中
        data.putAll(BeanConvertUtil.beanCovertJson(roomDtos.get(0)));
        // 构建房屋名称：楼层-单元-房间号
        data.put("payerObjName", roomDtos.get(0).getFloorNum() + "-" + roomDtos.get(0).getUnitNum() + "-" + roomDtos.get(0).getRoomNum());

        // 查询房屋与业主的关联关系
        OwnerRoomRelDto ownerRoomRelDto = new OwnerRoomRelDto();
        ownerRoomRelDto.setRoomId(roomDtos.get(0).getRoomId());
        List<OwnerRoomRelDto> ownerRoomRelDtos = ownerRoomRelV1InnerServiceSMOImpl.queryOwnerRoomRels(ownerRoomRelDto);
        
        // 如果没有关联的业主，直接返回
        if (ownerRoomRelDtos == null || ownerRoomRelDtos.size() < 1) {
            return;
        }

        // 查询业主详细信息
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setMemberId(ownerRoomRelDtos.get(0).getOwnerId());
        ownerDto.setCommunityId(reqJson.getString("communityId"));
        List<OwnerDto> ownerDtos = ownerV1InnerServiceSMOImpl.queryOwners(ownerDto);

        // 如果业主存在，将业主信息添加到返回数据中
        if (ownerDtos == null || ownerDtos.size() < 1) {
            return;
        }
        data.putAll(BeanConvertUtil.beanCovertJson(ownerDtos.get(0)));
    }

    /**
     * 处理车辆类型的费用对象
     * 
     * 查询车辆详细信息，包括车辆基本信息、车主信息等
     * 如果车辆存在关联的业主，还会查询业主的详细信息
     * 
     * @param payerObjId 费用对象ID
     * @param payerObjType 费用对象类型
     * @param reqJson 请求参数JSON对象
     * @param data 返回数据JSON对象
     */
    private void ifCar(String payerObjId, String payerObjType, JSONObject reqJson, JSONObject data) {
        // 如果不是车辆类型，直接返回
        if (!FeeDto.PAYER_OBJ_TYPE_CAR.equals(payerObjType)) {
            return;
        }

        // 查询车辆基本信息
        OwnerCarDto ownerCarDto = new OwnerCarDto();
        ownerCarDto.setMemberId(payerObjId);
        ownerCarDto.setCommunityId(reqJson.getString("communityId"));
        List<OwnerCarDto> ownerCarDtos = ownerCarV1InnerServiceSMOImpl.queryOwnerCars(ownerCarDto);

        // 验证车辆是否存在且唯一
        Assert.listOnlyOne(ownerCarDtos, "车辆不存在");

        // 将车辆信息添加到返回数据中
        data.putAll(BeanConvertUtil.beanCovertJson(ownerCarDtos.get(0)));
        // 使用车牌号作为费用对象名称
        data.put("payerObjName", ownerCarDtos.get(0).getCarNum());

        // 查询车主详细信息
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setMemberId(ownerCarDtos.get(0).getOwnerId());
        ownerDto.setCommunityId(reqJson.getString("communityId"));
        List<OwnerDto> ownerDtos = ownerV1InnerServiceSMOImpl.queryOwners(ownerDto);

        // 如果车主存在，将车主信息添加到返回数据中
        if (ownerDtos == null || ownerDtos.size() < 1) {
            return;
        }
        data.putAll(BeanConvertUtil.beanCovertJson(ownerDtos.get(0)));
    }

    /**
     * 处理合同类型的费用对象
     * 
     * 查询合同详细信息，包括合同基本信息、关联的业主信息等
     * 通过合同中的联系方式查询对应的业主信息
     * 
     * @param payerObjId 费用对象ID
     * @param payerObjType 费用对象类型
     * @param reqJson 请求参数JSON对象
     * @param data 返回数据JSON对象
     */
    private void ifContract(String payerObjId, String payerObjType, JSONObject reqJson, JSONObject data) {
        // 如果不是合同类型，直接返回
        if (!FeeDto.PAYER_OBJ_TYPE_CONTRACT.equals(payerObjType)) {
            return;
        }
        
        // 查询合同基本信息
        ContractDto contractDto = new ContractDto();
        contractDto.setContractId(payerObjId);
        List<ContractDto> contractDtos = contractInnerServiceSMOImpl.queryContracts(contractDto);
        
        // 验证合同是否存在且唯一
        Assert.listOnlyOne(contractDtos, "合同不存在");

        // 将合同信息添加到返回数据中
        data.putAll(BeanConvertUtil.beanCovertJson(contractDtos.get(0)));
        // 使用合同名称作为费用对象名称
        data.put("payerObjName", contractDtos.get(0).getContractName());

        // 通过合同中的联系方式查询业主信息
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setLink(contractDtos.get(0).getbLink());
        ownerDto.setCommunityId(reqJson.getString("communityId"));
        List<OwnerDto> ownerDtos = ownerV1InnerServiceSMOImpl.queryOwners(ownerDto);

        // 如果业主存在，将业主信息添加到返回数据中
        if (ownerDtos == null || ownerDtos.size() < 1) {
            return;
        }
        data.putAll(BeanConvertUtil.beanCovertJson(ownerDtos.get(0)));
    }

    /**
     * 处理费用ID参数
     * 
     * 如果请求中包含费用ID，则根据费用ID查询对应的费用对象信息
     * 自动设置payerObjId和payerObjType参数
     * 
     * @param reqJson 请求参数JSON对象
     */
    private void ifContainFeeId(JSONObject reqJson) {
        // 检查是否包含feeId参数
        if (!reqJson.containsKey("feeId")) {
            return;
        }

        String feeId = reqJson.getString("feeId");

        // 检查feeId是否为空
        if (StringUtil.isEmpty(feeId)) {
            return;
        }

        // 根据feeId查询费用信息
        PayFeeDto feeDto = new PayFeeDto();
        feeDto.setFeeId(feeId);
        feeDto.setCommunityId(reqJson.getString("communityId"));
        List<PayFeeDto> payFeeDtos = payFeeV1InnerServiceSMOImpl.queryPayFees(feeDto);

        // 验证费用是否存在且唯一
        Assert.listOnlyOne(payFeeDtos, "费用不存在");

        // 设置费用对象ID和类型参数
        reqJson.put("payerObjId", payFeeDtos.get(0).getPayerObjId());
        reqJson.put("payerObjType", payFeeDtos.get(0).getPayerObjType());
    }
}