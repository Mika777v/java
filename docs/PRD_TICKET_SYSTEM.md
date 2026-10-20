# PRD — チケット管理システム(オンボーディング管理からの発展)

- 版: 0.2(2026-10-20、オーナーの回答を反映)
- 関連: [PRODUCT_VISION.md](PRODUCT_VISION.md) / [ROADMAP_TICKET_SYSTEM.md](ROADMAP_TICKET_SYSTEM.md) /
  [TASKS_TICKET_SYSTEM.md](TASKS_TICKET_SYSTEM.md) / [FEATURE_TOGGLE_STRATEGY.md](FEATURE_TOGGLE_STRATEGY.md)
- 今の仕様: [requirements.md](requirements.md)(FR-xx)/ [ai/spec.md](ai/spec.md)
- 要件 ID は今の `FR-xx` と区別するため **`TK-xx`** とします

---

## 1. 背景と目的

今のオンボーディング進捗管理(タスクのひな形 → 人ごとの割り当て → 状態・期限で追う)を一般化し、
オンボーディング以外の仕事も扱える **チケット管理** にする。作り直しはせず、今のコード・DB・画面を土台にする。
移行の間は今の機能を Feature Toggle で守り、新機能は Toggle の下で段階的に公開する。

## 2. ユーザーロール

### 2.1 ロールの定義

| ロール | 説明 | 今のロールとの関係 |
|---|---|---|
| ADMIN | システム管理者。すべての操作、ユーザー・プロジェクト・Toggle の管理 | 今の `ADMIN` のまま |
| MANAGER | プロジェクトの運営者。担当プロジェクトのチケットの起票・割り振り・削除、メンバーの追加 | 新規(Phase7) |
| MEMBER | 一般メンバー。自分が入っているプロジェクトのチケットを起票・更新・コメント | 今の `MEMBER`(新入社員)を広げる |
| VIEWER | 閲覧者。自分が入っているプロジェクトのチケットを見るだけ | 新規(Phase7) |

### 2.2 権限表(Phase7 で有効。それまでチケット画面は ADMIN だけ)

| 操作 | ADMIN | MANAGER | MEMBER | VIEWER |
|---|---|---|---|---|
| チケットを見る | すべて | 参加プロジェクト | 参加プロジェクト | 参加プロジェクト |
| チケットを登録する | ○ | ○ | ○ | × |
| チケットを更新する(内容・担当者・期限) | ○ | ○ | 自分が担当 or 起票者のもの | × |
| ステータスを変える | ○ | ○ | 自分が担当のもの | × |
| チケットを削除する | ○ | ○(参加プロジェクト) | × | × |
| コメントする | ○ | ○ | ○ | × |
| 自分のコメントを直す・消す | ○ | ○ | ○ | — |
| プロジェクトの作成・アーカイブ | ○ | × | × | × |
| プロジェクトの編集・メンバー追加 | ○ | ○(担当プロジェクト) | × | × |
| ユーザー管理・Feature Toggle | ○ | × | × | × |
| 旧オンボーディング画面(移行中) | 今と同じ | 管理者画面は × | 今と同じ(マイページ) | × |

- 「参加プロジェクト」は Phase7 で追加する `project_members` で決める
- 新入社員(専用URLでログインする人)も MEMBER として、オンボーディング用のプロジェクトに参加させる。他のプロジェクトは見えない

### 2.3 ロールとログイン方法(Q5 の回答: 今のまま。Toggle で切り替え)

今は `MEMBER` = 「専用URLでログインする新入社員」です(`LoginSettings`、INV-11)。オーナーの回答(2026-10-20)により、**ロールとログイン方法は分けず、今の決まりを広げる** 方式にします。

| ロール | ログイン方法(`app.member-login=link` のとき) | 備考 |
|---|---|---|
| ADMIN | パスワード | 今のまま |
| MEMBER | 専用URL | 今のまま(`password` 方式に切り替えればパスワード) |
| MANAGER | パスワード | 新規(Phase7)。「専用URLを使えるのは MEMBER だけ」という今の決まりをそのまま使う |
| VIEWER | パスワード | 新規(Phase7) |

- MANAGER・VIEWER の追加と 4 ロールの権限表は、Toggle `extended-roles` で切り替える。OFF のときは今の 2 ロールの動きのまま
- パスワードでログインする一般メンバーが必要になったら、MEMBER ではなく MANAGER(または `password` 方式)で運用する。分ける設計は将来の候補として残す

## 3. ユースケース

| ID | ユースケース | 主なロール | フェーズ |
|---|---|---|---|
| UC-T01 | チケットを登録する(タイトル・説明・担当者・期限・ステータス・プロジェクト) | ADMIN / MANAGER / MEMBER | P1(P2 でプロジェクト) |
| UC-T02 | チケットを更新する(内容・担当者・期限・ステータス) | 同上 | P1 |
| UC-T03 | チケットを削除する | ADMIN / MANAGER | P1 |
| UC-T04 | チケットにコメントする・直す・消す | ADMIN / MANAGER / MEMBER | P4 |
| UC-T05 | チケットを検索する(担当者・ステータス・キーワード・プロジェクト) | 全員 | P5(P1 は簡単な絞り込み) |
| UC-T06 | プロジェクトを作る・編集する・アーカイブする・メンバーを追加する | ADMIN / MANAGER | P2(メンバーは P7) |
| UC-T07 | オンボーディングのタスクひな形から、新入社員のチケットをまとめて起票する | ADMIN / MANAGER | P2 |
| UC-T08 | 通知を見る(割り当てられた・コメントされた・期限が近い) | 全員 | P6 |
| UC-T09 | ダッシュボードで全体を見る | 全員(範囲はロールによる) | P8 |
| UC-T10 | カンバンボードでステータスを動かす | ADMIN / MANAGER / MEMBER | P9 |
| UC-T11 | 機能の ON/OFF を切り替える | ADMIN | P1 |

## 4. 機能要件

### 4.1 チケット(Phase1・3)

| ID | 要件 |
|---|---|
| TK-01 | チケットはタイトル(必須・200 文字)、説明(任意。URL はリンク表示)、ステータス、担当者(任意。ユーザー 1 人)、起票者(自動)、期限(任意)、完了日時(自動)を持つ |
| TK-02 | チケットは番号を持つ。プロジェクト導入後は `<プロジェクトキー>-<連番>`(例: `ONB-12`)。導入前は既定のプロジェクト `GEN` の番号 |
| TK-03 | ステータスは TODO / IN_PROGRESS / DONE(Phase1)、REVIEW / CLOSED を追加(Phase3)。DONE にすると完了日時を記録し、DONE・CLOSED 以外に戻すと消す(今の割り当てと同じ規則)。移り方: TODO・IN_PROGRESS・REVIEW・DONE の間は自由に移れる。どの状態からも CLOSED にできる。CLOSED から出るのは「再オープン」(→ TODO)だけ(S3.2、オーナー了承) |
| TK-04 | 期限を過ぎて DONE・CLOSED になっていないものを「期限切れ」として表示する(「今日」は日本時間。今と同じ規則) |
| TK-05 | 一覧は、ステータス・担当者で絞り込める(Phase1)。並び順は更新日時の新しい順 |
| TK-06 | 削除は論理削除(画面から見えなくなるが、DB には残る)とする。コメント・通知との整合と、元に戻せるようにするため(Q3 で了承) |
| TK-07 | チェックリスト: 今のサブ項目と同じ仕組みを、チケットにも付けられる(任意。Phase3 以降、要望があれば) |

### 4.2 プロジェクト(Phase2・7)

| ID | 要件 |
|---|---|
| TK-10 | プロジェクトはキー(英大文字 2〜10 文字、一意、変更不可)、名前、説明、アーカイブ済みかを持つ |
| TK-11 | チケットは必ず 1 つのプロジェクトに属する。既存・Toggle OFF 時のチケットは既定のプロジェクト `GEN` に入る |
| TK-12 | アーカイブしたプロジェクトは一覧に出ず、チケットの登録先に選べない(見ることはできる) |
| TK-13 | オンボーディングのタスクひな形(有効なもの)から、指定した新入社員を担当者にしてチケットをまとめて起票できる(期限は一括指定。サブ項目はチェックリストとして写す) |
| TK-14 | プロジェクトのメンバー(Phase7): ユーザーをプロジェクトに追加・削除できる。見えるチケットの範囲を決める |

### 4.3 コメント(Phase4)

| ID | 要件 |
|---|---|
| TK-20 | チケットにコメント(本文 必須・2000 文字)を書ける。投稿者・日時を記録し、古い順に表示 |
| TK-21 | 自分のコメントだけ編集・削除できる(ADMIN は全部)。編集したら「編集済み」と表示 |
| TK-22 | 本文は文字として表示し、URL だけリンクにする(今の `LinkText` を使う。INV-04) |

### 4.4 検索(Phase5)

| ID | 要件 |
|---|---|
| TK-30 | 担当者・ステータス(複数)・プロジェクト・キーワード(タイトルと説明。大文字小文字を区別しない)・期限切れのみ、を組み合わせて検索できる |
| TK-31 | 検索条件は URL に入れる(ブックマーク・共有できる。今のユーザー一覧の絞り込みと同じ方式) |
| TK-32 | 結果はページ分けする(1 ページ 50 件) |

### 4.5 通知(Phase6)

| ID | 要件 |
|---|---|
| TK-40 | 次のときに、相手にアプリ内の通知を作る: 担当者になった、担当のチケットにコメントが付いた、担当のチケットの期限が明日 |
| TK-41 | メニューに未読の数を出す。一覧で既読にできる |
| TK-42 | メール通知は対象外(送信用サーバーが必要。今の backlog「専用URLのメール送信」と合わせて検討) |

### 4.6 ダッシュボード・カンバン(Phase8・9)

| ID | 要件 |
|---|---|
| TK-50 | ダッシュボードに、ステータス別の件数、期限切れ、担当者別の件数、自分の担当チケットを表示(範囲はロールと参加プロジェクトで決まる) |
| TK-51 | カンバン: プロジェクトごとに、ステータスを列にしてチケットを並べる。ボタン(または少量の JavaScript でドラッグ)でステータスを変える |

### 4.7 機能の切り替え(Phase0・1)

| ID | 要件 |
|---|---|
| TK-60 | 新機能は Feature Toggle で ON/OFF できる。OFF のときはメニューに出さず、URL は 404 |
| TK-61 | 管理者は画面から再起動なしで ON/OFF できる(誰がいつ変えたか記録する) |

## 5. 画面・URL(案)

| 画面 | URL | フェーズ | Toggle |
|---|---|---|---|
| チケット一覧(絞り込み) | `GET /tickets` | P1 | ticket-system |
| チケット登録・編集 | `GET /tickets/new`, `GET /tickets/{key}/edit`, `POST /tickets`, `POST /tickets/{key}` | P1 | ticket-system |
| チケット詳細 | `GET /tickets/{key}` | P1 | ticket-system |
| チケット削除 | `POST /tickets/{key}/delete` | P1 | ticket-system |
| ステータス変更 | `POST /tickets/{key}/status` | P1 | ticket-system |
| プロジェクト一覧・登録・編集 | `/projects`, `/projects/new`, `/projects/{key}/edit` | P2 | project-management |
| テンプレートから一括起票 | `POST /projects/{key}/onboarding` | P2 | project-management |
| コメント | `POST /tickets/{key}/comments`, `POST /tickets/{key}/comments/{id}`, `.../delete` | P4 | comments |
| 検索 | `GET /tickets?assignee=&status=&project=&q=&overdue=&page=` | P5 | ticket-system |
| 通知 | `GET /notifications`, `POST /notifications/read` | P6 | notifications |
| ダッシュボード | `GET /`(チケットの集計を追加) | P8 | ticket-dashboard |
| カンバン | `GET /projects/{key}/board`, `POST /tickets/{key}/status` | P9 | kanban-board |
| 機能の切り替え | `GET /admin/features`, `POST /admin/features/{name}` | P1 | (ADMIN のみ。Toggle なし) |

- 画面の作りは今と同じ(Thymeleaf、POST → リダイレクト、結果は緑 / 赤の枠、`fragments/nav.html`)
- チケットの URL は番号(`ONB-12`)で表す。番号は変わらないので、共有しても壊れない

## 6. API(案)

| メソッド | URL | フェーズ | 備考 |
|---|---|---|---|
| GET / POST | `/api/tickets` | P5 | 検索条件は画面と同じ。入出力は専用のクラス(DTO)。Entity をそのまま返さない |
| GET / PUT / DELETE | `/api/tickets/{key}` | P5 | |
| GET | `/api/projects` | P5 | |
| — | `/api/tasks` | — | **今のまま残す**(互換性を壊さない)。全面移行後も、使っている人がいなければ消すかをオーナーと決める |

- 認証は今と同じ HTTP Basic(管理者)から始め、Phase7 でロールに合わせる

## 7. データ設計(案)

```
projects 1 ──── * tickets * ──── 0..1 users(担当者)
   │                │  └──── 1 users(起票者)
   │                ├── * ticket_comments * ── 1 users(投稿者)        … P4
   │                └── * notifications * ──── 1 users(受け取る人)    … P6
   └── * project_members * ── 1 users                                   … P7
tasks(今のひな形)── 0..1 tickets.origin_task_id(どのひな形から作ったか)
feature_flags(機能の ON/OFF の記録)                                    … P1
```

| テーブル | 主な列 | 追加 |
|---|---|---|
| `tickets` | id, project_id(P2 で追加。既定 `GEN`), number, title, description, status, assignee_id, reporter_id, due_date, completed_at, origin_task_id, origin_assignment_id(移行用), deleted_at, created_at, updated_at | V12(P1) |
| `projects` | id, key(UK), name, description, next_number, archived, created_at, updated_at | V13(P2) |
| `ticket_checklist_items` / `ticket_checks` | 今の `task_checklist_items` / `assignment_checks` と同じ形 | 要望があれば(P3 以降) |
| `ticket_comments` | id, ticket_id, author_id, body, edited, deleted_at, created_at, updated_at | P4 |
| `notifications` | id, user_id, ticket_id, type, message, read_at, created_at | P6 |
| `project_members` | project_id, user_id, (UK) | P7 |
| `feature_flags` | name(PK), enabled, updated_by, updated_at | P1 |
| `users.role` | CHECK に `MANAGER`・`VIEWER` を追加 | P7 |

- すべて「足すだけ」のマイグレーション。今のテーブルの列の削除・型の変更はしない(FEATURE_TOGGLE_STRATEGY 4.2)
- 番号の採番は `projects.next_number` を行ロック(`SELECT ... FOR UPDATE`)で進める(同時に起票しても番号が重ならない)

## 8. 非機能要件

### 8.1 保守性

| ID | 要件 |
|---|---|
| NFT-01 | 新しいコードは Controller → Service → Repository の順に呼ぶ(今の決まり)。チケット関連は機能ごとのパッケージ(`ticket`・`project` など)にまとめる |
| NFT-02 | 1 つの PR は 1 つのテーマ・小さく(目安: 変更 400 行以内、レビューできる大きさ) |
| NFT-03 | すべての新機能に「正常系・入力エラー・404・権限なし」のテストと、Toggle の ON/OFF 両方のテスト |
| NFT-04 | 既存のテスト(221 件)を消さない・弱めない。移行中は常に緑 |
| NFT-05 | 要件 ID(TK-xx)とテストの対応を `docs/ai/spec.md` に追記する(今の FR と同じ管理) |

### 8.2 セキュリティ

| ID | 要件 |
|---|---|
| NFT-10 | 今の不変条件(INV-01〜14)を守る。特に、新しい URL は何もしなければ管理者専用(INV-01)、他人のデータは 404(INV-02)、利用者の入力はエスケープ(INV-04) |
| NFT-11 | チケット・コメントを見る・変える権限は、ロールと参加プロジェクトを **クエリの条件** で確認する(画面で隠すだけにしない) |
| NFT-12 | Toggle が OFF の機能は、URL を直接開いても 404 |
| NFT-13 | ロールを変える・Toggle を切り替える操作は、誰がいつ行ったかを残す |
| NFT-14 | 権限の判定に迷ったら、弱い権限に倒す(Toggle OFF 時の MANAGER は MEMBER として扱う、など) |

### 8.3 拡張性

| ID | 要件 |
|---|---|
| NFT-20 | ステータスは enum で持ち、DB には文字で保存(今と同じ)。将来の追加は「足すだけ」のマイグレーションでできる |
| NFT-21 | 通知は「出来事」と「送り方」を分ける(まずアプリ内。メール・Slack は後から送り方を足すだけにする) |
| NFT-22 | API は DTO で入出力し、画面と同じ Service を使う(画面と API でルールが分かれないように) |
| NFT-23 | 一覧・検索はページ分けを前提にする(数千件のチケットでも遅くならない) |

### 8.4 そのほか

| ID | 分類 | 要件 |
|---|---|---|
| NFT-30 | 可用性 | 移行中、オンボーディング機能が使えない時間を作らない |
| NFT-31 | 運用 | 起動方法(Docker・Codespaces)は今と同じ。新しい設定は環境変数で渡す |
| NFT-32 | 性能 | 一覧・検索は 1 画面あたり数回のクエリにまとめる(N+1 を作らない) |

## 9. 対象外

- スプリント・見積もり・バーンダウン、ワークフローの自由設定、添付ファイル、時間の記録、外部ツール連携(6 か月より先の候補)
- マルチテナント(複数の会社での共有)
- メール通知(送信用サーバーが決まってから)

## 10. オーナーに決めてほしいこと

| # | 決めること | 案 | 決める時期 | 回答(2026-10-20) |
|---|---|---|---|---|
| Q1 | このロードマップ全体で進めてよいか | 本ドキュメント一式 | Phase0 の前 | **OK** |
| Q2 | チケット・プロジェクトのテーブル追加(V12・V13 …) | 7 章のとおり。すべて「足すだけ」 | 各フェーズの最初 | **OK**(7 章の範囲の追加は了承済み。範囲を超える変更は改めて確認) |
| Q3 | チケットの削除は論理削除でよいか | 論理削除(TK-06) | Phase1 | **OK**(論理削除) |
| Q4 | `feature_flags` テーブルで再起動なしの切り替えを作るか | 作る(レベル 2) | Phase1 | **YES** |
| Q5 | ロールとログイン方法を分けるか、4 ロールの権限表 | 2.2・2.3 のとおり | Phase7 の前 | **分けない(今のまま)。4 ロールは Toggle で切り替える**(2.3 を更新) |
| Q6 | オンボーディングのデータをチケットへ写し、旧画面を退役させるか | Phase10 で実施(バックアップ後) | Phase9 の後 | **OK** |
| Q7 | カンバンでドラッグ操作に JavaScript を少し使ってよいか | ボタンで動かす版を先に作り、ドラッグは任意 | Phase9 の前 | **OK** |
