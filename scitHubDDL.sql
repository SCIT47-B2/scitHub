create table album
(
    album_id    int auto_increment
        primary key,
    name        varchar(150) not null,
    description mediumtext   null
);

create table board
(
    board_id    int auto_increment
        primary key,
    name        varchar(100) not null,
    description varchar(255) null
);

create table classroom
(
    classroom_id int auto_increment
        primary key,
    name         varchar(100)                                        not null,
    type         enum ('CLASSROOM', 'STUDYROOM') default 'CLASSROOM' not null,
    is_active    tinyint                         default 1           not null
);

create table company
(
    company_id int auto_increment
        primary key,
    name       varchar(150)                                       not null,
    logo_url   varchar(500)                                       null,
    location   varchar(100)                                       not null,
    industry   enum ('IT', '제조', '금융', '산업', '통신', '서비스', '그 외')  not null,
    type       enum ('자사 개발', '자사 SI', '타사 SI', 'SES(파견)', '그 외') not null,
    headcount  int                                                null
);

create table course
(
    course_id       int auto_increment
        primary key,
    name            varchar(150)      not null,
    course_type     enum ('IT', 'JP') not null,
    cohort_no       int               not null,
    instructor_name varchar(50)       null
);

create table student_group
(
    student_group_id int auto_increment
        primary key,
    cohort_no        int             not null,
    class_section    enum ('A', 'B') not null,
    name             varchar(50)     not null,
    order_index      int             not null
);

create table user
(
    user_id           int auto_increment
        primary key,
    cohort_no         int                                              not null,
    username          varchar(32)                                      not null,
    password_hash     varchar(255)                                     not null,
    name_kor          varchar(50)                                      not null,
    birth_date        date                                             not null,
    gender            enum ('M', 'F', 'N')   default 'N'               not null,
    email             varchar(255)                                     not null,
    phone             varchar(30)                                      not null,
    avatar_url        varchar(500)                                     null,
    student_group_id  int                                              null,
    resv_strike_count int                    default 0                 not null,
    resv_banned_until datetime                                         null,
    is_active         tinyint                default 1                 not null,
    role              enum ('USER', 'ADMIN') default 'USER'            not null,
    last_login_at     datetime                                         null on update CURRENT_TIMESTAMP,
    created_at        datetime               default CURRENT_TIMESTAMP null,
    constraint user_student_group_student_group_id_fk
        foreign key (student_group_id) references student_group (student_group_id)
            on delete set null
);

create table board_bookmark
(
    board_bookmark_id int auto_increment
        primary key,
    board_id          int not null,
    user_id           int not null,
    constraint board_bookmark_uq
        unique (board_id, user_id),
    constraint board_bookmark_board_board_id_fk
        foreign key (board_id) references board (board_id)
            on delete cascade,
    constraint board_bookmark_user_user_id_fk
        foreign key (user_id) references user (user_id)
            on delete cascade
);

create table company_review
(
    company_review_id int auto_increment
        primary key,
    company_id        int                                not null,
    user_id           int                                null,
    rating            tinyint                            not null,
    content           mediumtext                         null,
    created_at        datetime default CURRENT_TIMESTAMP not null,
    constraint company_review_uq
        unique (company_id, user_id),
    constraint company_review_company_company_id_fk
        foreign key (company_id) references company (company_id)
            on delete cascade,
    constraint company_review_user_user_id_fk
        foreign key (user_id) references user (user_id)
            on delete set null
);

create table course_review
(
    course_review_id    int auto_increment
        primary key,
    course_id           int                                not null,
    user_id             int                                null,
    score_preparedness  tinyint                            not null,
    score_profesion     tinyint                            not null,
    score_communication tinyint                            not null,
    score_engagement    tinyint                            not null,
    score_fairness      tinyint                            not null,
    course_difficulty   tinyint                            not null,
    course_assignment   tinyint                            not null,
    course_connectivity tinyint                            not null,
    comment_text        mediumtext                         null,
    created_at          datetime default CURRENT_TIMESTAMP not null,
    constraint course_review_uq
        unique (user_id, course_id),
    constraint course_review_course_course_id_fk
        foreign key (course_id) references course (course_id)
            on delete cascade,
    constraint course_review_user_user_id_fk
        foreign key (user_id) references user (user_id)
            on delete set null
);

create table dday
(
    dday_id int auto_increment
        primary key,
    user_id int          not null,
    dday    date         not null,
    title   varchar(100) not null,
    constraint dday_user_user_id_fk
        foreign key (user_id) references user (user_id)
            on delete cascade
);

create table event
(
    event_id   int auto_increment
        primary key,
    visibility enum ('PUBLIC', 'PRIVATE') default 'PUBLIC' not null,
    user_id    int                                         null,
    title      varchar(150)                                not null,
    content    mediumtext                                  null,
    start_at   datetime                                    not null,
    end_at     datetime                                    null,
    is_all_day tinyint                    default 0        not null,
    constraint event_user_user_id_fk
        foreign key (user_id) references user (user_id)
            on delete cascade
);

create table message
(
    message_id  int auto_increment
        primary key,
    sender_id   int                                not null,
    receiver_id int                                not null,
    title       varchar(200)                       not null,
    content     mediumtext                         not null,
    is_read     tinyint  default 0                 not null,
    created_at  datetime default CURRENT_TIMESTAMP null,
    constraint message_user_user_id_fk
        foreign key (sender_id) references user (user_id),
    constraint message_user_user_id_fk_2
        foreign key (receiver_id) references user (user_id)
);

create table photo
(
    photo_id   int auto_increment
        primary key,
    album_id   int                                not null,
    user_id    int                                not null,
    file_url   varchar(1024)                      not null,
    caption    varchar(255)                       null,
    created_at datetime default CURRENT_TIMESTAMP not null,
    constraint photo_album_album_id_fk
        foreign key (album_id) references album (album_id)
            on delete cascade,
    constraint photo_user_user_id_fk
        foreign key (user_id) references user (user_id)
);

create table post
(
    post_id    int auto_increment
        primary key,
    board_id   int                                not null,
    user_id    int                                not null,
    title      varchar(200)                       not null,
    content    mediumtext                         not null,
    view_count int      default 0                 not null,
    created_at datetime default CURRENT_TIMESTAMP not null,
    updated_at datetime                           null on update CURRENT_TIMESTAMP,
    constraint post_board_board_id_fk
        foreign key (board_id) references board (board_id),
    constraint post_user_user_id_fk
        foreign key (user_id) references user (user_id)
);

create table attachment_file
(
    attachment_file_id int auto_increment
        primary key,
    post_id            int           not null,
    file_url           varchar(1024) not null,
    file_name          varchar(255)  not null,
    file_size          int           null,
    constraint attachment_file_post_post_id_fk
        foreign key (post_id) references post (post_id)
            on delete cascade
);

create table comment
(
    comment_id        int auto_increment
        primary key,
    user_id           int                                not null,
    comment           mediumtext                         not null,
    created_at        datetime default CURRENT_TIMESTAMP not null,
    updated_at        datetime                           null on update CURRENT_TIMESTAMP,
    post_id           int                                null,
    company_review_id int                                null,
    photo_id          int                                null,
    constraint comment_company_review_company_review_id_fk
        foreign key (company_review_id) references company_review (company_review_id)
            on delete cascade,
    constraint comment_photo_photo_id_fk
        foreign key (photo_id) references photo (photo_id)
            on delete cascade,
    constraint comment_post_post_id_fk
        foreign key (post_id) references post (post_id)
            on delete cascade,
    constraint comment_user_user_id_fk
        foreign key (user_id) references user (user_id)
);

create table post_bookmark
(
    post_bookmark_id int auto_increment
        primary key,
    post_id          int not null,
    user_id          int not null,
    constraint post_bookmark_uq
        unique (post_id, user_id),
    constraint post_bookmark_post_post_id_fk
        foreign key (post_id) references post (post_id)
            on delete cascade,
    constraint post_bookmark_user_user_id_fk
        foreign key (user_id) references user (user_id)
            on delete cascade
);

create table post_like
(
    post_id      int not null,
    user_id      int null,
    post_like_id int auto_increment
        primary key,
    constraint post_like_uq
        unique (post_id, user_id),
    constraint post_like_post_post_id_fk
        foreign key (post_id) references post (post_id)
            on delete cascade,
    constraint post_like_user_user_id_fk
        foreign key (user_id) references user (user_id)
            on delete set null
);

create table reservation
(
    reservation_id int auto_increment
        primary key,
    classroom_id   int                                not null,
    user_id        int                                not null,
    start_at       datetime                           not null,
    end_at         datetime                           not null,
    created_at     datetime default CURRENT_TIMESTAMP not null,
    constraint reservation_classroom_classroom_id_fk
        foreign key (classroom_id) references classroom (classroom_id)
            on delete cascade,
    constraint reservation_user_user_id_fk
        foreign key (user_id) references user (user_id)
            on delete cascade
);

create table notification
(
    notification_id  int auto_increment
        primary key,
    user_id          int                                not null,
    title            varchar(150)                       not null,
    content          mediumtext                         not null,
    target_url       varchar(500)                       null,
    is_read          tinyint  default 0                 not null,
    created_at       datetime default CURRENT_TIMESTAMP null,
    post_id          int                                null,
    comment_id       int                                null,
    message_id       int                                null,
    student_group_id int                                null,
    event_id         int                                null,
    reservation_id   int                                null,
    constraint notification_comment_comment_id_fk
        foreign key (comment_id) references comment (comment_id)
            on delete cascade,
    constraint notification_event_event_id_fk
        foreign key (event_id) references event (event_id)
            on delete cascade,
    constraint notification_message_message_id_fk
        foreign key (message_id) references message (message_id)
            on delete cascade,
    constraint notification_post_post_id_fk
        foreign key (post_id) references post (post_id)
            on delete cascade,
    constraint notification_reservation_reservation_id_fk
        foreign key (reservation_id) references reservation (reservation_id)
            on delete cascade,
    constraint notification_student_group_student_group_id_fk
        foreign key (student_group_id) references student_group (student_group_id)
            on delete cascade,
    constraint notification_user_user_id_fk
        foreign key (user_id) references user (user_id)
            on delete cascade
);

create table report
(
    report_id         int auto_increment
        primary key,
    user_id           int                                                                not null,
    content           varchar(255)                                                       null,
    status            enum ('PENDING', 'RESOLVED', 'REJECTED') default 'PENDING'         not null,
    created_at        datetime                                 default CURRENT_TIMESTAMP not null,
    post_id           int                                                                null,
    comment_id        int                                                                null,
    reservation_id    int                                                                null,
    company_review_id int                                                                null,
    constraint report_comment_comment_id_fk
        foreign key (comment_id) references comment (comment_id)
            on delete cascade,
    constraint report_company_review_company_review_id_fk
        foreign key (company_review_id) references company_review (company_review_id)
            on delete cascade,
    constraint report_post_post_id_fk
        foreign key (post_id) references post (post_id)
            on delete cascade,
    constraint report_reservation_reservation_id_fk
        foreign key (reservation_id) references reservation (reservation_id)
            on delete cascade,
    constraint report_user_user_id_fk
        foreign key (user_id) references user (user_id)
);

create table seat
(
    seat_id      int auto_increment
        primary key,
    classroom_id int not null,
    row_no       int not null,
    col_no       int not null,
    user_id      int null,
    constraint seat_classroom_classroom_id_fk
        foreign key (classroom_id) references classroom (classroom_id)
            on delete cascade,
    constraint seat_user_user_id_fk
        foreign key (user_id) references user (user_id)
            on delete set null
);

create table tag
(
    tag_id  int auto_increment
        primary key,
    post_id int         not null,
    name    varchar(50) not null,
    constraint tag_uq
        unique (post_id, name),
    constraint tag_post_post_id_fk
        foreign key (post_id) references post (post_id)
            on delete cascade
);


