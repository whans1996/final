<script setup>
// SC-10 "리뷰" 절 (F-11 AC13~AC18). 내 리뷰 상자/작성 폼/로그인 버튼 + 최신순 목록 + "더 보기".
// 평점·리뷰 수가 바뀌면 rating-change({ avgRating?, reviewCount })로 올린다. 목록을 처음 받을 때는 reviewCount만 담는다.
import { computed, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import {
  createReview,
  deleteReview,
  fetchMyReview,
  fetchReviews,
  updateReview,
} from '@/api/concepts'
import { errorCode, errorDetails, errorMessage } from '@/api/http'
import ReviewForm from '@/components/concept/ReviewForm.vue'
import StarRating from '@/components/concept/StarRating.vue'
import { useAuthStore } from '@/stores/auth'

const PAGE_SIZE = 10

const props = defineProps({
  conceptId: { type: Number, required: true },
})
const emit = defineEmits(['rating-change'])

const auth = useAuthStore()
const router = useRouter()

const items = ref([])
const total = ref(0)
const loadingList = ref(true)
const loadingMore = ref(false)
const listError = ref('')
const moreError = ref('')

const myReview = ref(null)
const editing = ref(false)
const submitting = ref(false)
const formErrors = ref({})
const formError = ref('')
const boxError = ref('')
// 삭제 후 빈 폼을 새로 그리기 위한 키
const formKey = ref(0)

const hasMore = computed(() => items.value.length < total.value)

// :id가 빠르게 바뀔 때 늦게 도착한 이전 카드의 응답은 버린다
let generation = 0

function resetForm() {
  formErrors.value = {}
  formError.value = ''
  boxError.value = ''
}

async function load(conceptId) {
  const current = ++generation
  items.value = []
  total.value = 0
  listError.value = ''
  moreError.value = ''
  myReview.value = null
  editing.value = false
  submitting.value = false
  resetForm()
  loadingList.value = true

  const list = fetchReviews(conceptId, { page: 1, size: PAGE_SIZE })
    .then((result) => {
      if (current !== generation) return
      items.value = result.items
      total.value = result.total
      emit('rating-change', { reviewCount: result.total })
    })
    .catch((error) => {
      if (current === generation) listError.value = errorMessage(error)
    })
    .finally(() => {
      if (current === generation) loadingList.value = false
    })

  const mine = auth.isLoggedIn ? loadMine(conceptId, current) : Promise.resolve()
  await Promise.all([list, mine])
}

async function loadMine(conceptId, current = generation) {
  try {
    const result = await fetchMyReview(conceptId)
    if (current === generation) myReview.value = result
  } catch {
    // 내 리뷰를 못 불러와도 목록은 보인다. 작성하면 409로 다시 불러온다
  }
}

// 다음 쪽은 이미 받은 개수로 정한다. 새 리뷰가 앞에 붙으면 항목이 뒤로 밀려 같은 항목이 다시 오므로 id로 거르고,
// 불러온 리뷰를 지우면 항목이 앞으로 당겨져 경계 쪽에서 빈자리 하나만 채워지므로, 한 쪽 분량이 안 되면 한 번 더 받는다.
async function loadMore() {
  const current = generation
  loadingMore.value = true
  moreError.value = ''
  try {
    let added = 0
    for (let request = 0; request < 2 && added < PAGE_SIZE && hasMore.value; request += 1) {
      const nextPage = Math.floor(items.value.length / PAGE_SIZE) + 1
      const result = await fetchReviews(props.conceptId, { page: nextPage, size: PAGE_SIZE })
      if (current !== generation) return
      const seen = new Set(items.value.map((r) => r.id))
      const fresh = result.items.filter((r) => !seen.has(r.id))
      items.value = [...items.value, ...fresh]
      total.value = result.total
      added += fresh.length
    }
  } catch (error) {
    if (current === generation) moreError.value = errorMessage(error)
  } finally {
    if (current === generation) loadingMore.value = false
  }
}

function applySaved(saved) {
  myReview.value = saved.review
  total.value = saved.reviewCount
  emit('rating-change', { avgRating: saved.avgRating, reviewCount: saved.reviewCount })
}

async function onCreate(body) {
  const current = generation
  submitting.value = true
  resetForm()
  try {
    const saved = await createReview(props.conceptId, body)
    if (current !== generation) return
    items.value = [saved.review, ...items.value.filter((r) => r.id !== saved.review.id)]
    applySaved(saved)
  } catch (error) {
    if (current !== generation) return
    const code = errorCode(error)
    if (code === 'COMMON_400') formErrors.value = errorDetails(error)
    else formError.value = errorMessage(error)
    if (code === 'CONCEPT_409') {
      // 다른 탭에서 이미 썼다: 내 리뷰 상자로 바뀌므로 메시지는 상자에도 남긴다
      boxError.value = formError.value
      await loadMine(props.conceptId, current)
    }
  } finally {
    if (current === generation) submitting.value = false
  }
}

async function onUpdate(body) {
  const current = generation
  submitting.value = true
  resetForm()
  try {
    const saved = await updateReview(props.conceptId, body)
    if (current !== generation) return
    items.value = items.value.map((r) => (r.id === saved.review.id ? saved.review : r))
    applySaved(saved)
    editing.value = false
  } catch (error) {
    if (current !== generation) return
    if (errorCode(error) === 'COMMON_400') formErrors.value = errorDetails(error)
    else formError.value = errorMessage(error)
  } finally {
    if (current === generation) submitting.value = false
  }
}

function startEdit() {
  resetForm()
  editing.value = true
}

function cancelEdit() {
  resetForm()
  editing.value = false
}

async function onDelete() {
  if (!window.confirm('내 리뷰를 삭제할까요?')) return
  const current = generation
  const id = myReview.value.id
  submitting.value = true
  resetForm()
  try {
    const rating = await deleteReview(props.conceptId)
    if (current !== generation) return
    items.value = items.value.filter((r) => r.id !== id)
    total.value = rating.reviewCount
    myReview.value = null
    formKey.value += 1
    emit('rating-change', { avgRating: rating.avgRating, reviewCount: rating.reviewCount })
  } catch (error) {
    if (current === generation) boxError.value = errorMessage(error)
  } finally {
    if (current === generation) submitting.value = false
  }
}

function goLogin() {
  router.push({ path: '/login', query: { redirect: `/concepts/${props.conceptId}` } })
}

const date = (createdAt) => createdAt?.slice(0, 10) ?? ''

watch(() => props.conceptId, load, { immediate: true })
</script>

<template>
  <div class="reviews">
    <div class="mine">
      <button v-if="!auth.isLoggedIn" type="button" data-action="login-to-review" @click="goLogin">
        로그인하고 리뷰 남기기
      </button>

      <ReviewForm
        v-else-if="myReview && editing"
        :initial="myReview"
        submit-label="저장"
        cancellable
        :submitting="submitting"
        :errors="formErrors"
        :error="formError"
        data-role="edit-form"
        @submit="onUpdate"
        @cancel="cancelEdit"
      />

      <div v-else-if="myReview" class="my-box" data-role="my-review">
        <div class="my-head">
          <strong>내 리뷰</strong>
          <StarRating :model-value="myReview.rating" readonly />
        </div>
        <p class="content">{{ myReview.content }}</p>
        <div class="my-actions">
          <button type="button" data-action="edit" :disabled="submitting" @click="startEdit">
            수정
          </button>
          <button type="button" data-action="delete" :disabled="submitting" @click="onDelete">
            삭제
          </button>
        </div>
        <p v-if="boxError" class="form-error" role="alert">{{ boxError }}</p>
      </div>

      <ReviewForm
        v-else
        :key="formKey"
        submit-label="등록"
        :submitting="submitting"
        :errors="formErrors"
        :error="formError"
        data-role="create-form"
        @submit="onCreate"
      />
    </div>

    <p v-if="loadingList" class="hint">리뷰를 불러오는 중…</p>
    <p v-else-if="listError" class="form-error" role="alert" data-role="list-error">
      {{ listError }}
    </p>
    <p v-else-if="items.length === 0" class="hint" data-role="empty">
      아직 리뷰가 없습니다. 첫 리뷰를 남겨 주세요
    </p>
    <template v-else>
      <ul class="list">
        <li v-for="review in items" :key="review.id" data-role="review" :data-id="review.id">
          <div class="item-head">
            <StarRating :model-value="review.rating" readonly />
            <RouterLink
              v-if="review.authorId !== null"
              :to="`/users/${review.authorId}`"
              class="author"
              >{{ review.authorNickname }}</RouterLink
            >
            <span v-else class="author withdrawn">{{ review.authorNickname }}</span>
            <time :datetime="review.createdAt" class="date">{{ date(review.createdAt) }}</time>
          </div>
          <p class="content">{{ review.content }}</p>
        </li>
      </ul>
      <p v-if="moreError" class="form-error" role="alert">{{ moreError }}</p>
      <button
        v-if="hasMore"
        type="button"
        class="more"
        data-action="more"
        :disabled="loadingMore"
        @click="loadMore"
      >
        더 보기
      </button>
    </template>
  </div>
</template>

<style scoped>
.reviews {
  display: grid;
  gap: var(--space-4);
  min-width: 0;
}

.my-box {
  display: grid;
  gap: var(--space-2);
  padding: var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
}

.my-head,
.item-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
}

.my-actions {
  display: flex;
  gap: var(--space-2);
}

.hint {
  margin: 0;
  color: var(--color-text-muted);
}

.list {
  display: grid;
  gap: var(--space-3);
  margin: 0;
  padding: 0;
  list-style: none;
}

.list li {
  display: grid;
  gap: var(--space-1);
  padding-bottom: var(--space-3);
  border-bottom: 1px solid var(--color-border);
  min-width: 0;
}

.content {
  margin: 0;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.author {
  overflow-wrap: anywhere;
}

.withdrawn,
.date {
  color: var(--color-text-muted);
}

.date {
  font-size: 0.85em;
}

.more {
  justify-self: center;
}
</style>
