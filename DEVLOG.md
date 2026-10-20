# DEVLOG

作業ログです。タスクが終わるたびに、**新しいものを上に** 追記します(運用ルールは `CLAUDE.md`)。
PR #29 以前の経緯は `docs/current-status.md` の「これまでの主な変更」と `docs/ai/backlog.md` の「完了済み」を参照してください。

書き方:

```
## YYYY-MM-DD 種類: タスク名(PR #番号)
- 何をしたか
- 判断したこと(と理由)
- テスト・確認の結果
- 残したこと / 次の一手
```

---

## 2026-10-20 feat: Feature Toggle の仕組み(S0.1)

- 移行計画の最初の Issue。新しいパッケージ `feature`: `Feature`(Toggle 8 つの一覧・親・既定値)、`FeatureFlags`(`@Component("features")`)、`@RequiresFeature` + `FeatureToggleInterceptor`(OFF なら 404)
- 設定: `application.yml` の `features.*.enabled`、`docker-compose.yml` の `FEATURE_*`(`onboarding-legacy` だけ既定 ON、ほかは OFF)。README・`architecture.md`・`conventions.md`(Toggle の使い方)・`spec.md`(INV-15、設計判断)・`project-context.md` を更新
- 判断:
  - `FeatureProperties` は作らず、`FeatureFlags` が `Environment` から読む(Toggle の一覧を `Feature` 1 か所にまとめ、二重管理を避ける)。計画書の T0.1.1 も直した
  - 知らないキー(`on('ticket-sytem')` のような打ち間違い)はエラーにする(黙って OFF にすると気づけない)。`true` / `false` 以外の値は起動時にエラー
  - `.devcontainer/start-app.sh` と CI は変えない(`.env` に `FEATURE_...=true` を書けば docker compose が読む。既定のままなら設定不要)
  - まだ `@RequiresFeature` を付けた画面はない。テストではテスト専用の Controller で確かめた
- PR #41 のマージ後に push した Q3(論理削除)の記録が master に入っていなかったため、このブランチに入れ直した
- 計画に残っていた確認 3 つへの回答も記録(`TASKS_TICKET_SYSTEM.md`・`GITHUB_ISSUES_TICKET_SYSTEM.md`・`PRD_TICKET_SYSTEM.md`):
  - S3.2 状態の移り方: 例のとおり。TODO・IN_PROGRESS・REVIEW・DONE の間は自由、どこからでも CLOSED、CLOSED から出るのは再オープン(→ TODO)だけ
  - S10.1 データの移し替え: まだ実運用していないので、実行日はいつでもよい
  - S10.4 旧コードの削除: 移行して問題がなければ、オーナーが削除の指示を出す(それまで着手しない)
  - これでラベル `needs-approval` の Story はなくなった
  - この記録は #42 のマージ後に push したため、別の PR にした
- テスト: 236 件すべて成功(15 件追加: `FeatureFlagsTest` 7、`FeatureToggleInterceptorTest` 3、`FeatureToggleTest` 5)。Interceptor の登録を外すと `FeatureToggleTest` が失敗することも確認。`docker compose config` で `FEATURE_*` が渡ることを確認

## 2026-10-20 docs: 移行計画への回答の反映と GitHub Issues 一覧(PR #41)

- オーナーの回答(PRD 10 章): Q1・Q2・Q4・Q6・Q7 は OK、Q5 は「今のまま(ロールとログイン方法を分けない)、4 ロールは Toggle で切り替え」。Q3 は説明のあと「論理削除で」と回答(削除したチケットは画面から消えるが DB に残り、元に戻せる)
- 反映: `PRD_TICKET_SYSTEM.md`(v0.2、回答欄と 2.3 章)、`ROADMAP_TICKET_SYSTEM.md`(v0.2、Phase7)、`TASKS_TICKET_SYSTEM.md`(v0.2、✅ 了承済み / ⚠ 未了承の印、S7.1 の名前)
- 追加: `docs/GITHUB_ISSUES_TICKET_SYSTEM.md`(Epic 11・Story 50・Task 78 の Issue 一覧、GitHub Projects の Backlog / Ready / In Progress / Review / Done の運用、ラベル・項目・マイルストーン・ビュー・自動化)、`.github/ISSUE_TEMPLATE/`(Epic / Story / Task)
- 判断:
  - Task は Issue にせず Story 本文のチェックリストにする(1 Story = 1 PR。Issue が 130 件を超えると管理が重いため)。単独で PR にする Task だけ sub-issue に格上げ
  - 初期の Ready は依存も事前確認もない S0.1・S0.2・S0.3・S0.5・S0.6。⚠ が残る S3.2・S10.1・S10.4 は `needs-approval`(S1.4 は Q3 の回答で `approved`)
  - Issue 一覧の 3 章以降は `TASKS_TICKET_SYSTEM.md` から機械的に作り、件数(Epic 11 / Story 50 / Task 78)が一致することを確認
- GitHub 上の Issue・Project はまだ作っていない(オーナーの了承後に作る)
- コード・DB の変更はなし

## 2026-10-20 docs: チケット管理への移行計画(案)(PR #40)

- オーナーの依頼: オンボーディング管理を、作り直さずに Jira / Backlog 風のチケット管理へ段階的に発展させる計画。Feature Toggle で移行中も今の機能を使えるようにする
- 追加: `docs/PRODUCT_VISION.md`、`docs/PRD_TICKET_SYSTEM.md`、`docs/ROADMAP_TICKET_SYSTEM.md`、`docs/TASKS_TICKET_SYSTEM.md`、`docs/FEATURE_TOGGLE_STRATEGY.md`
- 分析: クラス・テーブル・画面・権限・API を調べ、`TaskAssignment` がチケット、`Task` がテンプレートに近いと評価。結論は「新しいドメイン(チケット・プロジェクト)を横に足し、共通部分を使い回し、最後にオンボーディングを載せ替える」
- 判断:
  - Toggle は `features.<名前>.enabled`(既定 OFF)。OFF の画面は 404。今ある `LoginSettings` と同じ書き方にする
  - 再起動なしの切り替えは Phase1 で `feature_flags` テーブルを足して実現(DB 変更なので実装前に確認)
  - DB は「足す → 移す → 片付ける」。片付け(古いテーブルの削除)は任意で、行うならオーナーの了承とバックアップが必要
  - ルートパッケージ名・プロジェクト名は変えない(影響が大きく、得られるものが小さい)
  - Phase7 の前に、ロール(MEMBER)とログイン方法(専用URL)を分ける設計の確認が必要
- 6 か月で Phase0〜10。MVP は Phase1〜3(約 10 週)。Issue は Epic 11・Story 50
- コード・DB の変更はなし(計画だけ)

## 2026-10-20 docs: 利用ガイドの追加(PR #39)

- オーナーの依頼: アプリが完成に近いので、使い方・できること / できないことをまとめる
- `docs/user-guide.md` を追加(使い始める前に、管理者の使い方、新入社員の使い方、できること / できないこと、運用のヒント、よくある質問)
- 書いた内容はコードと照らし合わせて確認(期限が近い = 7 日以内、セッション 30 分、削除の確認画面、パスワードの長さ、専用URLの期限 60 日など)
- リンクを追加: `docs/current-status.md`(第33版)、`docs/requirements.md`、`docs/ai/project-context.md`、`CLAUDE.md`。`docs/ai/conventions.md` に「画面・操作を変えたら user-guide.md も更新」を追加
- 判断: #39(確認結果の記録)がまだマージ前だったため、同じブランチに追加して 1 つの PR にした(どちらもドキュメントだけ)

## 2026-10-20 docs: タスクのサブ項目(F1)の Codespaces での確認を記録

- オーナーが Codespaces で `docs/codespaces-check.md` の 6b〜6d を含めて確認し、すべて問題なし(#38 マージ後)
- 更新: `docs/current-status.md`(第32版、13 章に結果)、`docs/TASKS.md`(F1 を完了へ)、`docs/ai/backlog.md`

## 2026-10-19 feat: タスクのサブ項目(チェックボックス、F1)(PR #38)

- オーナーの回答: 3 つの質問はすべて「おすすめどおり」(全部チェックで自動完了・管理者画面にも表示・割り当て後の増減も反映)。テーブル追加の了承を兼ねる
- 追加: V11(`task_checklist_items`・`assignment_checks`)、Entity 2 つ、Repository 2 つ、`ChecklistService` / `Checklist`、`ChecklistItemForm`、`fragments/checklist.html`
- 画面: タスク編集でサブ項目の追加・削除、マイページでチェック(押すたびに付ける / 外す)、ユーザー詳細で「サブ項目 2 / 3」と ☑ を表示(見るだけ)
- 判断(軽微なので確認なし。理由つき):
  - チェックのボタンは「付ける / 外す」をはっきり送る(`checked=true/false`)。トグルにすると二度押しで元に戻るため
  - 同じ項目を同時に 2 回チェックしても 1 行だけ(`INSERT ... ON CONFLICT DO NOTHING`)。例外で止めずに済む
  - チェック 0 個になっても「未着手」には戻さない(完了からは「対応中」、ほかは今のまま)。手で選んだ状態を勝手に変えないため
  - サブ項目の追加・削除では、割り当て済みの人の状態を変えない。完了の記録を勝手に消さないため
  - 管理者はチェック状況を見るだけ(付け外しは新入社員のマイページだけ)。要望が「表示」だったため
  - 並べ替え・名前の変更は今回は作らない(`docs/TASKS.md` の低優先度に記録)
  - タスク編集画面に結果メッセージの表示がなかったので追加(サブ項目の追加・削除の結果を出すため)
- テスト: 221 件成功(`ChecklistTest` 21 件・`ChecklistStatusTest` 4 件を追加)。全部チェックで完了にする処理をわざと壊すと 4 件失敗することを確認
- 実際の起動で確認: 管理者がサブ項目を追加 → 新入社員が専用URLで開いてチェック → 2 つ目で自動で完了 → 管理者の画面に「2 / 2」。画面のスクリーンショットも確認
- CI のスモークテストに「サブ項目を追加して編集画面に出る」を追加(手元で同じ手順を実行して確認)
- ドキュメント: `docs/ai/`(spec・architecture・database)、`requirements.md`(FR-06・FR-26)、`design.md`、`design-diagrams.md`(ユースケース・状態・ER・CRUD・画面項目・メッセージ。図 12 枚が描けることを確認)、`current-status.md`(第31版)、`codespaces-check.md`(6b〜6d)、README

## 2026-10-19 docs: タスクのサブ項目(F1)と親子タスク(F2)をタスク一覧に登録(PR #37)

- オーナーの要望: 1 つのタスクに複数の作業がある場合(例: スマホに Teams・SSO ログインアプリ……を入れる)をまとめて管理したい
- 2 つの方式(親子タスク / チェックボックス)を比べ、オーナーと相談してチェックボックス方式(F1)に決定。親子タスク(F2)は優先度「低」で記録
- 判断: F1 はテーブルを 2 つ追加するので「事前確認が必要な作業」。3 つの質問(全部チェックで自動完了にするか、管理者画面に出すか、割り当て後の項目の増減を反映するか)の回答を待ってから実装する
- 更新: `docs/TASKS.md`(判断待ち・F1 の詳細・低優先度の欄を追加)、`docs/ai/backlog.md`(未実装の機能に F1・F2)
- テスト: ドキュメントだけの変更(CI はスキップ扱い)

## 2026-10-19 docs: AI 開発運用ルールの導入(PR #36)

- `CLAUDE.md` に AI 開発運用ルールを追加(基本方針、タスク実行フロー、確認不要 / 事前確認が必要な作業、自律実行ルール、PR 運用、コミット規約、管理ドキュメント、セッション終了時の作業)
- 管理ドキュメントを新規作成: `docs/TASKS.md`、`DEVLOG.md`、`SESSION_SUMMARY.md`。`SESSION_SUMMARY.md` と `docs/TASKS.md` は `CLAUDE.md` から自動で読み込む
- 判断: これまでの「PR はオーナーが『PRを作って』と言ってから作る」「大きな変更は先に了承」を、新しいルール(完了後は PR まで作る / 事前確認は決めた種類だけ)に置き換えた。「マージはオーナー」「`master` に直接 push しない」「認証情報を書かない」は残した
- 判断: コミットメッセージは「`種類:` + 英語の命令形」に統一(既存の英語の命令形と両立)
- 判断: ロードマップ・技術的負債の詳細は今までどおり `docs/ai/backlog.md`。`docs/TASKS.md` は今のタスクの一覧として、D 番号で参照する
- 古いルールが残っていた箇所も更新: `docs/ai/project-context.md`、`docs/ai/conventions.md`、`docs/ai/prompt-templates.md`、`.github/copilot-instructions.md`、スキル 3 つ(`feature-builder`・`review-and-refactor`・`project-memory-manager`)
- `docs/ai/backlog.md` の既知の課題から、#31 で直した `show-sql: true` の行を削除
- テスト: `mvn -B test` 196 件成功(ドキュメントだけの変更なので CI はスキップ扱い)

## 2026-10-19 docs: Codespaces での動作確認の記録(PR #35)

- オーナーが Codespaces で #31〜#34 の後の動作を確認(二度押し・メールの大文字小文字・削除された管理者のログアウト、すべて問題なし)
- 現状レポート(第29版)の 13 章に結果と途中の問題を記録

## 2026-10-19 ci: Codespaces の改善(PR #32・#33・#34)

- #32: Codespace の準備が終わらないため、土台を `universal:2`(10 GB 以上)から `devcontainers/java:1-21-bookworm`(約 2 GB)+ Maven・Docker の追加部品に変更
- #33: #32 の後、リカバリーモードで起動した。イメージに入っていた Yarn の apt ソースの署名エラーで Docker の追加部品が入らなかったため、`.devcontainer/Dockerfile` で削除
- #34: ポート 8080 が自動で出ないため `forwardPorts: [8080]` を追加
- 反省: #32 は Codespace 全体の組み立てを確認できないまま出した。作業環境から追加部品のダウンロード元につながらなかったため

## 2026-10-16 feat/fix: Claude Code のスキル追加、コードレビューの指摘の修正(PR #31)

- `.claude/skills/` に 4 つのスキル(`project-memory-manager`・`spring-boot-architect`・`feature-builder`・`review-and-refactor`)を追加
- コードレビューの指摘 10 件を、先にテストか実際の起動で再現してから 9 件修正(セッション、メールの大文字小文字 V10、二度押し、API の `active`、Flyway の baseline、SQL ログ、DB ポート、Docker の実行ユーザー、Actions の権限、最初の管理者のパスワードの長さ)。残りはオーナー判断待ちとして記録
- テスト: 196 件成功(再現用に 12 件追加)

## 2026-10-16 docs: 要件定義書・設計書・図と定義表(PR #30)

- 人向け `docs/requirements.md`・`docs/design.md`・`docs/design-diagrams.md`、AI 向け `docs/ai/spec.md` を追加
