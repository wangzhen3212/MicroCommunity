<template>
  <div class="contract-partya-center cc-page">
    <div class="cc-surface">
      <div class="cc-surface-head is-stacked">
        <div>
          <div class="cc-surface-title">{{ $t('contractPartyaCenter.title') }}</div>
          <div class="cc-surface-subtitle">{{ $t('contractPartyaCenter.subtitle') }}</div>
        </div>
      </div>

      <div class="cc-partya-bar">
        <div class="cc-partya-filter">
          <el-input
            class="cc-partya-search"
            v-model="searchForm.partyA"
            prefix-icon="el-icon-search"
            clearable
            :placeholder="$t('contractPartyaCenter.searchPlaceholder')"
            @keyup.enter.native="handleSearch"
            @clear="handleReset"
          />
          <el-button @click="handleSearch">
            <i class="el-icon-search"></i>
            <span>{{ $t('common.search') }}</span>
          </el-button>
        </div>
        <div class="cc-partya-actions">
          <el-button type="primary" @click="handleAdd">
            <i class="el-icon-plus"></i>
            <span>{{ $t('contractPartyaCenter.add') }}</span>
          </el-button>
        </div>
      </div>

      <div v-loading="loading" class="cc-partya-list">
        <div v-if="partyas.length" class="cc-card-grid">
          <el-row :gutter="16">
            <el-col v-for="item in partyas" :key="item.partyaId" :xs="24" :sm="12" :lg="8">
              <contract-partya-card
                :partya="item"
                @edit="handleEdit"
                @set-default="handleSetDefault"
                @delete="handleDelete"
              />
            </el-col>
          </el-row>
        </div>
        <div v-else-if="!loading" class="cc-empty">{{ $t('contractPartyaCenter.empty') }}</div>

        <el-pagination
          class="cc-pagination"
          :current-page="page.current"
          :page-sizes="[12, 24, 36, 48]"
          :page-size="page.size"
          :total="page.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
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
  .cc-partya-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 12px;
    margin: 16px 0;
  }

  .cc-partya-filter {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;

    .el-button {
      height: 36px;
      padding: 0 16px;
      margin-left: 0;
    }
  }

  .cc-partya-actions {
    display: flex;
    align-items: center;
    flex: none;
    gap: 8px;

    .el-button {
      height: 36px;
      padding: 0 16px;
      margin-left: 0;
    }
  }

  .cc-partya-search {
    width: 240px;

    ::v-deep .el-input__inner {
      height: 36px;
      line-height: 36px;
    }
  }

  .cc-partya-list {
    min-height: 200px;
  }
}
</style>
