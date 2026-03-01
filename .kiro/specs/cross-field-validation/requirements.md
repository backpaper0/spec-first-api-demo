# Requirements Document

## Project Description (Input)
相関バリデーション部品を追加

追加する部品は次の2つ。

- 2つの日付フィールドに対する前後関係のバリデーション
- 特定のフィールドに値が入っている場合、必須チェックを行う

### 2つの日付フィールドに対する前後関係のバリデーション

日付は文字列(uuuu/MM/dd)で表現される。

実装イメージは次の通り:

```java
@DateRange(from = "dateFrom", to = "dateTo")
public class Example {
    private String dateFrom;
    private String dateTo;
```

アノテーション等の名前は仮であり、より良い名前があれば提案してほしい。

### 特定のフィールドに値が入っている場合、必須チェックを行う

実装イメージは次の通り:

```java
public class Example {
    private String dateFrom;
    @RequiredIf(value = "dateFrom")
    private String dateTo;
```

これは`dateFrom`に値が入っている場合、`dateTo`が必須であることを示している。

アノテーション等の名前は仮であり、より良い名前があれば提案してほしい。

## Requirements
<!-- Will be generated in /kiro:spec-requirements phase -->


