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
package com.java110.fee.cmd.ownerCarNew;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.fee.IOwnerCarNewV1InnerServiceSMO;
import com.java110.po.car.OwnerCarPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;

/**
 * 类表述：保存
 * 服务编码：ownerCarNew.saveOwnerCarNew
 * 请求路劲：/app/ownerCarNew.SaveOwnerCarNew
 * add by 吴学文 at 2021-12-06 22:09:47 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 业主车辆信息保存命令类
 * 
 * 该类负责处理业主车辆信息的保存请求，包括参数验证和数据持久化操作
 * 通过注解@Java110Cmd指定服务编码，用于路由到对应的命令处理器
 * 
 * @author 吴学文
 * @version 1.0
 * @date 2021-12-06
 */
@Java110Cmd(serviceCode = "ownerCarNew.saveOwnerCarNew")
public class SaveOwnerCarNewCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveOwnerCarNewCmd.class);

    /**
     * ID生成前缀，用于生成车辆ID
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 业主车辆信息服务接口
     */
    @Autowired
    private IOwnerCarNewV1InnerServiceSMO ownerCarNewV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求报文中是否包含所有必需的参数，如果缺少必要参数则抛出异常
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求报文JSON对象，包含前端传递的参数
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求报文中是否包含carId参数
        Assert.hasKeyAndValue(reqJson, "carId", "请求报文中未包含carId");
        // 验证请求报文中是否包含ownerId参数
        Assert.hasKeyAndValue(reqJson, "ownerId", "请求报文中未包含ownerId");
        // 验证请求报文中是否包含carNum参数
        Assert.hasKeyAndValue(reqJson, "carNum", "请求报文中未包含carNum");
        // 验证请求报文中是否包含carBrand参数
        Assert.hasKeyAndValue(reqJson, "carBrand", "请求报文中未包含carBrand");
        // 验证请求报文中是否包含carType参数
        Assert.hasKeyAndValue(reqJson, "carType", "请求报文中未包含carType");
        // 验证请求报文中是否包含carColor参数
        Assert.hasKeyAndValue(reqJson, "carColor", "请求报文中未包含carColor");
        // 验证请求报文中是否包含psId参数
        Assert.hasKeyAndValue(reqJson, "psId", "请求报文中未包含psId");
        // 验证请求报文中是否包含userId参数
        Assert.hasKeyAndValue(reqJson, "userId", "请求报文中未包含userId");
        // 验证请求报文中是否包含communityId参数
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        // 验证请求报文中是否包含startTime参数
        Assert.hasKeyAndValue(reqJson, "startTime", "请求报文中未包含startTime");
        // 验证请求报文中是否包含endTime参数
        Assert.hasKeyAndValue(reqJson, "endTime", "请求报文中未包含endTime");
        // 验证请求报文中是否包含state参数
        Assert.hasKeyAndValue(reqJson, "state", "请求报文中未包含state");
        // 验证请求报文中是否包含carTypeCd参数
        Assert.hasKeyAndValue(reqJson, "carTypeCd", "请求报文中未包含carTypeCd");
        // 验证请求报文中是否包含memberId参数
        Assert.hasKeyAndValue(reqJson, "memberId", "请求报文中未包含memberId");
    }

    /**
     * 执行保存业主车辆信息命令
     * 
     * 将请求参数转换为实体对象，生成车辆ID，调用服务层保存数据
     * 使用@Java110Transactional注解确保操作的事务性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求报文JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为OwnerCarPo实体对象
        OwnerCarPo ownerCarNewPo = BeanConvertUtil.covertBean(reqJson, OwnerCarPo.class);
        // 生成车辆ID，使用前缀+序列号的方式
        ownerCarNewPo.setCarId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        // 调用服务层保存业主车辆信息
        int flag = ownerCarNewV1InnerServiceSMOImpl.saveOwnerCarNew(ownerCarNewPo);

        // 检查保存结果，如果影响行数小于1则认为保存失败
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}