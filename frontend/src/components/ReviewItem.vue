<script setup>
import { computed } from 'vue'

const props = defineProps({
  review: { type: Object, required: true },
  depth: { type: Number, default: 0 },
})

const emit = defineEmits(['reply', 'edit', 'delete'])

const isTopLevel = computed(() => props.review.parentId == null)
const indent = computed(() => Math.min(props.depth, 4) * 14)

function formatTime(value) {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return ''
  return d.toLocaleString('zh-CN', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}
</script>

<template>
  <div class="review-node" :style="{ marginLeft: indent + 'px' }">
    <div class="review-entry">
      <div class="review-entry-head">
        <span class="review-avatar" :class="{ 'is-anonymous': review.anonymous }">
          <van-image v-if="review.avatarUrl" :src="review.avatarUrl" fit="cover" round />
          <van-icon v-else name="manager" />
        </span>
        <span class="review-entry-user">{{ review.userName }}</span>
        <van-rate v-if="isTopLevel && review.rating != null" :model-value="review.rating" readonly size="14" />
      </div>
      <p v-if="review.content" class="review-entry-content">{{ review.content }}</p>
      <van-image v-if="review.imageUrl" :src="review.imageUrl" fit="cover" class="review-entry-image" />
      <div class="review-entry-meta">
        <span v-if="formatTime(review.updatedAt)">{{ formatTime(review.updatedAt) }}</span>
        <button class="review-link-btn" type="button" @click="emit('reply', review)">回复</button>
        <button v-if="review.mine" class="review-link-btn" type="button" @click="emit('edit', review)">编辑</button>
        <button v-if="review.mine" class="review-link-btn is-danger" type="button" @click="emit('delete', review)">删除</button>
      </div>
    </div>
    <div v-if="review.replies?.length" class="review-children">
      <ReviewItem
        v-for="child in review.replies"
        :key="child.id"
        :review="child"
        :depth="depth + 1"
        @reply="emit('reply', $event)"
        @edit="emit('edit', $event)"
        @delete="emit('delete', $event)"
      />
    </div>
  </div>
</template>
