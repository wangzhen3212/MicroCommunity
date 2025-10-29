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
package com.java110.fee.cmd.feeCombo;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.fee.IFeeComboV1InnerServiceSMO;
import com.java110.po.fee.FeeComboPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 删除费用套餐命令类
 * 
 * 该类负责处理删除费用套餐的业务逻辑，通过服务编码"feeCombo.deleteFeeCombo"标识，
 * 接收前端请求并删除指定的费用套餐信息
 * 
 * @author 吴学文
 * @version 1.0
 * @date 2022-05-07 15:31:19
 * @mail 928255095@qq.com
 * @see Cmd
 * @see IFeeComboV1InnerServiceSMO
 */
@Java110Cmd(serviceCode = "feeCombo.deleteFeeCombo")
public class DeleteFeeComboCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteFeeComboCmd.class);

    /**
     * 费用套餐服务接口
     */
    @Autowired
    private IFeeComboV1InnerServiceSMO feeComboV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段，确保删除操作的有效性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws IllegalArgumentException 当必要参数缺失时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含comboId字段
        Assert.hasKeyAndValue(reqJson, "comboId", "comboId不能为空");
        // 验证请求参数中必须包含communityId字段
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行删除费用套餐命令
     * 
     * 将请求参数转换为费用套餐对象，调用服务层进行删除操作，
     * 并根据删除结果设置响应信息
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为FeeComboPo对象
        FeeComboPo feeComboPo = BeanConvertUtil.covertBean(reqJson, FeeComboPo.class);
        
        // 调用服务层删除费用套餐，返回影响的行数
        int flag = feeComboV1InnerServiceSMOImpl.deleteFeeCombo(feeComboPo);

        // 检查删除操作是否成功，影响行数小于1表示删除失败
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 设置成功响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}