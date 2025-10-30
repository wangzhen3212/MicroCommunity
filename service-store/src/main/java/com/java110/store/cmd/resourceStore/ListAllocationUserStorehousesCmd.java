/**
 * 资源库分配用户仓库列表查询命令类
 * 
 * 该命令类负责处理资源库中分配用户仓库的列表查询请求，包括分页查询、转赠数量统计等功能
 * 通过注解@Java110Cmd声明服务代码为"resourceStore.listAllocationUserStorehouses"
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.store.cmd.resourceStore;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.PageDto;
import com.java110.dto.purchase.AllocationUserStorehouseDto;
import com.java110.intf.store.IAllocationUserStorehouseInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;


@Java110Cmd(serviceCode = "resourceStore.listAllocationUserStorehouses")
public class ListAllocationUserStorehousesCmd extends Cmd {

    /**
     * 分配用户仓库内部服务接口
     */
    @Autowired
    private IAllocationUserStorehouseInnerServiceSMO allocationUserStorehouseInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 主要验证分页参数的正确性，调用父类的分页信息验证方法
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 调用父类方法验证分页信息
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 
     * 处理分配用户仓库列表查询请求，包括：
     * 1. 查询符合条件的记录总数
     * 2. 计算当前页的转赠数量小计
     * 3. 计算所有记录的转赠数量总计
     * 4. 构建响应结果并返回
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当数据解析失败时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从请求头中获取店铺ID
        String storeId = context.getReqHeaders().get("store-id");
        // 将请求JSON转换为分配用户仓库数据传输对象
        AllocationUserStorehouseDto allocationUserStorehouseDto = BeanConvertUtil.covertBean(reqJson, AllocationUserStorehouseDto.class);
        // 设置店铺ID到查询条件中
        allocationUserStorehouseDto.setStoreId(storeId);

        // 查询符合条件的记录总数
        int count = allocationUserStorehouseInnerServiceSMOImpl.queryAllocationUserStorehousesCount(allocationUserStorehouseDto);

        // 初始化结果列表
        List<AllocationUserStorehouseDto> allocationUserStorehouseDtos = new ArrayList<>();

        // 如果有符合条件的记录
        if (count > 0) {
            // 查询当前页的分配用户仓库列表
            List<AllocationUserStorehouseDto> allocationUserStorehouseList = allocationUserStorehouseInnerServiceSMOImpl.queryAllocationUserStorehouses(allocationUserStorehouseDto);
            
            // 初始化转赠数量小计（当前页）
            BigDecimal subTotalQuantity = BigDecimal.ZERO;
            // 初始化转赠数量总计（所有记录）
            BigDecimal totalQuantity = BigDecimal.ZERO;
            
            // 计算当前页的转赠数量小计
            for (AllocationUserStorehouseDto allocationUserStorehouse : allocationUserStorehouseList) {
                // 获取单条记录的转赠数量
                String giveQuantity = allocationUserStorehouse.getGiveQuantity();
                BigDecimal quantity = new BigDecimal(giveQuantity);
                // 累加到小计中
                subTotalQuantity = subTotalQuantity.add(quantity);
            }
            
            // 查询所有转赠记录（不分页，用于计算总计）
            allocationUserStorehouseDto.setPage(PageDto.DEFAULT_PAGE);
            List<AllocationUserStorehouseDto> allocationUserStorehouses = allocationUserStorehouseInnerServiceSMOImpl.queryAllocationUserStorehouses(allocationUserStorehouseDto);
            
            // 计算所有记录的转赠数量总计
            for (AllocationUserStorehouseDto allocationUserStorehouse : allocationUserStorehouses) {
                // 获取单条记录的转赠数量
                String giveQuantity = allocationUserStorehouse.getGiveQuantity();
                BigDecimal quantity = new BigDecimal(giveQuantity);
                // 累加到总计中
                totalQuantity = totalQuantity.add(quantity);
            }
            
            // 为每条记录设置小计和总计信息
            for (AllocationUserStorehouseDto allocationUserStorehouse : allocationUserStorehouseList) {
                allocationUserStorehouse.setSubTotalQuantity(subTotalQuantity.toString());
                allocationUserStorehouse.setHighTotalQuantity(totalQuantity.toString());
                allocationUserStorehouseDtos.add(allocationUserStorehouse);
            }
        } else {
            // 如果没有记录，返回空列表
            allocationUserStorehouseDtos = new ArrayList<>();
        }

        // 构建分页结果对象
        // 计算总页数：总记录数 / 每页行数，向上取整
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, allocationUserStorehouseDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应实体到上下文
        context.setResponseEntity(responseEntity);
    }
}