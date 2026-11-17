<template>
  <el-dialog :title="$t('exportContractFeeImportExcel.templateExport')" :visible.sync="visible" width="50%">
    <el-form label-width="150px" class="text-left">
      <el-form-item :label="$t('exportContractFeeImportExcel.feeItem')">
        <div>
          <el-checkbox v-model="isConfigAll" @change="changeAllConfigs">
            {{ $t('exportContractFeeImportExcel.all') }}
          </el-checkbox>
          <el-checkbox-group v-model="selectedConfigs" @change="changeItemConfig">
            <template v-for="item in configs">
              <el-checkbox :key="item.configId" :label="item.configId" style="margin-left:15px"
                v-if="item.feeTypeCd !== '888800010001' && item.feeTypeCd !== '888800010009' && item.feeTypeCd !== '888800010011'">
                {{ item.feeName }}
              </el-checkbox>
            </template>
          </el-checkbox-group>
        </div>
      </el-form-item>
    </el-form>

    <div slot="footer" class="dialog-footer">
      <el-button @click="visible = false">{{ $t('exportContractFeeImportExcel.cancel') }}</el-button>
      <el-button type="primary" @click="handleExport">{{ $t('exportContractFeeImportExcel.export') }}</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { listFeeConfigs, exportContractFeeExcel } from '@/api/fee/contractCreateFeeApi'

export default {
  name: 'ExportContractFeeImportExcel',
  data() {
    return {
      visible: false,
      isConfigAll: true,
      selectedConfigs: [],
      configs: []
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    async fetchData() {
      try {
        // 获取费用配置
        const configRes = await listFeeConfigs({
          page: 1,
          row: 100,
          isDefault: 'F'
        })
        this.configs = configRes.feeConfigs || []
        this.selectedConfigs = this.configs
          .filter(item => item.feeTypeCd !== '888800010001' && item.feeTypeCd !== '888800010009' && item.feeTypeCd !== '888800010011')
          .map(item => item.configId)

      } catch (error) {
        console.error('获取数据失败:', error)
      }
    },
    open() {
      this.visible = true
    },
    changeAllConfigs() {
      if (this.isConfigAll) {
        this.selectedConfigs = this.configs
          .filter(item => item.feeTypeCd !== '888800010001' && item.feeTypeCd !== '888800010009' && item.feeTypeCd !== '888800010011')
          .map(item => item.configId)
      } else {
        this.selectedConfigs = []
      }
    },
    changeItemConfig() {
      this.isConfigAll = this.selectedConfigs.length === this.configs
        .filter(item => item.feeTypeCd !== '888800010001' && item.feeTypeCd !== '888800010009' && item.feeTypeCd !== '888800010011')
        .length
    },
    async handleExport() {
      try {
        const configIds = this.selectedConfigs.join(',')

        await exportContractFeeExcel({
          configIds
        })

        this.$message.success(this.$t('common.operationSuccess'))
        this.visible = false
      } catch (error) {
        this.$message.error(this.$t('common.exportError'))
      }
    }
  }
}
</script>
