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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.user.IRoleCommunityV1InnerServiceSMO;
import com.java110.po.privilege.RoleCommunityPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 角色小区关系保存命令类
 * 
 * 该类负责处理角色与小区关联关系的保存操作，实现角色与多个小区的批量关联功能
 * 服务编码：roleCommunity.saveRoleCommunity
 * 请求路径：/app/roleCommunity.SaveRoleCommunity
 * 
 * @author 吴学文 at 2022-07-25 17:12:49 mail: 928255095@qq.com
 * @open source address: https://gitee.com/wuxw7/MicroCommunity
 * @官网：http://www.homecommunity.cn
 * @温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "roleCommunity.saveRoleCommunity")
public class SaveRoleCommunityCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveRoleCommunityCmd.class);

    /**
     * ID生成前缀，用于生成角色小区关系ID
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 角色小区关系服务接口
     */
    @Autowired
    private IRoleCommunityV1InnerServiceSMO roleCommunityV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求报文中的必填参数，确保请求数据的完整性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求的JSON数据对象
     * @throws IllegalArgumentException 当请求参数不满足要求时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求报文中必须包含roleId参数
        Assert.hasKeyAndValue(reqJson, "roleId", "请求报文中未包含roleId");
        
        // 验证请求报文中必须包含小区信息且至少有一个小区
        if (!reqJson.containsKey("communitys") || reqJson.getJSONArray("communitys").size() < 1) {
            throw new IllegalArgumentException("未包含小区信息");
        }
    }

    /**
     * 执行保存角色小区关系命令
     * 
     * 该方法负责将角色与多个小区建立关联关系，采用事务处理确保数据一致性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 从请求头中获取店铺ID
        String storeId = cmdDataFlowContext.getReqHeaders().get("store-id");

        RoleCommunityPo roleCommunityPo = null;
        // 获取小区信息数组
        JSONArray communitys = reqJson.getJSONArray("communitys");
        
        // 遍历所有小区信息，为每个小区创建角色小区关系记录
        for (int communityIndex = 0; communityIndex < communitys.size(); communityIndex++) {
            roleCommunityPo = new RoleCommunityPo();
            
            // 设置小区ID
            roleCommunityPo.setCommunityId(communitys.getJSONObject(communityIndex).getString("communityId"));
            // 设置小区名称
            roleCommunityPo.setCommunityName(communitys.getJSONObject(communityIndex).getString("communityName"));
            // 设置角色ID
            roleCommunityPo.setRoleId(reqJson.getString("roleId"));
            // 设置店铺ID
            roleCommunityPo.setStoreId(storeId);
            // 生成角色小区关系ID
            roleCommunityPo.setRcId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
            
            // 调用服务保存角色小区关系
            int flag = roleCommunityV1InnerServiceSMOImpl.saveRoleCommunity(roleCommunityPo);

            // 检查保存结果，如果失败则抛出异常
            if (flag < 1) {
                throw new CmdException("保存数据失败");
            }
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}