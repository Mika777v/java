# TASKS — チケット管理システムへの移行(Epic / Story / Task)

- 版: 0.2(2026-10-20、オーナーの回答を反映。GitHub Issues の形は [GITHUB_ISSUES_TICKET_SYSTEM.md](GITHUB_ISSUES_TICKET_SYSTEM.md))
- 関連: [ROADMAP_TICKET_SYSTEM.md](ROADMAP_TICKET_SYSTEM.md) / [PRD_TICKET_SYSTEM.md](PRD_TICKET_SYSTEM.md) /
  [FEATURE_TOGGLE_STRATEGY.md](FEATURE_TOGGLE_STRATEGY.md)
- 進行中のタスクは今までどおり [TASKS.md](TASKS.md) で管理し、このファイルは「移行計画の Issue の一覧」として使う
- GitHub Issue にするときは、Story を 1 Issue(= 原則 1 PR)、Task を Issue の中のチェックリストにする

## 記号

| 列 | 意味 |
|---|---|
| ID | `E`=Epic、`S`=Story(GitHub Issue 1 件)、`T`=Task(Issue の中のチェック項目) |
| 規模 | S = 半日〜1 日、M = 2〜3 日、L = 4〜5 日(L は分けられないか見直す) |
| 確認 | ⚠ = 運用ルールの「事前確認が必要な作業」で、まだ了承がないもの。✅ = 了承済み(括弧内は PRD 10 章の質問番号) |
| Toggle | その Story の機能を守る Feature Toggle |

---

## Epic 一覧

| Epic | 名前 | Phase | Toggle | Story 数 |
|---|---|---|---|---|
| E0 | 移行の土台(Feature Toggle・Service 層の整理) | 0 | (仕組み) | 6 |
| E1 | チケットの基本(Task → Ticket の一般化) | 1 | `ticket-system` | 8 |
| E2 | プロジェクト | 2 | `project-management` | 6 |
| E3 | ステータス管理の強化 | 3 | `ticket-system` | 3 |
| E4 | コメント | 4 | `comments` | 3 |
| E5 | 検索と API | 5 | `ticket-system` | 4 |
| E6 | 通知 | 6 | `notifications` | 4 |
| E7 | 権限管理(4 ロール) | 7 | `extended-roles` | 5 |
| E8 | ダッシュボード | 8 | `ticket-dashboard` | 3 |
| E9 | カンバンボード | 9 | `kanban-board` | 3 |
| E10 | 全面移行と Toggle の撤去 | 10 | `onboarding-legacy` | 5 |

---

## E0 移行の土台(Phase0)

| ID | Story / Task | 規模 | 確認 | 依存 |
|---|---|---|---|---|
| **S0.1** | **Feature Toggle の仕組みを入れる** | M | | — |
| T0.1.1 | `Feature` enum(名前・親・既定値)を作る。設定 `features.*.enabled` は `Environment` から読む(`FeatureProperties` は作らないことにした) | | | |
| T0.1.2 | `FeatureFlags`(`@Component("features")`、親が OFF なら子も OFF)と単体テスト | | | |
| T0.1.3 | `@RequiresFeature` + `FeatureToggleInterceptor`(OFF なら 404)を `WebConfig` に登録、テスト | | | |
| T0.1.4 | `application.yml`・`docker-compose.yml`・`.devcontainer/start-app.sh`・README に `FEATURE_*` を追加(すべて既定 OFF) | | | |
| T0.1.5 | `docs/ai/architecture.md`・`conventions.md` に Toggle の使い方を追記 | | | |
| **S0.2** | **`TaskService` を導入する(D1 / R1)** | M | | — |
| T0.2.1 | `TaskPageController`・`TaskController` のタスク CRUD を `TaskService` に移す(動きは変えない) | | | |
| T0.2.2 | 既存テストがそのまま通ることを確認、`layer-check.sh` で直接呼び出しが減ったことを確認 | | | |
| **S0.3** | **`UserService` を導入する(D2・D6 / R2・R7)** | L | | — |
| T0.3.1 | ユーザーの登録・更新(メール重複・パスワード・専用URLの削除)を `UserService` に | | | |
| T0.3.2 | 削除(自分自身は不可)と、`MyPageController` のパスワード変更を `UserService` に | | | |
| T0.3.3 | 既存テスト(`UserPageControllerTest`・`PasswordResetTest` ほか)がそのまま通ることを確認 | | | |
| **S0.4** | **`UserPageController` を分割する(D3 / R3)** | M | | S0.3 |
| T0.4.1 | 割り当ての操作を `UserAssignmentController` に、専用URL・リセットを `UserAccessController` に(URL は変えない) | | | |
| **S0.5** | **画面の表を部品にする(D4 / R4)** | S | | — |
| T0.5.1 | マイページとユーザー詳細の割り当ての表を `fragments/assignments.html` に | | | |
| **S0.6** | **`open-in-view` の方針を決めて、新しい画面のルールにする(D8)** | S | | — |
| T0.6.1 | 新しいチケット画面は Service で必要な関連を読み込む(`@EntityGraph`)、をルールとして `conventions.md` に。全体の OFF は Phase10 の後に検討 | | | |

## E1 チケットの基本(Phase1・`ticket-system`)

| ID | Story / Task | 規模 | 確認 | 依存 |
|---|---|---|---|---|
| **S1.1** | **`tickets` テーブルと Entity を作る** | M | ✅ DB(Q2) | S0.1 |
| T1.1.1 | V12: `tickets`(id, number, title, description, status, assignee_id, reporter_id, due_date, completed_at, origin_task_id, origin_assignment_id, deleted_at, created_at, updated_at)、索引 | | | |
| T1.1.2 | `Ticket`・`TicketStatus`(TODO / IN_PROGRESS / DONE、`getLabel()`)、`changeStatus`・`isOverdue`(今の規則を写す) | | | |
| T1.1.3 | `AssignmentStatus` → `TicketStatus` の対応表(1 か所)と単体テスト | | | |
| T1.1.4 | `docs/ai/database.md`・`design-diagrams.md`(ER)を更新 | | | |
| **S1.2** | **`TicketService` と `TicketRepository`** | M | | S1.1 |
| T1.2.1 | 登録・更新・論理削除・ステータス変更・取得(削除済みは出さない)、`Clock` を使う | | | |
| T1.2.2 | 一覧(ステータス・担当者で絞り込み、更新日時の新しい順、関連は `@EntityGraph`) | | | |
| T1.2.3 | 単体・結合テスト(完了日時の記録・消去、期限切れ) | | | |
| **S1.3** | **チケット一覧・登録・編集の画面** | L | | S1.2 |
| T1.3.1 | `TicketController`(`@RequiresFeature(TICKET_SYSTEM)`)と `TicketForm`(タイトル必須・200 文字) | | | |
| T1.3.2 | `templates/tickets/list.html`・`form.html`(表は部品で作る) | | | |
| T1.3.3 | テスト: 正常系・入力エラー・404・新入社員は 403・Toggle OFF で 404 | | | |
| **S1.4** | **チケット詳細・削除・ステータス変更** | M | ✅ 論理削除(Q3) | S1.3 |
| T1.4.1 | 詳細画面(説明はリンク表示)、削除(論理削除、確認ダイアログ)、ステータス変更 | | | |
| T1.4.2 | テスト一式 | | | |
| **S1.5** | **メニューと Toggle の画面表示** | S | | S1.3 |
| T1.5.1 | `fragments/nav.html` に「チケット」(`${@features.on('ticket-system')}` のときだけ) | | | |
| T1.5.2 | Toggle OFF で今の画面と HTML が変わらないことのテスト | | | |
| **S1.6** | **再起動なしの ON/OFF(`feature_flags` と `/admin/features`)** | M | ✅ DB(Q2・Q4) | S0.1 |
| T1.6.1 | `feature_flags`(name, enabled, updated_by, updated_at)と、DB → 設定の順で読む処理 | | | |
| T1.6.2 | 管理画面(ADMIN のみ。親子関係の表示、切り替えの確認ダイアログ)、テスト | | | |
| **S1.7** | **CI のスモークテストに Toggle ON の確認を足す** | S | | S1.3 |
| T1.7.1 | `reusable-docker-smoke.yml` で `FEATURE_TICKET_SYSTEM=true` にして、チケットを 1 件登録・表示(今の確認は残す) | | | |
| **S1.8** | **ドキュメントと Codespaces の確認手順** | S | | S1.4 |
| T1.8.1 | `spec.md`(TK-01〜05・TK-60・61)、`requirements.md`、`user-guide.md`(β の章)、`codespaces-check.md`(Toggle を ON にする手順と確認 3〜5 項目) | | | |
| T1.8.2 | 公開 PR(既定を ON にする)は確認後に別で作る。撤去 Issue(S10.3)をこの時点で登録 | | | |

## E2 プロジェクト(Phase2・`project-management`)

| ID | Story / Task | 規模 | 確認 | 依存 |
|---|---|---|---|---|
| **S2.1** | **`projects` テーブルと既定のプロジェクト `GEN`** | M | ✅ DB(Q2) | S1.1 |
| T2.1.1 | V13: `projects`(key UK, name, description, next_number, archived)、`GEN` を作成、`tickets.project_id`(既存は `GEN`)、(project_id, number) の一意制約 | | | |
| T2.1.2 | `Project`・`ProjectRepository`・`ProjectService` | | | |
| **S2.2** | **チケット番号 `KEY-n` の採番** | M | | S2.1 |
| T2.2.1 | `next_number` を行ロックで進める。同時に起票しても重ならないテスト | | | |
| T2.2.2 | チケットの URL を番号(`/tickets/ONB-12`)に。古い id の URL は番号へリダイレクト | | | |
| **S2.3** | **プロジェクトの画面** | M | | S2.1 |
| T2.3.1 | 一覧・登録(キーは英大文字 2〜10 文字・一意)・編集・アーカイブ、テスト | | | |
| **S2.4** | **チケットにプロジェクトを付ける** | M | | S2.2 |
| T2.4.1 | 登録画面でプロジェクトを選ぶ(アーカイブ済みは出さない)、一覧をプロジェクトで分ける。Toggle OFF なら `GEN` 固定 | | | |
| **S2.5** | **テンプレートから一括起票** | L | | S2.4・S0.2 |
| T2.5.1 | `OnboardingTemplateService`: 有効なひな形 → 選んだ人を担当者にしてチケット作成(期限一括、`origin_task_id` を記録) | | | |
| T2.5.2 | ひな形のサブ項目をチェックリストとして写す(チケットのチェックリストを作る場合。作らない場合は説明に箇条書きで写す) | | | |
| T2.5.3 | 画面(プロジェクト詳細から「新入社員の受け入れを起票」)、二度押しで二重に作らないテスト | | | |
| **S2.6** | **表示名「タスク」→「オンボーディングのひな形」** | S | | S2.5 |
| T2.6.1 | 旧画面のメニュー・見出しの表示名だけ変える(URL・クラス名は変えない)。テストの文言を更新 | | | |

## E3 ステータス管理の強化(Phase3)

| ID | Story / Task | 規模 | 確認 | 依存 |
|---|---|---|---|---|
| **S3.1** | **REVIEW・CLOSED を追加** | M | ✅ DB(Q2) | S1.2 |
| T3.1.1 | 新しいマイグレーションで `tickets.status` の CHECK を広げる、`TicketStatus` に追加 | | | |
| T3.1.2 | 完了日時: DONE で記録、CLOSED は記録を保つ、それ以外に戻すと消す。期限切れは DONE・CLOSED を除く | | | |
| **S3.2** | **状態の移り方の決まり** | M | ✅ 仕様(オーナー了承) | S3.1 |
| T3.2.1 | 移れる組み合わせの表を `TicketStatus` に持たせ、テスト。決まり: TODO・IN_PROGRESS・REVIEW・DONE の間は自由に移れる。どの状態からも CLOSED にできる。CLOSED から出るのは「再オープン」(→ TODO)だけ | | | |
| T3.2.2 | `design-diagrams.md` に状態遷移図を追加 | | | |
| **S3.3** | **ステータスの見た目と MVP の確認** | S | | S3.2 |
| T3.3.1 | 色分け(`app.css`)、`codespaces-check.md` に MVP の確認項目、公開 PR | | | |

## E4 コメント(Phase4・`comments`)

| ID | Story / Task | 規模 | 確認 | 依存 |
|---|---|---|---|---|
| **S4.1** | **`ticket_comments` と Service** | M | ✅ DB(Q2) | S1.2 |
| T4.1.1 | テーブル、`Comment`、`CommentService`(本文必須・2000 文字、論理削除) | | | |
| **S4.2** | **詳細画面のコメント欄** | M | | S4.1 |
| T4.2.1 | 一覧(古い順・投稿者・日時・編集済み)、投稿フォーム、リンク表示(`@links`) | | | |
| **S4.3** | **自分のコメントの編集・削除** | M | | S4.2 |
| T4.3.1 | 他人のコメントは 404(ADMIN は可)、テスト一式、Toggle OFF で欄が出ないテスト | | | |

## E5 検索と API(Phase5)

| ID | Story / Task | 規模 | 確認 | 依存 |
|---|---|---|---|---|
| **S5.1** | **検索条件と Service** | M | | S2.4 |
| T5.1.1 | 担当者・ステータス(複数)・プロジェクト・キーワード・期限切れのみ。条件は URL に(`ProgressFilter` の方式) | | | |
| **S5.2** | **ページ分けと索引** | M | ✅ DB(Q2) | S5.1 |
| T5.2.1 | 1 ページ 50 件、索引(status・assignee_id・project_id・due_date)、1,000 件での速さの確認 | | | |
| **S5.3** | **検索画面** | M | | S5.1 |
| T5.3.1 | 一覧の上に検索フォーム、件数表示、条件のクリア | | | |
| **S5.4** | **チケット API(DTO)** | M | | S5.1 |
| T5.4.1 | `/api/tickets`・`/api/projects`(入出力は record の DTO)、`/api/tasks` は今のまま | | | |

## E6 通知(Phase6・`notifications`)

| ID | Story / Task | 規模 | 確認 | 依存 |
|---|---|---|---|---|
| **S6.1** | **`notifications` テーブルと Service** | M | ✅ DB(Q2) | S1.2 |
| T6.1.1 | テーブル、`NotificationService`(作成・未読数・既読) | | | |
| **S6.2** | **出来事から通知を作る** | M | | S6.1・S4.1 |
| T6.2.1 | 担当者になった・担当のチケットにコメント(Spring のイベントで「出来事」と「送り方」を分ける) | | | |
| **S6.3** | **期限前日の通知** | M | | S6.1 |
| T6.3.1 | `@Scheduled` で毎日 1 回(時刻は `Clock`)、同じ通知を二重に作らない | | | |
| **S6.4** | **通知の画面** | M | | S6.1 |
| T6.4.1 | メニューの未読数、一覧、既読、自分の通知以外は 404 | | | |

## E7 権限管理(Phase7・`extended-roles`)

| ID | Story / Task | 規模 | 確認 | 依存 |
|---|---|---|---|---|
| **S7.1** | **MANAGER・VIEWER のログイン規則(パスワード)** | M | ✅ Q5 で了承 | S0.3 |
| T7.1.1 | 変更前に、今のログインの動き(ADMIN はパスワード、MEMBER は専用URL)を押さえるテストを足す | | | |
| T7.1.2 | 「専用URLは MEMBER だけ、ほかはパスワード」を MANAGER・VIEWER に広げる(`AppUserDetailsService`・`AccessLinkService`)。`spec.md` の INV-11 を更新 | | | |
| **S7.2** | **MANAGER・VIEWER の追加** | M | ✅ DB(Q2)・認証(Q5) | S7.1 |
| T7.2.1 | `users.role` の CHECK を広げる、`Role` に追加、ユーザー画面の選択肢(Toggle ON のときだけ) | | | |
| **S7.3** | **プロジェクトのメンバー** | M | ✅ DB(Q2) | S2.1 |
| T7.3.1 | `project_members`、プロジェクト画面でメンバーの追加・削除 | | | |
| **S7.4** | **権限表をクエリ条件で実装** | L | ✅ 認可(Q5) | S7.2・S7.3 |
| T7.4.1 | 見えるチケット(参加プロジェクト)、更新・削除・コメントの可否(PRD 2.2) | | | |
| T7.4.2 | ロール × 画面の表でテスト(4 ロール × 主要操作)、Toggle OFF 時は弱いほうに倒すテスト | | | |
| **S7.5** | **新入社員をオンボーディングのプロジェクトのメンバーにする** | S | | S7.3・S2.5 |
| T7.5.1 | 一括起票のときに自動でメンバーに追加 | | | |

## E8 ダッシュボード(Phase8・`ticket-dashboard`)

| ID | Story / Task | 規模 | 確認 | 依存 |
|---|---|---|---|---|
| **S8.1** | **チケットの集計** | M | | S5.1 |
| T8.1.1 | ステータス別・期限切れ・担当者別を 1 クエリで(`DashboardService` の方式) | | | |
| **S8.2** | **ダッシュボード画面** | M | | S8.1 |
| T8.2.1 | トップページにチケットの欄を追加(Toggle ON)、範囲はロールで決める | | | |
| **S8.3** | **マイページに自分のチケット** | S | | S8.1 |
| T8.3.1 | `/me` に「自分の担当チケット」を追加(旧オンボーディングの表はそのまま) | | | |

## E9 カンバンボード(Phase9・`kanban-board`)

| ID | Story / Task | 規模 | 確認 | 依存 |
|---|---|---|---|---|
| **S9.1** | **ボード画面(ボタン版)** | M | | S3.2・S2.4 |
| T9.1.1 | プロジェクトごとにステータスの列、カードに番号・タイトル・担当者・期限 | | | |
| **S9.2** | **カードの移動** | M | | S9.1 |
| T9.2.1 | 「→」ボタンで隣の列へ(状態の移り方の決まりに従う)、権限チェック | | | |
| **S9.3** | **(任意)ドラッグ操作** | M | ✅ JavaScript(Q7) | S9.2 |
| T9.3.1 | 少量の JavaScript でドラッグ。JS が動かなくてもボタンで使える | | | |

## E10 全面移行と Toggle の撤去(Phase10)

| ID | Story / Task | 規模 | 確認 | 依存 |
|---|---|---|---|---|
| **S10.1** | **オンボーディングのデータをチケットへ写す** | L | ✅ 方針(Q6)・実行日はいつでもよい(まだ実運用していない) | Phase9 まで |
| T10.1.1 | 手順書(バックアップ・実行・件数の突き合わせ・戻し方)を書く。実運用前なので実行日はいつでもよい(オーナー了承) | | | |
| T10.1.2 | マイグレーション 1 本: `task_assignments` → `tickets`(プロジェクト `ONB`、`origin_assignment_id` で二重防止)、チェックも写す | | | |
| T10.1.3 | 本番に近いデータでのリハーサル(Codespaces) | | | |
| **S10.2** | **旧画面の退役(`onboarding-legacy` を OFF)** | M | ✅ 方針(Q6) | S10.1 |
| T10.2.1 | 旧 URL から新画面への案内、OFF にして 2 週間の確認 | | | |
| **S10.3** | **Toggle の撤去(1 Toggle = 1 PR)** | M × 7 | | 各 Toggle が既定 ON で 2 週間安定 |
| T10.3.1 | `ticket-system` / `project-management` / `comments` / `notifications` / `extended-roles` / `ticket-dashboard` / `kanban-board` の分岐・設定・環境変数・OFF 側のテストを消す | | | |
| **S10.4** | **旧コードの削除** | L | ✅ 移行して問題がなければ、オーナーの指示で削除 | S10.2 |
| T10.4.1 | 旧 Controller・画面・テスト、`onboarding-legacy` を削除。古いテーブルも含め、移行後に問題がないことを確かめてから、オーナーの指示で着手する(バックアップ後) | | | |
| **S10.5** | **ドキュメントの総仕上げ** | M | | S10.4 |
| T10.5.1 | `requirements.md`・`design.md`・`user-guide.md`・`current-status.md`・`docs/ai/*` をチケット管理の内容に更新 | | | |

---

## 最初に着手すべき Issue 上位 10 件

選び方: ① 今の機能に影響がない(見た目が変わらない)② 後のフェーズの前提になる ③ 小さく終わる、の順。

| 順 | ID | Issue | 規模 | 確認 | 理由 |
|---|---|---|---|---|---|
| 1 | S0.1 | Feature Toggle の仕組みを入れる | M | 不要 | すべての新機能の前提。既定 OFF なので今の動きは変わらない |
| 2 | S0.2 | `TaskService` を導入する | M | 不要 | テンプレートからの起票(Phase2)で使う。技術的負債 D1 の解消 |
| 3 | S0.3 | `UserService` を導入する | L | 不要 | 担当者・起票者・権限(Phase7)で何度も使う。D2・D6 の解消 |
| 4 | S0.6 | `open-in-view` の方針を新画面のルールにする | S | 不要 | チケット画面を作る前に決めておくと、後から直す必要がない |
| 5 | S1.1 | `tickets` テーブルと Entity | M | ✅(Q2) | チケット管理の中心 |
| 6 | S1.2 | `TicketService` と `TicketRepository` | M | 不要 | 画面より先に規則(完了日時・期限切れ)をテストで固める |
| 7 | S1.3 | チケット一覧・登録・編集の画面 | L | 不要 | ここで初めて Toggle ON で「使える」状態になる |
| 8 | S1.5 | メニューと Toggle の画面表示 | S | 不要 | OFF のとき今と同じ見た目であることを保証する |
| 9 | S1.4 | チケット詳細・削除・ステータス変更 | M | ✅ Q3 | Phase1 の残り。削除は論理削除 |
| 10 | S1.6 | 再起動なしの ON/OFF | M | ✅(Q2・Q4) | 公開前に「すぐ戻せる」状態を作っておく |

S0.4(Controller の分割)・S0.5(表の部品化)・S1.7(CI)・S1.8(ドキュメント)は、上の 10 件の間に並行して進めます。
