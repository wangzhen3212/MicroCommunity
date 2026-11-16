package com.java110.store.bmo.contractPartya.impl;

import com.java110.core.annotation.Java110Transactional;
import com.java110.dto.contract.ContractDto;
import com.java110.dto.contract.ContractPartyaDto;
import com.java110.intf.store.IContractInnerServiceSMO;
import com.java110.intf.store.IContractPartyaInnerServiceSMO;
import com.java110.po.contract.ContractPartyaPo;
import com.java110.store.bmo.contractPartya.IDeleteContractPartyaBMO;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("deleteContractPartyaBMOImpl")
public class DeleteContractPartyaBMOImpl implements IDeleteContractPartyaBMO {

    @Autowired
    private IContractPartyaInnerServiceSMO contractPartyaInnerServiceSMOImpl;

    @Autowired
    private IContractInnerServiceSMO contractInnerServiceSMOImpl;

    /**
     * @param contractPartyaPo 数据
     * @return 订单服务能够接受的报文
     */
    @Java110Transactional
    public ResponseEntity<String> delete(ContractPartyaPo contractPartyaPo) {

        if (StringUtil.isEmpty(contractPartyaPo.getPartyaId())) {
            throw new IllegalArgumentException("未包含甲方主体信息");
        }

        // 被合同引用时禁止删除
        ContractPartyaDto contractPartyaDto = new ContractPartyaDto();
        contractPartyaDto.setPartyaId(contractPartyaPo.getPartyaId());
        contractPartyaDto.setStoreId(contractPartyaPo.getStoreId());
        List<ContractPartyaDto> partyaDtos = contractPartyaInnerServiceSMOImpl.queryContractPartyas(contractPartyaDto);
        if (partyaDtos == null || partyaDtos.isEmpty()) {
            throw new IllegalArgumentException("甲方主体不存在");
        }
        checkPartyaReferenced(partyaDtos.get(0));

        int flag = contractPartyaInnerServiceSMOImpl.deleteContractPartya(contractPartyaPo);

        if (flag > 0) {
            return ResultVo.createResponseEntity(ResultVo.CODE_OK, "保存成功");
        }

        return ResultVo.createResponseEntity(ResultVo.CODE_ERROR, "保存失败");
    }

    /**
     * 校验甲方主体是否被合同引用（新数据按 partya_id，存量数据按 party_a 名称）
     */
    private void checkPartyaReferenced(ContractPartyaDto contractPartyaDto) {
        ContractDto contractDto = new ContractDto();
        contractDto.setPartyaId(contractPartyaDto.getPartyaId());
        contractDto.setStoreId(contractPartyaDto.getStoreId());
        contractDto.setStatusCd("0");
        if (contractInnerServiceSMOImpl.queryContractsCount(contractDto) > 0) {
            throw new IllegalArgumentException("该甲方主体已被合同引用，不能删除");
        }

        if (StringUtil.isEmpty(contractPartyaDto.getPartyA())) {
            return;
        }
        ContractDto legacyContractDto = new ContractDto();
        legacyContractDto.setPartyA(contractPartyaDto.getPartyA());
        legacyContractDto.setStoreId(contractPartyaDto.getStoreId());
        legacyContractDto.setStatusCd("0");
        if (contractInnerServiceSMOImpl.queryContractsCount(legacyContractDto) > 0) {
            throw new IllegalArgumentException("该甲方主体已被合同引用，不能删除");
        }
    }

}
