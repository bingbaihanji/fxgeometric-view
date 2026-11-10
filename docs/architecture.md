# FXGeometricView 架构说明

## 分层与依赖方向

view (渲染/视口手势)  ->  controller (交互语义/命令)  ->  util / constant / model
        |                        |
        +------------------------+--> view.layout.draw.geometry  (几何模型 + 绘制)

规则:

1. `view.layout.core` 只负责渲染、视口手势（缩放/平移/悬停/涟漪），不再承载点击语义。
2. 所有交互语义（选中、onClick、吸附、绘制工具）都在 `controller.handler` 下，通过 `canHandle(DrawMode)` 分派。
3. `util` 不得 import `controller` 或 `view`。需要上下文时，把方法搬到 `controller`。
4. `constant` / `model` 不得互相 import。
5. 新增几何图形: 继承 `AbstractWorldObject` + 增加 `ObjectType` 枚举 + 实现 `WorldObject`。
6. 新增绘制工具: 在 `controller.handler` 加一个 `AbstractDrawingHandler` 子类，并注册进 `DrawingController.registerHandlers()`。
7. 新增视口手势: 在 `view.layout.core` 加处理器，并注册进 `ViewInteractionHandler`。

## 已知债务（暂不处理，触发条件明确后再做）

| 债务 | 影响 | 触发条件 |
|---|---|---|
| `view.layout.draw.geometry` 同时承载领域模型与 JavaFX 绘制 | `util`/`io`/`factory` 反向依赖 `view`，核心算法无法脱离 JavaFX 单测 | 做序列化格式升级 或 多窗口状态隔离时，整体迁移到 `com.bingbaihanji.geometry` |
| `GridChartView` 665 行、`DrawingController` 约 40 个公开方法 | 职责偏多，新增功能容易继续往上堆 | 再次出现"往 GridChartView 加字段"的改动时，先拆 `ViewportController` 之外的职责 |
| `AbstractWorldObject` 25 个可变字段 | 胖基类，所有图形继承全套属性 | 出现第三种样式组合需求时，抽 `ShapeStyle`/`LabelStyle` 值对象 |
| 预览有 `PreviewManager.Previewable` 与 `DrawingHandler.paintPreview` 两套机制 | 新工具不知道用哪套 | 下一次新增需要预览的绘制工具时，二选一后删掉另一套 |
| 全局单例 `ConfigManager` / `PointNameManager` / `I18nContext` | 多窗口与单测隔离困难 | 做多文档窗口时改为实例注入 |

## 已删除的历史包袱

- `strategy` 包（5 个类）: 从未接线的吸附策略，真实逻辑在 `SnapCalculator`。
- `AxesPainter` / `GridPainter`: 已被 `CoordSystemRenderer` 取代。
- `CircleDrawingTool`: 从未被鼠标事件驱动。
- `PerformanceMonitor` / `BoundingBoxTransform`: 零引用。
- `ClickHandler` + `GridChartView.drawModeActive`: 点击语义已下沉到 `controller.handler.ObjectClickHandler`。
