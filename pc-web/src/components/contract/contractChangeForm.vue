<template>
  <el-dialog :title="$t('contractChangeManage.apply.title')" :visible.sync="visible" width="720px" @close="handleClose">
    <el-form :model="form" label-width="110px">
      <el-form-item :label="$t('contractChangeManage.apply.contract')" required>
        <el-select
          v-model="form.contractId"
          style="width: 100%"
          filterable
          @change="handleContractChange"
        >
          <el-option
            v-for="item in contracts"
            :key="item.contractId"
            :label="item.contractCode + ' ' + item.contractName"
            :value="item.contractId"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('contractChangeManage.apply.type')" required>
        <el-radio-group v-model="form.planType" @change="handleTypeChange">
          <el-radio :label="PLAN_TYPE_MAIN_BODY">{{ $t('contractChangeManage.tabs.mainBody') }}</el-radio>
          <el-radio :label="PLAN_TYPE_LEASE">{{ $t('contractChangeManage.tabs.lease') }}</el-radio>
          <el-radio :label="PLAN_TYPE_ASSETS">{{ $t('contractChangeManage.tabs.assets') }}</el-radio>
        </el-radio-group>
      </el-form-item>

      <template v-if="isMainBody">
        <el-form-item :label="$t('contractChangeManage.apply.partyA')">
          <el-input v-model="form.partyA" />
        </el-form-item>
        <el-form-item :label="$t('contractChangeManage.apply.aContacts')">
          <el-input v-model="form.aContacts" />
        </el-form-item>
        <el-form-item :label="$t('contractChangeManage.apply.aLink')">
          <el-input v-model="form.aLink" />
        </el-form-item>
      </template>

      <template v-if="isLease">
        <el-form-item :label="$t('contractChangeManage.apply.startTime')">
          <el-date-picker v-model="form.startTime" type="date" value-format="yyyy-MM-dd" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="$t('contractChangeManage.apply.endTime')">
          <el-date-picker v-model="form.endTime" type="date" value-format="yyyy-MM-dd" style="width: 100%" />
        </el-form-item>
      </template>

      <template v-if="isAssets">
        <el-form-item :label="$t('contractChangeManage.apply.rooms')">
          <el-button size="mini" @click="$refs.houseDialog.open(form.rooms)">
            {{ $t('contractWorkbench.target.add') }}
          </el-button>
          <span class="cc-room-count">{{ form.rooms.length }}</span>
        </el-form-item>
      </template>

      <el-form-item :label="$t('contractChangeManage.apply.effectiveDate')">
        <el-date-picker v-model="form.effectiveDate" type="date" value-format="yyyy-MM-dd" style="width: 100%" />
      </el-form-item>
      <el-form-item :label="$t('contractChangeManage.apply.remark')" required>
        <el-input type="textarea" :rows="3" maxlength="200" v-model="form.changeRemark" />
      </el-form-item>
    </el-form>

    <contract-house-dialog ref="houseDialog" @choose="handleChooseRooms" />

    <span slot="footer">
      <el-button @click="handleClose">{{ $t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="saving" @click="handleSubmit">
        {{ $t('contractChangeManage.apply.submit') }}
      </el-button>
    </span>
  </el-dialog>
</template>

<script>
import ContractHouseDialog from '@/components/contract/workbench/ContractHouseDialog'
import { saveContractChangePlan } from '@/api/contract/contractChangeDetailApi'
import { PLAN_TYPE, buildChangePayload } from '@/utils/contractPlanUtil'

export default {
  name: 'ContractChangeForm',
  components: {
    ContractHouseDialog
  },
  props: {
    contracts: {
      type: Array,
      default: function () {
        return []
      }
    }
  },
  data() {
    return {
      visible: false,
      saving: false,
      PLAN_TYPE_MAIN_BODY: PLAN_TYPE.MAIN_BODY,
      PLAN_TYPE_LEASE: PLAN_TYPE.LEASE,
      PLAN_TYPE_ASSETS: PLAN_TYPE.ASSETS,
      form: {
        contractId: '',
        planType: PLAN_TYPE.MAIN_BODY,
        partyA: '',
        aContacts: '',
        aLink: '',
        startTime: '',
        endTime: '',
        effectiveDate: '',
        changeRemark: '',
        rooms: []
      }
    }
  },
  computed: {
    isMainBody() {
      return this.form.planType === PLAN_TYPE.MAIN_BODY
    },
    isLease() {
      return this.form.planType === PLAN_TYPE.LEASE
    },
    isAssets() {
      return this.form.planType === PLAN_TYPE.ASSETS
    },
    currentContract() {
      return this.contracts.filter(item => item.contractId === this.form.contractId)[0] || {}
    }
  },
  methods: {
    open() {
      this.form = {
        contractId: '',
        planType: PLAN_TYPE.MAIN_BODY,
        partyA: '',
        aContacts: '',
        aLink: '',
        startTime: '',
        endTime: '',
        effectiveDate: '',
        changeRemark: '',
        rooms: []
      }
      this.visible = true
    },
    handleClose() {
      this.visible = false
    },
    handleContractChange() {
      const contract = this.currentContract
      this.form.partyA = contract.partyA || ''
      this.form.aContacts = contract.aContacts || ''
      this.form.aLink = contract.aLink || ''
      this.form.startTime = (contract.startTime || '').substring(0, 10)
      this.form.endTime = (contract.endTime || '').substring(0, 10)
    },
    handleTypeChange() {
      this.form.rooms = []
    },
    handleChooseRooms(rooms) {
      this.form.rooms = rooms || []
    },
    currentUserName() {
      const user = JSON.parse(localStorage.getItem('user') || '{}')
      return user.userName || user.name || ''
    },
    handleSubmit() {
      if (!this.form.contractId) {
        this.$message.error(this.$t('contractChangeManage.apply.contractRequired'))
        return
      }
      if (!this.form.changeRemark) {
        this.$message.error(this.$t('contractChangeManage.apply.remarkRequired'))
        return
      }
      const payload = buildChangePayload(this.currentContract, this.form)
      payload.changePersonName = this.currentUserName()
      this.saving = true
      saveContractChangePlan(payload).then(() => {
        this.$message.success(this.$t('contractChangeManage.apply.success'))
        this.$emit('success')
        this.handleClose()
      }).catch(error => {
        this.$message.error(error.message || this.$t('contractChangeManage.apply.error'))
      }).finally(() => {
        this.saving = false
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.cc-room-count {
  margin-left: 8px;
  font-size: 13px;
  color: #4f46e5;
}
</style>
