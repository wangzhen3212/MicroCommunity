package com.java110.user.bmo.userLogin.impl;

import com.java110.core.annotation.Java110Transactional;
import com.java110.dto.user.UserAttrDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.user.IUserAttrV1InnerServiceSMO;
import com.java110.intf.user.IUserLoginInnerServiceSMO;
import com.java110.po.user.UserAttrPo;
import com.java110.po.user.UserLoginPo;
import com.java110.user.bmo.userLogin.IDeleteUserLoginBMO;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service("deleteUserLoginBMOImpl")
public class DeleteUserLoginBMOImpl implements IDeleteUserLoginBMO {

    @Autowired
    private IUserLoginInnerServiceSMO userLoginInnerServiceSMOImpl;
    @Autowired
    private IUserAttrV1InnerServiceSMO userAttrV1InnerServiceSMOImpl;

    /**
     * @param userLoginPo 数据
     * @return 订单服务能够接受的报文
     */
    @Java110Transactional
    public ResponseEntity<String> delete(UserLoginPo userLoginPo) {

        int flag = userLoginInnerServiceSMOImpl.deleteUserLogin(userLoginPo);

        if (flag > 0) {
            return ResultVo.createResponseEntity(ResultVo.CODE_OK, "保存成功");
        }

        return ResultVo.createResponseEntity(ResultVo.CODE_ERROR, "保存失败");
    }
    /**
     * @param userDto 数据
     * @return 订单服务能够接受的报文
     */
    @Java110Transactional
    public ResponseEntity<String> deleteOpenId(UserDto userDto) {
        UserAttrPo userAttrPo = new UserAttrPo();
        userAttrPo.setUserId( userDto.getUserId() );
        userAttrPo.setSpecCd( UserAttrDto.SPEC_OPEN_ID );
        userAttrV1InnerServiceSMOImpl.deleteUserAttrInfo( userAttrPo );

        return ResultVo.createResponseEntity(ResultVo.CODE_OK, "清空openid成功");
    }

}
