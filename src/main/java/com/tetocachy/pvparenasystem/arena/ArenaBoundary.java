package com.tetocachy.pvparenasystem.arena;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class ArenaBoundary {
    public enum Shape {
        BOX,
        CYLINDER,
        POLYGON
    }

    public static class Point2D {
        public final double x;
        public final double z;

        public Point2D(double x, double z) {
            this.x = x;
            this.z = z;
        }
    }

    private Shape shape = Shape.BOX;
    private double minY;
    private double maxY;

    // Box bounds
    private double minX, minZ, maxX, maxZ;

    // Cylinder bounds
    private double centerX, centerZ;
    private double radius = 30.0;

    // Polygon bounds
    private final List<Point2D> polygonPoints = new ArrayList<>();

    public ArenaBoundary(BlockPos minPos, BlockPos maxPos) {
        this.minX = minPos.getX();
        this.minY = minPos.getY() - 1.0;
        this.minZ = minPos.getZ();
        this.maxX = maxPos.getX() + 1.0;
        this.maxY = maxPos.getY() + 20.0;
        this.maxZ = maxPos.getZ() + 1.0;

        this.centerX = (minX + maxX) / 2.0;
        this.centerZ = (minZ + maxZ) / 2.0;
        this.radius = Math.max(maxX - minX, maxZ - minZ) / 2.0;
    }

    public boolean isInside(double x, double y, double z) {
        if (y < minY || y > maxY) {
            return false;
        }

        return switch (shape) {
            case BOX -> x >= minX && x <= maxX && z >= minZ && z <= maxZ;
            case CYLINDER -> {
                double dx = x - centerX;
                double dz = z - centerZ;
                yield (dx * dx + dz * dz) <= (radius * radius);
            }
            case POLYGON -> isPointInPolygon(x, z, polygonPoints);
        };
    }

    public boolean isBelowVoid(double y) {
        return y < (minY - 3.0) || y < -40.0;
    }

    private static boolean isPointInPolygon(double x, double z, List<Point2D> polygon) {
        if (polygon.size() < 3) return true;
        boolean inside = false;
        int n = polygon.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            Point2D pi = polygon.get(i);
            Point2D pj = polygon.get(j);
            if (((pi.z > z) != (pj.z > z)) &&
                    (x < (pj.x - pi.x) * (z - pi.z) / (pj.z - pi.z) + pi.x)) {
                inside = !inside;
            }
        }
        return inside;
    }

    public void addPolygonPoint(double x, double z) {
        polygonPoints.add(new Point2D(x, z));
    }

    public void clearPolygonPoints() {
        polygonPoints.clear();
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("shape", shape.name());
        obj.addProperty("minY", minY);
        obj.addProperty("maxY", maxY);
        obj.addProperty("minX", minX);
        obj.addProperty("minZ", minZ);
        obj.addProperty("maxX", maxX);
        obj.addProperty("maxZ", maxZ);
        obj.addProperty("centerX", centerX);
        obj.addProperty("centerZ", centerZ);
        obj.addProperty("radius", radius);

        JsonArray polyArr = new JsonArray();
        for (Point2D p : polygonPoints) {
            JsonObject pObj = new JsonObject();
            pObj.addProperty("x", p.x);
            pObj.addProperty("z", p.z);
            polyArr.add(pObj);
        }
        obj.add("polygonPoints", polyArr);
        return obj;
    }

    public static ArenaBoundary fromJson(JsonObject obj, BlockPos minPos, BlockPos maxPos) {
        ArenaBoundary boundary = new ArenaBoundary(minPos, maxPos);
        if (obj.has("shape")) {
            try {
                boundary.setShape(Shape.valueOf(obj.get("shape").getAsString()));
            } catch (Exception ignored) {}
        }
        if (obj.has("minY")) boundary.minY = obj.get("minY").getAsDouble();
        if (obj.has("maxY")) boundary.maxY = obj.get("maxY").getAsDouble();
        if (obj.has("minX")) boundary.minX = obj.get("minX").getAsDouble();
        if (obj.has("minZ")) boundary.minZ = obj.get("minZ").getAsDouble();
        if (obj.has("maxX")) boundary.maxX = obj.get("maxX").getAsDouble();
        if (obj.has("maxZ")) boundary.maxZ = obj.get("maxZ").getAsDouble();
        if (obj.has("centerX")) boundary.centerX = obj.get("centerX").getAsDouble();
        if (obj.has("centerZ")) boundary.centerZ = obj.get("centerZ").getAsDouble();
        if (obj.has("radius")) boundary.radius = obj.get("radius").getAsDouble();

        if (obj.has("polygonPoints")) {
            boundary.clearPolygonPoints();
            for (JsonElement el : obj.getAsJsonArray("polygonPoints")) {
                JsonObject p = el.getAsJsonObject();
                boundary.addPolygonPoint(p.get("x").getAsDouble(), p.get("z").getAsDouble());
            }
        }
        return boundary;
    }

    public Shape getShape() { return shape; }
    public void setShape(Shape shape) { this.shape = shape; }
    public double getRadius() { return radius; }
    public void setRadius(double radius) { this.radius = Math.max(5.0, radius); }
    public List<Point2D> getPolygonPoints() { return polygonPoints; }
}