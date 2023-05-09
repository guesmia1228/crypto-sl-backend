-- changeset tin:add-data-users
INSERT INTO users (usr_active, usr_affiliate_link, business, usr_created_at, email, first_name, last_name, mfa, password, profile_picturepath, profilepic, usr_reset_tokem, secret, tel, usr_token, usr_updated_at)
VALUES (1, '88889990', 'Example Corp', NOW(), 'diamondplus@example.com', 'John', 'Doe', 0, '$2a$10$3vVPnvEICwoGxYsIAI5tjeHDuebhIIEFlYu/I6So3Qy9V6RrZQemC', '/images/profiles/johndoe.jpg', NULL, NULL, NULL, '555-1234', 'abcd1234', NOW());
