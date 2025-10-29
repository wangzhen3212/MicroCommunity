package com.java110.user.cmd.org;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.org.OrgCommunityDto;
import com.java110.intf.user.IOrgCommunityInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.org.ApiOrgCommunityDataVo;
import com.java110.vo.api.org.ApiOrgCommunityVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 组织社区关系查询命令类
 * 功能：处理组织与社区关联关系的列表查询请求，支持分页查询
 * 用途：通过服务码"org.listOrgCommunitys"对外提供组织社区关系查询接口
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "org.listOrgCommunitys")
public class ListOrgCommunitysCmd extends Cmd {

    /**
     * 组织社区关系内部服务接口
     * 用于查询组织与社区的关联关系数据
     */
    @Autowired
    private IOrgCommunityInnerServiceSMO orgCommunityInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 验证请求参数是否完整和有效
     *
     * @param event   命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 调用父类方法验证分页信息
        super.validatePageInfo(reqJson);
        
        // 验证商户ID参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "storeId", "必填，请填写商户ID");
        
        // 验证组织ID参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "orgId", "必填，请填写组织ID");
    }

    /**
     * 命令执行方法
     * 处理组织社区关系的查询请求，返回分页查询结果
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException    命令执行异常
     * @throws ParseException  数据解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求JSON对象转换为OrgCommunityDto数据传输对象
        OrgCommunityDto orgCommunityDto = BeanConvertUtil.covertBean(reqJson, OrgCommunityDto.class);

        // 查询组织社区关系的总记录数
        int count = orgCommunityInnerServiceSMOImpl.queryOrgCommunitysCount(orgCommunityDto);

        // 声明组织社区关系数据列表
        List<ApiOrgCommunityDataVo> orgCommunitys = null;

        // 根据查询结果数量判断是否进行数据查询
        if (count > 0) {
            // 查询组织社区关系列表数据，并转换为API视图对象列表
            orgCommunitys = BeanConvertUtil.covertBeanList(
                orgCommunityInnerServiceSMOImpl.queryOrgCommunitys(orgCommunityDto), 
                ApiOrgCommunityDataVo.class
            );
        } else {
            // 如果没有数据，返回空列表
            orgCommunitys = new ArrayList<>();
        }

        // 创建API响应对象
        ApiOrgCommunityVo apiOrgVo = new ApiOrgCommunityVo();

        // 设置总记录数
        apiOrgVo.setTotal(count);
        
        // 计算总页数：总记录数 / 每页记录数，向上取整
        apiOrgVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        
        // 设置组织社区关系数据列表
        apiOrgVo.setOrgCommunitys(orgCommunitys);

        // 创建HTTP响应实体，将API响应对象转换为JSON字符串
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(
            JSONObject.toJSONString(apiOrgVo), 
            HttpStatus.OK
        );

        // 将响应实体设置到上下文中
        context.setResponseEntity(responseEntity);
    }
}