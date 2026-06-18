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
package com.java110.community.cmd.propertyRightRegistrationDetail;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.community.IPropertyRightRegistrationDetailV1InnerServiceSMO;
import com.java110.po.room.PropertyRightRegistrationDetailPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;

/**
 * 类表述：保存
 * 服务编码：propertyRightRegistrationDetail.savePropertyRightRegistrationDetail
 * 请求路劲：/app/propertyRightRegistrationDetail.SavePropertyRightRegistrationDetail
 * add by 吴学文 at 2021-10-11 09:15:52 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 产权登记详情保存命令类
 * 
 * 该类负责处理产权登记详情的保存操作，包括参数验证和业务逻辑处理
 * 通过注解 @Java110Cmd 声明服务编码，与前端请求路径对应
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2021-10-11
 */
@Java110Cmd(serviceCode = "propertyRightRegistrationDetail.savePropertyRightRegistrationDetail")
public class SavePropertyRightRegistrationDetailCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SavePropertyRightRegistrationDetailCmd.class);

    /**
     * ID生成前缀，用于生成产权登记详情ID
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 产权登记详情服务接口
     */
    @Autowired
    private IPropertyRightRegistrationDetailV1InnerServiceSMO propertyRightRegistrationDetailV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整，确保必要的字段都存在
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求报文中必须包含prrdId字段
        Assert.hasKeyAndValue(reqJson, "prrdId", "请求报文中未包含prrdId");
        // 验证请求报文中必须包含prrId字段
        Assert.hasKeyAndValue(reqJson, "prrId", "请求报文中未包含prrId");
        // 验证请求报文中必须包含securities字段
        Assert.hasKeyAndValue(reqJson, "securities", "请求报文中未包含securities");
        // 验证请求报文中必须包含state字段
        Assert.hasKeyAndValue(reqJson, "state", "请求报文中未包含state");
        // 验证请求报文中必须包含createUser字段
        Assert.hasKeyAndValue(reqJson, "createUser", "请求报文中未包含createUser");
        // 验证请求报文中必须包含statusCd字段
        Assert.hasKeyAndValue(reqJson, "statusCd", "请求报文中未包含statusCd");
    }

    /**
     * 执行保存命令方法
     * 
     * 将请求参数转换为实体对象，生成唯一ID，并保存到数据库
     * 使用事务注解确保数据一致性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为产权登记详情实体对象
        PropertyRightRegistrationDetailPo propertyRightRegistrationDetailPo = BeanConvertUtil.covertBean(reqJson, PropertyRightRegistrationDetailPo.class);
        
        // 生成产权登记详情的唯一ID
        propertyRightRegistrationDetailPo.setPrrId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务层保存产权登记详情信息
        int flag = propertyRightRegistrationDetailV1InnerServiceSMOImpl.savePropertyRightRegistrationDetail(propertyRightRegistrationDetailPo);

        // 检查保存结果，如果影响行数小于1则认为保存失败
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}