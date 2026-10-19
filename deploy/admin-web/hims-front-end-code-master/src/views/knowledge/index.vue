<template>
  <div class="knowledge-page">
    <div class="knowledge-shell">
      <section class="hero-panel">
        <div class="hero-icon"><i class="el-icon-first-aid-kit" /></div>
        <div class="hero-content">
          <div class="hero-badges">
            <span class="hero-badge">管理端专用</span>
            <span class="hero-badge hero-badge--light">说明书知识检索</span>
          </div>
          <h1>药品知识助手</h1>
          <p>基于已收录的药品说明书查找用法、不良反应、禁忌及特殊人群注意事项。</p>
        </div>
        <div class="hero-notice">
          <i class="el-icon-warning-outline" />
          <span>回答仅供信息查询参考，不能替代医生或药师的诊断与用药建议。</span>
        </div>
      </section>

      <div class="workspace-grid">
        <main class="main-column">
          <el-card class="question-card" shadow="never">
            <div class="section-heading">
              <div>
                <span class="section-kicker">药品咨询</span>
                <h2>描述你想了解的问题</h2>
              </div>
              <span class="step-label">01 / 提问</span>
            </div>

            <div class="example-row">
              <span class="example-label">试试这样问</span>
              <button
                v-for="item in exampleQuestions"
                :key="item"
                type="button"
                class="example-chip"
                @click="useExample(item)"
              >
                {{ item }}
              </button>
            </div>

            <el-form class="question-form" @submit.native.prevent="submitQuestion">
              <el-input
                v-model.trim="question"
                type="textarea"
                :rows="5"
                maxlength="1000"
                show-word-limit
                placeholder="请输入具体药品名称和问题，例如：阿莫西林的常见不良反应有哪些？"
                @keydown.ctrl.enter.native="submitQuestion"
              />
              <div class="form-footer">
                <div class="input-hint">
                  <i class="el-icon-lock" />
                  请勿输入患者姓名、身份证号、病历等隐私信息
                </div>
                <div class="form-actions">
                  <el-button v-if="question" :disabled="loading" @click="clearQuestion">清空</el-button>
                  <el-button type="primary" :loading="loading" @click="submitQuestion">
                    <i v-if="!loading" class="el-icon-search" />
                    {{ loading ? '正在检索' : '查询药品信息' }}
                  </el-button>
                </div>
              </div>
            </el-form>
          </el-card>

          <el-card v-if="loading" class="loading-card" shadow="never">
            <div class="loading-mark"><i class="el-icon-loading" /></div>
            <div>
              <strong>正在查找说明书依据</strong>
              <p>系统正在检索相关内容并进行用药风险检查，请稍候。</p>
            </div>
          </el-card>

          <section v-else-if="result" class="result-panel">
            <div class="result-header">
              <div>
                <span class="section-kicker">检索结果</span>
                <h2>药品信息回答</h2>
              </div>
              <div class="result-statuses">
                <span class="status-pill status-pill--success">
                  <i class="el-icon-circle-check" /> 已完成
                </span>
                <span :class="['status-pill', 'status-pill--' + riskMeta.type]">{{ riskMeta.label }}</span>
              </div>
            </div>

            <div class="answer-card">
              <div class="answer-card__header">
                <div class="answer-icon"><i class="el-icon-chat-line-round" /></div>
                <div>
                  <strong>回答正文</strong>
                  <span>内容依据当前知识库生成</span>
                </div>
              </div>

              <div v-if="answerSections.length" class="answer-sections">
                <div
                  v-for="(section, index) in answerSections"
                  :key="section.title + index"
                  :class="['answer-section', { 'answer-section--primary': index === 0 }]"
                >
                  <div class="answer-section__title">
                    <span class="answer-section__index">{{ index + 1 }}</span>
                    {{ section.title }}
                  </div>
                  <div class="answer-section__content">{{ section.content }}</div>
                </div>
              </div>
              <div v-else class="answer-text">{{ result.answer }}</div>
            </div>

            <div v-if="result.safety" :class="['safety-panel', 'safety-panel--' + riskMeta.type]">
              <div class="safety-summary">
                <div class="safety-shield"><i class="el-icon-warning-outline" /></div>
                <div>
                  <span>用药风险提示</span>
                  <strong>{{ riskMeta.label }}</strong>
                </div>
                <div v-if="riskFindings.length" class="risk-tags">
                  <span v-for="item in riskFindings" :key="item.label" class="risk-tag">
                    {{ findingLabel(item.label) }}
                  </span>
                </div>
              </div>
              <ul v-if="safetyWarnings.length" class="warning-list">
                <li v-for="warning in safetyWarnings" :key="warning">{{ warning }}</li>
              </ul>
            </div>

            <div class="sources-panel">
              <div class="subsection-heading">
                <div>
                  <h3>参考来源</h3>
                  <p>建议结合原始说明书核对具体条款。</p>
                </div>
                <span class="source-count">{{ sources.length }} 条依据</span>
              </div>

              <div v-if="sources.length" class="source-list">
                <article v-for="(source, index) in sources" :key="source.source_anchor || index" class="source-item">
                  <div class="source-order">{{ index + 1 }}</div>
                  <div class="source-content">
                    <strong>{{ source.title || '药品说明书' }}</strong>
                    <div class="source-meta">
                      <span v-if="source.section"><i class="el-icon-collection-tag" /> {{ source.section }}</span>
                      <span v-if="pageLabel(source)"><i class="el-icon-document" /> {{ pageLabel(source) }}</span>
                    </div>
                    <p v-if="source.preview">{{ source.preview }}</p>
                  </div>
                </article>
              </div>
              <div v-else class="source-empty">
                <i class="el-icon-document-delete" />
                <div>
                  <strong>未检索到可引用的说明书依据</strong>
                  <p>请补充具体药品名称、通用名或用药场景后重新查询。</p>
                </div>
              </div>
            </div>

            <div class="result-footer">
              <span><i class="el-icon-info" /> 自动回答可能存在遗漏，请以药品说明书及专业人员意见为准。</span>
              <span v-if="result.requestId" class="request-id">请求编号：{{ result.requestId }}</span>
            </div>
          </section>

          <section v-else class="empty-panel">
            <div class="empty-illustration"><i class="el-icon-reading" /></div>
            <h3>从一个具体的药品问题开始</h3>
            <p>输入药品名称以及想了解的事项，系统会返回说明书依据和风险提醒。</p>
            <div class="empty-features">
              <span><i class="el-icon-circle-check" /> 说明书检索</span>
              <span><i class="el-icon-circle-check" /> 来源定位</span>
              <span><i class="el-icon-circle-check" /> 风险提示</span>
            </div>
          </section>
        </main>

        <aside class="guide-column">
          <el-card class="guide-card" shadow="never">
            <div class="guide-title">
              <i class="el-icon-s-opportunity" />
              <span>怎样提问更准确</span>
            </div>
            <ol class="guide-list">
              <li>
                <span>1</span>
                <div><strong>说明药品名称</strong><p>优先填写通用名，可补充商品名。</p></div>
              </li>
              <li>
                <span>2</span>
                <div><strong>明确查询事项</strong><p>如用法用量、禁忌、不良反应。</p></div>
              </li>
              <li>
                <span>3</span>
                <div><strong>补充必要场景</strong><p>如儿童、孕期或与其他药同用。</p></div>
              </li>
            </ol>
          </el-card>

          <div class="boundary-card">
            <div class="boundary-title"><i class="el-icon-circle-close" /> 助手能力边界</div>
            <p>不提供诊断，不替代处方，不直接给出个体化停药、换药或剂量调整决定。</p>
          </div>

          <div class="consult-card">
            <i class="el-icon-phone-outline" />
            <div>
              <strong>出现严重不适？</strong>
              <p>请立即停止自行判断，及时联系医生、执业药师或急救机构。</p>
            </div>
          </div>
        </aside>
      </div>
    </div>
  </div>
</template>

<script>
import { askKnowledge } from '@/api/knowledge'

const SECTION_TITLES = [
  '结论',
  '系统初步结论',
  '依据说明书',
  '用药/购药建议',
  '安全等级',
  '风险标签',
  '需要咨询医生或药师的情况',
  '来源'
]

export default {
  name: 'KnowledgeAssistant',
  data() {
    return {
      question: '',
      loading: false,
      result: null,
      exampleQuestions: [
        '阿莫西林有哪些常见不良反应？',
        '布洛芬哪些人群需要慎用？',
        '二甲双胍可以和哪些药同服？'
      ]
    }
  },
  computed: {
    sources() {
      return this.result && Array.isArray(this.result.sources) ? this.result.sources : []
    },
    safetyWarnings() {
      const safety = this.result && this.result.safety
      return safety && Array.isArray(safety.warnings) ? safety.warnings : []
    },
    riskFindings() {
      const safety = this.result && this.result.safety
      return safety && Array.isArray(safety.findings) ? safety.findings : []
    },
    riskMeta() {
      const safety = this.result && this.result.safety
      const level = safety && safety.risk_level ? safety.risk_level : 'unknown'
      const riskMap = {
        low: { label: '低风险提示', type: 'low' },
        medium: { label: '中风险提示', type: 'medium' },
        high: { label: '高风险提示', type: 'high' }
      }
      return riskMap[level] || { label: '风险待确认', type: 'unknown' }
    },
    answerSections() {
      const answer = this.result && this.result.answer
      if (!answer) return []

      const sections = []
      let current = null
      answer.replace(/\r/g, '').split('\n').forEach(line => {
        const match = line.match(/^(.+?)[：:]\s*(.*)$/)
        if (match && SECTION_TITLES.includes(match[1].trim())) {
          if (current && current.content.length) sections.push(this.buildSection(current))
          current = { title: match[1].trim(), content: match[2] ? [match[2]] : [] }
        } else if (current) {
          current.content.push(line)
        } else if (line.trim()) {
          current = { title: '回答', content: [line] }
        }
      })
      if (current && current.content.length) sections.push(this.buildSection(current))

      const separatedSections = ['来源', '安全等级', '风险标签', '需要咨询医生或药师的情况']
      return sections.filter(section => !separatedSections.includes(section.title))
    }
  },
  methods: {
    buildSection(section) {
      return {
        title: section.title === '系统初步结论' ? '结论' : section.title,
        content: section.content.join('\n').trim()
      }
    },
    useExample(question) {
      this.question = question
    },
    clearQuestion() {
      this.question = ''
    },
    findingLabel(label) {
      const labels = {
        adverse_reaction: '不良反应',
        contraindication: '禁忌',
        interaction: '药物相互作用',
        pregnancy: '孕期用药',
        lactation: '哺乳期用药',
        child: '儿童用药',
        elderly: '老年人用药',
        renal_hepatic: '肝肾功能异常',
        overdose: '过量用药',
        allergy: '过敏风险',
        emergency: '紧急情况',
        source_high_risk: '说明书高风险提示'
      }
      return labels[label] || '用药注意事项'
    },
    pageLabel(source) {
      if (source.page_start == null && source.page_end == null) return ''
      if (source.page_start === source.page_end || source.page_end == null) return `第 ${source.page_start} 页`
      return `第 ${source.page_start}-${source.page_end} 页`
    },
    submitQuestion() {
      if (!this.question) {
        this.$message.warning('请输入要查询的药品问题')
        return
      }
      this.loading = true
      this.result = null
      askKnowledge({ question: this.question, domain: 'drug' })
        .then(response => {
          this.result = response.data
        })
        .catch(error => {
          console.error(error)
        })
        .finally(() => {
          this.loading = false
        })
    }
  }
}
</script>

<style scoped lang="scss">
$navy: #17324d;
$blue: #2878ff;
$muted: #6b7a8c;
$border: #e6edf5;

.knowledge-page {
  min-height: calc(100vh - 84px);
  padding: 24px;
  background: radial-gradient(circle at 7% 3%, rgba(49, 130, 246, 0.08), transparent 24%), #f5f8fc;
}

.knowledge-shell { max-width: 1320px; margin: 0 auto; }

.hero-panel {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 154px;
  padding: 28px 32px;
  overflow: hidden;
  color: #fff;
  background: linear-gradient(120deg, #143452 0%, #1c4c73 58%, #26709d 100%);
  border-radius: 16px;
  box-shadow: 0 12px 32px rgba(23, 50, 77, 0.14);
}

.hero-panel::after {
  position: absolute;
  top: -100px;
  right: 180px;
  width: 260px;
  height: 260px;
  content: '';
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 50%;
  box-shadow: 0 0 0 45px rgba(255, 255, 255, 0.025), 0 0 0 90px rgba(255, 255, 255, 0.018);
}

.hero-icon {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 70px;
  height: 70px;
  margin-right: 22px;
  font-size: 33px;
  color: #d9eeff;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 18px;
}

.hero-content { position: relative; z-index: 1; }
.hero-content h1 { margin: 8px 0 6px; font-size: 27px; font-weight: 600; letter-spacing: 1px; }
.hero-content p { margin: 0; color: rgba(255, 255, 255, 0.72); font-size: 14px; }
.hero-badges { display: flex; gap: 8px; }

.hero-badge {
  padding: 4px 9px;
  color: #e8f6ff;
  font-size: 12px;
  background: rgba(72, 180, 255, 0.2);
  border: 1px solid rgba(137, 211, 255, 0.22);
  border-radius: 20px;
}

.hero-badge--light { background: rgba(255, 255, 255, 0.08); }

.hero-notice {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  max-width: 360px;
  padding: 14px 16px;
  margin-left: auto;
  color: rgba(255, 255, 255, 0.84);
  font-size: 13px;
  line-height: 1.7;
  background: rgba(6, 25, 43, 0.22);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 10px;
}

.hero-notice i { margin: 4px 9px 0 0; color: #ffd27d; font-size: 17px; }
.workspace-grid { display: grid; grid-template-columns: minmax(0, 1fr) 300px; gap: 20px; margin-top: 20px; }
.main-column, .guide-column { min-width: 0; }
.question-card, .guide-card { border: 1px solid $border; border-radius: 14px; }
.question-card ::v-deep .el-card__body, .guide-card ::v-deep .el-card__body { padding: 24px; }
.section-heading, .subsection-heading, .result-header { display: flex; align-items: flex-start; justify-content: space-between; }
.section-heading h2, .result-header h2 { margin: 5px 0 0; color: $navy; font-size: 20px; font-weight: 600; }
.section-kicker { color: $blue; font-size: 12px; font-weight: 600; letter-spacing: 1px; }
.step-label { color: #9aa8b8; font-size: 12px; }

.example-row { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin: 20px 0 12px; }
.example-label { margin-right: 2px; color: #7c8b9c; font-size: 12px; }

.example-chip {
  padding: 6px 11px;
  color: #52677d;
  font-size: 12px;
  cursor: pointer;
  background: #f5f8fc;
  border: 1px solid #e2eaf3;
  border-radius: 16px;
  transition: all 0.2s;
}

.example-chip:hover { color: $blue; background: #f0f6ff; border-color: #9fc2ff; }

.question-form ::v-deep .el-textarea__inner {
  padding: 15px 16px 34px;
  color: #26394d;
  line-height: 1.8;
  background: #fbfcfe;
  border-color: #dfe7f0;
  border-radius: 10px;
  resize: vertical;
}

.question-form ::v-deep .el-textarea__inner:focus { background: #fff; border-color: #75a8ff; box-shadow: 0 0 0 3px rgba(40, 120, 255, 0.08); }
.form-footer { display: flex; align-items: center; justify-content: space-between; margin-top: 16px; }
.input-hint { color: #93a0ae; font-size: 12px; }
.input-hint i { margin-right: 5px; }
.form-actions { display: flex; gap: 8px; }
.form-actions .el-button { border-radius: 8px; }
.form-actions .el-button--primary { min-width: 138px; background: $blue; border-color: $blue; box-shadow: 0 5px 12px rgba(40, 120, 255, 0.2); }
.form-actions .el-button i { margin-right: 5px; }
.loading-card { margin-top: 20px; border: 1px solid $border; border-radius: 14px; }
.loading-card ::v-deep .el-card__body { display: flex; align-items: center; padding: 32px; }

.loading-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  margin-right: 16px;
  color: $blue;
  font-size: 22px;
  background: #edf4ff;
  border-radius: 12px;
}

.loading-card strong { color: $navy; }
.loading-card p { margin: 6px 0 0; color: $muted; font-size: 13px; }
.result-panel { padding: 24px; margin-top: 20px; background: #fff; border: 1px solid $border; border-radius: 14px; }
.result-statuses { display: flex; gap: 8px; }

.status-pill {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  color: #65758a;
  font-size: 12px;
  background: #f4f6f8;
  border-radius: 16px;
}

.status-pill i { margin-right: 4px; }
.status-pill--success, .status-pill--low { color: #16805d; background: #eaf8f2; }
.status-pill--medium { color: #a56600; background: #fff4dc; }
.status-pill--high { color: #bd3f3f; background: #fff0f0; }
.answer-card { margin-top: 20px; overflow: hidden; border: 1px solid #e4ebf3; border-radius: 12px; }

.answer-card__header {
  display: flex;
  align-items: center;
  padding: 15px 18px;
  background: #f8fafc;
  border-bottom: 1px solid #e8eef5;
}

.answer-icon { display: flex; align-items: center; justify-content: center; width: 36px; height: 36px; margin-right: 11px; color: $blue; background: #e8f1ff; border-radius: 9px; }
.answer-card__header strong { display: block; color: $navy; font-size: 14px; }
.answer-card__header span { display: block; margin-top: 3px; color: #99a5b3; font-size: 11px; }
.answer-sections { padding: 4px 20px 8px; }
.answer-section { padding: 18px 0; border-bottom: 1px solid #edf1f5; }
.answer-section:last-child { border-bottom: 0; }

.answer-section--primary {
  padding: 17px 18px;
  margin: 16px 0 4px;
  background: linear-gradient(135deg, #f1f6ff, #f8fbff);
  border: 1px solid #dce9ff;
  border-radius: 10px;
}

.answer-section__title { display: flex; align-items: center; margin-bottom: 10px; color: $navy; font-size: 14px; font-weight: 600; }

.answer-section__index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  margin-right: 8px;
  color: $blue;
  font-size: 11px;
  background: #e8f1ff;
  border-radius: 6px;
}

.answer-section__content, .answer-text { color: #405166; font-size: 14px; line-height: 1.9; white-space: pre-wrap; }
.answer-text { padding: 20px; }
.safety-panel { padding: 18px; margin-top: 16px; background: #fff9ed; border: 1px solid #f5dfae; border-radius: 12px; }
.safety-panel--low { background: #f1faf6; border-color: #caeadd; }
.safety-panel--high { background: #fff4f4; border-color: #f2cccc; }
.safety-summary { display: flex; align-items: center; }

.safety-shield {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  margin-right: 11px;
  color: #bd7a0a;
  background: rgba(255, 190, 65, 0.16);
  border-radius: 9px;
}

.safety-summary span, .safety-summary strong { display: block; }
.safety-summary > div > span { color: #987038; font-size: 11px; }
.safety-summary > div > strong { margin-top: 3px; color: #6f501f; font-size: 14px; }
.risk-tags { display: flex; flex-wrap: wrap; gap: 6px; margin-left: auto; }
.risk-tag { padding: 5px 9px; color: #8d672c; font-size: 11px; background: rgba(255, 255, 255, 0.65); border: 1px solid rgba(199, 145, 60, 0.22); border-radius: 14px; }

.warning-list {
  padding: 13px 0 0 21px;
  margin: 14px 0 0;
  color: #715c3d;
  font-size: 13px;
  line-height: 1.8;
  border-top: 1px solid rgba(184, 133, 51, 0.16);
}

.subsection-heading { align-items: center; margin: 24px 0 12px; }
.subsection-heading h3 { margin: 0; color: $navy; font-size: 16px; }
.subsection-heading p { margin: 5px 0 0; color: #8c99a8; font-size: 12px; }
.source-count { color: #7d8b9b; font-size: 12px; }
.source-list { display: grid; gap: 10px; }
.source-item { display: flex; padding: 15px; background: #fbfcfe; border: 1px solid #e7edf4; border-radius: 10px; }

.source-order {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  margin-right: 12px;
  color: $blue;
  font-size: 12px;
  font-weight: 600;
  background: #eaf2ff;
  border-radius: 8px;
}

.source-content { min-width: 0; }
.source-content strong { color: #2f4359; font-size: 13px; }
.source-meta { display: flex; flex-wrap: wrap; gap: 14px; margin-top: 7px; color: #8391a2; font-size: 11px; }

.source-content p {
  display: -webkit-box;
  margin: 8px 0 0;
  overflow: hidden;
  color: #68798b;
  font-size: 12px;
  line-height: 1.65;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.source-empty { display: flex; align-items: center; padding: 18px; color: #8290a0; background: #fafbfc; border: 1px dashed #d9e1ea; border-radius: 10px; }
.source-empty > i { margin-right: 12px; font-size: 24px; }
.source-empty strong { color: #5e6e80; font-size: 13px; }
.source-empty p { margin: 5px 0 0; font-size: 12px; }
.result-footer { display: flex; justify-content: space-between; gap: 16px; padding-top: 16px; margin-top: 18px; color: #96a2b0; font-size: 11px; border-top: 1px solid #edf1f5; }
.request-id { flex: 0 0 auto; }
.empty-panel { padding: 46px 24px; margin-top: 20px; text-align: center; background: #fff; border: 1px solid $border; border-radius: 14px; }

.empty-illustration {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 68px;
  height: 68px;
  margin: 0 auto 17px;
  color: $blue;
  font-size: 30px;
  background: linear-gradient(135deg, #e6f0ff, #f4f8ff);
  border-radius: 20px;
}

.empty-panel h3 { margin: 0; color: $navy; font-size: 17px; }
.empty-panel > p { margin: 9px 0 18px; color: $muted; font-size: 13px; }
.empty-features { display: flex; justify-content: center; gap: 20px; color: #738397; font-size: 12px; }
.empty-features i { margin-right: 4px; color: #3db08a; }
.guide-column { align-self: start; position: sticky; top: 104px; }
.guide-title { display: flex; align-items: center; color: $navy; font-size: 15px; font-weight: 600; }
.guide-title i { margin-right: 8px; color: #f0a43c; font-size: 19px; }
.guide-list { padding: 0; margin: 20px 0 0; list-style: none; }
.guide-list li { display: flex; padding-bottom: 18px; margin-bottom: 18px; border-bottom: 1px solid #edf1f5; }
.guide-list li:last-child { padding-bottom: 0; margin-bottom: 0; border-bottom: 0; }

.guide-list li > span {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  margin-right: 11px;
  color: $blue;
  font-size: 12px;
  font-weight: 600;
  background: #edf4ff;
  border-radius: 8px;
}

.guide-list strong { color: #3c5066; font-size: 13px; }
.guide-list p { margin: 5px 0 0; color: #8996a5; font-size: 12px; line-height: 1.6; }
.boundary-card, .consult-card { margin-top: 14px; border-radius: 12px; }
.boundary-card { padding: 18px; color: #7c613e; background: #fff9ef; border: 1px solid #f3e2c4; }
.boundary-title { margin-bottom: 8px; color: #6a4e2b; font-size: 13px; font-weight: 600; }
.boundary-title i { margin-right: 5px; color: #d99539; }
.boundary-card p, .consult-card p { margin: 0; font-size: 12px; line-height: 1.7; }
.consult-card { display: flex; padding: 17px; color: #5c7690; background: #eef6fb; border: 1px solid #d9eaf4; }
.consult-card > i { margin: 2px 10px 0 0; color: #337faa; font-size: 19px; }
.consult-card strong { display: block; margin-bottom: 5px; color: #315c77; font-size: 13px; }

@media (max-width: 1100px) {
  .workspace-grid { grid-template-columns: 1fr; }
  .guide-column { display: grid; position: static; grid-template-columns: 1fr 1fr; gap: 14px; }
  .guide-card { grid-row: span 2; }
  .boundary-card, .consult-card { margin-top: 0; }
}

@media (max-width: 768px) {
  .knowledge-page { padding: 12px; }
  .hero-panel { display: block; padding: 22px; }
  .hero-icon { width: 52px; height: 52px; margin: 0 0 15px; font-size: 25px; }
  .hero-content h1 { font-size: 23px; }
  .hero-notice { max-width: none; margin: 18px 0 0; }
  .question-card ::v-deep .el-card__body, .result-panel { padding: 18px; }
  .example-row, .form-footer, .result-header, .result-footer { align-items: stretch; flex-direction: column; }
  .form-actions { justify-content: flex-end; }
  .result-statuses { margin-top: 12px; }
  .safety-summary { align-items: flex-start; flex-wrap: wrap; }
  .risk-tags { width: 100%; margin: 12px 0 0 49px; }
  .guide-column { display: block; }
  .boundary-card, .consult-card { margin-top: 14px; }
  .empty-features { flex-direction: column; gap: 9px; }
}
</style>
