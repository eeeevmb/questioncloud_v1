<template>
  <el-drawer v-model="visibleModel" size="980px" :with-header="false">
    <section class="create-drawer">
      <div class="create-header">
        <div>
          <h3>AI 出题</h3>
          <p>先选择题集，再生成并确认题目草稿。</p>
        </div>
        <div class="create-header-actions">
          <el-button text @click="visibleModel = false">关闭</el-button>
        </div>
      </div>

      <div class="create-mode-switch">
        <el-button :type="createMode === 'ai' ? 'primary' : 'default'" @click="$emit('activate-ai-mode')">AI 出题</el-button>
      </div>

      <template v-if="createMode === 'ai'">
        <el-alert type="info" :closable="false" class="create-tip" title="先描述出题方向，AI 生成草稿后再确认入库。" />

        <el-steps :active="aiCreateStep - 1" finish-status="success" simple class="create-steps">
          <el-step title="描述出题需求" />
          <el-step title="编辑草稿并确认创建" />
        </el-steps>

        <div class="ai-step-content">
          <el-form v-if="aiCreateStep === 1" label-position="top" class="create-form ai-requirement-form">
            <el-form-item label="题集">
              <el-select
                v-model="collectionIdModel"
                filterable
                clearable
                placeholder="请选择题集"
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

            <el-form-item label="知识点 / 出题方向">
              <el-input
                v-model="aiCreateForm.topic"
                type="textarea"
                :rows="4"
                maxlength="2000"
                show-word-limit
                placeholder="例如：导数在函数单调性中的应用"
              />
            </el-form-item>

            <el-form-item label="题型">
              <el-select v-model="aiCreateForm.questionType">
                <el-option v-for="type in questionTypeOptions" :key="type.code" :label="type.label" :value="type.code" />
              </el-select>
            </el-form-item>

            <el-form-item label="适用场景（可选）">
              <el-input
                v-model="aiCreateForm.scenario"
                maxlength="120"
                show-word-limit
                placeholder="例如：高一随堂练习 / 面试基础筛查"
              />
            </el-form-item>

            <el-form-item label="额外要求（可选）">
              <el-input
                v-model="aiCreateForm.requirements"
                type="textarea"
                :rows="4"
                maxlength="2000"
                show-word-limit
                placeholder="例如：避免计算量过大，强调思路引导"
              />
            </el-form-item>
          </el-form>

          <div v-else class="draft-workbench">
            <el-card shadow="never" class="draft-editor-card split-card">
              <template #header>草稿编辑</template>
              <el-form label-position="top" class="draft-editor-form">
                <el-form-item label="题型">
                  <el-select v-model="aiDraftForm.typeCode" disabled>
                    <el-option v-for="type in questionTypeOptions" :key="type.code" :label="type.label" :value="type.code" />
                  </el-select>
                </el-form-item>
                <el-form-item label="标题（可选）">
                  <el-input v-model="aiDraftForm.title" placeholder="请输入题目标题" />
                </el-form-item>
                <el-form-item label="难度（可选）">
                  <el-input-number
                    v-model="aiDraftForm.difficulty"
                    :min="0"
                    :max="1"
                    :step="0.01"
                    :precision="2"
                    controls-position="right"
                  />
                </el-form-item>
                <el-form-item label="题干">
                  <el-input
                    v-model="aiDraftForm.stem"
                    type="textarea"
                    :rows="5"
                    maxlength="4000"
                    show-word-limit
                    placeholder="支持 Markdown 与 LaTeX，例如：$x^2$"
                  />
                </el-form-item>

                <template v-if="isAiDraftChoiceType">
                  <div class="draft-options-head">
                    <span>选项</span>
                    <el-button size="small" @click="$emit('add-ai-draft-option')">新增选项</el-button>
                  </div>
                  <div v-for="(option, index) in aiDraftForm.options" :key="`${option.key}_${index}`" class="draft-option-edit-row">
                    <el-tag effect="plain">{{ option.key }}</el-tag>
                    <el-input
                      v-model="option.content"
                      type="textarea"
                      :rows="2"
                      placeholder="请输入选项内容（支持 Markdown / LaTeX）"
                    />
                    <el-button
                      size="small"
                      type="danger"
                      plain
                      :disabled="aiDraftForm.options.length <= 2"
                      @click="$emit('remove-ai-draft-option', index)"
                    >
                      删除
                    </el-button>
                  </div>

                  <el-form-item v-if="isAiDraftSingleChoice" label="正确答案">
                    <el-radio-group v-model="aiDraftSingleCorrectModel">
                      <el-radio v-for="option in aiDraftForm.options" :key="option.key" :label="option.key">
                        {{ option.key }}
                      </el-radio>
                    </el-radio-group>
                  </el-form-item>
                  <el-form-item v-else label="正确答案">
                    <el-checkbox-group v-model="aiDraftForm.correctOptions">
                      <el-checkbox v-for="option in aiDraftForm.options" :key="option.key" :label="option.key">
                        {{ option.key }}
                      </el-checkbox>
                    </el-checkbox-group>
                  </el-form-item>
                </template>

                <template v-else-if="isAiDraftTrueFalseType">
                  <el-form-item label="正确答案">
                    <el-radio-group v-model="aiDraftForm.judgeAnswer">
                      <el-radio label="T">正确</el-radio>
                      <el-radio label="F">错误</el-radio>
                    </el-radio-group>
                  </el-form-item>
                </template>

                <template v-else>
                  <el-form-item label="参考答案（可选）">
                    <el-input
                      v-model="aiDraftForm.answer"
                      type="textarea"
                      :rows="3"
                      maxlength="3000"
                      show-word-limit
                      placeholder="请输入参考答案"
                    />
                  </el-form-item>
                </template>

                <el-form-item label="解析（可选）">
                  <el-input
                    v-model="aiDraftForm.solution"
                    type="textarea"
                    :rows="5"
                    maxlength="4000"
                    show-word-limit
                    placeholder="支持 Markdown 与 LaTeX"
                  />
                </el-form-item>
              </el-form>
            </el-card>

            <el-card shadow="never" class="draft-preview-card split-card">
              <template #header>预览效果</template>
              <div class="draft-preview-content">
                <div class="draft-item">
                  <p class="draft-label">标题</p>
                  <p class="draft-value">{{ draftPreviewTitle || '（未填写）' }}</p>
                </div>
                <div class="draft-item">
                  <p class="draft-label">题干</p>
                  <AssistantMessageContent :content="draftPreviewStem" />
                </div>
                <div v-if="draftPreviewOptions.length" class="draft-item">
                  <p class="draft-label">选项</p>
                  <ol class="draft-preview-options">
                    <li v-for="(option, idx) in draftPreviewOptions" :key="`${option.key}_${idx}`">
                      <span class="option-key">{{ option.key }}.</span>
                      <AssistantMessageContent :content="option.content" />
                    </li>
                  </ol>
                </div>
                <div class="draft-item">
                  <p class="draft-label">正确答案</p>
                  <AssistantMessageContent :content="draftPreviewCorrectAnswer || '（未填写）'" />
                </div>
                <div class="draft-item">
                  <p class="draft-label">解析</p>
                  <AssistantMessageContent :content="draftPreviewSolution || '（未填写）'" />
                </div>
                <div v-if="draftPreviewAssumptions" class="draft-item">
                  <p class="draft-label">假设说明</p>
                  <AssistantMessageContent :content="draftPreviewAssumptions" />
                </div>
              </div>
            </el-card>
          </div>
        </div>

        <div class="create-footer">
          <template v-if="aiCreateStep === 1">
            <el-button @click="visibleModel = false">取消</el-button>
            <el-button
              type="primary"
              :loading="createAiDraftLoading"
              :disabled="!hasCreateCollectionContext"
              @click="$emit('generate-ai-draft')"
            >
              生成题目草稿
            </el-button>
          </template>
          <template v-else>
            <el-button :loading="createAiDraftLoading" :disabled="!hasCreateCollectionContext" @click="$emit('generate-ai-draft')">
              重新生成
            </el-button>
            <el-button @click="$emit('back-to-ai-requirement')">返回修改需求</el-button>
            <el-button
              type="primary"
              :disabled="!hasCreateCollectionContext"
              :loading="createSubmitting"
              @click="$emit('create-question-from-ai-draft')"
            >
              直接创建
            </el-button>
          </template>
        </div>
      </template>

      <template v-else>
        <el-alert type="info" :closable="false" class="create-tip" :title="createGuideText" />
        <el-form label-position="top" class="create-form">
          <div class="create-grid two-col">
            <el-form-item label="题型">
              <el-select v-model="createForm.questionType" @change="$emit('change-create-type')">
                <el-option v-for="type in questionTypeOptions" :key="type.code" :label="type.label" :value="type.code" />
              </el-select>
            </el-form-item>
            <el-form-item label="难度（可选）">
              <el-input-number
                v-model="createForm.difficulty"
                :min="0"
                :max="1"
                :step="0.01"
                :precision="2"
                controls-position="right"
              />
            </el-form-item>
          </div>

          <el-form-item label="标题（可选）">
            <el-input v-model="createForm.title" placeholder="例如：导数应用基础题" />
          </el-form-item>

          <el-form-item label="题干">
            <el-input
              v-model="createForm.stem"
              type="textarea"
              :rows="4"
              maxlength="3000"
              show-word-limit
              placeholder="请输入题干内容"
            />
          </el-form-item>

          <el-card v-if="isCreateChoiceType" shadow="never" class="create-subcard">
            <template #header>
              <div class="create-subhead">
                <span>选项设置</span>
                <el-button size="small" @click="$emit('add-create-option')">新增选项</el-button>
              </div>
            </template>
            <div v-for="(option, index) in createForm.options" :key="index" class="create-option-row">
              <el-form-item label="选项编号" class="option-key-field">
                <el-input v-model="option.key" placeholder="A / B / C" />
              </el-form-item>
              <el-form-item label="选项内容" class="option-content-field">
                <el-input v-model="option.content" type="textarea" :rows="2" placeholder="请输入选项内容" />
              </el-form-item>
              <div class="option-ops">
                <el-radio v-if="isCreateSingleChoice" v-model="createSingleCorrectModel" :label="option.key">设为正确</el-radio>
                <el-checkbox
                  v-else
                  :model-value="createForm.choiceCorrect.includes(option.key)"
                  @change="$emit('toggle-create-multiple', option.key)"
                >
                  设为正确
                </el-checkbox>
                <el-button size="small" type="danger" plain :disabled="createForm.options.length <= 2" @click="$emit('remove-create-option', index)">
                  删除
                </el-button>
              </div>
            </div>
          </el-card>

          <el-card v-if="isCreateTrueFalseType" shadow="never" class="create-subcard">
            <template #header>判断题答案</template>
            <el-radio-group v-model="createForm.judgeAnswer">
              <el-radio label="T">正确</el-radio>
              <el-radio label="F">错误</el-radio>
            </el-radio-group>
          </el-card>

          <el-form-item v-if="!isCreateChoiceType && !isCreateTrueFalseType" label="参考答案（可选）">
            <el-input
              v-model="createForm.answer"
              type="textarea"
              :rows="3"
              maxlength="2000"
              show-word-limit
              placeholder="可以先留空，后续再补"
            />
          </el-form-item>

          <el-form-item label="解析（可选）">
            <el-input
              v-model="createForm.solution"
              type="textarea"
              :rows="3"
              maxlength="3000"
              show-word-limit
              placeholder="可选，后续也可补充"
            />
          </el-form-item>
        </el-form>

        <div class="create-footer">
          <el-button @click="visibleModel = false">取消</el-button>
          <el-button type="primary" :loading="createSubmitting" :disabled="!hasCreateCollectionContext" @click="$emit('create-question')">
            直接创建
          </el-button>
        </div>
      </template>
    </section>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import AssistantMessageContent from '../AssistantMessageContent.vue';
import type { CollectionView } from '../../types/collection';
import type { QuestionOption } from '../../types/question';
import type {
  AgentQuestionTypeOption,
  AiCreateFormState,
  AiDraftFormState,
  CreateQuestionFormState
} from '../../types/agent-demo';

const props = defineProps<{
  visible: boolean;
  collectionLoading: boolean;
  collectionOptions: CollectionView[];
  questionTypeOptions: AgentQuestionTypeOption[];
  createQuestionCollectionId: string;
  createMode: 'ai' | 'manual';
  aiCreateStep: 1 | 2;
  createAiDraftLoading: boolean;
  createSubmitting: boolean;
  hasCreateCollectionContext: boolean;
  aiCreateForm: AiCreateFormState;
  aiDraftForm: AiDraftFormState;
  createForm: CreateQuestionFormState;
  isAiDraftChoiceType: boolean;
  isAiDraftSingleChoice: boolean;
  isAiDraftTrueFalseType: boolean;
  isCreateChoiceType: boolean;
  isCreateSingleChoice: boolean;
  isCreateTrueFalseType: boolean;
  createGuideText: string;
  draftPreviewTitle: string;
  draftPreviewStem: string;
  draftPreviewSolution: string;
  draftPreviewOptions: QuestionOption[];
  draftPreviewCorrectAnswer: string;
  draftPreviewAssumptions: string;
  createSingleCorrect: string;
  aiDraftSingleCorrect: string;
}>();

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void;
  (e: 'update:createQuestionCollectionId', value: string): void;
  (e: 'update:createSingleCorrect', value: string): void;
  (e: 'update:aiDraftSingleCorrect', value: string): void;
  (e: 'activate-ai-mode'): void;
  (e: 'add-ai-draft-option'): void;
  (e: 'remove-ai-draft-option', index: number): void;
  (e: 'generate-ai-draft'): void;
  (e: 'back-to-ai-requirement'): void;
  (e: 'create-question-from-ai-draft'): void;
  (e: 'change-create-type'): void;
  (e: 'add-create-option'): void;
  (e: 'remove-create-option', index: number): void;
  (e: 'toggle-create-multiple', key?: string): void;
  (e: 'create-question'): void;
}>();

const visibleModel = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value)
});

const collectionIdModel = computed({
  get: () => props.createQuestionCollectionId,
  set: (value: string) => emit('update:createQuestionCollectionId', value)
});

const createSingleCorrectModel = computed({
  get: () => props.createSingleCorrect,
  set: (value: string) => emit('update:createSingleCorrect', value)
});

const aiDraftSingleCorrectModel = computed({
  get: () => props.aiDraftSingleCorrect,
  set: (value: string) => emit('update:aiDraftSingleCorrect', value)
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

.create-mode-switch {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
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

.ai-requirement-form {
  width: 100%;
}

.create-grid {
  display: grid;
  gap: 10px;
}

.create-grid.two-col {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.create-subcard {
  margin-bottom: 10px;
}

.create-subhead {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.create-option-row {
  border-top: 1px dashed var(--el-border-color-light);
  padding-top: 10px;
  margin-top: 10px;
  display: grid;
  gap: 10px;
  grid-template-columns: 120px 1fr auto;
  align-items: end;
}

.create-option-row:first-child {
  border-top: none;
  padding-top: 0;
  margin-top: 0;
}

.option-key-field,
.option-content-field {
  margin-bottom: 0;
}

.option-ops {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.draft-preview-card {
  border: 1px solid var(--el-border-color-light);
}

.draft-workbench {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 12px;
  overflow: hidden;
}

.draft-editor-card {
  border: 1px solid var(--el-border-color-light);
}

.split-card {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.split-card :deep(.el-card__body) {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.draft-editor-form,
.draft-preview-content {
  min-height: 100%;
}

.draft-options-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.draft-option-edit-row {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: start;
  gap: 8px;
  margin-bottom: 10px;
}

.draft-item {
  padding: 6px 0;
  border-bottom: 1px dashed var(--el-border-color-lighter);
}

.draft-item:last-child {
  border-bottom: none;
}

.draft-label {
  margin: 0 0 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.draft-value {
  margin: 0;
  color: var(--el-text-color-primary);
}

.draft-preview-options {
  margin: 0;
  padding-left: 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.draft-preview-options li {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.option-key {
  min-width: 18px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
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
  .create-grid.two-col {
    grid-template-columns: 1fr;
  }

  .draft-workbench {
    grid-template-columns: 1fr;
  }

  .split-card {
    height: auto;
  }

  .create-option-row {
    grid-template-columns: 1fr;
  }

  .draft-option-edit-row {
    grid-template-columns: 1fr;
  }

  .create-footer {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>