/*
 * Copyright 2017-2020 吴学文 and java110 team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.java110.community.cmd.parkingSpaceApply;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.core.log.LoggerFactory;
import com.java110.dto.community.CommunityMemberDto;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.dto.parking.ParkingSpaceApplyDto;
import com.java110.intf.community.ICommunityMemberV1InnerServiceSMO;
import com.java110.intf.community.IParkingSpaceApplyV1InnerServiceSMO;
import com.java110.intf.community.IParkingSpaceV1InnerServiceSMO;
import com.java110.intf.fee.IFeeConfigInnerServiceSMO;
import com.java110.intf.fee.IPayFeeV1InnerServiceSMO;
import com.java110.intf.user.IOwnerCarV1InnerServiceSMO;
import com.java110.po.car.OwnerCarPo;
import com.java110.po.fee.PayFeePo;
import com.java110.po.parking.ParkingSpacePo;
import com.java110.po.parking.ParkingSpaceApplyPo;
import com.java110.utils.constant.CommonConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 车位申请审核命令类
 * 
 * 该类负责处理车位申请的审核操作，包括审核通过和审核失败两种情况。
 * 审核通过时，会创建车辆信息、更新车位状态；审核失败时，仅更新申请状态。
 * 
 * 类表述：更新
 * 服务编码：parkingSpaceApply.auditParkingSpaceApply
 * 请求路劲：/app/parkingSpaceApply.UpdateParkingSpaceApply
 * add by 吴学文 at 2021-10-18 13:00:02 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "parkingSpaceApply.auditParkingSpaceApply")
public class AuditParkingSpaceApplyCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(AuditParkingSpaceApplyCmd.class);

    /** ID生成前缀 */
    public static final String CODE_PREFIX_ID = "10";

    /** 车位申请服务接口 */
    @Autowired
    private IParkingSpaceApplyV1InnerServiceSMO parkingSpaceApplyV1InnerServiceSMOImpl;

    /** 业主车辆服务接口 */
    @Autowired
    private IOwnerCarV1InnerServiceSMO ownerCarV1InnerServiceSMOImpl;

    /** 费用服务接口 */
    @Autowired
    private IPayFeeV1InnerServiceSMO payFeeNewV1InnerServiceSMOImpl;

    /** 费用配置服务接口 */
    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    /** 小区成员服务接口 */
    @Autowired
    private ICommunityMemberV1InnerServiceSMO communityMemberV1InnerServiceSMOImpl;

    /** 车位服务接口 */
    @Autowired
    private IParkingSpaceV1InnerServiceSMO parkingSpaceV1InnerServiceSMOImpl;

    /**
     * 参数校验方法
     * 
     * 验证请求参数中是否包含必要的字段
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 校验申请ID和小区ID是否为空
        Assert.hasKeyAndValue(reqJson, "applyId", "applyId不能为空");
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行审核命令方法
     * 
     * 处理车位申请的审核逻辑，包括审核通过和审核失败两种情况
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 获取当前操作用户ID
        String userId = cmdDataFlowContext.getReqHeaders().get(CommonConstant.USER_ID);
        
        // 将请求参数转换为车位申请PO对象
        ParkingSpaceApplyPo parkingSpaceApplyPo = BeanConvertUtil.covertBean(reqJson, ParkingSpaceApplyPo.class);
        
        // 审核失败处理逻辑
        if (ParkingSpaceApplyDto.STATE_FAIL.equals(parkingSpaceApplyPo.getState())) {
            // 清空车位ID和配置ID
            parkingSpaceApplyPo.setPsId("");
            parkingSpaceApplyPo.setConfigId("");
            // 更新申请状态为失败
            int flag = parkingSpaceApplyV1InnerServiceSMOImpl.updateParkingSpaceApply(parkingSpaceApplyPo);
            if (flag < 1) {
                throw new CmdException("更新数据失败");
            }
            return;
        }
        
        // 审核通过处理逻辑
        // 设置费用ID为-1，表示未生成费用
        parkingSpaceApplyPo.setFeeId("-1");
        // 设置申请状态为成功
        parkingSpaceApplyPo.setState(ParkingSpaceApplyDto.STATE_SUCCESS);
        // 更新申请状态
        int flag = parkingSpaceApplyV1InnerServiceSMOImpl.updateParkingSpaceApply(parkingSpaceApplyPo);
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }
        
        // 查询申请详情信息
        ParkingSpaceApplyDto parkingSpaceApplyDto = new ParkingSpaceApplyDto();
        parkingSpaceApplyDto.setApplyId(parkingSpaceApplyPo.getApplyId());
        List<ParkingSpaceApplyDto> parkingSpaceApplyDtos = parkingSpaceApplyV1InnerServiceSMOImpl.queryParkingSpaceApplys(parkingSpaceApplyDto);
        if (ListUtil.isNull(parkingSpaceApplyDtos)) {
            throw new CmdException("未查询到申请单，请联系管理员");
        }
        ParkingSpaceApplyDto parkingSpaceApply = parkingSpaceApplyDtos.get(0);
        
        // 检查车辆是否已存在，不存在则创建车辆信息
        OwnerCarDto ownerCarDto = new OwnerCarDto();
        ownerCarDto.setCarNum(parkingSpaceApplyPo.getCarNum());
        ownerCarDto.setCommunityId(parkingSpaceApplyPo.getCommunityId());
        ownerCarDto.setLeaseType(OwnerCarDto.LEASE_TYPE_MONTH);
        List<OwnerCarDto> ownerCarDtos = ownerCarV1InnerServiceSMOImpl.queryOwnerCars(ownerCarDto);
        
        String catId = "";
        OwnerCarPo ownerCarPo = new OwnerCarPo();
        
        // 如果车辆不存在，创建新的车辆记录
        if (ListUtil.isNull(ownerCarDtos)) {
            ownerCarPo.setCarId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
            ownerCarPo.setOwnerId(parkingSpaceApply.getApplyPersonId());
            ownerCarPo.setbId("-1");
            ownerCarPo.setCarNum(parkingSpaceApply.getCarNum());
            ownerCarPo.setCarColor(parkingSpaceApply.getCarColor());
            ownerCarPo.setCarBrand(parkingSpaceApply.getCarBrand());
            ownerCarPo.setCarType(parkingSpaceApply.getCarType());
            ownerCarPo.setCarTypeCd("1001");
            ownerCarPo.setCommunityId(parkingSpaceApply.getCommunityId());
            ownerCarPo.setMemberId(ownerCarPo.getCarId());
            ownerCarPo.setStartTime(parkingSpaceApply.getStartTime());
            ownerCarPo.setEndTime(parkingSpaceApply.getEndTime());
            ownerCarPo.setPsId(parkingSpaceApply.getPsId());
            ownerCarPo.setState("1001");
            ownerCarPo.setUserId(userId);
            ownerCarPo.setRemark("车位申请，系统自动写入");
            ownerCarPo.setLeaseType(OwnerCarDto.LEASE_TYPE_MONTH);
            
            flag = ownerCarV1InnerServiceSMOImpl.saveOwnerCar(ownerCarPo);
            if (flag < 1) {
                throw new CmdException("更新数据失败");
            }
            catId = ownerCarPo.getCarId();
        } else {
            // 车辆已存在，使用现有车辆ID
            catId = ownerCarDtos.get(0).getCarId();
        }
        
        // 更新车位状态为已出租
        ParkingSpacePo parkingSpacePo = new ParkingSpacePo();
        parkingSpacePo.setPsId(parkingSpaceApply.getPsId());
        parkingSpacePo.setState(ParkingSpaceDto.STATE_HIRE);
        flag = parkingSpaceV1InnerServiceSMOImpl.updateParkingSpace(parkingSpacePo);
        if (flag < 1) {
            throw new CmdException("更新车位状态失败");
        }
        
        // 返回成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}