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
package com.java110.user.cmd.examine;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.user.IExamineProjectV1InnerServiceSMO;
import com.java110.po.examine.ExamineProjectPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：保存
 * 服务编码：examineProject.saveExamineProject
 * 请求路劲：/app/examineProject.SaveExamineProject
 * add by 吴学文 at 2023-03-07 15:13:15 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 考核项目管理命令类
 * 负责处理考核项目的新增保存操作，包括参数验证和业务逻辑处理
 * 通过注解@Java110Cmd声明服务编码，与前端请求路由对应
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023-03-07
 */
@Java110Cmd(serviceCode = "examine.saveExamineProject")
public class SaveExamineProjectCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveExamineProjectCmd.class);

    /**
     * 项目ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 考核项目服务接口
     */
    @Autowired
    private IExamineProjectV1InnerServiceSMO examineProjectV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 验证请求报文中是否包含必要的参数，确保数据的完整性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证小区ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        // 验证考核项目名称参数是否存在
        Assert.hasKeyAndValue(reqJson, "name", "请求报文中未包含name");
        // 验证岗位编码参数是否存在
        Assert.hasKeyAndValue(reqJson, "postCd", "请求报文中未包含postCd");
        // 验证岗位名称参数是否存在
        Assert.hasKeyAndValue(reqJson, "post", "请求报文中未包含post");
        // 验证权重参数是否存在
        Assert.hasKeyAndValue(reqJson, "weight", "请求报文中未包含weight");
        // 验证状态参数是否存在
        Assert.hasKeyAndValue(reqJson, "state", "请求报文中未包含state");
    }

    /**
     * 执行考核项目保存命令
     * 将请求参数转换为考核项目实体对象，生成项目ID并保存到数据库
     * 使用@Java110Transactional注解确保数据库操作的原子性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为考核项目实体对象
        ExamineProjectPo examineProjectPo = BeanConvertUtil.covertBean(reqJson, ExamineProjectPo.class);
        
        // 生成考核项目ID，使用前缀+序列号的方式
        examineProjectPo.setProjectId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务层保存考核项目数据
        int flag = examineProjectV1InnerServiceSMOImpl.saveExamineProject(examineProjectPo);

        // 检查保存结果，如果影响行数小于1则认为保存失败
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}