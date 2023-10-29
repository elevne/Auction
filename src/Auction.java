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
            "WHERE USERNAME = ?;";


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
        Category category;
        Condition condition;
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

        /* TODO: Query condition: item category */
        /* TODO: Query condition: item condition */
        /* TODO: Query condition: items whose description match the keyword (use LIKE operator) */
        /* TODO: Query condition: items from a particular seller */
        /* TODO: Query condition: posted date of item */

        /* TODO: List all items that match the query condition */
        System.out.println("Item ID | Item description | Condition | Seller | Buy-It-Now | Current Bid | highest bidder | Time left | bid close");
        System.out.println("-------------------------------------------------------------------------------------------------------");
                /*
                   while(rset.next()){
                   }
                 */

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

        /* TODO: Buy-it-now or bid: If the entered price is higher or equal to Buy-It-Now price, the bid ends. */
        /* Even if the bid price is higher than the Buy-It-Now price, the buyer pays the B-I-N price. */

        /* TODO: if you won, print the following */
        System.out.println("Congratulations, the item is yours now.\n");
        /* TODO: if you are the current highest bidder, print the following */
        System.out.println("Congratulations, you are the highest bidder.\n");
        return true;
    }

    public static void CheckBuyStatus(){
        /* TODO: Check the status of the item the current buyer is bidding on */
        /* Even if you are outbidded or the bid closing date has passed, all the items this user has bidded on must be displayed */

        System.out.println("item ID   | item description   | highest bidder | highest bidding price | your bidding price | bid closing date/time");
        System.out.println("--------------------------------------------------------------------------------------------------------------------");
                /*
                   while(rset.next(){
                   System.out.println();
                   }
                 */
    }

    public static void CheckAccount(){
        /* TODO: Check the balance of the current user.  */
        System.out.println("[Sold Items] \n");
        System.out.println("item category  | item ID   | sold date | sold price  | buyer ID | commission  ");
        System.out.println("------------------------------------------------------------------------------");
                /*
                   while(rset.next(){
                   System.out.println();
                   }
                 */
        System.out.println("[Purchased Items] \n");
        System.out.println("item category  | item ID   | purchased date | puchased price  | seller ID ");
        System.out.println("--------------------------------------------------------------------------");
                /*
                   while(rset.next(){
                   System.out.println();
                   }
                 */
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
                        CheckSellStatus();
                        break;
                    case '3':
                        ret = BuyItem();
                        if(!ret) continue;
                        break;
                    case '4':
                        CheckBuyStatus();
                        break;
                    case '5':
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
