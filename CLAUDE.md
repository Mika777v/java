# CLAUDE.md

このリポジトリで作業する Claude Code 向けの案内です。詳しい内容は `docs/ai/` にあります。

## いつも読むもの(自動で読み込まれます)

@docs/ai/project-context.md
@docs/ai/conventions.md

## 作業内容に応じて読むもの

| 作業 | 読むファイル |
|---|---|
| 機能の追加・変更(要件 ID・壊してはいけないルール・テストとの対応) | `docs/ai/spec.md` |
| 画面・URL・クラス・権限の変更 | `docs/ai/architecture.md` |
| テーブル・マイグレーション・サンプルデータ・テストデータ | `docs/ai/database.md` |
| 次に何をやるか、既知の課題 | `docs/ai/backlog.md` |
| 人間向けの画面説明・現状の詳細 | `docs/current-status.md` |

## スキル(`.claude/skills/`)

| スキル | 使う場面 |
|---|---|
| `project-memory-manager` | 現状調査・ドキュメント更新・次のタスク提案(実装の前に必ず) |
| `spring-boot-architect` | 設計レビュー(コードは変えない) |
| `feature-builder` | 新機能の実装 |
| `review-and-refactor` | 動きを変えないコード整理 |

## 特に守ること(詳細は上のファイル)

- `master` に直接 push しない。作業ブランチで変更し、オーナーが「PRを作って」と言ったら PR を作る
- パスワード・認証情報をコード・ログ・ドキュメントに書かない
- 修正は最小限。大きな変更は先に原因と方針を説明して了承をもらう
- ユーザーへの説明は日本語で、専門用語を使いすぎない
- push 前に `cd onboarding-progress-system && mvn -B test` を通す
- 機能やテーブルを変えたら `docs/ai/`(特に `spec.md`)、`docs/current-status.md`、`docs/requirements.md` / `docs/design.md` / `docs/design-diagrams.md` も更新する
