<template>
  <create-fee-layout :title="contractCreateFeeInfo.contractCode" :empty="contractCreateFeeInfo.contracts.length === 0"
    :empty-text="$t('contractCreateFee.emptyContract')" storage-key="contractCreateFeeTreeWidth">
    <div slot="tree" class="contract-tree-panel">
      <el-card class="contract-list-card">
        <div class="contract-search-input">
          <el-input :placeholder="$t('contractCreateFee.enterContractCode')"
            v-model="contractCreateFeeInfo.conditions.contractCode" clearable
            @keyup.enter.native="_queryContract"></el-input>
        </div>
        <div class="contract-list-wrapper">
          <ul class="contract-list">
            <li v-for="(item, index) in contractCreateFeeInfo.contracts" :key="index" class="contract-item"
              :class="{ 'selected': contractCreateFeeInfo.contractId === item.contractId }" @click="switchContract(item)">
              {{ item.contractCode }}({{ item.contractName }})--{{ item.bContacts }}
            </li>
          </ul>
        </div>
      </el-card>
    </div>

    <div slot="content">
      <div class="filter-container">
        <el-row :gutter="10" >
          <el-col :span="4">
            <el-input v-model="contractCreateFeeInfo.conditions.contractNameLike" 
              :placeholder="$t('contractCreateFee.enterContractName')" clearable
              @keyup.enter.native="_queryContract"></el-input>
          </el-col>
          <el-col :span="4">
            <el-input v-model="contractCreateFeeInfo.conditions.bContacts" 
              :placeholder="$t('contractCreateFee.enterPartyB')" clearable
              @keyup.enter.native="_queryContract"></el-input>
          </el-col>
          <el-col :span="4">
            <el-input v-model="contractCreateFeeInfo.conditions.ownerName" 
              :placeholder="$t('contractCreateFee.enterOwnerName')" clearable
              @keyup.enter.native="_queryContract"></el-input>
          </el-col>
          <el-col :span="4">
            <el-input v-model="contractCreateFeeInfo.conditions.roomNum"
              :placeholder="$t('contractCreateFee.enterRoomNum')" clearable
              @keyup.enter.native="_queryContract"></el-input>
          </el-col>
          <el-col :span="4">
            <el-input v-model="contractCreateFeeInfo.conditions.shopNum" 
              :placeholder="$t('contractCreateFee.enterShopNum')" clearable
              @keyup.enter.native="_queryContract"></el-input>
          </el-col>
          <el-col :span="4" class="filter-buttons">
            <el-button type="primary" size="small" @click="_queryContract">
              {{ $t('common.query') }}
            </el-button>
            <el-button size="small" @click="_resetContractMethod">
              {{ $t('common.reset') }}
            </el-button>
          </el-col>
        </el-row>
      </div>

      <el-tabs v-model="contractCreateFeeInfo._currentTab" @tab-click="changeTab(contractCreateFeeInfo._currentTab)">
        <el-tab-pane :label="$t('contractCreateFee.contractFee')" name="simplifyPayerObjFee">
          <simplify-contract-fee v-if="contractCreateFeeInfo._currentTab === 'simplifyPayerObjFee'"
            ref="simplifyPayerObjFee" :key="'simplifyPayerObjFee' + contractCreateFeeInfo.contractId"></simplify-contract-fee>
        </el-tab-pane>
        <el-tab-pane :label="$t('contractCreateFee.relRoomFee')" name="relRoomFee">
          <contract-room-fee v-if="contractCreateFeeInfo._currentTab === 'relRoomFee'" ref="relRoomFee"
            :key="'relRoomFee' + contractCreateFeeInfo.contractId"></contract-room-fee>
        </el-tab-pane>
        <el-tab-pane :label="$t('contractCreateFee.relShopFee')" name="relShopFee">
          <contract-shop-fee v-if="contractCreateFeeInfo._currentTab === 'relShopFee'" ref="relShopFee"
            :key="'relShopFee' + contractCreateFeeInfo.contractId"></contract-shop-fee>
        </el-tab-pane>
        <el-tab-pane :label="$t('contractCreateFee.paymentHistory')" name="simplifyHisFee">
          <simplify-his-fee v-if="contractCreateFeeInfo._currentTab === 'simplifyHisFee'" ref="simplifyHisFee"
            :key="'simplifyHisFee' + contractCreateFeeInfo.contractId"></simplify-his-fee>
        </el-tab-pane>
        <el-tab-pane :label="$t('contractCreateFee.prePayFee')" name="prePayFeeDetail">
          <pre-pay-fee-detail v-if="contractCreateFeeInfo._currentTab === 'prePayFeeDetail'" ref="prePayFeeDetail"
            :key="'prePayFeeDetail' + contractCreateFeeInfo.contractId"></pre-pay-fee-detail>
        </el-tab-pane>
        <el-tab-pane v-if="contractCreateFeeInfo.ownerId" :label="$t('contractCreateFee.owner')" name="contractDetailOwner">
          <car-detail-owner v-if="contractCreateFeeInfo._currentTab === 'contractDetailOwner'" ref="contractDetailOwner"
            :key="'contractDetailOwner' + contractCreateFeeInfo.contractId"></car-detail-owner>
        </el-tab-pane>
        <el-tab-pane :label="$t('contractCreateFee.contractRooms')" name="contractDetailRoom">
          <contract-detail-room v-if="contractCreateFeeInfo._currentTab === 'contractDetailRoom'" ref="contractDetailRoom"
            :key="'contractDetailRoom' + contractCreateFeeInfo.contractId"></contract-detail-room>
        </el-tab-pane>
        <el-tab-pane :label="$t('contractCreateFee.contractShops')" name="contractDetailShop">
          <contract-detail-shop v-if="contractCreateFeeInfo._currentTab === 'contractDetailShop'" ref="contractDetailShop"
            :key="'contractDetailShop' + contractCreateFeeInfo.contractId"></contract-detail-shop>
        </el-tab-pane>
        <el-tab-pane v-if="hasPrivilege('502020092373407363')" :label="$t('contractCreateFee.reprintReceipt')"
          name="simplifyFeeReceipt">
          <simplify-fee-receipt v-if="contractCreateFeeInfo._currentTab === 'simplifyFeeReceipt'" ref="simplifyFeeReceipt"
            :key="'simplifyFeeReceipt' + contractCreateFeeInfo.contractId"></simplify-fee-receipt>
        </el-tab-pane>
      </el-tabs>
    </div>

    <delete-fee ref="deleteFee" @success="handleSuccess" />
    <edit-fee ref="editFee" @success="handleSuccess" />
    <split-fee ref="splitFee" @success="handleSuccess" />
    <contract-create-fee-add ref="contractCreateFeeAdd" @success="handleSuccess" />
    <download-collection-letter-order ref="downloadCollectionLetterOrder" />
  </create-fee-layout>
</template>

<script>
import { getCommunityId } from '@/api/community/communityApi'
import { queryContract } from '@/api/fee/contractCreateFeeApi'

import CreateFeeLayout from '@/components/fee/createFeeLayout'
import SimplifyContractFee from '@/components/fee/simplifyContractFee'
import SimplifyHisFee from '@/components/fee/simplifyHisFee'
import PrePayFeeDetail from '@/components/fee/prePayFeeDetail'
import CarDetailOwner from '@/components/fee/carDetailOwner'
import ContractDetailRoom from '@/components/contract/contractDetailRoom'
import ContractDetailShop from '@/components/contract/contractDetailShop'
import ContractRoomFee from '@/components/fee/contractRoomFee'
import ContractShopFee from '@/components/fee/contractShopFee'
import SimplifyFeeReceipt from '@/components/fee/simplifyFeeReceipt'
import DeleteFee from '@/components/fee/deleteFee'
import EditFee from '@/components/fee/editFee'
import SplitFee from '@/components/fee/splitFee'

import ContractCreateFeeAdd from '@/components/fee/contractCreateFeeAdd'
import DownloadCollectionLetterOrder from '@/components/fee/downloadCollectionLetterOrder'
import { openFeeTabPane } from '@/utils/feeTabUtil'
import { CONTRACT_STATE } from '@/utils/contractUtil'

export default {
  name: 'ContractCreateFeeList',
  components: {
    CreateFeeLayout,
    SimplifyContractFee,
    SimplifyHisFee,
    PrePayFeeDetail,
    CarDetailOwner,
    ContractDetailRoom,
    ContractDetailShop,
    ContractRoomFee,
    ContractShopFee,
    SimplifyFeeReceipt,
    DeleteFee,
    EditFee,
    SplitFee,
    ContractCreateFeeAdd,
    DownloadCollectionLetterOrder
  },
  data() {
    return {
      communityId: '',
      contractCreateFeeInfo: {
        contracts: [],
        contractId: '',
        contractCode: '',
        contractName: '',
        ownerId: '',
        ownerName: '',
        total: 0,
        records: 1,
        _currentTab: 'simplifyPayerObjFee',
        conditions: {
          contractCode: '',
          contractNameLike: '',
          bContacts: '',
          ownerName: '',
          roomNum: '',
          shopNum: '',
          state: CONTRACT_STATE.EFFECTIVE
        }
      }
    }
  },
  async created() {
    this.communityId = await getCommunityId()
    await this.listContracts(1, 500)
  },
  methods: {
    async listContracts(page, row) {
      const params = {
        page,
        row,
        communityId: this.communityId,
        ...this.contractCreateFeeInfo.conditions,
        state: CONTRACT_STATE.EFFECTIVE
      }

      try {
        const response = await queryContract(params)
        this.contractCreateFeeInfo.contracts = response.data || []
        this.contractCreateFeeInfo.total = response.total || 0
        this.contractCreateFeeInfo.records = response.records || 0

        if (this.contractCreateFeeInfo.contracts.length > 0) {
          const lastSelected = localStorage.getItem('lastSelectedContract')
          if (lastSelected) {
            const selectedContract = JSON.parse(lastSelected)
            this.switchContract(selectedContract)
          } else {
            this.switchContract(this.contractCreateFeeInfo.contracts[0])
          }
        }
      } catch (error) {
        console.error('获取合同列表失败:', error)
        this.$message.error(this.$t('contractCreateFee.fetchError'))
      }
    },
    switchContract(contract) {
      this.contractCreateFeeInfo.contractId = contract.contractId
      this.contractCreateFeeInfo.contractCode = contract.contractCode
      this.contractCreateFeeInfo.contractName = contract.contractName
      this.contractCreateFeeInfo.ownerId = contract.objId
      this.contractCreateFeeInfo.ownerName = contract.ownerName
      this.changeTab(this.contractCreateFeeInfo._currentTab)
      localStorage.setItem('lastSelectedContract', JSON.stringify(contract))
    },
    changeTab(tabName) {
      this.contractCreateFeeInfo._currentTab = tabName
      openFeeTabPane(this, tabName, this._buildTabParams(tabName))
    },
    _buildTabParams(tabName) {
      const params = {
        contractId: this.contractCreateFeeInfo.contractId,
        contractCode: this.contractCreateFeeInfo.contractCode,
        contractName: this.contractCreateFeeInfo.contractName,
        ownerId: this.contractCreateFeeInfo.ownerId,
        ownerName: this.contractCreateFeeInfo.ownerName,
        payerObjId: this.contractCreateFeeInfo.contractId,
        payerObjName: this.contractCreateFeeInfo.contractCode,
        payerObjType: '7777'
      }

      if (tabName === 'simplifyPayerObjFee') {
        return params
      }
      if (tabName === 'simplifyHisFee') {
        return {
          ownerId: params.ownerId,
          payerObjId: params.payerObjId
        }
      }
      if (tabName === 'prePayFeeDetail') {
        return {
          payerObjId: params.payerObjId
        }
      }
      if (tabName === 'contractDetailOwner') {
        return {
          ownerId: params.ownerId
        }
      }
      if (tabName === 'contractDetailRoom') {
        return {
          contractId: params.contractId,
          payerObjType: '3333'
        }
      }
      if (tabName === 'contractDetailShop') {
        return {
          contractId: params.contractId,
          payerObjType: '4444'
        }
      }
      if (tabName === 'relRoomFee' || tabName === 'relShopFee') {
        return {
          contractId: params.contractId
        }
      }
      if (tabName === 'simplifyFeeReceipt') {
        return {
          payerObjId: params.payerObjId,
          ownerId: params.ownerId,
          payerObjType: params.payerObjType
        }
      }
      return params
    },
    async _queryContract() {
      await this.listContracts(1, 500)
    },
    _resetContractMethod() {
      this.contractCreateFeeInfo.conditions.contractCode = ''
      this.contractCreateFeeInfo.conditions.contractNameLike = ''
      this.contractCreateFeeInfo.conditions.bContacts = ''
      this.contractCreateFeeInfo.conditions.ownerName = ''
      this.contractCreateFeeInfo.conditions.roomNum = ''
      this.contractCreateFeeInfo.conditions.shopNum = ''
      this.contractCreateFeeInfo.conditions.state = CONTRACT_STATE.EFFECTIVE
      this._queryContract()
    },
    handleSuccess() {
      this.listContracts(1, 500)
    }
  }
}
</script>

<style lang="scss" scoped>
.filter-buttons {
  white-space: nowrap;
}

::v-deep .el-tabs__nav-scroll {
  overflow-x: auto;
}

.contract-tree-panel {
  height: 100%;
}

.contract-list-card {
  display: flex;
  flex-direction: column;
  height: 100%;
  border: none;
  box-shadow: none;

  ::v-deep .el-card__body {
    display: flex;
    flex-direction: column;
    height: 100%;
    padding: 0;
  }

  .contract-search-input {
    margin-bottom: 10px;
  }

  .contract-list-wrapper {
    flex: 1;
    overflow-y: auto;

    &::-webkit-scrollbar {
      width: 4px;
    }

    &::-webkit-scrollbar-thumb {
      background: #dcdfe6;
      border-radius: 2px;
    }

    &::-webkit-scrollbar-track {
      background: transparent;
    }

    .contract-list {
      list-style: none;
      padding: 0;
      padding-right: 5px;
      margin: 0;

      .contract-item {
        padding: 12px;
        margin-bottom: 8px;
        border: 1px solid #e6e6e6;
        border-radius: 4px;
        cursor: pointer;
        transition: all 0.3s;
        text-align: center;
        font-size: 14px;
        color: #606266;

        &:hover {
          background-color: #f5f7fa;
          border-color: #409eff;
        }

        &.selected {
          background-color: #ecf5ff;
          border-color: #409eff;
          color: #409eff;
          font-weight: bold;
        }
      }
    }
  }
}
</style>
