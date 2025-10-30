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
package com.java110.store.cmd.resourceStoreTimes;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.store.IResourceStoreTimesV1InnerServiceSMO;
import com.java110.po.resource.ResourceStoreTimesPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：更新
 * 服务编码：resourceStoreTimes.updateResourceStoreTimes
 * 请求路劲：/app/resourceStoreTimes.UpdateResourceStoreTimes
 * add by 吴学文 at 2022-10-06 21:26:56 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 资源库存次数更新命令类
 * 
 * 该类负责处理资源库存次数的更新操作，通过接收前端请求数据，
 * 验证数据完整性后调用服务层进行数据更新
 * 
 * @author 吴学文
 * @version 1.0
 * @date 2022-10-06
 */
@Java110Cmd(serviceCode = "resourceStoreTimes.updateResourceStoreTimes")
public class UpdateResourceStoreTimesCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(UpdateResourceStoreTimesCmd.class);

    /**
     * 资源库存次数服务接口
     */
    @Autowired
    private IResourceStoreTimesV1InnerServiceSMO resourceStoreTimesV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数中必须包含timesId和storeId字段，确保数据的完整性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证timesId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "timesId", "timesId不能为空");
        // 验证storeId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "storeId", "storeId不能为空");
    }

    /**
     * 执行资源库存次数更新命令
     * 
     * 将请求参数转换为实体对象，调用服务层进行数据更新操作，
     * 并根据更新结果设置响应信息
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当数据更新失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 将请求JSON数据转换为ResourceStoreTimesPo实体对象
        ResourceStoreTimesPo resourceStoreTimesPo = BeanConvertUtil.covertBean(reqJson, ResourceStoreTimesPo.class);
        
        // 调用服务层接口更新资源库存次数数据
        int flag = resourceStoreTimesV1InnerServiceSMOImpl.updateResourceStoreTimes(resourceStoreTimesPo);

        // 检查更新操作是否成功，flag小于1表示更新失败
        if (flag < 1) {
            // 更新失败时抛出异常
            throw new CmdException("更新数据失败");
        }

        // 设置成功响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}