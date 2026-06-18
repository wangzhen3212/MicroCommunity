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
package com.java110.fee.cmd.payFeeDetailNew;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.fee.IPayFeeDetailV1InnerServiceSMO;
import com.java110.po.fee.PayFeeDetailPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;

/**
 * 删除缴费明细新版本命令类
 * 
 * 该类负责处理删除缴费明细的请求，包含参数验证和业务逻辑处理
 * 服务编码：payFeeDetailNew.deletePayFeeDetailNew
 * 请求路径：/app/payFeeDetailNew.DeletePayFeeDetailNew
 * 
 * @author 吴学文 at 2021-12-06 21:10:16 mail: 928255095@qq.com
 * @version 1.0
 * @see Cmd
 * @see IPayFeeDetailV1InnerServiceSMO
 * @see PayFeeDetailPo
 */
@Java110Cmd(serviceCode = "payFeeDetailNew.deletePayFeeDetailNew")
public class DeletePayFeeDetailNewCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeletePayFeeDetailNewCmd.class);

    /**
     * 缴费明细服务接口
     */
    @Autowired
    private IPayFeeDetailV1InnerServiceSMO payFeeDetailNewV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含detailId字段
        Assert.hasKeyAndValue(reqJson, "detailId", "detailId不能为空");
        // 验证请求参数中必须包含communityId字段
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行删除缴费明细命令
     * 
     * 将请求参数转换为实体对象并调用服务层进行删除操作
     * 使用事务注解确保操作的原子性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为PayFeeDetailPo实体对象
        PayFeeDetailPo payFeeDetailNewPo = BeanConvertUtil.covertBean(reqJson, PayFeeDetailPo.class);
        
        // 调用服务层接口执行删除操作，返回受影响的行数
        int flag = payFeeDetailNewV1InnerServiceSMOImpl.deletePayFeeDetailNew(payFeeDetailNewPo);

        // 检查删除操作是否成功，如果受影响行数小于1则认为删除失败
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}