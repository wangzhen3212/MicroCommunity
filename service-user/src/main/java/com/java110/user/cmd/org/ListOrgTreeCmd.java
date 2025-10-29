package com.java110.user.cmd.org;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.privilege.BasePrivilegeDto;
import com.java110.dto.org.OrgDto;
import com.java110.dto.org.OrgStaffRelDto;
import com.java110.dto.org.OrgTreeDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.intf.user.IOrgStaffRelInnerServiceSMO;
import com.java110.intf.user.IOrgV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Java110CmdDoc(title = "查询组织",
        description = "主要用于添加员工时的组织ID 可以通过此接口查询， 此接口返回为树形接口，请其他系统用树形的方式展示数据，具体请参考物业系统组织架构展示方式",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/org.listOrgTree",
        resource = "userDoc",
        author = "吴学文",
        serviceCode = "org.listOrgTree",
        seq = 8
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "orgId", length = 30, remark = "组织ID，非必填"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "data", type = "Object", remark = "有效数据"),
                @Java110ParamDoc(parentNodeName = "data", name = "allOrgName", type = "String", remark = "组织名称"),
                @Java110ParamDoc(parentNodeName = "data", name = "id", type = "String", remark = "组织ID"),
                @Java110ParamDoc(parentNodeName = "data", name = "children", type = "Array", remark = "子节点"),
                @Java110ParamDoc(parentNodeName = "children", name = "allOrgName", type = "String", remark = "组织名称"),
                @Java110ParamDoc(parentNodeName = "children", name = "id", type = "String", remark = "组织ID"),
        }
)

@Java110ExampleDoc(
        reqBody = "http://{ip}:{port}/app/org.listOrgTree",
        resBody = "{\"code\":0,\"data\":{\"allOrgName\":\"演示物业\",\"children\":[{\"allOrgName\":\"演示物业 / 软件部\",\"icon\":\"/img/org.png\",\"id\":\"102022091988250052\",\"parentId\":\"842022081548770433\",\"text\":\"软件部\"}],\"icon\":\"/img/org.png\",\"id\":\"842022081548770433\",\"parentId\":\"-1\",\"text\":\"演示物业\"},\"msg\":\"成功\",\"page\":0,\"records\":1,\"rows\":0,\"total\":1}"
)

/**
 * 查询组织树形结构命令类
 * 
 * 该类用于查询组织架构的树形结构数据，主要用于员工添加时选择组织ID。
 * 返回的数据为树形结构，便于前端以树形方式展示组织架构。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "org.listOrgTree")
public class ListOrgTreeCmd extends Cmd {

    @Autowired
    private IOrgV1InnerServiceSMO orgV1InnerServiceSMOImpl;

    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    @Autowired
    private IOrgStaffRelInnerServiceSMO iOrgStaffRelInnerServiceSMO;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求中是否包含商户信息，商户信息可以从请求头或请求体中获取
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 从请求头获取商户ID
        String storeId = context.getReqHeaders().get("store-id");
        if (StringUtil.isEmpty(storeId)) {
            // 如果请求头中没有商户ID，则从请求体中获取
            storeId = reqJson.getString("storeId");
        }

        // 验证商户ID不能为空
        Assert.hasLength(storeId, "未包含商户信息");
    }

    /**
     * 执行命令方法
     * 
     * 根据用户权限查询组织架构树形数据：
     * 1. 管理员用户可以查看所有组织
     * 2. 普通用户只能查看自己所属的组织及其相关组织
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当执行过程中出现错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 获取商户ID和用户ID
        String storeId = context.getReqHeaders().get("store-id");
        String userId = context.getReqHeaders().get("user-id");
        if (StringUtil.isEmpty(storeId)) {
            storeId = reqJson.getString("storeId");
        }
        Assert.hasLength(storeId, "未包含商户信息");

        // 创建组织查询条件
        OrgDto orgDto = new OrgDto();
        orgDto.setStoreId(storeId);

        // 查询用户信息，判断用户权限
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        userDto.setPage(1);
        userDto.setRow(1);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);
        Assert.listOnlyOne(userDtos, "用户不存在");
        
        // 如果不是管理员用户，需要根据权限过滤组织数据
        if (!UserDto.LEVEL_CD_ADMIN.equals(userDtos.get(0).getLevelCd())) {
            // 检查用户是否有查看所有组织的权限
            BasePrivilegeDto basePrivilegeDto = new BasePrivilegeDto();
            basePrivilegeDto.setResource("/viewAllOrganization");
            basePrivilegeDto.setUserId(reqJson.getString("userId"));
            List<Map> privileges = menuInnerServiceSMOImpl.checkUserHasResource(basePrivilegeDto);
            
            // 如果没有查看所有组织的权限，则只返回用户所属的组织
            if (ListUtil.isNull(privileges)) {
                // 查询员工所属的组织关系
                OrgStaffRelDto orgStaffRelDto = new OrgStaffRelDto();
                orgStaffRelDto.setStaffId(reqJson.getString("userId"));
                List<OrgStaffRelDto> orgStaffRelDtos = iOrgStaffRelInnerServiceSMO.queryOrgInfoByStaffIdsNew(orgStaffRelDto);
                
                if (ListUtil.isNotNull(orgStaffRelDtos)) {
                    List<String> haveOrgList = new ArrayList<String>();
                    // 遍历员工所属的组织，构建可访问的组织ID列表
                    for (OrgStaffRelDto orgStaffRelDto1 : orgStaffRelDtos) {
                        OrgDto orgDto1 = new OrgDto();
                        orgDto1.setOrgId(orgStaffRelDto1.getOrgId());
                        List<OrgDto> orgDtoList = orgV1InnerServiceSMOImpl.queryOrgs(orgDto1);
                        // 递归查找相关的公司级组织
                        findCompany(haveOrgList, orgDtoList);
                    }
                    // 设置查询条件，只查询用户有权限的组织
                    orgDto.setOrgIds(haveOrgList.toArray(new String[haveOrgList.size()]));
                }
            }
        }

        // 查询组织数据
        List<OrgDto> orgDtos = orgV1InnerServiceSMOImpl.queryOrgs(orgDto);

        // 查找根节点（商户级别的组织）
        OrgTreeDto storeOrgTreeDto = null;
        for (OrgDto tmpOrgDto : orgDtos) {
            if (OrgDto.ORG_LEVEL_STORE.equals(tmpOrgDto.getOrgLevel())) {
                storeOrgTreeDto = new OrgTreeDto(tmpOrgDto.getOrgId(), tmpOrgDto.getOrgName(), 
                        tmpOrgDto.getParentOrgId(), tmpOrgDto.getOrgName());
            }
        }

        // 如果没有找到根节点，直接返回
        if (storeOrgTreeDto == null) {
            return;
        }

        // 递归构建组织树
        findChilds(storeOrgTreeDto, orgDtos);
        // 设置响应数据
        context.setResponseEntity(ResultVo.createResponseEntity(storeOrgTreeDto));
    }

    /**
     * 递归查找子组织节点
     * 
     * 根据父组织节点，在所有组织列表中查找其直接子节点，并递归构建完整的组织树
     * 
     * @param parentOrgDto 父组织节点
     * @param orgDtos 所有组织数据列表
     */
    private void findChilds(OrgTreeDto parentOrgDto, List<OrgDto> orgDtos) {
        List<OrgTreeDto> childs = new ArrayList<>();
        OrgTreeDto child = null;
        
        // 遍历所有组织，查找当前父节点的直接子节点
        for (OrgDto orgDto : orgDtos) {
            if (parentOrgDto.getId().equals(orgDto.getOrgId())) { 
                // 跳过自身节点
                continue;
            }
            if (orgDto.getParentOrgId().equals(parentOrgDto.getId())) {
                // 创建子节点，构建完整的组织名称路径
                child = new OrgTreeDto(orgDto.getOrgId(), orgDto.getOrgName(), 
                        orgDto.getParentOrgId(), parentOrgDto.getAllOrgName() + " / " + orgDto.getOrgName());
                childs.add(child);
            }
        }

        // 如果没有子节点，直接返回
        if (ListUtil.isNull(childs)) {
            return;
        }

        // 设置子节点列表
        parentOrgDto.setChildren(childs);

        // 递归处理每个子节点
        for (OrgTreeDto orgTreeDto : childs) {
            findChilds(orgTreeDto, orgDtos);
        }
    }

    /**
     * 递归查找公司级组织
     * 
     * 根据员工所属的组织，递归查找相关的公司级组织及其上下级组织，
     * 构建用户有权限访问的组织ID列表
     * 
     * @param haveOrgList 有权限的组织ID列表（输出参数）
     * @param orgDtoList 当前处理的组织列表
     */
    private void findCompany(List<String> haveOrgList, List<OrgDto> orgDtoList) {
        for (OrgDto orgDto : orgDtoList) {
            // 添加当前组织ID到权限列表
            haveOrgList.add(orgDto.getOrgId());
            
            if (OrgDto.ORG_LEVEL_STORE.equals(orgDto.getOrgLevel())) {
                // 商户级别的组织，不需要继续查找
                continue;
            }
            
            if (OrgDto.ORG_LEVEL_COMPANY.equals(orgDto.getOrgLevel())) {
                // 公司级别的组织，需要查找其上级和同级组织
                
                // 查找上级组织（商户级别）
                OrgDto orgDto1 = new OrgDto();
                orgDto1.setOrgId(orgDto.getParentOrgId());
                List<OrgDto> orgDtoList1 = orgV1InnerServiceSMOImpl.queryOrgs(orgDto1);
                for (OrgDto orgDto2 : orgDtoList1) {
                    haveOrgList.add(orgDto2.getOrgId());
                }
                
                // 查找同级组织（公司级别的子组织）
                OrgDto orgDto2 = new OrgDto();
                orgDto2.setParentOrgId(orgDto.getOrgId());
                List<OrgDto> orgDtoList2 = orgV1InnerServiceSMOImpl.queryOrgs(orgDto2);
                for (OrgDto orgDto3 : orgDtoList2) {
                    haveOrgList.add(orgDto3.getOrgId());
                }
            } else {
                // 部门级别的组织，需要递归查找上级组织和下级组织
                
                // 递归查找上级组织
                OrgDto orgDto1 = new OrgDto();
                orgDto1.setOrgId(orgDto.getParentOrgId());
                List<OrgDto> orgDtoList1 = orgV1InnerServiceSMOImpl.queryOrgs(orgDto1);
                findCompany(haveOrgList, orgDtoList1);

                // 如果不是公司级别，查找下级组织
                if (!OrgDto.ORG_LEVEL_COMPANY.equals(orgDto.getOrgLevel())) {
                    OrgDto orgDto3 = new OrgDto();
                    orgDto3.setParentOrgId(orgDto.getOrgId());
                    List<OrgDto> orgDtoList2 = orgV1InnerServiceSMOImpl.queryOrgs(orgDto3);
                    for (OrgDto orgDto4 : orgDtoList2) {
                        haveOrgList.add(orgDto4.getOrgId());
                    }
                }
            }
        }
    }
}