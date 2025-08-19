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
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;
USE scithub;

SET NAMES utf8mb4;
SET time_zone = '+09:00';

-- -------------------------------------------------------------
-- 1) 사용자/권한/인증 (U_001~U_014 등)
-- -------------------------------------------------------------
-- 사용자 기본 정보
CREATE TABLE users (
  -- PK
  user_id           BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
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
  jp_class          VARCHAR(32) NULL,
  -- 프로필 이미지 URL
  avatar_url        VARCHAR(500) NULL,
  -- 계정 활성화 여부
  is_active         TINYINT(1) NOT NULL DEFAULT 1,
  -- 관리자 권한 여부(간편 플래그; 확장 롤은 별도 테이블)
  is_admin          TINYINT(1) NOT NULL DEFAULT 0,
  -- 최근 로그인 시각
  last_login_at     DATETIME NULL,
  -- 가입 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- 수정 시각
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  -- 사용자명 길이 제한 체크(예: 4~32)
  CONSTRAINT chk_users_username CHECK (CHAR_LENGTH(username) BETWEEN 4 AND 32)
) ENGINE=InnoDB;

-- 기수/반 조회 최적화 인덱스
CREATE INDEX idx_users_cohort_class ON users (cohort_no, it_class);

-- (옵션) 롤 확장: 관리자 외 향후 MODERATOR/INSTRUCTOR 등
CREATE TABLE roles (
  role_id   BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  name      VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE user_roles (
  user_id   BIGINT UNSIGNED NOT NULL,
  role_id   BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 이메일 인증 코드 (회원가입/변경) [U_008]
CREATE TABLE email_verifications (
  -- PK
  verification_id   BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 대상 사용자
  user_id           BIGINT UNSIGNED NOT NULL,
  -- 인증번호(5자리 숫자)
  code              CHAR(5) NOT NULL,
  -- 용도(SIGNUP/RESET/CHANGE_EMAIL)
  purpose           ENUM('SIGNUP','RESET','CHANGE_EMAIL') NOT NULL DEFAULT 'SIGNUP',
  -- 활성 여부(사용자+purpose 활성 1건만 허용)
  is_active         TINYINT(1) NOT NULL DEFAULT 1,
  -- 만료 시각
  expires_at        DATETIME NOT NULL,
  -- 검증 완료 시각
  verified_at       DATETIME NULL,
  -- 생성 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- 활성 1건을 강제하기 위한 생성 컬럼(활성일 때만 1)
  active_one        TINYINT AS (IF(is_active=1, 1, NULL)) STORED,
  -- FK
  CONSTRAINT fk_ev_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  -- 코드 형식 체크(숫자 5자리)
  CONSTRAINT chk_ev_code CHECK (code REGEXP '^[0-9]{5}$'),
  -- 활성 1건 유니크
  UNIQUE KEY uq_ev_active_one (user_id, purpose, active_one)
) ENGINE=InnoDB;

-- 비밀번호 재설정 토큰 [U_004]
CREATE TABLE password_resets (
  -- PK
  reset_id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 대상 사용자
  user_id           BIGINT UNSIGNED NOT NULL,
  -- 재설정 토큰(예: SHA-256 hex 64자)
  token             CHAR(64) NOT NULL UNIQUE,
  -- 만료 시각
  expires_at        DATETIME NOT NULL,
  -- 사용(완료) 시각
  used_at           DATETIME NULL,
  -- 생성 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- FK
  CONSTRAINT fk_pr_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 쪽지함 [U_014]
CREATE TABLE direct_messages (
  -- PK
  message_id        BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 발신자
  sender_id         BIGINT UNSIGNED NOT NULL,
  -- 수신자
  receiver_id       BIGINT UNSIGNED NOT NULL,
  -- 제목(선택)
  subject           VARCHAR(200) NULL,
  -- 본문
  body              MEDIUMTEXT NOT NULL,
  -- 수신자 열람 여부
  is_read           TINYINT(1) NOT NULL DEFAULT 0,
  -- 열람 시각
  read_at           DATETIME NULL,
  -- 발송 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- 발신자/수신자 측 삭제 플래그(소프트 삭제)
  deleted_by_sender  TINYINT(1) NOT NULL DEFAULT 0,
  deleted_by_receiver TINYINT(1) NOT NULL DEFAULT 0,
  -- FK
  CONSTRAINT fk_dm_sender  FOREIGN KEY (sender_id)  REFERENCES users(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_dm_receiver FOREIGN KEY (receiver_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 수신자 미읽음 정렬 인덱스 / 발신함 인덱스
CREATE INDEX idx_dm_receiver_read  ON direct_messages (receiver_id, is_read, created_at DESC);
CREATE INDEX idx_dm_sender_created ON direct_messages (sender_id, created_at);

-- -------------------------------------------------------------
-- 2) 게시판/검색/신고/북마크 (B_*, A_*)
-- -------------------------------------------------------------
-- 게시판 마스터 (예: 공지-운영/IT/일본어, 자유, Q&A 등)
CREATE TABLE boards (
  board_id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 프로그램에서 쓰는 키(예: NOTICE_OPS, QNA)
  board_key         VARCHAR(50) NOT NULL UNIQUE,
  -- 표시 이름
  name              VARCHAR(100) NOT NULL,
  -- 설명(선택)
  description       VARCHAR(255) NULL,
  -- Q&A 성격 여부
  is_qna            TINYINT(1) NOT NULL DEFAULT 0,
  -- 공지 전용 여부
  is_notice         TINYINT(1) NOT NULL DEFAULT 0,
  -- 공개 여부(비로그인 열람 허용 등 정책)
  is_public         TINYINT(1) NOT NULL DEFAULT 1,
  -- 생성자
  created_by        BIGINT UNSIGNED NULL,
  -- 생성 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- FK
  CONSTRAINT fk_boards_creator FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 즐겨찾는 게시판 [B_003]
CREATE TABLE board_favorites (
  -- 사용자
  user_id           BIGINT UNSIGNED NOT NULL,
  -- 게시판
  board_id          BIGINT UNSIGNED NOT NULL,
  -- 등록 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, board_id),
  CONSTRAINT fk_bf_user  FOREIGN KEY (user_id)  REFERENCES users(user_id)  ON DELETE CASCADE,
  CONSTRAINT fk_bf_board FOREIGN KEY (board_id) REFERENCES boards(board_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 게시글
CREATE TABLE posts (
  post_id           BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 게시판
  board_id          BIGINT UNSIGNED NOT NULL,
  -- 작성자 (탈퇴 시 글 보존 → NULL 허용)
  author_id         BIGINT UNSIGNED NULL,
  -- 유형(일반/질문)
  type              ENUM('POST','QNA') NOT NULL DEFAULT 'POST',
  -- 제목
  title             VARCHAR(200) NOT NULL,
  -- 본문
  content           MEDIUMTEXT NOT NULL,
  -- Q&A 답변 상태
  answer_status     ENUM('PENDING','ANSWERED') NOT NULL DEFAULT 'PENDING',
  -- 상태(활성/삭제/차단)
  status            ENUM('ACTIVE','DELETED','BLOCKED') NOT NULL DEFAULT 'ACTIVE',
  -- 조회수
  view_count        INT UNSIGNED NOT NULL DEFAULT 0,
  -- 생성/수정 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  -- FK
  CONSTRAINT fk_posts_board  FOREIGN KEY (board_id)  REFERENCES boards(board_id) ON DELETE CASCADE,
  CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users(user_id)  ON DELETE SET NULL
) ENGINE=InnoDB;

-- CJK 품질 개선: ngram 파서 FULLTEXT
CREATE FULLTEXT INDEX ftx_posts_title_content
ON posts (title, content) WITH PARSER ngram;

-- 전체/게시판별 정렬
CREATE INDEX idx_posts_board_created   ON posts (board_id, created_at DESC);
CREATE INDEX idx_posts_author_created  ON posts (author_id, created_at);

-- 태그 마스터
CREATE TABLE tags (
  tag_id            BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  name              VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB;

-- 게시글-태그 매핑
CREATE TABLE post_tags (
  post_id           BIGINT UNSIGNED NOT NULL,
  tag_id            BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (post_id, tag_id),
  CONSTRAINT fk_pt_post FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE,
  CONSTRAINT fk_pt_tag  FOREIGN KEY (tag_id)  REFERENCES tags(tag_id)  ON DELETE CASCADE
) ENGINE=InnoDB;

-- 댓글
CREATE TABLE comments (
  comment_id        BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 대상 게시글
  post_id           BIGINT UNSIGNED NOT NULL,
  -- 작성자 (탈퇴 시 댓글 보존 → NULL 허용)
  author_id         BIGINT UNSIGNED NULL,
  -- 부모 댓글(대댓글)
  parent_id         BIGINT UNSIGNED NULL,
  -- 내용
  content           MEDIUMTEXT NOT NULL,
  -- Q&A 답변 표시 여부
  is_answer         TINYINT(1) NOT NULL DEFAULT 0,
  -- 생성/수정 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  -- FK
  CONSTRAINT fk_comments_post   FOREIGN KEY (post_id)  REFERENCES posts(post_id)     ON DELETE CASCADE,
  CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users(user_id)    ON DELETE SET NULL,
  CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id) REFERENCES comments(comment_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_comments_post_created   ON comments (post_id, created_at);
CREATE INDEX idx_comments_author_created ON comments (author_id, created_at);

-- 좋아요(1인 1회)
CREATE TABLE post_likes (
  post_id           BIGINT UNSIGNED NOT NULL,
  user_id           BIGINT UNSIGNED NOT NULL,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (post_id, user_id),
  CONSTRAINT fk_pl_post FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE,
  CONSTRAINT fk_pl_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 북마크
CREATE TABLE post_bookmarks (
  post_id           BIGINT UNSIGNED NOT NULL,
  user_id           BIGINT UNSIGNED NOT NULL,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (post_id, user_id),
  CONSTRAINT fk_pb_post FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE,
  CONSTRAINT fk_pb_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 첨부파일
CREATE TABLE post_attachments (
  attachment_id     BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 게시글
  post_id           BIGINT UNSIGNED NOT NULL,
  -- 저장 경로/URL
  file_url          VARCHAR(1024) NOT NULL,
  -- 원본 파일명
  file_name         VARCHAR(255) NOT NULL,
  -- 파일 크기(byte)
  file_size_bytes   BIGINT UNSIGNED NULL,
  -- 업로드 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- FK
  CONSTRAINT fk_att_post FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 게시글 신고
CREATE TABLE post_reports (
  report_id         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 대상 게시글
  post_id           BIGINT UNSIGNED NOT NULL,
  -- 신고자
  reporter_id       BIGINT UNSIGNED NOT NULL,
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
  CONSTRAINT fk_rep_user FOREIGN KEY (reporter_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_post_reports_status_created ON post_reports (status, created_at);

-- -------------------------------------------------------------
-- 3) 알림/토스트 (P_001~P_010)
-- -------------------------------------------------------------
CREATE TABLE notifications (
  notification_id   BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 수신자
  user_id           BIGINT UNSIGNED NOT NULL,
  -- 유형(공지/댓글/쪽지/조배정/일정/예약/시스템)
  type              ENUM('NOTICE','COMMENT','MESSAGE','GROUP_ASSIGN','SCHEDULE','RESERVATION','SYSTEM') NOT NULL,
  -- 제목
  title             VARCHAR(150) NOT NULL,
  -- 본문(선택)
  body              VARCHAR(500) NULL,
  -- 클릭 이동 경로(URL)
  target_url        VARCHAR(500) NULL,
  -- 참조 엔티티 타입/ID (예: POST,EVENT)
  ref_type          VARCHAR(50) NULL,
  ref_id            BIGINT UNSIGNED NULL,
  -- 읽음 여부/시각
  is_read           TINYINT(1) NOT NULL DEFAULT 0,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  read_at           DATETIME NULL,
  -- FK
  CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_notifications_user_latest ON notifications (user_id, is_read, created_at DESC);

-- -------------------------------------------------------------
-- 4) 캘린더/일정/디데이 (C_*, M_006~M_009)
-- -------------------------------------------------------------
CREATE TABLE events (
  event_id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 공통/개인 일정 구분
  visibility        ENUM('GLOBAL','PERSONAL') NOT NULL DEFAULT 'GLOBAL',
  -- PERSONAL 작성자
  owner_user_id     BIGINT UNSIGNED NULL,
  -- 대상 기수(공통 일정 필터)
  cohort_no         INT NULL,
  -- 대상 반 범위(ALL/A/B)
  it_class_scope    ENUM('ALL','A','B') NOT NULL DEFAULT 'ALL',
  -- 제목/설명
  title             VARCHAR(150) NOT NULL,
  description       MEDIUMTEXT NULL,
  -- 시작/종료 시각
  start_at          DATETIME NOT NULL,
  end_at            DATETIME NOT NULL,
  -- 종일 여부
  is_all_day        TINYINT(1) NOT NULL DEFAULT 0,
  -- 디데이 표시 여부
  dday_enabled      TINYINT(1) NOT NULL DEFAULT 0,
  -- 작성자/시각
  created_by        BIGINT UNSIGNED NULL,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  -- 시간 일관성 체크
  CONSTRAINT chk_events_time CHECK (end_at > start_at),
  -- FK
  CONSTRAINT fk_events_owner   FOREIGN KEY (owner_user_id) REFERENCES users(user_id) ON DELETE SET NULL,
  CONSTRAINT fk_events_creator FOREIGN KEY (created_by)     REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_events_visibility_time ON events (visibility, start_at, end_at);
CREATE INDEX idx_events_owner_time      ON events (owner_user_id, start_at);

-- 문의/답변 (좌석 피드백 포함) [M_012,M_013,S_004]
CREATE TABLE inquiries (
  inquiry_id        BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 작성자 (탈퇴 시 문의 보존 → NULL 허용)
  user_id           BIGINT UNSIGNED NULL,
  -- 카테고리
  category          ENUM('GENERAL','SEAT','RESERVATION','OTHER') NOT NULL DEFAULT 'GENERAL',
  -- 제목/내용
  subject           VARCHAR(150) NOT NULL,
  content           MEDIUMTEXT NOT NULL,
  -- 좌석 피드백 연계 시 좌석 ID (FK는 seats 생성 후 추가)
  seat_id           BIGINT UNSIGNED NULL,
  -- 상태(OPEN/ANSWERED/CLOSED)
  status            ENUM('OPEN','ANSWERED','CLOSED') NOT NULL DEFAULT 'OPEN',
  -- 생성/수정 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  -- FK (user)
  CONSTRAINT fk_inq_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE inquiry_replies (
  reply_id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 문의 ID
  inquiry_id        BIGINT UNSIGNED NOT NULL,
  -- 응답자(관리자 등; 탈퇴 시 답변 보존 → NULL)
  responder_id      BIGINT UNSIGNED NULL,
  -- 답변 본문
  body              MEDIUMTEXT NOT NULL,
  -- 생성 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- FK
  CONSTRAINT fk_ir_inq  FOREIGN KEY (inquiry_id)  REFERENCES inquiries(inquiry_id) ON DELETE CASCADE,
  CONSTRAINT fk_ir_user FOREIGN KEY (responder_id) REFERENCES users(user_id)     ON DELETE SET NULL
) ENGINE=InnoDB;

-- -------------------------------------------------------------
-- 5) 조편성/좌석 배치 (M_017~M_028, S_001~S_004)
-- -------------------------------------------------------------
-- 조(그룹) 마스터
CREATE TABLE study_groups (
  group_id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
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
) ENGINE=InnoDB;

-- 조 배정 (한 학생은 동일 기수/반에 1개 조만)
CREATE TABLE group_assignments (
  assignment_id     BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 기수/반/조
  cohort_no         INT NOT NULL,
  class_section     ENUM('A','B') NOT NULL,
  group_id          BIGINT UNSIGNED NOT NULL,
  -- 배정된 사용자
  user_id           BIGINT UNSIGNED NOT NULL,
  -- 배정 시각
  assigned_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- (동일 기수/반에서 사용자 중복 배정 방지)
  UNIQUE KEY uq_ga_unique (cohort_no, class_section, user_id),
  KEY idx_ga_group (group_id),
  -- FK
  CONSTRAINT fk_ga_group FOREIGN KEY (group_id) REFERENCES study_groups(group_id) ON DELETE CASCADE,
  CONSTRAINT fk_ga_user  FOREIGN KEY (user_id)  REFERENCES users(user_id)         ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_ga_user ON group_assignments (user_id);

-- 강의실/자습실 마스터
CREATE TABLE rooms (
  room_id           BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 예: 4F-401, 4F-자습1
  name              VARCHAR(100) NOT NULL,
  -- 유형(CLASSROOM/STUDY/MEETING)
  type              ENUM('CLASSROOM','STUDY','MEETING') NOT NULL DEFAULT 'CLASSROOM',
  -- 층 번호
  floor_no          INT NULL,
  -- 수용 인원
  capacity          INT NULL,
  -- 사용 여부
  is_active         TINYINT(1) NOT NULL DEFAULT 1,
  -- 생성 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 좌석 배치도 버전 (기수/반별)
CREATE TABLE seat_maps (
  seat_map_id       BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 강의실
  room_id           BIGINT UNSIGNED NOT NULL,
  -- 기수/반
  cohort_no         INT NOT NULL,
  class_section     ENUM('A','B') NOT NULL,
  -- 버전 번호/활성 플래그
  version_no        INT NOT NULL DEFAULT 1,
  is_active         TINYINT(1) NOT NULL DEFAULT 1,
  -- 활성 1개 강제용 생성 컬럼
  active_one        TINYINT AS (IF(is_active=1, 1, NULL)) STORED,
  -- 생성 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- 고유성
  UNIQUE KEY uq_active_map (room_id, cohort_no, class_section, version_no),
  UNIQUE KEY uq_sm_active_one (room_id, cohort_no, class_section, active_one),
  -- FK
  CONSTRAINT fk_sm_room FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 좌석 정의(배치도 내 좌표/코드)
CREATE TABLE seats (
  seat_id           BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 배치도
  seat_map_id       BIGINT UNSIGNED NOT NULL,
  -- 좌석 코드(예: A-01)
  seat_code         VARCHAR(20) NOT NULL,
  -- 행/열 좌표(선택)
  row_no            INT NULL,
  col_no            INT NULL,
  -- 동일 배치도 내 좌석 코드 중복 방지
  UNIQUE KEY uq_seat_code (seat_map_id, seat_code),
  -- FK
  CONSTRAINT fk_seat_map FOREIGN KEY (seat_map_id) REFERENCES seat_maps(seat_map_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 좌석 배정(좌석당 1명, 사용자당 1좌석)
CREATE TABLE seat_assignments (
  seat_assignment_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 배치도/좌석/사용자
  seat_map_id       BIGINT UNSIGNED NOT NULL,
  seat_id           BIGINT UNSIGNED NOT NULL,
  user_id           BIGINT UNSIGNED NOT NULL,
  -- 배정 시각
  assigned_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- 유일성 제약
  UNIQUE KEY uq_sa_seat (seat_map_id, seat_id),
  UNIQUE KEY uq_sa_user (seat_map_id, user_id),
  -- FK
  CONSTRAINT fk_sa_map  FOREIGN KEY (seat_map_id) REFERENCES seat_maps(seat_map_id) ON DELETE CASCADE,
  CONSTRAINT fk_sa_seat FOREIGN KEY (seat_id)     REFERENCES seats(seat_id)         ON DELETE CASCADE,
  CONSTRAINT fk_sa_user FOREIGN KEY (user_id)     REFERENCES users(user_id)        ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_sa_user ON seat_assignments (user_id);

-- 좌석 브로드캐스트 로그(관리자 메시지)
CREATE TABLE seat_broadcasts (
  broadcast_id      BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 배치도/좌석(NULL이면 전체)
  seat_map_id       BIGINT UNSIGNED NOT NULL,
  seat_id           BIGINT UNSIGNED NULL,
  -- 메시지(예: 일어나세요)
  message           VARCHAR(200) NOT NULL,
  -- 발신 관리자 (탈퇴 시 기록 보존 → NULL 허용)
  sent_by           BIGINT UNSIGNED NULL,
  -- 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- FK
  CONSTRAINT fk_sb_map  FOREIGN KEY (seat_map_id) REFERENCES seat_maps(seat_map_id) ON DELETE CASCADE,
  CONSTRAINT fk_sb_seat FOREIGN KEY (seat_id)     REFERENCES seats(seat_id)         ON DELETE SET NULL,
  CONSTRAINT fk_sb_user FOREIGN KEY (sent_by)     REFERENCES users(user_id)         ON DELETE SET NULL
) ENGINE=InnoDB;

-- 좌석 피드백 FK 추가(문의 -> 좌석)
ALTER TABLE inquiries
  ADD CONSTRAINT fk_inq_seat FOREIGN KEY (seat_id) REFERENCES seats(seat_id) ON DELETE SET NULL;

-- -------------------------------------------------------------
-- 6) 강의실 예약/신고/패널티 (R_001~R_006)
-- -------------------------------------------------------------
CREATE TABLE reservations (
  reservation_id    BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 강의실/사용자
  room_id           BIGINT UNSIGNED NOT NULL,
  user_id           BIGINT UNSIGNED NOT NULL,
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
) ENGINE=InnoDB;

CREATE INDEX idx_res_room_time ON reservations (room_id, start_at, end_at);
CREATE INDEX idx_res_user_time ON reservations (user_id, start_at, end_at);

-- 예약 미사용 신고
CREATE TABLE reservation_reports (
  report_id         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 대상 예약/신고자
  reservation_id    BIGINT UNSIGNED NOT NULL,
  reporter_id       BIGINT UNSIGNED NULL,
  -- 사유/상태
  reason            VARCHAR(255) NULL,
  status            ENUM('PENDING','CONFIRMED','REJECTED') NOT NULL DEFAULT 'PENDING',
  -- 생성 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- FK
  CONSTRAINT fk_rr_res      FOREIGN KEY (reservation_id) REFERENCES reservations(reservation_id) ON DELETE CASCADE,
  CONSTRAINT fk_rr_reporter FOREIGN KEY (reporter_id)     REFERENCES users(user_id)            ON DELETE SET NULL
) ENGINE=InnoDB;

-- 예약 패널티/차단(누적 3회 등)
CREATE TABLE reservation_penalties (
  penalty_id        BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 대상 사용자
  user_id           BIGINT UNSIGNED NOT NULL,
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
) ENGINE=InnoDB;

-- -------------------------------------------------------------
-- 7) 졸업생 현황/회사/리뷰
-- -------------------------------------------------------------
CREATE TABLE companies (
  company_id        BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
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
) ENGINE=InnoDB;

-- 회사 리뷰(1인 1회)
CREATE TABLE company_reviews (
  review_id         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 회사/리뷰어(탈퇴 시 리뷰 보존 → NULL)
  company_id        BIGINT UNSIGNED NOT NULL,
  reviewer_id       BIGINT UNSIGNED NULL,
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
  CONSTRAINT fk_cr_user    FOREIGN KEY (reviewer_id) REFERENCES users(user_id)     ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_cr_company_created ON company_reviews (company_id, created_at);

-- 리뷰 댓글
CREATE TABLE company_review_comments (
  comment_id        BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 리뷰/작성자(탈퇴 시 댓글 보존 → NULL)
  review_id         BIGINT UNSIGNED NOT NULL,
  author_id         BIGINT UNSIGNED NULL,
  -- 본문/시각
  body              MEDIUMTEXT NOT NULL,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- FK
  CONSTRAINT fk_crc_review FOREIGN KEY (review_id) REFERENCES company_reviews(review_id) ON DELETE CASCADE,
  CONSTRAINT fk_crc_user   FOREIGN KEY (author_id)  REFERENCES users(user_id)            ON DELETE SET NULL
) ENGINE=InnoDB;

-- 평균 평점 뷰(정렬용)
CREATE OR REPLACE VIEW v_company_ratings AS
SELECT c.company_id,
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
  course_id         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 과목명/강사명
  name              VARCHAR(150) NOT NULL,
  instructor_name   VARCHAR(100) NULL,
  -- 기수/반(선택)
  cohort_no         INT NULL,
  class_section     ENUM('A','B') NULL,
  -- 시간표 표기(예: 월/수 10:00-12:00)
  schedule_text     VARCHAR(200) NULL,
  -- 생성 시각
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 수강 등록(강의평가 범위 결정)
CREATE TABLE enrollments (
  course_id         BIGINT UNSIGNED NOT NULL,
  user_id           BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (course_id, user_id),
  CONSTRAINT fk_enr_course FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
  CONSTRAINT fk_enr_user   FOREIGN KEY (user_id)  REFERENCES users(user_id)     ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_enr_user ON enrollments (user_id);

-- 강의 평가(6항목 1~5, 1인 1회)
CREATE TABLE course_evaluations (
  evaluation_id     BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 과목/작성자 (탈퇴 시 평가 보존 → NULL)
  course_id         BIGINT UNSIGNED NOT NULL,
  user_id           BIGINT UNSIGNED NULL,
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
  CONSTRAINT fk_ce_user   FOREIGN KEY (user_id)  REFERENCES users(user_id)     ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_ce_course_created ON course_evaluations (course_id, created_at);

-- 과목별 평균 뷰
CREATE OR REPLACE VIEW v_course_eval_avg AS
SELECT course_id,
       AVG(score_preparedness) AS avg_prepared,
       AVG(score_clarity)      AS avg_clarity,
       AVG(score_fairness)     AS avg_fairness,
       AVG(score_respond)      AS avg_respond,
       AVG(score_engagement)   AS avg_engagement,
       AVG(score_passion)      AS avg_passion,
       COUNT(*)                AS eval_count
FROM course_evaluations
GROUP BY course_id;

-- -------------------------------------------------------------
-- 9) 과제 제출 (T_001~T_006)
-- -------------------------------------------------------------
CREATE TABLE assignments (
  assignment_id     BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 과목/대상 기수/반(선택)
  course_id         BIGINT UNSIGNED NULL,
  cohort_no         INT NULL,
  class_section     ENUM('A','B') NULL,
  -- 과제명/파일명 규칙/마감시각
  name              VARCHAR(150) NOT NULL,
  filename_pattern  VARCHAR(200) NULL,
  due_at            DATETIME NULL,
  -- 생성자/시각
  created_by        BIGINT UNSIGNED NULL,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- FK
  CONSTRAINT fk_asn_course  FOREIGN KEY (course_id)  REFERENCES courses(course_id) ON DELETE SET NULL,
  CONSTRAINT fk_asn_creator FOREIGN KEY (created_by) REFERENCES users(user_id)     ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_asn_scope_due ON assignments (cohort_no, class_section, due_at);

-- 과제 제출(재제출 시 갱신)
CREATE TABLE assignment_submissions (
  submission_id     BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 과제/제출자
  assignment_id     BIGINT UNSIGNED NOT NULL,
  user_id           BIGINT UNSIGNED NOT NULL,
  -- 제출 경로(업로드/이메일) 및 이메일 메타(선택)
  delivery_method   ENUM('UPLOAD','EMAIL') NOT NULL DEFAULT 'UPLOAD',
  email_meta        JSON NULL,
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
  CONSTRAINT fk_sub_user       FOREIGN KEY (user_id)       REFERENCES users(user_id)            ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_sub_assignment ON assignment_submissions (assignment_id);

-- -------------------------------------------------------------
-- 10) 사진 앨범
-- -------------------------------------------------------------
CREATE TABLE albums (
  album_id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 대상 기수(선택)
  cohort_no         INT NULL,
  -- 앨범명
  name              VARCHAR(150) NOT NULL,
  -- 생성자/시각
  created_by        BIGINT UNSIGNED NULL,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- FK
  CONSTRAINT fk_album_user FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE photos (
  photo_id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 앨범/업로더/파일
  album_id          BIGINT UNSIGNED NOT NULL,
  uploader_id       BIGINT UNSIGNED NULL,
  file_url          VARCHAR(1024) NOT NULL,
  -- 캡션/시각
  caption           VARCHAR(255) NULL,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- FK
  CONSTRAINT fk_ph_album    FOREIGN KEY (album_id)    REFERENCES albums(album_id) ON DELETE CASCADE,
  CONSTRAINT fk_ph_uploader FOREIGN KEY (uploader_id) REFERENCES users(user_id)  ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_ph_album_created ON photos (album_id, created_at);

CREATE TABLE photo_likes (
  -- 사진/사용자 PK
  photo_id          BIGINT UNSIGNED NOT NULL,
  user_id           BIGINT UNSIGNED NOT NULL,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (photo_id, user_id),
  CONSTRAINT fk_pl_photo FOREIGN KEY (photo_id) REFERENCES photos(photo_id) ON DELETE CASCADE,
  CONSTRAINT fk_pl_user2 FOREIGN KEY (user_id)  REFERENCES users(user_id)  ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE photo_comments (
  comment_id        BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- 사진/작성자(탈퇴 시 댓글 보존 → NULL)
  photo_id          BIGINT UNSIGNED NOT NULL,
  user_id           BIGINT UNSIGNED NULL,
  -- 내용/시각
  content           MEDIUMTEXT NOT NULL,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- FK
  CONSTRAINT fk_pc_photo FOREIGN KEY (photo_id) REFERENCES photos(photo_id) ON DELETE CASCADE,
  CONSTRAINT fk_pc_user  FOREIGN KEY (user_id)  REFERENCES users(user_id)  ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_pc_photo_created ON photo_comments (photo_id, created_at);

-- =============================================================
-- 트리거(이벤트 동기화/정합성 보강)
-- =============================================================

DELIMITER //

-- 이메일 인증: verified_at 설정 시 자동 비활성화
CREATE TRIGGER trg_ev_set_inactive
BEFORE UPDATE ON email_verifications
FOR EACH ROW
BEGIN
  IF NEW.verified_at IS NOT NULL THEN
    SET NEW.is_active = 0;
  END IF;
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
  IF NEW.is_answer = 1 AND (OLD.is_answer IS NULL OR OLD.is_answer = 0) THEN
    UPDATE posts SET answer_status='ANSWERED' WHERE post_id = NEW.post_id;
  END IF;

  IF OLD.is_answer = 1 AND NEW.is_answer = 0 THEN
    DECLARE cnt INT DEFAULT 0;
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
  IF OLD.is_answer = 1 THEN
    DECLARE cnt INT DEFAULT 0;
    SELECT COUNT(*) INTO cnt
      FROM comments
     WHERE post_id = OLD.post_id AND is_answer = 1;
    IF cnt = 0 THEN
      UPDATE posts SET answer_status='PENDING' WHERE post_id = OLD.post_id;
    END IF;
  END IF;
END//

DELIMITER ;

-- =============================================================
-- 끝. (ENGINE=InnoDB: 트랜잭션, FK, Row Lock, Crash Recovery 지원)
-- =============================================================
