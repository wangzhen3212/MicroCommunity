package com.java110.store.cmd.store;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.store.StoreAttrDto;
import com.java110.dto.store.StoreDto;
import com.java110.intf.store.IStoreAttrV1InnerServiceSMO;
import com.java110.intf.store.IStoreV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import com.java110.vo.api.store.ApiStoreDataVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 店铺列表查询命令类
 * 处理店铺列表查询请求，支持分页查询和店铺属性信息获取
 * 服务编码：store.listStores
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "store.listStores")
public class ListStoresCmd extends Cmd {

    @Autowired
    private IStoreV1InnerServiceSMO storeV1InnerServiceSMOImpl;

    @Autowired
    private IStoreAttrV1InnerServiceSMO storeAttrV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 验证分页参数，并根据用户权限过滤查询条件
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        
        // 根据storeId查询店铺信息
        StoreDto storeDto = new StoreDto();
        storeDto.setStoreId(reqJson.getString("storeId"));
        List<StoreDto> storeDtos = storeV1InnerServiceSMOImpl.queryStores(storeDto);

        // 只有运营管理员可以查看所有商户信息
        // 如果是运营管理员，则移除storeId参数，查询所有店铺
        if (StoreDto.STORE_TYPE_ADMIN.equals(storeDtos.get(0).getStoreTypeCd())) {
            reqJson.remove("storeId");
        }
    }

    /**
     * 执行命令方法
     * 查询店铺列表数据，包含分页信息和店铺属性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为StoreDto对象
        StoreDto storeDto = BeanConvertUtil.covertBean(reqJson, StoreDto.class);
        storeDto.setUserId("");
        
        // 查询店铺总数
        int count = storeV1InnerServiceSMOImpl.queryStoresCount(storeDto);
        List<StoreDto> storeDtos = null;
        List<ApiStoreDataVo> stores = null;
        
        if (count > 0) {
            // 查询店铺列表数据
            storeDtos = storeV1InnerServiceSMOImpl.queryStores(storeDto);
            // 转换为API响应对象
            stores = BeanConvertUtil.covertBeanList(storeDtos, ApiStoreDataVo.class);
            // 刷新店铺属性信息
            refreshStoreAttr(stores);
        } else {
            stores = new ArrayList<>();
        }

        // 构建分页响应结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, stores);

        // 设置HTTP响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 刷新店铺属性信息
     * 根据店铺ID批量查询店铺属性，并设置到对应的店铺对象中
     * 
     * @param stores 店铺数据列表
     */
    private void refreshStoreAttr(List<ApiStoreDataVo> stores) {
        // 构建店铺属性查询条件
        StoreAttrDto storeAttrDto = new StoreAttrDto();
        storeAttrDto.setStoreIds(getStoreIds(stores));
        
        // 批量查询店铺属性
        List<StoreAttrDto> storeAttrDtos = storeAttrV1InnerServiceSMOImpl.queryStoreAttrs(storeAttrDto);
        
        // 遍历店铺列表，设置属性信息
        for (ApiStoreDataVo storeDataVo : stores) {
            List<StoreAttrDto> storeAttrs = new ArrayList<StoreAttrDto>();
            
            // 遍历属性列表，匹配当前店铺的属性
            for (StoreAttrDto tmpStoreAttrDto : storeAttrDtos) {
                // 如果属性不属于当前店铺，跳过
                if (!storeDataVo.getStoreId().equals(tmpStoreAttrDto.getStoreId())) {
                    continue;
                }
                
                // 将匹配的属性添加到店铺属性列表中
                storeAttrs.add(tmpStoreAttrDto);
                
                // 根据规格代码设置特定的属性值
                if ("100201903001".equals(tmpStoreAttrDto.getSpecCd())) {
                    // 设置法人信息
                    storeDataVo.setArtificialPerson(tmpStoreAttrDto.getValue());
                } else if ("100201903003".equals(tmpStoreAttrDto.getSpecCd())) {
                    // 设置成立日期
                    storeDataVo.setEstablishment(tmpStoreAttrDto.getValue());
                } else if ("100201903005".equals(tmpStoreAttrDto.getSpecCd())) {
                    // 设置经营范围
                    storeDataVo.setBusinessScope(tmpStoreAttrDto.getValue());
                }
            }
            
            // 设置店铺属性列表
            storeDataVo.setStoreAttrDtoList(storeAttrs);
        }
    }

    /**
     * 从店铺数据列表中提取店铺ID数组
     * 
     * @param apiStoreDataVos 店铺数据列表
     * @return 店铺ID数组
     */
    private String[] getStoreIds(List<ApiStoreDataVo> apiStoreDataVos) {
        List<String> storeIds = new ArrayList<>();
        // 遍历店铺列表，收集所有店铺ID
        for (ApiStoreDataVo storeDataVo : apiStoreDataVos) {
            storeIds.add(storeDataVo.getStoreId());
        }

        // 转换为数组返回
        return storeIds.toArray(new String[storeIds.size()]);
    }
}