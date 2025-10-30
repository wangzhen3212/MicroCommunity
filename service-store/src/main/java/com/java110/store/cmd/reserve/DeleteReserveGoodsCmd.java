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
import com.java110.intf.store.IReserveGoodsV1InnerServiceSMO;
import com.java110.po.reserve.ReserveGoodsPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 删除预约商品命令类
 * 
 * 该类负责处理删除预约商品的业务逻辑，包括参数验证和执行删除操作。
 * 服务编码：reserveGoods.deleteReserveGoods
 * 请求路径：/app/reserveGoods.DeleteReserveGoods
 * 
 * @author 吴学文 at 2022-12-05 18:25:18 mail: 928255095@qq.com
 * @version 1.0
 * @see Cmd
 * @see IReserveGoodsV1InnerServiceSMO
 */
@Java110Cmd(serviceCode = "reserve.deleteReserveGoods")
public class DeleteReserveGoodsCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteReserveGoodsCmd.class);

    /**
     * 预约商品服务接口
     */
    @Autowired
    private IReserveGoodsV1InnerServiceSMO reserveGoodsV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段：goodsId和communityId
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证goodsId参数不能为空
        Assert.hasKeyAndValue(reqJson, "goodsId", "goodsId不能为空");
        // 验证communityId参数不能为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行删除预约商品命令
     * 
     * 将请求参数转换为预约商品PO对象，调用服务层执行删除操作，
     * 并根据删除结果设置响应信息
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为ReserveGoodsPo对象
        ReserveGoodsPo reserveGoodsPo = BeanConvertUtil.covertBean(reqJson, ReserveGoodsPo.class);
        
        // 调用服务层接口执行删除操作，返回受影响的行数
        int flag = reserveGoodsV1InnerServiceSMOImpl.deleteReserveGoods(reserveGoodsPo);

        // 检查删除操作是否成功，如果受影响行数小于1，则认为删除失败
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}