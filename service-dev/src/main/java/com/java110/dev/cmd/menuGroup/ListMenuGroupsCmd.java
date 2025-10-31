package com.java110.dev.cmd.menuGroup;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.menu.MenuGroupDto;
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.menuGroup.ApiMenuGroupDataVo;
import com.java110.vo.api.menuGroup.ApiMenuGroupVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单组列表查询命令类
 * 负责处理菜单组分页查询请求
 * 
 * @Java110Cmd 注解指定服务代码为 menuGroup.listMenuGroups
 */
@Java110Cmd(serviceCode = "menuGroup.listMenuGroups")
public class ListMenuGroupsCmd extends Cmd {

    /**
     * 菜单内部服务接口，用于查询菜单组数据
     */
    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 主要验证分页参数
     * 
     * @param event 命令事件
     * @param context 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 调用父类方法验证分页信息
        super.validatePageInfo(reqJson);
    }

    /**
     * 执行命令方法
     * 查询菜单组列表并返回分页结果
     * 
     * @param event 命令事件
     * @param context 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为MenuGroupDto对象
        MenuGroupDto menuGroupDto = BeanConvertUtil.covertBean(reqJson, MenuGroupDto.class);

        // 查询菜单组总数
        int count = menuInnerServiceSMOImpl.queryMenuGroupsCount(menuGroupDto);

        // 菜单组数据列表
        List<ApiMenuGroupDataVo> menuGroups = null;

        // 如果总数大于0，则查询具体数据
        if (count > 0) {
            // 查询菜单组列表并转换为VO对象
            menuGroups = BeanConvertUtil.covertBeanList(
                menuInnerServiceSMOImpl.queryMenuGroups(menuGroupDto), 
                ApiMenuGroupDataVo.class
            );
        } else {
            // 如果没有数据，返回空列表
            menuGroups = new ArrayList<>();
        }

        // 创建返回VO对象
        ApiMenuGroupVo apiMenuGroupVo = new ApiMenuGroupVo();

        // 设置总记录数
        apiMenuGroupVo.setTotal(count);
        // 计算总页数：总记录数/每页记录数，向上取整
        apiMenuGroupVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        // 设置菜单组数据
        apiMenuGroupVo.setMenuGroups(menuGroups);

        // 创建响应实体，将VO对象转换为JSON字符串
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(
            JSONObject.toJSONString(apiMenuGroupVo), 
            HttpStatus.OK
        );

        // 设置响应实体到上下文
        context.setResponseEntity(responseEntity);
    }
}