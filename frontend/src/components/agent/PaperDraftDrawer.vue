<template>
  <el-drawer v-model="visibleModel" size="980px" :with-header="false">
    <section class="create-drawer">
      <div class="create-header">
        <div>
          <h3>AI 组卷</h3>
          <p>先填写组卷需求，再查看候选题目草稿。</p>
        </div>
        <div class="create-header-actions">
          <el-button text @click="visibleModel = false">关闭</el-button>
        </div>
      </div>

      <el-alert
        type="info"
        :closable="false"
        class="create-tip"
        title="按题型与难度要求生成组卷草稿，确认后可用于后续流程。"
      />

      <el-steps :active="paperDraftStep - 1" finish-status="success" simple class="create-steps">
        <el-step title="填写组卷需求" />
        <el-step title="查看草稿结果" />
      </el-steps>

      <div class="ai-step-content">
        <el-form v-if="paperDraftStep === 1" label-position="top" class="create-form paper-requirement-form">
          <el-form-item label="组卷需求">
            <el-input
              v-model="paperDraftForm.message"
              type="textarea"
              :rows="4"
              maxlength="2000"
              show-word-limit
              placeholder="例如：生成一套基础难度的函数与导数小测，覆盖单选、多选和判断题"
            />
          </el-form-item>

          <el-form-item label="题集（可多选）">
            <el-select
              v-model="paperDraftForm.collectionIds"
              multiple
              filterable
              collapse-tags
              collapse-tags-tooltip
              placeholder="请选择至少一个题集"
              :loading="collectionLoading"
            >
              <el-option
                v-for="collection in collectionOptions"
                :key="collection.collectionId"
                :label="collection.name"
                :value="collection.collectionId"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="题型约束">
            <div class="paper-constrain-list">
              <div class="paper-constrain-head" aria-hidden="true">
                <span>题型</span>
                <span>题数</span>
                <span>难度下限</span>
                <span>难度上限</span>
                <span>操作</span>
              </div>
              <div
                v-for="(constrain, index) in paperDraftForm.constrains"
                :key="`paper_constrain_${index}`"
                class="paper-constrain-row"
              >
                <div class="paper-constrain-cell">
                  <span class="paper-constrain-label">题型</span>
                  <el-select v-model="constrain.typeCode" placeholder="题型">
                    <el-option v-for="type in questionTypeOptions" :key="type.code" :label="type.label" :value="type.code" />
                  </el-select>
                </div>
                <div class="paper-constrain-cell">
                  <span class="paper-constrain-label">题数</span>
                  <el-input-number v-model="constrain.count" :min="1" :step="1" controls-position="right" placeholder="题数" />
                </div>
                <div class="paper-constrain-cell">
                  <span class="paper-constrain-label">难度下限</span>
                  <el-input-number
                    v-model="constrain.difficultyMin"
                    :min="0"
                    :max="1"
                    :step="0.01"
                    :precision="2"
                    controls-position="right"
                    placeholder="难度下限"
                  />
                </div>
                <div class="paper-constrain-cell">
                  <span class="paper-constrain-label">难度上限</span>
                  <el-input-number
                    v-model="constrain.difficultyMax"
                    :min="0"
                    :max="1"
                    :step="0.01"
                    :precision="2"
                    controls-position="right"
                    placeholder="难度上限"
                  />
                </div>
                <div class="paper-constrain-cell paper-constrain-cell-action">
                  <span class="paper-constrain-label">操作</span>
                  <el-button
                    type="danger"
                    plain
                    :disabled="paperDraftForm.constrains.length <= 1"
                    @click="$emit('remove-constrain', index)"
                  >
                    删除
                  </el-button>
                </div>
              </div>
            </div>
            <div class="paper-constrain-actions">
              <el-button @click="$emit('add-constrain')">新增约束</el-button>
            </div>
          </el-form-item>

          <el-alert
            v-if="paperDraftConstraintLevelError"
            class="picker-level-error"
            :title="paperDraftConstraintLevelError"
            type="error"
            :closable="false"
          />
        </el-form>

        <div v-else class="paper-result-wrap">
          <div v-if="generatePaperDraftLoading" class="loading-wrap">
            <el-skeleton :rows="8" animated />
          </div>
          <template v-else-if="paperDraftErrorMessage">
            <el-alert :title="paperDraftErrorMessage" type="error" :closable="false" />
          </template>
          <template v-else-if="paperDraftResult">
            <el-card shadow="never" class="paper-reason-card">
              <template #header>生成说明</template>
              <p class="paper-reason-text">{{ paperDraftResult.reason || '暂无说明' }}</p>
            </el-card>

            <el-empty v-if="!paperDraftCandidateQuestions.length" description="暂无候选题目" />

            <el-table
              v-else
              :data="paperDraftCandidateQuestions"
              border
              stripe
              class="paper-result-table"
              max-height="460"
            >
              <el-table-column prop="questionId" label="questionId" min-width="160" />
              <el-table-column prop="questionVersionId" label="questionVersionId" min-width="180" />
              <el-table-column prop="title" label="title" min-width="280" show-overflow-tooltip />
              <el-table-column prop="typeCode" label="typeCode" min-width="130" />
              <el-table-column prop="difficulty" label="difficulty" min-width="100" />
            </el-table>
          </template>
          <el-empty v-else description="请先生成组卷草稿" />
        </div>
      </div>

      <div class="create-footer">
        <template v-if="paperDraftStep === 1">
          <el-button @click="visibleModel = false">取消</el-button>
          <el-button type="primary" :loading="generatePaperDraftLoading" @click="$emit('generate')">
            生成草稿
          </el-button>
        </template>
        <template v-else>
          <el-button :loading="generatePaperDraftLoading" @click="$emit('generate')">重新生成</el-button>
          <el-button @click="$emit('back-to-requirement')">返回修改需求</el-button>
        </template>
      </div>
    </section>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { CollectionView } from '../../types/collection';
import type { AgentPaperDraftVO, CandidateQuestionVO, GeneratePaperDraftReq } from '../../types/ai';
import type { AgentQuestionTypeOption } from '../../types/agent-demo';

const props = defineProps<{
  visible: boolean;
  collectionLoading: boolean;
  collectionOptions: CollectionView[];
  questionTypeOptions: AgentQuestionTypeOption[];
  paperDraftStep: 1 | 2;
  generatePaperDraftLoading: boolean;
  paperDraftErrorMessage: string;
  paperDraftResult: AgentPaperDraftVO | null;
  paperDraftForm: GeneratePaperDraftReq;
  paperDraftCandidateQuestions: CandidateQuestionVO[];
  paperDraftConstraintLevelError: string;
}>();

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void;
  (e: 'add-constrain'): void;
  (e: 'remove-constrain', index: number): void;
  (e: 'generate'): void;
  (e: 'back-to-requirement'): void;
}>();

const visibleModel = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value)
});
</script>

<style scoped>
.create-drawer {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.create-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.create-header h3 {
  margin: 0;
}

.create-header p {
  margin: 6px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.create-tip {
  margin-bottom: 2px;
}

.create-steps {
  margin-bottom: 2px;
}

.ai-step-content {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
}

.create-form {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 2px;
}

.paper-requirement-form {
  width: 100%;
}

.paper-constrain-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.paper-constrain-head {
  display: grid;
  grid-template-columns: minmax(0, 180px) minmax(0, 120px) minmax(0, 140px) minmax(0, 140px) auto;
  gap: 10px;
  align-items: center;
  padding: 0 2px;
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

.paper-constrain-row {
  display: grid;
  grid-template-columns: minmax(0, 180px) minmax(0, 120px) minmax(0, 140px) minmax(0, 140px) auto;
  gap: 10px;
  align-items: center;
}

.paper-constrain-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.paper-constrain-label {
  display: none;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.paper-constrain-cell :deep(.el-select),
.paper-constrain-cell :deep(.el-input-number) {
  width: 100%;
}

.paper-constrain-cell-action {
  align-self: stretch;
  justify-content: center;
}

.paper-constrain-actions {
  margin-top: 8px;
}

.paper-result-wrap {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-right: 2px;
}

.paper-reason-card {
  border: 1px solid var(--el-border-color-light);
}

.paper-reason-text {
  margin: 0;
  color: var(--el-text-color-primary);
  line-height: 1.6;
  white-space: pre-wrap;
}

.create-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  border-top: 1px solid var(--el-border-color-light);
  padding-top: 10px;
  background: #fff;
  position: sticky;
  bottom: 0;
  z-index: 3;
}

@media (max-width: 1024px) {
  .paper-constrain-head {
    display: none;
  }

  .paper-constrain-row {
    grid-template-columns: 1fr;
    gap: 8px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 10px;
    padding: 10px;
  }

  .paper-constrain-label {
    display: inline-block;
  }

  .paper-constrain-cell-action {
    justify-content: flex-start;
  }

  .create-footer {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>