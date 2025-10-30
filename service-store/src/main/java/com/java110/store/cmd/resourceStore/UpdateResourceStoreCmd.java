package com.java110.store.cmd.resourceStore;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.file.FileRelDto;
import com.java110.dto.resource.ResourceStoreDto;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.store.IResourceStoreInnerServiceSMO;
import com.java110.intf.store.IResourceStoreV1InnerServiceSMO;
import com.java110.po.file.FileRelPo;
import com.java110.po.purchase.ResourceStorePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

/**
 * 更新物品库存信息命令类
 * 负责处理物品库存信息的更新操作，包括物品基本信息更新和关联图片文件的管理
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "resourceStore.updateResourceStore")
public class UpdateResourceStoreCmd extends Cmd {

    @Autowired
    private IResourceStoreInnerServiceSMO resourceStoreInnerServiceSMOImpl;

    @Autowired
    private IResourceStoreV1InnerServiceSMO resourceStoreV1InnerServiceSMOImpl;

    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    /**
     * 验证请求参数的有效性
     * 检查必要的参数是否存在，并验证业务规则的合法性
     *
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，用于获取和设置数据流
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必要参数是否存在
        Assert.hasKeyAndValue(reqJson, "resId", "物品ID不能为空");
        Assert.hasKeyAndValue(reqJson, "resName", "必填，请填写物品名称");
        Assert.hasKeyAndValue(reqJson, "price", "必填，请填写物品价格");
        Assert.hasKeyAndValue(reqJson, "storeId", "商户信息不能为空");

        // 获取最低收费标准并验证价格范围
        double outLowPrice = Double.parseDouble(reqJson.getString("outLowPrice"));
        // 获取最高收费标准
        double outHighPrice = Double.parseDouble(reqJson.getString("outHighPrice"));
        // 验证最低收费标准不能大于最高收费标准
        if (outLowPrice > outHighPrice) {
            throw new IllegalArgumentException("最低收费标准不能大于最高收费标准！");
        }

    }

    /**
     * 执行物品库存更新命令
     * 更新物品基本信息，并处理相关的图片文件关联信息
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当数据保存失败时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 根据物品ID和商户ID查询现有的物品信息
        ResourceStoreDto resourceStoreDto = new ResourceStoreDto();
        resourceStoreDto.setResId(reqJson.getString("resId"));
        resourceStoreDto.setStoreId(reqJson.getString("storeId"));
        List<ResourceStoreDto> resourceStoreDtos = resourceStoreInnerServiceSMOImpl.queryResourceStores(resourceStoreDto);
        
        // 验证查询结果，确保只查询到一条记录
        Assert.isOne(resourceStoreDtos, "查询到多条物品 或未查到物品，resId=" + resourceStoreDto.getResId());
        
        // 准备更新数据，保留原有的库存数量
        JSONObject businessResourceStore = new JSONObject();
        businessResourceStore.putAll(reqJson);
        businessResourceStore.put("stock", resourceStoreDtos.get(0).getStock());
        
        // 将JSON对象转换为PO对象
        ResourceStorePo resourceStorePo = BeanConvertUtil.covertBean(businessResourceStore, ResourceStorePo.class);

        // 更新物品基本信息
        int flag = resourceStoreV1InnerServiceSMOImpl.updateResourceStore(resourceStorePo);

        // 检查更新操作是否成功
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }
        
        // 获取请求中的图片信息
        List<String> photos = resourceStorePo.getFileUrls();
        
        // 处理图片文件关联信息
        FileRelDto fileRelDto = new FileRelDto();
        fileRelDto.setObjId(reqJson.getString("resId"));
        
        // 查询该物品现有的文件关联记录
        List<FileRelDto> fileRelDtos = fileRelInnerServiceSMOImpl.queryFileRels(fileRelDto);
        
        // 如果存在现有的文件关联记录，先删除再重新插入
        if (fileRelDtos != null && fileRelDtos.size() > 0) {
            // 删除所有现有的文件关联记录
            for (FileRelDto fileRel : fileRelDtos) {
                FileRelPo fileRelPo = new FileRelPo();
                fileRelPo.setFileRelId(fileRel.getFileRelId());
                flag = fileRelInnerServiceSMOImpl.deleteFileRel(fileRelPo);
                if (flag < 1) {
                    throw new CmdException("保存数据失败");
                }
            }
            
            // 如果有新的图片需要关联，创建新的文件关联记录
            FileRelPo fileRel = new FileRelPo();
            fileRel.setObjId(resourceStorePo.getResId());
            // table表示表存储 ftp表示ftp文件存储
            fileRel.setSaveWay("ftp");
            fileRel.setCreateTime(new Date());
            
            if (photos != null && photos.size() > 0) {
                // 22000表示物品图片类型编码
                fileRel.setRelTypeCd("22000");
                for (String photo : photos) {
                    // 生成唯一的文件关联ID
                    fileRel.setFileRelId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_relId));
                    fileRel.setFileRealName(photo);
                    fileRel.setFileSaveName(photo);
                    flag = fileRelInnerServiceSMOImpl.saveFileRel(fileRel);
                    if (flag < 1) {
                        throw new CmdException("保存数据失败");
                    }
                }
            }
        } else if (photos != null && photos.size() > 0) {  
            // 如果不存在现有的文件关联记录，但有新的图片需要关联，直接创建新的文件关联记录
            FileRelPo fileRel = new FileRelPo();
            fileRel.setObjId(resourceStorePo.getResId());
            // table表示表存储 ftp表示ftp文件存储
            fileRel.setSaveWay("ftp");
            fileRel.setCreateTime(new Date());
            // 22000表示物品图片类型编码
            fileRel.setRelTypeCd("22000");
            for (String photo : photos) {
                // 生成唯一的文件关联ID
                fileRel.setFileRelId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_relId));
                fileRel.setFileRealName(photo);
                fileRel.setFileSaveName(photo);
                flag = fileRelInnerServiceSMOImpl.saveFileRel(fileRel);
                if (flag < 1) {
                    throw new CmdException("保存数据失败");
                }
            }
        }
    }
}