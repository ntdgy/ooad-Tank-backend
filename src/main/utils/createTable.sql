drop table if exists users cascade;
drop table if exists user_info cascade;
drop table if exists repo cascade;
drop table if exists user_repo cascade;

create table users
(
    id       serial primary key,
    name     varchar(20) unique not null,
    password varchar            not null,
    email    varchar            not null
);

create table user_info
(
    user_id      int primary key references users (id),
    display_name varchar not null,
    bio          varchar not null default '',
    create_time  int8    not null default (extract(epoch from now()) * 1000)
);

create table repo
(
    id       serial primary key,
    name     varchar not null,
    visible  int     not null default (0),
    owner_id int     not null references users (id)
);

create table user_repo
(
    user_id    int not null references users (id),
    repo_id    int not null references repo (id) on delete cascade,
    permission int not null default (0),
    unique (user_id, repo_id)
);
create index on user_repo (user_id);
create index on user_repo (repo_id);

create table repo_perm_invite
(
    id          serial primary key,
    inviter     int  not null references users (id),
    invited     int  not null references users (id),
    repo_id     int  not null references repo (id),
    create_time int8 not null default (extract(epoch from now()) * 1000)
);


drop table if exists issue cascade;
drop table if exists issue_content cascade;
create table issue
(
    id              serial primary key,
    repo_id         integer                                                       not null references repo (id) on delete cascade on update cascade,
    repo_issue_id   integer                                                       not null,
    issuer_user_id  integer                                                       not null references users (id) on delete cascade on update cascade,
    next_comment_id integer default 1                                             not null,
    title           text                                                          not null,
    status          text    default 'open'                                        not null,
    tag             text    default null,
    created_at      bigint  default (EXTRACT(epoch FROM now()) * (1000)::numeric) not null,
    unique (repo_id, repo_issue_id)
);
create index on issue (repo_id, issuer_user_id);
create table issue_content
(
    id             serial primary key,
    issue_id       integer                not null references issue (id),
    comment_id     int                    not null,
    sender_user_id integer                not null references users (id),
    type           text default 'comment' not null,
    content        text                   not null,
    unique (issue_id, comment_id)
);
create index on issue_content (issue_id, sender_user_id);


drop table if exists pull_requests cascade;
create table pull_requests
(
    id              serial primary key,
    from_repo_id    integer                                                       not null references repo (id) on delete cascade on update cascade,
    to_repo_id      integer                                                       not null references repo (id) on delete cascade on update cascade,
    from_branch     text                                                          not null,
    to_branch       text                                                          not null,
    repo_pr_id      integer                                                       not null,
    prer_user_id  integer                                                       not null references users (id) on delete cascade on update cascade,
    next_comment_id integer default 1                                             not null,
    title           text                                                          not null,
    status          text    default 'open'                                        not null,
    tag             text    default null,
    created_at      bigint  default (EXTRACT(epoch FROM now()) * (1000)::numeric) not null,
    unique (to_repo_id, repo_pr_id)
);
create index on pull_requests (from_repo_id, prer_user_id);