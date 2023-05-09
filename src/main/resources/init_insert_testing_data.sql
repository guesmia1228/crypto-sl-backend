
INSERT INTO users (usr_active, usr_affiliate_link, business, usr_created_at, email, first_name, last_name, mfa, password, profile_picturepath, profilepic, usr_reset_tokem, secret, tel, usr_token, usr_updated_at)
VALUES (1, '88889990', 'Example Corp', NOW(), 'diamondplus@example.com', 'John', 'Doe', 0, '$2a$10$3vVPnvEICwoGxYsIAI5tjeHDuebhIIEFlYu/I6So3Qy9V6RrZQemC', '/images/profiles/johndoe.jpg', NULL, NULL, NULL, '555-1234', 'abcd1234', NOW());


INSERT INTO tra_transaction(tra_created_at, tra_payment_method, tra_total_price, tra_user_id) VALUES
('2023-04-29 15:23:59.000000', 'QR', 12, 3 ),
('2023-04-29 15:24:59.000000', 'QR', 28, 3 ),
('2023-04-29 15:25:59.000000', 'QR', 24, 10 ),
('2023-04-29 15:27:59.000000', 'BANKING', 67, 10 ),
('2023-04-30 15:28:59.000000', 'QR', 34, 10 ),
('2023-04-30 15:20:59.000000', 'QR', 22, 10 ),
('2023-04-30 15:28:59.000000', 'QR', 24, 10 ),
('2023-05-01 15:21:59.000000', 'QR', 14, 7 ),
('2023-05-01 15:22:59.000000', 'QR', 15, 7 ),
('2023-05-01 15:23:59.000000', 'QR', 19, 6 ),
('2023-05-01 15:25:59.000000', 'QR', 24, 8 ),
('2023-05-02 15:26:59.000000', 'QR', 10, 8 ),
('2023-05-02 15:27:59.000000', 'QR', 12, 6 ),
('2023-05-02 15:29:59.000000', 'QR', 22, 3 ),
('2023-05-03 15:23:59.000000', 'QR', 18, 2 ),
('2023-05-03 15:24:59.000000', 'QR', 22, 2 ),
('2023-05-03 15:21:59.000000', 'QR', 12,  3 );

INSERT INTO hierarchy(commission_rate, created_at,relationship_type, child_id, parent_id ) VALUES
(1.25, '2023-04-26 11:01:45.485806', 'DIAMOND', 3, 2 ),
(1.25, '2023-04-27 11:01:45.485806', 'DIAMOND', 4, 3 ),
(1, '2023-04-29 10:36:37.000000', 'GOLD',  7 , 6 ),
(1.25, '2023-04-29 11:20:17.785508', 'GOLD',  8 , 6 ),
(1.25, '2023-04-29 11:42:42.962951', 'DIAMOND', 9 , 6 ),
(1.25, '2023-04-30 15:11:19.449798', 'GOLD', 10 , 6 );

INSERT INTO link_counter(timestamp, link_user) VALUES
('2023-04-26 11:01:45.485806', 3),
('2023-04-26 11:00:45.485806', 7),
('2023-04-26 11:01:45.485806', 6),
('2023-04-27 11:03:45.485806', 3),
('2023-04-28 11:02:45.485806', 7),
('2023-04-28 11:01:45.485806', 10),
('2023-04-28 11:09:45.485806', 10),
(CURRENT_TIMESTAMP, 6);

INSERT INTO affiliate_counter(timestamp, aff_c_id) VALUES
('2023-04-26 11:01:45.485806', 3),
('2023-04-26 11:05:45.485806', 3),
('2023-04-26 11:01:45.485806', 6),
('2023-04-27 11:02:45.485806', 6),
('2023-04-28 11:02:45.485806', 7),
('2023-04-28 11:01:45.485806', 10),
(CURRENT_TIMESTAMP, 10),
(CURRENT_TIMESTAMP, 10),
(CURRENT_TIMESTAMP, 10),
(CURRENT_TIMESTAMP, 6);

INSERT INTO aff_affiliate(aff_affiliate_link, aff_commission_rate, aff_created_at, aff_user_id) VALUES
('https://superstore.com/?ref=1234', 1.23, '2023-04-28 11:02:45.485806', 3),
('https://teststore.com', 1.23, '2023-04-28 11:05:45.485806', 6),
('https://test1store.com', 1.24, '2023-04-28 11:07:45.485806', 7),
('https://test2store.com', 1.25, '2023-04-29 11:08:45.485806', 7),
('https://test3store.com', 1.28, '2023-04-29 11:09:45.485806', 7),
('https://miniomstore.com/?ref=1222', 1.20, '2023-04-30 11:10:45.485806', 10),
('https://kingdom.com/?ref=1124', 1.20, CURRENT_TIMESTAMP, 10),
('https://hellokitty.com/?ref=1234', 1.20, CURRENT_TIMESTAMP, 3);


INSERT INTO prv_provision(prv_affiliate_id, prv_commission_amount, prv_created_at, prv_transaction_id) VALUES
(2, 1.23, '2023-04-28 11:05:45.485806', 10),
(1, 1.23, '2023-04-29 11:06:45.485806', 9),
(4, 1.23, '2023-04-28 11:07:45.485806', 30),
(4, 1.23, '2023-04-28 11:08:45.485806', 29),
(3, 1.23, '2023-04-28 11:09:45.485806', 19),
(1, 1.23, '2023-04-28 11:00:45.485806', 18),
(1, 1.25, CURRENT_TIMESTAMP, 23);

INSERT INTO prd_product(prd_created_at, prd_description, prd_name, prd_picture_path, prd_price, prd_stock, prd_updated_at) VALUES
('2023-04-28 11:05:45.485806', 'toy', 'superman','https://miniomstore.com/?ref=1222',12, 13, CURRENT_TIMESTAMP ),
('2023-04-28 11:03:45.485806', 'food', 'UIF','https://test1store.com/?ref=1222',18, 5, CURRENT_TIMESTAMP ),
('2023-04-28 11:06:45.485806', 'food', 'Nisian','https://test1store.com/?ref=1222',12, 1, CURRENT_TIMESTAMP ),
('2023-04-28 11:07:45.485806', 'car', 'Kotlin','https://test2store.com/?ref=1222',50, 10, CURRENT_TIMESTAMP )

INSERT INTO ord_order( ord_billing_email, ord_created_at, ord_date, ord_earnings, ord_quantity, ord_time, ord_transaction_id, ord_updated_at , ord_product_id ) VALUES
('test@gmail.com', '2023-04-28 11:05:45.485806', '2023-04-28', 12, 2, '11:05:45.485806', 2, NULL, 1 ),
('nguyentrongtin89@gmail.com', '2023-04-29 11:05:45.485806', '2023-04-29', 19, 10, '11:05:45.485806', 2, NULL, 2 ),
('diamond@example.com', '2023-04-30 11:05:45.485806', '2023-04-30', 14, 10, '11:05:45.485806', 2, NULL, 3 ),
('canh1@gmail.com', '2023-04-30 11:05:45.485806', '2023-04-30', 9, 10, '11:05:45.485806', 2, NULL, 4 ),
('nguyenhuuca@gmail.com', '2023-04-30 11:06:45.485806', '2023-04-30', 6, 10, '11:06:45.485806', 2, NULL, 3 );


