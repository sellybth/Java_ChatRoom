package com.chatroom.group;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public class GroupRepository {

    private final JdbcTemplate jdbcTemplate;

    public GroupRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Converts one DB row into a Group Java object
    private final RowMapper<Group> groupRowMapper = (rs, rowNum) -> {
        Group g = new Group();
        g.setGroupId(rs.getLong("group_id"));
        g.setName(rs.getString("name"));
        g.setType(rs.getString("type"));
        return g;
    };

    // INSERT new group into groups table
    // Returns the generated group_id
    public Long save(Group group) {
        jdbcTemplate.update(
            "INSERT INTO groups (name, type) VALUES (?, ?)",
            group.getName(),
            group.getType()
        );
        // Fetch the last inserted group_id
        return jdbcTemplate.queryForObject(
            "SELECT group_id FROM groups ORDER BY group_id DESC LIMIT 1",
            Long.class
        );
    }

    // INSERT a user into a group in members table
    public void addMember(Long groupId, Long userId, String role) {
        jdbcTemplate.update(
            "INSERT INTO members (group_id, user_id, role) VALUES (?, ?, ?)",
            groupId,
            userId,
            role
        );
    }

    // DELETE all members of a group first (FK constraint)
public void deleteMembersByGroupId(Long groupId) {
    jdbcTemplate.update(
        "DELETE FROM members WHERE group_id = ?",
        groupId
    );
}

// DELETE all messages in a group, then the group itself
public void deleteGroup(Long groupId) {
    jdbcTemplate.update("DELETE FROM messages WHERE group_id = ?", groupId);
    jdbcTemplate.update("DELETE FROM members  WHERE group_id = ?", groupId);
    jdbcTemplate.update("DELETE FROM groups   WHERE group_id = ?", groupId);
}

    // SELECT all groups a specific user belongs to
    // Used to show the user's chat list
    public List<Group> findGroupsByUserId(Long userId) {
        return jdbcTemplate.query(
            "SELECT g.* FROM groups g " +
            "JOIN members m ON g.group_id = m.group_id " +
            "WHERE m.user_id = ?",
            groupRowMapper,
            userId
        );
    }

    // SELECT one group by ID
    public Optional<Group> findById(Long groupId) {
        List<Group> groups = jdbcTemplate.query(
            "SELECT * FROM groups WHERE group_id = ?",
            groupRowMapper,
            groupId
        );
        return groups.stream().findFirst();
    }

    // CHECK if a direct message group already exists
    // between two users so we dont create duplicates
    public Optional<Group> findDirectGroup(Long userId1, Long userId2) {
        List<Group> groups = jdbcTemplate.query(
            "SELECT g.* FROM groups g " +
            "JOIN members m1 ON g.group_id = m1.group_id " +
            "JOIN members m2 ON g.group_id = m2.group_id " +
            "WHERE g.type = 'direct' " +
            "AND m1.user_id = ? " +
            "AND m2.user_id = ?",
            groupRowMapper,
            userId1,
            userId2
        );
        return groups.stream().findFirst();
    }

    // CHECK if a user is already a member of a group
    public boolean isMember(Long groupId, Long userId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM members WHERE group_id = ? AND user_id = ?",
            Integer.class,
            groupId,
            userId
        );
        return count != null && count > 0;
    }
}