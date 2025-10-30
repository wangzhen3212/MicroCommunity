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
package com.java110.store.cmd.reserve;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.store.IReserveCatalogV1InnerServiceSMO;
import com.java110.po.reserve.ReserveCatalogPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：保存预约目录信息命令类
 * 服务编码：reserveCatalog.saveReserveCatalog
 * 请求路径：/app/reserveCatalog.SaveReserveCatalog
 * 功能描述：该类用于处理预约目录的保存操作，包括参数验证和业务逻辑处理
 * add by 吴学文 at 2022-12-05 17:56:29 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "reserve.saveReserveCatalog")
public class SaveReserveCatalogCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveReserveCatalogCmd.class);

    /**
     * 目录ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 预约目录服务接口
     */
    @Autowired
    private IReserveCatalogV1InnerServiceSMO reserveCatalogV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 功能描述：验证请求参数是否完整和有效
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求报文中必须包含communityId参数
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        // 验证请求报文中必须包含name参数
        Assert.hasKeyAndValue(reqJson, "name", "请求报文中未包含name");
        // 验证请求报文中必须包含type参数
        Assert.hasKeyAndValue(reqJson, "type", "请求报文中未包含type");
    }

    /**
     * 执行保存预约目录命令
     * 功能描述：处理预约目录的保存业务逻辑，包括数据转换、ID生成和数据库操作
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为预约目录PO对象
        ReserveCatalogPo reserveCatalogPo = BeanConvertUtil.covertBean(reqJson, ReserveCatalogPo.class);
        
        // 生成目录ID，使用前缀+自增序列的方式
        reserveCatalogPo.setCatalogId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务层保存预约目录信息
        int flag = reserveCatalogV1InnerServiceSMOImpl.saveReserveCatalog(reserveCatalogPo);

        // 检查保存结果，如果影响行数小于1则认为保存失败
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}