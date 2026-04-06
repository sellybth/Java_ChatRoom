package com.chatroom.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    // Constructor — Spring automatically injects JdbcTemplate
    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Converts one row from DB result into a User Java object
    private final RowMapper<User> userRowMapper = (rs, rowNum) -> {
        User u = new User();
        u.setUserId(rs.getLong("user_id"));
        u.setName(rs.getString("name"));
        u.setEmail(rs.getString("email"));
        u.setPassword(rs.getString("password"));
        u.setPhoneNum(rs.getString("phone_num"));
        return u;
    };

    // INSERT - used during register
    public void save(User user) {
        jdbcTemplate.update(
            "INSERT INTO users (name, email, password, phone_num) VALUES (?, ?, ?, ?)",
            user.getName(),
            user.getEmail(),
            user.getPassword(),
            user.getPhoneNum()
        );
    }

    // SELECT by email - used during login
    public Optional<User> findByEmail(String email) {
        List<User> users = jdbcTemplate.query(
            "SELECT * FROM users WHERE email = ?",
            userRowMapper,
            email
        );
        return users.stream().findFirst();
    }

    // SELECT by ID - used to get user details
    public Optional<User> findById(Long id) {
        List<User> users = jdbcTemplate.query(
            "SELECT * FROM users WHERE user_id = ?",
            userRowMapper,
            id
        );
        return users.stream().findFirst();
    }

    // SELECT all - used to show all users to chat with
    public List<User> findAll() {
        return jdbcTemplate.query(
            "SELECT * FROM users",
            userRowMapper
        );
    }
}