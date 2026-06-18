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
package com.java110.fee.cmd.meterWater;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.meter.MeterWaterDto;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.intf.fee.IMeterWaterInnerServiceSMO;
import com.java110.intf.fee.IMeterWaterV1InnerServiceSMO;
import com.java110.intf.fee.IPayFeeV1InnerServiceSMO;
import com.java110.po.fee.PayFeePo;
import com.java110.po.meter.MeterWaterPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 类表述：删除水表读数记录命令类
 * 服务编码：meterWater.deleteMeterWater
 * 请求路劲：/app/meterWater.DeleteMeterWater
 * add by 吴学文 at 2022-07-21 09:17:10 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * 功能描述：该命令类用于处理删除水表读数记录的请求，包含删除关联的费用信息
 * 主要职责：
 * 1. 验证删除水表读数记录的必要参数
 * 2. 删除与水表读数关联的费用信息
 * 3. 删除水表读数记录本身
 * 4. 返回操作结果
 */
@Java110Cmd(serviceCode = "meterWater.deleteMeterWater")
public class DeleteMeterWaterCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteMeterWaterCmd.class);

    /**
     * 水表读数V1版本服务接口
     */
    @Autowired
    private IMeterWaterV1InnerServiceSMO meterWaterV1InnerServiceSMOImpl;

    /**
     * 水表读数服务接口
     */
    @Autowired
    private IMeterWaterInnerServiceSMO meterWaterInnerServiceSMOImpl;

    /**
     * 费用服务接口
     */
    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    /**
     * 支付费用V1版本服务接口
     */
    @Autowired
    private IPayFeeV1InnerServiceSMO payFeeV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证删除水表读数记录所需的必要参数是否完整
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含waterId
        Assert.hasKeyAndValue(reqJson, "waterId", "waterId不能为空");
        // 验证请求参数中必须包含communityId
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行删除水表读数记录命令
     * 该方法会先删除与水表读数关联的费用信息，然后再删除水表读数记录本身
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 构建查询条件，查找要删除的水表读数记录
        MeterWaterDto meterWaterDto = new MeterWaterDto();
        meterWaterDto.setWaterId(reqJson.getString("waterId"));
        meterWaterDto.setCommunityId(reqJson.getString("communityId"));
        List<MeterWaterDto> meterWaterDtos = meterWaterInnerServiceSMOImpl.queryMeterWaters(meterWaterDto);

        // 验证查询结果，确保只找到一条记录
        Assert.listOnlyOne(meterWaterDtos, "数据异常未找到费用信息");
        
        int flag = 0;
        
        // 判断费用是否已经被删除，如果存在关联费用则先删除费用
        FeeDto feeDto = new FeeDto();
        feeDto.setFeeId(meterWaterDtos.get(0).getFeeId());
        feeDto.setCommunityId(meterWaterDtos.get(0).getCommunityId());
        List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);
        
        // 如果存在关联的费用记录，则先删除费用信息
        if (feeDtos != null && feeDtos.size() > 0) {
            PayFeePo payFeePo = new PayFeePo();
            payFeePo.setFeeId(meterWaterDtos.get(0).getFeeId());
            payFeePo.setCommunityId(meterWaterDtos.get(0).getCommunityId());
            // 删除关联的费用记录
            flag = payFeeV1InnerServiceSMOImpl.deletePayFee(payFeePo);
            if (flag < 1) {
                throw new CmdException("删除数据失败");
            }
        }

        // 将请求参数转换为水表读数PO对象
        MeterWaterPo meterWaterPo = BeanConvertUtil.covertBean(reqJson, MeterWaterPo.class);
        // 删除水表读数记录
        flag = meterWaterV1InnerServiceSMOImpl.deleteMeterWater(meterWaterPo);

        // 检查删除操作是否成功
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}