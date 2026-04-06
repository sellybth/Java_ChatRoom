public class SessionManager {

    // Single instance shared across entire app
    private static SessionManager instance;

    // Stored after successful login
    private String token;
    private Long   userId;
    private String name;

    // Private constructor — use getInstance() instead
    private SessionManager() {}

    // Get the single instance
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // Called after successful login
    public void saveSession(String token, Long userId, String name) {
        this.token  = token;
        this.userId = userId;
        this.name   = name;
    }

    // Called on logout
    public void clearSession() {
        this.token  = null;
        this.userId = null;
        this.name   = null;
    }

    // Used by ApiService to add token to every request
    public String getToken() { return token; }

    // Used by ChatRoomFrame to show logged in user
    public Long getUserId() { return userId; }

    // Used by ChatRoomFrame to show name in UI
    public String getName() { return name; }

    // Check if user is logged in
    public boolean isLoggedIn() { return token != null; }
}