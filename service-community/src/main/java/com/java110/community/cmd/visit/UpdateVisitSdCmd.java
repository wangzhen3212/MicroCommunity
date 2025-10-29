package com.java110.community.cmd.visit;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.CommunitySettingFactory;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.intf.community.IParkingSpaceInnerServiceSMO;
import com.java110.intf.community.IVisitV1InnerServiceSMO;
import com.java110.intf.user.IOwnerCarAttrInnerServiceSMO;
import com.java110.intf.user.IOwnerCarInnerServiceSMO;
import com.java110.intf.user.IOwnerCarV1InnerServiceSMO;
import com.java110.po.car.OwnerCarPo;
import com.java110.po.owner.VisitPo;
import com.java110.po.owner.OwnerCarAttrPo;
import com.java110.po.parking.ParkingSpacePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * 更新访客信息命令类
 * 
 * 该类负责处理访客信息的更新操作，包括访客基本信息更新、车辆预约审核、
 * 车位分配等业务逻辑。主要功能包括：
 * 1. 验证访客信息必填字段
 * 2. 处理访客审核流程
 * 3. 分配临时车位给访客车辆
 * 4. 更新访客记录状态
 * 
 * @author Java110
 * @version 1.0
 */
//@Java110Cmd(serviceCode = "visit.updateVisit")
public class UpdateVisitSdCmd extends Cmd {

    /** 停车位内部服务接口 */
    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;

    /** 业主车辆V1内部服务接口 */
    @Autowired
    private IOwnerCarV1InnerServiceSMO ownerCarV1InnerServiceSMOImpl;

    /** 业主车辆属性内部服务接口 */
    @Autowired
    private IOwnerCarAttrInnerServiceSMO ownerCarAttrInnerServiceSMOImpl;

    /** 业主车辆内部服务接口 */
    @Autowired
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl;

    /** 访客V1内部服务接口 */
    @Autowired
    private IVisitV1InnerServiceSMO visitV1InnerServiceSMOImpl;

    /** 车辆免费时长配置键 */
    public static final String CAR_FREE_TIME = "CAR_FREE_TIME";

    /** 归属车场区域ID配置键 */
    public static final String ASCRIPTION_CAR_AREA_ID = "ASCRIPTION_CAR_AREA_ID";

    /**
     * 验证请求参数
     * 
     * 检查访客更新操作必需的参数是否完整，包括访客ID、姓名、性别、联系方式和拜访时间
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当必需参数缺失时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Assert.hasKeyAndValue(reqJson, "vId", "访客记录ID不能为空");
        Assert.hasKeyAndValue(reqJson, "vName", "必填，请填写访客姓名");
        Assert.hasKeyAndValue(reqJson, "visitGender", "必填，请填写访客姓名");
        Assert.hasKeyAndValue(reqJson, "phoneNumber", "必填，请填写访客联系方式");
        Assert.hasKeyAndValue(reqJson, "visitTime", "必填，请填写访客拜访时间");
    }

    /**
     * 执行访客更新命令
     * 
     * 处理访客信息的更新操作，包括：
     * 1. 检查车辆是否已存在
     * 2. 审核通过时分配临时车位
     * 3. 更新访客记录状态
     * 4. 创建临时车辆信息
     * 5. 更新车位状态
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当业务处理失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String userId = context.getReqHeaders().get("user-id");
        // 标记是否有空闲车位
        boolean freeSpace = false;
        // 标记车辆是否已存在
        boolean existCar = false;
        
        // 校验车牌号是否已存在
        OwnerCarDto ownerCarDto1 = new OwnerCarDto();
        ownerCarDto1.setCommunityId(reqJson.getString("communityId"));
        ownerCarDto1.setCarNum(reqJson.getString("carNum"));
        int count = ownerCarInnerServiceSMOImpl.queryOwnerCarsCount(ownerCarDto1);
        if (count > 0) {
            existCar = true;
            reqJson.put("psId", "-1"); // 车辆已存在，设置车位ID为-1
        }
        
        // 如果是审核操作且审核通过且车辆不存在的情况
        if ("1".equals(reqJson.getString("flag"))
                && "1".equals(reqJson.getString("state"))
                && !existCar) {
            // 获取预约车免费时长的配置值
            String freeTime = CommunitySettingFactory.getValue(reqJson.getString("communityId"), CAR_FREE_TIME);
            if (StringUtil.isEmpty(freeTime)) {
                freeTime = "120"; // 默认120分钟
            }
            
            // 计算免费结束时间
            Date time = DateUtil.getDateFromStringA(reqJson.getString("visitTime"));
            Calendar newTime = Calendar.getInstance();
            newTime.setTime(time);
            newTime.add(Calendar.MINUTE, Integer.parseInt(freeTime)); // 日期加上免费分钟数
            Date newDate = newTime.getTime();
            String finishFreeTime = DateUtil.getFormatTimeString(newDate, DateUtil.DATE_FORMATE_STRING_A);
            reqJson.put("freeTime", finishFreeTime);
            
            // 获取小区配置的停车场ID
            String parkingAreaId = CommunitySettingFactory.getValue(reqJson.getString("communityId"), ASCRIPTION_CAR_AREA_ID);
            if (StringUtil.isEmpty(parkingAreaId)) { 
                // 如果没有配置停车场ID，就分配该小区下的空闲车位
                ParkingSpaceDto parkingSpace = new ParkingSpaceDto();
                parkingSpace.setCommunityId(reqJson.getString("communityId"));
                parkingSpace.setState("F"); // 车位状态：空闲
                parkingSpace.setParkingType("1"); // 普通车位
                List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpace);
                if (parkingSpaceDtos == null || parkingSpaceDtos.size() < 1) {
                    freeSpace = true; // 没有空闲车位
                } else {
                    // 随机选择一个空闲车位
                    Random random = new Random();
                    int i = random.nextInt(parkingSpaceDtos.size());
                    String psId = parkingSpaceDtos.get(i).getPsId();
                    reqJson.put("psId", psId);
                }
            } else {
                // 在指定停车场中分配空闲车位
                ParkingSpaceDto parkingSpace = new ParkingSpaceDto();
                parkingSpace.setCommunityId(reqJson.getString("communityId"));
                parkingSpace.setPaId(parkingAreaId); // 指定停车场ID
                parkingSpace.setState("F"); // 车位状态：空闲
                parkingSpace.setParkingType("1"); // 普通车位
                List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpace);
                if (parkingSpaceDtos == null || parkingSpaceDtos.size() < 1) {
                    freeSpace = true; // 没有空闲车位
                } else {
                    // 随机选择一个空闲车位
                    Random random = new Random();
                    int i = random.nextInt(parkingSpaceDtos.size());
                    String psId = parkingSpaceDtos.get(i).getPsId();
                    reqJson.put("psId", psId);
                }
            }
        }
        
        // 设置审核结果提示信息
        String result = "";
        if ("1".equals(reqJson.getString("state"))) {
            result = "审核通过！";
        } else if ("2".equals(reqJson.getString("state"))) {
            result = "审核不通过！";
        }
        if (existCar) {
            result = "访客信息审核成功,车辆已经存在预约，请您在预约到期后，再次进行车辆预约，谢谢！";
        }
        if (freeSpace) {
            result = "访客信息审核成功，当前停车场已无空闲车位，登记车辆将暂时不能进入停车场，请您合理安排出行。";
        }
        reqJson.put("stateRemark", result);
        
        // 更新访客信息
        updateVisit(reqJson);

        // 根据不同的业务情况返回相应的响应
        if (existCar) {
            ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_BUSINESS_VERIFICATION, "访客信息审核成功,车辆已经存在预约，请您在预约到期后，再次进行车辆预约，谢谢！");
            context.setResponseEntity(responseEntity);
            return;
        }
        if (freeSpace) {
            ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_BUSINESS_VERIFICATION, "访客信息审核成功，当前停车场已无空闲车位，登记车辆将暂时不能进入停车场，请您合理安排出行。");
            context.setResponseEntity(responseEntity);
            return;
        }
        
        // 审核通过且有车位时，创建临时车辆信息并更新车位状态
        if (reqJson.containsKey("state") && "1".equals(reqJson.getString("state"))
                && "1".equals(reqJson.getString("flag"))
                && !existCar) {
            // 查询停车位信息
            ParkingSpaceDto parkingSpace = new ParkingSpaceDto();
            parkingSpace.setPsId(reqJson.getString("psId"));
            List<ParkingSpaceDto> parkingSpaces = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpace);
            Assert.listOnlyOne(parkingSpaces, "查询停车位错误！");
            
            // 创建临时车辆信息
            OwnerCarPo ownerCarPo = new OwnerCarPo();
            ownerCarPo.setCarId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_carId));
            ownerCarPo.setOwnerId(reqJson.getString("ownerId"));
            ownerCarPo.setbId("-1");
            ownerCarPo.setCarNum(reqJson.getString("carNum"));
            ownerCarPo.setCarBrand("无（预约车）");
            ownerCarPo.setCarType("9901");
            ownerCarPo.setCarColor("无（预约车）");
            ownerCarPo.setPsId(reqJson.getString("psId"));
            ownerCarPo.setUserId(userId);
            ownerCarPo.setRemark("访客登记预约车");
            ownerCarPo.setCommunityId(reqJson.getString("communityId"));
            ownerCarPo.setStartTime(reqJson.getString("visitTime"));
            ownerCarPo.setEndTime(reqJson.getString("freeTime"));
            ownerCarPo.setState(OwnerCarDto.STATE_NORMAL); // 正常状态
            ownerCarPo.setCarTypeCd(OwnerCarDto.CAR_TYPE_TEMP); // 临时车
            ownerCarPo.setMemberId(reqJson.getString("ownerId"));
            ownerCarPo.setLeaseType("R"); // 预约车
            ownerCarV1InnerServiceSMOImpl.saveOwnerCar(ownerCarPo);
            
            // 添加车辆属性
            OwnerCarAttrPo ownerCarAttrPo = new OwnerCarAttrPo();
            ownerCarAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_cartId));
            ownerCarAttrPo.setCarId(ownerCarPo.getCarId());
            ownerCarAttrPo.setCommunityId(ownerCarPo.getCommunityId());
            ownerCarAttrPo.setSpecCd("6443000036"); // 业主车辆规格代码
            ownerCarAttrPo.setValue("true"); // 预约车标识
            ownerCarAttrPo.setbId("-1");
            ownerCarAttrInnerServiceSMOImpl.saveOwnerCarAttr(ownerCarAttrPo);
            
            // 更新车位状态为已出租
            ParkingSpacePo parkingSpacePo = new ParkingSpacePo();
            parkingSpacePo.setPsId(reqJson.getString("psId"));
            parkingSpacePo.setState("H"); // 车位状态：出租
            parkingSpaceInnerServiceSMOImpl.updateParkingSpace(parkingSpacePo);
        }
    }

    /**
     * 更新访客信息
     * 
     * 将JSON参数转换为访客PO对象并执行更新操作
     *
     * @param paramInJson 接口调用方传入的参数JSON对象
     * @throws CmdException 当更新访客信息失败时抛出异常
     */
    public void updateVisit(JSONObject paramInJson) {
        JSONObject businessVisit = new JSONObject();
        businessVisit.putAll(paramInJson);

        // 将JSON对象转换为访客PO对象
        VisitPo visitPo = BeanConvertUtil.covertBean(businessVisit, VisitPo.class);
        int flag = visitV1InnerServiceSMOImpl.updateVisit(visitPo);
        if (flag < 1) {
            throw new CmdException("修改访客失败");
        }
    }
}