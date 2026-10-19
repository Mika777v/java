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
- **目指す構成は Controller → Service → Repository → DB。** 新しいコードは Service を通す
- 既存の単純な CRUD(タスク・ユーザー)は Controller から Repository を直接呼んでいる
  (`TaskPageController`, `TaskController`, `UserPageController`, `MyPageController`)。
  これは **改善候補(技術的負債)** として扱う。直すときは機能追加と混ぜず、別の PR で行う
  (候補と直し方: `.claude/skills/review-and-refactor/refactoring-catalog.md`、一覧: `backlog.md` の技術的負債)
- `security/`・`web/` が `UserRepository` を使うのは、ログイン処理・共通処理での検索だけなので対象外
- 画面のフォームは `form/` のクラスで受け取り、Entity に直接バインドしない(`id` などを書き換えられないように)。
  REST API(`TaskController`)は入力を `TaskController.TaskRequest`(record)で受け取る(`id`・日時は受け付けない。`active` の省略を見分けられる)。
  出力はまだ Entity `Task` をそのまま返している(JSON の形を変えないため。backlog.md の D5)

## クラス一覧

| パッケージ | クラス | 役割 |
|---|---|---|
| (ルート) | `OnboardingProgressSystemApplication` | 起動クラス |
| `config` | `WebConfig` | `RemovedUserLogoutInterceptor`(先)と `PasswordChangeRequiredInterceptor` を登録(除外: `/login`, `/join/**`, `/css/**`, `/error`。後者は `/me/password` も) |
| | `TimeConfig` | `Clock` Bean(`app.time-zone`)。「今日」は必ずこの `Clock` から取る |
| `controller` | `DashboardController` | `GET /`。新入社員は `/me` へリダイレクト |
| | `LoginController` | `GET /login` |
| | `JoinController` | `GET /join/{token}`。専用URLが有効ならログイン状態にして `/me` へ。無効なら `join-invalid.html`(404) |
| | `MyPageController` | `/me`、自分の割り当ての状態変更(他人の割り当ては 404)、自分のパスワード変更(`/me/password`。専用URL方式の新入社員は 404) |
| | `TaskController` | REST API `/api/tasks`(JSON) |
| | `TaskPageController` | `/tasks` 画面の CRUD と並べ替え(↑↓) |
| | `UserPageController` | `/users` 画面の CRUD、絞り込み、詳細、割り当て、状態変更 |
| `entity` | `Task`, `User`, `TaskAssignment` | JPA エンティティ。`created_at`/`updated_at` は `@PrePersist`/`@PreUpdate` で設定 |
| | `AssignmentStatus` | `NOT_STARTED`(未着手)/ `IN_PROGRESS`(対応中)/ `DONE`(完了)。`getLabel()` が日本語 |
| | `Role` | `ADMIN`(管理者)/ `MEMBER`(新入社員) |
| `form` | `TaskForm`, `UserForm` | 画面の入力値と検証。`UserForm` はパスワード(空欄 = 変更しない)を持つ |
| | `PasswordChangeForm` | 自分のパスワード変更の入力値。DB が必要なチェック(現在のパスワード)は Controller で行う |
| `repository` | `TaskRepository` | `findAllInDisplayOrder()`(`sortOrder`, `id` 順)、`findMaxSortOrder()` |
| | `UserRepository` | メールの重複確認(大文字小文字を区別しない)、役割での存在確認 |
| | `TaskAssignmentRepository` | ユーザーごとの集計 `countProgressByUser`、状態ごとの件数、期限が近い割り当て |
| `security` | `SecurityConfig` | 2 つの `SecurityFilterChain`(下記) |
| | `AppUserDetails`, `AppUserDetailsService` | ログインユーザー(`userId`, `displayName` を持つ)。パスワード未設定のユーザーはログイン不可。専用URL方式では、パスワードでログインできるのは管理者だけ |
| | `LoginSettings` | `@Component("loginSettings")`。新入社員のログイン方法(`app.member-login` = `link` / `password`)。テンプレートでは `${@loginSettings.linkMode}` |
| | `AdminAccountInitializer` | 起動時、管理者が 0 人なら `APP_ADMIN_*` で作成(既存ユーザーなら昇格)。`app.admin.password-reset=true` なら、その管理者のパスワードを戻して変更必須にする |
| | `TemporaryPasswords` | 仮パスワード(12 文字、紛らわしい文字を除く)を `SecureRandom` で生成 |
| `service` | `AccessLinkService` | 専用URLの発行(32 バイトの乱数 → URL 用 43 文字。DB には SHA-256 だけ保存)・確認(期限内か)・無効化 |
| | `AssignmentService` | 割り当て・進捗のルール。失敗は `AssignmentException`(日本語メッセージ)を投げる |
| | `TaskOrderService` | 並び順。`move()` で全タスクを 10 刻みに振り直す |
| | `DashboardService`, `Dashboard` | ダッシュボード用の集計(管理者は対象外、期限が近い = 今日から 7 日) |
| | `UserProgress`, `ProgressFilter` | 進捗(合計・完了・期限切れ・完了率)と一覧の絞り込み条件 |
| `web` | `RemovedUserLogoutInterceptor` | ログイン中のユーザーが削除されたか、役割が変わっていたら、セッションを無効にして `/login` へ(API は 401)。セッションにはログイン時の役割が残るため、毎回 DB と比べる |
| | `PasswordChangeRequiredInterceptor` | 「変更が必要」のユーザーを `/me/password` へ転送(API は 403)。毎回 DB を見るので再ログイン不要 |
| | `CurrentUserAdvice` | 全画面に `${currentUserName}` を渡す |
| | `LinkText` | `@Component("links")`。`${@links.linkify(text)}` + `th:utext` で、`http(s)://` の URL だけをリンク(新しいタブ、`rel="noopener noreferrer"`)にし、ほかは HTML エスケープ。タスクの説明の表示に使用 |
| | `DateTimeDisplay` | `@Component("dates")`。テンプレートで `${@dates...}` として日時を表示 |

## セキュリティ(`security/SecurityConfig`)

| 対象 | 認証方式 | 権限 | CSRF |
|---|---|---|---|
| `/api/**`(`@Order(1)`) | HTTP Basic(`curl -u email:password`) | `ADMIN` のみ | 無効(セッションなし) |
| `/login`, `/join/**`, `/css/**`, `/error`, `/favicon.ico` | — | 誰でも | — |
| `/actuator/health` | — | 誰でも(`UP` / `DOWN` だけを返す) | — |
| `/`, `/me`, `/me/**` | フォームログイン | ログイン済みなら誰でも | 有効 |
| それ以外すべて | フォームログイン | `ADMIN` のみ | 有効 |

- 新しい画面を追加すると、**何もしなければ管理者専用** になる(`anyRequest().hasRole("ADMIN")`)
- 新入社員向けの画面を増やすときは `/me/**` の下に作るのが一番簡単
- 新入社員のログインは、専用URL(既定)かパスワードか(`app.member-login`)。管理者は常にパスワード。
  専用URLでのログインは `JoinController` が SecurityContext を自分でセッションに保存している(セッション ID も変更)
- 専用URLを無効化しても、すでにログイン中のセッションはログアウトかタイムアウトまで有効
- **リダイレクトはパスだけ**(`/users/3`)にする。Codespaces などのプロキシの裏ではアプリから見たアドレスが `localhost:8080` なので、
  `http://localhost:8080/...` を返すとブラウザが localhost に行って失敗する。`server.tomcat.use-relative-redirects: true`、
  `SecurityConfig` のログイン画面への移動とログイン後の移動もパスだけにしている(`RelativeRedirectTest` で確認)
- 管理者がほかの人に設定したパスワードは `users.password_change_required = true` になり、本人が変更するまで
  `PasswordChangeRequiredInterceptor` が他の画面を止める。ログイン中でも開ける必要がある画面を増やしたら `WebConfig` の除外に追加する
- テンプレートでは `sec:authorize="hasRole('ADMIN')"` で表示を切り替える
- 管理者は自分を新入社員に変更したり、自分を削除したりできない(`UserPageController` でチェック)
- ほかの管理者に削除された・役割を変えられた人は、次の画面操作でログアウトされる(`RemovedUserLogoutInterceptor`)。
  専用URLの無効化だけではログアウトされない(オーナー判断。backlog.md)
- 「確認してから保存」(メールの重複・割り当ての重複)は、同時に 2 つのリクエストが来ると確認をすり抜ける。
  DB の一意制約(`uk_users_email_lower`, `uk_task_assignments_user_task`)が最後の砦で、違反したら
  `saveAndFlush` で例外を受け止め、いつものメッセージ(入力エラー / flash の `error`)を出す

## URL 一覧

### 画面(HTML)

| メソッド | URL | 権限 | 内容 |
|---|---|---|---|
| GET | `/login` | 誰でも | ログイン画面(`POST /login` は Spring Security が処理) |
| GET | `/actuator/health` | 誰でも | ヘルスチェック。正常 200 `{"status":"UP"}`、異常 503 `DOWN`。CI・Codespaces の起動待ちに使う。ほかの Actuator は非公開(`management.endpoints.web.exposure.include: health`) |
| GET | `/join/{token}` | 誰でも | 専用URLでログイン → `/me`。`password` 方式では 404 |
| POST | `/logout` | ログイン済み | ログアウト → `/login?logout` |
| GET | `/` | ログイン済み | 管理者: ダッシュボード / 新入社員: `/me` へ |
| GET | `/me` | ログイン済み | マイページ |
| POST | `/me/assignments/{aid}/status` | ログイン済み | 自分の割り当ての状態変更 |
| GET / POST | `/me/password` | ログイン済み | 自分のパスワード変更(成功後、管理者は `/`、新入社員は `/me` へ) |
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
| POST | `/users/{id}/password-reset` | ADMIN | 仮パスワードを発行(flash 属性 `temporaryPassword` で 1 回だけ表示、変更必須)。自分自身はエラー、専用URL方式の新入社員は 404 |
| POST | `/users/{id}/access-link` | ADMIN | 専用URLを発行・再発行(flash 属性 `accessLink` で次の画面に 1 回だけ表示)。`password` 方式では 404 |
| POST | `/users/{id}/access-link/delete` | ADMIN | 専用URLを無効化 |

画面の更新系はすべて **POST + リダイレクト**(PRG パターン)。結果は flash 属性 `message`(成功)/ `error`(失敗)で
次の画面に渡し、`fragments/nav :: messages` が表示する。PUT / DELETE は画面では使わない。

### REST API(JSON、ADMIN のみ)

| メソッド | URL | 成功時 | 補足 |
|---|---|---|---|
| GET | `/api/tasks` | 200 | 表示順 |
| GET | `/api/tasks/{id}` | 200 / 404 | |
| POST | `/api/tasks` | 201 | `Location: /api/tasks/{id}` を返す。`id` は無視。`active` 省略時 true、`sortOrder` 省略時は末尾 |
| PUT | `/api/tasks/{id}` | 200 / 404 | `active`・`sortOrder` 省略時は今の値のまま |
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
| `password-form.html` | 自分のパスワード変更 |
| `join-invalid.html` | 専用URLが無効(期限切れ・再発行済み・無効化)のときの画面 |
| `tasks.html`, `task-form.html` | タスク一覧、登録・編集(共用) |
| `users.html`, `user-form.html`, `user-detail.html` | ユーザー一覧、登録・編集(共用)、詳細 |
| `error/*.html` | エラー画面 |

## 実行環境

| 環境 | 構成 |
|---|---|
| Docker Compose | `docker-compose.yml`: `db`(postgres:15、ヘルスチェックあり)と `app`(Dockerfile のマルチステージビルド) |
| Codespaces | 土台は `mcr.microsoft.com/devcontainers/java:1-21-bookworm` + Maven・Docker の追加部品(`.devcontainer/devcontainer.json`)。以前の `universal:2`(10 GB 以上)は準備が終わらなくなったため変更。Compose は上に `docker-compose.codespaces.yml` を重ねる。`app` を `network_mode: host` にして DB へ `localhost:5432` で接続 (Codespaces の Docker 29 ではコンテナ間通信が失敗するための回避策) |
| GitHub Actions | `test` ジョブ(`mvn -B test`)と `call-reusable / run-smoke`(Docker で起動し curl で確認)。両方とも master のルールセットで必須チェック |
| テスト | Testcontainers が PostgreSQL 15 を起動。Flyway のマイグレーションとサンプルデータがそのまま入る |
