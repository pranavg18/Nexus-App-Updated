-- Create the database
CREATE DATABASE IF NOT EXISTS nexus_db;
USE nexus_db;

-- Create the Users table
CREATE TABLE users (
                       username VARCHAR(50) PRIMARY KEY,
                       password VARCHAR(255) NOT NULL
);

-- Create the Groups table
CREATE TABLE chat_groups (
                             group_name VARCHAR(50) PRIMARY KEY,
                             creator VARCHAR(50) NOT NULL
);

-- Create a mapping table for who is in which group
CREATE TABLE group_members (
                               group_name VARCHAR(50),
                               username VARCHAR(50),
                               PRIMARY KEY (group_name, username),
                               FOREIGN KEY (group_name) REFERENCES chat_groups(group_name) ON DELETE CASCADE,
                               FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE
);

-- Create the Messages table
CREATE TABLE messages (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          sender VARCHAR(50) NOT NULL,
                          recipient VARCHAR(50) NOT NULL,
                          content TEXT NOT NULL,
                          timestamp VARCHAR(50) NOT NULL,
                          is_group_message BOOLEAN DEFAULT FALSE,
                          deleted_for_everyone BOOLEAN DEFAULT FALSE,
                          FOREIGN KEY (sender) REFERENCES users(username) ON DELETE CASCADE
);

-- Replace the old "hiddenFor" list with a mapping table for cleared chats
CREATE TABLE hidden_messages (
                                 message_id BIGINT,
                                 username VARCHAR(50),
                                 PRIMARY KEY (message_id, username),
                                 FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
                                 FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE
);