package com.java110.fee.bmo.feePrintPageTemplate.impl;

import com.java110.core.annotation.Java110Transactional;
import com.java110.fee.bmo.feePrintPageTemplate.IUpdateFeePrintPageTemplateBMO;
import com.java110.intf.fee.IFeePrintPageTemplateV1InnerServiceSMO;
import com.java110.po.fee.FeePrintPageTemplatePo;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service("updateFeePrintPageTemplateBMOImpl")
public class UpdateFeePrintPageTemplateBMOImpl implements IUpdateFeePrintPageTemplateBMO {

    @Autowired
    private IFeePrintPageTemplateV1InnerServiceSMO feePrintPageTemplateV1InnerServiceSMOImpl;

    /**
     * 修改打印模板
     *
     * @param feePrintPageTemplatePo
     * @return 订单服务能够接受的报文
     */
    @Java110Transactional
    public ResponseEntity<String> update(FeePrintPageTemplatePo feePrintPageTemplatePo) {

        int flag = feePrintPageTemplateV1InnerServiceSMOImpl.updateFeePrintPageTemplate(feePrintPageTemplatePo);

        if (flag > 0) {
            return ResultVo.createResponseEntity(ResultVo.CODE_OK, "修改成功");
        }

        return ResultVo.createResponseEntity(ResultVo.CODE_ERROR, "修改失败");
    }

}
