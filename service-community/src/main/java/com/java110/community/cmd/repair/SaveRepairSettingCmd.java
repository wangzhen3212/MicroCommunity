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
package com.java110.community.cmd.repair;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.community.IRepairSettingV1InnerServiceSMO;
import com.java110.po.repair.RepairSettingPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 类表述：保存报修设置信息
 * 服务编码：repairSetting.saveRepairSetting
 * 请求路劲：/app/repairSetting.SaveRepairSetting
 * add by 吴学文 at 2022-08-06 12:43:34 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
/**
 * 报修设置保存命令类
 * 负责处理报修设置信息的保存业务逻辑，包括参数验证和数据持久化
 * 该类继承自Cmd基类，实现了命令模式的处理流程
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-08-06
 */
@Java110Cmd(serviceCode = "repair.saveRepairSetting")
public class SaveRepairSettingCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveRepairSettingCmd.class);

    /**
     * ID生成前缀，用于生成报修设置ID
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 报修设置服务接口，用于数据持久化操作
     */
    @Autowired
    private IRepairSettingV1InnerServiceSMO repairSettingV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求报文中的必填字段是否完整
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求报文JSON对象，包含前端传递的参数
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证报修类型名称参数
        Assert.hasKeyAndValue(reqJson, "repairTypeName", "请求报文中未包含repairTypeName");
        // 验证报修方式参数
        Assert.hasKeyAndValue(reqJson, "repairWay", "请求报文中未包含repairWay");
        // 验证小区ID参数
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        // 验证公共区域参数
        Assert.hasKeyAndValue(reqJson, "publicArea", "请求报文中未包含公共区域");
        // 验证收费标志参数
        Assert.hasKeyAndValue(reqJson, "payFeeFlag", "请求报文中未包含收费情况");
        // 验证回访标志参数
        Assert.hasKeyAndValue(reqJson, "returnVisitFlag", "请求报文中未包含回访设置");
        // 验证办理时长参数
        Assert.hasKeyAndValue(reqJson, "doTime", "请求报文中未包含办理时长");
        // 验证预警时间参数
        Assert.hasKeyAndValue(reqJson, "warningTime", "请求报文中未包含超时预警");
    }

    /**
     * 执行保存报修设置命令
     * 将请求参数转换为实体对象并保存到数据库
     * 使用事务注解确保数据一致性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求报文JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为报修设置实体对象
        RepairSettingPo repairSettingPo = BeanConvertUtil.covertBean(reqJson, RepairSettingPo.class);
        
        // 生成报修设置ID
        repairSettingPo.setSettingId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        // 设置报修类型，使用设置ID作为类型标识
        repairSettingPo.setRepairType(repairSettingPo.getSettingId());
        
        // 调用服务层保存报修设置信息
        int flag = repairSettingV1InnerServiceSMOImpl.saveRepairSetting(repairSettingPo);

        // 检查保存结果，如果影响行数小于1则认为保存失败
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}