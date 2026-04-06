import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ApiService {

    public static final String BASE_URL = "http://localhost:8080";

    private static final HttpClient client = HttpClient.newHttpClient();

    private static String getToken() {
        return SessionManager.getInstance().getToken();
    }

    // ─────────────────────────────────────────
    // AUTH
    // ─────────────────────────────────────────

    public static JSONObject login(String email, String password) throws Exception {
        JSONObject body = new JSONObject();
        body.put("email", email);
        body.put("password", password);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/auth/login"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();

        HttpResponse<String> response = client.send(request,
            HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return new JSONObject(response.body());
        } else {
            JSONObject error = new JSONObject(response.body());
            throw new Exception(error.getString("error"));
        }
    }

    public static void register(String name, String email,
                                String password, String phoneNum) throws Exception {
        JSONObject body = new JSONObject();
        body.put("name", name);
        body.put("email", email);
        body.put("password", password);
        body.put("phoneNum", phoneNum);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/auth/register"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();

        HttpResponse<String> response = client.send(request,
            HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            JSONObject error = new JSONObject(response.body());
            throw new Exception(error.getString("error"));
        }
    }

    // ─────────────────────────────────────────
    // USERS
    // ─────────────────────────────────────────

    public static JSONArray getUsers() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/users"))
            .header("Authorization", "Bearer " + getToken())
            .GET()
            .build();

        HttpResponse<String> response = client.send(request,
            HttpResponse.BodyHandlers.ofString());

        return new JSONArray(response.body());
    }

    // ─────────────────────────────────────────
    // GROUPS
    // ─────────────────────────────────────────

    public static JSONArray getGroups() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/groups"))
            .header("Authorization", "Bearer " + getToken())
            .GET()
            .build();

        HttpResponse<String> response = client.send(request,
            HttpResponse.BodyHandlers.ofString());

        return new JSONArray(response.body());
    }

    public static JSONObject createGroup(String name) throws Exception {
        JSONObject body = new JSONObject();
        body.put("name", name);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/groups"))
            .header("Authorization", "Bearer " + getToken())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();

        HttpResponse<String> response = client.send(request,
            HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            return new JSONObject(response.body());
        } else {
            throw new Exception("Failed to create group: " + response.body());
        }
    }

    public static JSONObject createDm(Long targetUserId) throws Exception {
        JSONObject body = new JSONObject();
        body.put("targetUserId", targetUserId);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/groups/dm"))
            .header("Authorization", "Bearer " + getToken())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();

        HttpResponse<String> response = client.send(request,
            HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            return new JSONObject(response.body());
        } else {
            throw new Exception("Failed to create DM: " + response.body());
        }
    }

    // Renamed from addMember → addMemberToGroup for clarity
    public static void addMemberToGroup(Long groupId, Long userId) throws Exception {
        JSONObject body = new JSONObject();
        body.put("userId", userId);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/groups/" + groupId + "/members"))
            .header("Authorization", "Bearer " + getToken())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();

        HttpResponse<String> response = client.send(request,
            HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new Exception("Failed to add member: " + response.body());
        }
    }

    // ─────────────────────────────────────────
    // MESSAGES
    // ─────────────────────────────────────────

    public static JSONArray getMessages(Long groupId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/messages/" + groupId))
            .header("Authorization", "Bearer " + getToken())
            .GET()
            .build();

        HttpResponse<String> response = client.send(request,
            HttpResponse.BodyHandlers.ofString());

        return new JSONArray(response.body());
    }
}