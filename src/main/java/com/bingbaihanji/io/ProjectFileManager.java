package com.bingbaihanji.io;

import com.bingbaihanji.constant.*;
import com.bingbaihanji.view.layout.core.GridChartView;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.*;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

/**
 * 工程文件管理器
 * <p>
 * 负责工程的保存和加载，使用自定义二进制格式：
 * [MAGIC(4)] + [VERSION(1)] + [RESERVED(4)] + [DATA_LENGTH(4)] + [Serialized ProjectData]
 */
public class ProjectFileManager {

    private static final Logger logger = LoggerFactory.getLogger(ProjectFileManager.class);

    /**
     * 保存工程到文件
     */
    public static void saveProject(Path filePath, GridChartView view, List<WorldObject> objects) {
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile());
             DataOutputStream dos = new DataOutputStream(fos)) {
            // 1. 构建 ProjectData
            ProjectData data = new ProjectData();
            WorldTransform transform = view.getTransform();
            data.setScaleX(transform.getScaleX());
            data.setScaleY(transform.getScaleY());
            data.setOffsetX(transform.getOffsetX());
            data.setOffsetY(transform.getOffsetY());
            data.setBackgroundColor(colorToArgb(view.getBackgroundColor()));
            data.setUnitLabelTypeOrdinal(view.getSettings().getUnitLabelType().ordinal());

            List<ObjectData> objectDataList = new ArrayList<>();
            for (WorldObject obj : objects) {
                objectDataList.add(serializeObject(obj));
            }
            data.setObjects(objectDataList);

            // 2. 序列化为字节数组
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(data);
            }
            byte[] serializedData = baos.toByteArray();

            // 3. 写入文件头 + 数据
            dos.write(ProjectData.MAGIC);
            dos.write(ProjectData.VERSION);
            dos.writeInt(0); // reserved
            dos.writeInt(serializedData.length);
            dos.write(serializedData);

            logger.info("工程已保存: {}", filePath);
        } catch (Exception e) {
            logger.error("保存工程失败", e);
            throw new RuntimeException("保存工程失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从文件加载工程
     *
     * @return 加载结果，包含对象列表和视图状态
     */
    public static LoadResult loadProject(Path filePath) {
        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             DataInputStream dis = new DataInputStream(fis)) {
            // 1. 读取文件头
            byte[] magic = new byte[4];
            int read = dis.read(magic);
            if (read != 4 || !Arrays.equals(magic, ProjectData.MAGIC)) {
                throw new IOException("无效的文件格式: 魔数不匹配");
            }

            int version = dis.read();
            if (version != ProjectData.VERSION) {
                throw new IOException("不支持的文件版本: " + version);
            }

            dis.skipBytes(4); // reserved
            int dataLength = dis.readInt();

            // 2. 读取序列化数据
            byte[] serializedData = new byte[dataLength];
            read = dis.read(serializedData);
            if (read != dataLength) {
                throw new IOException("文件数据不完整");
            }

            // 3. 反序列化
            ByteArrayInputStream bais = new ByteArrayInputStream(serializedData);
            ProjectData data;
            try (ObjectInputStream ois = new ObjectInputStream(bais)) {
                data = (ProjectData) ois.readObject();
            }

            // 4. 重建对象
            List<WorldObject> objects = new ArrayList<>();
            for (ObjectData objData : data.getObjects()) {
                WorldObject obj = deserializeObject(objData);
                if (obj != null) {
                    objects.add(obj);
                }
            }

            logger.info("工程已加载: {}，共 {} 个对象", filePath, objects.size());
            return new LoadResult(objects, data);

        } catch (Exception e) {
            logger.error("加载工程失败", e);
            throw new RuntimeException("加载工程失败: " + e.getMessage(), e);
        }
    }

    // ============== 序列化：WorldObject -> ObjectData ==============

    private static ObjectData serializeObject(WorldObject obj) {
        ObjectData data = new ObjectData();
        data.setType(obj.getObjectType().name());

        Map<String, Serializable> props = new LinkedHashMap<>();
        // 通用属性
        props.put("label", obj.getLabel());
        props.put("color", colorToArgb(obj.getColor()));
        props.put("lineType", obj.getLineType().ordinal());
        props.put("lineWidth", obj.getLineWidth());
        props.put("opacity", obj.getOpacity());
        props.put("fillType", obj.getFillType().ordinal());
        props.put("fillColor", colorToArgb(obj.getFillColor()));
        props.put("fillOpacity", obj.getFillOpacity());
        props.put("hatchAngle", obj.getHatchAngle());
        props.put("hatchDistance", obj.getHatchDistance());
        props.put("visible", obj.isVisible());
        props.put("locked", obj.isLocked());
        props.put("fixed", obj.isFixed());
        props.put("selectable", obj.isSelectable());
        props.put("labelVisible", obj.isLabelVisible());
        props.put("labelPosition", obj.getLabelPosition().ordinal());
        props.put("labelFontSize", obj.getLabelFontSize());
        props.put("labelColor", colorToArgb(obj.getLabelColor()));

        // 类型特有属性
        switch (obj.getObjectType()) {
            case POINT_FREE:
            case POINT_DEPENDENT:
            case POINT_ON_PATH:
            case POINT_INTERSECTION:
                serializePoint((PointGeo) obj, props);
                break;
            case SEGMENT:
            case LINE:
            case RAY:
            case VECTOR:
                serializeLine((LineGeo) obj, props);
                break;
            case CIRCLE:
                serializeCircle((CircleGeo) obj, props);
                break;
            case POLYGON:
            case TRIANGLE:
            case RECTANGLE:
                serializePolygon((PolygonGeo) obj, props);
                break;
            case REGULAR_POLYGON:
                serializeRegularPolygon((RegularPolygonGeo) obj, props);
                break;
            case PATH:
                serializePath((PathGeo) obj, props);
                break;
            case FUNCTION:
                serializeFunction((FunctionGeo) obj, props);
                break;
            default:
                if (obj instanceof InfiniteLineGeo) {
                    serializeInfiniteLine((InfiniteLineGeo) obj, props);
                } else {
                    logger.warn("未支持的序列化类型: {}", obj.getObjectType());
                }
                break;
        }

        data.setProperties(props);
        return data;
    }

    private static void serializePoint(PointGeo point, Map<String, Serializable> props) {
        props.put("x", point.getX());
        props.put("y", point.getY());
    }

    private static void serializeLine(LineGeo line, Map<String, Serializable> props) {
        props.put("startX", line.getStartX());
        props.put("startY", line.getStartY());
        props.put("endX", line.getEndX());
        props.put("endY", line.getEndY());
        props.put("startPointName", line.getStartPointName());
        props.put("endPointName", line.getEndPointName());
    }

    private static void serializeCircle(CircleGeo circle, Map<String, Serializable> props) {
        props.put("cx", circle.getCx());
        props.put("cy", circle.getCy());
        props.put("radius", circle.getR());
        props.put("centerName", circle.getCenterName());
    }

    private static void serializePolygon(PolygonGeo polygon, Map<String, Serializable> props) {
        List<Double> coords = new ArrayList<>();
        for (int i = 0; i < polygon.getVertexCount(); i++) {
            javafx.geometry.Point2D v = polygon.getVertex(i);
            coords.add(v.getX());
            coords.add(v.getY());
        }
        props.put("vertices", (Serializable) coords);
    }

    private static void serializeRegularPolygon(RegularPolygonGeo rp, Map<String, Serializable> props) {
        props.put("cx", rp.getCx());
        props.put("cy", rp.getCy());
        props.put("radius", rp.getRadius());
        props.put("sideCount", rp.getSideCount());
        props.put("startAngle", rp.getStartAngle());
        props.put("centerName", rp.getCenterName());
    }

    private static void serializePath(PathGeo path, Map<String, Serializable> props) {
        List<Double> coords = new ArrayList<>();
        for (WorldObject.DraggablePoint p : path.getDraggablePoints()) {
            coords.add(p.getX());
            coords.add(p.getY());
        }
        props.put("points", (Serializable) coords);
    }

    @SuppressWarnings("unchecked")
    private static void serializeFunction(FunctionGeo func, Map<String, Serializable> props) {
        props.put("expression", func.getExpression());
        props.put("domainMin", func.getDomainMin());
        props.put("domainMax", func.getDomainMax());

        // 子类特有参数
        if (func instanceof LinearFunctionGeo linear) {
            props.put("funcClass", "LinearFunctionGeo");
            props.put("k", linear.getK());
            props.put("b", linear.getB());
        } else if (func instanceof QuadraticFunctionGeo quad) {
            props.put("funcClass", "QuadraticFunctionGeo");
            props.put("a", quad.getA());
            props.put("b", quad.getB());
            props.put("c", quad.getC());
        } else if (func instanceof ReciprocalFunctionGeo reciprocal) {
            props.put("funcClass", "ReciprocalFunctionGeo");
            props.put("k", reciprocal.getK());
        } else if (func instanceof TrigonometricFunctionGeo trig) {
            props.put("funcClass", "TrigonometricFunctionGeo");
            props.put("trigType", trig.getTrigType().name());
            props.put("A", trig.getA());
            props.put("omega", trig.getOmega());
            props.put("phi", trig.getPhi());
            props.put("k", trig.getK());
        } else if (func instanceof ExponentialFunctionGeo exp) {
            props.put("funcClass", "ExponentialFunctionGeo");
            props.put("a", exp.getA());
        } else if (func instanceof LogarithmicFunctionGeo log) {
            props.put("funcClass", "LogarithmicFunctionGeo");
            props.put("a", log.getA());
        } else if (func instanceof EllipseFunctionGeo ellipse) {
            props.put("funcClass", "EllipseFunctionGeo");
            props.put("cx", ellipse.getCx());
            props.put("cy", ellipse.getCy());
            props.put("a", ellipse.getA());
            props.put("b", ellipse.getB());
        } else if (func instanceof HyperbolaFunctionGeo hyperbola) {
            props.put("funcClass", "HyperbolaFunctionGeo");
            props.put("cx", hyperbola.getCx());
            props.put("cy", hyperbola.getCy());
            props.put("a", hyperbola.getA());
            props.put("b", hyperbola.getB());
        } else if (func instanceof ParabolaConicFunctionGeo parabola) {
            props.put("funcClass", "ParabolaConicFunctionGeo");
            props.put("p", parabola.getP());
        } else if (func instanceof CustomFunctionGeo custom) {
            props.put("funcClass", "CustomFunctionGeo");
            props.put("expressionStr", custom.getExpressionStr());
        } else {
            props.put("funcClass", func.getClass().getSimpleName());
        }
    }

    private static void serializeInfiniteLine(InfiniteLineGeo line, Map<String, Serializable> props) {
        props.put("point1X", line.getPoint1X());
        props.put("point1Y", line.getPoint1Y());
        props.put("point2X", line.getPoint2X());
        props.put("point2Y", line.getPoint2Y());
        props.put("point1Name", line.getPoint1Name());
        props.put("point2Name", line.getPoint2Name());
    }

    // ============== 反序列化：ObjectData -> WorldObject ==============

    @SuppressWarnings("unchecked")
    private static WorldObject deserializeObject(ObjectData data) {
        String typeName = data.getType();
        Map<String, Serializable> props = data.getProperties();
        ObjectType type = ObjectType.valueOf(typeName);

        WorldObject obj;
        switch (type) {
            case POINT_FREE:
            case POINT_DEPENDENT:
            case POINT_ON_PATH:
            case POINT_INTERSECTION:
                obj = deserializePoint(props);
                break;
            case SEGMENT:
            case LINE:
            case RAY:
            case VECTOR:
                obj = deserializeLine(props, type);
                break;
            case CIRCLE:
                obj = deserializeCircle(props);
                break;
            case POLYGON:
            case TRIANGLE:
            case RECTANGLE:
                obj = deserializePolygon(props, type);
                break;
            case REGULAR_POLYGON:
                obj = deserializeRegularPolygon(props);
                break;
            case PATH:
                obj = deserializePath(props);
                break;
            case FUNCTION:
                obj = deserializeFunction(props);
                break;
            default:
                if (typeName.equals("INFINITE_LINE")) {
                    obj = deserializeInfiniteLine(props);
                } else {
                    logger.warn("未支持的反序列化类型: {}", type);
                    obj = null;
                }
                break;
        }

        if (obj != null) {
            applyCommonProperties(obj, props);
        }
        return obj;
    }

    private static void applyCommonProperties(WorldObject obj, Map<String, Serializable> props) {
        obj.setLabel((String) props.getOrDefault("label", ""));
        obj.setColor(argbToColor((Integer) props.getOrDefault("color", colorToArgb(Color.BLACK))));
        obj.setLineType(LineType.values()[(Integer) props.getOrDefault("lineType", 0)]);
        obj.setLineWidth((Double) props.getOrDefault("lineWidth", 2.0));
        obj.setOpacity((Double) props.getOrDefault("opacity", 1.0));
        obj.setFillType(FillType.values()[(Integer) props.getOrDefault("fillType", 0)]);
        obj.setFillColor(argbToColor((Integer) props.getOrDefault("fillColor", colorToArgb(Color.LIGHTGRAY))));
        obj.setFillOpacity((Double) props.getOrDefault("fillOpacity", 0.3));
        obj.setHatchAngle((Integer) props.getOrDefault("hatchAngle", 45));
        obj.setHatchDistance((Integer) props.getOrDefault("hatchDistance", 10));
        obj.setVisible((Boolean) props.getOrDefault("visible", true));
        obj.setLocked((Boolean) props.getOrDefault("locked", false));
        obj.setFixed((Boolean) props.getOrDefault("fixed", false));
        obj.setSelectable((Boolean) props.getOrDefault("selectable", true));
        obj.setLabelVisible((Boolean) props.getOrDefault("labelVisible", true));
        obj.setLabelPosition(LabelPosition.values()[(Integer) props.getOrDefault("labelPosition", 0)]);
        obj.setLabelFontSize((Double) props.getOrDefault("labelFontSize", 12.0));
        obj.setLabelColor(argbToColor((Integer) props.getOrDefault("labelColor", colorToArgb(Color.BLACK))));
    }

    private static PointGeo deserializePoint(Map<String, Serializable> props) {
        double x = (Double) props.get("x");
        double y = (Double) props.get("y");
        return new PointGeo(x, y, false);
    }

    private static LineGeo deserializeLine(Map<String, Serializable> props, ObjectType type) {
        double sx = (Double) props.get("startX");
        double sy = (Double) props.get("startY");
        double ex = (Double) props.get("endX");
        double ey = (Double) props.get("endY");
        LineGeo line = new LineGeo(sx, sy, ex, ey, false);
        line.setStartPointName((String) props.getOrDefault("startPointName", null));
        line.setEndPointName((String) props.getOrDefault("endPointName", null));
        return line;
    }

    private static CircleGeo deserializeCircle(Map<String, Serializable> props) {
        double cx = (Double) props.get("cx");
        double cy = (Double) props.get("cy");
        double r = (Double) props.get("radius");
        CircleGeo circle = new CircleGeo(cx, cy, r, false);
        circle.setCenterName((String) props.getOrDefault("centerName", null));
        return circle;
    }

    @SuppressWarnings("unchecked")
    private static PolygonGeo deserializePolygon(Map<String, Serializable> props, ObjectType type) {
        List<Double> coords = (List<Double>) props.get("vertices");
        double[] vertices = new double[coords.size()];
        for (int i = 0; i < coords.size(); i++) {
            vertices[i] = coords.get(i);
        }
        return new PolygonGeo(vertices);
    }

    private static RegularPolygonGeo deserializeRegularPolygon(Map<String, Serializable> props) {
        double cx = (Double) props.get("cx");
        double cy = (Double) props.get("cy");
        double radius = (Double) props.get("radius");
        int sideCount = (Integer) props.get("sideCount");
        double startAngle = (Double) props.get("startAngle");
        RegularPolygonGeo rp = new RegularPolygonGeo(cx, cy, radius, sideCount, false);
        rp.setStartAngle(startAngle);
        rp.setCenterName((String) props.getOrDefault("centerName", null));
        return rp;
    }

    @SuppressWarnings("unchecked")
    private static PathGeo deserializePath(Map<String, Serializable> props) {
        List<Double> coords = (List<Double>) props.get("points");
        List<javafx.geometry.Point2D> points = new ArrayList<>();
        for (int i = 0; i < coords.size(); i += 2) {
            points.add(new javafx.geometry.Point2D(coords.get(i), coords.get(i + 1)));
        }
        return new PathGeo(points);
    }

    @SuppressWarnings("unchecked")
    private static FunctionGeo deserializeFunction(Map<String, Serializable> props) {
        String funcClass = (String) props.get("funcClass");
        FunctionGeo func = null;

        switch (funcClass) {
            case "LinearFunctionGeo" -> func = new LinearFunctionGeo((Double) props.get("k"), (Double) props.get("b"));
            case "QuadraticFunctionGeo" ->
                    func = new QuadraticFunctionGeo((Double) props.get("a"), (Double) props.get("b"), (Double) props.get("c"));
            case "ReciprocalFunctionGeo" -> func = new ReciprocalFunctionGeo((Double) props.get("k"));
            case "TrigonometricFunctionGeo" -> {
                TrigonometricFunctionGeo.TrigType trigType = TrigonometricFunctionGeo.TrigType.valueOf((String) props.get("trigType"));
                FunctionType ft = switch (trigType) {
                    case SINE -> FunctionType.SINE;
                    case COSINE -> FunctionType.COSINE;
                    case TANGENT -> FunctionType.TANGENT;
                };
                func = new TrigonometricFunctionGeo(ft, (Double) props.get("A"), (Double) props.get("omega"), (Double) props.get("phi"), (Double) props.get("k"));
            }
            case "ExponentialFunctionGeo" -> func = new ExponentialFunctionGeo((Double) props.get("a"));
            case "LogarithmicFunctionGeo" -> func = new LogarithmicFunctionGeo((Double) props.get("a"));
            case "EllipseFunctionGeo" ->
                    func = new EllipseFunctionGeo((Double) props.get("cx"), (Double) props.get("cy"), (Double) props.get("a"), (Double) props.get("b"));
            case "HyperbolaFunctionGeo" ->
                    func = new HyperbolaFunctionGeo((Double) props.get("cx"), (Double) props.get("cy"), (Double) props.get("a"), (Double) props.get("b"));
            case "ParabolaConicFunctionGeo" -> func = new ParabolaConicFunctionGeo((Double) props.get("p"));
            case "CustomFunctionGeo" -> func = new CustomFunctionGeo((String) props.get("expressionStr"));
            default -> logger.warn("未支持的函数类型: {}", funcClass);
        }

        if (func != null) {
            double domainMin = (Double) props.getOrDefault("domainMin", Double.NEGATIVE_INFINITY);
            double domainMax = (Double) props.getOrDefault("domainMax", Double.POSITIVE_INFINITY);
            if (Double.isFinite(domainMin) || Double.isFinite(domainMax)) {
                func.setDomainRange(domainMin, domainMax);
            }
        }
        return func;
    }

    private static InfiniteLineGeo deserializeInfiniteLine(Map<String, Serializable> props) {
        double p1x = (Double) props.get("point1X");
        double p1y = (Double) props.get("point1Y");
        double p2x = (Double) props.get("point2X");
        double p2y = (Double) props.get("point2Y");
        InfiniteLineGeo line = new InfiniteLineGeo(p1x, p1y, p2x, p2y, false);
        line.setPoint1Name((String) props.getOrDefault("point1Name", null));
        line.setPoint2Name((String) props.getOrDefault("point2Name", null));
        return line;
    }

    // ============== 工具方法 ==============

    private static int colorToArgb(Color color) {
        if (color == null) return 0xFF000000;
        return ((int) (color.getOpacity() * 255) << 24)
                | ((int) (color.getRed() * 255) << 16)
                | ((int) (color.getGreen() * 255) << 8)
                | ((int) (color.getBlue() * 255));
    }

    private static Color argbToColor(int argb) {
        double a = ((argb >>> 24) & 0xFF) / 255.0;
        double r = ((argb >> 16) & 0xFF) / 255.0;
        double g = ((argb >> 8) & 0xFF) / 255.0;
        double b = (argb & 0xFF) / 255.0;
        return new Color(r, g, b, a);
    }

    /**
     * 加载结果
     */
    public record LoadResult(List<WorldObject> objects, ProjectData projectData) {
    }
}
