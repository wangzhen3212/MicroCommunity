<template>
  <create-fee-layout :title="shopCreateFeeInfo.shopName" :empty="isTreeEmpty"
    :empty-text="$t('shopCreateFee.shopEmpty')" storage-key="shopCreateFeeTreeWidth">
    <div slot="tree">
      <location-shop-tree-div ref="shopTreeDiv" @selectShop="handleSelectShop" @treeEmpty="handleTreeEmpty"></location-shop-tree-div>
    </div>

    <template slot="tools">
      <el-button size="small" @click="_openDoCreateShopFee">
        <i class="el-icon-plus"></i>
        {{ $t('shopCreateFee.customImport') }}
      </el-button>
    </template>

    <div slot="content">
      <div class="filter-container">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-input v-model="shopCreateFeeInfo.condition.shopNum"
              :placeholder="$t('shopCreateFee.shopNumPlaceholder')" clearable
              @blur="listShopInShopCreateFee"></el-input>
          </el-col>
          <el-col :span="6">
            <el-button type="primary" size="small" @click="_queryShopCreateFeeMethod">
              {{ $t('common.query') }}
            </el-button>
            <el-button size="small" @click="_resetShopCreateFeeMethod">
              {{ $t('common.reset') }}
            </el-button>
          </el-col>
        </el-row>
      </div>

      <el-tabs v-model="shopCreateFeeInfo._currentTab" @tab-click="changeTab(shopCreateFeeInfo._currentTab)">
        <el-tab-pane :label="$t('shopCreateFee.shopFee')" name="simplifyPayerObjFee">
          <simplify-shop-fee v-if="shopCreateFeeInfo._currentTab === 'simplifyPayerObjFee'" ref="simplifyPayerObjFee"
            :key="'simplifyPayerObjFee' + shopCreateFeeInfo.shopId"></simplify-shop-fee>
        </el-tab-pane>
        <el-tab-pane :label="$t('shopCreateFee.paymentHistory')" name="simplifyHisFee">
          <simplify-his-fee v-if="shopCreateFeeInfo._currentTab === 'simplifyHisFee'" ref="simplifyHisFee"
            :key="'simplifyHisFee' + shopCreateFeeInfo.shopId"></simplify-his-fee>
        </el-tab-pane>
        <el-tab-pane :label="$t('shopCreateFee.prePayFee')" name="prePayFeeDetail">
          <pre-pay-fee-detail v-if="shopCreateFeeInfo._currentTab === 'prePayFeeDetail'" ref="prePayFeeDetail"
            :key="'prePayFeeDetail' + shopCreateFeeInfo.shopId"></pre-pay-fee-detail>
        </el-tab-pane>
        <el-tab-pane v-if="shopCreateFeeInfo.ownerId" :label="$t('shopCreateFee.ownerShops')"
          name="simplifyShopOwnerShops">
          <simplify-shop-owner-shops v-if="shopCreateFeeInfo._currentTab === 'simplifyShopOwnerShops'"
            ref="simplifyShopOwnerShops" :key="'simplifyShopOwnerShops' + shopCreateFeeInfo.shopId"></simplify-shop-owner-shops>
        </el-tab-pane>
        <el-tab-pane v-if="shopCreateFeeInfo.ownerId" :label="$t('shopCreateFee.owner')" name="carDetailOwner">
          <car-detail-owner v-if="shopCreateFeeInfo._currentTab === 'carDetailOwner'" ref="carDetailOwner"
            :key="'carDetailOwner' + shopCreateFeeInfo.shopId"></car-detail-owner>
        </el-tab-pane>
        <el-tab-pane :label="$t('shopCreateFee.receiptReprint')" name="simplifyFeeReceipt">
          <simplify-fee-receipt v-if="shopCreateFeeInfo._currentTab === 'simplifyFeeReceipt'" ref="simplifyFeeReceipt"
            :key="'simplifyFeeReceipt' + shopCreateFeeInfo.shopId"></simplify-fee-receipt>
        </el-tab-pane>
      </el-tabs>
    </div>

    <do-import-shop-create-fee ref="doImportShopCreateFee"></do-import-shop-create-fee>
  </create-fee-layout>
</template>

<script>
import CreateFeeLayout from '@/components/fee/createFeeLayout'
import LocationShopTreeDiv from '@/components/fee/locationShopTreeDiv'
import SimplifyShopFee from '@/components/fee/simplifyShopFee'
import SimplifyShopOwnerShops from '@/components/fee/simplifyShopOwnerShops'
import SimplifyHisFee from '@/components/fee/simplifyHisFee'
import PrePayFeeDetail from '@/components/fee/prePayFeeDetail'
import CarDetailOwner from '@/components/fee/carDetailOwner'
import SimplifyFeeReceipt from '@/components/fee/simplifyFeeReceipt'
import DoImportShopCreateFee from '@/components/fee/doImportShopCreateFee'
import { getCommunityId } from '@/api/community/communityApi'
import { listShops } from '@/api/shop/shopCreateFeeApi'
import { openFeeTabPane } from '@/utils/feeTabUtil'

export default {
  name: 'ShopCreateFeeList',
  components: {
    CreateFeeLayout,
    LocationShopTreeDiv,
    SimplifyShopFee,
    SimplifyShopOwnerShops,
    SimplifyHisFee,
    PrePayFeeDetail,
    CarDetailOwner,
    SimplifyFeeReceipt,
    DoImportShopCreateFee
  },
  data() {
    return {
      shopCreateFeeInfo: {
        shopName: '',
        shopId: '',
        shopNum: '',
        ownerId: '',
        ownerName: '',
        builtUpArea: '',
        locationId: '',
        locationName: '',
        _currentTab: 'simplifyPayerObjFee',
        condition: {
          shopNum: ''
        }
      },
      communityId: '',
      isTreeEmpty: false
    }
  },
  created() {
    this.communityId = getCommunityId()
  },
  mounted() {
    this.changeTab(this.shopCreateFeeInfo._currentTab)
  },
  methods: {
    handleTreeEmpty() {
      this.isTreeEmpty = true
    },
    handleSelectShop(param) {
      this.shopCreateFeeInfo.ownerName = ''
      this.shopCreateFeeInfo.ownerId = param.ownerId || ''
      this.shopCreateFeeInfo.shopId = param.shopId
      this.shopCreateFeeInfo.shopName = param.shopName
      this.shopCreateFeeInfo.shopNum = param.shopNum || ''
      this.shopCreateFeeInfo.builtUpArea = param.builtUpArea || ''
      this.shopCreateFeeInfo.locationId = param.locationId || ''
      this.shopCreateFeeInfo.locationName = param.locationName || ''

      this.shopCreateFeeInfo.condition.shopNum = ''
      this.changeTab('simplifyPayerObjFee')
    },
    async listShopInShopCreateFee() {
      if (!this.shopCreateFeeInfo.condition.shopNum) {
        return
      }
      const param = {
        page: 1,
        row: 1,
        communityId: this.communityId,
        shopNum: this.shopCreateFeeInfo.condition.shopNum
      }

      try {
        const res = await listShops(param)
        const shops = res.data || []
        if (shops.length > 0) {
          const shop = shops[0]
          this.shopCreateFeeInfo.shopId = shop.shopId
          this.shopCreateFeeInfo.shopName = shop.shopNum
          this.shopCreateFeeInfo.shopNum = shop.shopNum
          this.shopCreateFeeInfo.ownerId = shop.ownerId || ''
          this.shopCreateFeeInfo.ownerName = shop.ownerName || ''
          this.shopCreateFeeInfo.builtUpArea = shop.builtUpArea || ''
          this.shopCreateFeeInfo.locationId = shop.locationId || ''
          this.shopCreateFeeInfo.locationName = shop.locationName || ''
          this.changeTab('simplifyPayerObjFee')
        }
      } catch (error) {
        console.error('Failed to load shop:', error)
      }
    },
    _queryShopCreateFeeMethod() {
      this.listShopInShopCreateFee()
    },
    _resetShopCreateFeeMethod() {
      this.shopCreateFeeInfo.shopNum = ''
      this.shopCreateFeeInfo.ownerName = ''
      this.shopCreateFeeInfo.ownerId = ''
      this.shopCreateFeeInfo.condition.shopNum = ''
      this.changeTab('simplifyPayerObjFee')
    },
    changeTab(tabName) {
      this.shopCreateFeeInfo._currentTab = tabName
      openFeeTabPane(this, tabName, {
        shopId: this.shopCreateFeeInfo.shopId,
        shopName: this.shopCreateFeeInfo.shopName,
        shopNum: this.shopCreateFeeInfo.shopNum,
        ownerId: this.shopCreateFeeInfo.ownerId,
        ownerName: this.shopCreateFeeInfo.ownerName,
        builtUpArea: this.shopCreateFeeInfo.builtUpArea,
        payerObjId: this.shopCreateFeeInfo.shopId,
        payerObjName: this.shopCreateFeeInfo.shopName,
        payerObjType: '4444'
      })
    },
    _toBatchCreateFee() {
      this.$router.push('/pages/fee/shopCreatePayFee')
    },
    _openDoCreateShopFee() {
      this.$refs.doImportShopCreateFee.open()
    }
  }
}
</script>
