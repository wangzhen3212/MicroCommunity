/**
 * 巡检路线点位关系更新命令类
 * 
 * 该类负责处理更新巡检路线与巡检点关联关系的业务逻辑，包括参数验证和数据更新操作。
 * 通过注解标识服务代码，与前端请求进行映射，实现巡检路线点位关系的修改功能。
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.community.cmd.inspectionRoute;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.community.IInspectionRoutePointRelV1InnerServiceSMO;
import com.java110.po.inspection.InspectionRoutePointRelPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 更新巡检路线点位关系命令类
 * 
 * 该类继承自Cmd基类，实现了巡检路线与巡检点关联关系的更新功能。
 * 通过服务码"inspectionRoute.updateInspectionRoutePointRel"与前端请求进行绑定。
 */
@Java110Cmd(serviceCode = "inspectionRoute.updateInspectionRoutePointRel")
public class UpdateInspectionRoutePointRelCmd extends Cmd {

    /**
     * 巡检路线点位关系服务接口
     * 
     * 通过Spring自动注入，提供巡检路线点位关系的数据操作能力
     */
    @Autowired
    private IInspectionRoutePointRelV1InnerServiceSMO inspectionRoutePointRelV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证前端传入的JSON参数是否包含必要的字段，确保业务操作的完整性。
     * 主要验证巡检路线点位关系ID、巡检路线ID和小区ID等关键参数。
     *
     * @param event 命令事件对象，包含请求的原始信息
     * @param context 命令数据流上下文，提供请求处理过程中的数据传递
     * @param reqJson 前端传入的JSON格式请求参数
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证巡检路线点位关系ID是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "irpRelId", "路线巡检点ID不能为空");
        // 验证巡检路线ID是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "inspectionRouteId", "路线ID不能为空");
        // 验证小区ID是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "communityId", "小区ID不能为空");
    }

    /**
     * 执行命令方法
     * 
     * 将前端传入的JSON参数转换为实体对象，并调用服务层接口更新巡检路线点位关系。
     * 根据更新结果判断操作是否成功，失败时抛出异常。
     *
     * @param event 命令事件对象，包含请求的原始信息
     * @param context 命令数据流上下文，提供请求处理过程中的数据传递
     * @param reqJson 前端传入的JSON格式请求参数
     * @throws CmdException 当数据更新失败时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将JSON参数转换为巡检路线点位关系实体对象
        InspectionRoutePointRelPo inspectionRoutePointRelPo = BeanConvertUtil.covertBean(reqJson, InspectionRoutePointRelPo.class);
        
        // 调用服务层接口更新巡检路线点位关系，返回影响的行数
        int flag = inspectionRoutePointRelV1InnerServiceSMOImpl.updateInspectionRoutePointRel(inspectionRoutePointRelPo);
        
        // 判断更新操作是否成功，影响行数小于1表示更新失败
        if (flag < 1) {
            throw new CmdException("删除巡检路线失败");
        }
    }
}