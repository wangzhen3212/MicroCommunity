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
package com.java110.community.cmd.inspectionItemTitleValue;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.community.IInspectionItemTitleValueV1InnerServiceSMO;
import com.java110.po.inspection.InspectionItemTitleValuePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：保存
 * 服务编码：inspectionItemTitleValue.saveInspectionItemTitleValue
 * 请求路劲：/app/inspectionItemTitleValue.SaveInspectionItemTitleValue
 * add by 吴学文 at 2022-03-15 11:10:16 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 巡检项标题值保存命令类
 * 
 * 该类负责处理巡检项标题值的保存操作，包括参数验证和数据持久化
 * 通过注解标识服务编码，与前端请求路径对应
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-03-15
 */
@Java110Cmd(serviceCode = "inspectionItemTitleValue.saveInspectionItemTitleValue")
public class SaveInspectionItemTitleValueCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveInspectionItemTitleValueCmd.class);

    /**
     * ID生成前缀，用于生成唯一标识符
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 巡检项标题值服务接口，用于数据持久化操作
     */
    @Autowired
    private IInspectionItemTitleValueV1InnerServiceSMO inspectionItemTitleValueV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求报文中是否包含必要的参数，确保数据的完整性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求报文中必须包含titleId参数
        Assert.hasKeyAndValue(reqJson, "titleId", "请求报文中未包含titleId");
        // 验证请求报文中必须包含itemValue参数
        Assert.hasKeyAndValue(reqJson, "itemValue", "请求报文中未包含itemValue");
        // 验证请求报文中必须包含communityId参数
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        // 验证请求报文中必须包含seq参数
        Assert.hasKeyAndValue(reqJson, "seq", "请求报文中未包含seq");
    }

    /**
     * 执行保存巡检项标题值命令
     * 
     * 将请求参数转换为实体对象，生成唯一标识符，调用服务层进行数据保存
     * 使用事务注解确保数据操作的原子性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当数据保存失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为巡检项标题值实体对象
        InspectionItemTitleValuePo inspectionItemTitleValuePo = BeanConvertUtil.covertBean(reqJson, InspectionItemTitleValuePo.class);
        
        // 生成唯一标识符并设置到实体对象中
        inspectionItemTitleValuePo.setValueId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务层保存数据，返回影响的行数
        int flag = inspectionItemTitleValueV1InnerServiceSMOImpl.saveInspectionItemTitleValue(inspectionItemTitleValuePo);

        // 检查保存操作是否成功，如果影响行数小于1则认为保存失败
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}