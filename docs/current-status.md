# リポジトリ現状レポート

- 調査日: 2026-10-16(第12版)
- 対象: PR #17 までの内容に、自分のパスワード変更機能を加えた状態
- デフォルトブランチは `main` から `master` に変更されています
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
| #11 | 404・400・500 などのエラー画面を日本語化、PR の作成・更新時に CI を自動実行 |
| #12 | タスク API の入力値の不具合を修正(`active: null` の 500 など) |
| #13 | 後片付け: 古いワークフロー削除、compose の `version` 削除、Actions を Node.js 24 対応版へ更新、初期パスワード削除 |
| #13 | 後片付け、Codespaces の自動起動、タスクの並び順(↑ / ↓)、ダッシュボード(トップページ) |
| #14 | Codespaces でアプリの準備ができてからブラウザを開く |
| #15 | **ログイン機能**: 管理者 / 新入社員の役割、マイページ、権限のない画面は 403 |
| #16, #17 | AI 向け開発メモ(`docs/ai/`、`CLAUDE.md`、`.github/copilot-instructions.md`)、不要な `wait-test.sh` の削除 |
| (第12版) | **自分のパスワード変更**(`/me/password`)。管理者も新入社員も使える |

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
├── .devcontainer/
│   ├── devcontainer.json                   … Codespaces の設定(8080 番の転送、起動時に start-app.sh)
│   └── start-app.sh                        … DB とアプリを起動(DB パスワードは初回に自動生成)
├── docs/current-status.md                  … このレポート
└── onboarding-progress-system/
    ├── README.md
    ├── Dockerfile
    ├── docker-compose.yml                  … アプリ(app)と DB(db)
    ├── docker-compose.codespaces.yml       … Codespaces 用の上書き設定
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/com/example/onboarding/
        │   │   ├── OnboardingProgressSystemApplication.java
        │   │   ├── config/TimeConfig.java            … 「今日」「現在時刻」のタイムゾーン設定
        │   │   ├── controller/
        │   │   │   ├── DashboardController.java      … ダッシュボード(トップページ。新入社員は /me へ)
        │   │   │   ├── LoginController.java          … ログイン画面
        │   │   │   ├── MyPageController.java         … マイページ(自分のタスク)
        │   │   │   ├── TaskController.java           … タスクの REST API(JSON)
        │   │   │   ├── TaskPageController.java       … タスクの画面
        │   │   │   └── UserPageController.java       … ユーザー・割り当ての画面
        │   │   ├── entity/
        │   │   │   ├── Task.java
        │   │   │   ├── User.java
        │   │   │   ├── TaskAssignment.java           … 割り当て・進捗
        │   │   │   └── AssignmentStatus.java         … 進捗の種類(未着手 / 対応中 / 完了)
        │   │   ├── form/
        │   │   │   ├── PasswordChangeForm.java
        │   │   │   ├── TaskForm.java
        │   │   │   └── UserForm.java
        │   │   ├── repository/
        │   │   │   ├── TaskRepository.java
        │   │   │   ├── UserRepository.java
        │   │   │   └── TaskAssignmentRepository.java
        │   │   ├── service/
        │   │   │   ├── AssignmentService.java        … 割り当てのルール
        │   │   │   ├── AssignmentException.java      … 割り当てできないときのエラー
        │   │   │   ├── Dashboard.java / DashboardService.java   … ダッシュボードの数字と一覧
        │   │   │   ├── TaskOrderService.java         … タスクの並び順(↑ / ↓)
        │   │   │   ├── ProgressFilter.java           … 一覧の絞り込み条件
        │   │   │   └── UserProgress.java             … 完了数・完了率・期限切れ件数
        │   │   ├── security/
        │   │   │   ├── SecurityConfig.java           … ログイン・権限の設定(どの画面を誰が開けるか)
        │   │   │   ├── AppUserDetails.java / AppUserDetailsService.java   … ログイン中のユーザー
        │   │   │   └── AdminAccountInitializer.java  … 最初の管理者を起動時に作成
        │   │   └── web/
        │   │       ├── DateTimeDisplay.java          … 画面用の日時表示・期限切れ判定
        │   │       └── CurrentUserAdvice.java        … メニューに出すログイン中の名前
        │   └── resources/
        │       ├── application.yml
        │       ├── db/migration/
        │       │   ├── V1__create_tasks_table.sql
        │       │   ├── V2__insert_sample_tasks.sql
        │       │   ├── V3__create_users_and_task_assignments.sql
        │       │   ├── V4__insert_sample_users_and_assignments.sql
        │       │   ├── V5__add_sort_order_to_tasks.sql
        │       │   └── V6__add_login_to_users.sql
        │       ├── static/css/app.css
        │       └── templates/
        │           ├── fragments/nav.html            … 共通メニュー・メッセージ表示
        │           ├── fragments/error.html          … エラー画面の共通部分
        │           ├── error/403.html / 404.html / 4xx.html / 5xx.html   … エラー画面
        │           ├── dashboard.html
        │           ├── login.html / my-page.html / password-form.html
        │           ├── tasks.html / task-form.html
        │           └── users.html / user-form.html / user-detail.html
        └── test/java/com/example/onboarding/          … 自動テスト(130 件)
```

まだ存在しないもの:

- ユーザー・割り当て用の REST API(画面のみ)

---

## 2. 実装済み機能

| 機能 | 内容 |
|---|---|
| ダッシュボード | トップページ(`/`)。全体の完了率・状態の内訳・期限切れ件数、フォローが必要な人、7 日以内が期限のタスク |
| タスク管理 | 一覧・登録・編集・削除(画面と REST API)。一覧の ↑ / ↓ で並び順を変更 |
| ユーザー管理 | 一覧・登録・編集・削除。メールアドレスの形式・重複チェック(大文字小文字を区別しない) |
| タスクの割り当て | ユーザー詳細画面から、有効なタスクを 1 件ずつ(期限付き)、またはまとめて割り当て。割り当て解除も可能 |
| 進捗管理 | 割り当てごとに 未着手 / 対応中 / 完了 を変更。完了にすると完了日時を記録、戻すと消去 |
| 進捗の確認 | ユーザー一覧に「完了数 / 割り当て数」と完了率のバー、期限切れ件数(赤いラベル)。詳細画面でも期限切れを表示 |
| 絞り込み | ユーザー一覧を部署と進捗(未完了あり / 期限切れあり / すべて完了 / 未割り当て)で絞り込み。条件は URL に入るのでブックマーク可能 |
| ログイン | メールアドレスとパスワードでログイン。管理者はすべての画面、新入社員はマイページだけ(12 章) |
| マイページ | 新入社員が自分のタスクの進捗を確認し、状態を変更する(`/me`) |
| パスワード変更 | ログイン中の人が自分のパスワードを変更する(`/me/password`)。現在のパスワードの入力が必要 |
| 共通メニュー | 全画面の上部にメニューとログイン中の名前・「ログアウト」。管理者は「ダッシュボード | タスク | ユーザー」、新入社員は「マイページ」 |
| Codespaces | Codespace を開くと DB とアプリが自動で起動し、8080 番ポートが転送される |
| サンプルデータ | タスク 5 件、ユーザー 3 人、割り当て 10 件(進み具合がばらばら) |
| エラー画面 | 404・その他 4xx・5xx を日本語の画面で表示。API は JSON のまま |
| 自動テスト | 130 件。`mvn test` で実行(Testcontainers で PostgreSQL を自動起動。「今日」は 2026-10-15 に固定) |
| CI | PR の作成・更新時と `master` へのマージ後に、自動テストと Docker での動作確認を実行 |

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
| `sort_order` | `INTEGER` | 不可 | `0` | 表示順(小さい順。V5 で追加。10, 20, 30 … と付け直す) |
| `created_at` / `updated_at` | `TIMESTAMPTZ` | 不可 | `now()` | 作成・更新日時 |

### `users`(ユーザー)— V3

| カラム | 型 | NULL | 初期値 | 説明 |
|---|---|---|---|---|
| `id` | `BIGSERIAL` | 不可 | 自動採番 | 主キー |
| `name` | `VARCHAR(100)` | 不可 | | 氏名 |
| `email` | `VARCHAR(255)` | 不可 | | メールアドレス(`UNIQUE`) |
| `department` | `VARCHAR(100)` | 可 | | 部署 |
| `joined_on` | `DATE` | 可 | | 入社日 |
| `password_hash` | `VARCHAR(100)` | 可 | | パスワード(BCrypt でハッシュ化。NULL ならログイン不可。V6) |
| `role` | `VARCHAR(20)` | 不可 | `'MEMBER'` | `ADMIN`(管理者)/ `MEMBER`(新入社員)(`CHECK` 制約。V6) |
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
| DB 接続 | 環境変数 `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | Docker では `db:5432`、Codespaces では `localhost:5432`。パスワードに初期値はなく、必ず環境変数で渡す |
| `ddl-auto` | `none` | テーブルは Flyway が作る |
| `app.time-zone` | 環境変数 `APP_TIME_ZONE`(初期値 `Asia/Tokyo`) | 完了日時の表示と「今日」(期限切れ判定)に使用 |
| `app.admin.*` | 環境変数 `APP_ADMIN_EMAIL`(初期値 `admin@example.com`)/ `APP_ADMIN_PASSWORD`(必須)/ `APP_ADMIN_NAME` | 最初の管理者。管理者が 1 人もいないときだけ起動時に作成 |

### レイヤー構成

```
全体:  すべてのリクエストは Spring Security(SecurityConfig)でログイン・権限を確認してから各 Controller へ
画面:  ブラウザ → DashboardController → DashboardService → TaskAssignmentRepository / UserRepository → DB
       ブラウザ → TaskPageController → TaskOrderService → TaskRepository → DB
       ブラウザ → UserPageController → AssignmentService → TaskAssignmentRepository / TaskRepository → DB
                                    └→ UserRepository → DB
API:   クライアント → TaskController → TaskOrderService / TaskRepository → DB
```

- 割り当てのルール(重複・無効タスクの禁止、完了日時の記録)は `AssignmentService` と `TaskAssignment` にまとめています。
- フォームの値は `TaskForm` / `UserForm` で受け取り、`id` や作成日時を画面から書き換えられないようにしています。

---

## 6. Entity 一覧

| Entity | テーブル | 主な項目・メソッド |
|---|---|---|
| `Task` | `tasks` | `title`, `description`, `active`, `sortOrder` |
| `User` | `users` | `name`, `email`, `department`, `joinedOn`, `passwordHash`, `role` / `isLoginEnabled()`, `isAdmin()` |
| `Role`(enum) | — | `ADMIN`(管理者), `MEMBER`(新入社員) |
| `TaskAssignment` | `task_assignments` | `user`(多対一), `task`(多対一), `status`, `dueDate`, `completedAt` / `changeStatus()`(完了日時の記録・消去), `isOverdue()`(期限切れ判定) |
| `AssignmentStatus`(enum) | — | `NOT_STARTED`(未着手), `IN_PROGRESS`(対応中), `DONE`(完了) |

---

## 7. Controller 一覧

| Controller | 種類 | 担当パス |
|---|---|---|
| `DashboardController` | `@Controller` | `/`(ダッシュボード。新入社員は `/me` へ転送) |
| `LoginController` | `@Controller` | `/login` |
| `MyPageController` | `@Controller` | `/me` 配下(自分のタスク、自分のパスワード変更) |
| `TaskController` | `@RestController` | `/api/tasks` 配下(JSON) |
| `TaskPageController` | `@Controller` | `/tasks` 配下(画面) |
| `UserPageController` | `@Controller` | `/users` 配下(画面) |

---

## 8. URL 一覧

### 画面(HTML)

| メソッド | パス | 内容 |
|---|---|---|
| `GET` | `/login` | ログイン画面(誰でも開ける) |
| `POST` | `/login` / `/logout` | ログイン / ログアウト(Spring Security が処理) |
| `GET` | `/` | ダッシュボード(新入社員は `/me` へ転送) |
| `GET` | `/me` | マイページ(自分のタスク) |
| `POST` | `/me/assignments/{aid}/status` | 自分のタスクの状態を変更(他人の割り当ては 404) |
| `GET` | `/me/password` | パスワード変更画面(ログイン中なら誰でも) |
| `POST` | `/me/password` | パスワードを変更(現在のパスワード・新しいパスワード・確認用) |
| `GET` | `/tasks` | タスク一覧(表示順) |
| `GET` / `POST` | `/tasks/new` / `/tasks` | タスク登録 |
| `GET` / `POST` | `/tasks/{id}/edit` / `/tasks/{id}` | タスク編集 |
| `POST` | `/tasks/{id}/delete` | タスク削除(割り当ても削除) |
| `POST` | `/tasks/{id}/move-up` / `/tasks/{id}/move-down` | 表示順を 1 つ上 / 下へ(先頭・末尾では何もしない) |
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

入力値の扱い(第8版で修正):

- `POST` で `"active": null` または省略 → 有効(`true`)として登録(以前は 500 エラー)
- `POST` の本文に `id` や `createdAt` を含めても無視し、必ず新しいタスクとして登録(以前は既存タスクを上書きしていた)
- `title` が 200 文字を超える → 400(以前は DB エラーで 500)
- `PUT` で `active` を省略すると `true` になります(従来どおり)

---

## 9. GitHub Actions の内容

**`onboarding-progress-system-ci.yml`**(`onboarding-progress-system/` 配下が変わった push と手動実行で起動)

| ジョブ | 内容 |
|---|---|
| `test` | Java 21 で `mvn -B test`(117 件)。失敗時はテスト結果を成果物として保存 |
| `call-reusable` | 下の `reusable-docker-smoke.yml` を呼ぶ |

**`reusable-docker-smoke.yml`**

| # | ステップ | 内容 |
|---|---|---|
| 1〜4 | 準備・起動 | チェックアウト、ランダムな DB パスワードと管理者パスワードの生成(ログでは `***`)、`docker compose up -d --build` |
| 5〜6 | 起動待ち | DB(`pg_isready`)とアプリ(`/login`)の応答を待つ |
| 7 | Write test helpers | ログイン(Cookie 保存・CSRF トークン取得)用の共通関数を用意 |
| 8 | Smoke test login | 未ログインはログイン画面へ転送、API は認証なしで 401、誤ったパスワードは失敗、管理者でログイン |
| 9 | Smoke test API | API(管理者の HTTP Basic)の登録・取得・削除、`/tasks` 画面、`/` のダッシュボード |
| 10 | Smoke test screens | 画面からのタスク登録・入力エラー・編集・削除 |
| 11 | Smoke test user screens | ユーザー一覧の進捗表示、詳細画面、タスクの割り当てと進捗への反映、期限切れでの絞り込み、日本語の 404 画面 |
| 12 | Smoke test new employee login | 鈴木にパスワードを設定 → 鈴木でログイン → `/` から `/me` へ、`/tasks` は日本語の 403 画面 → 鈴木が自分のパスワードを変更 → 新しいパスワードでログインでき、古いパスワードでは失敗 |
| 13〜14 | 後片付け | 失敗時のみ `docker compose logs`、最後に `docker compose down -v` |

使用しているアクション(すべて Node.js 24 で動作するバージョン):
`actions/checkout@v5`、`actions/setup-java@v5`、`actions/upload-artifact@v6`、`docker/setup-buildx-action@v4`

---

## 10. ブラウザで確認できる画面

起動方法: `export POSTGRES_PASSWORD=<任意>` と `export APP_ADMIN_PASSWORD=<任意>` → `docker compose up -d --build`
(Codespaces では自動で起動し、管理者のパスワードはターミナルと `onboarding-progress-system/.env` に表示・保存されます)

どの画面もログインが必要です。ログインしていないと `/login` に移動します。

全画面の上部に濃い紺色のメニューバーがあり、今いる画面に下線が付きます。管理者には「ダッシュボード | タスク | ユーザー」、新入社員には「マイページ」が表示され、右端にログイン中の名前、「パスワード変更」リンク、「ログアウト」ボタンがあります。
登録・更新・削除などの直後は、緑の枠でメッセージ(エラーは赤い枠)が表示されます。

| 画面 | URL | 主な内容 |
|---|---|---|
| ログイン | `/login` | メールアドレス・パスワード。誤りがあると赤い枠で「メールアドレスまたはパスワードが正しくありません。」 |
| マイページ(新入社員) | `/me` | 自分の進捗(バー・期限切れ件数)と、割り当てられたタスクの表。状態をプルダウンで変更 |
| パスワード変更 | `/me/password` | 現在のパスワード・新しいパスワード(8 文字以上)・確認用。メニュー右側の「パスワード変更」から開く |
| ダッシュボード(管理者) | `/` | 下記 |
| タスク一覧 | `/tasks` | タスクの表(表示順)、「新規登録」、各行の「↑」「↓」「編集」「削除」 |
| タスク登録・編集 | `/tasks/new`, `/tasks/{id}/edit` | タイトル(必須)・説明・有効 |
| ユーザー一覧 | `/users` | 下記 |
| ユーザー登録・編集 | `/users/new`, `/users/{id}/edit` | 氏名(必須)・メールアドレス(必須)・部署・役割(管理者 / 新入社員)・パスワード(8 文字以上。編集時は空欄なら変更なし)・入社日 |
| ユーザー詳細 | `/users/{id}` | 下記 |

### ダッシュボード(`/`)

```
ダッシュボード
2026-10-15 時点
┌──────────┐┌──────────────┐┌────────────┐┌──────────┐
│ユーザー   ││全体の完了率   ││状態の内訳   ││期限切れ   │(期限切れがあると赤)
│ 3 人     ││ 60%          ││未着手 2    ││ 2 件     │
│          ││██████░░░░    ││対応中 2    ││          │
│          ││6 / 10 件完了 ││完了 6      ││          │
└──────────┘└──────────────┘└────────────┘└──────────┘

フォローが必要な人(期限切れあり)
│ 氏名 │ 部署 │ 期限切れ │ 進捗 │   … 期限切れ件数の多い順。氏名から詳細画面へ
期限切れのある人をユーザー一覧で見る        … /users?status=OVERDUE へのリンク

期限が近いタスク(10/22 まで)
│ 期限 │ 氏名 │ タスク │ 状態 │   … 今日から 7 日以内が期限の未完了タスク(期限の早い順)
```

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
3. ~~**`active: null` の 500 エラー修正**~~ → 対応済み(あわせて `id` 上書き・長いタイトルの 500 も修正)

### 優先度 中

4. **ユーザー・割り当ての REST API**: 他のシステムと連携する場合に追加
5. ~~**ログイン・権限管理**~~ → 対応済み(第11版)
6. ~~**新入社員によるパスワード変更**~~ → 対応済み(第12版。管理者も使える)
7. **パスワードを忘れたときの再設定**: 今は管理者が編集画面で設定し直す
6. ~~**タスクの並び順**~~ → 対応済み(↑ / ↓ ボタン)
- ~~**Codespaces の自動起動**~~ → 対応済み(`.devcontainer/`)
- ~~**ダッシュボード**~~ → 対応済み(トップページ)

### 優先度 低(整理)— すべて対応済み

7. ~~実行されないワークフロー `onboarding-progress-system/.github/workflows/ci-docker-smoke.yml` を削除する~~
8. ~~`docker-compose.yml` から `version: "3.8"` を削除して警告を消す~~
9. ~~Actions のバージョンを上げて Node.js 20 の警告を消す~~
10. ~~`application.yml` の初期パスワード `pass` を削除する~~(DB パスワードは必ず環境変数で渡す)

---

## 12. ログインと権限(第11版で追加)

| 役割 | 開ける画面 | ログイン後の行き先 |
|---|---|---|
| 管理者(`ADMIN`) | すべて(ダッシュボード、タスク、ユーザー、割り当て、REST API) | `/`(ダッシュボード) |
| 新入社員(`MEMBER`) | マイページ(`/me`)だけ。自分のタスクの状態を変更できる | `/me` |
| ログインしていない | ログイン画面と CSS だけ | — |

- 権限のない画面を開くと、日本語の 403 画面「このページを表示する権限がありません」(新入社員には「マイページへ」ボタン)
- パスワードは BCrypt でハッシュ化して保存。パスワードが未設定のユーザーはログインできない
- 最初の管理者は、管理者が 1 人もいないときに起動時に作成(`APP_ADMIN_EMAIL` / `APP_ADMIN_PASSWORD`)。コードにパスワードは書かない
- 新入社員の初期パスワードは、管理者がユーザー登録・編集画面で設定(8 文字以上)
- ログイン中の人は、メニュー右側の「パスワード変更」(`/me/password`)で自分のパスワードを変えられる。現在のパスワードの入力が必要で、新しいパスワードは 8〜72 文字・確認用と一致・現在と別のもの(第12版)
- 管理者は自分の役割を新入社員に変えたり、自分を削除したりできない(締め出し防止)
- 画面のフォームは CSRF トークンで保護。REST API は HTTP Basic(管理者のみ)
- ユーザー一覧には「役割」列と「(ログイン不可)」の表示。進捗の絞り込みとダッシュボードの人数は新入社員だけが対象

```
ログイン                       マイページ(佐藤 花子 でログイン)
┌──────────────────┐           オンボーディング進捗管理  マイページ        佐藤 花子 [ログアウト]
│ メールアドレス   │           佐藤 花子 さんのタスク
│ [              ] │           進捗 █████░░░░░ 2 / 4 完了(50%) 期限切れ 2件
│ パスワード       │           │ タスク │ 状態 [対応中▼][変更] │ 期限 │ 完了日時 │
│ [              ] │
│ [ログイン]       │
└──────────────────┘
```
