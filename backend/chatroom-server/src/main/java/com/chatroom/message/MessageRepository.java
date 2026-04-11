package com.chatroom.message;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class MessageRepository {

    private final JdbcTemplate jdbcTemplate;

    public MessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Converts one DB row into a Message Java object
    private final RowMapper<Message> messageRowMapper = (rs, rowNum) -> {
        Message m = new Message();
        m.setMsgId(rs.getLong("msg_id"));
        m.setContent(rs.getString("content"));
        m.setUserId(rs.getLong("user_id"));
        m.setGroupId(rs.getLong("group_id"));
        m.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
        m.setSenderName(rs.getString("sender_name"));
        return m;
    };

    // INSERT new message into messages table
    public void save(Message message) {
        jdbcTemplate.update(
            "INSERT INTO messages (content, user_id, group_id) VALUES (?, ?, ?)",
            message.getContent(),
            message.getUserId(),
            message.getGroupId()
        );
    }

    // DELETE a single message by its ID
// Only used when the owner deletes their own message
public void deleteById(Long msgId) {
    jdbcTemplate.update(
        "DELETE FROM messages WHERE msg_id = ?",
        msgId
    );
}

    // SELECT all messages in a group
    // JOINs with users table to get sender name
    // Ordered oldest to newest
    public List<Message> findByGroupId(Long groupId) {
        return jdbcTemplate.query(
            "SELECT m.*, u.name as sender_name " +
            "FROM messages m " +
            "JOIN users u ON m.user_id = u.user_id " +
            "WHERE m.group_id = ? " +
            "ORDER BY m.timestamp ASC",
            messageRowMapper,
            groupId
        );
    }

    // SELECT last 50 messages in a group
    // Used when first opening a chat
    // So Swing doesnt load thousands of messages at once
    public List<Message> findRecentByGroupId(Long groupId) {
        return jdbcTemplate.query(
            "SELECT * FROM (" +
            "  SELECT m.*, u.name as sender_name " +
            "  FROM messages m " +
            "  JOIN users u ON m.user_id = u.user_id " +
            "  WHERE m.group_id = ? " +
            "  ORDER BY m.timestamp DESC " +
            "  LIMIT 50" +
            ") sub ORDER BY timestamp ASC",
            messageRowMapper,
            groupId
        );
    }
    public Message findById(Long msgId) {
    List<Message> result = jdbcTemplate.query(
        "SELECT m.*, u.name as sender_name FROM messages m " +
        "JOIN users u ON m.user_id = u.user_id " +
        "WHERE m.msg_id = ?",
        messageRowMapper, msgId
    );
    return result.isEmpty() ? null : result.get(0);
}
}