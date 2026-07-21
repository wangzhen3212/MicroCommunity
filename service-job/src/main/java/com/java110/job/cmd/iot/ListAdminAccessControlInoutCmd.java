package com.java110.job.cmd.iot;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.user.UserDto;
import com.java110.intf.user.IStaffCommunityV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.job.adapt.hcIot.http.ISendIot;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

@Java110Cmd(serviceCode = "iot.listAdminAccessControlInout")
public class ListAdminAccessControlInoutCmd extends Cmd {

    private static final Logger logger = LoggerFactory.getLogger(ListAdminAccessControlInoutCmd.class);

    @Autowired
    private ISendIot sendIotImpl;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    @Autowired
    private IStaffCommunityV1InnerServiceSMO staffCommunityV1InnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        super.validatePageInfo(reqJson);
        super.validateAdmin(context);

        String userId = CmdContextUtils.getUserId(context);

        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);

        Assert.listOnlyOne(userDtos, "用户未登录");
        String storeId = CmdContextUtils.getStoreId(context);

        reqJson.put("adminUserId", userDtos.get(0).getUserId());
        reqJson.put("adminUserTel", userDtos.get(0).getTel());
        reqJson.put("adminStoreId", storeId);
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        String iotSwitch = MappingCache.getValue("IOT", "IOT_SWITCH");
        if (!"ON".equals(iotSwitch)) {
            logger.debug("IOT开关未开启，跳过执行iot.listAdminAccessControlInout");
            context.setResponseEntity(ResultVo.createResponseEntity(new ResultVo(ResultVo.CODE_OK, "未部署IOT系统")));
            return;
        }

        String staffId = CmdContextUtils.getUserId(context);

        List<String> communityIds = staffCommunityV1InnerServiceSMOImpl.queryStaffCommunityIds(staffId);

        if (!ListUtil.isNull(communityIds)) {
            String communityIdStr = String.join(", ", communityIds);
            reqJson.put("communityIdStr",communityIdStr);
        }
        ResultVo resultVo = sendIotImpl.postAdmin("listAdminAccessControlInoutBmoImpl", reqJson);

        if (resultVo.getCode() != ResultVo.CODE_OK) {
            throw new CmdException(resultVo.getMsg());
        }

        context.setResponseEntity(ResultVo.createResponseEntity(resultVo));
    }
}
