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
import com.java110.dto.repair.RepairTypeUserDto;
import com.java110.intf.community.IRepairTypeUserInnerServiceSMO;
import com.java110.intf.community.IRepairTypeUserV1InnerServiceSMO;
import com.java110.po.repair.RepairTypeUserPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 类表述：保存维修类型用户关系
 * 服务编码：repairTypeUser.saveRepairTypeUser
 * 请求路劲：/app/repairTypeUser.SaveRepairTypeUser
 * add by 吴学文 at 2022-08-06 12:27:40 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
/**
 * 维修类型用户关系保存命令类
 * 负责处理维修师傅与维修类型的关联关系保存操作
 * 包括数据验证、业务逻辑处理和事务管理
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-08-06
 */
@Java110Cmd(serviceCode = "repair.saveRepairTypeUser")
public class SaveRepairTypeUserCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveRepairTypeUserCmd.class);

    /**
     * 类型用户ID前缀，用于生成唯一标识
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 维修类型用户V1版本服务接口
     */
    @Autowired
    private IRepairTypeUserV1InnerServiceSMO repairTypeUserV1InnerServiceSMOImpl;

    /**
     * 维修类型用户服务接口
     */
    @Autowired
    private IRepairTypeUserInnerServiceSMO repairTypeUserInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 验证请求报文中的必要参数，并检查维修师傅是否已存在
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {

        // 验证请求报文中必须包含staffId参数
        Assert.hasKeyAndValue(reqJson, "staffId", "请求报文中未包含staffId");
        // 验证请求报文中必须包含staffName参数
        Assert.hasKeyAndValue(reqJson, "staffName", "请求报文中未包含staffName");
        // 验证请求报文中必须包含repairType参数
        Assert.hasKeyAndValue(reqJson, "repairType", "请求报文中未包含repairType");
        // 验证请求报文中必须包含communityId参数
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        
        // 创建查询条件对象
        RepairTypeUserDto repairTypeUserDto = new RepairTypeUserDto();
        repairTypeUserDto.setRepairType(reqJson.getString("repairType"));
        repairTypeUserDto.setStaffId(reqJson.getString("staffId"));
        repairTypeUserDto.setCommunityId(reqJson.getString("communityId"));
        
        // 查询是否已存在相同的维修师傅记录
        List<RepairTypeUserDto> repairTypeUserDtos = repairTypeUserInnerServiceSMOImpl.queryRepairTypeUsers(repairTypeUserDto);
        
        // 如果已存在相同记录，则抛出异常，防止重复添加
        Assert.listIsNull(repairTypeUserDtos, "维修师傅已存在，不能重复添加！");
    }

    /**
     * 执行保存维修类型用户关系的核心命令方法
     * 该方法使用事务注解确保数据一致性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 设置维修师傅状态为在线状态
        reqJson.put("state", RepairTypeUserDto.STATE_ONLINE);
        
        // 将请求JSON对象转换为持久化对象
        RepairTypeUserPo repairTypeUserPo = BeanConvertUtil.covertBean(reqJson, RepairTypeUserPo.class);
        
        // 生成唯一的类型用户ID
        repairTypeUserPo.setTypeUserId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务保存维修类型用户关系
        int flag = repairTypeUserV1InnerServiceSMOImpl.saveRepairTypeUser(repairTypeUserPo);

        // 检查保存操作是否成功，flag小于1表示保存失败
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}