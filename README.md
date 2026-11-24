# Compose 적응형 설정 샘플

설정 목록에서 항목을 고르고 화면을 회전하거나 창 크기를 변경해 보세요. 설정 목록, 선택한 상세, 스크롤 위치와 편집 중인 값을 같은 화면에서 이어서 확인할 수 있습니다.

| 창 조건 | 처음 표시 | 항목을 열어 둔 상태에서 전환 |
| --- | --- | --- |
| 세로형 또는 가로 폭 600dp 미만 | 설정 목록 | 선택한 상세 한 화면과 목록으로 돌아가기 버튼 |
| 가로형이고 가로 폭 600dp 이상 | 왼쪽 목록과 오른쪽 디스플레이 상세 | 왼쪽 목록과 오른쪽에 선택한 상세 |

가로형은 시스템 바와 디스플레이 컷아웃을 제외한 콘텐츠 영역이 높이보다 넓을 때입니다. 600dp는 이 샘플에서 목록에 최소 240dp, 상세에 약 360dp를 배정하기 위해 정한 값입니다. 분할 화면이나 팝업 창에서 가로로 회전해도 이 폭을 확보할 수 없으면 한 화면을 사용합니다. 가로에서 목록 항목을 누른 뒤 창을 줄이면 해당 상세가 남습니다. 상세에서 뒤로 간 뒤 창을 넓히면 마지막으로 선택한 항목이 다시 오른쪽에 표시됩니다.

## 빌드와 실행

Android Studio에서 이 폴더를 열고 JDK 17 이상, Android SDK 37.1과 Build Tools 37.0.0으로 동기화합니다. Gradle 9.6.1, AGP 9.5.2, Kotlin 2.4.21, 안정 Compose BOM 2026.11.01(Compose UI 1.13.0, Material 3 1.5.0)을 사용합니다. 실행 대상은 Android 7.0(API 24) 이상입니다. `SettingsScreen.kt`에서 세로·가로 Compose Preview를 볼 수 있습니다.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.example.adaptivesettings/.MainActivity
```

Windows에서는 `gradlew.bat`를 사용하세요.

## 회전과 리사이즈 처리

- `MainActivity`는 크기와 방향에 관련된 네 가지 구성 변경을 직접 처리합니다. 동일한 Activity에서 Compose가 최신 제약으로 UI를 배치합니다. 구성 변경 로그의 `activity=` 값을 비교할 수 있습니다. 시스템 창에는 `ROTATION_ANIMATION_JUMPCUT`을 요청합니다. Android나 기기 설정이 이 요청을 덮어쓸 수 있습니다.
- `BoxWithConstraints`가 시스템 바와 컷아웃을 제외한 콘텐츠 크기로 모드를 고릅니다. 키보드 인셋은 모드를 고른 뒤 적용합니다. 2-pane 안에서 창을 드래그할 때 목록 폭은 Compose의 측정 단계에서 매번 계산하고, 상세는 남은 폭을 채웁니다.
- 상세가 열린 채로 경계를 오가면 같은 상세 pane과 편집기가 220ms 동안 위치와 크기를 바꿉니다. 목록은 경계선에 맞춰 펼쳐지거나 접히고, 상세의 뒤로 가기 영역도 함께 이동합니다. 앱은 페이드나 화면 복사본을 사용하지 않습니다. 같은 pane 모드에서는 이 애니메이션 없이 창 크기를 바로 따라갑니다.
- `rememberSaveable`로 선택 ID, 단일 pane에서의 목적지, 자동 밝기 값을 저장하고, `rememberTextFieldState`로 편집값과 텍스트 선택 범위를 저장합니다. 목록은 하나의 `LazyListState`를 공유하고, 상세 스크롤은 항목별로 `SaveableStateHolder`에 저장합니다. 창이 커져 스크롤 범위가 줄어들면 오프셋은 새 범위에 맞춰 보정됩니다.
- 자동 밝기와 기기 이름은 앱 내부의 데모 UI 상태로 관리합니다.

## 단말 확인 항목

| 동작 | 기대 결과 |
| --- | --- |
| 세로에서 목록을 스크롤하고 `디바이스 정보`를 열어 기기 이름 수정 → 가로 → 세로 | 가로에서 양쪽 pane이 보입니다. 같은 상세와 입력값이 유지되고, 목록으로 돌아가면 스크롤을 이어갑니다. |
| 가로에서 `디스플레이`의 자동 밝기를 켜고 창 폭을 경계 양쪽으로 반복 변경 | pane 수만 전환됩니다. 상세의 제목과 스위치 값이 이어집니다. |
| 분할 화면이나 팝업 창을 드래그하며 `adb logcat -s AdaptiveSettings` 확인 | 로그가 발생하면 같은 `activity=` 값을 유지합니다. 회전·리사이즈 시나리오에서 `onCreate`는 최초 실행 때 한 번 기록됩니다. 구성 변경 콜백의 빈도는 플랫폼 버전에 따라 달라질 수 있습니다. |
| 개발자 옵션의 ‘활동 유지 안 함’을 켜고 편집 후 홈 → 최근 앱에서 복귀 | 일반적인 Activity 재생성 후에도 선택과 입력을 복원합니다. 검증 후 개발자 옵션을 되돌립니다. |
| 회전·연속 리사이즈 중 Android Studio System Trace 수집 | 화면 녹화에서 빈 프레임과 스크롤 점프를 확인하고, Frame Timeline에서 앱·시스템 프레임 지연을 구분합니다. |

순수 Kotlin 단위 테스트는 600dp 경계, 선택한 상세의 창 크기 전환, 줄어든 창에서 뒤로 가기 흐름을 확인합니다. Compose UI 테스트는 편집기의 시작·중간·종료 좌표와, 반복 리사이즈 중 편집 포커스와 입력값이 유지되는지 확인합니다. 기기나 에뮬레이터를 연결하고 다음 명령을 실행하세요.

```sh
./gradlew :app:connectedDebugAndroidTest
```

실제 시스템 회전이나 창 드래그 중의 프레임 지연과 깜빡임은 위 절차에 따라 단말에서 별도로 확인합니다.

- [Android Developers — 구성 변경 처리](https://developer.android.com/guide/topics/resources/runtime-changes)
- [Android Developers — 목록·상세 레이아웃](https://developer.android.com/develop/adaptive-apps/guides/list-detail)
- [Android Developers — Compose UI 상태 저장](https://developer.android.com/develop/ui/compose/state-saving)
- [Android Developers — UI jank detection](https://developer.android.com/studio/profile/jank-detection)
