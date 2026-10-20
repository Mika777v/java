# Feature Toggle 戦略 — チケット管理への段階的な移行

- 版: 0.1(2026-10-20、案。オーナーの確認待ち)
- 関連: [PRODUCT_VISION.md](PRODUCT_VISION.md) / [PRD_TICKET_SYSTEM.md](PRD_TICKET_SYSTEM.md) /
  [ROADMAP_TICKET_SYSTEM.md](ROADMAP_TICKET_SYSTEM.md) / [TASKS_TICKET_SYSTEM.md](TASKS_TICKET_SYSTEM.md)

---

## 1. 方針

1. **今のオンボーディング機能は、移行が終わるまで常に使える状態を保つ。** 新しい機能は既定で OFF
2. **新機能は必ず Toggle の下に作る。** OFF のときは、画面のメニューにも出さず、URL も「見つからない」(404)にする
3. **DB の変更は Toggle で切り替えない。** テーブルや列の追加(Flyway)は常に適用し、古いコードが影響を受けない「足すだけ」の変更にする
4. **本番運用中でも、再起動なしで ON/OFF できる**(Phase1 で DB に切り替えの記録を持つ。それまでは設定 + 再起動)
5. **Toggle には寿命がある。** 完全に移行したら Toggle とその分岐コードを消す(撤去計画は 6 章)

このリポジトリにはすでに同じ考え方の仕組みがあります。新入社員のログイン方法を切り替える `app.member-login`(`LoginSettings`)です。
これと同じ書き方(`@Component` で画面から `${@...}` で参照)で作り、学習のしやすさと一貫性を保ちます。

---

## 2. 仕組み(設計)

### 2.1 設定の形

```yaml
# application.yml
features:
  ticket-system:
    enabled: ${FEATURE_TICKET_SYSTEM:false}
  project-management:
    enabled: ${FEATURE_PROJECT_MANAGEMENT:false}
  comments:
    enabled: ${FEATURE_COMMENTS:false}
  notifications:
    enabled: ${FEATURE_NOTIFICATIONS:false}
  extended-roles:
    enabled: ${FEATURE_EXTENDED_ROLES:false}
  ticket-dashboard:
    enabled: ${FEATURE_TICKET_DASHBOARD:false}
  kanban-board:
    enabled: ${FEATURE_KANBAN_BOARD:false}
  onboarding-legacy:
    enabled: ${FEATURE_ONBOARDING_LEGACY:true}     # 旧画面。最後に OFF → 撤去
```

- 名前は `features.<機能名>.enabled` に統一する(依頼の例 `features.ticket-system.enabled=true` の形)
- 環境変数は `FEATURE_<機能名>` にする(`docker-compose.yml`・`.devcontainer/start-app.sh`・CI に追加しやすいよう、明示的に書く)

### 2.2 コードの部品

| 部品 | 役割 | 手本にする既存コード |
|---|---|---|
| `Feature`(enum) | Toggle の名前の一覧。文字列の打ち間違いを防ぐ。親子関係(例: `KANBAN_BOARD` は `TICKET_SYSTEM` が前提)も持つ | `AssignmentStatus` |
| `FeatureProperties`(`@ConfigurationProperties("features")`) | 設定ファイル・環境変数の値 | `application.yml` の `app.*` |
| `FeatureFlags`(`@Component("features")`) | 「今 ON か」を答える。DB の記録 → 設定の順に見る。親が OFF なら子も OFF | `LoginSettings`(`@Component("loginSettings")`) |
| `@RequiresFeature(Feature.X)` + `FeatureToggleInterceptor` | Controller に付けると、OFF のときに 404 を返す | `PasswordChangeRequiredInterceptor` + `WebConfig` |
| テンプレートの `th:if="${@features.on('ticket-system')}"` | メニュー・ボタンを出し分ける | `${@loginSettings.linkMode}` |
| `feature_flags` テーブル + `/admin/features` 画面(Phase1) | 再起動なしで ON/OFF。誰がいつ変えたかも残す | — |

```
リクエスト ─▶ SecurityConfig(ログイン・権限)─▶ FeatureToggleInterceptor(OFF なら 404)─▶ Controller
画面       ─▶ fragments/nav.html: ${@features.on('ticket-system')} のときだけ「チケット」メニュー
```

### 2.3 OFF のときに 404 を返す理由

- 403(権限がない)だと「その機能がある」ことが分かってしまう。まだ公開していない機能は「ない」ことにする
- 既存の日本語の 404 画面(`error/404.html`)がそのまま使える

### 2.4 再起動なしの切り替え(「本番運用中でも安全に」)

| 段階 | 方法 | いつから |
|---|---|---|
| レベル 1 | 設定(環境変数)を変えて再起動 | Phase0(最初の PR) |
| レベル 2 | 管理者画面 `/admin/features` で切り替え。DB の `feature_flags` に記録し、次のリクエストから反映 | Phase1 の後半 |

- レベル 2 では DB の値があればそれを使い、なければ設定の値を使う
- 切り替えた人と日時を `feature_flags` に残し、`DEVLOG.md` と同じ考え方で後から追えるようにする
- 切り替えても DB のデータは消さない(OFF にしても、作ったチケットは残り、ON に戻すと元どおり見える)
- `feature_flags` テーブルの追加は DB 変更なので、運用ルールどおり実装前にオーナーの了承をもらう(TASKS の該当 Issue に明記)

### 2.5 Toggle の親子関係

```
ticket-system ─┬─ project-management
               ├─ comments
               ├─ notifications
               ├─ extended-roles(チケット画面の権限。ユーザー管理側の役割追加も含む)
               ├─ ticket-dashboard
               └─ kanban-board
onboarding-legacy(独立。旧画面)
```

親(`ticket-system`)が OFF のときは、子が ON でも OFF として扱います(片方だけ ON の中途半端な状態を作らない)。

---

## 3. Toggle 一覧

| 名称 | 目的 | ON のとき | OFF のとき | 既定 | 導入 | 撤去 |
|---|---|---|---|---|---|---|
| `ticket-system` | チケット管理の本体 | メニューに「チケット」。`/tickets` で一覧・登録・編集・削除、テンプレートからの一括起票 | メニューに出ない。`/tickets/**` は 404。作ったチケットは DB に残る | OFF | Phase1 | Phase10(全面移行後) |
| `project-management` | プロジェクト(Project └ Ticket) | メニューに「プロジェクト」。チケットの登録画面でプロジェクトを選ぶ。一覧をプロジェクトで分ける | `/projects/**` は 404。チケットはすべて既定のプロジェクト(`GEN`)に入り、プロジェクトの欄は出ない | OFF | Phase2 | Phase10 |
| `comments` | チケットのコメント | チケット詳細にコメント欄と投稿フォーム | コメント欄を出さない。投稿の URL は 404。書いたコメントは残る | OFF | Phase4 | Phase10 |
| `notifications` | アプリ内の通知 | 割り当て・コメント・期限の通知を作る。メニューに未読の数 | 通知を作らない(OFF の間の出来事は通知されない)。一覧は 404 | OFF | Phase6 | Phase10(メール通知を作るなら、そちらは運用の設定として別に残す) |
| `extended-roles` | 4 ロール(ADMIN / MANAGER / MEMBER / VIEWER) | ユーザー登録で MANAGER / VIEWER を選べる。チケット画面に 4 ロールの権限を適用 | 役割の選択は ADMIN / MEMBER だけ。すでに MANAGER の人は MEMBER として、VIEWER の人は見るだけとして扱う(権限は弱いほうに倒す) | OFF | Phase7 | Phase10 |
| `ticket-dashboard` | チケットのダッシュボード | トップページにチケットの集計(ステータス別、期限切れ、担当者別)を追加 | 今のダッシュボード(オンボーディング)のまま | OFF | Phase8 | Phase10 |
| `kanban-board` | カンバンボード | プロジェクトごとのボード画面。ステータスの列にチケットを並べ、移動できる | ボードは 404。一覧画面は使える | OFF | Phase9 | Phase10 |
| `onboarding-legacy` | 旧オンボーディング画面(タスク・割り当て・旧マイページ) | 今までどおり使える | 旧画面のメニューを隠し、URL は新しい画面へ案内する。データは残す | **ON** | Phase10 | Phase10 の最後(旧コードの削除と一緒に) |

- Phase3(ステータスの拡張)と Phase5(検索)は、`ticket-system` がベータ(OFF)の間に作るので、専用の Toggle は作りません(Toggle を増やしすぎないため)
- ステータスの拡張はチケットだけが対象です。今のオンボーディングの割り当て(未着手 / 対応中 / 完了)は変えません

---

## 4. 移行戦略

### 4.1 今の画面との共存

| 方法 | 内容 |
|---|---|
| URL を分ける | 新しい画面は `/tickets`・`/projects`・`/admin/features` に作る。今の URL(`/tasks`・`/users`・`/me` など)は変えない |
| テーブルを分ける | チケットは新しいテーブル(`tickets` など)に保存する。今の `tasks`・`task_assignments` は変えない |
| 共通部品は使い回す | ログイン・権限・ユーザー・画面の部品(`fragments/nav.html`・`error/*.html`・`app.css`)・日時の表示・リンク表示・チェックリストの考え方 |
| つなぎの機能 | 「オンボーディングのタスクひな形から、チケットをまとめて起票」(Phase2)。今の `tasks` を **読むだけ** で使う |
| メニュー | Toggle が ON のときだけ新しいメニューを足す。OFF なら今と見た目が変わらない |

### 4.2 DB を変えるタイミング(足す → 移す → 片付ける)

| 段階 | いつ | 内容 | 古いコードへの影響 |
|---|---|---|---|
| 足す(Expand) | 各フェーズの最初の PR | 新しいテーブル・NULL 可の列を Flyway で追加(`V12` 以降)。Toggle とは無関係に常に適用 | なし(使わないだけ) |
| 移す(Migrate) | Phase10 の前半 | 今の `task_assignments` を `tickets` へ写す(1 回だけ。写した元を記録して二重にしない)。実行前に DB のバックアップ | なし(元のデータはそのまま) |
| 片付ける(Contract) | Phase10 の後、安定してから(任意) | 使わなくなった列・テーブルを消すかどうかを決める。**消す場合は必ずオーナーの了承とバックアップ**。消さずに読み取り専用で残す選択肢もある | 旧コードを消した後なので影響なし |

ルール:
- 適用済みのマイグレーションは書き換えない(INV-08)。直すときは新しい番号で
- `NOT NULL` の列を足すときは既定値を付ける(既存のデータがあるため)
- データを消す・写す変更は、運用ルールの「事前確認が必要な作業」として扱う

### 4.3 段階リリースの流れ(1 つの Toggle の一生)

```
① 開発(OFF)        … 小さな PR を重ねる。テストは ON / OFF の両方で書く
② 社内確認(Codespaces で ON) … オーナーが codespaces-check.md の手順で確認
③ 公開(既定を ON にする PR)   … OFF に戻す方法を PR 本文に書いておく
④ 安定(2 週間問題なし)         … 問題があればすぐ OFF(再起動なし)
⑤ 撤去                          … Toggle の分岐・設定・テストの OFF 側を消す PR
```

### 4.4 ロールバック

| 何が起きたか | 戻し方 | かかる時間 |
|---|---|---|
| 新機能に不具合 | `/admin/features` で OFF(レベル 2)、または環境変数を戻して再起動(レベル 1) | 数秒〜数分 |
| コードの不具合で起動できない | その PR を GitHub で Revert する。DB は「足すだけ」なので古いコードでも動く | 数分 |
| マイグレーションの不具合 | 新しい番号の修正マイグレーションを出す(前に進めて直す。Flyway は元に戻す機能を使わない) | 数十分 |
| データの移行(Phase10)の失敗 | 実行前のバックアップから戻す。移行は 1 回のマイグレーションにまとめ、途中で失敗したら全部取り消し(トランザクション) | 数十分 |

---

## 5. テストと CI

| 確かめること | 方法 |
|---|---|
| OFF のとき、メニューに出ず URL は 404 | 既定(OFF)のままのテスト |
| ON のときの動き | `@SpringBootTest(properties = "features.ticket-system.enabled=true")` を付けたテストクラス |
| 親が OFF なら子も OFF | `FeatureFlags` の単体テスト |
| 今のオンボーディングが壊れていない | 既存の 221 件のテストをそのまま通す(1 件も消さない) |
| Docker で起動して動く | スモークテストに `FEATURE_TICKET_SYSTEM=true` での確認を追加(既存の確認はそのまま) |

---

## 6. Toggle の撤去計画

| 時期 | 撤去するもの | 条件 |
|---|---|---|
| Phase10 前半 | `ticket-system`・`project-management`・`comments`・`notifications`・`extended-roles`・`ticket-dashboard`・`kanban-board` | 既定 ON にしてから 2 週間、問題がないこと。1 つの Toggle につき 1 PR(分岐・設定・環境変数・OFF 側のテストを消す) |
| Phase10 後半 | `onboarding-legacy` | オンボーディングの全データをチケットへ写し終え、OFF にして 2 週間、問題がないこと。旧画面・旧 Controller・旧テストを消す |
| 最後 | `FeatureFlags` の仕組み自体を残すか決める | 将来の機能でも使うなら残す(`feature_flags` テーブルは空にする)。使わないなら消す |

撤去の約束:
- Toggle は「一時的なもの」として扱い、`docs/TASKS_TICKET_SYSTEM.md` の撤去 Issue を、Toggle を作る PR と同時に登録する
- 公開から 2 か月を過ぎても撤去していない Toggle は、`project-memory-manager` の技術的負債の一覧に載せる
