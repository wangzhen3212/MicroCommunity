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
package com.java110.store.cmd.orgStaffRel;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.store.IOrgStaffRelV1InnerServiceSMO;
import com.java110.po.org.OrgStaffRelPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;

/**
 * 组织员工关系删除命令类
 * 
 * 该类负责处理组织员工关系的删除操作，通过接收前端请求参数，
 * 验证参数完整性，执行删除操作并返回处理结果
 * 
 * @author 吴学文
 * @version 1.0
 * @className DeleteOrgStaffRelCmd
 * @date 2021-10-08 16:25:38
 * @mail 928255095@qq.com
 * @openSourceAddress https://gitee.com/wuxw7/MicroCommunity
 * @officialWebsite http://www.homecommunity.cn
 * 
 * @serviceCode orgStaffRel.deleteOrgStaffRel
 * @requestPath /app/orgStaffRel.DeleteOrgStaffRel
 */
@Java110Cmd(serviceCode = "orgStaffRel.deleteOrgStaffRel")
public class DeleteOrgStaffRelCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteOrgStaffRelCmd.class);

    /**
     * 组织员工关系服务接口
     */
    @Autowired
    private IOrgStaffRelV1InnerServiceSMO orgStaffRelV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段，确保删除操作的数据完整性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含relId字段
        Assert.hasKeyAndValue(reqJson, "relId", "relId不能为空");
        // 验证请求参数中必须包含storeId字段
        Assert.hasKeyAndValue(reqJson, "storeId", "storeId不能为空");
    }

    /**
     * 执行删除命令方法
     * 
     * 将请求参数转换为实体对象，调用服务层执行删除操作，
     * 并根据删除结果设置响应信息
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为OrgStaffRelPo实体对象
        OrgStaffRelPo orgStaffRelPo = BeanConvertUtil.covertBean(reqJson, OrgStaffRelPo.class);
        
        // 调用服务层接口执行删除操作，返回受影响的行数
        int flag = orgStaffRelV1InnerServiceSMOImpl.deleteOrgStaffRel(orgStaffRelPo);

        // 检查删除操作是否成功，如果受影响行数小于1则认为删除失败
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 设置成功的响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}