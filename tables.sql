-- SQLite schema for ToDoList
-- Compatible with sqlite3 CLI

PRAGMA foreign_keys = ON;
PRAGMA journal_mode = WAL;
PRAGMA synchronous = NORMAL;

-- 1) Main task table
CREATE TABLE IF NOT EXISTS tasks (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT,
    start_time TEXT NOT NULL,
    end_time TEXT NOT NULL,
    priority TEXT NOT NULL CHECK(priority IN ('URGENT','IMPORTANT','NORMAL')),
    category TEXT NOT NULL CHECK(category IN ('WORK','HOME','STUDY','OTHER')),
    completed INTEGER NOT NULL DEFAULT 0,
    overdue INTEGER NOT NULL DEFAULT 0,
    sort_index INTEGER NOT NULL DEFAULT 0,
    recurrence_rule TEXT,
    reminder_offset INTEGER NOT NULL DEFAULT 0,
    recurrence_end TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

-- 2) Tag dictionary
CREATE TABLE IF NOT EXISTS tags (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    color TEXT NOT NULL DEFAULT '#95a5a6'
);

-- 3) Many-to-many link table
CREATE TABLE IF NOT EXISTS task_tags (
    task_id TEXT NOT NULL,
    tag_id INTEGER NOT NULL,
    assigned_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (task_id, tag_id),
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

-- 4) Audit log table
CREATE TABLE IF NOT EXISTS task_statistics (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id TEXT NOT NULL,
    operation_type TEXT NOT NULL,
    operation_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details TEXT,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

-- 5) App settings table
CREATE TABLE IF NOT EXISTS app_settings (
    key TEXT PRIMARY KEY,
    value TEXT,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tasks_completed ON tasks(completed);
CREATE INDEX IF NOT EXISTS idx_tasks_priority ON tasks(priority);
CREATE INDEX IF NOT EXISTS idx_tasks_category ON tasks(category);
CREATE INDEX IF NOT EXISTS idx_tasks_end_time ON tasks(end_time);
CREATE INDEX IF NOT EXISTS idx_task_tags_task_id ON task_tags(task_id);
CREATE INDEX IF NOT EXISTS idx_task_statistics_task_id ON task_statistics(task_id);

INSERT OR IGNORE INTO tags(name, color) VALUES
    ('Важно', '#e67e22'),
    ('Срочно', '#e74c3c'),
    ('Работа', '#3498db'),
    ('Дом', '#2ecc71'),
    ('Учеба', '#16a085');

INSERT OR IGNORE INTO app_settings(key, value) VALUES
    ('use_database', 'true'),
    ('theme', 'summer'),
    ('reminder_minutes', '15'),
    ('notify_popup', 'true'),
    ('notify_sound', 'false'),
    ('notify_email', 'false'),
    ('notify_email_to', ''),
    ('music_enabled', 'true'),
    ('music_volume', '0.35'),
    ('widget_enabled', 'false');
