package com.java110.community.cmd.visit;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.dto.visit.VisitDto;
import com.java110.dto.visit.VisitSettingDto;
import com.java110.intf.common.IOaWorkflowActivitiInnerServiceSMO;
import com.java110.intf.community.IParkingSpaceInnerServiceSMO;
import com.java110.intf.community.IVisitInnerServiceSMO;
import com.java110.intf.community.IVisitSettingV1InnerServiceSMO;
import com.java110.intf.community.IVisitV1InnerServiceSMO;
import com.java110.po.owner.VisitPo;
import com.java110.po.parking.ParkingSpacePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * 访客审核撤销命令类
 * 负责处理访客信息的审核流程，包括审核通过、转单、退回等操作
 * 同时处理访客车辆的停车位分配、免费时长计算等业务逻辑
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "visit.auditUndoVisit")
public class AuditUndoVisitCmd extends Cmd {

    @Autowired
    private IOaWorkflowActivitiInnerServiceSMO oaWorkflowUserInnerServiceSMOImpl;

    @Autowired
    private IVisitV1InnerServiceSMO visitV1InnerServiceSMOImpl;

    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;

    @Autowired
    private IVisitInnerServiceSMO visitInnerServiceSMOImpl;

    @Autowired
    private IVisitSettingV1InnerServiceSMO visitSettingV1InnerServiceSMOImpl;

    /**
     * ID前缀常量
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 参数验证方法
     * 验证请求参数是否完整
     *
     * @param event 命令事件
     * @param context 数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证必要参数是否存在
        Assert.hasKeyAndValue(reqJson, "taskId", "未包含任务");
        Assert.hasKeyAndValue(reqJson, "vId", "未包含访客ID");
        Assert.hasKeyAndValue(reqJson, "flowId", "未包含流程");
        Assert.hasKeyAndValue(reqJson, "auditCode", "未包含状态");
        Assert.hasKeyAndValue(reqJson, "auditMessage", "未包含状态说明");
    }

    /**
     * 执行访客审核命令
     * 根据审核状态码执行不同的业务流程：
     * - 1100/1500: 办理操作，处理审核通过逻辑
     * - 1300: 转单操作，将任务转给其他用户
     * - 1200/1400: 退回操作，退回任务给发起人
     *
     * @param event 命令事件
     * @param context 数据流上下文
     * @param reqJson 请求JSON对象，包含审核相关参数
     * @throws CmdException 命令执行异常
     * @throws ParseException 日期解析异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        String storeId = context.getReqHeaders().get("store-id");
        
        // 查询访客信息
        VisitDto visitDto = new VisitDto();
        visitDto.setvId(reqJson.getString("vId"));
        visitDto.setCommunityId(reqJson.getString("communityId"));
        List<VisitDto> visitDtos = visitV1InnerServiceSMOImpl.queryVisits(visitDto);
        Assert.listOnlyOne(visitDtos, "访客不存在");
        
        // 准备更新访客信息
        VisitPo visitPo = new VisitPo();
        visitPo.setMark("1"); // 标记为已处理
        visitPo.setvId(visitDtos.get(0).getvId());
        visitPo.setCommunityId(reqJson.getString("communityId"));
        reqJson.put("id", reqJson.getString("vId"));
        reqJson.put("storeId", storeId);
        
        // 业务办理操作（审核通过）
        if ("1100".equals(reqJson.getString("auditCode"))
                || "1500".equals(reqJson.getString("auditCode"))) {
            reqJson.put("nextUserId", reqJson.getString("staffId"));
            // 完成当前任务并判断是否为最后一个任务
            boolean isLastTask = oaWorkflowUserInnerServiceSMOImpl.completeTask(reqJson);
            if (isLastTask) {
                visitPo.setState(VisitDto.STATE_C); // 审核完成
            } else {
                visitPo.setState(VisitDto.STATE_D); // 审核中
            }
            
            // 如果审核完成，处理访客车辆相关逻辑
            if (VisitDto.STATE_C.equals(visitPo.getState())) {
                // 查询访客配置信息
                VisitSettingDto visitSettingDto = new VisitSettingDto();
                visitSettingDto.setCommunityId(reqJson.getString("communityId"));
                List<VisitSettingDto> visitSettingDtos = visitSettingV1InnerServiceSMOImpl.queryVisitSettings(visitSettingDto);
                
                // 处理有车辆的情况
                if (!StringUtil.isEmpty(visitDtos.get(0).getCarNum())) {
                    // 车辆同步配置为Y时，处理车辆审核逻辑
                    if (!ListUtil.isNull(visitSettingDtos) && "Y".equals(visitSettingDtos.get(0).getCarNumWay())) {
                        // 获取车辆操作相关配置
                        JSONObject visitJson = getVisitCarOperate(reqJson);
                        // 判断车辆是否需要审核
                        String isNeedReview = visitJson.getString("isNeedReview");
                        if (!StringUtil.isEmpty(isNeedReview) && isNeedReview.equals("0")) {
                            // 需要审核，设置车辆状态为待审核
                            visitPo.setCarState(VisitDto.CAR_STATE_W);
                            int flag = visitV1InnerServiceSMOImpl.updateVisit(visitPo);
                            if (flag < 1) {
                                throw new CmdException("修改访客状态失败");
                            }
                        } else {
                            // 不需要审核，处理车辆自动审核逻辑
                            reqJson.put("ownerId", visitDtos.get(0).getOwnerId());
                            JSONObject param = dealVisitorRegistrationTimes(visitJson);
                            
                            // 检查是否超过车辆登记次数限制
                            if (param.containsKey("specifiedTimes") && !StringUtil.isEmpty(param.getString("specifiedTimes")) 
                                    && param.getString("specifiedTimes").equals("true")) {
                                // 超过限制，车辆无法审核
                                visitPo.setStateRemark("访客信息登记成功,您已经超过预约车辆登记次数限制，车辆将无法审核！");
                                visitPo.setCarStateRemark("访客信息登记成功,您已经超过预约车辆登记次数限制，车辆将无法审核！");
                                visitPo.setCarState(VisitDto.CAR_STATE_F); // 审核拒绝
                                ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_OK, 
                                        "访客信息登记成功,您已经超过预约车辆登记次数限制，车辆将无法自动审核！");
                                context.setResponseEntity(responseEntity);
                                int flag = visitV1InnerServiceSMOImpl.updateVisit(visitPo);
                                if (flag < 1) {
                                    throw new CmdException("保存访客失败");
                                }
                            } else {
                                // 未超过限制，处理停车位分配
                                JSONObject paramJson = dealParkingSpace(visitJson);
                                if (paramJson.containsKey("freeSpace") && !StringUtil.isEmpty(paramJson.getString("freeSpace")) 
                                        && paramJson.getString("freeSpace").equals("true")) {
                                    // 无空闲车位
                                    visitPo.setStateRemark("访客信息登记成功,当前停车场已无空闲车位，登记车辆将暂时不能进入停车场，请您合理安排出行。");
                                    visitPo.setCarStateRemark("访客信息登记成功,当前停车场已无空闲车位，登记车辆将暂时不能进入停车场，请您合理安排出行。");
                                    visitPo.setCarState(VisitDto.CAR_STATE_F); // 审核拒绝
                                    ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_OK, 
                                            "访客信息登记成功,当前停车场已无空闲车位，登记车辆将暂时不能进入停车场，请您合理安排出行。");
                                    context.setResponseEntity(responseEntity);
                                    int flag = visitV1InnerServiceSMOImpl.updateVisit(visitPo);
                                    if (flag < 1) {
                                        throw new CmdException("保存访客失败");
                                    }
                                } else {
                                    // 有空闲车位，分配车位并设置免费时长
                                    visitPo.setPsId(paramJson.getString("psId"));
                                    reqJson.put("visitTime", visitDtos.get(0).getVisitTime());
                                    String freeTime = dealVisitCarFreeTime(visitJson);
                                    visitPo.setFreeTime(freeTime); // 设置免费时长
                                    visitPo.setCarState(VisitDto.CAR_STATE_C); // 审核通过
                                    int flag = visitV1InnerServiceSMOImpl.updateVisit(visitPo);
                                    if (flag < 1) {
                                        throw new CmdException("修改访客状态失败");
                                    }
                                    // 修改车位状态为已占用
                                    modifyParkingSpaceSate(paramJson.getString("psId"));
                                    // 重新查询访客信息验证
                                    VisitDto visit = new VisitDto();
                                    visit.setvId(reqJson.getString("vId"));
                                    List<VisitDto> visits = visitV1InnerServiceSMOImpl.queryVisits(visitDto);
                                    Assert.listOnlyOne(visits, "查询访客信息错误！");
                                }
                            }
                        }
                    } else if (visitSettingDtos != null && visitSettingDtos.size() == 1 
                            && visitSettingDtos.get(0).getCarNumWay().equals("N")) {
                        // 车辆不同步配置，直接设置车辆审核通过
                        visitPo.setCarState(VisitDto.CAR_STATE_C);
                        visitPo.setCarStateRemark("车辆不同步！");
                        int flag = visitV1InnerServiceSMOImpl.updateVisit(visitPo);
                        if (flag < 1) {
                            throw new CmdException("修改访客状态失败");
                        }
                    }
                } else {
                    // 无车辆情况，直接更新访客状态
                    int flag = visitV1InnerServiceSMOImpl.updateVisit(visitPo);
                    if (flag < 1) {
                        throw new CmdException("修改访客状态失败");
                    }
                }
            } else {
                // 其他审核状态，直接更新访客状态
                int flag = visitV1InnerServiceSMOImpl.updateVisit(visitPo);
                if (flag < 1) {
                    throw new CmdException("修改访客状态失败");
                }
            }
        } else if ("1300".equals(reqJson.getString("auditCode"))) {
            // 转单操作：将任务转给其他用户处理
            reqJson.put("nextUserId", reqJson.getString("staffId"));
            oaWorkflowUserInnerServiceSMOImpl.changeTaskToOtherUser(reqJson);
            visitPo.setState(VisitDto.STATE_D); // 审核中
            visitPo.setSign("1"); // 标记已处理
            visitV1InnerServiceSMOImpl.updateVisit(visitPo);
        } else if ("1200".equals(reqJson.getString("auditCode"))
                || "1400".equals(reqJson.getString("auditCode"))) {
            // 退回操作：将任务退回给发起人
            reqJson.put("startUserId", reqJson.getString("userId"));
            oaWorkflowUserInnerServiceSMOImpl.goBackTask(reqJson);
            visitPo.setState(VisitDto.STATE_F); // 审核失败
            visitPo.setSign("1"); // 标记已处理
            visitV1InnerServiceSMOImpl.updateVisit(visitPo);
        } else {
            throw new IllegalArgumentException("不支持的类型");
        }
    }

    /**
     * 处理停车位分配
     * 根据小区配置分配空闲停车位，优先使用配置的停车场，如果没有配置则随机分配小区内空闲车位
     *
     * @param reqJson 请求JSON对象，包含小区ID等参数
     * @return JSONObject 处理结果，包含车位ID和空闲状态
     */
    public JSONObject dealParkingSpace(JSONObject reqJson) {
        boolean freeSpace = false; // 是否有空闲车位标志
        
        // 获取配置的停车场ID
        String ascriptionCarAreaId = reqJson.getString("ascriptionCarAreaId");
        if (StringUtil.isEmpty(ascriptionCarAreaId)) {
            // 未配置停车场ID，查询小区内所有空闲车位
            ParkingSpaceDto parkingSpace = new ParkingSpaceDto();
            parkingSpace.setCommunityId(reqJson.getString("communityId"));
            parkingSpace.setState("F"); // 空闲状态
            parkingSpace.setParkingType("1"); // 普通车位
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
            // 使用配置的停车场ID查询空闲车位
            ParkingSpaceDto parkingSpace = new ParkingSpaceDto();
            parkingSpace.setCommunityId(reqJson.getString("communityId"));
            parkingSpace.setPaId(ascriptionCarAreaId);
            parkingSpace.setState("F"); // 空闲状态
            parkingSpace.setParkingType("1"); // 普通车位
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
        
        reqJson.put("freeSpace", freeSpace);
        return reqJson;
    }

    /**
     * 修改停车位状态
     * 将指定的停车位状态从空闲改为已占用
     *
     * @param psId 停车位ID
     */
    public void modifyParkingSpaceSate(String psId) {
        // 查询车位信息
        ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
        parkingSpaceDto.setPsId(psId);
        List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);
        Assert.listOnlyOne(parkingSpaceDtos, "查询车位错误！");
        
        // 更新车位状态为已占用
        ParkingSpacePo parkingSpacePo = new ParkingSpacePo();
        parkingSpacePo.setPsId(parkingSpaceDtos.get(0).getPsId());
        parkingSpacePo.setState("H"); // 出租/占用状态
        parkingSpaceInnerServiceSMOImpl.updateParkingSpace(parkingSpacePo);
    }

    /**
     * 处理访客登记次数检查
     * 检查车主当天是否超过预约车辆登记次数限制
     *
     * @param reqJson 请求JSON对象，包含车主ID等参数
     * @return JSONObject 处理结果，包含是否超过限制的标志
     */
    public JSONObject dealVisitorRegistrationTimes(JSONObject reqJson) {
        boolean specifiedTimes = false; // 是否超过规定次数标志
        
        // 获取预约车辆登记次数限制
        String visitNumber = reqJson.getString("visitNumber");
        if (StringUtil.isEmpty(visitNumber)) {
            reqJson.put("specifiedTimes", specifiedTimes);
            return reqJson;
        }
        
        int number = Integer.parseInt(visitNumber);
        // 查询当天车辆登记次数
        VisitDto visitDto = new VisitDto();
        visitDto.setOwnerId(reqJson.getString("ownerId"));
        visitDto.setCarNumNoEmpty("1"); // 有车辆
        visitDto.setSameDay("1"); // 当天
        visitDto.setCarState("1"); // 车辆审核通过
        visitDto.setSign(reqJson.getString("vId")); // 排除当前记录
        List<VisitDto> visitDtos = visitInnerServiceSMOImpl.queryVisits(visitDto);
        
        int count = visitDtos.size();
        // 检查是否超过限制（0表示不限制）
        if (count >= number && number > 0) {
            reqJson.put("psId", null);
            reqJson.put("freeTime", null);
            specifiedTimes = true; // 超过限制
        }
        
        reqJson.put("specifiedTimes", specifiedTimes);
        return reqJson;
    }

    /**
     * 处理预约车辆免费时长计算
     * 根据访客预约时间和配置的免费时长，计算免费结束时间
     *
     * @param reqJson 请求JSON对象，包含访客预约时间
     * @return String 免费结束时间字符串
     */
    public String dealVisitCarFreeTime(JSONObject reqJson) {
        // 获取配置的免费时长，默认120分钟
        String carFreeTime = reqJson.getString("carFreeTime");
        if (StringUtil.isEmpty(carFreeTime)) {
            carFreeTime = "120";
        }
        
        // 计算免费结束时间
        Date time = DateUtil.getDateFromStringA(reqJson.getString("visitTime"));
        Calendar newTime = Calendar.getInstance();
        newTime.setTime(time);
        newTime.add(Calendar.MINUTE, Integer.parseInt(carFreeTime)); // 加上免费时长
        Date newDate = newTime.getTime();
        String finishFreeTime = DateUtil.getFormatTimeString(newDate, DateUtil.DATE_FORMATE_STRING_A);
        
        return finishFreeTime;
    }

    /**
     * 获取访客车辆操作配置
     * 查询访客设置，获取车辆相关的配置信息
     *
     * @param reqJson 请求JSON对象，包含小区ID
     * @return JSONObject 包含车辆操作配置的JSON对象
     */
    public JSONObject getVisitCarOperate(JSONObject reqJson) {
        VisitSettingDto visitSettingDto = new VisitSettingDto();
        visitSettingDto.setCommunityId(reqJson.getString("communityId"));
        List<VisitSettingDto> visitSettingDtos = visitSettingV1InnerServiceSMOImpl.queryVisitSettings(visitSettingDto);
        
        // 如果有配置信息，提取相关配置
        if (visitSettingDtos != null && visitSettingDtos.size() > 0) {
            reqJson.put("ascriptionCarAreaId", visitSettingDtos.get(0).getPaId()); // 停车场ID
            reqJson.put("carFreeTime", visitSettingDtos.get(0).getCarFreeTime()); // 免费时长
            reqJson.put("visitNumber", visitSettingDtos.get(0).getVisitNumber()); // 限制次数
            reqJson.put("isNeedReview", visitSettingDtos.get(0).getIsNeedReview()); // 是否需审核
        }
        
        return reqJson;
    }
}