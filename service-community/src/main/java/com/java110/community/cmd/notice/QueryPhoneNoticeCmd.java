/**
 * 查询用户端公告命令类
 * 
 * 该命令类用于处理手机端公告查询请求，支持分页查询小区公告信息
 * 主要功能包括参数验证、公告数据查询、数据转换和响应返回
 * 服务编码：notice.queryPhoneNotice
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.community.cmd.notice;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.floor.FloorDto;
import com.java110.dto.notice.NoticeDto;
import com.java110.dto.room.RoomDto;
import com.java110.dto.unit.FloorAndUnitDto;
import com.java110.intf.community.*;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
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
 * 查询用户端公告 功能
 * 请求地址为/app/login.pcUserLogin
 */
@Java110Cmd(serviceCode = "notice.queryPhoneNotice")
public class QueryPhoneNoticeCmd extends Cmd {

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
     * 请求参数验证方法
     * 
     * 验证请求参数是否合法，包括分页参数和必要字段验证
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 调用父类方法验证分页信息
        super.validatePageInfo(reqJson);
        // 验证请求中必须包含小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID");
    }

    /**
     * 执行公告查询命令
     * 
     * 处理手机端公告查询业务逻辑，包括：
     * 1. 参数转换和设置查询条件
     * 2. 查询公告总数和列表数据
     * 3. 数据格式转换和分页信息计算
     * 4. 构建并返回响应结果
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为NoticeDto对象
        NoticeDto noticeDto = BeanConvertUtil.covertBean(reqJson, NoticeDto.class);

        // 设置查询类型为业主公告类型
        noticeDto.setNoticeTypeCd(NoticeDto.NOTICE_TYPE_OWNER);
        // 查询符合条件的公告总数
        int count = noticeInnerServiceSMOImpl.queryNoticesCount(noticeDto);

        List<ApiNoticeDataVo> notices = null;

        // 根据查询结果数量处理公告列表
        if (count > 0) {
            // 查询公告列表并转换为前端VO对象
            notices = BeanConvertUtil.covertBeanList(noticeInnerServiceSMOImpl.queryNotices(noticeDto), ApiNoticeDataVo.class);
        } else {
            // 如果没有数据，返回空列表
            notices = new ArrayList<>();
        }

        // 创建响应VO对象
        ApiNoticeVo apiNoticeVo = new ApiNoticeVo();

        // 设置总记录数
        apiNoticeVo.setTotal(count);
        // 计算总页数：总记录数 / 每页记录数，向上取整
        apiNoticeVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        // 设置公告数据列表
        apiNoticeVo.setNotices(notices);

        // 构建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiNoticeVo), HttpStatus.OK);

        // 将响应设置到数据流上下文中
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}