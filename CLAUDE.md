# CLAUDE.md

このリポジトリで作業する Claude Code 向けの案内です。詳しい内容は `docs/ai/` にあります。

## いつも読むもの(自動で読み込まれます)

@docs/ai/project-context.md
@docs/ai/conventions.md
@SESSION_SUMMARY.md
@docs/TASKS.md

## 作業内容に応じて読むもの

| 作業 | 読むファイル |
|---|---|
| 機能の追加・変更(要件 ID・壊してはいけないルール・テストとの対応) | `docs/ai/spec.md` |
| 画面・URL・クラス・権限の変更 | `docs/ai/architecture.md` |
| テーブル・マイグレーション・サンプルデータ・テストデータ | `docs/ai/database.md` |
| 次に何をやるか、既知の課題、技術的負債 | `docs/ai/backlog.md`(ロードマップ)、`docs/TASKS.md`(今のタスク) |
| これまでの作業の経緯 | `DEVLOG.md` |
| 人間向けの画面説明・現状の詳細 | `docs/current-status.md` |
| 利用者向けの使い方・できること / できないこと | `docs/user-guide.md` |

## スキル(`.claude/skills/`)

| スキル | 使う場面 |
|---|---|
| `project-memory-manager` | 現状調査・ドキュメント更新・次のタスク提案(実装の前に必ず) |
| `spring-boot-architect` | 設計レビュー(コードは変えない) |
| `feature-builder` | 新機能の実装 |
| `review-and-refactor` | 動きを変えないコード整理 |

---

## AI 開発運用ルール

### 基本方針

- ユーザーは **プロダクトオーナー**。AI は開発担当として、実装できるタスクは自律的に進める
- 軽微な設計判断は確認不要。判断した内容と理由は PR 本文と `DEVLOG.md` に残す
- 確認するのは「不明点」「仕様変更」「下の『事前確認が必要な作業』」だけ
- 作業ログ(`DEVLOG.md`・`SESSION_SUMMARY.md`)を必ず更新する
- 完了後は PR 作成まで行う。**マージはオーナーが行う**(AI はマージしない)

### タスク実行フロー

TASK を受けたら、次の順で進める。

1. 要件確認(`docs/ai/spec.md`・`docs/TASKS.md` を読み、不明点があればここで質問する)
2. 実装(作業ブランチで。マージ済みの PR のブランチは最新の `master` から作り直す)
3. テスト実施(`cd onboarding-progress-system && mvn -B test`。画面を変えたらスモークテスト相当の確認も)
4. ビルド確認(`mvn -B package -DskipTests`。テストが通っていればビルドも通っている)
5. ドキュメント更新(`docs/ai/conventions.md` の「変更したら更新するもの」に従う)
6. `DEVLOG.md` 更新
7. `SESSION_SUMMARY.md` 更新(`docs/TASKS.md` の状態も合わせて更新)
8. Commit(下のコミット規約)
9. Push
10. Pull Request 作成(本文は日本語: 何を変えたか / なぜ / 確認方法 / テスト結果)
11. 作業結果報告(日本語。専門用語は少なめに)

### 確認不要な作業(自律的に進めて PR まで作る)

- バグ修正
- リファクタリング(1 つの PR は 1 つのテーマ。頼まれた範囲・関係する範囲だけ)
- テスト追加
- CI/CD 修正(必須チェック名 `test` / `call-reusable / run-smoke` は変えない)
- コメント修正
- README 更新
- 作業ログ更新
- PR 作成

### 事前確認が必要な作業(実装前に「原因・方針」を短く説明して了承をもらう)

- DB スキーマ変更(新しい Flyway マイグレーション、サンプルデータの変更を含む)
- API 互換性を壊す変更(URL・JSON の形・ステータスコードの変更)
- 認証・認可方式の変更(ログイン方法、権限、`SecurityConfig`)
- ライブラリの大幅な変更(Spring Boot のメジャーバージョン、新しいフレームワークの追加など)
- インフラ構成の変更(Docker Compose・Codespaces・GitHub Actions の構成を大きく変えるもの)
- データ削除処理(既存データを消す・消える処理の追加や変更)
- 仕様変更(`docs/requirements.md` の要件や、`docs/ai/spec.md` の壊してはいけないルール INV を変えるもの)

### 自律実行ルール

次がすべて満たされたら、追加確認なしで PR 作成まで進めてよい。

- 要件が明確
- DB 変更なし
- 認証方式の変更なし
- ビルド成功
- テスト成功

1 つでも満たさないときは、そこで止めて報告・確認する(テストが失敗したまま PR を作らない)。

### PR 運用

- 実装が終わったら、原則 PR 作成まで行う
- PR を作ったら、CI(`test` と `call-reusable / run-smoke`)の結果を確認し、失敗したら直して push する
- ドキュメントだけの PR は CI がスキップ扱いになる(正常)

### コミット規約

`<種類>: <英語の命令形の要約>` の形で書く(例: `feat: Add due date reminders to the dashboard`)。

| 種類 | 使う場面 |
|---|---|
| `feat:` | 機能の追加・変更 |
| `fix:` | バグ修正 |
| `refactor:` | 動きを変えない整理 |
| `test:` | テストの追加・修正 |
| `docs:` | ドキュメント・作業ログだけの変更 |
| `ci:` | GitHub Actions・Docker・Codespaces の設定 |

### 管理ドキュメント(なければ作る)

| ファイル | 役割 | 更新のタイミング |
|---|---|---|
| `docs/TASKS.md` | 今のタスクの一覧(進行中・次にやる・確認待ち・最近の完了) | タスクを受けたとき・終わったとき |
| `DEVLOG.md` | 作業ログ(日付ごとに、何をしたか・判断・テスト結果・PR) | タスクが終わるたび(新しいものを上に追記) |
| `SESSION_SUMMARY.md` | 次のセッションへの引き継ぎ(今の状態・直前の作業・次の一手・未解決) | セッションの終わりに書き換える |

ロードマップと技術的負債の詳細は、これまでどおり `docs/ai/backlog.md` に書く(`docs/TASKS.md` からは項目 ID で参照する)。

### セッション終了時

作業が終わったら、必ず次を行う。

- `DEVLOG.md` の更新
- `SESSION_SUMMARY.md` の更新

---

## 特に守ること(詳細は上のファイル)

- `master` に直接 push しない。作業ブランチで変更し、PR を作る(マージはオーナー)
- パスワード・認証情報をコード・ログ・ドキュメントに書かない
- 修正はタスクの範囲だけ。関係ない整形・リファクタリングを混ぜない
- ユーザーへの説明は日本語で、専門用語を使いすぎない
- push 前に `cd onboarding-progress-system && mvn -B test` を通す
- 機能やテーブルを変えたら `docs/ai/`(特に `spec.md`)、`docs/current-status.md`、`docs/requirements.md` / `docs/design.md` / `docs/design-diagrams.md` も更新する
