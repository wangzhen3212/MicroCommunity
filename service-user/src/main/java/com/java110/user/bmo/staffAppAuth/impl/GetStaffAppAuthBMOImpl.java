package com.java110.user.bmo.staffAppAuth.impl;

import com.java110.dto.user.StaffAppAuthDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.user.IStaffAppAuthInnerServiceSMO;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.user.bmo.staffAppAuth.IGetStaffAppAuthBMO;
import com.java110.utils.util.Assert;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("getStaffAppAuthBMOImpl")
public class GetStaffAppAuthBMOImpl implements IGetStaffAppAuthBMO {

    @Autowired
    private IStaffAppAuthInnerServiceSMO staffAppAuthInnerServiceSMOImpl;

    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;

    /**
     * @param staffAppAuthDto
     * @return 订单服务能够接受的报文
     */
    public ResponseEntity<String> get(StaffAppAuthDto staffAppAuthDto) {
        UserDto userDto = new UserDto();
        userDto.setUserId(staffAppAuthDto.getStaffId());
        List<UserDto> userDtos = userInnerServiceSMOImpl.getUsers(userDto);

        Assert.listOnlyOne(userDtos, "用户不存在");
        staffAppAuthDto.setStaffName(userDtos.get(0).getName());

        List<StaffAppAuthDto> staffAppAuthDtos = staffAppAuthInnerServiceSMOImpl.queryStaffAppAuths(staffAppAuthDto);

        if (staffAppAuthDtos == null || staffAppAuthDtos.size() < 1) {
            staffAppAuthDto.setStateName("未认证");
            staffAppAuthDto.setState("1001");
            staffAppAuthDto.setOpenId("-");
            staffAppAuthDto.setAppType("-");
            staffAppAuthDto.setOpenName("-");
        } else {
            staffAppAuthDto.setStateName("已认证");
            staffAppAuthDto.setState("2002");
            staffAppAuthDto.setOpenId(staffAppAuthDtos.get(0).getOpenId());
            staffAppAuthDto.setAppType(staffAppAuthDtos.get(0).getAppType());
            staffAppAuthDto.setOpenName(staffAppAuthDtos.get(0).getOpenName());
            staffAppAuthDto.setCreateTime(staffAppAuthDtos.get(0).getCreateTime());
        }
        return ResultVo.createResponseEntity(staffAppAuthDto);
    }

    /**
     * 查询商户下所有员工的微信认证信息
     * <p>
     * 逻辑：
     * 1. 通过 s_store_user 表查询商户下所有员工（不受组织层级影响）
     * 2. 查询 staff_app_auth 获取该商户下的认证记录
     * 3. 组装结果，未认证员工标记为"未认证"
     *
     * @param staffAppAuthDto 包含 storeId, page, row
     * @return
     */
    public ResponseEntity<String> getList(StaffAppAuthDto staffAppAuthDto) {
        String storeId = staffAppAuthDto.getStoreId();
        int page = staffAppAuthDto.getPage();
        int row = staffAppAuthDto.getRow();

        // 1. 通过 s_store_user 表查询商户下所有员工（不分页）
        UserDto userDto = new UserDto();
        userDto.setStoreId(storeId);
        List<UserDto> staffs = userInnerServiceSMOImpl.getStaffs(userDto);

        if (ListUtil.isNull(staffs)) {
            return ResultVo.createResponseEntity(new ArrayList<>());
        }

        // 2. 查询该商户下所有认证记录
        StaffAppAuthDto queryAuth = new StaffAppAuthDto();
        queryAuth.setStoreId(storeId);
        List<StaffAppAuthDto> authDtos = staffAppAuthInnerServiceSMOImpl.queryStaffAppAuths(queryAuth);

        // 3. 构建 staffId -> auth 映射
        Map<String, StaffAppAuthDto> authMap = new HashMap<>();
        if (authDtos != null) {
            for (StaffAppAuthDto auth : authDtos) {
                authMap.put(auth.getStaffId(), auth);
            }
        }

        // 4. 构建结果列表
        List<StaffAppAuthDto> resultList = new ArrayList<>();
        for (UserDto staff : staffs) {
            StaffAppAuthDto dto = new StaffAppAuthDto();
            dto.setStaffId(staff.getUserId());
            dto.setStaffName(staff.getName());

            StaffAppAuthDto auth = authMap.get(staff.getUserId());
            if (auth != null) {
                dto.setStateName("已认证");
                dto.setState("2002");
                dto.setOpenId(auth.getOpenId());
                dto.setAppType(auth.getAppType());
                dto.setOpenName(auth.getOpenName());
                dto.setCreateTime(auth.getCreateTime());
                dto.setAuId(auth.getAuId());
            } else {
                dto.setStateName("未认证");
                dto.setState("1001");
                dto.setOpenId("-");
                dto.setAppType("-");
                dto.setOpenName("-");
            }
            resultList.add(dto);
        }

        // 5. 手动分页
        int total = resultList.size();
        int fromIndex = (page - 1) * row;
        if (fromIndex >= total) {
            resultList = new ArrayList<>();
        } else {
            int toIndex = Math.min(fromIndex + row, total);
            resultList = new ArrayList<>(resultList.subList(fromIndex, toIndex));
        }

        ResultVo resultVo = new ResultVo((int) Math.ceil((double) total / (double) row), total, resultList);
        return new ResponseEntity<>(resultVo.toString(), HttpStatus.OK);
    }

}
