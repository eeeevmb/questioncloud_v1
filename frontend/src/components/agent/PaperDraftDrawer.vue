<template>
  <el-drawer v-model="visibleModel" size="min(1180px, 100vw)" :with-header="false" class="paper-draft-drawer">
    <section class="paper-drawer">
      <div class="paper-header">
        <div>
          <h3>
            <el-icon><MagicStick /></el-icon>
            <span>AI 组卷</span>
          </h3>
          <p>先填写组卷要求，AI 将为你生成高质量组卷草稿。</p>
        </div>
        <el-button class="close-btn" plain @click="visibleModel = false">
          <el-icon><Close /></el-icon>
          <span>关闭</span>
        </el-button>
      </div>

      <div class="paper-steps" aria-label="组卷流程">
        <div class="paper-step" :class="{ active: paperDraftStep === 1 }">
          <span class="step-index">1</span>
          <span>
            <strong>填写组卷要求</strong>
            <small>设置题集、题型与难度</small>
          </span>
        </div>
        <el-icon class="step-arrow"><ArrowRight /></el-icon>
        <div class="paper-step" :class="{ active: paperDraftStep === 2 }">
          <span class="step-index">2</span>
          <span>
            <strong>预览确认草稿</strong>
            <small>查看生成结果并确认</small>
          </span>
        </div>
      </div>

      <div class="paper-body">
        <template v-if="paperDraftStep === 1">
          <div class="paper-config-column">
            <section class="paper-section requirement-section">
              <div class="section-heading">
                <div>
                  <h4>1. 组卷要求</h4>
                  <p>描述试卷主题、难度、题型分布和使用场景。</p>
                </div>
              </div>
              <el-input
                v-model="paperDraftForm.message"
                type="textarea"
                :rows="3"
                maxlength="2000"
                show-word-limit
                placeholder="例如：生成一套高等数学期中测试卷，覆盖导数、微分、不定积分、定积分等知识点，难度中等。"
              />
              <div class="quick-requirements">
                <button type="button" @click="applyRequirementPreset('高等数学小测')">高等数学小测</button>
                <button type="button" @click="applyRequirementPreset('导数与微分专题')">导数与微分专题</button>
                <button type="button" @click="applyRequirementPreset('期中复习卷')">期中复习卷</button>
                <button type="button" @click="applyRequirementPreset('难度中等')">难度中等</button>
              </div>
            </section>

            <section class="paper-section">
              <div class="section-heading inline-heading">
                <div>
                  <h4>2. 选择题集</h4>
                  <p>可选择多个题集作为候选题来源。</p>
                </div>
              </div>
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
            </section>

            <section class="paper-section constraint-section">
              <div class="section-heading inline-heading">
                <div>
                  <h4>3. 题型约束</h4>
                  <p>默认预设 4 类题型，可按需要调整数量和难度。</p>
                </div>
              </div>
              <div class="paper-constrain-list">
                <div class="paper-constrain-head" aria-hidden="true">
                  <span>题型</span>
                  <span>题数</span>
                  <span>难度范围</span>
                  <span>操作</span>
                </div>
                <div
                  v-for="(constrain, index) in paperDraftForm.constrains"
                  :key="`paper_constrain_${index}`"
                  class="paper-constrain-row"
                >
                  <div class="paper-constrain-cell type-cell">
                    <span class="paper-constrain-label">题型</span>
                    <el-select v-model="constrain.typeCode" placeholder="题型">
                      <el-option v-for="type in questionTypeOptions" :key="type.code" :label="type.label" :value="type.code" />
                    </el-select>
                  </div>
                  <div class="paper-constrain-cell count-cell">
                    <span class="paper-constrain-label">题数</span>
                    <el-input-number v-model="constrain.count" :min="1" :step="1" controls-position="right" placeholder="题数" />
                  </div>
                  <div class="paper-constrain-cell range-cell">
                    <span class="paper-constrain-label">难度范围</span>
                    <div class="difficulty-range">
                      <el-input-number
                        v-model="constrain.difficultyMin"
                        :min="0"
                        :max="1"
                        :step="0.01"
                        :precision="2"
                        controls-position="right"
                        placeholder="下限"
                      />
                      <span>~</span>
                      <el-input-number
                        v-model="constrain.difficultyMax"
                        :min="0"
                        :max="1"
                        :step="0.01"
                        :precision="2"
                        controls-position="right"
                        placeholder="上限"
                      />
                    </div>
                  </div>
                  <div class="paper-constrain-cell action-cell">
                    <span class="paper-constrain-label">操作</span>
                    <el-button
                      class="delete-btn"
                      text
                      :disabled="paperDraftForm.constrains.length <= 1"
                      @click="$emit('remove-constrain', index)"
                    >
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </div>
                </div>
              </div>
              <div class="paper-constrain-actions">
                <el-button plain @click="$emit('add-constrain')">
                  <el-icon><Plus /></el-icon>
                  <span>新增题型约束</span>
                </el-button>
              </div>
              <el-alert
                v-if="paperDraftConstraintLevelError"
                class="picker-level-error"
                :title="paperDraftConstraintLevelError"
                type="error"
                :closable="false"
              />
              <div class="constraint-summary-line">
                <span class="status-dot"></span>
                <span>已配置 {{ paperDraftForm.constrains.length }} 种题型，共 {{ totalQuestionCount }} 题</span>
                <i></i>
                <span>将基于 {{ selectedCollectionNames.length }} 个题集生成草稿</span>
              </div>
            </section>
          </div>

          <aside class="paper-summary-column">
            <section class="summary-card">
              <div class="summary-title">
                <el-icon><Memo /></el-icon>
                <div>
                  <h4>配置摘要</h4>
                  <p>确认以下配置后，AI 将生成组卷草稿。</p>
                </div>
              </div>
              <div class="summary-list">
                <div class="summary-item">
                  <span>试卷来源题集</span>
                  <div class="summary-tags">
                    <el-tag v-if="!selectedCollectionNames.length" size="small" type="info" effect="plain">未选择</el-tag>
                    <el-tag v-for="name in selectedCollectionNames" v-else :key="name" size="small" effect="plain">{{ name }}</el-tag>
                  </div>
                </div>
                <div class="summary-item compact">
                  <span>预计题目总数</span>
                  <strong>{{ totalQuestionCount }} 题</strong>
                </div>
                <div class="summary-item compact">
                  <span>难度范围</span>
                  <strong>{{ difficultyRangeText }}</strong>
                </div>
                <div class="summary-item distribution-item">
                  <span>题型分布</span>
                  <div class="distribution-list">
                    <div v-for="item in typeDistribution" :key="item.key" class="distribution-row">
                      <span class="type-name">{{ item.label }}</span>
                      <span class="type-count">{{ item.count }} 题</span>
                      <span class="type-percent">{{ item.percent }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </section>

            <section class="summary-card generation-note-card">
              <div class="note-title">
                <el-icon><InfoFilled /></el-icon>
                <h4>生成说明</h4>
              </div>
              <ul>
                <li>AI 会根据组卷要求和题型约束筛选候选题。</li>
                <li>生成结果会进入下一步预览，你可以返回继续调整。</li>
                <li>如果候选题不理想，可以多次生成直到满足要求。</li>
              </ul>
            </section>
          </aside>
        </template>

        <div v-else class="paper-result-wrap">
          <div v-if="generatePaperDraftLoading" class="loading-wrap">
            <section class="paper-generating-card">
              <div class="generating-orbit" aria-hidden="true">
                <span></span>
              </div>
              <div class="generating-copy">
                <strong>{{ paperDraftGenerateStatusText || '正在生成组卷草稿' }}</strong>
                <p>AI 正在按题集、题型数量和难度范围匹配候选题，完成后会自动进入草稿预览。</p>
              </div>
              <div class="paper-generating-steps">
                <span v-for="(tip, index) in paperDraftGeneratingTips" :key="tip">
                  <i>{{ index + 1 }}</i>
                  {{ tip }}
                </span>
              </div>
              <div class="generating-summary">
                <span>{{ selectedCollectionNames.length || 0 }} 个题集</span>
                <span>{{ totalQuestionCount }} 道目标题量</span>
                <span>{{ difficultyRangeText }}</span>
              </div>
            </section>
          </div>
          <template v-else-if="paperDraftErrorMessage">
            <section class="paper-error-card">
              <el-icon><WarningFilled /></el-icon>
              <h4>{{ isPaperShortageError ? '题目数量不足' : '生成失败' }}</h4>
              <p>{{ paperDraftErrorMessage }}</p>
            </section>
          </template>
          <template v-else-if="paperDraftResult">
            <PaperDraftResultPreview
              :reason="paperDraftResult.reason"
              :source-names="selectedCollectionNames"
              :constraints="paperDraftForm.constrains"
              :groups="paperDraftPreviewGroups"
              :expected-total="paperDraftExpectedTotal"
              :actual-total="paperDraftActualTotal"
              :shortage-message="paperDraftShortageMessage"
              :loading-details="paperDraftDetailLoading"
              :detail-error-message="paperDraftDetailErrorMessage"
              :type-label="typeLabel"
            />
          </template>
          <el-empty v-else description="请先生成组卷草稿" />
        </div>
      </div>

      <div class="paper-footer">
        <template v-if="paperDraftStep === 1">
          <el-button @click="visibleModel = false">取消</el-button>
          <el-button
            type="primary"
            :loading="generatePaperDraftLoading"
            :disabled="generatePaperDraftLoading || Boolean(paperDraftConstraintLevelError)"
            @click="$emit('generate')"
          >
            <span>{{ generatePaperDraftLoading ? '正在生成...' : '生成草稿' }}</span>
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </template>
        <template v-else>
          <el-button :loading="generatePaperDraftLoading" :disabled="generatePaperDraftLoading" @click="$emit('generate')">
            {{ generatePaperDraftLoading ? '正在生成...' : '重新生成' }}
          </el-button>
          <el-button :disabled="generatePaperDraftLoading" @click="$emit('back-to-requirement')">返回修改需求</el-button>
          <el-button
            type="primary"
            :loading="savePaperDraftLoading"
            :disabled="!canSavePaperDraft || generatePaperDraftLoading"
            @click="$emit('save-paper')"
          >
            保存为试卷
          </el-button>
        </template>
      </div>
    </section>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { ArrowRight, Close, Delete, InfoFilled, MagicStick, Memo, Plus, WarningFilled } from '@element-plus/icons-vue';
import type { CollectionView } from '../../types/collection';
import type { AgentPaperDraftVO, CandidateQuestionVO, GeneratePaperDraftReq, PaperDraftPreviewGroup } from '../../types/ai';
import type { AgentQuestionTypeOption } from '../../types/agent-demo';
import PaperDraftResultPreview from './PaperDraftResultPreview.vue';

const props = defineProps<{
  visible: boolean;
  collectionLoading: boolean;
  collectionOptions: CollectionView[];
  questionTypeOptions: AgentQuestionTypeOption[];
  paperDraftStep: 1 | 2;
  generatePaperDraftLoading: boolean;
  savePaperDraftLoading: boolean;
  paperDraftErrorMessage: string;
  paperDraftDetailLoading: boolean;
  paperDraftDetailErrorMessage: string;
  paperDraftResult: AgentPaperDraftVO | null;
  paperDraftForm: GeneratePaperDraftReq;
  paperDraftCandidateQuestions: CandidateQuestionVO[];
  paperDraftPreviewGroups: PaperDraftPreviewGroup[];
  paperDraftExpectedTotal: number;
  paperDraftActualTotal: number;
  paperDraftShortageMessage: string;
  paperDraftConstraintLevelError: string;
  paperDraftGeneratingTips: string[];
  paperDraftGenerateStatusText: string;
  canSavePaperDraft: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void;
  (e: 'add-constrain'): void;
  (e: 'remove-constrain', index: number): void;
  (e: 'generate'): void;
  (e: 'save-paper'): void;
  (e: 'back-to-requirement'): void;
}>();

const visibleModel = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value)
});

const isPaperShortageError = computed(() => props.paperDraftErrorMessage.includes('题目数量不足'));

const typeLabelMap = computed(() => Object.fromEntries(props.questionTypeOptions.map((item) => [item.code, item.label])) as Record<string, string>);

const selectedCollectionNames = computed(() => {
  const selectedIds = new Set(props.paperDraftForm.collectionIds);
  return props.collectionOptions.filter((item) => selectedIds.has(item.collectionId)).map((item) => item.name);
});

const totalQuestionCount = computed(() => props.paperDraftForm.constrains.reduce((sum, item) => sum + (Number(item.count) || 0), 0));

const difficultyRangeText = computed(() => {
  const ranges = props.paperDraftForm.constrains
    .map((item) => [Number(item.difficultyMin), Number(item.difficultyMax)] as const)
    .filter(([min, max]) => Number.isFinite(min) && Number.isFinite(max));
  if (!ranges.length) {
    return '-';
  }
  const min = Math.min(...ranges.map(([value]) => value));
  const max = Math.max(...ranges.map(([, value]) => value));
  return `${min.toFixed(2)} ~ ${max.toFixed(2)}`;
});

const typeDistribution = computed(() => {
  const total = totalQuestionCount.value;
  return props.paperDraftForm.constrains.map((item, index) => {
    const count = Number(item.count) || 0;
    const percent = total > 0 ? `${((count / total) * 100).toFixed(1)}%` : '0.0%';
    return {
      key: `${item.typeCode}_${index}`,
      label: typeLabel(item.typeCode),
      count,
      percent
    };
  });
});

function typeLabel(code?: string) {
  if (!code) {
    return '未知题型';
  }
  return typeLabelMap.value[code] ?? code;
}

function applyRequirementPreset(text: string) {
  const current = props.paperDraftForm.message.trim();
  if (!current) {
    props.paperDraftForm.message = text;
    return;
  }
  if (!current.includes(text)) {
    props.paperDraftForm.message = `${current}，${text}`;
  }
}
</script>

<style scoped>
:deep(.paper-draft-drawer .el-drawer__body) {
  padding: 0;
  background: #f6f8fc;
}

.paper-drawer {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f6f8fc;
  color: #1f2937;
}

.paper-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 26px 10px;
  border-bottom: 1px solid #e6edf7;
  background: #fff;
}

.paper-header h3 {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 22px;
  line-height: 1.2;
  color: #182033;
}

.paper-header h3 :deep(.el-icon) {
  color: var(--el-color-primary);
  font-size: 20px;
}

.paper-header p {
  margin: 8px 0 0;
  color: #6b7587;
  font-size: 13px;
}

.close-btn {
  border-radius: 10px;
}

.paper-steps {
  margin: 10px 26px 0;
  padding: 8px 18px;
  border-radius: 12px;
  background: #eef3fb;
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 18px;
}

.paper-step {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #687385;
}

.paper-step.active {
  color: #1f2937;
}

.step-index {
  width: 34px;
  height: 34px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  background: #8793a8;
  color: #fff;
  font-weight: 700;
}

.paper-step.active .step-index {
  background: var(--el-color-primary);
  box-shadow: 0 8px 18px rgba(47, 125, 244, 0.2);
}

.paper-step strong,
.paper-step small {
  display: block;
}

.paper-step strong {
  font-size: 14px;
}

.paper-step small {
  margin-top: 3px;
  font-size: 12px;
  color: #7b8495;
}

.step-arrow {
  color: #7e8a9e;
  font-size: 24px;
}

.paper-body {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 14px;
  padding: 10px 26px 12px;
  overflow: hidden;
}

.paper-config-column {
  flex: 1 1 auto;
  min-width: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-right: 2px;
}

.paper-summary-column {
  width: 390px;
  flex: 0 0 390px;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.paper-section,
.summary-card {
  border: 1px solid #e4ebf5;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
}

.paper-section {
  padding: 12px 16px;
}

.section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.inline-heading {
  margin-bottom: 8px;
}

.section-heading h4 {
  margin: 0;
  font-size: 15px;
  color: #182033;
}

.section-heading p {
  margin: 3px 0 0;
  color: #7b8495;
  font-size: 12px;
}

.requirement-section :deep(.el-textarea__inner) {
  min-height: 70px !important;
  border-radius: 10px;
  line-height: 1.7;
}

.quick-requirements {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
}

.quick-requirements button {
  height: 26px;
  padding: 0 13px;
  border: 1px solid #dbe4f0;
  border-radius: 9px;
  background: #f8fafd;
  color: #536073;
  font-size: 12px;
  cursor: pointer;
  transition: color 0.18s ease, border-color 0.18s ease, background-color 0.18s ease;
}

.quick-requirements button:hover {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary-light-5);
  background: #f0f7ff;
}

.paper-section :deep(.el-select) {
  width: 100%;
}

.paper-constrain-list {
  width: 100%;
  border: 1px solid #eef2f7;
  border-radius: 12px;
  overflow: hidden;
}

.paper-constrain-head,
.paper-constrain-row {
  display: grid;
  grid-template-columns: minmax(120px, 1fr) 116px minmax(230px, 1.5fr) 64px;
  align-items: center;
  gap: 12px;
}

.paper-constrain-head {
  padding: 7px 12px;
  background: #f8fafd;
  color: #7b8495;
  font-size: 12px;
  font-weight: 600;
}

.paper-constrain-row {
  padding: 5px 12px;
  background: #fff;
  border-top: 1px solid #eef2f7;
}

.paper-constrain-cell {
  min-width: 0;
}

.paper-constrain-label {
  display: none;
  margin-bottom: 4px;
  font-size: 12px;
  color: #7b8495;
}

.paper-constrain-row :deep(.el-input-number),
.paper-constrain-row :deep(.el-select) {
  width: 100%;
}

.difficulty-range {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 10px;
  color: #8a94a6;
}

.action-cell {
  display: flex;
  justify-content: flex-end;
}

.delete-btn {
  width: 32px;
  height: 32px;
  padding: 0;
  color: #7b8495;
}

.delete-btn:not(.is-disabled):hover {
  color: var(--el-color-danger);
  background: var(--el-color-danger-light-9);
}

.paper-constrain-actions {
  margin-top: 9px;
}

.paper-constrain-actions :deep(.el-button) {
  border-style: dashed;
  border-radius: 10px;
}

.picker-level-error {
  margin-top: 12px;
}

.constraint-summary-line {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #edf2f8;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  color: #667085;
  font-size: 12px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #19b472;
  box-shadow: 0 0 0 4px rgba(25, 180, 114, 0.1);
}

.constraint-summary-line i {
  width: 1px;
  height: 12px;
  background: #d6deea;
}

.summary-card {
  padding: 20px;
}

.summary-title {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eef2f7;
}

.summary-title > :deep(.el-icon) {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: #edf5ff;
  color: var(--el-color-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}

.summary-title h4,
.note-title h4 {
  margin: 0;
  font-size: 17px;
  color: #1f2937;
}

.summary-title p {
  margin: 5px 0 0;
  color: #7b8495;
  font-size: 12px;
}

.summary-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-top: 16px;
}

.summary-item {
  display: grid;
  grid-template-columns: 110px minmax(0, 1fr);
  gap: 14px;
  align-items: flex-start;
  color: #667085;
  font-size: 13px;
}

.summary-item.compact {
  align-items: center;
}

.summary-item strong {
  color: #526075;
  font-size: 15px;
  font-weight: 700;
}

.summary-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.distribution-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.distribution-row {
  display: grid;
  grid-template-columns: 1fr auto auto;
  gap: 10px;
  align-items: center;
  color: #526075;
}

.type-name {
  color: #1f2937;
  font-weight: 600;
}

.type-count,
.type-percent {
  color: #7b8495;
  font-size: 12px;
}

.generation-note-card {
  border-color: #d8e8ff;
  background: #f8fbff;
}

.note-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.note-title :deep(.el-icon) {
  color: var(--el-color-primary);
  font-size: 18px;
}

.generation-note-card ul {
  margin: 14px 0 0;
  padding-left: 18px;
  color: #667085;
  line-height: 1.9;
  font-size: 13px;
}

.paper-result-wrap {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.loading-wrap {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 18px;
}

.paper-generating-card {
  width: min(620px, 100%);
  padding: 30px 32px;
  border: 1px solid #dce9fb;
  border-radius: 18px;
  background:
    radial-gradient(circle at 14% 10%, rgba(47, 125, 244, 0.1), transparent 34%),
    linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  box-shadow: 0 18px 45px rgba(28, 62, 120, 0.08);
}

.generating-orbit {
  width: 54px;
  height: 54px;
  border-radius: 18px;
  background: #eaf3ff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  position: relative;
  color: var(--el-color-primary);
}

.generating-orbit::before {
  content: '';
  position: absolute;
  inset: 11px;
  border: 2px solid rgba(47, 125, 244, 0.2);
  border-top-color: var(--el-color-primary);
  border-radius: 999px;
  animation: paper-spin 1s linear infinite;
}

.generating-orbit span {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: var(--el-color-primary);
  box-shadow: 0 0 0 8px rgba(47, 125, 244, 0.12);
}

.generating-copy {
  margin-top: 18px;
}

.generating-copy strong {
  display: block;
  color: #182033;
  font-size: 20px;
}

.generating-copy p {
  margin: 8px 0 0;
  color: #667085;
  line-height: 1.7;
  font-size: 13px;
}

.paper-generating-steps {
  margin-top: 22px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.paper-generating-steps span {
  min-height: 58px;
  padding: 12px;
  border: 1px solid #e1eaf7;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.82);
  color: #526075;
  font-size: 13px;
  font-weight: 600;
}

.paper-generating-steps i {
  width: 22px;
  height: 22px;
  margin-right: 8px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #eef5ff;
  color: var(--el-color-primary);
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
}

.generating-summary {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid #e6edf7;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.generating-summary span {
  padding: 5px 10px;
  border-radius: 999px;
  background: #f1f6fd;
  color: #667085;
  font-size: 12px;
}

@keyframes paper-spin {
  to {
    transform: rotate(360deg);
  }
}

.paper-error-card {
  margin: auto;
  width: min(520px, 100%);
  padding: 32px 28px;
  border: 1px solid #f5d7bf;
  border-radius: 14px;
  background: #fffaf6;
  text-align: center;
  box-shadow: 0 12px 28px rgba(154, 83, 30, 0.08);
}

.paper-error-card > :deep(.el-icon) {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  background: #fff0df;
  color: #e8891c;
  font-size: 22px;
}

.paper-error-card h4 {
  margin: 14px 0 8px;
  color: #1f2937;
  font-size: 18px;
}

.paper-error-card p {
  margin: 0;
  color: #667085;
  line-height: 1.7;
}

.paper-reason-card {
  border: 1px solid #e4ebf5;
  border-radius: 12px;
}

.paper-reason-text {
  margin: 0;
  color: var(--el-text-color-primary);
  line-height: 1.6;
  white-space: pre-wrap;
}

.paper-result-table {
  border-radius: 12px;
  overflow: hidden;
}

.paper-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 10px 26px 12px;
  border-top: 1px solid #e6edf7;
  background: #fff;
}

.paper-footer :deep(.el-button) {
  min-width: 104px;
  height: 36px;
  border-radius: 10px;
  font-weight: 600;
}

.paper-footer :deep(.el-button--primary) {
  min-width: 136px;
}

@media (max-width: 1024px) {
  .paper-body {
    flex-direction: column;
    overflow-y: auto;
  }

  .paper-config-column,
  .paper-summary-column {
    width: 100%;
    flex: none;
    overflow: visible;
  }

  .paper-constrain-head {
    display: none;
  }

  .paper-constrain-row {
    grid-template-columns: 1fr;
    gap: 8px;
    border-top: 1px solid #eef2f7;
  }

  .paper-constrain-label {
    display: inline-block;
  }

  .action-cell {
    justify-content: flex-start;
  }

  .paper-footer {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>
