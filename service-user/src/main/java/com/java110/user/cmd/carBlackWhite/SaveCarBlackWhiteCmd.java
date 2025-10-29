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
 * 类表述：保存车辆黑白名单命令类
 * 服务编码：carBlackWhite.saveCarBlackWhite
 * 请求路劲：/app/carBlackWhite.SaveCarBlackWhite
 * add by 吴学文 at 2022-07-16 23:23:24 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * @description 车辆黑白名单保存命令处理类，负责接收前端请求并保存车辆黑白名单信息
 * @author 吴学文
 * @date 2022-07-16
 * @version 1.0
 */
@Java110Cmd(serviceCode = "carBlackWhite.saveCarBlackWhite")
public class SaveCarBlackWhiteCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveCarBlackWhiteCmd.class);

    /**
     * ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 车辆黑白名单服务接口
     */
    @Autowired
    private ICarBlackWhiteV1InnerServiceSMO carBlackWhiteV1InnerServiceSMOImpl;

    /**
     * 参数校验方法
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数校验失败时抛出异常
     * @description 验证请求参数是否完整，包括名单类型、车牌号、小区ID、开始时间和结束时间
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 校验名单类型参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "blackWhite", "必填，请填写名单类型");
        // 校验车牌号参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "carNum", "必填，请填写车牌号");
        // 校验小区ID参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "communityId", "必填，请填写小区ID");
        // 校验开始时间参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "startTime", "必填，请选择开始时间");
        // 校验结束时间参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "endTime", "必填，请选择结束时间");
    }

    /**
     * 执行保存车辆黑白名单命令
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     * @description 将请求参数转换为实体对象，生成唯一ID，调用服务层保存数据，并设置响应结果
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为车辆黑白名单实体对象
        CarBlackWhitePo carBlackWhitePo = BeanConvertUtil.covertBean(reqJson, CarBlackWhitePo.class);
        // 生成黑白名单记录的唯一ID
        carBlackWhitePo.setBwId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务层保存车辆黑白名单信息
        int flag = carBlackWhiteV1InnerServiceSMOImpl.saveCarBlackWhite(carBlackWhitePo);

        // 检查保存操作是否成功，flag小于1表示保存失败
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置成功响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}