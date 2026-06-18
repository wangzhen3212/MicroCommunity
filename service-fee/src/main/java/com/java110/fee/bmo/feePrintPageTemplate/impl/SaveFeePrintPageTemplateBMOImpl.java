package com.java110.fee.bmo.feePrintPageTemplate.impl;

import com.java110.core.annotation.Java110Transactional;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.fee.bmo.feePrintPageTemplate.ISaveFeePrintPageTemplateBMO;
import com.java110.intf.fee.IFeePrintPageTemplateV1InnerServiceSMO;
import com.java110.po.fee.FeePrintPageTemplatePo;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service("saveFeePrintPageTemplateBMOImpl")
public class SaveFeePrintPageTemplateBMOImpl implements ISaveFeePrintPageTemplateBMO {

    @Autowired
    private IFeePrintPageTemplateV1InnerServiceSMO feePrintPageTemplateV1InnerServiceSMOImpl;

    /**
     * 添加打印模板
     *
     * @param feePrintPageTemplatePo
     * @return 订单服务能够接受的报文
     */
    @Java110Transactional
    public ResponseEntity<String> save(FeePrintPageTemplatePo feePrintPageTemplatePo) {

        feePrintPageTemplatePo.setTemplateId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_templateId));
        int flag = feePrintPageTemplateV1InnerServiceSMOImpl.saveFeePrintPageTemplate(feePrintPageTemplatePo);

        if (flag > 0) {
            return ResultVo.createResponseEntity(ResultVo.CODE_OK, "保存成功");
        }

        return ResultVo.createResponseEntity(ResultVo.CODE_ERROR, "保存失败");
    }

}
