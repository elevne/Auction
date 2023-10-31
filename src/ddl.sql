CREATE TABLE AUCTION_USER (
                              USER_ID int4 primary key,
                              USERNAME varchar(255) unique,
                              PASSWORD varchar(255),
                              IS_ADMIN char(1) CHECK ( IS_ADMIN IN ('Y', 'N') )
);

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


CREATE TABLE BID (
                     BID_ID int4 primary key,
                     ITEM_ID int4,
                     BID_PRICE int4,
                     BIDDER_ID int4,
                     DATE_POSTED timestamp,
                     BID_CLOSING_DATE timestamp,
                     FOREIGN KEY (ITEM_ID) REFERENCES ITEM,
                     FOREIGN KEY (BIDDER_ID) REFERENCES AUCTION_USER(USER_ID)
);

CREATE TABLE BID_HISTORY (
                             BID_HISTORY_ID int4 primary key ,
                             BID_ID int4,
                             BIDDER_ID int4,
                             BID_PRICE int4,
                             BID_DATE timestamp,
                             FOREIGN KEY (BID_ID) REFERENCES BID,
                             FOREIGN KEY (BIDDER_ID) REFERENCES AUCTION_USER(USER_ID)
);


CREATE TABLE BILLING (
                         BILLING_ID int4 primary key ,
                         SOLD_ITEM_ID int4 ,
                         PURCHASE_DATE timestamp,
                         SELLER_ID int4,
                         BUYER_ID int4,
                         BUYER_PAY int4,
                         SELLER_PAY int4,
                         FOREIGN KEY (SOLD_ITEM_ID) REFERENCES ITEM(ITEM_ID),
                         FOREIGN KEY (SELLER_ID) REFERENCES AUCTION_USER(USER_ID),
                         FOREIGN KEY (BUYER_ID) REFERENCES AUCTION_USER(USER_ID)
);