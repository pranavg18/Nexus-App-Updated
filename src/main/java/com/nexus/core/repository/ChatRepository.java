package com.nexus.core.repository;

import com.nexus.core.model.Message;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ChatRepository {

    private final JdbcTemplate jdbcTemplate;

    public ChatRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Helper method to convert a database row into a Message object
    private Message mapMessage(java.sql.ResultSet rs) throws java.sql.SQLException {
        Message m = new Message();
        m.setSender(rs.getString("sender"));
        m.setRecipient(rs.getString("recipient"));

        boolean deleted = rs.getBoolean("deleted_for_everyone");
        m.setDeletedForEveryone(deleted);

        // Mask the message if it was deleted for everyone
        if (deleted)
            m.setMessage("🚫 This message was deleted");
        else
            m.setMessage(rs.getString("content"));

        m.setTimestamp(rs.getString("timestamp"));
        m.setGroupMessage(rs.getBoolean("is_group_message"));
        return m;
    }

    public void saveMessage(Message msg) {
        String sql = "INSERT INTO messages (sender, recipient, content, timestamp, is_group_message, deleted_for_everyone) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, msg.getSender(), msg.getRecipient(), msg.getMessage(), msg.getTimestamp(), msg.isGroupMessage(), msg.isDeletedForEveryone());
    }

    public List<Message> getDirectHistory(String user1, String user2) {
        String sql = "SELECT * FROM messages WHERE is_group_message = FALSE "
                + "AND ((sender = ? AND recipient = ?) OR (sender = ? AND recipient = ?)) "
                + "AND id NOT IN (SELECT message_id FROM hidden_messages WHERE username = ?) "
                + "ORDER BY id ASC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapMessage(rs), user1, user2, user2, user1, user1);
    }

    public List<Message> getGroupHistory(String groupName, String requester) {
        String sql = "SELECT * FROM messages WHERE is_group_message = TRUE "
                + "AND recipient = ? "
                + "AND id NOT IN (SELECT message_id FROM hidden_messages WHERE username = ?) "
                + "ORDER BY id ASC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapMessage(rs), groupName, requester);
    }

    public void clearDirectChat(String requester, String target) {
        String sql = "INSERT IGNORE INTO hidden_messages (message_id, username) " +
                "SELECT id, ? FROM messages " +
                "WHERE is_group_message = FALSE AND " +
                "((sender = ? AND recipient = ?) OR (sender = ? AND recipient = ?))";
        jdbcTemplate.update(sql, requester, requester, target, target, requester);
    }

    public void clearGroupChat(String requester, String groupName) {
        String sql = "INSERT IGNORE INTO hidden_messages (message_id, username) " +
                "SELECT id, ? FROM messages " +
                "WHERE is_group_message = TRUE AND recipient = ?";
        jdbcTemplate.update(sql, requester, groupName);
    }

    public boolean deleteMessageForEveryone(String sender, String recipient, String timestamp, boolean isGroup) {
        String sql = "UPDATE messages SET deleted_for_everyone = TRUE " +
                "WHERE sender = ? AND recipient = ? AND timestamp = ? AND is_group_message = ?";
        int updated = jdbcTemplate.update(sql, sender, recipient, timestamp, isGroup);
        return (updated > 0);
    }

    public boolean createGroup(String groupName, String creator) {
        String checkSql = "SELECT COUNT(*) FROM chat_groups WHERE group_name = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, groupName);

        if (count != null && count > 0) // group already exists
            return false;

        // Else, create the group and immediately add the creator as a member
        jdbcTemplate.update("INSERT INTO chat_groups (group_name, creator) VALUES (?, ?)", groupName, creator);
        addMemberToGroup(groupName, creator);
        return true;
    }

    public boolean addMemberToGroup(String groupName, String newMember) {
        if (isGroupMember(groupName, newMember))
            return false; // Already a member

        jdbcTemplate.update("INSERT INTO group_members (group_name, username) VALUES (?, ?)", groupName, newMember);
        return true;
    }

    public boolean isGroupMember(String groupName, String username) {
        String sql = "SELECT COUNT(*) FROM group_members WHERE group_name = ? AND username = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, groupName, username);
        return (count != null && count > 0);
    }
}
