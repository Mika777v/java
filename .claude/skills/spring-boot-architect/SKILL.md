---
name: spring-boot-architect
description: オンボーディング進捗管理システム(Spring Boot + PostgreSQL + Flyway + Thymeleaf)のアーキテクチャをレビューする。Entity・Service・API(Controller)・Flyway マイグレーション・レイヤー構造(Controller → Service → Repository → DB)を確認し、問題点と改善案を重要度つきで報告する。「設計をレビューして」「このブランチの構成を見て」「マイグレーションを確認して」と頼まれたとき、または大きな機能を追加する前後に使う。コードは変更しない。
---

# spring-boot-architect — アーキテクチャレビュー

**読むだけで、コードは変えません。** 直すのは `review-and-refactor`(整理)か `feature-builder`(機能追加)の仕事です。

## 前提

- 先に `docs/ai/architecture.md`(レイヤー・クラス・セキュリティ)、`docs/ai/database.md`(テーブル・マイグレーション)、
  `docs/ai/spec.md`(壊してはいけないルール INV と設計判断)を読む。`docs/current-status.md` 5〜8 章も参考にする
- **推奨するレイヤー構成:**

  ```
  Controller(画面・API の入口。入力チェックと画面の選択だけ)
      ↓
  Service(業務ルール。@Transactional)
      ↓
  Repository(Spring Data JPA)
      ↓
  Database(PostgreSQL。テーブルは Flyway が作る)
  ```

- **Controller から Repository を直接呼んでいる箇所は「改善候補」として報告する。** 不具合ではないので重要度は「中」か「低」。
  今のコードには既存の箇所がある(`TaskPageController`、`TaskController`、`UserPageController`、`MyPageController`)。
  レビュー対象の変更で **新しく** 増えた場合は「中」、既存のままなら「低」にまとめて 1 件で報告する
- `docs/ai/spec.md` 6 章「設計判断の記録」で決まったこと(例: 専用URLの即時無効化はしない)は、指摘しない。
  変えたほうがよいと思う場合は「再検討の提案」として分けて書く

## 手順

### 1. 対象を決める

- 指定がなければ「今のブランチで `master` から変わったところ」(`git diff origin/master...HEAD`)。差分がなければリポジトリ全体
- 全体レビューのときは、Entity → Repository → Service → Controller → テンプレート → マイグレーション の順に読む

### 2. 機械的なチェックを流す

```bash
git fetch origin master
.claude/skills/spring-boot-architect/scripts/layer-check.sh          # 比較先を変えるなら第 1 引数に指定
```

出てきたものは **候補** です。必ずコードを読んで、本当に問題かを確かめてから指摘にします。

### 3. チェックリストに沿って読む

観点ごとの確認項目は [review-checklist.md](review-checklist.md)。

- Entity 設計(E)
- Service 設計(S)
- API・Controller 設計(A)
- Flyway(F)
- レイヤー構造(L)
- セキュリティ・テスト(X)

### 4. 報告する

日本語で、次の形にまとめます。各指摘には「なぜ問題か」を一言添えます(オーナーは Java の学習中)。

```
## まとめ(全体の評価を 2〜3 行)

## 指摘
| # | 重要度 | 観点 | 場所(ファイル:行) | 内容 | なぜ問題か | 改善案 |
|---|---|---|---|---|---|---|

重要度: 高 = 不具合・セキュリティ・データが壊れる / 中 = 将来の変更で問題になりやすい / 低 = 読みやすさ・好み

## 良い点(残すべき設計)

## 次の一手
- すぐ直すもの(review-and-refactor で対応)
- 機能追加のついでに直すもの
- 記録だけするもの(docs/ai/backlog.md の技術的負債へ)
```

指摘が 0 件でも、その旨と確認した観点を書きます。
