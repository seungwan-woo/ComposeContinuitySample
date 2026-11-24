package com.example.adaptivesettings

internal data class SettingSection(
    val id: String,
    val title: String,
    val description: String,
    val examples: List<String>,
)

internal val settingSections = listOf(
    SettingSection("connection", "연결", "네트워크와 주변 기기", listOf("Wi-Fi", "블루투스", "데이터 사용량")),
    SettingSection("sound", "소리 및 진동", "볼륨과 알림음", listOf("미디어 음량", "벨소리", "진동 세기")),
    SettingSection("notifications", "알림", "앱별 알림과 방해 금지", listOf("앱 알림", "알림 기록", "방해 금지")),
    SettingSection(
        "display", "디스플레이", "밝기와 화면 표시 방식",
        listOf("밝기", "화면 모드", "글자 크기", "화면 확대", "화면 꺼짐 시간", "화면 보호기"),
    ),
    SettingSection("battery", "배터리", "전원과 절전", listOf("배터리 사용량", "절전 모드", "충전 상태")),
    SettingSection("wallpaper", "배경화면", "홈 화면과 잠금 화면", listOf("홈 화면 배경", "잠금 화면 배경", "색상 팔레트")),
    SettingSection("security", "보안", "잠금 및 보호", listOf("화면 잠금", "보안 업데이트", "권한 확인")),
    SettingSection("privacy", "개인정보", "앱 권한과 활동 기록", listOf("권한 관리자", "카메라 접근", "마이크 접근")),
    SettingSection("accounts", "계정", "계정과 동기화", listOf("등록된 계정", "자동 동기화", "백업 상태")),
    SettingSection("apps", "애플리케이션", "기본 앱과 사용 정보", listOf("설치된 앱", "기본 앱", "앱 사용 시간")),
    SettingSection("language", "언어 및 입력", "표시 언어와 키보드", listOf("시스템 언어", "키보드", "자동 완성")),
    SettingSection("accessibility", "접근성", "화면 읽기와 표시 지원", listOf("화면 읽기", "시인성 향상", "접근성 바로가기")),
    SettingSection("storage", "저장공간", "기기의 저장공간 사용량", listOf("사진 및 동영상", "문서", "사용 가능한 공간")),
    SettingSection("device", "디바이스 정보", "기기 이름과 시스템 정보", listOf("모델 정보", "소프트웨어 정보", "상태 정보")),
)
