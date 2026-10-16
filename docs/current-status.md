# リポジトリ現状レポート

- 調査日: 2026-10-16(第23版)
- 対象: PR #29 までの内容(`master` の最新)に、要件定義書・設計書を加えた状態。Codespaces での動作確認(13 章)まで完了
- デフォルトブランチは `main` から `master` に変更されています
- Codespaces での動作確認手順: [codespaces-check.md](codespaces-check.md)
- Java の学習用教科書(このリポジトリのコードが教材): [java-guide.md](java-guide.md)
- 要件定義書: [requirements.md](requirements.md) / 設計書: [design.md](design.md)(AI 向けは [ai/spec.md](ai/spec.md))
- 対象プロジェクト: `onboarding-progress-system`(新入社員・中途入社者のオンボーディング進捗管理)
- 調査方法: ソースコードの確認、自動テストの実行、アプリを実際に起動してブラウザで操作、Codespaces での動作確認(オーナー)

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
| #13 | 後片付け(古いワークフロー・compose の `version`・初期パスワードの削除、Actions の更新)、Codespaces の自動起動、タスクの並び順(↑ / ↓)、ダッシュボード(トップページ) |
| #14 | Codespaces でアプリの準備ができてからブラウザを開く |
| #15 | **ログイン機能**: 管理者 / 新入社員の役割、マイページ、権限のない画面は 403 |
| #16, #17 | AI 向け開発メモ(`docs/ai/`、`CLAUDE.md`、`.github/copilot-instructions.md`)、不要な `wait-test.sh` の削除 |
| #18 | **自分のパスワード変更**(`/me/password`) |
| #19 | **新入社員の専用URL**: 管理者が発行したリンクを開くだけでマイページへ。パスワードでのログインは設定(`APP_MEMBER_LOGIN`)で切り替え可能 |
| #20 | **初回ログイン時のパスワード変更**: 管理者がほかの人に設定したパスワードは、本人が最初に変更するまで他の画面を開けない |
| #21 | **ヘルスチェック**: `/actuator/health` で稼働状態(DB 接続を含む)を確認。CI と Codespaces の起動待ちに使用 |
| #22 | **パスワードのリセット**: ほかの管理者が仮パスワードを発行。管理者が 1 人だけなら起動時の設定(`APP_ADMIN_PASSWORD_RESET`)で戻す |
| #23 | Codespaces での動作確認手順(`docs/codespaces-check.md`) |
| #25 | このレポートを最新化(第18版) |
| (第23版) | **要件定義書・設計書**を追加(人向け `docs/requirements.md` / `docs/design.md`、AI 向け `docs/ai/spec.md`。要件 ID とテストの対応表つき) |
| #29 | **テストの整理**: サンプルデータの件数・id・並び順をテスト用の `SampleData` にまとめた。サンプルデータを変えても、直すのはほぼこのファイルだけ |
| #28 | **タスクの説明欄の URL をリンクに**: `http://` / `https://` で始まる URL を新しいタブで開けるリンクとして表示(SSO のログイン画面への案内など)。確認用のサンプルタスク「Google を開く」を追加 |
| #27 | **Java の教科書**(`docs/java-guide.md`)を追加。このレポートにフロントエンド(画面)の状況を追記(5 章) |
| #26 | **CI**: ドキュメントだけの PR では自動テストと Docker の確認をスキップ(必須チェックは「スキップ=成功」扱いでマージ可能) |
| #24 | **不具合修正**: Codespaces で登録・発行などのボタンを押すと「localhost 接続が拒否されました」になる問題。画面移動の行き先をパスだけ(`/users/3` など)にした |

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
├── CLAUDE.md                               … Claude Code が自動で読む入口(docs/ai/ への案内)
├── .github/copilot-instructions.md         … GitHub Copilot が自動で読む入口
├── .github/workflows/
│   ├── onboarding-progress-system-ci.yml   … CI の呼び出し元(test ジョブ + 動作確認の呼び出し)
│   └── reusable-docker-smoke.yml           … Docker で起動して API・画面を確認する本体
├── .devcontainer/
│   ├── devcontainer.json                   … Codespaces の設定(8080 番の転送、起動時に start-app.sh)
│   └── start-app.sh                        … DB とアプリを起動(DB・管理者のパスワードは初回に自動生成して .env に保存)
├── docs/
│   ├── current-status.md                   … このレポート
│   ├── codespaces-check.md                 … Codespaces での動作確認手順(チェックリスト)
│   ├── java-guide.md                       … このリポジトリのコードで学ぶ Java の教科書
│   ├── requirements.md / design.md         … 要件定義書 / 設計書
│   └── ai/                                 … AI(Claude / Copilot)向けの開発メモ
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
        │   │   ├── config/
        │   │   │   ├── TimeConfig.java               … 「今日」「現在時刻」のタイムゾーン設定
        │   │   │   └── WebConfig.java                … 初回ログイン時のパスワード変更チェックを登録
        │   │   ├── controller/
        │   │   │   ├── DashboardController.java      … ダッシュボード(トップページ。新入社員は /me へ)
        │   │   │   ├── LoginController.java          … ログイン画面
        │   │   │   ├── JoinController.java           … 専用URL(/join/…)でのログイン
        │   │   │   ├── MyPageController.java         … マイページ(自分のタスク)
        │   │   │   ├── TaskController.java           … タスクの REST API(JSON)
        │   │   │   ├── TaskPageController.java       … タスクの画面
        │   │   │   └── UserPageController.java       … ユーザー・割り当ての画面
        │   │   ├── entity/
        │   │   │   ├── Task.java
        │   │   │   ├── User.java
        │   │   │   ├── TaskAssignment.java           … 割り当て・進捗
        │   │   │   ├── AssignmentStatus.java         … 進捗の種類(未着手 / 対応中 / 完了)
        │   │   │   └── Role.java                     … 役割(管理者 / 新入社員)
        │   │   ├── form/
        │   │   │   ├── PasswordChangeForm.java
        │   │   │   ├── TaskForm.java
        │   │   │   └── UserForm.java
        │   │   ├── repository/
        │   │   │   ├── TaskRepository.java
        │   │   │   ├── UserRepository.java
        │   │   │   └── TaskAssignmentRepository.java
        │   │   ├── service/
        │   │   │   ├── AccessLinkService.java        … 専用URLの発行・確認・無効化
        │   │   │   ├── AssignmentService.java        … 割り当てのルール
        │   │   │   ├── AssignmentException.java      … 割り当てできないときのエラー
        │   │   │   ├── Dashboard.java / DashboardService.java   … ダッシュボードの数字と一覧
        │   │   │   ├── TaskOrderService.java         … タスクの並び順(↑ / ↓)
        │   │   │   ├── ProgressFilter.java           … 一覧の絞り込み条件
        │   │   │   └── UserProgress.java             … 完了数・完了率・期限切れ件数
        │   │   ├── security/
        │   │   │   ├── SecurityConfig.java           … ログイン・権限の設定(どの画面を誰が開けるか)
        │   │   │   ├── AppUserDetails.java / AppUserDetailsService.java   … ログイン中のユーザー
        │   │   │   ├── LoginSettings.java            … 新入社員のログイン方法(専用URL / パスワード)
        │   │   │   ├── TemporaryPasswords.java       … 仮パスワード(12 文字のランダム)の生成
        │   │   │   └── AdminAccountInitializer.java  … 最初の管理者を起動時に作成(APP_ADMIN_PASSWORD_RESET での復旧も)
        │   │   └── web/
        │   │       ├── DateTimeDisplay.java          … 画面用の日時表示・期限切れ判定
        │   │       ├── CurrentUserAdvice.java        … メニューに出すログイン中の名前
        │   │       ├── LinkText.java                 … 説明欄の URL をリンクにする(ほかの文字は無害化)
        │   │       └── PasswordChangeRequiredInterceptor.java   … パスワード変更が必要な人を変更画面へ移動
        │   └── resources/
        │       ├── application.yml
        │       ├── db/migration/
        │       │   ├── V1__create_tasks_table.sql
        │       │   ├── V2__insert_sample_tasks.sql
        │       │   ├── V3__create_users_and_task_assignments.sql
        │       │   ├── V4__insert_sample_users_and_assignments.sql
        │       │   ├── V5__add_sort_order_to_tasks.sql
        │       │   ├── V6__add_login_to_users.sql
        │       │   ├── V7__add_access_link_to_users.sql
        │       │   ├── V8__add_password_change_required_to_users.sql
        │       │   └── V9__insert_sample_link_task.sql
        │       ├── static/css/app.css
        │       └── templates/
        │           ├── fragments/nav.html            … 共通メニュー・メッセージ表示
        │           ├── fragments/error.html          … エラー画面の共通部分
        │           ├── error/403.html / 404.html / 4xx.html / 5xx.html   … エラー画面
        │           ├── dashboard.html
        │           ├── login.html / my-page.html / password-form.html / join-invalid.html
        │           ├── tasks.html / task-form.html
        │           └── users.html / user-form.html / user-detail.html
        └── test/java/com/example/onboarding/          … 自動テスト(184 件)
```

まだ存在しないもの:

- ユーザー・割り当て用の REST API(画面のみ。3 章)

---

## 2. 実装済み機能

| 機能 | 内容 |
|---|---|
| ダッシュボード | トップページ(`/`)。全体の完了率・状態の内訳・期限切れ件数、フォローが必要な人、7 日以内が期限のタスク |
| タスク管理 | 一覧・登録・編集・削除(画面と REST API)。一覧の ↑ / ↓ で並び順を変更。説明に書いた `http://` / `https://` の URL はリンクになる(新しいタブで開く。それ以外の文字や HTML タグはそのまま文字として表示) |
| ユーザー管理 | 一覧・登録・編集・削除。メールアドレスの形式・重複チェック(大文字小文字を区別しない) |
| タスクの割り当て | ユーザー詳細画面から、有効なタスクを 1 件ずつ(期限付き)、またはまとめて割り当て。割り当て解除も可能 |
| 進捗管理 | 割り当てごとに 未着手 / 対応中 / 完了 を変更。完了にすると完了日時を記録、戻すと消去 |
| 進捗の確認 | ユーザー一覧に「完了数 / 割り当て数」と完了率のバー、期限切れ件数(赤いラベル)。詳細画面でも期限切れを表示 |
| 絞り込み | ユーザー一覧を部署と進捗(未完了あり / 期限切れあり / すべて完了 / 未割り当て)で絞り込み。条件は URL に入るのでブックマーク可能 |
| ログイン | 管理者はメールアドレスとパスワード、新入社員は専用URL(または設定でパスワード)でログイン。管理者はすべての画面、新入社員はマイページだけ(12 章) |
| 専用URL | 管理者がユーザー詳細画面で新入社員ごとのリンクを発行・再発行・無効化。有効期限 60 日 |
| マイページ | 新入社員が自分のタスクの進捗を確認し、状態を変更する(`/me`) |
| パスワード変更 | パスワードでログインする人が自分のパスワードを変更する(`/me/password`)。現在のパスワードの入力が必要 |
| 初回ログイン時のパスワード変更 | 管理者がほかの人に設定したパスワードは、本人が変更するまでほかの画面を開けない |
| パスワードのリセット | 管理者がユーザー詳細画面で仮パスワードを発行(1 回だけ表示)。管理者が 1 人だけなら起動時の設定で復旧 |
| ヘルスチェック | `/actuator/health` で稼働状態(DB 接続を含む)を確認。ログイン不要。CI と Codespaces の起動待ちに使用 |
| 共通メニュー | 全画面の上部にメニュー。管理者は「ダッシュボード | タスク | ユーザー」、新入社員は「マイページ」。右端にログイン中の名前・「パスワード変更」(パスワードでログインする人だけ)・「ログアウト」 |
| Codespaces | Codespace を開くと DB とアプリが自動で起動し、準備ができると 8080 番ポートが転送されてブラウザが開く。専用URLは Codespace のアドレスで表示 |
| サンプルデータ | タスク 6 件(うち 1 件はリンクの確認用)、ユーザー 3 人、割り当て 10 件(進み具合がばらばら) |
| エラー画面 | 404・その他 4xx・5xx を日本語の画面で表示。API は JSON のまま |
| 自動テスト | 184 件。`mvn test` で実行(Testcontainers で PostgreSQL を自動起動。「今日」は 2026-10-15 に固定) |
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
| ユーザー・割り当ての REST API | 保留。人事システム・チャット通知・集計ツールなどと連携することになったら検討 |
| 専用URLのメール送信 | 候補。送信用メールサーバーの設定と、新入社員がリンクを開ける公開サーバーが必要 |
| ユーザー一覧のページ分け | 候補。今は全件表示。ユーザーが数百人規模になったら検討 |

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
| `access_token_hash` | `VARCHAR(64)` | 可 | | 専用URLの鍵の SHA-256 ハッシュ(`UNIQUE`。NULL なら未発行。V7) |
| `access_token_expires_at` | `TIMESTAMPTZ` | 可 | | 専用URLの有効期限(V7) |
| `password_change_required` | `BOOLEAN` | 不可 | `false` | 管理者が設定したパスワードのため、本人の変更が必要(V8) |
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

### 初期データ(V2・V4・V9。それぞれ 1 回だけ投入。V9 は既存の DB にも次の起動時に追加される)

| ユーザー | 部署 | 入社日 | 割り当て | 進捗 |
|---|---|---|---|---|
| 山田 太郎 | 開発部 | 2026-04-01 | タスク 1〜4 | 4 / 4 完了(100%) |
| 佐藤 花子 | 営業部 | 2026-04-01 | タスク 1〜4 | 2 / 4 完了(50%)。残り 2 件は期限切れ |
| 鈴木 一郎 | 開発部 | 2026-10-01 | タスク 1・2 | 0 / 2 完了(0%)。タスク 3・4 は未割り当て |

タスク 5「旧オリエンテーション資料の確認」は無効なので、誰にも割り当てられていません。
タスク 6「サンプル: Google を開く(リンクの確認用)」(V9)は、説明欄の URL がリンクになることを試すためのタスクです。押すと Google のホームページが新しいタブで開きます。誰にも割り当てていないので、試すときはユーザー詳細画面で割り当ててください。
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
| `app.admin.password-reset` | 環境変数 `APP_ADMIN_PASSWORD_RESET`(初期値 `false`) | `true` で起動すると、上の管理者のパスワードを `APP_ADMIN_PASSWORD` に戻す(忘れたとき用。使ったら外す) |
| `app.member-login` | 環境変数 `APP_MEMBER_LOGIN`(初期値 `link`) | 新入社員のログイン方法。`link`(専用URL)/ `password` |
| `app.access-link.valid-days` | 環境変数 `APP_ACCESS_LINK_VALID_DAYS`(初期値 `60`) | 専用URLの有効日数 |
| `app.base-url` | 環境変数 `APP_BASE_URL`(初期値 空) | 専用URLに表示するアドレス。空なら管理者のブラウザが使ったアドレス。Codespaces では自動設定 |
| `server.tomcat.use-relative-redirects` | `true` | 画面移動の行き先をパスだけにする(Codespaces で localhost に飛ばないように) |
| `management.*` | `/actuator/health` だけ公開、詳細は出さない | ヘルスチェック |

### レイヤー構成

```
全体:  すべてのリクエストは Spring Security(SecurityConfig)でログイン・権限を確認してから各 Controller へ
       パスワード変更が必要な人は PasswordChangeRequiredInterceptor が /me/password へ移動させる
画面:  ブラウザ → DashboardController → DashboardService → TaskAssignmentRepository / UserRepository → DB
       ブラウザ → TaskPageController → TaskOrderService → TaskRepository → DB
       ブラウザ → UserPageController → AssignmentService → TaskAssignmentRepository / TaskRepository → DB
                                    ├→ AccessLinkService(専用URL)→ UserRepository → DB
                                    └→ UserRepository → DB
       ブラウザ → MyPageController → AssignmentService / UserRepository → DB
       ブラウザ → JoinController(専用URL)→ AccessLinkService → UserRepository → DB
API:   クライアント → TaskController → TaskOrderService / TaskRepository → DB
```

- 割り当てのルール(重複・無効タスクの禁止、完了日時の記録)は `AssignmentService` と `TaskAssignment` にまとめています。
- フォームの値は `TaskForm` / `UserForm` / `PasswordChangeForm` で受け取り、`id` や作成日時を画面から書き換えられないようにしています。

### 画面(フロントエンド)の作り

バックエンド(Java)に比べて、画面側はシンプルな作りです。

| 部分 | 作り | 状態 |
|---|---|---|
| 画面(HTML) | Thymeleaf でサーバーが HTML を組み立てて返す(`templates/*.html`、15 画面) | できている。ボタンを押すとページ全体を読み込み直す方式 |
| 見た目(CSS) | `static/css/app.css` の 1 ファイル | 最低限(メニュー・表・ボタン・進捗バー・色分け)。スマホ向けの調整はほぼなし |
| 動き(JavaScript) | 削除前の確認(`confirm()`)などだけ | ほぼなし。React / Vue などの画面フレームワークは使っていない |

社内の少人数・短期間のツールとしては十分と判断しています。改善するなら「CSS の整理・スマホ対応 → 小さな JavaScript(専用URLのコピーボタンなど)→ 画面フレームワークで作り直し」の順(11 章)。

---

## 6. Entity 一覧

| Entity | テーブル | 主な項目・メソッド |
|---|---|---|
| `Task` | `tasks` | `title`, `description`, `active`, `sortOrder` |
| `User` | `users` | `name`, `email`, `department`, `joinedOn`, `passwordHash`, `role`, `passwordChangeRequired`, `accessTokenHash`, `accessTokenExpiresAt` / `isLoginEnabled()`, `isAdmin()`, `hasValidAccessLink()` |
| `Role`(enum) | — | `ADMIN`(管理者), `MEMBER`(新入社員) |
| `TaskAssignment` | `task_assignments` | `user`(多対一), `task`(多対一), `status`, `dueDate`, `completedAt` / `changeStatus()`(完了日時の記録・消去), `isOverdue()`(期限切れ判定) |
| `AssignmentStatus`(enum) | — | `NOT_STARTED`(未着手), `IN_PROGRESS`(対応中), `DONE`(完了) |

---

## 7. Controller 一覧

| Controller | 種類 | 担当パス |
|---|---|---|
| `DashboardController` | `@Controller` | `/`(ダッシュボード。新入社員は `/me` へ転送) |
| `LoginController` | `@Controller` | `/login` |
| `JoinController` | `@Controller` | `/join/{token}`(専用URLでのログイン) |
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
| `GET` | `/join/{token}` | 専用URL。有効ならログインして `/me` へ。無効なら「このリンクは使えません」(404) |
| `GET` | `/me/password` | パスワード変更画面(専用URLで入った新入社員は 404) |
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
| `POST` | `/users/{id}/password-reset` | 仮パスワードを発行(12 文字のランダム。次の画面に 1 回だけ表示。本人はログイン後に変更が必要)。自分自身と、専用URL方式の新入社員は対象外 |
| `POST` | `/users/{id}/access-link` | 専用URLを発行・再発行(URL は次の画面に 1 回だけ表示) |
| `POST` | `/users/{id}/access-link/delete` | 専用URLを無効化 |
| `GET` | `/actuator/health` | ヘルスチェック(誰でも開ける)。正常なら `{"status":"UP"}`(200)、DB に接続できないなどの異常時は `{"status":"DOWN"}`(503)。詳細は出さない。ほかの Actuator の機能は公開しない |

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

入力値の扱い(#12 で修正):

- `POST` で `"active": null` または省略 → 有効(`true`)として登録(以前は 500 エラー)
- `POST` の本文に `id` や `createdAt` を含めても無視し、必ず新しいタスクとして登録(以前は既存タスクを上書きしていた)
- `title` が 200 文字を超える → 400(以前は DB エラーで 500)
- `PUT` で `active` を省略すると `true` になります(従来どおり)

---

## 9. GitHub Actions の内容

**`onboarding-progress-system-ci.yml`**(起動するタイミング: PR の作成・更新時(すべて)、`master` への push(アプリ・ワークフローが変わったとき)、手動実行。同じ PR で新しい push があると古い実行は止まる)

| ジョブ | 内容 |
|---|---|
| `changes` | PR で変わったファイルを調べる。**すべて** `docs/` 配下・`*.md`・`LICENSE` なら「ドキュメントだけ」と判定(`master` への push・手動実行は常に「実行する」) |
| `test` | Java 21 で `mvn -B test`(173 件)。失敗時はテスト結果を成果物として保存。ドキュメントだけの PR ではスキップ |
| `call-reusable` | 下の `reusable-docker-smoke.yml` を呼ぶ。ドキュメントだけの PR では、呼び出し先の `run-smoke` がスキップ |

- スキップされたジョブは、必須チェックでは「成功」と同じ扱いになるので、ドキュメントだけの PR も数十秒でマージできる
- `run-smoke` のスキップは呼び出し先の中で判定する(呼び出し元でスキップすると、チェックの名前が `call-reusable / run-smoke` から変わり、必須チェックが「待ち」のままになるため)
- 判定ジョブ自体が失敗した場合は、安全のため両方とも実行する

**`reusable-docker-smoke.yml`**

| # | ステップ | 内容 |
|---|---|---|
| 1〜4 | 準備・起動 | チェックアウト、ランダムな DB パスワードと管理者パスワードの生成(ログでは `***`)、`docker compose up -d --build` |
| 5〜6 | 起動待ち | DB(`pg_isready`)とアプリ(`/actuator/health` が `UP`)を待つ |
| 7 | Write test helpers | ログイン(Cookie 保存・CSRF トークン取得)用の共通関数を用意 |
| 8 | Smoke test login | 未ログインはログイン画面へ転送、API は認証なしで 401、誤ったパスワードは失敗、管理者でログイン |
| 9 | Smoke test API | API(管理者の HTTP Basic)の登録・取得・削除、`/tasks` 画面、`/` のダッシュボード |
| 10 | Smoke test screens | 画面からのタスク登録・入力エラー・編集・削除 |
| 11 | Smoke test user screens | ユーザー一覧の進捗表示、詳細画面、タスクの割り当てと進捗への反映、期限切れでの絞り込み、日本語の 404 画面 |
| 12 | Smoke test new employee personal link | 鈴木の専用URLを発行(1 回だけ表示)→ 開くと `/me` へ、`/tasks` は日本語の 403 画面 → でたらめな URL は 404 → 新入社員はパスワードではログインできない → 無効化すると開けない |
| 13 | Smoke test administrator password change | 管理者が自分のパスワードを変更 → 新しいパスワードでログインでき、古いパスワードでは失敗 |
| 14 | Smoke test first login password change | 2 人目の管理者を初期パスワード付きで登録 → その人でログイン → 他の画面は `/me/password` へ転送 → 変更すると通常どおり開ける |
| 15 | Smoke test password reset | 2 人目の管理者のパスワードをリセット → 仮パスワードが 1 回だけ表示される → 仮パスワードでログインでき、パスワード変更画面へ転送 |
| 16〜17 | 後片付け | 失敗時のみ `docker compose logs`、最後に `docker compose down -v` |

2 つのジョブ(`test`、`call-reusable / run-smoke`)は、`master` のルールセットで「マージ前に必須」になっています。

使用しているアクション(すべて Node.js 24 で動作するバージョン):
`actions/checkout@v5`、`actions/setup-java@v5`、`actions/upload-artifact@v6`、`docker/setup-buildx-action@v4`

---

## 10. ブラウザで確認できる画面

起動方法: `export POSTGRES_PASSWORD=<任意>` と `export APP_ADMIN_PASSWORD=<任意>` → `docker compose up -d --build`
(Codespaces では自動で起動し、管理者のパスワードはターミナルと `onboarding-progress-system/.env` に表示・保存されます。確認手順は [codespaces-check.md](codespaces-check.md))

ログイン画面・専用URL・ヘルスチェック以外は、ログインが必要です。ログインしていないと `/login` に移動します。

全画面の上部に濃い紺色のメニューバーがあり、今いる画面に下線が付きます。管理者には「ダッシュボード | タスク | ユーザー」、新入社員には「マイページ」が表示され、右端にログイン中の名前、「パスワード変更」リンク、「ログアウト」ボタンがあります。
登録・更新・削除などの直後は、緑の枠でメッセージ(エラーは赤い枠)が表示されます。

| 画面 | URL | 主な内容 |
|---|---|---|
| ログイン | `/login` | メールアドレス・パスワード。誤りがあると赤い枠で「メールアドレスまたはパスワードが正しくありません。」。下に「新入社員の方は、管理者から受け取った専用URLを開いてください」 |
| 専用URLが使えない | `/join/{token}` | 「このリンクは使えません」(期限切れ・再発行済み・無効化)。管理者に再発行を依頼するよう案内 |
| マイページ(新入社員) | `/me` | 自分の進捗(バー・期限切れ件数)と、割り当てられたタスクの表。状態をプルダウンで変更 |
| パスワード変更 | `/me/password` | 現在のパスワード・新しいパスワード(8 文字以上)・確認用。メニュー右側の「パスワード変更」から開く(専用URLで入った新入社員には表示しない)。変更が必要な人には赤い枠で理由を表示し、「戻る」は出さない |
| ダッシュボード(管理者) | `/` | 下記 |
| タスク一覧 | `/tasks` | タスクの表(表示順)、「新規登録」、各行の「↑」「↓」「編集」「削除」 |
| タスク登録・編集 | `/tasks/new`, `/tasks/{id}/edit` | タイトル(必須)・説明・有効 |
| ユーザー一覧 | `/users` | 下記 |
| ユーザー登録・編集 | `/users/new`, `/users/{id}/edit` | 氏名(必須)・メールアドレス(必須)・部署・役割(管理者 / 新入社員)・パスワード(8 文字以上。編集時は空欄なら変更なし。ほかの人に設定すると本人は初回ログイン時に変更)・入社日 |
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
┌──┬──────────┬──────┬──────────┬──────────────────────┬────────────────────┬──────────────────┐
│ID│ 氏名     │ 部署 │ 入社日   │ 役割                 │ 進捗               │ 操作             │
├──┼──────────┼──────┼──────────┼──────────────────────┼────────────────────┼──────────────────┤
│1 │ 山田 太郎│開発部│2026-04-01│新入社員(ログイン不可)│ ██████████         │[詳細][編集][削除]│
│  │          │      │          │                      │ 4 / 4 完了(100%)  │                  │
│2 │ 佐藤 花子│営業部│2026-04-01│新入社員(ログイン不可)│ █████░░░░░         │[詳細][編集][削除]│
│  │          │      │          │                      │ 2 / 4 完了(50%) 期限切れ 2件 │        │
│3 │ 鈴木 一郎│開発部│2026-10-01│新入社員              │ ░░░░░░░░░░         │[詳細][編集][削除]│
│  │          │      │          │                      │ 0 / 2 完了(0%)    │                  │
│4 │ 管理者   │      │          │管理者                │ -                  │[詳細][編集][削除]│
└──┴──────────┴──────┴──────────┴──────────────────────┴────────────────────┴──────────────────┘
(例: 鈴木さんだけ専用URLを発行済み)
```

- 役割の横の「(ログイン不可)」は、まだログインできない人(専用URL未発行・期限切れ、またはパスワード未設定)
- 進捗は緑のバーと「完了数 / 割り当て数 完了(完了率)」で表示。期限切れがある人は赤いラベル「期限切れ N件」。割り当てがない人は「未割り当て」。管理者は「-」
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

パスワード(管理者、またはパスワード方式の新入社員のとき。自分自身には出ない)
本人が設定したパスワードでログインしています。忘れた場合は、仮パスワードを発行してください。
[パスワードをリセット]   → 黄色の枠に仮パスワードを 1 回だけ表示

専用URL(専用URL方式の新入社員のとき)
発行済み(有効期限 2026-12-14 09:00)
[専用URLを再発行] [無効にする]   → 発行・再発行すると黄色の枠に URL を 1 回だけ表示

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
[ダッシュボードへ] [タスク一覧へ] [ユーザー一覧へ]
```

- ボタンは見ている人に合わせて変わります(管理者: 上の 3 つ / 新入社員: 「マイページへ」/ ログインしていない: 「ログイン画面へ」)

- エラーの詳しい内容(例外のメッセージなど)は画面に出しません(情報漏えい対策)。
- REST API(`/api/...`)に JSON で問い合わせた場合は、これまでどおり JSON でエラーを返します。

---

## 11. 次に実装すべき内容

これまで候補に挙げたものは、ほぼすべて対応しました。残りは急ぎではないものだけです(詳細は `docs/ai/backlog.md`)。

| 区分 | 項目 | メモ |
|---|---|---|
| 将来の候補 | 専用URLのメール送信 | 送信用メールサーバー(SMTP)の設定と認証情報、新入社員がリンクを開ける公開サーバーが必要。メールでのパスワード再設定もこれと一緒に検討 |
| 将来の候補 | ユーザー一覧のページ分け | ユーザーが数百人規模になったら |
| 将来の候補 | 画面の見た目・使い勝手 | CSS の整理・スマホ対応、専用URL・仮パスワードのコピーボタン(小さな JavaScript)。今は使い勝手に問題なし(オーナー確認済み) |
| 保留 | ユーザー・割り当ての REST API | 人事システム・チャット通知・集計ツールなどと連携することになったら |
| 対応しない | 専用URLを無効化したら、ログイン中の画面もすぐ使えなくする | 公開しない前提なので影響が小さい(オーナー判断)。今はログアウトかセッション切れ(30 分)まで使える |

対応済みの主なもの: 検索・絞り込み、エラー画面、API の 500 エラー修正、タスクの並び順、ダッシュボード、Codespaces の自動起動、ログイン・権限、パスワード変更、専用URL、初回ログイン時のパスワード変更、ヘルスチェック、パスワードのリセット、後片付け(古いワークフロー・不要ファイル・初期パスワードの削除、Actions の更新)

---

## 12. ログインと権限

| 役割 | 開ける画面 | ログイン後の行き先 |
|---|---|---|
| 管理者(`ADMIN`) | すべて(ダッシュボード、タスク、ユーザー、割り当て、REST API) | `/`(ダッシュボード) |
| 新入社員(`MEMBER`) | マイページ(`/me`)だけ。自分のタスクの状態を変更できる | `/me` |
| ログインしていない | ログイン画面、専用URL(`/join/...`)、ヘルスチェック、CSS だけ | — |

- 権限のない画面を開くと、日本語の 403 画面「このページを表示する権限がありません」(新入社員には「マイページへ」ボタン)
- パスワードは BCrypt でハッシュ化して保存。パスワードが未設定のユーザーはログインできない
- 最初の管理者は、管理者が 1 人もいないときに起動時に作成(`APP_ADMIN_EMAIL` / `APP_ADMIN_PASSWORD`)。コードにパスワードは書かない
- 新入社員のログイン方法は `APP_MEMBER_LOGIN` で切り替える:
  - `link`(初期値): 管理者がユーザー詳細画面で専用URLを発行して本人に渡す。開くだけでログインし `/me` へ。URL は発行直後に 1 回だけ表示(DB には SHA-256 ハッシュだけを保存)。有効期限は `APP_ACCESS_LINK_VALID_DAYS`(初期値 60 日)。再発行すると古い URL は使えなくなる。無効化も可能。すでにログイン中の画面は、ログアウトするかセッションが切れるまで使える
  - `password`: 新入社員の初期パスワードは、管理者がユーザー登録・編集画面で設定(8 文字以上)し、`/login` でログイン
- 管理者がほかの人に設定したパスワード(登録・編集画面)は「変更が必要」になり、本人はログイン後、パスワード変更画面以外を開けない(REST API は 403)。変更すると解除。自分で自分に設定した場合と、起動時に作られる最初の管理者は対象外
- **パスワードを忘れたとき**:
  - ほかの管理者がユーザー詳細画面の「パスワードをリセット」で仮パスワード(12 文字のランダム)を発行し、本人に伝える。仮パスワードは 1 回だけ表示。本人はログイン後に変更が必要
  - 管理者が 1 人だけで忘れた場合: `APP_ADMIN_PASSWORD_RESET=true` を付けて再起動すると、`APP_ADMIN_EMAIL` の管理者のパスワードが `APP_ADMIN_PASSWORD` に戻る(ログイン後に変更が必要)。使ったら設定を外す(外さないと起動のたびに戻る。ログに警告を出す)。Codespaces では `.env` に 1 行足して `bash .devcontainer/start-app.sh`
  - 新入社員(専用URL方式)は、専用URLを再発行すればよい
- 管理者はどちらの設定でもパスワードでログインする。管理者には専用URLを発行できない(新入社員を管理者に変えると専用URLは消える)
- Codespaces のポート 8080 は初期設定では本人(GitHub にログインした持ち主)しか開けない。新入社員に専用URLを開いてもらうには、ポートを Public にする必要がある
- パスワードでログインする人は、メニュー右側の「パスワード変更」(`/me/password`)で自分のパスワードを変えられる。現在のパスワードの入力が必要で、新しいパスワードは 8〜72 文字・確認用と一致・現在と別のもの
- 管理者は自分の役割を新入社員に変えたり、自分を削除したりできない(締め出し防止)
- 画面のフォームは CSRF トークンで保護。REST API は HTTP Basic(管理者のみ)
- ユーザー一覧には「役割」列と「(ログイン不可)」の表示。進捗の絞り込みとダッシュボードの人数は新入社員だけが対象

```
ログイン                       マイページ(佐藤 花子 でログイン)
┌──────────────────┐           オンボーディング進捗管理  マイページ        佐藤 花子 [ログアウト]
│ メールアドレス   │           佐藤 花子 さんのタスク
│ [              ] │           進捗 █████░░░░░ 2 / 4 完了(50%) 期限切れ 2件
│ パスワード       │           │ タスク │ 状態 [対応中▼][変更] │ 期限 │ 完了日時 │
│ [              ] │           (専用URLで入った新入社員には「パスワード変更」は表示されない)
│ [ログイン]       │
│ 新入社員の方は…  │
└──────────────────┘
```

---

## 13. Codespaces での動作確認(2026-10-16)

[codespaces-check.md](codespaces-check.md) のチェックリスト 12 項目を、オーナーが Codespaces で実施し、すべて期待どおりに動くことを確認しました。

| # | 確認内容 | 結果 |
|---|---|---|
| 1 | ヘルスチェック(`/actuator/health` → `{"status":"UP"}`) | ✅ |
| 2 | 管理者でログイン → ダッシュボード | ✅ |
| 3 | タスクの登録・編集・並べ替え・削除 | ✅ |
| 4 | タスクの割り当てと状態の変更 | ✅ |
| 5〜6 | 専用URLの発行(1 回だけ表示)→ プライベートウィンドウで開くとマイページ | ✅ |
| 7 | 再発行すると古い URL は「このリンクは使えません」 | ✅ |
| 8〜10 | 管理者を初期パスワード付きで追加 → 初回ログインでパスワード変更画面 → 変更後はダッシュボード | ✅ |
| 11〜12 | パスワードのリセット(仮パスワードを 1 回だけ表示)→ 仮パスワードでログインすると変更画面 | ✅ |

確認の途中で見つかった問題と対応:

| 問題 | 対応 |
|---|---|
| ボタンを押すと「localhost 接続が拒否されました」になる | #24 で修正(画面移動の行き先をパスだけにした) |
| ログから管理者のメールアドレスをコピーすると時刻が混ざってログインできない | 手入力で解決。`codespaces-check.md` に注意を追記 |
| 8080 を開くと「One second, just getting your codespace ready」のまま進まないことがある | Codespaces 側の転送の問題。開き直しで解決。`codespaces-check.md` に対処を追記 |
| 専用URLが「このリンクは使えません」になった | 不具合の影響で発行を複数回押し、古い URL を開いていたため。#24 の修正後、1 回だけ発行して解決 |
