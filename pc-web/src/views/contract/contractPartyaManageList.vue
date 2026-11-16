<template>
  <div class="contract-partya-center">
    <div class="cc-page-head">
      <div>
        <div class="cc-page-title">{{ $t('contractPartyaCenter.title') }}</div>
        <div class="cc-page-subtitle">{{ $t('contractPartyaCenter.subtitle') }}</div>
      </div>
      <div>
        <el-button type="primary" size="small" @click="handleAdd">
          <i class="el-icon-plus"></i>
          {{ $t('contractPartyaCenter.add') }}
        </el-button>
      </div>
    </div>

    <div class="cc-toolbar">
      <el-input
        v-model="searchForm.partyA"
        class="cc-toolbar-item"
        style="width: 260px"
        clearable
        :placeholder="$t('contractPartyaCenter.searchPlaceholder')"
        @keyup.enter.native="handleSearch"
      />
      <el-button type="primary" size="small" @click="handleSearch">
        <i class="el-icon-search"></i>
        {{ $t('common.search') }}
      </el-button>
      <el-button size="small" @click="handleReset">
        <i class="el-icon-refresh"></i>
        {{ $t('common.reset') }}
      </el-button>
    </div>

    <div v-loading="loading" class="cc-partya-list">
      <el-row v-if="partyas.length" :gutter="16">
        <el-col v-for="item in partyas" :key="item.partyaId" :xs="24" :sm="12" :md="12" :lg="8">
          <contract-partya-card
            :partya="item"
            @edit="handleEdit"
            @set-default="handleSetDefault"
            @delete="handleDelete"
          />
        </el-col>
      </el-row>
      <div v-else-if="!loading" class="cc-empty">{{ $t('contractPartyaCenter.empty') }}</div>

      <el-pagination
        :current-page="page.current"
        :page-sizes="[12, 24, 36, 48]"
        :page-size="page.size"
        :total="page.total"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 16px; text-align: right"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <add-contract-partya ref="addContractPartya" @success="refresh" />
    <edit-contract-partya ref="editContractPartya" @success="refresh" />
  </div>
</template>

<script>
import ContractPartyaCard from '@/components/contract/contractPartyaCard'
import AddContractPartya from '@/components/contract/addContractPartya'
import EditContractPartya from '@/components/contract/editContractPartya'
import {
  deleteContractPartya,
  queryContractPartya,
  setDefaultContractPartya
} from '@/api/contract/contractPartyaApi'

export default {
  name: 'ContractPartyaManageList',
  components: {
    ContractPartyaCard,
    AddContractPartya,
    EditContractPartya
  },
  data() {
    return {
      loading: false,
      searchForm: {
        partyA: ''
      },
      partyas: [],
      page: {
        current: 1,
        size: 12,
        total: 0
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      queryContractPartya({
        page: this.page.current,
        row: this.page.size,
        partyA: this.searchForm.partyA
      }).then(res => {
        // 逻辑删除的主体不展示
        this.partyas = (res.data || []).filter(item => String(item.statusCd) === '0')
        this.page.total = res.total || 0
      }).catch(error => {
        this.partyas = []
        this.page.total = 0
        this.$message.error(error.message || this.$t('contractPartyaCenter.msg.fetchError'))
      }).finally(() => {
        this.loading = false
      })
    },
    handleSearch() {
      this.page.current = 1
      this.getList()
    },
    handleReset() {
      this.searchForm = { partyA: '' }
      this.handleSearch()
    },
    handleSizeChange(size) {
      this.page.size = size
      this.getList()
    },
    handleCurrentChange(current) {
      this.page.current = current
      this.getList()
    },
    refresh() {
      this.getList()
    },
    handleAdd() {
      this.$refs.addContractPartya.open()
    },
    handleEdit(row) {
      this.$refs.editContractPartya.open(row)
    },
    handleSetDefault(row) {
      setDefaultContractPartya({ partyaId: row.partyaId }).then(() => {
        this.$message.success(this.$t('contractPartyaCenter.msg.setDefaultSuccess'))
        this.getList()
      }).catch(error => {
        this.$message.error(error.message || this.$t('contractPartyaCenter.msg.setDefaultError'))
      })
    },
    handleDelete(row) {
      this.$confirm(
        this.$t('contractPartyaCenter.msg.deleteConfirm', { name: row.partyA }),
        '提示',
        { type: 'warning' }
      ).then(() => {
        deleteContractPartya({ partyaId: row.partyaId }).then(() => {
          this.$message.success(this.$t('contractPartyaCenter.msg.deleteSuccess'))
          this.getList()
        }).catch(error => {
          this.$message.error(error.message || this.$t('contractPartyaCenter.msg.deleteReferenced'))
        })
      }).catch(() => {
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.contract-partya-center {
  padding: 16px;

  .cc-page-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 16px;

    .cc-page-title {
      font-size: 18px;
      font-weight: 600;
      color: #303133;
    }

    .cc-page-subtitle {
      margin-top: 4px;
      font-size: 12px;
      color: #909399;
    }
  }

  .cc-toolbar {
    display: flex;
    align-items: center;
    margin-bottom: 16px;
  }

  .cc-partya-list {
    min-height: 200px;
  }
}
</style>
