# リポジトリ現状レポート

- 調査日: 2026-10-15
- 対象: `main` ブランチ(コミット `b1f14f2`、PR #2 マージ後)
- 対象プロジェクト: `onboarding-progress-system`(新入社員・中途入社者のオンボーディング進捗管理)

---

## 1. ディレクトリ構成

```
java/                                   … リポジトリのルート(Java 学習用プロジェクト置き場)
├── README.md
├── LICENSE
├── .gitignore
├── .github/
│   └── workflows/
│       ├── onboarding-progress-system-ci.yml   … CI の呼び出し元(push / 手動実行で起動)
│       └── reusable-docker-smoke.yml           … Docker で起動して API を確認する本体
├── docs/
│   └── current-status.md                       … このレポート
└── onboarding-progress-system/         … アプリ本体
    ├── README.md
    ├── .gitignore
    ├── Dockerfile                      … アプリのコンテナイメージ定義
    ├── docker-compose.yml              … アプリ(app)と DB(db)の 2 コンテナ構成
    ├── pom.xml                         … Maven のビルド設定
    ├── .github/workflows/ci-docker-smoke.yml   … ※この場所では GitHub に実行されない(後述)
    └── src/
        └── main/
            ├── java/com/example/onboarding/
            │   ├── OnboardingProgressSystemApplication.java   … 起動クラス
            │   ├── controller/TaskController.java
            │   ├── entity/Task.java
            │   └── repository/TaskRepository.java
            └── resources/
                ├── application.yml
                └── db/migration/V1__create_tasks_table.sql
```

存在しないもの:

- `src/test/`(テストコードなし)
- `src/main/resources/templates/`(画面テンプレートなし)
- `src/main/resources/static/`(CSS・JS などの静的ファイルなし)
- `service/` 層(Controller が Repository を直接呼んでいる)

---

## 2. 実装済み機能

| 機能 | 内容 |
|---|---|
| タスク管理 API(CRUD) | タスクの一覧・1 件取得・登録・更新・削除 |
| 入力チェック | `title` が空なら 400 エラー(`@NotBlank`) |
| 作成日時・更新日時の自動設定 | 保存時に `createdAt` / `updatedAt` を自動で入れる |
| DB テーブルの自動作成 | 起動時に Flyway が `tasks` テーブルを作る |
| Docker での起動 | `docker compose up --build` でアプリと PostgreSQL が起動する |
| CI(GitHub Actions) | Docker で起動し、API を実際に呼んで動作を確認する |

---

## 3. 未実装機能

`onboarding-progress-system/README.md` の「Features (MVP)」と比較した結果です。

| README に書かれた機能 | 状態 | 不足している内容 |
|---|---|---|
| Manage onboarding tasks(タスク管理) | △ 一部実装 | API のみ。画面はない |
| Assign tasks to users(ユーザーへの割り当て) | ✕ 未実装 | ユーザーのテーブル・Entity・API がない |
| Track progress and status(進捗・状態の管理) | ✕ 未実装 | 「未着手/対応中/完了」のような状態を持つ項目がない |
| View task completion status(完了状況の表示) | ✕ 未実装 | 一覧画面・集計がない |

その他、まだないもの:

- 画面(Thymeleaf は依存関係に入っているが、テンプレートが 1 つもない)
- テストコード
- ログイン・権限管理
- エラーレスポンスの共通化(現在は Spring Boot 標準のエラー JSON)
- ヘルスチェック用エンドポイント(Spring Boot Actuator は未導入)

---

## 4. テーブル定義

マイグレーションファイル: `src/main/resources/db/migration/V1__create_tasks_table.sql`

### `tasks`(オンボーディングのタスク)

| カラム | 型 | NULL | 初期値 | 説明 |
|---|---|---|---|---|
| `id` | `BIGSERIAL` | 不可 | 自動採番 | 主キー |
| `title` | `VARCHAR(200)` | 不可 | なし | タスク名 |
| `description` | `TEXT` | 可 | なし | 説明 |
| `active` | `BOOLEAN` | 不可 | `true` | 有効フラグ |
| `created_at` | `TIMESTAMPTZ` | 不可 | `now()` | 作成日時 |
| `updated_at` | `TIMESTAMPTZ` | 不可 | `now()` | 更新日時 |

テーブルは `tasks` の 1 つだけです。インデックス・外部キーはありません。

---

## 5. Spring Boot の構成

### バージョン

| 項目 | 値 |
|---|---|
| Java | 21 |
| Spring Boot | 3.3.4 |
| ビルドツール | Maven(Docker 内で `maven:3.9.6-eclipse-temurin-21` を使用) |
| 実行環境 | `eclipse-temurin:21-jre-jammy` |
| DB | PostgreSQL 15 |
| Flyway | 10.10.0(Spring Boot 管理のバージョン) |

### 依存ライブラリ(`pom.xml`)

| ライブラリ | 用途 | 使用状況 |
|---|---|---|
| `spring-boot-starter-web` | REST API | 使用中 |
| `spring-boot-starter-data-jpa` | DB アクセス | 使用中 |
| `spring-boot-starter-validation` | 入力チェック | 使用中(`@NotBlank`) |
| `spring-boot-starter-thymeleaf` | HTML 画面 | **未使用**(テンプレートなし) |
| `flyway-core` / `flyway-database-postgresql` | テーブルの自動作成 | 使用中 |
| `postgresql` | DB ドライバ | 使用中 |
| `spring-boot-starter-test` | テスト | **未使用**(テストなし) |

### 設定(`application.yml`)

| 設定 | 値 | 補足 |
|---|---|---|
| ポート | `8080` | |
| DB 接続先 | 環境変数 `SPRING_DATASOURCE_URL`(なければ `localhost:5432/onboarding_db`) | Docker では `db:5432` |
| DB ユーザー | 環境変数 `SPRING_DATASOURCE_USERNAME`(なければ `user`) | |
| DB パスワード | 環境変数 `SPRING_DATASOURCE_PASSWORD`(なければ `pass`) | Docker では `POSTGRES_PASSWORD` から渡す |
| `ddl-auto` | `none` | テーブルは Hibernate ではなく Flyway が作る |
| `show-sql` | `true` | 実行した SQL をログに出す |
| Flyway | 有効、`baseline-on-migrate: true` | |

### レイヤー構成

```
Controller(TaskController) → Repository(TaskRepository) → DB
```

Service 層はなく、Controller から Repository を直接呼んでいます。

---

## 6. Entity 一覧

| Entity | テーブル | ファイル |
|---|---|---|
| `Task` | `tasks` | `entity/Task.java` |

`Task` のフィールド:

| フィールド | 型 | 補足 |
|---|---|---|
| `id` | `Long` | 自動採番(`IDENTITY`) |
| `title` | `String` | 必須(`@NotBlank`)、最大 200 文字 |
| `description` | `String` | 任意 |
| `active` | `Boolean` | 初期値 `true` |
| `createdAt` | `Instant` | 登録時に自動設定(`@PrePersist`)、更新不可 |
| `updatedAt` | `Instant` | 登録・更新時に自動設定(`@PreUpdate`) |

Repository は `TaskRepository extends JpaRepository<Task, Long>` の 1 つだけです(独自の検索メソッドはありません)。

---

## 7. Controller 一覧

| Controller | 種類 | ベースパス | ファイル |
|---|---|---|---|
| `TaskController` | `@RestController`(JSON を返す) | `/api/tasks` | `controller/TaskController.java` |

画面(HTML)を返す `@Controller` はありません。

---

## 8. API 一覧

| メソッド | パス | 内容 | 成功時 | 失敗時 |
|---|---|---|---|---|
| `GET` | `/api/tasks` | タスク一覧を取得 | 200 + 配列 | — |
| `GET` | `/api/tasks/{id}` | タスクを 1 件取得 | 200 + オブジェクト | 404(存在しない) |
| `POST` | `/api/tasks` | タスクを登録 | 201 + 登録内容 | 400(`title` が空) |
| `PUT` | `/api/tasks/{id}` | タスクを更新 | 200 + 更新内容 | 400(`title` が空) / 404 |
| `DELETE` | `/api/tasks/{id}` | タスクを削除 | 204(本文なし) | 404(存在しない) |

リクエスト例(`POST` / `PUT`):

```json
{ "title": "Set up laptop", "description": "Install tools", "active": true }
```

レスポンス例:

```json
{
  "id": 1,
  "title": "Set up laptop",
  "description": "Install tools",
  "active": true,
  "createdAt": "2026-10-15T01:24:53.365710188Z",
  "updatedAt": "2026-10-15T01:24:53.365710188Z"
}
```

注意点:

- `PUT` で `active` を省略すると `true` に戻ります。
- `POST` で `"active": null` を明示的に送ると、DB の NOT NULL 制約に違反して 500 エラーになります(コードからの推測で、未確認)。

---

## 9. GitHub Actions の内容

### 実行されるワークフロー(`.github/workflows/`)

**`onboarding-progress-system-ci.yml`(呼び出し元)**

- 起動条件
  - `onboarding-progress-system/` 配下のファイルが変わった push(ブランチは問わない)
  - Actions 画面からの手動実行(`workflow_dispatch`)
- 処理: 下の `reusable-docker-smoke.yml` を `project_dir: onboarding-progress-system` で呼ぶ

**`reusable-docker-smoke.yml`(本体)**

| # | ステップ | 内容 |
|---|---|---|
| 1 | Checkout repository | ソースを取得 |
| 2 | Set up Docker Buildx | Docker のビルド環境を準備 |
| 3 | Generate temporary DB password | 実行ごとにランダムなパスワードを作る(ログでは `***` と表示) |
| 4 | Build and start services | `docker compose up -d --build` |
| 5 | Wait for database to be ready | `pg_isready` で DB の起動を待つ(最大 150 秒) |
| 6 | Wait for app to be ready | `/api/tasks` が応答するまで待つ(最大 180 秒) |
| 7 | Smoke test API | 一覧取得 200 → 登録 201 → 1 件取得 200 → 削除 204 を確認 |
| 8 | Show logs on failure | 失敗したときだけ `docker compose logs` を表示 |
| 9 | Cleanup containers | 成否に関係なく `docker compose down -v` で後片付け |

直近の結果: 修正後の実行([run 38665372787](https://github.com/Mika777v/java/actions/runs/38665372787))は成功しています。

### 実行されないワークフロー

- `onboarding-progress-system/.github/workflows/ci-docker-smoke.yml`
  - GitHub はリポジトリ直下の `.github/workflows/` しか読まないため、このファイルは実行されません。
  - コミット `d7ba65e` で一度削除されていますが、main 側でも別に追加されていたため、マージ後も残っています。
  - 内容は上のワークフローの古い版なので、削除して問題ありません。

### 出ている警告(動作には影響なし)

- `docker-compose.yml` の `version: "3.8"` は古い書き方だという警告
- `actions/checkout@v4`・`docker/setup-buildx-action@v3` が Node.js 20 向けだという警告

---

## 10. 現在ブラウザで確認できる画面

**HTML の画面はまだ 1 つもありません。**

`docker compose up --build` で起動したあと、ブラウザで開くと次のようになります。

| URL | 表示内容 |
|---|---|
| `http://localhost:8080/` | Spring Boot 標準のエラーページ「Whitelabel Error Page」(404) |
| `http://localhost:8080/api/tasks` | タスク一覧の JSON(最初は `[]`) |
| `http://localhost:8080/api/tasks/1` | ID 1 のタスクの JSON(なければ 404 のエラー JSON) |

ブラウザのアドレスバーからは `GET` しか送れません。登録・更新・削除を試すには `curl` などのツールが必要です。

---

## 11. 次に実装すべき内容

README の MVP を実現する順番で並べています。上から順に進めるのがおすすめです。

### 優先度 高(MVP に必要)

1. **テストの追加**
   - 今ある Task API のテストを書きます(`@SpringBootTest` + `MockMvc` など)。
   - このあと機能を増やしたときに、既存の機能が壊れていないかを確認できるようになります。
2. **タスク一覧画面(Thymeleaf)**
   - `/tasks` で一覧を表示し、登録・編集・削除のフォームを付けます。
   - ブラウザだけで動作を確認できるようになります。
3. **ユーザー(社員)の管理**
   - `users` テーブル(`V2__create_users_table.sql`)、`User` Entity、API または画面を作ります。
4. **タスクの割り当てと進捗状態**
   - 「誰に・どのタスクを・どの状態で」を持つテーブルを追加します(例: `task_assignments`。`user_id`、`task_id`、`status`(未着手 / 対応中 / 完了)、`due_date`、`completed_at`)。
   - `tasks` はタスクのテンプレート(マスタ)、`task_assignments` は各人の進捗、という役割分担にすると README の要件に合います。
5. **進捗の確認画面**
   - ユーザーごとの完了率、未完了タスクの一覧を表示します。

### 優先度 中(品質・保守性)

6. **Service 層の追加**: Controller から業務ロジックを分けます。割り当て機能を作るタイミングで導入すると自然です。
7. **エラーレスポンスの統一**: `@RestControllerAdvice` で、400・404 などを同じ形の JSON で返すようにします。
8. **CI でテストを実行する**: 1 を作ったら、ワークフローに `mvn test` を追加します。
9. **`active: null` の扱いを修正**: 8 章の注意点のとおり、500 エラーにならないようにします。

### 優先度 低(整理・後回しでよいもの)

10. 実行されないワークフロー `onboarding-progress-system/.github/workflows/ci-docker-smoke.yml` を削除する
11. `docker-compose.yml` から `version: "3.8"` を削除して警告を消す
12. Actions のバージョンを上げて Node.js 20 の警告を消す
13. `application.yml` の予備パスワード `pass` を見直す(ローカル開発用の初期値)
14. ログイン機能(Spring Security)。管理者と新入社員で見られる画面を分ける場合に必要になります
