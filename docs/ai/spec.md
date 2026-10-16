# 仕様(AI 向け): 要件 ID・不変条件・テストとの対応

> 人向けの [要件定義書](../requirements.md) と [設計書](../design.md) を、AI が作業に使いやすい形にまとめたものです。
> **機能を追加・変更するときは、まずここで関係する要件 ID と不変条件(INV)を確認し、対応するテストを壊さないこと。**
> 要件を変える・増やす場合は、オーナーの了承を得てから、このファイルと人向けの 2 つを同時に更新する。

パスの省略: `main/` = `onboarding-progress-system/src/main/java/com/example/onboarding/`、`test/` = `onboarding-progress-system/src/test/java/com/example/onboarding/`

## 1. 前提(要件定義書 3 章)

- 社内・非公開・少人数(ユーザー数十人、タスク数十件)・短期間の利用。インターネット公開は想定しない
- 画面は日本語のみ。JavaScript はほぼ使わない(追加はオーナーに相談)
- 認証情報はコード・テスト・ドキュメント・ログに書かない(環境変数 / GitHub Secrets)

## 2. 機能要件と実装・テストの対応

| ID | 要件(要約) | 主な実装 | 確かめているテスト |
|---|---|---|---|
| FR-01 | タスクの CRUD(タイトル必須・200 文字) | `main/controller/TaskPageController`, `main/form/TaskForm`, `main/entity/Task` | `test/controller/TaskPageControllerTest`, `test/form/TaskFormTest` |
| FR-02 | 表示順(↑↓、新規は末尾) | `main/service/TaskOrderService` | `test/controller/TaskOrderTest` |
| FR-03 | 無効タスクは割り当て不可 | `AssignmentService.assign`, `findAssignableTasks` | `UserAssignmentPageTest.assignShowsErrorForInactiveTask` |
| FR-04 | タスク削除で割り当ても削除 | DB の `ON DELETE CASCADE`(V3) | `UserAssignmentPageTest.deletingTaskAlsoRemovesItsAssignments` |
| FR-05 | 説明の URL をリンクに(他はエスケープ) | `main/web/LinkText`(`@links`)、テンプレートの `th:utext` | `test/web/LinkTextTest`, `TaskPageControllerTest.addressInDescriptionIsShownAsLinkAndHtmlIsNot`, `SecurityTest.myPageShowsAddressesInTaskDescriptionAsLinks` |
| FR-10 | ユーザーの CRUD、メール重複不可(大文字小文字無視) | `main/controller/UserPageController`, `main/form/UserForm` | `test/controller/UserPageControllerTest` |
| FR-11 | ユーザー削除で割り当ても削除 | DB の `ON DELETE CASCADE` | `UserPageControllerTest.deleteRemovesUserAndTheirAssignments` |
| FR-12 | 自分の削除・自分の降格は不可 | `UserPageController`(`isSelf`) | `SecurityTest.administratorCannotDeleteThemselves`, `administratorCannotTakeAwayOwnAdministratorRole` |
| FR-20 | 1 件 / まとめて割り当て(期限つき) | `AssignmentService.assign`, `assignAllActive` | `UserAssignmentPageTest.assign*` |
| FR-21 | 同じタスクの重複割り当て不可 | UNIQUE(user_id, task_id) + `existsByUserIdAndTaskId` | `UserAssignmentPageTest.assignShowsErrorWhenAlreadyAssigned` |
| FR-22 | 状態変更と完了日時の記録・消去 | `TaskAssignment.changeStatus` | `test/entity/TaskAssignmentTest`, `UserAssignmentPageTest.changingStatus*` |
| FR-23 | 期限切れ判定(日本時間の今日) | `TaskAssignment.isOverdue`, `config/TimeConfig`(`Clock`) | `TaskAssignmentTest.overdue*`, `UserPageControllerTest.listCanBeFilteredByOverdue` |
| FR-24 | ユーザー一覧の進捗表示と絞り込み | `UserPageController.listUsers`, `service/UserProgress`, `service/ProgressFilter`, `TaskAssignmentRepository.countProgressByUser` | `UserPageControllerTest.list*`, `test/service/UserProgressTest` |
| FR-25 | ダッシュボード | `main/service/DashboardService`, `Dashboard` | `test/controller/DashboardTest` |
| FR-30 | 管理者はパスワードでログイン | `security/SecurityConfig`, `AppUserDetailsService` | `SecurityTest.administratorCanLogIn` ほか |
| FR-31 | 新入社員は専用URLでログイン | `controller/JoinController`, `service/AccessLinkService` | `AccessLinkTest.validLinkLogsInAndOpensMyPage` |
| FR-32 | 専用URLは推測不可・1 回だけ表示・期限 60 日 | `AccessLinkService`(32 バイト乱数、SHA-256 保存) | `AccessLinkTest.issuedLinkIsLongAndRandomAndOnlyItsHashIsSaved`, `linkIsShownOnlyRightAfterIssuing`, `expiredLinkIsRejected` |
| FR-33 | 再発行・無効化、管理者には発行不可 | `UserPageController.issueAccessLink` / `revokeAccessLink` | `AccessLinkTest.reissuingMakesOldLinkUnusable`, `revokedLinkIsRejected`, `administratorCannotGetLink`, `makingUserAnAdministratorRemovesLink` |
| FR-34 | パスワード方式への切り替え | `security/LoginSettings`(`app.member-login`) | `SecurityTest`(password モードで実行), `SecurityTest.personalLinksAreNotAvailableWithPasswordLogin`, `AccessLinkTest.newEmployeeCannotLogInWithPassword` |
| FR-35 | 新入社員は自分のマイページだけ | `SecurityConfig`, `MyPageController`(`findByIdAndUserId`) | `SecurityTest.newEmployeeCannotOpenAdministratorPages`, `newEmployeeCannotChangeSomeoneElsesTask` |
| FR-36 | 自分のパスワード変更 | `MyPageController`, `form/PasswordChangeForm` | `test/controller/PasswordChangeTest` |
| FR-37 | 管理者が設定したパスワードは初回に変更必須 | `web/PasswordChangeRequiredInterceptor`, `config/WebConfig` | `test/controller/FirstLoginPasswordChangeTest` |
| FR-38 | 仮パスワード発行、最初の管理者の復旧 | `UserPageController.resetPassword`, `security/TemporaryPasswords`, `AdminAccountInitializer` | `test/controller/PasswordResetTest`, `test/security/AdminPasswordRecoveryTest` |
| FR-39 | 起動時に最初の管理者を作成 | `security/AdminAccountInitializer` | `AdminPasswordRecoveryTest.firstAdministratorIsCreatedWithoutChangeRequired` |
| FR-40 | 日本語のエラー画面(詳細は出さない) | `templates/error/*.html`, `fragments/error.html` | `test/web/ErrorPageTest` |
| FR-41 | ヘルスチェック | Actuator(`management.*` in `application.yml`) | `test/web/HealthCheckTest` |
| FR-42 | タスクの REST API(管理者) | `controller/TaskController` | `test/controller/TaskControllerTest`, `SecurityTest.apiRequiresAdministrator` |

## 3. 不変条件(壊してはいけないルール)

変更でこれらが崩れる場合は、実装前にオーナーへ相談すること。

| ID | ルール | 理由 / 関連 |
|---|---|---|
| INV-01 | 新しい URL は、明示しない限り **管理者専用**(`SecurityConfig` の `anyRequest().hasRole("ADMIN")`) | 新入社員にうっかり見せない |
| INV-02 | 新入社員が触るデータは、ログイン中のユーザーのものかを **クエリ条件** で確認し、他人のものは 404 | FR-35 |
| INV-03 | パスワードは BCrypt、専用URLの鍵は SHA-256 で保存。**平文・鍵そのものを保存・表示・ログ出力しない**(発行直後の 1 回の表示を除く) | NFR-01, FR-32 |
| INV-04 | 利用者の入力は `th:text` で表示。`th:utext` は `${@links.linkify(...)}` だけ | NFR-03, FR-05 |
| INV-05 | 更新系フォームは POST + CSRF トークン(`th:action`)。テストは `WebTestSupport.post` | NFR-02 |
| INV-06 | リダイレクトはパスだけ(`server.tomcat.use-relative-redirects: true`、`SecurityConfig` の entry point / success handler) | NFR-06。`RelativeRedirectTest` |
| INV-07 | 「今日」「今」は注入した `Clock` から取る(`LocalDate.now(clock)`) | FR-23。テストは 2026-10-15 固定 |
| INV-08 | 適用済みの Flyway マイグレーションは書き換えない。変更は新しい番号で | 既存 DB が起動できなくなる |
| INV-09 | サンプルデータを変えたら `test/SampleData.java` を手で更新する | テストの前提 |
| INV-10 | 管理者が他人に設定したパスワード(登録・編集・リセット)は `passwordChangeRequired = true` | FR-37, FR-38 |
| INV-11 | 管理者には専用URLを発行しない。役割を管理者に変えたら専用URLを消す | FR-33 |
| INV-12 | CI の必須チェック名 `test` / `call-reusable / run-smoke` を変えない。呼び出し元の job に `if` を付けない | NFR-07。ルールセットが名前で判定 |

## 4. 非機能要件(要約)

| ID | 要件 | どこで担保しているか |
|---|---|---|
| NFR-01 | パスワード・専用URLは元に戻せない形で保存 | INV-03 |
| NFR-02 | CSRF 対策 | Spring Security(既定で有効)、INV-05 |
| NFR-03 | 入力値を HTML として動かさない | INV-04、`LinkTextTest` |
| NFR-04 | 認証情報を書かない。CI はランダム値 + `::add-mask::` | `reusable-docker-smoke.yml` |
| NFR-05 | Docker 1 コマンドで起動、DB は自動作成 | `docker-compose.yml`、Flyway、`.devcontainer/start-app.sh` |
| NFR-06 | プロキシの裏で正しく動く | INV-06、`APP_BASE_URL` |
| NFR-07 | 自動テスト + Docker 確認が通らないとマージ不可(ドキュメントだけは省略) | `onboarding-progress-system-ci.yml`(`changes` ジョブ) |
| NFR-08 | 数十人規模で十分な速さ | 一覧の集計は 1 クエリ(`countProgressByUser`)、関連は `@EntityGraph` |
| NFR-09 | 学習者にも読める標準的な書き方 | `docs/ai/conventions.md`、`docs/java-guide.md` |

## 5. 対象外(作らないと決めたもの)

| 項目 | 状態 | 判断 |
|---|---|---|
| 専用URL無効化時のセッション即時失効 | 対応しない | 非公開前提で影響小(オーナー判断) |
| ユーザー・割り当ての REST API | 保留 | 連携先が決まったら |
| メール送信(専用URL・パスワード再設定) | 候補 | SMTP と公開サーバーが必要 |
| 一覧のページ分け | 候補 | 規模が大きくなったら |
| 画面の作り込み(スマホ対応・画面フレームワーク) | 候補 | 今の使い勝手で問題なし |

最新の候補・優先度は [backlog.md](backlog.md)。

## 6. 設計判断の記録(なぜそうなっているか)

| 判断 | 理由 | PR |
|---|---|---|
| 画面は Thymeleaf(サーバーで HTML を作る) | 小規模で動きが少ない。JS をほぼ書かずに済む | #4〜 |
| 新入社員は専用URL、パスワード方式は設定で残す | 短期間・非公開。パスワード管理の手間を減らす | #19 |
| 専用URLはハッシュだけ保存、1 回だけ表示 | DB 漏えい時に URL が分からない | #19 |
| 管理者が設定したパスワードは初回変更必須 | 管理者が知るパスワードを使い続けない | #20 |
| 最初の管理者の復旧は起動時の設定で | メール送信なしで復旧できる | #22 |
| リダイレクトはパスだけ | Codespaces のプロキシで localhost に飛ぶ不具合(#24) | #24 |
| ドキュメントだけの PR はテストをスキップ扱い | 必須チェックを満たしたまま待ち時間をなくす。パスフィルタだとチェックが始まらずマージ不可になった | #26 |
| テストのサンプルデータは `SampleData` に集約 | サンプルデータ変更時の修正箇所を減らす(#28 で 18 件直した反省) | #29 |

## 7. このファイルの更新ルール

- 機能を追加・変更したら、2 章の表(要件・実装・テスト)を更新する。新しい要件には新しい ID を振る(番号は詰めない)
- 不変条件を変えるときは、オーナーの了承を得て 3 章を更新する
- 人向けの [requirements.md](../requirements.md)・[design.md](../design.md) も同じ PR で合わせて更新する
