/**
 * 采购申请明细查询命令类
 * 
 * 该类负责处理采购申请明细列表查询请求，根据用户权限返回相应的采购申请明细数据
 * 实现了分页查询和权限控制功能，支持普通用户查看自己的申请和管理员查看所有申请
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.store.cmd.purchaseApplyDetail;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.privilege.BasePrivilegeDto;
import com.java110.dto.purchase.PurchaseApplyDetailDto;
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.intf.store.IPurchaseApplyDetailInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.purchaseApplyDetail.ApiPurchaseApplyDetailDataVo;
import com.java110.vo.api.purchaseApplyDetail.ApiPurchaseApplyDetailVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Java110Cmd(serviceCode = "purchaseApplyDetail.listPurchaseApplyDetails")
public class ListPurchaseApplyDetailsCmd extends Cmd {

    /**
     * 采购申请明细内部服务接口
     */
    @Autowired
    private IPurchaseApplyDetailInnerServiceSMO purchaseApplyDetailInnerServiceSMOImpl;

    /**
     * 菜单权限内部服务接口
     */
    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 主要验证分页参数的正确性，包括页码和每页记录数
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 调用父类的分页信息验证方法，验证页码和每页记录数参数
        super.validatePageInfo(reqJson);
    }

    /**
     * 执行采购申请明细查询命令
     * 
     * 根据用户权限查询采购申请明细列表，支持分页显示
     * 普通用户只能查看自己创建的申请，有权限的用户可以查看所有申请
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象，包含查询条件和分页参数
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当数据解析失败时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从上下文中获取店铺ID
        String storeId = CmdContextUtils.getStoreId(context);
        
        // 将请求JSON转换为采购申请明细DTO对象
        PurchaseApplyDetailDto purchaseApplyDetailDto = BeanConvertUtil.covertBean(reqJson, PurchaseApplyDetailDto.class);
        purchaseApplyDetailDto.setStoreId(storeId);
        
        // 权限检查：判断用户是否有查看所有采购申请明细的权限
        BasePrivilegeDto basePrivilegeDto = new BasePrivilegeDto();
        basePrivilegeDto.setResource("/viewAllPurchaseApplyDetail"); // 设置权限资源路径
        basePrivilegeDto.setUserId(reqJson.getString("userId")); // 设置用户ID
        
        // 检查用户是否拥有指定资源的权限
        List<Map> privileges = menuInnerServiceSMOImpl.checkUserHasResource(basePrivilegeDto);
        
        // 根据权限设置查询条件
        if (privileges == null || privileges.size() == 0) {
            // 无权限用户只能查看自己创建的申请
            purchaseApplyDetailDto.setCreateUserId(reqJson.getString("userId"));
            purchaseApplyDetailDto.setCreateUserName(reqJson.getString("userName"));
        } else {
            // 有权限用户可以查看所有申请，清空创建人过滤条件
            purchaseApplyDetailDto.setCreateUserId("");
            purchaseApplyDetailDto.setCreateUserName("");
        }
        
        // 查询采购申请明细总数
        int count = purchaseApplyDetailInnerServiceSMOImpl.queryPurchaseApplyDetailsCount(purchaseApplyDetailDto);
        
        List<ApiPurchaseApplyDetailDataVo> purchaseApplyDetails = null;
        
        // 如果有数据则查询详情，否则返回空列表
        if (count > 0) {
            // 查询采购申请明细列表并转换为VO对象
            purchaseApplyDetails = BeanConvertUtil.covertBeanList(
                purchaseApplyDetailInnerServiceSMOImpl.queryPurchaseApplyDetails(purchaseApplyDetailDto), 
                ApiPurchaseApplyDetailDataVo.class
            );
        } else {
            // 无数据时返回空列表
            purchaseApplyDetails = new ArrayList<>();
        }
        
        // 构建返回VO对象
        ApiPurchaseApplyDetailVo apiPurchaseApplyDetailVo = new ApiPurchaseApplyDetailVo();
        apiPurchaseApplyDetailVo.setTotal(count); // 设置总记录数
        // 计算总页数：总记录数 / 每页记录数，向上取整
        apiPurchaseApplyDetailVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        apiPurchaseApplyDetailVo.setPurchaseApplyDetails(purchaseApplyDetails); // 设置明细数据列表
        
        // 构建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(
            JSONObject.toJSONString(apiPurchaseApplyDetailVo), // 将VO对象转换为JSON字符串
            HttpStatus.OK // 设置HTTP状态码为200
        );
        
        // 将响应设置到上下文中
        context.setResponseEntity(responseEntity);
    }
}