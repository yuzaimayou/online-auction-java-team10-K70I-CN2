import com.auction.server.database.DatabaseInit;

public class MainTest {
    public static void main(String[] args) {
        DatabaseInit.init();
        System.out.println("DB initialized");
    }
}
