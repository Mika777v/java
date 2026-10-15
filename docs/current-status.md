# リポジトリ現状レポート

- 調査日: 2026-10-15(第3版)
- 対象: PR #6 マージ後の `main` に、ルート転送と画面からの登録・編集・削除を加えた状態
- 対象プロジェクト: `onboarding-progress-system`(新入社員・中途入社者のオンボーディング進捗管理)
- 調査方法: ソースコードの確認に加え、アプリを実際に起動し、各 URL をブラウザ(Chromium)で開いて表示を確認

### 前回(第1版、PR #3 時点)からの主な変更

| PR | 内容 |
|---|---|
| #4 | タスク一覧画面 `GET /tasks` を追加(`TaskPageController`、`tasks.html`)、サンプルデータ 5 件を追加 |
| #5 | GitHub Codespaces 用の Docker Compose 設定 `docker-compose.codespaces.yml` を追加 |
| (第3版) | `/` を `/tasks` へ転送、画面からのタスク登録・編集・削除を追加 |

---

## 0. README の記載と実装の照合(再調査結果)

README には「`http://localhost:8080/tasks` でタスク一覧画面が見られる」と書かれています。実装と照合した結果、**記載どおり存在し、実際に表示できる**ことを確認しました。

| # | 確認項目 | 結果 | 根拠 |
|---|---|---|---|
| 1 | `templates/tasks.html` は存在するか | ✅ 存在する | `onboarding-progress-system/src/main/resources/templates/tasks.html` |
| 2 | `@Controller` は存在するか | ✅ 存在する | `controller/TaskPageController.java` の 9 行目 `@Controller` |
| 3 | `GET /tasks` は実装されているか | ✅ 実装済み | `TaskPageController.java` の 18 行目 `@GetMapping("/tasks")`。起動して HTTP 200(`text/html`)が返ることを確認 |
| 4 | ブラウザで確認できる画面一覧 | 画面は 3 つ(一覧・登録・編集) | 10 章を参照 |
| 5 | スクリーンショット相当の説明 | 10 章に記載 | 実際に撮影した画面をもとに記述 |

補足として、GitHub Actions も毎回 `/tasks` が 200 を返し、表にサンプル 5 件が並ぶことを自動で確認しています(9 章)。

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
│       └── reusable-docker-smoke.yml           … Docker で起動して API・画面を確認する本体
├── docs/
│   └── current-status.md                       … このレポート
└── onboarding-progress-system/         … アプリ本体
    ├── README.md
    ├── .gitignore
    ├── Dockerfile                      … アプリのコンテナイメージ定義
    ├── docker-compose.yml              … アプリ(app)と DB(db)の 2 コンテナ構成
    ├── docker-compose.codespaces.yml   … Codespaces 用の上書き設定(PR #5)
    ├── pom.xml                         … Maven のビルド設定
    ├── .github/workflows/ci-docker-smoke.yml   … ※この場所では GitHub に実行されない(9 章)
    └── src/
        └── main/
            ├── java/com/example/onboarding/
            │   ├── OnboardingProgressSystemApplication.java   … 起動クラス
            │   ├── controller/
            │   │   ├── TaskController.java       … REST API(JSON)
            │   │   └── TaskPageController.java   … 画面(HTML)
            │   ├── entity/Task.java
            │   ├── form/TaskForm.java            … 登録・編集フォームの入力値
            │   └── repository/TaskRepository.java
            └── resources/
                ├── application.yml
                ├── db/migration/
                │   ├── V1__create_tasks_table.sql
                │   └── V2__insert_sample_tasks.sql   … サンプルデータ 5 件(PR #4)
                ├── static/css/app.css            … 画面共通のデザイン
                └── templates/
                    ├── tasks.html                    … タスク一覧画面
                    └── task-form.html                … タスク登録・編集画面(共通)
```

まだ存在しないもの:

- `src/test/`(テストコードなし)
- `service/` 層(Controller が Repository を直接呼んでいる)

---

## 2. 実装済み機能

| 機能 | 内容 |
|---|---|
| タスク管理 API(CRUD) | タスクの一覧・1 件取得・登録・更新・削除 |
| タスク一覧画面 | `/tasks` で全タスクを ID 順に表で表示。`/` を開いても一覧に移動 |
| タスク登録・編集・削除画面 | 一覧の「新規登録」「編集」「削除」ボタンから操作。完了後は一覧に戻り、メッセージを表示 |
| サンプルデータ | 初回起動時に 5 件のタスクを自動登録 |
| 入力チェック | API: `title` が空なら 400 エラー。画面: タイトル未入力・200 文字超でフォームにエラーメッセージを表示 |
| 作成日時・更新日時の自動設定 | 保存時に `createdAt` / `updatedAt` を自動で入れる |
| DB テーブルの自動作成 | 起動時に Flyway が `tasks` テーブルを作る |
| Docker での起動 | `docker compose up --build` でアプリと PostgreSQL が起動する |
| Codespaces での起動 | `docker-compose.codespaces.yml` を重ねて指定すると起動できる |
| CI(GitHub Actions) | Docker で起動し、API と画面(一覧・登録・編集・削除・入力チェック)を実際に呼んで確認する |

---

## 3. 未実装機能

`onboarding-progress-system/README.md` の「Features (MVP)」と比較した結果です。

| README に書かれた機能 | 状態 | 不足している内容 |
|---|---|---|
| Manage onboarding tasks(タスク管理) | ✅ 実装済み | API・画面ともに一覧・登録・編集・削除ができる |
| Assign tasks to users(ユーザーへの割り当て) | ✕ 未実装 | ユーザーのテーブル・Entity・API・画面がない |
| Track progress and status(進捗・状態の管理) | ✕ 未実装 | 「未着手/対応中/完了」のような状態を持つ項目がない |
| View task completion status(完了状況の表示) | ✕ 未実装 | 完了率などの集計・画面がない |

その他、まだないもの:

- タスク詳細画面(`/tasks/{id}` は 404)
- テストコード
- ログイン・権限管理
- エラーレスポンス・エラー画面の共通化(現在は Spring Boot 標準のもの)
- ヘルスチェック用エンドポイント(Spring Boot Actuator は未導入)

---

## 4. テーブル定義

### `tasks`(オンボーディングのタスク)

作成: `db/migration/V1__create_tasks_table.sql`

| カラム | 型 | NULL | 初期値 | 説明 |
|---|---|---|---|---|
| `id` | `BIGSERIAL` | 不可 | 自動採番 | 主キー |
| `title` | `VARCHAR(200)` | 不可 | なし | タスク名 |
| `description` | `TEXT` | 可 | なし | 説明 |
| `active` | `BOOLEAN` | 不可 | `true` | 有効フラグ |
| `created_at` | `TIMESTAMPTZ` | 不可 | `now()` | 作成日時 |
| `updated_at` | `TIMESTAMPTZ` | 不可 | `now()` | 更新日時 |

テーブルは `tasks` の 1 つだけです。インデックス・外部キーはありません。

### 初期データ

投入: `db/migration/V2__insert_sample_tasks.sql`(Flyway により DB が空の状態での初回起動時に 1 回だけ実行)

| id | title | description | active |
|---|---|---|---|
| 1 | PCのセットアップ | 支給PCに開発ツールをインストールする | true |
| 2 | 社内アカウントの登録 | メール・チャット・勤怠システムのアカウントを作成する | true |
| 3 | 就業規則の確認 | 就業規則とセキュリティポリシーを読む | true |
| 4 | チームメンバーとの顔合わせ | 配属チームのメンバーと1on1を行う | true |
| 5 | 旧オリエンテーション資料の確認 | 現在は使われていない旧資料(参考用) | false |

データを初期状態に戻したいときは `docker compose down -v` で DB のデータ(ボリューム)を削除してから起動し直します。

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
| `spring-boot-starter-web` | REST API・Web サーバー | 使用中 |
| `spring-boot-starter-data-jpa` | DB アクセス | 使用中 |
| `spring-boot-starter-validation` | 入力チェック | 使用中(`@NotBlank`) |
| `spring-boot-starter-thymeleaf` | HTML 画面 | **使用中**(`tasks.html`)※第1版では未使用 |
| `flyway-core` / `flyway-database-postgresql` | テーブル作成・初期データ投入 | 使用中 |
| `postgresql` | DB ドライバ | 使用中 |
| `spring-boot-starter-test` | テスト | **未使用**(テストなし) |

### 設定(`application.yml`)

| 設定 | 値 | 補足 |
|---|---|---|
| ポート | `8080` | |
| DB 接続先 | 環境変数 `SPRING_DATASOURCE_URL`(なければ `localhost:5432/onboarding_db`) | Docker では `db:5432`、Codespaces では `localhost:5432` |
| DB ユーザー | 環境変数 `SPRING_DATASOURCE_USERNAME`(なければ `user`) | |
| DB パスワード | 環境変数 `SPRING_DATASOURCE_PASSWORD`(なければ `pass`) | Docker では `POSTGRES_PASSWORD` から渡す |
| `ddl-auto` | `none` | テーブルは Hibernate ではなく Flyway が作る |
| `show-sql` | `true` | 実行した SQL をログに出す |
| Flyway | 有効、`baseline-on-migrate: true` | |
| Thymeleaf | 設定なし(Spring Boot の初期値) | テンプレートは `classpath:/templates/*.html` |

### レイヤー構成

```
画面:  ブラウザ → TaskPageController(@Controller)     → TaskRepository → DB
                                    ↓
                               templates/tasks.html(Thymeleaf)

API:   クライアント → TaskController(@RestController) → TaskRepository → DB
```

Service 層はなく、どちらの Controller も Repository を直接呼んでいます。

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

| Controller | 種類 | 担当パス | 返すもの | ファイル |
|---|---|---|---|---|
| `TaskController` | `@RestController` | `/api/tasks` 配下 | JSON | `controller/TaskController.java` |
| `TaskPageController` | `@Controller` | `/`、`/tasks` 配下 | HTML(`tasks.html`、`task-form.html`) | `controller/TaskPageController.java` |

`TaskPageController` のメソッド:

| メソッド | URL | 処理 |
|---|---|---|
| `home` | `GET /` | `/tasks` へ転送 |
| `listTasks` | `GET /tasks` | 全タスクを ID 順で取得し、`tasks.html` を表示 |
| `newTask` | `GET /tasks/new` | 空のフォームで `task-form.html` を表示 |
| `createTask` | `POST /tasks` | 入力チェック → 保存 → 一覧へ転送(エラー時はフォームを再表示) |
| `editTask` | `GET /tasks/{id}/edit` | 既存の値を入れたフォームを表示 |
| `updateTask` | `POST /tasks/{id}` | 入力チェック → 更新 → 一覧へ転送(エラー時はフォームを再表示) |
| `deleteTask` | `POST /tasks/{id}/delete` | 削除 → 一覧へ転送 |

フォームの値は Entity(`Task`)に直接入れず、`form/TaskForm.java` で受け取ります。こうすると、画面から `id` や作成日時などを書き換えられないようにできます。

---

## 8. API・URL 一覧

### 画面(HTML)

| メソッド | パス | 内容 | 応答 |
|---|---|---|---|
| `GET` | `/` | `/tasks` へ転送 | 302 |
| `GET` | `/tasks` | タスク一覧画面 | 200 |
| `GET` | `/tasks/new` | タスク登録画面 | 200 |
| `POST` | `/tasks` | 登録を実行 | 302(一覧へ)/ 200(入力エラー時はフォーム再表示) |
| `GET` | `/tasks/{id}/edit` | タスク編集画面 | 200 / 404(存在しない) |
| `POST` | `/tasks/{id}` | 更新を実行 | 302(一覧へ)/ 200(入力エラー時)/ 404 |
| `POST` | `/tasks/{id}/delete` | 削除を実行 | 302(一覧へ)/ 404 |

### REST API(JSON)

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

注意点:

- サンプルデータが 5 件あるため、API で新しく登録したタスクの ID は 6 から始まります。
- `PUT` で `active` を省略すると `true` に戻ります。
- `POST` で `"active": null` を明示的に送ると、DB の NOT NULL 制約に違反して 500 エラーになる見込みです(コードからの推測で、未確認)。

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
| 4 | Build and start services | `docker compose up -d --build`(標準の `docker-compose.yml` のみ使用) |
| 5 | Wait for database to be ready | `pg_isready` で DB の起動を待つ(最大 150 秒) |
| 6 | Wait for app to be ready | `/api/tasks` が応答するまで待つ(最大 180 秒) |
| 7 | Smoke test API (HTTP checks) | 一覧取得 200 → 登録 201 → 1 件取得 200 → 削除 204 → `/tasks` 画面 200 と表の行数(見出し + 5 件)→ `/` が `/tasks` へ転送されること |
| 8 | Smoke test screens | 登録画面 200 → 空タイトルでエラーメッセージ表示 → フォームで登録 → 編集画面 200 → フォームで更新(Active を外す)→ フォームで削除、の順に確認 |
| 9 | Show logs on failure | 失敗したときだけ `docker compose logs` を表示 |
| 10 | Cleanup containers | 成否に関係なく `docker compose down -v` で後片付け |

### 実行されないワークフロー

- `onboarding-progress-system/.github/workflows/ci-docker-smoke.yml`
  - GitHub はリポジトリ直下の `.github/workflows/` しか読まないため、実行されません。
  - 上のワークフローの古い版なので、削除して問題ありません。

### 出ている警告(動作には影響なし)

- `docker-compose.yml` の `version: "3.8"` は古い書き方だという警告
- `actions/checkout@v4`・`docker/setup-buildx-action@v3` が Node.js 20 向けだという警告

---

## 10. docker compose 起動後にブラウザで確認できる画面

### 起動方法

| 環境 | コマンド |
|---|---|
| 通常(PC・CI) | `export POSTGRES_PASSWORD=<任意>` → `docker compose up -d --build` |
| GitHub Codespaces | `export POSTGRES_PASSWORD=<任意>` → `docker compose -f docker-compose.yml -f docker-compose.codespaces.yml up -d --build` |

Codespaces では、コンテナ同士の通信(アプリ → `db:5432`)が届かないため、専用の上書き設定が必要です。ブラウザで開くときは「ポート」タブの 8080 を開きます(自動で一覧画面に移動します)。

### URL ごとの表示(2026-10-15 に実際に起動して確認)

| URL | HTTP | ブラウザでの表示 | 種類 |
|---|---|---|---|
| `/` | 302 → 200 | `/tasks` に自動で移動し、タスク一覧画面を表示 | ✅ 画面(転送) |
| `/tasks` | 200 | **タスク一覧画面**(下記) | ✅ 画面 |
| `/tasks/new` | 200 | **タスク登録画面**(下記) | ✅ 画面 |
| `/tasks/1/edit` | 200 | **タスク編集画面**(下記) | ✅ 画面 |
| `/api/tasks` | 200 | タスク一覧の JSON(文字の羅列) | API |
| `/api/tasks/1` | 200 | ID 1 のタスクの JSON | API |
| `/tasks/1` | 404 | Whitelabel Error Page | エラー |
| `/tasks/999/edit` | 404 | Whitelabel Error Page | エラー |
| `/api/tasks/999` | 404 | Whitelabel Error Page | エラー |

**画面は「一覧」「登録」「編集」の 3 つです。** `/` を開くと自動で一覧に移動し、そこからボタンで各画面に進めます。

### スクリーンショット相当の説明

#### `/tasks` — タスク一覧画面

```
┌──────────────────────────────────────────────────────────────────────────┐
│                                                                          │
│  タスク一覧                                                              │
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐    │
│  │ タスクを登録しました。            ← 登録・更新・削除の直後だけ表示│    │
│  └──────────────────────────────────────────────────────────────────┘    │
│  [新規登録]                                                              │
│  ┌────┬────────────────────┬──────────────────────┬──────┬───────────┐   │
│  │ ID │ タイトル           │ 説明                 │Active│ 操作      │   │
│  ├────┼────────────────────┼──────────────────────┼──────┼───────────┤   │
│  │ 1  │ PCのセットアップ   │ 支給PCに開発ツール…  │ Yes  │[編集][削除]│  │
│  │ 2  │ 社内アカウントの…  │ メール・チャット…    │ Yes  │[編集][削除]│  │
│  │ 3  │ 就業規則の確認     │ 就業規則とセキュ…    │ Yes  │[編集][削除]│  │
│  │ 4  │ チームメンバーと…  │ 配属チームのメン…    │ Yes  │[編集][削除]│  │
│  │ 5  │ 旧オリエンテー…    │ 現在は使われてい…    │ No   │[編集][削除]│  │
│  └────┴────────────────────┴──────────────────────┴──────┴───────────┘   │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

- 白い背景に、左上に大きな見出し「タスク一覧」が表示されます。
- その下に、画面の横幅いっぱいの表があります。
  - 見出し行(ID・タイトル・説明・Active)は薄いグレーの背景です。
  - 各セルは薄いグレーの線で区切られています。
  - 文字はゴシック体(sans-serif)で表示されます。
- 行は ID の小さい順に並びます。サンプルデータのみの状態では 5 行です。
- Active 列は `true` が「Yes」、`false` が「No」と表示されます(5 行目のみ No)。
- 表の上に青い「新規登録」ボタンがあります。押すと登録画面に移動します。
- 各行の右端に、白い「編集」ボタンと赤い「削除」ボタンがあります。
  - 「削除」を押すと「このタスクを削除しますか?」という確認が出て、OK で削除されます。
- 登録・更新・削除の直後は、表の上に薄い緑の枠で「タスクを登録しました。」などのメッセージが表示されます(再読み込みすると消えます)。
- タスクが 0 件のときは、表の代わりに「タスクはまだありません。」と表示されます。
- API で追加・削除したタスクは、ページを再読み込みすると反映されます。

#### `/tasks/new` — タスク登録画面 / `/tasks/{id}/edit` — タスク編集画面

```
┌──────────────────────────────────────────────┐
│  タスク登録            (編集時は「タスク編集」)│
│                                              │
│  タイトル *                                  │
│  ┌────────────────────────────────────┐      │
│  │                                    │      │
│  └────────────────────────────────────┘      │
│  タイトルを入力してください  ← 未入力で送信した時だけ赤字で表示 │
│                                              │
│  説明                                        │
│  ┌────────────────────────────────────┐      │
│  │                                    │      │
│  │                                    │      │
│  └────────────────────────────────────┘      │
│                                              │
│  ☑ 有効(Active)                             │
│                                              │
│  [登録する] [一覧に戻る]  (編集時は「更新する」)│
└──────────────────────────────────────────────┘
```

- 登録と編集は同じ画面(`task-form.html`)を使い、見出しとボタンの文字だけが変わります。
- 登録画面では入力欄が空で、「有効(Active)」にはチェックが入っています。
- 編集画面では、今の値があらかじめ入っています。
- 「タイトル」の横の赤い `*` は必須項目の印です。
- タイトルを空のまま送信すると、同じ画面のまま、入力欄の下に赤字で「タイトルを入力してください」と表示されます。
- 「一覧に戻る」を押すと、保存せずに一覧に戻ります。

#### `/api/tasks` — JSON 表示

- 画面上部に「Pretty-print」のチェックボックス(Chrome の機能)があり、その下に JSON がそのまま 1 行で表示されます。
- 例: `[{"id":1,"title":"PCのセットアップ","description":"支給PCに開発ツールをインストールする","active":true,"createdAt":"…","updatedAt":"…"}, …]`
- 「Pretty-print」にチェックを入れると、改行と字下げで読みやすくなります。

#### `/tasks/1` など存在しない URL — エラーページ

```
Whitelabel Error Page
This application has no explicit mapping for /error, so you are seeing this as a fallback.

Thu Oct 15 03:13:59 UTC 2026
There was an unexpected error (type=Not Found, status=404).
```

- 白い背景に、大きな見出し「Whitelabel Error Page」と英語の説明文が表示されます。
- アプリの故障ではなく、「その URL に対応する画面がない」という意味です。

---

## 11. 次に実装すべき内容

README の MVP を実現する順番で並べています。上から順に進めるのがおすすめです。

### 優先度 高(MVP に必要)

1. **テストの追加**
   - 今ある Task API と画面(一覧・登録・編集・削除)のテストを書きます(`@SpringBootTest` + `MockMvc` など)。
   - このあと機能を増やしたときに、既存の機能が壊れていないかを確認できるようになります。
2. ~~**トップページ(`/`)**~~ → 対応済み(`/` は `/tasks` へ自動で移動)
3. ~~**画面からのタスク登録・編集・削除**~~ → 対応済み
4. **ユーザー(社員)の管理**
   - `users` テーブル(`V3__create_users_table.sql`)、`User` Entity、一覧画面を作ります。
5. **タスクの割り当てと進捗状態**
   - 「誰に・どのタスクを・どの状態で」を持つテーブルを追加します(例: `task_assignments`。`user_id`、`task_id`、`status`(未着手 / 対応中 / 完了)、`due_date`、`completed_at`)。
   - `tasks` はタスクのテンプレート(マスタ)、`task_assignments` は各人の進捗、という役割分担にすると README の要件に合います。
6. **進捗の確認画面**
   - ユーザーごとの完了率、未完了タスクの一覧を表示します。

### 優先度 中(品質・保守性)

7. **Service 層の追加**: Controller から業務ロジックを分けます。割り当て機能を作るタイミングで導入すると自然です。
8. **エラーレスポンス・エラー画面の統一**: `@RestControllerAdvice` で API のエラーを同じ形の JSON にし、画面用には `templates/error/404.html` などを用意します。
9. **CI でテストを実行する**: 1 を作ったら、ワークフローに `mvn test` を追加します。
10. **`active: null` の扱いを修正**: 8 章の注意点のとおり、500 エラーにならないようにします。

### 優先度 低(整理・後回しでよいもの)

11. 実行されないワークフロー `onboarding-progress-system/.github/workflows/ci-docker-smoke.yml` を削除する
12. `docker-compose.yml` から `version: "3.8"` を削除して警告を消す
13. Actions のバージョンを上げて Node.js 20 の警告を消す
14. `application.yml` の予備パスワード `pass` を見直す(ローカル開発用の初期値)
15. ログイン機能(Spring Security)。管理者と新入社員で見られる画面を分ける場合に必要になります
