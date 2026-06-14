package com.java110.community.cmd.notice;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.floor.FloorDto;
import com.java110.dto.room.RoomDto;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.notice.NoticeDto;
import com.java110.dto.unit.FloorAndUnitDto;
import com.java110.intf.community.*;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.api.notice.ApiNoticeDataVo;
import com.java110.vo.api.notice.ApiNoticeVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * 查询公告 功能
 * 请求地址为/app/login.pcUserLogin
 */

@Java110CmdDoc(title = "查询公告",
        description = "提供外系统查询物业系统公告",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/notice.listNotices",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "notice.listNotices",
        seq = 21
)
//入参要求
@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "page",type = "int",length = 11, remark = "页数"),
        @Java110ParamDoc(name = "row", type = "int",length = 30, remark = "行数"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "data", type = "Object", remark = "有效数据"),
                @Java110ParamDoc(parentNodeName = "data",name = "userId", type = "String", remark = "用户ID"),
                @Java110ParamDoc(parentNodeName = "data",name = "token", type = "String", remark = "临时票据"),
                @Java110ParamDoc(parentNodeName = "token",name = "abc", type = "String", remark = "临时票据"),

        }
)

@Java110ExampleDoc(
        reqBody="http://localhost:3000/app/notice.listNotices?title=&noticeTypeCd=&state=&noticeId=&startTime=&endTime=&page=1&row=10&communityId=2022110264250009",
        resBody="{\"notices\":[],\"page\":0,\"records\":0,\"rows\":0,\"total\":0}"
)

/**
 * 公告查询命令类
 * 负责处理公告列表查询请求，支持按不同对象类型（小区、楼栋、单元、房屋等）查询公告信息
 * 提供分页查询功能，并返回格式化后的公告数据
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "notice.listNotices")
public class ListNoticesCmd extends Cmd {

    @Autowired
    private INoticeInnerServiceSMO noticeInnerServiceSMOImpl;

    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    @Autowired
    private IFloorInnerServiceSMO floorInnerServiceSMOImpl;

    @Autowired
    private IUnitInnerServiceSMO unitInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数中的分页信息是否合法
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        super.validatePageInfo(reqJson);
    }

    /**
     * 执行公告查询命令
     * 处理公告查询业务逻辑，包括参数处理、数据查询、结果封装等
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行过程中出现错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 移除用户ID参数，避免影响查询
        reqJson.remove("userId");
        
        // 将请求参数转换为NoticeDto对象
        NoticeDto noticeDto = BeanConvertUtil.covertBean(reqJson, NoticeDto.class);
        
        // 处理H5客户端的特殊时间查询条件
        if (!StringUtil.isEmpty("clientType") && "H5".equals(reqJson.get("clientType"))) {
            Date day = new Date();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            // 设置查询时间为当前时间，实现实时查询
            noticeDto.setStartTime(df.format(day));
            noticeDto.setEndTime(df.format(day));
        }

        // 处理多公告类型查询条件
        if (!StringUtil.isEmpty(noticeDto.getNoticeTypeCd()) && noticeDto.getNoticeTypeCd().contains(",")) {
            // 将逗号分隔的公告类型转换为数组
            noticeDto.setNoticeTypeCds(noticeDto.getNoticeTypeCd().split(","));
            noticeDto.setNoticeTypeCd(""); // 清空原字段，避免重复查询
        }

        // 查询公告总数
        int count = noticeInnerServiceSMOImpl.queryNoticesCount(noticeDto);

        List<ApiNoticeDataVo> notices = null;

        if (count > 0) {
            // 查询公告列表数据并转换为VO对象
            notices = BeanConvertUtil.covertBeanList(noticeInnerServiceSMOImpl.queryNotices(noticeDto), ApiNoticeDataVo.class);
            // 刷新公告关联对象信息
            refreshNotice(notices);
        } else {
            notices = new ArrayList<>();
        }

        // 构建返回结果VO对象
        ApiNoticeVo apiNoticeVo = new ApiNoticeVo();
        apiNoticeVo.setTotal(count); // 设置总记录数
        apiNoticeVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row"))); // 计算总页数
        apiNoticeVo.setNotices(notices); // 设置公告列表数据

        // 构建HTTP响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiNoticeVo), HttpStatus.OK);
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 刷新公告关联对象信息
     * 根据公告的对象类型，分别刷新对应的对象名称信息
     *
     * @param notices 公告数据列表
     */
    private void refreshNotice(List<ApiNoticeDataVo> notices) {
        // 刷新微信用户信息
        refreshWechats(notices);
        // 批量处理小区信息
        refreshCommunitys(notices);
        // 刷新楼栋信息
        refreshFloors(notices);
        // 批量处理单元信息
        refreshUnits(notices);
        // 批量处理房屋信息
        refreshRooms(notices);
    }

    /**
     * 刷新微信用户信息
     * 处理面向所有用户的公告，设置对象名称为"关注用户"
     *
     * @param notices 公告数据列表
     */
    private void refreshWechats(List<ApiNoticeDataVo> notices) {
        for (ApiNoticeDataVo noticeDto : notices) {
            // 判断是否为面向所有用户的公告
            if (NoticeDto.OBJ_TYPE_ALL.equals(noticeDto.getObjType())) {
                noticeDto.setObjName("关注用户"); // 设置对象名称
            }
        }
    }

    /**
     * 刷新小区信息
     * 根据小区ID查询小区名称并设置到公告对象中
     *
     * @param notices 公告数据列表
     */
    private void refreshCommunitys(List<ApiNoticeDataVo> notices) {
        List<String> communityIds = new ArrayList<String>();
        List<ApiNoticeDataVo> tmpNoticeDtos = new ArrayList<>();
        
        // 收集需要查询的小区ID
        for (ApiNoticeDataVo noticeDto : notices) {
            if (NoticeDto.OBJ_TYPE_COMMUNITY.equals(noticeDto.getObjType())) {
                communityIds.add(noticeDto.getObjId());
                tmpNoticeDtos.add(noticeDto);
            }
        }

        if (communityIds.size() < 1) {
            return; // 没有小区类型的公告，直接返回
        }
        
        String[] tmpCommunityIds = communityIds.toArray(new String[communityIds.size()]);

        CommunityDto communityDto = new CommunityDto();
        communityDto.setCommunityIds(tmpCommunityIds);
        // 根据小区ID批量查询小区信息
        List<CommunityDto> communityDtos = communityInnerServiceSMOImpl.queryCommunitys(communityDto);

        // 设置小区名称到公告对象中
        for (ApiNoticeDataVo noticeDto : tmpNoticeDtos) {
            for (CommunityDto tmpCommunityDto : communityDtos) {
                if (noticeDto.getObjId().equals(tmpCommunityDto.getCommunityId())) {
                    noticeDto.setObjName(tmpCommunityDto.getName());
                }
            }
        }
    }

    /**
     * 刷新楼栋信息
     * 根据楼栋ID查询楼栋信息并设置楼栋名称
     *
     * @param notices 公告数据列表
     */
    private void refreshFloors(List<ApiNoticeDataVo> notices) {
        List<String> floorIds = new ArrayList<String>();
        List<ApiNoticeDataVo> tmpNoticeDtos = new ArrayList<>();
        
        // 收集需要查询的楼栋ID
        for (ApiNoticeDataVo noticeDto : notices) {
            if (NoticeDto.OBJ_TYPE_UNIT.equals(noticeDto.getObjType())) {
                floorIds.add(noticeDto.getObjId());
                tmpNoticeDtos.add(noticeDto);
            }
        }

        if (floorIds.size() < 1) {
            return; // 没有楼栋类型的公告，直接返回
        }
        
        String[] tmpFloorIds = floorIds.toArray(new String[floorIds.size()]);

        FloorDto floorDto = new FloorDto();
        floorDto.setFloorIds(tmpFloorIds);
        // 根据楼栋ID批量查询楼栋信息
        List<FloorDto> floorDtos = floorInnerServiceSMOImpl.queryFloors(floorDto);

        // 设置楼栋名称到公告对象中
        for (ApiNoticeDataVo noticeDto : tmpNoticeDtos) {
            for (FloorDto tmpFloorDto : floorDtos) {
                if (noticeDto.getObjId().equals(tmpFloorDto.getFloorId())) {
                    noticeDto.setObjName(tmpFloorDto.getFloorNum() + "栋");
                }
            }
        }
    }

    /**
     * 刷新单元信息
     * 根据单元ID查询单元信息并设置单元名称（包含楼栋和单元信息）
     *
     * @param notices 公告数据列表
     */
    private void refreshUnits(List<ApiNoticeDataVo> notices) {
        List<String> unitIds = new ArrayList<String>();
        List<ApiNoticeDataVo> tmpNoticeDtos = new ArrayList<>();
        
        // 收集需要查询的单元ID
        for (ApiNoticeDataVo noticeDto : notices) {
            if (NoticeDto.OBJ_TYPE_UNIT.equals(noticeDto.getObjType())) {
                unitIds.add(noticeDto.getObjId());
                tmpNoticeDtos.add(noticeDto);
            }
        }

        if (unitIds.size() < 1) {
            return; // 没有单元类型的公告，直接返回
        }
        
        String[] tmpUnitIds = unitIds.toArray(new String[unitIds.size()]);

        FloorAndUnitDto floorAndUnitDto = new FloorAndUnitDto();
        floorAndUnitDto.setUnitIds(tmpUnitIds);
        // 根据单元ID批量查询单元信息（包含楼栋信息）
        List<FloorAndUnitDto> unitDtos = unitInnerServiceSMOImpl.getFloorAndUnitInfo(floorAndUnitDto);

        // 设置单元名称到公告对象中（格式：X栋X单元）
        for (ApiNoticeDataVo noticeDto : tmpNoticeDtos) {
            for (FloorAndUnitDto tmpUnitDto : unitDtos) {
                if (noticeDto.getObjId().equals(tmpUnitDto.getUnitId())) {
                    noticeDto.setObjName(tmpUnitDto.getFloorNum() + "栋" + tmpUnitDto.getUnitNum() + "单元");
                }
            }
        }
    }

    /**
     * 刷新房屋信息
     * 根据房屋ID查询房屋信息并设置房屋名称（包含完整地址信息）
     *
     * @param notices 公告数据列表
     */
    private void refreshRooms(List<ApiNoticeDataVo> notices) {
        List<String> roomIds = new ArrayList<String>();
        List<ApiNoticeDataVo> tmpNoticeDtos = new ArrayList<>();
        
        // 收集需要查询的房屋ID
        for (ApiNoticeDataVo noticeDto : notices) {
            if (NoticeDto.OBJ_TYPE_ROOM.equals(noticeDto.getObjType())) {
                if ("3000".equals(noticeDto.getObjType())) {
                    roomIds.add(noticeDto.getObjId());
                    tmpNoticeDtos.add(noticeDto);
                }
            }
            
            if (roomIds.size() < 1) {
                return; // 没有房屋类型的公告，直接返回
            }
            
            String[] tmpRoomIds = roomIds.toArray(new String[roomIds.size()]);

            RoomDto roomDto = new RoomDto();
            roomDto.setRoomIds(tmpRoomIds);
            roomDto.setCommunityId(notices.get(0).getCommunityId()); // 使用第一个公告的小区ID
            // 根据房屋ID批量查询房屋信息
            List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);

            // 设置房屋名称到公告对象中（格式：X栋X单元X室）
            for (ApiNoticeDataVo tmpNoticeDto : tmpNoticeDtos) {
                for (RoomDto tmpRoomDto : roomDtos) {
                    if (tmpNoticeDto.getObjId().equals(tmpRoomDto.getRoomId())) {
                        tmpNoticeDto.setObjName(tmpRoomDto.getFloorNum() + "栋" + tmpRoomDto.getUnitNum() + "单元" + tmpRoomDto.getRoomNum() + "室");
                    }
                }
            }
        }
    }
}