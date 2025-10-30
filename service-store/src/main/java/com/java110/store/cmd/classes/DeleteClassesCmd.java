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
import com.java110.po.classes.ClassesTimePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 删除课程信息命令类
 * 
 * 该类负责处理删除课程及其相关时间信息的业务逻辑，包括参数验证和事务性删除操作
 * 服务编码：classes.deleteClasses
 * 请求路径：/app/classes.DeleteClasses
 * 
 * @author 吴学文 at 2022-11-01 00:27:10 mail: 928255095@qq.com
 * @open source address: https://gitee.com/wuxw7/MicroCommunity
 * @官网：http://www.homecommunity.cn
 * @温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "classes.deleteClasses")
public class DeleteClassesCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteClassesCmd.class);

    /**
     * 课程信息服务接口
     */
    @Autowired
    private IClassesV1InnerServiceSMO classesV1InnerServiceSMOImpl;

    /**
     * 课程时间信息服务接口
     */
    @Autowired
    private IClassesTimeV1InnerServiceSMO classesTimeV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的课程ID信息
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于处理数据流
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含classesId字段
        Assert.hasKeyAndValue(reqJson, "classesId", "classesId不能为空");
    }

    /**
     * 执行删除课程命令
     * 
     * 该方法在事务中执行，先删除课程信息，再删除对应的课程时间信息
     * 如果任一删除操作失败，则抛出异常并回滚事务
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为课程PO对象
        ClassesPo classesPo = BeanConvertUtil.covertBean(reqJson, ClassesPo.class);
        
        // 调用服务删除课程信息
        int flag = classesV1InnerServiceSMOImpl.deleteClasses(classesPo);

        // 检查删除操作是否成功
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 创建课程时间PO对象，用于删除对应的课程时间信息
        ClassesTimePo classesTimePo = null;
        classesTimePo = new ClassesTimePo();
        // 设置课程ID，关联要删除的课程时间记录
        classesTimePo.setClassesId(classesPo.getClassesId());
        
        // 调用服务删除课程时间信息
        flag = classesTimeV1InnerServiceSMOImpl.deleteClassesTime(classesTimePo);
        
        // 检查删除操作是否成功
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}