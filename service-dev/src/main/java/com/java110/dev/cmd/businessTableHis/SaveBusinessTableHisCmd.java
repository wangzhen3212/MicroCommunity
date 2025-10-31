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
package com.java110.dev.cmd.businessTableHis;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.dev.IBusinessTableHisV1InnerServiceSMO;
import com.java110.po.business.BusinessTableHisPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;

/**
 * 类表述：保存业务表历史记录
 * 服务编码：businessTableHis.SaveBusinessTableHis
 * 请求路径：/app/businessTableHis.SaveBusinessTableHis
 * add by 吴学文 at 2021-09-12 13:58:05 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "businessTableHis.saveBusinessTableHis")
public class SaveBusinessTableHisCmd extends Cmd {

    // 日志记录器
    private static Logger logger = LoggerFactory.getLogger(SaveBusinessTableHisCmd.class);

    // ID生成前缀
    public static final String CODE_PREFIX_ID = "10";

    // 业务表历史记录服务接口
    @Autowired
    private IBusinessTableHisV1InnerServiceSMO businessTableHisV1InnerServiceSMOImpl;

    /**
     * 参数校验方法
     * 验证请求参数是否完整和有效
     * 
     * @param event 命令事件
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 校验必填参数
        Assert.hasKeyAndValue(reqJson, "action", "请求报文中未包含action");
        Assert.hasKeyAndValue(reqJson, "actionObj", "请求报文中未包含actionObj");
        Assert.hasKeyAndValue(reqJson, "actionObjHis", "请求报文中未包含actionObjHis");
        Assert.hasKeyAndValue(reqJson, "businessTypeCd", "请求报文中未包含businessTypeCd");
    }

    /**
     * 业务处理方法
     * 保存业务表历史记录到数据库
     * 
     * @param event 命令事件
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 命令异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON对象转换为业务实体对象
        BusinessTableHisPo businessTableHisPo = BeanConvertUtil.covertBean(reqJson, BusinessTableHisPo.class);
        
        // 生成历史记录ID
        businessTableHisPo.setHisId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务保存数据
        int flag = businessTableHisV1InnerServiceSMOImpl.saveBusinessTableHis(businessTableHisPo);

        // 检查保存结果
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}