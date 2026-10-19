<template>
  <div class="documents-page">
    <div class="page-heading">
      <div>
        <span class="eyebrow">知识库管理 / 文档管理</span>
        <h1>文档管理</h1>
        <p>维护药品说明书等知识来源，查看文档状态和索引进度。</p>
      </div>
      <el-button type="primary" icon="el-icon-upload2" :loading="uploading" @click="$refs.documentInput.click()">
        上传文档
      </el-button>
      <input ref="documentInput" type="file" accept=".pdf,.txt,.md,.docx" hidden @change="handleUpload">
    </div>

    <el-alert
      title="文档列表仍是本地知识库快照；当前已支持上传并返回解析结果，分类修改、实时列表和版本管理尚未接入。"
      type="info"
      :closable="false"
      show-icon
    />

    <div class="summary-grid">
      <div v-for="item in summaryItems" :key="item.label" class="summary-card">
        <div :class="['summary-icon', item.type]"><i :class="item.icon" /></div>
        <div>
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </div>
    </div>

    <div class="content-layout">
      <el-card class="category-card" shadow="never">
        <div slot="header" class="category-header">文档分类</div>
        <button
          v-for="item in categories"
          :key="item.name"
          type="button"
          :class="['category-item', { active: selectedCategory === item.name }]"
          @click="selectedCategory = item.name"
        >
          <span><i :class="item.icon" /> {{ item.name }}</span>
          <em>{{ item.count }}</em>
        </button>
      </el-card>

      <el-card class="documents-card" shadow="never">
        <div slot="header" class="card-header">
          <div>
            <strong>知识文档</strong>
            <span>当前本地知识库已有 {{ filteredDocuments.length }} 份文档</span>
          </div>
          <el-button size="small" icon="el-icon-refresh" @click="showUnavailableMessage">刷新</el-button>
        </div>

        <div v-if="filteredDocuments.length" class="document-list">
          <div v-for="document in filteredDocuments" :key="document.name" class="document-row">
            <div class="file-icon"><i class="el-icon-document" /></div>
            <div class="document-main">
              <strong>{{ document.name }}</strong>
              <div class="document-meta">
                <span>{{ document.category }}</span>
                <span>{{ document.pages }} 页</span>
                <span>{{ document.size }}</span>
              </div>
            </div>
            <span class="document-status"><i class="el-icon-circle-check" /> 已生成知识片段</span>
            <span class="document-source">{{ document.source }}</span>
          </div>
        </div>
        <div v-else class="empty-state">
          <div class="empty-icon"><i class="el-icon-document" /></div>
          <h2>该分类暂无文档</h2>
          <p>上传接口接入后，文档会在这里展示名称、分类、版本、索引状态和更新时间。</p>
        </div>
      </el-card>
    </div>

    <div class="workflow-card">
      <div class="workflow-title"><i class="el-icon-s-operation" /> 文档进入知识库的流程</div>
      <div class="workflow-steps">
        <div v-for="(step, index) in workflowSteps" :key="step.title" class="workflow-step">
          <span>{{ index + 1 }}</span>
          <div><strong>{{ step.title }}</strong><p>{{ step.description }}</p></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { uploadKnowledgeDocument } from '@/api/knowledge'

export default {
  name: 'KnowledgeDocuments',
  data() {
    return {
      summaryItems: [
        { label: '文档总数', value: '4', icon: 'el-icon-document', type: 'blue' },
        { label: '已处理文档', value: '4', icon: 'el-icon-circle-check', type: 'green' },
        { label: '知识片段', value: '—', icon: 'el-icon-loading', type: 'orange' },
        { label: '当前分类', value: '药品说明书', icon: 'el-icon-collection-tag', type: 'purple' }
      ],
      selectedCategory: '全部文档',
      uploading: false,
      categories: [
        { name: '全部文档', count: 4, icon: 'el-icon-folder-opened' },
        { name: '药品说明书', count: 4, icon: 'el-icon-notebook-1' },
        { name: '上传文档', count: 4, icon: 'el-icon-upload2' }
      ],
      documents: [
        { name: '奥美拉唑肠溶胶囊.pdf', category: '药品说明书', source: '上传文档', pages: 4, size: '192 KB' },
        { name: '达可替尼片.pdf', category: '药品说明书', source: '上传文档', pages: 12, size: '—' },
        { name: '必洛斯_坎地沙坦酯片__说明书_20240522.pdf', category: '药品说明书', source: '上传文档', pages: 10, size: '535 KB' },
        { name: '氯雷他定片.pdf', category: '药品说明书', source: '上传文档', pages: 4, size: '—' }
      ],
      workflowSteps: [
        { title: '上传文档', description: '校验格式、大小和文档来源。' },
        { title: '解析与切分', description: '提取章节内容，生成可检索片段。' },
        { title: '生成索引', description: '完成向量化和检索索引构建。' },
        { title: '审核发布', description: '确认文档版本后切换为当前知识库版本。' }
      ]
    }
  },
  computed: {
    filteredDocuments() {
      if (this.selectedCategory === '全部文档') return this.documents
      return this.documents.filter(document => document.category === this.selectedCategory || document.source === this.selectedCategory)
    }
  },
  methods: {
    handleUpload(event) {
      const file = event.target.files && event.target.files[0]
      event.target.value = ''
      if (!file) return
      const formData = new FormData()
      formData.append('file', file)
      formData.append('category', '药品说明书')
      this.uploading = true
      uploadKnowledgeDocument(formData)
        .then(response => {
          const result = response.data || {}
          this.$message.success(result.message || '文档已上传并完成解析')
        })
        .finally(() => {
          this.uploading = false
        })
    },
    showUnavailableMessage() {
      this.$message.info('当前列表是本地快照，实时刷新接口将在后续阶段接入。')
    }
  }
}
</script>

<style scoped lang="scss">
.documents-page {
  min-height: calc(100vh - 84px);
  padding: 24px;
  background: #f5f8fc;
}

.page-heading,
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-heading {
  max-width: 1280px;
  margin: 0 auto 20px;
}

.eyebrow {
  color: #2878ff;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 1px;
}

.page-heading h1 {
  margin: 7px 0;
  color: #17324d;
  font-size: 26px;
}

.page-heading p,
.card-header span {
  margin: 0;
  color: #7b8998;
  font-size: 13px;
}

.page-heading .el-button { border-radius: 8px; }
.documents-page > .el-alert { max-width: 1280px; margin: 0 auto 20px; }

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  max-width: 1280px;
  margin: 0 auto 20px;
}

.summary-card {
  display: flex;
  align-items: center;
  padding: 18px;
  background: #fff;
  border: 1px solid #e6edf5;
  border-radius: 12px;
}

.summary-icon,
.empty-icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

.summary-icon {
  width: 40px;
  height: 40px;
  margin-right: 12px;
  font-size: 19px;
  border-radius: 10px;
}

.summary-icon.blue { color: #2878ff; background: #eaf2ff; }
.summary-icon.green { color: #24a375; background: #e9f8f2; }
.summary-icon.orange { color: #c98522; background: #fff4df; }
.summary-icon.purple { color: #8059c7; background: #f1ebff; }
.summary-card span { display: block; color: #8391a0; font-size: 12px; }
.summary-card strong { display: block; margin-top: 4px; color: #263d55; font-size: 20px; }

.content-layout { display: grid; grid-template-columns: 220px minmax(0, 1fr); gap: 16px; max-width: 1280px; margin: 0 auto 20px; }
.category-card { height: fit-content; border: 1px solid #e6edf5; border-radius: 12px; }
.category-header { color: #17324d; font-size: 15px; font-weight: 600; }
.category-item { display: flex; align-items: center; justify-content: space-between; width: 100%; padding: 12px 10px; color: #607386; font-size: 13px; text-align: left; background: transparent; border: 0; border-radius: 8px; cursor: pointer; }
.category-item i { margin-right: 6px; color: #8da0b4; }
.category-item em { color: #9aa9b8; font-size: 12px; font-style: normal; }
.category-item:hover, .category-item.active { color: #2878ff; background: #edf4ff; }
.category-item.active i, .category-item.active em { color: #2878ff; }

.documents-card,
.workflow-card {
  max-width: 1280px;
  margin: 0 auto 20px;
  border: 1px solid #e6edf5;
  border-radius: 12px;
}

.content-layout .documents-card { min-width: 0; max-width: none; margin: 0; }
.document-list { padding: 4px 20px 8px; }
.document-row { display: flex; align-items: center; gap: 12px; padding: 16px 0; border-bottom: 1px solid #eef2f6; }
.document-row:last-child { border-bottom: 0; }
.file-icon { display: flex; align-items: center; justify-content: center; width: 38px; height: 38px; color: #2878ff; font-size: 18px; background: #edf4ff; border-radius: 10px; }
.document-main { min-width: 0; flex: 1; }
.document-main > strong { display: block; overflow: hidden; color: #30465c; font-size: 14px; text-overflow: ellipsis; white-space: nowrap; }
.document-meta { display: flex; gap: 14px; margin-top: 7px; color: #8a98a7; font-size: 12px; }
.document-status, .document-source { flex: 0 0 auto; font-size: 12px; }
.document-status { padding: 5px 9px; color: #26966d; background: #eaf8f2; border-radius: 6px; }
.document-source { color: #8a98a7; }

.card-header strong { display: block; margin-bottom: 5px; color: #17324d; font-size: 16px; }
.card-header .el-button { border-radius: 6px; }
.empty-state { padding: 58px 24px 65px; text-align: center; }
.empty-icon { width: 64px; height: 64px; margin: 0 auto 16px; color: #2878ff; font-size: 28px; background: #edf4ff; border-radius: 18px; }
.empty-state h2 { margin: 0; color: #30465c; font-size: 18px; font-weight: 600; }
.empty-state p { max-width: 520px; margin: 10px auto 20px; color: #8a98a7; font-size: 13px; line-height: 1.8; }
.empty-state .el-button { border-radius: 7px; }
.workflow-card { padding: 20px 24px; }
.workflow-title { color: #30465c; font-size: 15px; font-weight: 600; }
.workflow-title i { margin-right: 7px; color: #2878ff; }
.workflow-steps { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-top: 20px; }
.workflow-step { display: flex; gap: 10px; }
.workflow-step > span { display: flex; flex: 0 0 auto; align-items: center; justify-content: center; width: 25px; height: 25px; color: #2878ff; font-size: 12px; background: #edf4ff; border-radius: 8px; }
.workflow-step strong { color: #41566b; font-size: 13px; }
.workflow-step p { margin: 5px 0 0; color: #8b98a6; font-size: 12px; line-height: 1.6; }

@media (max-width: 900px) {
  .summary-grid { grid-template-columns: repeat(2, 1fr); }
  .content-layout { grid-template-columns: 1fr; }
  .workflow-steps { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 600px) {
  .documents-page { padding: 14px; }
  .page-heading { align-items: flex-start; gap: 14px; flex-direction: column; }
  .page-heading .el-button { align-self: stretch; }
  .summary-grid, .workflow-steps { grid-template-columns: 1fr; }
  .document-row { align-items: flex-start; flex-wrap: wrap; }
  .document-status, .document-source { margin-left: 50px; }
}
</style>
