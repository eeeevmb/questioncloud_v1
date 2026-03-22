<template>
  <section class="chat-page">
      <el-container class="chat-layout">
      <el-aside class="session-aside" width="300px">
        <div class="aside-header">
          <div class="aside-title-wrap">
            <p class="aside-title">最近会话</p>
            <p class="aside-subtitle">继续你的题库学习</p>
          </div>
          <el-button type="primary" :disabled="streaming || sessionLoading" @click="createNewSession">新会话</el-button>
        </div>
        <div v-if="sessionLoading" class="session-loading">
          <el-skeleton :rows="6" animated />
        </div>
        <el-scrollbar v-else class="session-scroll">
          <el-empty v-if="!sessions.length" description="暂无会话，点击上方创建" />
          <div v-else class="session-list">
            <div
              v-for="(session, index) in sessions"
              :key="session.sessionId"
              class="session-item"
              :class="{ active: String(session.sessionId) === activeSessionId }"
            >
              <button
                class="session-main"
                type="button"
                :disabled="streaming && String(session.sessionId) !== activeSessionId"
                @click="selectSession(String(session.sessionId))"
              >
                <span class="title">{{ getSessionTitle(session, index) }}</span>
                <span class="preview">{{ getSessionPreview(String(session.sessionId)) }}</span>
                <div class="session-meta-line">
                  <span class="time">{{ formatSessionTime(session.updatedAt || session.createdAt) }}</span>
                </div>
              </button>
              <el-dropdown trigger="click" @command="onSessionCommand($event, session)" @click.stop>
                <el-button text class="session-more" @click.stop>···</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="rename">重命名</el-dropdown-item>
                    <el-dropdown-item command="delete">删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </el-scrollbar>
      </el-aside>

      <el-main class="chat-main">
        <el-card shadow="never" class="toolbar-card">
          <div class="workspace-toolbar">
            <div class="toolbar-actions">
              <el-button class="tool-btn" type="primary" :disabled="streaming" @click="handleToolbarSelectQuestions">选题学习</el-button>
              <el-button class="tool-btn-secondary" :disabled="streaming" @click="handleToolbarCreateQuestion">AI 出题</el-button>
              <el-button class="tool-btn-secondary" :disabled="streaming" @click="handleToolbarGeneratePaperDraft">AI 组卷</el-button>
            </div>
          </div>
        </el-card>

        <div ref="messageScrollRef" class="message-scroll">
          <div v-if="messageLoading" class="loading-wrap">
            <el-skeleton :rows="6" animated />
          </div>
          <div v-else-if="!activeMessages.length" class="empty-wrap">
            <el-empty description="开始你的题库学习">
              <p class="empty-tip">你可以先选题学习并直接讲解，或直接输入问题开始学习交流。</p>
              <div class="quick-prompts">
                <el-button
                  v-for="prompt in quickPrompts"
                  :key="prompt"
                  size="small"
                  @click="applyQuickPrompt(prompt)"
                >
                  {{ prompt }}
                </el-button>
              </div>
            </el-empty>
          </div>
          <div v-else class="message-list">
            <article v-for="item in activeMessages" :key="item.id" class="message-row" :class="item.role">
              <div class="message-bubble" :class="{ pending: item.status === 'pending', error: item.status === 'error' }">
                <div class="role">{{ roleLabel(item.role) }}</div>
                <template v-if="item.role === 'assistant'">
                  <div v-if="item.status === 'pending'" class="pending-text">AI 助手思考中…</div>
                  <AssistantMessageContent v-else :content="item.content" />
                </template>
                <pre v-else class="content">{{ item.content }}</pre>
              </div>
            </article>
          </div>
        </div>

        <footer class="composer-wrap">
          <el-card shadow="never" class="composer-card">
            <div class="composer-head">
              <span class="composer-title">学习提问</span>
            </div>
            <el-input
              v-model="composerMessage"
              type="textarea"
              :rows="2"
              resize="none"
              :disabled="!activeSessionId || streaming"
              placeholder="输入问题，直接开始学习交流"
              @keydown.enter.exact.prevent="handleSend"
            />
            <div class="composer-footer">
              <div class="action-group">
                <el-button :disabled="!streaming" @click="abortStreaming">停止生成</el-button>
                <el-button type="primary" :loading="streaming" :disabled="!canSend" @click="handleSend">发送</el-button>
              </div>
            </div>
          </el-card>
        </footer>
      </el-main>
    </el-container>

    <el-drawer v-model="questionPickerVisible" size="980px" :with-header="false">
      <section class="picker-drawer">
        <div class="picker-header">
          <div>
            <h3>选题学习</h3>
            <p>先选择题集，再检索并勾选题目。</p>
          </div>
          <div class="picker-header-actions">
            <el-button text @click="questionPickerVisible = false">关闭</el-button>
          </div>
        </div>

        <el-form label-position="top" class="picker-filter-form">
          <div class="picker-filter-grid">
            <el-form-item label="题集">
              <el-select
                v-model="questionPickerCollectionId"
                filterable
                clearable
                placeholder="请选择题集"
                :loading="collectionLoading"
                @change="onQuestionPickerCollectionChange"
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
                v-model="questionPickerQuery.keyword"
                clearable
                placeholder="输入题干/标题关键词"
                @keyup.enter="onQuestionFilterChange"
              />
            </el-form-item>
            <el-form-item label="题型">
              <el-select v-model="questionPickerQuery.typeCode" clearable placeholder="全部" @change="onQuestionFilterChange">
                <el-option v-for="type in questionTypeOptions" :key="type.code" :label="type.label" :value="type.code" />
              </el-select>
            </el-form-item>
            <el-form-item label="难度下限">
              <el-input-number
                v-model="questionPickerQuery.levelMin"
                :min="0"
                :max="1"
                :step="0.01"
                :precision="2"
                controls-position="right"
                @change="onQuestionFilterChange"
              />
            </el-form-item>
            <el-form-item label="难度上限">
              <el-input-number
                v-model="questionPickerQuery.levelMax"
                :min="0"
                :max="1"
                :step="0.01"
                :precision="2"
                controls-position="right"
                @change="onQuestionFilterChange"
              />
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
              :disabled="!questionPickerCollectionId"
              @click="loadQuestionCandidates"
            >
              搜索
            </el-button>
            <el-button :disabled="!questionPickerCollectionId" @click="handleResetQuestionPickerFilters">重置</el-button>
          </div>
        </el-form>

        <div class="picker-content">
          <div class="picker-list-panel">
            <div class="picker-list" v-loading="questionPickerLoading">
              <el-empty v-if="!questionPickerCollectionId" description="请先选择题集" />
              <el-empty v-else-if="!questionPickerRecords.length" description="暂无可选题目" />
              <div
                v-for="(question, index) in questionPickerRecords"
                :key="question.id"
                class="picker-item"
                :class="{ active: question.id === pickerActiveQuestionId }"
              >
                <el-checkbox
                  :model-value="isQuestionSelectedInDrawer(question.id)"
                  @change="(checked) => toggleDrawerQuestion(question, checked)"
                />
                <button class="picker-item-main" type="button" @click="selectQuestionForDetail(question)">
                  <p class="item-index">第 {{ questionDisplayNo(index) }} 题</p>
                  <p class="item-title" :title="question.title">{{ question.title || '（暂无题干摘要）' }}</p>
                  <div class="item-meta">
                    <el-tag size="small" effect="plain">{{ typeLabel(question.typeCode) }}</el-tag>
                    <el-tag size="small" type="success" effect="plain">{{ difficultyLabel(question.difficulty) }}</el-tag>
                  </div>
                </button>
                <el-button text size="small" class="picker-detail-trigger" @click="selectQuestionForDetail(question)">查看详情</el-button>
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

                <el-collapse v-model="pickerDetailCollapseNames" class="detail-collapse">
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

        <div class="picker-pagination" v-if="questionPickerPage">
          <span class="picker-page-summary">{{ pickerPageSummary }}</span>
          <el-pagination
            v-model:current-page="questionPickerQuery.pageNum"
            v-model:page-size="questionPickerQuery.pageSize"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            :total="questionPickerPage.total"
            :hide-on-single-page="false"
            :disabled="questionPickerLoading"
            @current-change="onQuestionPageChange"
            @size-change="onQuestionSizeChange"
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
              @click="applyAndExplainFromDrawer"
            >
              直接讲解
            </el-button>
          </div>
        </div>
      </section>
    </el-drawer>

    <el-drawer v-model="createQuestionDrawerVisible" size="980px" :with-header="false">
      <section class="create-drawer">
        <div class="create-header">
          <div>
            <h3>创建题目</h3>
            <p>先选择题集，再生成并确认题目草稿。</p>
          </div>
          <div class="picker-header-actions">
            <el-button text @click="createQuestionDrawerVisible = false">关闭</el-button>
          </div>
        </div>

        <div class="create-mode-switch">
          <el-button :type="createMode === 'ai' ? 'primary' : 'default'" @click="activateAiMode">AI 出题</el-button>
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
                  v-model="createQuestionCollectionId"
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
                      <el-button size="small" @click="addAiDraftOption">新增选项</el-button>
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
                        @click="removeAiDraftOption(index)"
                      >
                        删除
                      </el-button>
                    </div>

                    <el-form-item v-if="isAiDraftSingleChoice" label="正确答案">
                      <el-radio-group v-model="aiDraftSingleCorrect">
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
              <el-button @click="createQuestionDrawerVisible = false">取消</el-button>
              <el-button
                type="primary"
                :loading="createAiDraftLoading"
                :disabled="!hasCreateCollectionContext"
                @click="handleGenerateAiDraft"
              >
                生成题目草稿
              </el-button>
            </template>
            <template v-else>
              <el-button :loading="createAiDraftLoading" :disabled="!hasCreateCollectionContext" @click="handleGenerateAiDraft">
                重新生成
              </el-button>
              <el-button @click="backToAiRequirementForm">返回修改需求</el-button>
              <el-button
                type="primary"
                :disabled="!hasCreateCollectionContext"
                :loading="createSubmitting"
                @click="handleCreateQuestionFromAiDraft"
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
                <el-select v-model="createForm.questionType" @change="onCreateTypeChange">
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
                  <el-button size="small" @click="addCreateOption">新增选项</el-button>
                </div>
              </template>
              <div v-for="(option, index) in createForm.options" :key="index" class="create-option-row">
                <el-form-item label="选项编号" class="option-key">
                  <el-input v-model="option.key" placeholder="A / B / C" />
                </el-form-item>
                <el-form-item label="选项内容" class="option-content">
                  <el-input v-model="option.content" type="textarea" :rows="2" placeholder="请输入选项内容" />
                </el-form-item>
                <div class="option-ops">
                  <el-radio v-if="isCreateSingleChoice" v-model="createSingleCorrect" :label="option.key">设为正确</el-radio>
                  <el-checkbox
                    v-else
                    :model-value="createForm.choiceCorrect.includes(option.key)"
                    @change="toggleCreateMultiple(option.key)"
                  >
                    设为正确
                  </el-checkbox>
                  <el-button size="small" type="danger" plain :disabled="createForm.options.length <= 2" @click="removeCreateOption(index)">
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
            <el-button @click="createQuestionDrawerVisible = false">取消</el-button>
            <el-button type="primary" :loading="createSubmitting" :disabled="!hasCreateCollectionContext" @click="handleCreateQuestion">
              直接创建
            </el-button>
          </div>
        </template>
      </section>
    </el-drawer>

    <el-drawer v-model="paperDraftDrawerVisible" size="980px" :with-header="false">
      <section class="create-drawer paper-drawer">
        <div class="create-header">
          <div>
            <h3>AI 组卷</h3>
            <p>先填写组卷需求，再查看候选题目草稿。</p>
          </div>
          <div class="picker-header-actions">
            <el-button text @click="paperDraftDrawerVisible = false">关闭</el-button>
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
                      @click="removePaperDraftConstrain(index)"
                    >
                      删除
                    </el-button>
                  </div>
                </div>
              </div>
              <div class="paper-constrain-actions">
                <el-button @click="addPaperDraftConstrain">新增约束</el-button>
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
            <el-button @click="paperDraftDrawerVisible = false">取消</el-button>
            <el-button type="primary" :loading="generatePaperDraftLoading" @click="handleGenerateAgentPaperDraft">
              生成草稿
            </el-button>
          </template>
          <template v-else>
            <el-button :loading="generatePaperDraftLoading" @click="handleGenerateAgentPaperDraft">重新生成</el-button>
            <el-button @click="backToPaperRequirementForm">返回修改需求</el-button>
          </template>
        </div>
      </section>
    </el-drawer>

  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { ElMessageBox } from 'element-plus';
import {
  createAgentSession,
  deleteSession,
  fetchAgentSessions,
  fetchSessionMessages,
  streamAgentChat,
  updateSessionTitle,
  type AgentChatMessageVO,
  type AgentChatPayload,
  type AgentSessionVO
} from '../api/agent';
import { createQuestion, fetchQuestionDetail, type QuestionCreatePayload } from '../api/question';
import { generateAgentPaperDraft, generateQuestionDraft } from '../api/ai';
import { fetchCollections, fetchCollectionQuestions } from '../api/collection';
import type { CollectionView } from '../types/collection';
import type { QuestionDetail, QuestionOption, QuestionSummary, QuestionSummaryPage } from '../types/question';
import type {
  AgentPaperDraftVO,
  GeneratePaperDraftBucketConstrain,
  GeneratePaperDraftReq,
  GenerateQuestionDraftReq,
  QuestionDraft
} from '../types/ai';
import { showError, showInfo, showSuccess } from '../utils/messages';
import AssistantMessageContent from '../components/AssistantMessageContent.vue';

type MessageRole = 'user' | 'assistant' | 'system';
type ResultType = 'explain' | 'search' | 'generate' | 'general';

interface SendMessageOptions {
  toolContext?: {
    collectionId?: string;
    selectedQuestionIds?: string[];
  };
}

interface MessageItem {
  id: string;
  role: MessageRole;
  content: string;
  status?: 'pending' | 'streaming' | 'done' | 'error';
  resultType?: ResultType;
}

interface QuestionMeta {
  title: string;
  typeCode: string;
  difficulty: number | null;
}

interface QuestionPickerQueryState {
  pageNum: number;
  pageSize: number;
  keyword: string;
  typeCode: string;
  levelMin?: number;
  levelMax?: number;
}

interface CreateQuestionFormState {
  questionType: string;
  title: string;
  stem: string;
  difficulty: number | null;
  options: QuestionOption[];
  choiceCorrect: string[];
  judgeAnswer: 'T' | 'F';
  answer: string;
  solution: string;
}

interface AiCreateFormState {
  topic: string;
  questionType: string;
  scenario: string;
  requirements: string;
}

interface AiDraftFormState {
  typeCode: string;
  title: string;
  difficulty: number | null;
  stem: string;
  options: QuestionOption[];
  correctOptions: string[];
  judgeAnswer: 'T' | 'F' | '';
  answer: string;
  solution: string;
  assumptions: string;
}

const AGENT_NAME = '题库小助手';
const quickPrompts = ['找几道基础练习题', '搜索某个知识点题目', '讲解一道我不会的题'];

const questionTypeOptions = [
  { code: 'single-choice', label: '单选题' },
  { code: 'multiple-choice', label: '多选题' },
  { code: 'true-false', label: '判断题' },
  { code: 'fill-in', label: '填空题' },
  { code: 'short-answer', label: '简答题' }
];

const typeLabelMap = Object.fromEntries(questionTypeOptions.map((item) => [item.code, item.label])) as Record<string, string>;

const contextState = reactive({
  collectionIdValues: [] as string[]
});
const composerMessage = ref('');

const sessions = ref<AgentSessionVO[]>([]);
const activeSessionId = ref('');
const messagesBySession = reactive<Record<string, MessageItem[]>>({});
const streaming = ref(false);
const currentAbortController = ref<AbortController | null>(null);
const collectionLoading = ref(false);
const sessionLoading = ref(false);
const messageLoading = ref(false);
const collectionOptions = ref<CollectionView[]>([]);
const messageScrollRef = ref<HTMLElement | null>(null);

const questionPickerVisible = ref(false);
const questionPickerLoading = ref(false);
const questionPickerCollectionId = ref('');
const questionPickerPage = ref<QuestionSummaryPage | null>(null);
const pickerActiveQuestionId = ref('');
const pickerQuestionDetailLoadingId = ref('');
const pickerDetailCollapseNames = ref<string[]>([]);
const questionPickerQuery = reactive<QuestionPickerQueryState>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  typeCode: '',
  levelMin: undefined,
  levelMax: undefined
});

const drawerSelectedQuestionIds = ref<string[]>([]);
const selectedQuestionMetaMap = reactive<Record<string, QuestionMeta>>({});
const questionDetailMap = reactive<Record<string, QuestionDetail>>({});

const createQuestionDrawerVisible = ref(false);
const createQuestionCollectionId = ref('');
const createSubmitting = ref(false);
const createAiDraftLoading = ref(false);
const createMode = ref<'ai' | 'manual'>('ai');
const aiCreateStep = ref<1 | 2>(1);
const paperDraftDrawerVisible = ref(false);
const paperDraftStep = ref<1 | 2>(1);
const generatePaperDraftLoading = ref(false);
const paperDraftErrorMessage = ref('');
const paperDraftResult = ref<AgentPaperDraftVO | null>(null);
const paperDraftForm = reactive<GeneratePaperDraftReq>({
  message: '',
  collectionIds: [],
  constrains: [defaultPaperDraftConstrain()]
});
const createForm = reactive<CreateQuestionFormState>({
  questionType: 'single-choice',
  title: '',
  stem: '',
  difficulty: null,
  options: defaultCreateOptions(),
  choiceCorrect: [],
  judgeAnswer: 'T',
  answer: '',
  solution: ''
});
const aiCreateForm = reactive<AiCreateFormState>({
  topic: '',
  questionType: 'single-choice',
  scenario: '',
  requirements: ''
});
const aiDraftPreview = ref<QuestionDraft | null>(null);
const aiDraftForm = reactive<AiDraftFormState>(defaultAiDraftForm());

const currentCollectionId = computed(() => contextState.collectionIdValues[0] ?? '');
const hasCreateCollectionContext = computed(() => Boolean(createQuestionCollectionId.value));

const activeMessages = computed(() => {
  if (!activeSessionId.value) {
    return [];
  }
  return messagesBySession[activeSessionId.value] ?? [];
});

const canSend = computed(() => {
  if (streaming.value || !activeSessionId.value) {
    return false;
  }
  const hasMessage = Boolean(composerMessage.value.trim());
  return hasMessage;
});

const canExplainBase = computed(() => {
  if (streaming.value) {
    return false;
  }
  if (!activeSessionId.value) {
    return false;
  }
  return true;
});

const canApplyDrawerSelection = computed(() => Boolean(questionPickerCollectionId.value) && drawerSelectedQuestionIds.value.length > 0);

const questionPickerRecords = computed(() => questionPickerPage.value?.records ?? []);
const pickerPageSummary = computed(() => {
  const page = questionPickerPage.value;
  if (!page) {
    return '';
  }
  const total = Number.isFinite(page.total) ? page.total : 0;
  const current = page.pageNum || questionPickerQuery.pageNum || 1;
  const pageSize = page.pageSize || questionPickerQuery.pageSize || 10;
  const pages = Math.max(page.pages || Math.ceil(total / Math.max(pageSize, 1)) || 1, 1);
  return `共 ${total} 题 · 第 ${Math.min(current, pages)} / ${pages} 页`;
});
const pickerActiveQuestionDetail = computed(() => {
  if (!pickerActiveQuestionId.value) {
    return null;
  }
  return questionDetailMap[pickerActiveQuestionId.value] ?? null;
});

const pickerActiveQuestionNo = computed(() => {
  if (!pickerActiveQuestionId.value) {
    return '-';
  }
  const idx = questionPickerRecords.value.findIndex((item) => item.id === pickerActiveQuestionId.value);
  if (idx === -1) {
    return '-';
  }
  return String(questionDisplayNo(idx));
});

const pickerDetailOptions = computed(() => normalizeDraftOptions(pickerActiveQuestionDetail.value?.options ?? []));

const pickerDetailAnswer = computed(() => {
  const detail = pickerActiveQuestionDetail.value;
  if (!detail) {
    return '';
  }
  if (detail.typeCode === 'single-choice' || detail.typeCode === 'multiple-choice') {
    const keys = (detail.correctOptions ?? []).map((item) => normalizeText(item).toUpperCase()).filter(Boolean);
    if (keys.length) {
      return keys.join('、');
    }
  }
  if (detail.typeCode === 'true-false') {
    const judge = parseJudgeAnswer(detail.judgeAnswer ?? detail.answer);
    if (judge === 'T') {
      return '正确';
    }
    if (judge === 'F') {
      return '错误';
    }
  }
  return normalizeText(detail.answer) || normalizeText(detail.answerKey);
});

const pickerDetailSolution = computed(() => normalizeText(pickerActiveQuestionDetail.value?.solution));

const pickerDetailLoading = computed(
  () => Boolean(pickerActiveQuestionId.value) && pickerQuestionDetailLoadingId.value === pickerActiveQuestionId.value
);

const questionPickerLevelError = computed(() => {
  if (
    questionPickerQuery.levelMin !== undefined &&
    questionPickerQuery.levelMax !== undefined &&
    questionPickerQuery.levelMin > questionPickerQuery.levelMax
  ) {
    return '难度下限不能大于上限';
  }
  return '';
});

const drawerSelectedPreview = computed(() =>
  drawerSelectedQuestionIds.value.slice(0, 4).map((id, index) => {
    const meta = selectedQuestionMetaMap[id];
    return meta?.title ? clipText(meta.title, 12) : `第${index + 1}题`;
  })
);

const isCreateChoiceType = computed(
  () => createForm.questionType === 'single-choice' || createForm.questionType === 'multiple-choice'
);
const isCreateSingleChoice = computed(() => createForm.questionType === 'single-choice');
const isCreateTrueFalseType = computed(() => createForm.questionType === 'true-false');

const createSingleCorrect = computed({
  get: () => createForm.choiceCorrect[0] ?? '',
  set: (value: string) => {
    createForm.choiceCorrect.splice(0, createForm.choiceCorrect.length);
    if (value?.trim()) {
      createForm.choiceCorrect.push(value.trim());
    }
  }
});

const createGuideText = computed(() => {
  if (isCreateChoiceType.value) {
    return '先填写题干和选项即可开始创建，正确答案和解析可继续完善。';
  }
  if (isCreateTrueFalseType.value) {
    return '请填写清晰的判断陈述，并选择正确或错误。';
  }
  return '先填写题干即可创建，答案和解析可以后续补充。';
});

const isAiDraftChoiceType = computed(
  () => aiDraftForm.typeCode === 'single-choice' || aiDraftForm.typeCode === 'multiple-choice'
);
const isAiDraftSingleChoice = computed(() => aiDraftForm.typeCode === 'single-choice');
const isAiDraftTrueFalseType = computed(() => aiDraftForm.typeCode === 'true-false');

const aiDraftSingleCorrect = computed({
  get: () => aiDraftForm.correctOptions[0] ?? '',
  set: (value: string) => {
    const normalized = normalizeText(value).toUpperCase();
    aiDraftForm.correctOptions = normalized ? [normalized] : [];
  }
});

const draftPreviewTitle = computed(() => normalizeText(aiDraftForm.title));
const draftPreviewStem = computed(() => normalizeText(aiDraftForm.stem));
const draftPreviewSolution = computed(() => normalizeText(aiDraftForm.solution));
const draftPreviewOptions = computed(() => normalizeDraftOptions(aiDraftForm.options));
const draftPreviewCorrectAnswer = computed(() => formatDraftCorrectAnswerFromForm());
const draftPreviewAssumptions = computed(() => normalizeText(aiDraftForm.assumptions));
const paperDraftCandidateQuestions = computed(() => paperDraftResult.value?.candidateQuestions ?? []);
const paperDraftConstraintLevelError = computed(() => {
  for (let i = 0; i < paperDraftForm.constrains.length; i += 1) {
    const item = paperDraftForm.constrains[i];
    if (
      typeof item?.difficultyMin === 'number' &&
      typeof item?.difficultyMax === 'number' &&
      item.difficultyMin > item.difficultyMax
    ) {
      return `第 ${i + 1} 条约束的难度下限不能大于上限`;
    }
  }
  return '';
});

onMounted(async () => {
  await Promise.all([loadCollections(), loadSessions()]);
  if (sessions.value.length) {
    await selectSession(String(sessions.value[0].sessionId));
  } else {
    await createNewSession();
  }
});

async function loadCollections() {
  collectionLoading.value = true;
  try {
    const list = await fetchCollections();
    collectionOptions.value = list;

    if (currentCollectionId.value && !list.some((item) => item.collectionId === currentCollectionId.value)) {
      contextState.collectionIdValues = [];
    }
    if (questionPickerCollectionId.value && !list.some((item) => item.collectionId === questionPickerCollectionId.value)) {
      questionPickerCollectionId.value = '';
      resetQuestionPickerState();
    }
    if (createQuestionCollectionId.value && !list.some((item) => item.collectionId === createQuestionCollectionId.value)) {
      createQuestionCollectionId.value = '';
    }
    if (paperDraftForm.collectionIds.length) {
      paperDraftForm.collectionIds = paperDraftForm.collectionIds.filter((id) =>
        list.some((item) => item.collectionId === id)
      );
    }
  } catch (error) {
    showError(error instanceof Error ? error.message : '加载题集失败');
  } finally {
    collectionLoading.value = false;
  }
}

async function loadSessions() {
  sessionLoading.value = true;
  try {
    const list = await fetchAgentSessions();
    sessions.value = sortSessions(list);
  } catch (error) {
    showError(error instanceof Error ? error.message : '加载会话列表失败');
  } finally {
    sessionLoading.value = false;
  }
}

function sortSessions(list: AgentSessionVO[]) {
  return [...list].sort((a, b) => {
    const aTime = Date.parse(a.updatedAt || a.createdAt || '') || 0;
    const bTime = Date.parse(b.updatedAt || b.createdAt || '') || 0;
    return bTime - aTime;
  });
}

async function createNewSession() {
  if (streaming.value) {
    return;
  }
  try {
    const session = await createAgentSession(AGENT_NAME);
    const sid = String(session.sessionId);
    sessions.value = sortSessions([session, ...sessions.value]);
    activeSessionId.value = sid;
    messagesBySession[sid] = [];
    composerMessage.value = '';
    await nextTick();
    scrollToBottom();
    showSuccess('已创建新会话');
  } catch (error) {
    showError(error instanceof Error ? error.message : '创建会话失败');
  }
}

async function selectSession(sessionId: string) {
  if (!sessionId || (streaming.value && sessionId !== activeSessionId.value)) {
    if (streaming.value) {
      showInfo('当前正在响应，请稍后切换会话');
    }
    return;
  }

  activeSessionId.value = sessionId;
  if (messagesBySession[sessionId]) {
    await nextTick();
    scrollToBottom();
    return;
  }

  messageLoading.value = true;
  try {
    const rawMessages = await fetchSessionMessages(sessionId);
    messagesBySession[sessionId] = mapServerMessages(rawMessages, sessionId);
  } catch (error) {
    showError(error instanceof Error ? error.message : '加载会话历史失败');
    messagesBySession[sessionId] = [];
  } finally {
    messageLoading.value = false;
    await nextTick();
    scrollToBottom();
  }
}

async function refreshSessionsKeepCurrent() {
  const current = activeSessionId.value;
  await loadSessions();
  if (!current) {
    return;
  }
  const exists = sessions.value.some((session) => String(session.sessionId) === current);
  if (!exists && sessions.value.length) {
    await selectSession(String(sessions.value[0].sessionId));
  }
}

async function handleSessionCommand(command: string, session: AgentSessionVO) {
  const sid = String(session.sessionId);

  if (command === 'rename') {
    let title = '';
    try {
      const result = await ElMessageBox.prompt('请输入新的会话标题（最多20字符）', '重命名会话', {
        inputValue: session.title ?? '',
        inputPattern: /^.{1,20}$/,
        inputErrorMessage: '标题长度需在 1-20 字符',
        confirmButtonText: '保存',
        cancelButtonText: '取消'
      });
      title = result.value;
    } catch {
      return;
    }

    try {
      await updateSessionTitle(sid, title);
      showSuccess('会话标题已更新');
      await refreshSessionsKeepCurrent();
    } catch (error) {
      showError(error instanceof Error ? error.message : '更新会话标题失败');
    }
    return;
  }

  if (command === 'delete') {
    try {
      await ElMessageBox.confirm('确认删除该会话？删除后无法恢复。', '删除会话', {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      });
    } catch {
      return;
    }

    try {
      await deleteSession(sid);
      delete messagesBySession[sid];
      if (activeSessionId.value === sid) {
        activeSessionId.value = '';
      }
      showSuccess('会话已删除');
      await loadSessions();
      if (!activeSessionId.value && sessions.value.length) {
        await selectSession(String(sessions.value[0].sessionId));
      }
    } catch (error) {
      showError(error instanceof Error ? error.message : '删除会话失败');
    }
  }
}

function onSessionCommand(command: string | number | object, session: AgentSessionVO) {
  void handleSessionCommand(String(command), session);
}

function ensureCollectionsLoaded() {
  if (!collectionOptions.value.length && !collectionLoading.value) {
    void loadCollections();
  }
}

function handleToolbarSelectQuestions() {
  ensureCollectionsLoaded();
  openQuestionPicker();
}

function handleToolbarCreateQuestion() {
  ensureCollectionsLoaded();
  openCreateQuestionDrawer();
}

function handleToolbarGeneratePaperDraft() {
  ensureCollectionsLoaded();
  openPaperDraftDrawer();
}

function openCreateQuestionDrawer() {
  createQuestionCollectionId.value = currentCollectionId.value;
  createMode.value = 'ai';
  aiCreateStep.value = 1;
  resetAiCreateForm();
  resetCreateForm();
  resetAiDraftForm();
  aiDraftPreview.value = null;
  createQuestionDrawerVisible.value = true;
}

function openPaperDraftDrawer() {
  resetPaperDraftForm();
  paperDraftResult.value = null;
  paperDraftErrorMessage.value = '';
  generatePaperDraftLoading.value = false;
  paperDraftStep.value = 1;
  paperDraftDrawerVisible.value = true;
}

function activateAiMode() {
  createMode.value = 'ai';
  aiCreateStep.value = aiDraftPreview.value ? 2 : 1;
}

function defaultPaperDraftConstrain(): GeneratePaperDraftBucketConstrain {
  return {
    typeCode: 'single-choice',
    count: 5,
    difficultyMin: 0,
    difficultyMax: 1
  };
}

function defaultCreateOptions(): QuestionOption[] {
  return [
    { key: 'A', content: '' },
    { key: 'B', content: '' }
  ];
}

function defaultAiDraftForm(): AiDraftFormState {
  return {
    typeCode: 'single-choice',
    title: '',
    difficulty: null,
    stem: '',
    options: defaultCreateOptions(),
    correctOptions: [],
    judgeAnswer: '',
    answer: '',
    solution: '',
    assumptions: ''
  };
}

function resetCreateForm() {
  createForm.questionType = 'single-choice';
  createForm.title = '';
  createForm.stem = '';
  createForm.difficulty = null;
  createForm.options = defaultCreateOptions();
  createForm.choiceCorrect = [];
  createForm.judgeAnswer = 'T';
  createForm.answer = '';
  createForm.solution = '';
}

function resetAiCreateForm() {
  aiCreateForm.topic = '';
  aiCreateForm.questionType = 'single-choice';
  aiCreateForm.scenario = '';
  aiCreateForm.requirements = '';
}

function resetAiDraftForm() {
  const next = defaultAiDraftForm();
  aiDraftForm.typeCode = next.typeCode;
  aiDraftForm.title = next.title;
  aiDraftForm.difficulty = next.difficulty;
  aiDraftForm.stem = next.stem;
  aiDraftForm.options = next.options;
  aiDraftForm.correctOptions = next.correctOptions;
  aiDraftForm.judgeAnswer = next.judgeAnswer;
  aiDraftForm.answer = next.answer;
  aiDraftForm.solution = next.solution;
  aiDraftForm.assumptions = next.assumptions;
}

function resetPaperDraftForm() {
  paperDraftForm.message = '';
  paperDraftForm.collectionIds = currentCollectionId.value ? [currentCollectionId.value] : [];
  paperDraftForm.constrains = [defaultPaperDraftConstrain()];
}

function addPaperDraftConstrain() {
  paperDraftForm.constrains.push(defaultPaperDraftConstrain());
}

function removePaperDraftConstrain(index: number) {
  paperDraftForm.constrains.splice(index, 1);
}

function onCreateTypeChange() {
  if (isCreateChoiceType.value) {
    if (createForm.options.length < 2) {
      createForm.options = defaultCreateOptions();
    }
    if (isCreateSingleChoice.value && createForm.choiceCorrect.length > 1) {
      createForm.choiceCorrect = createForm.choiceCorrect.slice(0, 1);
    }
    createForm.judgeAnswer = 'T';
    createForm.answer = '';
    return;
  }

  createForm.options = defaultCreateOptions();
  createForm.choiceCorrect = [];
  if (isCreateTrueFalseType.value) {
    createForm.answer = '';
    return;
  }
  createForm.judgeAnswer = 'T';
}

function addCreateOption() {
  createForm.options.push({ key: '', content: '' });
}

function removeCreateOption(index: number) {
  const removed = createForm.options.splice(index, 1)[0];
  if (!removed?.key) {
    return;
  }
  createForm.choiceCorrect = createForm.choiceCorrect.filter((item) => item !== removed.key);
}

function toggleCreateMultiple(key?: string) {
  if (!key?.trim()) {
    return;
  }
  const normalized = key.trim();
  const index = createForm.choiceCorrect.indexOf(normalized);
  if (index === -1) {
    createForm.choiceCorrect.push(normalized);
    return;
  }
  createForm.choiceCorrect.splice(index, 1);
}

function normalizeCreateOptions() {
  return createForm.options
    .map((option) => ({
      key: option.key.trim().toUpperCase(),
      content: option.content.trim()
    }))
    .filter((option) => option.key && option.content);
}

function resolveCreateTitle(stem: string) {
  const rawTitle = createForm.title.trim();
  if (rawTitle) {
    return rawTitle;
  }
  if (stem.length <= 18) {
    return stem;
  }
  return `${stem.slice(0, 18)}...`;
}

function buildCreatePayload(): QuestionCreatePayload {
  const stem = createForm.stem.trim();
  if (!stem) {
    throw new Error('请先填写题干');
  }
  if (!createQuestionCollectionId.value) {
    throw new Error('请先选择题集');
  }

  const payload: QuestionCreatePayload = {
    typeCode: createForm.questionType,
    title: resolveCreateTitle(stem),
    stem,
    answer: null,
    solution: createForm.solution.trim() || null,
    difficulty: createForm.difficulty,
    collectionId: createQuestionCollectionId.value,
    assets: []
  };

  if (isCreateChoiceType.value) {
    const options = normalizeCreateOptions();
    if (options.length < 2) {
      throw new Error('选择题请至少填写 2 个有效选项');
    }
    payload.options = options;
    const validKeys = new Set(options.map((option) => option.key));
    payload.correctOptions = createForm.choiceCorrect.map((key) => key.trim().toUpperCase()).filter((key) => validKeys.has(key));
    return payload;
  }

  if (isCreateTrueFalseType.value) {
    payload.judgeAnswer = createForm.judgeAnswer;
    return payload;
  }

  payload.answer = createForm.answer.trim() || null;
  return payload;
}

async function handleCreateQuestion() {
  if (!hasCreateCollectionContext.value || !createQuestionCollectionId.value) {
    showInfo('请先选择题集');
    return;
  }

  createSubmitting.value = true;
  try {
    const payload = buildCreatePayload();
    const created = await createQuestion(payload);
    selectedQuestionMetaMap[created.questionId] = {
      title: payload.title,
      typeCode: payload.typeCode,
      difficulty: payload.difficulty ?? null
    };
    contextState.collectionIdValues = [createQuestionCollectionId.value];
    createQuestionDrawerVisible.value = false;
    showSuccess('题目已创建');
  } catch (error: any) {
    showError(error?.message ?? '创建题目失败');
  } finally {
    createSubmitting.value = false;
  }
}

function normalizeText(value: unknown): string {
  if (value === undefined || value === null) {
    return '';
  }
  return String(value).trim();
}

function normalizeDraftOptions(rawOptions: QuestionOption[] | null | undefined, includeEmpty = false): QuestionOption[] {
  if (!Array.isArray(rawOptions)) {
    return [];
  }
  return rawOptions
    .map((item, index) => {
      const key = normalizeText(item?.key).toUpperCase() || String.fromCharCode(65 + index);
      const content = normalizeText(item?.content);
      return { key, content };
    })
    .filter((item) => (includeEmpty ? item.key : item.key && item.content));
}

function reindexDraftOptions(options: QuestionOption[]) {
  return options.map((item, index) => ({
    key: String.fromCharCode(65 + index),
    content: normalizeText(item.content)
  }));
}

function addAiDraftOption() {
  aiDraftForm.options = reindexDraftOptions([...aiDraftForm.options, { key: '', content: '' }]);
}

function removeAiDraftOption(index: number) {
  const next = [...aiDraftForm.options];
  next.splice(index, 1);
  aiDraftForm.options = reindexDraftOptions(next.length >= 2 ? next : defaultCreateOptions());
  const validKeys = new Set(aiDraftForm.options.map((item) => item.key));
  aiDraftForm.correctOptions = aiDraftForm.correctOptions.filter((item) => validKeys.has(item));
}

function parseCorrectOptionsFromText(input: string): string[] {
  if (!input.trim()) {
    return [];
  }
  const result = new Set<string>();
  input
    .split(/[,\uFF0C\u3001;\uFF1B\s]+/)
    .map((item) => item.trim().toUpperCase())
    .filter(Boolean)
    .forEach((item) => {
      if (/^[A-Z]$/.test(item)) {
        result.add(item);
      }
    });
  return [...result];
}

function parseJudgeAnswer(input?: string | null): 'T' | 'F' | null {
  const normalized = normalizeText(input).toUpperCase();
  if (!normalized) {
    return null;
  }
  if (['T', 'TRUE', '正确', '对', '是'].includes(normalized)) {
    return 'T';
  }
  if (['F', 'FALSE', '错误', '错', '否'].includes(normalized)) {
    return 'F';
  }
  return null;
}

function formatDraftCorrectAnswerFromForm(): string {
  const typeCode = normalizeText(aiDraftForm.typeCode || aiCreateForm.questionType);
  if (typeCode === 'single-choice' || typeCode === 'multiple-choice') {
    const normalized = aiDraftForm.correctOptions.map((item) => normalizeText(item).toUpperCase()).filter(Boolean);
    if (normalized.length > 0) {
      return normalized.join('、');
    }
  }
  if (typeCode === 'true-false') {
    const judge = parseJudgeAnswer(aiDraftForm.judgeAnswer || aiDraftForm.answer);
    if (judge === 'T') {
      return '正确';
    }
    if (judge === 'F') {
      return '错误';
    }
  }
  return normalizeText(aiDraftForm.answer);
}

function applyGeneratedDraftToEditableForm(draft: QuestionDraft) {
  const typeCode = normalizeText(draft.typeCode) || aiCreateForm.questionType;
  const normalizedOptions = reindexDraftOptions(normalizeDraftOptions(draft.options, true));
  const choiceOptions = normalizedOptions.length >= 2 ? normalizedOptions : defaultCreateOptions();
  const validKeys = new Set(choiceOptions.map((item) => item.key));
  const directCorrect = Array.isArray(draft.correctOptions)
    ? draft.correctOptions.map((item) => normalizeText(item).toUpperCase()).filter((item) => validKeys.has(item))
    : [];
  const fallbackCorrect = parseCorrectOptionsFromText(normalizeText(draft.answer)).filter((item) => validKeys.has(item));

  aiDraftForm.typeCode = typeCode;
  aiDraftForm.title = normalizeText(draft.title);
  aiDraftForm.difficulty = typeof draft.difficulty === 'number' ? draft.difficulty : null;
  aiDraftForm.stem = normalizeText(draft.stem);
  aiDraftForm.options = choiceOptions;
  const mergedCorrect = [...new Set([...directCorrect, ...fallbackCorrect])];
  aiDraftForm.correctOptions = typeCode === 'single-choice' ? mergedCorrect.slice(0, 1) : mergedCorrect;
  aiDraftForm.judgeAnswer = parseJudgeAnswer(draft.judgeAnswer ?? draft.answer) ?? '';
  aiDraftForm.answer = normalizeText(draft.answer);
  aiDraftForm.solution = normalizeText(draft.solution);
  aiDraftForm.assumptions = normalizeText(draft.assumptions);
}

function buildGenerateDraftRequest(): GenerateQuestionDraftReq {
  const topic = aiCreateForm.topic.trim();
  if (!topic) {
    throw new Error('请先填写知识点 / 出题方向');
  }

  const payload: GenerateQuestionDraftReq = {
    topic,
    typeCode: aiCreateForm.questionType
  };

  const scene = aiCreateForm.scenario.trim();
  if (scene) {
    payload.scene = scene;
  }

  const extraRequirements = aiCreateForm.requirements.trim();
  if (extraRequirements) {
    payload.extraRequirements = extraRequirements;
  }

  return payload;
}

function buildGeneratePaperDraftRequest(): GeneratePaperDraftReq {
  const message = paperDraftForm.message.trim();
  if (!message) {
    throw new Error('请先填写组卷需求');
  }

  const collectionIds = paperDraftForm.collectionIds.map((item) => normalizeText(item)).filter(Boolean);
  if (!collectionIds.length) {
    throw new Error('请至少选择一个题集');
  }

  if (!paperDraftForm.constrains.length) {
    throw new Error('请至少添加一条题型约束');
  }

  const constrains = paperDraftForm.constrains.map((item, index) => {
    const typeCode = normalizeText(item?.typeCode);
    const count = Number(item?.count);
    const difficultyMin = Number(item?.difficultyMin);
    const difficultyMax = Number(item?.difficultyMax);
    const rowText = `第 ${index + 1} 条约束`;

    if (!typeCode) {
      throw new Error(`${rowText}缺少题型`);
    }
    if (!Number.isInteger(count) || count <= 0) {
      throw new Error(`${rowText}题数必须是大于 0 的整数`);
    }
    if (!Number.isFinite(difficultyMin) || !Number.isFinite(difficultyMax)) {
      throw new Error(`${rowText}缺少难度范围`);
    }
    if (difficultyMin > difficultyMax) {
      throw new Error(`${rowText}的难度下限不能大于上限`);
    }

    return {
      typeCode,
      count,
      difficultyMin,
      difficultyMax
    };
  });

  return {
    message,
    collectionIds: [...new Set(collectionIds)],
    constrains
  };
}

function normalizeAgentPaperDraftResult(payload: AgentPaperDraftVO): AgentPaperDraftVO {
  const candidateQuestions = Array.isArray(payload?.candidateQuestions)
    ? payload.candidateQuestions.map((item) => ({
        questionId: normalizeText(item?.questionId),
        questionVersionId: normalizeText(item?.questionVersionId),
        title: normalizeText(item?.title),
        typeCode: normalizeText(item?.typeCode),
        difficulty: Number.isFinite(item?.difficulty) ? Number(item.difficulty) : 0
      }))
    : [];

  return {
    reason: normalizeText(payload?.reason),
    candidateQuestions
  };
}

async function handleGenerateAgentPaperDraft() {
  let requestPayload: GeneratePaperDraftReq;
  try {
    requestPayload = buildGeneratePaperDraftRequest();
  } catch (error: any) {
    const message = error?.message ?? '组卷需求参数校验失败';
    paperDraftErrorMessage.value = '';
    showError(message);
    return;
  }

  paperDraftStep.value = 2;
  paperDraftErrorMessage.value = '';
  paperDraftResult.value = null;
  generatePaperDraftLoading.value = true;
  try {
    const result = await generateAgentPaperDraft(requestPayload);
    paperDraftResult.value = normalizeAgentPaperDraftResult(result);
  } catch (error: any) {
    const message = error?.message ?? '组卷草稿生成失败，请重试';
    paperDraftErrorMessage.value = message;
    showError(message);
  } finally {
    generatePaperDraftLoading.value = false;
  }
}

async function handleGenerateAiDraft() {
  if (!hasCreateCollectionContext.value || !createQuestionCollectionId.value) {
    showInfo('请先选择题集');
    return;
  }

  createAiDraftLoading.value = true;
  try {
    const requestPayload = buildGenerateDraftRequest();
    const draft = await generateQuestionDraft(requestPayload);
    if (!normalizeText(draft?.stem)) {
      throw new Error('草稿缺少题干，请调整需求后重新生成');
    }
    applyGeneratedDraftToEditableForm(draft);
    aiDraftPreview.value = draft;
    aiCreateStep.value = 2;
    showSuccess('已生成题目草稿，请确认后再创建');
  } catch (error: any) {
    showError(error?.message ?? '题目草稿生成失败，请重试');
  } finally {
    createAiDraftLoading.value = false;
  }
}

function backToAiRequirementForm() {
  aiCreateStep.value = 1;
  aiDraftPreview.value = null;
  resetAiDraftForm();
}

function backToPaperRequirementForm() {
  paperDraftStep.value = 1;
}

function buildAiDraftPayload(): QuestionCreatePayload {
  if (!aiDraftPreview.value) {
    throw new Error('请先生成题目草稿');
  }
  if (!createQuestionCollectionId.value) {
    throw new Error('请先选择题集');
  }

  const typeCode = normalizeText(aiDraftForm.typeCode) || aiCreateForm.questionType;
  const stem = normalizeText(aiDraftForm.stem);
  if (!stem) {
    throw new Error('草稿题干为空，请重新生成');
  }

  const payload: QuestionCreatePayload = {
    typeCode,
    title: normalizeText(aiDraftForm.title) || resolveCreateTitle(stem),
    stem,
    answer: null,
    solution: normalizeText(aiDraftForm.solution) || null,
    difficulty: aiDraftForm.difficulty,
    collectionId: createQuestionCollectionId.value,
    assets: []
  };

  if (typeCode === 'single-choice' || typeCode === 'multiple-choice') {
    const options = normalizeDraftOptions(aiDraftForm.options);
    if (options.length < 2) {
      throw new Error('草稿选项不足，请重新生成或切换手动创建');
    }
    payload.options = options;
    const validKeys = new Set(options.map((option) => option.key));
    const directCorrect = aiDraftForm.correctOptions.map((item) => normalizeText(item).toUpperCase()).filter((item) => validKeys.has(item));
    const fallbackCorrect = parseCorrectOptionsFromText(normalizeText(aiDraftForm.answer)).filter((item) => validKeys.has(item));
    const mergedCorrect = [...new Set([...directCorrect, ...fallbackCorrect])];
    if (!mergedCorrect.length) {
      throw new Error('草稿缺少正确答案，请重新生成或切换手动创建');
    }
    if (typeCode === 'single-choice' && mergedCorrect.length > 1) {
      throw new Error('单选题草稿包含多个正确答案，请重新生成或切换手动创建');
    }
    payload.correctOptions = mergedCorrect;
    return payload;
  }

  if (typeCode === 'true-false') {
    const judgeAnswer = parseJudgeAnswer(aiDraftForm.judgeAnswer || aiDraftForm.answer);
    if (!judgeAnswer) {
      throw new Error('草稿缺少判断题答案，请重新生成或切换手动创建');
    }
    payload.judgeAnswer = judgeAnswer;
    return payload;
  }

  payload.answer = normalizeText(aiDraftForm.answer) || null;
  return payload;
}

async function handleCreateQuestionFromAiDraft() {
  if (!hasCreateCollectionContext.value || !createQuestionCollectionId.value) {
    showInfo('请先选择题集');
    return;
  }
  if (!aiDraftPreview.value) {
    showInfo('请先生成题目草稿');
    return;
  }

  createSubmitting.value = true;
  try {
    const payload = buildAiDraftPayload();
    const created = await createQuestion(payload);
    selectedQuestionMetaMap[created.questionId] = {
      title: payload.title,
      typeCode: payload.typeCode,
      difficulty: payload.difficulty ?? null
    };
    contextState.collectionIdValues = [createQuestionCollectionId.value];
    createQuestionDrawerVisible.value = false;
    showSuccess('题目已写入题库');
  } catch (error: any) {
    showError(error?.message ?? '写入题库失败');
  } finally {
    createSubmitting.value = false;
  }
}

function openQuestionPicker() {
  questionPickerVisible.value = true;
  drawerSelectedQuestionIds.value = [];
  pickerDetailCollapseNames.value = [];
  if (!questionPickerCollectionId.value) {
    questionPickerCollectionId.value = currentCollectionId.value;
  }

  if (questionPickerCollectionId.value) {
    void loadQuestionCandidates();
    return;
  }

  questionPickerPage.value = null;
  pickerActiveQuestionId.value = '';
}

function resetQuestionPickerFilters() {
  questionPickerQuery.keyword = '';
  questionPickerQuery.typeCode = '';
  questionPickerQuery.levelMin = undefined;
  questionPickerQuery.levelMax = undefined;
  questionPickerQuery.pageNum = 1;
  questionPickerQuery.pageSize = 10;
}

function handleResetQuestionPickerFilters() {
  resetQuestionPickerFilters();
  if (questionPickerCollectionId.value) {
    void loadQuestionCandidates();
  }
}

function onQuestionPickerCollectionChange() {
  drawerSelectedQuestionIds.value = [];
  questionPickerPage.value = null;
  pickerActiveQuestionId.value = '';
  pickerQuestionDetailLoadingId.value = '';
  pickerDetailCollapseNames.value = [];
  resetQuestionPickerFilters();

  if (!questionPickerCollectionId.value) {
    return;
  }

  void loadQuestionCandidates();
}

function onQuestionFilterChange() {
  if (!questionPickerCollectionId.value) {
    return;
  }
  questionPickerQuery.pageNum = 1;
  void loadQuestionCandidates();
}

function onQuestionPageChange(pageNum: number) {
  if (questionPickerLoading.value) {
    return;
  }
  questionPickerQuery.pageNum = pageNum;
  void loadQuestionCandidates();
}

function onQuestionSizeChange(pageSize: number) {
  if (questionPickerLoading.value) {
    return;
  }
  questionPickerQuery.pageSize = pageSize;
  questionPickerQuery.pageNum = 1;
  void loadQuestionCandidates();
}

function normalizeInteger(value: unknown, fallback: number, min = 0) {
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) {
    return fallback;
  }
  const normalized = Math.trunc(parsed);
  return normalized < min ? min : normalized;
}

function normalizeQuestionPickerPage(raw: unknown): QuestionSummaryPage {
  const payload = raw && typeof raw === 'object' ? (raw as Record<string, unknown>) : {};
  const recordsRaw = payload.records ?? payload.list ?? payload.items ?? [];
  const records = Array.isArray(recordsRaw) ? (recordsRaw as QuestionSummary[]) : [];
  const fallbackPageSize = questionPickerQuery.pageSize || 10;
  const pageSize = normalizeInteger(payload.pageSize ?? payload.size, fallbackPageSize, 1);
  const total = normalizeInteger(payload.total, records.length, 0);
  const fallbackPageNum = questionPickerQuery.pageNum || 1;
  const pageNum = normalizeInteger(payload.pageNum ?? payload.current ?? payload.page, fallbackPageNum, 1);
  const pagesFallback = Math.max(Math.ceil(total / pageSize), 1);
  const pages = normalizeInteger(payload.pages ?? payload.pageCount, pagesFallback, 1);

  return {
    records,
    total,
    pageNum,
    pageSize,
    pages,
    hasPrevious: Boolean(payload.hasPrevious ?? pageNum > 1),
    hasNext: Boolean(payload.hasNext ?? pageNum < pages)
  };
}

async function loadQuestionCandidates() {
  if (!questionPickerCollectionId.value) {
    return;
  }

  if (questionPickerLevelError.value) {
    showInfo(questionPickerLevelError.value);
    return;
  }

  questionPickerLoading.value = true;
  try {
    const response = await fetchCollectionQuestions(questionPickerCollectionId.value, {
      pageNum: questionPickerQuery.pageNum,
      pageSize: questionPickerQuery.pageSize,
      keyword: questionPickerQuery.keyword || undefined,
      typeCode: questionPickerQuery.typeCode || undefined,
      levelMin: questionPickerQuery.levelMin,
      levelMax: questionPickerQuery.levelMax,
      sortField: 'updatedAt',
      sortDirection: 'DESC'
    });
    const page = normalizeQuestionPickerPage(response);
    questionPickerPage.value = page;
    questionPickerQuery.pageNum = page.pageNum || questionPickerQuery.pageNum;
    questionPickerQuery.pageSize = page.pageSize || questionPickerQuery.pageSize;
    page.records.forEach((question) => cacheQuestionMeta(question));

    if (!page.records.length) {
      pickerActiveQuestionId.value = '';
      pickerDetailCollapseNames.value = [];
      return;
    }

    const activeStillExists = page.records.some((item) => item.id === pickerActiveQuestionId.value);
    const target = activeStillExists ? page.records.find((item) => item.id === pickerActiveQuestionId.value)! : page.records[0];
    void selectQuestionForDetail(target);
  } catch (error) {
    showError(error instanceof Error ? error.message : '加载题目列表失败');
  } finally {
    questionPickerLoading.value = false;
  }
}

async function loadQuestionDetail(questionId: string) {
  if (!questionId || questionDetailMap[questionId]) {
    return;
  }
  pickerQuestionDetailLoadingId.value = questionId;
  try {
    const detail = await fetchQuestionDetail(questionId);
    questionDetailMap[questionId] = detail;
  } catch (error) {
    showError(error instanceof Error ? error.message : '加载题目详情失败');
  } finally {
    if (pickerQuestionDetailLoadingId.value === questionId) {
      pickerQuestionDetailLoadingId.value = '';
    }
  }
}

async function selectQuestionForDetail(question: QuestionSummary) {
  pickerActiveQuestionId.value = question.id;
  pickerDetailCollapseNames.value = [];
  cacheQuestionMeta(question);
  await loadQuestionDetail(question.id);
}

function cacheQuestionMeta(question: QuestionSummary) {
  selectedQuestionMetaMap[question.id] = {
    title: question.title,
    typeCode: question.typeCode,
    difficulty: question.difficulty ?? null
  };
}

function isQuestionSelectedInDrawer(questionId: string) {
  return drawerSelectedQuestionIds.value.includes(questionId);
}

function toggleDrawerQuestion(question: QuestionSummary, checked: string | number | boolean) {
  const shouldSelect = Boolean(checked);
  const id = question.id;
  const index = drawerSelectedQuestionIds.value.indexOf(id);

  if (shouldSelect && index === -1) {
    drawerSelectedQuestionIds.value.push(id);
    cacheQuestionMeta(question);
    return;
  }

  if (!shouldSelect && index !== -1) {
    drawerSelectedQuestionIds.value.splice(index, 1);
  }
}

async function applyAndExplainFromDrawer() {
  if (!canExplainBase.value) {
    if (streaming.value) {
      showInfo('当前正在生成回复，请稍后再试。');
    } else if (!activeSessionId.value) {
      showInfo('请先创建或选择一个会话。');
    }
    return;
  }
  if (!questionPickerCollectionId.value) {
    showInfo('请先选择题集');
    return;
  }
  if (!drawerSelectedQuestionIds.value.length) {
    showInfo('请先勾选题目');
    return;
  }
  const selectedQuestionIds = normalizeLongIdList(drawerSelectedQuestionIds.value);
  if (!selectedQuestionIds.length) {
    showInfo('请先勾选题目');
    return;
  }
  contextState.collectionIdValues = [questionPickerCollectionId.value];
  questionPickerVisible.value = false;
  await sendChatMessage('请讲解我刚刚选择的题目，按“考点、解题思路、关键步骤、易错点”逐题说明。', 'explain', {
    toolContext: {
      collectionId: questionPickerCollectionId.value,
      selectedQuestionIds
    }
  });
}

async function handleSend() {
  const userText = composerMessage.value.trim();
  if (!userText) {
    return;
  }
  composerMessage.value = '';
  await sendChatMessage(userText, inferResultTypeFromText(userText));
}

async function sendChatMessage(
  message: string,
  resultType: ResultType = 'general',
  options: SendMessageOptions = {}
): Promise<string> {
  if (!activeSessionId.value || streaming.value) {
    return '';
  }

  const collectionIds = options.toolContext?.collectionId ? normalizeLongIdList([options.toolContext.collectionId]) : [];
  const selectedIds = options.toolContext?.selectedQuestionIds
    ? normalizeLongIdList(options.toolContext.selectedQuestionIds)
    : [];
  const sid = activeSessionId.value;
  const list = ensureSessionMessages(sid);

  appendMessage(list, 'user', message, resultType);
  const assistantMessageId = appendPendingAssistantMessage(list, resultType);
  await nextTick();
  scrollToBottom();

  streaming.value = true;
  const controller = new AbortController();
  currentAbortController.value = controller;

  const payload: AgentChatPayload = {
    sessionId: sid,
    message,
    agentName: AGENT_NAME,
    context: {
      collectionIds,
      selectedQuestionIds: selectedIds
    }
  };

  let assistantFinalText = '';
  try {
    await streamAgentChat(
      payload,
      {
        onChunk: (text) => {
          updateMessage(list, assistantMessageId, (prev) => prev + text);
          scrollToBottom();
        },
        onDone: () => {
          const assistant = list.find((item) => item.id === assistantMessageId);
          if (assistant && assistant.role === 'assistant') {
            if (!assistant.content.trim()) {
              assistant.content = '正在整理中，暂时没有可展示的文本。';
            }
            assistant.status = 'done';
          }
          streaming.value = false;
          currentAbortController.value = null;
        }
      },
      controller.signal
    );
    const assistant = list.find((item) => item.id === assistantMessageId);
    assistantFinalText = assistant?.content ?? '';
    await refreshSessionsKeepCurrent();
  } catch (error) {
    if (!controller.signal.aborted) {
      showError(error instanceof Error ? error.message : '聊天请求失败');
      const assistant = list.find((item) => item.id === assistantMessageId);
      if (assistant && assistant.role === 'assistant') {
        assistant.content = assistant.content.trim() || '抱歉，当前无法生成回复，请稍后再试。';
        assistant.status = 'error';
      }
    }
  } finally {
    streaming.value = false;
    currentAbortController.value = null;
    await nextTick();
    scrollToBottom();
  }
  return assistantFinalText;
}

function ensureSessionMessages(sessionId: string): MessageItem[] {
  if (!messagesBySession[sessionId]) {
    messagesBySession[sessionId] = [];
  }
  return messagesBySession[sessionId];
}

function appendMessage(list: MessageItem[], role: MessageRole, content: string, resultType?: ResultType): string {
  const id = `${Date.now()}_${Math.random().toString(36).slice(2)}`;
  list.push({
    id,
    role,
    content,
    status: role === 'assistant' ? 'done' : undefined,
    resultType
  });
  return id;
}

function appendPendingAssistantMessage(list: MessageItem[], resultType: ResultType): string {
  const id = `${Date.now()}_${Math.random().toString(36).slice(2)}`;
  list.push({
    id,
    role: 'assistant',
    content: '',
    status: 'pending',
    resultType
  });
  return id;
}

function updateMessage(list: MessageItem[], id: string, updater: (prev: string) => string) {
  const target = list.find((item) => item.id === id);
  if (!target) {
    return;
  }
  target.content = updater(target.content);
  if (target.role === 'assistant') {
    target.status = target.content.trim() ? 'streaming' : target.status;
  }
}

function normalizeLongIdList(values: string[]): string[] {
  return values
    .map((item) => item.trim())
    .filter((item) => /^\d+$/.test(item))
    .filter((item) => {
      try {
        return BigInt(item) > 0n;
      } catch {
        return false;
      }
    });
}

function mapServerMessages(rawMessages: AgentChatMessageVO[], sessionId: string): MessageItem[] {
  let latestIntent: ResultType = 'general';
  return rawMessages
    .map((item, index) => {
      const role = normalizeRole(item.role);
      const rawContent = (item.text ?? item.thinking ?? '').trim();
      const content = role === 'assistant' ? normalizeWelcomeMessage(rawContent) : rawContent;
      if (role === 'user') {
        latestIntent = inferResultTypeFromText(content);
      }
      return {
        id: `${sessionId}_${index}`,
        role,
        content,
        status: role === 'assistant' ? 'done' : undefined,
        resultType: role === 'assistant' ? latestIntent : undefined
      } as MessageItem;
    })
    .filter((item) => item.role !== 'system' && Boolean(item.content));
}

function normalizeRole(role?: string): MessageRole {
  if (role === 'assistant') {
    return 'assistant';
  }
  if (role === 'user') {
    return 'user';
  }
  return 'system';
}

function normalizeWelcomeMessage(content: string) {
  if (!content) {
    return '';
  }
  return content
    .replace(/创建新题目并加入当前题集/g, '生成同主题练习内容')
    .replace(/创建新题目/g, '生成练习内容');
}

function roleLabel(role: MessageRole) {
  if (role === 'assistant') {
    return '助手';
  }
  if (role === 'user') {
    return '我';
  }
  return '系统';
}

function getSessionTitle(session: AgentSessionVO, index: number) {
  const title = session.title?.trim();
  if (title) {
    return title;
  }
  return index === 0 ? '新对话' : '未命名对话';
}

function inferResultTypeFromText(input: string): ResultType {
  const text = input.trim();
  if (!text) {
    return 'general';
  }
  if (/讲解|解析|思路|易错点|逐题|步骤/.test(text)) {
    return 'explain';
  }
  if (/出题|生成|草稿|练习题|新题/.test(text)) {
    return 'generate';
  }
  if (/检索|搜索|找|筛选|推荐|题目/.test(text)) {
    return 'search';
  }
  return 'general';
}

function getSessionPreview(sessionId: string) {
  const list = messagesBySession[sessionId];
  if (!list?.length) {
    return '题库学习助手欢迎会话';
  }
  const userMessages = list.filter((item) => item.role === 'user' && item.content.trim());
  if (!userMessages.length) {
    return '题库学习助手欢迎会话';
  }
  const intents = userMessages.map((item) => inferResultTypeFromText(item.content));
  const hasExplain = intents.includes('explain');
  const hasSearch = intents.includes('search');
  const hasGenerate = intents.includes('generate');

  if (hasSearch && hasExplain) {
    return '检索了题目并进行了讲解';
  }
  if (hasExplain) {
    return '围绕题目进行了讲解分析';
  }
  if (hasSearch) {
    return '查看了题库中的题目';
  }
  if (hasGenerate) {
    return '生成了同主题练习内容';
  }
  return '进行了题库学习交流';
}

function formatSessionTime(value?: string) {
  if (!value) {
    return '';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '';
  }
  return date.toLocaleString();
}

function typeLabel(code?: string) {
  if (!code) {
    return '未知题型';
  }
  return typeLabelMap[code] ?? code;
}

function difficultyLabel(difficulty?: number | null) {
  if (difficulty === undefined || difficulty === null) {
    return '难度未标注';
  }
  return `难度 ${difficulty.toFixed(2)}`;
}

function questionDisplayNo(index: number) {
  const pageNum = questionPickerQuery.pageNum || 1;
  const pageSize = questionPickerQuery.pageSize || 10;
  return (pageNum - 1) * pageSize + index + 1;
}

function clipText(text: string, limit: number) {
  if (text.length <= limit) {
    return text;
  }
  return `${text.slice(0, limit)}...`;
}

function resetQuestionPickerState() {
  questionPickerCollectionId.value = '';
  questionPickerPage.value = null;
  pickerActiveQuestionId.value = '';
  pickerQuestionDetailLoadingId.value = '';
  pickerDetailCollapseNames.value = [];
  resetQuestionPickerFilters();
}

function applyQuickPrompt(prompt: string) {
  composerMessage.value = prompt;
}

function abortStreaming() {
  currentAbortController.value?.abort();
  currentAbortController.value = null;
  streaming.value = false;
}

function scrollToBottom() {
  const el = messageScrollRef.value;
  if (!el) {
    return;
  }
  el.scrollTop = el.scrollHeight;
}

onBeforeUnmount(() => {
  currentAbortController.value?.abort();
});
</script>

<style scoped>
.chat-page {
  height: 100%;
  min-height: 0;
  overflow: hidden;
  overscroll-behavior-y: none;
}

.chat-layout {
  height: 100%;
  min-height: 0;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  overflow: hidden;
  background: #fff;
}

.session-aside {
  min-height: 0;
  border-right: 1px solid var(--el-border-color-light);
  background: #f7f9fc;
  display: flex;
  flex-direction: column;
}

.aside-header {
  padding: 14px 12px;
  border-bottom: 1px solid var(--el-border-color-light);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.aside-title-wrap {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.aside-title {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.aside-subtitle {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.session-loading {
  padding: 12px;
}

.session-scroll {
  flex: 1;
  min-height: 0;
  padding: 10px;
}

.session-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.session-item {
  border: 1px solid #e9edf5;
  border-radius: 12px;
  background: #fff;
  display: flex;
  align-items: center;
  transition: border-color 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease;
}

.session-item:hover {
  border-color: var(--el-color-primary-light-6);
  box-shadow: 0 3px 10px rgba(15, 23, 42, 0.06);
}

.session-item.active {
  border-color: var(--el-color-primary);
  background: linear-gradient(120deg, var(--el-color-primary-light-9), #ffffff 75%);
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.14);
}

.session-main {
  flex: 1;
  border: none;
  background: transparent;
  text-align: left;
  padding: 10px 10px 10px 12px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.session-main .title {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-main .preview {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-main .time {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

.session-meta-line {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}

.session-more {
  padding: 6px 9px;
  border-radius: 8px;
  color: var(--el-text-color-secondary);
}

.session-more:hover {
  background: var(--el-fill-color-light);
}

.chat-main {
  display: flex;
  flex-direction: column;
  gap: 0;
  padding: 0;
  min-height: 0;
}

.toolbar-card {
  border: none;
  border-bottom: 1px solid var(--el-border-color-light);
  border-radius: 0;
}

.toolbar-card :deep(.el-card__body) {
  padding: 8px 12px;
}

.workspace-toolbar {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.tool-btn {
  height: 40px;
  padding: 0 22px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 10px;
}

.tool-btn-secondary {
  height: 40px;
  padding: 0 18px;
  font-size: 14px;
  border-radius: 10px;
}

.message-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px 16px 18px;
  background: linear-gradient(180deg, #f8fbff 0%, #f3f7fd 60%, #f8fbff 100%);
  overscroll-behavior: contain;
}

.loading-wrap {
  padding: 10px;
}

.empty-wrap {
  min-height: 260px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.quick-prompts {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  margin-top: 10px;
}

.empty-tip {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 980px;
  margin: 0 auto;
}

.message-row {
  display: flex;
}

.message-row.user {
  justify-content: flex-end;
}

.message-row.assistant,
.message-row.system {
  justify-content: flex-start;
}

.message-bubble {
  max-width: min(74ch, 86%);
  border-radius: 14px;
  padding: 11px 13px;
  border: 1px solid #e5eaf2;
  background: #fff;
  box-shadow: 0 3px 10px rgba(15, 23, 42, 0.06);
}

.message-bubble.pending {
  background: var(--el-fill-color-light);
}

.message-bubble.error {
  border-color: var(--el-color-danger-light-5);
  background: var(--el-color-danger-light-9);
}

.message-row.user .message-bubble {
  background: linear-gradient(120deg, #e8f1ff, #f3f8ff 70%);
  border-color: #bcd7ff;
}

.message-bubble .role {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
}

.message-bubble .content {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.6;
}

.message-bubble .pending-text {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}

.composer-wrap {
  border-top: 1px solid var(--el-border-color-light);
  padding: 10px 12px 12px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), #ffffff);
  backdrop-filter: blur(6px);
}

.composer-card {
  border: 1px solid #d9e3f0;
  border-radius: 14px;
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.08);
}

.composer-card :deep(.el-card__body) {
  padding: 10px 12px;
}

.composer-card :deep(.el-textarea__inner) {
  border: none;
  box-shadow: none;
  background: transparent;
  padding: 2px 0;
  font-size: 14px;
  line-height: 1.7;
}

.composer-head {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.composer-title {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

.composer-footer {
  margin-top: 6px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.action-group {
  display: flex;
  gap: 8px;
  margin-left: auto;
}

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

.option-key,
.option-content {
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

.draft-value.pre-wrap {
  white-space: pre-wrap;
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
  .chat-page {
    height: 100%;
    min-height: 0;
  }

  .chat-layout {
    display: flex;
    flex-direction: column;
  }

  .session-aside {
    width: 100% !important;
    max-height: 280px;
    border-right: none;
    border-bottom: 1px solid var(--el-border-color-light);
  }

  .workspace-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .toolbar-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .tool-btn,
  .tool-btn-secondary {
    width: 100%;
  }

  .composer-footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .picker-filter-grid {
    grid-template-columns: 1fr;
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

  .create-grid.two-col {
    grid-template-columns: 1fr;
  }

  .draft-workbench {
    grid-template-columns: 1fr;
  }

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
