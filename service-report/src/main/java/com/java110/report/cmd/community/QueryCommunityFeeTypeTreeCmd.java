package com.java110.report.cmd.community;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.dict.DictDto;
import com.java110.dto.parking.ParkingAreaDto;
import com.java110.intf.community.ICommunityV1InnerServiceSMO;
import com.java110.intf.dev.IDictV1InnerServiceSMO;
import com.java110.intf.report.IReportCommunityInnerServiceSMO;
import com.java110.intf.user.IStaffCommunityV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

/**
 * 查询小区费用项树命令类
 * 
 * 该命令类用于查询小区费用项树形结构数据，包括小区信息和对应的费用类型配置
 * 主要功能包括：
 * 1. 验证用户权限
 * 2. 查询用户管理的小区列表
 * 3. 查询费用类型字典数据
 * 4. 构建费用项树形结构返回给前端
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "community.queryCommunityFeeTypeTree")
public class QueryCommunityFeeTypeTreeCmd extends Cmd {

    /**
     * 小区服务接口
     */
    @Autowired
    private ICommunityV1InnerServiceSMO communityV1InnerServiceSMOImpl;

    /**
     * 字典服务接口
     */
    @Autowired
    private IDictV1InnerServiceSMO dictV1InnerServiceSMOImpl;

    /**
     * 员工小区关联服务接口
     */
    @Autowired
    private IStaffCommunityV1InnerServiceSMO staffCommunityV1InnerServiceSMOImpl;

    /**
     * 命令验证方法
     * 
     * 验证用户权限，必须是管理员才能执行此命令
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证用户必须是管理员权限
        super.validateAdmin(context);
    }

    /**
     * 命令执行方法
     * 
     * 主要执行逻辑：
     * 1. 查询用户管理的小区列表
     * 2. 查询费用类型字典配置
     * 3. 构建小区费用项树形结构
     * 4. 返回树形数据给前端
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        List<CommunityDto> communityDtos = null;

        // 创建小区查询条件对象
        CommunityDto communityDto = new CommunityDto();
        // 获取当前用户ID
        String staffId = CmdContextUtils.getUserId(context);

        // 查询员工管理的小区ID列表
        List<String> communityIds = staffCommunityV1InnerServiceSMOImpl.queryStaffCommunityIds(staffId);

        // 如果员工有管理的小区，设置查询条件
        if (!ListUtil.isNull(communityIds)) {
            communityDto.setCommunityIds(communityIds.toArray(new String[communityIds.size()]));
        }

        // 查询小区信息列表
        communityDtos = communityV1InnerServiceSMOImpl.queryCommunitys(communityDto);
        JSONArray communitys = new JSONArray();
        
        // 如果没有查询到小区数据，返回空数组
        if (ListUtil.isNull(communityDtos)) {
            context.setResponseEntity(ResultVo.createResponseEntity(communitys));
            return;
        }

        JSONObject communityInfo = null;
        // 遍历小区列表，构建小区节点信息
        for (CommunityDto tmpCommunityDto : communityDtos) {
            communityInfo = new JSONObject();
            // 设置小区节点ID，前缀为"c_"加上小区ID
            communityInfo.put("id", "c_" + tmpCommunityDto.getCommunityId());
            // 设置小区ID
            communityInfo.put("communityId", tmpCommunityDto.getCommunityId());
            // 设置显示文本为小区名称
            communityInfo.put("text", tmpCommunityDto.getName());
            // 设置节点图标
            communityInfo.put("icon", "/img/org.png");
            // 初始化子节点数组
            communityInfo.put("children", new JSONArray());
            communitys.add(communityInfo);
        }

        // 查询费用类型字典数据
        DictDto dictDto = new DictDto();
        // 设置表名为支付费用配置表
        dictDto.setTableName("pay_fee_config");
        // 设置查询列为费用类型编码
        dictDto.setTableColumns("fee_type_cd");
        List<DictDto> dictDtos = dictV1InnerServiceSMOImpl.queryDicts(dictDto);

        JSONObject community = null;
        // 为每个小区节点添加费用类型子节点
        for (int cIndex = 0; cIndex < communitys.size(); cIndex++) {
            community = communitys.getJSONObject(cIndex);
            // 为当前小区查找并添加费用类型数据
            findFeeTypeCd(community, dictDtos);
        }
        
        // 设置响应数据
        context.setResponseEntity(ResultVo.createResponseEntity(communitys));
    }

    /**
     * 查找费用类型数据并构建树形结构
     * 
     * 为指定的小区节点添加费用类型子节点，并为每个费用类型添加费用标志子节点
     * 
     * @param community 小区节点JSON对象，包含小区基本信息
     * @param dictDtos 费用类型字典数据列表
     */
    private void findFeeTypeCd(JSONObject community, List<DictDto> dictDtos) {
        // 获取小区节点的子节点数组，用于存放费用类型节点
        JSONArray feeTypeCds = community.getJSONArray("children");
        JSONObject feeTypeCd = null;
        
        // 遍历费用类型字典数据，构建费用类型节点
        for (DictDto tmpDictDto : dictDtos) {
            feeTypeCd = new JSONObject();
            // 设置费用类型节点ID，前缀为"f_"加上UUID
            feeTypeCd.put("id", "f_" + GenerateCodeFactory.getUUID());
            // 设置费用类型编码
            feeTypeCd.put("feeTypeCd", tmpDictDto.getStatusCd());
            // 设置关联的小区ID
            feeTypeCd.put("communityId", community.getString("communityId"));
            // 设置显示文本为费用类型名称
            feeTypeCd.put("text", tmpDictDto.getName());
            // 设置节点图标
            feeTypeCd.put("icon", "/img/floor.png");
            // 初始化子节点数组，用于存放费用标志节点
            feeTypeCd.put("children", new JSONArray());
            // 将费用类型节点添加到小区子节点中
            feeTypeCds.add(feeTypeCd);
        }

        JSONObject feeFlag = null;
        JSONArray feeFlags = null;
        // 为每个费用类型节点添加费用标志子节点
        for (int cIndex = 0; cIndex < feeTypeCds.size(); cIndex++) {
            feeTypeCd = feeTypeCds.getJSONObject(cIndex);
            // 获取费用类型节点的子节点数组
            feeFlags = feeTypeCd.getJSONArray("children");
            
            // 添加周期性费用标志节点
            feeFlag = new JSONObject();
            feeFlag.put("id", "l_"+GenerateCodeFactory.getUUID());
            feeFlag.put("feeFlag", "1003006"); // 周期性费用标志
            feeFlag.put("communityId", community.getString("communityId"));
            feeFlag.put("feeTypeCd", feeTypeCd.getString("feeTypeCd"));
            feeFlag.put("text", "周期性费用");
            feeFlag.put("icon", "/img/unit.png");
            feeFlags.add(feeFlag);

            // 添加一次性费用标志节点
            feeFlag = new JSONObject();
            feeFlag.put("id", "l_"+GenerateCodeFactory.getUUID());
            feeFlag.put("feeFlag", "2006012"); // 一次性费用标志
            feeFlag.put("communityId", community.getString("communityId"));
            feeFlag.put("feeTypeCd", feeTypeCd.getString("feeTypeCd"));
            feeFlag.put("text", "一次性费用");
            feeFlag.put("icon", "/img/unit.png");
            feeFlags.add(feeFlag);
        }
    }
}