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
import com.java110.dto.classes.ClassesDto;
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
 * 类表述：保存课程信息命令类
 * 服务编码：classes.saveClasses
 * 请求路劲：/app/classes.SaveClasses
 * 功能描述：该类负责处理课程信息的保存请求，包括课程基本信息及其关联的时间安排信息的保存
 * add by 吴学文 at 2022-11-01 00:27:10 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "classes.saveClasses")
public class SaveClassesCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveClassesCmd.class);

    /**
     * 课程ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

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
     * 功能描述：验证请求参数是否完整有效，包括课程名称和时间安排信息的验证
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求报文中必须包含课程名称
        Assert.hasKeyAndValue(reqJson, "name", "请求报文中未包含name");

        // 验证请求报文中必须包含时间安排信息
        if(!reqJson.containsKey("times")){
            throw new CmdException("未包含时间");
        }

        // 获取时间安排数组
        JSONArray times = reqJson.getJSONArray("times");

        // 验证时间安排数组不能为空
        if(times.size() < 1){
            throw new CmdException("未包含时间");
        }

    }

    /**
     * 执行保存课程信息命令
     * 功能描述：保存课程基本信息及其关联的时间安排信息，使用事务保证数据一致性
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 从请求头中获取店铺ID
        String storeId = cmdDataFlowContext.getReqHeaders().get("store-id");

        // 将请求JSON转换为课程PO对象
        ClassesPo classesPo = BeanConvertUtil.covertBean(reqJson, ClassesPo.class);
        // 生成课程ID
        classesPo.setClassesId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        // 设置课程状态为启用状态
        classesPo.setState(ClassesDto.STATE_START);
        // 设置店铺ID
        classesPo.setStoreId(storeId);
        // 保存课程基本信息
        int flag = classesV1InnerServiceSMOImpl.saveClasses(classesPo);

        // 验证课程信息保存是否成功
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 获取时间安排数组
        JSONArray times = reqJson.getJSONArray("times");
        JSONObject time = null;
        ClassesTimePo classesTimePo = null;
        // 遍历时间安排数组，保存每个时间安排信息
        for(int timeIndex = 0; timeIndex < times.size() ; timeIndex ++){
            time = times.getJSONObject(timeIndex);
            classesTimePo = new ClassesTimePo();
            // 设置课程ID，关联课程基本信息
            classesTimePo.setClassesId(classesPo.getClassesId());
            // 设置开始时间
            classesTimePo.setStartTime(time.getString("startTime"));
            // 设置结束时间
            classesTimePo.setEndTime(time.getString("endTime"));
            // 生成时间安排ID
            classesTimePo.setTimeId(GenerateCodeFactory.getGeneratorId("11"));
            // 保存时间安排信息
            flag = classesTimeV1InnerServiceSMOImpl.saveClassesTime(classesTimePo);
            // 验证时间安排信息保存是否成功
            if (flag < 1) {
                throw new CmdException("保存数据失败");
            }
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}