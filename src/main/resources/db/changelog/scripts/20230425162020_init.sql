-- liquibase formatted sql

-- changeset tin:init-migration
create table  if not exists  clicks
(
    id         bigint auto_increment
        primary key,
    created_at datetime(6) null
);

 create table if not exists prd_product
(
    prd_id           int auto_increment
        primary key,
    prd_created_at   datetime(6)  null,
    prd_description  varchar(255) null,
    prd_name         varchar(255) null,
    prd_picture_path varchar(255) null,
    prd_price        float        null,
    prd_stock        int          null,
    prd_updated_at   datetime(6)  null
);

 create table if not exists roles
(
    id   int auto_increment
        primary key,
    name varchar(50) null
);

 create table if not exists users
(
    id                  bigint auto_increment
        primary key,
    usr_active          bit          null,
    usr_affiliate_link  varchar(255) null,
    business            varchar(255) null,
    usr_created_at      datetime(6)  null,
    email               varchar(70)  null,
    first_name          varchar(120) null,
    last_name           varchar(120) null,
    mfa                 bit          not null,
    password            varchar(120) null,
    profile_picturepath varchar(255) null,
    profilepic          mediumblob   null,
    usr_reset_tokem     varchar(255) null,
    secret              varchar(255) null,
    tel                 varchar(120) null,
    usr_token           varchar(255) null,
    usr_updated_at      datetime(6)  null,
    constraint UK6dotkott2kjsp8vw4d0m25fb7
        unique (email),
    constraint UK_opaau0844qqbnof1rnx2gevxt
        unique (usr_affiliate_link)
);

 create table if not exists aff_affiliate
(
    aff_id              int auto_increment
        primary key,
    aff_affiliate_link  varchar(255) null,
    aff_commission_rate float        null,
    aff_created_at      datetime(6)  null,
    aff_user_id         bigint       null,
    constraint FKieaocf1jfed9sb556tuo5hy2q
        foreign key (aff_user_id) references  users (id)
);

 create table if not exists affiliate_counter
(
    id        bigint auto_increment
        primary key,
    timestamp datetime(6) null,
    aff_c_id  bigint      null,
    constraint FKm373s46gmiksggge10e1mqm8p
        foreign key (aff_c_id) references  users (id)
);

 create table if not exists hierarchy
(
    id                bigint auto_increment
        primary key,
    commission_rate   float        null,
    created_at        datetime(6)  null,
    relationship_type varchar(255) null,
    child_id          bigint       null,
    parent_id         bigint       null,
    constraint UKr4xgw4cx7ylclubos7f74w55g
        unique (child_id),
    constraint FKgetqmitxvp33ve0rt3ehejaau
        foreign key (child_id) references  users (id),
    constraint FKhgr0evoqkxbqqymwjs4w6xtpo
        foreign key (parent_id) references  users (id)
);

 create table if not exists kyc_image
(
    id        bigint auto_increment
        primary key,
    confirmed bit          null,
    data      blob         null,
    type      varchar(255) null,
    user_id   bigint       null,
    constraint FKt1xq5uapi9n3m22rqqljy3wwr
        foreign key (user_id) references  users (id)
);

 create table if not exists link_counter
(
    id        bigint auto_increment
        primary key,
    timestamp datetime(6) null,
    link_user bigint      null,
    constraint FKi08180tgmumsluco0lmj2m9ui
        foreign key (link_user) references  users (id)
);

 create table if not exists password_reset_token
(
    id           bigint auto_increment
        primary key,
    expiry_date  datetime(6)  not null,
    new_password varchar(255) not null,
    token        varchar(255) not null,
    user_id      bigint       not null,
    constraint FK83nsrttkwkb6ym0anu051mtxn
        foreign key (user_id) references  users (id)
);

 create table if not exists tra_transaction
(
    tra_id             bigint auto_increment
        primary key,
    tra_created_at     datetime(6)  null,
    tra_payment_method varchar(255) null,
    tra_total_price    float        null,
    tra_user_id        bigint       null,
    constraint FKl7b0rhm4c4thxmwan7ftbwoj2
        foreign key (tra_user_id) references  users (id)
);

 create table if not exists ord_order
(
    ord_id             int auto_increment
        primary key,
    ord_billing_email  varchar(255) null,
    ord_created_at     datetime(6)  null,
    ord_date           date         null,
    ord_earnings       float        null,
    ord_quantity       int          null,
    ord_time           time         null,
    ord_transaction_id bigint       null,
    ord_updated_at     datetime(6)  null,
    ord_product_id     int          null,
    constraint FK633expfjam6i63rxdi6gbc3sa
        foreign key (ord_product_id) references  prd_product (prd_id),
    constraint FKtqbdgy9bddjrivf868bncuf2n
        foreign key (ord_transaction_id) references  tra_transaction (tra_id)
);

 create table if not exists prv_provision
(
    prv_id                int auto_increment
        primary key,
    prv_affiliate_id      int         null,
    prv_commission_amount float       null,
    prv_created_at        datetime(6) null,
    prv_transaction_id    bigint      null,
    constraint FK8l4qppuwtre9rejfmcqrqv1f1
        foreign key (prv_transaction_id) references  tra_transaction (tra_id),
    constraint FKj2r7iy6s6sklxsxdmpkxqbuky
        foreign key (prv_affiliate_id) references  aff_affiliate (aff_id)
);

 create table if not exists user_roles
(
    user_id bigint not null,
    role_id int    not null,
    primary key (user_id, role_id),
    constraint FKh8ciramu9cc9q3qcqiv4ue8a6
        foreign key (role_id) references  roles (id),
    constraint FKhfh9dx7w3ubf1co1vdev94g3f
        foreign key (user_id) references  users (id)
);

 create table if not exists users_transactions
(
    user_id             bigint not null,
    transactions_tra_id bigint not null,
    primary key (user_id, transactions_tra_id),
    constraint UK_tqv5g77oogwa9uq6fjsbf0qgf
        unique (transactions_tra_id),
    constraint FK8f5s2gcq0hqnt6uockuy1dvw6
        foreign key (transactions_tra_id) references  tra_transaction (tra_id),
    constraint FKj3y3d0uo8vpnm9s9ydvxrmehg
        foreign key (user_id) references  users (id)
);

