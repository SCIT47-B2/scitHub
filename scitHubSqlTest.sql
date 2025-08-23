-- =============================================================
-- SCIT Hub — Lean Schema v0 (MySQL 8.0+)
-- Charset: utf8mb4 / Collation: utf8mb4_0900_ai_ci
-- 목적: 요구사항 충족 + 테이블 수 축소(폴리모픽/JSON/인라인화)
-- 포함: 고스트(삭제) 유저, 핵심 보조 인덱스, Q&A 동기화/삭제 시 정리 트리거
-- 주석: 각 컬럼(속성) 단위 상세 설명을 컬럼 선언 "직전" 라인에 -- 주석으로 기재
-- =============================================================

-- -------------------------------------------------------------
-- 0) 기본 설정
-- -------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS scithub0
    -- 기본 문자셋: 이모지 포함
    DEFAULT CHARACTER SET utf8mb4
    -- 한국어/일본어 등 CJK 문자열 비교에 무난한 Collation
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE scithub0;

SET NAMES utf8mb4;
SET time_zone = '+09:00';

-- =============================================================
-- 1) 사용자/그룹(조)
-- =============================================================

-- 조(그룹) — 사용자에게 직접 FK(1:다)로 소속. 조 배정 이력 관리가 필요하면 별도 히스토리 테이블로 확장.
CREATE TABLE study_groups (
    -- 내부 식별자(PK)
    group_id      INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    -- 대상 기수(예: 47). 동일 기수 내에서 반/조 이름 유니크.
    cohort_no     INT NOT NULL,
    -- 반 구분(A/B)
    class_section ENUM('A','B') NOT NULL,
    -- 조 이름(예: 1조, Red 등)
    name          VARCHAR(50) NOT NULL,
    -- UI 표시 정렬용(작을수록 상단)
    order_index   INT NOT NULL DEFAULT 0,
    -- 생성 시각
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- (cohort_no + class_section + name) 유니크 보장
    UNIQUE KEY uq_group_name (cohort_no, class_section, name)
) ENGINE=InnoDB;

-- 사용자
CREATE TABLE users (
    -- 내부 식별자(PK)
    user_id        INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    -- 로그인 ID(유니크, 4~32자 제약)
    username       VARCHAR(32)  NOT NULL UNIQUE,
    -- 로그인/알림용 이메일(유니크)
    email          VARCHAR(255) NOT NULL UNIQUE,
    -- 전화번호(선택)
    phone          VARCHAR(20)  NULL,
    -- 비밀번호 해시(BCrypt/Argon2 등)
    password_hash  VARCHAR(255) NOT NULL,
    -- 실명(한글)
    name_kor       VARCHAR(50)  NOT NULL,
    -- 생년월일(선택)
    birth_date     DATE NULL,
    -- 성별: M/F/기타(O)/무응답(N)
    gender         ENUM('M','F','O','N') NOT NULL DEFAULT 'N',
    -- 기수(예: 47) — 조회/필터용
    cohort_no      INT NULL,
    -- IT 반(A/B) — 조회/필터용
    it_class       ENUM('A','B') NULL,
    -- IT 세션(AM/PM) — 오전/오후
    it_session     ENUM('AM','PM') NULL,
    -- 일본어 반(A~F)
    jp_class       ENUM('A','B','C','D','E','F') NULL,
    -- 프로필 이미지 URL
    avatar_url     VARCHAR(500) NULL,
    -- 현재 소속 조(FK). 동일 기수/반에서 1개 조만 소속.
    group_id       INT UNSIGNED NULL,
    -- 예약 패널티: 신고 누적 횟수(3회 이상이면 금지)
    resv_strike_count INT NOT NULL DEFAULT 0,
    -- 예약 금지 해제 시각(금지 중이면 값 존재)
    resv_banned_until DATETIME NULL,
    -- 계정 활성(로그인 가능 여부)
    is_active      TINYINT NOT NULL DEFAULT 1,
    -- 관리자 권한 여부(간단 플래그)
    is_admin       TINYINT NOT NULL DEFAULT 0,
    -- 최근 로그인 시각
    last_login_at  DATETIME NULL,
    -- 생성 시각
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 업데이트 시각
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    -- username 길이 제약
    CONSTRAINT chk_users_username CHECK (CHAR_LENGTH(username) BETWEEN 4 AND 32),
    -- 그룹 FK: 조가 삭제되면 사용자 측은 NULL(소속 해제)
    CONSTRAINT fk_users_group FOREIGN KEY (group_id) REFERENCES study_groups(group_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 기수/반 조회 최적화
CREATE INDEX idx_users_cohort_class ON users (cohort_no, it_class);

-- 삭제 사용자(고스트) 계정 선생성
INSERT INTO users (
    username, email, phone, password_hash, name_kor, birth_date, gender,
    cohort_no, it_class, it_session, jp_class, avatar_url,
    group_id, resv_strike_count, resv_banned_until,
    is_active, is_admin, last_login_at
) VALUES (
    -- 고정 username/이메일(로그인 불가)
    '__deleted_user__',
    '__deleted_user__@local.invalid',
    NULL,
    '!',                    -- 임의값: 인증/로그인 불가
    '탈퇴한 회원',          -- 표시용
    NULL, 'N',
    NULL, NULL, NULL, NULL, NULL,
    NULL, 0, NULL,
    0, 0, NULL
);

-- =============================================================
-- 2) 쪽지/알림
-- =============================================================

-- 쪽지
CREATE TABLE direct_messages (
    -- 내부 식별자(PK)
    message_id         INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    -- 발신자(삭제 시 RESTRICT → 고스트로 치환 트리거에서 처리)
    sender_id          INT UNSIGNED NOT NULL,
    -- 수신자(삭제 시 RESTRICT → 고스트로 치환 트리거에서 처리)
    receiver_id        INT UNSIGNED NOT NULL,
    -- 제목(선택)
    subject            VARCHAR(200) NULL,
    -- 본문
    body               MEDIUMTEXT NOT NULL,
    -- 수신자 열람 여부(0/1)
    is_read            TINYINT NOT NULL DEFAULT 0,
    -- 열람 시각
    read_at            DATETIME NULL,
    -- 발송 시각
    created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 발신자 측 삭제 플래그(소프트 삭제)
    deleted_by_sender   TINYINT NOT NULL DEFAULT 0,
    -- 수신자 측 삭제 플래그(소프트 삭제)
    deleted_by_receiver TINYINT NOT NULL DEFAULT 0,
    -- FK
    CONSTRAINT fk_dm_sender   FOREIGN KEY (sender_id)   REFERENCES users(user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_dm_receiver FOREIGN KEY (receiver_id) REFERENCES users(user_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- 수신자/읽음여부/최신순
CREATE INDEX idx_dm_receiver_read  ON direct_messages (receiver_id, is_read, created_at DESC);
-- 발신함 최신순
CREATE INDEX idx_dm_sender_created ON direct_messages (sender_id, created_at);
-- 대량 업데이트 대비 보조 인덱스
CREATE INDEX idx_dm_sender_id      ON direct_messages (sender_id);
CREATE INDEX idx_dm_receiver_id    ON direct_messages (receiver_id);

-- 알림
CREATE TABLE notifications (
    -- 내부 식별자(PK)
    notification_id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    -- 수신자(삭제 시 함께 삭제)
    user_id         INT UNSIGNED NOT NULL,
    -- 알림 유형(공지/댓글/쪽지/조배정/일정/예약/시스템)
    type            ENUM('NOTICE','COMMENT','MESSAGE','GROUP_ASSIGN','SCHEDULE','RESERVATION','SYSTEM') NOT NULL,
    -- 알림 제목(토스트/목록 표기)
    title           VARCHAR(150) NOT NULL,
    -- 알림 본문(간단 설명)
    body            VARCHAR(500) NULL,
    -- 클릭 시 이동 경로(URL)
    target_url      VARCHAR(500) NULL,
    -- 참조 엔티티 유형(예: POST/EVENT)
    ref_type        VARCHAR(50) NULL,
    -- 참조 엔티티 ID
    ref_id          INT UNSIGNED NULL,
    -- 읽음 여부(0/1)
    is_read         TINYINT NOT NULL DEFAULT 0,
    -- 생성 시각
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 읽음 시각
    read_at         DATETIME NULL,
    -- FK
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 사용자별 미읽음 우선 최신순
CREATE INDEX idx_notifications_user_latest ON notifications (user_id, is_read, created_at DESC);

-- =============================================================
-- 3) 게시판/게시글/댓글/리액션/첨부
-- =============================================================

-- 게시판
CREATE TABLE boards (
    -- 내부 식별자(PK)
    board_id     INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    -- 프로그램에서 식별하는 키(예: NOTICE_OPS, QNA) — 유니크
    board_key    VARCHAR(50) NOT NULL UNIQUE,
    -- 표시 이름(예: 공지-운영)
    name         VARCHAR(100) NOT NULL,
    -- 설명(선택)
    description  VARCHAR(255) NULL,
    -- Q&A 게시판 여부(1이면 작성 글이 QnA 성격)
    is_qna       TINYINT NOT NULL DEFAULT 0,
    -- 공지 전용 여부(1이면 운영측만 작성)
    is_notice    TINYINT NOT NULL DEFAULT 0,
    -- 공개 여부(1=비로그인 열람 허용 등 정책)
    is_public    TINYINT NOT NULL DEFAULT 1,
    -- 생성 시각
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 게시글
CREATE TABLE posts (
    -- 내부 식별자(PK)
    post_id        INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    -- 게시판(FK, 삭제 시 글 같이 삭제)
    board_id       INT UNSIGNED NOT NULL,
    -- 작성자(FK, 삭제는 RESTRICT → 고스트로 치환)
    author_id      INT UNSIGNED NOT NULL,
    -- 제목
    title          VARCHAR(200) NOT NULL,
    -- 본문(HTML/Markdown 가능)
    content        MEDIUMTEXT NOT NULL,
    -- QnA 답변 상태(PENDING/ANSWERED). 댓글 is_answer와 동기화 트리거로 유지.
    answer_status  ENUM('PENDING','ANSWERED') NOT NULL DEFAULT 'PENDING',
    -- 태그 JSON 배열(예: ["spring","mysql"])
    tags_json      JSON NULL,
    -- 상태(활성/삭제/차단)
    status         ENUM('ACTIVE','DELETED','BLOCKED') NOT NULL DEFAULT 'ACTIVE',
    -- 조회수(증가 시 단순 +1)
    view_count     INT UNSIGNED NOT NULL DEFAULT 0,
    -- 생성 시각
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 수정 시각
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    -- FK
    CONSTRAINT fk_posts_board  FOREIGN KEY (board_id)  REFERENCES boards(board_id) ON DELETE CASCADE,
    CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users(user_id)  ON DELETE RESTRICT
) ENGINE=InnoDB;

-- 작성자별 조회/관리
CREATE INDEX idx_posts_author_id      ON posts (author_id);
-- 게시판별 최신순 리스트
CREATE INDEX idx_posts_board_created  ON posts (board_id, created_at DESC);
-- 작성자별 최신순
CREATE INDEX idx_posts_author_created ON posts (author_id, created_at);

-- CJK(한국어/일본어) 검색 품질 향상을 위한 ngram FULLTEXT
CREATE FULLTEXT INDEX ftx_posts_title_content
ON posts (title, content) WITH PARSER ngram;

-- 폴리모픽 첨부: POST/ASSIGNMENT/PHOTO 등에서 공용 사용
CREATE TABLE attachments (
    -- 내부 식별자(PK)
    attachment_id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    -- 참조 유형(POST/ASSIGNMENT/PHOTO/OTHER)
    ref_type      ENUM('POST','ASSIGNMENT','PHOTO','OTHER') NOT NULL DEFAULT 'POST',
    -- 참조 대상 ID(각 ref_type에 해당하는 PK)
    ref_id        INT UNSIGNED NOT NULL,
    -- 저장 URL(객체 스토리지 권장)
    file_url      VARCHAR(1024) NOT NULL,
    -- 원본 파일명(다운로드 시 표시)
    file_name     VARCHAR(255) NOT NULL,
    -- 파일 크기(Byte)
    file_size     INT UNSIGNED NULL,
    -- 업로드 시각
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 폴리모픽 댓글: POST/PHOTO/REVIEW 공용, 대댓글(parent_id) 가능
CREATE TABLE comments (
    -- 내부 식별자(PK)
    comment_id   INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    -- 참조 유형(POST/PHOTO/REVIEW)
    ref_type     ENUM('POST','PHOTO','REVIEW') NOT NULL,
    -- 참조 대상 ID
    ref_id       INT UNSIGNED NOT NULL,
    -- 작성자(FK, 삭제 RESTRICT → 고스트로 치환)
    author_id    INT UNSIGNED NOT NULL,
    -- 부모 댓글 ID(대댓글). NULL이면 최상위 댓글.
    parent_id    INT UNSIGNED NULL,
    -- 본문
    content      MEDIUMTEXT NOT NULL,
    -- QnA 답변 플래그(POST에서만 의미). 1개 이상이면 글 상태 ANSWERED.
    is_answer    TINYINT NOT NULL DEFAULT 0,
    -- 생성 시각
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 수정 시각
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    -- FK
    CONSTRAINT fk_cmt_author FOREIGN KEY (author_id) REFERENCES users(user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_cmt_parent FOREIGN KEY (parent_id) REFERENCES comments(comment_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 참조별 최신순
CREATE INDEX idx_comments_ref_created    ON comments (ref_type, ref_id, created_at);
-- 작성자별 최신순
CREATE INDEX idx_comments_author_created ON comments (author_id, created_at);
-- 작성자 보조 인덱스
CREATE INDEX idx_comments_author_id      ON comments (author_id);

-- 폴리모픽 리액션: 좋아요/북마크/신고 공용
CREATE TABLE reactions (
    -- 내부 식별자(PK)
    reaction_id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    -- 참조 유형(POST/PHOTO/REVIEW/RESERVATION/BOARD)
    ref_type    ENUM('POST','PHOTO','REVIEW','RESERVATION','BOARD') NOT NULL,
    -- 참조 대상 ID
    ref_id      INT UNSIGNED NOT NULL,
    -- 행위자(리액션 주체)
    user_id     INT UNSIGNED NOT NULL,
    -- 리액션 유형(좋아요/북마크/신고)
    type        ENUM('LIKE','BOOKMARK','REPORT') NOT NULL,
    -- 신고 심사 상태(신고에만 사용)
    status      ENUM('PENDING','CONFIRMED','REJECTED') NULL,
    -- 신고 사유(선택)
    reason      VARCHAR(255) NULL,
    -- 생성 시각
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- FK
    CONSTRAINT fk_react_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    -- 동일 ref + user + type 중복 방지(1인 1회 규칙)
    UNIQUE KEY uq_react_once (ref_type, ref_id, user_id, type)
) ENGINE=InnoDB;

-- =============================================================
-- 4) 캘린더/문의
-- =============================================================

-- 일정 (캘린더)
CREATE TABLE events (
    -- 내부 식별자(PK)
    event_id       INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    -- GLOBAL: 공통 일정 / PERSONAL: 개인 일정
    visibility     ENUM('GLOBAL','PERSONAL') NOT NULL DEFAULT 'GLOBAL',
    -- 개인 일정일 때 소유자(FK, 삭제 시 함께 삭제)
    owner_user_id  INT UNSIGNED NULL,
    -- 대상 기수(공통 일정 필터)
    cohort_no      INT NULL,
    -- 대상 반 범위(ALL/A/B)
    it_class_scope ENUM('ALL','A','B') NOT NULL DEFAULT 'ALL',
    -- 제목
    title          VARCHAR(150) NOT NULL,
    -- 설명(유의 사항 등)
    description    MEDIUMTEXT NULL,
    -- 시작 시각
    start_at       DATETIME NOT NULL,
    -- 종료 시각(> start_at)
    end_at         DATETIME NOT NULL,
    -- 종일 여부(1=종일)
    is_all_day     TINYINT NOT NULL DEFAULT 0,
    -- D-Day 표시 여부(홈 위젯 등)
    dday_enabled   TINYINT NOT NULL DEFAULT 0,
    -- 작성자(주로 관리자)
    created_by     INT UNSIGNED NULL,
    -- 생성 시각
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 수정 시각
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    -- 시간 일관성 제약
    CONSTRAINT chk_events_time CHECK (end_at > start_at),
    -- FK
    CONSTRAINT fk_events_owner   FOREIGN KEY (owner_user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_events_creator FOREIGN KEY (created_by)     REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- GLOBAL/PERSONAL + 기간 조회 최적화
CREATE INDEX idx_events_visibility_time ON events (visibility, start_at, end_at);
-- 소유자 일정 조회
CREATE INDEX idx_events_owner_time      ON events (owner_user_id, start_at);

-- 문의(좌석 피드백 포함). 답변 테이블 없이 인라인 컬럼으로 단순화.
CREATE TABLE inquiries (
    -- 내부 식별자(PK)
    inquiry_id    INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    -- 작성자(FK, 삭제 RESTRICT → 고스트로 치환)
    user_id       INT UNSIGNED NOT NULL,
    -- 카테고리(GENERAL/SEAT/RESERVATION/OTHER)
    category      ENUM('GENERAL','SEAT','RESERVATION','OTHER') NOT NULL DEFAULT 'GENERAL',
    -- 제목
    subject       VARCHAR(150) NOT NULL,
    -- 본문(문의 내용)
    content       MEDIUMTEXT NOT NULL,
    -- 좌석 피드백 연계 시 좌석 ID(아래 좌석 생성 후 FK 추가)
    seat_id       INT UNSIGNED NULL,
    -- 상태(OPEN/ANSWERED/CLOSED)
    status        ENUM('OPEN','ANSWERED','CLOSED') NOT NULL DEFAULT 'OPEN',
    -- 답변 본문(간단응답; 상세 스레딩 필요하면 comments 사용)
    answer_text   MEDIUMTEXT NULL,
    -- 답변자(FK, 삭제 시 NULL)
    answered_by   INT UNSIGNED NULL,
    -- 답변 시각
    answered_at   DATETIME NULL,
    -- 생성 시각
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 수정 시각
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    -- FK
    CONSTRAINT fk_inq_user     FOREIGN KEY (user_id)     REFERENCES users(user_id)     ON DELETE RESTRICT,
    CONSTRAINT fk_inq_answerer FOREIGN KEY (answered_by) REFERENCES users(user_id)     ON DELETE SET NULL
) ENGINE=InnoDB;

-- 사용자별/상태별 최신순
CREATE INDEX idx_inquiries_user_status_created ON inquiries (user_id, status, created_at);

-- =============================================================
-- 5) 좌석/예약
-- =============================================================

-- 방 테이블
CREATE TABLE rooms (
    -- 내부 식별자(PK)
    room_id    INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    -- 방 이름(예: 4F-401, 4F-자습1)
    name       VARCHAR(100) NOT NULL,
    -- 유형: 강의실/자습/회의
    type       ENUM('CLASSROOM','STUDY','MEETING') NOT NULL DEFAULT 'CLASSROOM',
    -- 층수(선택)
    floor_no   INT NULL,
    -- 수용 인원(선택)
    capacity   INT NULL,
    -- 사용 여부(비활성 시 배정/예약 제한)
    is_active  TINYINT NOT NULL DEFAULT 1,
    -- 생성 시각
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 좌석맵/배정 테이블 없이 단일 seats로 축소 관리
CREATE TABLE seats (
    -- 내부 식별자(PK)
    seat_id          INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    -- 소속 방(FK)
    room_id          INT UNSIGNED NOT NULL,
    -- 대상 기수(좌석 배치 버전 대체: 기수별로 구분)
    cohort_no        INT NOT NULL,
    -- 반(A/B)
    class_section    ENUM('A','B') NOT NULL,
    -- 좌석 코드(표시용/유니크: 예 A-01)
    seat_code        VARCHAR(20) NOT NULL,
    -- UI 배치용 좌표(행)
    row_no           INT NULL,
    -- UI 배치용 좌표(열)
    col_no           INT NULL,
    -- 현재 배정 사용자(FK, 자리 비움은 NULL)
    assigned_user_id INT UNSIGNED NULL,
    -- 배정 시각
    assigned_at      DATETIME NULL,
    -- 생성 시각
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 동일 방/기수/반 내 좌석 코드 유니크
    UNIQUE KEY uq_seat_code (room_id, cohort_no, class_section, seat_code),
    -- 사용자당 해당 기수/반에서 1좌석만 허용(NULL은 제외)
    UNIQUE KEY uq_seat_user (cohort_no, class_section, assigned_user_id),
    -- FK
    CONSTRAINT fk_seat_room FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE CASCADE,
    CONSTRAINT fk_seat_user FOREIGN KEY (assigned_user_id) REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- (inquiries.seat_id) 좌석 FK 연결
ALTER TABLE inquiries
    ADD CONSTRAINT fk_inq_seat FOREIGN KEY (seat_id) REFERENCES seats(seat_id) ON DELETE SET NULL;

-- 예약 테이블
CREATE TABLE reservations (
    -- 내부 식별자(PK)
    reservation_id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    -- 방(FK)
    room_id        INT UNSIGNED NOT NULL,
    -- 예약자(FK, 삭제 시 예약도 같이 삭제)
    user_id        INT UNSIGNED NOT NULL,
    -- 시작 시각
    start_at       DATETIME NOT NULL,
    -- 종료 시각
    end_at         DATETIME NOT NULL,
    -- 상태(ACTIVE/취소/노쇼/차단)
    status         ENUM('ACTIVE','CANCELLED','NO_SHOW','BLOCKED') NOT NULL DEFAULT 'ACTIVE',
    -- 생성 시각
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 시간 일관성
    CONSTRAINT chk_res_time CHECK (end_at > start_at),
    -- 1인 최대 120분 제약
    CONSTRAINT chk_res_duration CHECK (TIMESTAMPDIFF(MINUTE, start_at, end_at) BETWEEN 1 AND 120),
    -- FK
    CONSTRAINT fk_res_room FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE CASCADE,
    CONSTRAINT fk_res_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 방/시간대 조회
CREATE INDEX idx_res_room_time ON reservations (room_id, start_at, end_at);
-- 사용자/시간대 조회
CREATE INDEX idx_res_user_time ON reservations (user_id, start_at, end_at);

-- =============================================================
-- 6) 졸업생/강의/과제/사진
-- =============================================================

-- 회사(졸업생 리뷰용)
CREATE TABLE companies (
    -- 내부 식별자(PK)
    company_id     INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    -- 회사명(유니크)
    name           VARCHAR(150) NOT NULL UNIQUE,
    -- 로고 URL(선택)
    logo_url       VARCHAR(500) NULL,
    -- 산업/계열(선택)
    sector         VARCHAR(100) NULL,
    -- 위치 텍스트(선택)
    location_text  VARCHAR(150) NULL,
    -- 인원수(표기용)
    headcount      INT NULL,
    -- 생성 시각
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 회사 리뷰 테이블
CREATE TABLE company_reviews (
    -- 내부 식별자(PK)
    review_id    INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    -- 회사(FK)
    company_id   INT UNSIGNED NOT NULL,
    -- 리뷰어(FK, 삭제 RESTRICT → 고스트로 치환)
    reviewer_id  INT UNSIGNED NULL,
    -- 평점(1~5)
    rating       TINYINT NOT NULL,
    -- 본문(선택)
    body         MEDIUMTEXT NULL,
    -- 작성 시각
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 제약
    CONSTRAINT chk_cr_rating CHECK (rating BETWEEN 1 AND 5),
    -- 1인 1회 리뷰
    UNIQUE KEY uq_review_once (company_id, reviewer_id),
    -- FK
    CONSTRAINT fk_cr_company FOREIGN KEY (company_id)  REFERENCES companies(company_id) ON DELETE CASCADE,
    CONSTRAINT fk_cr_user    FOREIGN KEY (reviewer_id) REFERENCES users(user_id)        ON DELETE SET NULL
) ENGINE=InnoDB;

-- 리뷰어별 조회
CREATE INDEX idx_cr_user            ON company_reviews (reviewer_id);
-- 회사별 최신순
CREATE INDEX idx_cr_company_created ON company_reviews (company_id, created_at);

-- 회사 평점 요약 뷰
CREATE OR REPLACE VIEW v_company_ratings AS
SELECT
    c.company_id,
    c.name,
    c.sector,
    c.location_text,
    c.headcount,
    AVG(r.rating) AS avg_rating,
    COUNT(r.review_id) AS review_count
FROM companies c
LEFT JOIN company_reviews r ON r.company_id = c.company_id
GROUP BY c.company_id;

-- 과목(강의)
CREATE TABLE courses (
    -- 내부 식별자(PK)
    course_id       INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    -- 과목명(예: Spring MVC)
    name            VARCHAR(150) NOT NULL,
    -- 담당 강사(FK, 삭제 시 NULL)
    instructor_id   INT UNSIGNED NULL,
    -- 과목 트랙(IT/JP)
    course_type     ENUM('IT','JP') NOT NULL,
    -- 대상 기수(선택)
    cohort_no       INT NULL,
    -- 반(A/B, 선택)
    class_section   ENUM('A','B') NULL,
    -- 생성 시각
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- FK
    CONSTRAINT fk_courses_instructor FOREIGN KEY (instructor_id) REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 강사/기수/반 조회
CREATE INDEX idx_courses_instructor ON courses (instructor_id, cohort_no, class_section);
-- 기수/반/트랙 조회
CREATE INDEX idx_courses_scope_type ON courses (cohort_no, class_section, course_type);

-- 수강 등록(강의평가 가능 범위)
CREATE TABLE enrollments (
    -- 과목(FK)
    course_id INT UNSIGNED NOT NULL,
    -- 수강자(FK)
    user_id   INT UNSIGNED NOT NULL,
    -- PK(복합): 1과목 1유저 1레코드
    PRIMARY KEY (course_id, user_id),
    -- FK
    CONSTRAINT fk_enr_course FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
    CONSTRAINT fk_enr_user   FOREIGN KEY (user_id)   REFERENCES users(user_id)   ON DELETE CASCADE
) ENGINE=InnoDB;

-- 유저별 수강 조회
CREATE INDEX idx_enr_user ON enrollments (user_id);

-- 강의 평가(6문항 1~5, 1인 1회)
CREATE TABLE course_evaluations (
    -- 내부 식별자(PK)
    evaluation_id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    -- 과목(FK)
    course_id     INT UNSIGNED NOT NULL,
    -- 평가자(FK, 삭제 RESTRICT → 고스트 치환)
    user_id       INT UNSIGNED NULL,
    -- 성실성: 수업 준비 철저
    score_preparedness TINYINT NOT NULL,
    -- 전문성: 명확한 설명
    score_clarity     TINYINT NOT NULL,
    -- 명확성: 평가 기준/방식 객관·합리
    score_fairness    TINYINT NOT NULL,
    -- 소통성: 질문 수용/응답
    score_respond     TINYINT NOT NULL,
    -- 흥미도: 몰입/구성
    score_engagement  TINYINT NOT NULL,
    -- 열정성: 열정적으로 임함
    score_passion     TINYINT NOT NULL,
    -- 개선 의견(서술)
    comment_text      MEDIUMTEXT NULL,
    -- 작성 시각
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 점수 범위 제약
    CONSTRAINT chk_ce_scores CHECK (
        score_preparedness BETWEEN 1 AND 5 AND
        score_clarity      BETWEEN 1 AND 5 AND
        score_fairness     BETWEEN 1 AND 5 AND
        score_respond      BETWEEN 1 AND 5 AND
        score_engagement   BETWEEN 1 AND 5 AND
        score_passion      BETWEEN 1 AND 5
    ),
    -- 1과목 1유저 1회
    UNIQUE KEY uq_ce_once (course_id, user_id),
    -- FK
    CONSTRAINT fk_ce_course FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
    CONSTRAINT fk_ce_user   FOREIGN KEY (user_id)   REFERENCES users(user_id)   ON DELETE SET NULL
) ENGINE=InnoDB;

-- 평가자별 조회
CREATE INDEX idx_ce_user            ON course_evaluations (user_id);
-- 과목별 최신순
CREATE INDEX idx_ce_course_created  ON course_evaluations (course_id, created_at);

-- 과목별 평가 요약 뷰
CREATE OR REPLACE VIEW v_course_eval_avg AS
SELECT
    course_id,
    AVG(score_preparedness) AS avg_prepared,
    AVG(score_clarity)      AS avg_clarity,
    AVG(score_fairness)     AS avg_fairness,
    AVG(score_respond)      AS avg_respond,
    AVG(score_engagement)   AS avg_engagement,
    AVG(score_passion)      AS avg_passion,
    COUNT(*)                AS eval_count
FROM course_evaluations
GROUP BY course_id;

-- 강사별 과목 평가 요약 뷰
CREATE OR REPLACE VIEW v_instructor_eval_avg AS
SELECT
    c.instructor_id,
    c.course_id,
    c.name AS course_name,
    AVG(e.score_preparedness) AS avg_prepared,
    AVG(e.score_clarity)      AS avg_clarity,
    AVG(e.score_fairness)     AS avg_fairness,
    AVG(e.score_respond)      AS avg_respond,
    AVG(e.score_engagement)   AS avg_engagement,
    AVG(e.score_passion)      AS avg_passion,
    COUNT(e.evaluation_id)    AS eval_count
FROM courses c
LEFT JOIN course_evaluations e ON e.course_id = c.course_id
GROUP BY c.instructor_id, c.course_id, c.name;

-- 과제
CREATE TABLE assignments (
    -- 내부 식별자(PK)
    assignment_id    INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    -- 과목(FK, 선택)
    course_id        INT UNSIGNED NULL,
    -- 대상 기수(선택)
    cohort_no        INT NULL,
    -- 반(A/B, 선택)
    class_section    ENUM('A','B') NULL,
    -- 과제명
    name             VARCHAR(150) NOT NULL,
    -- 파일명 규칙(예: SCIT_47_B_홍길동_YYYYMMDD)
    filename_pattern VARCHAR(200) NULL,
    -- 마감 시각(선택)
    due_at           DATETIME NULL,
    -- 생성자(주로 강사/관리자)
    created_by       INT UNSIGNED NOT NULL,
    -- 생성 시각
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- FK
    CONSTRAINT fk_asn_course  FOREIGN KEY (course_id)  REFERENCES courses(course_id) ON DELETE SET NULL,
    CONSTRAINT fk_asn_creator FOREIGN KEY (created_by) REFERENCES users(user_id)    ON DELETE RESTRICT
) ENGINE=InnoDB;

-- 생성자별 조회
CREATE INDEX idx_asn_creator   ON assignments (created_by);
-- 기수/반/마감 정렬
CREATE INDEX idx_asn_scope_due ON assignments (cohort_no, class_section, due_at);

-- 과제 제출(재제출 시 덮어쓰기)
CREATE TABLE assignment_submissions (
    -- 내부 식별자(PK)
    submission_id     INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    -- 과제(FK)
    assignment_id     INT UNSIGNED NOT NULL,
    -- 제출자(FK, 삭제 RESTRICT → 고스트 치환)
    user_id           INT UNSIGNED NULL,
    -- 업로드 당시 원본 파일명
    original_filename VARCHAR(255) NOT NULL,
    -- 저장 파일명(서버/스토리지상의 유니크명)
    stored_filename   VARCHAR(255) NOT NULL,
    -- 저장 URL
    file_url          VARCHAR(1024) NOT NULL,
    -- 업로드 시각
    uploaded_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 갱신 시각(재제출 시)
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    -- 동일 과제+유저 1건만 허용(재제출은 UPDATE)
    UNIQUE KEY uq_submission_once (assignment_id, user_id),
    -- FK
    CONSTRAINT fk_sub_assignment FOREIGN KEY (assignment_id) REFERENCES assignments(assignment_id) ON DELETE CASCADE,
    CONSTRAINT fk_sub_user       FOREIGN KEY (user_id)       REFERENCES users(user_id)            ON DELETE SET NULL
) ENGINE=InnoDB;

-- 제출자별 조회
CREATE INDEX idx_sub_user        ON assignment_submissions (user_id);
-- 과제별 조회
CREATE INDEX idx_sub_assignment  ON assignment_submissions (assignment_id);

-- 사진(앨범 테이블 제거 → album_name으로 그룹핑)
CREATE TABLE photos (
    -- 내부 식별자(PK)
    photo_id     INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    -- 앨범 이름(자유 텍스트, 예: '47기 OT')
    album_name   VARCHAR(150) NOT NULL,
    -- 대상 기수(선택)
    cohort_no    INT NULL,
    -- 업로더(FK, 삭제 RESTRICT → 고스트 치환)
    uploader_id  INT UNSIGNED NOT NULL,
    -- 파일 URL
    file_url     VARCHAR(1024) NOT NULL,
    -- 캡션(짧은 설명)
    caption      VARCHAR(255) NULL,
    -- 업로드 시각
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- FK
    CONSTRAINT fk_ph_uploader FOREIGN KEY (uploader_id) REFERENCES users(user_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- 앨범별 최신순
CREATE INDEX idx_ph_album_created ON photos (album_name, created_at);

-- =============================================================
-- 트리거: 고스트 유저 처리 + Q&A 동기화
-- =============================================================

DELIMITER //

-- 사용자 삭제 전, RESTRICT FK를 고스트로 치환하여 삭제 진행 가능하게 만듦
DROP TRIGGER IF EXISTS trg_users_before_delete//
CREATE TRIGGER trg_users_before_delete
BEFORE DELETE ON users
FOR EACH ROW
BEGIN
    DECLARE v_ghost INT UNSIGNED;

    -- 고스트 계정 자체 삭제 방지
    IF OLD.username = '__deleted_user__' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cannot delete ghost user';
    END IF;

    -- 고스트 user_id 조회(없으면 오류)
    SELECT user_id INTO v_ghost
    FROM users
    WHERE username = '__deleted_user__'
    LIMIT 1;

    IF v_ghost IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Ghost user not found';
    END IF;

    -- 쪽지 발신/수신자 고스트로 교체
    UPDATE direct_messages SET sender_id = v_ghost   WHERE sender_id   = OLD.user_id;
    UPDATE direct_messages SET receiver_id = v_ghost WHERE receiver_id = OLD.user_id;

    -- 게시글/댓글 작성자 고스트로 교체
    UPDATE posts    SET author_id   = v_ghost WHERE author_id   = OLD.user_id;
    UPDATE comments SET author_id   = v_ghost WHERE author_id   = OLD.user_id;

    -- 문의 작성자/답변자 정리
    UPDATE inquiries SET user_id     = v_ghost WHERE user_id     = OLD.user_id;
    UPDATE inquiries SET answered_by = v_ghost WHERE answered_by = OLD.user_id;

    -- 과제/사진 업로더
    UPDATE assignments            SET created_by  = v_ghost WHERE created_by  = OLD.user_id;
    UPDATE photos                 SET uploader_id = v_ghost WHERE uploader_id = OLD.user_id;

    -- ★ UNIQUE 충돌 방지: 익명화만 (NULL)
    UPDATE company_reviews        SET reviewer_id = NULL WHERE reviewer_id = OLD.user_id;
    UPDATE course_evaluations     SET user_id     = NULL WHERE user_id     = OLD.user_id;
    UPDATE assignment_submissions SET user_id     = NULL WHERE user_id     = OLD.user_id;

    -- 좌석 배정: 자연스러운 '자리 비움' 처리
    UPDATE seats SET assigned_user_id = NULL, assigned_at = NULL
    WHERE assigned_user_id = OLD.user_id;

    -- reactions, reservations 등 CASCADE 처리는 별도 교체 불필요
END//

DELIMITER ;

-- =============================================================
-- 끝. (ENGINE=InnoDB / FK 정합성 / 핵심 인덱스 포함)
-- =============================================================

-- ========= 보조 인덱스 =========
-- 이벤트: 공통 일정 스코프 + 시간대 조회 최적화
CREATE INDEX idx_events_scope_time ON events (cohort_no, it_class_scope, start_at);
-- 첨부파일 조회 최적화
CREATE INDEX idx_attachments_ref ON attachments (ref_type, ref_id);

-- 게시판 즐겨찾기 뷰 (reactions로 구현)
CREATE OR REPLACE VIEW v_board_favorites AS
SELECT user_id, ref_id AS board_id, created_at
FROM reactions
WHERE ref_type='BOARD' AND type='BOOKMARK';

-- ========= 폴리모픽 객체(POST/PHOTO/REVIEW/RESERVATION/BOARD) 삭제 시 정리 트리거 =========
DELIMITER //

-- 게시글 삭제 → 첨부/댓글/리액션 정리
DROP TRIGGER IF EXISTS trg_posts_ad_cascade//
CREATE TRIGGER trg_posts_ad_cascade
AFTER DELETE ON posts
FOR EACH ROW
BEGIN
    DELETE FROM attachments WHERE ref_type='POST' AND ref_id=OLD.post_id;
    DELETE FROM comments    WHERE ref_type='POST' AND ref_id=OLD.post_id;
    DELETE FROM reactions   WHERE ref_type='POST' AND ref_id=OLD.post_id;
END//

-- 사진 삭제 → 첨부/댓글/리액션 정리
DROP TRIGGER IF EXISTS trg_photos_ad_cascade//
CREATE TRIGGER trg_photos_ad_cascade
AFTER DELETE ON photos
FOR EACH ROW
BEGIN
    DELETE FROM attachments WHERE ref_type='PHOTO' AND ref_id=OLD.photo_id;
    DELETE FROM comments    WHERE ref_type='PHOTO' AND ref_id=OLD.photo_id;
    DELETE FROM reactions   WHERE ref_type='PHOTO' AND ref_id=OLD.photo_id;
END//

-- 회사 리뷰 삭제 → 댓글/리액션 정리
DROP TRIGGER IF EXISTS trg_company_reviews_ad_cascade//
CREATE TRIGGER trg_company_reviews_ad_cascade
AFTER DELETE ON company_reviews
FOR EACH ROW
BEGIN
    DELETE FROM comments  WHERE ref_type='REVIEW' AND ref_id=OLD.review_id;
    DELETE FROM reactions WHERE ref_type='REVIEW' AND ref_id=OLD.review_id;
END//

-- 예약 삭제 → 리액션(신고) 정리
DROP TRIGGER IF EXISTS trg_reservations_ad_cascade//
CREATE TRIGGER trg_reservations_ad_cascade
AFTER DELETE ON reservations
FOR EACH ROW
BEGIN
    DELETE FROM reactions WHERE ref_type='RESERVATION' AND ref_id=OLD.reservation_id;
END//

-- 게시판 삭제 → 즐겨찾기(BOARD BOOKMARK) 정리
DROP TRIGGER IF EXISTS trg_boards_ad_cascade//
CREATE TRIGGER trg_boards_ad_cascade
AFTER DELETE ON boards
FOR EACH ROW
BEGIN
    DELETE FROM reactions WHERE ref_type='BOARD' AND ref_id=OLD.board_id;
END//

-- ========= 예약 신고(3회→1주 차단) 동기화 트리거 =========
-- 규칙:
-- - 같은 '예약'에 대해 'CONFIRMED'가 1개 이상이어도 strike는 "1건"만 카운트
-- - PENDING→CONFIRMED 전환 시: 현재 CONFIRMED 수가 1개(=이번이 첫 확정)일 때만 +1
-- - CONFIRMED→(기타) 전환/삭제 시: 해당 예약의 CONFIRMED 수가 0이 되면 -1
-- - 누적 3회 이상인 순간 banned_until = NOW()+7 DAY (이미 금지 중이면 더 멀면 그대로 유지)

-- 리액션 등록 시, 예약 신고(3회→1주 차단) 동기화
DROP TRIGGER IF EXISTS trg_react_ai_reservation_report//
CREATE TRIGGER trg_react_ai_reservation_report
AFTER INSERT ON reactions
FOR EACH ROW
BEGIN
    DECLARE v_target_user INT UNSIGNED;
    DECLARE v_cnt INT DEFAULT 0;

    IF NEW.ref_type='RESERVATION' AND NEW.type='REPORT' AND NEW.status='CONFIRMED' THEN
        SELECT user_id INTO v_target_user FROM reservations
        WHERE reservation_id=NEW.ref_id LIMIT 1;

        IF v_target_user IS NOT NULL THEN
        SELECT COUNT(*) INTO v_cnt
            FROM reactions
        WHERE ref_type='RESERVATION' AND type='REPORT'
            AND ref_id=NEW.ref_id AND status='CONFIRMED';
        IF v_cnt=1 THEN
            UPDATE users
            SET resv_strike_count = resv_strike_count + 1,
                resv_banned_until = CASE
                    WHEN resv_strike_count + 1 >= 3
                    THEN GREATEST(IFNULL(resv_banned_until,'1970-01-01'),
                                    NOW() + INTERVAL 7 DAY)
                    ELSE resv_banned_until
                END
            WHERE user_id=v_target_user;
        END IF;
        END IF;
    END IF;
END//

-- 리액션 수정 시, 예약 신고(3회→1주 차단) 동기화
DROP TRIGGER IF EXISTS trg_react_au_reservation_report//
CREATE TRIGGER trg_react_au_reservation_report
AFTER UPDATE ON reactions
FOR EACH ROW
BEGIN
    DECLARE v_target_user INT UNSIGNED;
    DECLARE v_cnt INT DEFAULT 0;

    IF NEW.ref_type='RESERVATION' AND NEW.type='REPORT' THEN
        -- 대상 예약자 조회
        SELECT user_id INTO v_target_user
        FROM reservations WHERE reservation_id = NEW.ref_id LIMIT 1;

        -- PENDING/REJECTED → CONFIRMED : 첫 확정이면 +1
        IF IFNULL(OLD.status,'PENDING') <> 'CONFIRMED'
        AND NEW.status='CONFIRMED'
        AND v_target_user IS NOT NULL THEN
        SELECT COUNT(*) INTO v_cnt
            FROM reactions
        WHERE ref_type='RESERVATION' AND type='REPORT'
            AND ref_id=NEW.ref_id AND status='CONFIRMED';
        IF v_cnt = 1 THEN
            UPDATE users
            SET resv_strike_count = resv_strike_count + 1,
                resv_banned_until = CASE
                    WHEN resv_strike_count + 1 >= 3
                        THEN GREATEST(IFNULL(resv_banned_until, '1970-01-01'),
                                        NOW() + INTERVAL 7 DAY)
                    ELSE resv_banned_until
                END
            WHERE user_id = v_target_user;
        END IF;
        END IF;

        -- CONFIRMED → PENDING/REJECTED : 더 이상 확정 없으면 -1
        IF OLD.status='CONFIRMED'
        AND IFNULL(NEW.status,'PENDING') <> 'CONFIRMED'
        AND v_target_user IS NOT NULL THEN
        SELECT COUNT(*) INTO v_cnt
            FROM reactions
        WHERE ref_type='RESERVATION' AND type='REPORT'
            AND ref_id=NEW.ref_id AND status='CONFIRMED';
        IF v_cnt = 0 THEN
            UPDATE users
            SET resv_strike_count = GREATEST(resv_strike_count - 1, 0)
            WHERE user_id = v_target_user;
        END IF;
        END IF;
    END IF;
END//

-- 리액션 삭제 → 예약 신고(3회→1주 차단) 정리
DROP TRIGGER IF EXISTS trg_react_ad_reservation_report//
CREATE TRIGGER trg_react_ad_reservation_report
AFTER DELETE ON reactions
FOR EACH ROW
BEGIN
    DECLARE v_target_user INT UNSIGNED;
    DECLARE v_cnt INT DEFAULT 0;

    IF OLD.ref_type='RESERVATION' AND OLD.type='REPORT' AND OLD.status='CONFIRMED' THEN
        SELECT user_id INTO v_target_user
        FROM reservations WHERE reservation_id = OLD.ref_id LIMIT 1;

        IF v_target_user IS NOT NULL THEN
        SELECT COUNT(*) INTO v_cnt
            FROM reactions
        WHERE ref_type='RESERVATION' AND type='REPORT'
            AND ref_id=OLD.ref_id AND status='CONFIRMED';
        IF v_cnt = 0 THEN
            UPDATE users
            SET resv_strike_count = GREATEST(resv_strike_count - 1, 0)
            WHERE user_id = v_target_user;
        END IF;
        END IF;
    END IF;
END//

-- ========= Q&A 상태 동기화(해당 게시판이 Q&A일 때만) =========
-- 댓글 등록 시, Q&A 상태 동기화
DROP TRIGGER IF EXISTS trg_cmt_ai_qna//
CREATE TRIGGER trg_cmt_ai_qna
AFTER INSERT ON comments
FOR EACH ROW
BEGIN
    IF NEW.ref_type='POST' AND NEW.is_answer=1 AND EXISTS (
        SELECT 1 FROM posts p JOIN boards b ON b.board_id=p.board_id
        WHERE p.post_id=NEW.ref_id AND b.is_qna=1
    ) THEN
        UPDATE posts SET answer_status='ANSWERED' WHERE post_id=NEW.ref_id;
    END IF;
END//

-- 댓글 수정 시, Q&A 상태 동기화
DROP TRIGGER IF EXISTS trg_cmt_au_qna//
CREATE TRIGGER trg_cmt_au_qna
AFTER UPDATE ON comments
FOR EACH ROW
BEGIN
    DECLARE v_cnt INT DEFAULT 0;

    IF NEW.ref_type='POST' AND EXISTS (
        SELECT 1 FROM posts p JOIN boards b ON b.board_id=p.board_id
        WHERE p.post_id=NEW.ref_id AND b.is_qna=1
    ) THEN
        IF NEW.is_answer=1 AND IFNULL(OLD.is_answer,0)=0 THEN
        UPDATE posts SET answer_status='ANSWERED' WHERE post_id=NEW.ref_id;
        END IF;

        IF OLD.is_answer=1 AND IFNULL(NEW.is_answer,0)=0 THEN
        SELECT COUNT(*) INTO v_cnt
            FROM comments
        WHERE ref_type='POST' AND ref_id=NEW.ref_id AND is_answer=1;
        IF v_cnt=0 THEN
            UPDATE posts SET answer_status='PENDING' WHERE post_id=NEW.ref_id;
        END IF;
        END IF;
    END IF;
END//

-- 댓글 삭제 시, Q&A 상태 동기화
DROP TRIGGER IF EXISTS trg_cmt_ad_qna//
CREATE TRIGGER trg_cmt_ad_qna
AFTER DELETE ON comments
FOR EACH ROW
BEGIN
    DECLARE v_cnt INT DEFAULT 0;

    IF OLD.ref_type='POST' AND OLD.is_answer=1 AND EXISTS (
        SELECT 1 FROM posts p JOIN boards b ON b.board_id=p.board_id
        WHERE p.post_id=OLD.ref_id AND b.is_qna=1
    ) THEN
        SELECT COUNT(*) INTO v_cnt
        FROM comments
        WHERE ref_type='POST' AND ref_id=OLD.ref_id AND is_answer=1;
        IF v_cnt=0 THEN
        UPDATE posts SET answer_status='PENDING' WHERE post_id=OLD.ref_id;
        END IF;
    END IF;
END//

-- ========= 좌석 배정 ↔ 사용자(기수/반) 정합성 =========
-- 좌석 배정 시, 사용자(기수/반) 정합성 검증
DROP TRIGGER IF EXISTS trg_seats_bi_validate//
CREATE TRIGGER trg_seats_bi_validate
BEFORE INSERT ON seats
FOR EACH ROW
BEGIN
    DECLARE v_cohort INT; DECLARE v_class CHAR(1);
    IF NEW.assigned_user_id IS NOT NULL THEN
        SELECT cohort_no, it_class INTO v_cohort, v_class
            FROM users WHERE user_id=NEW.assigned_user_id;
        IF v_cohort IS NULL OR v_class IS NULL
            OR v_cohort <> NEW.cohort_no OR v_class <> NEW.class_section THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Seat assignee must match seat cohort/class';
        END IF;
    END IF;
END//

-- 좌석 변경 시, 사용자(기수/반) 정합성 검증
DROP TRIGGER IF EXISTS trg_seats_bu_validate//
CREATE TRIGGER trg_seats_bu_validate
BEFORE UPDATE ON seats
FOR EACH ROW
BEGIN
    DECLARE v_cohort INT; DECLARE v_class CHAR(1);
    IF NEW.assigned_user_id IS NOT NULL THEN
        SELECT cohort_no, it_class INTO v_cohort, v_class
            FROM users WHERE user_id=NEW.assigned_user_id;
        IF v_cohort IS NULL OR v_class IS NULL
            OR v_cohort <> NEW.cohort_no OR v_class <> NEW.class_section THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Seat assignee must match seat cohort/class';
        END IF;
    END IF;
END//

-- 사용자의 기수/반 변경 시, 맞지 않는 좌석 자동 해제
DROP TRIGGER IF EXISTS trg_users_au_seat_unassign//
CREATE TRIGGER trg_users_au_seat_unassign
AFTER UPDATE ON users
FOR EACH ROW
BEGIN
    IF (OLD.cohort_no <> NEW.cohort_no) OR (OLD.it_class <> NEW.it_class) THEN
        UPDATE seats
        SET assigned_user_id = NULL, assigned_at = NULL
        WHERE assigned_user_id = NEW.user_id
        AND (cohort_no <> NEW.cohort_no OR class_section <> NEW.it_class);
    END IF;
END//

-- ========= 사용자 ↔ 소속 조 정합성 검증 =========
-- 사용자 등록 시, 소속 그룹 정합성 검증
DROP TRIGGER IF EXISTS trg_users_bi_group_validate//
CREATE TRIGGER trg_users_bi_group_validate
BEFORE INSERT ON users
FOR EACH ROW
BEGIN
    IF NEW.group_id IS NOT NULL THEN
        IF NOT EXISTS (
        SELECT 1 FROM study_groups g
        WHERE g.group_id=NEW.group_id
            AND g.cohort_no=NEW.cohort_no
            AND g.class_section=NEW.it_class
        ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT='User group must match user cohort/class';
        END IF;
    END IF;
END//

-- 사용자 수정 시, 소속 그룹 정합성 검증
DROP TRIGGER IF EXISTS trg_users_bu_group_validate//
CREATE TRIGGER trg_users_bu_group_validate
BEFORE UPDATE ON users
FOR EACH ROW
BEGIN
    IF NEW.group_id IS NOT NULL THEN
        IF NOT EXISTS (
        SELECT 1 FROM study_groups g
        WHERE g.group_id=NEW.group_id
            AND g.cohort_no=NEW.cohort_no
            AND g.class_section=NEW.it_class
        ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT='User group must match user cohort/class';
        END IF;
    END IF;
END//

DELIMITER ;

-- 권장 보조 인덱스(집계/조회용)
CREATE INDEX idx_react_ref  ON reactions (ref_type, ref_id, type);
CREATE INDEX idx_react_user ON reactions (user_id, created_at);
CREATE INDEX idx_react_reservation ON reactions (ref_type, type, ref_id, status);

DELIMITER //

-- INSERT 시 대상 존재/타입-상태 일관성 검증
DROP TRIGGER IF EXISTS trg_react_bi_validate//
CREATE TRIGGER trg_react_bi_validate
BEFORE INSERT ON reactions
FOR EACH ROW
BEGIN
    -- 대상 존재 체크
    IF NEW.ref_type='POST' AND NOT EXISTS (SELECT 1 FROM posts WHERE post_id=NEW.ref_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Reaction target POST not found';
    ELSEIF NEW.ref_type='PHOTO' AND NOT EXISTS (SELECT 1 FROM photos WHERE photo_id=NEW.ref_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Reaction target PHOTO not found';
    ELSEIF NEW.ref_type='REVIEW' AND NOT EXISTS (SELECT 1 FROM company_reviews WHERE review_id=NEW.ref_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Reaction target REVIEW not found';
    ELSEIF NEW.ref_type='RESERVATION' AND NOT EXISTS (SELECT 1 FROM reservations WHERE reservation_id=NEW.ref_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Reaction target RESERVATION not found';
    ELSEIF NEW.ref_type='BOARD' AND NOT EXISTS (SELECT 1 FROM boards WHERE board_id=NEW.ref_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Reaction target BOARD not found';
    END IF;

    -- 타입-상태 일관성: LIKE/BOOKMARK에는 status 금지
    IF NEW.type IN ('LIKE','BOOKMARK') AND NEW.status IS NOT NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='status can be used only with REPORT reactions';
    ELSEIF NEW.type='REPORT' AND NEW.status IS NULL THEN
        SET NEW.status='PENDING';
    END IF;
END//

-- UPDATE 시도에도 동일 검증
DROP TRIGGER IF EXISTS trg_react_bu_validate//
CREATE TRIGGER trg_react_bu_validate
BEFORE UPDATE ON reactions
FOR EACH ROW
BEGIN
    -- ref_type/ref_id가 바뀌는 경우만 재검증(안 바뀌어도 안전하게 검사해도 됨)
    IF NEW.ref_type='POST' AND NOT EXISTS (SELECT 1 FROM posts WHERE post_id=NEW.ref_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Reaction target POST not found';
    ELSEIF NEW.ref_type='PHOTO' AND NOT EXISTS (SELECT 1 FROM photos WHERE photo_id=NEW.ref_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Reaction target PHOTO not found';
    ELSEIF NEW.ref_type='REVIEW' AND NOT EXISTS (SELECT 1 FROM company_reviews WHERE review_id=NEW.ref_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Reaction target REVIEW not found';
    ELSEIF NEW.ref_type='RESERVATION' AND NOT EXISTS (SELECT 1 FROM reservations WHERE reservation_id=NEW.ref_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Reaction target RESERVATION not found';
    ELSEIF NEW.ref_type='BOARD' AND NOT EXISTS (SELECT 1 FROM boards WHERE board_id=NEW.ref_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Reaction target BOARD not found';
    END IF;

    IF NEW.type IN ('LIKE','BOOKMARK') AND NEW.status IS NOT NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='status can be used only with REPORT reactions';
    END IF;
END//

-- 예약 관련 트리거
-- 사용자가 1주 차단 중일 때 예약 불가
DROP TRIGGER IF EXISTS trg_res_bi_enforce_ban//
CREATE TRIGGER trg_res_bi_enforce_ban
BEFORE INSERT ON reservations
FOR EACH ROW
BEGIN
    IF EXISTS (
        SELECT 1 FROM users
        WHERE user_id = NEW.user_id
        AND resv_banned_until IS NOT NULL
        AND resv_banned_until > NOW()
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'User is temporarily banned from making reservations';
    END IF;
END//

-- 과제 제출 마감일 관련 트리거
-- 제출 시 마감일 초과 체크
DROP TRIGGER IF EXISTS trg_sub_bi_due_guard//
CREATE TRIGGER trg_sub_bi_due_guard
BEFORE INSERT ON assignment_submissions
FOR EACH ROW
BEGIN
    DECLARE v_due DATETIME;
    SELECT due_at INTO v_due FROM assignments WHERE assignment_id = NEW.assignment_id;
    IF v_due IS NOT NULL AND NOW() > v_due THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Assignment is past due';
    END IF;
END//

-- 제출 수정 시 마감일 초과 체크
DROP TRIGGER IF EXISTS trg_sub_bu_due_guard//
CREATE TRIGGER trg_sub_bu_due_guard
BEFORE UPDATE ON assignment_submissions
FOR EACH ROW
BEGIN
    DECLARE v_due DATETIME;
    SELECT due_at INTO v_due FROM assignments WHERE assignment_id = NEW.assignment_id;
    IF v_due IS NOT NULL AND NOW() > v_due THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Assignment is past due';
    END IF;
END//

DELIMITER ;