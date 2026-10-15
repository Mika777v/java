INSERT INTO users (name, email, department, joined_on) VALUES
    ('山田 太郎', 'yamada@example.com', '開発部', '2026-04-01'),
    ('佐藤 花子', 'sato@example.com', '営業部', '2026-04-01'),
    ('鈴木 一郎', 'suzuki@example.com', '開発部', '2026-10-01');

-- 山田: all active tasks done / 佐藤: half done / 鈴木: just started (tasks 3 and 4 not assigned yet)
INSERT INTO task_assignments (user_id, task_id, status, due_date, completed_at) VALUES
    (1, 1, 'DONE',        '2026-04-03', '2026-04-02 10:00:00+09'),
    (1, 2, 'DONE',        '2026-04-03', '2026-04-02 15:00:00+09'),
    (1, 3, 'DONE',        '2026-04-10', '2026-04-08 11:00:00+09'),
    (1, 4, 'DONE',        '2026-04-10', '2026-04-09 16:00:00+09'),
    (2, 1, 'DONE',        '2026-04-03', '2026-04-03 09:30:00+09'),
    (2, 2, 'DONE',        '2026-04-03', '2026-04-03 17:00:00+09'),
    (2, 3, 'IN_PROGRESS', '2026-04-10', NULL),
    (2, 4, 'NOT_STARTED', '2026-04-10', NULL),
    (3, 1, 'IN_PROGRESS', '2026-10-20', NULL),
    (3, 2, 'NOT_STARTED', '2026-10-31', NULL);
