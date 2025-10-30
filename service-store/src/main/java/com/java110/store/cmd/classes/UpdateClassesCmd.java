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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
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
 * 班级信息更新命令类
 * 负责处理班级信息的更新请求，包括班级基本信息和班级时间安排的更新
 * 使用事务保证数据一致性
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-11-01
 */
@Java110Cmd(serviceCode = "classes.updateClasses")
public class UpdateClassesCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(UpdateClassesCmd.class);

    /**
     * 班级信息服务接口
     */
    @Autowired
    private IClassesV1InnerServiceSMO classesV1InnerServiceSMOImpl;

    /**
     * 班级时间信息服务接口
     */
    @Autowired
    private IClassesTimeV1InnerServiceSMO classesTimeV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证classesId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "classesId", "classesId不能为空");

        // 检查是否包含时间安排数据
        if(!reqJson.containsKey("times")){
            throw new CmdException("未包含时间");
        }

        // 获取时间安排数组
        JSONArray times = reqJson.getJSONArray("times");

        // 验证时间安排数组是否至少包含一个元素
        if(times.size() < 1){
            throw new CmdException("未包含时间");
        }
    }

    /**
     * 执行班级更新命令
     * 更新班级基本信息，删除原有时间安排，并添加新的时间安排
     * 使用事务保证操作的原子性
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 将请求JSON数据转换为班级POJO对象
        ClassesPo classesPo = BeanConvertUtil.covertBean(reqJson, ClassesPo.class);
        
        // 更新班级基本信息
        int flag = classesV1InnerServiceSMOImpl.updateClasses(classesPo);

        // 检查更新是否成功
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }
        
        // 创建班级时间POJO对象用于删除操作
        ClassesTimePo classesTimePo = null;
        classesTimePo = new ClassesTimePo();
        // 设置班级ID，用于删除该班级的所有时间安排
        classesTimePo.setClassesId(classesPo.getClassesId());
        
        // 删除该班级原有的所有时间安排
        flag = classesTimeV1InnerServiceSMOImpl.deleteClassesTime(classesTimePo);
        
        // 检查删除是否成功
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }
        
        // 获取新的时间安排数组
        JSONArray times = reqJson.getJSONArray("times");
        JSONObject time = null;

        // 遍历时间安排数组，逐个添加新的时间安排
        for(int timeIndex = 0; timeIndex < times.size() ; timeIndex ++){
            // 获取单个时间安排对象
            time = times.getJSONObject(timeIndex);
            
            // 创建新的班级时间POJO对象
            classesTimePo = new ClassesTimePo();
            // 设置班级ID
            classesTimePo.setClassesId(classesPo.getClassesId());
            // 设置开始时间
            classesTimePo.setStartTime(time.getString("startTime"));
            // 设置结束时间
            classesTimePo.setEndTime(time.getString("endTime"));
            // 生成唯一的时间ID
            classesTimePo.setTimeId(GenerateCodeFactory.getGeneratorId("11"));
            
            // 保存时间安排到数据库
            flag = classesTimeV1InnerServiceSMOImpl.saveClassesTime(classesTimePo);
            
            // 检查保存是否成功
            if (flag < 1) {
                throw new CmdException("保存数据失败");
            }
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}