/**
 * 用户物品仓库查询命令类
 * 
 * 该命令类用于处理用户物品仓库列表查询请求，支持多种查询场景：
 * 1. 普通用户查询自己的物品
 * 2. 维修场景下查询可用物品
 * 3. 转赠场景下查询可转赠物品
 * 4. 权限控制下的物品查询
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.store.cmd.resourceStore;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.privilege.BasePrivilegeDto;
import com.java110.dto.repair.RepairUserDto;
import com.java110.dto.user.UserDto;
import com.java110.dto.user.UserStorehouseDto;
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.intf.community.IRepairUserInnerServiceSMO;
import com.java110.intf.store.IUserStorehouseInnerServiceSMO;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Java110Cmd(serviceCode = "resourceStore.listUserStorehouses")
public class ListUserStorehousesCmd extends Cmd {

    /** 用户物品仓库服务接口 */
    @Autowired
    private IUserStorehouseInnerServiceSMO userStorehouseInnerServiceSMOImpl;

    /** 菜单权限服务接口 */
    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    /** 维修人员服务接口 */
    @Autowired
    private IRepairUserInnerServiceSMO repairUserInnerServiceSMOImpl;

    /** 用户信息服务接口 */
    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 主要验证分页参数是否合法
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 调用父类分页信息验证方法
        super.validatePageInfo(reqJson);
    }

    /**
     * 执行命令方法
     * 
     * 处理用户物品仓库查询请求，根据不同的业务场景（维修、转赠、普通查询）返回相应的物品列表
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当数据解析失败时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求参数转换为用户物品仓库DTO对象
        UserStorehouseDto userStorehouseDto = BeanConvertUtil.covertBean(reqJson, UserStorehouseDto.class);
        
        // 获取用户ID，优先从请求参数中获取
        String userId = reqJson.getString("userId");
        
        // 处理维修场景下的用户ID获取
        if (!StringUtil.isEmpty(reqJson.getString("sign")) && reqJson.getString("sign").equals("1")) {
            RepairUserDto repairUserDto = new RepairUserDto();
            repairUserDto.setRepairId(reqJson.getString("repairId"));
            repairUserDto.setState(RepairUserDto.STATE_DOING); // 设置维修状态为处理中
            // 查询处理中的维修人员信息
            List<RepairUserDto> repairUserDtos = repairUserInnerServiceSMOImpl.queryRepairUsers(repairUserDto);
            if (repairUserDtos != null && repairUserDtos.size() == 1) {
                // 获取维修人员的员工ID作为用户ID
                userId = repairUserDtos.get(0).getStaffId();
            }
        }
        
        // 如果用户ID为空，从请求头中获取
        if (StringUtil.isEmpty(userId)) {
            userId = context.getReqHeaders().get("user-id");
        }
        
        List<Map> privileges = null;
        // 检查用户是否拥有查看所有个人物品的权限
        BasePrivilegeDto basePrivilegeDto = new BasePrivilegeDto();
        basePrivilegeDto.setResource("/everythingGoods"); // 设置资源路径
        basePrivilegeDto.setUserId(userId); // 设置用户ID
        privileges = menuInnerServiceSMOImpl.checkUserHasResource(basePrivilegeDto);
        
        // 如果用户有查看所有个人物品的权限
        if (privileges != null && privileges.size() > 0) {
            if (!StringUtil.isEmpty(reqJson.getString("sign")) && reqJson.getString("sign").equals("1")) {
                // 维修场景：查询当前用户信息
                UserDto userDto = new UserDto();
                userDto.setUserId(userId);
                List<UserDto> users = userInnerServiceSMOImpl.getUsers(userDto);
                Assert.listOnlyOne(users, "查询用户信息错误！"); // 验证只能查询到一个用户
                userStorehouseDto.setUserId(userId);
                userStorehouseDto.setUserName(users.get(0).getName());
            } else {
                // 普通场景：使用搜索参数
                userStorehouseDto.setUserId(reqJson.getString("searchUserId"));
                userStorehouseDto.setUserName(reqJson.getString("searchUserName"));
            }
        }
        
        // 处理转赠场景：转赠时只能查询自己的物品
        if (!StringUtil.isEmpty(reqJson.getString("giveType")) && "1".equals(reqJson.getString("giveType"))) {
            if (!StringUtil.isEmpty(reqJson.getString("sign")) && reqJson.getString("sign").equals("1")) {
                // 维修场景下的转赠：查询当前用户信息
                UserDto userDto = new UserDto();
                userDto.setUserId(userId);
                List<UserDto> users = userInnerServiceSMOImpl.getUsers(userDto);
                Assert.listOnlyOne(users, "查询用户信息错误！");
                userStorehouseDto.setUserId(userId);
                userStorehouseDto.setUserName(users.get(0).getName());
            } else {
                // 普通转赠场景：使用请求参数中的用户信息
                userStorehouseDto.setUserId(reqJson.getString("userId"));
                userStorehouseDto.setUserName(reqJson.getString("searchUserName"));
            }
        }
        
        // 设置查询条件：库存大于零
        userStorehouseDto.setLagerStockZero("1");

        // 查询符合条件的物品总数
        int count = userStorehouseInnerServiceSMOImpl.queryUserStorehousesCount(userStorehouseDto);

        List<UserStorehouseDto> userStorehouseDtos = new ArrayList<>();

        if (count > 0) {
            if (!StringUtil.isEmpty(reqJson.getString("flag")) && reqJson.getString("flag").equals("1")) {
                // 报修场景：过滤掉固定物品，只返回非固定物品
                List<UserStorehouseDto> userStorehouses = userStorehouseInnerServiceSMOImpl.queryUserStorehouses(userStorehouseDto);
                for (UserStorehouseDto userStorehouse : userStorehouses) {
                    // 获取物品是否是固定物品
                    String isFixed = userStorehouse.getIsFixed();
                    // Y表示是固定物品;N表示不是固定物品;T表示是通用物品
                    if (!StringUtil.isEmpty(isFixed) && isFixed.equals("Y")) {
                        continue; // 跳过固定物品
                    } else {
                        userStorehouseDtos.add(userStorehouse); // 添加非固定物品
                    }
                }
            } else {
                // 普通查询：返回所有符合条件的物品
                userStorehouseDtos = userStorehouseInnerServiceSMOImpl.queryUserStorehouses(userStorehouseDto);
            }
        } else {
            // 处理无数据情况
            Object chooseType = reqJson.get("chooseType");
            // 如果是维修选择类型且无物品，返回提示信息
            if (chooseType != null && !StringUtil.isEmpty(chooseType.toString()) && reqJson.get("chooseType").equals("repair")) {
                ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_BUSINESS_VERIFICATION, "您还没有该类型的物品，请您先申领物品！");
                context.setResponseEntity(responseEntity);
                return; // 直接返回，不继续执行
            }
            userStorehouseDtos = new ArrayList<>(); // 返回空列表
        }

        // 构建返回结果对象
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, userStorehouseDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应实体到上下文
        context.setResponseEntity(responseEntity);
    }
}