package com.java110.fee.bmo.feePrintPageTemplate;

import com.java110.po.fee.FeePrintPageTemplatePo;
import org.springframework.http.ResponseEntity;

public interface ISaveFeePrintPageTemplateBMO {

    /**
     * 添加打印模板
     * add by wuxw
     *
     * @param feePrintPageTemplatePo
     * @return
     */
    ResponseEntity<String> save(FeePrintPageTemplatePo feePrintPageTemplatePo);

}
