# レビューのチェックリスト(spring-boot-architect 用)

各項目の「ここを見る」は、このリポジトリで実際にある例です。

## E. Entity 設計(`entity/`)

| # | 確認すること | 理由 | ここを見る |
|---|---|---|---|
| E1 | enum は `@Enumerated(EnumType.STRING)` で保存している | 番号で保存すると、enum の順番を変えたときに意味が変わる | `TaskAssignment.status`, `User.role` |
| E2 | `@ManyToOne` は `fetch = FetchType.LAZY` | 使わない関連まで毎回読み込むのを防ぐ。必要な画面では `@EntityGraph` で読む | `TaskAssignment.user/task` |
| E3 | `@Column(nullable, length)` がマイグレーションの `NOT NULL` / `VARCHAR(n)` と合っている | 食い違うとアプリの検証と DB のエラーがずれる | `V3`, `V6`〜`V8` と各 Entity |
| E4 | `created_at` / `updated_at` は `@PrePersist` / `@PreUpdate` で設定 | 決まりごと(ここだけ `Instant.now()` を使ってよい) | 3 つの Entity |
| E5 | 状態を変える業務ルールは Entity のメソッドにまとめている | 呼び出し側ごとに書くと、ルールがずれる | `TaskAssignment.changeStatus`, `User.clearAccessLink` |
| E6 | 秘密の値(パスワード・専用URLの鍵)はハッシュだけを持つ | DB が漏れても使えないように(INV-03) | `User.passwordHash`, `accessTokenHash` |
| E7 | 画面のフォームに Entity を直接使っていない(`form/` を使う) | `id` などを外から書き換えられないように | `TaskForm`, `UserForm` |

## S. Service 設計(`service/`)

| # | 確認すること | 理由 | ここを見る |
|---|---|---|---|
| S1 | クラスに `@Transactional`、読むだけのメソッドは `@Transactional(readOnly = true)` | 途中で失敗したときに全部取り消す。読み取りは軽くする | `AssignmentService` |
| S2 | 「確認してから保存」(重複チェックなど)が 1 つのトランザクションの中にある | 確認と保存の間に別の操作が入るとずれる。DB の一意制約も最後の砦として必要 | `AssignmentService.assign` と `UNIQUE (user_id, task_id)` |
| S3 | 「今日」「今」は `Clock` から取る(`LocalDate.now(clock)`) | テストで日付を 2026-10-15 に固定できるように | `AssignmentService`, `DashboardService` |
| S4 | 業務ルール違反は日本語メッセージの例外(`AssignmentException` など)で返し、Controller が flash の `error` にする | 画面に分かりやすく出すため。500 エラーにしない | `UserPageController` の割り当て |
| S5 | 一覧の集計を Java のループで 1 件ずつ問い合わせていない(N+1) | 人数が増えると遅くなる | `countProgressByUser`(1 回で集計) |
| S6 | 他人のデータを触れないよう、条件にユーザー ID を入れて検索している | 新入社員が他人の割り当てを変えられないように(INV-02) | `findByIdAndUserId` |

## A. API・Controller 設計(`controller/`)

| # | 確認すること | 理由 | ここを見る |
|---|---|---|---|
| A1 | 画面の更新は `POST` → リダイレクト(PRG)、結果は flash の `message` / `error` | 再読み込みで二重登録しない | すべての `*PageController` |
| A2 | 入力は `@Valid` + `BindingResult`、エラー時は同じ画面を返す(パスワード欄は空に戻す) | | `UserPageController.createUser` |
| A3 | 見つからないときは `ResponseStatusException(HttpStatus.NOT_FOUND, ...)` | 決まりごと。存在しない id で 500 にしない | `findTask`, `findUser` |
| A4 | REST API は適切なステータス(201 / 204 / 404)を返す | | `TaskController` |
| A5 | REST API の入力・出力に Entity をそのまま使っていない(DTO を使う) | 項目を増やしたときに、意図せず受け付けたり外に出したりしてしまう。**今の `TaskController` は Entity を直接使っている(改善候補・低)** | `TaskController.createTask/updateTask` |
| A6 | リダイレクト先はパスだけ(`redirect:/users/3`) | Codespaces のプロキシの裏で localhost に飛ばないように(INV-06) | すべての `redirect:` |
| A7 | Controller が長すぎない(目安 250 行) | 読みにくく、テストもしにくい。**`UserPageController` は約 380 行(改善候補)** | `UserPageController` |

## F. Flyway(`src/main/resources/db/migration/`)

| # | 確認すること | 理由 |
|---|---|---|
| F1 | すでに `master` に入った `V*.sql` を編集していない | 起動時にチェックサムが合わずエラーになる(INV-08)。変更は新しい番号のファイルで行う |
| F2 | ファイル名が `V{次の番号}__{英語で内容}.sql`、番号の重複・飛びがない | |
| F3 | 既存のデータがある DB でも動く(`NOT NULL` の列追加には `DEFAULT` を付ける、など) | Codespaces などの既存 DB で起動に失敗しないように。例: `V8` |
| F4 | 外部キーに `ON DELETE CASCADE` を付けるかを意図して決めている | ユーザー・タスクの削除で割り当ても消す設計(`V3`) |
| F5 | 検索条件に使う列に一意制約・インデックスがある | 例: `users.email`, `users.access_token_hash` |
| F6 | サンプルデータを変えたら、テスト用の `SampleData.java` と `docs/ai/database.md` も直している | テストがサンプルデータの件数を前提にしているため |
| F7 | Entity の `@Column` とマイグレーションの型・長さ・NULL 可否が一致 | E3 と同じ |

## L. レイヤー構造

| # | 確認すること | 理由 |
|---|---|---|
| L1 | Controller → Service → Repository の向きになっている。**Controller から Repository の直接呼び出しは改善候補** | 業務ルールが Controller に散らばると、画面と API で同じルールを二重に書くことになる |
| L2 | Service が Controller や画面(`Model`, `RedirectAttributes`)に依存していない | 画面以外(API・テスト)から使えるように |
| L3 | `security/`, `web/` が Repository を使うのは、ログインや共通処理での検索だけ | 業務ルールはここに置かない |
| L4 | Controller に `@Transactional` がない | トランザクションは Service の責任 |
| L5 | 同じ業務ルールが 2 か所以上に書かれていない | 例: タスクの登録が `TaskPageController` と `TaskController` の両方にある(Service にまとめる候補) |

## X. セキュリティ・テスト(レイヤー変更の影響確認)

| # | 確認すること |
|---|---|
| X1 | 新しい URL は、何もしなければ管理者専用になる(`SecurityConfig` の `anyRequest().hasRole("ADMIN")`)。新入社員用は `/me/**` |
| X2 | `th:utext` は `${@links.linkify(...)}` だけ |
| X3 | 新しい機能に「正常系」「入力エラー」「存在しない id で 404」「権限がない場合」のテストがある |
| X4 | テストの件数・id は `SampleData` の定数を使っている |
