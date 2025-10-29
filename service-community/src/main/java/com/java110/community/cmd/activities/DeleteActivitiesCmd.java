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
package com.java110.community.cmd.activities;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.community.IActivitiesV1InnerServiceSMO;
import com.java110.po.activities.ActivitiesPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 删除社区活动命令类
 * 
 * 该类负责处理删除社区活动的业务逻辑，通过接收前端请求参数，
 * 验证参数有效性后调用服务层删除指定的活动记录
 * 
 * 服务编码：activities.deleteActivities
 * 请求路径：/app/activities.DeleteActivities
 * 
 * @author 吴学文 at 2022-06-19 10:49:17 mail: 928255095@qq.com
 * @open source address: https://gitee.com/wuxw7/MicroCommunity
 * @官网：http://www.homecommunity.cn
 * 
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "activities.deleteActivities")
public class DeleteActivitiesCmd extends Cmd {
    
    /**
     * 日志记录器，用于记录命令执行过程中的日志信息
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteActivitiesCmd.class);

    /**
     * 社区活动服务接口，用于调用活动相关的业务操作
     */
    @Autowired
    private IActivitiesV1InnerServiceSMO activitiesV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的活动ID字段，确保删除操作有明确的标识
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象，包含前端传递的活动信息
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含activitiesId字段且不能为空
        // 该字段是删除活动的唯一标识，必须提供
        Assert.hasKeyAndValue(reqJson, "activitiesId", "activitiesId不能为空");
    }

    /**
     * 执行删除活动命令
     * 
     * 将请求参数转换为活动实体对象，调用服务层删除活动记录，
     * 并根据删除结果返回相应的响应信息
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于设置响应结果
     * @param reqJson 请求参数的JSON对象，包含要删除的活动信息
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为ActivitiesPo实体对象
        // 使用BeanConvertUtil工具类进行对象转换，简化参数映射过程
        ActivitiesPo activitiesPo = BeanConvertUtil.covertBean(reqJson, ActivitiesPo.class);
        
        // 调用服务层接口删除活动记录，返回受影响的行数
        // 通过activitiesV1InnerServiceSMOImpl服务接口执行实际的数据库删除操作
        int flag = activitiesV1InnerServiceSMOImpl.deleteActivities(activitiesPo);

        // 检查删除操作是否成功，如果受影响行数小于1则认为删除失败
        // flag表示数据库操作影响的行数，正常情况下应该为1（删除一条记录）
        if (flag < 1) {
            // 删除失败时抛出CmdException异常，中断流程并返回错误信息
            throw new CmdException("删除数据失败");
        }

        // 设置响应结果为成功状态
        // 使用ResultVo.success()创建成功的响应结果，并设置到上下文中返回给前端
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}