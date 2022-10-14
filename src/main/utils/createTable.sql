-- drop table user_repo cascade;

create table users
(
    id       serial
        constraint users_pkey
            primary key,
    name     varchar(20) not null
        constraint users_name_key
            unique,
    password varchar     not null,
    email    varchar     not null
);

create table user_info
(
    user_id      integer                                                       not null
        constraint user_info_pkey
            primary key
        constraint user_info_user_id_fkey
            references users,
    display_name varchar                                                       not null,
    bio          varchar default ''::character varying                         not null,
    create_time  bigint  default (EXTRACT(epoch FROM now()) * (1000)::numeric) not null,
    url          text,
    avatar       text,
    github       text
);

comment on column user_info.url is 'user homepage url';

create table repo
(
    id            serial
        constraint repo_pkey
            primary key,
    name          varchar           not null,
    visible       integer default 0 not null,
    next_issue_id integer default 1 not null,
    owner_id      integer           not null,
    constraint repo_pk
        unique (owner_id, name)
);

create table user_repo
(
    user_id    integer           not null
        constraint user_repo_user_id_fkey
            references users,
    repo_id    integer           not null
        constraint user_repo_repo_id_fkey
            references repo
            on delete cascade,
    permission integer default 0 not null,
    constraint user_repo_user_id_repo_id_key
        unique (user_id, repo_id)
);

create index user_repo_user_id_idx
    on user_repo (user_id);

create index user_repo_repo_id_idx
    on user_repo (repo_id);

create table repo_perm_invite
(
    id          serial
        constraint repo_perm_invite_pkey
            primary key,
    inviter     integer                                                      not null
        constraint repo_perm_invite_inviter_fkey
            references users,
    invited     integer                                                      not null
        constraint repo_perm_invite_invited_fkey
            references users,
    repo_id     integer                                                      not null
        constraint repo_perm_invite_repo_id_fkey
            references repo,
    create_time bigint default (EXTRACT(epoch FROM now()) * (1000)::numeric) not null
);

create table issue
(
    id              serial
        constraint issue_pkey
            primary key,
    repo_id         integer                                                       not null
        constraint issue_repo_id_fkey
            references repo
            on update cascade on delete cascade,
    repo_issue_id   integer                                                       not null,
    issuer_user_id  integer                                                       not null
        constraint issue_issuer_user_id_fkey
            references users
            on update cascade on delete cascade,
    next_comment_id integer default 1                                             not null,
    title           text                                                          not null,
    status          text    default 'open'::text                                  not null,
    tag             text    default ''::text,
    created_at      bigint  default (EXTRACT(epoch FROM now()) * (1000)::numeric) not null,
    constraint issue_repo_id_repo_issue_id_key
        unique (repo_id, repo_issue_id)
);

create table issue_content
(
    id             serial
        constraint issue_content_pkey
            primary key,
    issue_id       integer                      not null
        constraint issue_content_issue_id_fkey
            references issue,
    comment_id     integer                      not null,
    sender_user_id integer                      not null
        constraint issue_content_sender_user_id_fkey
            references users,
    type           text default 'comment'::text not null,
    content        text                         not null,
    constraint issue_content_issue_id_comment_id_key
        unique (issue_id, comment_id)
);

create table pull_requests
(
    id              serial
        constraint pull_requests_pkey
            primary key,
    from_repo_id    integer                                                       not null
        constraint pull_requests_from_repo_id_fkey
            references repo
            on update cascade on delete cascade,
    to_repo_id      integer                                                       not null
        constraint pull_requests_to_repo_id_fkey
            references repo
            on update cascade on delete cascade,
    from_branch     text                                                          not null,
    to_branch       text                                                          not null,
    repo_pr_id      integer                                                       not null,
    prer_user_id    integer                                                       not null
        constraint pull_requests_prer_user_id_fkey
            references users
            on update cascade on delete cascade,
    next_comment_id integer default 1                                             not null,
    title           text                                                          not null,
    status          text    default 'open'::text                                  not null,
    tag             text,
    created_at      bigint  default (EXTRACT(epoch FROM now()) * (1000)::numeric) not null,
    constraint pull_requests_to_repo_id_repo_pr_id_key
        unique (to_repo_id, repo_pr_id)
);


