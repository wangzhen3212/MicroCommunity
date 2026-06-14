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
package com.java110.store.cmd.classes;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.store.IClassesTimeV1InnerServiceSMO;
import com.java110.intf.store.IClassesV1InnerServiceSMO;
import com.java110.po.classes.ClassesPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * 类表述：更新
 * 服务编码：classes.updateClasses
 * 请求路劲：/app/classes.UpdateClasses
 * add by 吴学文 at 2022-11-01 00:27:10 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 班级状态更新命令类
 * 
 * 该类负责处理班级状态更新的业务逻辑，包括参数验证和状态更新操作。
 * 通过注解@Java110Cmd标识为命令类，服务编码为"classes.updateClassesState"。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-11-01
 */
@Java110Cmd(serviceCode = "classes.updateClassesState")
public class UpdateClassesStateCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(UpdateClassesStateCmd.class);

    /**
     * 班级服务接口
     * 用于操作班级数据的增删改查
     */
    @Autowired
    private IClassesV1InnerServiceSMO classesV1InnerServiceSMOImpl;

    /**
     * 班级时间服务接口
     * 用于处理班级时间相关的业务逻辑
     */
    @Autowired
    private IClassesTimeV1InnerServiceSMO classesTimeV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段，确保业务逻辑执行的正确性。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含classesId字段
        Assert.hasKeyAndValue(reqJson, "classesId", "classesId不能为空");
        // 验证请求参数中必须包含state字段
        Assert.hasKeyAndValue(reqJson, "state", "状态不能为空");
    }

    /**
     * 执行命令方法
     * 
     * 处理班级状态更新的核心业务逻辑，包括数据转换、数据库更新操作。
     * 使用@Java110Transactional注解确保事务性操作。
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON数据转换为班级POJO对象
        ClassesPo classesPo = BeanConvertUtil.covertBean(reqJson, ClassesPo.class);
        
        // 调用班级服务接口更新班级状态
        int flag = classesV1InnerServiceSMOImpl.updateClasses(classesPo);

        // 检查更新操作是否成功，flag小于1表示更新失败
        if (flag < 1) {
            // 抛出命令异常，提示更新数据失败
            throw new CmdException("更新数据失败");
        }
        
        // 设置响应结果为成功状态
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}