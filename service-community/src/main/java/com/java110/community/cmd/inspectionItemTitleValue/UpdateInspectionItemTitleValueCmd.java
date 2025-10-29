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
 * 类表述：更新
 * 服务编码：inspectionItemTitleValue.updateInspectionItemTitleValue
 * 请求路劲：/app/inspectionItemTitleValue.UpdateInspectionItemTitleValue
 * add by 吴学文 at 2022-03-15 11:10:16 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 巡检项标题值更新命令类
 * 
 * 该类负责处理巡检项标题值的更新操作，继承自Cmd基类，通过注解标识服务编码，
 * 实现请求参数的验证和业务逻辑处理功能
 * 
 * @author 吴学文
 * @version 1.0
 * @date 2022-03-15
 */
@Java110Cmd(serviceCode = "inspectionItemTitleValue.updateInspectionItemTitleValue")
public class UpdateInspectionItemTitleValueCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(UpdateInspectionItemTitleValueCmd.class);

    /**
     * 巡检项标题值服务接口
     */
    @Autowired
    private IInspectionItemTitleValueV1InnerServiceSMO inspectionItemTitleValueV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求JSON中是否包含必要的参数，确保更新操作的数据完整性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证valueId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "valueId", "valueId不能为空");
        // 验证communityId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行巡检项标题值更新命令
     * 
     * 将请求参数转换为实体对象，调用服务层进行数据更新操作，
     * 并处理更新结果返回响应信息
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为巡检项标题值实体对象
        InspectionItemTitleValuePo inspectionItemTitleValuePo = BeanConvertUtil.covertBean(reqJson, InspectionItemTitleValuePo.class);
        
        // 调用服务层接口执行更新操作，返回受影响的行数
        int flag = inspectionItemTitleValueV1InnerServiceSMOImpl.updateInspectionItemTitleValue(inspectionItemTitleValuePo);

        // 检查更新操作是否成功，如果受影响行数小于1则认为更新失败
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        // 设置响应结果为成功状态
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}