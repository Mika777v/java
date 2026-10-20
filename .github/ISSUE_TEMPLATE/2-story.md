---
name: Story
about: 1 つの PR で終える単位。Task はチェックリストで書く
title: "[S?.?] "
labels: ["type:story"]
---

## 目的

<!-- 何のために、何を作るか(親の Epic も書く) -->

## やること

- [ ] T?.?.1

## 完了の条件

- 
- `mvn -B test` が通り、CI(`test`・`call-reusable / run-smoke`)が緑
- `DEVLOG.md`・`SESSION_SUMMARY.md`・`docs/TASKS.md` を更新

## 依存

<!-- 先に Done になっている必要がある Story。なければ「—」 -->

## 事前確認

<!-- DB・認証・データ削除・仕様変更などがあれば書き、ラベル needs-approval を付ける。了承されたら approved に替える -->
