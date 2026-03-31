<template>
  <el-drawer v-model="visibleModel" size="980px" :with-header="false">
    <section class="picker-drawer">
      <div class="picker-header">
        <div>
          <h3>选题学习</h3>
          <p>先选择题集，再搜索并勾选题目。</p>
        </div>
        <div class="picker-header-actions">
          <el-button text @click="visibleModel = false">关闭</el-button>
        </div>
      </div>

      <el-form label-position="top" class="picker-filter-form">
        <div class="picker-filter-grid">
          <el-form-item label="题集">
            <el-select
              v-model="collectionIdModel"
              filterable
              clearable
              placeholder="请选择题集"
              :loading="collectionLoading"
              @change="$emit('change-collection')"
            >
              <el-option
                v-for="collection in collectionOptions"
                :key="collection.collectionId"
                :label="collection.name"
                :value="collection.collectionId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="关键词">
            <el-input
              v-model="query.keyword"
              clearable
              placeholder="输入题干/标题关键词"
              @keyup.enter="$emit('filter-change')"
            />
          </el-form-item>
          <el-form-item label="题型">
            <el-select v-model="query.typeCode" clearable placeholder="全部" @change="$emit('filter-change')">
              <el-option v-for="type in questionTypeOptions" :key="type.code" :label="type.label" :value="type.code" />
            </el-select>
          </el-form-item>
          <el-form-item label="难度范围" class="picker-range-item">
            <div class="picker-range-row">
              <el-input-number
                v-model="query.levelMin"
                :min="0"
                :max="1"
                :step="0.01"
                :precision="2"
                controls-position="right"
                @change="$emit('filter-change')"
              />
              <span class="picker-range-separator">-</span>
              <el-input-number
                v-model="query.levelMax"
                :min="0"
                :max="1"
                :step="0.01"
                :precision="2"
                controls-position="right"
                @change="$emit('filter-change')"
              />
            </div>
          </el-form-item>
        </div>
        <el-alert
          v-if="questionPickerLevelError"
          class="picker-level-error"
          :title="questionPickerLevelError"
          type="error"
          :closable="false"
        />
        <div class="picker-filter-actions">
          <el-button
            type="primary"
            :loading="questionPickerLoading"
            :disabled="!collectionIdModel"
            @click="$emit('search')"
          >
            搜索
          </el-button>
          <el-button :disabled="!collectionIdModel" @click="$emit('reset-filters')">重置</el-button>
        </div>
      </el-form>

      <div class="picker-content">
        <div class="picker-list-panel">
          <div class="picker-list" v-loading="questionPickerLoading">
            <el-empty v-if="!collectionIdModel" description="请先选择题集" />
            <el-empty v-else-if="!records.length" description="暂无可选题目" />
            <div
              v-for="(question, index) in records"
              :key="question.id"
              class="picker-item"
              :class="{ active: question.id === pickerActiveQuestionId }"
            >
              <el-checkbox
                :model-value="isQuestionSelectedInDrawer(question.id)"
                @change="(checked) => $emit('toggle-question', question, checked)"
              />
              <button class="picker-item-main" type="button" @click="$emit('select-detail', question)">
                <p class="item-index">第 {{ questionDisplayNo(index) }} 题</p>
                <p class="item-title" :title="question.title">{{ question.title || '（暂无题干摘要）' }}</p>
                <div class="item-meta">
                  <el-tag size="small" effect="plain">{{ typeLabel(question.typeCode) }}</el-tag>
                  <el-tag size="small" type="success" effect="plain">{{ difficultyLabel(question.difficulty) }}</el-tag>
                </div>
              </button>
              <el-button text size="small" class="picker-detail-trigger" @click="$emit('select-detail', question)">查看详情</el-button>
            </div>
          </div>
        </div>

        <div class="picker-detail-panel" v-loading="pickerDetailLoading">
          <el-empty
            v-if="!pickerActiveQuestionId"
            description="请选择左侧题目查看详情"
          />
          <el-empty
            v-else-if="!pickerActiveQuestionDetail && !pickerDetailLoading"
            description="题目详情加载失败，请重试"
          />
          <template v-else-if="pickerActiveQuestionDetail">
            <div class="picker-detail-header">
              <div>
                <p class="detail-index">第 {{ pickerActiveQuestionNo }} 题</p>
                <h4 class="detail-title">{{ pickerActiveQuestionDetail.title || '未命名题目' }}</h4>
              </div>
              <div class="detail-tags">
                <el-tag size="small" effect="plain">{{ typeLabel(pickerActiveQuestionDetail.typeCode) }}</el-tag>
                <el-tag size="small" type="success" effect="plain">{{ difficultyLabel(pickerActiveQuestionDetail.difficulty) }}</el-tag>
              </div>
            </div>

            <div class="picker-detail-body">
              <div class="detail-section">
                <p class="detail-label">题干</p>
                <AssistantMessageContent :content="pickerActiveQuestionDetail.stem || '（暂无题干）'" />
              </div>

              <div v-if="pickerDetailOptions.length" class="detail-section">
                <p class="detail-label">选项</p>
                <ol class="detail-option-list">
                  <li v-for="(option, idx) in pickerDetailOptions" :key="`${option.key}_${idx}`">
                    <span class="option-key">{{ option.key }}.</span>
                    <AssistantMessageContent :content="option.content || '（无内容）'" />
                  </li>
                </ol>
              </div>

              <el-collapse v-model="collapseNamesModel" class="detail-collapse">
                <el-collapse-item name="answer" title="正确答案">
                  <AssistantMessageContent :content="pickerDetailAnswer || '暂无答案'" />
                </el-collapse-item>
                <el-collapse-item name="solution" title="解析">
                  <AssistantMessageContent :content="pickerDetailSolution || '暂无解析'" />
                </el-collapse-item>
              </el-collapse>
            </div>
          </template>
        </div>
      </div>

      <div class="picker-pagination" v-if="page">
        <span class="picker-page-summary">{{ pickerPageSummary }}</span>
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          :total="page.total"
          :hide-on-single-page="false"
          :disabled="questionPickerLoading"
          @current-change="$emit('page-change', $event)"
          @size-change="$emit('size-change', $event)"
        />
      </div>

      <div class="picker-footer">
        <div class="footer-summary">
          <p>已勾选 {{ drawerSelectedQuestionIds.length }} 道题</p>
          <div v-if="drawerSelectedPreview.length" class="drawer-preview-tags">
            <el-tag v-for="item in drawerSelectedPreview" :key="item" size="small" effect="plain">{{ item }}</el-tag>
            <el-tag v-if="drawerSelectedQuestionIds.length > drawerSelectedPreview.length" size="small" effect="plain">
              +{{ drawerSelectedQuestionIds.length - drawerSelectedPreview.length }}
            </el-tag>
          </div>
        </div>
        <div class="footer-actions">
          <el-button
            type="primary"
            :disabled="!canApplyDrawerSelection || !canExplainBase"
            @click="$emit('apply-from-drawer')"
          >
            直接讲解
          </el-button>
        </div>
      </div>
    </section>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import AssistantMessageContent from '../AssistantMessageContent.vue';
import type { CollectionView } from '../../types/collection';
import type { QuestionDetail, QuestionOption, QuestionSummary, QuestionSummaryPage } from '../../types/question';
import type { AgentQuestionTypeOption, QuestionPickerQueryState } from '../../types/agent-demo';

const props = defineProps<{
  visible: boolean;
  collectionLoading: boolean;
  questionPickerLoading: boolean;
  collectionOptions: CollectionView[];
  questionTypeOptions: AgentQuestionTypeOption[];
  collectionId: string;
  query: QuestionPickerQueryState;
  questionPickerLevelError: string;
  records: QuestionSummary[];
  pickerActiveQuestionId: string;
  pickerActiveQuestionDetail: QuestionDetail | null;
  pickerActiveQuestionNo: string;
  pickerDetailOptions: QuestionOption[];
  pickerDetailAnswer: string;
  pickerDetailSolution: string;
  pickerDetailLoading: boolean;
  pickerDetailCollapseNames: string[];
  page: QuestionSummaryPage | null;
  pickerPageSummary: string;
  drawerSelectedQuestionIds: string[];
  drawerSelectedPreview: string[];
  canApplyDrawerSelection: boolean;
  canExplainBase: boolean;
  typeLabel: (code?: string) => string;
  difficultyLabel: (difficulty?: number | null) => string;
  questionDisplayNo: (index: number) => number;
  isQuestionSelectedInDrawer: (questionId: string) => boolean;
}>();

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void;
  (e: 'update:pickerDetailCollapseNames', value: string[]): void;
  (e: 'update:collectionId', value: string): void;
  (e: 'change-collection'): void;
  (e: 'filter-change'): void;
  (e: 'search'): void;
  (e: 'reset-filters'): void;
  (e: 'toggle-question', question: QuestionSummary, checked: string | number | boolean): void;
  (e: 'select-detail', question: QuestionSummary): void;
  (e: 'page-change', pageNum: number): void;
  (e: 'size-change', pageSize: number): void;
  (e: 'apply-from-drawer'): void;
}>();

const visibleModel = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value)
});

const collapseNamesModel = computed({
  get: () => props.pickerDetailCollapseNames,
  set: (value: string[]) => emit('update:pickerDetailCollapseNames', value)
});

const collectionIdModel = computed({
  get: () => props.collectionId,
  set: (value: string) => emit('update:collectionId', value)
});
</script>

<style scoped>
.picker-drawer {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.picker-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.picker-header h3 {
  margin: 0;
}

.picker-header p {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.picker-filter-form {
  border: 1px solid var(--el-border-color-light);
  border-radius: 10px;
  padding: 10px;
}

.picker-filter-grid {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.picker-level-error {
  margin-bottom: 8px;
}

.picker-range-item {
  grid-column: span 2;
}

.picker-range-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.picker-range-row :deep(.el-input-number) {
  flex: 1;
  min-width: 0;
}

.picker-range-separator {
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

.picker-filter-actions {
  display: flex;
  gap: 8px;
}

.picker-content {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 0.9fr);
  gap: 12px;
}

.picker-list-panel,
.picker-detail-panel {
  border: 1px solid var(--el-border-color-light);
  border-radius: 10px;
  background: #fff;
  min-height: 0;
}

.picker-list {
  height: 100%;
  padding: 10px;
  overflow-y: auto;
  min-height: 220px;
  overscroll-behavior: contain;
}

.picker-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  padding: 10px 6px;
  border-bottom: 1px dashed var(--el-border-color-light);
}

.picker-item.active {
  background: var(--el-color-primary-light-9);
}

.picker-item:last-child {
  border-bottom: none;
}

.picker-item-main {
  min-width: 0;
  flex: 1;
  border: none;
  background: transparent;
  padding: 0;
  text-align: left;
  cursor: pointer;
}

.picker-detail-trigger {
  margin-left: auto;
}

.item-index {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.item-title {
  margin: 6px 0;
  color: var(--el-text-color-primary);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-meta {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.picker-detail-panel {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.picker-detail-header {
  padding: 12px;
  border-bottom: 1px solid var(--el-border-color-light);
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: flex-start;
}

.detail-index {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.detail-title {
  margin: 4px 0 0;
  font-size: 15px;
  line-height: 1.5;
  color: var(--el-text-color-primary);
}

.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.picker-detail-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px;
}

.detail-section {
  margin-bottom: 12px;
}

.detail-label {
  margin: 0 0 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.detail-option-list {
  margin: 0;
  padding-left: 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-option-list li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.detail-collapse {
  margin-top: 8px;
}

.picker-pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  flex: 0 0 auto;
  padding: 2px 2px 0;
}

.picker-page-summary {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.picker-footer {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  border-top: 1px solid var(--el-border-color-light);
  padding-top: 10px;
}

.footer-summary p {
  margin: 0;
  font-size: 13px;
}

.drawer-preview-tags {
  margin-top: 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.footer-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

@media (max-width: 1024px) {
  .picker-filter-grid {
    grid-template-columns: 1fr;
  }

  .picker-range-item {
    grid-column: span 1;
  }

  .picker-content {
    grid-template-columns: 1fr;
  }

  .picker-detail-panel {
    min-height: 320px;
  }

  .picker-footer {
    flex-direction: column;
  }

  .picker-pagination {
    align-items: flex-start;
  }

  .footer-actions {
    justify-content: flex-start;
  }
}
</style>