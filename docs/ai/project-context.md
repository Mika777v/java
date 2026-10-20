# プロジェクト概要(AI 向け)

> このフォルダ(`docs/ai/`)は Claude / Copilot などの AI に作業を頼むときの前提知識です。
> 作業を始める前に、このファイルと関係するファイルを読んでください。
> 人間向けの詳しい説明は `docs/current-status.md` と `onboarding-progress-system/README.md` にあります。

## 何を作っているか

**オンボーディング進捗管理システム**(`onboarding-progress-system/`)。
新入社員に「入社時にやること(タスク)」を割り当て、進み具合を管理者が確認する Web アプリです。

| 役割 | できること |
|---|---|
| 管理者(`ADMIN`) | ダッシュボード、タスクの登録・並べ替え、ユーザー登録、タスクの割り当て、進捗の変更、REST API |
| 新入社員(`MEMBER`) | マイページ(`/me`)で自分のタスクの状態を変更するだけ。管理者が発行した **専用URL** を開いてログインする(パスワードなし。設定でパスワード方式にも切り替え可) |

画面の文言・メッセージはすべて日本語です。コード中の識別子とコメントは英語です。

## 技術スタック

| 分類 | 内容 |
|---|---|
| 言語 | Java 21 |
| フレームワーク | Spring Boot 3.3.4(Web / Data JPA / Validation / Security / Thymeleaf) |
| 画面 | Thymeleaf + `thymeleaf-extras-springsecurity6`、CSS は `static/css/app.css` 1 ファイル(JS フレームワークなし) |
| DB | PostgreSQL 15、マイグレーションは Flyway(`db/migration/V*.sql`) |
| ビルド | Maven(`pom.xml`)。Maven Wrapper はない(`mvn` を直接使う) |
| テスト | JUnit 5、MockMvc、spring-security-test、Testcontainers 1.21.4(PostgreSQL を Docker で起動) |
| 実行環境 | Docker Compose(`app` + `db`)、GitHub Codespaces、GitHub Actions |

## リポジトリ構成(要点だけ)

```
/                                  … ルート(学習用リポジトリ。サブプロジェクトは 1 つ)
├─ CLAUDE.md                       … Claude Code が自動で読む入口(AI 開発運用ルールと docs/ai/ への案内)
├─ DEVLOG.md                       … 作業ログ(タスクごとに追記)
├─ SESSION_SUMMARY.md              … 次のセッションへの引き継ぎ
├─ .claude/skills/                 … Claude Code のスキル(現状調査・設計レビュー・機能追加・リファクタリング)
├─ .devcontainer/                  … Codespaces 設定。起動時に start-app.sh がアプリを立ち上げる
├─ .github/copilot-instructions.md … GitHub Copilot が自動で読む入口(docs/ai/ への案内)
├─ .github/workflows/
│   ├─ onboarding-progress-system-ci.yml   … PR・master への push で実行(test ジョブ + smoke 呼び出し。ドキュメントだけの PR はスキップ)
│   └─ reusable-docker-smoke.yml           … Docker で起動して curl で画面・API を確認
├─ docs/
│   ├─ TASKS.md                    … 今のタスクの一覧(進行中・次にやる・判断待ち)
│   ├─ user-guide.md               … 利用ガイド(管理者・新入社員の使い方、できること / できないこと)
│   ├─ current-status.md           … 人間向けの現状レポート(機能・画面・URL の一覧)
│   ├─ codespaces-check.md         … Codespaces での動作確認手順
│   ├─ java-guide.md               … オーナー向けの Java 教科書
│   ├─ requirements.md             … 要件定義書(人向け)
│   ├─ design.md                   … 設計書(人向け)
│   ├─ design-diagrams.md          … 図と定義表(状態遷移図・画面遷移図・ER 図など。Mermaid)
│   └─ ai/                         … このフォルダ
└─ onboarding-progress-system/     … アプリ本体(Maven プロジェクト。コマンドはここで実行)
    ├─ Dockerfile, docker-compose.yml, docker-compose.codespaces.yml
    └─ src/main/java/com/example/onboarding/…   … 詳細は architecture.md
```

## 開発の進め方(ユーザーとの約束)

このプロジェクトのオーナー(プロダクトオーナー)は Java と Docker の学習中です。次のルールを必ず守ってください。
作業の進め方(タスク実行フロー・確認が必要な作業・PR 運用)は **`CLAUDE.md` の「AI 開発運用ルール」** が正本です。

1. **`master` に直接コミットしない。** 作業ブランチで変更し、完了したら PR まで作る。マージはオーナーが行う
   (デフォルトブランチは `main` ではなく **`master`**)
2. **パスワードや認証情報をコード・ログ・ドキュメントに書かない。** 環境変数や GitHub Secrets で渡す。
   CI では毎回ランダムなパスワードを作り、`::add-mask::` で隠している
3. **修正はタスクの範囲だけ。** 関係ない整形やリファクタリングを混ぜない
4. **DB スキーマ・API 互換性・認証認可・ライブラリの大幅変更・インフラ構成・データ削除・仕様変更は、先に「原因・方針」を短く説明して了承をもらう。**
   それ以外(バグ修正・リファクタリング・テスト・CI・ドキュメントなど)は確認なしで進めてよい
5. **説明は専門用語を使いすぎず、日本語で。** 「なぜそうするか」を一言添える
6. オーナーはローカルで Docker / git を使わない。**確認は GitHub Actions と Codespaces で行う**。
   そのため、AI 側で push 前にテストとスモークテスト相当の確認を済ませておく

## よく使うコマンド

```bash
cd onboarding-progress-system

mvn -B test                      # 全テスト(Docker が必要。Testcontainers が PostgreSQL を起動)
mvn -B clean test                # テンプレートを変えたのに結果が変わらないときは clean する
mvn -B test -Dtest=SecurityTest  # 1 クラスだけ

# Docker で起動(パスワードは任意の値を自分で決める。コミットしない)
export POSTGRES_PASSWORD=...  APP_ADMIN_PASSWORD=...
docker compose up -d --build
docker compose logs --tail 100 app
docker compose down -v           # DB も消して最初からやり直す
```

Codespaces では `.devcontainer/start-app.sh` が自動で起動し、`.env`(git 管理外)にパスワードを作って、
管理者のログイン情報をターミナルに表示します。

## 環境変数

| 変数 | 必須 | 用途 |
|---|---|---|
| `POSTGRES_PASSWORD` | Docker 起動時は必須 | DB パスワード(`SPRING_DATASOURCE_PASSWORD` としてアプリに渡る) |
| `APP_ADMIN_PASSWORD` | Docker 起動時は必須 | 管理者が 1 人もいないとき、起動時に作る管理者のパスワード |
| `APP_ADMIN_EMAIL` | 任意 | 上記管理者のメールアドレス(既定 `admin@example.com`) |
| `APP_ADMIN_PASSWORD_RESET` | 任意 | `true` で起動すると `APP_ADMIN_EMAIL` の管理者のパスワードを `APP_ADMIN_PASSWORD` に戻す(管理者が 1 人で忘れたとき用。使ったら外す) |
| `APP_ADMIN_NAME` | 任意 | 上記管理者の名前(既定 `管理者`) |
| `APP_MEMBER_LOGIN` | 任意 | 新入社員のログイン方法。`link`(既定。専用URL)または `password` |
| `APP_ACCESS_LINK_VALID_DAYS` | 任意 | 専用URLの有効日数(既定 60) |
| `APP_BASE_URL` | 任意 | 専用URLに表示するアプリのアドレス。空なら管理者のブラウザが使ったアドレス。Codespaces では start-app.sh が自動設定 |
| `APP_TIME_ZONE` | 任意 | 「今日」や完了日時の表示に使うタイムゾーン(既定 `Asia/Tokyo`) |
| `SPRING_DATASOURCE_URL` | 任意 | 既定 `jdbc:postgresql://localhost:5432/onboarding_db` |
| `SPRING_JPA_SHOW_SQL` | 任意 | `true` で実行した SQL をログに出す(既定 `false`。学習・調査用) |
| `SPRING_FLYWAY_BASELINE_ON_MIGRATE` | 任意 | 既定 `false`。Flyway の履歴がない既存 DB に途中から導入するときだけ一時的に `true` |

## 関連ドキュメント

| ファイル | 内容 |
|---|---|
| [spec.md](spec.md) | 要件 ID(FR / NFR)、壊してはいけないルール(INV)、要件と実装・テストの対応、設計判断の記録。**機能を変える前に読む** |
| [architecture.md](architecture.md) | レイヤー構成、クラスの役割、セキュリティ、URL 一覧 |
| [database.md](database.md) | テーブル定義、マイグレーション、サンプルデータ |
| [conventions.md](conventions.md) | コーディング・テスト・コミット・PR の決まりごと |
| [backlog.md](backlog.md) | 未実装の機能、既知の課題 |
| [../codespaces-check.md](../codespaces-check.md) | Codespaces での動作確認手順(オーナーが PR マージ後に使う。画面や機能を変えたらチェックリストも更新する) |
| [../java-guide.md](../java-guide.md) | オーナー向けの Java 教科書(このリポジトリのコードが教材)。説明するときに章を案内すると伝わりやすい。コードの例を変えたら合わせて更新する |
| [prompt-templates.md](prompt-templates.md) | AI に頼むときの依頼文のひな形(スキルの呼び方も) |

## Claude Code のスキル(`.claude/skills/`)

| スキル | 使う場面 | 変更するもの |
|---|---|---|
| `project-memory-manager` | 現状調査、`current-status.md` とロードマップ(`backlog.md`)の更新、技術的負債と次のタスクの提案。**実装の前に必ず** | ドキュメントだけ |
| `spring-boot-architect` | Entity・Service・API・Flyway・レイヤー構造のレビュー | なし(報告だけ) |
| `feature-builder` | 新機能の実装(要件分析 → 方針の確認 → 実装 → テスト → ドキュメント → 要約) | コード・テスト・ドキュメント |
| `review-and-refactor` | 動きを変えないコード整理(重複・Service 層・命名・性能)。理由を説明する | コード(了承を得てから) |

おすすめの順番: `project-memory-manager` → `spring-boot-architect` → `feature-builder` / `review-and-refactor` → `project-memory-manager`(ドキュメントの更新)
