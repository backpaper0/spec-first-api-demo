# spec-first-api-demo

[cc-sdd](https://github.com/gotalab/cc-sdd) と Claude Code の Plan モードを組み合わせた **仕様駆動開発（Spec-Driven Development）** のデモリポジトリ。

AI-DLC（AI Development Life Cycle）の実践として、Kiro スタイルの Spec-Driven Development をどのように Spring Boot アプリケーション開発へ適用するかを示す。

---

## 概要

このリポジトリは「仕様をコードより先に書く」開発スタイルのリファレンス実装です。

- **3 フェーズ承認ワークフロー**: 要件 → 設計 → タスク → 実装の順に人間レビューを挟む
- **品質ゲートの自動化**: Spotless・Checkstyle・SpotBugs・JaCoCo をビルドに組み込み、コード品質を継続的に保証
- **Steering によるプロジェクト知識管理**: `.kiro/steering/` に蓄積したルールで AI の一貫性を維持

---

## 技術スタック

| カテゴリ | 採用技術 |
|---|---|
| 言語 | Java 21 |
| フレームワーク | Spring Boot 3.5.11（Web, Validation） |
| ビルド | Maven Wrapper (`./mvnw`) |
| フォーマット | Spotless + Google Java Format（AOSP スタイル） |
| 静的解析 | Checkstyle / SpotBugs + FindSecBugs |
| カバレッジ | JaCoCo（クラス単位で行カバレッジ 80% 以上） |

---

## 前提条件

- [mise](https://mise.jdx.dev/) がインストール済みであること（Java/Maven のバージョン管理）
- または Java 21 と Maven が別途用意されていること

```bash
# mise を使う場合
mise install
```

---

## クイックスタート

```bash
# テスト実行（カバレッジ含む）
./mvnw test

# フォーマット適用
./mvnw spotless:apply

# 全検証（format check + static analysis + test）
./mvnw verify
```

---

## プロジェクト構成

```
.
├── .kiro/
│   ├── steering/          # プロジェクト全体のルールと文脈（AI への常時参照）
│   │   ├── product.md     # プロダクト概要・目的
│   │   ├── tech.md        # 技術スタック・開発標準
│   │   └── structure.md   # パッケージ構成・命名規則
│   └── specs/             # 機能ごとの仕様（要件・設計・タスク）
│       └── {feature}/
│           ├── requirements.md
│           ├── design.md
│           └── tasks.md
├── src/
│   ├── main/java/com/example/api/
│   │   ├── common/        # 横断的な共通処理（バリデーション等）
│   │   └── features/      # 機能ごとのパッケージ（フィーチャーファースト）
│   └── test/              # ソースと同じパッケージ構成
├── config/checkstyle/     # Checkstyle 設定
├── CLAUDE.md              # AI への開発ガイドライン
└── pom.xml
```

---

## 開発事例

### PR #1 – 相関バリデーション部品の追加（cc-sdd による仕様駆動開発）

[Pull Request #1](https://github.com/backpaper0/spec-first-api-demo/pull/1)

cc-sdd の 3 フェーズ承認ワークフロー（要件 → 設計 → タスク → 実装）を使って開発した事例。

- `@RequiredWhen`：特定フィールドの値に応じて他フィールドを必須化するカスタムアノテーション
- `@ValidDateRange`：日付範囲の前後関係（開始日 ≤ 終了日）を検証するカスタムアノテーション
- 仕様: `.kiro/specs/cross-field-validation/`

### PR #2 – @RequiredWhen.dependsOn への Spring EL 式サポート追加（Plan モードによる開発）

[Pull Request #2](https://github.com/backpaper0/spec-first-api-demo/pull/2)

Claude Code の Plan モードを用いて設計・実装した事例。

- `dependsOn` に Spring Expression Language (SpEL) 式を渡せるよう `RequiredWhenValidator` を拡張
- Java 識別子であれば従来のフィールド名モード（後方互換）、それ以外は SpEL 式として評価
- `PrivateFieldPropertyAccessor` を追加し、private フィールドへの参照を可能化

