-- =============================================================
-- SCIT Hub — Database Schema v1 (MySQL 8.0+)
-- Charset: utf8mb4 / Collation: utf8mb4_0900_ai_ci
-- Author: ChatGPT (for 세현 / SCIT 47)
-- 목적: 요구사항(U_*, B_*, A_*, M_*, E_*, S_*, R_*, T_*, P_*, C_*) 충족 스키마
--      v1.0 기반 개선: FK-NULL정책 정합화, ngram FULLTEXT, 좌석맵 활성1개,
--      예약 120분 체크, Q&A 동기화 트리거, 실전용 보조 인덱스 등
-- =============================================================

-- -------------------------------------------------------------
-- 0) 기본 설정
-- -------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS scithub
  -- 문자 인코딩 설정
  DEFAULT CHARACTER SET utf8mb4
  -- 문자열 비교 규칙 설정(악센트와 대소문자 구분하지 않음)
  DEFAULT COLLATE utf8mb4_0900_ai_ci;
USE scithub;

-- -------------------------------------------------------------
-- 1) 사용자/권한/인증 (U_001~U_014 등)
-- -------------------------------------------------------------
-- 사용자 기본 정보
CREATE TABLE users (
  -- PK
  user_id           INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 로그인용 아이디(중복 불가)
  username          VARCHAR(32) NOT NULL UNIQUE,
  -- 이메일(중복 불가, 인증 대상)
  email             VARCHAR(255) NOT NULL UNIQUE,
  -- 전화번호(아이디/알림용)
  phone             VARCHAR(20) NULL,
  -- 암호화 비밀번호(BCrypt/Argon2 등)
  password_hash     VARCHAR(255) NOT NULL,
  -- 이름(실명)
  name_kor          VARCHAR(50) NOT NULL,
  -- 생년월일
  birth_date        DATE NULL,
  -- 성별(M/F/기타/무응답)
  gender            ENUM('M','F','O','N') NOT NULL DEFAULT 'N',
  -- 기수(예: 47)
  cohort_no         INT NULL,
  -- IT 반(A/B)
  it_class          ENUM('A','B') NULL,
  -- IT 오전/오후(AM/PM)
  it_session        ENUM('AM','PM') NULL,
  -- 일본어 반(자유 텍스트)
  jp_class          ENUM('A','B','C','D','E','F') NULL,
  -- 프로필 이미지 URL
  avatar_url        VARCHAR(500) NULL,
  -- 계정 활성화 여부
  is_active         TINYINT NOT NULL DEFAULT 1,
  -- 관리자 권한 여부(간편 플래그; 확장 롤은 별도 테이블)
  is_admin          TINYINT NOT NULL DEFAULT 0,
  -- 최근 로그인 시각
  last_login_at     DATETIME NULL,
  -- 가입 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- 수정 시각
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  -- 사용자명 길이 제한 체크(예: 4~32)
  CONSTRAINT chk_users_username CHECK (CHAR_LENGTH(username) BETWEEN 4 AND 32)
);

-- 기수/반 조회 최적화 인덱스
CREATE INDEX idx_users_cohort_class ON users (cohort_no, it_class);

-- 탈퇴한 회원을 나타내는 고스트 유저 생성
INSERT INTO users (
  username, email, phone, password_hash, name_kor, birth_date, gender,
  cohort_no, it_class, it_session, jp_class, avatar_url,
  is_active, is_admin, last_login_at
) VALUES (
  '__deleted_user__',
  '__deleted_user__@local.invalid',
  NULL,
  '!',                -- 실제 로그인 불가한 더미 값
  '탈퇴한 회원',
  NULL, 'N',
  NULL, NULL, NULL, NULL, NULL,
  0, 0, NULL
);


-- 쪽지함 [U_014]
CREATE TABLE direct_messages (
  -- PK
  message_id        INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 발신자
  sender_id         INT UNSIGNED NOT NULL,
  -- 수신자
  receiver_id       INT UNSIGNED NOT NULL,
  -- 제목(선택)
  subject           VARCHAR(200) NULL,
  -- 본문
  body              MEDIUMTEXT NOT NULL,
  -- 수신자 열람 여부
  is_read           TINYINT NOT NULL DEFAULT 0,
  -- 열람 시각
  read_at           DATETIME NULL,
  -- 발송 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- 발신자/수신자 측 삭제 플래그(소프트 삭제)
  deleted_by_sender  TINYINT NOT NULL DEFAULT 0,
  deleted_by_receiver TINYINT NOT NULL DEFAULT 0,
  -- FK
  CONSTRAINT fk_dm_sender  FOREIGN KEY (sender_id)  REFERENCES users(user_id) ON DELETE RESTRICT,
  CONSTRAINT fk_dm_receiver FOREIGN KEY (receiver_id) REFERENCES users(user_id) ON DELETE RESTRICT
);

-- 수신자 미읽음 정렬 인덱스 / 발신함 인덱스
CREATE INDEX idx_dm_receiver_read  ON direct_messages (receiver_id, is_read, created_at DESC);
CREATE INDEX idx_dm_sender_created ON direct_messages (sender_id, created_at);
-- direct_messages에 보조 인덱스 (대량 업데이트 성능에 중요)
CREATE INDEX idx_dm_sender_id   ON direct_messages (sender_id);
CREATE INDEX idx_dm_receiver_id ON direct_messages (receiver_id);

-- -------------------------------------------------------------
-- 2) 게시판/검색/신고/북마크
-- -------------------------------------------------------------
-- 게시판 마스터 (예: 공지-운영/IT/일본어, 자유, Q&A 등)
CREATE TABLE boards (
  board_id          INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 프로그램에서 쓰는 키(예: NOTICE_OPS, QNA)
  board_key         VARCHAR(50) NOT NULL UNIQUE,
  -- 표시 이름
  name              VARCHAR(100) NOT NULL,
  -- 설명(선택)
  description       VARCHAR(255) NULL,
  -- Q&A 성격 여부
  is_qna            TINYINT NOT NULL DEFAULT 0,
  -- 공지 전용 여부
  is_notice         TINYINT NOT NULL DEFAULT 0,
  -- 공개 여부(비로그인 열람 허용 등 정책)
  is_public         TINYINT NOT NULL DEFAULT 1,
  -- 생성 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 즐겨찾는 게시판 [B_003]
CREATE TABLE board_favorites (
  -- 사용자
  user_id           INT UNSIGNED NOT NULL,
  -- 게시판
  board_id          INT UNSIGNED NOT NULL,
  -- 등록 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, board_id),
  -- FK
  -- 게시판이나 유저가 삭제될 경우 즐겨찾기 정보도 같이 삭제
  CONSTRAINT fk_bf_user  FOREIGN KEY (user_id)  REFERENCES users(user_id)  ON DELETE CASCADE,
  CONSTRAINT fk_bf_board FOREIGN KEY (board_id) REFERENCES boards(board_id) ON DELETE CASCADE
);

-- 게시글
CREATE TABLE posts (
  post_id           INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 게시판
  board_id          INT UNSIGNED NOT NULL,
  -- 작성자
  author_id         INT UNSIGNED NOT NULL,
  -- 제목
  title             VARCHAR(200) NOT NULL,
  -- 본문
  content           MEDIUMTEXT NOT NULL,
  -- 상태(활성/삭제/차단)
  status            ENUM('ACTIVE','DELETED','BLOCKED') NOT NULL DEFAULT 'ACTIVE',
  -- 조회수
  view_count        INT UNSIGNED NOT NULL DEFAULT 0,
  -- 생성/수정 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  -- FK
  CONSTRAINT fk_posts_board  FOREIGN KEY (board_id)  REFERENCES boards(board_id) ON DELETE CASCADE,
  CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users(user_id)  ON DELETE RESTRICT
);

-- author_id 갱신 시를 위한 보조 인덱스
CREATE INDEX idx_posts_author_id   ON posts (author_id);

-- CJK 품질 개선: ngram 파서 FULLTEXT
-- 한국어, 중국어, 일본어 검색 성능 개선을 위한 인덱스
CREATE FULLTEXT INDEX ftx_posts_title_content
ON posts (title, content) WITH PARSER ngram;

-- 전체/게시판별 정렬 인덱스
CREATE INDEX idx_posts_board_created   ON posts (board_id, created_at DESC);
CREATE INDEX idx_posts_author_created  ON posts (author_id, created_at);

-- Q&A 게시글의 응답 여부
CREATE TABLE qna_posts (
  qna_posts_id      INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  post_id           INT UNSIGNED NOT NULL,
  -- Q&A 답변 상태
  answer_status     ENUM('PENDING','ANSWERED') NOT NULL DEFAULT 'PENDING',
  -- FK
  CONSTRAINT fk_qna_posts_posts FOREIGN KEY (qna_posts_id) REFERENCES posts(post_id) ON DELETE CASCADE 
);

-- 태그 모음
CREATE TABLE tags (
  tag_id            INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  name              VARCHAR(50) NOT NULL UNIQUE
);

-- 게시글-태그 매핑
CREATE TABLE post_tags (
  post_id           INT UNSIGNED NOT NULL,
  tag_id            INT UNSIGNED NOT NULL,
  PRIMARY KEY (post_id, tag_id),
  CONSTRAINT fk_pt_post FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE,
  CONSTRAINT fk_pt_tag  FOREIGN KEY (tag_id)  REFERENCES tags(tag_id)  ON DELETE CASCADE
);

-- 댓글
CREATE TABLE comments (
  comment_id        INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 대상 게시글
  post_id           INT UNSIGNED NOT NULL,
  -- 작성자
  author_id         INT UNSIGNED NOT NULL,
  -- 부모 댓글(대댓글)
  parent_id         INT UNSIGNED NULL,
  -- 내용
  content           MEDIUMTEXT NOT NULL,
  -- Q&A 답변 표시 여부
  is_answer         TINYINT NOT NULL DEFAULT 0,
  -- 생성/수정 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  -- FK
  CONSTRAINT fk_comments_post   FOREIGN KEY (post_id)  REFERENCES posts(post_id)     ON DELETE CASCADE,
  CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users(user_id)    ON DELETE RESTRICT,
  CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id) REFERENCES comments(comment_id) ON DELETE CASCADE
);

-- 댓글 원글별/작성자별 정렬 인덱스
CREATE INDEX idx_comments_post_created   ON comments (post_id, created_at);
CREATE INDEX idx_comments_author_created ON comments (author_id, created_at);

-- author_id 갱신 시를 위한 보조 인덱스
CREATE INDEX idx_comments_author_id   ON comments (author_id);

-- 좋아요(1인 1회)
CREATE TABLE post_likes (
  post_like_id      INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  post_id           INT UNSIGNED NOT NULL,
  user_id           INT UNSIGNED NULL,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY (post_id, user_id),
  CONSTRAINT fk_pl_post FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE,
  CONSTRAINT fk_pl_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
);

-- 북마크
CREATE TABLE post_bookmarks (
  post_id           INT UNSIGNED NOT NULL,
  user_id           INT UNSIGNED NOT NULL,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (post_id, user_id),
  CONSTRAINT fk_pb_post FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE,
  CONSTRAINT fk_pb_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 첨부파일
CREATE TABLE post_attachments (
  attachment_id     INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 게시글
  post_id           INT UNSIGNED NOT NULL,
  -- 저장 경로/URL
  file_url          VARCHAR(1024) NOT NULL,
  -- 원본 파일명
  file_name         VARCHAR(255) NOT NULL,
  -- 파일 크기(byte)
  file_size_bytes   INT UNSIGNED NULL,
  -- 업로드 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- FK
  CONSTRAINT fk_att_post FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE
);

-- 게시글 신고
CREATE TABLE post_reports (
  report_id         INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 대상 게시글
  post_id           INT UNSIGNED NOT NULL,
  -- 신고자
  reporter_id       INT UNSIGNED NULL,
  -- 사유(선택)
  reason            VARCHAR(255) NULL,
  -- 상태(PENDING/CONFIRMED/REJECTED)
  status            ENUM('PENDING','CONFIRMED','REJECTED') NOT NULL DEFAULT 'PENDING',
  -- 생성 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- 동일 사용자의 중복 신고 방지
  CONSTRAINT uq_report_once UNIQUE (post_id, reporter_id),
  -- FK
  CONSTRAINT fk_rep_post FOREIGN KEY (post_id)     REFERENCES posts(post_id)  ON DELETE CASCADE,
  CONSTRAINT fk_rep_user FOREIGN KEY (reporter_id) REFERENCES users(user_id)  ON DELETE CASCADE
);

-- 신고 상태별 정렬 인덱스
CREATE INDEX idx_post_reports_status_created ON post_reports (status, created_at);

-- -------------------------------------------------------------
-- 3) 알림/토스트 (P_001~P_010)
-- -------------------------------------------------------------
CREATE TABLE notifications (
  notification_id   INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 수신자
  user_id           INT UNSIGNED NOT NULL,
  -- 유형(공지/댓글/쪽지/조배정/일정/예약/시스템)
  type              ENUM('NOTICE','COMMENT','MESSAGE','GROUP_ASSIGN','SCHEDULE','RESERVATION','SYSTEM') NOT NULL,
  -- 제목
  title             VARCHAR(150) NOT NULL,
  -- 본문(선택)
  body              VARCHAR(500) NULL,
  -- 클릭 이동 경로(URL)
  target_url        VARCHAR(500) NULL,
  -- 참조 엔티티 타입/ID (예: POST,EVENT)
  -- 어떤 컨텐츠에 대한 알림인지를 나타냄
  ref_type          VARCHAR(50) NULL,
  ref_id            INT UNSIGNED NULL,
  -- 읽음 여부/시각
  is_read           TINYINT NOT NULL DEFAULT 0,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  read_at           DATETIME NULL,
  -- FK
  CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 유저별, 기독 여부, 최신 순 정렬 인덱스
CREATE INDEX idx_notifications_user_latest ON notifications (user_id, is_read, created_at DESC);

-- -------------------------------------------------------------
-- 4) 캘린더/일정/디데이/문의 (C_*, M_006~M_009)
-- -------------------------------------------------------------
CREATE TABLE events (
  event_id          INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 공통/개인 일정 구분
  visibility        ENUM('GLOBAL','PERSONAL') NOT NULL DEFAULT 'GLOBAL',
  -- 개인 일정일 경우, 해당자
  owner_user_id     INT UNSIGNED NULL,
  -- 대상 기수(공통 일정 필터)
  cohort_no         INT NULL,
  -- 대상 반 범위(ALL/A/B - 전체, 오전, 오후)
  it_class_scope    ENUM('ALL','A','B') NOT NULL DEFAULT 'ALL',
  -- 제목/설명
  title             VARCHAR(150) NOT NULL,
  description       MEDIUMTEXT NULL,
  -- 시작/종료 시각
  start_at          DATETIME NOT NULL,
  end_at            DATETIME NOT NULL,
  -- 종일 여부
  is_all_day        TINYINT NOT NULL DEFAULT 0,
  -- 디데이 표시 여부
  dday_enabled      TINYINT NOT NULL DEFAULT 0,
  -- 알림 작성자/시각
  created_by        INT UNSIGNED NULL,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  -- 시간 일관성 체크
  CONSTRAINT chk_events_time CHECK (end_at > start_at),
  -- FK
  CONSTRAINT fk_events_owner   FOREIGN KEY (owner_user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_events_creator FOREIGN KEY (created_by)    REFERENCES users(user_id) ON DELETE SET NULL
);

-- 알림 개인/단체 여부, 일정 해당자별 인덱스
CREATE INDEX idx_events_visibility_time ON events (visibility, start_at, end_at);
CREATE INDEX idx_events_owner_time      ON events (owner_user_id, start_at);

-- 문의/답변 (좌석 피드백 포함) [M_012,M_013,S_004]
CREATE TABLE inquiries (
  inquiry_id        INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 작성자
  user_id           INT UNSIGNED NOT NULL,
  -- 카테고리
  category          ENUM('GENERAL','SEAT','RESERVATION','OTHER') NOT NULL DEFAULT 'GENERAL',
  -- 제목/내용
  subject           VARCHAR(150) NOT NULL,
  content           MEDIUMTEXT NOT NULL,
  -- 좌석 피드백 연계 시 좌석 ID (FK는 seats 생성 후 추가)
  seat_id           INT UNSIGNED NULL,
  -- 상태(OPEN/ANSWERED/CLOSED)
  status            ENUM('OPEN','ANSWERED','CLOSED') NOT NULL DEFAULT 'OPEN',
  -- 생성/수정 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  -- FK (user)
  CONSTRAINT fk_inq_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE RESTRICT
);

-- inquiries의 user_id 갱신 시를 위한 보조 인덱스
CREATE INDEX idx_inquiries_user_id   ON inquiries (user_id);

CREATE TABLE inquiry_replies (
  reply_id          INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 문의 ID
  inquiry_id        INT UNSIGNED NOT NULL,
  -- 응답자 ID
  responder_id      INT UNSIGNED NOT NULL,
  -- 답변 본문
  body              MEDIUMTEXT NOT NULL,
  -- 생성 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- FK
  CONSTRAINT fk_ir_inq  FOREIGN KEY (inquiry_id)  REFERENCES inquiries(inquiry_id) ON DELETE CASCADE,
  CONSTRAINT fk_ir_user FOREIGN KEY (responder_id) REFERENCES users(user_id)       ON DELETE RESTRICT
);

-- 응답자 ID 갱신 시를 위한 보조 인덱스
CREATE INDEX idx_inquiries_replies_responder_id   ON inquiry_replies (responder_id);


-- -------------------------------------------------------------
-- 5) 조편성/좌석 배치 (M_017~M_028, S_001~S_004)
-- -------------------------------------------------------------
-- 조(그룹) 마스터
CREATE TABLE study_groups (
  group_id          INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 기수
  cohort_no         INT NOT NULL,
  -- 반(A/B)
  class_section     ENUM('A','B') NOT NULL,
  -- 조 이름
  name              VARCHAR(50) NOT NULL,
  -- UI 정렬 순서
  order_index       INT NOT NULL DEFAULT 0,
  -- 생성 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- (동일 기수/반 내 조 이름 중복 방지)
  UNIQUE KEY uq_group_name (cohort_no, class_section, name)
);

-- 조 배정 (한 학생은 동일 기수/반에 1개 조만)
CREATE TABLE group_assignments (
  assignment_id     INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 기수/반/조
  cohort_no         INT NOT NULL,
  class_section     ENUM('A','B') NOT NULL,
  group_id          INT UNSIGNED NOT NULL,
  -- 배정된 사용자
  user_id           INT UNSIGNED NOT NULL,
  -- 배정 시각
  assigned_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- (동일 기수/반에서 사용자 중복 배정 방지)
  UNIQUE KEY uq_ga_unique (cohort_no, class_section, user_id),
  KEY idx_ga_group (group_id),
  -- FK
  CONSTRAINT fk_ga_group FOREIGN KEY (group_id) REFERENCES study_groups(group_id) ON DELETE CASCADE,
  CONSTRAINT fk_ga_user  FOREIGN KEY (user_id)  REFERENCES users(user_id)         ON DELETE CASCADE
);

-- 조 배정 사용자별 인덱스
CREATE INDEX idx_ga_user ON group_assignments (user_id);

-- 강의실/자습실 마스터
CREATE TABLE rooms (
  room_id           INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 예: 4F-401, 4F-자습1
  name              VARCHAR(100) NOT NULL,
  -- 유형(CLASSROOM/STUDY/MEETING)
  type              ENUM('CLASSROOM','STUDY','MEETING') NOT NULL DEFAULT 'CLASSROOM',
  -- 층 번호
  floor_no          INT NULL,
  -- 수용 인원
  capacity          INT NULL,
  -- 사용 여부
  is_active         TINYINT NOT NULL DEFAULT 1,
  -- 생성 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 좌석 배치도 버전 (기수/반별)
CREATE TABLE seat_maps (
  seat_map_id       INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 강의실
  room_id           INT UNSIGNED NOT NULL,
  -- 기수/반
  cohort_no         INT NOT NULL,
  class_section     ENUM('A','B') NOT NULL,
  -- 버전 번호/활성 플래그
  version_no        INT NOT NULL DEFAULT 1,
  is_active         TINYINT NOT NULL DEFAULT 1,
  -- 활성 1개 강제용 생성 컬럼
  active_one        TINYINT AS (IF(is_active=1, 1, NULL)) STORED,
  -- 생성 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- 고유성
  UNIQUE KEY uq_active_map (room_id, cohort_no, class_section, version_no),
  UNIQUE KEY uq_sm_active_one (room_id, cohort_no, class_section, active_one),
  -- FK
  CONSTRAINT fk_sm_room FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE CASCADE
);

-- 좌석 정의(배치도 내 좌표/코드)
CREATE TABLE seats (
  seat_id           INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 배치도
  seat_map_id       INT UNSIGNED NOT NULL,
  -- 좌석 코드(예: A-01)
  seat_code         VARCHAR(20) NOT NULL,
  -- 행/열 좌표(선택)
  row_no            INT NULL,
  col_no            INT NULL,
  -- 동일 배치도 내 좌석 코드 중복 방지
  UNIQUE KEY uq_seat_code (seat_map_id, seat_code),
  -- FK
  CONSTRAINT fk_seat_map FOREIGN KEY (seat_map_id) REFERENCES seat_maps(seat_map_id) ON DELETE CASCADE
);

-- 좌석 배정(좌석당 1명, 사용자당 1좌석)
CREATE TABLE seat_assignments (
  seat_assignment_id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 배치도/좌석/사용자
  seat_map_id       INT UNSIGNED NOT NULL,
  seat_id           INT UNSIGNED NOT NULL,
  user_id           INT UNSIGNED NOT NULL,
  -- 배정 시각
  assigned_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- 유일성 제약
  UNIQUE KEY uq_sa_seat (seat_map_id, seat_id),
  UNIQUE KEY uq_sa_user (seat_map_id, user_id),
  -- FK
  CONSTRAINT fk_sa_map  FOREIGN KEY (seat_map_id) REFERENCES seat_maps(seat_map_id) ON DELETE CASCADE,
  CONSTRAINT fk_sa_seat FOREIGN KEY (seat_id)     REFERENCES seats(seat_id)         ON DELETE CASCADE,
  CONSTRAINT fk_sa_user FOREIGN KEY (user_id)     REFERENCES users(user_id)        ON DELETE CASCADE
);

-- 좌석 배정 사용자별 인덱스
CREATE INDEX idx_sa_user ON seat_assignments (user_id);

-- 좌석 피드백 FK 추가(문의 -> 좌석)
ALTER TABLE inquiries
  ADD CONSTRAINT fk_inq_seat FOREIGN KEY (seat_id) REFERENCES seats(seat_id) ON DELETE SET NULL;

-- -------------------------------------------------------------
-- 6) 강의실 예약/신고/패널티 (R_001~R_006)
-- -------------------------------------------------------------
CREATE TABLE reservations (
	-- PK
  reservation_id    INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 강의실/사용자
  room_id           INT UNSIGNED NOT NULL,
	-- 사용자 삭제되었을 시 예약 자동 삭제
  user_id           INT UNSIGNED NOT NULL,
  -- 예약 시작/종료 시각
  start_at          DATETIME NOT NULL,
  end_at            DATETIME NOT NULL,
  -- 상태(ACTIVE/CANCELLED/NO_SHOW/BLOCKED)
  status            ENUM('ACTIVE','CANCELLED','NO_SHOW','BLOCKED') NOT NULL DEFAULT 'ACTIVE',
  -- 생성 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- 시간 일관성 체크
  CONSTRAINT chk_res_time CHECK (end_at > start_at),
  -- 최대 120분 제한
  CONSTRAINT chk_res_duration CHECK (TIMESTAMPDIFF(MINUTE, start_at, end_at) BETWEEN 1 AND 120),
  -- FK
  CONSTRAINT fk_res_room FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE CASCADE,
  CONSTRAINT fk_res_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 강의실/예약 시작/종료 시각 필터링 및 정렬용 인덱스
CREATE INDEX idx_res_room_time ON reservations (room_id, start_at, end_at);

-- 사용자/예약 시작/종료 시각 필터링 및 정렬용 인덱스
CREATE INDEX idx_res_user_time ON reservations (user_id, start_at, end_at);

-- 예약 미사용 신고
CREATE TABLE reservation_reports (
	-- PK
  report_id         INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 대상 예약/신고자 (탈퇴 시 신고 보존)
  reservation_id    INT UNSIGNED NOT NULL,
  reporter_id       INT UNSIGNED NOT NULL,
  -- 사유/상태
  reason            VARCHAR(255) NULL,
  -- 상태 -> PENDING: 검토중, CONFIRMED: 신고 인정, REJECTED: 신고 기각
  status            ENUM('PENDING','CONFIRMED','REJECTED') NOT NULL DEFAULT 'PENDING',
  -- 생성 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- FK
  CONSTRAINT fk_rr_res      FOREIGN KEY (reservation_id) REFERENCES reservations(reservation_id) ON DELETE CASCADE,
  CONSTRAINT fk_rr_reporter FOREIGN KEY (reporter_id)     REFERENCES users(user_id)
);

-- reporter_id 갱신 / 필터링 및 정렬용 인덱스
CREATE INDEX idx_rr_reporter ON reservation_reports (reporter_id);

-- 예약 패널티/차단(누적 3회 등)
CREATE TABLE reservation_penalties (
	-- PK
  penalty_id        INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 대상 사용자
  user_id           INT UNSIGNED NOT NULL,
  -- 누적 신고 횟수
  strike_count      INT NOT NULL DEFAULT 0,
  -- 예약 금지 해제 시각
  banned_until      DATETIME NULL,
  -- 갱신 시각
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  -- 1인 1레코드
  UNIQUE KEY uq_penalty_user (user_id),
  -- FK
  CONSTRAINT fk_penalty_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- -------------------------------------------------------------
-- 7) 졸업생 현황/회사/리뷰
-- -------------------------------------------------------------
CREATE TABLE companies (
	-- PK
  company_id        INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 회사명
  name              VARCHAR(150) NOT NULL UNIQUE,
  -- 로고 URL
  logo_url          VARCHAR(500) NULL,
  -- 계열/산업
  sector            VARCHAR(100) NULL,
  -- 위치(자유 입력)
  location_text     VARCHAR(150) NULL,
  -- 인원(표기용)
  headcount         INT NULL,
  -- 생성 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 회사 리뷰(1인 1회)
CREATE TABLE company_reviews (
	-- PK
  review_id         INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 회사/리뷰어(탈퇴 시 리뷰 보존)
  company_id        INT UNSIGNED NOT NULL,
  reviewer_id       INT UNSIGNED NOT NULL,
  -- 평점 1~5 / 본문
  rating            TINYINT NOT NULL,
  body              MEDIUMTEXT NULL,
  -- 생성 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- 제약/고유성
  CONSTRAINT chk_cr_rating CHECK (rating BETWEEN 1 AND 5),
  UNIQUE KEY uq_review_once (company_id, reviewer_id),
  -- FK
  CONSTRAINT fk_cr_company FOREIGN KEY (company_id) REFERENCES companies(company_id) ON DELETE CASCADE,
  CONSTRAINT fk_cr_user    FOREIGN KEY (reviewer_id) REFERENCES users(user_id)
);

-- reviewer_id 갱신 / 필터링 및 정렬용 인덱스
CREATE INDEX idx_cr_user ON company_reviews (reviewer_id);

-- 회사/생성 시각 필터링 및 정렬용 인덱스
CREATE INDEX idx_cr_company_created ON company_reviews (company_id, created_at);

-- 리뷰 댓글
CREATE TABLE company_review_comments (
  -- PK
  comment_id        INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 리뷰/작성자(탈퇴 시 댓글 보존)
  review_id         INT UNSIGNED NOT NULL,
  author_id         INT UNSIGNED NOT NULL,
  -- 본문/시각
  body              MEDIUMTEXT NOT NULL,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- FK
  CONSTRAINT fk_crc_review FOREIGN KEY (review_id) REFERENCES company_reviews(review_id) ON DELETE CASCADE,
  CONSTRAINT fk_crc_user   FOREIGN KEY (author_id) REFERENCES users(user_id)             ON DELETE RESTRICT
);

-- author_id 갱신 / 필터링 및 정렬용 인덱스
CREATE INDEX idx_crc_user ON company_review_comments (author_id);

-- 평균 평점 뷰(정렬용)
CREATE VIEW v_company_ratings AS
SELECT
  c.company_id,
  c.name,
  c.sector,
  c.location_text,
  c.headcount,
  AVG(r.rating)  AS avg_rating,
  COUNT(r.review_id) AS review_count
FROM companies c
LEFT JOIN company_reviews r ON r.company_id = c.company_id
GROUP BY c.company_id;

-- -------------------------------------------------------------
-- 8) 강의/강의평가 (E_001,E_002)
-- -------------------------------------------------------------
CREATE TABLE courses (
	-- PK
  course_id       INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 과목명
  name            VARCHAR(150) NOT NULL,
  -- 담당 강사
  instructor_id   INT UNSIGNED NULL,
  -- 과목 트랙(IT/JP)
  course_type     ENUM('IT','JP') NOT NULL,
  -- 대상 기수/반(선택)
  cohort_no       INT NULL,
  class_section   ENUM('A','B') NULL,
  -- 생성 시각
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- FK
  CONSTRAINT fk_courses_instructor
    FOREIGN KEY (instructor_id) REFERENCES users(user_id) ON DELETE SET NULL
);

-- 강사/기수/반 필터링 및 정렬용 인덱스
CREATE INDEX idx_courses_instructor
	ON courses (instructor_id, cohort_no, class_section);
-- 기수/반/과목 트랙 필터링 및 정렬용 인덱스
CREATE INDEX idx_courses_scope_type
	ON courses (cohort_no, class_section, course_type);

-- 수강 등록(강의평가 가능 범위 결정)
CREATE TABLE enrollments (
	-- PK
  course_id         INT UNSIGNED NOT NULL,
  user_id           INT UNSIGNED NOT NULL,
  PRIMARY KEY (course_id, user_id),
  CONSTRAINT fk_enr_course FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
  CONSTRAINT fk_enr_user   FOREIGN KEY (user_id)  REFERENCES users(user_id)      ON DELETE CASCADE
);

-- user_id 필터링 및 정렬용 인덱스
CREATE INDEX idx_enr_user ON enrollments (user_id);

-- 강의 평가(6항목 1~5, 1인 1회)
CREATE TABLE course_evaluations (
	-- PK
  evaluation_id     INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 과목/작성자 (탈퇴 시 평가 보존)
  course_id         INT UNSIGNED NOT NULL,
  user_id           INT UNSIGNED NOT NULL,
  -- 항목 점수(1~5)
  score_preparedness TINYINT NOT NULL,
  score_clarity     TINYINT NOT NULL,
  score_fairness    TINYINT NOT NULL,
  score_respond     TINYINT NOT NULL,
  score_engagement  TINYINT NOT NULL,
  score_passion     TINYINT NOT NULL,
  -- 개선 의견(서술)
  comment_text      MEDIUMTEXT NULL,
  -- 작성 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- 점수 범위 체크/1인 1회
  CONSTRAINT chk_ce_scores CHECK (
    score_preparedness BETWEEN 1 AND 5 AND
    score_clarity      BETWEEN 1 AND 5 AND
    score_fairness     BETWEEN 1 AND 5 AND
    score_respond      BETWEEN 1 AND 5 AND
    score_engagement   BETWEEN 1 AND 5 AND
    score_passion      BETWEEN 1 AND 5
  ),
  UNIQUE KEY uq_ce_once (course_id, user_id),
  -- FK
  CONSTRAINT fk_ce_course FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
  CONSTRAINT fk_ce_user   FOREIGN KEY (user_id)  REFERENCES users(user_id)
);

-- user_id 갱신 / 필터링 및 정렬용 인덱스
CREATE INDEX idx_ce_user ON course_evaluations (user_id);

-- 강좌/작성 시각 필터링 및 정렬용 인덱스
CREATE INDEX idx_ce_course_created ON course_evaluations (course_id, created_at);

-- 반별 고정 시간표(템플릿) — 주중/주말 & 세션 타입 저장
-- 주의: ‘과목’이 아닌 고정 블록(IT/JP/LUNCH/REVIEW/SELF_STUDY)을 정의
--       과목(courses)은 course_type(IT/JP)만 알려주고, 실제 시간은 여기서 매핑됨
-- UI/캘린더는 courses.course_type와 class_section을 기반으로
-- 블록(label=IT/JP)**과 매칭해서  "그 반의 해당 과목 시간"을 계산해 표시하면 됨.
CREATE TABLE class_schedule_blocks (
	-- PK
  block_id        INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 대상 기수(선택): 특정 기수별로 바뀌면 값 세팅, 공통이면 NULL
  cohort_no       INT NULL,
  -- 반
  class_section   ENUM('A','B') NOT NULL,
  -- 블록 종류
  label           ENUM('IT','JP','LUNCH','REVIEW','SELF_STUDY') NOT NULL,
  -- 적용 요일(SET): 예 'MON,TUE,WED,THU,FRI'
  days            SET('MON','TUE','WED','THU','FRI','SAT','SUN') NOT NULL,
  -- 시간(고정)
  start_time      TIME NOT NULL,
  end_time        TIME NOT NULL,
  -- 활성화 플래그
  is_active       TINYINT NOT NULL DEFAULT 1,
  -- 생성 시각
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- 시간 일관성 체크
  CONSTRAINT chk_csb_time CHECK (end_time > start_time)
);

-- 대상 기수/ 반/블록 종류/활성화 플래그 필터링 및 정렬용 인덱스
CREATE INDEX idx_csb_scope ON class_schedule_blocks (cohort_no, class_section, label, is_active);

-- 과목별 평가 요약 뷰
CREATE VIEW v_course_eval_avg AS
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
CREATE VIEW v_instructor_eval_avg AS
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

-- -------------------------------------------------------------
-- 9) 과제 제출 (T_001~T_006)
-- -------------------------------------------------------------
CREATE TABLE assignments (
	-- PK
  assignment_id     INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 과목/대상 기수/반(선택)
  course_id         INT UNSIGNED NULL,
  cohort_no         INT NULL,
  class_section     ENUM('A','B') NULL,
  -- 과제명/파일명 규칙/마감시각
  name              VARCHAR(150) NOT NULL,
  filename_pattern  VARCHAR(200) NULL,
  due_at            DATETIME NULL,
  -- 생성자/시각
  created_by        INT UNSIGNED NOT NULL,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- FK
  CONSTRAINT fk_asn_course  FOREIGN KEY (course_id)  REFERENCES courses(course_id) ON DELETE SET NULL,
  CONSTRAINT fk_asn_creator FOREIGN KEY (created_by) REFERENCES users(user_id)
);

-- created_by 갱신 / 필터링 및 정렬용 인덱스
CREATE INDEX idx_asn_creator ON assignments (created_by);

-- 기수/반/마감시각 필터링 및 정렬용 인덱스
CREATE INDEX idx_asn_scope_due ON assignments (cohort_no, class_section, due_at);

-- 과제 제출(재제출 시 갱신)
CREATE TABLE assignment_submissions (
	-- PK
  submission_id     INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 과제/제출자
  assignment_id     INT UNSIGNED NOT NULL,
  user_id           INT UNSIGNED NOT NULL,
  -- 원본 파일명/저장 파일명/URL
  original_filename VARCHAR(255) NOT NULL,
  stored_filename   VARCHAR(255) NOT NULL,
  file_url          VARCHAR(1024) NOT NULL,
  -- 업로드/갱신 시각
  uploaded_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  -- 1인 1건(재제출 시 덮어쓰기)
  UNIQUE KEY uq_submission_once (assignment_id, user_id),
  -- FK
  CONSTRAINT fk_sub_assignment FOREIGN KEY (assignment_id) REFERENCES assignments(assignment_id) ON DELETE CASCADE,
  CONSTRAINT fk_sub_user       FOREIGN KEY (user_id)       REFERENCES users(user_id)
);

-- user_id 갱신 / 필터링 및 정렬용 인덱스
CREATE INDEX idx_sub_user ON assignment_submissions (user_id);

-- 과제 필터링 및 정렬용 인덱스
CREATE INDEX idx_sub_assignment ON assignment_submissions (assignment_id);

-- -------------------------------------------------------------
-- 10) 사진 앨범
-- -------------------------------------------------------------
-- 앨범 테이블
CREATE TABLE albums (
	-- PK
  album_id          INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 대상 기수(선택)
  cohort_no         INT NULL,
  -- 앨범명
  name              VARCHAR(150) NOT NULL,
  -- 생성자/시각
  created_by        INT UNSIGNED NOT NULL,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- FK
  CONSTRAINT fk_album_user FOREIGN KEY (created_by) REFERENCES users(user_id)
);

-- created_by 갱신 / 필터링 및 정렬용 인덱스
CREATE INDEX idx_album_user ON albums (created_by);

-- 사진 테이블
CREATE TABLE photos (
	-- PK
  photo_id          INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 앨범/업로더/파일
  album_id          INT UNSIGNED NOT NULL,
  uploader_id       INT UNSIGNED NOT NULL,
  file_url          VARCHAR(1024) NOT NULL,
  -- 캡션 (사진에 붙일 수 있는 짦은 설명이나 코멘트)
  caption           VARCHAR(255) NULL,
  -- 생성 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- FK
  -- ***앨범의 생성자와 사진의 업로더의 권한 문제로 보류***
  CONSTRAINT fk_ph_album    FOREIGN KEY (album_id)    REFERENCES albums(album_id) ON DELETE CASCADE,

  CONSTRAINT fk_ph_uploader FOREIGN KEY (uploader_id) REFERENCES users(user_id)
);

-- 앨범/생성 시각 필터링 및 정렬용 인덱스
CREATE INDEX idx_ph_album_created ON photos (album_id, created_at);

-- 사진 좋아요(1인 1회)
CREATE TABLE photo_likes (
	-- PK
	photo_like_id     INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  photo_id          INT UNSIGNED NOT NULL,
  user_id           INT UNSIGNED NULL,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY (photo_id, user_id),
  CONSTRAINT fk_pl_photo FOREIGN KEY (photo_id) REFERENCES photos(photo_id) ON DELETE CASCADE,
  CONSTRAINT fk_pl_user2 FOREIGN KEY (user_id)  REFERENCES users(user_id)  ON DELETE SET NULL
);

CREATE TABLE photo_comments (
	-- PK
  comment_id        INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 사진/작성자(탈퇴 시 댓글 보존 → NULL)
  photo_id          INT UNSIGNED NOT NULL,
  user_id           INT UNSIGNED NOT NULL,
  -- 내용/생성 시각
  content           MEDIUMTEXT NOT NULL,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- FK
  CONSTRAINT fk_pc_photo FOREIGN KEY (photo_id) REFERENCES photos(photo_id) ON DELETE CASCADE,
  CONSTRAINT fk_pc_user  FOREIGN KEY (user_id)  REFERENCES users(user_id)
);

-- user_id 갱신 / 필터링 및 정렬용 인덱스
CREATE INDEX idx_pc_user ON photo_comments (user_id);

-- 사진/생성 시각 필터링 및 정렬용 인덱스
CREATE INDEX idx_pc_photo_created ON photo_comments (photo_id, created_at);

-- =============================================================
-- 트리거(이벤트 동기화/정합성 보강)
-- =============================================================

DELIMITER //

-- 유저 삭제 시 어떤 행동을 할지에 관한 트리거
CREATE TRIGGER trg_users_before_delete
BEFORE DELETE ON users
FOR EACH ROW
BEGIN
  DECLARE v_ghost INT UNSIGNED;

  -- 고스트 계정 삭제 시도 차단
  IF OLD.username = '__deleted_user__' THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Cannot delete ghost user';
  END IF;

  -- 고스트 user_id 조회(유니크 username 기반)
  SELECT user_id INTO v_ghost
  FROM users
  WHERE username = '__deleted_user__'
  LIMIT 1;

  IF v_ghost IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Ghost user not found';
  END IF;

  -- 발신자/수신자 FK를 고스트로 업데이트
	-- user 삭제 전 이 user가 수/발신한 쪽지의 수/발신자 id를
	-- 삭제된 유저를 나타내는 고스트 계정 id로 바꿈
    UPDATE direct_messages
        SET sender_id = v_ghost
    WHERE sender_id = OLD.user_id;

    UPDATE direct_messages
        SET receiver_id = v_ghost
    WHERE receiver_id = OLD.user_id;

    -- user 삭제 전 이 user가 작성한 게시글의 작성자 id를 고스트 계정 id로 바꿈
	UPDATE posts
		SET author_id = v_ghost
	WHERE author_id = OLD.user_id;

	-- user 삭제 전 이 user가 작성한 게시글 댓글의 작성자 id를 고스트 계정 id로 바꿈
	UPDATE comments
		SET author_id = v_ghost
	WHERE author_id = OLD.user_id;

    -- user 삭제 전 이 user가 작성한 문의의 작성자 id를 고스트 계정 id로 바꿈
	UPDATE inquiries
		SET user_id = v_ghost
	WHERE user_id = OLD.user_id;

    -- user 삭제 전 이 user가 작성한 답변의 작성자 id를 고스트 계정 id로 바꿈
	UPDATE inquiry_replies
		SET responder_id = v_ghost
	WHERE responder_id = OLD.user_id;

	-- user 삭제 전 이 user가 작성한 강의실 예약 신고의 신고자 id를 고스트 계정 id로 바꿈
	UPDATE reservation_reports
		SET reporter_id = v_ghost
	WHERE reporter_id = OLD.user_id;

	-- user 삭제 전 이 user가 작성한 회사 리뷰의 작성자 id를 고스트 계정 id로 바꿈
	UPDATE company_reviews
		SET reviewer_id = v_ghost
	WHERE reviewer_id = OLD.user_id;

	-- user 삭제 전 이 user가 작성한 회사 리뷰 댓글의 작성자 id를 고스트 계정 id로 바꿈
	UPDATE company_review_comments
		SET author_id = v_ghost
	WHERE author_id = OLD.user_id;

	-- user 삭제 전 이 user가 작성한 강의 평가의 작성자 id를 고스트 계정 id로 바꿈
	UPDATE course_evaluations
		SET user_id = v_ghost
	WHERE user_id = OLD.user_id;

	-- user 삭제 전 이 user가 만든 과제의 생성자 id를 고스트 계정 id로 바꿈
	UPDATE assignments
		SET created_by = v_ghost
	WHERE created_by = OLD.user_id;

	-- user 삭제 전 이 user가 제출한 과제의 제출자 id를 고스트 계정 id로 바꿈
	UPDATE assignment_submissions
		SET user_id = v_ghost
	WHERE user_id = OLD.user_id;

	-- user 삭제 전 이 user가 만든 앨범의 생성자 id를 고스트 계정 id로 바꿈
	UPDATE albums
		SET created_by = v_ghost
	WHERE created_by = OLD.user_id;

	-- user 삭제 전 이 user가 작성한 사진 댓글의 작성자 id를 고스트 계정 id로 바꿈
	UPDATE photo_comments
		SET user_id = v_ghost
	WHERE user_id = OLD.user_id;
END//

-- Q&A 동기화: 정답 댓글 생성 시 → 게시글 ANSWERED
CREATE TRIGGER trg_comments_ai_answer
AFTER INSERT ON comments
FOR EACH ROW
BEGIN
  IF NEW.is_answer = 1 THEN
    UPDATE posts SET answer_status='ANSWERED' WHERE post_id = NEW.post_id;
  END IF;
END//

-- Q&A 동기화: 정답 플래그 변경 시 상태 반영
CREATE TRIGGER trg_comments_au_answer
AFTER UPDATE ON comments
FOR EACH ROW
BEGIN
  DECLARE cnt INT DEFAULT 0;
  IF NEW.is_answer = 1 AND (OLD.is_answer IS NULL OR OLD.is_answer = 0) THEN
    UPDATE posts SET answer_status='ANSWERED' WHERE post_id = NEW.post_id;
  END IF;

  IF OLD.is_answer = 1 AND NEW.is_answer = 0 THEN
    SELECT COUNT(*) INTO cnt
      FROM comments
    WHERE post_id = NEW.post_id AND is_answer = 1;
    IF cnt = 0 THEN
      UPDATE posts SET answer_status='PENDING' WHERE post_id = NEW.post_id;
    END IF;
  END IF;
END//

-- Q&A 동기화: 정답 댓글 삭제 시 마지막 정답인지 확인
CREATE TRIGGER trg_comments_ad_answer
AFTER DELETE ON comments
FOR EACH ROW
BEGIN
  DECLARE cnt INT DEFAULT 0;
  IF OLD.is_answer = 1 THEN
    SELECT COUNT(*) INTO cnt
      FROM comments
    WHERE post_id = OLD.post_id AND is_answer = 1;
    IF cnt = 0 THEN
      UPDATE posts SET answer_status='PENDING' WHERE post_id = OLD.post_id;
    END IF;
  END IF;
END//

DELIMITER ;