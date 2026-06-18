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
package com.java110.community.cmd.dataPrivilege;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.community.IDataPrivilegeV1InnerServiceSMO;
import com.java110.po.privilege.DataPrivilegePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 数据权限保存命令类
 * 
 * 该类负责处理数据权限的保存操作，包括参数验证和数据持久化
 * 服务编码：dataPrivilege.saveDataPrivilege
 * 请求路径：/app/dataPrivilege.SaveDataPrivilege
 * 
 * @author 吴学文 at 2022-09-05 10:53:58 mail: 928255095@qq.com
 * @open source address: https://gitee.com/wuxw7/MicroCommunity
 * @官网：http://www.homecommunity.cn
 * @温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "dataPrivilege.saveDataPrivilege")
public class SaveDataPrivilegeCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveDataPrivilegeCmd.class);

    /**
     * 数据权限ID前缀，用于生成唯一标识
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 数据权限服务接口
     */
    @Autowired
    private IDataPrivilegeV1InnerServiceSMO dataPrivilegeV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求报文中必须包含name字段
        Assert.hasKeyAndValue(reqJson, "name", "请求报文中未包含name");
        // 验证请求报文中必须包含communityId字段
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
    }

    /**
     * 执行保存数据权限命令
     * 
     * 将请求参数转换为数据权限对象，生成唯一ID并保存到数据库
     * 该方法使用事务注解确保数据一致性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为DataPrivilegePo对象
        DataPrivilegePo dataPrivilegePo = BeanConvertUtil.covertBean(reqJson, DataPrivilegePo.class);
        
        // 生成数据权限的唯一ID
        dataPrivilegePo.setDpId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务保存数据权限信息
        int flag = dataPrivilegeV1InnerServiceSMOImpl.saveDataPrivilege(dataPrivilegePo);

        // 检查保存操作是否成功
        if (flag < 1) {
            // 保存失败时抛出异常
            throw new CmdException("保存数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}