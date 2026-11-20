<script setup>
// SC-10 개념 카드 상세 (F-10 AC9~AC13, F-11 리뷰 절 AC13~AC18, F-12 찜 버튼 AC12~AC14, F-17 AC18 글쓰기 버튼). 공개 화면. 로그인하면 응답의 appearances(내 코드 등장 위치)가 채워진다.
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchConcept } from '@/api/concepts'
import { errorCode, errorMessage } from '@/api/http'
import CodeBlock from '@/components/concept/CodeBlock.vue'
import ConceptAppearances from '@/components/concept/ConceptAppearances.vue'
import ConceptCard from '@/components/concept/ConceptCard.vue'
import ConceptReviews from '@/components/concept/ConceptReviews.vue'
import MarkdownBody from '@/components/concept/MarkdownBody.vue'
import WishButton from '@/components/concept/WishButton.vue'
import ReviewPlanAddButton from '@/components/reviewplan/ReviewPlanAddButton.vue'
import { useAuthStore } from '@/stores/auth'
import { LEVELS } from '@/utils/authValidation'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const concept = ref(null)
const loading = ref(true)
const notFound = ref(false)
const loadError = ref('')
// 리뷰 목록 total (F-11 AC13). 리뷰 절이 rating-change로 알려 준다
const reviewCount = ref(null)

const difficultyLabel = computed(
  () =>
    LEVELS.find((l) => l.value === concept.value?.difficulty)?.label ?? concept.value?.difficulty,
)
const rating = computed(() => Number(concept.value?.avgRating ?? 0).toFixed(1))
// javascript: 같은 URL이 링크가 되지 않게 http(s)만 <a>로 그린다
const referenceIsLink = computed(() => /^https?:\/\//i.test(concept.value?.referenceUrl ?? ''))

// :id를 빠르게 바꿀 때 늦게 도착한 이전 요청의 응답은 버린다
let lastRequest = 0

async function load(id) {
  const request = ++lastRequest
  loading.value = true
  notFound.value = false
  loadError.value = ''
  reviewCount.value = null
  window.scrollTo(0, 0)
  try {
    const result = await fetchConcept(id)
    if (request !== lastRequest) return
    concept.value = result
  } catch (error) {
    if (request !== lastRequest) return
    concept.value = null
    if (errorCode(error) === 'CONCEPT_404') notFound.value = true
    else loadError.value = errorMessage(error)
  } finally {
    if (request === lastRequest) loading.value = false
  }
}

// 관련 카드를 눌러 :id만 바뀌어도 다시 조회한다. 다른 화면으로 떠나는 순간에는 조회하지 않는다
watch(
  () => route.params.id,
  (id) => {
    if (route.name === 'concept-detail') load(Number(id))
  },
  { immediate: true },
)

/** 리뷰를 쓰거나 고치거나 지우면 헤더 평점·리뷰 수를 바꾼다 (카드 상세를 다시 부르지 않는다) */
function onRatingChange({ avgRating, reviewCount: count }) {
  if (avgRating !== undefined && concept.value) concept.value.avgRating = avgRating
  reviewCount.value = count
}

/** 찜·해제 결과로 헤더 찜 상태를 맞춘다 (F-12 AC13·AC14, 카드 상세를 다시 부르지 않는다) */
function onWishChange({ wished, wishCount }) {
  if (!concept.value) return
  concept.value.wished = wished
  concept.value.wishCount = wishCount
}

/** 이 카드를 첨부해 커뮤니티 글쓰기로 (F-17 AC18). 비로그인은 로그인 후 돌아온다 */
function writePost() {
  const target = `/community/write?conceptId=${concept.value.id}`
  if (auth.isLoggedIn) router.push(target)
  else router.push({ path: '/login', query: { redirect: target } })
}

/** 직전 화면이 목록(/concepts, 쿼리 포함)이면 뒤로 가서 검색 조건을 살리고, 아니면 목록 첫 화면으로 (AC12) */
function backToList() {
  const back = router.options.history.state?.back
  if (typeof back === 'string' && /^\/concepts(\?|$)/.test(back)) router.back()
  else router.push('/concepts')
}
</script>

<template>
  <article class="detail">
    <button type="button" class="back" data-action="back-to-list" @click="backToList">
      ← 목록으로
    </button>

    <p v-if="loading" class="hint">불러오는 중…</p>

    <div v-else-if="notFound" class="not-found">
      <p>개념 카드를 찾을 수 없습니다.</p>
      <button type="button" data-action="not-found-to-list" @click="router.push('/concepts')">
        목록으로
      </button>
    </div>

    <p v-else-if="loadError" class="form-error" role="alert">{{ loadError }}</p>

    <template v-else-if="concept">
      <header>
        <div class="title-row">
          <h1>{{ concept.name }}</h1>
          <span class="stats">
            <WishButton
              :concept-id="concept.id"
              :wished="concept.wished"
              :wish-count="concept.wishCount"
              @change="onWishChange"
            />
            <span class="rating" :aria-label="`평점 ${rating}`">★ {{ rating }}</span>
            <span v-if="reviewCount !== null" class="review-count" data-role="review-count"
              >· 리뷰 {{ reviewCount }}</span
            >
          </span>
        </div>
        <span class="tags">
          <span class="tag">{{ concept.category }}</span>
          <span class="tag">{{ difficultyLabel }}</span>
        </span>
        <p class="summary">{{ concept.summary }}</p>
        <ReviewPlanAddButton :concept-id="concept.id" />
        <button type="button" class="write-post" data-action="write-post" @click="writePost">
          이 카드로 글쓰기
        </button>
      </header>

      <section v-if="concept.body" data-section="body">
        <h2>쉬운 설명</h2>
        <MarkdownBody :source="concept.body" />
      </section>

      <section v-if="concept.exampleCode" data-section="example">
        <h2>예제 코드</h2>
        <CodeBlock :code="concept.exampleCode" />
      </section>

      <section v-if="concept.referenceUrl" data-section="reference">
        <h2>출처</h2>
        <a
          v-if="referenceIsLink"
          :href="concept.referenceUrl"
          target="_blank"
          rel="noopener noreferrer"
        >
          {{ concept.referenceUrl }}
        </a>
        <span v-else>{{ concept.referenceUrl }}</span>
      </section>

      <section v-if="concept.appearances?.length" data-section="appearances">
        <h2>내 코드에서 등장한 위치</h2>
        <ConceptAppearances :items="concept.appearances" />
      </section>

      <section v-if="concept.related?.length" data-section="related">
        <h2>관련 카드</h2>
        <ul class="related">
          <li v-for="card in concept.related" :key="card.id">
            <ConceptCard :concept="card" />
          </li>
        </ul>
      </section>

      <section data-section="reviews">
        <h2>리뷰</h2>
        <ConceptReviews :concept-id="concept.id" @rating-change="onRatingChange" />
      </section>
    </template>
  </article>
</template>

<style scoped>
.write-post {
  margin-top: var(--space-3);
  padding: var(--space-2) var(--space-4);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius);
  background: var(--color-surface);
  color: var(--color-primary);
  font: inherit;
  cursor: pointer;
}

.detail {
  max-width: 760px;
  margin: 0 auto;
  padding: var(--space-8) 0;
  min-width: 0;
}

.back {
  padding: 0;
  border: none;
  background: none;
  color: var(--color-primary);
  cursor: pointer;
}

.hint {
  margin-top: var(--space-6);
  color: var(--color-text-muted);
}

.not-found {
  display: grid;
  justify-items: start;
  gap: var(--space-3);
  margin-top: var(--space-6);
}

header {
  display: grid;
  gap: var(--space-2);
  margin-top: var(--space-4);
}

.title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  column-gap: var(--space-4);
  row-gap: var(--space-1);
}

h1 {
  margin: 0;
  overflow-wrap: anywhere;
}

.stats {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: var(--space-4);
}

.rating {
  color: #b45309;
}

.review-count {
  color: var(--color-text-muted);
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  font-size: 0.85em;
}

.tag {
  padding: 0 var(--space-2);
  border-radius: var(--radius);
  background: var(--color-background);
  color: var(--color-text-muted);
}

.summary {
  margin: 0;
  color: var(--color-text-muted);
  overflow-wrap: anywhere;
}

section {
  margin-top: var(--space-8);
  min-width: 0;
}

h2 {
  margin: 0 0 var(--space-3);
  font-size: 1.2em;
}

section a {
  overflow-wrap: anywhere;
}

.related {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: var(--space-4);
  margin: 0;
  padding: 0;
  list-style: none;
}
</style>
