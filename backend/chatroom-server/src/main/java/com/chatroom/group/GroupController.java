package com.chatroom.group;

import com.chatroom.user.UserRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.chatroom.user.User;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public GroupController(GroupRepository groupRepository, UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    // GET /api/groups
    // Returns all groups the logged in user belongs to
    // This is the user's chat list in Swing
    @GetMapping
    public List<Group> getMyGroups(Authentication authentication) {
        // Authentication object contains userId from JWT token
        Long userId = (Long) authentication.getPrincipal();
        return groupRepository.findGroupsByUserId(userId);
    }

    // POST /api/groups
    // Creates a new group chat
    // Body: { "name": "Team Chat" }
    @PostMapping
    public ResponseEntity<?> createGroup(@RequestBody Map<String, String> request,
                                         Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String name = request.get("name");

        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.status(400)
                .body(Map.of("error", "Group name cannot be empty"));
        }

        // Create the group
        Group group = new Group();
        group.setName(name);
        group.setType("group");
        Long groupId = groupRepository.save(group);

        // Add creator as admin automatically
        groupRepository.addMember(groupId, userId, "admin");

        return ResponseEntity.ok(Map.of(
            "groupId", groupId,
            "message", "Group created successfully"
        ));
    }

    // POST /api/groups/dm
    // Starts a direct message with another user
    // Body: { "targetUserId": 2 }
    @PostMapping("/dm")
    public ResponseEntity<?> createDm(@RequestBody Map<String, Long> request,
                                      Authentication authentication) {
        Long myUserId = (Long) authentication.getPrincipal();
        Long targetUserId = request.get("targetUserId");

        // Check DM doesnt already exist between these 2 users
        Optional<Group> existing = groupRepository.findDirectGroup(myUserId, targetUserId);
        if (existing.isPresent()) {
            return ResponseEntity.ok(Map.of(
                "groupId", existing.get().getGroupId(),
                "message", "DM already exists"
            ));
        }

       // Get both users' names to store in the group name as "userId1:name1|userId2:name2"
    // This lets each client resolve the OTHER person's name
    String myName     = userRepository.findById(myUserId)
                                  .map(User::getName)
                                  .orElse("Unknown");
    String targetName = userRepository.findById(targetUserId)
    .map(User::getName)
    .orElse("Unknown");

    // Store as "id:name|id:name" so any client can pick the other person's name
    String dmName = myUserId + ":" + myName + "|" + targetUserId + ":" + targetName;

    Group dm = new Group();
    dm.setName(dmName);   // ← store encoded names instead of "direct"
    dm.setType("direct");
    Long groupId = groupRepository.save(dm);

        // Add both users as members
        groupRepository.addMember(groupId, myUserId, "member");
        groupRepository.addMember(groupId, targetUserId, "member");

        return ResponseEntity.ok(Map.of(
            "groupId", groupId,
            "message", "DM created successfully"
        ));
    }

    // POST /api/groups/{groupId}/members
    // Adds a new member to an existing group
    // Body: { "userId": 3 }
    @PostMapping("/{groupId}/members")
    public ResponseEntity<?> addMember(@PathVariable Long groupId,
                                       @RequestBody Map<String, Long> request,
                                       Authentication authentication) {
        Long myUserId = (Long) authentication.getPrincipal();
        Long newUserId = request.get("userId");

        // Check group exists
        if (groupRepository.findById(groupId).isEmpty()) {
            return ResponseEntity.status(404)
                .body(Map.of("error", "Group not found"));
        }

        // Check requester is actually in the group
        if (!groupRepository.isMember(groupId, myUserId)) {
            return ResponseEntity.status(403)
                .body(Map.of("error", "You are not a member of this group"));
        }

        // Check user isnt already in group
        if (groupRepository.isMember(groupId, newUserId)) {
            return ResponseEntity.status(400)
                .body(Map.of("error", "User is already a member"));
        }

        groupRepository.addMember(groupId, newUserId, "member");

        return ResponseEntity.ok(Map.of("message", "Member added successfully"));
    }
}