package com.chatroom.message;

import java.time.LocalDateTime;

public class Message {

    private Long msgId;
    private String content;
    private Long userId;
    private Long groupId;
    private LocalDateTime timestamp;
    private String senderName; // comes from JOIN query, not in messages table

    // Getters
    public Long getMsgId() { return msgId; }
    public String getContent() { return content; }
    public Long getUserId() { return userId; }
    public Long getGroupId() { return groupId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getSenderName() { return senderName; }

    // Setters
    public void setMsgId(Long msgId) { this.msgId = msgId; }
    public void setContent(String content) { this.content = content; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
}