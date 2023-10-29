DROP TABLE AUCTION_USER;

// 유저 테이블 DDL
CREATE TABLE AUCTION_USER (
                              USER_ID int4 primary key,
                              USERNAME varchar(255) unique,
                              PASSWORD varchar(255),
                              IS_ADMIN char(1) CHECK ( IS_ADMIN IN ('Y', 'N') )
);

// 로그인 시 사용되는 쿼리
SELECT * FROM AUCTION_USER WHERE USERNAME = ?;

// 회원가입 쿼리
INSERT INTO
    AUCTION_USER(user_id, username, password, is_admin)
VALUES
    (
            (SELECT COALESCE(MAX(USER_ID), 0) FROM AUCTION_USER) + 1,
            ?, ?, ?
    );

// 어드민 로그인 시 사용되는 쿼리
SELECT * FROM AUCTION_USER WHERE USERNAME = ?  AND IS_ADMIN = 'Y';


// 아이템 관련 테이블 DDL
CREATE TABLE CATEGORY (
                          CATEGORY_ID int2 primary key,
                          CATEGORY_NAME varchar(20)
);
CREATE TABLE CONDITION (
                           CONDITION_ID int2 primary key,
                           CONDITION_NAME varchar(20)
);
CREATE TABLE ITEM (
                      ITEM_ID int4 primary key,
                      CATEGORY_ID int2,
                      DESCRIPTION varchar(500),
                      CONDITION_ID int2,
                      SELLER_ID int4,
                      BUY_NOW_PRICE int4,
                      DATE_POSTED timestamp,
                      STATUS char(1),
                      FOREIGN KEY (CATEGORY_ID) references CATEGORY,
                      FOREIGN KEY (CONDITION_ID) references CONDITION,
                      FOREIGN KEY (SELLER_ID) references AUCTION_USER(USER_ID)
);

INSERT INTO CATEGORY(CATEGORY_ID, CATEGORY_NAME)
VALUES (1, 'Electronics'), (2, 'Books'), (3, 'Home'), (4, 'Clothing'), (5, 'Sporting Goods'), (6, 'Other Categories');

INSERT INTO CONDITION(CONDITION_ID, CONDITION_NAME)
VALUES (1, 'New'), (2, 'Like-new'), (3, 'Used (Good)'), (4, 'Used (Acceptable)');

INSERT INTO ITEM(ITEM_ID, CATEGORY_ID, DESCRIPTION, CONDITION_ID, SELLER_ID, BUY_NOW_PRICE, DATE_POSTED, STATUS)
VALUES (
               (SELECT COALESCE(MAX(ITEM_ID), 0) FROM ITEM) + 1,
               ?,
               ?,
               ?,
               (SELECT USER_ID FROM AUCTION_USER WHERE USERNAME = ?),
               ?,
               current_timestamp,
               'Y'
       );

CREATE TABLE BID (
                     BID_ID int4 primary key,
                     ITEM_ID int4,
                     BID_PRICE int4,
                     BIDDER_ID int4,
                     DATE_POSTED timestamp,
                     BID_CLOSING_DATE timestamp,
                     FOREIGN KEY (ITEM_ID) REFERENCES ITEM,
                     FOREIGN KEY (BIDDER_ID) REFERENCES AUCTION_USER(USER_ID)
)