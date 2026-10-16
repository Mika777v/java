# データベース(AI 向け)

- PostgreSQL 15、DB 名 `onboarding_db`、ユーザー `user`(パスワードは環境変数 `POSTGRES_PASSWORD`)
- スキーマは **Flyway だけ** で管理する。`spring.jpa.hibernate.ddl-auto` は `none`
- マイグレーション: `onboarding-progress-system/src/main/resources/db/migration/`

## マイグレーション一覧

| ファイル | 内容 |
|---|---|
| `V1__create_tasks_table.sql` | `tasks` 作成 |
| `V2__insert_sample_tasks.sql` | サンプルタスク 5 件(5 件目は無効) |
| `V3__create_users_and_task_assignments.sql` | `users`, `task_assignments` 作成 |
| `V4__insert_sample_users_and_assignments.sql` | サンプルユーザー 3 人と割り当て 10 件 |
| `V5__add_sort_order_to_tasks.sql` | `tasks.sort_order` 追加(既存は `id * 10`) |
| `V6__add_login_to_users.sql` | `users.password_hash`, `users.role` 追加 |
| `V7__add_access_link_to_users.sql` | `users.access_token_hash`, `users.access_token_expires_at` 追加(専用URL) |
| `V8__add_password_change_required_to_users.sql` | `users.password_change_required` 追加(初回ログイン時のパスワード変更) |

### マイグレーションのルール

- **適用済みのファイルは絶対に書き換えない。** Flyway のチェックサムが合わなくなり、既存環境で起動に失敗する。
  変更は必ず新しい番号のファイル(次は `V7__...sql`)で行う
- ファイル名は `V<番号>__<英語の説明(スネークケース)>.sql`
- 列を追加したら、対応する Entity(`entity/`)の項目も追加する
- 値の候補が決まっている列は `CHECK` 制約を付け、Java 側は `@Enumerated(EnumType.STRING)` の enum にする
- 既存データがある前提で書く(`NOT NULL` 列の追加は `DEFAULT` を付けるか、`UPDATE` で埋めてから)
- **パスワード(ハッシュを含む)をマイグレーションに書かない。** サンプルユーザーは `password_hash` が `NULL`(ログイン不可)

## テーブル定義

### `tasks`(タスク。全員共通のひな形)

| 列 | 型 | 制約 | 説明 |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `title` | VARCHAR(200) | NOT NULL | Java 側でも `@NotBlank @Size(max=200)` |
| `description` | TEXT | | |
| `active` | BOOLEAN | NOT NULL DEFAULT true | false のタスクは新しく割り当てられない |
| `sort_order` | INTEGER | NOT NULL DEFAULT 0 | 表示順(小さい順、同じなら `id` 順)。10 刻み |
| `created_at`, `updated_at` | TIMESTAMPTZ | NOT NULL | |

### `users`(ユーザー)

| 列 | 型 | 制約 | 説明 |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `name` | VARCHAR(100) | NOT NULL | |
| `email` | VARCHAR(255) | NOT NULL UNIQUE | ログイン ID。重複チェックは大文字小文字を区別しない(アプリ側) |
| `department` | VARCHAR(100) | | 空欄は NULL で保存 |
| `joined_on` | DATE | | |
| `password_hash` | VARCHAR(100) | | BCrypt。NULL = ログインできない |
| `role` | VARCHAR(20) | NOT NULL DEFAULT 'MEMBER', CHECK IN ('ADMIN','MEMBER') | |
| `access_token_hash` | VARCHAR(64) | UNIQUE | 専用URLの鍵の SHA-256(16 進)。NULL = 未発行。鍵そのものは保存しない |
| `access_token_expires_at` | TIMESTAMPTZ | | 専用URLの有効期限 |
| `password_change_required` | BOOLEAN | NOT NULL DEFAULT false | 管理者がほかの人に設定したパスワード。本人が変更すると false |
| `created_at`, `updated_at` | TIMESTAMPTZ | NOT NULL | |

### `task_assignments`(誰にどのタスクを割り当てたか、と進捗)

| 列 | 型 | 制約 | 説明 |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `user_id` | BIGINT | NOT NULL, FK → users ON DELETE CASCADE | |
| `task_id` | BIGINT | NOT NULL, FK → tasks ON DELETE CASCADE(索引あり) | |
| `status` | VARCHAR(20) | NOT NULL DEFAULT 'NOT_STARTED', CHECK IN ('NOT_STARTED','IN_PROGRESS','DONE') | |
| `due_date` | DATE | | 期限。NULL なら期限切れにならない |
| `completed_at` | TIMESTAMPTZ | | `DONE` にした時刻。`DONE` 以外に戻すと NULL |
| `created_at`, `updated_at` | TIMESTAMPTZ | NOT NULL | |
| | | UNIQUE (`user_id`, `task_id`) | 同じタスクを同じ人に 2 回割り当てない |

## 業務ルール(DB とコードの対応)

| ルール | 実装場所 |
|---|---|
| 期限切れ = 未完了 かつ `due_date < 今日` | `TaskAssignment.isOverdue(LocalDate)`、集計は `TaskAssignmentRepository.countProgressByUser` |
| 「今日」は `app.time-zone`(既定 Asia/Tokyo)の日付 | `config/TimeConfig` の `Clock` |
| `DONE` にすると `completed_at` を記録、戻すと消す | `TaskAssignment.changeStatus(status, now)` |
| 無効タスク・割り当て済みタスクは割り当てられない | `AssignmentService.assign` |
| ユーザー・タスクを削除すると割り当ても消える | DB の `ON DELETE CASCADE` |
| 管理者は進捗の集計・絞り込み・ダッシュボードの人数に含めない | `UserPageController`, `DashboardService` |

## サンプルデータ(V2・V4)

テストはこのデータを前提に書かれています。**値を変えるとテストが壊れます**(変えるなら新しいマイグレーションで)。

| タスク id | タイトル | active |
|---|---|---|
| 1 | PCのセットアップ | true |
| 2 | 社内アカウントの登録 | true |
| 3 | 就業規則の確認 | true |
| 4 | チームメンバーとの顔合わせ | true |
| 5 | 旧オリエンテーション資料の確認 | **false** |

| ユーザー id | 氏名 | 部署 | 入社日 | 割り当て |
|---|---|---|---|---|
| 1 | 山田 太郎 | 開発部 | 2026-04-01 | タスク 1〜4 すべて完了(4/4) |
| 2 | 佐藤 花子 | 営業部 | 2026-04-01 | 1,2 完了、3 対応中、4 未着手(2/4)。3,4 は期限 2026-04-10 で **期限切れ** |
| 3 | 鈴木 一郎 | 開発部 | 2026-10-01 | 1 対応中(期限 10-20)、2 未着手(期限 10-31)(0/2)。3,4 は未割り当て |

- 3 人とも役割は `MEMBER`、パスワードも専用URLもなし(ログイン不可)
- 管理者は起動時に `AdminAccountInitializer` が作る(id は 4 以降。テストでは作られない場合がある)
- テストの「今日」は `TestClockConfiguration` で **2026-10-15** に固定

## DB を直接見る

```bash
cd onboarding-progress-system
docker compose exec db psql -U user -d onboarding_db -c 'select * from flyway_schema_history;'
docker compose exec db psql -U user -d onboarding_db -c '\d task_assignments'
```
