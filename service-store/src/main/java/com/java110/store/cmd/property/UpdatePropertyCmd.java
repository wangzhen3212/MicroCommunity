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
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.doc.annotation.*;
import com.java110.dto.org.OrgDto;
import com.java110.dto.store.StoreAttrDto;
import com.java110.dto.store.StoreUserDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.store.IStoreAttrV1InnerServiceSMO;
import com.java110.intf.store.IStoreUserV1InnerServiceSMO;
import com.java110.intf.store.IStoreV1InnerServiceSMO;
import com.java110.intf.user.IOrgV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.org.OrgPo;
import com.java110.po.store.StoreAttrPo;
import com.java110.po.store.StorePo;
import com.java110.po.user.UserPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;


@Java110CmdDoc(title = "修改物业公司",
        description = "主要提供给外系统修改物业公司",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/property.updateProperty",
        resource = "storeDoc",
        author = "吴学文",
        serviceCode = "property.updateProperty",
        seq = 2
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "storeId", length = 30, remark = "物业编号"),
        @Java110ParamDoc(name = "name", length = 64, remark = "物业名称"),
        @Java110ParamDoc(name = "nearbyLandmarks", length = 64, remark = "地标，如xx 公园旁"),
        @Java110ParamDoc(name = "tel", length = 11, remark = "物业管理员电话 作为管理员账号密码 添加后请及时修改密码"),
        @Java110ParamDoc(name = "address", length = 11, remark = "公司地址"),
        @Java110ParamDoc(name = "corporation", length = 11, remark = "法人"),
        @Java110ParamDoc(name = "foundingTime", length = 11, remark = "成立日期"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

@Java110ExampleDoc(
        reqBody = "{\"storeId\":\"102022083062960025\",\"name\":\"培训物业公司\",\"address\":\"培训物业公司\",\"tel\":\"18909715555\",\"corporation\":\"无\",\"foundingTime\":\"2022-08-01\",\"nearByLandmarks\":\"123\"}",
        resBody = "{'code':0,'msg':'成功'}"
)

/**
 * 物业公司更新命令类
 * 
 * 该类负责处理物业公司信息的更新操作，包括物业基本信息、法人信息、成立日期等属性的更新，
 * 同时会同步更新相关的管理员用户信息和组织信息
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-02-28
 */
@Java110Cmd(serviceCode = "property.updateProperty")
public class UpdatePropertyCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdatePropertyCmd.class);

    @Autowired
    private IStoreV1InnerServiceSMO storeV1InnerServiceSMOImpl;

    @Autowired
    private IStoreAttrV1InnerServiceSMO storeAttrV1InnerServiceSMOImpl;

    @Autowired
    private IStoreUserV1InnerServiceSMO storeUserV1InnerServiceSMOImpl;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    @Autowired
    private IOrgV1InnerServiceSMO orgV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，确保必要的参数存在且符合要求
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置数据流信息
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证storeId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "storeId", "storeId不能为空");

        // 检查当前环境是否为开发环境
        Environment.isDevEnv();
    }

    /**
     * 执行物业公司更新命令
     * 
     * 该方法负责执行物业公司的完整更新流程，包括：
     * 1. 更新物业基本信息
     * 2. 更新法人信息（如果请求中包含）
     * 3. 更新成立日期信息（如果请求中包含）
     * 4. 同步更新管理员用户信息
     * 5. 同步更新组织信息
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为StorePo对象
        StorePo storePo = BeanConvertUtil.covertBean(reqJson, StorePo.class);
        // 更新物业基本信息
        int flag = storeV1InnerServiceSMOImpl.updateStore(storePo);

        // 检查更新是否成功
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        // 处理法人信息更新
        if (reqJson.containsKey("corporation")) {
            // 查询现有的法人信息
            StoreAttrDto storeAttrDto = new StoreAttrDto();
            storeAttrDto.setStoreId(reqJson.getString("storeId"));
            storeAttrDto.setSpecCd(StoreAttrDto.SPEC_CD_CORPORATION);
            List<StoreAttrDto> storeAttrDtos = storeAttrV1InnerServiceSMOImpl.queryStoreAttrs(storeAttrDto);
            
            if (storeAttrDtos == null || storeAttrDtos.size() < 1) {
                // 如果不存在法人信息，则新增
                StoreAttrPo storeAttrPo = new StoreAttrPo();
                storeAttrPo.setAttrId(GenerateCodeFactory.getAttrId());
                storeAttrPo.setSpecCd(StoreAttrDto.SPEC_CD_CORPORATION);
                storeAttrPo.setStoreId(storePo.getStoreId());
                storeAttrPo.setValue(reqJson.getString("corporation"));
                flag = storeAttrV1InnerServiceSMOImpl.saveStoreAttr(storeAttrPo);
            } else {
                // 如果已存在法人信息，则更新
                StoreAttrPo storeAttrPo = new StoreAttrPo();
                storeAttrPo.setAttrId(storeAttrDtos.get(0).getAttrId());
                storeAttrPo.setSpecCd(StoreAttrDto.SPEC_CD_CORPORATION);
                storeAttrPo.setValue(reqJson.getString("corporation"));
                flag = storeAttrV1InnerServiceSMOImpl.updateStoreAttr(storeAttrPo);
            }

            // 检查法人信息更新是否成功
            if (flag < 1) {
                throw new CmdException("保存数据失败");
            }
        }
        
        // 处理成立日期信息更新
        if (reqJson.containsKey("foundingTime")) {
            // 查询现有的成立日期信息
            StoreAttrDto storeAttrDto = new StoreAttrDto();
            storeAttrDto.setStoreId(reqJson.getString("storeId"));
            storeAttrDto.setSpecCd(StoreAttrDto.SPEC_CD_FOUNDINGTIME);
            List<StoreAttrDto> storeAttrDtos = storeAttrV1InnerServiceSMOImpl.queryStoreAttrs(storeAttrDto);
            
            if (storeAttrDtos == null || storeAttrDtos.size() < 1) {
                // 如果不存在成立日期信息，则新增
                StoreAttrPo storeAttrPo = new StoreAttrPo();
                storeAttrPo.setAttrId(GenerateCodeFactory.getAttrId());
                storeAttrPo.setSpecCd(StoreAttrDto.SPEC_CD_FOUNDINGTIME);
                storeAttrPo.setStoreId(storePo.getStoreId());
                storeAttrPo.setValue(reqJson.getString("foundingTime"));
                flag = storeAttrV1InnerServiceSMOImpl.saveStoreAttr(storeAttrPo);
            } else {
                // 如果已存在成立日期信息，则更新
                StoreAttrPo storeAttrPo = new StoreAttrPo();
                storeAttrPo.setAttrId(storeAttrDtos.get(0).getAttrId());
                storeAttrPo.setSpecCd(StoreAttrDto.SPEC_CD_FOUNDINGTIME);
                storeAttrPo.setValue(reqJson.getString("foundingTime"));
                flag = storeAttrV1InnerServiceSMOImpl.updateStoreAttr(storeAttrPo);
            }
            
            // 检查成立日期信息更新是否成功
            if (flag < 1) {
                throw new CmdException("保存数据失败");
            }
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());

        // 如果物业名称为空，则不需要更新相关用户和组织信息
        if (StringUtil.isEmpty(storePo.getName())) {
            return;
        }

        // 更新管理员用户信息
        StoreUserDto storeUserDto = new StoreUserDto();
        storeUserDto.setStoreId(storePo.getStoreId());
        storeUserDto.setRelCd(StoreUserDto.REL_CD_MANAGER);
        List<StoreUserDto> storeUserDtos = storeUserV1InnerServiceSMOImpl.queryStoreUsers(storeUserDto);
        
        // 检查是否存在唯一的管理员用户
        if (storeUserDtos == null || storeUserDtos.size() != 1) {
            return;
        }

        // 更新管理员用户的名称
        UserPo userPo = new UserPo();
        userPo.setUserId(storeUserDtos.get(0).getUserId());
        userPo.setName(storePo.getName());
        userV1InnerServiceSMOImpl.updateUser(userPo);

        // 更新组织信息
        OrgDto orgDto = new OrgDto();
        orgDto.setStoreId(storePo.getStoreId());
        orgDto.setOrgLevel(OrgDto.ORG_LEVEL_STORE);
        List<OrgDto> orgDtos = orgV1InnerServiceSMOImpl.queryOrgs(orgDto);
        
        // 检查是否存在对应的组织信息
        if (orgDtos == null || orgDtos.size() < 1) {
            return;
        }

        // 更新组织名称
        OrgPo orgPo = new OrgPo();
        orgPo.setOrgId(orgDtos.get(0).getOrgId());
        orgPo.setOrgName(storePo.getName());
        orgV1InnerServiceSMOImpl.updateOrg(orgPo);
    }
}