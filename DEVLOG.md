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
