package com.java110.user.cmd.user;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.Environment;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.doc.annotation.*;
import com.java110.dto.file.FileDto;
import com.java110.dto.file.FileRelDto;
import com.java110.dto.org.OrgStaffRelDto;
import com.java110.dto.store.StoreUserDto;
import com.java110.dto.user.UserAttrDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.common.IFileInnerServiceSMO;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.store.IOrgStaffRelV1InnerServiceSMO;
import com.java110.intf.store.IStoreUserV1InnerServiceSMO;
import com.java110.intf.user.IOrgStaffRelInnerServiceSMO;
import com.java110.intf.user.IUserAttrV1InnerServiceSMO;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.file.FileRelPo;
import com.java110.po.org.OrgStaffRelPo;
import com.java110.po.store.StoreUserPo;
import com.java110.po.user.UserAttrPo;
import com.java110.po.user.UserPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 员工修改命令类
 * 
 * 该类负责处理员工信息的修改操作，包括基本信息、组织关系、岗位信息、身份证信息和头像图片等
 * 需要物业管理员账号登录，系统会根据管理员登录信息自动获取店铺ID
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023
 */
@Java110CmdDoc(title = "修改员工",
        description = "外部系统通过修改员工接口 修改员工，注意需要物业管理员账号登录，因为不需要传storeId 是根据管理员登录信息获取的",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/user.staff.modify",
        resource = "userDoc",
        author = "吴学文",
        serviceCode = "user.staff.modify",
        seq = 4
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "userId", length = 30, remark = "员工ID"),
        @Java110ParamDoc(name = "orgId", length = 30, remark = "组织ID"),
        @Java110ParamDoc(name = "orgName", length = 64, remark = "组织名称"),
        @Java110ParamDoc(name = "name", length = 64, remark = "名称"),
        @Java110ParamDoc(name = "sex", length = 64, remark = "性别 0女 1男"),
        @Java110ParamDoc(name = "email", length = 64, remark = "邮箱"),
        @Java110ParamDoc(name = "tel", length = 11, remark = "手机号"),
        @Java110ParamDoc(name = "address", length = 64, remark = "地址"),
        @Java110ParamDoc(name = "relCd", length = 64, remark = "岗位,普通员工 1000 部门经理 2000 部门副经理 3000 部门组长 4000 分公司总经理 5000 分公司副总经理 6000 总经理助理 7000 总公司总经理 8000 总公司副总经理 9000"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

@Java110ExampleDoc(
        reqBody = "{\"userId\":\"123123\",\"orgId\":\"102022091988250052\",\"orgName\":\"演示物业 / 件部\",\"username\":\"张三\",\"sex\":\"0\",\"email\":\"231@qq.com\",\"tel\":\"123\",\"address\":\"123\",\"relCd\":\"1000\",\"photo\":\"\",\"name\":\"张三\"}",
        resBody = "{'code':0,'msg':'成功'"
)

@Java110Cmd(serviceCode = "user.staff.modify")
public class UserStaffModifyCmd extends Cmd {

    @Autowired
    private IFileInnerServiceSMO fileInnerServiceSMOImpl;

    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    @Autowired
    private IOrgStaffRelInnerServiceSMO orgStaffRelInnerServiceSMOImpl;

    @Autowired
    private IOrgStaffRelV1InnerServiceSMO orgStaffRelV1InnerServiceSMOImpl;

    @Autowired
    private IStoreUserV1InnerServiceSMO storeUserV1InnerServiceSMOImpl;

    @Autowired
    private IUserAttrV1InnerServiceSMO userAttrV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的完整性和业务规则，包括：
     * 1. 必填字段检查
     * 2. 员工归属检查
     * 3. 手机号唯一性检查
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 开发环境检查
        Environment.isDevEnv();
        
        // 检查必填字段
        Assert.jsonObjectHaveKey(reqJson, "staffId", "请求参数中未包含员工 节点，请确认");
        Assert.jsonObjectHaveKey(reqJson, "name", "请求参数中未包含name 节点，请确认");
        Assert.jsonObjectHaveKey(reqJson, "tel", "请求参数中未包含tel 节点，请确认");
        
        // 验证员工归属：检查员工是否属于当前店铺
        StoreUserDto storeUserDto = new StoreUserDto();
        storeUserDto.setUserId(reqJson.getString("staffId"));
        storeUserDto.setStoreId(CmdContextUtils.getStoreId(context));
        List<StoreUserDto> storeUserDtos = storeUserV1InnerServiceSMOImpl.queryStoreUsers(storeUserDto);

        if (ListUtil.isNull(storeUserDtos)) {
            throw new CmdException("修改员工不是贵公司员工");
        }

        // 验证手机号唯一性：检查手机号是否已被其他员工使用
        UserDto userDto = new UserDto();
        userDto.setTel(reqJson.getString("tel"));
        userDto.setUserFlag("1"); // 用户标识
        userDto.setLevelCd(UserDto.LEVEL_CD_STAFF); // 员工级别
        List<UserDto> users = userInnerServiceSMOImpl.getUsers(userDto);
        if (!ListUtil.isNull(users)) {
            for (UserDto user : users) {
                // 如果手机号已被其他员工使用，则抛出异常
                if (!user.getUserId().equals(reqJson.getString("staffId"))) {
                    throw new IllegalArgumentException("员工手机号不能重复，请重新输入");
                }
            }
        }
    }

    /**
     * 执行员工修改命令
     * 
     * 主要处理逻辑：
     * 1. 处理员工头像图片的保存和更新
     * 2. 调用员工信息修改方法
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当业务处理失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 处理员工头像图片
        if (reqJson.containsKey("photo") && !StringUtils.isEmpty(reqJson.getString("photo"))) {
            
            // 如果图片内容是base64编码的长字符串，则保存为文件
            if (reqJson.getString("photo").length() > 200) {
                FileDto fileDto = new FileDto();
                fileDto.setFileId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_file_id));
                fileDto.setFileName(fileDto.getFileId());
                fileDto.setContext(reqJson.getString("photo")); // 图片内容
                fileDto.setSuffix("jpeg"); // 文件后缀
                fileDto.setCommunityId(reqJson.getString("communityId"));
                String fileName = fileInnerServiceSMOImpl.saveFile(fileDto);

                reqJson.put("photo", fileName); // 将文件名替换原来的base64内容
            }

            // 处理文件关联关系
            FileRelDto fileRelDto = new FileRelDto();
            fileRelDto.setRelTypeCd("12000"); // 关联类型代码
            fileRelDto.setObjId(reqJson.getString("userId")); // 关联对象ID
            List<FileRelDto> fileRelDtos = fileRelInnerServiceSMOImpl.queryFileRels(fileRelDto);
            
            if (ListUtil.isNull(fileRelDtos)) {
                // 如果没有关联记录，则创建新的文件关联
                JSONObject businessUnit = new JSONObject();
                businessUnit.put("fileRelId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_relId));
                businessUnit.put("relTypeCd", "12000");
                businessUnit.put("saveWay", "table"); // 存储方式
                businessUnit.put("objId", reqJson.getString("userId"));
                businessUnit.put("fileRealName", reqJson.getString("photo")); // 文件真实名称
                businessUnit.put("fileSaveName", reqJson.getString("photo")); // 文件保存名称
                FileRelPo fileRelPo = BeanConvertUtil.covertBean(businessUnit, FileRelPo.class);
                int flag = fileRelInnerServiceSMOImpl.saveFileRel(fileRelPo);
                if (flag < 1) {
                    throw new CmdException("保存图片异常");
                }
            } else {
                // 如果已有关联记录，则更新文件关联信息
                JSONObject businessUnit = new JSONObject();
                businessUnit.putAll(BeanConvertUtil.beanCovertMap(fileRelDtos.get(0)));
                businessUnit.put("fileRealName", reqJson.getString("photo"));
                businessUnit.put("fileSaveName", reqJson.getString("photo"));
                FileRelPo fileRelPo = BeanConvertUtil.covertBean(businessUnit, FileRelPo.class);
                int flag = fileRelInnerServiceSMOImpl.updateFileRel(fileRelPo);
                if (flag < 1) {
                    throw new CmdException("保存图片异常");
                }
            }
        }
        
        // 调用员工信息修改方法
        modifyStaff(reqJson);
    }

    /**
     * 修改员工基本信息
     * 
     * 更新员工的核心信息，包括：
     * 1. 用户基本信息
     * 2. 店铺用户关系
     * 3. 组织员工关系
     * 4. 身份证信息
     * 
     * @param paramObj 包含员工信息的参数对象
     */
    private void modifyStaff(JSONObject paramObj) {
        // 转换参数为用户持久化对象
        UserPo userPo = BeanConvertUtil.covertBean(paramObj, UserPo.class);
        userPo.setUserId(paramObj.getString("staffId"));
        
        // 邮箱格式验证
        if (paramObj.containsKey("email") && !StringUtil.isEmpty(paramObj.getString("email"))) {
            Assert.isEmail(paramObj, "email", "不是有效的邮箱格式");
        }
        
        // 更新用户基本信息
        int flag = userV1InnerServiceSMOImpl.updateUser(userPo);
        if (flag < 1) {
            throw new CmdException("保存用户异常");
        }
        
        // 查询用户信息用于后续处理
        UserDto userDto = new UserDto();
        userDto.setUserId(userPo.getUserId());
        List<UserDto> users = userInnerServiceSMOImpl.getUsers(userDto);
        
        // 更新员工身份证信息
        updateStaffIdCard(users.get(0), paramObj);

        // 更新店铺用户关系
        StoreUserDto storeUserDto = new StoreUserDto();
        storeUserDto.setUserId(userPo.getUserId());
        List<StoreUserDto> storeUserDtos = storeUserV1InnerServiceSMOImpl.queryStoreUsers(storeUserDto);

        if (ListUtil.isNull(storeUserDtos)) {
            return; // 如果没有店铺用户关系，则直接返回
        }

        StoreUserPo storeUserPo = new StoreUserPo();
        storeUserPo.setRelCd(paramObj.getString("relCd")); // 岗位代码
        storeUserPo.setStoreUserId(storeUserDtos.get(0).getStoreUserId());
        storeUserPo.setUserId(userPo.getUserId());

        flag = storeUserV1InnerServiceSMOImpl.updateStoreUser(storeUserPo);
        if (flag < 1) {
            throw new CmdException("保存员工 失败");
        }

        // 更新组织员工关系
        OrgStaffRelDto orgStaffRelDto = new OrgStaffRelDto();
        orgStaffRelDto.setStaffId(userPo.getUserId());
        List<OrgStaffRelDto> orgStaffRelDtos = orgStaffRelInnerServiceSMOImpl.queryOrgInfoByStaffIds(orgStaffRelDto);

        if (ListUtil.isNull(orgStaffRelDtos)) {
            return; // 如果没有组织员工关系，则直接返回
        }

        OrgStaffRelPo orgStaffRelPo = new OrgStaffRelPo();
        orgStaffRelPo.setRelCd(paramObj.getString("relCd")); // 岗位代码
        orgStaffRelPo.setRelId(orgStaffRelDtos.get(0).getRelId());
        orgStaffRelPo.setOrgId(paramObj.getString("orgId")); // 组织ID

        flag = orgStaffRelV1InnerServiceSMOImpl.updateOrgStaffRel(orgStaffRelPo);
        if (flag < 1) {
            throw new CmdException("保存员工 失败");
        }
    }

    /**
     * 更新员工身份证信息
     * 
     * 处理员工身份证信息的更新逻辑：
     * 1. 如果请求中不包含身份证信息，则直接返回
     * 2. 如果已有身份证记录，则先删除再新增
     * 3. 如果身份证信息为空，则不进行保存
     * 
     * @param userDto 用户数据传输对象
     * @param paramObj 包含身份证信息的参数对象
     */
    private void updateStaffIdCard(UserDto userDto, JSONObject paramObj) {
        // 检查是否包含身份证参数
        if (!paramObj.containsKey("idCard")) {
            return;
        }

        // 查询现有的身份证属性记录
        UserAttrDto userAttrDto = new UserAttrDto();
        userAttrDto.setUserId(userDto.getUserId());
        userAttrDto.setSpecCd(UserAttrDto.SPEC_ID_CARD); // 身份证特殊代码
        List<UserAttrDto> userAttrDtos = userAttrV1InnerServiceSMOImpl.queryUserAttrs(userAttrDto);

        // 如果存在身份证记录，则先删除
        if (!ListUtil.isNull(userAttrDtos)) {
            UserAttrPo userAttrPo = new UserAttrPo();
            userAttrPo.setAttrId(userAttrDtos.get(0).getAttrId());
            userAttrV1InnerServiceSMOImpl.deleteUserAttr(userAttrPo);
        }

        // 获取身份证号码
        String idCard = paramObj.getString("idCard");
        if (StringUtil.isEmpty(idCard)) {
            return; // 如果身份证为空，则不保存
        }

        // 保存新的身份证信息
        UserAttrPo userAttrPo = new UserAttrPo();
        userAttrPo.setAttrId(GenerateCodeFactory.getAttrId()); // 生成属性ID
        userAttrPo.setSpecCd(UserAttrDto.SPEC_ID_CARD); // 设置特殊代码为身份证
        userAttrPo.setUserId(userDto.getUserId()); // 设置用户ID
        userAttrPo.setValue(idCard); // 设置身份证号码

        userAttrV1InnerServiceSMOImpl.saveUserAttr(userAttrPo);
    }
}