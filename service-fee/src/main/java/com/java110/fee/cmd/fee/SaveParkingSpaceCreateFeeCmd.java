/**
 * 停车位费用创建命令类
 * 
 * 功能：处理停车位相关费用的创建和批量生成，支持按小区、停车场、单个车辆等不同范围创建费用
 * 用途：通过接收前端请求参数，生成费用批次，查询相关车辆信息，创建费用记录和费用属性
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.fee.FeeAttrDto;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.dto.payFee.PayFeeBatchDto;
import com.java110.dto.user.UserDto;
import com.java110.fee.bmo.fee.IFeeBMO;
import com.java110.fee.feeMonth.IPayFeeMonth;
import com.java110.intf.community.IParkingSpaceInnerServiceSMO;
import com.java110.intf.fee.*;
import com.java110.intf.user.IOwnerCarInnerServiceSMO;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.po.fee.FeeAttrPo;
import com.java110.po.fee.PayFeePo;
import com.java110.po.payFee.PayFeeBatchPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Java110Cmd(serviceCode = "fee.saveParkingSpaceCreateFee")
public class SaveParkingSpaceCreateFeeCmd extends Cmd {

    @Autowired
    private IFeeBMO feeBMOImpl;

    /** 默认批量添加费用记录的数量 */
    private static final int DEFAULT_ADD_FEE_COUNT = 200;

    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;

    @Autowired
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl;

    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    @Autowired
    private IPayFeeBatchV1InnerServiceSMO payFeeBatchV1InnerServiceSMOImpl;

    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;

    @Autowired
    private IPayFeeMonth payFeeMonthImpl;

    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    @Autowired
    private IFeeAttrInnerServiceSMO feeAttrInnerServiceSMOImpl;

    @Autowired
    private IRuleGeneratorPayFeeBillV1InnerServiceSMO ruleGeneratorPayFeeBillV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整，并对结束时间进行格式化处理
     * 
     * @param event 命令事件
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必要参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID");
        Assert.hasKeyAndValue(reqJson, "locationTypeCd", "未包含收费类型");
        Assert.hasKeyAndValue(reqJson, "locationObjId", "未包含收费对象");
        Assert.hasKeyAndValue(reqJson, "configId", "未包含收费项目");
        Assert.hasKeyAndValue(reqJson, "storeId", "未包含商户ID");
        Assert.hasKeyAndValue(reqJson, "startTime", "未包含费用起始时间");
        Assert.hasKeyAndValue(reqJson, "endTime", "未包含费用结束时间");

        // 格式化结束时间，如果没有时分秒则添加默认值
        String endTime = reqJson.getString("endTime");
        if (!endTime.contains(":")) {
            endTime += " 23:59:59";
            reqJson.put("endTime", endTime);
        }
    }

    /**
     * 命令执行方法
     * 
     * 主要业务逻辑：查询费用配置、生成批次、根据收费范围查询车辆信息、处理停车位费用
     * 
     * @param event 命令事件
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        List<OwnerCarDto> ownerCarDtos = null;
        
        // 查询费用配置信息
        FeeConfigDto feeConfigDto = new FeeConfigDto();
        feeConfigDto.setCommunityId(reqJson.getString("communityId"));
        feeConfigDto.setConfigId(reqJson.getString("configId"));
        List<FeeConfigDto> feeConfigDtos = feeConfigInnerServiceSMOImpl.queryFeeConfigs(feeConfigDto);
        
        // 验证费用配置是否存在且唯一
        Assert.listOnlyOne(feeConfigDtos, "当前费用项ID不存在或存在多条" + reqJson.getString("configId"));
        
        // 将费用配置信息添加到请求参数中
        reqJson.put("feeTypeCd", feeConfigDtos.get(0).getFeeTypeCd());
        reqJson.put("feeFlag", feeConfigDtos.get(0).getFeeFlag());
        reqJson.put("configEndTime", feeConfigDtos.get(0).getEndTime());

        // 生成费用批次
        generatorBatch(reqJson);
        
        // 根据收费范围类型查询车辆信息
        OwnerCarDto ownerCarDto = new OwnerCarDto();

        if ("1000".equals(reqJson.getString("locationTypeCd"))) {
            // 小区范围收费：清空locationObjId，查询整个小区的车辆
            reqJson.put("locationObjId", "");
            ownerCarDtos = getOwnerCarByParkingArea(reqJson);
        } else if ("2000".equals(reqJson.getString("locationTypeCd"))) {
            // 单个车辆收费：查询指定车辆信息
            ownerCarDto.setCarTypeCd("1001"); // 1001 业主车辆   1002 成员车辆
            ownerCarDto.setCommunityId(reqJson.getString("communityId"));
            ownerCarDto.setCarId(reqJson.getString("locationObjId"));
            ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);
        } else if ("3000".equals(reqJson.getString("locationTypeCd"))) {
            // 停车场范围收费：查询停车场内的所有车辆
            ownerCarDtos = getOwnerCarByParkingArea(reqJson);
        } else {
            throw new IllegalArgumentException("收费范围错误");
        }

        // 验证是否查询到需要付费的车位
        if (ListUtil.isNull(ownerCarDtos)) {
            throw new IllegalArgumentException("未查到需要付费的车位");
        }
        
        // 处理停车位费用
        dealParkingSpaceFee(ownerCarDtos, cmdDataFlowContext, reqJson, event);
    }

    /**
     * 根据停车场区域查询车辆信息
     * 
     * 通过停车场ID查询停车位，再根据停车位ID查询关联的车辆信息
     * 
     * @param reqJson 请求参数JSON对象
     * @return 车辆信息列表
     */
    private List<OwnerCarDto> getOwnerCarByParkingArea(JSONObject reqJson) {
        List<OwnerCarDto> ownerCarDtos = new ArrayList<>();
        ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
        parkingSpaceDto.setCommunityId(reqJson.getString("communityId"));
        
        // 处理车位状态参数
        List<String> states = new ArrayList<>();
        JSONArray carStates = reqJson.getJSONArray("carState");
        if (ListUtil.isNull(carStates)) {
            throw new IllegalArgumentException("未选择车位状态");
        }

        // 将车位状态转换为列表
        for (int carStateIndex = 0; carStateIndex < carStates.size(); carStateIndex++) {
            states.add(carStates.getString(carStateIndex));
        }
        parkingSpaceDto.setStates(states.toArray(new String[states.size()]));
        parkingSpaceDto.setPaId(reqJson.getString("locationObjId"));
        
        // 查询停车位信息
        List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);

        if (parkingSpaceDtos == null || parkingSpaceDtos.size() < 1) {
            return null;
        }
        
        // 分批处理停车位ID，避免查询数据量过大
        List<String> psIds = new ArrayList<>();
        for (int parkingSpaceIndex = 0; parkingSpaceIndex < parkingSpaceDtos.size(); parkingSpaceIndex++) {
            psIds.add(parkingSpaceDtos.get(parkingSpaceIndex).getPsId());

            // 每处理DEFAULT_ADD_FEE_COUNT个停车位查询一次车辆信息
            if (parkingSpaceIndex % DEFAULT_ADD_FEE_COUNT == 0 && parkingSpaceIndex != 0) {
                queryOwnerCar(reqJson, psIds, ownerCarDtos);
                psIds = new ArrayList<>();
            }
        }
        
        // 处理剩余的停车位ID
        if (psIds.size() > 0) {
            queryOwnerCar(reqJson, psIds, ownerCarDtos);
        }

        return ownerCarDtos;
    }

    /**
     * 生成费用批次号
     * 
     * 创建费用批次记录，用于标识本次批量费用创建操作
     * 
     * @param reqJson 请求参数JSON对象
     */
    private void generatorBatch(JSONObject reqJson) {
        PayFeeBatchPo payFeeBatchPo = new PayFeeBatchPo();
        // 生成批次ID
        payFeeBatchPo.setBatchId(GenerateCodeFactory.getGeneratorId("12"));
        payFeeBatchPo.setCommunityId(reqJson.getString("communityId"));
        payFeeBatchPo.setCreateUserId(reqJson.getString("userId"));
        
        // 查询用户信息
        UserDto userDto = new UserDto();
        userDto.setUserId(reqJson.getString("userId"));
        List<UserDto> userDtos = userInnerServiceSMOImpl.getUsers(userDto);

        Assert.listOnlyOne(userDtos, "用户不存在");
        payFeeBatchPo.setCreateUserName(userDtos.get(0).getUserName());
        payFeeBatchPo.setState(PayFeeBatchDto.STATE_NORMAL);
        payFeeBatchPo.setMsg("正常");
        
        // 保存批次信息
        int flag = payFeeBatchV1InnerServiceSMOImpl.savePayFeeBatch(payFeeBatchPo);

        if (flag < 1) {
            throw new IllegalArgumentException("生成批次失败");
        }

        // 将批次ID添加到请求参数中
        reqJson.put("batchId", payFeeBatchPo.getBatchId());
    }

    /**
     * 根据停车位ID查询车辆信息
     * 
     * 通过停车位ID列表查询对应的车辆信息
     * 
     * @param reqJson 请求参数JSON对象
     * @param psIds 停车位ID列表
     * @param ownerCarDtos 车辆信息列表（输出参数）
     */
    private void queryOwnerCar(JSONObject reqJson, List<String> psIds, List<OwnerCarDto> ownerCarDtos) {
        OwnerCarDto ownerCarDto = new OwnerCarDto();
        ownerCarDto.setCommunityId(reqJson.getString("communityId"));
        ownerCarDto.setPsIds(psIds.toArray(new String[psIds.size()]));
        List<OwnerCarDto> townerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);
        ownerCarDtos.addAll(townerCarDtos);
    }

    /**
     * 处理停车位费用
     * 
     * 批量创建费用记录和费用属性，支持分批处理大量数据
     * 
     * @param ownerCarDtos 车辆信息列表
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @param event 命令事件
     */
    private void dealParkingSpaceFee(List<OwnerCarDto> ownerCarDtos, ICmdDataFlowContext context, JSONObject reqJson, CmdEvent event) {

        List<PayFeePo> feePos = new ArrayList<>();
        List<FeeAttrPo> feeAttrsPos = new ArrayList<>();
        JSONObject paramInObj = null;
        ResponseEntity<String> responseEntity = null;
        int failParkingSpaces = 0; // 失败的车位数量
        int curFailRoomCount = 0; // 当前批次失败的数量
        // 添加单元信息
        int saveFlag = 0;
        List<ParkingSpaceDto> parkingSpaceDtos = null;
        ParkingSpaceDto parkingSpaceDto = null;
        String carName = "";
        
        // 遍历所有车辆，创建费用记录和属性
        for (int ownerCarIndex = 0; ownerCarIndex < ownerCarDtos.size(); ownerCarIndex++) {
            curFailRoomCount++;
            
            // 创建费用记录
            feePos.add(BeanConvertUtil.covertBean(feeBMOImpl.addFee(ownerCarDtos.get(ownerCarIndex), reqJson, context), PayFeePo.class));
            
            // 添加费用属性：一次性费用截止时间
            feeAttrsPos.add(feeBMOImpl.addFeeAttr(reqJson, context, FeeAttrDto.SPEC_CD_ONCE_FEE_DEADLINE_TIME,
                    reqJson.getString("endTime")));
            
            // 如果车辆有业主信息，添加业主相关属性
            if (!StringUtil.isEmpty(ownerCarDtos.get(ownerCarIndex).getOwnerId())) {
                feeAttrsPos.add(feeBMOImpl.addFeeAttr(reqJson, context, FeeAttrDto.SPEC_CD_OWNER_ID, ownerCarDtos.get(ownerCarIndex).getOwnerId()));
                feeAttrsPos.add(feeBMOImpl.addFeeAttr(reqJson, context, FeeAttrDto.SPEC_CD_OWNER_LINK, ownerCarDtos.get(ownerCarIndex).getLink()));
                feeAttrsPos.add(feeBMOImpl.addFeeAttr(reqJson, context, FeeAttrDto.SPEC_CD_OWNER_NAME, ownerCarDtos.get(ownerCarIndex).getOwnerName()));
            }

            // 查询停车位信息，构建车辆显示名称
            parkingSpaceDto = new ParkingSpaceDto();
            parkingSpaceDto.setPsId(ownerCarDtos.get(ownerCarIndex).getPsId());
            parkingSpaceDto.setCommunityId(ownerCarDtos.get(0).getCommunityId());
            parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);

            if (!ListUtil.isNull(parkingSpaceDtos)) {
                // 构建格式：区域编号+车位编号(车牌号)
                carName = parkingSpaceDtos.get(0).getAreaNum() + parkingSpaceDtos.get(0).getNum() + "(" + ownerCarDtos.get(ownerCarIndex).getCarNum() + ")";
            } else {
                carName = ownerCarDtos.get(ownerCarIndex).getCarNum();
            }

            // 添加付费对象名称属性
            feeAttrsPos.add(feeBMOImpl.addFeeAttr(reqJson, context, FeeAttrDto.SPEC_CD_PAY_OBJECT_NAME,
                    carName));

            // 每处理DEFAULT_ADD_FEE_COUNT条记录保存一次
            if (ownerCarIndex % DEFAULT_ADD_FEE_COUNT == 0 && ownerCarIndex != 0) {
                saveFlag = saveFeeAndAttrs(feePos, feeAttrsPos);
                feePos = new ArrayList<>();
                feeAttrsPos = new ArrayList<>();
                if (saveFlag < 1) {
                    failParkingSpaces += curFailRoomCount;
                } else {
                    curFailRoomCount = 0;
                }
            }
        }
        
        // 处理剩余的费用记录
        if (feePos != null && feePos.size() > 0) {
            saveFlag = saveFeeAndAttrs(feePos, feeAttrsPos);
            if (saveFlag < 1) {
                failParkingSpaces += curFailRoomCount;
            }
        }

        // 构建响应结果
        JSONObject paramOut = new JSONObject();
        paramOut.put("totalCar", ownerCarDtos.size());
        paramOut.put("successCar", ownerCarDtos.size() - failParkingSpaces);
        paramOut.put("errorCar", failParkingSpaces);

        responseEntity = new ResponseEntity<>(paramOut.toJSONString(), HttpStatus.OK);

        context.setResponseEntity(responseEntity);
    }

    /**
     * 保存费用记录和费用属性
     * 
     * 批量保存费用信息和费用属性，并触发月费用数据生成
     * 
     * @param feePos 费用记录列表
     * @param feeAttrsPos 费用属性列表
     * @return 保存结果，大于0表示成功
     */
    private int saveFeeAndAttrs(List<PayFeePo> feePos, List<FeeAttrPo> feeAttrsPos) {

        // 检查是否需要生成账单数据
        String billModal = ruleGeneratorPayFeeBillV1InnerServiceSMOImpl.needGeneratorBillData(feePos);
        if ("Y".equals(billModal)) {
            return 1;
        }

        // 保存费用记录
        int flag = feeInnerServiceSMOImpl.saveFee(feePos);
        if (flag < 1) {
            return flag;
        }

        // 保存费用属性
        flag = feeAttrInnerServiceSMOImpl.saveFeeAttrs(feeAttrsPos);

        // 异步生成月费用数据和欠费数据
        List<String> feeIds = new ArrayList<>();
        for (PayFeePo feePo : feePos) {
            feeIds.add(feePo.getFeeId());
        }
        payFeeMonthImpl.doGeneratorFeeMonths(feeIds, feePos.get(0).getCommunityId());

        return flag;
    }

}