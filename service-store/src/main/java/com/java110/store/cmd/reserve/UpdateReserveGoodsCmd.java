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
import com.java110.dto.reserve.ReserveGoodsDetailDto;
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

import java.util.List;


/**
 * 类表述：更新
 * 服务编码：reserveGoods.updateReserveGoods
 * 请求路劲：/app/reserveGoods.UpdateReserveGoods
 * add by 吴学文 at 2022-12-05 18:25:18 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 预约商品更新命令类
 * 负责处理预约商品的更新请求，包括商品基本信息更新和商品详情信息的维护
 * 服务编码：reserve.updateReserveGoods
 * 
 * @author 吴学文
 * @version 1.0
 * @date 2022-12-05
 */
@Java110Cmd(serviceCode = "reserve.updateReserveGoods")
public class UpdateReserveGoodsCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdateReserveGoodsCmd.class);

    /**
     * 预约商品服务接口
     */
    @Autowired
    private IReserveGoodsV1InnerServiceSMO reserveGoodsV1InnerServiceSMOImpl;

    /**
     * 预约商品详情服务接口
     */
    @Autowired
    private IReserveGoodsDetailV1InnerServiceSMO reserveGoodsDetailV1InnerServiceSMOImpl;

    /**
     * 参数校验方法
     * 验证请求参数中是否包含必要的字段
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 校验goodsId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "goodsId", "goodsId不能为空");
        // 校验communityId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行更新预约商品命令
     * 更新预约商品基本信息，并根据情况更新或新增商品详情信息
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当更新或保存操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON数据转换为预约商品PO对象
        ReserveGoodsPo reserveGoodsPo = BeanConvertUtil.covertBean(reqJson, ReserveGoodsPo.class);
        // 调用服务更新预约商品基本信息
        int flag = reserveGoodsV1InnerServiceSMOImpl.updateReserveGoods(reserveGoodsPo);

        // 检查更新操作是否成功
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        // 查询商品详情信息
        ReserveGoodsDetailDto reserveGoodsDetailDto = new ReserveGoodsDetailDto();
        reserveGoodsDetailDto.setGoodsId(reserveGoodsPo.getGoodsId());
        List<ReserveGoodsDetailDto> reserveGoodsDetailDtos = reserveGoodsDetailV1InnerServiceSMOImpl.queryReserveGoodsDetails(reserveGoodsDetailDto);
        
        // 判断商品详情是否存在
        if(reserveGoodsDetailDtos == null || reserveGoodsDetailDtos.size()< 1){
            // 如果商品详情不存在，则创建新的商品详情记录
            ReserveGoodsDetailPo reserveGoodsDetailPo = new ReserveGoodsDetailPo();
            reserveGoodsDetailPo.setGoodsId(reserveGoodsPo.getGoodsId());
            reserveGoodsDetailPo.setCommunityId(reserveGoodsPo.getCommunityId());
            reserveGoodsDetailPo.setContent(reqJson.getString("content"));
            // 生成详情ID
            reserveGoodsDetailPo.setDetailId(GenerateCodeFactory.getGeneratorId("11"));
            // 保存商品详情信息
            flag = reserveGoodsDetailV1InnerServiceSMOImpl.saveReserveGoodsDetail(reserveGoodsDetailPo);
            if (flag < 1) {
                throw new CmdException("保存详情失败");
            }
        }else{
            // 如果商品详情已存在，则更新现有商品详情信息
            ReserveGoodsDetailPo reserveGoodsDetailPo = new ReserveGoodsDetailPo();
            reserveGoodsDetailPo.setGoodsId(reserveGoodsPo.getGoodsId());
            reserveGoodsDetailPo.setCommunityId(reserveGoodsPo.getCommunityId());
            reserveGoodsDetailPo.setContent(reqJson.getString("content"));
            // 使用已存在的详情ID
            reserveGoodsDetailPo.setDetailId(reserveGoodsDetailDtos.get(0).getDetailId());
            // 更新商品详情信息
            reserveGoodsDetailV1InnerServiceSMOImpl.updateReserveGoodsDetail(reserveGoodsDetailPo);
        }
        
        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}