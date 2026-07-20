# ⚾ BaseLog (베이스로그)
> **"나의 모든 직관 기록이 팀의 승률이 되는 곳"**  
> 사용자의 야구 직관 기록을 체계적으로 관리하고, 친구들과 실시간 승률 랭킹을 공유하는 서비스입니다.

---

## 📸 Screenshots

| 홈 대시보드 | 직관 기록부 | 친구 관리 | 실시간 랭킹 | 마이페이지 |
| :---: | :---: | :---: | :---: | :---: |
| <img width="1080" height="2400" alt="Image" src="https://github.com/user-attachments/assets/ae27962c-9fea-4bed-8f70-235d58a2f1bb" /> | <img width="1080" height="2400" alt="Image" src="https://github.com/user-attachments/assets/4c201ec8-0b4b-43a0-b925-7f1586d26749" /> | <img width="1080" height="2400" alt="Image" src="https://github.com/user-attachments/assets/8c96f023-f615-4c70-94e3-40745a0f5e92" /> | <img width="1080" height="2400" alt="Image" src="https://github.com/user-attachments/assets/47e27c84-704c-44ff-9f61-26fb004b91b6" /> | <img width="1080" height="2400" alt="Image" src="https://github.com/user-attachments/assets/0d2b73c5-9541-46bc-b120-30aea3ef5e52" /> |

---

## ✨ 주요 기능

### 🏠 실시간 직관 대시보드
- **승률 시각화**: Firebase 데이터를 실시간으로 반영하여 전체 및 시즌별 직관 승률을 차트로 제공합니다.
- **맞춤형 피드백**: 현재 승률에 따라 동적으로 변하는 응원 메시지 로직을 구현했습니다.

### 📝 스마트 직관 기록부 (Logbook)
- **기록 관리**: 경기 날짜, 응원 팀, 결과를 간편하게 기록하고 관리(CRUD)합니다.
- **지능형 필터링**: `Flow` 연산자를 활용하여 다년간의 데이터를 연도별로 즉시 분류 및 조회할 수 있습니다.

### 🏆 소셜 리더보드 & 친구 시스템
- **실시간 랭킹**: 나와 친구들의 성적을 결합하여 승률/승수 기반의 실시간 순위를 산출합니다.
- **친구 인터랙션**: 닉네임 검색을 통한 친구 신청/수락 및 실시간 상태 관리를 지원합니다.

---

## 🛠️ Tech Stack

| Category | Technology |
| :--- | :--- |
| **Language** | Kotlin |
| **UI** | Jetpack Compose (Material 3) |
| **Architecture** | MVVM, Clean Architecture, Unidirectional Data Flow (UDF) |
| **DI** | Dagger Hilt |
| **Local DB** | Jetpack DataStore (Preferences) |
| **Backend** | Firebase (Firestore, Authentication) |
| **Auth** | Google Identity Service (Social Login) |
| **Async** | Coroutines, Kotlin Flow (StateFlow, SharedFlow) |

---

## 📂 Project Structure

```
com.mokostudio.baselog
├── core             # 공통 비즈니스 로직 및 전역 데이터 관리
│   ├── auth         # 인증 관련 Repository 및 데이터 소스
│   ├── startup      # 앱 시작 시 초기 목적지 판단 로직
│   └── user         # 사용자 프로필 관리 로직
├── feature          # 화면별 기능 구현 (UI & ViewModel)
│   ├── auth         # 로그인 및 인증 UI
│   ├── friends      # 친구 검색, 신청, 소셜 랭킹 UI
│   ├── home         # 메인 대시보드 UI
│   └── log          # 직관 기록 작성 및 통계 조회 UI
├── navigation       # 앱 전체 내비게이션 그래프 및 목적지 정의
└── ui.theme         # 디자인 시스템 및 Compose 테마 설정
```
---

## 🏗 Architecture & Design

BaseLog는 **단방향 데이터 흐름(UDF)** 과 **Clean Architecture** 원칙을 준수하여 유지보수성과 확장성을 확보했습니다.

- **Presentation Layer**: UI 상태와 이벤트를 엄격히 분리하여 `UiState`와 `SharedFlow(Event)` 기반의 안정적인 화면 제어를 구현했습니다.
- **Domain Layer**: 비즈니스 로직을 추상화된 Repository 인터페이스 뒤로 숨겨 확장성을 높였습니다.
- **Data Layer**: Firebase Firestore를 활용하며, 서버 데이터의 불완전성을 대비해 `mapNotNull`과 `orEmpty`를 활용한 데이터 파싱 방어 로직을 적용했습니다.

---

## 🚀 핵심 구현 상세

### 1️⃣ 복잡한 실시간 데이터 파이프라인 (`combine` & `flatMapLatest`)
리더보드 기능을 위해 **내 기록 + 친구 목록 + 각 친구들의 개별 기록** 이라는 8개 이상의 서로 다른 `Flow`를 결합해야 했습니다. 중첩된 `combine` 연산자를 활용하여 데이터 일관성을 유지하면서도 메모리 누수를 방지하는 반응형 구조를 설계했습니다.

### 2️⃣ 유연한 순위 산정 로직 (`toRankedEntries`)
단순 정렬이 아닌 **4단계 조건부 정렬(승률 -> 판수 -> 승수 -> 이름순)** 을 적용했습니다. `Comparator`와 확장 함수를 활용하여 동점자 처리까지 포함된 엄격한 순위 시스템을 구축했습니다.

### 3️⃣ 사용자 경험(UX) 최적화
- **Action In-Flight Pattern**: 친구 신청/수락 시, 작업 중인 항목에만 로딩 상태를 부여하여 중복 클릭을 방지하고 즉각적인 피드백을 제공합니다.
- **Pending Action**: 중요 데이터 삭제 시 `pendingDelete` 상태를 도입하여 사용자의 실수 방지를 위한 확인 절차를 체계화했습니다.

---

## 💡 개발 회고
- **비동기 통신의 이해**: Firebase의 리스너 방식을 `callbackFlow`로 변환하며 비동기 리소스 관리(`awaitClose`)의 중요성을 깊이 이해했습니다.
- **확장성 있는 설계**: 인터페이스를 통한 의존성 분리를 실천하며, 코드의 결합도를 낮추고 테스트 용이성을 확보하는 경험을 했습니다.
