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
package com.java110.store.cmd.resourceStore;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.doc.annotation.*;
import com.java110.dto.purchase.PurchaseApplyDto;
import com.java110.dto.resource.ResourceStoreDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.store.IPurchaseApplyInnerServiceSMO;
import com.java110.intf.store.IResourceStoreInnerServiceSMO;
import com.java110.intf.store.IResourceStoreTimesV1InnerServiceSMO;
import com.java110.intf.store.IResourceStoreV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.file.FileRelPo;
import com.java110.po.purchase.PurchaseApplyDetailPo;
import com.java110.po.purchase.PurchaseApplyPo;
import com.java110.po.purchase.ResourceStorePo;
import com.java110.po.resource.ResourceStoreTimesPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 物品保存命令类
 * 负责处理物品信息的保存操作，包括物品基本信息、图片关联和入库流程
 * 服务编码：resourceStore.saveResourceStore
 * 请求路径：/app/resourceStore.SaveResourceStore
 * 
 * @author 吴学文 at 2022-06-30 12:03:19 mail: 928255095@qq.com
 * @version 1.0
 * @see Cmd
 */
@Java110CmdDoc(title = "添加物品",
        description = "外部系统通过此接口添加物品",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/resourceStore.saveResourceStore",
        resource = "storeDoc",
        author = "吴学文",
        serviceCode = "resourceStore.saveResourceStore",
        seq = 9
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "resName", length = 64, remark = "名称"),
        @Java110ParamDoc(name = "resCode", length = 64, remark = "编号"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

@Java110ExampleDoc(
        reqBody = "{" +
                "\"resName\":\"电动车\",\"resCode\":\"002\",\"resId\":\"\",\"parentRstId\":\"282023082523150002\"," +
                "\"rstId\":\"282023082516650004\",\"price\":\"15\",\"description\":\"\",\"outLowPrice\":\"1\"," +
                "\"outHighPrice\":\"3\",\"showMobile\":\"N\",\"remark\":\"\",\"unitCode\":\"1001\",\"shId\":\"102023082412640003\"," +
                "\"isFixed\":\"N\",\"rssId\":\"\",\"miniUnitCode\":\"1001\",\"miniUnitStock\":\"1\",\"warningStock\":\"10\"," +
                "\"communityId\":\"2023052267100146\"}",
        resBody = "{\n" +
                "    \"code\": 0,\n" +
                "    \"data\": [\n" +
                "    ],\n" +
                "    \"msg\": \"成功\",\n" +
                "    \"page\": 0,\n" +
                "    \"records\": 1,\n" +
                "    \"rows\": 0,\n" +
                "    \"total\": 1\n" +
                "}"
)
@Java110Cmd(serviceCode = "resourceStore.saveResourceStore")
public class SaveResourceStoreCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveResourceStoreCmd.class);

    /** 编码前缀ID */
    public static final String CODE_PREFIX_ID = "10";

    @Autowired
    private IResourceStoreV1InnerServiceSMO resourceStoreV1InnerServiceSMOImpl;

    @Autowired
    private IResourceStoreInnerServiceSMO resourceStoreInnerServiceSMOImpl;

    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    @Autowired
    private IResourceStoreTimesV1InnerServiceSMO resourceStoreTimesV1InnerServiceSMOImpl;

    @Autowired
    private IPurchaseApplyInnerServiceSMO purchaseApplyInnerServiceSMOImpl;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性和业务规则
     *
     * @param event 命令事件
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws IllegalArgumentException 当参数验证失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填参数
        Assert.hasKeyAndValue(reqJson, "resName", "必填，请填写物品名称");
        Assert.hasKeyAndValue(reqJson, "storeId", "必填，请填写商户信息");
        Assert.hasKeyAndValue(reqJson, "price", "必填，请填写物品价格");
        Assert.hasKeyAndValue(reqJson, "shId", "必填，请填写仓库");
        Assert.hasKeyAndValue(reqJson, "communityId", "必填，请填写小区");
        
        // 验证价格范围逻辑
        double outLowPrice = Double.parseDouble(reqJson.getString("outLowPrice"));
        double outHighPrice = Double.parseDouble(reqJson.getString("outHighPrice"));
        if (outLowPrice > outHighPrice) {
            throw new IllegalArgumentException("最低收费标准不能大于最高收费标准！");
        }
        
        // 验证物品编码唯一性
        String resCode = reqJson.getString("resCode");
        String storeId = cmdDataFlowContext.getReqHeaders().get("store-id");
        ResourceStoreDto resourceStoreDto = new ResourceStoreDto();
        resourceStoreDto.setResCode(resCode);
        resourceStoreDto.setStoreId(storeId);
        List<ResourceStoreDto> resourceStoreDtos = resourceStoreInnerServiceSMOImpl.queryResourceStores(resourceStoreDto);
        // 检查物品编码是否已存在，避免重复
        Assert.listIsNull(resourceStoreDtos, "物品编码重复，请重新添加！");
    }

    /**
     * 执行命令方法
     * 处理物品保存的核心业务逻辑，包括物品信息保存、图片关联和入库流程
     *
     * @param event 命令事件
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当业务处理失败时抛出
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 获取用户和商户信息
        String userId = CmdContextUtils.getUserId(cmdDataFlowContext);
        String storeId = CmdContextUtils.getStoreId(cmdDataFlowContext);
        
        // 构建物品信息对象
        JSONObject businessResourceStore = new JSONObject();
        businessResourceStore.putAll(reqJson);
        businessResourceStore.put("resId", GenerateCodeFactory.getResId(GenerateCodeFactory.CODE_PREFIX_resId));
        
        // 计算最小单位库存
        if (!StringUtil.isEmpty(reqJson.getString("stock")) && !StringUtil.isEmpty(reqJson.getString("miniUnitStock"))) {
            double stock = Double.parseDouble(reqJson.getString("stock"));
            double miniUnitStock = Double.parseDouble(reqJson.getString("miniUnitStock"));
            double miniStock = stock * miniUnitStock;
            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            businessResourceStore.put("miniStock", decimalFormat.format(miniStock));
        } else {
            businessResourceStore.put("miniStock", "0");
        }
        
        businessResourceStore.put("createTime", new Date());
        ResourceStorePo resourceStorePo = BeanConvertUtil.covertBean(businessResourceStore, ResourceStorePo.class);
        
        // 设置默认平均价格
        if (StringUtil.isEmpty(resourceStorePo.getAveragePrice())) {
            resourceStorePo.setAveragePrice("0.00");
        }
        
        // 保存物品基本信息
        int flag = resourceStoreV1InnerServiceSMOImpl.saveResourceStore(resourceStorePo);
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }
        
        // 执行入库操作
        inStore(reqJson, userId, storeId, resourceStorePo);
        
        // 处理物品图片关联
        FileRelPo fileRelPo = new FileRelPo();
        fileRelPo.setObjId(resourceStorePo.getResId());
        fileRelPo.setSaveWay("ftp"); // 设置存储方式为FTP
        fileRelPo.setCreateTime(new Date());
        
        // 保存物品图片信息
        List<String> photos = resourceStorePo.getPhotos();
        if (photos != null && photos.size() > 0) {
            fileRelPo.setRelTypeCd("22000"); // 22000表示物品图片类型
            for (String photo : photos) {
                fileRelPo.setFileRelId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_relId));
                fileRelPo.setFileRealName(photo);
                fileRelPo.setFileSaveName(photo);
                flag = fileRelInnerServiceSMOImpl.saveFileRel(fileRelPo);
                if (flag < 1) {
                    throw new CmdException("保存数据失败");
                }
            }
        }
        
        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }

    /**
     * 入库处理方法
     * 处理物品的入库流程，包括生成采购申请和更新库存记录
     *
     * @param reqJson 请求JSON对象
     * @param userId 用户ID
     * @param storeId 商户ID
     * @param resourceStorePo 物品持久化对象
     */
    private void inStore(JSONObject reqJson, String userId, String storeId, ResourceStorePo resourceStorePo) {
        // 查询用户信息
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        userDto.setPage(1);
        userDto.setRow(1);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);
        Assert.listOnlyOne(userDtos, "用户不存在");
        
        String applyOrderId = "-1";
        double stock = Double.parseDouble(resourceStorePo.getStock());
        
        // 如果有库存，生成采购申请流程
        if (stock > 0) {
            applyOrderId = GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_applyOrderId);
            
            // 构建采购申请信息
            PurchaseApplyPo purchaseApplyPo = new PurchaseApplyPo();
            purchaseApplyPo.setApplyOrderId(applyOrderId);
            purchaseApplyPo.setDescription("入库单（物品导入）");
            purchaseApplyPo.setUserId(userId);
            purchaseApplyPo.setUserName(userDtos.get(0).getName());
            purchaseApplyPo.setEndUserName(userDtos.get(0).getName());
            purchaseApplyPo.setEndUserTel(userDtos.get(0).getTel());
            purchaseApplyPo.setStoreId(storeId);
            purchaseApplyPo.setResOrderType(PurchaseApplyDto.RES_ORDER_TYPE_ENTER); // 入库类型
            purchaseApplyPo.setState(PurchaseApplyDto.STATE_END); // 已完成状态
            purchaseApplyPo.setCreateTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
            purchaseApplyPo.setCreateUserId(userId);
            purchaseApplyPo.setCreateUserName(userDtos.get(0).getName());
            purchaseApplyPo.setWarehousingWay(PurchaseApplyDto.WAREHOUSING_TYPE_URGENT); // 紧急入库
            purchaseApplyPo.setCommunityId(reqJson.getString("communityId"));
            
            // 构建采购申请明细
            List<PurchaseApplyDetailPo> purchaseApplyDetailPos = new ArrayList<>();
            PurchaseApplyDetailPo purchaseApplyDetailPo = new PurchaseApplyDetailPo();
            purchaseApplyDetailPo.setApplyOrderId(applyOrderId);
            purchaseApplyDetailPo.setId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_applyOrderId));
            purchaseApplyDetailPo.setPurchaseQuantity(resourceStorePo.getStock());
            purchaseApplyDetailPo.setPurchaseRemark(resourceStorePo.getRemark());
            purchaseApplyDetailPo.setOriginalStock(resourceStorePo.getStock());
            purchaseApplyDetailPo.setQuantity(resourceStorePo.getStock());
            purchaseApplyDetailPo.setPrice(resourceStorePo.getPrice());
            purchaseApplyDetailPo.setResId(resourceStorePo.getResId());
            purchaseApplyDetailPo.setRsId(resourceStorePo.getRssId());
            purchaseApplyDetailPos.add(purchaseApplyDetailPo);
            purchaseApplyPo.setPurchaseApplyDetailPos(purchaseApplyDetailPos);
            
            // 保存采购申请
            int saveFlag = purchaseApplyInnerServiceSMOImpl.savePurchaseApply(purchaseApplyPo);
            if (saveFlag < 1) {
                throw new CmdException("采购申请失败");
            }
        }
        
        // 保存物品库存时间记录
        ResourceStoreTimesPo resourceStoreTimesPo = new ResourceStoreTimesPo();
        resourceStoreTimesPo.setApplyOrderId(applyOrderId);
        resourceStoreTimesPo.setPrice(resourceStorePo.getPrice());
        resourceStoreTimesPo.setStock(resourceStorePo.getStock());
        resourceStoreTimesPo.setResCode(resourceStorePo.getResCode());
        resourceStoreTimesPo.setStoreId(resourceStorePo.getStoreId());
        resourceStoreTimesPo.setTimesId(GenerateCodeFactory.getGeneratorId("10"));
        resourceStoreTimesPo.setShId(resourceStorePo.getShId());
        resourceStoreTimesPo.setCommunityId(reqJson.getString("communityId"));
        resourceStoreTimesV1InnerServiceSMOImpl.saveOrUpdateResourceStoreTimes(resourceStoreTimesPo);
    }
}