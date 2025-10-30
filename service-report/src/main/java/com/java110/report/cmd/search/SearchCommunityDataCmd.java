package com.java110.report.cmd.search;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.data.SearchDataDto;
import com.java110.report.bmo.search.*;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;

/**
 * 社区数据搜索命令类
 * 
 * <p>该类实现了一个综合搜索功能，支持在社区管理系统中对多种数据类型进行模糊搜索。
 * 主要用于简化用户操作，无论在任何页面看到想要搜索的内容，都可以通过此功能进行统一搜索。</p>
 * 
 * <p>搜索范围包括：房屋、业主、业主成员、车辆、车辆成员、合同、报修单、员工、访客等数据。</p>
 * 
 * @author wuxw
 * @version 1.0
 * @since 2023-04-16
 */
@Java110Cmd(serviceCode = "search.searchCommunityData")
public class SearchCommunityDataCmd extends Cmd {

    /**
     * 房屋搜索业务对象
     */
    @Autowired
    private ISearchRoomBMO searchRoomBMOImpl;

    /**
     * 业主搜索业务对象
     */
    @Autowired
    private ISearchOwnerBMO searchOwnerBMOImpl;

    /**
     * 业主成员搜索业务对象
     */
    @Autowired
    private ISearchOwnerMemberBMO searchOwnerMemberBMOImpl;

    /**
     * 车辆搜索业务对象
     */
    @Autowired
    private ISearchCarBMO searchCarBMOImpl;

    /**
     * 车辆成员搜索业务对象
     */
    @Autowired
    private ISearchCarMemberBMO searchCarMemberBMOImpl;

    /**
     * 合同搜索业务对象
     */
    @Autowired
    private ISearchContractBMO searchContractBMOImpl;

    /**
     * 报修单搜索业务对象
     */
    @Autowired
    private ISearchRepairBMO searchRepairBMOImpl;

    /**
     * 员工搜索业务对象
     */
    @Autowired
    private ISearchStaffBMO searchStaffBMOImpl;

    /**
     * 访客搜索业务对象
     */
    @Autowired
    private ISearchVisitBMO searchVisitBMOImpl;

    /**
     * 参数验证方法
     * 
     * <p>验证请求参数是否包含必要的信息，确保搜索操作能够正常执行。</p>
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当参数解析失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求中必须包含小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区信息");
        // 验证请求中必须包含搜索内容
        Assert.hasKeyAndValue(reqJson, "searchValue", "未包含搜索内容");
    }

    /**
     * 执行搜索命令
     * 
     * <p>该方法负责执行实际的搜索操作，按照预定的搜索顺序对各类数据进行搜索，
     * 并将搜索结果整合后返回。</p>
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当参数解析失败时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从请求头中获取商户ID
        String storeId = context.getReqHeaders().get("store-id");

        // 创建搜索数据传输对象
        SearchDataDto searchDataDto = new SearchDataDto();
        // 设置小区ID
        searchDataDto.setCommunityId(reqJson.getString("communityId"));
        // 设置搜索值并去除首尾空格
        searchDataDto.setSearchValue(reqJson.getString("searchValue").trim());
        // 设置商户ID
        searchDataDto.setStoreId(storeId);
        
        // 如果搜索值是数字，则同时设置为电话号码进行搜索
        if (StringUtil.isNumber(reqJson.getString("searchValue").trim())) {
            searchDataDto.setTel(reqJson.getString("searchValue").trim());
        }

        // 执行房屋搜索
        searchDataDto = searchRoomBMOImpl.query(searchDataDto);

        // 执行业主搜索
        searchDataDto = searchOwnerBMOImpl.query(searchDataDto);

        // 执行业主成员搜索
        searchDataDto = searchOwnerMemberBMOImpl.query(searchDataDto);

        // 执行车辆搜索
        searchDataDto = searchCarBMOImpl.query(searchDataDto);

        // 执行车辆成员搜索
        searchDataDto = searchCarMemberBMOImpl.query(searchDataDto);

        // 执行合同搜索
        searchDataDto = searchContractBMOImpl.query(searchDataDto);

        // 执行报修单搜索
        searchDataDto = searchRepairBMOImpl.query(searchDataDto);

        // 执行员工搜索
        searchDataDto = searchStaffBMOImpl.query(searchDataDto);

        // 执行访客搜索
        searchDataDto = searchVisitBMOImpl.query(searchDataDto);

        // 设置响应结果
        context.setResponseEntity(ResultVo.createResponseEntity(searchDataDto));
    }
}