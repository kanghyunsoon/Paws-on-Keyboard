export type RecommendedPlace = {
  name: string;
  reason: string;
  category?: string;
  address?: string;
  latitude?: number;
  longitude?: number;
  sourceProvider?: string;
  sourceApi?: string;
  sourceContentId?: string;
  petInfo?: string;
  distanceMeters?: number;
};

export type GenerateDiaryResponse = {
  diaryId: string;
  userId?: string | null;
  dogId?: string | null;
  walkRecordId?: string | null;
  originalImageUrl: string | null;
  generatedImageUrl: string | null;
  title: string;
  content: string;
  detectedObjects: string[];
  recommendedPlaces: RecommendedPlace[];
  createdAt?: string | null;
};

export type GenerateDiaryRequest = {
  userId?: string;
  dogId: string;
  walkRecordId: string;
  dogName?: string;
  dogAge?: number;
  dogBreed?: string;
  dogPhotoUrl?: string | null;
  dogGender?: string;
  dogPersonality?: string;
  favoriteThings?: string;
  dislikedThings?: string;
  ownerName?: string;
  ownerRole?: string;
  relationshipNote?: string;
  walkLocation?: string;
  walkWeather?: string;
  walkActivity?: string;
  diaryTone?: string;
  diaryPromptNotes?: string;
  imagePromptNotes?: string;
};

export type UserProfileRequest = {
  userId?: string;
  userName: string;
  dogName: string;
  dogAge?: number;
  dogBreed: string;
  dogAppearance: string;
  dogFavoriteThings: string;
  dogDislikedThings: string;
  dogTraits: string;
  ownerName: string;
  ownerRole: string;
  ownerNickname: string;
  ownerGender: string;
  relationshipNote: string;
  dogPhotoUrl?: string | null;
  ownerPhotoUrl?: string | null;
};

export type UserProfileResponse = {
  id: string;
  userName: string;
  dogId: string;
  dogName: string;
  dogAge: number | null;
  dogBreed: string;
  dogAppearance?: string | null;
  dogFavoriteThings?: string | null;
  dogDislikedThings?: string | null;
  dogTraits: string;
  ownerName: string;
  ownerRole: string;
  ownerNickname?: string | null;
  ownerGender: string;
  relationshipNote: string;
  dogPhotoUrl: string | null;
  ownerPhotoUrl: string | null;
};

export type UploadImageResponse = {
  filename: string;
  url: string;
};

export type WalkRecord = {
  id: string;
  dogId: string;
  originalImageUrl: string;
  latitude: number | null;
  longitude: number | null;
  address: string;
  weather: string;
  temperature: number | null;
  walkedAt: string;
};

export type AuthUserResponse = {
  id: string;
  email: string;
  name: string;
  token?: string;
  tokenExpiresAt?: string;
};

export type CommunityCommentResponse = {
  id: string;
  authorId: string;
  authorName: string;
  content: string;
  createdAt: string;
};

export type CommunityPostResponse = {
  id: string;
  authorId: string;
  authorName: string;
  dogName: string;
  dogPhotoPreview: string | null;
  ownerPhotoPreview: string | null;
  diaryId: string;
  title: string;
  content: string;
  imagePreview: string | null;
  place: string;
  createdAt: string;
  likes: string[];
  comments: CommunityCommentResponse[];
};

export type FollowSummaryResponse = {
  followingIds: string[];
  followerIds: string[];
};

export type BadgeResponse = {
  userId: string;
  postId: string;
  title: string;
  badgeImagePrompt: string;
  dogPhotoPreview: string | null;
  ownerPhotoPreview: string | null;
  rank: number;
  period: string;
  awardedAt: string;
};

export type DiagnosticsResponse = {
  externalApiEnabled: boolean;
  configured: Record<string, boolean>;
  notes: Record<string, string>;
};

export async function generateDiary(request: GenerateDiaryRequest): Promise<GenerateDiaryResponse> {
  const response = await fetch('/api/diaries/generate', {
    method: 'POST',
    headers: authHeaders({
      'Content-Type': 'application/json',
    }),
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `일기 생성 요청 실패: ${response.status}`);
  }

  return response.json();
}

export async function saveUserProfile(request: UserProfileRequest): Promise<UserProfileResponse> {
  const path = request.userId ? `/api/profile/${encodeURIComponent(request.userId)}` : '/api/profile/current';
  const response = await fetch(path, {
    method: 'PUT',
    headers: authHeaders({
      'Content-Type': 'application/json',
    }),
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `프로필 저장 실패: ${response.status}`);
  }

  return response.json();
}

export async function getUserProfile(userId?: string): Promise<UserProfileResponse> {
  const path = userId ? `/api/profile/${encodeURIComponent(userId)}` : '/api/profile/current';
  const response = await fetch(path, { headers: authHeaders() });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `프로필 조회 실패: ${response.status}`);
  }

  return response.json();
}

export async function uploadImage(file: File, type: 'dog' | 'owner' | 'walk'): Promise<UploadImageResponse> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('type', type);

  const response = await fetch('/api/uploads/image', {
    method: 'POST',
    headers: authHeaders(),
    body: formData,
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `이미지 업로드 실패: ${response.status}`);
  }

  return response.json();
}

export async function createWalkRecord(request: {
  dogId: string;
  originalImageUrl?: string | null;
  address?: string;
  weather?: string;
  latitude?: number | null;
  longitude?: number | null;
}): Promise<WalkRecord> {
  const response = await fetch('/api/walk-records', {
    method: 'POST',
    headers: authHeaders({
      'Content-Type': 'application/json',
    }),
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `산책 기록 저장 실패: ${response.status}`);
  }

  return response.json();
}

export async function listDiaries(dogId = 'current-dog', userId?: string): Promise<GenerateDiaryResponse[]> {
  const params = new URLSearchParams({ dogId });
  if (userId) params.set('userId', userId);
  const response = await fetch(`/api/diaries?${params.toString()}`, { headers: authHeaders() });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `일기 목록 조회 실패: ${response.status}`);
  }

  return response.json();
}

export async function signup(request: { email: string; name: string; password: string }): Promise<AuthUserResponse> {
  const response = await fetch('/api/auth/signup', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });
  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `회원가입 실패: ${response.status}`);
  }
  return response.json();
}

export async function login(request: { email: string; password: string }): Promise<AuthUserResponse> {
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });
  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `로그인 실패: ${response.status}`);
  }
  return response.json();
}

export async function getCurrentUser(): Promise<AuthUserResponse> {
  const response = await fetch('/api/auth/me', {
    headers: authHeaders(),
  });
  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `세션 확인 실패: ${response.status}`);
  }
  return response.json();
}

export async function listCommunityPosts(sort: 'latest' | 'popular' = 'latest', query = ''): Promise<CommunityPostResponse[]> {
  const response = await fetch(`/api/community/posts?sort=${encodeURIComponent(sort)}&query=${encodeURIComponent(query)}`, { headers: authHeaders() });
  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `게시물 조회 실패: ${response.status}`);
  }
  return response.json();
}

export async function createCommunityPost(request: Omit<CommunityPostResponse, 'id' | 'createdAt' | 'likes' | 'comments'>): Promise<CommunityPostResponse> {
  const response = await fetch('/api/community/posts', {
    method: 'POST',
    headers: authHeaders({ 'Content-Type': 'application/json' }),
    body: JSON.stringify(request),
  });
  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `게시물 저장 실패: ${response.status}`);
  }
  return response.json();
}

export async function toggleCommunityLike(postId: string, userId: string): Promise<CommunityPostResponse> {
  const response = await fetch(`/api/community/posts/${encodeURIComponent(postId)}/like`, {
    method: 'POST',
    headers: authHeaders({ 'Content-Type': 'application/json' }),
    body: JSON.stringify({ userId }),
  });
  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `좋아요 실패: ${response.status}`);
  }
  return response.json();
}

export async function addCommunityComment(postId: string, request: { authorId: string; authorName: string; content: string }): Promise<CommunityPostResponse> {
  const response = await fetch(`/api/community/posts/${encodeURIComponent(postId)}/comments`, {
    method: 'POST',
    headers: authHeaders({ 'Content-Type': 'application/json' }),
    body: JSON.stringify(request),
  });
  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `댓글 저장 실패: ${response.status}`);
  }
  return response.json();
}

export async function toggleCommunityFollow(followerId: string, followingId: string): Promise<FollowSummaryResponse> {
  const response = await fetch('/api/community/follow', {
    method: 'POST',
    headers: authHeaders({ 'Content-Type': 'application/json' }),
    body: JSON.stringify({ followerId, followingId }),
  });
  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `팔로우 실패: ${response.status}`);
  }
  return response.json();
}

export async function getFollowSummary(userId?: string): Promise<FollowSummaryResponse> {
  const path = userId ? `/api/community/follows/${encodeURIComponent(userId)}` : '/api/community/follows/current';
  const response = await fetch(path, { headers: authHeaders() });
  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `팔로우 조회 실패: ${response.status}`);
  }
  return response.json();
}

export async function listLeaderboard(period: 'weekly' | 'monthly' = 'weekly', limit = 10): Promise<CommunityPostResponse[]> {
  const response = await fetch(`/api/community/leaderboard?period=${encodeURIComponent(period)}&limit=${limit}`, { headers: authHeaders() });
  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `인기 게시물 조회 실패: ${response.status}`);
  }
  return response.json();
}

export async function listBadges(userId?: string, period: 'weekly' | 'monthly' = 'monthly'): Promise<BadgeResponse[]> {
  const params = new URLSearchParams({ period });
  if (userId) params.set('userId', userId);
  const response = await fetch(`/api/community/badges?${params.toString()}`, { headers: authHeaders() });
  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `배지 조회 실패: ${response.status}`);
  }
  return response.json();
}

export async function getDiagnostics(): Promise<DiagnosticsResponse> {
  const response = await fetch('/api/diagnostics', { headers: authHeaders() });
  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `진단 정보 조회 실패: ${response.status}`);
  }
  return response.json();
}

function authHeaders(extra: Record<string, string> = {}) {
  const token = readAuthToken();
  return token ? { ...extra, Authorization: `Bearer ${token}` } : extra;
}

function readAuthToken() {
  try {
    const session = JSON.parse(
      localStorage.getItem('paws-session-v3') ?? localStorage.getItem('paws-session') ?? 'null',
    ) as { token?: string } | null;
    return session?.token || '';
  } catch {
    return '';
  }
}
