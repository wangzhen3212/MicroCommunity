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
package com.java110.community.cmd.communitySettingKey;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.community.ICommunitySettingKeyV1InnerServiceSMO;
import com.java110.po.community.CommunitySettingKeyPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 类表述：保存小区设置键值对信息命令类
 * 服务编码：communitySettingKey.saveCommunitySettingKey
 * 请求路径：/app/communitySettingKey.SaveCommunitySettingKey
 * 
 * 该类负责处理小区设置键值对信息的保存操作，包括参数验证和数据持久化
 * 
 * @author 吴学文 at 2021-10-10 10:56:18 mail: 928255095@qq.com
 * @open source address: https://gitee.com/wuxw7/MicroCommunity
 * @官网：http://www.homecommunity.cn
 * @温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "communitySettingKey.saveCommunitySettingKey")
public class SaveCommunitySettingKeyCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveCommunitySettingKeyCmd.class);

    /**
     * 主键ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 小区设置键值对服务接口
     */
    @Autowired
    private ICommunitySettingKeyV1InnerServiceSMO communitySettingKeyV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段：settingType、settingName、settingKey
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含settingType字段
        Assert.hasKeyAndValue(reqJson, "settingType", "请求报文中未包含settingType");
        // 验证请求参数中必须包含settingName字段
        Assert.hasKeyAndValue(reqJson, "settingName", "请求报文中未包含settingName");
        // 验证请求参数中必须包含settingKey字段
        Assert.hasKeyAndValue(reqJson, "settingKey", "请求报文中未包含settingKey");
    }

    /**
     * 执行保存小区设置键值对信息的命令方法
     * 
     * 该方法将请求参数转换为实体对象，生成主键ID，并调用服务层进行数据保存
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当数据保存失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为小区设置键值对实体对象
        CommunitySettingKeyPo communitySettingKeyPo = BeanConvertUtil.covertBean(reqJson, CommunitySettingKeyPo.class);
        
        // 生成主键ID，使用预设的前缀
        communitySettingKeyPo.setKeyId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务层接口保存数据，返回影响的行数
        int flag = communitySettingKeyV1InnerServiceSMOImpl.saveCommunitySettingKey(communitySettingKeyPo);

        // 检查保存操作是否成功，如果影响行数小于1则认为保存失败
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}