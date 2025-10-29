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
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.floorShareMeter.FloorShareMeterDto;
import com.java110.dto.floorShareReading.FloorShareReadingDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.fee.IFloorShareMeterV1InnerServiceSMO;
import com.java110.intf.fee.IFloorShareReadingV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.floorShareMeter.FloorShareMeterPo;
import com.java110.po.floorShareReading.FloorShareReadingPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 类表述：保存
 * 服务编码：floorShareReading.saveFloorShareReading
 * 请求路劲：/app/floorShareReading.SaveFloorShareReading
 * add by 吴学文 at 2025-03-26 09:29:54 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 公摊表抄表数据保存命令类
 * 负责处理公摊表抄表数据的保存业务逻辑，包括数据验证、抄表记录保存和表计信息更新
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2025-03-26
 */
@Java110Cmd(serviceCode = "meter.saveFloorShareReading")
public class SaveFloorShareReadingCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveFloorShareReadingCmd.class);

    /** 抄表记录ID前缀 */
    public static final String CODE_PREFIX_ID = "10";

    /** 公摊抄表记录服务接口 */
    @Autowired
    private IFloorShareReadingV1InnerServiceSMO floorShareReadingV1InnerServiceSMOImpl;

    /** 公摊表计服务接口 */
    @Autowired
    private IFloorShareMeterV1InnerServiceSMO floorShareMeterV1InnerServiceSMOImpl;

    /** 用户服务接口 */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 验证请求参数的有效性
     * 检查必要的参数是否存在且不为空
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必要参数是否存在
        Assert.hasKeyAndValue(reqJson, "fsmId", "请求报文中未包含fsmId");
        Assert.hasKeyAndValue(reqJson, "preDegrees", "请求报文中未包含preDegrees");
        Assert.hasKeyAndValue(reqJson, "curDegrees", "请求报文中未包含curDegrees");
        Assert.hasKeyAndValue(reqJson, "preReadingTime", "请求报文中未包含preReadingTime");
        Assert.hasKeyAndValue(reqJson, "curReadingTime", "请求报文中未包含curReadingTime");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        
        // 调用父类的属性验证方法
        super.validateProperty(cmdDataFlowContext);
    }

    /**
     * 执行公摊表抄表数据保存命令
     * 主要流程：验证用户信息 -> 验证表计信息 -> 保存抄表记录 -> 更新表计当前读数
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当业务处理失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 从上下文中获取当前用户ID
        String userId = CmdContextUtils.getUserId(cmdDataFlowContext);

        // 查询用户信息
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);
        // 验证用户是否存在且唯一
        Assert.listOnlyOne(userDtos, "用户不存在");

        // 验证公摊表计信息
        FloorShareMeterDto floorShareMeterDto = new FloorShareMeterDto();
        floorShareMeterDto.setFsmId(reqJson.getString("fsmId"));
        floorShareMeterDto.setCommunityId(reqJson.getString("communityId"));
        List<FloorShareMeterDto> floorShareMeterDtos = floorShareMeterV1InnerServiceSMOImpl.queryFloorShareMeters(floorShareMeterDto);
        // 检查表计是否存在
        if (ListUtil.isNull(floorShareMeterDtos)) {
            throw new CmdException("未包含公摊表");
        }

        // 创建抄表记录对象并设置属性
        FloorShareReadingPo floorShareReadingPo = BeanConvertUtil.covertBean(reqJson, FloorShareReadingPo.class);
        floorShareReadingPo.setReadingId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID)); // 生成抄表记录ID
        floorShareReadingPo.setCreateStaffName(userDtos.get(0).getName()); // 设置创建人姓名
        floorShareReadingPo.setState(FloorShareReadingDto.STATE_W); // 设置状态为待处理
        // 生成抄表标题，格式：用户名+提交+栋号+表类型+抄表
        floorShareReadingPo.setTitle(userDtos.get(0).getName() + "提交" + floorShareMeterDtos.get(0).getFloorNum() + "栋"+floorShareMeterDtos.get(0).getMeterTypeName()+"抄表");
        
        // 保存抄表记录
        int flag = floorShareReadingV1InnerServiceSMOImpl.saveFloorShareReading(floorShareReadingPo);

        // 检查保存是否成功
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 更新公摊表计的当前度数和抄表时间
        FloorShareMeterPo floorShareMeterPo = new FloorShareMeterPo();
        floorShareMeterPo.setFsmId(floorShareMeterDtos.get(0).getFsmId()); // 设置表计ID
        floorShareMeterPo.setCurDegree(reqJson.getString("curDegrees")); // 设置当前度数
        floorShareMeterPo.setCurReadingTime(reqJson.getString("curReadingTime")); // 设置当前抄表时间
        floorShareMeterV1InnerServiceSMOImpl.updateFloorShareMeter(floorShareMeterPo);

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}