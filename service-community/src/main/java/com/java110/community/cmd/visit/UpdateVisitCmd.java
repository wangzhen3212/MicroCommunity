/**
 * 访客信息更新命令类
 * 
 * 该类负责处理访客信息的更新操作，包括：
 * 1. 更新访客基本信息
 * 2. 同步访客人脸信息到门禁白名单
 * 3. 同步访客车辆信息到停车场白名单
 * 4. 处理访客照片保存
 * 
 * 使用@Java110Cmd注解标识为命令类，服务码为"visit.updateVisit"
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.community.cmd.visit;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.core.smo.IPhotoSMO;
import com.java110.dto.room.RoomDto;
import com.java110.dto.accessControl.AccessControlWhiteDto;
import com.java110.dto.machine.CarBlackWhiteDto;
import com.java110.dto.machine.MachineDto;
import com.java110.dto.visit.VisitDto;
import com.java110.dto.visit.VisitSettingDto;
import com.java110.intf.common.IAccessControlWhiteV1InnerServiceSMO;
import com.java110.intf.common.IMachineInnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.community.IVisitSettingV1InnerServiceSMO;
import com.java110.intf.community.IVisitV1InnerServiceSMO;
import com.java110.intf.user.ICarBlackWhiteV1InnerServiceSMO;
import com.java110.po.accessControl.AccessControlWhitePo;
import com.java110.po.car.CarBlackWhitePo;
import com.java110.po.owner.VisitPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

@Java110Cmd(serviceCode = "visit.updateVisit")
public class UpdateVisitCmd extends Cmd {

    /** 访客信息服务接口 */
    @Autowired
    private IVisitV1InnerServiceSMO visitV1InnerServiceSMOImpl;

    /** 照片服务接口 */
    @Autowired
    private IPhotoSMO photoSMOImpl;

    /** 访客设置服务接口 */
    @Autowired
    private IVisitSettingV1InnerServiceSMO visitSettingV1InnerServiceSMOImpl;

    /** 车辆黑白名单服务接口 */
    @Autowired
    private ICarBlackWhiteV1InnerServiceSMO carBlackWhiteV1InnerServiceSMOImpl;

    /** 门禁白名单服务接口 */
    @Autowired
    private IAccessControlWhiteV1InnerServiceSMO accessControlWhiteV1InnerServiceSMOImpl;

    /** 房屋信息服务接口 */
    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    /** 设备信息服务接口 */
    @Autowired
    private IMachineInnerServiceSMO machineInnerServiceSMOImpl;

    /** ID生成前缀 */
    public static final String CODE_PREFIX_ID = "10";

    /** 车辆免费时间配置键 */
    public static final String CAR_FREE_TIME = "CAR_FREE_TIME";

    /** 车辆归属区域ID配置键 */
    public static final String ASCRIPTION_CAR_AREA_ID = "ASCRIPTION_CAR_AREA_ID";

    /**
     * 参数验证方法
     * 
     * 验证更新访客信息所需的必要参数是否完整
     * 
     * @param event 命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证访客记录ID不能为空
        Assert.hasKeyAndValue(reqJson, "vId", "访客记录ID不能为空");
        // 验证小区ID不能为空
        Assert.hasKeyAndValue(reqJson, "communityId", "小区ID不能为空");
        // 验证访客姓名不能为空
        Assert.hasKeyAndValue(reqJson, "vName", "必填，请填写访客姓名");
    }

    /**
     * 执行访客更新命令
     * 
     * 主要功能：
     * 1. 更新访客基本信息
     * 2. 保存访客照片
     * 3. 根据访客设置同步人脸和车辆信息
     * 
     * @param event 命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当更新失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 构建查询条件，查找指定访客记录
        VisitDto visitDto = new VisitDto();
        visitDto.setvId(reqJson.getString("vId"));
        visitDto.setCommunityId(reqJson.getString("communityId"));
        List<VisitDto> visitDtos = visitV1InnerServiceSMOImpl.queryVisits(visitDto);
        
        // 验证访客记录存在且唯一
        Assert.listOnlyOne(visitDtos, "访客不存在");
        
        // 将请求参数转换为持久化对象
        VisitPo visitPo = BeanConvertUtil.covertBean(reqJson, VisitPo.class);
        // 保持原有状态不变
        visitPo.setState(visitDtos.get(0).getState());
        
        // 执行访客信息更新
        int flag = visitV1InnerServiceSMOImpl.updateVisit(visitPo);
        if (flag < 1) {
            throw new CmdException("保存访客失败");
        }
        
        // 保存访客照片
        photoSMOImpl.savePhoto(reqJson, reqJson.getString("vId"), reqJson.getString("communityId"));
        
        // 如果访客状态不是已确认状态，则不同步人脸和车辆信息
        if (!VisitDto.STATE_C.equals(visitDtos.get(0).getState())) {
            return;
        }
        
        // 默认启用人脸同步，禁用车辆同步
        String faceWay = "Y";
        String carNumWay = "N";
        
        // 查询访客设置配置
        VisitSettingDto visitSettingDto = new VisitSettingDto();
        visitSettingDto.setCommunityId(reqJson.getString("communityId"));
        List<VisitSettingDto> visitSettingDtos = visitSettingV1InnerServiceSMOImpl.queryVisitSettings(visitSettingDto);
        
        // 如果存在访客设置，则使用配置的值
        if (visitSettingDtos != null && visitSettingDtos.size() > 0) {
            faceWay = visitSettingDtos.get(0).getFaceWay();
            carNumWay = visitSettingDtos.get(0).getCarNumWay();
            // 同步车牌信息到停车场白名单
            synchronizedVisitCarNum(visitPo, carNumWay, visitSettingDtos.get(0));
        }
        
        // 同步访客人脸信息到门禁白名单
        synchronousVisitFace(visitPo, faceWay, reqJson.getString("photo"));
    }

    /**
     * 同步访客人脸信息到门禁白名单
     * 
     * 根据访客设置的访客方式和照片信息，将访客人脸同步到相关门禁设备的白名单中
     * 
     * @param visitPo 访客持久化对象
     * @param faceWay 人脸同步方式（Y-同步，N-不同步）
     * @param photo 访客照片数据
     */
    private void synchronousVisitFace(VisitPo visitPo, String faceWay, String photo) {
        // 如果不需要同步人脸或照片为空，则直接返回
        if (VisitSettingDto.FACE_WAY_NO.equals(faceWay) || StringUtil.isEmpty(photo)) {
            return;
        }
        
        // 如果访客没有关联业主ID，则无法同步到具体房屋的门禁设备
        if (StringUtil.isEmpty(visitPo.getOwnerId())) {
            return;
        }
        
        // 查询访客可以访问的房屋信息
        RoomDto roomDto = new RoomDto();
        roomDto.setOwnerId(visitPo.getOwnerId());
        List<RoomDto> rooms = roomInnerServiceSMOImpl.queryRoomsByOwner(roomDto);
        
        // 获取小区ID
        String communityId = visitPo.getCommunityId();
        
        // 构建设备查询条件
        MachineDto machineDto = new MachineDto();
        machineDto.setCommunityId(communityId);
        List<String> locationObjIds = new ArrayList<>();
        locationObjIds.add(communityId);
        
        // 添加房屋相关的所有位置ID（单元、房间、楼层）
        for (RoomDto tRoomDto : rooms) {
            locationObjIds.add(tRoomDto.getUnitId());
            locationObjIds.add(tRoomDto.getRoomId());
            locationObjIds.add(tRoomDto.getFloorId());
        }
        machineDto.setLocationObjIds(locationObjIds.toArray(new String[locationObjIds.size()]));
        
        // 查询符合条件的门禁设备
        List<MachineDto> machineDtos = machineInnerServiceSMOImpl.queryMachines(machineDto);
        if (machineDtos == null || machineDtos.size() < 1) {
            return;
        }
        
        // 遍历所有门禁设备，将访客信息同步到白名单
        for (MachineDto tmpMachineDto : machineDtos) {
            // 只处理门禁设备（设备类型码为9999）
            if (!"9999".equals(tmpMachineDto.getMachineTypeCd())) {
                continue;
            }
            
            // 检查是否已存在该访客的白名单记录
            AccessControlWhiteDto accessControlWhiteDto = new AccessControlWhiteDto();
            accessControlWhiteDto.setCommunityId(communityId);
            accessControlWhiteDto.setTel(visitPo.getPhoneNumber());
            accessControlWhiteDto.setMachineId(tmpMachineDto.getMachineId());
            List<AccessControlWhiteDto> accessControlWhiteDtos = accessControlWhiteV1InnerServiceSMOImpl.queryAccessControlWhites(accessControlWhiteDto);
            
            AccessControlWhitePo accessControlWhitePo = new AccessControlWhitePo();
            
            // 如果不存在记录，则创建新的白名单记录
            if (accessControlWhiteDtos == null || accessControlWhiteDtos.size() < 1) {
                accessControlWhitePo.setAcwId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
                accessControlWhitePo.setCommunityId(visitPo.getCommunityId());
                accessControlWhitePo.setEndTime(visitPo.getDepartureTime()); // 设置有效结束时间
                accessControlWhitePo.setIdCard("");
                accessControlWhitePo.setMachineId(tmpMachineDto.getMachineId());
                accessControlWhitePo.setPersonName(visitPo.getvName());
                accessControlWhitePo.setPersonType(AccessControlWhiteDto.PERSON_TYPE_VISIT); // 设置人员类型为访客
                accessControlWhitePo.setStartTime(visitPo.getVisitTime()); // 设置有效开始时间
                accessControlWhitePo.setTel(visitPo.getPhoneNumber());
                accessControlWhitePo.setThirdId(visitPo.getvId()); // 设置第三方ID为访客ID
                
                int flag = accessControlWhiteV1InnerServiceSMOImpl.saveAccessControlWhite(accessControlWhitePo);
                if (flag < 1) {
                    throw new CmdException("同步门禁白名单失败");
                }
            } else {
                // 如果已存在记录，则更新有效时间
                accessControlWhitePo.setAcwId(accessControlWhiteDtos.get(0).getAcwId());
                accessControlWhitePo.setStartTime(visitPo.getVisitTime());
                accessControlWhitePo.setEndTime(visitPo.getDepartureTime());
                int flag = accessControlWhiteV1InnerServiceSMOImpl.updateAccessControlWhite(accessControlWhitePo);
                if (flag < 1) {
                    throw new CmdException("保存数据失败");
                }
            }
            
            // 保存人脸照片到门禁白名单记录
            photoSMOImpl.savePhoto(photo, accessControlWhitePo.getAcwId(), accessControlWhitePo.getCommunityId());
        }
    }

    /**
     * 同步访客车辆信息到停车场白名单
     * 
     * 将访客车辆信息添加到停车场白名单，使访客车辆在指定时间段内可以进出停车场
     * 
     * @param visitPo 访客持久化对象
     * @param carNumWay 车辆同步方式（Y-同步，N-不同步）
     * @param visitSettingDto 访客设置信息
     */
    private void synchronizedVisitCarNum(VisitPo visitPo, String carNumWay, VisitSettingDto visitSettingDto) {
        // 如果不需要同步车辆或车辆号码为空，则直接返回
        if (VisitSettingDto.CAR_NUM_WAY_NO.equals(carNumWay)) {
            return;
        }
        if (StringUtil.isEmpty(visitPo.getCarNum())) {
            return;
        }
        
        // 检查是否已存在该车辆的停车场白名单记录
        CarBlackWhiteDto carBlackWhiteDto = new CarBlackWhiteDto();
        carBlackWhiteDto.setBlackWhite(CarBlackWhiteDto.BLACK_WHITE_WHITE); // 设置为白名单
        carBlackWhiteDto.setCarNum(visitPo.getCarNum());
        carBlackWhiteDto.setPaId(visitSettingDto.getPaId()); // 停车场区域ID
        List<CarBlackWhiteDto> carBlackWhiteDtos = carBlackWhiteV1InnerServiceSMOImpl.queryCarBlackWhites(carBlackWhiteDto);
        
        // 构建车辆白名单持久化对象
        CarBlackWhitePo carBlackWhitePo = new CarBlackWhitePo();
        carBlackWhitePo.setCarNum(visitPo.getCarNum());
        carBlackWhitePo.setBlackWhite(CarBlackWhiteDto.BLACK_WHITE_WHITE);
        carBlackWhitePo.setCommunityId(visitPo.getCommunityId());
        carBlackWhitePo.setPaId(visitSettingDto.getPaId());
        carBlackWhitePo.setStartTime(visitPo.getVisitTime()); // 设置有效开始时间
        carBlackWhitePo.setEndTime(visitPo.getDepartureTime()); // 设置有效结束时间
        
        int flag = 0;
        // 根据是否存在记录决定是新增还是更新
        if (carBlackWhiteDtos == null || carBlackWhiteDtos.size() < 1) {
            carBlackWhitePo.setBwId(GenerateCodeFactory.getGeneratorId("11")); // 生成白名单ID
            flag = carBlackWhiteV1InnerServiceSMOImpl.saveCarBlackWhite(carBlackWhitePo);
        } else {
            carBlackWhitePo.setBwId(carBlackWhiteDtos.get(0).getBwId());
            flag = carBlackWhiteV1InnerServiceSMOImpl.updateCarBlackWhite(carBlackWhitePo);
        }

        if (flag < 1) {
            throw new CmdException("预约车辆添加白名单失败");
        }
    }

    /**
     * 检查访客记录是否需要审核
     * 
     * 根据小区访客设置判断当前访客记录是否需要审核流程
     * 
     * @param visitPo 访客持久化对象
     * @param reqJson 请求参数JSON对象
     * @param storeId 店铺ID
     * @param userId 用户ID
     * @return boolean 是否需要审核（true-需要审核，false-不需要审核）
     */
    private boolean hasAuditVisit(VisitPo visitPo, JSONObject reqJson, String storeId, String userId) {
        // 查询小区访客设置
        VisitSettingDto visitSettingDto = new VisitSettingDto();
        visitSettingDto.setCommunityId(reqJson.getString("communityId"));
        List<VisitSettingDto> visitSettingDtos = visitSettingV1InnerServiceSMOImpl.queryVisitSettings(visitSettingDto);
        
        // 如果没有设置，则默认不需要审核
        if (visitSettingDtos == null || visitSettingDtos.size() < 1) {
            return false;
        }
        
        // 根据审核方式判断是否需要审核
        if (!VisitSettingDto.AUDIT_WAY_YES.equals(visitSettingDtos.get(0).getAuditWay())) {
            return false;
        }
        
        return true;
    }
}