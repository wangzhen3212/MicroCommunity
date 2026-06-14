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
import com.java110.core.factory.GenerateCodeFactory;
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
 * 类表述：保存组织信息的命令类
 * 服务编码：org.saveOrg
 * 请求路劲：/app/org.SaveOrg
 * add by 吴学文 at 2022-02-28 17:26:28 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * 功能描述：处理组织信息保存的命令类，负责验证请求参数并保存组织数据到数据库
 */
@Java110Cmd(serviceCode = "org.saveOrg")
public class SaveOrgCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveOrgCmd.class);

    /**
     * 组织ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 组织服务接口
     */
    @Autowired
    private IOrgV1InnerServiceSMO orgV1InnerServiceSMOImpl;

    /**
     * 验证请求参数的有效性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文，包含请求和响应信息
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填参数：组织名称
        Assert.hasKeyAndValue(reqJson, "orgName", "必填，请填写组织名称");
        // 验证必填参数：上级组织ID
        Assert.hasKeyAndValue(reqJson, "parentOrgId", "必填，请选择上级ID");
        // 注释掉的验证：隶属小区ID
        //Assert.hasKeyAndValue(reqJson, "belongCommunityId", "必填，请选择隶属小区");
        // 注释掉的验证：描述信息
        //Assert.hasKeyAndValue(reqJson, "description", "必填，请填写描述");

        // 创建组织DTO对象用于查询上级组织信息
        OrgDto orgDto = new OrgDto();
        orgDto.setOrgId(reqJson.getString("parentOrgId"));
        // 查询上级组织信息
        List<OrgDto> orgDtos = orgV1InnerServiceSMOImpl.queryOrgs(orgDto);

        // 验证上级组织存在且唯一
        Assert.listOnlyOne(orgDtos,"上级组织不存在");

        // 计算当前组织的层级：上级组织层级+1
        int orgLevel = Integer.parseInt(orgDtos.get(0).getOrgLevel());
        orgLevel +=1;
        // 将计算出的组织层级设置到请求参数中
        reqJson.put("orgLevel",orgLevel);
    }

    /**
     * 执行组织保存操作
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文，包含请求和响应信息
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 从请求头中获取店铺ID
        String storeId = cmdDataFlowContext.getReqHeaders().get("store-id");

        // 创建业务组织对象并复制请求参数
        JSONObject businessOrg = new JSONObject();
        businessOrg.putAll(reqJson);
        // 设置允许操作标志为真
        businessOrg.put("allowOperation", "T");
        // 设置隶属小区ID为空字符串
        businessOrg.put("belongCommunityId", "");

        // 将JSON对象转换为组织PO对象
        OrgPo orgPo = BeanConvertUtil.covertBean(businessOrg, OrgPo.class);
        // 生成组织ID
        orgPo.setOrgId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        // 设置店铺ID
        orgPo.setStoreId(storeId);

        // 保存组织信息到数据库
        int flag = orgV1InnerServiceSMOImpl.saveOrg(orgPo);

        // 检查保存结果，失败则抛出异常
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}