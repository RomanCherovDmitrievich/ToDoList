-- MySQL 8 schema for ToDoList

CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(64) PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    password_salt VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    reset_code VARCHAR(32),
    reset_code_expires_at DATETIME
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tasks (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description LONGTEXT,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    priority VARCHAR(32) NOT NULL,
    category VARCHAR(32) NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    overdue BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    sort_index INT NOT NULL DEFAULT 0,
    owner_user_id VARCHAR(64),
    recurrence_rule VARCHAR(64),
    reminder_offset INT NOT NULL DEFAULT 0,
    recurrence_end DATETIME,
    CONSTRAINT chk_tasks_priority CHECK (priority IN ('URGENT', 'IMPORTANT', 'NORMAL')),
    CONSTRAINT chk_tasks_category CHECK (category IN ('WORK', 'HOME', 'STUDY', 'OTHER')),
    CONSTRAINT fk_tasks_owner FOREIGN KEY (owner_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) UNIQUE NOT NULL,
    color VARCHAR(16) NOT NULL DEFAULT '#95a5a6'
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS task_tags (
    task_id VARCHAR(64) NOT NULL,
    tag_id BIGINT NOT NULL,
    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (task_id, tag_id),
    CONSTRAINT fk_task_tags_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS task_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL,
    operation_type VARCHAR(32) NOT NULL,
    operation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details LONGTEXT,
    CONSTRAINT fk_task_statistics_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS app_settings (
    `key` VARCHAR(128) PRIMARY KEY,
    value LONGTEXT,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_tasks_completed ON tasks(completed);
CREATE INDEX idx_tasks_priority ON tasks(priority);
CREATE INDEX idx_tasks_category ON tasks(category);
CREATE INDEX idx_tasks_end_time ON tasks(end_time);
CREATE INDEX idx_tasks_owner_user_id ON tasks(owner_user_id);
CREATE INDEX idx_task_tags_task_id ON task_tags(task_id);
CREATE INDEX idx_task_statistics_task_id ON task_statistics(task_id);
