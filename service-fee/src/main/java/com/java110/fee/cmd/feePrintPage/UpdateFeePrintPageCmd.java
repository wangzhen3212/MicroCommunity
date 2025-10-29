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
 * 类表述：更新
 * 服务编码：feePrintPage.updateFeePrintPage
 * 请求路劲：/app/feePrintPage.UpdateFeePrintPage
 * add by 吴学文 at 2021-09-16 22:26:04 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 费用打印页面更新命令类
 * 
 * 该类负责处理费用打印页面的更新操作，包括参数验证和业务逻辑处理。
 * 主要功能包括更新打印页面状态，确保同一小区内只有一个模板处于激活状态。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2021-09-16
 */
@Java110Cmd(serviceCode = "feePrintPage.updateFeePrintPage")
public class UpdateFeePrintPageCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdateFeePrintPageCmd.class);

    /**
     * 费用打印页面服务接口
     */
    @Autowired
    private IFeePrintPageV1InnerServiceSMO feePrintPageV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段，确保更新操作的数据完整性。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证pageId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "pageId", "pageId不能为空");
        // 验证communityId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行更新命令方法
     * 
     * 处理费用打印页面的更新逻辑，包括状态管理和数据持久化操作。
     * 当设置某个模板为激活状态时，会自动将同小区的其他模板设置为非激活状态。
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为费用打印页面POJO对象
        FeePrintPagePo feePrintPagePo = BeanConvertUtil.covertBean(reqJson, FeePrintPagePo.class);
        
        // 如果当前要设置的模板状态为激活状态（T）
        if("T".equals(feePrintPagePo.getState())){
            // 创建新的POJO对象用于更新其他模板
            FeePrintPagePo newFeePrintPagePo = new FeePrintPagePo();
            // 设置小区ID，确保只更新同一小区的模板
            newFeePrintPagePo.setCommunityId(feePrintPagePo.getCommunityId());
            // 将所有其他模板状态设置为非激活状态（F）
            newFeePrintPagePo.setState("F");
            // 设置查询状态码为0，避免更新已删除的记录
            newFeePrintPagePo.setQueryStatusCd("0");
            // 执行批量更新操作，将同小区其他模板设置为非激活状态
            feePrintPageV1InnerServiceSMOImpl.updateFeePrintPage(newFeePrintPagePo);
        }

        // 更新当前指定的费用打印页面模板
        int flag = feePrintPageV1InnerServiceSMOImpl.updateFeePrintPage(feePrintPagePo);

        // 检查更新操作是否成功，如果影响行数小于1则认为更新失败
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}