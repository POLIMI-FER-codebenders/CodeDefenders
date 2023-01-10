ALTER TABLE `users`
    CHANGE `Token` `Frontend_Token` CHAR(32) DEFAULT NULL;
ALTER TABLE users
    ADD `API_Token` char(32) DEFAULT NULL;
