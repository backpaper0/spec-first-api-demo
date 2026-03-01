# Technology Stack

## Architecture

Spring MVCを用いたシンプルなRESTful APIサーバー。レイヤーはControllerを中心に、必要に応じてService・Repositoryを追加するMVC構成。

## Core Technologies

- **Language**: Java 21
- **Framework**: Spring Boot 3.5.11（Web, Validation）
- **Build**: Maven Wrapper（`./mvnw`）
- **Tool Manager**: mise（Java/Mavenのバージョン管理）

## Development Standards

### Code Formatting
Google Java Format（AOSPスタイル）をSpotlessで自動適用。`process-sources`フェーズで自動フォーマット、`verify`フェーズで検証。

```java
// インデント: 4スペース
// 最大行長: 120文字
// ワイルドカードimport禁止
```

### Static Analysis
- **Checkstyle**: インデント・空白・import規約を強制
- **SpotBugs + FindSecBugs**: バグパターンとセキュリティ脆弱性を検出（Effortレベル: Max）

### Testing
- JUnit 5 + MockMvc（`@WebMvcTest`でControllerを単体テスト）
- **JaCoCoカバレッジ要件**: クラス単位で行カバレッジ80%以上（エントリーポイントクラスは除外）

## Common Commands

```bash
# フォーマット適用
./mvnw spotless:apply

# テスト実行（カバレッジ含む）
./mvnw test

# 全検証（format check + static analysis + test）
./mvnw verify
```

## Key Technical Decisions

- **Spotless auto-apply**: 手動フォーマット不要、ビルドで自動適用することでスタイル議論をゼロに
- **SpotBugs Threshold: Low**: セキュリティインシデントを早期に検出するため高感度設定
- **JaCoCo 80% per class**: ファイル全体の集計ではなくクラス単位で品質を保証

---
_Document standards and patterns, not every dependency_
