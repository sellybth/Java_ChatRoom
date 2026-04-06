import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class WebSocketService {

    private static WebSocketService instance;
    private WebSocketClient wsClient;
    private Consumer<String> messageHandler;

    private String savedToken;
    private Long subscribedGroupId;
    private boolean intentionalClose = false;

    private final ScheduledExecutorService reconnectExecutor =
        Executors.newSingleThreadScheduledExecutor();

    private WebSocketService() {}

    public static WebSocketService getInstance() {
        if (instance == null) instance = new WebSocketService();
        return instance;
    }

    // ── Connect & subscribe to a group topic ─────────────────────────────────
    public void connect(String token, Long groupId, Consumer<String> onMessage) {
        this.savedToken       = token;
        this.subscribedGroupId = groupId;
        this.messageHandler   = onMessage;
        this.intentionalClose = false;
        doConnect(token, groupId);
    }

    private void doConnect(String token, Long groupId) {
        String wsUrl = ApiService.BASE_URL
            .replace("http://", "ws://")
            .replace("https://", "wss://")
            + "/ws?token=" + token;

        try {
            wsClient = new WebSocketClient(new URI(wsUrl)) {

                @Override
                public void onOpen(ServerHandshake handshake) {
                    System.out.println("[WS] Connected — sending STOMP CONNECT");
                    // 1. Send STOMP CONNECT frame
                    send(stompConnect());
                }

                @Override
                public void onMessage(String frame) {
                    System.out.println("[WS] Frame received: " + frame);

                    if (frame.startsWith("CONNECTED")) {
                        // 2. After CONNECTED, subscribe to the group topic
                        System.out.println("[WS] STOMP connected — subscribing to group " + groupId);
                        send(stompSubscribe("/topic/group/" + groupId, "sub-0"));

                    } else if (frame.startsWith("MESSAGE")) {
                        // 3. Incoming message — extract body (after blank line)
                        String body = extractStompBody(frame);
                        if (messageHandler != null && body != null) {
                            messageHandler.accept(body);
                        }

                    } else if (frame.startsWith("ERROR")) {
                        System.err.println("[WS] STOMP ERROR frame: " + frame);
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("[WS] Closed: " + code + " " + reason);
                    if (!intentionalClose) scheduleReconnect();
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("[WS] Error: " + ex.getMessage());
                    if (!intentionalClose) scheduleReconnect();
                }
            };

            wsClient.connect();

        } catch (Exception e) {
            System.err.println("[WS] Failed to build client: " + e.getMessage());
        }
    }

    // ── Send a chat message via STOMP SEND ────────────────────────────────────
    public void sendMessage(Long groupId, String content) {
        if (wsClient == null || !wsClient.isOpen()) {
            System.err.println("[WS] Not connected");
            return;
        }

        JSONObject payload = new JSONObject();
        payload.put("groupId",    groupId);
        payload.put("content",    content);
        payload.put("userId",     SessionManager.getInstance().getUserId());
        payload.put("senderName", SessionManager.getInstance().getName());

        System.out.println("[WS] isOpen: " + wsClient.isOpen());  // ← is it even connected?
    System.out.println("[WS] Sending to /app/chat/" + groupId);


        wsClient.send(stompSend("/app/chat/" + groupId, payload.toString()));
    }

    // ── Disconnect ────────────────────────────────────────────────────────────
    public void disconnect() {
        intentionalClose = true;
        if (wsClient != null && wsClient.isOpen()) {
            wsClient.send(stompDisconnect());
            wsClient.close();
        }
        wsClient = null;
    }

    // ── Reconnect ─────────────────────────────────────────────────────────────
    private void scheduleReconnect() {
        reconnectExecutor.schedule(() -> {
            System.out.println("[WS] Reconnecting...");
            doConnect(savedToken, subscribedGroupId);
        }, 3, TimeUnit.SECONDS);
    }

    // ── STOMP frame builders ──────────────────────────────────────────────────

    private String stompConnect() {
        return "CONNECT\n" +
               "accept-version:1.2\n" +
               "heart-beat:0,0\n" +
               "\n\0";
    }

    private String stompSubscribe(String destination, String id) {
        return "SUBSCRIBE\n" +
               "id:" + id + "\n" +
               "destination:" + destination + "\n" +
               "\n\0";
    }

    private String stompSend(String destination, String body) {
        return "SEND\n" +
               "destination:" + destination + "\n" +
               "content-type:application/json\n" +
               "content-length:" + body.getBytes().length + "\n" +
               "\n" + body + "\0";
    }

    private String stompDisconnect() {
        return "DISCONNECT\n\n\0";
    }

    private String extractStompBody(String frame) {
        int bodyStart = frame.indexOf("\n\n");
        if (bodyStart == -1) return null;
        String body = frame.substring(bodyStart + 2);
        // Strip trailing null byte
        if (body.endsWith("\0")) body = body.substring(0, body.length() - 1);
        return body.trim();
    }
}