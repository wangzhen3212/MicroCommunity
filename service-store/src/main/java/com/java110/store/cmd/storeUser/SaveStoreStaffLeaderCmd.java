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
package com.java110.store.cmd.storeUser;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.user.UserDto;
import com.java110.intf.store.IStoreStaffLeaderV1InnerServiceSMO;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.storeStaffLeader.StoreStaffLeaderPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 类表述：保存店铺员工领导关系命令类
 * 服务编码：storeStaffLeader.saveStoreStaffLeader
 * 请求路劲：/app/storeStaffLeader.SaveStoreStaffLeader
 * add by 吴学文 at 2025-06-03 16:25:11 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * 功能描述：处理保存店铺员工与领导关系的命令，包括参数验证、业务逻辑处理和事务管理
 * 主要职责：
 * 1. 验证请求参数完整性
 * 2. 检查领导用户是否存在
 * 3. 生成主键ID并保存员工领导关系
 * 4. 处理事务回滚和异常情况
 */
@Java110Cmd(serviceCode = "storeUser.saveStoreStaffLeader")
public class SaveStoreStaffLeaderCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveStoreStaffLeaderCmd.class);

    /**
     * ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 店铺员工领导关系服务接口
     */
    @Autowired
    private IStoreStaffLeaderV1InnerServiceSMO storeStaffLeaderV1InnerServiceSMOImpl;

    /**
     * 用户服务接口
     */
    @Autowired
    private IUserV1InnerServiceSMO userInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取请求上下文信息
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求报文中必须包含staffId参数
        Assert.hasKeyAndValue(reqJson, "staffId", "请求报文中未包含staffId");
        // 验证请求报文中必须包含leaderId参数
        Assert.hasKeyAndValue(reqJson, "leaderId", "请求报文中未包含leaderId");
        // 验证请求报文中必须包含relCd参数
        Assert.hasKeyAndValue(reqJson, "relCd", "请求报文中未包含relCd");
    }

    /**
     * 执行保存店铺员工领导关系命令
     * 该方法包含事务管理，确保数据一致性
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当保存数据失败或业务逻辑异常时抛出
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 创建用户查询对象，用于验证领导用户是否存在
        UserDto userDto = new UserDto();
        userDto.setUserId(reqJson.getString("leaderId"));
        // 查询领导用户信息
        List<UserDto> userDtos = userInnerServiceSMOImpl.queryUsers(userDto);
        // 验证领导用户存在且唯一
        Assert.listOnlyOne(userDtos, "领导不存在");

        // 从上下文中获取店铺ID
        String storeId = CmdContextUtils.getStoreId(cmdDataFlowContext);
        
        // 将请求JSON转换为店铺员工领导关系PO对象
        StoreStaffLeaderPo storeStaffLeaderPo = BeanConvertUtil.covertBean(reqJson, StoreStaffLeaderPo.class);
        // 生成主键ID
        storeStaffLeaderPo.setSslId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        // 设置领导姓名
        storeStaffLeaderPo.setLeaderName(userDtos.get(0).getName());
        // 设置店铺ID
        storeStaffLeaderPo.setStoreId(storeId);
        
        // 调用服务保存店铺员工领导关系
        int flag = storeStaffLeaderV1InnerServiceSMOImpl.saveStoreStaffLeader(storeStaffLeaderPo);

        // 检查保存结果，如果保存失败则抛出异常
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}