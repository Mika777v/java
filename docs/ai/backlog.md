# バックログ(AI 向け)

最終更新: 2026-10-19(Codespaces のリカバリーモードを修正)。
完了したら行を「完了済み」へ移し、`docs/current-status.md` も更新してください。
このファイルが **ロードマップ** を兼ねます。更新の手順は `.claude/skills/project-memory-manager/SKILL.md`。

## ロードマップ(次にやることの推奨順)

不具合・セキュリティ → オーナーが頼んだもの → 次の機能追加の邪魔になる技術的負債 → 未実装の機能、の順に並べています。

| 順 | 項目 | なぜこの順か | 規模 |
|---|---|---|---|
| 1 | 技術的負債 D1: `TaskService` の導入 | 画面と API で同じ登録処理を 2 か所に書いている。機能を足す前に 1 か所にまとめると、以後の変更が楽になる | 小(2 クラス) |
| 2 | 技術的負債 D2: `UserService` の導入(D6 も一緒に) | ユーザーの業務ルールが Controller に散らばっている。ユーザーの API やメール送信を作るなら先に必要 | 中 |
| 3 | 未実装: 専用URLのメール送信 | 公開されたサーバーで使い始めるときに必要。SMTP の準備が前提 | 中 |
| 4 | 技術的負債 D3〜D5 | 見た目・読みやすさの改善。急がない | 小 |

不具合・セキュリティの問題は、今は見つかっていません(2026-10-16 時点)。

## 技術的負債

`spring-boot-architect` の `layer-check.sh` と `review-and-refactor` の `find-duplicates.py` で見つけたもの。
詳しい直し方は `.claude/skills/review-and-refactor/refactoring-catalog.md`(R 番号)。

| # | 種類 | 場所 | 困ること | 対応案 | 優先度 |
|---|---|---|---|---|---|
| D1 | レイヤー | `TaskPageController`, `TaskController` が `TaskRepository` を直接使う | 登録のルールが 2 か所にあり、片方だけ直すと画面と API で動きがずれる | `TaskService` を作る(R1) | 中 |
| D2 | レイヤー | `UserPageController` が `UserRepository` を直接使い、業務ルール(メール重複・パスワード変更必須・専用URLの削除)も持つ | ユーザーの API などを作るときに、同じルールをもう一度書くことになる | `UserService` を作る(R2) | 中 |
| D3 | 大きすぎるクラス | `UserPageController`(約 380 行) | どこを直せばよいか探しにくい | URL は変えずに、割り当て・専用URLの Controller に分ける(R3、D2 の後) | 低 |
| D4 | 重複 | `my-page.html` と `user-detail.html` の割り当ての表 | 表示を変えるとき 2 か所直す | Thymeleaf の部品にする(R4) | 低 |
| D5 | API | `TaskController` が Entity をそのまま JSON で返す(受け取りは `TaskRequest` に変更済み) | Entity に項目を足すと、意図せず API に出る | 出力も DTO にする。`createdAt` などを出すかはオーナーと相談(R5) | 低 |
| D8 | 設定 | `spring.jpa.open-in-view` が既定の true(起動時に WARN) | 画面の表示中に気づかないまま DB を読む(N+1 の原因になりうる) | false にする。画面で関連を読む箇所を `@EntityGraph` で読んでおく必要があるので、全画面の確認とセットで(D2 と一緒に) | 低 |
| D6 | レイヤー | `MyPageController` のパスワード変更が `UserRepository` を直接使う | D2 と同じ | D2 と一緒に `UserService` へ(R7) | 低 |
| D7 | 性能 | `DashboardService` が全ユーザーを読んでから管理者を除く | 今の規模では問題なし | `findByRole(Role.MEMBER)`(R6) | 低 |

## 未実装の機能

| 優先度 | 項目 | メモ |
|---|---|---|
| 低 | 専用URLのメール送信 | 発行した専用URLを本人のメールアドレスへ送る。送信用メールサーバー(SMTP)の設定と、その認証情報(Secrets / `.env`)が必要。新入社員がリンクを開ける場所(公開されたサーバー)で使うことが前提。メールでのパスワード再設定(本人が自分でリセット)もこれと一緒に検討 |
| 保留 | ユーザー・割り当ての REST API | 人事システムとの自動連携、チャットへの期限切れ通知、集計ツールへの取り込みなどを使い始めたら検討。今は画面だけで足りる |
| 低 | 一覧のページ分け | 今は全件表示。ユーザーが数百人規模になったら検討 |
| 低 | 画面の見た目・使い勝手 | 画面は Thymeleaf + CSS 1 ファイル、JavaScript はほぼなし。改善するなら CSS の整理・スマホ対応 → 専用URL・仮パスワードのコピーボタン(JavaScript の追加はオーナーに相談)→ 画面フレームワークで作り直し。今は使い勝手に問題なし(オーナー確認済み) |

## 既知の課題・整理候補

| 項目 | メモ |
|---|---|
| ルートの `README.md` が 1 行だけ | 詳細は `onboarding-progress-system/README.md`。必要ならリンクを追加 |
| `show-sql: true` | 開発用のまま。本番運用するならログが多すぎるので切り替えを検討 |
| サンプルデータが本番 DB にも入る | V2・V4 は通常のマイグレーション。本番運用するなら分離を検討 |
| 専用URLを無効化してもログイン中のセッションは残る | セッションが切れる(既定 30 分操作なし)かログアウトするまで使える。公開しない前提なので影響は小さく、**対応しない**(オーナー判断) |
| Codespaces では新入社員が専用URLを開けない | ポート 8080 は初期設定で非公開。Public にするか、どこかのサーバーに置く必要がある |
| Codespaces のホストネットワーク回避策 | Docker 29 のコンテナ間通信の問題が直れば `docker-compose.codespaces.yml` は不要になる |

## オーナー判断待ち(コードレビューで指摘されたが、今回は直していないもの)

| 指摘 | 確認した事実 | 直さなかった理由 |
|---|---|---|
| サンプルデータ(V2・V4・V9)が本番の DB にも入る | 新しい DB で起動すると、架空の 3 人・6 タスク・10 件の割り当てが入る(確認済み) | 本番で使うかどうか未定。分けるなら `db/sample/` + 開発用プロファイル。テストと Codespaces の前提が変わる大きな変更 |
| タスクを削除すると、全員の割り当てと完了記録も消える | `DELETE /api/tasks/{id}` 1 回で消える(テスト `deleteRemovesTask` のとおり) | FR-04 の要件どおりの動き。履歴を残すなら「割り当てがあるタスクは削除不可(無効にする)」に仕様を変える必要がある |
| 最初の管理者(`APP_ADMIN_PASSWORD`)にも初回のパスワード変更を求める | 今は変更を求めていない | Codespaces と CI のスモークテストのログイン手順が変わる。設計(design-diagrams.md 4.3)でも「通常」としている |
| ユーザーの登録・更新を `UserService` に移す | Controller が Repository を直接呼んでいる | 技術的負債 D2 として記録済み。動きは変わらない整理なので別の PR で |

## 完了済み(主なもの)

| 内容 | PR(Mika777v/java) |
|---|---|
| GitHub Actions で Docker 起動・スモークテスト | #2 |
| `/tasks` 画面(サンプルデータ付き) | #4 |
| Codespaces 用の Docker Compose 設定 | #5 |
| タスクの登録・編集・削除画面 | #7 |
| 自動テスト(Testcontainers)と CI での実行 | #8 |
| ユーザー管理・タスクの割り当て・進捗 | #9 |
| 部署・進捗での絞り込み、期限切れ表示 | #10 |
| 日本語のエラー画面、PR 作成時の CI | #11 |
| REST API の 500 エラー修正 | #12 |
| 整理、Codespaces の自動起動、タスクの並べ替え、ダッシュボード | #13 |
| Codespaces で準備完了後にブラウザを開く | #14 |
| ログイン(管理者・新入社員の役割) | #15 |
| AI 向け開発メモ(`docs/ai/`) | #16 |
| 不要な `wait-test.sh` の削除、`CLAUDE.md` / `copilot-instructions.md` の追加 | #17 |
| 自分のパスワード変更(`/me/password`、管理者・新入社員とも) | #18 |
| 新入社員の専用URL(パスワードなしのログイン)、ログイン方法の切り替え(`APP_MEMBER_LOGIN`) | #19 |
| 初回ログイン時のパスワード変更(管理者がほかの人に設定したパスワード) | #20 |
| ヘルスチェック(`/actuator/health`)、CI・Codespaces の起動待ちに使用 | #21 |
| パスワードのリセット(ほかの管理者が仮パスワードを発行、`APP_ADMIN_PASSWORD_RESET` で最初の管理者を復旧) | #22 |
| Codespaces での動作確認手順(`docs/codespaces-check.md`) | #23 |
| 不具合修正: Codespaces でボタンを押すと localhost に飛ぶ(リダイレクトをパスだけに) | #24 |
| 現状レポートの最新化(第18版) | #25 |
| ドキュメントだけの PR で自動テストと Docker の確認をスキップ | #26 |
| Java の教科書(`docs/java-guide.md`)、現状レポートにフロントエンドの状況を追記 | #27 |
| タスクの説明欄の URL をリンクにする(SSO などへの案内用) | #28 |
| テストのサンプルデータの件数・id を `SampleData` にまとめる(サンプルデータを変えたときの修正箇所を減らす) | #29 |
| 要件定義書・設計書(人向け `docs/requirements.md` / `docs/design.md`、AI 向け `docs/ai/spec.md`)、図と定義表(`docs/design-diagrams.md`、spec.md 6.5 章に状態遷移の表) | #30 |
| コードレビューの指摘の修正(削除・役割変更されたユーザーのログアウト、メールの大文字小文字の重複を DB でも拒否、二度押しで 500 にしない、API の PUT で `active` を省略しても今の値のまま・201 に Location、Flyway の baseline を既定 false、SQL のログを既定で出さない、DB ポートを localhost だけに、Docker を root 以外で実行・HEALTHCHECK、Actions の権限を読み取りだけに、最初の管理者のパスワードの長さチェック) | #31 |
| Codespaces の土台を `universal:2` から `devcontainers/java:1-21-bookworm` + Maven・Docker に変更(準備が終わらず開けなくなったため) | #32 |
| Codespaces のリカバリーモードを修正(Java 21 のイメージの Yarn の取得元を削除する `.devcontainer/Dockerfile`) | (第27版) |
