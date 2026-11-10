# FXGeometricView 架构说明

## 分层

```
presentation   view        渲染、视口手势、菜单与对话框
application    controller  交互语义、命令历史、绘制工具责任链
domain         geometry    几何模型、约束、交点、吸附算法、坐标变换
support        util        通用工具 (I18n、数学、样式)
               constant / model / config
```

依赖方向: `view -> controller -> geometry -> util -> constant / config`。

## 规则

1. `geometry` 是领域层: 几何接口与实现、约束、交点、吸附、`WorldTransform` 都在这里, 不依赖 `view` / `controller`。
2. `view.layout.core` 只负责渲染与视口手势, 不承载点击语义。
3. 交互语义(选中、onClick、吸附、绘制工具)都在 `controller.handler`, 由 `canHandle(DrawMode)` 分派。
4. `util` 只能依赖 `config` / `constant`, 不得 import `view` / `controller` / `geometry`。
5. `constant` 不得 import `model`; `constant` 与 `util` 之间的现有环属已知债务, 不要加深。
6. 新增几何图形: 在 `geometry.impl` 继承 `AbstractWorldObject`, 增加 `ObjectType` 枚举。
7. 新增绘制工具: 在 `controller.handler` 加一个 `AbstractDrawingHandler` 子类, 并注册进 `DrawingController.registerHandlers()`。
8. 新增视口手势: 在 `view.layout.core` 加处理器, 并注册进 `ViewInteractionHandler`。

## 仍存在的依赖环

- `view <-> controller`: `view.menu` / `InitView` / `DetachedCanvasWindow` 依赖 controller, controller 又依赖 `view.layout.core` 与 `view.menu`。
- `view <-> io`: `ProjectFileManager` 直接接收 `GridChartView`, 而 view 的菜单调用 io 保存/加载。
- `constant <-> util`: 6 个枚举引用 `I18nUtil`, util 又引用 `constant`。

## 已知债务（暂不处理，触发条件明确后再做）

| 债务 | 影响 | 触发条件 |
|---|---|---|
| `view <-> controller` / `view <-> io` 两个环 | 分层不严格, 单测仍难隔离 | 做序列化格式升级或多窗口状态隔离时, 让 io 只接收对象集合与设置而非 `GridChartView` |
| `GridChartView` 约 680 行、`DrawingController` 约 40 个公开方法 | 职责偏多, 新增功能容易继续往上堆 | 再次出现"往 GridChartView 加字段"的改动时, 先拆职责 |
| `AbstractWorldObject` 25 个可变字段 | 胖基类, 所有图形继承全套属性 | 出现第三种样式组合需求时, 抽 `ShapeStyle`/`LabelStyle` 值对象 |
| 预览有 `PreviewManager.Previewable` 与 `DrawingHandler.paintPreview` 两套机制 | 新工具不知道用哪套 | 下一次新增需要预览的绘制工具时, 二选一后删掉另一套 |
| `GridChartView.painters` 扩展点已无任何实现者与调用者 | 死扩展点, 增加阅读成本 | 下次改动 `redrawBackground()` 时一并删除 |
| 点击语义(`ObjectClickHandler`)无自动化覆盖 | 等价性只能靠源码对照 | 下次改动点击链路前, 先补一个用伪造 `IDrawingContext` 的 handler 级测试 |
| 全局单例 `ConfigManager` / `PointNameManager` / `I18nContext` | 多窗口与单测隔离困难 | 做多文档窗口时改为实例注入 |

## 已删除 / 已迁移的历史包袱

- `strategy` 包（5 个类）: 从未接线的吸附策略，真实逻辑在 `SnapCalculator`。
- `AxesPainter` / `GridPainter`: 已被 `CoordSystemRenderer` 取代。
- `CircleDrawingTool`: 从未被鼠标事件驱动。
- `PerformanceMonitor` / `BoundingBoxTransform` / `SnapController` / `GeometryCache` / `LabelClipper` / `LabelPositionTracker`: 零引用。
- `ClickHandler` + `GridChartView.drawModeActive`: 点击语义已下沉到 `controller.handler.ObjectClickHandler`。
- `ZoomAnimation`: 原本零引用, 已接线到 `ViewportController` 的缩放/自适应/重置。
- `view.layout.draw.geometry` + `util` 中的领域算法: 已统一迁入 `com.bingbaihanji.geometry`。
