package com.java110.community.cmd.visit;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.CommunitySettingFactory;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.file.FileDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.dto.visit.VisitDto;
import com.java110.intf.common.IFileInnerServiceSMO;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.community.IParkingSpaceInnerServiceSMO;
import com.java110.intf.community.IVisitInnerServiceSMO;
import com.java110.intf.community.IVisitV1InnerServiceSMO;
import com.java110.intf.user.IOwnerCarAttrInnerServiceSMO;
import com.java110.intf.user.IOwnerCarInnerServiceSMO;
import com.java110.intf.user.IOwnerCarV1InnerServiceSMO;
import com.java110.po.car.OwnerCarPo;
import com.java110.po.file.FileRelPo;
import com.java110.po.owner.VisitPo;
import com.java110.po.owner.OwnerCarAttrPo;
import com.java110.po.parking.ParkingSpacePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * 访客信息保存命令类
 * 
 * 该类负责处理访客登记的业务逻辑，包括访客信息验证、车位分配、车辆登记、
 * 图片文件处理等功能，支持自动审核和人工审核两种模式
 * 
 * @author Java110
 * @version 1.0
 */
//@Java110Cmd(serviceCode = "visit.saveVisit")
public class SaveVisitSdCmd extends Cmd {

    @Autowired
    private IVisitInnerServiceSMO visitInnerServiceSMOImpl;

    @Autowired
    private IVisitV1InnerServiceSMO visitV1InnerServiceSMOImpl;

    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;

    @Autowired
    private IOwnerCarV1InnerServiceSMO ownerCarV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerCarAttrInnerServiceSMO ownerCarAttrInnerServiceSMOImpl;

    @Autowired
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl;

    @Autowired
    private IFileInnerServiceSMO fileInnerServiceSMOImpl;

    /** 是否需要审核配置键 */
    public static final String IS_NEED_REVIEW = "IS_NEED_REVIEW";

    /** 访客车辆登记次数限制配置键 */
    public static final String VISIT_NUMBER = "VISIT_NUMBER";

    /** 车辆免费时长配置键 */
    public static final String CAR_FREE_TIME = "CAR_FREE_TIME";

    /** 归属停车场区域ID配置键 */
    public static final String ASCRIPTION_CAR_AREA_ID = "ASCRIPTION_CAR_AREA_ID";

    /**
     * 验证请求参数
     * 
     * 检查访客登记必需的参数是否完整，包括访客姓名、性别、联系方式和拜访时间
     * 
     * @param event 命令事件对象
     * @param context 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Assert.hasKeyAndValue(reqJson, "vName", "必填，请填写访客姓名");
        Assert.hasKeyAndValue(reqJson, "visitGender", "必填，请填写访客姓名");
        Assert.hasKeyAndValue(reqJson, "phoneNumber", "必填，请填写访客联系方式");
        Assert.hasKeyAndValue(reqJson, "visitTime", "必填，请填写访客拜访时间");
    }

    /**
     * 执行访客登记命令
     * 
     * 处理访客登记的核心业务逻辑，包括：
     * 1. 访客信息验证和状态设置
     * 2. 车位分配逻辑
     * 3. 车辆登记次数检查
     * 4. 图片文件处理
     * 5. 车辆和车位信息更新
     * 
     * @param event 命令事件对象
     * @param context 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当业务处理失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String userId = context.getReqHeaders().get("user-id");
        // 生成访客ID
        reqJson.put("vId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_vId));
        
        // 获取小区是否需要审核的配置
        String isNeedReviewFlag = CommunitySettingFactory.getValue(reqJson.getString("communityId"), IS_NEED_REVIEW);
        
        // 初始化状态标志
        boolean freeSpace = false; // 是否有空闲车位 false 有空闲  true无空闲
        boolean specifiedTimes = false; // 是否超过规定次数
        boolean existCar = false; // 是否存在车辆
        
        // 设置审核状态：如果需要审核且有车牌号，则状态为待审核(0)，否则为审核通过(1)
        if ("true".equals(isNeedReviewFlag) &&  !StringUtil.isEmpty(reqJson.getString("carNum"))) {
            reqJson.put("state", "0"); // 0表示未审核；1表示审核通过；2表示审核拒绝
        } else {
            reqJson.put("state", "1");
        }
        
        // 设置随行人数，默认为0
        if (StringUtil.isEmpty(reqJson.getString("entourage"))) {
            reqJson.put("entourage", "0");
        }
        
        reqJson.put("recordState", "0");
        
        // 处理车辆相关逻辑
        if (reqJson.containsKey("carNum") && !StringUtil.isEmpty(reqJson.getString("carNum"))) {
            // 获取预约车免费时长的配置值，默认120分钟
            String freeTime = CommunitySettingFactory.getValue(reqJson.getString("communityId"), CAR_FREE_TIME);
            if (StringUtil.isEmpty(freeTime)) {
                freeTime = "120";
            }
            
            // 计算免费结束时间
            Date time = DateUtil.getDateFromStringA(reqJson.getString("visitTime"));
            Calendar newTime = Calendar.getInstance();
            newTime.setTime(time);
            newTime.add(Calendar.MINUTE, Integer.parseInt(freeTime)); // 日期加上分钟
            Date newDate = newTime.getTime();
            String finishFreeTime = DateUtil.getFormatTimeString(newDate, DateUtil.DATE_FORMATE_STRING_A);
            reqJson.put("freeTime", finishFreeTime);
            
            // 不需要审核时的车位分配逻辑
            if (!StringUtils.isEmpty(isNeedReviewFlag) && isNeedReviewFlag.equals("false")) {
                // 获取小区配置里配置的停车场id
                String parkingAreaId = CommunitySettingFactory.getValue(reqJson.getString("communityId"), ASCRIPTION_CAR_AREA_ID);
                if (StringUtil.isEmpty(parkingAreaId)) {
                    // 如果没有配置停车场id，就随便分配该小区下一个空闲车位
                    ParkingSpaceDto parkingSpace = new ParkingSpaceDto();
                    parkingSpace.setCommunityId(reqJson.getString("communityId"));
                    parkingSpace.setState("F"); // 车位状态 出售 S，出租 H ，空闲 F
                    parkingSpace.setParkingType("1"); // 1：普通车位  2：子母车位  3：豪华车位
                    
                    // 查询小区空闲车位
                    List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpace);
                    if (parkingSpaceDtos == null || parkingSpaceDtos.size() < 1) {
                        freeSpace = true; // 无空闲车位
                    } else {
                        // 随机分配一个空闲车位
                        Random random = new Random();
                        int i = random.nextInt(parkingSpaceDtos.size());
                        String psId = parkingSpaceDtos.get(i).getPsId();
                        reqJson.put("psId", psId);
                    }
                } else {
                    // 在指定停车场区域分配空闲车位
                    ParkingSpaceDto parkingSpace = new ParkingSpaceDto();
                    parkingSpace.setCommunityId(reqJson.getString("communityId"));
                    parkingSpace.setPaId(parkingAreaId); // 停车场id
                    parkingSpace.setState("F"); // 车位状态 出售 S，出租 H ，空闲 F
                    parkingSpace.setParkingType("1"); // 1：普通车位  2：子母车位  3：豪华车位
                    
                    List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpace);
                    if (parkingSpaceDtos == null || parkingSpaceDtos.size() < 1) {
                        freeSpace = true; // 无空闲车位
                    } else {
                        // 随机分配一个空闲车位
                        Random random = new Random();
                        int i = random.nextInt(parkingSpaceDtos.size());
                        String psId = parkingSpaceDtos.get(i).getPsId();
                        reqJson.put("psId", psId);
                    }
                }
            } else {
                // 需要审核时暂不分配车位，等待审核通过后再分配
                reqJson.put("psId", "-1");
            }
            
            // 检查预约车辆登记次数限制
            String visitNumber = CommunitySettingFactory.getValue(reqJson.getString("communityId"), VISIT_NUMBER);
            if (StringUtil.isEmpty(visitNumber)) {
                visitNumber = "10000"; // 默认值
            }
            int number = Integer.parseInt(visitNumber);

            // 查询当天车辆登记次数
            VisitDto visitDto = new VisitDto();
            visitDto.setOwnerId(reqJson.getString("ownerId"));
            visitDto.setCarNumNoEmpty("1");
            visitDto.setSameDay("1");
            List<VisitDto> visitDtos = visitInnerServiceSMOImpl.queryVisits(visitDto);
            int count = visitDtos.size();
            
            // 预约车辆登记次数0不做限制，否则检查是否超过限制
            if (count >= number && number > 0) {
                if (!StringUtils.isEmpty(isNeedReviewFlag) && isNeedReviewFlag.equals("false")) {
                    // 超过次数限制且不需要审核时，重置车位和免费时间信息
                    reqJson.put("state", "0");
                    reqJson.put("psId", null);
                    reqJson.put("freeTime", null);
                }
                specifiedTimes = true; // 标记超过次数限制
            }
            
            // 校验车牌号是否已存在
            OwnerCarDto ownerCarDto1 = new OwnerCarDto();
            ownerCarDto1.setCommunityId(reqJson.getString("communityId"));
            ownerCarDto1.setCarNum(reqJson.getString("carNum"));
            int existCarCount = ownerCarInnerServiceSMOImpl.queryOwnerCarsCount(ownerCarDto1);
            if (existCarCount > 0) {
                existCar = true; // 标记车辆已存在
                reqJson.put("psId", "-1");
            }
        }
        
        // 根据不同的异常情况设置返回结果信息
        String result = "";
        if (existCar) {
            result = "访客信息登记成功,车辆已经存在预约，请您在预约到期后，再次进行车辆预约，谢谢！";
        }
        if (specifiedTimes) {
            result = "访客信息登记成功,您已经超过预约车辆登记次数限制，车辆将无法自动审核！";
        }
        if (freeSpace) {
            result = "访客信息登记成功,当前停车场已无空闲车位，登记车辆将暂时不能进入停车场，请您合理安排出行。";
        }
        reqJson.put("stateRemark", result);
        
        // 保存访客信息
        addVisit(reqJson);
        
        // 处理访客照片
        if (reqJson.containsKey("photo") && !StringUtils.isEmpty(reqJson.getString("photo"))) {
            if(reqJson.getString("photo").length()>512){ // 说明是图片base64数据
                // 保存图片文件
                FileDto fileDto = new FileDto();
                fileDto.setFileId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_file_id));
                fileDto.setFileName(fileDto.getFileId());
                fileDto.setContext(reqJson.getString("photo"));
                fileDto.setSuffix("jpeg");
                fileDto.setCommunityId(reqJson.getString("communityId"));
                String fileName = fileInnerServiceSMOImpl.saveFile(fileDto);
                reqJson.put("photo", fileName);
            }
            
            // 建立文件关联关系
            JSONObject businessUnit = new JSONObject();
            businessUnit.put("fileRelId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_fileRelId));
            businessUnit.put("relTypeCd", "11000");
            businessUnit.put("saveWay", "table");
            businessUnit.put("objId", reqJson.getString("vId"));
            businessUnit.put("fileRealName", reqJson.getString("photo"));
            businessUnit.put("fileSaveName", reqJson.getString("photo"));
            FileRelPo fileRelPo = BeanConvertUtil.covertBean(businessUnit, FileRelPo.class);
            fileRelInnerServiceSMOImpl.saveFileRel(fileRelPo);
        }
        
        // 审核通过且有车位时的车辆和车位处理
        if ("1".equals(reqJson.getString("state"))
                && reqJson.containsKey("carNum") && !StringUtil.isEmpty(reqJson.getString("carNum"))
                && !existCar && !StringUtil.isEmpty(reqJson.getString("psId")) && !"-1".equals(reqJson.getString("psId"))) {
            
            // 查询停车位信息
            ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
            parkingSpaceDto.setPsId(reqJson.getString("psId"));
            List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);
            Assert.listOnlyOne(parkingSpaceDtos, "访客登记,查询停车位错误！");
            
            // 添加车辆信息
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
            ownerCarPo.setState(OwnerCarDto.STATE_NORMAL); // 1001 正常状态，2002 车位释放欠费状态  3003 车位释放
            ownerCarPo.setCarTypeCd(OwnerCarDto.CAR_TYPE_TEMP); // 1001 业主车辆 1002 成员车辆 1003 临时车
            ownerCarPo.setMemberId(reqJson.getString("ownerId"));
            ownerCarPo.setLeaseType(OwnerCarDto.LEASE_TYPE_RESERVE); // H 月租车   S 出售车   I 内部车   NM 免费车   R 预约车
            ownerCarV1InnerServiceSMOImpl.saveOwnerCar(ownerCarPo);
            
            // 添加车辆属性
            OwnerCarAttrPo ownerCarAttrPo = new OwnerCarAttrPo();
            ownerCarAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_cartId));
            ownerCarAttrPo.setCarId(ownerCarPo.getCarId());
            ownerCarAttrPo.setCommunityId(ownerCarPo.getCommunityId());
            ownerCarAttrPo.setSpecCd("6443000036"); // 6443000036业主车辆
            ownerCarAttrPo.setValue("true"); // 预约车
            ownerCarAttrPo.setbId("-1");
            ownerCarAttrInnerServiceSMOImpl.saveOwnerCarAttr(ownerCarAttrPo);
            
            // 更改车位状态为出租状态
            ParkingSpacePo parkingSpacePo = new ParkingSpacePo();
            parkingSpacePo.setPsId(reqJson.getString("psId"));
            parkingSpacePo.setState("H"); // 车位状态 出售 S，出租 H ，空闲 F
            parkingSpaceInnerServiceSMOImpl.updateParkingSpace(parkingSpacePo);
        }
        
        // 根据不同的异常情况返回相应的响应信息
        if (existCar) {
            ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_OK, "访客信息登记成功,车辆已经存在预约，请您在预约到期后，再次进行车辆预约，谢谢！");
            context.setResponseEntity(responseEntity);
            return;
        }
        if (specifiedTimes) {
            ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_OK, "访客信息登记成功,您已经超过预约车辆登记次数限制，车辆将无法自动审核！");
            context.setResponseEntity(responseEntity);
            return;
        }
        if (freeSpace) {
            ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_OK, "访客信息登记成功,当前停车场已无空闲车位，登记车辆将暂时不能进入停车场，请您合理安排出行。");
            context.setResponseEntity(responseEntity);
            return;
        }
    }

    /**
     * 添加访客信息
     * 
     * 将访客信息保存到数据库中
     * 
     * @param paramInJson 包含访客信息的JSON对象
     * @throws CmdException 当保存失败时抛出异常
     */
    public void addVisit(JSONObject paramInJson) {
        JSONObject businessVisit = new JSONObject();
        businessVisit.putAll(paramInJson);

        // 转换JSON对象为PO对象并保存
        VisitPo visitPo = BeanConvertUtil.covertBean(businessVisit, VisitPo.class);
        int flag = visitV1InnerServiceSMOImpl.saveVisit(visitPo);
        if (flag < 1) {
            throw new CmdException("保存访客失败");
        }
    }
}