ALTER TABLE users
	ADD marketing_updates        bit            not null,
	ADD email_notifications      bit            not null,
	ADD app_notifications        bit            not null,
	ADD notification_language    varchar(255)   null,
	ADD enable_invoicing         bit            not null