# Project Structure

## Organization Philosophy

**フィーチャーファースト**パッケージ構成。機能（feature）を最上位の軸とし、その配下にレイヤー（controller, service, repository など）を置く。横断的な共通処理のみ `common` または `shared` パッケージへ切り出す。

## Directory Patterns

### Feature Package
**Location**: `src/main/java/com/example/api/features/{feature-name}/{layer}/`
**Purpose**: 1機能に関するすべてのクラスをグループ化する
**Example**: `features/helloworld/controller/HelloController.java`

### Test Mirror
**Location**: `src/test/java/com/example/api/features/{feature-name}/{layer}/`
**Purpose**: ソースと同じパッケージ構成でテストを配置
**Example**: `features/helloworld/controller/HelloControllerTest.java`

## Naming Conventions

- **クラス名**: PascalCase（`HelloController`, `UserService`）
- **テストクラス**: `{TargetClass}Test`（例: `HelloControllerTest`）
- **テストメソッド**: `{methodName}_should{ExpectedBehavior}()`（例: `get_shouldReturnHelloWorldMessage`）
- **パッケージ名**: すべて小文字、単語区切りなし

## Code Organization Principles

- Controllerは`@RestController` + `@RequestMapping`でルートパスを宣言
- 各Controllerのテストは`@WebMvcTest(TargetController.class)`でスライステスト
- importはワイルドカード禁止（`AvoidStarImport`）、staticインポートはテストのアサーション用のみ
- ビジネスロジックはServiceへ委譲し、ControllerはHTTPの入出力変換のみを担う

## Spec-Driven Workflow

仕様は `.kiro/specs/{feature-name}/` に格納。機能実装の前に`requirements.md` → `design.md` → `tasks.md` の順に人間レビューを経ること。

---
_Document patterns, not file trees. New files following patterns shouldn't require updates_
