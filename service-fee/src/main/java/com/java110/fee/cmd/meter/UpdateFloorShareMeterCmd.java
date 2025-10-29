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
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.intf.fee.IFeeConfigInnerServiceSMO;
import com.java110.intf.fee.IFloorShareMeterV1InnerServiceSMO;
import com.java110.po.floorShareMeter.FloorShareMeterPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


/**
 * 类表述：更新
 * 服务编码：floorShareMeter.updateFloorShareMeter
 * 请求路劲：/app/floorShareMeter.UpdateFloorShareMeter
 * add by 吴学文 at 2025-03-25 15:01:11 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 楼层公摊电表更新命令类
 * 
 * 该类负责处理更新楼层公摊电表信息的业务逻辑，包括参数验证和数据库更新操作
 * 通过注解标识服务编码，与前端请求路径对应
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2025-03-25
 */
@Java110Cmd(serviceCode = "meter.updateFloorShareMeter")
public class UpdateFloorShareMeterCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdateFloorShareMeterCmd.class);

    /**
     * 楼层公摊电表服务接口
     */
    @Autowired
    private IFloorShareMeterV1InnerServiceSMO floorShareMeterV1InnerServiceSMOImpl;

    /**
     * 费用配置服务接口
     */
    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整和有效，确保必要的字段都存在且不为空
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填参数：公摊电表ID
        Assert.hasKeyAndValue(reqJson, "fsmId", "fsmId不能为空");
        // 验证必填参数：小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
        // 验证必填参数：费用配置ID
        Assert.hasKeyAndValue(reqJson, "configId", "请求报文中未包含费用项");
        // 验证必填参数：公摊单价
        Assert.hasKeyAndValue(reqJson, "sharePrice", "请求报文中未包含公摊单价");
        // 调用父类的属性验证方法
        super.validateProperty(cmdDataFlowContext);
    }

    /**
     * 执行更新命令方法
     * 
     * 处理楼层公摊电表的更新操作，包括费用项验证和数据更新
     * 使用事务注解确保操作的原子性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当费用项不存在或更新失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 创建费用配置查询对象
        FeeConfigDto feeConfigDto = new FeeConfigDto();
        // 设置费用配置ID
        feeConfigDto.setConfigId(reqJson.getString("configId"));
        // 查询费用配置信息
        List<FeeConfigDto> feeConfigDtos = feeConfigInnerServiceSMOImpl.queryFeeConfigs(feeConfigDto);
        // 验证费用配置存在且唯一
        Assert.listOnlyOne(feeConfigDtos, "未包含费用项");

        // 将请求JSON转换为楼层公摊电表PO对象
        FloorShareMeterPo floorShareMeterPo = BeanConvertUtil.covertBean(reqJson, FloorShareMeterPo.class);
        // 设置费用项名称
        floorShareMeterPo.setConfigName(feeConfigDtos.get(0).getFeeName());
        // 执行更新操作
        int flag = floorShareMeterV1InnerServiceSMOImpl.updateFloorShareMeter(floorShareMeterPo);

        // 检查更新结果
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}