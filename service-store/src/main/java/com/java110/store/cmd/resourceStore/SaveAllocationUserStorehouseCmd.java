package com.java110.store.cmd.resourceStore;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.resource.ResourceStoreDto;
import com.java110.dto.user.UserDto;
import com.java110.dto.user.UserStorehouseDto;
import com.java110.intf.store.*;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.po.purchase.AllocationUserStorehousePo;
import com.java110.po.resource.ResourceStoreUseRecordPo;
import com.java110.po.user.UserStorehousePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

/**
 * 保存用户仓库分配命令类
 * 处理用户物品的转赠和损耗操作，包括：
 * 1. 物品转赠：将用户的物品转赠给其他用户
 * 2. 物品损耗：记录物品的损耗情况并更新库存
 * 支持物品单位和最小计量单位的转换计算
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "resourceStore.saveAllocationUserStorehouse")
public class SaveAllocationUserStorehouseCmd extends Cmd {

    @Autowired
    private IUserStorehouseInnerServiceSMO userStorehouseInnerServiceSMOImpl;

    @Autowired
    private IUserStorehouseV1InnerServiceSMO userStorehouseV1InnerServiceSMOImpl;

    @Autowired
    private IResourceStoreInnerServiceSMO resourceStoreInnerServiceSMOImpl;

    @Autowired
    private IResourceStoreUseRecordV1InnerServiceSMO resourceStoreUseRecordV1InnerServiceSMOImpl;

    @Autowired
    private IAllocationUserStorehouseV1InnerServiceSMO allocationUserStorehouseV1InnerServiceSMOImpl;

    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 检查必要的参数是否存在
     *
     * @param event 命令事件
     * @param context 数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当参数验证失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证storeId参数是否存在
        Assert.hasKeyAndValue(reqJson, "storeId", "请求报文中未包含storeId");
    }

    /**
     * 执行主要命令逻辑
     * 处理用户物品的转赠或损耗操作
     *
     * @param event 命令事件
     * @param context 数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当命令执行失败时抛出
     * @throws ParseException 当数据解析失败时抛出
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 获取用户ID，优先从请求参数中获取，如果没有则从请求头中获取
        String userId = reqJson.getString("userId");
        if (StringUtil.isEmpty(userId)) {
            userId = context.getReqHeaders().get("user-id");
        }
        
        // 查询用户信息
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        userDto.setPage(1);
        userDto.setRow(1);
        List<UserDto> userDtos = userInnerServiceSMOImpl.getUsers(userDto);

        // 验证用户是否存在且唯一
        Assert.listOnlyOne(userDtos, "用户不存在");

        // 将用户名添加到请求参数中
        reqJson.put("userName", userDtos.get(0).getName());

        // 检查接受用户ID是否与当前用户ID相同（不能转赠给自己）
        String acceptUserId = reqJson.getString("acceptUserId");
        if (!StringUtil.isEmpty(userId) && !StringUtil.isEmpty(acceptUserId) && acceptUserId.equals(userId)) {
            ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_ERROR, "物品接受人不能是本人，所以无法进行转赠操作！");
            context.setResponseEntity(responseEntity);
            return;
        }

        // 执行分配用户仓库操作
        addAllocationUserStorehouse(reqJson, context);
    }

    /**
     * 添加用户仓库分配记录
     * 根据操作类型（损耗或转赠）执行不同的处理逻辑
     *
     * @param paramInJson 输入参数JSON对象
     * @param dataFlowContext 数据流上下文
     */
    public void addAllocationUserStorehouse(JSONObject paramInJson, ICmdDataFlowContext dataFlowContext) {
        // 获取资源物品列表
        String resourceStores = paramInJson.getString("resourceStores");
        JSONArray json = JSONArray.parseArray(resourceStores);
        int flag1 = 0; // 操作结果标志
        
        if (json.size() > 0) {
            Object[] objects = json.toArray();
            String flag = paramInJson.getString("flag");
            
            // 损耗处理逻辑
            if (!StringUtil.isEmpty(flag) && flag.equals("1")) {
                for (int i = 0; i < objects.length; i++) {
                    Object object = objects[i];
                    JSONObject paramIn = JSONObject.parseObject(String.valueOf(object));
                    
                    // 创建物品使用记录
                    ResourceStoreUseRecordPo resourceStoreUseRecordPo = new ResourceStoreUseRecordPo();
                    resourceStoreUseRecordPo.setRsurId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_rsurId));
                    resourceStoreUseRecordPo.setRepairId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_repairId)); // 报修记录ID
                    resourceStoreUseRecordPo.setResId(paramIn.getString("resId")); // 物品资源ID
                    resourceStoreUseRecordPo.setCommunityId(paramInJson.getString("communityId")); // 小区ID
                    resourceStoreUseRecordPo.setStoreId(paramInJson.getString("storeId")); // 商户ID
                    resourceStoreUseRecordPo.setQuantity(paramIn.getString("giveQuantity")); // 损耗数量
                    
                    // 根据物品资源ID查询物品资源信息
                    ResourceStoreDto resourceStoreDto = new ResourceStoreDto();
                    resourceStoreDto.setResId(paramIn.getString("resId"));
                    List<ResourceStoreDto> resourceStoreDtos = resourceStoreInnerServiceSMOImpl.queryResourceStores(resourceStoreDto);
                    Assert.listOnlyOne(resourceStoreDtos, "查询房屋信息错误！");
                    
                    resourceStoreUseRecordPo.setUnitPrice(resourceStoreDtos.get(0).getPrice()); // 物品资源单价
                    resourceStoreUseRecordPo.setCreateUserId(paramInJson.getString("userId")); // 创建人ID
                    resourceStoreUseRecordPo.setCreateUserName(paramInJson.getString("userName")); // 创建人名称
                    resourceStoreUseRecordPo.setRemark(paramIn.getString("purchaseRemark")); // 备注
                    resourceStoreUseRecordPo.setResourceStoreName(paramIn.getString("resName")); // 物品名称
                    resourceStoreUseRecordPo.setState(paramIn.getString("state")); // 状态：1001 报废回收 2002 工单损耗 3003 公用损耗
                    
                    // 保存物品使用记录
                    flag1 = resourceStoreUseRecordV1InnerServiceSMOImpl.saveResourceStoreUseRecord(resourceStoreUseRecordPo);
                    if (flag1 < 1) {
                        throw new CmdException("保存失败");
                    }
                    
                    // 个人物品处理
                    UserStorehouseDto userStorehouseDto = new UserStorehouseDto();
                    userStorehouseDto.setUserId(paramInJson.getString("userId"));
                    userStorehouseDto.setResId(paramIn.getString("resId"));
                    // 查询个人物品信息
                    List<UserStorehouseDto> userStorehouseDtos = userStorehouseInnerServiceSMOImpl.queryUserStorehouses(userStorehouseDto);
                    Assert.listOnlyOne(userStorehouseDtos, "查询个人物品信息错误！");
                    
                    // 获取个人物品信息ID
                    String usId = userStorehouseDtos.get(0).getUsId();
                    // 验证物品单位不能为空
                    if (StringUtil.isEmpty(userStorehouseDtos.get(0).getUnitCode())) {
                        throw new IllegalArgumentException("物品单位不能为空");
                    }
                    String unitCode = userStorehouseDtos.get(0).getUnitCode(); // 物品单位
                    
                    // 验证物品最小计量单位不能为空
                    if (StringUtil.isEmpty(userStorehouseDtos.get(0).getMiniUnitCode())) {
                        throw new IllegalArgumentException("物品最小计量单位不能为空");
                    }
                    String miniUnitCode = userStorehouseDtos.get(0).getMiniUnitCode(); // 物品最小计量单位
                    
                    UserStorehousePo userStorehousePo = new UserStorehousePo();
                    userStorehousePo.setUsId(usId); // 个人物品ID
                    
                    // 获取最小单位计量总数
                    String miniStock = userStorehouseDtos.get(0).getMiniStock();
                    // 获取最小计量单位数量
                    String miniUnitStock = paramIn.getString("miniUnitStock");
                    // 获取报废数量
                    String giveQuantity = paramIn.getString("giveQuantity");
                    
                    // 计算剩余最小计量单位总数
                    BigDecimal num1 = new BigDecimal(miniStock);
                    BigDecimal num2 = new BigDecimal(giveQuantity);
                    BigDecimal quantity = num1.subtract(num2);
                    
                    if (quantity.doubleValue() == 0.0) {
                        // 如果减去报废后剩余0个，那么最小计量单位总数和物品数量都变为0
                        userStorehousePo.setMiniStock("0");
                        userStorehousePo.setStock("0");
                    } else {
                        userStorehousePo.setMiniStock(String.valueOf(quantity)); // 减去报废后剩余的最小计量单位总数
                        BigDecimal reduceNum = num1.subtract(num2);
                        
                        if (unitCode.equals(miniUnitCode)) {
                            // 如果物品单位与最小计量单位相同
                            userStorehousePo.setStock(String.valueOf(reduceNum));
                        } else {
                            // 如果物品最小计量单位与物品单位不同
                            // 用转赠后最小计量总数除以最小计量单位数量，四舍五入保留两位小数，获取转赠后的库存数
                            BigDecimal num3 = new BigDecimal(miniUnitStock);
                            BigDecimal unitStock = reduceNum.divide(num3, 2, BigDecimal.ROUND_HALF_UP);
                            userStorehousePo.setStock(String.valueOf(unitStock)); // 减去报废后剩余的个人物品数量
                        }
                    }
                    
                    // 更新个人物品信息
                    flag1 = userStorehouseV1InnerServiceSMOImpl.updateUserStorehouse(userStorehousePo);
                    if (flag1 < 1) {
                        throw new CmdException("保存失败");
                    }
                }
            } else {
                // 转赠处理逻辑
                for (int i = 0; i < objects.length; i++) {
                    Object object = objects[i];
                    JSONObject paramIn = JSONObject.parseObject(String.valueOf(object));
                    
                    // 获取物品相关信息
                    String stock = paramIn.getString("stock");
                    String miniStock = paramIn.getString("miniStock"); // 最小计量总数
                    String miniUnitStock = paramIn.getString("miniUnitStock"); // 最小计量单位数量
                    String giveQuantity = paramIn.getString("giveQuantity"); // 转赠数量
                    String resId = paramIn.getString("resId"); // 物品ID
                    String resCode = paramIn.getString("resCode"); // 物品编码
                    String resName = paramIn.getString("resName"); // 物品名称
                    String userId = paramInJson.getString("userId"); // 当前用户ID
                    String acceptUserId = paramInJson.getString("acceptUserId"); // 接受转赠用户ID
                    String acceptUserName = paramInJson.getString("acceptUserName"); // 接受转赠用户名称
                    String timesId = paramIn.getString("timesId"); // 批次ID
                    String storeId = paramInJson.getString("storeId"); // 商户ID
                    
                    // 创建分配记录
                    JSONObject allocationUserStorehouseJson = new JSONObject();
                    allocationUserStorehouseJson.put("ausId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_ausId));
                    allocationUserStorehouseJson.put("resId", resId);
                    allocationUserStorehouseJson.put("resCode", resCode);
                    allocationUserStorehouseJson.put("resName", resName);
                    allocationUserStorehouseJson.put("storeId", storeId);
                    allocationUserStorehouseJson.put("stock", stock);
                    allocationUserStorehouseJson.put("giveQuantity", giveQuantity);
                    allocationUserStorehouseJson.put("startUserId", userId);
                    allocationUserStorehouseJson.put("startUserName", paramInJson.getString("userName"));
                    allocationUserStorehouseJson.put("acceptUserId", acceptUserId);
                    allocationUserStorehouseJson.put("acceptUserName", acceptUserName);
                    allocationUserStorehouseJson.put("createTime", new Date());
                    allocationUserStorehouseJson.put("remark", paramInJson.getString("description"));
                    
                    AllocationUserStorehousePo allocationUserStorehousePo = BeanConvertUtil.covertBean(allocationUserStorehouseJson, AllocationUserStorehousePo.class);
                    flag1 = allocationUserStorehouseV1InnerServiceSMOImpl.saveAllocationUserStorehouse(allocationUserStorehousePo);
                    if (flag1 < 1) {
                        throw new CmdException("保存失败");
                    }
                    
                    // 处理转赠用户的库存
                    UserStorehouseDto userStorehouseDto = new UserStorehouseDto();
                    userStorehouseDto.setUserId(userId);
                    userStorehouseDto.setResId(resId);
                    List<UserStorehouseDto> userStorehouseDtos = userStorehouseInnerServiceSMOImpl.queryUserStorehouses(userStorehouseDto);
                    Assert.listOnlyOne(userStorehouseDtos, "查询个人物品信息错误！");
                    
                    // 获取个人物品信息ID
                    String usId = userStorehouseDtos.get(0).getUsId();
                    // 验证物品单位不能为空
                    if (StringUtil.isEmpty(userStorehouseDtos.get(0).getUnitCode())) {
                        throw new IllegalArgumentException("物品单位不能为空");
                    }
                    String unitCode = userStorehouseDtos.get(0).getUnitCode();
                    // 验证物品最小计量单位不能为空
                    if (StringUtil.isEmpty(userStorehouseDtos.get(0).getMiniUnitCode())) {
                        throw new IllegalArgumentException("物品最小计量单位不能为空");
                    }
                    String miniUnitCode = userStorehouseDtos.get(0).getMiniUnitCode();
                    
                    UserStorehousePo userStorehousePo = new UserStorehousePo();
                    userStorehousePo.setUsId(usId);
                    
                    // 计算转赠后个人物品最小计量总数
                    BigDecimal num1 = new BigDecimal(miniStock);
                    BigDecimal num2 = new BigDecimal(giveQuantity);
                    BigDecimal quantity = num1.subtract(num2);
                    
                    if (quantity.doubleValue() == 0.0) {
                        // 如果转赠后剩余0个，清空库存
                        userStorehousePo.setMiniStock("0");
                        userStorehousePo.setStock("0");
                    } else {
                        userStorehousePo.setMiniStock(String.valueOf(quantity));
                        BigDecimal reduceNum = num1.subtract(num2);
                        
                        if (unitCode.equals(miniUnitCode)) {
                            // 如果物品单位与最小计量单位相同，直接使用减少后的数量
                            userStorehousePo.setStock(String.valueOf(reduceNum));
                        } else {
                            // 如果物品最小计量单位与物品单位不同，进行单位转换计算
                            BigDecimal num3 = new BigDecimal(miniUnitStock);
                            BigDecimal unitStock = reduceNum.divide(num3, 2, BigDecimal.ROUND_HALF_UP);
                            userStorehousePo.setStock(String.valueOf(unitStock));
                        }
                    }
                    
                    // 更新当前用户库存
                    flag1 = userStorehouseV1InnerServiceSMOImpl.updateUserStorehouse(userStorehousePo);
                    if (flag1 < 1) {
                        throw new CmdException("保存失败");
                    }
                    
                    // 处理接受转赠用户的库存
                    UserStorehouseDto userStorehouse = new UserStorehouseDto();
                    userStorehouse.setUserId(acceptUserId);
                    userStorehouse.setResCode(resCode);
                    // 查询接受转赠人的个人物品信息
                    List<UserStorehouseDto> userStorehouses = userStorehouseInnerServiceSMOImpl.queryUserStorehouses(userStorehouse);
                    
                    if (userStorehouses != null && userStorehouses.size() == 1) {
                        // 如果接受用户已有该物品，更新库存
                        UserStorehousePo userStorePo = new UserStorehousePo();
                        // 计算接受用户的最小计量总数
                        BigDecimal num4 = new BigDecimal(userStorehouses.get(0).getMiniStock());
                        BigDecimal num5 = new BigDecimal(giveQuantity);
                        BigDecimal addNum = num4.add(num5);
                        BigDecimal acceptMiniStock = num4.add(num5);
                        userStorePo.setMiniStock(String.valueOf(acceptMiniStock));
                        
                        // 验证物品单位不能为空
                        if (StringUtil.isEmpty(userStorehouses.get(0).getUnitCode())) {
                            throw new IllegalArgumentException("物品单位不能为空");
                        }
                        String unitCode1 = userStorehouses.get(0).getUnitCode();
                        // 验证物品最小计量单位不能为空
                        if (StringUtil.isEmpty(userStorehouses.get(0).getMiniUnitCode())) {
                            throw new IllegalArgumentException("物品最小计量单位不能为空");
                        }
                        String miniUnitCode1 = userStorehouses.get(0).getMiniUnitCode();
                        
                        // 计算接受用户的库存数量
                        BigDecimal num6 = new BigDecimal(miniUnitStock);
                        BigDecimal unitStock = addNum.divide(num6, 2, BigDecimal.ROUND_HALF_UP);
                        
                        if (unitCode1.equals(miniUnitCode1)) {
                            // 如果物品单位与物品最小计量单位相同，库存等于最小计量总数
                            userStorePo.setStock(String.valueOf(acceptMiniStock));
                        } else {
                            // 如果物品单位与物品最小计量单位不同，进行单位转换
                            userStorePo.setStock(String.valueOf(unitStock));
                        }
                        
                        userStorePo.setUsId(userStorehouses.get(0).getUsId());
                        userStorePo.setTimesId(timesId);
                        // 更新接受用户的库存
                        flag1 = userStorehouseV1InnerServiceSMOImpl.updateUserStorehouse(userStorePo);
                        if (flag1 < 1) {
                            throw new CmdException("保存失败");
                        }
                    } else if (userStorehouses != null && userStorehouses.size() > 1) {
                        throw new IllegalArgumentException("查询个人物品信息错误！");
                    } else {
                        // 如果接受用户没有该物品，创建新的物品记录
                        // 计算转赠后库存数量
                        BigDecimal num7 = new BigDecimal(giveQuantity); // 转增物品最小计量单位数量
                        BigDecimal num8 = new BigDecimal(miniUnitStock); // 最小计量单位数量
                        BigDecimal unitStock = num7.divide(num8, 2, BigDecimal.ROUND_HALF_UP); // 转增物品数量
                        
                        UserStorehousePo userStorePo = new UserStorehousePo();
                        userStorePo.setUsId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_usId));
                        userStorePo.setResId(resId);
                        userStorePo.setResName(resName);
                        userStorePo.setStoreId(storeId);
                        userStorePo.setResCode(resCode);
                        
                        if (unitCode.equals(miniUnitCode)) {
                            // 如果物品单位与物品最小计量单位相同
                            userStorePo.setStock(String.valueOf(num7));
                        } else {
                            // 如果物品单位与物品最小计量单位不同，进行单位转换
                            userStorePo.setStock(String.valueOf(unitStock));
                        }
                        
                        userStorePo.setMiniStock(giveQuantity);
                        userStorePo.setUserId(acceptUserId);
                        userStorePo.setTimesId(timesId);
                        // 保存接受转赠用户个人物品信息
                        flag1 = userStorehouseV1InnerServiceSMOImpl.saveUserStorehouse(userStorePo);
                        if (flag1 < 1) {
                            throw new CmdException("保存失败");
                        }
                    }
                }
            }
        }
    }
}