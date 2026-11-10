# FXGeometricView 架构说明

## 分层

```
root         com.bingbaihanji      组合根: FXGeometricApplication / FXGeometricView / InitView
presentation view                  渲染、视口手势、独立窗口、属性对话框
application  controller (.menu)    交互语义、命令历史、绘制工具责任链、动作型右键菜单
domain       geometry              几何模型、约束、交点、吸附、坐标变换
support      util / constant / model / config / i18n / io / factory
```

依赖方向: `root -> controller -> view -> geometry -> util -> constant -> i18n`。

当前**没有任何循环依赖**，包依赖图是有向无环图。以下包为叶子侧，只被上层依赖:
`config`、`i18n`、`constant`、`model`、`io`、`factory`。

## 规则

1. `geometry` 是领域层: 几何接口与实现、约束、交点、吸附、`WorldTransform` 都在这里, 不依赖 `view` / `controller`。
2. `view` 不得 import `controller`。需要驱动控制器时, 通过 `view` 包内的接口(如 `DrawingWindowController`)由组合根注入。
3. `view.layout.core` 只负责渲染与视口手势, 不承载点击语义。
4. 交互语义(选中、onClick、吸附、绘制工具)都在 `controller.handler`, 由 `canHandle(DrawMode)` 分派。
5. 直接操作绘制模型的菜单(撤销、层级、增删对象)放在 `controller.menu`; 纯展示的对话框留在 `view.menu`。
6. `util` 只能依赖 `config` / `constant`, 不得 import `view` / `controller` / `geometry`。
7. `constant` 不得 import `model`。
8. 新增几何图形: 在 `geometry.impl` 继承 `AbstractWorldObject`, 增加 `ObjectType` 枚举。
9. 新增绘制工具: 在 `controller.handler` 加一个 `AbstractDrawingHandler` 子类, 并注册进 `DrawingController.registerHandlers()`。
10. 新增视口手势: 在 `view.layout.core` 加处理器, 并注册进 `ViewInteractionHandler`。

## 已知债务（暂不处理，触发条件明确后再做）

| 债务 | 影响 | 触发条件 |
|---|---|---|
| `GridChartView` 约 660 行、`DrawingController` 约 40 个公开方法 | 职责偏多, 新增功能容易继续往上堆 | 再次出现"往 GridChartView 加字段"的改动时, 先拆职责 |
| `AbstractWorldObject` 25 个可变字段 | 胖基类, 所有图形继承全套属性 | 出现第三种样式组合需求时, 抽 `ShapeStyle`/`LabelStyle` 值对象 |
| 点击语义(`ObjectClickHandler`)无自动化覆盖 | 等价性只能靠源码对照 | 下次改动点击链路前, 先补一个用伪造 `IDrawingContext` 的 handler 级测试 |
| 全局单例 `ConfigManager` / `PointNameManager` / `I18nContext` | 多窗口与单测隔离困难 | 做多文档窗口时改为实例注入 |
| `constant` 的枚举内嵌展示文案并调用 `I18nUtil` | 常量层依赖 i18n, 严格说是层内泄漏 | 需要非 UI 场景复用枚举时, 改为暴露 key 由 UI 解析 |

## 已删除 / 已迁移的历史包袱

- `strategy` 包（5 个类）: 从未接线的吸附策略，真实逻辑在 `SnapCalculator`。
- `AxesPainter` / `GridPainter`: 已被 `CoordSystemRenderer` 取代。
- `CircleDrawingTool`: 从未被鼠标事件驱动。
- `PerformanceMonitor` / `BoundingBoxTransform` / `SnapController` / `GeometryCache` / `LabelClipper` / `LabelPositionTracker`: 零引用。
- `ClickHandler` + `GridChartView.drawModeActive`: 点击语义已下沉到 `controller.handler.ObjectClickHandler`。
- `ZoomAnimation`: 原本零引用, 已接线到 `ViewportController` 的缩放/自适应/重置。
- `view.layout.draw.geometry` + `util` 中的领域算法: 已统一迁入 `com.bingbaihanji.geometry`。
- `I18nUtil` / `I18nContext` / `I18nRefreshable`: 已抽成独立的 `com.bingbaihanji.i18n`。
- `GridChartView.painters` 扩展点: 无实现者, 已删除。
- `PreviewManager`(636 行): 两套预览机制已合并, 所有预览由各 Handler 在 `paintPreview` 中自绘。
- `SnapCalculator`: 从 controller 迁入 `view.layout.core`。
- 动作型右键菜单 / 组合根: 分别迁入 `controller.menu` 与基包 `com.bingbaihanji`。
