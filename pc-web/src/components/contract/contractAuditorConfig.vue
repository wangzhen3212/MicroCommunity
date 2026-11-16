<template>
  <el-dialog
    :title="$t('contractAuditCenter.config.title')"
    :visible.sync="visible"
    width="640px"
    @close="handleClose"
  >
    <div class="cc-config-tip">{{ $t('contractAuditCenter.config.tip') }}</div>
    <div class="cc-config-section">
      <div class="cc-config-title">{{ $t('contractAuditCenter.config.legal') }}</div>
      <div v-for="(item, index) in legalAuditors" :key="'legal' + index" class="cc-config-row">
        <el-select
          style="width: 320px"
          v-model="item.staffId"
          filterable
          :placeholder="$t('contractAuditCenter.config.staffPlaceholder')"
          @change="handleStaffChange(item, 'legal')"
        >
          <el-option
            v-for="staff in staffs"
            :key="staff.staffId || staff.userId"
            :label="(staff.name || staff.userName) + ' ' + (staff.tel || '')"
            :value="staff.staffId || staff.userId"
          />
        </el-select>
        <el-button type="text" @click="removeAuditor('legal', index)">
          {{ $t('contractAuditCenter.config.remove') }}
        </el-button>
      </div>
      <el-button size="mini" @click="addAuditor('legal')">
        <i class="el-icon-plus"></i>{{ $t('contractAuditCenter.config.add') }}
      </el-button>
    </div>
    <div class="cc-config-section">
      <div class="cc-config-title">{{ $t('contractAuditCenter.config.leader') }}</div>
      <div v-for="(item, index) in leaderAuditors" :key="'leader' + index" class="cc-config-row">
        <el-select
          style="width: 320px"
          v-model="item.staffId"
          filterable
          :placeholder="$t('contractAuditCenter.config.staffPlaceholder')"
          @change="handleStaffChange(item, 'leader')"
        >
          <el-option
            v-for="staff in staffs"
            :key="(staff.staffId || staff.userId) + 'l'"
            :label="(staff.name || staff.userName) + ' ' + (staff.tel || '')"
            :value="staff.staffId || staff.userId"
          />
        </el-select>
        <el-button type="text" @click="removeAuditor('leader', index)">
          {{ $t('contractAuditCenter.config.remove') }}
        </el-button>
      </div>
      <el-button size="mini" @click="addAuditor('leader')">
        <i class="el-icon-plus"></i>{{ $t('contractAuditCenter.config.add') }}
      </el-button>
    </div>
    <span slot="footer">
      <el-button @click="handleClose">{{ $t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleSave">
        {{ $t('contractAuditCenter.config.save') }}
      </el-button>
    </span>
  </el-dialog>
</template>

<script>
import { queryContractAuditorConfig, saveContractAuditorConfig } from '@/api/contract/contractAuditApi'
import { listStaffs } from '@/api/org/orgApi'

export default {
  name: 'ContractAuditorConfig',
  data() {
    return {
      visible: false,
      loading: false,
      staffs: [],
      legalAuditors: [],
      leaderAuditors: []
    }
  },
  methods: {
    open() {
      this.visible = true
      this.loadStaffs()
      this.loadConfig()
    },
    handleClose() {
      this.visible = false
    },
    loadStaffs() {
      listStaffs({ page: 1, row: 200 }).then(res => {
        const data = (res && res.data) || {}
        this.staffs = data.staffs || []
      }).catch(() => {
        this.staffs = []
      })
    },
    loadConfig() {
      queryContractAuditorConfig({}).then(data => {
        this.legalAuditors = (data.legalAuditors || []).map(item => {
          return {
            staffId: item.staffId,
            staffName: item.staffName,
            staffTel: item.staffTel
          }
        })
        this.leaderAuditors = (data.leaderAuditors || []).map(item => {
          return {
            staffId: item.staffId,
            staffName: item.staffName,
            staffTel: item.staffTel
          }
        })
        if (!this.legalAuditors.length) {
          this.addAuditor('legal')
        }
        if (!this.leaderAuditors.length) {
          this.addAuditor('leader')
        }
      }).catch(error => {
        this.$message.error(error.message || this.$t('contractAuditCenter.config.error'))
      })
    },
    addAuditor(type) {
      const auditor = { staffId: '', staffName: '', staffTel: '' }
      if (type === 'legal') {
        this.legalAuditors.push(auditor)
        return
      }
      this.leaderAuditors.push(auditor)
    },
    removeAuditor(type, index) {
      if (type === 'legal') {
        this.legalAuditors.splice(index, 1)
        return
      }
      this.leaderAuditors.splice(index, 1)
    },
    handleStaffChange(auditor, type) {
      const staffId = auditor.staffId
      for (let index = 0; index < this.staffs.length; index++) {
        const staff = this.staffs[index]
        const currentId = staff.staffId || staff.userId
        if (currentId === staffId) {
          auditor.staffId = currentId
          auditor.staffName = staff.name || staff.userName
          auditor.staffTel = staff.tel
          break
        }
      }
      if (type === 'legal') {
        this.legalAuditors = this.legalAuditors.slice()
      } else {
        this.leaderAuditors = this.leaderAuditors.slice()
      }
    },
    handleSave() {
      const legal = this.legalAuditors.filter(item => !!item.staffId)
      const leader = this.leaderAuditors.filter(item => !!item.staffId)
      if (!legal.length || !leader.length) {
        this.$message.error(this.$t('contractAuditCenter.config.error'))
        return
      }
      this.loading = true
      saveContractAuditorConfig({
        legalAuditors: legal,
        leaderAuditors: leader
      }).then(() => {
        this.loading = false
        this.$message.success(this.$t('contractAuditCenter.config.success'))
        this.$emit('success')
        this.handleClose()
      }).catch(error => {
        this.loading = false
        this.$message.error(error.message || this.$t('contractAuditCenter.config.error'))
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.cc-config-tip {
  font-size: 12px;
  color: #909399;
  margin-bottom: 12px;
}

.cc-config-section {
  margin-bottom: 16px;

  .cc-config-title {
    font-size: 13px;
    font-weight: 600;
    margin-bottom: 8px;
  }

  .cc-config-row {
    display: flex;
    align-items: center;
    margin-bottom: 8px;

    .el-button {
      margin-left: 8px;
    }
  }
}
</style>
