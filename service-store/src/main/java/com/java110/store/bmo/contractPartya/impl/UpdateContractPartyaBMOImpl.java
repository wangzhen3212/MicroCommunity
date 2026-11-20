package com.java110.store.bmo.contractPartya.impl;

import com.java110.core.annotation.Java110Transactional;
import com.java110.intf.store.IContractPartyaInnerServiceSMO;
import com.java110.po.contract.ContractPartyaPo;
import com.java110.store.bmo.contractPartya.IUpdateContractPartyaBMO;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service("updateContractPartyaBMOImpl")
public class UpdateContractPartyaBMOImpl implements IUpdateContractPartyaBMO {

    @Autowired
    private IContractPartyaInnerServiceSMO contractPartyaInnerServiceSMOImpl;

    /**
     * @param contractPartyaPo
     * @return 订单服务能够接受的报文
     */
    @Java110Transactional
    public ResponseEntity<String> update(ContractPartyaPo contractPartyaPo) {

        // 默认主体唯一：设置为默认时先清除其它默认
        if ("1".equals(contractPartyaPo.getDefaultFlag())) {
            ContractPartyaPo clearPo = new ContractPartyaPo();
            clearPo.setPartyaId(contractPartyaPo.getPartyaId());
            clearPo.setStoreId(contractPartyaPo.getStoreId());
            clearPo.setCommunityId(contractPartyaPo.getCommunityId());
            contractPartyaInnerServiceSMOImpl.clearDefaultContractPartya(clearPo);
        }

        int flag = contractPartyaInnerServiceSMOImpl.updateContractPartya(contractPartyaPo);

        if (flag > 0) {
            return ResultVo.createResponseEntity(ResultVo.CODE_OK, "保存成功");
        }

        return ResultVo.createResponseEntity(ResultVo.CODE_ERROR, "保存失败");
    }

}
