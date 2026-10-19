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

## 2026-10-19 docs: AI 開発運用ルールの導入

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
