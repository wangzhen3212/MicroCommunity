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
package com.java110.user.cmd.roleCommunity;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.user.IRoleCommunityV1InnerServiceSMO;
import com.java110.po.privilege.RoleCommunityPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 类表述：删除角色社区关联关系命令类
 * 服务编码：roleCommunity.deleteRoleCommunity
 * 请求路劲：/app/roleCommunity.DeleteRoleCommunity
 * add by 吴学文 at 2022-07-25 17:12:49 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * @description 删除角色社区关联关系的命令处理类，负责处理删除角色与社区关联关系的业务逻辑
 * @author 吴学文
 * @date 2022-07-25
 * @version 1.0
 */
@Java110Cmd(serviceCode = "roleCommunity.deleteRoleCommunity")
public class DeleteRoleCommunityCmd extends Cmd {
    
    /**
     * 日志记录器，用于记录操作日志和异常信息
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteRoleCommunityCmd.class);

    /**
     * 角色社区关联关系服务接口，用于调用数据层的删除操作
     */
    @Autowired
    private IRoleCommunityV1InnerServiceSMO roleCommunityV1InnerServiceSMOImpl;

    /**
     * 参数校验方法
     * 
     * @description 验证请求参数是否完整和有效，主要检查必填参数rcId是否存在
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象，包含需要删除的角色社区关联关系ID
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含rcId字段，rcId为角色社区关联关系的主键ID
        Assert.hasKeyAndValue(reqJson, "rcId", "rcId不能为空");
    }

    /**
     * 命令执行方法
     * 
     * @description 执行删除角色社区关联关系的核心业务逻辑，包括参数转换、数据删除和结果处理
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于设置响应结果
     * @param reqJson 请求参数的JSON对象，包含需要删除的角色社区关联关系信息
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为角色社区关联关系实体对象，便于后续数据库操作
        RoleCommunityPo roleCommunityPo = BeanConvertUtil.covertBean(reqJson, RoleCommunityPo.class);
        
        // 调用服务层接口执行删除操作，返回影响的行数
        int flag = roleCommunityV1InnerServiceSMOImpl.deleteRoleCommunity(roleCommunityPo);

        // 检查删除操作是否成功，如果影响行数小于1则认为删除失败
        if (flag < 1) {
            // 删除失败时抛出异常，事务会自动回滚
            throw new CmdException("删除数据失败");
        }

        // 设置响应结果为成功，返回操作成功的标准响应格式
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}