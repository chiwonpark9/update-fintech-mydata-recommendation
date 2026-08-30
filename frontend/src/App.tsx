import './App.css'
import { BackendStatus } from './features/system/components/BackendStatus'

const modules = [
  {
    number: '01',
    eyebrow: 'MYDATA SUMMARY',
    title: '흩어진 소비를\n한눈에 정리해요',
    description:
      '카드·계좌 사용 내역을 공통 기준으로 분류해 고객의 실제 소비 습관을 보여줍니다.',
    tags: ['자산 연결', '소비 카테고리', '월간 리포트'],
    icon: 'data',
  },
  {
    number: '02',
    eyebrow: 'CARD MATCHING',
    title: '혜택보다 먼저\n생활을 이해해요',
    description:
      '소비 금액과 실적 조건을 함께 계산해, 고객이 실제로 받을 수 있는 혜택을 비교합니다.',
    tags: ['근거 기반 점수', '실적 계산', '추천 설명'],
    icon: 'card',
  },
  {
    number: '03',
    eyebrow: 'AI ADVISOR',
    title: '말로 설명하면\n조건을 찾아드려요',
    description:
      '채팅과 음성으로 상황을 듣고, 상품 정보에 근거한 카드 후보와 추천 이유를 안내합니다.',
    tags: ['대화형 탐색', 'RAG 근거', '음성 상담'],
    icon: 'chat',
  },
]

const categories = [
  { name: '생활·마트', value: '38%', width: '84%' },
  { name: '교통', value: '24%', width: '62%' },
  { name: '구독·통신', value: '18%', width: '47%' },
]

function BrandMark() {
  return (
    <svg viewBox="0 0 38 38" role="img" aria-label="MyData 로고">
      <rect width="38" height="38" rx="12" fill="currentColor" />
      <path d="M10 12.5h18v13H10z" fill="var(--signal)" />
      <path d="M14 17h10M14 21h6" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" />
    </svg>
  )
}

function ModuleIcon({ name }: { name: string }) {
  if (name === 'data') {
    return (
      <svg viewBox="0 0 48 48" aria-hidden="true">
        <path d="M10 36V24M19 36V15M28 36V28M37 36V10" />
      </svg>
    )
  }

  if (name === 'card') {
    return (
      <svg viewBox="0 0 48 48" aria-hidden="true">
        <rect x="7" y="12" width="34" height="24" rx="5" />
        <path d="M7 20h34M13 29h9" />
      </svg>
    )
  }

  return (
    <svg viewBox="0 0 48 48" aria-hidden="true">
      <path d="M9 12h30v22H23l-8 6v-6H9z" />
      <path d="M16 21h16M16 27h10" />
    </svg>
  )
}

function App() {
  return (
    <div className="site-shell">
      <header className="site-header">
        <a className="brand" href="#top" aria-label="처음으로 이동">
          <BrandMark />
          <span>
            <strong>MyData</strong>
            <small>Card Intelligence</small>
          </span>
        </a>

        <nav aria-label="주요 메뉴">
          <a href="#modules">서비스 모듈</a>
          <a href="#foundation">구현 방향</a>
          <a href="#system">연결 상태</a>
        </nav>

        <a className="header-cta" href="#modules">
          서비스 살펴보기
          <span aria-hidden="true">↗</span>
        </a>
      </header>

      <main id="top">
        <section className="hero-section" aria-labelledby="hero-title">
          <div className="hero-copy">
            <p className="eyebrow">
              <span /> MYDATA-DRIVEN FINTECH
            </p>
            <h1 id="hero-title">
              소비 데이터가
              <br />
              카드 선택의 <em>근거</em>가 되도록
            </h1>
            <p className="hero-description">
              사용자의 실제 소비 패턴을 읽고, 받을 수 있는 혜택을 계산하고,
              추천 이유까지 설명하는 금융상품 추천 모듈입니다.
            </p>
            <div className="hero-actions">
              <a className="primary-button" href="#modules">
                추천 과정 보기 <span aria-hidden="true">→</span>
              </a>
              <a className="text-link" href="#foundation">
                프로젝트 구현 원칙
              </a>
            </div>
            <div id="system">
              <BackendStatus />
            </div>
          </div>

          <div className="insight-visual" aria-label="샘플 소비 분석 화면">
            <div className="visual-orbit orbit-one" />
            <div className="visual-orbit orbit-two" />
            <div className="insight-card">
              <div className="insight-header">
                <div>
                  <span className="panel-label">이번 달 소비</span>
                  <strong>1,284,600원</strong>
                </div>
                <span className="demo-badge">DEMO DATA</span>
              </div>

              <div className="category-list">
                {categories.map((category) => (
                  <div className="category-row" key={category.name}>
                    <div>
                      <span>{category.name}</span>
                      <strong>{category.value}</strong>
                    </div>
                    <div className="progress-track">
                      <span style={{ width: category.width }} />
                    </div>
                  </div>
                ))}
              </div>

              <div className="recommendation-preview">
                <span className="spark" aria-hidden="true">✦</span>
                <div>
                  <span>소비 패턴 분석 결과</span>
                  <strong>생활비 절약형 카드가 잘 맞아요</strong>
                </div>
                <span className="match-score">92</span>
              </div>
            </div>

            <div className="floating-note note-top">
              <span>예상 연간 혜택</span>
              <strong>+ 124,000원</strong>
            </div>
            <div className="floating-note note-bottom">
              <span className="check" aria-hidden="true">✓</span>
              <div>
                <strong>근거 확인 완료</strong>
                <span>상품 약관 · 소비 데이터</span>
              </div>
            </div>
          </div>
        </section>

        <section className="trust-strip" aria-label="서비스 핵심 원칙">
          <p>데이터는 안전하게</p>
          <span />
          <p>추천은 투명하게</p>
          <span />
          <p>설명은 이해하기 쉽게</p>
        </section>

        <section className="modules-section" id="modules" aria-labelledby="modules-title">
          <div className="section-heading">
            <div>
              <p className="eyebrow"><span /> EMBEDDABLE MODULES</p>
              <h2 id="modules-title">필요한 기능만 골라<br />서비스에 연결합니다</h2>
            </div>
            <p>
              금융사 홈페이지에 독립적으로 탑재할 수 있도록 사용자 정보,
              카드 추천, AI 상담 기능을 분리된 모듈로 설계합니다.
            </p>
          </div>

          <div className="module-grid">
            {modules.map((module) => (
              <article className="module-card" key={module.number}>
                <div className="module-topline">
                  <span>{module.number}</span>
                  <div className="module-icon">
                    <ModuleIcon name={module.icon} />
                  </div>
                </div>
                <p className="module-eyebrow">{module.eyebrow}</p>
                <h3>{module.title.split('\n').map((line) => <span key={line}>{line}</span>)}</h3>
                <p className="module-description">{module.description}</p>
                <ul>
                  {module.tags.map((tag) => <li key={tag}>{tag}</li>)}
                </ul>
              </article>
            ))}
          </div>
        </section>

        <section className="foundation-section" id="foundation" aria-labelledby="foundation-title">
          <div className="foundation-copy">
            <p className="eyebrow light"><span /> BUILD FOUNDATION</p>
            <h2 id="foundation-title">보이는 화면보다<br />신뢰할 수 있는 과정부터</h2>
            <p>
              포트폴리오를 위한 데모에 그치지 않고, 실제 서비스에서 설명하고
              운영할 수 있는 구조를 단계별로 완성합니다.
            </p>
          </div>

          <ol className="foundation-list">
            <li className="active">
              <span>01</span>
              <div>
                <strong>서비스 기반과 연결</strong>
                <p>Spring API와 React 화면의 계약을 테스트로 검증합니다.</p>
              </div>
              <span className="step-status">NOW</span>
            </li>
            <li>
              <span>02</span>
              <div>
                <strong>사용자와 마이데이터</strong>
                <p>인증, 권한, 합성 거래 데이터를 안전한 흐름으로 연결합니다.</p>
              </div>
              <span className="step-status">NEXT</span>
            </li>
            <li>
              <span>03</span>
              <div>
                <strong>추천과 운영 고도화</strong>
                <p>추천 엔진, Redis, AI 상담, AWS 운영까지 확장합니다.</p>
              </div>
              <span className="step-status">PLAN</span>
            </li>
          </ol>
        </section>
      </main>

      <footer>
        <div className="brand footer-brand">
          <BrandMark />
          <span><strong>MyData</strong><small>Card Intelligence</small></span>
        </div>
        <p>MyData-based card recommendation service · 2026</p>
        <a href="#top">맨 위로 ↑</a>
      </footer>
    </div>
  )
}

export default App
