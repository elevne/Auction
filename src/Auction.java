import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Auction {
    private static Scanner scanner = new Scanner(System.in);
    private static String username;
    private static Connection conn;

    private static final String LOGIN_SQL = "SELECT * FROM AUCTION_USER WHERE USERNAME = ?";
    private static final String SIGNUP_SQL = "INSERT INTO " +
            " AUCTION_USER(user_id, username, password, is_admin) " +
            "VALUES " +
            "( " +
            " (SELECT COALESCE(MAX(USER_ID), 0) FROM AUCTION_USER) + 1, " +
            " ?, ?, ? );";
    private static final String ADMIN_LOGIN_SQL = "SELECT * FROM AUCTION_USER WHERE USERNAME = ? AND IS_ADMIN = 'Y';";
    private static final String ITEM_ID_SQL = "SELECT COALESCE(MAX(ITEM_ID), 0) + 1 AS ITEM_ID FROM ITEM;";
    private static final String SELL_ITEM_SQL = "INSERT INTO ITEM(ITEM_ID, CATEGORY_ID, DESCRIPTION, CONDITION_ID, SELLER_ID, BUY_NOW_PRICE, DATE_POSTED, STATUS)\n" +
            "VALUES (\n" +
            "        ?,\n" +
            "        ?,\n" +
            "        ?,\n" +
            "        ?,\n" +
            "        (SELECT USER_ID FROM AUCTION_USER WHERE USERNAME = ?),\n" +
            "        ?,\n" +
            "        ?,\n" +
            "        'Y'\n" +
            ");";
    private static final String BID_INSERT_SQL = "INSERT INTO BID(BID_ID, ITEM_ID, BID_PRICE, BIDDER_ID, DATE_POSTED, BID_CLOSING_DATE) VALUES \n" +
            "(\n" +
            " (SELECT COALESCE(MAX(BID_ID), 0) + 1 FROM BID),\n" +
            " ?,\n" +
            " null,\n" +
            " null,\n" +
            " ?,\n" +
            " ?\n" +
            ");";
    private static final String SELLING_ITEMS_SQL = "SELECT *, COALESCE((SELECT USERNAME FROM AUCTION_USER WHERE USER_ID = B.BIDDER_ID), 'No one yet') AS BIDDER_USERNAME\n" +
            "FROM AUCTION_USER\n" +
            "JOIN ITEM I on AUCTION_USER.USER_ID = I.SELLER_ID\n" +
            "JOIN BID B on I.ITEM_ID = B.ITEM_ID\n" +
            "WHERE USERNAME = ? AND BID_CLOSING_DATE > CURRENT_TIMESTAMP;";
    private static final String BUY_LIST_SQL = "SELECT\n" +
            "    I.ITEM_ID,\n" +
            "    I.DESCRIPTION,\n" +
            "    C.CONDITION_NAME,\n" +
            "    (SELECT USERNAME FROM AUCTION_USER WHERE USER_ID = I.SELLER_ID) AS SELLER,\n" +
            "    I.BUY_NOW_PRICE,\n" +
            "    B.BID_PRICE,\n" +
            "       COALESCE((SELECT USERNAME FROM AUCTION_USER WHERE USER_ID = B.BIDDER_ID), 'No one yet') AS HIGHEST_BIDDER,\n" +
            "       extract(year from BID_CLOSING_DATE - CURRENT_TIMESTAMP) || ','\n" +
            "           || extract(month from BID_CLOSING_DATE - CURRENT_TIMESTAMP) || ','\n" +
            "           || extract(day from BID_CLOSING_DATE - CURRENT_TIMESTAMP) || ','\n" +
            "           || extract(hour from BID_CLOSING_DATE - CURRENT_TIMESTAMP) || ','\n" +
            "           || extract(minute from BID_CLOSING_DATE - CURRENT_TIMESTAMP) || ',' AS TIME_LEFT,\n" +
            "    B.BID_CLOSING_DATE\n" +
            "FROM AUCTION_USER\n" +
            "JOIN ITEM I on AUCTION_USER.USER_ID = I.SELLER_ID\n" +
            "JOIN BID B on I.ITEM_ID = B.ITEM_ID\n" +
            "JOIN CONDITION C on I.CONDITION_ID = C.CONDITION_ID\n" +
            "JOIN CATEGORY C2 on I.CATEGORY_ID = C2.CATEGORY_ID\n" +
            "WHERE BID_CLOSING_DATE >= CURRENT_TIMESTAMP AND I.STATUS = 'Y'";
    private static final String ITEM_SELECT_SQL = "SELECT BID_PRICE,  BUY_NOW_PRICE,\n" +
            "       CASE\n" +
            "           WHEN CURRENT_TIMESTAMP > BID_CLOSING_DATE\n" +
            "           THEN 'TRUE'\n" +
            "           ELSE 'FALSE'\n" +
            "      END AS PASSED_DATE\n" +
            "FROM ITEM I\n" +
            "JOIN BID B on I.ITEM_ID = B.ITEM_ID\n" +
            "WHERE I.ITEM_ID = ?;";
    private static final String BIDDING_SQL = "UPDATE BID\n" +
            "SET BID_PRICE = ?, BIDDER_ID = (SELECT USER_ID FROM AUCTION_USER WHERE USERNAME = ?);";
    private static final String BID_HISTORY_INSERT_SQL = "INSERT INTO BID_HISTORY(BID_HISTORY_ID, BID_ID, BIDDER_ID, BID_PRICE, BID_DATE) \n" +
            "VALUES (\n" +
            "        (SELECT COALESCE(MAX(BID_HISTORY_ID), 0) + 1 FROM BID_HISTORY),\n" +
            "        (SELECT BID_ID FROM BID WHERE ITEM_ID = ?),\n" +
            "        (SELECT USER_ID FROM AUCTION_USER WHERE USERNAME = ?),\n" +
            "        ?,\n" +
            "        current_timestamp\n" +
            "       );";
    private static final String ITEM_STATUS_UPDATE_SQL = "UPDATE ITEM\n" +
            "SET STATUS = 'N'\n" +
            "WHERE ITEM_ID = ?;";
    private static final String INSERT_BILLING_SQL = "INSERT INTO BILLING(BILLING_ID, SOLD_ITEM_ID, PURCHASE_DATE, SELLER_ID, BUYER_ID, BUYER_PAY, SELLER_PAY)\n" +
            "VALUES (\n" +
            "        (SELECT COALESCE(MAX(BILLING_ID), 0) FROM BILLING) + 1,\n" +
            "        ?,\n" +
            "        CURRENT_TIMESTAMP,\n" +
            "        (SELECT USER_ID FROM ITEM JOIN AUCTION_USER AU on ITEM.SELLER_ID = AU.USER_ID WHERE ITEM_ID = ? ),\n" +
            "        (SELECT USER_ID FROM AUCTION_USER WHERE USERNAME = ?),\n" +
            "        ?,\n" +
            "        ?\n" +
            "       );";
    private static final String CHECK_BID_STATUS = "    SELECT B.ITEM_ID, DESCRIPTION, (SELECT USERNAME FROM AUCTION_USER WHERE USER_ID = B.BIDDER_ID) AS HIGHEST_BIDDER, B.BID_PRICE, H.BID_PRICE AS YOUR_PRICE, to_char(B.BID_CLOSING_DATE, 'YYYY-MM-DD HH24:MI') AS BID_CLOSING_DATE\n" +
            "    FROM BID_HISTORY H\n" +
            "    JOIN BID B on H.BID_ID = B.BID_ID\n" +
            "    JOIN ITEM I on B.ITEM_ID = I.ITEM_ID\n" +
            "    JOIN AUCTION_USER AU on H.BIDDER_ID = AU.USER_ID\n" +
            "    WHERE USER_ID = ?;";
    private static final String SOLD_ITEM_SQL = "SELECT CATEGORY_NAME, ITEM_ID, to_char(PURCHASE_DATE, 'YYYY-MM-DD HH24:MI') AS SOLD_DATE, BILLING.BUYER_PAY, BUYER_ID, BILLING.SELLER_PAY\n" +
            "FROM BILLING\n" +
            "JOIN ITEM I on BILLING.SOLD_ITEM_ID = I.ITEM_ID\n" +
            "JOIN CATEGORY C on I.CATEGORY_ID = C.CATEGORY_ID\n" +
            "WHERE BILLING.SELLER_ID = (SELECT USER_ID FROM AUCTION_USER WHERE USERNAME = ?);";
    private static final String PURCHASED_ITEM_SQL = "SELECT CATEGORY_NAME, ITEM_ID, to_char(PURCHASE_DATE, 'YYYY-MM-DD HH24:MI') AS SOLD_DATE, BILLING.BUYER_PAY, BILLING.SELLER_ID, BILLING.SELLER_PAY\n" +
            "FROM BILLING\n" +
            "JOIN ITEM I on BILLING.SOLD_ITEM_ID = I.ITEM_ID\n" +
            "JOIN CATEGORY C on I.CATEGORY_ID = C.CATEGORY_ID\n" +
            "WHERE BILLING.BUYER_ID = (SELECT USER_ID FROM AUCTION_USER WHERE USERNAME = ?);";
    enum Category {
        ELECTRONICS,
        BOOKS,
        HOME,
        CLOTHING,
        SPORTINGGOODS,
        OTHERS;

         public int getNum() {
            switch (this) {
                case ELECTRONICS:
                    return 1;
                case BOOKS:
                    return 2;
                case HOME:
                    return 3;
                case CLOTHING:
                    return 4;
                case SPORTINGGOODS:
                    return 5;
                case OTHERS:
                    return 6;
            }
            // will never return 0
            return 0;
        }
    }
    enum Condition {
        NEW,
        LIKE_NEW,
        GOOD,
        ACCEPTABLE;

        public int getNum() {
            switch (this) {
                case NEW:
                    return 1;
                case LIKE_NEW:
                    return 2;
                case GOOD:
                    return 3;
                case ACCEPTABLE:
                    return 4;
            }
            // will never return 0
            return 0;
        }
    }

    private static boolean LoginMenu() {
        String userpass, isAdmin;

        System.out.print("----< User Login >\n" +
                " ** To go back, enter 'back' in user ID.\n" +
                "     user ID: ");
        try{
            username = scanner.next();
            scanner.nextLine();
            // back 입력하면 뒤로 돌아갈 수 있음
            if (username.equalsIgnoreCase("back")) {
                return false;
            }

            System.out.print("     password: ");
            userpass = scanner.next();
            scanner.nextLine();
        } catch (InputMismatchException e) {
            System.out.println("Error: Invalid input is entered. Try again.");
            username = null;
            return false;
        }

        String userpass_check = null;
        boolean login_result = false;
        /* Your code should come here to check ID and password */
        try {

            PreparedStatement pstmt = conn.prepareStatement(LOGIN_SQL);
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                userpass_check = rs.getString("PASSWORD");
            }

            // id 조회가 안되는 경우
            if (userpass_check == null) {
                username = null;
                return false;
            }
            // id 가 조회되고 패스워드가 일치할 경우
            else if (userpass_check.equals(userpass)) login_result = true;
            // id 가 조회되지만 패스워드가 일치하지 않는 경우
            else System.out.println("Password is incorrect. Please try again");

            pstmt.close();
            rs.close();
        } catch (SQLException e) {
            // todo(3): 에러 메시지 출력 형식 찾아보기
            System.out.println("Error: SQL Error");
            return false;
        }

        if (!login_result) {
            /* If Login Fails */
            System.out.println("Error: Incorrect user name or password");
            return false;
        }
        System.out.println("You are successfully logged in.\n");
        return true;
    }

    private static boolean SellMenu() {
        Category category = null;
        Condition condition = null;
        String description;
        LocalDateTime dateTime;
        char choice;
        int price;
        boolean flag_catg = true, flag_cond = true;

        do{
            System.out.println(
                    "----< Sell Item >\n" +
                            "---- Choose a category.\n" +
                            "    1. Electronics\n" +
                            "    2. Books\n" +
                            "    3. Home\n" +
                            "    4. Clothing\n" +
                            "    5. Sporting Goods\n" +
                            "    6. Other Categories\n" +
                            "    P. Go Back to Previous Menu"
            );

            try {
                choice = scanner.next().charAt(0);;
            }catch (InputMismatchException e) {
                System.out.println("Error: Invalid input is entered. Try again.");
                continue;
            }

            flag_catg = true;

            switch ((int) choice){
                case '1':
                    category = Category.ELECTRONICS;
                    continue;
                case '2':
                    category = Category.BOOKS;
                    continue;
                case '3':
                    category = Category.HOME;
                    continue;
                case '4':
                    category = Category.CLOTHING;
                    continue;
                case '5':
                    category = Category.SPORTINGGOODS;
                    continue;
                case '6':
                    category = Category.OTHERS;
                    continue;
                case 'p':
                case 'P':
                    return false;
                default:
                    System.out.println("Error: Invalid input is entered. Try again.");
                    flag_catg = false;
                    continue;
            }
        } while (!flag_catg);

        do{
            System.out.println(
                    "---- Select the condition of the item to sell.\n" +
                            "   1. New\n" +
                            "   2. Like-new\n" +
                            "   3. Used (Good)\n" +
                            "   4. Used (Acceptable)\n" +
                            "   P. Go Back to Previous Menu"
            );

            try {
                choice = scanner.next().charAt(0);;
                scanner.nextLine();
            } catch (InputMismatchException e) {
                System.out.println("Error: Invalid input is entered. Try again.");
                continue;
            }

            flag_cond = true;

            switch (choice) {
                case '1':
                    condition = Condition.NEW;
                    break;
                case '2':
                    condition = Condition.LIKE_NEW;
                    break;
                case '3':
                    condition = Condition.GOOD;
                    break;
                case '4':
                    condition = Condition.ACCEPTABLE;
                    break;
                case 'p':
                case 'P':
                    return false;
                default:
                    System.out.println("Error: Invalid input is entered. Try again.");
                    flag_cond = false;
                    continue;
            }
        } while (!flag_cond);

        try {
            System.out.println("---- Description of the item (one line): ");
            description = scanner.nextLine();
            System.out.println("---- Buy-It-Now price: ");

            while (!scanner.hasNextInt()) {
                scanner.next();
                System.out.println("Invalid input is entered. Please enter Buy-It-Now price: ");
            }

            price = scanner.nextInt();
            scanner.nextLine();

            System.out.print("---- Bid closing date and time (YYYY-MM-DD HH:MM): ");
            // you may assume users always enter valid date/time
            String date = scanner.nextLine();  /* "2023-03-04 11:30"; */
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            dateTime = LocalDateTime.parse(date, formatter);
        } catch (Exception e) {
            System.out.println("Error: Invalid input is entered. Going back to the previous menu.");
            return false;
        }

        LocalDateTime currentDateTime = LocalDateTime.now();
        try {
            PreparedStatement idStmt = conn.prepareStatement(ITEM_ID_SQL);
            ResultSet rs1 = idStmt.executeQuery();
            int item_id = 0;
            while (rs1.next()) {
                item_id = rs1.getInt("ITEM_ID");
            }
            if (item_id == 0) throw new SQLException();

            PreparedStatement pstmt1 = conn.prepareStatement(SELL_ITEM_SQL);
            pstmt1.setInt(1, item_id);
            pstmt1.setInt(2, category.getNum());
            pstmt1.setString(3, description);
            pstmt1.setInt(4, condition.getNum());
            pstmt1.setString(5, username);
            pstmt1.setInt(6, price);
            pstmt1.setObject(7, currentDateTime);
            pstmt1.executeUpdate();

            PreparedStatement pstmt2 = conn.prepareStatement(BID_INSERT_SQL);
            pstmt2.setInt(1, item_id);
            pstmt2.setObject(2, currentDateTime);
            pstmt2.setObject(3, dateTime);
            pstmt2.executeUpdate();

            conn.commit();
            idStmt.close();
            rs1.close();
            pstmt1.close();
            pstmt2.close();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException e2) {}
            // todo (3): 발생할 수 있는 예외 다시 찾아보기
            System.out.println("Something went wrong while registering new item on Auction. Please try again.");
            return false;
        }

        System.out.println("Your item has been successfully listed.\n");
        return true;
    }

    private static boolean SignupMenu() {
        /* 2. Sign Up */
        String new_username, userpass, isAdmin;
        System.out.print("----< Sign Up >\n" +
                " ** To go back, enter 'back' in user ID.\n" +
                "---- user name: ");
        try {
            new_username = scanner.next();
            scanner.nextLine();
            if(new_username.equalsIgnoreCase("back")){
                return false;
            }
            System.out.print("---- password: ");
            userpass = scanner.next();
            scanner.nextLine();
            System.out.print("---- In this user an administrator? (Y/N): ");
            isAdmin = scanner.next();
            // isAdmin 에 올바른 값만 넣을 수 있도록
            if (!isAdmin.equals("Y") && !isAdmin.equals("N")) {
                throw new InputMismatchException();
            }
            scanner.nextLine();
        } catch (InputMismatchException e) {
            System.out.println("Error: Invalid input is entered. Please select again.");
            return false;
        }

        /* Your code should come here to create a user account in your database */
        try {
            PreparedStatement pstmt = conn.prepareStatement(SIGNUP_SQL);
            pstmt.setString(1, new_username);
            pstmt.setString(2, userpass);
            pstmt.setString(3, isAdmin);
            pstmt.executeUpdate();
            conn.commit();

            pstmt.close();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException e2) {}
            System.out.println("User ID " + new_username + " is already in use. Please use another id.");
            return false;
        }

        System.out.println("Your account has been successfully created.\n");
        return true;
    }

    private static boolean AdminMenu() {
        /* 3. Login as Administrator */
        char choice;
        String adminname, adminpass;
        String keyword, seller;
        System.out.print("----< Login as Administrator >\n" +
                " ** To go back, enter 'back' in user ID.\n" +
                "---- admin ID: ");

        try {
            adminname = scanner.next();
            scanner.nextLine();
            if(adminname.equalsIgnoreCase("back")){
                return false;
            }
            System.out.print("---- password: ");
            adminpass = scanner.nextLine();

            String password_check = null;
            try {
                PreparedStatement pstmt = conn.prepareStatement(LOGIN_SQL);
                pstmt.setString(1, adminname);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) password_check = rs.getString("PASSWORD");

                if (password_check == null) {
                    System.out.println("User is not found");
                    return false;
                } else if (!password_check.equals(adminpass)) {
                    System.out.println("Password is incorrect");
                    return false;
                }

                pstmt.close();
                rs.close();
            } catch (SQLException sqlException) {
                // todo (3): 이 부분 예외 있는지 다시 확인해보기
                System.out.println("Something went wrong whiel logging in. Please try again.");
                return false;
            }
        } catch (InputMismatchException e) {
            System.out.println("Error: Invalid input is entered. Try again.");
            return false;
        }

        do {
            System.out.println(
                    "----< Admin menu > \n" +
                            "    1. Print Sold Items per Category \n" +
                            "    2. Print Account Balance for Seller \n" +
                            "    3. Print Seller Ranking \n" +
                            "    4. Print Buyer Ranking \n" +
                            "    P. Go Back to Previous Menu"
            );

            try {
                choice = scanner.next().charAt(0);;
                scanner.nextLine();
            } catch (InputMismatchException e) {
                System.out.println("Error: Invalid input is entered. Try again.");
                continue;
            }

            if (choice == '1') {
                System.out.println("----Enter Category to search : ");
                keyword = scanner.next();
                scanner.nextLine();
                /*TODO: Print Sold Items per Category */
                System.out.println("sold item       | sold date       | seller ID   | buyer ID   | price | commissions");
                System.out.println("----------------------------------------------------------------------------------");
                                /*
                                   while(rset.next()){
                                   }
                                 */
                continue;
            } else if (choice == '2') {
                /*TODO: Print Account Balance for Seller */
                System.out.println("---- Enter Seller ID to search : ");
                seller = scanner.next();
                scanner.nextLine();
                System.out.println("sold item       | sold date       | buyer ID   | price | commissions");
                System.out.println("--------------------------------------------------------------------");
                                /*
                                   while(rset.next()){
                                   }
                                 */
                continue;
            } else if (choice == '3') {
                /*TODO: Print Seller Ranking */
                System.out.println("seller ID   | # of items sold | Total Profit (excluding commissions)");
                System.out.println("--------------------------------------------------------------------");
                                /*
                                   while(rset.next()){
                                   }
                                 */
                continue;
            } else if (choice == '4') {
                /*TODO: Print Buyer Ranking */
                System.out.println("buyer ID   | # of items purchased | Total Money Spent ");
                System.out.println("------------------------------------------------------");
                                /*
                                   while(rset.next()){
                                   }
                                 */
                continue;
            } else if (choice == 'P' || choice == 'p') {
                return false;
            } else {
                System.out.println("Error: Invalid input is entered. Try again.");
                continue;
            }
        } while(true);
    }

    public static void CheckSellStatus(){
        System.out.println("item listed in Auction | bidder (buyer ID) | bidding price | bidding date/time \n");
        System.out.println("-------------------------------------------------------------------------------\n");

        try {
            PreparedStatement pstmt = conn.prepareStatement(SELLING_ITEMS_SQL);
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            while (rs.next()) {
                String itemName = rs.getString("description");
                String bidder = rs.getString("bidder_username");
                int biddingPrice = rs.getInt("bid_price");
                LocalDateTime biddingDate = rs.getObject("bid_closing_date", LocalDateTime.class);
                String date = biddingDate.format(formatter);
                System.out.println(itemName + " | " + bidder + " | " + Integer.toString(biddingPrice) + " | " + date);
            }
            pstmt.close();
            rs.close();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (Exception e2) {}
            System.out.println("Failed to load status of items.");
        }
    }

    public static boolean BuyItem(){
        Category category = null;
        Condition condition = null;
        char choice;
        int price;
        String keyword, seller, datePosted;
        boolean flag_catg = true, flag_cond = true;

        do {

            System.out.println( "----< Select category > : \n" +
                    "    1. Electronics\n"+
                    "    2. Books\n" +
                    "    3. Home\n" +
                    "    4. Clothing\n" +
                    "    5. Sporting Goods\n" +
                    "    6. Other categories\n" +
                    "    7. Any category\n" +
                    "    P. Go Back to Previous Menu"
            );

            try {
                choice = scanner.next().charAt(0);;
                scanner.nextLine();
            } catch (InputMismatchException e) {
                System.out.println("Error: Invalid input is entered. Try again.");
                return false;
            }

            flag_catg = true;

            switch (choice) {
                case '1':
                    category = Category.ELECTRONICS;
                    break;
                case '2':
                    category = Category.BOOKS;
                    break;
                case '3':
                    category = Category.HOME;
                    break;
                case '4':
                    category = Category.CLOTHING;
                    break;
                case '5':
                    category = Category.SPORTINGGOODS;
                    break;
                case '6':
                    category = Category.OTHERS;
                    break;
                case '7':
                    break;
                case 'p':
                case 'P':
                    return false;
                default:
                    System.out.println("Error: Invalid input is entered. Try again.");
                    flag_catg = false;
                    continue;
            }
        } while(!flag_catg);

        do {

            System.out.println(
                    "----< Select the condition > \n" +
                            "   1. New\n" +
                            "   2. Like-new\n" +
                            "   3. Used (Good)\n" +
                            "   4. Used (Acceptable)\n" +
                            "   P. Go Back to Previous Menu"
            );
            try {
                choice = scanner.next().charAt(0);;
                scanner.nextLine();
            } catch (InputMismatchException e) {
                System.out.println("Error: Invalid input is entered. Try again.");
                return false;
            }

            flag_cond = true;

            switch (choice) {
                case '1':
                    condition = Condition.NEW;
                    break;
                case '2':
                    condition = Condition.LIKE_NEW;
                    break;
                case '3':
                    condition = Condition.GOOD;
                    break;
                case '4':
                    condition = Condition.ACCEPTABLE;
                    break;
                case 'p':
                case 'P':
                    return false;
                default:
                    System.out.println("Error: Invalid input is entered. Try again.");
                    flag_cond = false;
                    continue;
            }
        } while(!flag_cond);

        try {
            System.out.println("---- Enter keyword to search the description : ");
            keyword = scanner.next();
            scanner.nextLine();

            System.out.println("---- Enter Seller ID to search : ");
            System.out.println(" ** Enter 'any' if you want to see items from any seller. ");
            seller = scanner.next();
            scanner.nextLine();

            System.out.println("---- Enter date posted (YYYY-MM-DD): ");
            System.out.println(" ** This will search items that have been posted after the designated date.");
            datePosted = scanner.next();
            scanner.nextLine();
        } catch (InputMismatchException e) {
            System.out.println("Error: Invalid input is entered. Try again.");
            return false;
        }

        String buy_list_sql = BUY_LIST_SQL;
        if (category != null) {
            buy_list_sql += " AND I.CATEGORY_ID = ?";
        }
        buy_list_sql += " AND I.CONDITION_ID = ?";
        buy_list_sql += " AND I.DESCRIPTION LIKE '%' || ? ||'%'";
        buy_list_sql += " AND AUCTION_USER.USERNAME LIKE '%' || ? ||'%'";
        buy_list_sql += " AND I.DATE_POSTED >= ?";

        if (seller.equals("any")) {
            seller = "";
        }
        LocalDateTime dateTime = LocalDateTime.parse(datePosted + "T00:00");

        ResultSet rs = null;
        int cnt = 0;
        try (PreparedStatement pstmt = conn.prepareStatement(buy_list_sql))
        {
            if (category != null) {
                pstmt.setInt(1, category.getNum());
                pstmt.setInt(2, condition.getNum());
                pstmt.setString(3, keyword);
                pstmt.setString(4, seller);
                pstmt.setObject(5, dateTime);
            } else {
                pstmt.setInt(1, condition.getNum());
                pstmt.setString(2, keyword);
                pstmt.setString(3, seller);
                pstmt.setObject(4, dateTime);
            }
            rs = pstmt.executeQuery();

            System.out.println("Item ID | Item description | Condition | Seller | Buy-It-Now | Current Bid | highest bidder | Time left | bid close");
            System.out.println("-------------------------------------------------------------------------------------------------------");

            while (rs.next()) {
                cnt += 1;
                String itemId = Integer.toString(rs.getInt("item_id"));
                String description = rs.getString("description");
                String con = rs.getString("condition_name");
                String sell = rs.getString("seller");
                String buyNow = Integer.toString(rs.getInt("buy_now_price"));
                String curBid = Integer.toString(rs.getInt("bid_price"));
                String highestBidder = rs.getString("highest_bidder");
                String timeLeft = rs.getString("time_left");
                String[] timeLeftArr = timeLeft.split(",");
                String timeLeftRes = "";
                if (!timeLeftArr[0].equals("0")) timeLeftRes += timeLeftArr[0] + " year ";
                if (!timeLeftArr[1].equals("0")) timeLeftRes += timeLeftArr[1] + " month ";
                if (!timeLeftArr[2].equals("0")) timeLeftRes += timeLeftArr[2] + " day ";
                if (!timeLeftArr[3].equals("0")) timeLeftRes += timeLeftArr[3] + " hour ";
                if (!timeLeftArr[4].equals("0")) timeLeftRes += timeLeftArr[4] + " min";
                LocalDateTime bidClose = rs.getObject("bid_closing_date", LocalDateTime.class);
                System.out.println(itemId  + " | " + description + " | " + con + " | " + sell +  " | " + buyNow +  " | " + curBid +  " | " + highestBidder +  " | " + timeLeftRes +  " | " + bidClose.toString().replace("T", " ") );
            }
            rs.close();
        } catch (SQLException e) {
            System.out.println("Failed to load items from auction. Please tyy again.");
        }

        if (cnt == 0) {
            System.out.println("There are no items to show.");
            return false;
        }

        System.out.println("---- Select Item ID to buy or bid: ");
        try {
            choice = scanner.next().charAt(0);;
            scanner.nextLine();
            System.out.println("     Price: ");
            price = scanner.nextInt();
            scanner.nextLine();
        } catch (InputMismatchException e) {
            System.out.println("Error: Invalid input is entered. Try again.");
            return false;
        }

        // todo (1): 이 기능 테스트해보기
        // 전부 하나의 트랜잭션으로 처리하기 (위 조회는 같이 안해도 됨)
        // 1. choice 로 item, bid 조인해서 조회
        // 2. buy now price 와 price 비교해서 Price 가 더 높으면 곧바로 구매 처리 + bid 종료처리 + 아이템 판매 처리
        // 3. price 가 더 낮을 경우 bid 갱신 처리만 해주기
        try {
            PreparedStatement pstmt1 = conn.prepareStatement(ITEM_SELECT_SQL);
            pstmt1.setInt(1, choice);
            ResultSet rs1 = pstmt1.executeQuery();

            int rsCnt = 0;
            int bidPrice = 0;
            int buyNowPrice = 0;
            String passedDate = "";
            while (rs1.next()) {
                rsCnt++;
                bidPrice = rs1.getInt("bid_price");
                passedDate = rs1.getString("passed_date");
                buyNowPrice = rs1.getInt("buy_now_price");
            }
            if (rsCnt == 0) {
                System.out.println("Item with ID " + choice + " is not found. Please try again");
                return false;
            }
            if ("TRUE".equals(passedDate)) {
                System.out.println("Bid Ended.");
                return false;
            }

            if (price <= bidPrice) {
                System.out.println("You have to bid with price higher than current highest bid price: " + bidPrice);
                return false;
            }
            // 바로 구매
            else if (buyNowPrice <= price) {
                PreparedStatement pstmt2 = conn.prepareStatement(ITEM_STATUS_UPDATE_SQL);
                pstmt2.setInt(1, choice);
                PreparedStatement pstmt3 = conn.prepareStatement(INSERT_BILLING_SQL);
                pstmt3.setInt(1, choice);
                pstmt3.setInt(2, choice);
                pstmt3.setString(3, username);
                pstmt3.setInt(4, buyNowPrice);
                pstmt3.setInt(5,  buyNowPrice / 10);
                pstmt2.executeUpdate();
                pstmt3.executeUpdate();
                conn.commit();
                System.out.println("Congratulations, the item is yours now.\n");
                pstmt1.close();
                pstmt2.close();
                pstmt3.close();
                rs1.close();
            }
            // bid
            else {
                PreparedStatement pstmt2 = conn.prepareStatement(BIDDING_SQL);
                pstmt2.setInt(1, price);
                pstmt2.setString(2, username);
                pstmt2.executeUpdate();
                PreparedStatement pstmt3 = conn.prepareStatement(BID_HISTORY_INSERT_SQL);
                pstmt3.setInt(1, choice);
                pstmt3.setString(2, username);
                pstmt3.setInt(3, price);
                pstmt3.executeUpdate();
                conn.commit();
                System.out.println("Congratulations, you are the highest bidder.\n");
                pstmt1.close();
                rs1.close();
                pstmt2.close();
            }
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {}
            System.out.println("Failed to buy item. Please try again.");
        }
        return true;
    }

    public static void CheckBuyStatus(){
        System.out.println("item ID   | item description   | highest bidder | highest bidding price | your bidding price | bid closing date/time");
        System.out.println("--------------------------------------------------------------------------------------------------------------------");
        try {
            PreparedStatement pstmt = conn.prepareStatement(CHECK_BID_STATUS);
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String itemId = Integer.toString(rs.getInt("item_id"));
                String description = rs.getString("description");
                String highestBidder = rs.getString("highest_bidder");
                String highestBidPrice = Integer.toString(rs.getInt("bid_price"));
                String yourPrice = Integer.toString(rs.getInt("your_price"));
                String closingDate = rs.getString("bid_closing_date");
                System.out.println(
                        itemId + " | " + description + " | " + highestBidder + " | " + highestBidPrice + " | " + yourPrice + " | " + closingDate
                );
            }
        } catch (SQLException e) {
            System.out.println("Failed to load bid status. Please try again");
            return;
        }
    }

    public static void CheckAccount(){
        try {
            System.out.println("[Sold Items] \n");
            System.out.println("item category  | item ID   | sold date | sold price  | buyer ID | commission  ");
            System.out.println("------------------------------------------------------------------------------");
            PreparedStatement pstmt1 = conn.prepareStatement(SOLD_ITEM_SQL);
            pstmt1.setString(1, username);
            ResultSet rs1 = pstmt1.executeQuery();
            while (rs1.next()) {
                String category = rs1.getString("category_name");
                String itemId = Integer.toString(rs1.getInt("item_id"));
                String soldDate = rs1.getString("sold_date");
                String soldPrice = Integer.toString(rs1.getInt("buyer_pay"));
                String buyerId = rs1.getString("buyer_id");
                String commission = Integer.toString(rs1.getInt("seller_pay"));
                System.out.println(category + " | " + itemId + " | " + soldDate + " | " + soldPrice + " | " + buyerId + " | " + commission);
            }

            System.out.println("[Purchased Items] \n");
            System.out.println("item category  | item ID   | purchased date | puchased price  | seller ID ");
            System.out.println("--------------------------------------------------------------------------");
            PreparedStatement pstmt2 = conn.prepareStatement(PURCHASED_ITEM_SQL);
            pstmt2.setString(1, username);
            ResultSet rs2 = pstmt2.executeQuery();
            while (rs2.next()) {
                String category = rs1.getString("category_name");
                String itemId = Integer.toString(rs1.getInt("item_id"));
                String soldDate = rs1.getString("sold_date");
                String soldPrice = Integer.toString(rs1.getInt("buyer_pay"));
                String sellerId = rs1.getString("seller_id");
                System.out.println(category + " | " + itemId + " | " + soldDate + " | " + soldPrice + " | " + sellerId);
            }
        } catch (SQLException e) {
            System.out.println("Failed to check account info. Please try again");
            return;
        }

    }

    public static void main(String[] args) {
        char choice;
        boolean ret;

        if (args.length < 2) {
            System.out.println("Usage: java Auction postgres_id password");
            System.exit(1);
        }


        try{
            // todo(3): connection 만들 때 인자로 넘어오는 args 사용해서 아래와 같이 작성하기
            //  conn = DriverManager.getConnection("jdbc:postgresql://localhost/"+args[0], args[0], args[1]);
            conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/auction", "postgres", "1234");
            conn.setAutoCommit(false);
        }
        catch(SQLException e){
            System.out.println("SQLException : " + e);
            System.exit(1);
        }

        do {
            username = null;
            System.out.println(
                    "----< Login menu >\n" +
                            "----(1) Login\n" +
                            "----(2) Sign up\n" +
                            "----(3) Login as Administrator\n" +
                            "----(Q) Quit"
            );

            try {
                choice = scanner.next().charAt(0);;
                scanner.nextLine();
            } catch (InputMismatchException e) {
                System.out.println("Error: Invalid input is entered. Try again.");
                continue;
            }

            try {
                switch ((int) choice) {
                    case '1':
                        // 완료
                        ret = LoginMenu();
                        if (!ret) continue;
                        break;
                    case '2':
                        // 완료
                        ret = SignupMenu();
                        if (!ret) continue;
                        break;
                    case '3':
                        // todo (2): 어드민 메뉴에서 사용되는 기능들은 다른 거 구현한 다음 구현하기
                        ret = AdminMenu();
                        if (!ret) continue;
                    case 'q':
                    case 'Q':
                        System.out.println("Good Bye");
                        /* TODO: close the connection and clean up everything here (뭘 더 해야 하는 거?)*/
                        conn.close();
                        System.exit(0);
                    default:
                        System.out.println("Error: Invalid input is entered. Try again.");
                }
            } catch (SQLException e) {
                System.out.println("SQLException : " + e);
            }
        } while (username==null || username.equalsIgnoreCase("back"));

        // logged in as a normal user
        do {
            System.out.println(
                    "---< Main menu > :\n" +
                            "----(1) Sell Item\n" +
                            "----(2) Status of Your Item Listed on Auction\n" +
                            "----(3) Buy Item\n" +
                            "----(4) Check Status of your Bid \n" +
                            "----(5) Check your Account \n" +
                            "----(Q) Quit"
            );

            try {
                choice = scanner.next().charAt(0);;
                scanner.nextLine();
            } catch (InputMismatchException e) {
                System.out.println("Error: Invalid input is entered. Try again.");
                continue;
            }

            try{
                switch (choice) {
                    case '1':
                        // 완료
                        ret = SellMenu();
                        if(!ret) continue;
                        break;
                    case '2':
                        // 완료
                        CheckSellStatus();
                        break;
                    case '3':
                        // 테스트 필요
                        ret = BuyItem();
                        if(!ret) continue;
                        break;
                        // todo: case 4, 5 bid 시간 종료된거 체크하고 구매처리하는 거 하기
                    case '4':
                        // 테스트 필요 (수정 필요한지 다시 생각해보기)
                        CheckBuyStatus();
                        break;
                    case '5':
                        // 테스트 필요
                        CheckAccount();
                        break;
                    case 'q':
                    case 'Q':
                        System.out.println("Good Bye");
                        /* TODO: close the connection and clean up everything here (뭘 더 해야 하는 거?) */
                        conn.close();
                        System.exit(0);
                }
            } catch (SQLException e) {
                System.out.println("SQLException : " + e);
                System.exit(1);
            }
        } while(true);
    } // End of main
} // End of class
