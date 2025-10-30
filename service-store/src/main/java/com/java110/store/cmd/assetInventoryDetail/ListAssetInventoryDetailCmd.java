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
package com.java110.store.cmd.assetInventoryDetail;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.purchase.AssetInventoryDetailDto;
import com.java110.intf.store.IAssetInventoryDetailV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;


/**
 * 类表述：查询
 * 服务编码：assetInventoryDetail.listAssetInventoryDetail
 * 请求路劲：/app/assetInventoryDetail.ListAssetInventoryDetail
 * add by 吴学文 at 2022-09-23 03:40:30 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 资产盘点明细查询命令类
 * 
 * 该类负责处理资产盘点明细的列表查询请求，包括参数验证、数据查询和结果返回等功能
 * 通过调用资产盘点明细服务接口获取数据，并按照分页要求返回查询结果
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-09-23
 */
@Java110Cmd(serviceCode = "assetInventoryDetail.listAssetInventoryDetail")
public class ListAssetInventoryDetailCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(ListAssetInventoryDetailCmd.class);
    
    /**
     * 资产盘点明细服务接口
     */
    @Autowired
    private IAssetInventoryDetailV1InnerServiceSMO assetInventoryDetailV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，主要验证分页参数
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息验证方法，验证分页参数
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 
     * 处理资产盘点明细的查询请求，包括数据查询、分页处理和结果返回
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为资产盘点明细数据传输对象
        AssetInventoryDetailDto assetInventoryDetailDto = BeanConvertUtil.covertBean(reqJson, AssetInventoryDetailDto.class);

        // 查询符合条件的资产盘点明细总记录数
        int count = assetInventoryDetailV1InnerServiceSMOImpl.queryAssetInventoryDetailsCount(assetInventoryDetailDto);

        // 声明资产盘点明细列表
        List<AssetInventoryDetailDto> assetInventoryDetailDtos = null;

        // 根据查询结果数量决定如何处理数据
        if (count > 0) {
            // 如果存在记录，则查询具体的资产盘点明细列表
            assetInventoryDetailDtos = assetInventoryDetailV1InnerServiceSMOImpl.queryAssetInventoryDetails(assetInventoryDetailDto);
        } else {
            // 如果没有记录，返回空列表
            assetInventoryDetailDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 创建结果对象，包含分页信息和数据
        ResultVo resultVo = new ResultVo(totalPage, count, assetInventoryDetailDtos);

        // 创建HTTP响应实体，状态码为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到数据流上下文中
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}