# 测试规范指南

## 1. 概述

本文档定义了项目的统一测试规范，包括测试依赖管理、测试框架使用、测试编写规范等，旨在确保项目中所有测试的一致性和质量。

## 2. 测试依赖管理

### 2.1 测试依赖版本统一

项目使用 `gradle/libs.versions.toml` 文件统一管理所有测试依赖的版本。所有模块应使用此文件中定义的版本，避免硬编码版本号。

### 2.2 常用测试依赖

| 依赖名称 | 版本 | 用途 | 使用方式 |
|---------|------|------|----------|
| JUnit 4 | 4.13.2 | 基础单元测试框架 | `testImplementation(libs.junit)` |
| JUnit 5 | 5.10.1 | 现代单元测试框架 | `testImplementation(libs.junit.jupiter.api)` |
| Mockito | 5.10.0 |  mocking 框架 | `testImplementation(libs.mockito.core)` |
| Mockito Kotlin | 5.3.1 | Mockito 的 Kotlin 扩展 | `testImplementation(libs.mockito.kotlin)` |
| Truth | 1.4.4 | 断言库 | `testImplementation(libs.truth)` |
| Kotlin Coroutines Test | 1.8.1 | 协程测试支持 | `testImplementation(libs.kotlinx.coroutines.test)` |
| AndroidX Test | 1.2.1 | Android 测试框架 | `androidTestImplementation(libs.androidx.junit)` |
| Espresso | 3.6.1 | Android UI 测试框架 | `androidTestImplementation(libs.androidx.espresso.core)` |

## 3. 测试框架选择

### 3.1 单元测试

- **推荐使用 JUnit 5**：现代化的测试框架，支持参数化测试、嵌套测试等高级特性
- **兼容 JUnit 4**：对于现有测试，允许继续使用 JUnit 4
- **使用 Truth 进行断言**：提供更流畅的断言语法
- **使用 Mockito 进行 mocking**：支持复杂对象的 mocking

### 3.2 Android 测试

- **使用 AndroidX Test 框架**：提供 Android 特定的测试支持
- **使用 Espresso 进行 UI 测试**：自动化 UI 交互测试
- **使用 Mockito Android 进行 Android 组件 mocking**：支持 Android 特定组件的 mocking

## 4. 测试编写规范

### 4.1 测试类命名

- 测试类名应与被测试类名一致，并以 `Test` 结尾
- 示例：`CalculatorTest` 测试 `Calculator` 类

### 4.2 测试方法命名

- 测试方法名应清晰描述测试场景和预期结果
- 使用驼峰命名法
- 示例：`testAdditionWithPositiveNumbers`、`testSubtractionWithNegativeNumbers`

### 4.3 测试方法结构

每个测试方法应遵循 **AAA（Arrange-Act-Assert）** 模式：

```kotlin
@Test
fun testAddition() {
    // Arrange - 设置测试环境
    val calculator = Calculator()
    
    // Act - 执行被测试的操作
    val result = calculator.add(2, 3)
    
    // Assert - 验证结果
    assertEquals(5, result, "2 + 3 应该等于 5")
}
```

### 4.4 使用显示名称

为测试方法添加有意义的显示名称，提高测试报告的可读性：

```kotlin
@Test
@DisplayName("测试加法运算")
fun testAddition() {
    // 测试代码
}
```

### 4.5 参数化测试

对于相同逻辑的多个测试用例，使用参数化测试：

```kotlin
@ParameterizedTest
@CsvSource(
    "1, 1, 2",
    "2, 3, 5",
    "-1, 1, 0"
)
@DisplayName("参数化测试 - 加法运算")
fun testAdditionWithParameters(a: Int, b: Int, expected: Int) {
    val calculator = Calculator()
    val result = calculator.add(a, b)
    assertEquals(expected, result, "$a + $b 应该等于 $expected")
}
```

### 4.6 Mocking 规范

- 使用 `mock()` 函数创建 mock 对象
- 使用 `when()` 设置 mock 对象的行为
- 使用 `verify()` 验证 mock 对象的方法调用

```kotlin
@Test
fun testWithMocking() {
    // 创建 mock 对象
    val service = mock(Service::class.java)
    
    // 设置 mock 行为
    `when`(service.getValue()).thenReturn(42)
    
    // 使用 mock 对象
    val result = calculator.useService(service)
    
    // 验证结果
    assertEquals(42, result)
    
    // 验证方法调用
    verify(service).getValue()
}
```

## 5. 测试覆盖率

- **目标覆盖率**：核心业务逻辑代码覆盖率应达到 80% 以上
- **使用 Jacoco 生成覆盖率报告**：运行 `./gradlew jacocoTestReport` 生成覆盖率报告
- **覆盖率报告位置**：`build/reports/jacoco/test/html/index.html`

## 6. 测试执行

### 6.1 运行单元测试

```bash
# 运行单个模块的单元测试
./gradlew :module:test

# 运行所有模块的单元测试
./gradlew test
```

### 6.2 运行 Android 测试

```bash
# 运行单个模块的 Android 测试
./gradlew :module:connectedAndroidTest

# 运行所有模块的 Android 测试
./gradlew connectedAndroidTest
```

## 7. 测试示例

### 7.1 JUnit 5 示例

请参考 `Sample/Test/src/test/java/com/cn/sample/test/ExampleUnitTest5.kt` 文件，该文件展示了如何使用 JUnit 5、Truth 和 Mockito 编写规范的测试代码。

### 7.2 Android 测试示例

```kotlin
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.cn.sample.test", appContext.packageName)
    }
}
```

## 8. 最佳实践

1. **测试应该是独立的**：每个测试方法应独立运行，不依赖其他测试方法的状态
2. **测试应该是可重复的**：多次运行相同的测试应得到相同的结果
3. **测试应该是快速的**：避免在测试中进行耗时操作（如网络请求、数据库操作等）
4. **测试应该是清晰的**：测试代码应易于理解，便于维护
5. **测试应该覆盖边界情况**：除了正常情况，还应测试边界值、异常情况等
6. **测试应该测试单一功能**：每个测试方法应只测试一个功能点

## 9. 代码审查要点

在审查测试代码时，应重点关注以下几点：

1. 是否遵循了 AAA 模式
2. 测试方法名是否清晰描述了测试场景
3. 是否使用了适当的断言库
4. 是否使用了适当的 mocking 技术
5. 是否覆盖了边界情况和异常情况
6. 测试是否独立、可重复、快速
7. 是否使用了统一的测试依赖版本

## 10. 总结

遵循统一的测试规范有助于提高测试的质量和可维护性，减少测试代码的重复，提高团队的测试效率。请所有团队成员严格遵循本规范编写测试代码。