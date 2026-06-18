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
import com.java110.intf.store.IReserveGoodsDetailV1InnerServiceSMO;
import com.java110.intf.store.IReserveGoodsV1InnerServiceSMO;
import com.java110.po.reserve.ReserveGoodsPo;
import com.java110.po.reserve.ReserveGoodsDetailPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：保存
 * 服务编码：reserveGoods.saveReserveGoods
 * 请求路劲：/app/reserveGoods.SaveReserveGoods
 * add by 吴学文 at 2022-12-05 18:25:18 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 预约商品保存命令类
 * 
 * 该类负责处理预约商品的保存请求，包括商品基本信息验证、商品数据保存和商品详情数据保存
 * 实现了完整的预约商品创建流程，确保数据的完整性和一致性
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-12-05
 */
@Java110Cmd(serviceCode = "reserve.saveReserveGoods")
public class SaveReserveGoodsCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveReserveGoodsCmd.class);

    /**
     * ID生成前缀，用于生成商品ID和详情ID
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 预约商品服务接口，用于操作预约商品基础信息
     */
    @Autowired
    private IReserveGoodsV1InnerServiceSMO reserveGoodsV1InnerServiceSMOImpl;

    /**
     * 预约商品详情服务接口，用于操作预约商品详情信息
     */
    @Autowired
    private IReserveGoodsDetailV1InnerServiceSMO reserveGoodsDetailV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求报文中的必需字段是否完整，确保后续业务处理所需数据齐全
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数JSON对象，包含前端传递的所有参数
     * @throws CmdException 当必需参数缺失时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证小区ID参数
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        // 验证分类ID参数
        Assert.hasKeyAndValue(reqJson, "catalogId", "请求报文中未包含catalogId");
        // 验证商品名称参数
        Assert.hasKeyAndValue(reqJson, "goodsName", "请求报文中未包含goodsName");
        // 验证商品描述参数
        Assert.hasKeyAndValue(reqJson, "goodsDesc", "请求报文中未包含goodsDesc");
        // 验证商品类型参数
        Assert.hasKeyAndValue(reqJson, "type", "请求报文中未包含type");
        // 验证参数ID参数
        Assert.hasKeyAndValue(reqJson, "paramsId", "请求报文中未包含paramsId");
        // 验证价格参数
        Assert.hasKeyAndValue(reqJson, "price", "请求报文中未包含price");
        // 验证开始日期参数
        Assert.hasKeyAndValue(reqJson, "startDate", "请求报文中未包含startDate");
        // 验证结束日期参数
        Assert.hasKeyAndValue(reqJson, "endDate", "请求报文中未包含endDate");
        // 验证图片URL参数
        Assert.hasKeyAndValue(reqJson, "imgUrl", "请求报文中未包含imgUrl");
    }

    /**
     * 执行预约商品保存命令
     * 
     * 该方法负责保存预约商品的基本信息和详情信息，采用事务处理确保数据一致性
     * 包括：生成商品ID、保存商品基本信息、保存商品详情信息
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当保存操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 将请求JSON转换为预约商品PO对象
        ReserveGoodsPo reserveGoodsPo = BeanConvertUtil.covertBean(reqJson, ReserveGoodsPo.class);
        // 生成商品ID
        reserveGoodsPo.setGoodsId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        // 保存预约商品基本信息
        int flag = reserveGoodsV1InnerServiceSMOImpl.saveReserveGoods(reserveGoodsPo);

        // 检查商品基本信息保存结果
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 创建预约商品详情对象
        ReserveGoodsDetailPo reserveGoodsDetailPo = new ReserveGoodsDetailPo();
        // 设置商品ID，关联商品基本信息
        reserveGoodsDetailPo.setGoodsId(reserveGoodsPo.getGoodsId());
        // 设置小区ID
        reserveGoodsDetailPo.setCommunityId(reserveGoodsPo.getCommunityId());
        // 设置商品详情内容
        reserveGoodsDetailPo.setContent(reqJson.getString("content"));
        // 生成详情ID
        reserveGoodsDetailPo.setDetailId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        // 保存预约商品详情信息
        flag = reserveGoodsDetailV1InnerServiceSMOImpl.saveReserveGoodsDetail(reserveGoodsDetailPo);
        // 检查商品详情保存结果
        if (flag < 1) {
            throw new CmdException("保存详情失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}