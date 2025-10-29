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
package com.java110.fee.cmd.feePrintPage;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.fee.IFeePrintPageV1InnerServiceSMO;
import com.java110.po.fee.FeePrintPagePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 类表述：保存
 * 服务编码：feePrintPage.saveFeePrintPage
 * 请求路劲：/app/feePrintPage.SaveFeePrintPage
 * add by 吴学文 at 2021-09-16 22:26:04 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 费用打印页面保存命令类
 * 
 * 该类负责处理费用打印页面信息的保存操作，包括参数验证和业务逻辑处理
 * 通过注解@Java110Cmd标识为命令类，服务编码为feePrintPage.saveFeePrintPage
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2021-09-16
 */
@Java110Cmd(serviceCode = "feePrintPage.saveFeePrintPage")
public class SaveFeePrintPageCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveFeePrintPageCmd.class);

    /**
     * ID生成前缀，用于生成页面ID
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 费用打印页面服务接口
     */
    @Autowired
    private IFeePrintPageV1InnerServiceSMO feePrintPageV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否包含必需的字段：pageName、communityId、pageUrl
     * 如果缺少任一必需参数，将抛出异常
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param cmdDataFlowContext 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求报文中是否包含pageName参数
        Assert.hasKeyAndValue(reqJson, "pageName", "请求报文中未包含pageName");
        // 验证请求报文中是否包含communityId参数
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        // 验证请求报文中是否包含pageUrl参数
        Assert.hasKeyAndValue(reqJson, "pageUrl", "请求报文中未包含pageUrl");
    }

    /**
     * 执行保存费用打印页面命令
     * 
     * 将请求参数转换为FeePrintPagePo对象，生成页面ID，设置默认状态，
     * 调用服务层保存数据，并根据保存结果返回响应
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param cmdDataFlowContext 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当数据保存失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为FeePrintPagePo实体对象
        FeePrintPagePo feePrintPagePo = BeanConvertUtil.covertBean(reqJson, FeePrintPagePo.class);
        
        // 生成页面ID，使用前缀+自增序列的方式
        feePrintPagePo.setPageId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 设置页面状态为"F"，表示未激活或待处理状态
        feePrintPagePo.setState("F");
        
        // 调用服务层保存费用打印页面信息
        int flag = feePrintPageV1InnerServiceSMOImpl.saveFeePrintPage(feePrintPagePo);

        // 检查保存结果，如果影响行数小于1，则认为保存失败
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}