# リファクタリング候補のカタログ(review-and-refactor 用)

2026-10-16(PR #30 マージ後)時点で、このリポジトリに実際にある候補です。直したら、この表から消すか「済」と書いてください。
行番号は目安です。必ずコードを開いて確かめてから使います。

## 一覧

| # | 種類 | 場所 | 優先度 |
|---|---|---|---|
| R1 | Service 層 | `TaskPageController` と `TaskController` → 新しい `TaskService` | 中 |
| R2 | Service 層 | `UserPageController` のユーザー登録・更新・削除 → 新しい `UserService` | 中 |
| R3 | 重複 / 大きすぎるクラス | `UserPageController`(約 380 行)を役割ごとに分ける | 低(R2 の後) |
| R4 | 重複 | `my-page.html` と `user-detail.html` の割り当ての表・状態の選択 | 低 |
| R5 | ベストプラクティス | `TaskController` が Entity `Task` をそのまま JSON で受け取る・返す | 低 |
| R6 | パフォーマンス | `DashboardService` が全ユーザーを読んでから管理者を除いている | 低 |
| R7 | Service 層 | `MyPageController` のパスワード変更が `UserRepository` を直接使う | 低(R2 と一緒に) |

---

## R1. `TaskService` を作る

- **今:** タスクの登録(並び順を末尾にする)・更新・削除が、画面用の `TaskPageController` と API 用の `TaskController` の両方に書かれていて、
  どちらも `TaskRepository` を直接呼んでいる
- **直し方:** `service/TaskService`(`@Service @Transactional`)に `findAllInDisplayOrder` / `find(id)` / `create(...)` / `update(...)` / `delete(id)` を置き、
  2 つの Controller はそれを呼ぶ。並び順は今どおり `TaskOrderService` を使う
- **なぜ:** 登録のルール(例: 新しいタスクは末尾)が 2 か所にあると、片方だけ直して画面と API で動きがずれる。トランザクションも Service でそろう
- **確かめ方:** `TaskPageControllerTest`, `TaskControllerTest`, `TaskOrderTest` が変更なしで通る
- **注意:** 見つからないときの 404 は今と同じにする(Service が `Optional` を返し、Controller が 404 にする、など)

## R2. `UserService` を作る

- **今:** `UserPageController` が、メールの重複確認 → 保存、パスワードの設定(他人に設定したら変更必須)、
  役割を管理者にしたら専用URLを消す、自分自身の削除の禁止、をすべて自分で行っている
- **直し方:** `service/UserService` に「登録」「更新」「削除」をまとめる。入力エラー(メールの重複など)は、
  今どおり `BindingResult` に載せられるよう、Service は結果(重複あり / なし)を返すか、専用の例外を投げる
- **なぜ:** ユーザーの業務ルール(INV-10, INV-11, FR-12)が Controller に散らばっていて、ユーザーの API や一括登録を作るときに同じルールをもう一度書くことになる
- **確かめ方:** `UserPageControllerTest`, `PasswordResetTest`, `FirstLoginPasswordChangeTest`, `AccessLinkTest` が変更なしで通る
- **注意:** 大きい変更なので、必ず先に了承をもらう

## R3. `UserPageController` を分ける

- **今:** ユーザーの CRUD、割り当て、専用URL、パスワードのリセットが 1 つのクラス(約 380 行)にある
- **直し方:** URL は変えずに、`UserAssignmentController`(`/users/{id}/assignments/**`)と
  `UserAccessController`(`/users/{id}/access-link/**`, `/users/{id}/password-reset`)に分ける
- **なぜ:** 1 つのクラスが長いと、どこを直せばよいか探しにくく、変更がぶつかりやすい
- **確かめ方:** `UserAssignmentPageTest`, `AccessLinkTest`, `PasswordResetTest` が変更なしで通る。URL 一覧(`docs/ai/architecture.md`)は変わらない
- **注意:** `@ModelAttribute`(役割の選択肢など)や `findUser` を分けた先でも使えるようにする

## R4. 割り当ての表を Thymeleaf の部品にする

- **今:** `my-page.html` と `user-detail.html` に、割り当ての表(タスク名・説明のリンク・状態の選択・変更ボタン)がほぼ同じ形で書かれている
  (`find-duplicates.py` で 7〜8 行 × 2 か所)
- **直し方:** `fragments/assignments.html` に、フォームの送り先を引数で受け取る部品を作る
- **なぜ:** 表示を変えるとき(例: 期限切れの表示)に 2 か所直す必要がある
- **確かめ方:** マイページ・ユーザー詳細のテスト(文言の確認)が通る。画面を開いて見た目が変わっていないことを確認
- **注意:** 2 つの画面で列が少し違う(管理者は「解除」ボタンがある)。部品の引数が増えすぎるなら、やらないほうがよい

## R5. API の入出力を DTO にする

- **今:** `TaskController` は `@RequestBody Task` で Entity を受け取り、`id` を `null` にしてから保存している。返すのも Entity
- **直し方:** `TaskRequest` / `TaskResponse`(Java の `record`)を作る。**JSON の項目名は今と同じにする**
- **なぜ:** Entity に項目を足すと、意図せず API から書き換えられたり、外に出たりする(例: 将来の内部向けの列)
- **確かめ方:** `TaskControllerTest` が変更なしで通る(JSON の形が変わっていない)

## R6. ダッシュボードで新入社員だけを DB から読む

- **今:** `DashboardService` が `userRepository.findAll()` で全員を読み、Java で管理者を除いている
- **直し方:** `UserRepository` に `List<User> findByRole(Role role)` を足して `findByRole(Role.MEMBER)` を使う
- **なぜ:** 必要な行だけを DB から読む。今の規模(数十人)では体感は変わらないので優先度は低い
- **確かめ方:** `DashboardTest` が通る

## R7. パスワード変更を Service に移す

- **今:** `MyPageController` が `UserRepository` で自分を読み、パスワードを変えて保存している
- **直し方:** R2 の `UserService` に `changeOwnPassword(userId, current, next)` を置く
- **確かめ方:** `PasswordChangeTest`, `FirstLoginPasswordChangeTest`

---

## 直さないもの(わざとそうしている)

| 場所 | 理由 |
|---|---|
| `PasswordChangeRequiredInterceptor` が毎回 DB を読む | 管理者がリセットしたとき、再ログインなしで変更画面へ送るため(`docs/ai/architecture.md`) |
| `security/`・`web/` が `UserRepository` を使う | ログイン処理・共通処理での検索だけで、業務ルールではない |
| 専用URLを無効化してもログイン中のセッションは残る | オーナー判断で対応しない(`docs/ai/spec.md` 6 章) |
| Entity と Form の項目が重複している | 画面から `id` などを書き換えられないように、わざと分けている |
| `show-sql: true` | 開発用の設定のまま。本番で使うときに検討する(`docs/ai/backlog.md` の既知の課題) |

## 命名の決まり(改善するときの基準)

- クラス: 画面の Controller は `XxxPageController`、API は `XxxController`(`/api/xxx`)、Service は `XxxService`
- テンプレート: 一覧は複数形(`tasks.html`)、登録・編集は `xxx-form.html`、詳細は `xxx-detail.html`
- Repository: Spring Data の命名規則(`findByXxxOrderByYyyAsc`)。長すぎる場合だけ `@Query` に名前を付ける
- 真偽値: `isXxx` / `hasXxx`(例: `isAdmin`, `hasValidAccessLink`)
- 変数名を変えるだけの修正は、そのファイルを別の理由で直すときについでに行う(単独の PR にしない)
