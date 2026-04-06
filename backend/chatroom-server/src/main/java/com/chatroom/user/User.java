package com.chatroom.user;

public class User {

    private Long userId;
    private String name;
    private String email;
    private String password;
    private String phoneNum;

    // Getters
    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getPhoneNum() { return phoneNum; }

    // Setters
    public void setUserId(Long userId) { this.userId = userId; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setPhoneNum(String phoneNum) { this.phoneNum = phoneNum; }
}