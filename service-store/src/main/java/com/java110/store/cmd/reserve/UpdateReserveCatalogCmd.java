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
 * 类表述：更新
 * 服务编码：reserveCatalog.updateReserveCatalog
 * 请求路劲：/app/reserveCatalog.UpdateReserveCatalog
 * add by 吴学文 at 2022-12-05 17:56:29 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 预约目录更新命令类
 * 
 * 该类负责处理预约目录信息的更新操作，包括参数验证和业务逻辑处理
 * 通过注解标识服务编码，与前端请求进行映射
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-12-05
 */
@Java110Cmd(serviceCode = "reserve.updateReserveCatalog")
public class UpdateReserveCatalogCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(UpdateReserveCatalogCmd.class);

    /**
     * 预约目录服务接口
     * 用于调用底层的预约目录数据操作
     */
    @Autowired
    private IReserveCatalogV1InnerServiceSMO reserveCatalogV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整和有效，确保必要的参数不为空
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证catalogId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "catalogId", "catalogId不能为空");
        // 验证communityId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 命令执行方法
     * 
     * 执行预约目录更新的核心业务逻辑，包括数据转换和服务调用
     * 使用事务注解确保数据操作的原子性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为预约目录POJO对象
        ReserveCatalogPo reserveCatalogPo = BeanConvertUtil.covertBean(reqJson, ReserveCatalogPo.class);
        
        // 调用服务层接口更新预约目录信息
        int flag = reserveCatalogV1InnerServiceSMOImpl.updateReserveCatalog(reserveCatalogPo);

        // 检查更新操作是否成功，flag表示受影响的行数
        if (flag < 1) {
            // 更新失败时抛出异常
            throw new CmdException("更新数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}