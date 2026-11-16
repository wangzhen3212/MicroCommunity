<template>
  <div class="room-picked-list">
    <div class="picked-head">
      <span class="picked-title">{{ $t('roomCreatePayFee.pickedListTitle') }}</span>
      <el-button type="text" class="remove-all" @click="$emit('clear-all')">{{ $t('roomCreatePayFee.removeAll') }}</el-button>
    </div>
    <div v-if="rooms && rooms.length > 0" class="picked-body">
      <span v-for="room in rooms" :key="room.roomId" class="picked-chip">
        <span class="chip-text">{{ getRoomLabel(room) }}</span>
        <i class="el-icon-close chip-close" @click="$emit('remove', room.roomId)"></i>
      </span>
    </div>
    <div v-else class="picked-empty">{{ $t('roomCreatePayFee.noPickedRoom') }}</div>
  </div>
</template>

<script>
import { getRoomLabel } from '@/utils/roomBatchUtil'

export default {
  name: 'RoomPickedList',
  props: {
    rooms: {
      type: Array,
      default: () => []
    }
  },
  methods: {
    getRoomLabel
  }
}
</script>

<style lang="scss" scoped>
.room-picked-list {
  .picked-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;

    .picked-title {
      font-size: 12px;
      font-weight: 500;
      color: #475569;
    }

    .remove-all {
      padding: 0;
      font-size: 12px;
      color: #f43f5e;
    }
  }

  .picked-body {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    max-height: 132px;
    overflow-y: auto;
  }

  .picked-chip {
    display: flex;
    align-items: center;
    padding: 3px 6px 3px 10px;
    font-size: 12px;
    color: #475569;
    background: #f1f5f9;
    border-radius: 8px;

    &:hover {
      background: #e2e8f0;
    }

    .chip-close {
      margin-left: 4px;
      font-size: 11px;
      color: #94a3b8;
      cursor: pointer;

      &:hover {
        color: #f43f5e;
      }
    }
  }

  .picked-empty {
    font-size: 12px;
    color: #94a3b8;
  }
}
</style>
