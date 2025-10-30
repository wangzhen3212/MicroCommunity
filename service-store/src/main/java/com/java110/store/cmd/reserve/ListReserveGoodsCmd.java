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
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.reserve.ReserveGoodsDetailDto;
import com.java110.intf.store.IReserveGoodsDetailV1InnerServiceSMO;
import com.java110.intf.store.IReserveGoodsV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.reserve.ReserveGoodsDto;

import java.util.List;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询
 * 服务编码：reserveGoods.listReserveGoods
 * 请求路劲：/app/reserveGoods.ListReserveGoods
 * add by 吴学文 at 2022-12-05 18:25:18 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 预约商品查询命令类
 * 负责处理预约商品列表查询请求，包括分页查询和商品详情内容的关联查询
 * 服务编码：reserve.listReserveGoods
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-12-05
 */
@Java110Cmd(serviceCode = "reserve.listReserveGoods")
public class ListReserveGoodsCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListReserveGoodsCmd.class);
    
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
     * 参数验证方法
     * 验证请求参数的有效性，包括分页参数和必要字段
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证小区ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
    }

    /**
     * 命令执行方法
     * 处理预约商品列表查询业务逻辑，包括分页查询和商品详情关联
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为ReserveGoodsDto对象
        ReserveGoodsDto reserveGoodsDto = BeanConvertUtil.covertBean(reqJson, ReserveGoodsDto.class);

        // 查询预约商品总数
        int count = reserveGoodsV1InnerServiceSMOImpl.queryReserveGoodssCount(reserveGoodsDto);

        List<ReserveGoodsDto> reserveGoodsDtos = null;

        if (count > 0) {
            // 查询预约商品列表
            reserveGoodsDtos = reserveGoodsV1InnerServiceSMOImpl.queryReserveGoodss(reserveGoodsDto);
            // 查询商品详情内容
            queryContent(reserveGoodsDtos);
        } else {
            // 如果没有数据，返回空列表
            reserveGoodsDtos = new ArrayList<>();
        }

        // 计算总页数并构建返回结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, reserveGoodsDtos);

        // 设置HTTP响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 查询商品详情内容方法
     * 为预约商品列表中的商品查询关联的详情内容
     * 注意：只有当查询结果为单条记录时才会查询详情内容
     *
     * @param reserveGoodsDtos 预约商品数据列表
     */
    private void queryContent(List<ReserveGoodsDto> reserveGoodsDtos) {
        // 只有当查询结果为单条记录时才查询详情内容
        if (reserveGoodsDtos == null || reserveGoodsDtos.size() != 1) {
            return;
        }
        
        // 构建商品详情查询条件
        ReserveGoodsDetailDto reserveGoodsDetailDto = new ReserveGoodsDetailDto();
        reserveGoodsDetailDto.setGoodsId(reserveGoodsDtos.get(0).getGoodsId());
        
        // 查询商品详情列表
        List<ReserveGoodsDetailDto> reserveGoodsDetailDtos = reserveGoodsDetailV1InnerServiceSMOImpl.queryReserveGoodsDetails(reserveGoodsDetailDto);
        
        // 如果存在详情数据，将第一个详情的内容设置到商品对象中
        if (reserveGoodsDetailDtos == null || reserveGoodsDetailDtos.size() < 1) {
            return;
        }
        reserveGoodsDtos.get(0).setContent(reserveGoodsDetailDtos.get(0).getContent());
    }
}