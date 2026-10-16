-- Sample task to try the links in task descriptions (#28): opens the Google home page in a new tab.
-- Added at the end of the task list. Not assigned to anyone; assign it from a user's detail page.
INSERT INTO tasks (title, description, active, sort_order)
SELECT 'サンプル: Google を開く(リンクの確認用)',
       'リンクの動作確認用のサンプルです。押すと Google のホームページが新しいタブで開きます: https://www.google.com/',
       true,
       COALESCE(MAX(sort_order), 0) + 10
FROM tasks;
