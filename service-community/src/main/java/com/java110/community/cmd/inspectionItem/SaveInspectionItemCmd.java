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
package com.java110.community.cmd.inspectionItem;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.community.IInspectionItemV1InnerServiceSMO;
import com.java110.po.inspection.InspectionItemPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 类表述：保存
 * 服务编码：inspectionItem.saveInspectionItem
 * 请求路劲：/app/inspectionItem.SaveInspectionItem
 * add by 吴学文 at 2022-03-15 10:55:20 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 巡检项保存命令类
 * 负责处理巡检项的新增保存操作，包括参数验证和业务逻辑处理
 * 通过@Java110Cmd注解标识服务编码，与前端请求路径对应
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-03-15
 */
@Java110Cmd(serviceCode = "inspectionItem.saveInspectionItem")
public class SaveInspectionItemCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveInspectionItemCmd.class);

    /**
     * ID生成前缀，用于生成巡检项ID
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 巡检项服务接口，用于数据持久化操作
     */
    @Autowired
    private IInspectionItemV1InnerServiceSMO inspectionItemV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数中是否包含必要的字段
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求报文中必须包含巡检项名称
        Assert.hasKeyAndValue(reqJson, "itemName", "请求报文中未包含itemName");
        // 验证请求报文中必须包含小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
    }

    /**
     * 执行命令方法
     * 处理巡检项的新增保存业务逻辑，包括数据转换、ID生成和数据持久化
     * 使用@Java110Transactional注解确保事务性操作
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当数据保存失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为巡检项POJO对象
        InspectionItemPo inspectionItemPo = BeanConvertUtil.covertBean(reqJson, InspectionItemPo.class);
        
        // 生成巡检项ID，使用前缀确保ID的唯一性
        inspectionItemPo.setItemId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务层保存巡检项数据，返回影响的行数
        int flag = inspectionItemV1InnerServiceSMOImpl.saveInspectionItem(inspectionItemPo);

        // 检查保存操作是否成功，影响行数小于1表示保存失败
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置响应结果为成功状态
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}