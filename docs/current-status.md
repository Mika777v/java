# リポジトリ現状レポート

- 調査日: 2026-10-15(第7版)
- 対象: PR #10 マージ後の `main` に、日本語のエラー画面を加えた状態
- 対象プロジェクト: `onboarding-progress-system`(新入社員・中途入社者のオンボーディング進捗管理)
- 調査方法: ソースコードの確認、自動テストの実行、アプリを実際に起動してブラウザで操作

### これまでの主な変更

| PR | 内容 |
|---|---|
| #2 | GitHub Actions の Docker 起動エラーを修正、動作確認ワークフローを整備 |
| #4 | タスク一覧画面 `GET /tasks` とサンプルタスク 5 件を追加 |
| #5 | GitHub Codespaces 用の Docker Compose 設定を追加 |
| #7 | `/` を `/tasks` へ転送、画面からのタスク登録・編集・削除を追加 |
| #8 | 自動テストを追加し、GitHub Actions で毎回実行 |
| #9 | ユーザー管理、タスクの割り当て、進捗管理(未着手 / 対応中 / 完了)を追加 |
| #10 | ユーザー一覧の絞り込み(部署・進捗)と期限切れ件数の表示を追加 |
| (第7版) | 404・400・500 などのエラー画面を日本語化 |

---

## 0. README の MVP と実装の照合

| README の機能 | 状態 | 実装 |
|---|---|---|
| Manage onboarding tasks | ✅ | タスクの一覧・登録・編集・削除(画面と REST API) |
| Assign tasks to users | ✅ | ユーザー詳細画面からタスクを 1 件ずつ、またはまとめて割り当て |
| Track progress and status | ✅ | 割り当てごとに 未着手 / 対応中 / 完了 を変更。完了日時を自動記録 |
| View task completion status | ✅ | ユーザー一覧に完了数・完了率(進捗バー)、詳細画面に期限切れ表示 |

README に書かれた MVP の 4 機能はすべて画面から使えるようになりました。

---

## 1. ディレクトリ構成

```
java/
├── README.md
├── .github/workflows/
│   ├── onboarding-progress-system-ci.yml   … CI の呼び出し元(test ジョブ + 動作確認の呼び出し)
│   └── reusable-docker-smoke.yml           … Docker で起動して API・画面を確認する本体
├── docs/current-status.md                  … このレポート
└── onboarding-progress-system/
    ├── README.md
    ├── Dockerfile
    ├── docker-compose.yml                  … アプリ(app)と DB(db)
    ├── docker-compose.codespaces.yml       … Codespaces 用の上書き設定
    ├── pom.xml
    ├── .github/workflows/ci-docker-smoke.yml   … ※この場所では実行されない(9 章)
    └── src/
        ├── main/
        │   ├── java/com/example/onboarding/
        │   │   ├── OnboardingProgressSystemApplication.java
        │   │   ├── config/TimeConfig.java            … 「今日」「現在時刻」のタイムゾーン設定
        │   │   ├── controller/
        │   │   │   ├── TaskController.java           … タスクの REST API(JSON)
        │   │   │   ├── TaskPageController.java       … タスクの画面
        │   │   │   └── UserPageController.java       … ユーザー・割り当ての画面
        │   │   ├── entity/
        │   │   │   ├── Task.java
        │   │   │   ├── User.java
        │   │   │   ├── TaskAssignment.java           … 割り当て・進捗
        │   │   │   └── AssignmentStatus.java         … 進捗の種類(未着手 / 対応中 / 完了)
        │   │   ├── form/
        │   │   │   ├── TaskForm.java
        │   │   │   └── UserForm.java
        │   │   ├── repository/
        │   │   │   ├── TaskRepository.java
        │   │   │   ├── UserRepository.java
        │   │   │   └── TaskAssignmentRepository.java
        │   │   ├── service/
        │   │   │   ├── AssignmentService.java        … 割り当てのルール
        │   │   │   ├── AssignmentException.java      … 割り当てできないときのエラー
        │   │   │   ├── ProgressFilter.java           … 一覧の絞り込み条件
        │   │   │   └── UserProgress.java             … 完了数・完了率・期限切れ件数
        │   │   └── web/DateTimeDisplay.java          … 画面用の日時表示・期限切れ判定
        │   └── resources/
        │       ├── application.yml
        │       ├── db/migration/
        │       │   ├── V1__create_tasks_table.sql
        │       │   ├── V2__insert_sample_tasks.sql
        │       │   ├── V3__create_users_and_task_assignments.sql
        │       │   └── V4__insert_sample_users_and_assignments.sql
        │       ├── static/css/app.css
        │       └── templates/
        │           ├── fragments/nav.html            … 共通メニュー・メッセージ表示
        │           ├── fragments/error.html          … エラー画面の共通部分
        │           ├── error/404.html / 4xx.html / 5xx.html   … エラー画面
        │           ├── tasks.html / task-form.html
        │           └── users.html / user-form.html / user-detail.html
        └── test/java/com/example/onboarding/          … 自動テスト(77 件)
```

まだ存在しないもの:

- ユーザー・割り当て用の REST API(画面のみ)
- ログイン・権限管理

---

## 2. 実装済み機能

| 機能 | 内容 |
|---|---|
| タスク管理 | 一覧・登録・編集・削除(画面と REST API)。`/` を開くとタスク一覧へ |
| ユーザー管理 | 一覧・登録・編集・削除。メールアドレスの形式・重複チェック(大文字小文字を区別しない) |
| タスクの割り当て | ユーザー詳細画面から、有効なタスクを 1 件ずつ(期限付き)、またはまとめて割り当て。割り当て解除も可能 |
| 進捗管理 | 割り当てごとに 未着手 / 対応中 / 完了 を変更。完了にすると完了日時を記録、戻すと消去 |
| 進捗の確認 | ユーザー一覧に「完了数 / 割り当て数」と完了率のバー、期限切れ件数(赤いラベル)。詳細画面でも期限切れを表示 |
| 絞り込み | ユーザー一覧を部署と進捗(未完了あり / 期限切れあり / すべて完了 / 未割り当て)で絞り込み。条件は URL に入るのでブックマーク可能 |
| 共通メニュー | 全画面の上部に「タスク | ユーザー」のメニュー |
| サンプルデータ | タスク 5 件、ユーザー 3 人、割り当て 10 件(進み具合がばらばら) |
| エラー画面 | 404・その他 4xx・5xx を日本語の画面で表示。API は JSON のまま |
| 自動テスト | 77 件。`mvn test` で実行(Testcontainers で PostgreSQL を自動起動。「今日」は 2026-10-15 に固定) |
| CI | push のたびに自動テストと、Docker で起動しての動作確認を実行 |

### 割り当てのルール

- 割り当てられるのは **有効(Active: Yes)なタスク** だけ
- 同じユーザーに同じタスクは 1 回だけ(DB の一意制約と画面のチェックの両方)
- ユーザーやタスクを削除すると、関係する割り当ても削除される(DB の `ON DELETE CASCADE`)
- 「期限切れ」= 期限が今日より前で、まだ完了していないもの。「今日」は `app.time-zone`(初期値 `Asia/Tokyo`)で判断

---

## 3. 未実装機能

| 機能 | 補足 |
|---|---|
| ユーザー・割り当ての REST API | 今回は画面のみ |
| ログイン・権限管理 | 管理者と新入社員で見られる画面を分ける場合に必要 |
| ヘルスチェック | Spring Boot Actuator は未導入 |

---

## 4. テーブル定義

### `tasks`(タスク。全員共通のひな形)— V1

| カラム | 型 | NULL | 初期値 | 説明 |
|---|---|---|---|---|
| `id` | `BIGSERIAL` | 不可 | 自動採番 | 主キー |
| `title` | `VARCHAR(200)` | 不可 | | タスク名 |
| `description` | `TEXT` | 可 | | 説明 |
| `active` | `BOOLEAN` | 不可 | `true` | 有効フラグ(無効なタスクは割り当てられない) |
| `created_at` / `updated_at` | `TIMESTAMPTZ` | 不可 | `now()` | 作成・更新日時 |

### `users`(ユーザー)— V3

| カラム | 型 | NULL | 初期値 | 説明 |
|---|---|---|---|---|
| `id` | `BIGSERIAL` | 不可 | 自動採番 | 主キー |
| `name` | `VARCHAR(100)` | 不可 | | 氏名 |
| `email` | `VARCHAR(255)` | 不可 | | メールアドレス(`UNIQUE`) |
| `department` | `VARCHAR(100)` | 可 | | 部署 |
| `joined_on` | `DATE` | 可 | | 入社日 |
| `created_at` / `updated_at` | `TIMESTAMPTZ` | 不可 | `now()` | 作成・更新日時 |

### `task_assignments`(割り当て・進捗)— V3

| カラム | 型 | NULL | 初期値 | 説明 |
|---|---|---|---|---|
| `id` | `BIGSERIAL` | 不可 | 自動採番 | 主キー |
| `user_id` | `BIGINT` | 不可 | | `users.id` への外部キー(`ON DELETE CASCADE`) |
| `task_id` | `BIGINT` | 不可 | | `tasks.id` への外部キー(`ON DELETE CASCADE`) |
| `status` | `VARCHAR(20)` | 不可 | `'NOT_STARTED'` | `NOT_STARTED` / `IN_PROGRESS` / `DONE`(`CHECK` 制約) |
| `due_date` | `DATE` | 可 | | 期限 |
| `completed_at` | `TIMESTAMPTZ` | 可 | | 完了日時(完了にしたとき自動で記録) |
| `created_at` / `updated_at` | `TIMESTAMPTZ` | 不可 | `now()` | 作成・更新日時 |

- 一意制約: `(user_id, task_id)` … 同じ人に同じタスクは 1 回だけ
- インデックス: `task_id`(タスク削除時の連鎖削除を速くするため)

```
users 1 ──── * task_assignments * ──── 1 tasks
```

### 初期データ(V2・V4。DB が空の状態での初回起動時に 1 回だけ投入)

| ユーザー | 部署 | 入社日 | 割り当て | 進捗 |
|---|---|---|---|---|
| 山田 太郎 | 開発部 | 2026-04-01 | タスク 1〜4 | 4 / 4 完了(100%) |
| 佐藤 花子 | 営業部 | 2026-04-01 | タスク 1〜4 | 2 / 4 完了(50%)。残り 2 件は期限切れ |
| 鈴木 一郎 | 開発部 | 2026-10-01 | タスク 1・2 | 0 / 2 完了(0%)。タスク 3・4 は未割り当て |

タスク 5「旧オリエンテーション資料の確認」は無効なので、誰にも割り当てられていません。
データを初期状態に戻すには `docker compose down -v` で DB のデータを削除してから起動し直します。

---

## 5. Spring Boot の構成

### バージョン

| 項目 | 値 |
|---|---|
| Java | 21 |
| Spring Boot | 3.3.4 |
| DB | PostgreSQL 15 |
| Flyway | 10.10.0 |
| Testcontainers | 1.21.4(Docker 29 対応のため明示指定) |

### 設定(`application.yml`)

| 設定 | 値 | 補足 |
|---|---|---|
| ポート | `8080` | |
| DB 接続 | 環境変数 `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | Docker では `db:5432`、Codespaces では `localhost:5432` |
| `ddl-auto` | `none` | テーブルは Flyway が作る |
| `app.time-zone` | 環境変数 `APP_TIME_ZONE`(初期値 `Asia/Tokyo`) | 完了日時の表示と「今日」(期限切れ判定)に使用 |

### レイヤー構成

```
画面:  ブラウザ → TaskPageController ─────────────→ TaskRepository → DB
       ブラウザ → UserPageController → AssignmentService → TaskAssignmentRepository / TaskRepository → DB
                                    └→ UserRepository → DB
API:   クライアント → TaskController → TaskRepository → DB
```

- 割り当てのルール(重複・無効タスクの禁止、完了日時の記録)は `AssignmentService` と `TaskAssignment` にまとめています。
- フォームの値は `TaskForm` / `UserForm` で受け取り、`id` や作成日時を画面から書き換えられないようにしています。

---

## 6. Entity 一覧

| Entity | テーブル | 主な項目・メソッド |
|---|---|---|
| `Task` | `tasks` | `title`, `description`, `active` |
| `User` | `users` | `name`, `email`, `department`, `joinedOn` |
| `TaskAssignment` | `task_assignments` | `user`(多対一), `task`(多対一), `status`, `dueDate`, `completedAt` / `changeStatus()`(完了日時の記録・消去), `isOverdue()`(期限切れ判定) |
| `AssignmentStatus`(enum) | — | `NOT_STARTED`(未着手), `IN_PROGRESS`(対応中), `DONE`(完了) |

---

## 7. Controller 一覧

| Controller | 種類 | 担当パス |
|---|---|---|
| `TaskController` | `@RestController` | `/api/tasks` 配下(JSON) |
| `TaskPageController` | `@Controller` | `/`, `/tasks` 配下(画面) |
| `UserPageController` | `@Controller` | `/users` 配下(画面) |

---

## 8. URL 一覧

### 画面(HTML)

| メソッド | パス | 内容 |
|---|---|---|
| `GET` | `/` | `/tasks` へ転送 |
| `GET` | `/tasks` | タスク一覧 |
| `GET` / `POST` | `/tasks/new` / `/tasks` | タスク登録 |
| `GET` / `POST` | `/tasks/{id}/edit` / `/tasks/{id}` | タスク編集 |
| `POST` | `/tasks/{id}/delete` | タスク削除(割り当ても削除) |
| `GET` | `/users` | ユーザー一覧(進捗付き)。`?department=開発部&status=OVERDUE` で絞り込み(`status` は `INCOMPLETE` / `OVERDUE` / `COMPLETED` / `UNASSIGNED`。それ以外の値は 400) |
| `GET` / `POST` | `/users/new` / `/users` | ユーザー登録 → 詳細画面へ |
| `GET` | `/users/{id}` | ユーザー詳細(割り当て・進捗) |
| `GET` / `POST` | `/users/{id}/edit` / `/users/{id}` | ユーザー編集 |
| `POST` | `/users/{id}/delete` | ユーザー削除(割り当ても削除) |
| `POST` | `/users/{id}/assignments` | タスクを 1 件割り当て(`taskId`, `dueDate`) |
| `POST` | `/users/{id}/assignments/all` | 有効なタスクをまとめて割り当て(`dueDate`) |
| `POST` | `/users/{id}/assignments/{aid}/status` | 進捗の変更(`status`) |
| `POST` | `/users/{id}/assignments/{aid}/delete` | 割り当て解除 |

- 存在しないユーザー・タスク・割り当て、または別のユーザーの割り当てを指定した場合は 404(日本語のエラー画面。10 章)
- 割り当てできない場合(未選択・重複・無効なタスク)は詳細画面に戻り、赤い枠でエラーを表示

### REST API(JSON)

| メソッド | パス | 内容 |
|---|---|---|
| `GET` | `/api/tasks` | タスク一覧 |
| `GET` | `/api/tasks/{id}` | タスク 1 件 |
| `POST` | `/api/tasks` | 登録(201) |
| `PUT` | `/api/tasks/{id}` | 更新 |
| `DELETE` | `/api/tasks/{id}` | 削除(204。割り当ても削除) |

注意: `POST /api/tasks` で `"active": null` を明示的に送ると 500 エラーになる見込みです(未修正)。

---

## 9. GitHub Actions の内容

**`onboarding-progress-system-ci.yml`**(`onboarding-progress-system/` 配下が変わった push と手動実行で起動)

| ジョブ | 内容 |
|---|---|
| `test` | Java 21 で `mvn -B test`(77 件)。失敗時はテスト結果を成果物として保存 |
| `call-reusable` | 下の `reusable-docker-smoke.yml` を呼ぶ |

**`reusable-docker-smoke.yml`**

| # | ステップ | 内容 |
|---|---|---|
| 1〜4 | 準備・起動 | チェックアウト、ランダムな DB パスワード生成(ログでは `***`)、`docker compose up -d --build` |
| 5〜6 | 起動待ち | DB(`pg_isready`)とアプリ(`/api/tasks`)の応答を待つ |
| 7 | Smoke test API | API の登録・取得・削除、`/tasks` 画面、`/` の転送 |
| 8 | Smoke test screens | 画面からのタスク登録・入力エラー・編集・削除 |
| 9 | Smoke test user screens | ユーザー一覧の進捗表示、詳細画面、タスクの割り当てと進捗への反映、期限切れでの絞り込み、日本語の 404 画面 |
| 10〜11 | 後片付け | 失敗時のみ `docker compose logs`、最後に `docker compose down -v` |

実行されないワークフロー `onboarding-progress-system/.github/workflows/ci-docker-smoke.yml` は古い版なので削除して問題ありません。

---

## 10. ブラウザで確認できる画面

起動方法: `export POSTGRES_PASSWORD=<任意>` → `docker compose up -d --build`
(Codespaces では `docker compose -f docker-compose.yml -f docker-compose.codespaces.yml up -d --build`)

全画面の上部に濃い紺色のメニューバー「オンボーディング進捗管理 | タスク | ユーザー」があり、今いる方に下線が付きます。
登録・更新・削除などの直後は、緑の枠でメッセージ(エラーは赤い枠)が表示されます。

| 画面 | URL | 主な内容 |
|---|---|---|
| タスク一覧 | `/tasks`(`/` からも) | タスクの表、「新規登録」、各行の「編集」「削除」 |
| タスク登録・編集 | `/tasks/new`, `/tasks/{id}/edit` | タイトル(必須)・説明・有効 |
| ユーザー一覧 | `/users` | 下記 |
| ユーザー登録・編集 | `/users/new`, `/users/{id}/edit` | 氏名(必須)・メールアドレス(必須)・部署・入社日(カレンダー入力) |
| ユーザー詳細 | `/users/{id}` | 下記 |

### ユーザー一覧(`/users`)

```
ユーザー一覧
[新規登録]
┌──────────────────────────────────────────────────────────────┐
│ 部署 [すべて ▼]  進捗 [すべて ▼]  [絞り込む]  条件をクリア     │
└──────────────────────────────────────────────────────────────┘
(絞り込み中は「3 人中 1 人を表示しています。」と表示)
┌──┬──────────┬──────┬──────────┬────────────────────┬──────────────────┐
│ID│ 氏名     │ 部署 │ 入社日   │ 進捗               │ 操作             │
├──┼──────────┼──────┼──────────┼────────────────────┼──────────────────┤
│1 │ 山田 太郎│開発部│2026-04-01│ ██████████         │[詳細][編集][削除]│
│  │          │      │          │ 4 / 4 完了(100%)  │                  │
│2 │ 佐藤 花子│営業部│2026-04-01│ █████░░░░░         │[詳細][編集][削除]│
│  │          │      │          │ 2 / 4 完了(50%) 期限切れ 2件 │        │
│3 │ 鈴木 一郎│開発部│2026-10-01│ ░░░░░░░░░░         │[詳細][編集][削除]│
│  │          │      │          │ 0 / 2 完了(0%)    │                  │
└──┴──────────┴──────┴──────────┴────────────────────┴──────────────────┘
```

- 進捗は緑のバーと「完了数 / 割り当て数 完了(完了率)」で表示。期限切れがある人は赤いラベル「期限切れ N件」。割り当てがない人は「未割り当て」
- 上部のフォームで部署・進捗を選んで「絞り込む」。条件に合う人がいないときは「条件に一致するユーザーはいません。」
- 氏名をクリックすると詳細画面へ

### ユーザー詳細(`/users/{id}`)

```
佐藤 花子 さんの進捗
メールアドレス  sato@example.com
部署            営業部
入社日          2026-04-01
進捗            █████░░░░░ 2 / 4 完了(50%)
[ユーザー情報を編集] [一覧に戻る]

割り当てられたタスク
┌────────────────────┬──────────────┬─────────────────────┬────────────────┬────────────┐
│ タスク             │ 状態         │ 期限                │ 完了日時       │ 操作       │
├────────────────────┼──────────────┼─────────────────────┼────────────────┼────────────┤
│ PCのセットアップ   │[完了 ▼][変更]│ 2026-04-03          │2026-04-03 09:30│[割り当て解除]│
│ 就業規則の確認     │[対応中▼][変更]│ 2026-04-10 期限切れ │                │[割り当て解除]│
│ チームメンバー…    │[未着手▼][変更]│ 2026-04-10 期限切れ │                │[割り当て解除]│
└────────────────────┴──────────────┴─────────────────────┴────────────────┴────────────┘

タスクを割り当てる
タスク [選択してください ▼]  期限 [年/月/日]  [割り当てる]
期限 [年/月/日]  [有効なタスクをすべて割り当てる(2件)]
```

- 状態はプルダウンで選んで「変更」。プルダウンの色は 未着手=白、対応中=黄、完了=緑
- 期限切れは赤いラベル「期限切れ」
- 割り当てられるタスクがないときは「割り当てられる有効なタスクはありません」と表示

---

### エラー画面

ブラウザで開いたときにエラーになると、日本語のエラー画面が表示されます(上部のメニュー付き)。

| 状況 | 表示 | 例 |
|---|---|---|
| 存在しない URL・データ(404) | 「ページが見つかりません」 | `/users/999`, `/no-such-page` |
| その他の入力・URL の誤り(4xx) | 「リクエストを処理できません」 | `/users?status=BAD`(400) |
| システムエラー(5xx) | 「エラーが発生しました」 | 予期しない例外 |

```
エラー 404
ページが見つかりません
お探しのページは存在しないか、削除された可能性があります。URL に誤りがないか確認してください。
URL: /users/999
[タスク一覧へ] [ユーザー一覧へ]
```

- エラーの詳しい内容(例外のメッセージなど)は画面に出しません(情報漏えい対策)。
- REST API(`/api/...`)に JSON で問い合わせた場合は、これまでどおり JSON でエラーを返します。

---

## 11. 次に実装すべき内容

### 優先度 高

1. ~~**検索・絞り込み**~~ → 対応済み(部署・進捗で絞り込み、期限切れ件数を表示)
2. ~~**エラー画面の整備**~~ → 対応済み(404・4xx・5xx の日本語画面)
3. **`active: null` の 500 エラー修正**(8 章の注意点)

### 優先度 中

4. **ユーザー・割り当ての REST API**: 他のシステムと連携する場合に追加
5. **ログイン・権限管理**(Spring Security): 新入社員本人は自分の進捗だけ更新できる、など
6. **タスクの並び順**: オンボーディングの順番(表示順)をタスクに持たせる

### 優先度 低(整理)

7. 実行されないワークフロー `onboarding-progress-system/.github/workflows/ci-docker-smoke.yml` を削除する
8. `docker-compose.yml` から `version: "3.8"` を削除して警告を消す
9. Actions のバージョンを上げて Node.js 20 の警告を消す
10. `application.yml` の予備パスワード `pass` を見直す
