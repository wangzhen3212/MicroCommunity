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
package com.java110.fee.cmd.feeSub;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.fee.FeeAttrDto;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.dto.fee.FeeDto;
import com.java110.intf.fee.*;
import com.java110.po.fee.FeeAttrPo;
import com.java110.po.fee.PayFeePo;
import com.java110.po.payFee.PayFeeSubPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 费用拆分命令类
 * 
 * 该类负责处理费用拆分业务逻辑，将原有的周期性费用按照指定时间点拆分成两段独立的费用记录。
 * 主要功能包括：参数验证、费用拆分、原费用状态更新等。
 * 
 * 服务编码：feeSub.splitPayFee
 * 请求路径：/app/feeSub.splitPayFee
 * 
 * @author 吴学文 at 2024-01-05 12:10:47 mail: 928255095@qq.com
 * @version 1.0
 * @see Cmd
 */
@Java110Cmd(serviceCode = "feeSub.splitPayFee")
public class SplitPayFeeCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SplitPayFeeCmd.class);

    /** ID生成前缀 */
    public static final String CODE_PREFIX_ID = "10";

    /** 费用子项服务接口 */
    @Autowired
    private IPayFeeSubV1InnerServiceSMO payFeeSubV1InnerServiceSMOImpl;

    /** 费用服务接口 */
    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    /** 支付费用服务接口 */
    @Autowired
    private IPayFeeV1InnerServiceSMO payFeeV1InnerServiceSMOImpl;

    /** 费用属性服务接口 */
    @Autowired
    private IFeeAttrInnerServiceSMO feeAttrInnerServiceSMOImpl;

    /** 费用配置服务接口 */
    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，包括：
     * 1. 必填参数检查
     * 2. 费用存在性检查
     * 3. 费用状态检查
     * 4. 费用类型检查
     * 5. 拆分时间有效性检查
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 检查必填参数
        Assert.hasKeyAndValue(reqJson, "preFeeId", "请求报文中未包含preFeeId");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.hasKeyAndValue(reqJson, "splitTime", "请求报文中未包含splitTime");
        Assert.hasKeyAndValue(reqJson, "remark", "请求报文中未包含remark");

        // 根据费用ID查询费用信息
        FeeDto feeDto = new FeeDto();
        feeDto.setFeeId(reqJson.getString("preFeeId"));
        feeDto.setCommunityId(reqJson.getString("communityId"));

        List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);

        // 验证费用存在且唯一
        Assert.listOnlyOne(feeDtos, "费用不存在");

        // 检查费用状态，只有进行中的费用才能拆分
        if (!FeeDto.STATE_DOING.equals(feeDtos.get(0).getState())) {
            throw new CmdException("费用已结束不能拆分");
        }

        // 查询费用配置信息
        FeeConfigDto feeConfigDto = new FeeConfigDto();
        feeConfigDto.setConfigId(feeDtos.get(0).getConfigId());
        feeConfigDto.setCommunityId(reqJson.getString("communityId"));

        List<FeeConfigDto> feeConfigDtos = feeConfigInnerServiceSMOImpl.queryFeeConfigs(feeConfigDto);

        // 验证费用配置存在且唯一
        Assert.listOnlyOne(feeConfigDtos, "费用项不存在");

        // 检查费用类型，只有周期性费用才能拆分
        if (!FeeDto.FEE_FLAG_CYCLE.equals(feeConfigDtos.get(0).getFeeFlag())) {
            throw new CmdException("只有周期性费用才能做拆分");
        }

        // 检查费用属性是否为空
        if (ListUtil.isNull(feeDtos.get(0).getFeeAttrDtos())) {
            throw new CmdException("费用属性为空");
        }

        // 获取费用的结束时间和最大结束时间
        Date endTime = feeDtos.get(0).getEndTime();
        Date maxTime = feeDtos.get(0).getDeadlineTime();

        // 验证最大结束时间是否存在
        if (maxTime == null) {
            throw new CmdException("费用错误未包含最大结束时间");
        }

        // 解析拆分时间并验证其有效性
        Date splitTime = DateUtil.getDateFromStringB(reqJson.getString("splitTime"));
        
        // 拆分时间必须大于计费起始时间
        if (splitTime.before(endTime)) {
            throw new CmdException("拆分时间错误，应大于计费起始时间");
        }

        // 拆分时间必须小于最大结束时间
        if (splitTime.after(maxTime)) {
            throw new CmdException("拆分时间错误，应小于最大结束时间");
        }

        // 格式化时间字符串用于比较
        String startDate = DateUtil.getFormatTimeStringB(endTime);
        String deadlineTime = DateUtil.getFormatTimeStringB(maxTime);

        // 拆分时间不能等于开始时间或结束时间
        if (splitTime.getTime() == DateUtil.getDateFromStringB(startDate).getTime()
                || splitTime.getTime() == DateUtil.getDateFromStringB(deadlineTime).getTime()) {
            throw new CmdException("拆分时间不能为 开始时间或者结束时间");
        }

        // 将费用对象存入请求参数中，供后续处理使用
        reqJson.put("feeDto", feeDtos.get(0));
    }

    /**
     * 执行费用拆分命令
     * 
     * 将原费用按照拆分时间点拆分成两段独立的费用记录，并更新原费用状态为已完成。
     * 该方法使用事务注解确保数据一致性。
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当费用拆分或状态更新失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 从请求参数中获取费用对象
        FeeDto feeDto = (FeeDto) reqJson.get("feeDto");

        // 保存前半段费用：从原费用开始时间到拆分时间
        saveSubFee(feeDto, DateUtil.getFormatTimeStringA(feeDto.getEndTime()), reqJson.getString("splitTime"));

        // 保存后半段费用：从拆分时间到原费用最大结束时间
        saveSubFee(feeDto, reqJson.getString("splitTime"), DateUtil.getFormatTimeStringA(feeDto.getDeadlineTime()));

        // 更新原费用状态为已完成
        PayFeePo payFeePo = new PayFeePo();
        payFeePo.setFeeId(feeDto.getFeeId());
        payFeePo.setState(FeeDto.STATE_FINISH);
        payFeePo.setCommunityId(feeDto.getCommunityId());
        int flag = payFeeV1InnerServiceSMOImpl.updatePayFee(payFeePo);

        // 检查状态更新是否成功
        if (flag < 1) {
            throw new CmdException("结束费用失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }

    /**
     * 保存拆分后的费用子项
     * 
     * 根据指定的时间范围创建新的费用子项记录，包括：
     * 1. 创建费用子项记录
     * 2. 创建对应的费用记录
     * 3. 复制原费用的属性信息
     * 
     * @param feeDto 原费用对象
     * @param endTime 新费用的计费起始时间
     * @param maxTime 新费用的最大结束时间
     * @throws CmdException 当保存数据失败时抛出异常
     */
    private void saveSubFee(FeeDto feeDto, String endTime, String maxTime) {
        // 创建费用子项对象并复制属性
        PayFeeSubPo payFeeSubPo = BeanConvertUtil.covertBean(feeDto, PayFeeSubPo.class);
        payFeeSubPo.setEndTime(endTime);
        payFeeSubPo.setMaxTime(maxTime);
        // 生成新的费用ID
        payFeeSubPo.setFeeId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_feeId));
        payFeeSubPo.setPayerObjName(feeDto.getPayerObjName());
        payFeeSubPo.setPreFeeId(feeDto.getFeeId());
        
        // 保存费用子项记录
        int flag = payFeeSubV1InnerServiceSMOImpl.savePayFeeSub(payFeeSubPo);
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 创建费用记录并设置状态为进行中
        PayFeePo payFeePo = BeanConvertUtil.covertBean(feeDto, PayFeePo.class);
        payFeePo.setEndTime(endTime);
        payFeePo.setFeeId(payFeeSubPo.getFeeId());
        payFeePo.setState(FeeDto.STATE_DOING);
        
        // 保存费用记录
        flag = payFeeV1InnerServiceSMOImpl.savePayFee(payFeePo);
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 获取原费用的属性列表
        List<FeeAttrDto> feeAttrDtos = feeDto.getFeeAttrDtos();

        // 如果属性列表为空，则直接返回
        if (ListUtil.isNull(feeAttrDtos)) {
            return;
        }
        
        // 复制费用属性到新的费用
        List<FeeAttrPo> feeAttrPos = new ArrayList<>();
        FeeAttrPo feeAttrPo = null;
        for (FeeAttrDto feeAttrDto : feeAttrDtos) {
            feeAttrPo = BeanConvertUtil.covertBean(feeAttrDto, FeeAttrPo.class);
            feeAttrPo.setFeeId(payFeePo.getFeeId());
            // 生成新的属性ID
            feeAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_attrId));
            
            // 如果是费用截止时间属性，更新为新的最大结束时间
            if (FeeAttrDto.SPEC_CD_ONCE_FEE_DEADLINE_TIME.equals(feeAttrPo.getSpecCd())) {
                feeAttrPo.setValue(payFeeSubPo.getMaxTime());
            }
            feeAttrPos.add(feeAttrPo);
        }

        // 批量保存费用属性
        feeAttrInnerServiceSMOImpl.saveFeeAttrs(feeAttrPos);
    }
}