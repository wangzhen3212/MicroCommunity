package com.java110.user.cmd.org;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.org.OrgDto;
import com.java110.dto.org.OrgTreeDto;
import com.java110.dto.store.StoreDto;
import com.java110.dto.unit.UnitDto;
import com.java110.intf.user.IOrgV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 查询管理员组织树命令类
 * 
 * 该命令用于查询物业类型的组织架构树形结构数据，主要用于管理后台的组织架构展示
 * 通过递归方式构建组织树，支持多级组织结构的展示
 * 
 * @serviceCode org.queryAdminOrgTree
 */
@Java110Cmd(serviceCode = "org.queryAdminOrgTree")
public class QueryAdminOrgTreeCmd extends Cmd {

    @Autowired
    private IOrgV1InnerServiceSMO orgV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证当前用户是否具有管理员权限
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证管理员权限
        super.validateAdmin(context);
    }

    /**
     * 命令执行方法
     * 
     * 查询物业类型的组织架构，构建组织树形结构并返回
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 创建组织查询条件对象
        OrgDto orgDto = new OrgDto();
        // 设置查询条件：只查询物业类型的组织
        orgDto.setStoreTypeCd(StoreDto.STORE_TYPE_PROPERTY);
        // 调用服务查询组织列表
        List<OrgDto> orgDtos = orgV1InnerServiceSMOImpl.queryOrgs(orgDto);
        // 初始化组织树JSON数组
        JSONArray orgs = new JSONArray();

        // 如果查询结果为空，直接返回空数组
        if(ListUtil.isNull(orgDtos)){
            context.setResponseEntity(ResultVo.createResponseEntity(orgs));
            return;
        }

        JSONObject orgInfo = null;
        // 遍历所有组织，筛选出顶级组织（店铺级别的组织）
        for (OrgDto tmpOrgDto : orgDtos) {
            if (OrgDto.ORG_LEVEL_STORE.equals(tmpOrgDto.getOrgLevel())) {
                // 创建组织节点信息
                orgInfo = new JSONObject();
                orgInfo.put("id",  tmpOrgDto.getOrgId());
                orgInfo.put("orgId", tmpOrgDto.getOrgId());
                orgInfo.put("text", tmpOrgDto.getOrgName());
                orgInfo.put("icon", "/img/org.png");
                orgs.add(orgInfo);
            }
        }

        // 如果没有顶级组织，直接返回空数组
        if(ListUtil.isNull(orgs)){
            context.setResponseEntity(ResultVo.createResponseEntity(orgs));
            return;
        }

        // 为每个顶级组织递归查找子组织
        for(int orgIndex = 0 ;orgIndex < orgs.size();orgIndex++){
            orgInfo = orgs.getJSONObject(orgIndex);
            // 递归构建组织树
            findChilds(orgInfo, orgDtos);
        }
        
        // 设置响应结果
        context.setResponseEntity(ResultVo.createResponseEntity(orgs));
    }

    /**
     * 递归查找子组织方法
     * 
     * 根据当前组织节点，在组织列表中查找其所有子组织，并递归构建完整的组织树
     * 
     * @param curOrg 当前组织节点JSON对象
     * @param orgDtos 所有组织数据列表
     */
    private void findChilds(JSONObject curOrg, List<OrgDto> orgDtos) {

        // 初始化子组织数组
        JSONArray childs = new JSONArray();
        JSONObject child = null;
        
        // 遍历所有组织，查找当前组织的直接子组织
        for (OrgDto orgDto : orgDtos) {
            // 跳过当前组织自身
            if (curOrg.getString("id").equals(orgDto.getOrgId())) { 
                continue;
            }
            // 如果组织的父ID等于当前组织ID，说明是直接子组织
            if (orgDto.getParentOrgId().equals(curOrg.getString("id"))) {
                // 创建子组织节点
                child = new JSONObject();
                child.put("id",  orgDto.getOrgId());
                child.put("orgId", orgDto.getOrgId());
                child.put("text", orgDto.getOrgName());
                child.put("icon", "/img/org.png");
                childs.add(child);
            }
        }

        // 如果没有子组织，直接返回
        if (ListUtil.isNull(childs)) {
            return;
        }

        // 将子组织数组添加到当前组织节点
        curOrg.put("children",childs);

        // 递归为每个子组织查找其子组织
        for(int orgIndex = 0 ;orgIndex < childs.size();orgIndex++){
            child = childs.getJSONObject(orgIndex);
            findChilds(child, orgDtos);
        }
    }
}