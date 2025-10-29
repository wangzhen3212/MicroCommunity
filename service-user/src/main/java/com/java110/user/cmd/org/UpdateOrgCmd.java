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
package com.java110.user.cmd.org;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.org.OrgDto;
import com.java110.intf.user.IOrgV1InnerServiceSMO;
import com.java110.po.org.OrgPo;
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
 * 服务编码：org.updateOrg
 * 请求路劲：/app/org.UpdateOrg
 * add by 吴学文 at 2022-02-28 17:26:28 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 组织更新命令类
 * 
 * 该类负责处理组织信息的更新操作，包括参数验证、数据查询和更新执行
 * 通过注解标识服务编码，与前端请求路径对应
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-02-28
 */
@Java110Cmd(serviceCode = "org.updateOrg")
public class UpdateOrgCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(UpdateOrgCmd.class);

    /**
     * 组织服务接口，用于调用组织相关的业务操作
     */
    @Autowired
    private IOrgV1InnerServiceSMO orgV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整和有效，包括必填字段检查和数据格式验证
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取请求头和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证组织ID不能为空
        Assert.hasKeyAndValue(reqJson, "orgId", "组织ID不能为空");
        // 验证组织名称不能为空
        Assert.hasKeyAndValue(reqJson, "orgName", "必填，请填写组织名称");
        // 描述字段暂时注释，可根据业务需要开启
        //Assert.hasKeyAndValue(reqJson, "description", "必填，请填写描述");
        
        // 从请求头中获取商户ID并设置到请求参数中
        String storeId = cmdDataFlowContext.getReqHeaders().get("store-id");
        reqJson.put("storeId", storeId);
        // 验证商户ID不能为空
        Assert.hasKeyAndValue(reqJson, "storeId", "必填，请填写商户ID");
    }

    /**
     * 命令执行方法
     * 
     * 执行组织信息的更新操作，包括数据查询、数据转换和更新操作
     * 使用事务注解确保操作的原子性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于设置响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 创建组织数据传输对象，用于查询现有组织信息
        OrgDto orgDto = new OrgDto();
        orgDto.setOrgId(reqJson.getString("orgId"));
        orgDto.setStoreId(reqJson.getString("storeId"));
        
        // 根据组织ID和商户ID查询组织信息
        List<OrgDto> orgDtos = orgV1InnerServiceSMOImpl.queryOrgs(orgDto);

        // 验证查询结果，确保只查询到一条记录
        Assert.listOnlyOne(orgDtos, "未查询到组织信息 或查询到多条数据");

        // 创建业务组织对象，用于更新操作
        JSONObject businessOrg = new JSONObject();
        // 复制所有请求参数到业务对象
        businessOrg.putAll(reqJson);
        // 保留原有操作权限和所属小区ID，避免被覆盖
        businessOrg.put("allowOperation", orgDtos.get(0).getAllowOperation());
        businessOrg.put("belongCommunityId", orgDtos.get(0).getBelongCommunityId());
        
        // 将JSON对象转换为持久化对象
        OrgPo orgPo = BeanConvertUtil.covertBean(businessOrg, OrgPo.class);

        // 执行更新操作，返回影响的行数
        int flag = orgV1InnerServiceSMOImpl.updateOrg(orgPo);

        // 检查更新是否成功，失败则抛出异常
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}