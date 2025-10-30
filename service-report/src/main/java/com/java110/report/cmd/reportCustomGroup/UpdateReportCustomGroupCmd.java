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
package com.java110.report.cmd.reportCustomGroup;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.report.IReportCustomGroupV1InnerServiceSMO;
import com.java110.po.reportCustom.ReportCustomGroupPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * 类表述：更新
 * 服务编码：reportCustomGroup.updateReportCustomGroup
 * 请求路劲：/app/reportCustomGroup.UpdateReportCustomGroup
 * add by 吴学文 at 2021-11-09 13:30:41 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 报表自定义分组更新命令类
 * 
 * 该类负责处理报表自定义分组的更新操作，包括参数验证和业务逻辑执行。
 * 通过注解标识服务编码，与前端请求路径对应，实现具体的更新功能。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2021-11-09
 */
@Java110Cmd(serviceCode = "reportCustomGroup.updateReportCustomGroup")
public class UpdateReportCustomGroupCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(UpdateReportCustomGroupCmd.class);

    /**
     * 报表自定义分组服务接口
     * 用于调用底层的业务逻辑处理
     */
    @Autowired
    private IReportCustomGroupV1InnerServiceSMO reportCustomGroupV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段，确保更新操作的完整性。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含groupId字段
        Assert.hasKeyAndValue(reqJson, "groupId", "groupId不能为空");
        // 重复验证groupId字段，确保数据完整性（可能存在业务逻辑需要）
        Assert.hasKeyAndValue(reqJson, "groupId", "groupId不能为空");
    }

    /**
     * 命令执行方法
     * 
     * 执行报表自定义分组的更新操作，包括数据转换、业务处理和数据返回。
     * 使用事务注解确保操作的原子性。
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为报表自定义分组POJO对象
        ReportCustomGroupPo reportCustomGroupPo = BeanConvertUtil.covertBean(reqJson, ReportCustomGroupPo.class);
        
        // 调用服务层接口执行更新操作，返回受影响的行数
        int flag = reportCustomGroupV1InnerServiceSMOImpl.updateReportCustomGroup(reportCustomGroupPo);

        // 检查更新操作是否成功，如果受影响行数小于1则认为更新失败
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        // 设置响应结果为成功状态
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}