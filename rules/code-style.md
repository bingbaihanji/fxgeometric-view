
# Java 代码风格规范 (Code Style Guide)

> 适用范围：Java 8 及以上项目  

---

## 1. 源文件基础

### 1.1 文件名
- 源文件以 `.java` 结尾，文件名**必须**与其中唯一 `public` 类的类名完全一致（区分大小写）。
- 每个文件只包含一个顶层 `public` 类，允许存在包级私有辅助类，但应尽量避免。

### 1.2 文件编码
- 所有源文件统一使用 **UTF-8** 编码。

### 1.3 空白字符
- 仅使用**空格**进行缩进，禁止使用制表符（Tab）。
- 行尾不应有空白字符。
- 文件末尾保留**一个空行**。

### 1.4 特殊转义字符
- 优先使用 `\n`、`\t` 等转义字符，而非八进制或 Unicode 转义；非 ASCII 字符在注释或字符串中可直接书写（文件为 UTF-8）。

---

## 2. 格式规范

### 2.1 大括号
遵循 **K&R 风格**：左大括号不另起新行，右大括号单独占一行，并在其后换行（除非紧跟 `else`、`catch` 等）。

```java
//  正确
if (condition) {
    doSomething();
} else {
    doOther();
}

// 错误
if (condition)
{
    doSomething();
}
```

- 即使只有一条语句，`if`/`for`/`while` 也**必须**使用大括号，不可省略。

### 2.2 缩进与块
- 每增加一级代码块，缩进 **4 个空格**（不勾选 Tab）。
- `switch` 中 `case` 与 `default` 缩进一层，其内部语句再缩进一层。

```java
switch (value) {
    case 1:
        handleOne();
        break;
    case 2:
        handleTwo();
        break;
    default:
        handleDefault();
}
```

### 2.3 每行长度
- 单行最大长度建议 **120 字符**，超出时应换行。
- 长方法调用在参数前换行，操作符在行首换行时放在新行首。

```java
SomeVeryLongClass.someVeryLongMethod(
    longParamOne,
    longParamTwo,
    longParamThree);
```

### 2.4 空格
- 关键字与其后括号之间加空格（`if (`、`for (`、`while (`、`switch (`）。
- 保留 `{` 前一个空格。
- 二元/三元运算符两侧加空格，一元运算符不加。
- 逗号、分号、冒号后加空格，之前不加。
- 方法声明参数列表的 `(` 和方法名之间**不加空格**；方法调用的 `(` 与方法名之间也不加空格。

```java
// 正确
int result = a + b * c;
if (flag) { ... }
someObject.someMethod(arg1, arg2);

// 错误
int result=a+b*c;
if(flag){...}
someObject.someMethod (arg1, arg2);
```

### 2.5 空行
- 类成员（字段、构造器、方法、内部类）之间使用一个空行分隔。
- 方法体内，按逻辑段落使用空行增加可读性，避免连续多行空白。

---

## 3. 命名规范

| 标识符类型           | 风格                   | 示例                          |
|----------------------|------------------------|-------------------------------|
| 包名 (package)       | 全小写，点分隔          | `com.example.myapp`           |
| 类名 (class/interface/enum/record) | 大驼峰（PascalCase）     | `UserService`, `OrderStatus`  |
| 方法名               | 小驼峰（camelCase）     | `getUserName`, `findById`     |
| 变量名               | 小驼峰                  | `orderList`, `maxRetryCount`  |
| 常量名 (static final) | 全大写，下划线分隔      | `MAX_SIZE`, `DEFAULT_PORT`    |
| 枚举常量             | 全大写，下划线分隔      | `READY`, `RUNNING`            |
| 泛型类型参数         | 单个大写字母或 `T` 后缀 | `E`, `T`, `R`, `TRequest`     |

- 命名应简洁且有意含，避免单字母变量（循环控制变量 `i`, `j`, `k` 除外）。
- 禁止使用拼音或拼音缩写，也避免中英混合。

---

## 4. 注释规范

### 4.1 Javadoc
- 所有 `public` 及 `protected` 的类、接口、方法、字段**必须**编写 Javadoc（简单 字段 方法 getter/setter 可酌情省略但需保持清晰）。
- Javadoc 格式：`/** ... */`，每行以 `*` 开头，第一句为概要。
- `@param`、`@return`、`@throws` 等标签顺序固定，且描述要完整。
- 简单的方法(三个参数及以下)的简单方法可以使用 "//" 进行简单的行注释

```java
/**
 * Calculates total price after applying discount.
 *
 * @param basePrice original price
 * @param discountRate discount rate between 0 and 1
 * @return final price after discount
 * @throws IllegalArgumentException if discountRate is out of range
 */
public double calculateFinalPrice(double basePrice, double discountRate) {
    ...
}
```

### 4.2 块注释与行注释
- 实现层解释使用 `//` 行注释或 `/* ... */` 块注释，说明“为什么这样做”而非“做了什么”。
- 注释应与代码保持同步更新，过时注释比没有注释更糟糕。

### 4.3 TODO 标记
- 使用标准大写 `TODO`，后跟责任人/日期及说明，例如：`// TODO(zhangsan): 优化查询逻辑，考虑分页 2026-05-23`。

---

## 5. 编程实践

### 5.1 字段声明
- 每个字段独立声明一行，禁止 `int a, b;` 这种一行多变量。
- 访问修饰符顺序：`public` / `protected` / `private`，然后是 `static`，然后是 `final` 等。

### 5.2 数组声明
- 数组的方括号紧贴类型，不贴变量：`String[] args`，而非 `String args[]`。

### 5.3 switch 语句
- 必须包含 `default` 分支，即使什么也不做，也要注释说明。
- 不要从一个 `case` 贯穿到下一个（fall-through），如需共享逻辑使用显式分组，或加注释 `// fall through`。

### 5.4 导入（import）
- 不使用通配符导入（如 `import java.util.*`），必须具体列出。
- 导入按分组排序：静态导入 > 第三方包 > `java.*` > `javax.*`，每组之间空行分隔。
- 未使用的导入必须删除。

### 5.5 静态成员与访问
- 直接通过类名访问静态成员，而非实例引用。
- 能用局部变量就不要重复调用 `getter`，保证清晰且微小性能友好。

### 5.6 异常处理
- 不允许捕获后不做任何处理（至少记录日志并向上抛或明确注释说明原因）。
- 不应捕捉 `Throwable` 或 `Error`，除非极特殊情况并附详细注释。

### 5.7 重写方法标注
- 所有重写的方法必须使用 `@Override` 注解。

### 5.8 Lambda 表达式
- 短小的 lambda 可单行书写；参数需加括号（即使单参数也无类型推断需求时可不加，但建议统一风格：单参数不用括号，多参数用括号。团队统一即可）。
- 多行 lambda 体必须用大括号。

```java
list.forEach(item -> process(item));
list.forEach(item -> {
    if (item.isValid()) {
        process(item);
    }
});
```

### 5.9 常量与魔术数字
- 魔法数值（除 0、1、-1 等特例外）均应定义为常量，并赋予有意义的名称。

---

## 6. 可空性处理（推荐）
- 公共 API 返回或参数接受对象时，需明确是否接受 `null`，并在 Javadoc 中声明。
- 推荐使用 `Optional` 作为可能为空的返回值类型，而不是直接返回 `null`。
- 使用 `Objects.requireNonNull()` 对关键参数进行前置校验。

---

## 7. 测试风格（补充建议）
- 测试类命名：被测试类名 + `Test`，如 `CalculatorTest`。
- 测试方法命名：`methodName_condition_expectedBehavior` 或行为描述，如 `sum_negativeNumbers_throwsException`。
- 善用断言，单个测试方法验证一个概念。

---

 