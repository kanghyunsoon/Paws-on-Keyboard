import {
  BookOpen,
  Camera,
  CloudSun,
  Dog,
  Heart,
  Home,
  ImagePlus,
  Loader2,
  LogOut,
  MapPinned,
  MessageCircle,
  PawPrint,
  Play,
  Search,
  Settings,
  Sparkles,
  Trophy,
  UserRound,
  Users,
} from 'lucide-react';
import { FormEvent, ReactNode, useEffect, useRef, useState } from 'react';
import {
  createWalkRecord,
  generateDiary as requestDiaryGeneration,
  login as requestLogin,
  saveUserProfile,
  signup as requestSignup,
  uploadImage,
  type GenerateDiaryResponse,
} from './api';
import './styles.css';

type View = 'create' | 'memories' | 'community' | 'profile' | 'settings' | 'result';
type OwnerRole = '엄마' | '아빠' | '누나' | '언니' | '형' | '오빠' | '친구';

type User = {
  id: string;
  email: string;
  name: string;
  password?: string;
  token?: string;
  tokenExpiresAt?: string;
};

type Profile = {
  userName: string;
  userGender: string;
  ownerRole: OwnerRole;
  ownerNickname: string;
  relationshipNote: string;
  ownerPhotoName: string | null;
  ownerPhotoPreview: string | null;
  ownerPhotoUrl: string | null;
  dogName: string;
  dogAge: string;
  dogBreed: string;
  dogPersonality: string;
  dogAppearance: string;
  dogFavoriteThings: string;
  dogDislikedThings: string;
  dogPhotoName: string | null;
  dogPhotoPreview: string | null;
  dogPhotoUrl: string | null;
};

type Draft = {
  walkPhotoName: string | null;
  walkPhotoPreview: string | null;
  walkPhotoUrl: string | null;
  walkRecordId: string | null;
  place: string;
  weather: string;
  activity: string;
  story: string;
};

type Palette = {
  average: string;
  darkRatio: number;
  lightRatio: number;
  brownRatio: number;
};

type Diary = {
  id: string;
  title: string;
  content: string;
  drawing: string;
  fallbackDrawing?: string;
  place: string;
  weather: string;
  createdAt: string;
  originalPhoto: string | null;
  recommendations: { name: string; category: string; reason: string }[];
};

type Post = {
  id: string;
  authorId: string;
  authorName: string;
  dogName: string;
  image: string | null;
  title: string;
  content: string;
  place: string;
  createdAt: string;
  likes: string[];
  comments: { id: string; authorName: string; content: string }[];
};

const USERS_KEY = 'paws-users-v4';
const SESSION_KEY = 'paws-session-v3';
const POSTS_KEY = 'paws-posts-v4';

const emptyProfile: Profile = {
  userName: '',
  userGender: '',
  ownerRole: '엄마',
  ownerNickname: '',
  relationshipNote: '',
  ownerPhotoName: null,
  ownerPhotoPreview: null,
  ownerPhotoUrl: null,
  dogName: '',
  dogAge: '',
  dogBreed: '',
  dogPersonality: '',
  dogAppearance: '',
  dogFavoriteThings: '',
  dogDislikedThings: '',
  dogPhotoName: null,
  dogPhotoPreview: null,
  dogPhotoUrl: null,
};

const emptyDraft: Draft = {
  walkPhotoName: null,
  walkPhotoPreview: null,
  walkPhotoUrl: null,
  walkRecordId: null,
  place: '',
  weather: '',
  activity: '',
  story: '',
};

export default function App() {
  const [user, setUser] = useState<User | null>(() => readJson<User | null>(SESSION_KEY, null));
  const [profile, setProfile] = useState<Profile>(emptyProfile);
  const [draft, setDraft] = useState<Draft>(emptyDraft);
  const [view, setView] = useState<View>('settings');
  const [diaries, setDiaries] = useState<Diary[]>([]);
  const [posts, setPosts] = useState<Post[]>(() => readJson<Post[]>(POSTS_KEY, []));
  const [currentDiary, setCurrentDiary] = useState<Diary | null>(null);
  const [loading, setLoading] = useState(false);
  const [step, setStep] = useState('');

  const ownerPhotoRef = useRef<HTMLInputElement>(null);
  const dogPhotoRef = useRef<HTMLInputElement>(null);
  const walkPhotoRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (!user) return;
    const nextProfile = readJson<Profile>(profileKey(user.id), emptyProfile);
    const nextDiaries = readJson<Diary[]>(diariesKey(user.id), []);
    setProfile(nextProfile);
    setDiaries(nextDiaries);
    setCurrentDiary(nextDiaries[0] ?? null);
    setView(hasProfile(nextProfile) ? 'create' : 'settings');
  }, [user?.id]);

  if (!user) return <AuthScreen onAuth={setUser} />;

  const updateProfile = (patch: Partial<Profile>) => {
    setProfile((current) => ({ ...current, ...patch }));
  };

  const saveProfile = async () => {
    localStorage.setItem(profileKey(user.id), JSON.stringify(profile));
    setView('create');
    await saveUserProfile({
      userId: user.id,
      userName: profile.userName || user.name,
      dogName: profile.dogName,
      dogAge: Number.parseInt(profile.dogAge, 10) || undefined,
      dogBreed: profile.dogBreed,
      dogAppearance: profile.dogAppearance,
      dogFavoriteThings: profile.dogFavoriteThings,
      dogDislikedThings: profile.dogDislikedThings,
      dogTraits: profile.dogPersonality,
      ownerName: profile.userName || user.name,
      ownerRole: profile.ownerRole,
      ownerNickname: profile.ownerNickname,
      ownerGender: profile.userGender,
      relationshipNote: profile.relationshipNote,
      dogPhotoUrl: profile.dogPhotoUrl,
      ownerPhotoUrl: profile.ownerPhotoUrl,
    }).catch(() => undefined);
  };

  const onImage = async (kind: 'owner' | 'dog' | 'walk', file?: File) => {
    if (!file) return;
    const preview = await readFile(file);
    const uploaded = await uploadImage(file, kind).catch(() => null);

    if (kind === 'owner') {
      updateProfile({ ownerPhotoName: file.name, ownerPhotoPreview: preview, ownerPhotoUrl: uploaded?.url ?? null });
    }
    if (kind === 'dog') {
      updateProfile({ dogPhotoName: file.name, dogPhotoPreview: preview, dogPhotoUrl: uploaded?.url ?? null });
    }
    if (kind === 'walk') {
      setDraft((current) => ({
        ...current,
        walkPhotoName: file.name,
        walkPhotoPreview: preview,
        walkPhotoUrl: uploaded?.url ?? null,
        walkRecordId: null,
      }));
    }
  };

  const generate = async (event: FormEvent) => {
    event.preventDefault();
    if (!hasProfile(profile)) {
      setView('settings');
      return;
    }
    if (!draft.walkPhotoPreview) return;
    if (!profile.dogPhotoUrl) {
      alert('초기 설정의 강아지 사진이 서버에 업로드되어야 그림을 만들 수 있어요. 설정에서 강아지 사진을 다시 등록해 주세요.');
      setView('settings');
      return;
    }
    if (!draft.walkPhotoUrl) {
      alert('오늘 산책 사진이 서버에 업로드되지 않았어요. 사진을 다시 선택해 주세요.');
      return;
    }

    setLoading(true);
    try {
      setStep('사진 키워드와 산책 기록을 서버에 저장하는 중');
      const walkRecord = await createWalkRecord({
        dogId: dogIdFor(user.id),
        originalImageUrl: draft.walkPhotoUrl,
        address: draft.place,
        weather: draft.weather,
      });

      const generationDraft = {
        ...draft,
        walkPhotoUrl: walkRecord.originalImageUrl,
        walkRecordId: walkRecord.id,
      };

      setStep('강아지 사진과 산책 사진을 함께 보고 그림일기를 생성하는 중');
      const serverDiary = await requestDiaryGeneration({
        userId: user.id,
        dogId: dogIdFor(user.id),
        walkRecordId: generationDraft.walkRecordId,
        dogName: profile.dogName,
        dogAge: Number.parseInt(profile.dogAge, 10) || undefined,
        dogBreed: profile.dogBreed,
        dogPhotoUrl: profile.dogPhotoUrl,
        dogGender: profile.dogAppearance,
        dogPersonality: profile.dogPersonality,
        favoriteThings: profile.dogFavoriteThings,
        dislikedThings: profile.dogDislikedThings,
        ownerName: ownerName(profile, user),
        ownerRole: profile.ownerRole,
        relationshipNote: `${profile.relationshipNote}\n보호자 특징: ${profile.userGender}`,
        walkLocation: generationDraft.place,
        walkWeather: generationDraft.weather,
        walkActivity: [generationDraft.activity, generationDraft.story].filter(Boolean).join(' / '),
        imagePromptNotes: buildImagePromptNotes(profile, generationDraft),
        diaryPromptNotes: buildDiaryPromptNotes(profile, generationDraft, user),
      });

      if (!serverDiary.generatedImageUrl) {
        throw new Error('서버가 생성 이미지 URL을 반환하지 않았습니다.');
      }

      const diary = fromServerDiary(serverDiary, generationDraft, '');
      const nextDiaries = [diary, ...diaries];
      setDraft((current) => ({
        ...current,
        walkPhotoUrl: generationDraft.walkPhotoUrl,
        walkRecordId: generationDraft.walkRecordId,
      }));
      setCurrentDiary(diary);
      setDiaries(nextDiaries);
      localStorage.setItem(diariesKey(user.id), JSON.stringify(nextDiaries));
      setView('result');
    } catch (error) {
      const message = error instanceof Error ? error.message : '그림일기 생성에 실패했습니다.';
      alert(`그림일기 생성 실패: ${message}`);
    } finally {
      setLoading(false);
    }
  };

  const publish = (diary: Diary) => {
    const post: Post = {
      id: crypto.randomUUID(),
      authorId: user.id,
      authorName: profile.userName || user.name,
      dogName: profile.dogName || '우리 강아지',
      image: diary.drawing,
      title: diary.title,
      content: diary.content,
      place: diary.place,
      createdAt: new Date().toISOString(),
      likes: [],
      comments: [],
    };
    const nextPosts = [post, ...posts];
    setPosts(nextPosts);
    localStorage.setItem(POSTS_KEY, JSON.stringify(nextPosts));
    setView('community');
  };

  const logout = () => {
    localStorage.removeItem(SESSION_KEY);
    localStorage.removeItem('paws-session');
    setUser(null);
  };

  return (
    <main className="app-shell">
      <header className="top-bar">
        <button className="brand-button" type="button" onClick={() => setView('create')}>
          <span><PawPrint size={20} /></span>
          <strong>Paws-on-Keyboard</strong>
        </button>
        <button className="ghost-button" type="button" onClick={logout}><LogOut size={17} />로그아웃</button>
      </header>

      <section className="app-content">
        {view === 'create' && (
          <CreateView
            profile={profile}
            draft={draft}
            loading={loading}
            step={step}
            onDraft={(patch) => setDraft((current) => ({ ...current, ...patch }))}
            onWalkPhoto={() => walkPhotoRef.current?.click()}
            onSubmit={generate}
            onSettings={() => setView('settings')}
          />
        )}
        {view === 'settings' && (
          <SettingsView
            profile={profile}
            onChange={updateProfile}
            onOwnerPhoto={() => ownerPhotoRef.current?.click()}
            onDogPhoto={() => dogPhotoRef.current?.click()}
            onSave={saveProfile}
          />
        )}
        {view === 'result' && currentDiary && (
          <ResultView diary={currentDiary} onPublish={publish} onCreate={() => setView('create')} onMemories={() => setView('memories')} />
        )}
        {view === 'memories' && (
          <MemoriesView diaries={diaries} onSelect={(diary) => { setCurrentDiary(diary); setView('result'); }} />
        )}
        {view === 'community' && <CommunityView posts={posts} user={user} onPosts={setPosts} />}
        {view === 'profile' && <ProfileView profile={profile} diaries={diaries} posts={posts.filter((post) => post.authorId === user.id)} onSettings={() => setView('settings')} />}
      </section>

      <BottomNav view={view} onView={setView} />
      <input ref={ownerPhotoRef} className="hidden-file" type="file" accept="image/*" onChange={(event) => onImage('owner', event.target.files?.[0])} />
      <input ref={dogPhotoRef} className="hidden-file" type="file" accept="image/*" onChange={(event) => onImage('dog', event.target.files?.[0])} />
      <input ref={walkPhotoRef} className="hidden-file" type="file" accept="image/*" onChange={(event) => onImage('walk', event.target.files?.[0])} />
    </main>
  );
}

function AuthScreen({ onAuth }: { onAuth: (user: User) => void }) {
  const [mode, setMode] = useState<'login' | 'signup'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [error, setError] = useState('');

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError('');
    try {
      const response = mode === 'signup'
        ? await requestSignup({ email, password, name: name || email.split('@')[0] || '사용자' })
        : await requestLogin({ email, password });
      localStorage.setItem(SESSION_KEY, JSON.stringify(response));
      localStorage.setItem('paws-session', JSON.stringify(response));
      onAuth(response);
    } catch (err) {
      const users = readJson<User[]>(USERS_KEY, []);
      if (mode === 'signup') {
        const localUser: User = { id: crypto.randomUUID(), email, password, name: name || email.split('@')[0] || '사용자' };
        localStorage.setItem(USERS_KEY, JSON.stringify([localUser, ...users]));
        localStorage.setItem(SESSION_KEY, JSON.stringify(localUser));
        localStorage.setItem('paws-session', JSON.stringify(localUser));
        onAuth(localUser);
        return;
      }
      const found = users.find((item) => item.email === email && item.password === password);
      if (!found) {
        setError(err instanceof Error ? err.message : '이메일과 비밀번호를 확인해 주세요.');
        return;
      }
      localStorage.setItem(SESSION_KEY, JSON.stringify(found));
      localStorage.setItem('paws-session', JSON.stringify(found));
      onAuth(found);
    }
  };

  return (
    <main className="auth-screen">
      <section className="auth-card">
        <div className="auth-brand">
          <span><PawPrint size={28} /></span>
          <div><strong>Paws-on-Keyboard</strong><p>강아지가 직접 기억하는 그림일기</p></div>
        </div>
        <div className="auth-tabs">
          <button className={mode === 'login' ? 'active' : ''} type="button" onClick={() => setMode('login')}>로그인</button>
          <button className={mode === 'signup' ? 'active' : ''} type="button" onClick={() => setMode('signup')}>회원가입</button>
        </div>
        <form className="auth-form" onSubmit={submit}>
          {mode === 'signup' && <TextField label="이름" value={name} onChange={setName} placeholder="보호자 이름" />}
          <TextField label="이메일" value={email} onChange={setEmail} placeholder="you@example.com" />
          <TextField label="비밀번호" value={password} onChange={setPassword} type="password" placeholder="비밀번호" />
          {error && <p className="app-error compact">{error}</p>}
          <button className="primary-button full" type="submit">{mode === 'signup' ? '가입하고 시작' : '로그인'}</button>
        </form>
      </section>
    </main>
  );
}

function CreateView({ profile, draft, loading, step, onDraft, onWalkPhoto, onSubmit, onSettings }: {
  profile: Profile;
  draft: Draft;
  loading: boolean;
  step: string;
  onDraft: (patch: Partial<Draft>) => void;
  onWalkPhoto: () => void;
  onSubmit: (event: FormEvent) => void;
  onSettings: () => void;
}) {
  return (
    <section className="screen-stack">
      <div className="home-hero">
        <div>
          <p>{profile.dogName ? `${profile.dogName}의 오늘` : '먼저 초기 설정이 필요해요'}</p>
          <h1>오늘 찍은 사진을 강아지 시점 그림일기로 바꿔요</h1>
        </div>
        {profile.dogPhotoPreview ? <img src={profile.dogPhotoPreview} alt="강아지 프로필" /> : <Dog size={42} />}
      </div>
      <form className="capture-card" onSubmit={onSubmit}>
        <PhotoButton label="오늘 찍은 사진" name={draft.walkPhotoName} preview={draft.walkPhotoPreview} onClick={onWalkPhoto} icon={<Camera size={28} />} large />
        <div className="field-grid">
          <TextField label="장소" value={draft.place} onChange={(value) => onDraft({ place: value })} placeholder="예: 한강공원, 동네 산책로" icon={<MapPinned size={16} />} />
          <TextField label="날씨" value={draft.weather} onChange={(value) => onDraft({ weather: value })} placeholder="예: 맑음, 흐림, 비 온 뒤" icon={<CloudSun size={16} />} />
        </div>
        <TextField label="무엇을 했나요?" value={draft.activity} onChange={(value) => onDraft({ activity: value })} placeholder="예: 공놀이, 카페 앞에서 쉬기" />
        <TextArea label="기억하고 싶은 상황" value={draft.story} onChange={(value) => onDraft({ story: value })} placeholder="비워두면 사진과 강아지 설정만으로 작성합니다." />
        <div className="button-row">
          <button className="secondary-button" type="button" onClick={onSettings}>설정 수정</button>
          <button className="primary-button" type="submit" disabled={loading || !draft.walkPhotoPreview}>{loading ? <Loader2 className="spin" size={18} /> : <Play size={18} />}그림일기 만들기</button>
        </div>
        {loading && <GenerationProgress step={step} draft={draft} profile={profile} />}
      </form>
      <InfoPanel />
    </section>
  );
}

function SettingsView({ profile, onChange, onOwnerPhoto, onDogPhoto, onSave }: {
  profile: Profile;
  onChange: (patch: Partial<Profile>) => void;
  onOwnerPhoto: () => void;
  onDogPhoto: () => void;
  onSave: () => void;
}) {
  return (
    <section className="screen-stack">
      <ScreenTitle eyebrow="초기 설정" title="강아지의 고정 성격과 보호자 관계를 알려주세요" />
      <SetupChecklist profile={profile} />
      <div className="profile-photo-row">
        <PhotoButton label="보호자 사진" name={profile.ownerPhotoName} preview={profile.ownerPhotoPreview} onClick={onOwnerPhoto} icon={<UserRound size={24} />} />
        <PhotoButton label="강아지 사진" name={profile.dogPhotoName} preview={profile.dogPhotoPreview} onClick={onDogPhoto} icon={<Dog size={24} />} />
      </div>
      <FormSection title="보호자 정보">
        <div className="field-grid">
          <TextField label="이름" value={profile.userName} onChange={(value) => onChange({ userName: value })} />
          <TextField label="성격/특징" value={profile.userGender} onChange={(value) => onChange({ userGender: value })} placeholder="예: 차분함, 짧은 머리, 운동화" />
          <SelectField label="강아지가 부르는 호칭" value={profile.ownerRole} onChange={(value) => onChange({ ownerRole: value as OwnerRole })} options={['엄마', '아빠', '누나', '언니', '형', '오빠', '친구']} />
          <TextField label="보호자 애칭" value={profile.ownerNickname} onChange={(value) => onChange({ ownerNickname: value })} placeholder="예: 콩이 엄마" />
        </div>
        <TextArea label="강아지와의 관계" value={profile.relationshipNote} onChange={(value) => onChange({ relationshipNote: value })} placeholder="예: 산책할 때 자주 눈을 맞추고 간식을 줘요." />
      </FormSection>
      <FormSection title="강아지 고정 정보">
        <div className="field-grid">
          <TextField label="이름" value={profile.dogName} onChange={(value) => onChange({ dogName: value })} />
          <TextField label="나이" value={profile.dogAge} onChange={(value) => onChange({ dogAge: value })} placeholder="예: 4" />
          <TextField label="견종" value={profile.dogBreed} onChange={(value) => onChange({ dogBreed: value })} placeholder="예: 보더콜리, 믹스견" />
          <TextField label="외모" value={profile.dogAppearance} onChange={(value) => onChange({ dogAppearance: value })} placeholder="예: 검은 귀, 흰 가슴, 짧은 꼬리" />
        </div>
        <TextArea label="성격" value={profile.dogPersonality} onChange={(value) => onChange({ dogPersonality: value })} placeholder="예: 겁이 조금 많지만 보호자를 잘 따라요." />
        <div className="field-grid">
          <TextArea label="좋아하는 것" value={profile.dogFavoriteThings} onChange={(value) => onChange({ dogFavoriteThings: value })} placeholder="예: 공놀이, 고구마, 바람 냄새" />
          <TextArea label="싫어하는 것" value={profile.dogDislikedThings} onChange={(value) => onChange({ dogDislikedThings: value })} placeholder="예: 큰 소리, 비 오는 날" />
        </div>
      </FormSection>
      <button className="primary-button full" type="button" onClick={onSave}>설정 저장</button>
    </section>
  );
}

function ResultView({ diary, onPublish, onCreate, onMemories }: { diary: Diary; onPublish: (diary: Diary) => void; onCreate: () => void; onMemories: () => void }) {
  const [drawingSrc, setDrawingSrc] = useState(assetUrl(diary.drawing));

  useEffect(() => {
    setDrawingSrc(assetUrl(diary.drawing));
  }, [diary.drawing]);

  const recoverDrawing = () => {
    const fallback = assetUrl(diary.fallbackDrawing || diary.originalPhoto || '');
    if (fallback && fallback !== drawingSrc) {
      setDrawingSrc(fallback);
    } else {
      setDrawingSrc('');
    }
  };

  return (
    <section className="screen-stack">
      <div className="button-row">
        <button className="secondary-button" type="button" onClick={onCreate}>다른 사진 만들기</button>
        <button className="secondary-button" type="button" onClick={onMemories}>우리집 추억</button>
        <button className="primary-button" type="button" onClick={() => onPublish(diary)}>자랑하기</button>
      </div>
      <article className="diary-sheet">
        <header className="diary-header">
          <div><span>{formatDate(diary.createdAt)}</span><h2>{diary.title}</h2></div>
          <PawPrint size={24} />
        </header>
        <div className="drawing-box">
          {drawingSrc
            ? <img className="drawing-image" src={drawingSrc} alt="생성된 그림일기" onError={recoverDrawing} />
            : <div className="empty-drawing"><ImagePlus size={42} /><span>그림을 다시 불러오지 못했어요.</span></div>}
        </div>
        <div className="diary-text-box"><HandwritingText text={diary.content} /></div>
      </article>
      <section className="recommend-section">
        <ScreenTitle eyebrow="다음 산책 추천" title="사진과 장소를 바탕으로 이어갈 곳" />
        {diary.recommendations.map((place) => (
          <article className="recommend-card" key={`${place.name}-${place.category}`}>
            <strong>{place.name}</strong>
            <span>{place.category}</span>
            <p>{place.reason}</p>
          </article>
        ))}
      </section>
    </section>
  );
}

function MemoriesView({ diaries, onSelect }: { diaries: Diary[]; onSelect: (diary: Diary) => void }) {
  const [query, setQuery] = useState('');
  const filtered = diaries.filter((diary) => `${diary.title} ${diary.content} ${diary.place}`.toLowerCase().includes(query.toLowerCase()));
  return (
    <section className="screen-stack">
      <ScreenTitle eyebrow="우리집 추억" title="강아지가 기억한 산책 일기" />
      <label className="search-box"><Search size={18} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="장소, 내용, 제목 검색" /></label>
      <div className="memory-grid">
        {filtered.map((diary) => (
          <button className="memory-card" type="button" onClick={() => onSelect(diary)} key={diary.id}>
            <img src={diary.drawing} alt={diary.title} />
            <strong>{diary.title}</strong>
            <span>{diary.place} · {formatDate(diary.createdAt)}</span>
          </button>
        ))}
      </div>
      {!filtered.length && <EmptyState title="아직 저장된 추억이 없어요" body="오늘 찍은 사진으로 첫 그림일기를 만들어 보세요." />}
    </section>
  );
}

function CommunityView({ posts, user, onPosts }: { posts: Post[]; user: User; onPosts: (posts: Post[]) => void }) {
  const [query, setQuery] = useState('');
  const filtered = posts.filter((post) => `${post.title} ${post.content} ${post.dogName} ${post.place}`.toLowerCase().includes(query.toLowerCase()));

  const toggleLike = (postId: string) => {
    const next = posts.map((post) => {
      if (post.id !== postId) return post;
      const liked = post.likes.includes(user.id);
      return { ...post, likes: liked ? post.likes.filter((id) => id !== user.id) : [...post.likes, user.id] };
    });
    onPosts(next);
    localStorage.setItem(POSTS_KEY, JSON.stringify(next));
  };

  const addComment = (postId: string, content: string) => {
    if (!content.trim()) return;
    const next = posts.map((post) => post.id === postId
      ? { ...post, comments: [...post.comments, { id: crypto.randomUUID(), authorName: user.name, content }] }
      : post);
    onPosts(next);
    localStorage.setItem(POSTS_KEY, JSON.stringify(next));
  };

  return (
    <section className="screen-stack">
      <ScreenTitle eyebrow="우리 강아지 자랑하기" title="다른 보호자의 그림일기도 둘러봐요" />
      <label className="search-box"><Search size={18} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="강아지, 장소, 내용 검색" /></label>
      <div className="post-list">
        {filtered.map((post) => <PostCard post={post} user={user} onLike={toggleLike} onComment={addComment} key={post.id} />)}
      </div>
      {!filtered.length && <EmptyState title="아직 게시물이 없어요" body="그림일기를 만든 뒤 자랑하기를 눌러 첫 게시물을 올려보세요." />}
    </section>
  );
}

function PostCard({ post, user, onLike, onComment }: { post: Post; user: User; onLike: (postId: string) => void; onComment: (postId: string, content: string) => void }) {
  const [comment, setComment] = useState('');
  const liked = post.likes.includes(user.id);
  return (
    <article className="post-card">
      <div className="post-head"><strong>{post.dogName}</strong><span>{post.authorName} · {formatDate(post.createdAt)}</span></div>
      {post.image && <img className="post-image" src={post.image} alt={post.title} />}
      <h2>{post.title}</h2>
      <p>{post.content}</p>
      <span className="place-chip"><MapPinned size={14} />{post.place || '사진 속 산책지'}</span>
      <div className="post-actions">
        <button className={liked ? 'active' : ''} type="button" onClick={() => onLike(post.id)}><Heart size={17} />{post.likes.length}</button>
        <span><MessageCircle size={17} />{post.comments.length}</span>
      </div>
      <div className="comment-list">{post.comments.map((item) => <p key={item.id}><strong>{item.authorName}</strong> {item.content}</p>)}</div>
      <form className="comment-form" onSubmit={(event) => { event.preventDefault(); onComment(post.id, comment); setComment(''); }}>
        <input value={comment} onChange={(event) => setComment(event.target.value)} placeholder="댓글 쓰기" />
        <button type="submit">등록</button>
      </form>
    </article>
  );
}

function ProfileView({ profile, diaries, posts, onSettings }: { profile: Profile; diaries: Diary[]; posts: Post[]; onSettings: () => void }) {
  return (
    <section className="screen-stack">
      <div className="profile-panel">
        <div className="profile-avatars">
          {profile.ownerPhotoPreview && <img src={profile.ownerPhotoPreview} alt="보호자" />}
          {profile.dogPhotoPreview && <img src={profile.dogPhotoPreview} alt="강아지" />}
        </div>
        <h1>{profile.dogName || '우리 강아지'}</h1>
        <p>{[profile.dogBreed, profile.dogAge && `${profile.dogAge}살`].filter(Boolean).join(' · ')}</p>
        <div className="profile-stats">
          <span><strong>{diaries.length}</strong>추억</span>
          <span><strong>{posts.length}</strong>게시물</span>
          <span><strong>0</strong>팔로워</span>
        </div>
        <button className="secondary-button full" type="button" onClick={onSettings}>프로필 설정 수정</button>
      </div>
      <section className="badge-notice">
        <Trophy size={20} />
        <div><strong>인기 게시물 배지 예정</strong><p>주간/월간 좋아요 상위 게시물에 강아지와 보호자 사진 기반 배지를 지급하는 구조입니다.</p></div>
      </section>
    </section>
  );
}

function BottomNav({ view, onView }: { view: View; onView: (view: View) => void }) {
  const items = [
    { view: 'create' as const, label: '오늘', icon: <Home size={20} /> },
    { view: 'memories' as const, label: '추억', icon: <BookOpen size={20} /> },
    { view: 'community' as const, label: '자랑', icon: <Users size={20} /> },
    { view: 'profile' as const, label: '내 정보', icon: <UserRound size={20} /> },
    { view: 'settings' as const, label: '설정', icon: <Settings size={20} /> },
  ];
  return <nav className="bottom-nav" aria-label="주요 메뉴">{items.map((item) => <button className={view === item.view ? 'active' : ''} type="button" key={item.view} onClick={() => onView(item.view)}>{item.icon}<span>{item.label}</span></button>)}</nav>;
}

function SetupChecklist({ profile }: { profile: Profile }) {
  const items = [
    ['강아지 사진', Boolean(profile.dogPhotoPreview)],
    ['보호자 사진', Boolean(profile.ownerPhotoPreview)],
    ['강아지 이름/나이', Boolean(profile.dogName && profile.dogAge)],
    ['견종/외모', Boolean(profile.dogBreed && profile.dogAppearance)],
    ['성격/취향', Boolean(profile.dogPersonality && profile.dogFavoriteThings)],
    ['보호자 호칭', Boolean(profile.ownerRole)],
  ];
  const done = items.filter(([, ok]) => ok).length;
  return (
    <section className="setup-card">
      <div><span className="setup-eyebrow">생성 정확도</span><strong>{done}/{items.length}개 준비됨</strong><p>고정 정보가 많을수록 사진 속 상황과 강아지 성격이 더 잘 맞습니다.</p></div>
      <div className="setup-meter"><span style={{ width: `${Math.round((done / items.length) * 100)}%` }} /></div>
      <div className="setup-chip-row">{items.map(([label, ok]) => <span className={ok ? 'done' : ''} key={String(label)}>{ok ? '완료' : '필요'} · {label}</span>)}</div>
    </section>
  );
}

function GenerationProgress({ step, draft, profile }: { step: string; draft: Draft; profile: Profile }) {
  const lines = [
    '사진에서 장소, 색감, 자세, 리드줄 같은 키워드를 뽑는 중',
    `${profile.dogName || '강아지'}의 성격과 나이에 맞춰 이야기를 다시 쓰는 중`,
    '강아지가 본 시점으로 귀엽고 삐뚤빼뚤한 그림을 그리는 중',
  ];
  return (
    <section className="diary-sheet live-diary-preview" aria-live="polite">
      <div className="drawing-box generating">
        {draft.walkPhotoPreview ? <img className="sketch-image" src={draft.walkPhotoPreview} alt="분석 중인 산책 사진" /> : <ImagePlus size={42} />}
        <div className="sketch-paper" /><div className="sketch-pencil-lines" /><div className="sketch-color-wash" />
      </div>
      <div className="diary-text-box live-text-box">{lines.map((line, index) => <p key={line}><span className="stream-handwriting" style={{ animationDelay: `${index * 0.6}s` }}>{line}</span></p>)}</div>
      <div className="live-step"><Loader2 className="spin" size={16} /><span>{step}</span></div>
    </section>
  );
}

function ScreenTitle({ eyebrow, title }: { eyebrow: string; title: string }) {
  return <div className="section-title"><Sparkles size={18} /><div><span>{eyebrow}</span><h2>{title}</h2></div></div>;
}

function FormSection({ title, children }: { title: string; children: ReactNode }) {
  return <section className="form-section"><h2>{title}</h2>{children}</section>;
}

function TextField({ label, value, onChange, placeholder, icon, type = 'text' }: { label: string; value: string; onChange: (value: string) => void; placeholder?: string; icon?: ReactNode; type?: string }) {
  return <label className="field-label"><span>{icon}{label}</span><input type={type} value={value} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} /></label>;
}

function TextArea({ label, value, onChange, placeholder }: { label: string; value: string; onChange: (value: string) => void; placeholder?: string }) {
  return <label className="field-label"><span>{label}</span><textarea value={value} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} /></label>;
}

function SelectField({ label, value, onChange, options }: { label: string; value: string; onChange: (value: string) => void; options: string[] }) {
  return <label className="field-label"><span>{label}</span><select value={value} onChange={(event) => onChange(event.target.value)}>{options.map((item) => <option value={item} key={item}>{item}</option>)}</select></label>;
}

function PhotoButton({ label, name, preview, onClick, icon, large = false }: { label: string; name: string | null; preview: string | null; onClick: () => void; icon: ReactNode; large?: boolean }) {
  return <button className={large ? 'photo-button large' : 'photo-button'} type="button" onClick={onClick}>{preview ? <img src={preview} alt={`${label} 미리보기`} /> : <span>{icon}</span>}<strong>{label}</strong><em>{name ?? '사진 추가'}</em></button>;
}

function InfoPanel() {
  return (
    <section className="info-panel">
      <div><Sparkles size={18} /><strong>사진 기반 생성</strong><p>사진 키워드를 먼저 추출하고, 강아지 성격과 나이에 맞게 이야기로 재구성한 뒤 강아지 시점 그림일기를 만듭니다.</p></div>
      <div><MapPinned size={18} /><strong>추천 데이터 확장</strong><p>TourAPI, 반려동물 동반 여행, 날씨, 지역 API를 붙여 추천을 고도화할 수 있는 구조입니다.</p></div>
    </section>
  );
}

function EmptyState({ title, body }: { title: string; body: string }) {
  return <div className="empty-state"><PawPrint size={28} /><strong>{title}</strong><p>{body}</p></div>;
}

function HandwritingText({ text }: { text: string }) {
  const sentences = text
    .split(/(?<=[.!?。！？]|[.?!]|다\.|어\.|지\.|요\.|까\?)/)
    .map((line) => line.trim())
    .filter(Boolean);
  const lines = sentences.length ? sentences : [text];
  return (
    <>
      {lines.map((line, lineIndex) => (
        <p className="diary-handwriting-line" key={`${line}-${lineIndex}`}>
          {Array.from(line).map((char, charIndex) => (
            <span
              className="diary-handwriting-char"
              style={{
                transform: `rotate(${((charIndex + lineIndex) % 5) - 2}deg) translateY(${((charIndex * 2 + lineIndex) % 3) - 1}px)`,
              }}
              key={`${char}-${charIndex}`}
            >
              {char === ' ' ? '\u00a0' : char}
            </span>
          ))}
        </p>
      ))}
    </>
  );
}

function buildLocalDiary(profile: Profile, draft: Draft, drawing: string, user: User): Diary {
  const dog = profile.dogName || '우리 강아지';
  const place = draft.place || '사진 속 산책지';
  const weather = draft.weather || '기분 좋은 날씨';
  const activity = draft.activity || '보호자와 천천히 산책했다';
  const detail = draft.story || '사진 속 냄새와 발바닥 느낌이 아직 남아 있었다';
  const owner = ownerName(profile, user);
  return {
    id: crypto.randomUUID(),
    title: `${dog}의 ${place} 그림일기`,
    content: `오늘 나는 ${owner}와 ${place}에 갔다. ${weather}라서 코끝에 닿는 냄새가 선명했다. ${activity}. 사진 속 순간을 다시 떠올리니 내 발바닥이 먼저 기억했다. ${detail}. 그래서 나는 오늘을 그림으로 남겼다. 내가 본 세상은 이렇게 폭신하고 반짝였다.`,
    drawing,
    fallbackDrawing: drawing,
    place,
    weather,
    createdAt: new Date().toISOString(),
    originalPhoto: draft.walkPhotoPreview,
    recommendations: [
      { name: `${place} 근처 반려견 산책 코스`, category: 'MVP 추천', reason: '현재는 입력 장소 기반 추천이며, 공공데이터 연동 후 실제 장소로 대체할 수 있습니다.' },
      { name: '반려견 동반 가능 휴식 장소', category: '확장 예정', reason: '산책 후 쉬어가기 좋은 장소를 추천하는 흐름입니다.' },
    ],
  };
}

function fromServerDiary(response: GenerateDiaryResponse, draft: Draft, fallbackDrawing: string): Diary {
  const drawing = normalizeGeneratedImageUrl(response.generatedImageUrl) || fallbackDrawing;
  return {
    id: response.diaryId || crypto.randomUUID(),
    title: response.title,
    content: response.content,
    drawing,
    fallbackDrawing,
    place: draft.place || '사진 속 산책지',
    weather: draft.weather || '날씨 미입력',
    createdAt: response.createdAt || new Date().toISOString(),
    originalPhoto: draft.walkPhotoPreview,
    recommendations: response.recommendedPlaces.map((place) => ({
      name: place.name,
      category: place.category || '추천 장소',
      reason: place.reason,
    })),
  };
}

function normalizeGeneratedImageUrl(url: string | null | undefined) {
  if (!url) return '';
  const clean = url.trim();
  if (!clean) return '';
  if (clean.startsWith('data:') || clean.startsWith('http://') || clean.startsWith('https://')) return clean;
  if (clean.startsWith('/uploads/')) return clean;
  if (clean.startsWith('uploads/')) return `/${clean}`;
  return clean;
}

function assetUrl(url: string | null | undefined) {
  const clean = normalizeGeneratedImageUrl(url);
  if (!clean) return '';
  if (clean.startsWith('/uploads/')) return `http://localhost:8080${clean}`;
  return clean;
}

function buildImagePromptNotes(profile: Profile, draft: Draft) {
  return [
    '업로드 사진을 단순 필터로 바꾸지 말고, 사진 속 상황 단서와 강아지 고정 정보를 합쳐 새 그림일기로 그린다.',
    '강아지 외모, 견종, 무늬, 보호자와의 관계는 초기 설정을 우선한다.',
    '사진에 없는 계절, 꽃, 벤치, 유명 장소, 다른 사람이나 동물을 지어내지 않는다.',
    '그림 안에 글자, 말풍선, 날짜 텍스트를 넣지 않는다.',
    `강아지 외모: ${profile.dogAppearance}`,
    `견종: ${profile.dogBreed}`,
    `성격: ${profile.dogPersonality}`,
    `장소: ${draft.place || '사진 기반'}`,
    `활동: ${draft.activity || '사진 기반'}`,
    `상황: ${draft.story || '사진 기반'}`,
  ].join('\n');
}

function buildDiaryPromptNotes(profile: Profile, draft: Draft, user: User) {
  return [
    `${profile.dogName || '강아지'}가 직접 기억해서 쓰는 1인칭 일기.`,
    `보호자는 "${ownerName(profile, user)}"라고 부른다.`,
    '사람이 해설하는 말투가 아니라 강아지가 냄새, 발바닥, 꼬리, 보호자 반응을 기억하는 말투.',
    `오늘 장소: ${draft.place || '사진 속 장소'}`,
    `오늘 활동: ${draft.activity || '사진 기반'}`,
    `상황 설명: ${draft.story || '사진 기반'}`,
  ].join('\n');
}

function createMemoryDrawing(profile: Profile, draft: Draft, palette: Palette) {
  const text = `${profile.dogAppearance} ${profile.dogBreed}`.toLowerCase();
  const blackWhite = /검|black|white|흰|하얀/.test(text) || (palette.darkRatio > 0.14 && palette.lightRatio > 0.2);
  const brown = /갈색|브라운|brown|tan|gold/.test(text) || palette.brownRatio > 0.18;
  const light = /크림|흰|하얀|white|cream/.test(text) || palette.lightRatio > 0.25;
  const colors = blackWhite
    ? { body: '#fbf7ec', patch: '#272b31', ear: '#252a30', shadow: '#e9e1d2', collar: '#77b5e8', accent: palette.average }
    : brown
      ? { body: '#dca76d', patch: '#8d5b3a', ear: '#8d5b3a', shadow: '#bd814f', collar: '#73b7dd', accent: palette.average }
      : light
        ? { body: '#fbf7ec', patch: '#e1d7c4', ear: '#e7ddca', shadow: '#e9e1d2', collar: '#77b5e8', accent: palette.average }
        : { body: '#f0d49a', patch: '#c39762', ear: '#b98655', shadow: '#ddc08a', collar: '#73b7dd', accent: palette.average };
  const sceneText = `${draft.place} ${draft.story}`.toLowerCase();
  const scene = /바다|해변|강|물|sea|beach|river/.test(sceneText) ? 'water' : /카페|집|실내|home|cafe|indoor/.test(sceneText) ? 'indoor' : 'park';
  const background = scene === 'water'
    ? '<rect y="270" width="960" height="120" fill="#7fc8e3"/><path d="M70 318 C180 294 270 334 370 314 M430 318 C555 292 650 330 770 310 M805 320 C860 302 910 318 940 304" stroke="#fff" stroke-width="8" stroke-linecap="round" fill="none" opacity=".82"/>'
    : scene === 'indoor'
      ? '<rect x="74" y="92" width="270" height="176" rx="18" fill="#d7f0f2" stroke="#91bcc0" stroke-width="6"/><ellipse cx="760" cy="190" rx="120" ry="48" fill="#f5d7a5" opacity=".85"/>'
      : '<circle cx="126" cy="105" r="54" fill="#ffd66e"/><path d="M100 372 L106 210 M240 372 L252 230 M810 372 L820 210" stroke="#7b765f" stroke-width="11" stroke-linecap="round"/><circle cx="96" cy="220" r="58" fill="#8fc981" opacity=".62"/><circle cx="250" cy="220" r="66" fill="#7fbd70" opacity=".58"/><circle cx="820" cy="218" r="64" fill="#8fc981" opacity=".58"/>';
  const sky = scene === 'indoor' ? '#fff1dc' : '#cceefe';
  const ground = scene === 'water' ? '#f4cf8a' : scene === 'indoor' ? '#d9ad72' : '#91cc76';
  const patches = blackWhite
    ? `<path d="M350 292 C392 208 484 200 506 324 C460 362 394 358 350 292 Z" fill="${colors.patch}"/><path d="M504 318 C548 224 636 232 660 304 C610 360 552 360 504 318 Z" fill="${colors.patch}"/>`
    : `<path d="M370 308 C410 238 482 238 500 326 C452 354 404 350 370 308 Z" fill="${colors.patch}" opacity=".62"/><path d="M522 326 C552 250 620 254 642 316 C600 356 558 354 522 326 Z" fill="${colors.patch}" opacity=".58"/>`;
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="960" height="680" viewBox="0 0 960 680"><defs><filter id="paper"><feTurbulence type="fractalNoise" baseFrequency=".75" numOctaves="2" result="n"/><feBlend in="SourceGraphic" in2="n" mode="multiply" opacity=".07"/></filter><pattern id="lines" width="18" height="18" patternUnits="userSpaceOnUse"><path d="M0 16 Q8 14 18 16" fill="none" stroke="#eadfc7" opacity=".45"/></pattern></defs><rect width="960" height="680" rx="34" fill="#fffdf2"/><rect width="960" height="680" rx="34" fill="url(#lines)"/><g filter="url(#paper)"><rect width="960" height="380" rx="34" fill="${sky}"/>${background}<rect y="360" width="960" height="320" fill="${ground}"/><path d="M50 610 C210 576 348 640 506 608 C690 570 792 636 930 600" stroke="${colors.accent}" stroke-width="16" stroke-linecap="round" fill="none" opacity=".18"/><ellipse cx="500" cy="590" rx="238" ry="52" fill="rgba(100,78,48,.16)"/><path d="M382 248 C318 260 278 342 322 414 C382 396 420 326 382 248 Z" fill="${colors.ear}" stroke="#554a42" stroke-width="8"/><path d="M620 248 C686 260 722 344 678 414 C620 396 582 326 620 248 Z" fill="${colors.ear}" stroke="#554a42" stroke-width="8"/><path d="M332 508 C338 414 408 362 500 362 C592 362 662 414 668 508 C650 610 580 638 500 638 C420 638 350 610 332 508 Z" fill="${colors.body}" stroke="#554a42" stroke-width="9"/><path d="M372 510 C420 456 472 466 500 532 C460 590 392 578 372 510 Z" fill="${colors.shadow}" stroke="#554a42" stroke-width="5" opacity=".82"/><path d="M500 532 C528 466 582 456 628 510 C608 578 540 590 500 532 Z" fill="${colors.shadow}" stroke="#554a42" stroke-width="5" opacity=".82"/><ellipse cx="500" cy="338" rx="156" ry="136" fill="${colors.body}" stroke="#554a42" stroke-width="9"/>${patches}<ellipse cx="448" cy="344" rx="25" ry="31" fill="#17191f"/><ellipse cx="552" cy="344" rx="25" ry="31" fill="#17191f"/><circle cx="458" cy="331" r="8" fill="#fff"/><circle cx="562" cy="331" r="8" fill="#fff"/><ellipse cx="500" cy="402" rx="62" ry="48" fill="#fffaf0" stroke="#554a42" stroke-width="6"/><ellipse cx="500" cy="384" rx="36" ry="25" fill="#202126"/><path d="M500 398 C486 416 468 424 448 424" stroke="#554a42" stroke-width="6" stroke-linecap="round" fill="none"/><path d="M500 398 C516 416 536 424 558 422" stroke="#554a42" stroke-width="6" stroke-linecap="round" fill="none"/><path d="M488 430 C504 450 526 450 542 430" stroke="#bd6570" stroke-width="5" stroke-linecap="round" fill="none"/><circle cx="384" cy="414" r="27" fill="#f7b9bf" opacity=".75"/><circle cx="616" cy="414" r="27" fill="#f7b9bf" opacity=".75"/><path d="M342 494 C430 528 568 528 658 492" stroke="${colors.collar}" stroke-width="18" stroke-linecap="round" fill="none"/><circle cx="500" cy="525" r="21" fill="#ffd46c" stroke="#ad8a39" stroke-width="5"/></g></svg>`;
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
}

async function analyzePhotoPalette(src: string): Promise<Palette> {
  try {
    const image = await loadImage(src);
    const canvas = document.createElement('canvas');
    const size = 96;
    canvas.width = size;
    canvas.height = size;
    const context = canvas.getContext('2d', { willReadFrequently: true });
    if (!context) throw new Error('canvas unavailable');
    context.drawImage(image, 0, 0, size, size);
    const data = context.getImageData(0, 0, size, size).data;
    let dark = 0;
    let light = 0;
    let brown = 0;
    let rSum = 0;
    let gSum = 0;
    let bSum = 0;
    let count = 0;
    for (let y = Math.floor(size * 0.38); y < Math.floor(size * 0.82); y += 2) {
      for (let x = Math.floor(size * 0.3); x < Math.floor(size * 0.72); x += 2) {
        const i = (y * size + x) * 4;
        const r = data[i];
        const g = data[i + 1];
        const b = data[i + 2];
        const brightness = (r + g + b) / 3;
        if (brightness < 72) dark += 1;
        if (brightness > 202) light += 1;
        if (r > 92 && r > g * 0.95 && g > b * 1.08 && brightness > 70 && brightness < 205) brown += 1;
        rSum += r;
        gSum += g;
        bSum += b;
        count += 1;
      }
    }
    return {
      darkRatio: dark / count,
      lightRatio: light / count,
      brownRatio: brown / count,
      average: rgbToHex(Math.round(rSum / count), Math.round(gSum / count), Math.round(bSum / count)),
    };
  } catch {
    return { darkRatio: 0, lightRatio: 0, brownRatio: 0, average: '#8fbf7a' };
  }
}

function loadImage(src: string) {
  return new Promise<HTMLImageElement>((resolve, reject) => {
    const image = new Image();
    image.onload = () => resolve(image);
    image.onerror = reject;
    image.src = src;
  });
}

function rgbToHex(r: number, g: number, b: number) {
  return `#${[r, g, b].map((value) => value.toString(16).padStart(2, '0')).join('')}`;
}

function ownerName(profile: Profile, user: User) {
  return profile.ownerNickname || profile.ownerRole || profile.userName || user.name || '보호자';
}

function dogIdFor(userId: string) {
  return `dog-${userId}`;
}

function hasProfile(profile: Profile) {
  return Boolean(profile.dogName && profile.dogAppearance && profile.dogPersonality && profile.ownerRole);
}

function profileKey(userId: string) {
  return `paws-profile-v4:${userId}`;
}

function diariesKey(userId: string) {
  return `paws-diaries-v4:${userId}`;
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('ko-KR', { month: 'short', day: 'numeric' }).format(new Date(value));
}

function readJson<T>(key: string, fallback: T): T {
  try {
    const value = localStorage.getItem(key);
    return value ? JSON.parse(value) as T : fallback;
  } catch {
    return fallback;
  }
}

function readFile(file: File) {
  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}
