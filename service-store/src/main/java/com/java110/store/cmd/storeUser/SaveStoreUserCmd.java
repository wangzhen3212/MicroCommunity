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
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.store.IStoreUserV1InnerServiceSMO;
import com.java110.po.store.StoreUserPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;

/**
 * 类表述：保存商铺用户关系信息
 * 服务编码：storeUser.saveStoreUser
 * 请求路劲：/app/storeUser.SaveStoreUser
 * add by 吴学文 at 2021-10-08 16:05:30 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 商铺用户关系保存命令类
 * 负责处理商铺用户关系信息的保存请求，包括参数验证和数据持久化操作
 * 通过注解@Java110Cmd声明服务编码，与前端请求进行映射
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2021-10-08
 */
@Java110Cmd(serviceCode = "storeUser.saveStoreUser")
public class SaveStoreUserCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveStoreUserCmd.class);

    /**
     * ID生成前缀，用于生成商铺用户关系ID
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 商铺用户关系服务接口，用于数据持久化操作
     */
    @Autowired
    private IStoreUserV1InnerServiceSMO storeUserV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求报文中是否包含必要的参数，确保数据的完整性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求报文中是否包含storeUserId参数
        Assert.hasKeyAndValue(reqJson, "storeUserId", "请求报文中未包含storeUserId");
        // 验证请求报文中是否包含storeId参数
        Assert.hasKeyAndValue(reqJson, "storeId", "请求报文中未包含storeId");
        // 验证请求报文中是否包含userId参数
        Assert.hasKeyAndValue(reqJson, "userId", "请求报文中未包含userId");
        // 验证请求报文中是否包含relCd参数（关系编码）
        Assert.hasKeyAndValue(reqJson, "relCd", "请求报文中未包含relCd");
    }

    /**
     * 命令执行方法
     * 处理商铺用户关系信息的保存逻辑，包括数据转换、ID生成和数据持久化
     * 使用@Java110Transactional注解确保操作的原子性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当数据保存失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为StoreUserPo对象
        StoreUserPo storeUserPo = BeanConvertUtil.covertBean(reqJson, StoreUserPo.class);
        
        // 生成商铺用户关系ID，使用前缀+序列号的方式
        storeUserPo.setStoreUserId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务层保存商铺用户关系信息
        int flag = storeUserV1InnerServiceSMOImpl.saveStoreUser(storeUserPo);

        // 检查保存结果，如果影响行数小于1则认为保存失败
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}