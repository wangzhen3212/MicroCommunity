package com.java110.store.bmo.contractPartya.impl;

import com.java110.core.annotation.Java110Transactional;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.store.IContractPartyaInnerServiceSMO;
import com.java110.po.contract.ContractPartyaPo;
import com.java110.store.bmo.contractPartya.ISaveContractPartyaBMO;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service("saveContractPartyaBMOImpl")
public class SaveContractPartyaBMOImpl implements ISaveContractPartyaBMO {

    @Autowired
    private IContractPartyaInnerServiceSMO contractPartyaInnerServiceSMOImpl;

    /**
     * 添加小区信息
     *
     * @param contractPartyaPo
     * @return 订单服务能够接受的报文
     */
    @Java110Transactional
    public ResponseEntity<String> save(ContractPartyaPo contractPartyaPo) {

        contractPartyaPo.setPartyaId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_partyaId));
        // 默认主体唯一：新增默认主体时先清除其它默认
        if ("1".equals(contractPartyaPo.getDefaultFlag())) {
            ContractPartyaPo clearPo = new ContractPartyaPo();
            clearPo.setPartyaId(contractPartyaPo.getPartyaId());
            clearPo.setStoreId(contractPartyaPo.getStoreId());
            clearPo.setCommunityId(contractPartyaPo.getCommunityId());
            contractPartyaInnerServiceSMOImpl.clearDefaultContractPartya(clearPo);
        }
        int flag = contractPartyaInnerServiceSMOImpl.saveContractPartya(contractPartyaPo);

        if (flag > 0) {
            return ResultVo.createResponseEntity(ResultVo.CODE_OK, "保存成功");
        }

        return ResultVo.createResponseEntity(ResultVo.CODE_ERROR, "保存失败");
    }

}
