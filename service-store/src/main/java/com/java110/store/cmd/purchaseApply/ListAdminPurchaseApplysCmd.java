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
package com.java110.store.cmd.purchaseApply;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.privilege.BasePrivilegeDto;
import com.java110.dto.purchase.PurchaseApplyDto;
import com.java110.intf.common.IPurchaseApplyUserInnerServiceSMO;
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.intf.store.IPurchaseApplyInnerServiceSMO;
import com.java110.intf.store.IResourceStoreInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.api.purchaseApply.ApiPurchaseApplyDataVo;
import com.java110.vo.api.purchaseApply.ApiPurchaseApplyVo;
import com.java110.vo.api.purchaseApply.PurchaseApplyDetailVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 采购申请管理命令类
 * 负责处理管理员查询采购申请列表的请求
 * 服务编码：purchaseApply.listAdminPurchaseApplys
 * 请求路径：/app/purchaseApply.ListPurchaseApply
 * 
 * @author 吴学文 at 2022-08-08 13:21:13 mail: 928255095@qq.com
 * @open source address: https://gitee.com/wuxw7/MicroCommunity
 * @官网：http://www.homecommunity.cn
 * @温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "purchaseApply.listAdminPurchaseApplys")
public class ListAdminPurchaseApplysCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListAdminPurchaseApplysCmd.class);

    @Autowired
    private IPurchaseApplyInnerServiceSMO purchaseApplyInnerServiceSMOImpl;

    @Autowired
    private IPurchaseApplyUserInnerServiceSMO purchaseApplyUserInnerServiceSMOImpl;

    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    @Autowired
    private IResourceStoreInnerServiceSMO resourceStoreInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性和完整性
     *
     * @param event CMD事件对象
     * @param cmdDataFlowContext 数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填参数：订单类型
        Assert.hasKeyAndValue(reqJson, "resOrderType", "必填，请填写订单类型");
        // 验证分页信息
        super.validatePageInfo(reqJson);
        // 验证管理员权限
        super.validateAdmin(cmdDataFlowContext);
    }

    /**
     * 命令执行方法
     * 处理采购申请列表查询业务逻辑
     *
     * @param event CMD事件对象
     * @param context 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为采购申请DTO对象
        PurchaseApplyDto purchaseApplyDto = BeanConvertUtil.covertBean(reqJson, PurchaseApplyDto.class);
        // 设置查询条件：清空店铺ID和用户名相关条件
        purchaseApplyDto.setStoreId("");
        purchaseApplyDto.setUserName("");//解除与用户名相关问题
        // 设置查询条件：清空用户ID
        purchaseApplyDto.setUserId("");
        // 如果请求中包含申请人姓名，则设置查询条件
        if (!StringUtil.isEmpty(reqJson.getString("applyUserName"))) {
            purchaseApplyDto.setUserName(reqJson.getString("applyUserName"));
        }
        
        // 查询采购申请总数
        int count = purchaseApplyInnerServiceSMOImpl.queryPurchaseApplysCount(purchaseApplyDto);
        List<ApiPurchaseApplyDataVo> purchaseApplys = new ArrayList<>();
        
        if (count > 0) {
            // 查询采购申请详情列表
            List<PurchaseApplyDto> purchaseApplyDtos = purchaseApplyInnerServiceSMOImpl.queryPurchaseApplyAndDetails(purchaseApplyDto);
            // 刷新当前用户信息
            purchaseApplyDtos = freshCurrentUser(purchaseApplyDtos);
            // 转换为API响应对象
            purchaseApplys = BeanConvertUtil.covertBeanList(purchaseApplyDtos, ApiPurchaseApplyDataVo.class);
            // 刷新申请数据
            purchaseApplys = refreshApplys(purchaseApplys, reqJson);
        } else {
            purchaseApplys = new ArrayList<>();
        }
        
        // 构建API响应对象
        ApiPurchaseApplyVo apiPurchaseApplyVo = new ApiPurchaseApplyVo();
        apiPurchaseApplyVo.setTotal(count);
        // 计算总页数
        apiPurchaseApplyVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        apiPurchaseApplyVo.setPurchaseApplys(purchaseApplys);
        
        // 设置响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiPurchaseApplyVo), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 刷新申请数据方法
     * 处理采购申请数据的格式化和过滤
     *
     * @param purchaseApplys 原始采购申请数据列表
     * @param reqJson 请求参数JSON对象
     * @return 处理后的采购申请数据列表
     */
    private List<ApiPurchaseApplyDataVo> refreshApplys(List<ApiPurchaseApplyDataVo> purchaseApplys, JSONObject reqJson) {
        List<ApiPurchaseApplyDataVo> purchaseApplyDatas = new ArrayList<>();
        
        for (ApiPurchaseApplyDataVo apiPurchaseApplyDataVo : purchaseApplys) {
            // 获取采购申请详情列表
            List<PurchaseApplyDetailVo> applyDetailList = apiPurchaseApplyDataVo.getPurchaseApplyDetailVo();
            // 如果没有物品详情，跳过当前申请
            if (ListUtil.isNull(applyDetailList)) {
                continue;
            }
            
            StringBuffer resNames = new StringBuffer();
            BigDecimal totalPrice = new BigDecimal(0);
            BigDecimal purchaseTotalPrice = new BigDecimal(0);
            Integer cursor = 0;
            
            // 遍历采购申请详情，计算总价和构建资源名称字符串
            for (PurchaseApplyDetailVo purchaseApplyDetailVo : applyDetailList) {
                // 设置是否为固定物品标识
                apiPurchaseApplyDataVo.setIsFixed(purchaseApplyDetailVo.getIsFixed());

                cursor++;
                // 构建资源名称显示字符串
                if (!StringUtil.isEmpty(purchaseApplyDetailVo.getSpecName())) {
                    resNames.append(cursor + "：" + purchaseApplyDetailVo.getResName() + "(" + purchaseApplyDetailVo.getSpecName() + ")      ");
                } else {
                    resNames.append(cursor + ":" + purchaseApplyDetailVo.getResName());
                }
                
                // 计算申请总价
                BigDecimal price = new BigDecimal(purchaseApplyDetailVo.getPrice());
                BigDecimal quantity = new BigDecimal(purchaseApplyDetailVo.getQuantity());
                totalPrice = totalPrice.add(price.multiply(quantity));
                
                // 计算采购总价（如果存在采购价格和数量）
                if (!StringUtil.isEmpty(purchaseApplyDetailVo.getPurchasePrice()) && !StringUtil.isEmpty(purchaseApplyDetailVo.getPurchaseQuantity())) {
                    BigDecimal purchasePrice = new BigDecimal(purchaseApplyDetailVo.getPurchasePrice());
                    BigDecimal purchaseQuantity = new BigDecimal(purchaseApplyDetailVo.getPurchaseQuantity());
                    purchaseTotalPrice = purchaseTotalPrice.add(purchasePrice.multiply(purchaseQuantity));
                }
            }
            
            // 设置处理后的数据
            apiPurchaseApplyDataVo.setResourceNames(resNames.toString());
            apiPurchaseApplyDataVo.setTotalPrice(totalPrice.toString());
            apiPurchaseApplyDataVo.setPurchaseTotalPrice(purchaseTotalPrice.toString());
            
            // 根据资源名称进行过滤
            if (reqJson.containsKey("resName") && !StringUtil.isEmpty(reqJson.getString("resName"))) {
                if (resNames.toString().contains(reqJson.getString("resName"))) {
                    purchaseApplyDatas.add(apiPurchaseApplyDataVo);
                } else {
                    continue;
                }
            } else {
                purchaseApplyDatas.add(apiPurchaseApplyDataVo);
            }
        }
        return purchaseApplyDatas;
    }

    /**
     * 刷新当前用户信息方法
     * 为采购申请数据添加当前处理用户信息
     *
     * @param purchaseApplyDtos 原始采购申请DTO列表
     * @return 包含当前用户信息的采购申请DTO列表
     */
    private List<PurchaseApplyDto> freshCurrentUser(List<PurchaseApplyDto> purchaseApplyDtos) {
        List<PurchaseApplyDto> tmpPurchaseApplyDtos = new ArrayList<>();
        
        for (PurchaseApplyDto purchaseApplyDto : purchaseApplyDtos) {
            // 获取当前处理用户信息
            purchaseApplyDto = purchaseApplyUserInnerServiceSMOImpl.getTaskCurrentUser(purchaseApplyDto);
            tmpPurchaseApplyDtos.add(purchaseApplyDto);
        }
        
        return tmpPurchaseApplyDtos;
    }
}