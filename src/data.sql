/* 유저(ADMIN) 생성 */
INSERT INTO
    AUCTION_USER(user_id, username, password, is_admin)
VALUES
    (
            (SELECT COALESCE(MAX(USER_ID), 0) FROM AUCTION_USER) + 1,
            'user_a', '1234', 'Y'
    );
/* 일반 유저 생성 */
INSERT INTO
    AUCTION_USER(user_id, username, password, is_admin)
VALUES
    (
            (SELECT COALESCE(MAX(USER_ID), 0) FROM AUCTION_USER) + 1,
            'user_b', '1234', 'N'
    );
INSERT INTO
    AUCTION_USER(user_id, username, password, is_admin)
VALUES
    (
            (SELECT COALESCE(MAX(USER_ID), 0) FROM AUCTION_USER) + 1,
            'user_c', '1234', 'N'
    );

/* 카테고리 테이블 정보 입력 */
INSERT INTO CATEGORY(CATEGORY_ID, CATEGORY_NAME)
VALUES (1, 'Electronics'), (2, 'Books'), (3, 'Home'), (4, 'Clothing'), (5, 'Sporting Goods'), (6, 'Other Categories');
/* 컨디션 테이블 정보 입력 */
INSERT INTO CONDITION(CONDITION_ID, CONDITION_NAME)
VALUES (1, 'New'), (2, 'Like-new'), (3, 'Used (Good)'), (4, 'Used (Acceptable)');


/* 판매 아이템 입력 */
INSERT INTO ITEM(ITEM_ID, CATEGORY_ID, DESCRIPTION, CONDITION_ID, SELLER_ID, BUY_NOW_PRICE, DATE_POSTED, STATUS)
VALUES (
           1,
           1,
           'Macbook',
           2,
           (SELECT USER_ID FROM AUCTION_USER WHERE USERNAME = 'user_b'),
           100000,
           current_timestamp,
           'Y'
       );
INSERT INTO BID(BID_ID, ITEM_ID, BID_PRICE, BIDDER_ID, DATE_POSTED, BID_CLOSING_DATE) VALUES
    (
        (SELECT COALESCE(MAX(BID_ID), 0) + 1 FROM BID),
        1,
        null,
        null,
        current_timestamp,
        current_timestamp + interval '1 hour'
    );

INSERT INTO ITEM(ITEM_ID, CATEGORY_ID, DESCRIPTION, CONDITION_ID, SELLER_ID, BUY_NOW_PRICE, DATE_POSTED, STATUS)
VALUES (
           2,
           1,
           'ipad',
           3,
           (SELECT USER_ID FROM AUCTION_USER WHERE USERNAME = 'user_b'),
           88000,
           current_timestamp,
           'Y'
       );
INSERT INTO BID(BID_ID, ITEM_ID, BID_PRICE, BIDDER_ID, DATE_POSTED, BID_CLOSING_DATE) VALUES
    (
        (SELECT COALESCE(MAX(BID_ID), 0) + 1 FROM BID),
        2,
        null,
        null,
        current_timestamp,
        current_timestamp + interval '3 hour'
    );

INSERT INTO ITEM(ITEM_ID, CATEGORY_ID, DESCRIPTION, CONDITION_ID, SELLER_ID, BUY_NOW_PRICE, DATE_POSTED, STATUS)
VALUES (
           3,
           2,
           'Database Intro',
           3,
           (SELECT USER_ID FROM AUCTION_USER WHERE USERNAME = 'user_c'),
           33000,
           current_timestamp,
           'Y'
       );
INSERT INTO BID(BID_ID, ITEM_ID, BID_PRICE, BIDDER_ID, DATE_POSTED, BID_CLOSING_DATE) VALUES
    (
        (SELECT COALESCE(MAX(BID_ID), 0) + 1 FROM BID),
        3,
        null,
        null,
        current_timestamp,
        current_timestamp + interval '3 hour'
    );

INSERT INTO ITEM(ITEM_ID, CATEGORY_ID, DESCRIPTION, CONDITION_ID, SELLER_ID, BUY_NOW_PRICE, DATE_POSTED, STATUS)
VALUES (
           4,
           2,
           'Java Intro',
           3,
           (SELECT USER_ID FROM AUCTION_USER WHERE USERNAME = 'user_a'),
           23000,
           current_timestamp,
           'Y'
       );
INSERT INTO BID(BID_ID, ITEM_ID, BID_PRICE, BIDDER_ID, DATE_POSTED, BID_CLOSING_DATE) VALUES
    (
        (SELECT COALESCE(MAX(BID_ID), 0) + 1 FROM BID),
        3,
        22000,
        2,
        current_timestamp - interval '2 hour',
        current_timestamp
    );
INSERT INTO BID_HISTORY(BID_HISTORY_ID, BID_ID, BIDDER_ID, BID_PRICE, BID_DATE)
VALUES (
           (SELECT COALESCE(MAX(BID_HISTORY_ID), 0) + 1 FROM BID_HISTORY),
           (SELECT BID_ID FROM BID WHERE ITEM_ID = 4),
           2,
           22000,
           current_timestamp
       );