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
package com.java110.store.cmd.smallWechat;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.app.AppDto;
import com.java110.dto.wechat.SmallWeChatDto;
import com.java110.dto.wechat.SmallWechatAttrDto;
import com.java110.intf.store.ISmallWechatAttrV1InnerServiceSMO;
import com.java110.intf.store.ISmallWechatV1InnerServiceSMO;
import com.java110.po.wechat.SmallWechatPo;
import com.java110.po.wechat.SmallWechatAttrPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 保存小程序配置信息命令类
 * 
 * 该类负责处理保存小程序配置信息的业务逻辑，包括参数验证、数据保存和微信模板配置等功能。
 * 主要功能包括：
 * 1. 验证请求参数完整性
 * 2. 保存小程序基础信息
 * 3. 根据小程序类型配置不同的微信模板
 * 
 * @author 吴学文 at 2022-05-25 10:46:07 mail: 928255095@qq.com
 * @version 1.0
 * @serviceCode smallWeChat.saveSmallWeChat
 * @requestPath /app/smallWechat.SaveSmallWechat
 */
@Java110Cmd(serviceCode = "smallWeChat.saveSmallWeChat")
public class SaveSmallWechatCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveSmallWechatCmd.class);

    /** 属性ID前缀 */
    public static final String CODE_PREFIX_ID = "10";

    /** 小程序信息服务接口 */
    @Autowired
    private ISmallWechatV1InnerServiceSMO smallWechatV1InnerServiceSMOImpl;

    /** 小程序属性服务接口 */
    @Autowired
    private ISmallWechatAttrV1InnerServiceSMO smallWechatAttrV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整，确保必要的参数都存在
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填参数
        Assert.hasKeyAndValue(reqJson, "name", "请求报文中未包含name");
        Assert.hasKeyAndValue(reqJson, "appId", "请求报文中未包含appId");
        Assert.hasKeyAndValue(reqJson, "appSecret", "请求报文中未包含appSecret");
        Assert.hasKeyAndValue(reqJson, "payPassword", "请求报文中未包含payPassword");
        Assert.hasKeyAndValue(reqJson, "objId", "请求报文中未包含objId(小区id)");
        Assert.hasKeyAndValue(reqJson, "weChatType", "请求报文中未包含类型");
        
        // 调用父类属性验证方法
        super.validateProperty(cmdDataFlowContext);
    }

    /**
     * 执行保存小程序配置命令
     * 
     * 主要执行逻辑：
     * 1. 生成小程序ID
     * 2. 保存小程序基础信息
     * 3. 如果是公众号类型，配置相关微信模板
     * 4. 设置默认的微信对接token
     * 5. 配置各种业务场景的微信模板
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 获取应用ID
        String appId = cmdDataFlowContext.getReqHeaders().get("app-id");
        if (StringUtil.isEmpty(appId)) {
            appId = cmdDataFlowContext.getReqHeaders().get("app-id");
        }

        // 生成小程序ID
        String wechatId = GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_weChatId);
        reqJson.put("wechatId", wechatId);
        reqJson.put("objType", SmallWeChatDto.OBJ_TYPE_COMMUNITY);
        
        // 转换并保存小程序基础信息
        SmallWechatPo smallWechatPo = BeanConvertUtil.covertBean(reqJson, SmallWechatPo.class);
        smallWechatPo.setWechatId(wechatId);
        smallWechatPo.setWechatType(reqJson.getString("weChatType"));
        int flag = smallWechatV1InnerServiceSMOImpl.saveSmallWechat(smallWechatPo);

        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 判断是否为公众号类型，非公众号类型直接返回
        if (!SmallWeChatDto.WECHAT_TYPE_PUBLIC.equals(reqJson.getString("weChatType"))) {
            return;
        }

        // 添加微信对接token配置
        SmallWechatAttrPo smallWechatAttrPo = new SmallWechatAttrPo();
        smallWechatAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        smallWechatAttrPo.setCommunityId(reqJson.getString("objId"));
        smallWechatAttrPo.setSpecCd(SmallWechatAttrDto.SPEC_CD_TOKEN);
        smallWechatAttrPo.setValue("java110"); // 默认token值
        smallWechatAttrPo.setWechatId(wechatId);
        flag = smallWechatAttrV1InnerServiceSMOImpl.saveSmallWechatAttr(smallWechatAttrPo);
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }
        
        // 如果是商城应用，不创建后续模板配置
        if (AppDto.WECHAT_MALL_APP_ID.equals(appId)) {
            return;
        }

        // 配置物业管理通知模板
        smallWechatAttrPo = new SmallWechatAttrPo();
        smallWechatAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        smallWechatAttrPo.setCommunityId(reqJson.getString("objId"));
        smallWechatAttrPo.setSpecCd(SmallWechatAttrDto.SPEC_CD_WECHAT_TEMPLATE);
        smallWechatAttrPo.setValue("这里请填写物业管理通知模板ID");
        smallWechatAttrPo.setWechatId(wechatId);
        flag = smallWechatAttrV1InnerServiceSMOImpl.saveSmallWechatAttr(smallWechatAttrPo);

        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }
        
        // 配置业主费用到期通知模板
        smallWechatAttrPo = new SmallWechatAttrPo();
        smallWechatAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        smallWechatAttrPo.setCommunityId(reqJson.getString("objId"));
        smallWechatAttrPo.setSpecCd(SmallWechatAttrDto.SPEC_CD_WECHAT_EXPIRE_TEMPLATE);
        smallWechatAttrPo.setValue("这里请填写业主费用到期通知模板ID");
        smallWechatAttrPo.setWechatId(wechatId);
        flag = smallWechatAttrV1InnerServiceSMOImpl.saveSmallWechatAttr(smallWechatAttrPo);

        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }
        
        // 配置流程通知模板（用于空置房验房申请、审批流程等）
        smallWechatAttrPo = new SmallWechatAttrPo();
        smallWechatAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        smallWechatAttrPo.setCommunityId(reqJson.getString("objId"));
        smallWechatAttrPo.setSpecCd(SmallWechatAttrDto.SPEC_CD_WECHAT_PROCESS_TEMPLATE);
        smallWechatAttrPo.setValue("这里请填写流程通知模板ID");
        smallWechatAttrPo.setWechatId(wechatId);
        flag = smallWechatAttrV1InnerServiceSMOImpl.saveSmallWechatAttr(smallWechatAttrPo);

        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }
        
        // 配置空置房状态通知模板
        smallWechatAttrPo = new SmallWechatAttrPo();
        smallWechatAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        smallWechatAttrPo.setCommunityId(reqJson.getString("objId"));
        smallWechatAttrPo.setSpecCd(SmallWechatAttrDto.SPEC_CD_WECHAT_ROOM_STATE_TEMPLATE);
        smallWechatAttrPo.setValue("这里请填写空置房状态通知模板ID");
        smallWechatAttrPo.setWechatId(wechatId);
        flag = smallWechatAttrV1InnerServiceSMOImpl.saveSmallWechatAttr(smallWechatAttrPo);

        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }
        
        // 配置装修申请提醒模板（提醒业主缴纳装修押金等）
        smallWechatAttrPo = new SmallWechatAttrPo();
        smallWechatAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        smallWechatAttrPo.setCommunityId(reqJson.getString("objId"));
        smallWechatAttrPo.setSpecCd(SmallWechatAttrDto.SPEC_CD_WECHAT_HOUSE_DECORATION_APPLY_TEMPLATE);
        smallWechatAttrPo.setValue("这里请填写装修申请提醒给业主进行缴纳装修押金和确认装修信息通知模板ID");
        smallWechatAttrPo.setWechatId(wechatId);
        flag = smallWechatAttrV1InnerServiceSMOImpl.saveSmallWechatAttr(smallWechatAttrPo);

        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }
        
        // 配置装修申请审核提醒模板（提醒物业人员审核）
        smallWechatAttrPo = new SmallWechatAttrPo();
        smallWechatAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        smallWechatAttrPo.setCommunityId(reqJson.getString("objId"));
        smallWechatAttrPo.setSpecCd(SmallWechatAttrDto.SPEC_CD_WECHAT_HOUSE_DECORATION_CHECK_TEMPLATE);
        smallWechatAttrPo.setValue("这里请填写装修申请提醒给物业人员进行审核操作通知模板ID");
        smallWechatAttrPo.setWechatId(wechatId);
        flag = smallWechatAttrV1InnerServiceSMOImpl.saveSmallWechatAttr(smallWechatAttrPo);

        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }
        
        // 配置装修申请审核结果通知模板
        smallWechatAttrPo = new SmallWechatAttrPo();
        smallWechatAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        smallWechatAttrPo.setCommunityId(reqJson.getString("objId"));
        smallWechatAttrPo.setSpecCd(SmallWechatAttrDto.SPEC_CD_WECHAT_HOUSE_DECORATION_CHECK_RESULT_TEMPLATE);
        smallWechatAttrPo.setValue("这里请填写装修申请审核结果提醒给业主,通知模板ID");
        smallWechatAttrPo.setWechatId(wechatId);
        flag = smallWechatAttrV1InnerServiceSMOImpl.saveSmallWechatAttr(smallWechatAttrPo);

        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }
        
        // 配置装修验收结果通知模板
        smallWechatAttrPo = new SmallWechatAttrPo();
        smallWechatAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        smallWechatAttrPo.setCommunityId(reqJson.getString("objId"));
        smallWechatAttrPo.setSpecCd(SmallWechatAttrDto.SPEC_CD_WECHAT_HOUSE_DECORATION_COMPLETED_TEMPLATE);
        smallWechatAttrPo.setValue("这里请填写装修验收结果提醒给业主,通知模板ID");
        smallWechatAttrPo.setWechatId(wechatId);
        flag = smallWechatAttrV1InnerServiceSMOImpl.saveSmallWechatAttr(smallWechatAttrPo);

        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}