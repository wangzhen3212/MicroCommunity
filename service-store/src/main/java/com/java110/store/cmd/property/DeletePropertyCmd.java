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
package com.java110.store.cmd.property;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.Environment;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.community.CommunityMemberDto;
import com.java110.dto.store.StoreUserDto;
import com.java110.intf.community.ICommunityMemberV1InnerServiceSMO;
import com.java110.intf.store.IStoreUserV1InnerServiceSMO;
import com.java110.intf.store.IStoreV1InnerServiceSMO;
import com.java110.intf.user.IMenuGroupCommunityV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.community.CommunityMemberPo;
import com.java110.po.menu.MenuGroupCommunityPo;
import com.java110.po.store.StorePo;
import com.java110.po.store.StoreUserPo;
import com.java110.po.user.UserPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;


/**
 * 物业公司删除命令文档注解
 * 提供外系统删除物业公司的接口文档信息
 */
@Java110CmdDoc(title = "删除物业公司",
        description = "主要提供给外系统删除物业公司",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/property.deleteProperty",
        resource = "storeDoc",
        author = "吴学文",
        serviceCode = "property.deleteProperty",
        seq = 3
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "storeId", length = 30, remark = "物业编号"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

@Java110ExampleDoc(
        reqBody="{\"storeId\":\"102022083062960025\"}",
        resBody="{'code':0,'msg':'成功'}"
)

/**
 * 物业公司删除命令类
 * 负责处理物业公司的删除操作，包括删除物业公司基本信息、员工信息、小区关联信息和权限信息
 * 服务编码：property.deleteProperty
 * 请求路径：/app/property.deleteProperty
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-02-28
 */
@Java110Cmd(serviceCode = "property.deleteProperty")
public class DeletePropertyCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeletePropertyCmd.class);

    /**
     * 物业公司服务接口
     */
    @Autowired
    private IStoreV1InnerServiceSMO storeV1InnerServiceSMOImpl;

    /**
     * 物业员工服务接口
     */
    @Autowired
    private IStoreUserV1InnerServiceSMO storeUserV1InnerServiceSMOImpl;

    /**
     * 小区成员服务接口
     */
    @Autowired
    private ICommunityMemberV1InnerServiceSMO communityMemberV1InnerServiceSMOImpl;

    /**
     * 菜单组小区服务接口
     */
    @Autowired
    private IMenuGroupCommunityV1InnerServiceSMO menuGroupCommunityV1InnerServiceSMOImpl;

    /**
     * 用户服务接口
     */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 检查当前环境是否为开发环境
        Environment.isDevEnv();
        // 验证storeId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "storeId", "storeId不能为空");
        // 验证storeTypeCd参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "storeTypeCd", "storeTypeCd不能为空");
    }

    /**
     * 执行删除物业公司命令
     * 删除物业公司及其相关的所有数据，包括员工信息、小区关联信息和权限信息
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为StorePo对象
        StorePo storePo = BeanConvertUtil.covertBean(reqJson, StorePo.class);
        // 调用服务删除物业公司基本信息
        int flag = storeV1InnerServiceSMOImpl.deleteStore(storePo);

        // 检查删除操作是否成功
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 删除物业公司下的所有员工数据
        deleteStaff(storePo);

        // 查询物业公司关联的小区信息
        CommunityMemberDto communityMemberDto = new CommunityMemberDto();
        communityMemberDto.setMemberId(storePo.getStoreId());
        List<CommunityMemberDto> communityMemberDtos = communityMemberV1InnerServiceSMOImpl.queryCommunityMembers(communityMemberDto);

        // 如果没有关联的小区，直接返回成功
        if (communityMemberDtos == null || communityMemberDtos.size() < 1) {
            cmdDataFlowContext.setResponseEntity(ResultVo.success());
            return;
        }

        // 释放物业公司与小区的关联关系
        CommunityMemberPo communityMemberPo = new CommunityMemberPo();
        communityMemberPo.setMemberId(storePo.getStoreId());
        flag = communityMemberV1InnerServiceSMOImpl.deleteCommunityMember(communityMemberPo);
        if (flag < 1) {
            throw new CmdException("释放小区失败");
        }

        // 删除所有关联小区的权限信息
        for (CommunityMemberDto tmpCommunityMemberDto : communityMemberDtos) {
            MenuGroupCommunityPo menuGroupCommunityPo = new MenuGroupCommunityPo();
            menuGroupCommunityPo.setCommunityId(tmpCommunityMemberDto.getCommunityId());
            flag = menuGroupCommunityV1InnerServiceSMOImpl.deleteMenuGroupCommunity(menuGroupCommunityPo);
            if (flag < 1) {
                throw new CmdException("小区权限踢掉失败");
            }
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }

    /**
     * 删除物业公司下的所有员工信息
     * 包括删除员工关联关系和员工用户信息
     *
     * @param storePo 物业公司对象，包含物业公司ID等信息
     */
    private void deleteStaff(StorePo storePo) {
        // 查询物业公司下的所有员工
        StoreUserDto storeUserDto = new StoreUserDto();
        storeUserDto.setStoreId(storePo.getStoreId());
        List<StoreUserDto> storeUserDtos = storeUserV1InnerServiceSMOImpl.queryStoreUsers(storeUserDto);
        
        // 如果没有员工，直接返回
        if(storeUserDtos == null || storeUserDtos.size() < 1){
            return;
        }
        
        // 删除物业公司与员工的关联关系
        StoreUserPo storeUserPo = new StoreUserPo();
        storeUserPo.setStoreId(storePo.getStoreId());
        storeUserV1InnerServiceSMOImpl.deleteStoreUser(storeUserPo);
        
        // 删除所有员工对应的用户信息
        UserPo userPo = null;
        for(StoreUserDto staff : storeUserDtos){
            userPo = new UserPo();
            userPo.setUserId(staff.getUserId());
            userV1InnerServiceSMOImpl.deleteUser(userPo);
        }
    }
}