-- liquibase formatted sql

-- changeset tin:init-migration
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
    s3_url              nvarchar(512) null,
	country             varchar(512) null,
	is_require_kyc      bit          not null,
    constraint UK6dotkott2kjsp8vw4d0m25fb7
        unique (email),
    constraint UK_opaau0844qqbnof1rnx2gevxt
        unique (usr_affiliate_link)
);

 create table if not exists prd_product
(
    prd_id           bigint auto_increment
        primary key,
    prd_created_at   datetime(6)   null,
    prd_description  varchar(255)  null,
    prd_name         varchar(255)  null,
	prd_user_id      bigint        null,
    prd_s3key        varchar(255)  null,
    prd_price        decimal(12,2) null,
    prd_stock        int           null,
    prd_updated_at   datetime(6)   null,
	prd_link		 varchar(255)  null,
	constraint prd_user_id_match
        foreign key (prd_user_id) references  users (id)
);

 create table if not exists wlt_wallet
(
    wlt_id           bigint auto_increment
        primary key,
	wlt_owner_id     bigint        null,
	wlt_type         varchar(50)   null,
	wlt_address      varchar(255)  null,
	wlt_private_key  varchar(255)  null,
	wlt_nonce        varbinary(50) null,
    wlt_created_at   datetime(6)   null,
	constraint wlt_user_match
        foreign key (wlt_owner_id) references  users (id)
);

 create table if not exists hierarchy
(
    id                bigint auto_increment
        primary key,
    commission_rate   decimal(7,6) null,
    created_at        datetime(6)  null,
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
    confirmed bit           null,
    data      blob          null,
    type      varchar(255)  null,
    user_id   bigint        null,
	s3_key    nvarchar(512) null,
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

 create table if not exists inv_invoice
(
    inv_id             bigint auto_increment 
	    primary key,
	inv_link           varchar(63)    null,
	inv_email          varchar(70)    null,
	inv_name           varchar(120)   null,
	inv_company        varchar(255)   null,
	inv_address         varchar(255)   null,
	inv_tax_number     varchar(255)   null,
    inv_created_at     datetime(6)    null,
	inv_paid_at        datetime(6)    null,
	inv_price          decimal(12, 2) null,
	inv_user_id        bigint         null,
    constraint unique_inv_link
        unique (inv_link),
    constraint inv_user_id_match
		foreign key (inv_user_id) references  users (id)
);

 create table if not exists ord_order
(
    ord_id             bigint auto_increment
        primary key,
    ord_created_at     datetime(6)   null,
	ord_finished_at    datetime(6)   null,
	ord_updated_at     datetime(6)   null,
    ord_quantity       int           null,
	ord_total_price    decimal(12,2) null,
    ord_product_id     bigint        null,
	ord_invoice_id     bigint        null,
	ord_seller_id      bigint        null,
	ord_currency       varchar(255)  null,
	ord_stablecoin	   varchar(255)  null,
	ord_status         varchar(255)  null,
    constraint ord_product_id_match
        foreign key (ord_product_id) references  prd_product (prd_id),
	constraint ord_invoice_id_match
        foreign key (ord_invoice_id) references  inv_invoice (inv_id),
	constraint seller_id_match
		foreign key (ord_seller_id) references  users (id)
);

 create table if not exists tra_transaction
(
    tra_id             bigint auto_increment
        primary key,
	tra_order_id	      bigint        null,
	tra_contract_address  varchar(255)  null,
	tra_blockchain	      varchar(255)  null,
	tra_status		      varchar(255)  null,
	tra_gas_price	      bigint        null,
	tra_gas_used	      bigint        null,
	tra_currency_value    bigint  	    null,
	tra_seller_wallet     bigint        null,
	tra_affiliate_wallet  bigint        null,
	tra_broker_wallet	  bigint        null,
	tra_senior_broker_wallet bigint     null,
	tra_leader_wallet     bigint        null,
	tra_buyer_wallet      bigint        null,
	tra_seller_amount	  bigint        null,
	tra_affiliate_amount  bigint        null,
	tra_broker_amount	  bigint        null,
	tra_senior_broker_amount bigint     null,
	tra_leader_amount	  bigint        null,
	tra_owner_amount	  bigint        null,
	tra_swapped_amount	  bigint        null,
    constraint order_id_match
        foreign key (tra_order_id) references  ord_order (ord_id),
	constraint seller_wallet_match
		foreign key (tra_seller_wallet) references  wlt_wallet (wlt_id),
	constraint affiliate_wallet_match
		foreign key (tra_affiliate_wallet) references  wlt_wallet (wlt_id),
	constraint broker_wallet_match
		foreign key (tra_broker_wallet) references  wlt_wallet (wlt_id),
	constraint senior_broker_wallet_match
		foreign key (tra_senior_broker_wallet) references  wlt_wallet (wlt_id),
	constraint leader_wallet_match
		foreign key (tra_leader_wallet) references  wlt_wallet (wlt_id),
	constraint buyer_wallet_match
		foreign key (tra_buyer_wallet) references  wlt_wallet (wlt_id)
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

