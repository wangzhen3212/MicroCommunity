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
 * 类表述：删除
 * 服务编码：reserveCatalog.deleteReserveCatalog
 * 请求路劲：/app/reserveCatalog.DeleteReserveCatalog
 * add by 吴学文 at 2022-12-05 17:56:29 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 预约目录删除命令类
 * 
 * 该类负责处理删除预约目录的业务逻辑，包括参数验证和执行删除操作。
 * 通过注解@Java110Cmd指定服务编码，与前端请求进行映射。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-12-05
 */
@Java110Cmd(serviceCode = "reserve.deleteReserveCatalog")
public class DeleteReserveCatalogCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteReserveCatalogCmd.class);

    /**
     * 预约目录服务接口
     * 用于调用预约目录相关的数据操作
     */
    @Autowired
    private IReserveCatalogV1InnerServiceSMO reserveCatalogV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否包含必要的字段，确保删除操作的基本数据完整性。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含catalogId字段
        Assert.hasKeyAndValue(reqJson, "catalogId", "catalogId不能为空");
        // 验证请求参数中必须包含communityId字段
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行删除命令方法
     * 
     * 将请求参数转换为预约目录PO对象，调用服务层执行删除操作，
     * 并根据删除结果设置响应信息。
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为ReserveCatalogPo对象
        ReserveCatalogPo reserveCatalogPo = BeanConvertUtil.covertBean(reqJson, ReserveCatalogPo.class);
        
        // 调用服务层删除预约目录，返回影响的行数
        int flag = reserveCatalogV1InnerServiceSMOImpl.deleteReserveCatalog(reserveCatalogPo);

        // 检查删除操作是否成功，flag小于1表示删除失败
        if (flag < 1) {
            // 删除失败时抛出异常
            throw new CmdException("删除数据失败");
        }

        // 设置成功响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}