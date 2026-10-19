---
name: review-and-refactor
description: オンボーディング進捗管理システム(Spring Boot + PostgreSQL + Flyway + Thymeleaf)のコード品質を上げる。重複コードの検出、Service 層の導入、命名の改善、パフォーマンス改善、Spring Boot のベストプラクティスの適用を、動きを変えずに行い、各修正の理由を説明して PR まで作る。「リファクタリングして」「コードを整理して」「重複を減らして」「このクラスを見直して」と頼まれたときに使う。
---

# review-and-refactor — コード品質の改善

**動きを変えずに、読みやすく・直しやすくする** のが目的です。新しい機能は追加しません(それは `feature-builder`)。

## 前提(必ず守る)

- 構成: Spring Boot 3.3 + PostgreSQL 15 + Flyway + Thymeleaf。目指すレイヤーは Controller → Service → Repository → DB
- `docs/ai/conventions.md`(書き方)と `docs/ai/spec.md` の **壊してはいけないルール(INV)** と **設計判断の記録(6 章)** を先に読む。
  設計判断で決まったことは「改善」として変えない
- 進め方は `CLAUDE.md` の「AI 開発運用ルール」に従う。**リファクタリングは確認不要の作業** なので、頼まれた範囲は自分で直して PR まで作る
- **タスクの範囲だけを直す。** 範囲外で見つけたものは、`docs/TASKS.md` / `docs/ai/backlog.md` に候補として記録するだけ
- 範囲が決まっていない依頼(「全体を整理して」など)は、候補の一覧を出して、どれから直すかを確認する
- URL・JSON の形・DB が変わる場合は、リファクタリングではなく「事前確認が必要な作業」になるので、先に了承をもらう
- 1 つの PR は 1 つのテーマ(例: 「TaskService の導入」だけ)。整形だけの変更(並べ替え・空白)を混ぜない
- URL、画面の文言、JSON の項目名、DB は変えない(変えるなら、それは機能変更なので別の相談)
- 説明は日本語で、専門用語を使いすぎない

## 手順

### 1. 範囲を決める

指定がなければ「今のブランチの変更」(`git diff origin/master...HEAD`)。リポジトリ全体なら、まず提案だけを出す。

### 2. 候補を集める

```bash
python3 .claude/skills/review-and-refactor/scripts/find-duplicates.py 5     # 重複(5 行以上)。数字を変えると感度が変わる
.claude/skills/spring-boot-architect/scripts/layer-check.sh                # レイヤー違反・now()・Entity の直接受け取りなど
```

**出てきたものは候補です。** 次のものは重複として扱いません。

- Entity と Form の getter / setter(役割が違う。共通化すると、画面から `id` を書き換えられる危険が戻る)
- テンプレート先頭の `<head>` の数行(Thymeleaf の部品化で得られるものが小さい)
- `@PrePersist` / `@PreUpdate`(共通の親クラスにする案はあるが、効果は小さい。提案するなら「低」)

### 3. 候補を整理する(範囲が決まっていない依頼では、ここで一覧を見せて確認する)

[refactoring-catalog.md](refactoring-catalog.md) に、このリポジトリで実際にある候補と、直し方・理由・確かめ方をまとめています。

```
| # | 優先度 | 種類 | 場所 | 今どうなっているか | どう直すか | なぜ(得られるもの) | 影響範囲・リスク |
```

種類: 重複 / Service 層 / 命名 / パフォーマンス / ベストプラクティス。優先度は「次の機能追加の邪魔になるか」で決める。

### 4. 直す

1. **先にテストで今の動きを押さえる。** 直す箇所にテストがなければ、今の動きのままで通るテストを先に足す
2. 小さく 1 つずつ直し、そのたびに関係するテストを流す(`mvn -B test -Dtest=TaskPageControllerTest` など)
3. 全部終わったら `cd onboarding-progress-system && mvn -B test`(全テスト)。テンプレートを変えたら `mvn -B clean test`
4. 画面のテンプレートを変えたら、起動して画面を確認(`reusable-docker-smoke.yml` と同じ手順)
5. `layer-check.sh` を流し直して、候補が減った(増えていない)ことを確かめる
6. 直した結果、ドキュメントの記述(クラス一覧など)が変わったら `docs/ai/architecture.md` などを更新する
   (`docs/java-guide.md` に引用しているコードを変えたら、そこも直す)
7. `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新する
8. `refactor:` + 英語の命令形でコミットし、作業ブランチに push して PR を作る(CI の結果も確認する)

### 5. 理由を説明する

修正ごとに、次の 4 点を日本語で書きます。

```
### (修正の名前)
- 何を変えたか: (ファイルと、前 → 後)
- なぜ: (困っていたこと。例: 同じ登録処理が画面と API の 2 か所にあり、片方だけ直すと動きがずれる)
- 効果: (例: 登録のルールが TaskService の 1 か所だけになった)
- 動きが変わっていないことの確認: (どのテストで確かめたか、テストの件数)
```

最後に「今回は直さなかった候補」を一覧で残します(必要なら `docs/ai/backlog.md` の技術的負債に追加)。
