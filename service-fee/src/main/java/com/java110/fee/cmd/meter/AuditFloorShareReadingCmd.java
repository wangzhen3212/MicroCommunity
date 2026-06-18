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
package com.java110.fee.cmd.meter;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.data.DatabusDataDto;
import com.java110.dto.floorShareReading.FloorShareReadingDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.fee.IFloorShareMeterV1InnerServiceSMO;
import com.java110.intf.fee.IFloorShareReadingV1InnerServiceSMO;
import com.java110.intf.job.IDataBusInnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.floorShareMeter.FloorShareMeterPo;
import com.java110.po.floorShareReading.FloorShareReadingPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


/**
 * 类表述：更新
 * 服务编码：floorShareReading.updateFloorShareReading
 * 请求路劲：/app/floorShareReading.UpdateFloorShareReading
 * add by 吴学文 at 2025-03-26 09:29:54 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 公摊读数审核命令类
 * 负责处理公摊读数的审核操作，包括审核状态的更新和相关业务逻辑处理
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2025-03-26
 */
@Java110Cmd(serviceCode = "meter.auditFloorShareReading")
public class AuditFloorShareReadingCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(AuditFloorShareReadingCmd.class);

    /**
     * 公摊读数服务接口
     */
    @Autowired
    private IFloorShareReadingV1InnerServiceSMO floorShareReadingV1InnerServiceSMOImpl;

    /**
     * 公摊仪表服务接口
     */
    @Autowired
    private IFloorShareMeterV1InnerServiceSMO floorShareMeterV1InnerServiceSMOImpl;

    /**
     * 数据总线服务接口
     */
    @Autowired
    private IDataBusInnerServiceSMO dataBusInnerServiceSMOImpl;

    /**
     * 用户服务接口
     */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填参数是否存在
        Assert.hasKeyAndValue(reqJson, "readingId", "readingId不能为空");
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
        Assert.hasKeyAndValue(reqJson, "state", "审核状态不能为空");
        // 调用父类的属性验证方法
        super.validateProperty(cmdDataFlowContext);
    }

    /**
     * 执行审核命令
     * 处理公摊读数的审核逻辑，包括状态更新、审核信息记录和后续业务处理
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当业务处理失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 获取当前操作用户ID
        String userId = CmdContextUtils.getUserId(cmdDataFlowContext);
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        // 查询用户信息
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);
        Assert.listOnlyOne(userDtos, "用户不存在");

        // 根据readingId和communityId查询公摊读数信息
        FloorShareReadingDto floorShareReadingDto = new FloorShareReadingDto();
        floorShareReadingDto.setReadingId(reqJson.getString("readingId"));
        floorShareReadingDto.setCommunityId(reqJson.getString("communityId"));
        List<FloorShareReadingDto> floorShareReadingDtos
                = floorShareReadingV1InnerServiceSMOImpl.queryFloorShareReadings(floorShareReadingDto);
        // 验证记录是否存在
        if (ListUtil.isNull(floorShareReadingDtos)) {
            throw new CmdException("记录不存在");
        }
        String state = reqJson.getString("state");

        // 构建公摊读数更新对象
        FloorShareReadingPo floorShareReadingPo = new FloorShareReadingPo();
        floorShareReadingPo.setReadingId(reqJson.getString("readingId"));
        floorShareReadingPo.setState(state);
        // 设置审核人员姓名
        floorShareReadingPo.setAuditStaffName(userDtos.get(0).getName());
        // 设置审核意见
        floorShareReadingPo.setStateMsg("审核意见：" + reqJson.getString("auditRemark"));
        // 如果审核状态为通过，设置公摊开始信息
        if (FloorShareReadingDto.STATE_C.equals(state)) {
            floorShareReadingPo.setShareMsg("开始公摊");
        }
        // 更新公摊读数状态
        int flag = floorShareReadingV1InnerServiceSMOImpl.updateFloorShareReading(floorShareReadingPo);

        // 验证更新是否成功
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }
        // 设置响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.success());

        // 如果审核状态为拒绝，需要回退仪表读数
        if (FloorShareReadingDto.STATE_F.equals(state)) {
            FloorShareMeterPo floorShareMeterPo = new FloorShareMeterPo();
            // 设置仪表ID
            floorShareMeterPo.setFsmId(floorShareReadingDtos.get(0).getFsmId());
            // 将当前读数时间回退到上次读数时间
            floorShareMeterPo.setCurReadingTime(floorShareReadingDtos.get(0).getPreReadingTime());
            // 将当前度数回退到上次度数
            floorShareMeterPo.setCurDegree(floorShareReadingDtos.get(0).getPreDegrees());
            floorShareMeterPo.setCommunityId(floorShareReadingDtos.get(0).getCommunityId());
            // 更新仪表信息
            floorShareMeterV1InnerServiceSMOImpl.updateFloorShareMeter(floorShareMeterPo);
            return ;
        }

        // 将用户ID添加到请求参数中
        reqJson.put("staffId",userId);
        // 通过数据总线处理公摊读数业务
        dataBusInnerServiceSMOImpl.databusData(new DatabusDataDto(DatabusDataDto.BUSINESS_TYPE_SHARE_READING, reqJson));


    }
}