# 坐标系统重构设计文档

日期：2026-05-27
参考：GeoGebra (geogebra-desktop/shared/common)

## 一、目标

重构 FXGeometricView 的坐标轴和网格绘制系统，解决以下问题：
- 缩放时网格线闪烁/卡顿，刻度标签重叠
- 极坐标网格与笛卡尔轴混合时视觉混乱
- 等距网格缺少子网格线、与轴不对齐
- 刻度标签在边界处被截断、与网格线重叠
- 网格线和轴线颜色/粗细区分不明显
- AxesPainter（579行）和GridPainter（300行）过大，扩展困难

## 二、总体架构

```
BackgroundBuffer          ← 离屏缓存，减少重绘
CoordSystemRenderer      ← 统一编排绘制管线
├── CoordinateSystem     ← 接口，生成网格元素+刻度+轴行为
│   ├── CartesianCoordinateSystem
│   ├── PolarCoordinateSystem
│   └── IsometricCoordinateSystem
├── GridElementPainter   ← 统一绘制GridElement（支持标签避让）
├── LabelPositionTracker ← 收集标签屏幕位置
├── LabelClipper         ← 边界裁剪
├── AxesLineRenderer     ← 轴线+箭头
├── TickLineRenderer     ← 主次刻度线
├── TickLabelRenderer    ← 刻度数字标签
├── AxisLabelRenderer    ← X/Y 轴名
└── ZoomAnimation        ← 缩放动画
```

## 三、模块详细设计

### 3.1 BackgroundBuffer（离屏缓存）

- offscreenCanvas: Canvas — 离屏画布
- dirty: boolean — 脏标记
- invalidate() — 标脏
- isValid(): boolean — 是否可直接复用
- beginDraw(): GraphicsContext — 开始绘制
- endDraw(): void — 完成绘制，清除脏标记
- copyTo(Canvas target): void — 复制到目标画布

### 3.2 CoordinateSystem（坐标系统接口）

方法：
- generateGrid(transform, settings, viewWidth, viewHeight): List<GridElement>
- calculateTicks(axis, transform, settings): List<TickInfo>
- paintAxes(gc, transform, settings, width, height): void
- isAxesRatioLocked(): boolean

实现：
- CartesianCoordinateSystem — 标准笛卡尔坐标系
- PolarCoordinateSystem — 极坐标系（锁定轴比例1:1）
- IsometricCoordinateSystem — 等距网格坐标系

### 3.3 数据对象

GridElement（sealed interface）:
- GridLineSegment(Point2D start, Point2D end, boolean isSubGrid)
- GridCircle(Point2D center, double radius)
- GridDot(Point2D position)

TickInfo(record):
- double worldPos
- double screenPos
- String label
- TickType type（MAJOR / MINOR）

### 3.4 绘制管线（CoordSystemRenderer）

绘制顺序：
1. 清空 + 填充背景色
2. 子网格线（覆盖全部，不避让，颜色极淡）
3. 刻度标签（首次粗略布局，收集屏幕坐标到LabelPositionTracker）
4. 主网格线（避开标签矩形，保留4px padding）
5. 轴线 + 箭头
6. 主次刻度线
7. 刻度标签（第二次精确绘制，在网格线上方）
8. X/Y 轴标签

### 3.5 视觉模块

**LabelPositionTracker**:
- addLabel(screenX, screenY, text, font): void — 记录标签包围盒
- getAvoidIntervals(lineStart, lineEnd, clipRegion): List<Double> — 返回需要跳过的区间

**LabelClipper**:
- isNearEdge(screenX, screenY, viewWidth, viewHeight, margin): boolean

**AxesLineRenderer**（~100行）:
- drawMainAxis(gc, transform, axis, settings)
- drawBoundaryAxis(gc, transform, axis, settings)
- drawArrows(gc, transform, axis, arrowType) — 5种箭头

**TickLineRenderer**（~80行）:
- drawMajorTicks(gc, transform, ticks, settings) — 8px刻线
- drawMinorTicks(gc, transform, ticks, settings) — 4px刻线

**TickLabelRenderer**（~150行）:
- drawLabels(gc, transform, ticks, tracker, settings)
- formatNumber(value, isPiUnit): String
- computeLabelPosition(tick, transform): Point2D

**AxisLabelRenderer**（~60行）:
- drawAxisLabel(gc, label, position, settings)
- drawOriginZero(gc, transform, settings)

**GridElementPainter**（~120行）:
- drawMainGridLine(x1,y1,x2,y2)
- drawSubGridLine(x1,y1,x2,y2)
- drawCircle(cx,cy,r,type)
- drawDot(x,y)
- drawWithLabelAvoidance(elements, tracker)

### 3.6 颜色与透明度

新增属性（EuclidianViewSettings）：
- subGridOpacity: double = 0.4
- gridLineOpacity: double = 0.6
轴线始终不透明（1.0）

### 3.7 设置变更传播

```
EuclidianViewSettings.batchUpdate(Consumer<EuclidianViewSettings>):
  - 修改前快照 → 执行updater → 比较差异
  - 合并通知 SettingsChangeListener.onSettingsChanged(Set<SettingKey>)

SettingKey → 动作映射：
  GRID_TYPE / GRID_COLOR / GRID_LINE_TYPE → 重建 GridGenerator + invalidate buffer
  AXIS_TICK_STYLE / AXIS_ARROW_TYPE → invalidate buffer
  AXIS_COLOR / AXIS_LINE_TYPE → invalidate buffer
  GRID_DISTANCE_FACTOR → 重算 step + invalidate buffer
  SHOW_AXES / SHOW_GRID / SHOW_NUMBERS → invalidate buffer
```

### 3.8 动画缩放（ZoomAnimation）

- Timeline 驱动，15帧，400ms
- easeInOutCubic 缓动
- 插值：xZero, yZero, xscale, yscale
- 每帧触发 invalidate + redrawBackground

## 四、新增文件清单

```
src/main/java/com/bingbaihanji/view/layout/core/
├── BackgroundBuffer.java           (~60 行)
├── CoordSystemRenderer.java        (~100 行)
├── ZoomAnimation.java              (~80 行)

src/main/java/com/bingbaihanji/view/layout/draw/coordinate/
├── CoordinateSystem.java           (接口, ~30 行)
├── CartesianCoordinateSystem.java  (~50 行)
├── PolarCoordinateSystem.java      (~50 行)
├── IsometricCoordinateSystem.java  (~50 行)
├── GridElement.java                (sealed interface + records, ~60 行)
├── TickInfo.java                   (record, ~20 行)

src/main/java/com/bingbaihanji/view/layout/draw/coordinate/render/
├── AxesLineRenderer.java           (~100 行)
├── TickLineRenderer.java           (~80 行)
├── TickLabelRenderer.java          (~150 行)
├── AxisLabelRenderer.java          (~60 行)
├── GridElementPainter.java         (~120 行)

src/main/java/com/bingbaihanji/view/layout/draw/coordinate/grid/
├── CartesianGridGenerator.java     (~80 行)
├── PolarGridGenerator.java         (~80 行)
├── IsometricGridGenerator.java     (~80 行)
├── DotGridGenerator.java           (~30 行)

src/main/java/com/bingbaihanji/view/layout/draw/coordinate/util/
├── LabelPositionTracker.java       (~80 行)
├── LabelClipper.java               (~40 行)
```

## 五、修改文件清单

- `EuclidianViewSettings.java` — 新增 batchUpdate, listener 机制, subGridOpacity, gridLineOpacity
- `GridChartView.java` — redrawBackground() 改用 BackgroundBuffer + CoordSystemRenderer
- `WorldTransform.java` — 无结构性变更
- `GridPainter.java` — 标记 @Deprecated，内部委托给新实现
- `AxesPainter.java` — 标记 @Deprecated，内部委托给新实现

## 六、迁移步骤

每步独立可测，可回滚：

1. BackgroundBuffer — 不改 API，加缓存
2. 数据对象 — GridElement, TickInfo 定义
3. GridGenerator + GridRenderer — 策略模式
4. Axes 拆分 — AxesLine/TickLine/TickLabel/AxisLabel Renderer
5. LabelPositionTracker + LabelClipper — 位置收集
6. CoordinateSystem 接口 + 3 个实现
7. CoordSystemRenderer — 统一管线
8. 标签避让 + 边界裁剪
9. Settings 变更传播 + ZoomAnimation

## 七、测试策略

| 层级 | 内容 | 工具 |
|------|------|------|
| 单元 | TickCalculator 各 scale 下的刻度间距 | JUnit Jupiter |
| 单元 | GridGenerator 产出元素坐标范围 | JUnit Jupiter |
| 单元 | LabelPositionTracker 避让区间 | JUnit Jupiter |
| 单元 | ZoomAnimation 插值点 | JUnit Jupiter |
| 集成 | Setting变更 → buffer脏 → 重绘 | JUnit Jupiter |
| 视觉 | 固定坐标下截图对比 | 手动验证 |
