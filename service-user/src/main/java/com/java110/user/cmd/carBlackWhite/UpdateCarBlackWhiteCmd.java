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
package com.java110.user.cmd.carBlackWhite;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.user.ICarBlackWhiteV1InnerServiceSMO;
import com.java110.po.car.CarBlackWhitePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：更新
 * 服务编码：carBlackWhite.updateCarBlackWhite
 * 请求路劲：/app/carBlackWhite.UpdateCarBlackWhite
 * add by 吴学文 at 2022-07-16 23:23:24 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 车辆黑白名单更新命令类
 * 
 * 该类负责处理车辆黑白名单信息的更新操作，通过接收前端请求数据，
 * 验证数据完整性后更新数据库中的黑白名单记录
 * 
 * @author 吴学文
 * @version 1.0
 * @date 2022-07-16
 */
@Java110Cmd(serviceCode = "carBlackWhite.updateCarBlackWhite")
public class UpdateCarBlackWhiteCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(UpdateCarBlackWhiteCmd.class);

    /**
     * 车辆黑白名单服务接口
     */
    @Autowired
    private ICarBlackWhiteV1InnerServiceSMO carBlackWhiteV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段，确保更新操作的数据完整性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证黑白名单ID是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "bwId", "bwId不能为空");
        // 验证小区ID是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行车辆黑白名单更新命令
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

        // 将JSON请求参数转换为车辆黑白名单实体对象
        CarBlackWhitePo carBlackWhitePo = BeanConvertUtil.covertBean(reqJson, CarBlackWhitePo.class);
        
        // 调用服务层接口更新车辆黑白名单信息
        int flag = carBlackWhiteV1InnerServiceSMOImpl.updateCarBlackWhite(carBlackWhitePo);

        // 检查更新操作是否成功
        if (flag < 1) {
            // 更新失败，抛出异常
            throw new CmdException("更新数据失败");
        }

        // 设置成功响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}