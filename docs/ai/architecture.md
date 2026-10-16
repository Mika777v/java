# アーキテクチャ(AI 向け)

パッケージのルートは `onboarding-progress-system/src/main/java/com/example/onboarding/`。
以下、パスはこのルートからの相対パスです。

## レイヤー構成と依存の向き

```
ブラウザ ──▶ controller/*PageController, DashboardController, MyPageController ──▶ templates/*.html
curl ────▶ controller/TaskController(REST, /api/tasks)
                 │
                 ▼
            service/(業務ルール。@Transactional)      form/(画面の入力値。検証メッセージは日本語)
                 │
                 ▼
            repository/(Spring Data JPA)──▶ entity/ ──▶ PostgreSQL(Flyway で作成)
```

- **Controller は薄く保つ。** 複数テーブルにまたがる処理や業務ルールは `service/` に置く
  (例: 割り当ての重複チェック・無効タスクの禁止は `AssignmentService`)
- 単純な 1 テーブルの CRUD(タスク・ユーザー)は Controller から Repository を直接呼んでいる。
  新しいコードもこの既存スタイルに合わせてよい(無理にサービス層を挟まない)
- 画面のフォームは `form/` のクラスで受け取り、Entity に直接バインドしない(`id` などを書き換えられないように)。
  REST API(`TaskController`)だけは Entity `Task` をそのまま受け取り、`id` を `null` にしてから保存している

## クラス一覧

| パッケージ | クラス | 役割 |
|---|---|---|
| (ルート) | `OnboardingProgressSystemApplication` | 起動クラス |
| `config` | `TimeConfig` | `Clock` Bean(`app.time-zone`)。「今日」は必ずこの `Clock` から取る |
| `controller` | `DashboardController` | `GET /`。新入社員は `/me` へリダイレクト |
| | `LoginController` | `GET /login` |
| | `MyPageController` | `/me`、自分の割り当ての状態変更(他人の割り当ては 404) |
| | `TaskController` | REST API `/api/tasks`(JSON) |
| | `TaskPageController` | `/tasks` 画面の CRUD と並べ替え(↑↓) |
| | `UserPageController` | `/users` 画面の CRUD、絞り込み、詳細、割り当て、状態変更 |
| `entity` | `Task`, `User`, `TaskAssignment` | JPA エンティティ。`created_at`/`updated_at` は `@PrePersist`/`@PreUpdate` で設定 |
| | `AssignmentStatus` | `NOT_STARTED`(未着手)/ `IN_PROGRESS`(対応中)/ `DONE`(完了)。`getLabel()` が日本語 |
| | `Role` | `ADMIN`(管理者)/ `MEMBER`(新入社員) |
| `form` | `TaskForm`, `UserForm` | 画面の入力値と検証。`UserForm` はパスワード(空欄 = 変更しない)を持つ |
| `repository` | `TaskRepository` | `findAllInDisplayOrder()`(`sortOrder`, `id` 順)、`findMaxSortOrder()` |
| | `UserRepository` | メールの重複確認(大文字小文字を区別しない)、役割での存在確認 |
| | `TaskAssignmentRepository` | ユーザーごとの集計 `countProgressByUser`、状態ごとの件数、期限が近い割り当て |
| `security` | `SecurityConfig` | 2 つの `SecurityFilterChain`(下記) |
| | `AppUserDetails`, `AppUserDetailsService` | ログインユーザー(`userId`, `displayName` を持つ)。パスワード未設定のユーザーはログイン不可 |
| | `AdminAccountInitializer` | 起動時、管理者が 0 人なら `APP_ADMIN_*` で作成(既存ユーザーなら昇格) |
| `service` | `AssignmentService` | 割り当て・進捗のルール。失敗は `AssignmentException`(日本語メッセージ)を投げる |
| | `TaskOrderService` | 並び順。`move()` で全タスクを 10 刻みに振り直す |
| | `DashboardService`, `Dashboard` | ダッシュボード用の集計(管理者は対象外、期限が近い = 今日から 7 日) |
| | `UserProgress`, `ProgressFilter` | 進捗(合計・完了・期限切れ・完了率)と一覧の絞り込み条件 |
| `web` | `CurrentUserAdvice` | 全画面に `${currentUserName}` を渡す |
| | `DateTimeDisplay` | `@Component("dates")`。テンプレートで `${@dates...}` として日時を表示 |

## セキュリティ(`security/SecurityConfig`)

| 対象 | 認証方式 | 権限 | CSRF |
|---|---|---|---|
| `/api/**`(`@Order(1)`) | HTTP Basic(`curl -u email:password`) | `ADMIN` のみ | 無効(セッションなし) |
| `/login`, `/css/**`, `/error`, `/favicon.ico` | — | 誰でも | — |
| `/`, `/me`, `/me/**` | フォームログイン | ログイン済みなら誰でも | 有効 |
| それ以外すべて | フォームログイン | `ADMIN` のみ | 有効 |

- 新しい画面を追加すると、**何もしなければ管理者専用** になる(`anyRequest().hasRole("ADMIN")`)
- 新入社員向けの画面を増やすときは `/me/**` の下に作るのが一番簡単
- テンプレートでは `sec:authorize="hasRole('ADMIN')"` で表示を切り替える
- 管理者は自分を新入社員に変更したり、自分を削除したりできない(`UserPageController` でチェック)

## URL 一覧

### 画面(HTML)

| メソッド | URL | 権限 | 内容 |
|---|---|---|---|
| GET | `/login` | 誰でも | ログイン画面(`POST /login` は Spring Security が処理) |
| POST | `/logout` | ログイン済み | ログアウト → `/login?logout` |
| GET | `/` | ログイン済み | 管理者: ダッシュボード / 新入社員: `/me` へ |
| GET | `/me` | ログイン済み | マイページ |
| POST | `/me/assignments/{aid}/status` | ログイン済み | 自分の割り当ての状態変更 |
| GET | `/tasks`, `/tasks/new`, `/tasks/{id}/edit` | ADMIN | タスク一覧・登録・編集 |
| POST | `/tasks`, `/tasks/{id}`, `/tasks/{id}/delete` | ADMIN | 登録・更新・削除 |
| POST | `/tasks/{id}/move-up`, `/tasks/{id}/move-down` | ADMIN | 並べ替え |
| GET | `/users?department=&status=` | ADMIN | ユーザー一覧(`status` は `ProgressFilter`) |
| GET | `/users/new`, `/users/{id}/edit`, `/users/{id}` | ADMIN | 登録・編集・詳細 |
| POST | `/users`, `/users/{id}`, `/users/{id}/delete` | ADMIN | 登録・更新・削除 |
| POST | `/users/{id}/assignments` | ADMIN | タスクを 1 件割り当て(`taskId`, `dueDate`) |
| POST | `/users/{id}/assignments/all` | ADMIN | 有効なタスクをすべて割り当て |
| POST | `/users/{id}/assignments/{aid}/status` | ADMIN | 状態変更 |
| POST | `/users/{id}/assignments/{aid}/delete` | ADMIN | 割り当て解除 |

画面の更新系はすべて **POST + リダイレクト**(PRG パターン)。結果は flash 属性 `message`(成功)/ `error`(失敗)で
次の画面に渡し、`fragments/nav :: messages` が表示する。PUT / DELETE は画面では使わない。

### REST API(JSON、ADMIN のみ)

| メソッド | URL | 成功時 | 補足 |
|---|---|---|---|
| GET | `/api/tasks` | 200 | 表示順 |
| GET | `/api/tasks/{id}` | 200 / 404 | |
| POST | `/api/tasks` | 201 | `id` は無視。`active` 省略時 true、`sortOrder` 省略時は末尾 |
| PUT | `/api/tasks/{id}` | 200 / 404 | `sortOrder` 省略時は今の値のまま |
| DELETE | `/api/tasks/{id}` | 204 / 404 | 割り当ても削除される(DB の `ON DELETE CASCADE`) |

ユーザー・割り当ての REST API はまだない(backlog.md)。

## エラー処理

- 見つからないときは Controller で `throw new ResponseStatusException(HttpStatus.NOT_FOUND, ...)`
- 不正な enum 値(`?status=BAD` など)は Spring の型変換エラーで 400 になる
- 画面のエラーは `templates/error/404.html`, `403.html`, `4xx.html`, `5xx.html`(共通部品 `fragments/error.html`)。
  例外の詳細は画面に出さない。`/api/**` の JSON リクエストには Spring 標準の JSON エラーが返る
- 業務ルール違反(重複割り当てなど)は 4xx にせず、flash の `error` で元の画面に戻す

## テンプレート(`src/main/resources/templates/`)

| ファイル | 画面 |
|---|---|
| `fragments/nav.html` | 上部メニュー `nav(current)` と結果メッセージ `messages` |
| `fragments/error.html` | エラー画面の本文(権限に応じたボタン) |
| `login.html`, `my-page.html`, `dashboard.html` | ログイン、マイページ、ダッシュボード |
| `tasks.html`, `task-form.html` | タスク一覧、登録・編集(共用) |
| `users.html`, `user-form.html`, `user-detail.html` | ユーザー一覧、登録・編集(共用)、詳細 |
| `error/*.html` | エラー画面 |

## 実行環境

| 環境 | 構成 |
|---|---|
| Docker Compose | `docker-compose.yml`: `db`(postgres:15、ヘルスチェックあり)と `app`(Dockerfile のマルチステージビルド) |
| Codespaces | 上に `docker-compose.codespaces.yml` を重ねる。`app` を `network_mode: host` にして DB へ `localhost:5432` で接続 (Codespaces の Docker 29 ではコンテナ間通信が失敗するための回避策) |
| GitHub Actions | `test` ジョブ(`mvn -B test`)と `call-reusable / run-smoke`(Docker で起動し curl で確認)。両方とも master のルールセットで必須チェック |
| テスト | Testcontainers が PostgreSQL 15 を起動。Flyway のマイグレーションとサンプルデータがそのまま入る |
