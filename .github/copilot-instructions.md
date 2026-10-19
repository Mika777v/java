# GitHub Copilot 向けの指示

このリポジトリはオンボーディング進捗管理システム(`onboarding-progress-system/`)です。
Java 21 / Spring Boot 3.3 / Thymeleaf / Spring Security / Spring Data JPA / PostgreSQL 15 / Flyway / Maven。
詳しい前提は `docs/ai/` にあります(`project-context.md`, `spec.md`, `architecture.md`, `database.md`, `conventions.md`)。
機能を変えるときは `docs/ai/spec.md` の要件 ID と「壊してはいけないルール(INV)」を確認してください。

## コードを書くときの決まり

- 識別子・コメントは英語、画面の文言・メッセージ・検証メッセージは日本語
- コンストラクタインジェクションを使う。Lombok は使わない
- 「今日」「今」は注入された `Clock` から取る(`LocalDate.now(clock)`)。業務ロジックで `LocalDate.now()` を使わない
- 見つからないときは `ResponseStatusException(HttpStatus.NOT_FOUND, ...)`
- 画面の入力は `form/` のクラスで受ける(Entity に直接バインドしない)。更新は POST + リダイレクト、結果は flash 属性 `message` / `error`
- 新しい画面は、何もしなければ管理者(`ADMIN`)専用になる。新入社員向けは `/me/**` の下に作る
- 新入社員が触るデータは、ログイン中のユーザーのものかをクエリ条件で確認する(例: `findByIdAndUserId`)
- 新入社員は既定で専用URL(`/join/{token}`、`AccessLinkService`)でログインし、パスワードは使わない(`app.member-login=password` で切り替え)。管理者は常にパスワード
- パスワード・認証情報をコード・テスト・ログに書かない。パスワードは `PasswordEncoder`(BCrypt)でハッシュ化する

## データベース

- 適用済みの `db/migration/V*.sql` は書き換えない。変更は新しい番号のファイルで行う
- 値の候補が決まっている列は `CHECK` 制約 + `@Enumerated(EnumType.STRING)` の enum

## テスト

- 画面・API のテストは `@SpringBootTest` + `@AutoConfigureMockMvc` +
  `@Import({TestcontainersConfiguration.class, TestClockConfiguration.class})` + `@Transactional` + `@WithMockUser(roles = "ADMIN")`
- POST / PUT / DELETE は `com.example.onboarding.WebTestSupport.post/put/delete` を使う(CSRF トークン付き。使わないと 403)
- 日付は 2026-10-15 に固定。サンプルデータ(山田 4/4 完了、佐藤 2/4 で 2 件期限切れ、鈴木 0/2)を前提にしてよい
- 実行: `cd onboarding-progress-system && mvn -B test`(Docker が必要)

## 変更の進め方

運用ルールの正本はリポジトリ直下の `CLAUDE.md`(「AI 開発運用ルール」)です。要点:

- 修正はタスクの範囲だけ。関係ない整形やリファクタリングを混ぜない
- `master` に直接変更しない。作業ブランチで変更して PR を作る(マージはオーナー)
- DB スキーマ・API 互換性・認証認可・ライブラリの大幅変更・インフラ構成・データ削除・仕様変更は、先にオーナーに確認する
- コミットメッセージは `feat:` / `fix:` / `refactor:` / `test:` / `docs:` / `ci:` + 英語の命令形
- 作業ログ `DEVLOG.md` と引き継ぎ `SESSION_SUMMARY.md`、タスク一覧 `docs/TASKS.md` を更新する
