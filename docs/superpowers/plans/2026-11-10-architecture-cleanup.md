# FXGeometricView 架构治理实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 清掉 1500+ 行死代码、把点击语义从视图层下沉到控制器并删除 `drawModeActive` 跨层标志、去掉两处包依赖环，让后续加交互/加图形的改动不再牵一发动全身。

**Architecture:** 只做"删除"和"搬移"，不新增抽象层、不引入框架。视图层负责渲染与视口手势，控制器层负责交互语义；`util` 只留真正通用的工具。已知的深层问题（`view.layout` 同时承载领域模型）**明确不在本次范围内**，写入文档作为后续触发式债务。

**Tech Stack:** Java 17、JavaFX、Maven、SLF4J。

**验证口径:** 每步以 `mvn -q compile` 通过为准；不新增任何测试类（测试由你按业务流程人工验证），已有的 33 个用例必须仍然全绿。

---

## 文件结构

| 动作 | 文件 | 职责 |
|---|---|---|
| 删除 | `strategy/`（5 个）、`AxesPainter`、`GridPainter`、`CircleDrawingTool`、`PerformanceMonitor`、`BoundingBoxTransform` | 零引用的死代码 |
| 新建 | `controller/handler/ObjectClickHandler.java` | 非绘制模式下的点击语义（吸附 → 命中 → onClick） |
| 删除 | `view/layout/core/ClickHandler.java` | 职责迁到上面那个类 |
| 修改 | `view/layout/core/GridChartView.java` | 移除 drawModeActive / CircleDrawingTool，暴露 SnapCalculator |
| 修改 | `view/layout/core/ViewInteractionHandler.java` | 五个处理器减为四个 |
| 修改 | `controller/DrawingController.java` | 分发去重、注册新 Handler、删掉 setDrawModeActive |
| 搬移 | `util/GeometryCommand` → `controller/`；`util/ConstraintUtils` → `controller/` | 打断 util → controller |
| 搬移 | `model/FunctionParameter` → `constant/` | 打断 constant ↔ model |
| 新建 | `docs/architecture.md` | 分层规约与债务清单 |

---

## Task 1: 删除未接线的策略包与两个旧绘制器

**Files:**
- Delete: `src/main/java/com/bingbaihanji/strategy/SnapStrategy.java`
- Delete: `src/main/java/com/bingbaihanji/strategy/CompositeSnapStrategy.java`
- Delete: `src/main/java/com/bingbaihanji/strategy/EdgeSnapStrategy.java`
- Delete: `src/main/java/com/bingbaihanji/strategy/GridSnapStrategy.java`
- Delete: `src/main/java/com/bingbaihanji/strategy/PointSnapStrategy.java`
- Delete: `src/main/java/com/bingbaihanji/view/layout/draw/geometry/impl/AxesPainter.java`
- Delete: `src/main/java/com/bingbaihanji/view/layout/draw/geometry/impl/GridPainter.java`

**依据:** `strategy` 包的 `SnapStrategy` 全部引用都在包内，包外零引用；真实吸附逻辑在 `SnapCalculator` 里已有一份。`AxesPainter`/`GridPainter` 只剩自身构造器引用，已被 `CoordSystemRenderer` 取代。

- [ ] **Step 1: 删除文件**

```bash
cd /d/bingbaihanji/FXGeometricView
git rm -r src/main/java/com/bingbaihanji/strategy
git rm src/main/java/com/bingbaihanji/view/layout/draw/geometry/impl/AxesPainter.java
git rm src/main/java/com/bingbaihanji/view/layout/draw/geometry/impl/GridPainter.java
```

- [ ] **Step 2: 编译自检**

```bash
mvn -q compile
```

Expected: 无输出（BUILD SUCCESS）。若报 `找不到符号 AxesPainter/GridPainter`，说明还有隐藏引用，用
`grep -rn "AxesPainter\|GridPainter" src/main/java` 定位后再处理。

- [ ] **Step 3: 提交**

```bash
git add -A
git commit -m "chore: 删除未接线的 strategy 包与已被 CoordSystemRenderer 取代的 AxesPainter/GridPainter

Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

## Task 2: 删除零引用工具类与 CircleDrawingTool 接线

**Files:**
- Delete: `src/main/java/com/bingbaihanji/util/PerformanceMonitor.java`
- Delete: `src/main/java/com/bingbaihanji/util/BoundingBoxTransform.java`
- Delete: `src/main/java/com/bingbaihanji/view/layout/draw/tools/CircleDrawingTool.java`
- Modify: `src/main/java/com/bingbaihanji/view/layout/core/GridChartView.java:12,40-41,120,340-342`

**依据:** `CircleDrawingTool` 被 new 出来、每帧调用 `paintPreview`，但 `onMouseClicked/onMouseMoved` 无人调用，永远画空。

**注意:** `view/layout/draw/tools/` 不是空包，同目录的 `FreehandDrawingTool` 正被 `FreehandHandler` 使用，
所以只删单个文件，不能删目录，也不要动 `module-info` 里对该包的 export。

- [ ] **Step 1: 删除三个文件**

```bash
cd /d/bingbaihanji/FXGeometricView
git rm src/main/java/com/bingbaihanji/util/PerformanceMonitor.java
git rm src/main/java/com/bingbaihanji/util/BoundingBoxTransform.java
git rm src/main/java/com/bingbaihanji/view/layout/draw/tools/CircleDrawingTool.java
```

- [ ] **Step 2: 摘掉 GridChartView 里的接线**

删除第 12 行的 import：

```java
import com.bingbaihanji.view.layout.draw.tools.CircleDrawingTool;
```

删除第 40 行的字段：

```java
    private final CircleDrawingTool circleTool;
```

删除构造函数第 120 行：

```java
        circleTool = new CircleDrawingTool();
```

删除 `redrawInteraction()` 中第 340-342 行：

```java
        if (circleTool != null) {
            circleTool.paintPreview(gc, transform);
        }
```

- [ ] **Step 3: 编译自检**

```bash
mvn -q compile
```

Expected: 无输出。

- [ ] **Step 4: 提交**

```bash
git add -A
git commit -m "chore: 删除零引用工具类与永远空绘制的 CircleDrawingTool

Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

## Task 3: 依赖声明与模块描述对齐

**Files:**
- Modify: `pom.xml`（移除 javafx-fxml）
- Modify: `src/main/java/module-info.java`

**依据:** 源码中 `FXMLLoader` 与 `.fxml` 零引用，但 pom 与 module-info 都声明了它。

**注意:** `joml` 虽然源码零引用，但 pom 里带注释"后续可能会添加3D坐标系"，属于你有意留的占位，本次不动。

- [ ] **Step 1: 删除 pom 中的 javafx-fxml 依赖块**

删除 `pom.xml` 中这一整块：

```xml
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-fxml</artifactId>
            <version>${javafx.version}</version>
        </dependency>
```

- [ ] **Step 2: 同步 module-info**

`src/main/java/module-info.java` 中删除：

```java
    requires javafx.fxml;
```

保留 `requires javafx.swing;`（`SwingFXUtils` 有 2 处使用）与 `requires commons.math3;`（3 处使用）。

- [ ] **Step 3: 编译自检**

```bash
mvn -q compile
```

Expected: 无输出。若报模块找不到，检查是否有任何 FXML 加载代码（`grep -rn "FXMLLoader" src/main/java` 应为空）。

- [ ] **Step 4: 提交**

```bash
git add pom.xml src/main/java/module-info.java
git commit -m "chore: 移除未使用的 javafx-fxml 依赖并同步 module-info

Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

## Task 4: 点击语义下沉到控制器，删除 drawModeActive

**Files:**
- Create: `src/main/java/com/bingbaihanji/controller/handler/ObjectClickHandler.java`
- Delete: `src/main/java/com/bingbaihanji/view/layout/core/ClickHandler.java`
- Modify: `src/main/java/com/bingbaihanji/controller/IDrawingContext.java`
- Modify: `src/main/java/com/bingbaihanji/controller/DrawingContext.java`
- Modify: `src/main/java/com/bingbaihanji/controller/DrawingController.java:54,192`
- Modify: `src/main/java/com/bingbaihanji/view/layout/core/ViewInteractionHandler.java`
- Modify: `src/main/java/com/bingbaihanji/view/layout/core/GridChartView.java:74-81,597-609`

**原理:** `drawModeActive` 存在的唯一原因，是视图层的 `ClickHandler` 不知道控制器当前处于什么模式。把点击语义搬进控制器后，`canHandle(DrawMode.NONE)` 天然完成隔离，标志可以彻底删除。

- [ ] **Step 1: 给上下文补一个 SnapCalculator 出口**

`IDrawingContext.java` 在"画布操作"区追加：

```java
    /**
     * 获取吸附计算器
     */
    SnapCalculator getSnapCalculator();
```

`DrawingContext.java` 实现：

```java
    /**
     * 获取吸附计算器，由画布持有
     */
    @Override
    public SnapCalculator getSnapCalculator() {
        return gridChartPane.getSnapCalculator();
    }
```

- [ ] **Step 2: GridChartView 暴露吸附计算器**

在 `getTransform()` 附近追加：

```java
    /**
     * 获取吸附计算器
     */
    public SnapCalculator getSnapCalculator() {
        return snapCalculator;
    }
```

- [ ] **Step 3: 新建 ObjectClickHandler**

```java
package com.bingbaihanji.controller.handler;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.constant.DrawMode;
import com.bingbaihanji.controller.IDrawingContext;
import com.bingbaihanji.controller.SnapCalculator;
import com.bingbaihanji.view.layout.core.GridChartView;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import javafx.scene.input.MouseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 对象点击处理器
 * <p>
 * 非绘制模式下的点击语义: 吸附计算 -> 命中测试 -> 分派 onClick 或记录坐标。
 * 绘制模式由 canHandle 天然隔离, 因此不再需要视图层的 drawModeActive 标志。
 *
 * @author bingbaihanji
 * @date 2026-11-10
 */
public class ObjectClickHandler extends AbstractDrawingHandler {

    private static final Logger log = LoggerFactory.getLogger(ObjectClickHandler.class);

    @Override
    public boolean canHandle(DrawMode mode) {
        // 只在非绘制模式下接管点击语义
        return mode == DrawMode.NONE;
    }

    @Override
    public boolean handleMouseClicked(MouseEvent e, IDrawingContext context) {
        GridChartView view = context.getGridChartPane();
        WorldTransform transform = context.getTransform();
        SnapCalculator snapCalculator = context.getSnapCalculator();

        // 屏幕坐标转世界坐标
        double worldX = view.screenToWorldX(e.getX());
        double worldY = view.screenToWorldY(e.getY());

        // 吸附计算并把吸附提示状态写回视图
        SnapCalculator.SnapResult snapped = snapCalculator.calculate(worldX, worldY, view.getObjects());
        view.setNearbySpecialPoint(snapped.snappedPoint);
        view.setAxisSnapInfo(snapped.axisSnapInfo);
        worldX = snapped.x;
        worldY = snapped.y;

        // 由上到下做命中测试，命中最上层对象即分派
        double tolerance = GeometryConfig.Tolerance.HIT_TEST_PIXELS / transform.getScale();
        List<WorldObject> objects = view.getObjects();
        for (int i = objects.size() - 1; i >= 0; i--) {
            WorldObject obj = objects.get(i);
            if (obj.hitTest(worldX, worldY, tolerance)) {
                obj.onClick(worldX, worldY);
                context.redraw();
                return true;
            }
        }

        // 未命中任何对象时记录坐标
        log.info("point(x = {}, y = {})", String.format("%.2f", worldX), String.format("%.2f", worldY));
        return false;
    }
}
```

- [ ] **Step 4: 在控制器里注册它**

`DrawingController.registerHandlers()` 末尾追加一行（放在最后即可，其它 NONE 模式 Handler 都不实现 clicked）：

```java
        handlers.add(new ObjectClickHandler());
```

- [ ] **Step 5: 视图层摘掉 ClickHandler**

删除 `src/main/java/com/bingbaihanji/view/layout/core/ClickHandler.java`：

```bash
git rm src/main/java/com/bingbaihanji/view/layout/core/ClickHandler.java
```

`ViewInteractionHandler.java` 改为四个处理器（完整替换文件内容）：

```java
package com.bingbaihanji.view.layout.core;

import com.bingbaihanji.controller.SnapCalculator;

/**
 * 视图交互处理器(协调器)
 * <p>
 * 协调四个视口级子处理器: 缩放、平移、悬停、点击涟漪。
 * 点击语义(选中/onClick)属于控制器层, 由 ObjectClickHandler 负责。
 */
public class ViewInteractionHandler {

    private final ZoomHandler zoomHandler;
    private final PanHandler panHandler;
    private final HoverHandler hoverHandler;
    private final RippleHandler rippleHandler;

    public ViewInteractionHandler(GridChartView view, WorldTransform transform,
                                  SnapCalculator snapCalculator, HoverTooltipManager tooltipManager) {
        this.zoomHandler = new ZoomHandler(view, transform, tooltipManager);
        this.panHandler = new PanHandler(view, transform, tooltipManager);
        this.hoverHandler = new HoverHandler(view, transform, snapCalculator);
        this.rippleHandler = new RippleHandler(view);
    }

    public void install() {
        zoomHandler.install();
        panHandler.install();
        hoverHandler.install();
        rippleHandler.install();
    }
}
```

- [ ] **Step 6: 删除 drawModeActive**

`GridChartView.java` 删除字段（第 74-81 行整段）：

```java
    /**
     * 绘制模式激活标识
     * <p>
     * 当 DrawingController 处于绘制模式时设为 true，
     * ClickHandler 检查此标识以跳过 onClick 分派，避免与 DrawingController 事件处理器冲突。
     * </p>
     */
    private boolean drawModeActive = false;
```

删除第 597-609 行整段：

```java
    /**
     * 获取绘制模式激活状态（供 ClickHandler 查询）
     */
    public boolean isDrawModeActive() {
        return drawModeActive;
    }

    /**
     * 设置绘制模式激活状态（由 DrawingController.setDrawMode 调用）
     */
    public void setDrawModeActive(boolean drawModeActive) {
        this.drawModeActive = drawModeActive;
    }
```

`DrawingController.setDrawMode()` 删除这一行：

```java
        // 通知 GridChartView 当前是否在绘制模式，防止 ClickHandler 冗余分派 onClick
        context.getGridChartPane().setDrawModeActive(mode != DrawMode.NONE);
```

- [ ] **Step 7: 编译自检**

```bash
mvn -q compile
```

Expected: 无输出。
再确认标志确实清干净：`grep -rn "drawModeActive" src/main/java` 应无结果。

- [ ] **Step 8: 人工冒烟（启动应用）**

```bash
mvn javafx:run
```

按业务流程检查：非绘制模式下点击图形仍能选中并触发 `onClick`、点击空白仍打印坐标、切换到任意绘制模式后点击不再选中图形。

- [ ] **Step 9: 提交**

```bash
git add -A
git commit -m "refactor: 点击语义下沉到控制器 ObjectClickHandler, 删除跨层标志 drawModeActive

Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

## Task 5: DrawingController 事件分发去重

**Files:**
- Modify: `src/main/java/com/bingbaihanji/controller/DrawingController.java:5,64-165,276-283`

**依据:** `handleMouseClicked/Moved/Pressed/Dragged/Released` 五段代码完全相同，共约 60 行；`showRegularPolygonDialog` 用 `instanceof` 下转型从列表里捞 Handler。

- [ ] **Step 1: 换成通配符导入**

第 5 行 `import com.bingbaihanji.controller.handler.*;` 替换为显式导入（规范禁止通配符导入）。
`DrawingController` 用到的 Handler 共 14 个，一个都不能漏：

```java
import com.bingbaihanji.controller.handler.BasicShapeHandler;
import com.bingbaihanji.controller.handler.ConstraintHandler;
import com.bingbaihanji.controller.handler.ConstructionToolHandler;
import com.bingbaihanji.controller.handler.DragHandler;
import com.bingbaihanji.controller.handler.DrawingHandler;
import com.bingbaihanji.controller.handler.FreehandHandler;
import com.bingbaihanji.controller.handler.FunctionHandler;
import com.bingbaihanji.controller.handler.IntersectionHandler;
import com.bingbaihanji.controller.handler.ObjectClickHandler;
import com.bingbaihanji.controller.handler.PolygonHandler;
import com.bingbaihanji.controller.handler.RegularPolygonHandler;
import com.bingbaihanji.controller.handler.RotationHandler;
import com.bingbaihanji.controller.handler.SelectionHandler;
import com.bingbaihanji.controller.handler.SnappingHandler;
```

- [ ] **Step 2: 记住 RegularPolygonHandler 实例**

字段区追加：

```java
    /**
     * 正多边形处理器，供菜单直接调用其对话框
     */
    private RegularPolygonHandler regularPolygonHandler;
```

`registerHandlers()` 中把

```java
        handlers.add(new RegularPolygonHandler());
```

改为

```java
        regularPolygonHandler = new RegularPolygonHandler();
        handlers.add(regularPolygonHandler);
```

`showRegularPolygonDialog()`（第 276-283 行）整体替换为：

```java
    public void showRegularPolygonDialog() {
        regularPolygonHandler.showSidesSelectionDialog();
    }
```

- [ ] **Step 3: 五个分发方法合并**

把第 93-165 行的五个 `handleMouseXxx` 全部替换为：

```java
    /**
     * 鼠标事件分派动作
     */
    @FunctionalInterface
    private interface MouseEventAction {

        /**
         * 把事件交给指定的 Handler 方法
         *
         * @param handler 目标 Handler
         * @param event   鼠标事件
         * @param context 绘制上下文
         * @return true 表示已处理
         */
        boolean handle(DrawingHandler handler, MouseEvent event, IDrawingContext context);
    }

    /**
     * 按注册顺序把事件交给第一个能处理当前绘制模式的 Handler
     *
     * @param event  鼠标事件
     * @param action 分派动作
     */
    private void dispatch(MouseEvent event, MouseEventAction action) {
        try {
            for (DrawingHandler handler : handlers) {
                if (handler.canHandle(context.getDrawMode()) && action.handle(handler, event, context)) {
                    return;
                }
            }
        } catch (Exception ex) {
            logger.error("分发鼠标事件时发生错误", ex);
        }
    }

    public void handleMouseClicked(MouseEvent e) {
        dispatch(e, DrawingHandler::handleMouseClicked);
    }

    public void handleMouseMoved(MouseEvent e) {
        dispatch(e, DrawingHandler::handleMouseMoved);
    }

    public void handleMousePressed(MouseEvent e) {
        dispatch(e, DrawingHandler::handleMousePressed);
    }

    public void handleMouseDragged(MouseEvent e) {
        dispatch(e, DrawingHandler::handleMouseDragged);
    }

    public void handleMouseReleased(MouseEvent e) {
        dispatch(e, DrawingHandler::handleMouseReleased);
    }
```

`handleKeyPressed` 保持原样（签名不同，且没有 try/catch），不要改。

- [ ] **Step 4: 编译自检**

```bash
mvn -q compile
```

Expected: 无输出。

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/bingbaihanji/controller/DrawingController.java
git commit -m "refactor: 合并 DrawingController 五份重复的事件分发, Handler 改为按字段查找

Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

## Task 6: util 中的控制器依赖上移

**Files:**
- Move: `src/main/java/com/bingbaihanji/util/GeometryCommand.java` → `src/main/java/com/bingbaihanji/controller/GeometryCommand.java`
- Move: `src/main/java/com/bingbaihanji/util/ConstraintUtils.java` → `src/main/java/com/bingbaihanji/controller/ConstraintUtils.java`
- Modify: `controller/handler/BasicShapeHandler.java`、`ConstructionToolHandler.java`、`FreehandHandler.java`、`PolygonHandler.java`、`RegularPolygonHandler.java`
- Modify: `view/menu/PointMenuBuilder.java:79,89,205,215`

**原理:** 这两个类都用 `IDrawingContext`，本质是控制器层 Helper，被放在 util 造成了 `util → controller` 的反向依赖。

- [ ] **Step 1: 搬移两个文件**

```bash
cd /d/bingbaihanji/FXGeometricView
git mv src/main/java/com/bingbaihanji/util/GeometryCommand.java src/main/java/com/bingbaihanji/controller/GeometryCommand.java
git mv src/main/java/com/bingbaihanji/util/ConstraintUtils.java src/main/java/com/bingbaihanji/controller/ConstraintUtils.java
```

- [ ] **Step 2: 改包声明**

两个文件的 `package com.bingbaihanji.util;` 改为 `package com.bingbaihanji.controller;`。

`ConstraintUtils.java` 中删除已同包的 import：

```java
import com.bingbaihanji.controller.DrawingController;
import com.bingbaihanji.controller.IDrawingContext;
```

`GeometryCommand.java` 中同样删除：

```java
import com.bingbaihanji.controller.IDrawingContext;
```

- [ ] **Step 3: 修掉 ConstraintUtils 的未使用参数与过窄类型**

`findNearbyConstrainableShapes` 的 controller 参数从未被使用，删掉：

```java
    /**
     * 查找附近可以作为约束对象的几何图形
     */
    public static List<WorldObject> findNearbyConstrainableShapes(
            PointGeo point, GridChartView canvas) {
```

`addConstraintToPoint` 只用到 context，参数类型收窄：

```java
    /**
     * 为点添加约束
     */
    public static void addConstraintToPoint(PointGeo point, WorldObject shape,
                                            IDrawingContext context, GridChartView canvas) {
        try {
            PointConstraint constraint = context.getConstraintHandler().createConstraint(shape, point);
```

并删除方法体内原来的这一行：

```java
            IDrawingContext context = controller.getContext();
```

- [ ] **Step 4: 更新调用方**

五个 handler 中 `import com.bingbaihanji.util.GeometryCommand;` 改为 `import com.bingbaihanji.controller.GeometryCommand;`。

`PointMenuBuilder.java` 增加导入：

```java
import com.bingbaihanji.controller.ConstraintUtils;
```

四处调用改为：

```java
        List<WorldObject> nearbyShapes = ConstraintUtils.findNearbyConstrainableShapes(point, canvas);
```
```java
                    shapeItem.setOnAction(e -> ConstraintUtils.addConstraintToPoint(point, shape, controller.getContext(), canvas));
```

- [ ] **Step 5: 编译自检**

```bash
mvn -q compile
```

Expected: 无输出。
确认 util 不再依赖 controller：`grep -rn "import com.bingbaihanji.controller" src/main/java/com/bingbaihanji/util/` 应无结果。

- [ ] **Step 6: 提交**

```bash
git add -A
git commit -m "refactor: GeometryCommand/ConstraintUtils 迁入 controller, 消除 util -> controller 反向依赖

Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

## Task 7: 打断 constant 与 model 的循环依赖

**Files:**
- Move: `src/main/java/com/bingbaihanji/model/FunctionParameter.java` → `src/main/java/com/bingbaihanji/constant/FunctionParameter.java`
- Modify: `constant/FunctionType.java:3`
- Modify: `view/menu/FunctionInputDialog.java`

**原理:** `FunctionType`（constant）内嵌 `FunctionParameter[]`（model），而 `FunctionInputResult`（model）又用 `FunctionType`，形成 constant ↔ model 环。`FunctionParameter` 是纯元信息 record，无任何依赖，放进 constant 即可单向化。

- [ ] **Step 1: 搬移文件**

```bash
git mv src/main/java/com/bingbaihanji/model/FunctionParameter.java src/main/java/com/bingbaihanji/constant/FunctionParameter.java
```

- [ ] **Step 2: 改包声明与导入**

`FunctionParameter.java` 首行改为：

```java
package com.bingbaihanji.constant;
```

`FunctionType.java` 删除：

```java
import com.bingbaihanji.model.FunctionParameter;
```

`FunctionInputDialog.java` 的导入改为：

```java
import com.bingbaihanji.constant.FunctionParameter;
```

- [ ] **Step 3: 编译自检**

```bash
mvn -q compile
```

Expected: 无输出。
确认环已断：`grep -rn "import com.bingbaihanji.model" src/main/java/com/bingbaihanji/constant/` 应无结果。

- [ ] **Step 4: 提交**

```bash
git add -A
git commit -m "refactor: FunctionParameter 迁入 constant, 打断 constant 与 model 的循环依赖

Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

## Task 8: 日志规范收尾与架构说明

**Files:**
- Modify: `src/main/java/com/bingbaihanji/view/layout/pane/ShapeToolPane.java:313,317`
- Modify: `src/main/java/com/bingbaihanji/view/menu/SystemSettingsDialog.java:96,312`
- Create: `docs/architecture.md`

- [ ] **Step 1: 替换 System.err / printStackTrace**

这两个类当前都没有 Logger，先各加一个。

`ShapeToolPane.java` 类体顶部追加：

```java
    private static final Logger logger = LoggerFactory.getLogger(ShapeToolPane.class);
```

并补导入：

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
```

把原来的：

```java
                } else {
                    System.err.println("Icon not found: " + iconPath);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
```

改成：

```java
                } else {
                    logger.warn("Icon not found: {}", iconPath);
                }
            } catch (Exception e) {
                logger.error("加载工具图标失败", e);
            }
```

`SystemSettingsDialog.java` 类体顶部追加：

```java
    private static final Logger logger = LoggerFactory.getLogger(SystemSettingsDialog.class);
```

并补同样的两个导入。两处 `catch (Exception e) { e.printStackTrace(); }` 改为：

```java
        } catch (Exception e) {
            logger.error("加载语言列表失败", e);
        }
```

```java
        } catch (Exception e) {
            logger.error("应用系统设置失败", e);
        }
```

- [ ] **Step 2: 写架构说明**

`docs/architecture.md`：

```markdown
# FXGeometricView 架构说明

## 分层与依赖方向

```
view (渲染/视口手势)  ->  controller (交互语义/命令)  ->  util / constant / model
        |                        |
        +------------------------+--> view.layout.draw.geometry  (几何模型 + 绘制)
```

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
```

- [ ] **Step 3: 编译自检**

```bash
mvn -q compile
```

Expected: 无输出。再确认：`grep -rn "printStackTrace\|System.err" src/main/java` 应无结果。

- [ ] **Step 4: 提交**

```bash
git add -A
git commit -m "docs: 补充架构说明与债务清单, 清理 printStackTrace/System.err

Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

## 明确不做（避免过度设计）

| 事项 | 原因 |
|---|---|
| 把 `geometry`/`WorldTransform`/`EuclidianViewSettings` 整体迁到 domain 包 | 纯机械重命名 100+ 文件，功能收益为零，破坏 git blame；等有真实需求再动 |
| 给 `util` 包再做拆分 | 先按"只删不加"清理，等 util 只剩真正通用的类后再看 |
| 为 Handler 引入 IoC 容器 / 事件总线 | 9 个 Handler 用显式注册完全够用，引入容器是净负债 |
| 给 `AbstractWorldObject` 抽样式值对象 | 目前只有一套样式组合，rule of three 未满足 |
| 删除 `src/test` 下的 `Exp4jLearn`/`FormulaCalculator`/`main`/`test/FxTools` | 这是你的个人草稿文件，已入库，删除前需要你确认 |
| 删除 `joml` 依赖 | 源码零引用，但 pom 里有你写的"后续可能加 3D 坐标系"注释，属于有意占位，由你决定去留 |
| 新增任何测试类 | 按你的规范，测试由你按业务流程人工验证 |

---

## 验收清单

- [ ] `mvn -q compile` 通过
- [ ] 已有 33 个用例仍全绿（`mvn test`，不新增用例）
- [ ] `grep -rn "drawModeActive" src/main/java` 无结果
- [ ] `grep -rn "import com.bingbaihanji.controller" src/main/java/com/bingbaihanji/util/` 无结果
- [ ] `grep -rn "printStackTrace\|System.err" src/main/java` 只剩 `FxTools.java` 的 `printStackTrace(PrintWriter)`（那是把堆栈写进字符串给对话框用，属于合法用法，不算吞异常）
- [ ] 启动应用冒烟：非绘制模式点击图形可选中、绘制模式点击不再选中、涟漪与缩放平移正常
```
