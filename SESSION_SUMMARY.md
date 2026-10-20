# SESSION_SUMMARY

次のセッションへの引き継ぎです。セッションの終わりに書き換えます(運用ルールは `CLAUDE.md`)。

最終更新: 2026-10-20

## 今の状態

- `master` は PR #42(S0.1 Feature Toggle の仕組み)まで反映済み。自動テスト 236 件
- S3.2・S10.1・S10.4 への回答の記録は、#42 のマージ後に push したため別の PR(マージ待ち)
- 移行計画への回答: Q1・Q2・Q4・Q6・Q7 は OK、Q5 は「今のまま + 4 ロールは Toggle」、Q3 は論理削除。PRD 10 章の質問はすべて回答済み

## 直前の作業

- S0.1: `feature` パッケージ(`Feature`・`FeatureFlags`・`@RequiresFeature`・`FeatureToggleInterceptor`)と `FEATURE_*` の設定。まだ使う画面はない
- Q3(論理削除)の記録を入れ直した(#41 のマージ後に push したため master に入っていなかった)

## 次の一手

- オーナーの了承があれば、GitHub 上に Issue(Epic・Story)・ラベル・マイルストーンを作る(Projects のボードと項目はオーナーが画面で作る)
- 次の実装は S0.2(`TaskService`)、S0.3(`UserService`)
- S1.1(`tickets` テーブル)からは画面に `@RequiresFeature(Feature.TICKET_SYSTEM)` を付けて作る

## 未解決・オーナーの判断待ち

- 移行計画の事前確認はすべて了承済み(S3.2・S10.1・S10.4 も 2026-10-20)。S10.4(旧コードの削除)だけは、移行後にオーナーの指示を待って着手
- `docs/TASKS.md` の「オーナーの判断待ち」

## 注意点

- デフォルトブランチは `master`。作業ブランチ `claude/github-actions-docker-error-06b46h` は、PR がマージされるたびに最新の `master` から作り直す
- `GITHUB_ISSUES_TICKET_SYSTEM.md` の 3 章以降は `TASKS_TICKET_SYSTEM.md` から作った。Story を変えるときは先に `TASKS_TICKET_SYSTEM.md` を直す
- この作業環境からは Debian のパッケージサーバーと GitHub のパッケージ(`pkg-containers.githubusercontent.com`)につながらない
