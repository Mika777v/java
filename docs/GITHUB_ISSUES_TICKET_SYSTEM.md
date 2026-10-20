# GitHub Issues 一覧 — チケット管理への移行

- 版: 1.0(2026-10-20)
- 元にしたもの: [TASKS_TICKET_SYSTEM.md](TASKS_TICKET_SYSTEM.md)(Epic / Story / Task)、[ROADMAP_TICKET_SYSTEM.md](ROADMAP_TICKET_SYSTEM.md)
- このファイルの 3 章以降は `TASKS_TICKET_SYSTEM.md` から作っています。Issue を足す・変えるときは、先に `TASKS_TICKET_SYSTEM.md` を直してください

---

## 1. GitHub Projects の運用

### 1.1 Issue の階層

| 種類 | GitHub での形 | 例 | 数 |
|---|---|---|---|
| Epic | Issue(ラベル `type:epic`)。Story を **sub-issue** としてぶら下げる | `[E1] チケットの基本` | 11 |
| Story | Issue(ラベル `type:story`)。**1 Story = 原則 1 PR**。PR 本文に `Closes #<Story の番号>` | `[S1.3] チケット一覧・登録・編集の画面` | 50 |
| Task | Story の本文のチェックリスト(`- [ ] T1.3.1 …`)。1 つの Task だけで別 PR にするときは、その Task を sub-issue に格上げする | `T1.3.1 TicketController と TicketForm` | 78 |

Task をすべて Issue にすると 78 件増えて管理が重くなるため、普段は Story の中のチェックリストにします。

Issue のひな形は `.github/ISSUE_TEMPLATE/`(`1-epic.md`・`2-story.md`・`3-task.md`)にあります。ラベル `type:*` が自動で付きます。

### 1.2 Status(ボードの列)

| Status | 意味(この列に入る条件) | 誰が動かす |
|---|---|---|
| **Backlog** | 登録済み。まだ始められない(依存する Story が終わっていない、または ⚠ の確認が済んでいない) | 登録時の初期値 |
| **Ready** | すぐ始められる(Definition of Ready を満たす: 依存が Done、⚠ が了承済み、完了の条件が書いてある) | オーナー または AI(条件を満たしたとき) |
| **In Progress** | 作業中(作業ブランチで実装中)。**同時に 1〜2 件まで** | AI(着手したとき) |
| **Review** | PR を作成済み、CI が緑。オーナーのレビュー・Codespaces での確認待ち | AI(PR を作ったとき) |
| **Done** | PR がマージされた(画面のある Story は Codespaces での確認も済み) | 自動(PR のマージで Issue が閉じる) |

```
Backlog ──(依存 Done・了承済み)──▶ Ready ──(着手)──▶ In Progress ──(PR・CI 緑)──▶ Review ──(マージ)──▶ Done
                                                       ▲                                 │
                                                       └──────(CI 赤・レビューで直す)─────┘
```

- Epic は、最初の Story が In Progress になったら In Progress、すべての Story が Done になったら Done にする
- CI が赤のまま Review に置かない(直してから Review)
- 「事前確認が必要な作業」(⚠)は、了承をもらうまで Ready にしない

### 1.3 項目(Projects のカスタムフィールド)

| 項目 | 種類 | 値 |
|---|---|---|
| Status | 単一選択 | Backlog / Ready / In Progress / Review / Done |
| Type | 単一選択 | Epic / Story / Task |
| Phase | 単一選択 | P0〜P10 |
| Size | 単一選択 | S(半日〜1 日)/ M(2〜3 日)/ L(4〜5 日) |
| Toggle | 単一選択 | なし / ticket-system / project-management / comments / notifications / extended-roles / ticket-dashboard / kanban-board / onboarding-legacy |
| Approval | 単一選択 | 不要 / 了承済み / 確認待ち |

### 1.4 ラベル

| ラベル | 用途 |
|---|---|
| `type:epic` / `type:story` / `type:task` | 種類 |
| `phase:0` 〜 `phase:10` | フェーズ |
| `toggle:<名前>` | その Story の機能を守る Feature Toggle |
| `area:db` / `area:domain` / `area:ui` / `area:api` / `area:auth` / `area:ci` / `area:config` / `area:refactor` / `area:docs` | 主に触る場所 |
| `needs-approval` | 運用ルールの「事前確認が必要な作業」で、まだ了承がない(Ready にしない) |
| `approved` | 事前確認が必要な作業で、了承済み |

### 1.5 マイルストーン

| マイルストーン | 含む Phase | 目安 |
|---|---|---|
| M1 MVP(Phase0〜3) | 0〜3 | 開始から約 10 週 |
| M2 拡張(Phase4〜9) | 4〜9 | 約 24 週 |
| M3 全面移行(Phase10) | 10 | 約 26 週 |

### 1.6 ビュー(Projects の画面)

| ビュー | 形 | 絞り込み・並び |
|---|---|---|
| ボード | Board(Status ごとの列) | `Type: Story`。今の作業を見る |
| フェーズ別 | Table | Phase でグループ、Status で並べる |
| Epic 一覧 | Table | `Type: Epic`。sub-issue の進み具合を見る |
| 確認待ち | Table | ラベル `needs-approval`。オーナーが決めることの一覧 |

### 1.7 自動化(Projects の組み込みワークフロー)

| ワークフロー | 設定 |
|---|---|
| Auto-add to project | このリポジトリの Issue・PR を自動で追加 |
| Item added to project | Status を **Backlog** に |
| Item closed / Pull request merged | Status を **Done** に |
| Item reopened | Status を **In Progress** に |

Ready・In Progress・Review への移動は手で行います(AI が作業の区切りで動かす。オーナーは Ready にするかどうかを決める)。

### 1.8 PR との結び付け

- PR 本文に `Closes #<Story の Issue 番号>` を書く(マージで Story が閉じ、Done へ)
- PR のタイトルは今のコミット規約どおり(`feat: …` など)。本文の「何を変えたか」に Story の ID(例: `S1.3`)を書く
- 1 つの PR で Story の Task を全部終えられないときは、終わった Task にチェックを付け、残りは次の PR で(Story は最後の PR で閉じる)

---

## 2. 今の状況(2026-10-20)

- PRD 10 章の回答: Q1・Q2・Q3(論理削除)・Q4・Q6・Q7 は OK、Q5 は「ロールとログイン方法を分けない(今のまま)、4 ロールは Toggle で切り替え」
- 初期の Ready: S0.1、S0.2、S0.3、S0.5、S0.6(依存がなく、事前確認も不要)
- 確認待ち(`needs-approval`): なし(S3.2・S10.1・S10.4 も 2026-10-20 に了承。S10.4 はオーナーの指示で着手)

---

## 3. Epic 一覧

| Epic | タイトル | Phase | Toggle | Story 数 | マイルストーン | 初期 Status |
|---|---|---|---|---|---|---|
| E0 | `[E0] 移行の土台(Feature Toggle・Service 層の整理)` | P0 | (仕組み) | 6 | M1 MVP(Phase0〜3) | Ready |
| E1 | `[E1] チケットの基本(Task → Ticket の一般化)` | P1 | `ticket-system` | 8 | M1 MVP(Phase0〜3) | Backlog |
| E2 | `[E2] プロジェクト` | P2 | `project-management` | 6 | M1 MVP(Phase0〜3) | Backlog |
| E3 | `[E3] ステータス管理の強化` | P3 | `ticket-system` | 3 | M1 MVP(Phase0〜3) | Backlog |
| E4 | `[E4] コメント` | P4 | `comments` | 3 | M2 拡張(Phase4〜9) | Backlog |
| E5 | `[E5] 検索と API` | P5 | `ticket-system` | 4 | M2 拡張(Phase4〜9) | Backlog |
| E6 | `[E6] 通知` | P6 | `notifications` | 4 | M2 拡張(Phase4〜9) | Backlog |
| E7 | `[E7] 権限管理(4 ロール)` | P7 | `extended-roles` | 5 | M2 拡張(Phase4〜9) | Backlog |
| E8 | `[E8] ダッシュボード` | P8 | `ticket-dashboard` | 3 | M2 拡張(Phase4〜9) | Backlog |
| E9 | `[E9] カンバンボード` | P9 | `kanban-board` | 3 | M2 拡張(Phase4〜9) | Backlog |
| E10 | `[E10] 全面移行と Toggle の撤去` | P10 | `onboarding-legacy` | 5 | M3 全面移行(Phase10) | Backlog |

---

## 4. Story 一覧(GitHub Issue)

各 Story の Issue のタイトル・ラベル・初期 Status・完了の条件です。本文の Task(チェックリスト)は 5 章。

| Issue タイトル | 親 Epic | ラベル | Size | 依存 | 初期 Status | 完了の条件 |
|---|---|---|---|---|---|---|
| `[S0.1] Feature Toggle の仕組みを入れる` | E0 | `type:story` `phase:0` `area:config` | M | — | Ready | Toggle を ON/OFF にしたテストが両方通る。OFF の URL は 404。既存 221 件のテストが変わらず通る |
| `[S0.2] `TaskService` を導入する(D1 / R1)` | E0 | `type:story` `phase:0` `area:refactor` | M | — | Ready | タスクの CRUD が `TaskService` 経由になり、`layer-check.sh` で `TaskPageController`・`TaskController` の Repository 直接呼び出しが 0。既存テストが変わらず通る |
| `[S0.3] `UserService` を導入する(D2・D6 / R2・R7)` | E0 | `type:story` `phase:0` `area:refactor` | L | — | Ready | ユーザーの登録・更新・削除・パスワード変更が `UserService` 経由。既存テストが変わらず通る |
| `[S0.4] `UserPageController` を分割する(D3 / R3)` | E0 | `type:story` `phase:0` `area:refactor` | M | S0.3 | Backlog | URL は変わらず、`UserPageController` が 250 行以下。既存テストが変わらず通る |
| `[S0.5] 画面の表を部品にする(D4 / R4)` | E0 | `type:story` `phase:0` `area:ui` | S | — | Ready | マイページとユーザー詳細の表が同じ部品を使う。画面の文言のテストが変わらず通る |
| `[S0.6] `open-in-view` の方針を決めて、新しい画面のルールにする(D8)` | E0 | `type:story` `phase:0` `area:docs` | S | — | Ready | `conventions.md` に新画面のルールが書かれている |
| `[S1.1] `tickets` テーブルと Entity を作る` | E1 | `type:story` `phase:1` `area:db` `toggle:ticket-system` `approved` | M | S0.1 | Backlog | V12 が既存の DB に適用でき、旧画面に影響がない。`Ticket` の単体テスト(完了日時・期限切れ)が通る |
| `[S1.2] `TicketService` と `TicketRepository`` | E1 | `type:story` `phase:1` `area:domain` `toggle:ticket-system` | M | S1.1 | Backlog | 登録・更新・論理削除・ステータス変更・一覧のテストが通る。一覧は関連を 1 回で読む |
| `[S1.3] チケット一覧・登録・編集の画面` | E1 | `type:story` `phase:1` `area:ui` `toggle:ticket-system` | L | S1.2 | Backlog | Toggle ON で一覧・登録・編集ができる。入力エラー・404・新入社員 403・Toggle OFF 404 のテストが通る |
| `[S1.4] チケット詳細・削除・ステータス変更` | E1 | `type:story` `phase:1` `area:ui` `toggle:ticket-system` `approved` | M | S1.3 | Backlog | 詳細・削除・ステータス変更ができる。論理削除したチケットは一覧・詳細に出ないが、DB には残る |
| `[S1.5] メニューと Toggle の画面表示` | E1 | `type:story` `phase:1` `area:ui` `toggle:ticket-system` | S | S1.3 | Backlog | Toggle OFF で今の画面とメニューが変わらない(テストで確認) |
| `[S1.6] 再起動なしの ON/OFF(`feature_flags` と `/admin/features`)` | E1 | `type:story` `phase:1` `area:db` `toggle:ticket-system` `approved` | M | S0.1 | Backlog | `/admin/features` で切り替えると、再起動なしで次の画面から反映。ADMIN 以外は開けない |
| `[S1.7] CI のスモークテストに Toggle ON の確認を足す` | E1 | `type:story` `phase:1` `area:ci` `toggle:ticket-system` | S | S1.3 | Backlog | CI のスモークテストで Toggle ON のチケット登録・表示が通る。今の確認はそのまま |
| `[S1.8] ドキュメントと Codespaces の確認手順` | E1 | `type:story` `phase:1` `area:docs` `toggle:ticket-system` | S | S1.4 | Backlog | `spec.md`・`requirements.md`・`user-guide.md`・`codespaces-check.md` が更新され、撤去 Issue(S10.3)が登録済み |
| `[S2.1] `projects` テーブルと既定のプロジェクト `GEN`` | E2 | `type:story` `phase:2` `area:db` `toggle:project-management` `approved` | M | S1.1 | Backlog | V13 が既存の DB に適用でき、既存のチケットは `GEN` に入る |
| `[S2.2] チケット番号 `KEY-n` の採番` | E2 | `type:story` `phase:2` `area:domain` `toggle:project-management` | M | S2.1 | Backlog | 同時に起票しても番号が重ならないテストが通る。番号の URL で開ける |
| `[S2.3] プロジェクトの画面` | E2 | `type:story` `phase:2` `area:ui` `toggle:project-management` | M | S2.1 | Backlog | プロジェクトの一覧・登録・編集・アーカイブができる。キーの重複・形式のエラーが出る |
| `[S2.4] チケットにプロジェクトを付ける` | E2 | `type:story` `phase:2` `area:ui` `toggle:project-management` | M | S2.2 | Backlog | 登録時にプロジェクトを選べる。Toggle OFF なら `GEN` 固定 |
| `[S2.5] テンプレートから一括起票` | E2 | `type:story` `phase:2` `area:domain` `toggle:project-management` | L | S2.4・S0.2 | Backlog | 有効なひな形から、選んだ人のチケットがまとめて作られる。二度押しで二重にならない |
| `[S2.6] 表示名「タスク」→「オンボーディングのひな形」` | E2 | `type:story` `phase:2` `area:ui` `toggle:project-management` | S | S2.5 | Backlog | 旧画面の表示名が変わり、URL・クラス名は変わらない |
| `[S3.1] REVIEW・CLOSED を追加` | E3 | `type:story` `phase:3` `area:db` `toggle:ticket-system` `approved` | M | S1.2 | Backlog | REVIEW・CLOSED を選べる。完了日時の規則のテストが通る |
| `[S3.2] 状態の移り方の決まり` | E3 | `type:story` `phase:3` `area:domain` `toggle:ticket-system` `approved` | M | S3.1 | Backlog | 移れない組み合わせはエラー。`design-diagrams.md` に状態遷移図 |
| `[S3.3] ステータスの見た目と MVP の確認` | E3 | `type:story` `phase:3` `area:ui` `toggle:ticket-system` | S | S3.2 | Backlog | 色分け済み。オーナーが Codespaces で MVP を確認 |
| `[S4.1] `ticket_comments` と Service` | E4 | `type:story` `phase:4` `area:db` `toggle:comments` `approved` | M | S1.2 | Backlog | コメントの保存・論理削除のテストが通る |
| `[S4.2] 詳細画面のコメント欄` | E4 | `type:story` `phase:4` `area:ui` `toggle:comments` | M | S4.1 | Backlog | 詳細画面でコメントの一覧と投稿ができ、URL はリンク、そのほかはエスケープ |
| `[S4.3] 自分のコメントの編集・削除` | E4 | `type:story` `phase:4` `area:ui` `toggle:comments` | M | S4.2 | Backlog | 他人のコメントは 404(ADMIN は可)。Toggle OFF で欄が出ない |
| `[S5.1] 検索条件と Service` | E5 | `type:story` `phase:5` `area:domain` `toggle:ticket-system` | M | S2.4 | Backlog | 条件の組み合わせのテストが通る。条件は URL に残る |
| `[S5.2] ページ分けと索引` | E5 | `type:story` `phase:5` `area:db` `toggle:ticket-system` `approved` | M | S5.1 | Backlog | 1,000 件のデータで一覧が 1 画面数回のクエリ |
| `[S5.3] 検索画面` | E5 | `type:story` `phase:5` `area:ui` `toggle:ticket-system` | M | S5.1 | Backlog | 検索フォーム・件数・クリアが使える |
| `[S5.4] チケット API(DTO)` | E5 | `type:story` `phase:5` `area:api` `toggle:ticket-system` | M | S5.1 | Backlog | `/api/tickets` が DTO で入出力。`/api/tasks` は変わらない |
| `[S6.1] `notifications` テーブルと Service` | E6 | `type:story` `phase:6` `area:db` `toggle:notifications` `approved` | M | S1.2 | Backlog | 通知の作成・未読数・既読のテストが通る |
| `[S6.2] 出来事から通知を作る` | E6 | `type:story` `phase:6` `area:domain` `toggle:notifications` | M | S6.1・S4.1 | Backlog | 担当者になった・コメントが付いたときに通知ができる。Toggle OFF なら作られない |
| `[S6.3] 期限前日の通知` | E6 | `type:story` `phase:6` `area:domain` `toggle:notifications` | M | S6.1 | Backlog | 期限前日の通知が 1 日 1 回、二重にならない(時刻は `Clock`) |
| `[S6.4] 通知の画面` | E6 | `type:story` `phase:6` `area:ui` `toggle:notifications` | M | S6.1 | Backlog | 未読数・一覧・既読が使える。他人の通知は 404 |
| `[S7.1] MANAGER・VIEWER のログイン規則(パスワード)` | E7 | `type:story` `phase:7` `area:auth` `toggle:extended-roles` `approved` | M | S0.3 | Backlog | 今のログインの動きのテストが通ったまま、MANAGER・VIEWER がパスワードでログインできる |
| `[S7.2] MANAGER・VIEWER の追加` | E7 | `type:story` `phase:7` `area:auth` `toggle:extended-roles` `approved` | M | S7.1 | Backlog | Toggle ON で MANAGER・VIEWER を選べる。OFF なら ADMIN / MEMBER だけ |
| `[S7.3] プロジェクトのメンバー` | E7 | `type:story` `phase:7` `area:db` `toggle:extended-roles` `approved` | M | S2.1 | Backlog | プロジェクトにメンバーを追加・削除できる |
| `[S7.4] 権限表をクエリ条件で実装` | E7 | `type:story` `phase:7` `area:auth` `toggle:extended-roles` `approved` | L | S7.2・S7.3 | Backlog | 4 ロール × 主要操作のテスト表がすべて通る。Toggle OFF 時は弱い権限に倒れる |
| `[S7.5] 新入社員をオンボーディングのプロジェクトのメンバーにする` | E7 | `type:story` `phase:7` `area:domain` `toggle:extended-roles` | S | S7.3・S2.5 | Backlog | 一括起票した新入社員が自動でメンバーになる |
| `[S8.1] チケットの集計` | E8 | `type:story` `phase:8` `area:domain` `toggle:ticket-dashboard` | M | S5.1 | Backlog | 集計が 1 クエリでロールの範囲どおり |
| `[S8.2] ダッシュボード画面` | E8 | `type:story` `phase:8` `area:ui` `toggle:ticket-dashboard` | M | S8.1 | Backlog | Toggle ON でトップページにチケットの欄。OFF なら今のまま |
| `[S8.3] マイページに自分のチケット` | E8 | `type:story` `phase:8` `area:ui` `toggle:ticket-dashboard` | S | S8.1 | Backlog | `/me` に自分の担当チケット。旧オンボーディングの表はそのまま |
| `[S9.1] ボード画面(ボタン版)` | E9 | `type:story` `phase:9` `area:ui` `toggle:kanban-board` | M | S3.2・S2.4 | Backlog | プロジェクトのボードにステータスの列とカード |
| `[S9.2] カードの移動` | E9 | `type:story` `phase:9` `area:ui` `toggle:kanban-board` | M | S9.1 | Backlog | ボタンで隣の列へ動かせる。移れない組み合わせ・権限のないカードは動かない |
| `[S9.3] (任意)ドラッグ操作` | E9 | `type:story` `phase:9` `area:ui` `toggle:kanban-board` `approved` | M | S9.2 | Backlog | ドラッグで動かせる。JavaScript なしでもボタンで使える |
| `[S10.1] オンボーディングのデータをチケットへ写す` | E10 | `type:story` `phase:10` `area:db` `toggle:onboarding-legacy` `approved` | L | Phase9 まで | Backlog | リハーサルで件数が一致。本番実行後も一致し、二重に写っていない |
| `[S10.2] 旧画面の退役(`onboarding-legacy` を OFF)` | E10 | `type:story` `phase:10` `area:ui` `toggle:onboarding-legacy` `approved` | M | S10.1 | Backlog | 旧 URL から新画面へ案内。OFF にして 2 週間問題なし |
| `[S10.3] Toggle の撤去(1 Toggle = 1 PR)` | E10 | `type:story` `phase:10` `area:config` `toggle:onboarding-legacy` | M × 7 | 各 Toggle が既定 ON で 2 週間安定 | Backlog | Toggle 1 つにつき 1 PR で、分岐・設定・環境変数・OFF 側のテストが消えている |
| `[S10.4] 旧コードの削除` | E10 | `type:story` `phase:10` `area:refactor` `toggle:onboarding-legacy` `approved` | L | S10.2 | Backlog | 旧 Controller・画面・テストが消え、全テストが通る。古いテーブルの扱いが記録されている |
| `[S10.5] ドキュメントの総仕上げ` | E10 | `type:story` `phase:10` `area:docs` `toggle:onboarding-legacy` | M | S10.4 | Backlog | 人向け・AI 向けのドキュメントがチケット管理の内容になっている |

---

## 5. Issue 本文(Epic → Story → Task)

Issue を作るときは、下の Story ごとのブロックを本文にそのまま使えます。

### [E0] 移行の土台(Feature Toggle・Service 層の整理)(Phase0・Toggle: (仕組み))

<details><summary><b>[S0.1] Feature Toggle の仕組みを入れる</b>(Size M・依存 —・確認 不要)</summary>

```markdown
## 目的
Feature Toggle の仕組みを入れる(親: E0 移行の土台(Feature Toggle・Service 層の整理))

## やること
- [ ] T0.1.1 `Feature` enum(名前・親・既定値)を作る。設定 `features.*.enabled` は `Environment` から読む(`FeatureProperties` は作らないことにした)
- [ ] T0.1.2 `FeatureFlags`(`@Component("features")`、親が OFF なら子も OFF)と単体テスト
- [ ] T0.1.3 `@RequiresFeature` + `FeatureToggleInterceptor`(OFF なら 404)を `WebConfig` に登録、テスト
- [ ] T0.1.4 `application.yml`・`docker-compose.yml`・`.devcontainer/start-app.sh`・README に `FEATURE_*` を追加(すべて既定 OFF)
- [ ] T0.1.5 `docs/ai/architecture.md`・`conventions.md` に Toggle の使い方を追記

## 完了の条件
- Toggle を ON/OFF にしたテストが両方通る。OFF の URL は 404。既存 221 件のテストが変わらず通る
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
—

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S0.1
- docs/ROADMAP_TICKET_SYSTEM.md の Phase0
```

</details>
<details><summary><b>[S0.2] `TaskService` を導入する(D1 / R1)</b>(Size M・依存 —・確認 不要)</summary>

```markdown
## 目的
`TaskService` を導入する(D1 / R1)(親: E0 移行の土台(Feature Toggle・Service 層の整理))

## やること
- [ ] T0.2.1 `TaskPageController`・`TaskController` のタスク CRUD を `TaskService` に移す(動きは変えない)
- [ ] T0.2.2 既存テストがそのまま通ることを確認、`layer-check.sh` で直接呼び出しが減ったことを確認

## 完了の条件
- タスクの CRUD が `TaskService` 経由になり、`layer-check.sh` で `TaskPageController`・`TaskController` の Repository 直接呼び出しが 0。既存テストが変わらず通る
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
—

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S0.2
- docs/ROADMAP_TICKET_SYSTEM.md の Phase0
```

</details>
<details><summary><b>[S0.3] `UserService` を導入する(D2・D6 / R2・R7)</b>(Size L・依存 —・確認 不要)</summary>

```markdown
## 目的
`UserService` を導入する(D2・D6 / R2・R7)(親: E0 移行の土台(Feature Toggle・Service 層の整理))

## やること
- [ ] T0.3.1 ユーザーの登録・更新(メール重複・パスワード・専用URLの削除)を `UserService` に
- [ ] T0.3.2 削除(自分自身は不可)と、`MyPageController` のパスワード変更を `UserService` に
- [ ] T0.3.3 既存テスト(`UserPageControllerTest`・`PasswordResetTest` ほか)がそのまま通ることを確認

## 完了の条件
- ユーザーの登録・更新・削除・パスワード変更が `UserService` 経由。既存テストが変わらず通る
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
—

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S0.3
- docs/ROADMAP_TICKET_SYSTEM.md の Phase0
```

</details>
<details><summary><b>[S0.4] `UserPageController` を分割する(D3 / R3)</b>(Size M・依存 S0.3・確認 不要)</summary>

```markdown
## 目的
`UserPageController` を分割する(D3 / R3)(親: E0 移行の土台(Feature Toggle・Service 層の整理))

## やること
- [ ] T0.4.1 割り当ての操作を `UserAssignmentController` に、専用URL・リセットを `UserAccessController` に(URL は変えない)

## 完了の条件
- URL は変わらず、`UserPageController` が 250 行以下。既存テストが変わらず通る
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S0.3

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S0.4
- docs/ROADMAP_TICKET_SYSTEM.md の Phase0
```

</details>
<details><summary><b>[S0.5] 画面の表を部品にする(D4 / R4)</b>(Size S・依存 —・確認 不要)</summary>

```markdown
## 目的
画面の表を部品にする(D4 / R4)(親: E0 移行の土台(Feature Toggle・Service 層の整理))

## やること
- [ ] T0.5.1 マイページとユーザー詳細の割り当ての表を `fragments/assignments.html` に

## 完了の条件
- マイページとユーザー詳細の表が同じ部品を使う。画面の文言のテストが変わらず通る
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
—

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S0.5
- docs/ROADMAP_TICKET_SYSTEM.md の Phase0
```

</details>
<details><summary><b>[S0.6] `open-in-view` の方針を決めて、新しい画面のルールにする(D8)</b>(Size S・依存 —・確認 不要)</summary>

```markdown
## 目的
`open-in-view` の方針を決めて、新しい画面のルールにする(D8)(親: E0 移行の土台(Feature Toggle・Service 層の整理))

## やること
- [ ] T0.6.1 新しいチケット画面は Service で必要な関連を読み込む(`@EntityGraph`)、をルールとして `conventions.md` に。全体の OFF は Phase10 の後に検討

## 完了の条件
- `conventions.md` に新画面のルールが書かれている
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
—

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S0.6
- docs/ROADMAP_TICKET_SYSTEM.md の Phase0
```

</details>

### [E1] チケットの基本(Task → Ticket の一般化)(Phase1・Toggle: `ticket-system`)

<details><summary><b>[S1.1] `tickets` テーブルと Entity を作る</b>(Size M・依存 S0.1・確認 ✅ DB(Q2))</summary>

```markdown
## 目的
`tickets` テーブルと Entity を作る(親: E1 チケットの基本(Task → Ticket の一般化))

## やること
- [ ] T1.1.1 V12: `tickets`(id, number, title, description, status, assignee_id, reporter_id, due_date, completed_at, origin_task_id, origin_assignment_id, deleted_at, created_at, updated_at)、索引
- [ ] T1.1.2 `Ticket`・`TicketStatus`(TODO / IN_PROGRESS / DONE、`getLabel()`)、`changeStatus`・`isOverdue`(今の規則を写す)
- [ ] T1.1.3 `AssignmentStatus` → `TicketStatus` の対応表(1 か所)と単体テスト
- [ ] T1.1.4 `docs/ai/database.md`・`design-diagrams.md`(ER)を更新

## 完了の条件
- V12 が既存の DB に適用でき、旧画面に影響がない。`Ticket` の単体テスト(完了日時・期限切れ)が通る
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S0.1

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S1.1
- docs/ROADMAP_TICKET_SYSTEM.md の Phase1
```

</details>
<details><summary><b>[S1.2] `TicketService` と `TicketRepository`</b>(Size M・依存 S1.1・確認 不要)</summary>

```markdown
## 目的
`TicketService` と `TicketRepository`(親: E1 チケットの基本(Task → Ticket の一般化))

## やること
- [ ] T1.2.1 登録・更新・論理削除・ステータス変更・取得(削除済みは出さない)、`Clock` を使う
- [ ] T1.2.2 一覧(ステータス・担当者で絞り込み、更新日時の新しい順、関連は `@EntityGraph`)
- [ ] T1.2.3 単体・結合テスト(完了日時の記録・消去、期限切れ)

## 完了の条件
- 登録・更新・論理削除・ステータス変更・一覧のテストが通る。一覧は関連を 1 回で読む
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S1.1

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S1.2
- docs/ROADMAP_TICKET_SYSTEM.md の Phase1
```

</details>
<details><summary><b>[S1.3] チケット一覧・登録・編集の画面</b>(Size L・依存 S1.2・確認 不要)</summary>

```markdown
## 目的
チケット一覧・登録・編集の画面(親: E1 チケットの基本(Task → Ticket の一般化))

## やること
- [ ] T1.3.1 `TicketController`(`@RequiresFeature(TICKET_SYSTEM)`)と `TicketForm`(タイトル必須・200 文字)
- [ ] T1.3.2 `templates/tickets/list.html`・`form.html`(表は部品で作る)
- [ ] T1.3.3 テスト: 正常系・入力エラー・404・新入社員は 403・Toggle OFF で 404

## 完了の条件
- Toggle ON で一覧・登録・編集ができる。入力エラー・404・新入社員 403・Toggle OFF 404 のテストが通る
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S1.2

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S1.3
- docs/ROADMAP_TICKET_SYSTEM.md の Phase1
```

</details>
<details><summary><b>[S1.4] チケット詳細・削除・ステータス変更</b>(Size M・依存 S1.3・確認 ✅ 論理削除(Q3))</summary>

```markdown
## 目的
チケット詳細・削除・ステータス変更(親: E1 チケットの基本(Task → Ticket の一般化))

## やること
- [ ] T1.4.1 詳細画面(説明はリンク表示)、削除(論理削除、確認ダイアログ)、ステータス変更
- [ ] T1.4.2 テスト一式

## 完了の条件
- 詳細・削除・ステータス変更ができる。論理削除したチケットは一覧・詳細に出ないが、DB には残る
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S1.3

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S1.4
- docs/ROADMAP_TICKET_SYSTEM.md の Phase1
```

</details>
<details><summary><b>[S1.5] メニューと Toggle の画面表示</b>(Size S・依存 S1.3・確認 不要)</summary>

```markdown
## 目的
メニューと Toggle の画面表示(親: E1 チケットの基本(Task → Ticket の一般化))

## やること
- [ ] T1.5.1 `fragments/nav.html` に「チケット」(`${@features.on('ticket-system')}` のときだけ)
- [ ] T1.5.2 Toggle OFF で今の画面と HTML が変わらないことのテスト

## 完了の条件
- Toggle OFF で今の画面とメニューが変わらない(テストで確認)
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S1.3

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S1.5
- docs/ROADMAP_TICKET_SYSTEM.md の Phase1
```

</details>
<details><summary><b>[S1.6] 再起動なしの ON/OFF(`feature_flags` と `/admin/features`)</b>(Size M・依存 S0.1・確認 ✅ DB(Q2・Q4))</summary>

```markdown
## 目的
再起動なしの ON/OFF(`feature_flags` と `/admin/features`)(親: E1 チケットの基本(Task → Ticket の一般化))

## やること
- [ ] T1.6.1 `feature_flags`(name, enabled, updated_by, updated_at)と、DB → 設定の順で読む処理
- [ ] T1.6.2 管理画面(ADMIN のみ。親子関係の表示、切り替えの確認ダイアログ)、テスト

## 完了の条件
- `/admin/features` で切り替えると、再起動なしで次の画面から反映。ADMIN 以外は開けない
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S0.1

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S1.6
- docs/ROADMAP_TICKET_SYSTEM.md の Phase1
```

</details>
<details><summary><b>[S1.7] CI のスモークテストに Toggle ON の確認を足す</b>(Size S・依存 S1.3・確認 不要)</summary>

```markdown
## 目的
CI のスモークテストに Toggle ON の確認を足す(親: E1 チケットの基本(Task → Ticket の一般化))

## やること
- [ ] T1.7.1 `reusable-docker-smoke.yml` で `FEATURE_TICKET_SYSTEM=true` にして、チケットを 1 件登録・表示(今の確認は残す)

## 完了の条件
- CI のスモークテストで Toggle ON のチケット登録・表示が通る。今の確認はそのまま
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S1.3

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S1.7
- docs/ROADMAP_TICKET_SYSTEM.md の Phase1
```

</details>
<details><summary><b>[S1.8] ドキュメントと Codespaces の確認手順</b>(Size S・依存 S1.4・確認 不要)</summary>

```markdown
## 目的
ドキュメントと Codespaces の確認手順(親: E1 チケットの基本(Task → Ticket の一般化))

## やること
- [ ] T1.8.1 `spec.md`(TK-01〜05・TK-60・61)、`requirements.md`、`user-guide.md`(β の章)、`codespaces-check.md`(Toggle を ON にする手順と確認 3〜5 項目)
- [ ] T1.8.2 公開 PR(既定を ON にする)は確認後に別で作る。撤去 Issue(S10.3)をこの時点で登録

## 完了の条件
- `spec.md`・`requirements.md`・`user-guide.md`・`codespaces-check.md` が更新され、撤去 Issue(S10.3)が登録済み
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S1.4

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S1.8
- docs/ROADMAP_TICKET_SYSTEM.md の Phase1
```

</details>

### [E2] プロジェクト(Phase2・Toggle: `project-management`)

<details><summary><b>[S2.1] `projects` テーブルと既定のプロジェクト `GEN`</b>(Size M・依存 S1.1・確認 ✅ DB(Q2))</summary>

```markdown
## 目的
`projects` テーブルと既定のプロジェクト `GEN`(親: E2 プロジェクト)

## やること
- [ ] T2.1.1 V13: `projects`(key UK, name, description, next_number, archived)、`GEN` を作成、`tickets.project_id`(既存は `GEN`)、(project_id, number) の一意制約
- [ ] T2.1.2 `Project`・`ProjectRepository`・`ProjectService`

## 完了の条件
- V13 が既存の DB に適用でき、既存のチケットは `GEN` に入る
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S1.1

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S2.1
- docs/ROADMAP_TICKET_SYSTEM.md の Phase2
```

</details>
<details><summary><b>[S2.2] チケット番号 `KEY-n` の採番</b>(Size M・依存 S2.1・確認 不要)</summary>

```markdown
## 目的
チケット番号 `KEY-n` の採番(親: E2 プロジェクト)

## やること
- [ ] T2.2.1 `next_number` を行ロックで進める。同時に起票しても重ならないテスト
- [ ] T2.2.2 チケットの URL を番号(`/tickets/ONB-12`)に。古い id の URL は番号へリダイレクト

## 完了の条件
- 同時に起票しても番号が重ならないテストが通る。番号の URL で開ける
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S2.1

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S2.2
- docs/ROADMAP_TICKET_SYSTEM.md の Phase2
```

</details>
<details><summary><b>[S2.3] プロジェクトの画面</b>(Size M・依存 S2.1・確認 不要)</summary>

```markdown
## 目的
プロジェクトの画面(親: E2 プロジェクト)

## やること
- [ ] T2.3.1 一覧・登録(キーは英大文字 2〜10 文字・一意)・編集・アーカイブ、テスト

## 完了の条件
- プロジェクトの一覧・登録・編集・アーカイブができる。キーの重複・形式のエラーが出る
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S2.1

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S2.3
- docs/ROADMAP_TICKET_SYSTEM.md の Phase2
```

</details>
<details><summary><b>[S2.4] チケットにプロジェクトを付ける</b>(Size M・依存 S2.2・確認 不要)</summary>

```markdown
## 目的
チケットにプロジェクトを付ける(親: E2 プロジェクト)

## やること
- [ ] T2.4.1 登録画面でプロジェクトを選ぶ(アーカイブ済みは出さない)、一覧をプロジェクトで分ける。Toggle OFF なら `GEN` 固定

## 完了の条件
- 登録時にプロジェクトを選べる。Toggle OFF なら `GEN` 固定
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S2.2

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S2.4
- docs/ROADMAP_TICKET_SYSTEM.md の Phase2
```

</details>
<details><summary><b>[S2.5] テンプレートから一括起票</b>(Size L・依存 S2.4・S0.2・確認 不要)</summary>

```markdown
## 目的
テンプレートから一括起票(親: E2 プロジェクト)

## やること
- [ ] T2.5.1 `OnboardingTemplateService`: 有効なひな形 → 選んだ人を担当者にしてチケット作成(期限一括、`origin_task_id` を記録)
- [ ] T2.5.2 ひな形のサブ項目をチェックリストとして写す(チケットのチェックリストを作る場合。作らない場合は説明に箇条書きで写す)
- [ ] T2.5.3 画面(プロジェクト詳細から「新入社員の受け入れを起票」)、二度押しで二重に作らないテスト

## 完了の条件
- 有効なひな形から、選んだ人のチケットがまとめて作られる。二度押しで二重にならない
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S2.4・S0.2

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S2.5
- docs/ROADMAP_TICKET_SYSTEM.md の Phase2
```

</details>
<details><summary><b>[S2.6] 表示名「タスク」→「オンボーディングのひな形」</b>(Size S・依存 S2.5・確認 不要)</summary>

```markdown
## 目的
表示名「タスク」→「オンボーディングのひな形」(親: E2 プロジェクト)

## やること
- [ ] T2.6.1 旧画面のメニュー・見出しの表示名だけ変える(URL・クラス名は変えない)。テストの文言を更新

## 完了の条件
- 旧画面の表示名が変わり、URL・クラス名は変わらない
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S2.5

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S2.6
- docs/ROADMAP_TICKET_SYSTEM.md の Phase2
```

</details>

### [E3] ステータス管理の強化(Phase3・Toggle: `ticket-system`)

<details><summary><b>[S3.1] REVIEW・CLOSED を追加</b>(Size M・依存 S1.2・確認 ✅ DB(Q2))</summary>

```markdown
## 目的
REVIEW・CLOSED を追加(親: E3 ステータス管理の強化)

## やること
- [ ] T3.1.1 新しいマイグレーションで `tickets.status` の CHECK を広げる、`TicketStatus` に追加
- [ ] T3.1.2 完了日時: DONE で記録、CLOSED は記録を保つ、それ以外に戻すと消す。期限切れは DONE・CLOSED を除く

## 完了の条件
- REVIEW・CLOSED を選べる。完了日時の規則のテストが通る
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S1.2

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S3.1
- docs/ROADMAP_TICKET_SYSTEM.md の Phase3
```

</details>
<details><summary><b>[S3.2] 状態の移り方の決まり</b>(Size M・依存 S3.1・確認 ✅ 仕様(オーナー了承))</summary>

```markdown
## 目的
状態の移り方の決まり(親: E3 ステータス管理の強化)

## やること
- [ ] T3.2.1 移れる組み合わせの表を `TicketStatus` に持たせ、テスト。決まり: TODO・IN_PROGRESS・REVIEW・DONE の間は自由に移れる。どの状態からも CLOSED にできる。CLOSED から出るのは「再オープン」(→ TODO)だけ
- [ ] T3.2.2 `design-diagrams.md` に状態遷移図を追加

## 完了の条件
- 移れない組み合わせはエラー。`design-diagrams.md` に状態遷移図
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S3.1

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S3.2
- docs/ROADMAP_TICKET_SYSTEM.md の Phase3
```

</details>
<details><summary><b>[S3.3] ステータスの見た目と MVP の確認</b>(Size S・依存 S3.2・確認 不要)</summary>

```markdown
## 目的
ステータスの見た目と MVP の確認(親: E3 ステータス管理の強化)

## やること
- [ ] T3.3.1 色分け(`app.css`)、`codespaces-check.md` に MVP の確認項目、公開 PR

## 完了の条件
- 色分け済み。オーナーが Codespaces で MVP を確認
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S3.2

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S3.3
- docs/ROADMAP_TICKET_SYSTEM.md の Phase3
```

</details>

### [E4] コメント(Phase4・Toggle: `comments`)

<details><summary><b>[S4.1] `ticket_comments` と Service</b>(Size M・依存 S1.2・確認 ✅ DB(Q2))</summary>

```markdown
## 目的
`ticket_comments` と Service(親: E4 コメント)

## やること
- [ ] T4.1.1 テーブル、`Comment`、`CommentService`(本文必須・2000 文字、論理削除)

## 完了の条件
- コメントの保存・論理削除のテストが通る
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S1.2

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S4.1
- docs/ROADMAP_TICKET_SYSTEM.md の Phase4
```

</details>
<details><summary><b>[S4.2] 詳細画面のコメント欄</b>(Size M・依存 S4.1・確認 不要)</summary>

```markdown
## 目的
詳細画面のコメント欄(親: E4 コメント)

## やること
- [ ] T4.2.1 一覧(古い順・投稿者・日時・編集済み)、投稿フォーム、リンク表示(`@links`)

## 完了の条件
- 詳細画面でコメントの一覧と投稿ができ、URL はリンク、そのほかはエスケープ
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S4.1

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S4.2
- docs/ROADMAP_TICKET_SYSTEM.md の Phase4
```

</details>
<details><summary><b>[S4.3] 自分のコメントの編集・削除</b>(Size M・依存 S4.2・確認 不要)</summary>

```markdown
## 目的
自分のコメントの編集・削除(親: E4 コメント)

## やること
- [ ] T4.3.1 他人のコメントは 404(ADMIN は可)、テスト一式、Toggle OFF で欄が出ないテスト

## 完了の条件
- 他人のコメントは 404(ADMIN は可)。Toggle OFF で欄が出ない
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S4.2

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S4.3
- docs/ROADMAP_TICKET_SYSTEM.md の Phase4
```

</details>

### [E5] 検索と API(Phase5・Toggle: `ticket-system`)

<details><summary><b>[S5.1] 検索条件と Service</b>(Size M・依存 S2.4・確認 不要)</summary>

```markdown
## 目的
検索条件と Service(親: E5 検索と API)

## やること
- [ ] T5.1.1 担当者・ステータス(複数)・プロジェクト・キーワード・期限切れのみ。条件は URL に(`ProgressFilter` の方式)

## 完了の条件
- 条件の組み合わせのテストが通る。条件は URL に残る
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S2.4

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S5.1
- docs/ROADMAP_TICKET_SYSTEM.md の Phase5
```

</details>
<details><summary><b>[S5.2] ページ分けと索引</b>(Size M・依存 S5.1・確認 ✅ DB(Q2))</summary>

```markdown
## 目的
ページ分けと索引(親: E5 検索と API)

## やること
- [ ] T5.2.1 1 ページ 50 件、索引(status・assignee_id・project_id・due_date)、1,000 件での速さの確認

## 完了の条件
- 1,000 件のデータで一覧が 1 画面数回のクエリ
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S5.1

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S5.2
- docs/ROADMAP_TICKET_SYSTEM.md の Phase5
```

</details>
<details><summary><b>[S5.3] 検索画面</b>(Size M・依存 S5.1・確認 不要)</summary>

```markdown
## 目的
検索画面(親: E5 検索と API)

## やること
- [ ] T5.3.1 一覧の上に検索フォーム、件数表示、条件のクリア

## 完了の条件
- 検索フォーム・件数・クリアが使える
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S5.1

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S5.3
- docs/ROADMAP_TICKET_SYSTEM.md の Phase5
```

</details>
<details><summary><b>[S5.4] チケット API(DTO)</b>(Size M・依存 S5.1・確認 不要)</summary>

```markdown
## 目的
チケット API(DTO)(親: E5 検索と API)

## やること
- [ ] T5.4.1 `/api/tickets`・`/api/projects`(入出力は record の DTO)、`/api/tasks` は今のまま

## 完了の条件
- `/api/tickets` が DTO で入出力。`/api/tasks` は変わらない
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S5.1

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S5.4
- docs/ROADMAP_TICKET_SYSTEM.md の Phase5
```

</details>

### [E6] 通知(Phase6・Toggle: `notifications`)

<details><summary><b>[S6.1] `notifications` テーブルと Service</b>(Size M・依存 S1.2・確認 ✅ DB(Q2))</summary>

```markdown
## 目的
`notifications` テーブルと Service(親: E6 通知)

## やること
- [ ] T6.1.1 テーブル、`NotificationService`(作成・未読数・既読)

## 完了の条件
- 通知の作成・未読数・既読のテストが通る
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S1.2

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S6.1
- docs/ROADMAP_TICKET_SYSTEM.md の Phase6
```

</details>
<details><summary><b>[S6.2] 出来事から通知を作る</b>(Size M・依存 S6.1・S4.1・確認 不要)</summary>

```markdown
## 目的
出来事から通知を作る(親: E6 通知)

## やること
- [ ] T6.2.1 担当者になった・担当のチケットにコメント(Spring のイベントで「出来事」と「送り方」を分ける)

## 完了の条件
- 担当者になった・コメントが付いたときに通知ができる。Toggle OFF なら作られない
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S6.1・S4.1

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S6.2
- docs/ROADMAP_TICKET_SYSTEM.md の Phase6
```

</details>
<details><summary><b>[S6.3] 期限前日の通知</b>(Size M・依存 S6.1・確認 不要)</summary>

```markdown
## 目的
期限前日の通知(親: E6 通知)

## やること
- [ ] T6.3.1 `@Scheduled` で毎日 1 回(時刻は `Clock`)、同じ通知を二重に作らない

## 完了の条件
- 期限前日の通知が 1 日 1 回、二重にならない(時刻は `Clock`)
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S6.1

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S6.3
- docs/ROADMAP_TICKET_SYSTEM.md の Phase6
```

</details>
<details><summary><b>[S6.4] 通知の画面</b>(Size M・依存 S6.1・確認 不要)</summary>

```markdown
## 目的
通知の画面(親: E6 通知)

## やること
- [ ] T6.4.1 メニューの未読数、一覧、既読、自分の通知以外は 404

## 完了の条件
- 未読数・一覧・既読が使える。他人の通知は 404
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S6.1

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S6.4
- docs/ROADMAP_TICKET_SYSTEM.md の Phase6
```

</details>

### [E7] 権限管理(4 ロール)(Phase7・Toggle: `extended-roles`)

<details><summary><b>[S7.1] MANAGER・VIEWER のログイン規則(パスワード)</b>(Size M・依存 S0.3・確認 ✅ Q5 で了承)</summary>

```markdown
## 目的
MANAGER・VIEWER のログイン規則(パスワード)(親: E7 権限管理(4 ロール))

## やること
- [ ] T7.1.1 変更前に、今のログインの動き(ADMIN はパスワード、MEMBER は専用URL)を押さえるテストを足す
- [ ] T7.1.2 「専用URLは MEMBER だけ、ほかはパスワード」を MANAGER・VIEWER に広げる(`AppUserDetailsService`・`AccessLinkService`)。`spec.md` の INV-11 を更新

## 完了の条件
- 今のログインの動きのテストが通ったまま、MANAGER・VIEWER がパスワードでログインできる
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S0.3

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S7.1
- docs/ROADMAP_TICKET_SYSTEM.md の Phase7
```

</details>
<details><summary><b>[S7.2] MANAGER・VIEWER の追加</b>(Size M・依存 S7.1・確認 ✅ DB(Q2)・認証(Q5))</summary>

```markdown
## 目的
MANAGER・VIEWER の追加(親: E7 権限管理(4 ロール))

## やること
- [ ] T7.2.1 `users.role` の CHECK を広げる、`Role` に追加、ユーザー画面の選択肢(Toggle ON のときだけ)

## 完了の条件
- Toggle ON で MANAGER・VIEWER を選べる。OFF なら ADMIN / MEMBER だけ
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S7.1

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S7.2
- docs/ROADMAP_TICKET_SYSTEM.md の Phase7
```

</details>
<details><summary><b>[S7.3] プロジェクトのメンバー</b>(Size M・依存 S2.1・確認 ✅ DB(Q2))</summary>

```markdown
## 目的
プロジェクトのメンバー(親: E7 権限管理(4 ロール))

## やること
- [ ] T7.3.1 `project_members`、プロジェクト画面でメンバーの追加・削除

## 完了の条件
- プロジェクトにメンバーを追加・削除できる
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S2.1

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S7.3
- docs/ROADMAP_TICKET_SYSTEM.md の Phase7
```

</details>
<details><summary><b>[S7.4] 権限表をクエリ条件で実装</b>(Size L・依存 S7.2・S7.3・確認 ✅ 認可(Q5))</summary>

```markdown
## 目的
権限表をクエリ条件で実装(親: E7 権限管理(4 ロール))

## やること
- [ ] T7.4.1 見えるチケット(参加プロジェクト)、更新・削除・コメントの可否(PRD 2.2)
- [ ] T7.4.2 ロール × 画面の表でテスト(4 ロール × 主要操作)、Toggle OFF 時は弱いほうに倒すテスト

## 完了の条件
- 4 ロール × 主要操作のテスト表がすべて通る。Toggle OFF 時は弱い権限に倒れる
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S7.2・S7.3

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S7.4
- docs/ROADMAP_TICKET_SYSTEM.md の Phase7
```

</details>
<details><summary><b>[S7.5] 新入社員をオンボーディングのプロジェクトのメンバーにする</b>(Size S・依存 S7.3・S2.5・確認 不要)</summary>

```markdown
## 目的
新入社員をオンボーディングのプロジェクトのメンバーにする(親: E7 権限管理(4 ロール))

## やること
- [ ] T7.5.1 一括起票のときに自動でメンバーに追加

## 完了の条件
- 一括起票した新入社員が自動でメンバーになる
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S7.3・S2.5

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S7.5
- docs/ROADMAP_TICKET_SYSTEM.md の Phase7
```

</details>

### [E8] ダッシュボード(Phase8・Toggle: `ticket-dashboard`)

<details><summary><b>[S8.1] チケットの集計</b>(Size M・依存 S5.1・確認 不要)</summary>

```markdown
## 目的
チケットの集計(親: E8 ダッシュボード)

## やること
- [ ] T8.1.1 ステータス別・期限切れ・担当者別を 1 クエリで(`DashboardService` の方式)

## 完了の条件
- 集計が 1 クエリでロールの範囲どおり
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S5.1

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S8.1
- docs/ROADMAP_TICKET_SYSTEM.md の Phase8
```

</details>
<details><summary><b>[S8.2] ダッシュボード画面</b>(Size M・依存 S8.1・確認 不要)</summary>

```markdown
## 目的
ダッシュボード画面(親: E8 ダッシュボード)

## やること
- [ ] T8.2.1 トップページにチケットの欄を追加(Toggle ON)、範囲はロールで決める

## 完了の条件
- Toggle ON でトップページにチケットの欄。OFF なら今のまま
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S8.1

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S8.2
- docs/ROADMAP_TICKET_SYSTEM.md の Phase8
```

</details>
<details><summary><b>[S8.3] マイページに自分のチケット</b>(Size S・依存 S8.1・確認 不要)</summary>

```markdown
## 目的
マイページに自分のチケット(親: E8 ダッシュボード)

## やること
- [ ] T8.3.1 `/me` に「自分の担当チケット」を追加(旧オンボーディングの表はそのまま)

## 完了の条件
- `/me` に自分の担当チケット。旧オンボーディングの表はそのまま
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S8.1

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S8.3
- docs/ROADMAP_TICKET_SYSTEM.md の Phase8
```

</details>

### [E9] カンバンボード(Phase9・Toggle: `kanban-board`)

<details><summary><b>[S9.1] ボード画面(ボタン版)</b>(Size M・依存 S3.2・S2.4・確認 不要)</summary>

```markdown
## 目的
ボード画面(ボタン版)(親: E9 カンバンボード)

## やること
- [ ] T9.1.1 プロジェクトごとにステータスの列、カードに番号・タイトル・担当者・期限

## 完了の条件
- プロジェクトのボードにステータスの列とカード
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S3.2・S2.4

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S9.1
- docs/ROADMAP_TICKET_SYSTEM.md の Phase9
```

</details>
<details><summary><b>[S9.2] カードの移動</b>(Size M・依存 S9.1・確認 不要)</summary>

```markdown
## 目的
カードの移動(親: E9 カンバンボード)

## やること
- [ ] T9.2.1 「→」ボタンで隣の列へ(状態の移り方の決まりに従う)、権限チェック

## 完了の条件
- ボタンで隣の列へ動かせる。移れない組み合わせ・権限のないカードは動かない
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S9.1

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S9.2
- docs/ROADMAP_TICKET_SYSTEM.md の Phase9
```

</details>
<details><summary><b>[S9.3] (任意)ドラッグ操作</b>(Size M・依存 S9.2・確認 ✅ JavaScript(Q7))</summary>

```markdown
## 目的
(任意)ドラッグ操作(親: E9 カンバンボード)

## やること
- [ ] T9.3.1 少量の JavaScript でドラッグ。JS が動かなくてもボタンで使える

## 完了の条件
- ドラッグで動かせる。JavaScript なしでもボタンで使える
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S9.2

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S9.3
- docs/ROADMAP_TICKET_SYSTEM.md の Phase9
```

</details>

### [E10] 全面移行と Toggle の撤去(Phase10・Toggle: `onboarding-legacy`)

<details><summary><b>[S10.1] オンボーディングのデータをチケットへ写す</b>(Size L・依存 Phase9 まで・確認 ✅ 方針(Q6)・実行日はいつでもよい)</summary>

```markdown
## 目的
オンボーディングのデータをチケットへ写す(親: E10 全面移行と Toggle の撤去)

## やること
- [ ] T10.1.1 手順書(バックアップ・実行・件数の突き合わせ・戻し方)を書く。実運用前なので実行日はいつでもよい(オーナー了承)
- [ ] T10.1.2 マイグレーション 1 本: `task_assignments` → `tickets`(プロジェクト `ONB`、`origin_assignment_id` で二重防止)、チェックも写す
- [ ] T10.1.3 本番に近いデータでのリハーサル(Codespaces)

## 完了の条件
- リハーサルで件数が一致。本番実行後も一致し、二重に写っていない
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
Phase9 まで

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S10.1
- docs/ROADMAP_TICKET_SYSTEM.md の Phase10
```

</details>
<details><summary><b>[S10.2] 旧画面の退役(`onboarding-legacy` を OFF)</b>(Size M・依存 S10.1・確認 ✅ 方針(Q6))</summary>

```markdown
## 目的
旧画面の退役(`onboarding-legacy` を OFF)(親: E10 全面移行と Toggle の撤去)

## やること
- [ ] T10.2.1 旧 URL から新画面への案内、OFF にして 2 週間の確認

## 完了の条件
- 旧 URL から新画面へ案内。OFF にして 2 週間問題なし
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S10.1

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S10.2
- docs/ROADMAP_TICKET_SYSTEM.md の Phase10
```

</details>
<details><summary><b>[S10.3] Toggle の撤去(1 Toggle = 1 PR)</b>(Size M × 7・依存 各 Toggle が既定 ON で 2 週間安定・確認 不要)</summary>

```markdown
## 目的
Toggle の撤去(1 Toggle = 1 PR)(親: E10 全面移行と Toggle の撤去)

## やること
- [ ] T10.3.1 `ticket-system` / `project-management` / `comments` / `notifications` / `extended-roles` / `ticket-dashboard` / `kanban-board` の分岐・設定・環境変数・OFF 側のテストを消す

## 完了の条件
- Toggle 1 つにつき 1 PR で、分岐・設定・環境変数・OFF 側のテストが消えている
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
各 Toggle が既定 ON で 2 週間安定

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S10.3
- docs/ROADMAP_TICKET_SYSTEM.md の Phase10
```

</details>
<details><summary><b>[S10.4] 旧コードの削除</b>(Size L・依存 S10.2・確認 ✅ 移行後、オーナーの指示で削除)</summary>

```markdown
## 目的
旧コードの削除(親: E10 全面移行と Toggle の撤去)

## やること
- [ ] T10.4.1 旧 Controller・画面・テスト、`onboarding-legacy` を削除。古いテーブルも含め、移行後に問題がないことを確かめてから、オーナーの指示で着手する(バックアップ後)

## 完了の条件
- 旧 Controller・画面・テストが消え、全テストが通る。古いテーブルの扱いが記録されている
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S10.2

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S10.4
- docs/ROADMAP_TICKET_SYSTEM.md の Phase10
```

</details>
<details><summary><b>[S10.5] ドキュメントの総仕上げ</b>(Size M・依存 S10.4・確認 不要)</summary>

```markdown
## 目的
ドキュメントの総仕上げ(親: E10 全面移行と Toggle の撤去)

## やること
- [ ] T10.5.1 `requirements.md`・`design.md`・`user-guide.md`・`current-status.md`・`docs/ai/*` をチケット管理の内容に更新

## 完了の条件
- 人向け・AI 向けのドキュメントがチケット管理の内容になっている
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存
S10.4

## 参照
- docs/TASKS_TICKET_SYSTEM.md の S10.5
- docs/ROADMAP_TICKET_SYSTEM.md の Phase10
```

</details>

---

## 6. 最初の 2 週間(Phase0)のボードの例

| Backlog | Ready | In Progress | Review | Done |
|---|---|---|---|---|
| S0.4(S0.3 待ち)<br>S1.1〜S1.8(S0.1 待ち)<br>… | S0.1<br>S0.2<br>S0.3<br>S0.5<br>S0.6 | (着手したら 1〜2 件) | (PR を作ったら) | |

着手の順番は `TASKS_TICKET_SYSTEM.md` の「最初に着手すべき Issue 上位 10 件」に従います(S0.1 → S0.2 → S0.3 → S0.6 → …)。

