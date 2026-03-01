# リサーチ & 設計決定ノート

---

## サマリー

- **フィーチャー**: `cross-field-validation`
- **ディスカバリースコープ**: Extension（既存 Spring Boot アプリへの追加）
- **主要な知見**:
  - `spring-boot-starter-validation` は pom.xml に既に存在するため、追加ライブラリ不要
  - フィールドレベル `ConstraintValidator` は自フィールド値のみ受け取るため、クロスフィールドバリデーションにはクラスレベル制約が必要
  - `common/validation` パッケージを新設することで steering の「横断的関心事は common へ」方針に準拠できる

---

## リサーチログ

### Jakarta Bean Validation におけるクロスフィールドバリデーションの制約

- **コンテキスト**: 要件では `@RequiredWhen` をフィールドレベルアノテーションとして定義していたが、実装可能性の検討が必要
- **ソース**: Jakarta Bean Validation 3.0 仕様、Hibernate Validator 8.x ドキュメント
- **調査結果**:
  - `ConstraintValidator<A, T>` の `isValid(T value, ...)` は、フィールドレベル使用時に当該フィールドの値のみ受け取る
  - 他フィールドへのアクセスは仕様上不可能（ `ConstraintValidatorContext` は含有 Bean を提供しない）
  - クラスレベル制約 (`@Target(ElementType.TYPE)`) では Bean 全体を受け取れる
  - Hibernate Validator 固有の拡張（`HibernateConstraintValidatorContext`）でも含有 Bean へのアクセス手段はない
- **影響**: `@RequiredWhen` をクラスレベルアノテーションとして再設計する必要がある

### `spring-boot-starter-validation` の内容確認

- **コンテキスト**: 追加ライブラリが必要かどうかの確認
- **ソース**: pom.xml（既存）
- **調査結果**:
  - `spring-boot-starter-validation` が依存に存在する → Hibernate Validator 8.x が推移的依存として含まれる
  - `jakarta.validation-api` も含まれる
  - 追加依存なしで `ConstraintValidator` を実装できる
- **影響**: 設計変更不要。既存スタックで全要件を満たせる

### Java リフレクションとモジュールシステム

- **コンテキスト**: `field.setAccessible(true)` を使ったフィールド値取得のJava 21での動作
- **調査結果**:
  - 本プロジェクトは `module-info.java` なし（unnamed module）
  - unnamed module では `setAccessible(true)` は引き続き動作する（JDK 17+ のStrong Encapsulationは named module 間のみ適用）
  - `private` フィールドへのアクセスも問題なし
- **影響**: リフレクション使用に制限なし

### DateTimeFormatter での `uuuu/MM/dd` パターン

- **コンテキスト**: 日付フォーマットの正確なパース動作の確認
- **調査結果**:
  - `uuuu` は ISO プロレプティック暦の年フィールド（`yyyy` との違いは紀元前の扱い）
  - 現代日付では `uuuu` と `yyyy` は同値
  - `DateTimeFormatter.ofPattern("uuuu/MM/dd").withResolverStyle(ResolverStyle.STRICT)` でパース
  - `STRICT` モードで月日の範囲外値（例: `2024/02/30`）を拒否できる
  - `DateTimeParseException` がスローされた場合 → バリデーションエラー（要件 1.5）
- **影響**: `ValidDateRangeValidator` で `ResolverStyle.STRICT` を必須とする

---

## アーキテクチャパターン評価

| オプション | 説明 | 強み | リスク / 制限 | 備考 |
|---|---|---|---|---|
| クラスレベル制約（採用） | `@Target(TYPE)` + `ConstraintValidator<A, Object>` | 標準 Jakarta BV 準拠、含有 Bean へ直接アクセス可能 | アノテーションをクラスに付与する必要あり（フィールドより冗長） | Hibernate Validator が推奨するクロスフィールドバリデーションの正規パターン |
| フィールドレベル制約 | `@Target(FIELD)` + `ConstraintValidator<A, String>` | 直感的な API | 他フィールドへのアクセス不可（技術的に実現不能） | 棄却 |
| AOP / Spring インターセプター | `@Aspect` でバリデーションロジックを横断的に挿入 | 柔軟性高い | Bean Validation のエラー伝播と統合が複雑、過剰設計 | 棄却 |

---

## 設計決定

### 決定: `@RequiredWhen` をクラスレベルアノテーションとして再設計

- **コンテキスト**: 要件 2.4 はフィールドレベル適用を示唆していたが、技術的制約により不可能
- **検討した代替案**:
  1. フィールドレベルアノテーション — Jakarta BV の仕様上実現不能
  2. Hibernate Validator 独自拡張 — ポータビリティ低下、公式 API ではない
- **選択したアプローチ**: クラスレベルアノテーション `@RequiredWhen(field = "dateTo", dependsOn = "dateFrom")`
- **根拠**: 標準 Jakarta Bean Validation に完全準拠。Spring Boot が採用する Hibernate Validator のベストプラクティスに沿う。
- **トレードオフ**: フィールドに直接付与できないため、やや冗長。ただし意図が明示的になり可読性は同等。
- **フォローアップ**: `Repeatable` アノテーション（`@RequiredWhen.List`）を用意し、同一クラスへの複数指定を可能にする

### 決定: `common/validation` パッケージを新設

- **コンテキスト**: バリデーション部品は特定フィーチャーに属さない横断的関心事
- **選択したアプローチ**: `src/main/java/com/example/api/common/validation/`
- **根拠**: structure.md「横断的な共通処理のみ common または shared パッケージへ切り出す」方針に従う
- **トレードオフ**: なし（パッケージ新設のみ）

### 決定: ConstraintViolation をフィールドに関連付ける

- **コンテキスト**: クラスレベル制約のデフォルトではバリデーションエラーがクラス自体に紐付く。Spring MVC の `BindingResult` でフィールドエラーとして扱うにはフィールドノードの指定が必要。
- **選択したアプローチ**: `context.buildConstraintViolationWithTemplate(message).addPropertyNode(fieldName).addConstraintViolation()` を使用
- **根拠**: クライアントが受け取る 400 エラーレスポンスで、どのフィールドが問題かを明示できる
- **`@ValidDateRange`**: 違反を `to` フィールドに関連付ける（範囲の終端）
- **`@RequiredWhen`**: 違反を `field` 属性で指定されたフィールドに関連付ける

---

## リスクと軽減策

- `field`/`from`/`to`/`dependsOn` に存在しないフィールド名を指定 → `ValidationException` をスロー（フェイルファスト）。テストで検出可能。
- `String` 以外の型フィールドを `field` 属性に指定 → `ClassCastException`。仕様上 String のみサポートと明示し、非 String 型は `ValidationException` でフェイルファスト。
- `ResolverStyle.STRICT` を忘れると `2024/02/30` 等を通過させてしまう → 単体テストで境界値を必ず検証。

---

## 参考

- [Jakarta Bean Validation 3.0 仕様](https://jakarta.ee/specifications/bean-validation/3.0/jakarta-bean-validation-spec-3.0.html) — クラスレベル制約の定義
- [Hibernate Validator 公式ガイド: Cross-field constraints](https://docs.jboss.org/hibernate/validator/8.0/reference/en-US/html_single/#section-class-level-constraints) — クロスフィールドバリデーションの推奨実装パターン
