# 実装ガイド(feature-builder 用)

下の層から順に作ります。各ひな形は、このリポジトリの既存コードの書き方に合わせてあります。
パッケージのルートは `onboarding-progress-system/src/main/java/com/example/onboarding/`。
例として「タスクにメモ(`notes`)を付ける」「お知らせ(`notices`)を管理する」を使います。

## 1. Flyway マイグレーション(`src/main/resources/db/migration/`)

- ファイル名: `V{次の番号}__{英語で内容}.sql`(例: `V10__create_notices_table.sql`)。**既存の `V*.sql` は絶対に編集しない**
- 先頭に「なぜこの変更をするか」のコメントを英語で 1〜2 行書く(既存のファイルと同じ)
- 既存のデータがある DB でも動くようにする: `NOT NULL` の列を足すときは `DEFAULT` を付ける

```sql
-- Notes for administrators, shown on the task list.
ALTER TABLE tasks ADD COLUMN notes TEXT;
```

```sql
-- Announcements shown on the dashboard.
CREATE TABLE notices (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    published BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

- 状態の列は `VARCHAR(20)` + `CHECK (status IN (...))`(`V3` の `task_assignments.status` と同じ)
- 関連は `REFERENCES 親 (id) ON DELETE CASCADE` にするかを決め、検索に使う外部キーにはインデックスを付ける
- サンプルデータを足す・変えるなら、テスト用の `src/test/java/com/example/onboarding/SampleData.java` と `docs/ai/database.md` も直す

## 2. Entity(`entity/`)

- Lombok なし。getter / setter は手で書く
- `created_at` / `updated_at` は `@PrePersist` / `@PreUpdate`(既存の `Task` をそのまま真似する)
- enum は `@Enumerated(EnumType.STRING)`、`getLabel()` で日本語名(`AssignmentStatus` を真似する)
- `@ManyToOne(fetch = FetchType.LAZY, optional = false)`
- `@Column` の `nullable` / `length` をマイグレーションと合わせる
- 状態を変えるルールはメソッドにまとめる(例: `TaskAssignment.changeStatus(newStatus, now)`。「今」は引数でもらう)

## 3. Repository(`repository/`)

```java
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findByPublishedTrueOrderByIdDesc();
}
```

- 一覧で関連を表示するなら `@EntityGraph(attributePaths = "task")` を付ける(N+1 を防ぐ)
- 集計は Java のループではなく、1 回のクエリで(`TaskAssignmentRepository.countProgressByUser` を参照)
- 新入社員が触るデータは、ユーザー ID を条件に入れたメソッドで探す(`findByIdAndUserId`)

## 4. Service(`service/`)

```java
@Service
@Transactional
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final Clock clock;

    public NoticeService(NoticeRepository noticeRepository, Clock clock) {
        this.noticeRepository = noticeRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Notice> findPublished() {
        return noticeRepository.findByPublishedTrueOrderByIdDesc();
    }

    public Notice create(Notice notice) {
        return noticeRepository.save(notice);
    }
}
```

- コンストラクタインジェクション(`@Autowired` フィールドは使わない)
- 「今日」は `LocalDate.now(clock)`、「今」は `Instant.now(clock)`。`LocalDate.now()` を直接呼ばない
- 業務ルール違反は日本語メッセージの例外で知らせる(`AssignmentException` を参照)。Controller が flash の `error` にする
- Service は `Model` や `RedirectAttributes` を知らない(画面の都合は Controller の仕事)

## 5. Form(`form/`)

```java
public class NoticeForm {

    @NotBlank(message = "タイトルを入力してください")
    @Size(max = 200, message = "タイトルは200文字以内で入力してください")
    private String title;

    private boolean published;

    public static NoticeForm from(Notice notice) { ... }

    public void applyTo(Notice notice) {
        notice.setTitle(title.strip());     // 前後の空白は form 側で取り除く
        notice.setPublished(published);
    }

    // getter / setter
}
```

- 画面の入力に Entity を直接使わない(`id` などを外から書き換えられないように)
- メッセージは日本語。文字数の上限は DB の列と合わせる

## 6. Controller(`controller/`)

画面(`TaskPageController` が手本。ただし Repository ではなく Service を呼ぶ):

```java
@PostMapping("/notices")
public String createNotice(@Valid @ModelAttribute("noticeForm") NoticeForm form,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
        return "notice-form";
    }
    Notice notice = new Notice();
    form.applyTo(notice);
    noticeService.create(notice);

    redirectAttributes.addFlashAttribute("message", "お知らせを登録しました。");
    return "redirect:/notices";             // パスだけ(INV-06)
}
```

- 更新系は `POST` → リダイレクト(PRG)。PUT / DELETE は画面では使わない。削除は `POST /xxx/{id}/delete`
- 見つからないときは `throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notice not found")`
- REST API(要件で必要なときだけ): `/api/**` の下に `@RestController`。成功時 201 / 204、なければ 404。
  入出力は Entity ではなく専用のクラス(DTO。Java の `record` が簡単)にする

## 7. Thymeleaf(`src/main/resources/templates/`)

```html
<!DOCTYPE html>
<html lang="ja" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>お知らせ</title>
    <link rel="stylesheet" th:href="@{/css/app.css}">
</head>
<body>
<nav th:replace="~{fragments/nav :: nav('notices')}"></nav>

<h1>お知らせ</h1>

<div th:replace="~{fragments/nav :: messages}"></div>

<form method="post" th:action="@{/notices/{id}/delete(id=${notice.id})}"
      onsubmit="return confirm('削除しますか?')">
    <button type="submit" class="button button-danger">削除</button>
</form>
</body>
</html>
```

- 利用者が入力した文字は `th:text`。`th:utext` は `${@links.linkify(...)}` だけ(INV-04)
- フォームは `method="post"` + `th:action`(CSRF トークンが自動で入る)
- メニューに出すなら `fragments/nav.html` に追加。管理者だけなら `sec:authorize="hasRole('ADMIN')"`
- スタイルは `static/css/app.css` に追記。JavaScript は使わない(使うならオーナーに相談)
- 日時は `${@dates.format(...)}`、期限切れは `${@dates.isOverdue(...)}`

## 8. 権限(`security/SecurityConfig`)

- 何もしなければ新しい URL は **管理者専用**(INV-01)。新入社員にも見せるなら `/me/**` の下に作る
- 新入社員が触るデータは、本人のものかをクエリ条件で確かめ、他人のものは 404(INV-02)
- ログイン中でも「パスワード変更が必要」な人に開かせたい画面なら、`WebConfig` の除外に追加する

## 9. テスト(`src/test/java/com/example/onboarding/`)

```java
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestClockConfiguration.class})
@Transactional
@WithMockUser(roles = "ADMIN")
class NoticePageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NoticeRepository noticeRepository;

    @Test
    void createSavesNoticeAndRedirects() throws Exception {
        long before = noticeRepository.count();
        mockMvc.perform(post("/notices").param("title", "健康診断のお知らせ"))   // WebTestSupport.post(CSRF つき)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notices"))
                .andExpect(flash().attribute("message", "お知らせを登録しました。"));
        assertThat(noticeRepository.count()).isEqualTo(before + 1);
    }

    @Test
    void createShowsErrorWhenTitleIsBlank() throws Exception { ... containsString("タイトルを入力してください") ... }

    @Test
    void editReturns404WhenNoticeDoesNotExist() throws Exception { ... status().isNotFound() ... }

    @Test
    @WithMockUser(roles = "MEMBER")
    void membersCannotOpenNotices() throws Exception { ... status().isForbidden() ... }
}
```

- 最低限: **正常系 / 入力エラー / 存在しない id で 404 / 権限がない場合**
- POST / PUT / DELETE は `com.example.onboarding.WebTestSupport.post/put/delete`(`MockMvcRequestBuilders.post` を直接使うと 403)
- 件数・id は `SampleData` の定数を使う。日付は 2026-10-15 に固定されている
- 本物のログインユーザーが必要なら `.with(user(new AppUserDetails(user)))`(`AccessLinkTest` を参照)
- 画面の確認は `content().string(containsString("日本語の文言"))`
- 業務ルールだけなら Spring なしの単体テスト(`TaskAssignmentTest` を参照)

## 10. 動作確認

```bash
cd onboarding-progress-system
mvn -B test
```

画面を変えたら、`.github/workflows/reusable-docker-smoke.yml` と同じ手順(起動 → ヘルスチェック → ログイン → 画面を curl)で確認し、
必要ならスモークテストに新しい画面の確認を 1 行足す。
