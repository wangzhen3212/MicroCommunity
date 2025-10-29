/*
 * Copyright 2017-2020 吴学文 and java110 team.
 *
 * Licensed under the Apache License, Version 2.0 (the License);
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
package com.java110.user.cmd.ownerCarOpenUser;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.user.IOwnerCarOpenUserV1InnerServiceSMO;
import com.java110.po.owner.OwnerCarOpenUserPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;

/**
 * 类表述：保存
 * 服务编码：ownerCarOpenUser.saveOwnerCarOpenUser
 * 请求路劲：/app/ownerCarOpenUser.SaveOwnerCarOpenUser
 * add by 吴学文 at 2021-10-17 00:47:27 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 业主车辆开门用户保存命令类
 * 
 * 该类负责处理业主车辆开门用户信息的保存操作，包括参数验证和数据持久化
 * 通过注解@Java110Cmd声明服务编码，与前端请求路径对应
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2021-10-17
 */
@Java110Cmd(serviceCode = "ownerCarOpenUser.saveOwnerCarOpenUser")
public class SaveOwnerCarOpenUserCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveOwnerCarOpenUserCmd.class);

    /**
     * ID生成前缀常量，用于生成开门用户ID
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 业主车辆开门用户服务接口
     */
    @Autowired
    private IOwnerCarOpenUserV1InnerServiceSMO ownerCarOpenUserV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求报文中是否包含必要的参数，确保数据的完整性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求报文中是否包含openId参数
        Assert.hasKeyAndValue(reqJson, "openId", "请求报文中未包含openId");
        // 验证请求报文中是否包含carNum参数
        Assert.hasKeyAndValue(reqJson, "carNum", "请求报文中未包含carNum");
        // 验证请求报文中是否包含openType参数
        Assert.hasKeyAndValue(reqJson, "openType", "请求报文中未包含openType");
    }

    /**
     * 执行保存命令方法
     * 
     * 将请求参数转换为实体对象，生成唯一ID，并调用服务层进行数据保存
     * 使用@Java110Transactional注解确保操作的原子性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当数据保存失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为OwnerCarOpenUserPo实体对象
        OwnerCarOpenUserPo ownerCarOpenUserPo = BeanConvertUtil.covertBean(reqJson, OwnerCarOpenUserPo.class);
        
        // 生成开门用户的唯一ID
        ownerCarOpenUserPo.setOpenUserId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务层保存数据，返回影响的行数
        int flag = ownerCarOpenUserV1InnerServiceSMOImpl.saveOwnerCarOpenUser(ownerCarOpenUserPo);

        // 检查保存操作是否成功，flag小于1表示保存失败
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}