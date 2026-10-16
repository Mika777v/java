# このリポジトリで学ぶ Java(教科書)

このアプリ(`onboarding-progress-system`)のソースコードを教材にして、Java の基本と「Java ならではの動き・ハマりどころ」、
そして Spring Boot の仕組みを説明します。例はすべて **このリポジトリの実際のコード** です。

- 対象: Java を学び始めた人(ほかの言語を少し知っていると読みやすい)
- 読み方: 1 章から順に読むのがおすすめ。気になる章だけ読んでも分かるようにしています
- ソースの場所: 以下、`…/onboarding/` は `onboarding-progress-system/src/main/java/com/example/onboarding/` の省略です
- 試し方: コードを変えたら `cd onboarding-progress-system && mvn -B test` でテストが通るか確かめられます(13 章)

## 目次

- 第 1 部 Java の基本
  - [1. ファイル・クラス・パッケージ](#1-ファイルクラスパッケージ)
  - [2. 型: プリミティブ型と参照型](#2-型-プリミティブ型と参照型)
  - [3. クラスとオブジェクト](#3-クラスとオブジェクト)
  - [4. static と final](#4-static-と-final)
  - [5. アクセス修飾子](#5-アクセス修飾子)
  - [6. enum(列挙型)](#6-enum列挙型)
  - [7. record(データの入れ物)](#7-recordデータの入れ物)
  - [8. 継承とインターフェース](#8-継承とインターフェース)
  - [9. 例外](#9-例外)
- 第 2 部 少し進んだ書き方
  - [10. ジェネリクス](#10-ジェネリクス)
  - [11. null と Optional](#11-null-と-optional)
  - [12. ラムダ式・メソッド参照・Stream](#12-ラムダ式メソッド参照stream)
  - [13. 文字列](#13-文字列)
  - [14. 日付と時刻(java.time)](#14-日付と時刻javatime)
  - [15. 新しめの文法(switch 式・パターンマッチング・テキストブロック)](#15-新しめの文法switch-式パターンマッチングテキストブロック)
- 第 3 部 Java のハマりどころ
  - [16. ハマりどころ集](#16-ハマりどころ集)
- 第 4 部 Spring Boot とまわりの道具
  - [17. アノテーション](#17-アノテーション)
  - [18. DI(依存性の注入)と Bean](#18-di依存性の注入と-bean)
  - [19. 画面とリクエストの流れ(Spring MVC)](#19-画面とリクエストの流れspring-mvc)
  - [20. データベース(JPA・Spring Data・Flyway)](#20-データベースjpaspring-dataflyway)
  - [21. 設定ファイル(application.yml)](#21-設定ファイルapplicationyml)
  - [22. Maven とビルド](#22-maven-とビルド)
  - [23. テスト(JUnit・MockMvc・Testcontainers)](#23-テストjunitmockmvctestcontainers)
- [24. 練習問題](#24-練習問題)
- [25. 用語集](#25-用語集)

---

# 第 1 部 Java の基本

## 1. ファイル・クラス・パッケージ

### 1 ファイル = 1 クラス

Java では、`public` なクラスは **ファイル名とクラス名を同じにする** 決まりがあります。
`User.java` の中には `public class User` が 1 つだけ入っています。

### パッケージ = フォルダ

ファイルの先頭の `package` は「このクラスがどのグループに属するか」を表し、**フォルダの場所と一致させる** 決まりです。

```java
// …/onboarding/entity/User.java
package com.example.onboarding.entity;
```

フォルダ `com/example/onboarding/entity/` に置いたので、パッケージは `com.example.onboarding.entity` です。
このリポジトリは役割ごとにパッケージを分けています。

| パッケージ | 役割 |
|---|---|
| `controller` | 画面・API の入口(URL ごとの処理) |
| `service` | 業務ルール |
| `repository` | データベースの読み書き |
| `entity` | データベースの 1 行を表すクラス |
| `form` | 画面から送られた入力値 |
| `security` | ログイン・権限 |
| `config`, `web` | 設定、画面の補助 |

### import

別のパッケージのクラスを使うときは `import` を書きます。同じパッケージや `java.lang`(`String` など)は不要です。

```java
// …/onboarding/service/AssignmentService.java
import com.example.onboarding.entity.Task;
import java.time.LocalDate;
import java.util.List;
```

### プログラムの入口

Java のプログラムは `main` メソッドから始まります。このアプリの入口は 1 つだけです。

```java
// …/onboarding/OnboardingProgressSystemApplication.java
@SpringBootApplication
public class OnboardingProgressSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(OnboardingProgressSystemApplication.class, args);
    }
}
```

`SpringApplication.run(...)` が Spring Boot を起動し、あとは Spring が各クラスを動かします(18 章)。

---

## 2. 型: プリミティブ型と参照型

Java の型は大きく 2 種類あります。ここが Java 独特で、ハマりどころの多い部分です。

| 種類 | 例 | 特徴 |
|---|---|---|
| プリミティブ型(基本型) | `int`, `long`, `boolean`, `double` | 値そのもの。**`null` にならない**。小文字で始まる |
| 参照型 | `String`, `Integer`, `Long`, `Boolean`, `List`, 自作クラス | 「データの場所」を指す。**`null` になれる**。大文字で始まる |

プリミティブ型には、対応する参照型(**ラッパークラス**)があります: `int` ↔ `Integer`、`long` ↔ `Long`、`boolean` ↔ `Boolean`。

### このリポジトリでの使い分け

```java
// …/onboarding/entity/Task.java(データベースの 1 行)
private Long id;            // 保存前は id がまだない → null を表したいので Long
private Boolean active = true;
private Integer sortOrder;
```

```java
// …/onboarding/form/TaskForm.java(画面の入力)
private boolean active = true;   // チェックボックスは「ある / ない」の 2 択 → boolean
```

```java
// …/onboarding/service/UserProgress.java
public record UserProgress(long total, long done, long overdue) {   // 件数は必ずある → long
```

**目安:** 「値がない」状態(`null`)を表す必要があれば参照型、なければプリミティブ型。

### オートボクシング(自動変換)と、その落とし穴

Java は `int` と `Integer` などを **自動で変換** します(オートボクシング / アンボクシング)。便利ですが、
参照型が `null` のときにプリミティブ型へ変換しようとすると **`NullPointerException`** で止まります。

```java
Boolean active = null;
if (active) { ... }   // ここで NullPointerException(null を boolean に変換できない)
```

このリポジトリでは、それを避けるために次のように書いています。

```java
// …/onboarding/service/AssignmentService.java
if (!Boolean.TRUE.equals(task.getActive())) {
    throw new AssignmentException("無効(Active: No)のタスクは割り当てられません。");
}
```

`Boolean.TRUE.equals(x)` は、`x` が `null` でも安全に `false` を返します。

> **実話:** PR #12 では、API に `"active": null` を送ると 500 エラーになる不具合を直しました。
> `null` のまま保存しようとして、データベースの「NULL 禁止」に引っかかっていたためです。
> 今は `TaskController` で `null` のときは `true` にしています。

---

## 3. クラスとオブジェクト

**クラス** は設計図、**オブジェクト(インスタンス)** は設計図から作った実物です。`new` で作ります。

```java
// …/onboarding/service/AssignmentService.java
return saveNew(new TaskAssignment(user, task, dueDate));
```

### フィールド・コンストラクタ・メソッド

```java
// …/onboarding/entity/TaskAssignment.java(抜粋)
public class TaskAssignment {

    private User user;                 // フィールド(オブジェクトが持つデータ)
    private Task task;
    private AssignmentStatus status = AssignmentStatus.NOT_STARTED;
    private LocalDate dueDate;
    private Instant completedAt;

    protected TaskAssignment() {       // 引数なしのコンストラクタ(JPA が使う。20 章)
    }

    public TaskAssignment(User user, Task task, LocalDate dueDate) {   // コンストラクタ(作るときの初期化)
        this.user = user;              // this.user = フィールド、user = 引数
        this.task = task;
        this.dueDate = dueDate;
    }

    /** 期限切れかどうか */
    public boolean isOverdue(LocalDate today) {                         // メソッド(振る舞い)
        return dueDate != null && status != AssignmentStatus.DONE && dueDate.isBefore(today);
    }
}
```

- `this` は「このオブジェクト自身」。引数とフィールドの名前が同じときに区別するために使います
- コンストラクタは **クラス名と同じ名前** で、戻り値の型を書きません

### カプセル化と getter / setter

フィールドは `private` にして外から直接さわれないようにし、読み書きは **getter / setter** というメソッド経由にするのが Java の習慣です。

```java
// …/onboarding/entity/User.java
private String passwordHash;

public String getPasswordHash() { return passwordHash; }
public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

/** パスワードが設定されていれば、ログインできる */
public boolean isLoginEnabled() { return passwordHash != null; }
```

- `boolean` の getter は `getXxx` ではなく **`isXxx`** と名付ける習慣があります(`isLoginEnabled`, `isAdmin`)
- Thymeleaf(画面)では `${user.loginEnabled}` と書くと `isLoginEnabled()` が呼ばれます。この名前の決まりを **JavaBeans 規約** といいます
- 多くのプロジェクトは Lombok というライブラリで getter / setter を自動生成しますが、このリポジトリは学習のため手で書いています

### 振る舞いをデータの近くに置く

「完了にしたら完了日時を記録し、戻したら消す」というルールは、データを持つ `TaskAssignment` 自身に書いています。

```java
// …/onboarding/entity/TaskAssignment.java
public void changeStatus(AssignmentStatus newStatus, Instant now) {
    if (newStatus == AssignmentStatus.DONE && this.status != AssignmentStatus.DONE) {
        this.completedAt = now;
    } else if (newStatus != AssignmentStatus.DONE) {
        this.completedAt = null;
    }
    this.status = newStatus;
}
```

こうすると、どこから状態を変えても同じルールが必ず守られます。

---

## 4. static と final

### static: オブジェクトではなくクラスに属する

`static` を付けたものは、オブジェクトを作らなくても `クラス名.名前` で使えます。

```java
// …/onboarding/form/TaskForm.java
public static TaskForm from(Task task) {      // static メソッド: TaskForm.from(task) と呼ぶ
    TaskForm form = new TaskForm();
    form.setTitle(task.getTitle());
    ...
    return form;
}
```

このように「オブジェクトを作って返す static メソッド」を **ファクトリメソッド** と呼びます。

### final: 一度決めたら変えない

```java
// …/onboarding/service/TaskOrderService.java
private static final int STEP = 10;           // 定数(クラスに 1 つ、変更不可)
```

```java
// …/onboarding/service/AssignmentService.java
private final TaskAssignmentRepository assignmentRepository;   // コンストラクタで 1 回だけ代入できる
```

- `static final` の組み合わせは **定数**。名前は `UPPER_SNAKE_CASE`(大文字とアンダースコア)にする習慣です
- フィールドの `final` は「コンストラクタで必ず入れる・あとで変えない」という約束。Spring の部品(18 章)はこの形で受け取ります

### static な「空っぽの値」

```java
// …/onboarding/service/UserProgress.java
public static final UserProgress NONE = new UserProgress(0, 0, 0);
```

割り当てがない人に `null` ではなく `UserProgress.NONE` を使うことで、`null` チェックを減らしています。

---

## 5. アクセス修飾子

誰がそのクラス・メソッド・フィールドを使えるかを決めます。

| 修飾子 | 使える範囲 | このリポジトリの例 |
|---|---|---|
| `public` | どこからでも | ほとんどのクラスとメソッド |
| `protected` | 同じパッケージと、継承したクラス | `TaskAssignment()`(JPA 用の引数なしコンストラクタ) |
| (何も書かない) | **同じパッケージだけ**(パッケージプライベート) | `AccessLinkService.hash(...)`、テストクラス |
| `private` | そのクラスの中だけ | フィールド全般、`UserPageController.findUser(...)` |

```java
// …/onboarding/service/AccessLinkService.java
static String hash(String token) { ... }      // 何も書かない → 同じパッケージからだけ使える
```

```java
// src/test/java/com/example/onboarding/controller/TaskPageControllerTest.java
class TaskPageControllerTest { ... }          // テストクラスは public 不要(JUnit 5 から)
```

**目安:** できるだけ狭くする。外から使われるものだけ `public` にします。

---

## 6. enum(列挙型)

「決まった選択肢」を表す型です。Java の enum は **ただの定数の集まりではなく、フィールドやメソッドを持てる** のが特徴です。

```java
// …/onboarding/entity/AssignmentStatus.java
public enum AssignmentStatus {
    NOT_STARTED("未着手"),
    IN_PROGRESS("対応中"),
    DONE("完了");

    private final String label;

    AssignmentStatus(String label) {     // enum のコンストラクタ(外からは呼べない)
        this.label = label;
    }

    public String getLabel() {            // 画面に出す日本語名
        return label;
    }
}
```

- 使い方: `AssignmentStatus.DONE`、日本語名は `AssignmentStatus.DONE.getLabel()` → `"完了"`
- 比較は `==` で OK(enum は 1 つずつしか存在しないため)。`status != AssignmentStatus.DONE` のように書きます
- 全部の値: `AssignmentStatus.values()`(画面のプルダウンを作るのに使用)
- 文字列から: Spring が URL の `?status=OVERDUE` を自動で `ProgressFilter.OVERDUE` に変換します。存在しない値なら 400 エラー
- DB には `@Enumerated(EnumType.STRING)` で `"IN_PROGRESS"` のような **名前の文字列** で保存しています(20 章)。
  番号で保存すると、あとで並び順を変えたときにデータの意味が変わってしまうためです

enum にメソッドを持たせると、条件分岐をきれいに書けます(15 章の switch 式も参照)。

```java
// …/onboarding/service/ProgressFilter.java
public boolean matches(UserProgress progress) {
    return switch (this) {
        case INCOMPLETE -> progress.hasIncomplete();
        case OVERDUE -> progress.hasOverdue();
        case COMPLETED -> progress.isAllDone();
        case UNASSIGNED -> progress.isUnassigned();
    };
}
```

---

## 7. record(データの入れ物)

Java 16 から使える **record** は、「値を入れて運ぶだけ」のクラスを 1 行で書ける仕組みです。

```java
// …/onboarding/service/UserProgress.java
public record UserProgress(long total, long done, long overdue) {

    public int percent() {
        return total == 0 ? 0 : (int) (done * 100 / total);
    }
    ...
}
```

この 1 行だけで、Java が次のものを自動で作ります。

| 自動で作られるもの | 使い方 |
|---|---|
| コンストラクタ | `new UserProgress(4, 2, 1)` |
| 値を読むメソッド | `progress.total()`, `progress.done()`(**`get` が付かない**ことに注意) |
| `equals` / `hashCode` / `toString` | 中身が同じなら等しい、ログに中身を表示できる |

- record の値は **あとから変更できません**(setter がない)。だから安心して受け渡しできます
- `Dashboard` のように、record の中に record を入れることもできます

```java
// …/onboarding/service/Dashboard.java
public record Dashboard(long userCount, UserProgress overall, ..., List<FollowUp> followUps, ...) {
    public record FollowUp(User user, UserProgress progress) {
    }
}
```

**クラスと record の使い分け:** データベースに保存して値を変えていくもの(`User` など)はクラス、
計算結果を画面に渡すだけのもの(`UserProgress`, `Dashboard`)は record にしています。

---

## 8. 継承とインターフェース

### 継承(extends): 既存のクラスを引き継いで機能を足す

```java
// …/onboarding/security/AppUserDetails.java
public class AppUserDetails extends org.springframework.security.core.userdetails.User {

    private final Long userId;
    private final String displayName;

    public AppUserDetails(User user) {
        // Users who log in with their personal link may have no password
        super(user.getEmail(), user.getPasswordHash() == null ? "" : user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        this.userId = user.getId();
        this.displayName = user.getName();
    }
}
```

- Spring Security の「ログイン中のユーザー」クラスを引き継ぎ、`userId` と `displayName` を足しています
- `super(...)` は **親クラスのコンストラクタを呼ぶ** 書き方。コンストラクタの最初に書く必要があります
- 名前が同じ `User` クラスが 2 つ(自分の `entity.User` と Spring の `User`)あるので、片方はパッケージ名付きのフルネームで書いています
- Java は **親クラスを 1 つしか持てません**(単一継承)

```java
// …/onboarding/service/AssignmentException.java
public class AssignmentException extends RuntimeException {   // 例外も継承で作る(9 章)
    public AssignmentException(String message) {
        super(message);
    }
}
```

### インターフェース(implements): 「このメソッドを持っています」という約束

インターフェースはメソッドの **名前と形だけ** を決めたものです。クラスは複数のインターフェースを実装できます。

```java
// …/onboarding/web/PasswordChangeRequiredInterceptor.java
public class PasswordChangeRequiredInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) ... {
        ...
    }
}
```

- `implements HandlerInterceptor` で「リクエストの前に呼ばれる部品です」と Spring に伝えています
- `@Override` は「親やインターフェースのメソッドを上書きしています」という印。名前を間違えるとコンパイルエラーで教えてくれます

### 実装を書かないインターフェース(Spring Data の魔法)

```java
// …/onboarding/repository/UserRepository.java
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmailIgnoreCase(String email);
    Optional<User> findByEmailIgnoreCase(String email);
}
```

このインターフェースには **実装クラスがどこにもありません**。Spring Data が起動時にメソッド名を読んで、
SQL を実行するクラスを自動で作ります(20 章)。インターフェースだけで使える、Java + Spring らしい仕組みです。

---

## 9. 例外

エラーが起きたことを伝える仕組みです。`throw` で投げ、`try` / `catch` で受け止めます。

```java
// …/onboarding/service/AssignmentService.java(投げる側)
if (assignmentRepository.existsByUserIdAndTaskId(user.getId(), taskId)) {
    throw new AssignmentException("このタスクは既に割り当てられています。");
}
```

```java
// …/onboarding/controller/UserPageController.java(受け止める側)
try {
    TaskAssignment assignment = assignmentService.assign(user, taskId, dueDate);
    redirectAttributes.addFlashAttribute("message", "「" + assignment.getTask().getTitle() + "」を割り当てました。");
} catch (AssignmentException e) {
    redirectAttributes.addFlashAttribute("error", e.getMessage());   // 画面に赤い枠で表示
}
```

例外を受け止めて、別の(分かりやすい)例外に包み直すこともできます。

```java
// …/onboarding/service/AssignmentService.java
try {
    return assignmentRepository.saveAndFlush(assignment);
} catch (DataIntegrityViolationException e) {        // DB の「同じ組み合わせは 1 回だけ」に違反
    throw new AssignmentException(ALREADY_ASSIGNED); // → 画面にいつものメッセージを出す
}
```

> **実話:** 「割り当て」ボタンを素早く 2 回押すと、2 つのリクエストがどちらも「まだ割り当てていない」と判断し、
> 2 つ目が DB に断られて 500 エラー画面になっていました(20 回中 17 回再現)。DB の制約を最後の砦にして、
> 断られたら上のように包み直すことで、いつものメッセージを出すようにしました。

### Java 独特: チェック例外と非チェック例外

| 種類 | 例 | 特徴 |
|---|---|---|
| **チェック例外** | `IOException`, `NoSuchAlgorithmException` | `catch` するか、メソッドに `throws` と書かないと **コンパイルエラー** |
| 非チェック例外(`RuntimeException` の子) | `NullPointerException`, `IllegalStateException`, `AssignmentException` | 書かなくてもコンパイルできる |

チェック例外は「必ず考えなさい」と強制する、ほかの言語にはあまりない Java 独特の仕組みです。

```java
// …/onboarding/service/AccessLinkService.java
try {
    byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(digest);
} catch (NoSuchAlgorithmException e) {                       // チェック例外なので catch が必須
    throw new IllegalStateException("SHA-256 is not available", e);   // 非チェック例外に包み直す
}
```

SHA-256 は Java に必ず入っているので、実際には起きません。それでも `catch` が必要なのがチェック例外です。

```java
// …/onboarding/web/PasswordChangeRequiredInterceptor.java
public boolean preHandle(...) throws IOException {   // catch しない代わりに「投げるかも」と宣言
```

### Spring での例外の使い方

```java
// …/onboarding/controller/UserPageController.java
private User findUser(Long id) {
    return userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
}
```

`ResponseStatusException` を投げると、Spring が 404 などの HTTP エラーに変え、日本語のエラー画面(`templates/error/404.html`)を表示します。

---

# 第 2 部 少し進んだ書き方

## 10. ジェネリクス

`<>` の中に型を書いて、「何を入れる入れ物か」を決める仕組みです。

```java
List<User> users;                          // User だけが入るリスト
Map<Long, UserProgress> progressByUser;    // キーが Long、値が UserProgress の対応表
Optional<User> user;                       // User が「あるかもしれない」入れ物(11 章)
interface UserRepository extends JpaRepository<User, Long>   // User を、Long 型の id で扱う
```

- 間違った型を入れようとすると、実行する前に **コンパイルエラー** で分かります
- 右側の `<>` は省略できます: `new ArrayList<>(...)`(ダイヤモンド演算子)

```java
// …/onboarding/service/TaskOrderService.java
List<Task> tasks = new ArrayList<>(taskRepository.findAllInDisplayOrder());
```

- `<?>` は「型は何でもよい」: テストの `PostgreSQLContainer<?>`

---

## 11. null と Optional

`null` は「何も指していない」状態です。`null` のままメソッドを呼ぶと **`NullPointerException`(通称ヌルポ)** で止まります。
Java で一番よく見るエラーです。

### Optional: 「ないかもしれない」を型で表す

```java
// …/onboarding/repository/UserRepository.java
Optional<User> findByEmailIgnoreCase(String email);   // 見つからないかもしれない
```

`Optional` から中身を取り出すときに、「なかったらどうするか」を必ず書くことになるので、`null` の見落としが減ります。

```java
// ない場合は例外(404)
User user = userRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

// ない場合の代わりの値
boolean required = userRepository.findById(me.getUserId())
        .map(user -> user.isPasswordChangeRequired())
        .orElse(false);
```

```java
// …/onboarding/security/AdminAccountInitializer.java(ある場合・ない場合で処理を分ける)
userRepository.findByEmailIgnoreCase(email).ifPresentOrElse(user -> {
    ... // 見つかったとき
}, () -> log.warn("... no user {} exists: nothing was reset.", email));
```

```java
// …/onboarding/service/AccessLinkService.java(条件で絞り込む)
return userRepository.findByAccessTokenHash(hash(token))
        .filter(user -> !user.isAdmin())
        .filter(user -> user.hasValidAccessLink(now));   // 条件に合わなければ「空」になる
```

---

## 12. ラムダ式・メソッド参照・Stream

### ラムダ式: 「処理」を値として渡す

`引数 -> 処理` の形で、その場で小さな関数を書けます。

```java
task -> Boolean.TRUE.equals(task.getActive())          // task を受け取って true/false を返す
(request, response, exception) -> response.sendRedirect(request.getContextPath() + "/login")
```

Spring Security の設定は、ほぼラムダ式で書かれています。

```java
// …/onboarding/security/SecurityConfig.java
.authorizeHttpRequests(auth -> auth
        .requestMatchers("/login", "/join/**", "/css/**", "/error", "/favicon.ico").permitAll()
        .requestMatchers("/", "/me", "/me/**").authenticated()
        .anyRequest().hasRole("ADMIN"))
```

### メソッド参照: ラムダ式の短縮形

| メソッド参照 | 同じ意味のラムダ式 |
|---|---|
| `User::getDepartment` | `user -> user.getDepartment()` |
| `Objects::nonNull` | `x -> x != null` |
| `AppUserDetails::new` | `user -> new AppUserDetails(user)` |

### Stream: リストを「流れ作業」で加工する

```java
// …/onboarding/controller/UserPageController.java(部署の一覧を作る)
List<String> departments = allUsers.stream()   // ① 流れを作る
        .map(User::getDepartment)              // ② 各ユーザー → 部署名に変換
        .filter(Objects::nonNull)              // ③ 部署が空の人を除く
        .distinct()                            // ④ 重複を除く
        .sorted()                              // ⑤ 並べ替え
        .toList();                             // ⑥ リストに戻す
```

`for` 文で書くと 10 行近くになる処理を、「何をするか」の順に並べて書けます。
よく使う操作:

| 操作 | 意味 | このリポジトリの例 |
|---|---|---|
| `filter` | 条件に合うものだけ残す | 管理者を除く: `.filter(user -> !user.isAdmin())` |
| `map` | 別の値に変換する | `.map(User::getDepartment)` |
| `anyMatch` | 1 つでも条件に合うか | `DashboardController` の管理者判定 |
| `sorted` | 並べ替え | ダッシュボードの「フォローが必要な人」 |
| `mapToLong(...).sum()` | 数値の合計 | ダッシュボードの期限切れ件数 |
| `collect(Collectors.toMap(...))` | 対応表(Map)にする | `AssignmentService.progressByUser()` |
| `toList()` | リストにする | あちこち |

```java
// …/onboarding/service/DashboardService.java(並べ替えの条件を重ねる)
.sorted(Comparator.comparingLong((Dashboard.FollowUp f) -> f.progress().overdue()).reversed()   // 期限切れの多い順
        .thenComparingInt(f -> f.progress().percent())                                         // 同じなら完了率の低い順
        .thenComparing(f -> f.user().getId()))                                                 // それでも同じなら id 順
```

**注意:** `toList()` で作ったリストは **変更できません**(`add` すると例外)。
変更したいときは `TaskOrderService` のように `new ArrayList<>(...)` でコピーします。

---

## 13. 文字列

### 文字列の比較は `equals`(`==` は使わない)

```java
// …/onboarding/controller/MyPageController.java
if (!form.getNewPassword().equals(form.getConfirmPassword())) {   // 中身を比べる
```

`==` は「同じ場所を指しているか」を比べるので、中身が同じ文字列でも `false` になることがあります(16 章)。

```java
"anonymousUser".equals(authentication.getName())   // 定数を左に置くと、右が null でも安全
```

### 文字列は変更できない(immutable)

`strip()` などのメソッドは元の文字列を変えず、**新しい文字列を返します**。

```java
// …/onboarding/form/TaskForm.java
task.setTitle(title.strip());   // 前後の空白を除いた「新しい文字列」を渡す
```

| メソッド | 意味 |
|---|---|
| `strip()` | 前後の空白(全角スペースも)を除く。古い `trim()` は全角を除けない |
| `isBlank()` | 空、または空白だけなら true |
| `isEmpty()` | 長さ 0 なら true(空白だけは false) |
| `"a" + "b"` | 連結 |

---

## 14. 日付と時刻(java.time)

Java 8 からの `java.time` パッケージを使っています。

| クラス | 意味 | 例 |
|---|---|---|
| `LocalDate` | 日付だけ(タイムゾーンなし) | 期限 `dueDate`、入社日 `joinedOn` |
| `Instant` | 世界共通の「ある瞬間」 | 完了日時 `completedAt`、作成日時 |
| `ZoneId` | タイムゾーン | `Asia/Tokyo` |
| `Clock` | 「今」を教えてくれる時計 | `TimeConfig` |
| `Duration` | 時間の長さ | 専用URLの有効期間(60 日) |

### 「今」を直接取らず、Clock から取る

```java
// …/onboarding/service/AssignmentService.java
LocalDate today = LocalDate.now(clock);   // LocalDate.now() とは書かない
```

`clock` は Spring から受け取った部品で、本番では本当の時計、**テストでは 2026-10-15 で止まった時計** です。

```java
// src/test/java/com/example/onboarding/TestClockConfiguration.java
public static final Instant NOW = Instant.parse("2026-10-15T00:00:00Z");

@Bean
@Primary
Clock fixedClock() {
    return Clock.fixed(NOW, ZoneId.of("Asia/Tokyo"));
}
```

こうすると「期限切れ」のテストが、いつ実行しても同じ結果になります。

### 日付は不変、計算は新しい値を返す

```java
LocalDate upcomingUntil = today.plusDays(UPCOMING_DAYS);   // today は変わらない
dueDate.isBefore(today)                                    // 比較
Instant.now(clock).plus(validity)                          // 専用URLの有効期限
```

---

## 15. 新しめの文法(switch 式・パターンマッチング・テキストブロック)

Java は毎年少しずつ文法が増えています。このリポジトリは Java 21 なので、新しい書き方も使っています。

### switch 式(Java 14〜)

値を返す `switch` を `->` で書けます。昔の `case ...: break;` より短く、`break` 忘れの心配もありません。

```java
// …/onboarding/security/LoginSettings.java
this.linkMode = switch (memberLogin.strip().toLowerCase(Locale.ROOT)) {
    case "link" -> true;
    case "password" -> false;
    default -> throw new IllegalStateException(
            "app.member-login (APP_MEMBER_LOGIN) must be 'link' or 'password', but was: " + memberLogin);
};
```

enum を switch すると、**値の書き忘れがあればコンパイルエラー** になります(6 章の `ProgressFilter.matches`)。

### instanceof のパターンマッチング(Java 16〜)

「この型か?」の確認と、その型の変数への代入を 1 回で書けます。

```java
// …/onboarding/web/CurrentUserAdvice.java
if (authentication.getPrincipal() instanceof AppUserDetails user) {
    return user.getDisplayName();     // ここでは user を AppUserDetails として使える
}
```

昔は `if (x instanceof AppUserDetails) { AppUserDetails user = (AppUserDetails) x; ... }` と 2 回書いていました。

### テキストブロック(Java 15〜)

`"""` で囲むと、複数行の文字列をそのまま書けます。SQL や JSON を書くときに便利です。

```java
// …/onboarding/repository/TaskAssignmentRepository.java
@Query("""
        select a.user.id as userId,
               count(a) as total,
               sum(case when a.status = :done then 1 else 0 end) as done,
               ...
        from TaskAssignment a
        group by a.user.id
        """)
List<ProgressRow> countProgressByUser(@Param("done") AssignmentStatus done, @Param("today") LocalDate today);
```

### 三項演算子

`条件 ? 真のとき : 偽のとき` で、短い if を 1 行にできます(新しくはありませんがよく出てきます)。

```java
return user.isAdmin() ? "redirect:/" : "redirect:/me";
```

---

# 第 3 部 Java のハマりどころ

## 16. ハマりどころ集

### ① `==` と `equals` の違い(最重要)

| 比べるもの | 使うもの | 理由 |
|---|---|---|
| `int`, `long`, `boolean`(プリミティブ型) | `==` | 値そのものを比べる |
| enum | `==` | 同じ値は 1 つしか存在しない |
| `String`, `Long`, `Integer`, 自作クラス(参照型) | **`equals`** | `==` は「同じ場所か」を比べてしまう |

特に危険なのが `Long` です。

```java
// …/onboarding/service/TaskOrderService.java
if (tasks.get(i).getId().equals(task.getId())) {   // 正しい
// if (tasks.get(i).getId() == task.getId()) {     // 間違い!
```

Java は `-128〜127` の `Long` / `Integer` だけを使い回す(キャッシュする)ため、`==` で比べると
**id が 127 までは正しく動き、128 を超えると突然 false になる** という、テストで見つけにくい不具合になります。

```java
// …/onboarding/controller/UserPageController.java(自分自身かどうか)
return me != null && userId.equals(me.getUserId());
```

### ② null のアンボクシング

2 章のとおり、`Boolean` / `Long` などが `null` のときに `boolean` / `long` として使うと `NullPointerException`。
`Boolean.TRUE.equals(...)` や `null` チェックで防ぎます。

### ③ 整数の割り算は切り捨て

```java
// …/onboarding/service/UserProgress.java
return total == 0 ? 0 : (int) (done * 100 / total);
```

- `long` 同士の割り算は **小数点以下が切り捨て**: 1 ÷ 3 × 100 → `33`(33.33… ではない)
- 先に `* 100` しているのは、`done / total * 100` だと `1 / 3 = 0` になって常に 0% になるため
- `total == 0` のときに割ると `ArithmeticException`(0 で割った)になるので、先に確認しています
- `(int)` は **型変換(キャスト)**。`long` を `int` に変えています

テスト `UserProgressTest.percentIsRoundedDown` で `33` になることを確かめています。

### ④ 変更できないリスト

`List.of(...)` や Stream の `toList()` で作ったリストに `add` / `remove` すると `UnsupportedOperationException`。
変更するなら `new ArrayList<>(...)` でコピーします(12 章)。

### ⑤ チェック例外の「とりあえず握りつぶし」

`catch (Exception e) {}` のように何もしない catch は、エラーが起きたことが分からなくなるので書かない。
このリポジトリでは、包み直して投げる(9 章)か、ログに出しています。

### ⑥ 文字コードを明示する

```java
token.getBytes(StandardCharsets.UTF_8)   // 文字コードを指定しないと、動かす環境によって結果が変わることがある
```

---

# 第 4 部 Spring Boot とまわりの道具

## 17. アノテーション

`@` で始まるものは **アノテーション(注釈)** です。それ自体は何もしませんが、**Spring などのライブラリが読み取って動きを変えます**。

```java
@Entity                     // JPA:「これは DB の 1 行を表すクラス」
@Table(name = "tasks")      // JPA: テーブル名は tasks
public class Task {
    @NotBlank(message = "title is required")   // 入力チェック: 空はダメ
    @Size(max = 200, ...)                      // 入力チェック: 200 文字まで
    @Column(nullable = false, length = 200)    // JPA: 列の設定
    private String title;
```

よく出てくるもの:

| アノテーション | 意味 |
|---|---|
| `@Controller` / `@RestController` | 画面 / API の入口(19 章) |
| `@Service` / `@Component` / `@Configuration` | Spring に部品として登録する(18 章) |
| `@GetMapping("/tasks")` / `@PostMapping` | この URL のこのメソッドで処理する |
| `@Transactional` | このメソッドの DB 操作をひとまとまりにする(途中で失敗したら全部取り消す) |
| `@Valid` | フォームの入力チェックを実行する |
| `@Entity`, `@Id`, `@Column`, `@ManyToOne` | DB との対応(20 章) |
| `@Value("${app.time-zone}")` | 設定ファイルの値を受け取る(21 章) |
| `@Test` | JUnit のテストメソッド(23 章) |
| `@Override` | 親のメソッドを上書きしている印(Java 標準) |

---

## 18. DI(依存性の注入)と Bean

Spring の一番大事な仕組みです。

### 自分で new しない

`AssignmentService` は `TaskAssignmentRepository` などを使いますが、**自分で `new` していません**。

```java
// …/onboarding/service/AssignmentService.java
@Service
public class AssignmentService {

    private final TaskAssignmentRepository assignmentRepository;
    private final TaskRepository taskRepository;
    private final Clock clock;

    public AssignmentService(TaskAssignmentRepository assignmentRepository,   // Spring が用意して渡してくれる
                             TaskRepository taskRepository,
                             Clock clock) {
        this.assignmentRepository = assignmentRepository;
        this.taskRepository = taskRepository;
        this.clock = clock;
    }
```

1. 起動時、Spring は `@Service` / `@Controller` / `@Component` / `@Repository` などが付いたクラスを探し(**コンポーネントスキャン**)、オブジェクトを 1 つずつ作ります。これを **Bean** と呼びます
2. コンストラクタの引数を見て、必要な Bean を **自動で渡します**。これが **DI(依存性の注入)** です
3. このリポジトリは、引数で受け取る **コンストラクタインジェクション** に統一しています(`final` にできて、テストもしやすい)

### 何がうれしいか

- テストでは `Clock` を「止まった時計」に差し替えるだけで、`AssignmentService` を変えずに日付を固定できる(14 章)
- 部品同士のつながりを Spring がまとめて管理してくれる

### Bean を自分で作る(@Configuration + @Bean)

ライブラリのクラスなど、`@Component` を付けられないものは、メソッドで作って登録します。

```java
// …/onboarding/config/TimeConfig.java
@Configuration
public class TimeConfig {
    @Bean
    public Clock clock(@Value("${app.time-zone:Asia/Tokyo}") String timeZone) {   // 戻り値が Bean として登録され、
        return Clock.system(ZoneId.of(timeZone));                                  // Clock が必要なところに渡される
    }
}
```

```java
// …/onboarding/security/SecurityConfig.java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

---

## 19. 画面とリクエストの流れ(Spring MVC)

ブラウザで「専用URLを発行」ボタンを押したときの流れです。

```
ブラウザ  POST /users/3/access-link
  ↓
Spring Security(SecurityConfig)… ログイン済み? 管理者? CSRF トークンは正しい?
  ↓
RemovedUserLogoutInterceptor … 削除された人・役割が変わった人ならログアウトして /login へ
  ↓
PasswordChangeRequiredInterceptor … パスワード変更が必要な人なら /me/password へ
  ↓
UserPageController.issueAccessLink(3, ...)   … @PostMapping("/users/{id}/access-link")
  ↓
AccessLinkService.issue(user)                … 乱数を作り、ハッシュを保存
  ↓
UserRepository.save(user)                    … DB に保存(UPDATE 文は JPA が作る)
  ↓
"redirect:/users/3"                          … ブラウザに「/users/3 を開き直して」と返す
  ↓
ブラウザ  GET /users/3 → showUser(...) → "user-detail" → templates/user-detail.html を HTML にして返す
```

```java
// …/onboarding/controller/UserPageController.java
@PostMapping("/users/{id}/access-link")
public String issueAccessLink(@PathVariable Long id, RedirectAttributes redirectAttributes) {
    User user = findUserForAccessLink(id);
    ...
    String token = accessLinkService.issue(user);
    redirectAttributes.addFlashAttribute("accessLink", appBaseUrl() + "/join/" + token);   // 次の画面に 1 回だけ渡す
    redirectAttributes.addFlashAttribute("message", "専用URLを発行しました。");
    return "redirect:/users/" + id;
}
```

| 書き方 | 意味 |
|---|---|
| `@PathVariable Long id` | URL の `{id}` 部分を受け取る(文字列 → `Long` に自動変換) |
| `@RequestParam` | `?status=OVERDUE` やフォームの値を受け取る |
| `@ModelAttribute` + `@Valid` | フォーム全体を `TaskForm` などに入れて入力チェック |
| `Model model` | 画面(テンプレート)に渡す値を入れる |
| 戻り値 `"user-detail"` | `templates/user-detail.html` を表示する |
| 戻り値 `"redirect:/users/3"` | 別の URL へ移動させる(登録後の二重送信を防ぐ **PRG パターン**) |

画面側(Thymeleaf)では `${...}` で値を表示します。

```html
<!-- templates/user-detail.html -->
<h1 th:text="${user.name} + ' さんの進捗'">山田 太郎 さんの進捗</h1>
```

---

## 20. データベース(JPA・Spring Data・Flyway)

### JPA: クラスとテーブルを対応させる

| Java | DB |
|---|---|
| `@Entity class Task` | テーブル `tasks` |
| フィールド `sortOrder` + `@Column(name = "sort_order")` | 列 `sort_order` |
| `@Id @GeneratedValue` | 自動採番の主キー |
| `@ManyToOne` | 外部キー(割り当て → ユーザー) |
| `@Enumerated(EnumType.STRING)` | enum を文字列で保存 |

- JPA はオブジェクトを作るときに **引数なしのコンストラクタ** を使うので、`TaskAssignment` には `protected TaskAssignment() {}` があります
- `@PrePersist` / `@PreUpdate` を付けたメソッドは、保存の直前に自動で呼ばれます(作成日時・更新日時の記録)
- `fetch = FetchType.LAZY` は「関連するデータは、使うときに初めて読む」設定。一覧で何度も読みに行かないよう、`@EntityGraph` でまとめて読んでいます

### Spring Data: メソッド名から SQL を作る

```java
// …/onboarding/repository/TaskAssignmentRepository.java
List<TaskAssignment> findByUserIdOrderByTaskSortOrderAscTaskIdAsc(Long userId);
```

メソッド名を Spring が読み解いて、次のような SQL を作ります。

```
find By UserId           OrderBy TaskSortOrder Asc  TaskId Asc
     WHERE user_id = ?   ORDER BY task.sort_order ASC, task.id ASC
```

`save`, `findById`, `findAll`, `delete` などの基本操作は、`JpaRepository` を引き継ぐだけで使えます。

### Flyway: テーブルの作り方をファイルで管理

`src/main/resources/db/migration/V1__create_tasks_table.sql` 〜 `V8__...sql` が、起動時に **番号順に 1 回だけ** 実行されます。
一度実行したファイルは書き換えず、変更は新しい番号のファイルで行います(`docs/ai/database.md`)。

---

## 21. 設定ファイル(application.yml)

```yaml
# src/main/resources/application.yml
app:
  time-zone: ${APP_TIME_ZONE:Asia/Tokyo}
```

- `${APP_TIME_ZONE:Asia/Tokyo}` は「環境変数 `APP_TIME_ZONE` があればその値、なければ `Asia/Tokyo`」
- パスワードはここに書かず、必ず環境変数で渡します(`SPRING_DATASOURCE_PASSWORD: ${...:}` は初期値なし)

Java 側では `@Value` で受け取ります。

```java
// …/onboarding/service/AccessLinkService.java
public AccessLinkService(UserRepository userRepository,
                         Clock clock,
                         @Value("${app.access-link.valid-days:60}") int validDays) {   // 文字列 "60" → int に自動変換
```

---

## 22. Maven とビルド

`pom.xml` は「このプロジェクトに必要なライブラリと、ビルドの方法」を書いたファイルです。

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

| コマンド | すること |
|---|---|
| `mvn -B test` | コンパイルしてテストを実行 |
| `mvn -B clean test` | 前回の結果(`target/`)を消してからやり直す |
| `mvn -B package` | 実行できる jar ファイル(`target/*.jar`)を作る。Docker の中でも同じことをしている |

- Java のソース(`.java`)は **コンパイル** して `.class` にしてから動きます。型の間違いなどはこの時点でエラーになります
- ライブラリはインターネットから自動でダウンロードされ、`~/.m2/` に保存されます
- `spring-boot-starter-*` は、関連するライブラリをまとめて入れてくれるセットです

---

## 23. テスト(JUnit・MockMvc・Testcontainers)

### 単体テスト(Spring なし)

```java
// src/test/java/com/example/onboarding/service/UserProgressTest.java
class UserProgressTest {

    @Test
    void percentIsRoundedDown() {
        assertThat(new UserProgress(3, 1, 0).percent()).isEqualTo(33);
        assertThat(new UserProgress(4, 2, 0).percent()).isEqualTo(50);
    }
}
```

- `@Test` を付けたメソッドが 1 つのテスト
- `assertThat(実際の値).isEqualTo(期待する値)` で確かめる(AssertJ というライブラリ)

### 画面のテスト(MockMvc)

```java
// src/test/java/com/example/onboarding/controller/TaskPageControllerTest.java
@Test
void createShowsErrorWhenTitleIsBlank() throws Exception {
    long before = taskRepository.count();                // 送る前の件数
    mockMvc.perform(post("/tasks")
                    .param("title", " ")
                    .param("description", "no title"))
            .andExpect(status().isOk())
            .andExpect(view().name("task-form"))
            .andExpect(content().string(containsString("タイトルを入力してください")));

    assertThat(taskRepository.count()).isEqualTo(before);   // 件数が変わっていない = 保存されていない
}
```

- ブラウザを使わずに、リクエストを送って結果を確かめられます
- `@Transactional` を付けたテストクラスは、テストごとに DB の変更が取り消されるので、テスト同士が影響し合いません

### サンプルデータの数字は 1 か所にまとめる(SampleData)

テストはサンプルデータ(マイグレーションで入るタスク 6 件・ユーザー 3 人など)を前提にしています。
件数や id をテストのあちこちに直接書くと、サンプルデータを 1 件増やしただけで、たくさんのテストを直すことになります
(実際に #28 でサンプルタスクを 1 件足したとき、18 件のテストを直しました)。

そこで、サンプルデータの件数・id・並び順を `SampleData` という 1 つのクラスに **定数(4 章)** としてまとめています。

```java
// src/test/java/com/example/onboarding/SampleData.java
public final class SampleData {
    public static final List<Long> TASK_IDS_IN_ORDER = List.of(1L, 2L, 3L, 4L, 5L, 6L);
    public static final int TASK_COUNT = TASK_IDS_IN_ORDER.size();
    public static final long SUZUKI = 3L;
    public static final List<Long> TASKS_ASSIGNABLE_TO_SUZUKI = List.of(3L, 4L, 6L);
    ...
}
```

```java
// テスト側
.andExpect(model().attribute("tasks", hasSize(SampleData.TASK_COUNT)))
assertThat(displayedTaskIds()).isEqualTo(TASK_IDS_IN_ORDER);
```

- **件数が変わるかどうか** は、上の例のように「前の件数 `before` と比べる」と、サンプルデータが何件でも直す必要がありません
- `SampleData` の値は、アプリのコードで計算せず、マイグレーションを見て **手で書きます**。
  アプリと同じ方法で答えを計算すると、アプリが間違っていてもテストが通ってしまうためです
- 実験として、サンプルタスクをもう 1 件足してみたところ、直したのは `SampleData.java` の 5 か所だけで、全テストが通りました
- `import static com.example.onboarding.SampleData.SUZUKI;` のように **static import** すると、`SampleData.` を省略して `SUZUKI` と書けます

### 本物の DB でテスト(Testcontainers)

テスト用の PostgreSQL を Docker で自動起動し、本番と同じ Flyway のマイグレーションとサンプルデータで動かしています
(`TestcontainersConfiguration.java`)。だから `mvn test` には Docker が必要です。

### テストが本当に役立つかを確かめる

このリポジトリでは、新しい機能のたびに「わざとコードを壊して、テストが失敗するか」を確認しています(ミューテーションテストの考え方)。
壊しても通ってしまうテストは、何も守れていないからです。

---

## 24. 練習問題

実際にコードを読んで答えてみましょう(答えはそれぞれのファイルにあります)。

1. `UserProgress` の `percent()` で、`done * 100 / total` を `done / total * 100` に変えると、`new UserProgress(3, 1, 0).percent()` はいくつになる? なぜ?(16 章 ③)
2. `TaskOrderService.move` で `.equals` を `==` に変えると、どんなときに不具合が出る?(16 章 ①)
3. `AssignmentStatus` に `ON_HOLD("保留")` を追加すると、Java 以外にどこを直す必要がある?(ヒント: `V3__...sql` の `CHECK` 制約。ただし V3 は書き換えず、新しいマイグレーションで変える。`docs/ai/database.md`)
4. `UserRepository` に「部署で絞り込んで名前順に並べる」メソッドを足すとしたら、名前はどうなる?(20 章)
5. `AccessLinkService` が `LocalDate.now()` ではなく `Instant.now(clock)` を使っているのはなぜ?(14 章)
6. `Dashboard` が class ではなく record なのはなぜ?(7 章)
7. `UserPageController` のコンストラクタには引数が 7 つある。誰がそれを渡している? 最後の `String baseUrl` はどこから来る?(18・21 章)

試すときは、作業ブランチで変更して `mvn -B test` を実行し、どのテストが失敗するか見るのがおすすめです。

---

## 25. 用語集

| 用語 | 意味 |
|---|---|
| クラス / オブジェクト | 設計図 / 設計図から `new` で作った実物 |
| フィールド / メソッド | オブジェクトが持つデータ / 振る舞い |
| コンストラクタ | オブジェクトを作るときに呼ばれる初期化処理 |
| プリミティブ型 / 参照型 | 値そのもの(`int` など、null 不可)/ 場所を指す(`Integer` など、null 可) |
| ラッパークラス | プリミティブ型に対応する参照型(`Integer`, `Long`, `Boolean`) |
| オートボクシング | `int` ↔ `Integer` などの自動変換 |
| static | オブジェクトではなくクラスに属する |
| final | 一度決めたら変えない |
| enum | 決まった選択肢を表す型 |
| record | 値を運ぶための、変更できないクラス(Java 16〜) |
| 継承(extends) | 親クラスを引き継ぐ(1 つだけ) |
| インターフェース(implements) | 「このメソッドを持つ」という約束(複数可) |
| チェック例外 | catch か throws を書かないとコンパイルできない例外 |
| ジェネリクス | `List<User>` のように中身の型を決める仕組み |
| Optional | 「ないかもしれない」値の入れ物 |
| ラムダ式 | `x -> 処理` の形の小さな関数 |
| Stream | リストを流れ作業で加工する仕組み |
| アノテーション | `@` で始まる注釈。ライブラリが読み取って動きを変える |
| Bean | Spring が作って管理するオブジェクト |
| DI(依存性の注入) | 必要な部品を Spring が自動で渡してくれる仕組み |
| JPA | Java のクラスと DB のテーブルを対応させる仕組み |
| Entity | DB の 1 行を表すクラス |
| Repository | DB の読み書きを担当する部品 |
| トランザクション | DB 操作のひとまとまり(全部成功か、全部取り消しか) |
| Maven | ライブラリの管理とビルドの道具(`pom.xml`) |
| コンパイル | `.java` を実行できる形(`.class`)に変換すること |
| JUnit | Java のテストの道具 |
