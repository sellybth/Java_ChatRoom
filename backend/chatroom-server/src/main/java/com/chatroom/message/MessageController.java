package com.chatroom.message;

import com.chatroom.group.GroupRepository;
import com.chatroom.user.UserRepository;
import com.chatroom.user.User;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageRepository messageRepository;
    private final GroupRepository groupRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    public MessageController(MessageRepository messageRepository,
                             GroupRepository groupRepository,
                             SimpMessagingTemplate messagingTemplate, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.groupRepository = groupRepository;
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }

    // GET /api/messages/{groupId}
    // Loads last 50 messages when Swing opens a chat window
    // Requires valid JWT token
    @GetMapping("/{groupId}")
    public ResponseEntity<?> getMessages(@PathVariable Long groupId,
                                         Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();

        // Block if user is not a member of this group
        if (!groupRepository.isMember(groupId, userId)) {
            return ResponseEntity.status(403)
                .body(Map.of("error", "You are not a member of this group"));
        }

        List<Message> messages = messageRepository.findRecentByGroupId(groupId);
        return ResponseEntity.ok(messages);
    }

    // WebSocket endpoint
    // Swing sends message to /app/chat/{groupId}
    // Server saves it then broadcasts to /topic/group/{groupId}
    // Everyone subscribed to that topic receives it instantly
    @org.springframework.messaging.handler.annotation.MessageMapping("/chat/{groupId}")
    public void sendMessage(@DestinationVariable Long groupId,
                            @Payload Map<String, Object> payload) {

        // Build message from payload sent by Swing
        Message message = new Message();
        message.setContent((String) payload.get("content"));
        message.setUserId(Long.valueOf(payload.get("userId").toString()));
        message.setGroupId(groupId);
        message.setSenderName((String) payload.get("senderName"));
        message.setTimestamp(LocalDateTime.now());
        // Save to Neon DB first
        messageRepository.save(message);

        // Broadcast to ALL members of this group in real time
        // Every Swing client subscribed to /topic/group/{groupId}
        // will receive this message instantly
        messagingTemplate.convertAndSend(
            "/topic/group/" + groupId,
            message
        );
    }
}