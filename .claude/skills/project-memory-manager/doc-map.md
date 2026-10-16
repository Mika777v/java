# ドキュメントの地図(project-memory-manager 用)

どの情報がどこに書いてあるか、何を変えたらどこを直すか。

## 情報の置き場所

| 情報 | AI 向け(優先) | 人向け |
|---|---|---|
| 構成・技術スタック・環境変数 | `docs/ai/project-context.md` | `docs/current-status.md` 1・5 章、`onboarding-progress-system/README.md` |
| 要件(FR / NFR)・対象外 | `docs/ai/spec.md` 1〜5 章 | `docs/requirements.md` |
| 壊してはいけないルール(INV) | `docs/ai/spec.md` 3 章 | `docs/design.md` |
| 状態遷移 | `docs/ai/spec.md` 6.5 章 | `docs/design-diagrams.md` 4 章 |
| クラス・レイヤー・URL・セキュリティ | `docs/ai/architecture.md` | `docs/current-status.md` 5・7・8・12 章、`docs/design.md` |
| テーブル・マイグレーション・サンプルデータ | `docs/ai/database.md` | `docs/current-status.md` 4 章、`docs/design-diagrams.md` 5 章 |
| 画面の入力チェック・メッセージ | (AI も人向けを読む) | `docs/design-diagrams.md` 8・9 章 |
| コーディング・テスト・PR の決まり | `docs/ai/conventions.md` | — |
| ロードマップ・技術的負債・完了済み | `docs/ai/backlog.md` | `docs/current-status.md` 3・11 章 |
| 設計判断の理由 | `docs/ai/spec.md` 6 章 | `docs/design.md` 8 章 |
| 依頼文のひな形 | `docs/ai/prompt-templates.md` | — |
| Codespaces での確認手順 | — | `docs/codespaces-check.md` |
| Java の教科書 | — | `docs/java-guide.md` |

## 変えたら直すもの

`docs/ai/conventions.md` の「変更したら更新するもの」が正本です。要点だけ:

| 変更 | 直すもの |
|---|---|
| 画面・URL | `architecture.md`、`current-status.md` 8 章、`design-diagrams.md` 3 章、`codespaces-check.md`、必要ならスモークテスト |
| テーブル | `database.md`、`current-status.md` 4 章、`design-diagrams.md` 5・7 章 |
| 業務ルール・権限 | `spec.md`、`requirements.md`、`design.md` |
| 状態・メッセージ・入力チェック | `spec.md` 6.5 章、`design-diagrams.md` 4・8・9 章 |
| 機能の完了 | `backlog.md`(ロードマップから「完了済み」へ)、`current-status.md` の変更履歴 |
| テストの件数 | `current-status.md`(2 か所)、`design.md` 7 章 |

## `docs/current-status.md` の書き方

- 冒頭の「調査日: YYYY-MM-DD(第N版)」を更新する。版数は内容を変えるたびに 1 つ上げる
- 「これまでの主な変更」の表に `| #PR番号 | 内容 |` を追加する。PR 番号がまだないときは `| (第N版) | 内容 |`
- 章の番号と構成は変えない(ほかのドキュメントから「4 章」のように参照されているため)

## `docs/ai/backlog.md` の書き方

- 「最終更新: YYYY-MM-DD(何をしたか)」を更新する
- 終わった項目は「ロードマップ」から消して「完了済み」に `| 内容 | #PR番号 |` で追加する
- 「対応しない(オーナー判断)」「保留」の項目は、オーナーの指示なしに消したり優先度を上げたりしない
