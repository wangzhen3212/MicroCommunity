package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.owner.OwnerAppUserDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.fee.bmo.IQueryOweFee;
import com.java110.intf.user.IOwnerAppUserInnerServiceSMO;
import com.java110.intf.user.IOwnerAppUserV1InnerServiceSMO;
import com.java110.intf.user.IOwnerV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.List;

/**
 * 查询业主综合缴费（欠费）命令类
 * 该命令用于处理业主欠费查询请求，包括用户认证验证和欠费数据查询
 * 服务码：fee.queryOwnerOweFee
 */
@Java110Cmd(serviceCode = "fee.queryOwnerOweFee")
public class QueryOwnerOweFeeCmd extends Cmd {

    /**
     * 业主应用用户V1版本内部服务
     */
    @Autowired
    private IOwnerAppUserV1InnerServiceSMO ownerAppUserV1InnerServiceSMOImpl;

    /**
     * 业主V1版本内部服务
     */
    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;

    /**
     * 欠费查询业务接口
     */
    @Autowired
    private IQueryOweFee queryOweFeeImpl;

    /**
     * 请求参数验证方法
     * 验证请求参数的有效性，包括小区ID必填、用户认证状态和业主信息验证
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException   命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证必填参数：小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
        // 调用父类分页信息验证
        super.validatePageInfo(reqJson);

        // 从上下文中获取当前用户ID
        String userId = CmdContextUtils.getUserId(context);
        // 构建业主应用用户查询条件
        OwnerAppUserDto ownerAppUserDto = new OwnerAppUserDto();
        ownerAppUserDto.setUserId(userId);
        ownerAppUserDto.setCommunityId(reqJson.getString("communityId"));
        
        // 查询业主应用用户信息
        List<OwnerAppUserDto> ownerAppUserDtos = ownerAppUserV1InnerServiceSMOImpl.queryOwnerAppUsers(ownerAppUserDto);
        
        // 验证用户认证状态
        if (ListUtil.isNull(ownerAppUserDtos)
                || OwnerAppUserDto.STATE_AUDIT_ERROR.equals(ownerAppUserDtos.get(0).getState())) {
            throw new CmdException("用户未认证,请先认证");
        }

        // 检查认证审核状态
        if (OwnerAppUserDto.STATE_AUDITING.equals(ownerAppUserDtos.get(0).getState())) {
            ResultVo resultVo = new ResultVo(-101, "认证审核中");
            context.setResponseEntity(ResultVo.createResponseEntity(resultVo));
            return;
        }

        // 构建业主查询条件
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setMemberId(ownerAppUserDtos.get(0).getMemberId());
        ownerDto.setCommunityId(ownerAppUserDtos.get(0).getCommunityId());
        // 查询业主信息
        List<OwnerDto> ownerDtos = ownerV1InnerServiceSMOImpl.queryOwners(ownerDto);
        
        // 验证业主是否存在
        if (ListUtil.isNull(ownerDtos)) {
            throw new CmdException("业主不存在");
        }

        // 获取第一个业主信息
        ownerDto = ownerDtos.get(0);
        // 将业主ID设置到请求参数中，供后续处理使用
        reqJson.put("ownerId", ownerDto.getOwnerId());
    }

    /**
     * 执行欠费查询命令
     * 处理欠费查询业务逻辑，包括参数转换和欠费数据查询
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException   命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求参数转换为FeeDto对象
        FeeDto feeDto = BeanConvertUtil.covertBean(reqJson, FeeDto.class);

        // 处理缴费对象ID参数
        String payObjId = reqJson.getString("payObjId");
        if (!StringUtil.isEmpty(payObjId)) {
            feeDto.setPayerObjId("");
            // 判断是否为多个缴费对象ID（逗号分隔）
            if (payObjId.contains(",")) {
                feeDto.setPayerObjIds(payObjId.split(","));
            } else {
                feeDto.setPayerObjId(payObjId);
            }
        }
        
        // 处理目标结束时间参数
        String targetEndTime = reqJson.getString("targetEndTime");
        if (!StringUtil.isEmpty(targetEndTime)) {
            // 将日期格式化为当天的最后时刻
            targetEndTime = targetEndTime + " 23:59:59";
            feeDto.setTargetEndTime(targetEndTime);
        }
        
        // 调用欠费查询服务
        ResponseEntity<String> responseEntity = queryOweFeeImpl.query(feeDto);
        // 设置响应结果
        context.setResponseEntity(responseEntity);
    }
}