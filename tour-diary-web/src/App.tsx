import {
  BookOpen,
  Camera,
  Image,
  Loader2,
  MapPinned,
  PawPrint,
  Play,
  Sparkles,
} from 'lucide-react';
import { FormEvent, useMemo, useState } from 'react';
import { generateDiary, type GenerateDiaryResponse } from './api';
import { debugText, demoDiary } from './demoData';

type Tab = 'result' | 'debug';

function App() {
  const [dogId, setDogId] = useState('1');
  const [walkRecordId, setWalkRecordId] = useState('3');
  const [activeTab, setActiveTab] = useState<Tab>('result');
  const [diary, setDiary] = useState<GenerateDiaryResponse>(demoDiary);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [useDemo, setUseDemo] = useState(true);

  const detectedText = useMemo(() => diary.detectedObjects.join(' · '), [diary.detectedObjects]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);

    if (useDemo) {
      setDiary(demoDiary);
      return;
    }

    setIsLoading(true);
    try {
      const result = await generateDiary({ dogId, walkRecordId });
      setDiary(result);
      setActiveTab('result');
    } catch (err) {
      setError(err instanceof Error ? err.message : '알 수 없는 오류가 발생했습니다.');
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">
            <PawPrint size={22} />
          </span>
          <div>
            <strong>댕댕이 투어 일기</strong>
            <span>AI 그림일기 생성</span>
          </div>
        </div>

        <form className="generate-panel" onSubmit={handleSubmit}>
          <label>
            <span>강아지 ID</span>
            <input value={dogId} onChange={(event) => setDogId(event.target.value)} />
          </label>
          <label>
            <span>산책 기록 ID</span>
            <input value={walkRecordId} onChange={(event) => setWalkRecordId(event.target.value)} />
          </label>
          <label className="toggle-row">
            <input
              type="checkbox"
              checked={useDemo}
              onChange={(event) => setUseDemo(event.target.checked)}
            />
            <span>데모 데이터로 보기</span>
          </label>
          <button className="primary-button" type="submit" disabled={isLoading}>
            {isLoading ? <Loader2 className="spin" size={18} /> : <Play size={18} />}
            그림일기 생성
          </button>
          {error && <p className="error-text">{error}</p>}
        </form>

        <nav className="flow-list" aria-label="MVP 생성 흐름">
          <FlowItem icon={<Camera size={17} />} text="사진 업로드" />
          <FlowItem icon={<Sparkles size={17} />} text="Vision 분석" />
          <FlowItem icon={<BookOpen size={17} />} text="강아지 시점 일기" />
          <FlowItem icon={<Image size={17} />} text="크레파스 그림 생성" />
          <FlowItem icon={<MapPinned size={17} />} text="관광데이터 추천" />
        </nav>
      </aside>

      <section className="workspace">
        <header className="workspace-header">
          <div>
            <p className="eyebrow">관광데이터와 연결되는 반려견 시점 AI 그림일기</p>
            <h1>{diary.title}</h1>
          </div>
          <div className="tabs" role="tablist">
            <button
              className={activeTab === 'result' ? 'active' : ''}
              type="button"
              onClick={() => setActiveTab('result')}
            >
              결과
            </button>
            <button
              className={activeTab === 'debug' ? 'active' : ''}
              type="button"
              onClick={() => setActiveTab('debug')}
            >
              프롬프트
            </button>
          </div>
        </header>

        {activeTab === 'result' ? (
          <ResultView diary={diary} detectedText={detectedText} />
        ) : (
          <DebugView />
        )}
      </section>
    </main>
  );
}

function FlowItem({ icon, text }: { icon: React.ReactNode; text: string }) {
  return (
    <div className="flow-item">
      {icon}
      <span>{text}</span>
    </div>
  );
}

function ResultView({ diary, detectedText }: { diary: GenerateDiaryResponse; detectedText: string }) {
  return (
    <div className="result-layout">
      <section className="visual-compare" aria-label="원본 사진과 AI 그림일기">
        <figure className="media-frame original-frame">
          {diary.originalImageUrl ? (
            <img src={diary.originalImageUrl} alt="원본 산책 사진" />
          ) : (
            <div className="placeholder-photo">
              <Camera size={44} />
              <span>원본 산책 사진</span>
            </div>
          )}
          <figcaption>원본 산책 사진</figcaption>
        </figure>
        <figure className="media-frame drawing-frame">
          {diary.generatedImageUrl ? (
            <img src={diary.generatedImageUrl} alt="AI 그림일기 이미지" />
          ) : (
            <div className="crayon-preview" aria-label="AI 그림일기 예시">
              <div className="sun" />
              <div className="bench" />
              <div className="path" />
              <div className="dog-body" />
              <div className="dog-head" />
              <div className="leaf leaf-a" />
              <div className="leaf leaf-b" />
              <div className="leaf leaf-c" />
            </div>
          )}
          <figcaption>AI 그림일기 이미지</figcaption>
        </figure>
      </section>

      <section className="diary-section">
        <div className="section-heading">
          <BookOpen size={19} />
          <h2>오늘의 일기</h2>
        </div>
        <p className="diary-content">{diary.content}</p>
        <div className="detected-row">
          <span>Vision 분석 객체</span>
          <strong>{detectedText}</strong>
        </div>
      </section>

      <section className="places-section">
        <div className="section-heading">
          <MapPinned size={19} />
          <h2>다음 산책 추천</h2>
        </div>
        <div className="places-grid">
          {diary.recommendedPlaces.map((place) => (
            <article className="place-card" key={`${place.name}-${place.category}`}>
              <span>{place.category ?? '추천 장소'}</span>
              <h3>{place.name}</h3>
              <p>{place.reason}</p>
              {place.address && <small>{place.address}</small>}
            </article>
          ))}
        </div>
      </section>
    </div>
  );
}

function DebugView() {
  return (
    <div className="debug-layout">
      <DebugBlock title="사진 분석 결과" content={debugText.vision} />
      <DebugBlock title="일기 생성 프롬프트" content={debugText.diaryPrompt} />
      <DebugBlock title="이미지 생성 프롬프트" content={debugText.imagePrompt} />
    </div>
  );
}

function DebugBlock({ title, content }: { title: string; content: string }) {
  return (
    <section className="debug-block">
      <h2>{title}</h2>
      <pre>{content}</pre>
    </section>
  );
}

export default App;
