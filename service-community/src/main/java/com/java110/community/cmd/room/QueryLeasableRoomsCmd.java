package com.java110.community.cmd.room;

import com.alibaba.fastjson.JSONObject;
import com.java110.community.dao.IRoomServiceDao;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.data.DataPrivilegeStaffDto;
import com.java110.intf.community.IDataPrivilegeUnitV1InnerServiceSMO;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.SMOException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Java110CmdDoc(title = "查询可出租房源",
        description = "查询无业主的房屋/商铺，返回租金 roomRent，供合同关联出租房屋选择",
        httpMethod = "get",
        url = "http://{ip}:{port}/app/room.queryLeasableRooms",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "room.queryLeasableRooms",
        seq = 17
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "roomType", length = 12, remark = "房屋类型 1010301房屋 2020602商铺"),
        @Java110ParamDoc(name = "roomNumLike", length = 12, remark = "房号模糊搜索"),
        @Java110ParamDoc(name = "floorNum", length = 12, remark = "楼栋编号"),
        @Java110ParamDoc(name = "unitNum", length = 12, remark = "单元编号"),
        @Java110ParamDoc(name = "roomState", length = 64, remark = "房屋状态，多个用逗号分隔"),
        @Java110ParamDoc(name = "page", type = "int", length = 11, remark = "页数"),
        @Java110ParamDoc(name = "row", type = "int", length = 11, remark = "行数"),
})

@Java110ResponseDoc(params = {
        @Java110ParamDoc(name = "records", type = "int", length = 11, remark = "总页数"),
        @Java110ParamDoc(name = "total", type = "int", length = 11, remark = "总数据"),
        @Java110ParamDoc(name = "data", type = "Object", remark = "有效数据"),
        @Java110ParamDoc(parentNodeName = "data", name = "roomId", type = "String", remark = "房屋ID"),
        @Java110ParamDoc(parentNodeName = "data", name = "roomName", type = "String", remark = "房屋名称"),
        @Java110ParamDoc(parentNodeName = "data", name = "roomRent", type = "String", remark = "租金"),
})

@Java110ExampleDoc(
        reqBody = "http://{ip}:{port}/app/room.queryLeasableRooms?communityId=2022081539020475&roomType=2020602&page=1&row=10",
        resBody = "{\"code\":0,\"msg\":\"成功\",\"records\":1,\"total\":1,\"data\":[{\"roomId\":\"752022082030880010\",\"roomName\":\"D-1-1001\",\"roomNum\":\"1001\",\"floorNum\":\"D\",\"unitNum\":\"1\",\"roomType\":\"2020602\",\"roomTypeName\":\"商铺\",\"roomArea\":\"11.00\",\"builtUpArea\":\"11.00\",\"roomRent\":\"3000.00\",\"state\":\"2008\",\"stateName\":\"空闲\",\"communityId\":\"2022081539020475\"}]}"
)
@Java110Cmd(serviceCode = "room.queryLeasableRooms")
public class QueryLeasableRoomsCmd extends Cmd {

    @Autowired
    private IRoomServiceDao roomServiceDaoImpl;

    @Autowired
    private IDataPrivilegeUnitV1InnerServiceSMO dataPrivilegeUnitV1InnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        super.validateProperty(context);
        Assert.hasKeyAndValue(reqJson, "communityId", "请求中未包含communityId信息");
        Assert.hasKeyAndValue(reqJson, "roomType", "请求中未包含roomType信息");
        super.validatePageInfo(reqJson);
        Assert.isInteger(reqJson.getString("page"), "page不是数字");
        Assert.isInteger(reqJson.getString("row"), "row不是数字");
        int row = reqJson.getIntValue("row");
        if (row < 1 || row > MAX_ROW) {
            throw new SMOException(ResponseConstant.RESULT_CODE_ERROR, "row 数量不能大于" + MAX_ROW);
        }
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Map<String, Object> info = new HashMap<>();
        info.put("communityId", reqJson.getString("communityId"));
        info.put("roomType", reqJson.getString("roomType"));
        info.put("roomNumLike", reqJson.getString("roomNumLike"));
        info.put("floorNum", reqJson.getString("floorNum"));
        info.put("unitNum", reqJson.getString("unitNum"));

        String roomState = reqJson.getString("roomState");
        if (!StringUtil.isEmpty(roomState)) {
            info.put("states", roomState.split(","));
        }

        DataPrivilegeStaffDto dataPrivilegeStaffDto = new DataPrivilegeStaffDto();
        dataPrivilegeStaffDto.setStaffId(context.getReqHeaders().get("user-id"));
        String[] unitIds = dataPrivilegeUnitV1InnerServiceSMOImpl.queryDataPrivilegeUnitsByStaff(dataPrivilegeStaffDto);
        if (unitIds != null && unitIds.length > 0) {
            info.put("unitIds", unitIds);
        }

        int row = reqJson.getIntValue("row");
        int total = roomServiceDaoImpl.queryLeasableRoomsCount(info);
        List<Map> rooms = new ArrayList<>();
        if (total > 0) {
            info.put("page", (reqJson.getIntValue("page") - 1) * row);
            info.put("row", row);
            rooms = roomServiceDaoImpl.getLeasableRooms(info);
        }

        context.setResponseEntity(ResultVo.createResponseEntity((int) Math.ceil((double) total / (double) row), total, rooms));
    }
}
