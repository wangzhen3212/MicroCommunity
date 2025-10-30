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
package com.java110.store.cmd.assetInventory;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.purchase.AssetInventoryDto;
import com.java110.intf.store.IAssetInventoryV1InnerServiceSMO;
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
 * 服务编码：assetInventory.listAssetInventory
 * 请求路劲：/app/assetInventory.ListAssetInventory
 * add by 吴学文 at 2022-09-22 14:23:28 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 资产盘点查询命令类
 * 
 * 该类用于处理资产盘点列表查询请求，实现分页查询资产盘点信息的功能
 * 服务编码：assetInventory.listAssetInventory
 * 请求路径：/app/assetInventory.ListAssetInventory
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-09-22
 */
@Java110Cmd(serviceCode = "assetInventory.listAssetInventory")
public class ListAssetInventoryCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(ListAssetInventoryCmd.class);
    
    /**
     * 资产盘点服务接口
     */
    @Autowired
    private IAssetInventoryV1InnerServiceSMO assetInventoryV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，主要验证分页参数
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息验证方法
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 
     * 处理资产盘点列表查询请求，根据条件查询资产盘点信息并返回分页结果
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 从上下文中获取店铺ID
        String storeId = CmdContextUtils.getStoreId(cmdDataFlowContext);

        // 将请求JSON转换为资产盘点DTO对象
        AssetInventoryDto assetInventoryDto = BeanConvertUtil.covertBean(reqJson, AssetInventoryDto.class);
        // 设置店铺ID到DTO对象中
        assetInventoryDto.setStoreId(storeId);
        
        // 查询符合条件的资产盘点记录总数
        int count = assetInventoryV1InnerServiceSMOImpl.queryAssetInventorysCount(assetInventoryDto);

        List<AssetInventoryDto> assetInventoryDtos = null;

        // 根据查询结果数量决定返回的数据
        if (count > 0) {
            // 如果有数据，则查询具体的资产盘点列表
            assetInventoryDtos = assetInventoryV1InnerServiceSMOImpl.queryAssetInventorys(assetInventoryDto);
        } else {
            // 如果没有数据，返回空列表
            assetInventoryDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 创建结果VO对象，包含分页信息和数据
        ResultVo resultVo = new ResultVo(totalPage, count, assetInventoryDtos);

        // 创建HTTP响应实体，设置响应状态为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到数据流上下文中
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}