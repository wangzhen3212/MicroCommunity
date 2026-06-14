/**
 * 查询用户已入驻小区列表命令类
 * 
 * 该命令用于处理查询当前用户已入驻的小区列表请求，根据用户角色（管理员/普通员工）
 * 返回对应的小区信息列表。管理员可以查看物业下的所有小区，普通员工只能查看自己关联的小区。
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.community.cmd.community;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.privilege.RoleCommunityDto;
import com.java110.dto.staffCommunity.StaffCommunityDto;
import com.java110.dto.store.StoreDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.community.ICommunityInnerServiceSMO;
import com.java110.intf.store.IStoreV1InnerServiceSMO;
import com.java110.intf.user.*;
import com.java110.utils.constant.StateConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import com.java110.vo.api.community.ApiCommunityDataVo;
import com.java110.vo.api.community.ApiCommunityVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "community.listMyEnteredCommunitys")
public class ListMyEnteredCommunitysCmd extends Cmd {

    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    @Autowired
    private IOrgStaffRelInnerServiceSMO orgStaffRelInnerServiceSMOImpl;

    @Autowired
    private IStaffCommunityV1InnerServiceSMO staffCommunityV1InnerServiceSMOImpl;

    @Autowired
    private IOrgInnerServiceSMO orgInnerServiceSMOImpl;

    @Autowired
    private IStoreV1InnerServiceSMO storeV1InnerServiceSMOImpl;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求报文中是否包含必要的商户信息和用户信息，并将商户ID和用户ID设置到请求JSON中
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 从上下文中获取商户ID和用户ID并设置到请求JSON中
        reqJson.put("storeId", CmdContextUtils.getStoreId(cmdDataFlowContext));
        reqJson.put("userId", CmdContextUtils.getUserId(cmdDataFlowContext));
        
        // 验证请求报文中必须包含商户信息和用户信息
        Assert.hasKeyAndValue(reqJson, "storeId", "请求报文中未包含商户信息");
        Assert.hasKeyAndValue(reqJson, "userId", "请求报文中未包含用户信息");
    }

    /**
     * 执行命令的主方法
     * 
     * 根据用户角色查询对应的已入驻小区列表：
     * - 管理员：查询物业下的所有小区
     * - 普通员工：查询员工关联的小区
     * 对于管理员和开发商户，当没有小区时提供默认小区
     * 
     * @param event 命令事件对象
     * @param context 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当执行过程中出现错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 查询用户信息，验证用户是否存在
        UserDto userDto = new UserDto();
        userDto.setUserId(reqJson.getString("userId"));
        userDto.setPage(1);
        userDto.setRow(1);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);

        Assert.listOnlyOne(userDtos, "用户不存在");

        // 校验商户是否存在
        StoreDto storeDto = new StoreDto();
        storeDto.setStoreId(reqJson.getString("storeId"));
        List<StoreDto> storeDtos = storeV1InnerServiceSMOImpl.queryStores(storeDto);

        Assert.listOnlyOne(storeDtos, "商户不存在");

        int count = 0;
        List<ApiCommunityDataVo> communitys = null;
        ApiCommunityDataVo tmpApiCommunityDataVo = null;
        
        // 根据用户角色级别区分查询逻辑
        if (UserDto.LEVEL_CD_ADMIN.equals(userDtos.get(0).getLevelCd())) {
            // 管理员角色：查询物业下的所有小区
            CommunityDto communityDto = BeanConvertUtil.covertBean(reqJson, CommunityDto.class);
            communityDto.setMemberId(reqJson.getString("storeId")); // 设置商户ID作为成员ID
            communityDto.setAuditStatusCd(StateConstant.AGREE_AUDIT); // 设置审核状态为已同意
            
            // 如果请求中包含小区名称，则添加名称过滤条件
            if (reqJson.containsKey("communityName")) {
                communityDto.setName(reqJson.getString("communityName"));
            }
            
            // 查询小区总数和小区列表
            count = communityInnerServiceSMOImpl.queryCommunitysCount(communityDto);
            if (count > 0) {
                communitys = BeanConvertUtil.covertBeanList(communityInnerServiceSMOImpl.queryCommunitys(communityDto), ApiCommunityDataVo.class);
            } else {
                communitys = new ArrayList<>();
            }
        } else {
            // 普通员工角色：查询员工关联的小区
            StaffCommunityDto staffCommunityDto = BeanConvertUtil.covertBean(reqJson, StaffCommunityDto.class);
            staffCommunityDto.setStaffId(userDtos.get(0).getUserId()); // 设置员工ID
            
            // 查询员工关联的小区总数和列表
            count = staffCommunityV1InnerServiceSMOImpl.queryStaffCommunitysCount(staffCommunityDto);
            if (count > 0) {
                List<StaffCommunityDto> staffCommunityDtos = staffCommunityV1InnerServiceSMOImpl.queryStaffCommunitys(staffCommunityDto);
                communitys = new ArrayList<>();
                
                // 将员工小区DTO转换为API响应VO对象
                for (StaffCommunityDto tmpStaffCommunityDto : staffCommunityDtos) {
                    tmpApiCommunityDataVo = new ApiCommunityDataVo();
                    tmpApiCommunityDataVo.setCommunityId(tmpStaffCommunityDto.getCommunityId());
                    tmpApiCommunityDataVo.setName(tmpStaffCommunityDto.getCommunityName());
                    tmpApiCommunityDataVo.setStoreId(tmpStaffCommunityDto.getStoreId());
                    tmpApiCommunityDataVo.setStoreName(tmpStaffCommunityDto.getStoreName());
                    tmpApiCommunityDataVo.setState("1100"); // 设置小区状态为正常
                    communitys.add(tmpApiCommunityDataVo);
                }
            } else {
                communitys = new ArrayList<>();
            }
        }
        
        // 兼容系统刚开始没有小区时的情况，为管理员和开发商户提供默认小区
        if (ListUtil.isNull(communitys) && (StoreDto.STORE_TYPE_ADMIN.equals(storeDtos.get(0).getStoreTypeCd()) || StoreDto.STORE_TYPE_DEV.equals(storeDtos.get(0).getStoreTypeCd()))) {
            ApiCommunityDataVo apiCommunityDataVo = new ApiCommunityDataVo();
            apiCommunityDataVo.setCommunityId("-1"); // 默认小区ID
            apiCommunityDataVo.setName("默认小区"); // 默认小区名称
            apiCommunityDataVo.setTel("18909711234"); // 默认联系电话
            apiCommunityDataVo.setState("1100"); // 设置小区状态为正常
            communitys.add(apiCommunityDataVo);
        }

        // 构建API响应对象
        ApiCommunityVo apiCommunityVo = new ApiCommunityVo();
        apiCommunityVo.setTotal(count); // 设置总记录数
        apiCommunityVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row"))); // 计算总页数
        apiCommunityVo.setCommunitys(communitys); // 设置小区列表
        apiCommunityVo.setCode(ResultVo.CODE_OK); // 设置响应码为成功
        apiCommunityVo.setMsg(ResultVo.MSG_OK); // 设置响应消息
        
        // 创建HTTP响应实体并设置到上下文中
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiCommunityVo), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }
}