<template>
  <div class="room-batch-building-list">
    <div
      v-for="item in buildings"
      :key="item.floorId"
      class="building-row"
      :class="{ 'is-active': item.floorId === activeFloorId }"
      @click="$emit('select-building', item.floorId)">
      <el-checkbox
        class="building-check"
        :value="item.checked"
        :indeterminate="item.indeterminate"
        @click.native.stop
        @change="$emit('toggle-building', item.floorId)">
      </el-checkbox>
      <span class="building-name">{{ item.floorNum }}{{ $t('roomCreatePayFee.buildingSuffix') }}</span>
      <span class="building-count" :class="{ 'has-selected': item.selCount > 0 }">{{ item.selCount }}/{{ item.loaded ? item.total : '-' }}</span>
    </div>
    <div v-if="!buildings || buildings.length === 0" class="list-empty">{{ $t('roomCreatePayFee.noRoom') }}</div>
  </div>
</template>

<script>
export default {
  name: 'RoomBatchBuildingList',
  props: {
    buildings: {
      type: Array,
      default: () => []
    },
    activeFloorId: {
      type: String,
      default: ''
    }
  }
}
</script>

<style lang="scss" scoped>
.room-batch-building-list {
  padding: 6px;
  text-align: left;

  .building-row {
    display: flex;
    align-items: center;
    padding: 7px 8px;
    border-radius: 8px;
    cursor: pointer;
    transition: background 0.14s;

    &:hover {
      background: #f1f5f9;
    }

    &.is-active {
      background: #eff6ff;

      .building-name {
        font-weight: 600;
        color: #1d4ed8;
      }
    }
  }

  .building-check {
    margin-right: 6px;
  }

  .building-name {
    flex: 1;
    overflow: hidden;
    font-size: 13px;
    font-weight: 500;
    color: #334155;
    text-align: left;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  .building-count {
    font-size: 11px;
    color: #94a3b8;
    text-align: right;
    white-space: nowrap;

    &.has-selected {
      font-weight: 500;
      color: #2563eb;
    }
  }

  .list-empty {
    padding: 24px 8px;
    font-size: 12px;
    color: #94a3b8;
    text-align: center;
  }
}
</style>
