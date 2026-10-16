# 開発ルール・コーディング規約(AI 向け)

既存コードに合わせることが最優先です。迷ったら同じ種類の既存ファイルを開いて真似してください。

## 言語の使い分け

| 対象 | 言語 |
|---|---|
| クラス名・メソッド名・変数名・コメント・Javadoc | 英語 |
| 画面の文言、flash メッセージ、検証メッセージ、例外 `AssignmentException` のメッセージ | 日本語 |
| テストメソッド名 | 英語の文章(`createShowsErrorWhenTitleIsBlank`) |
| コミットメッセージ・PR タイトル | 英語(命令形。例: `Add login with administrator and new employee roles`) |
| PR 本文、`docs/` のドキュメント、ユーザーへの説明 | 日本語(専門用語は少なめに) |

## Java

- コンストラクタインジェクションを使う(`@Autowired` フィールドは本体コードでは使わない。テストでは可)
- Lombok は使っていない。getter / setter は手で書く
- コメントは「なぜそうするか」を短く書く。何をしているかが明らかなコードには書かない
- 見つからない場合は `ResponseStatusException(HttpStatus.NOT_FOUND, "... not found")`
- 「今日」「今」は `LocalDate.now(clock)` / `Instant.now(clock)`。**業務ロジックで `LocalDate.now()` を直接呼ばない**(テストで日付を固定できなくなる)。
  例外は Entity の `created_at` / `updated_at`(`@PrePersist` / `@PreUpdate` で `Instant.now()`)だけ
- 状態や役割は enum(`getLabel()` で日本語名)。DB には文字列で保存(`EnumType.STRING`)
- 画面の入力は `form/` のクラスで受け、`@Valid` + `BindingResult`。前後の空白は form 側で取り除く
- 読み取りだけのサービスメソッドは `@Transactional(readOnly = true)`
- 一覧の N+1 を避けるため、関連を表示するクエリには `@EntityGraph` を付ける

## 画面(Thymeleaf)

- 各画面の先頭で `<nav th:replace="~{fragments/nav :: nav('tasks')}">` と `<div th:replace="~{fragments/nav :: messages}">` を使う
- 更新系フォームは `method="post"` + `th:action`(CSRF トークンが自動で入る)。削除は `onsubmit="return confirm('...')"`
- ボタンのクラスは `button` / `button-secondary` / `button-danger`、補足文は `muted small`
- 日時の表示は `${@dates.format(...)}`、期限切れ判定は `${@dates.isOverdue(...)}`
- 利用者が入力した文字は `th:text`(エスケープあり)で表示する。`th:utext` はエスケープ済みの HTML を返す `${@links.linkify(...)}` にだけ使う
- スタイルは `static/css/app.css` に追記する(インライン style は進捗バーの幅など値が変わるものだけ)
- JavaScript は使っていない。追加する場合は事前にオーナーに相談

## セキュリティ

- 新しい画面は既定で管理者専用。新入社員にも見せるなら `/me/**` の下に作るか `SecurityConfig` を変更し、テストを追加する
- 新入社員が触れるデータは「ログイン中のユーザーのものか」を必ずクエリ条件で確認する
  (例: `findByIdAndUserId`。他人のデータは 404 にする)
- パスワードは `PasswordEncoder`(BCrypt)でハッシュ化。平文をログやモデル属性に出さない。8〜72 文字
- **認証情報をコード・テスト・ドキュメント・ログに書かない。** テストで必要なパスワードは
  テスト用プロパティ(`@SpringBootTest(properties = ...)`)にダミー値を置く

## テスト

| 種類 | 書き方 | 例 |
|---|---|---|
| 画面・API(大半) | `@SpringBootTest` + `@AutoConfigureMockMvc` + `@Import({TestcontainersConfiguration.class, TestClockConfiguration.class})` + `@Transactional` + `@WithMockUser(roles = "ADMIN")` | `TaskPageControllerTest` |
| 権限 | 上記で `@WithMockUser` を変える、または `user(...)` / `httpBasic(...)` | `SecurityTest` |
| 実サーバー(エラー画面など) | `webEnvironment = RANDOM_PORT` + `web/BrowserSession`(Cookie と CSRF を扱う HTTP クライアント) | `ErrorPageTest` |
| 単体 | Spring なしの普通の JUnit | `UserProgressTest`, `TaskAssignmentTest` |

- POST / PUT / DELETE は `com.example.onboarding.WebTestSupport.post/put/delete` を使う(CSRF トークン付き)。
  `MockMvcRequestBuilders.post` を直接使うと 403 になる
- `@Transactional` でテストごとにロールバックされるので、サンプルデータ(database.md)を前提にしてよい
- サンプルデータの件数・id・並び順は **`SampleData`(テスト用)の定数を使う**。数字を直接書かない。
  サンプルデータ(V2・V4・V9 など)を変えたら `SampleData.java` を直す。値はアプリのコードで計算せず、マイグレーションから手で写す
- 登録・削除で件数が変わるかは、`long before = repository.count();` と比べて確かめる(`before + 1` など)
- 日付は 2026-10-15 に固定されている。期限切れのテストはこの日付を基準に書く
- 画面の確認は `content().string(containsString("..."))` で日本語の文言を直接確かめる
- 新機能には「正常系」「入力エラー」「存在しない id で 404」「権限がない場合」を最低限書く
- Testcontainers のバージョン(1.21.4)は Docker 29 対応のために固定している。下げない

## 変更したら更新するもの

| 変更 | 一緒に更新するもの |
|---|---|
| 画面・URL の追加 | `reusable-docker-smoke.yml` のスモークテスト(必要なら)、`docs/current-status.md`、`docs/ai/architecture.md` |
| テーブル変更 | 新しい `V*.sql`、Entity、`docs/ai/database.md`、`docs/current-status.md` の表 |
| 環境変数の追加 | `application.yml`、`docker-compose.yml`、CI(`reusable-docker-smoke.yml`)、`.devcontainer/start-app.sh`、README |
| 機能の完了・追加 | `docs/ai/backlog.md` |
| 画面・操作の追加や変更 | `docs/codespaces-check.md` のチェックリスト |
| `docs/java-guide.md` に引用しているコードの変更 | `docs/java-guide.md` の該当箇所 |

## Git / PR

- ブランチ: 作業用ブランチで作業(`master` には直接 push しない)。マージはオーナーが GitHub 上で行う
- マージ済みの PR のブランチを再利用するときは、最新の `master` から作り直す
- 1 つの PR は 1 つのテーマ。関係ない整形・リファクタリングを混ぜない
- PR 本文(日本語): 「何を変えたか」「なぜ」「確認方法(Codespaces での手順)」「テスト結果」
- push 前に必ずローカルで確認する:
  1. `mvn -B test` が通る
  2. 画面を変えたら Docker で起動して、スモークテストの手順(`reusable-docker-smoke.yml`)が通る
- CI の 2 つのチェック(`test`, `call-reusable / run-smoke`)は master のルールセットで必須。両方緑になるまでが作業
  (ドキュメントだけの PR では `changes` ジョブの判定でスキップされ、スキップ=成功扱い。
  `run-smoke` のスキップは呼び出し先の `inputs.run` で行う。呼び出し元の job に `if` を付けるとチェック名が変わるので付けない)
