---
name: feature-builder
description: オンボーディング進捗管理システム(Spring Boot + PostgreSQL + Flyway + Thymeleaf)に新しい機能を実装する。要件分析 → タスク分解 → (DB 変更などのときだけ方針の確認)→ Flyway・Entity・Repository・Service・Controller(画面 / API)・Thymeleaf・テストの追加 → ドキュメント・README・DEVLOG・SESSION_SUMMARY の更新 → PR 作成 → 変更内容の要約、までを行う。「〇〇機能を追加して」「〇〇できるようにして」と頼まれたときに使う。
---

# feature-builder — 新機能の実装

## 前提(必ず守る)

- 構成: Spring Boot 3.3 + PostgreSQL 15 + Flyway + Thymeleaf。新しいコードは **Controller → Service → Repository** の向きで書く
  (既存の Controller には Repository を直接呼ぶ箇所があるが、新しいコードでは真似しない)
- `docs/ai/conventions.md`(書き方)と `docs/ai/spec.md` の **壊してはいけないルール(INV)** を守る
- 進め方は `CLAUDE.md` の「AI 開発運用ルール」(タスク実行フロー・自律実行ルール)に従う
- **事前確認が必要な作業だけ、実装の前に「原因・方針」を短く説明してオーナーの了承をもらう**:
  DB スキーマ変更(新しいマイグレーション)、API 互換性を壊す変更、認証・認可の変更、ライブラリの大幅変更、
  インフラ構成の変更、データ削除処理、仕様変更(要件・INV の変更)。それ以外の軽微な設計判断は自分で決め、理由を残す
- 修正はタスクの範囲だけ。頼まれていない整理はしない(見つけたら最後の要約に「気づいたこと」として書くだけ)
- パスワード・認証情報をコード・テスト・ドキュメント・ログに書かない
- `master` に直接 push しない。完了したら PR まで作る(マージはオーナー)
- 説明は日本語で、専門用語を使いすぎない

## 手順

### 0. 現状を確認する

`project-memory-manager` スキルの手順 1・2 を行う(`survey.sh` と `docs/ai/` を読む)。ブランチが最新の `master` から始まっているかも確かめる
(マージ済みの PR のブランチは `git checkout -B <ブランチ> origin/master` で作り直す)。

### 1. 要件を分析する

次の表を作る。分からないことは推測で埋めず、質問にする。

| 項目 | 内容 |
|---|---|
| やりたいこと(誰が・何を・なぜ) | |
| 要件 ID | `docs/ai/spec.md` の既存の FR を変えるか、新しい番号(空いている次の番号)を振る |
| 使う人と権限 | 管理者だけ / 新入社員も(`/me/**`) |
| 画面・URL | 追加 / 変更する画面 |
| データ | 追加 / 変更するテーブル・列(マイグレーションが要るか) |
| 関係する INV | 例: INV-02(本人のデータだけ)、INV-07(Clock) |
| 対象外 | 今回やらないこと |
| 確認したいこと | オーナーへの質問 |

### 2. タスクに分解する(事前確認が必要な作業なら、ここで了承をもらう)

[implementation-guide.md](implementation-guide.md) の順番(下の層から)でチェックリストを作る。
事前確認が必要な作業(DB 変更など)を含むなら、ここで要件の表とチェックリストを見せて了承をもらう。
含まないなら、そのまま実装に進む。

```
- [ ] Flyway: V{次の番号}__xxx.sql
- [ ] Entity / enum
- [ ] Repository
- [ ] Service(業務ルール、@Transactional)
- [ ] Form(入力チェック、日本語メッセージ)
- [ ] Controller(画面: POST → リダイレクト / API: 必要なときだけ)
- [ ] Thymeleaf(nav と messages の部品、th:text)
- [ ] SecurityConfig(新入社員にも見せる場合だけ)
- [ ] テスト(正常系・入力エラー・404・権限)
- [ ] ドキュメント・README
```

REST API は、要件で必要なときだけ追加する(`docs/ai/backlog.md` でユーザー・割り当ての API は「保留」)。

### 3. 実装する

[implementation-guide.md](implementation-guide.md) の各層のひな形(このリポジトリの既存コードから取ったもの)に合わせて書く。
マイグレーションの次の番号は `spring-boot-architect/scripts/layer-check.sh` の F3 で分かる。

### 4. 確認する

```bash
cd onboarding-progress-system
mvn -B test                       # 全テスト(Docker が必要)。テンプレートを変えて結果が変だったら mvn -B clean test
```

- 画面を変えたら、Docker(またはテスト用の PostgreSQL + jar)で起動して、`reusable-docker-smoke.yml` と同じ手順の確認をする
- 新しいテストが、わざと実装を壊したときに失敗することを 1 回は確かめる
- `spring-boot-architect/scripts/layer-check.sh` を流して、新しい問題が増えていないことを確かめる

### 5. ドキュメントを更新する

`docs/ai/conventions.md` の「変更したら更新するもの」に従う。よく使うのは:

| 変更 | 更新するもの |
|---|---|
| 機能・ルール | `docs/ai/spec.md`(要件・実装・テストの対応表)、`docs/requirements.md`、`docs/design.md` |
| 画面・URL | `docs/ai/architecture.md`、`docs/current-status.md`(版数と変更履歴も)、`docs/design-diagrams.md`、`docs/codespaces-check.md` |
| テーブル | `docs/ai/database.md`、`docs/current-status.md` 4 章、`docs/design-diagrams.md` 5・7 章 |
| 環境変数 | `application.yml`、`docker-compose.yml`、`reusable-docker-smoke.yml`、`.devcontainer/start-app.sh` |
| 使い方が変わる | `onboarding-progress-system/README.md`(英語)の該当箇所 |
| 完了 | `docs/ai/backlog.md`(ロードマップから「完了済み」へ)、`docs/TASKS.md` |
| すべて | `DEVLOG.md`(作業ログを上に追記)、`SESSION_SUMMARY.md`(引き継ぎを書き換え) |

### 6. コミット・push・PR 作成をして、要約する

- コミットメッセージは `feat:` + 英語の命令形(例: `feat: Add due date reminders to the dashboard`)
- 作業ブランチに push し、PR を作る(本文は日本語: 何を変えたか / なぜ / 確認方法 / テスト結果)
- PR の CI(`test`・`call-reusable / run-smoke`)を確認し、失敗したら直して push する
- **最後に必ず [summary-template.md](summary-template.md) の形で変更内容を要約する**
