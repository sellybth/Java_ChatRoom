package com.chatroom.group;

public class Group {

    private Long groupId;
    private String name;
    private String type; // "direct" or "group"

    // Getters
    public Long getGroupId() { return groupId; }
    public String getName() { return name; }
    public String getType() { return type; }

    // Setters
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public void setName(String name) { this.name = name; }
    public void setType(String type) { this.type = type; }
}