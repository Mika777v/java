# GitHub Copilot 向けの指示

このリポジトリはオンボーディング進捗管理システム(`onboarding-progress-system/`)です。
Java 21 / Spring Boot 3.3 / Thymeleaf / Spring Security / Spring Data JPA / PostgreSQL 15 / Flyway / Maven。
詳しい前提は `docs/ai/` にあります(`project-context.md`, `architecture.md`, `database.md`, `conventions.md`)。

## コードを書くときの決まり

- 識別子・コメントは英語、画面の文言・メッセージ・検証メッセージは日本語
- コンストラクタインジェクションを使う。Lombok は使わない
- 「今日」「今」は注入された `Clock` から取る(`LocalDate.now(clock)`)。業務ロジックで `LocalDate.now()` を使わない
- 見つからないときは `ResponseStatusException(HttpStatus.NOT_FOUND, ...)`
- 画面の入力は `form/` のクラスで受ける(Entity に直接バインドしない)。更新は POST + リダイレクト、結果は flash 属性 `message` / `error`
- 新しい画面は、何もしなければ管理者(`ADMIN`)専用になる。新入社員向けは `/me/**` の下に作る
- 新入社員が触るデータは、ログイン中のユーザーのものかをクエリ条件で確認する(例: `findByIdAndUserId`)
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

- 修正は最小限。頼まれていないリファクタリングや整形はしない
- `master` に直接変更しない。作業ブランチで変更して PR を作る
