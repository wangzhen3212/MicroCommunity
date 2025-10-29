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
import com.java110.core.log.LoggerFactory;
import com.java110.dto.fee.PayFeeDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.parking.ParkingSpaceApplyDto;
import com.java110.intf.community.ICommunityMemberV1InnerServiceSMO;
import com.java110.intf.community.IParkingSpaceApplyV1InnerServiceSMO;
import com.java110.intf.community.IParkingSpaceV1InnerServiceSMO;
import com.java110.intf.fee.IFeeConfigInnerServiceSMO;
import com.java110.intf.fee.IPayFeeV1InnerServiceSMO;
import com.java110.intf.user.IOwnerCarV1InnerServiceSMO;
import com.java110.po.car.OwnerCarPo;
import com.java110.po.fee.PayFeePo;
import com.java110.po.parking.ParkingSpaceApplyPo;
import com.java110.utils.constant.CommonConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;


/**
 * 类表述：更新
 * 服务编码：parkingSpaceApply.updateParkingSpaceApply
 * 请求路劲：/app/parkingSpaceApply.UpdateParkingSpaceApply
 * add by 吴学文 at 2021-10-18 13:00:02 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 停车位申请更新命令类
 * 
 * 该类负责处理停车位申请信息的更新操作，包括更新申请信息、关联车辆信息和费用信息
 * 主要用于审批停车位申请后的数据更新处理
 * 
 * @author 吴学文
 * @version 1.0
 * @date 2021-10-18
 */
@Java110Cmd(serviceCode = "parkingSpaceApply.updateParkingSpaceApply")
public class UpdateParkingSpaceApplyCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdateParkingSpaceApplyCmd.class);

    public static final String CODE_PREFIX_ID = "10";
    
    @Autowired
    private IParkingSpaceApplyV1InnerServiceSMO parkingSpaceApplyV1InnerServiceSMOImpl;
    
    @Autowired
    private IOwnerCarV1InnerServiceSMO ownerCarV1InnerServiceSMOImpl;
    
    @Autowired
    private IPayFeeV1InnerServiceSMO payFeeV1InnerServiceSMOImpl;
    
    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;
    
    @Autowired
    private ICommunityMemberV1InnerServiceSMO communityMemberV1InnerServiceSMOImpl;

    @Autowired
    private IParkingSpaceV1InnerServiceSMO parkingSpaceV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的applyId字段
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws IllegalArgumentException 当applyId参数缺失时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含applyId字段
        Assert.hasKeyAndValue(reqJson, "applyId", "applyId不能为空");
    }

    /**
     * 执行停车位申请更新命令
     * 
     * 该方法主要完成以下功能：
     * 1. 更新停车位申请信息
     * 2. 更新关联的车辆信息
     * 3. 更新相关的费用信息
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当更新操作失败时抛出
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 获取当前操作用户ID
        String userId = cmdDataFlowContext.getReqHeaders().get(CommonConstant.USER_ID);
        
        // 将请求JSON数据转换为停车位申请PO对象
        ParkingSpaceApplyPo parkingSpaceApplyPo = BeanConvertUtil.covertBean(reqJson, ParkingSpaceApplyPo.class);

        // 更新停车位申请信息
        int flag = parkingSpaceApplyV1InnerServiceSMOImpl.updateParkingSpaceApply(parkingSpaceApplyPo);

        // 检查更新是否成功
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        // 查询更新后的停车位申请信息
        ParkingSpaceApplyDto parkingSpaceApplyDto = new ParkingSpaceApplyDto();
        parkingSpaceApplyDto.setApplyId(parkingSpaceApplyPo.getApplyId());
        List<ParkingSpaceApplyDto> parkingSpaceApplyDtos = parkingSpaceApplyV1InnerServiceSMOImpl.queryParkingSpaceApplys(parkingSpaceApplyDto);

        // 验证查询结果
        if (parkingSpaceApplyDtos == null || parkingSpaceApplyDtos.size() < 1) {
            throw new CmdException("未查询到申请单，请联系管理员");
        }
        
        // 获取第一个申请记录
        ParkingSpaceApplyDto parkingSpaceApply = parkingSpaceApplyDtos.get(0);

        // 处理车辆信息更新逻辑
        // 2、审核的时，判断车辆是否在owner_car中有，有就跳过。  没有的话写入owner_car，--都要写入pay_fee。
        OwnerCarDto ownerCarDto = new OwnerCarDto();
        ownerCarDto.setCarNum(parkingSpaceApplyPo.getCarNum());
        ownerCarDto.setCommunityId(parkingSpaceApplyPo.getCommunityId());
        ownerCarDto.setLeaseType(OwnerCarDto.LEASE_TYPE_MONTH);
        
        // 查询车辆信息
        List<OwnerCarDto> ownerCarDtos = ownerCarV1InnerServiceSMOImpl.queryOwnerCars(ownerCarDto);
        String catId = "";
        
        // 如果车辆存在，则更新车辆信息
        if (ownerCarDtos == null || ownerCarDtos.size() < 1) {
            return; // 车辆不存在，无需更新
        } else {
            // 获取车辆ID
            catId = ownerCarDtos.get(0).getCarId();
            OwnerCarPo ownerCarPo = new OwnerCarPo();
            ownerCarPo.setCarId(catId);
            // 设置车辆使用时间范围
            ownerCarPo.setStartTime(parkingSpaceApply.getStartTime());
            ownerCarPo.setEndTime(parkingSpaceApply.getEndTime());
            
            // 更新车辆信息
            flag = ownerCarV1InnerServiceSMOImpl.updateOwnerCar(ownerCarPo);
            if (flag < 1) {
                throw new CmdException("更新数据失败");
            }
        }
        
        // 处理费用信息更新逻辑
        PayFeeDto feeDto = new PayFeeDto();
        feeDto.setFeeId(parkingSpaceApply.getFeeId());
        feeDto.setCommunityId(parkingSpaceApply.getCommunityId());
        feeDto.setConfigId(parkingSpaceApply.getConfigId());
        
        // 查询费用信息
        List<PayFeeDto> payFeeDtos =  payFeeV1InnerServiceSMOImpl.queryPayFees(feeDto);
        
        // 如果费用信息不存在，则直接返回
        if(payFeeDtos == null || payFeeDtos.size() <1){
            return ;
        }
        
        // 更新费用信息
        PayFeePo payFeePo = new PayFeePo();
        payFeePo.setCommunityId(payFeeDtos.get(0).getCommunityId());
        payFeePo.setFeeId(payFeeDtos.get(0).getFeeId());
        // 设置费用结束时间
        payFeePo.setEndTime(parkingSpaceApply.getStartTime());

        // 执行费用更新操作
        flag = payFeeV1InnerServiceSMOImpl.updatePayFee(payFeePo);
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }
        
        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}