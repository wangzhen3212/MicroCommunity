<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="10">
        <el-form-item :label="$t('contractWorkbench.field.contractName')" required>
          <el-input v-model="form.contractName" maxlength="100" />
        </el-form-item>
      </el-col>
      <el-col :span="7">
        <el-form-item :label="$t('contractWorkbench.field.contractCode')" required>
          <el-input v-model="form.contractCode" disabled>
            <el-button slot="append" size="mini" @click="$emit('regenerate-code')">
              {{ $t('contractWorkbench.regenerate') }}
            </el-button>
          </el-input>
        </el-form-item>
      </el-col>
      <el-col :span="7">
        <el-form-item :label="$t('contractWorkbench.field.template')" required>
          <el-select
            v-model="form.templateId"
            style="width: 100%"
            clearable
            @change="$emit('template-change', form.templateId)"
          >
            <el-option
              v-for="item in templates"
              :key="item.templateId"
              :label="item.templateName || $t('contractTemplate.title')"
              :value="item.templateId"
            />
          </el-select>
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="8">
        <el-form-item :label="$t('contractWorkbench.field.partyA')" required>
          <el-select
            v-model="form.partyaId"
            style="width: 100%"
            clearable
            filterable
            @change="$emit('partya-change', form.partyaId)"
          >
            <el-option
              v-for="item in partyas"
              :key="item.partyaId"
              :label="item.partyA"
              :value="item.partyaId"
            />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item :label="$t('contractWorkbench.field.contacts')">
          <el-input v-model="form.aContacts" disabled />
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item :label="$t('contractWorkbench.field.partyB')" required>
          <el-input v-model="form.partyB" :disabled="isRent">
            <el-button v-if="isRent" slot="append" size="mini" @click="$emit('select-owner')">
              {{ $t('common.choose') }}
            </el-button>
          </el-input>
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="6">
        <el-form-item :label="$t('contractWorkbench.field.creditCode')">
          <el-input v-model="form.creditCode" maxlength="18" />
        </el-form-item>
      </el-col>
      <el-col :span="6">
        <el-form-item :label="$t('contractWorkbench.field.contacts')">
          <el-input v-model="form.bContacts" />
        </el-form-item>
      </el-col>
      <el-col :span="6">
        <el-form-item :label="$t('contractWorkbench.field.tel')">
          <el-input v-model="form.bLink" maxlength="20" />
        </el-form-item>
      </el-col>
      <el-col :span="6">
        <el-form-item :label="$t('contractWorkbench.field.amount')" required>
          <el-input v-model="form.amount" @input="$emit('amount-change', form.amount)" />
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="6">
        <el-form-item :label="$t('contractWorkbench.field.signingTime')" required>
          <el-date-picker
            v-model="form.signingTime"
            type="date"
            value-format="yyyy-MM-dd HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
      <el-col :span="6">
        <el-form-item :label="$t('contractWorkbench.field.startTime')" required>
          <el-date-picker
            v-model="form.startTime"
            type="date"
            value-format="yyyy-MM-dd"
            style="width: 100%"
            @change="$emit('term-change')"
          />
        </el-form-item>
      </el-col>
      <el-col :span="6">
        <el-form-item :label="$t('contractWorkbench.field.endTime')" required>
          <el-date-picker
            v-model="form.endTime"
            type="date"
            value-format="yyyy-MM-dd"
            style="width: 100%"
            @change="$emit('term-change')"
          />
        </el-form-item>
      </el-col>
      <el-col :span="6">
        <el-form-item :label="$t('contractWorkbench.field.term')">
          <el-input :value="termText" disabled />
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="6">
        <el-form-item :label="$t('contractWorkbench.field.operator')" required>
          <el-input v-model="form.operator" maxlength="50" />
        </el-form-item>
      </el-col>
      <el-col :span="6">
        <el-form-item :label="$t('contractWorkbench.field.operatorLink')">
          <el-input v-model="form.operatorLink" maxlength="20" />
        </el-form-item>
      </el-col>
      <el-col :span="6">
        <el-form-item :label="$t('contractWorkbench.field.payMode')">
          <el-input :value="form.payMode" disabled />
        </el-form-item>
      </el-col>
      <el-col :span="6">
        <el-form-item :label="$t('contractWorkbench.field.place')">
          <el-input v-model="form.place" maxlength="100" />
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="24">
        <el-form-item :label="$t('contractWorkbench.field.remark')">
          <el-input v-model="form.remark" maxlength="200" />
        </el-form-item>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { CONTRACT_CATEGORY, amountToChinese, termMonths } from '@/utils/contractUtil'

export default {
  name: 'ContractBaseInfoForm',
  props: {
    form: {
      type: Object,
      required: true
    },
    partyas: {
      type: Array,
      default: function () {
        return []
      }
    },
    templates: {
      type: Array,
      default: function () {
        return []
      }
    }
  },
  computed: {
    isRent() {
      return String(this.form.category) === CONTRACT_CATEGORY.RENT
    },
    amountText() {
      return amountToChinese(this.form.amount)
    },
    termText() {
      const months = termMonths(this.form.startTime, this.form.endTime)
      return months > 0 ? months + ' 个月' : '-'
    }
  }
}
</script>
