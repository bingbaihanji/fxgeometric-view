package com.bingbaihanji.view.layout.draw.geometry.impl;

import javafx.geometry.Point2D;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomFunctionGeoTest {

    @Test
    void reciprocalExpressionDoesNotConnectAcrossAsymptote() {
        TestableCustomFunctionGeo function = new TestableCustomFunctionGeo("1/x");

        function.sample(-2, 2, -2, 2, 50);

        List<List<Point2D>> segments = function.getSampledSegments();
        assertTrue(segments.size() >= 2, "Expected separate branches around x=0");
        assertFalse(hasSegmentCrossingZero(segments), "No segment should connect across x=0");
    }

    @Test
    void invalidDomainCreatesNewSegmentInsteadOfBridge() {
        TestableCustomFunctionGeo function = new TestableCustomFunctionGeo("sqrt(x)");

        function.sample(-2, 2, -2, 2, 50);

        List<List<Point2D>> segments = function.getSampledSegments();
        assertFalse(segments.isEmpty(), "Expected visible sqrt branch");
        assertTrue(segments.stream()
                .flatMap(List::stream)
                .allMatch(point -> point.getX() >= 0), "sqrt branch should start at x >= 0");
    }

    @Test
    void keepsOffscreenSamplesWhenSegmentCrossesViewport() {
        TestableCustomFunctionGeo function = new TestableCustomFunctionGeo("100*x");

        function.sample(-1, 1, -1, 1, 50);

        List<Point2D> points = function.getSampledPoints();
        assertTrue(points.stream().anyMatch(point -> point.getY() < -1));
        assertTrue(points.stream().anyMatch(point -> point.getY() > 1));
        assertTrue(function.getSampledSegments().stream()
                .anyMatch(segment -> segment.size() > 2), "Steep continuous line should remain connected");
    }

    private boolean hasSegmentCrossingZero(List<List<Point2D>> segments) {
        for (List<Point2D> segment : segments) {
            for (int i = 0; i < segment.size() - 1; i++) {
                Point2D p1 = segment.get(i);
                Point2D p2 = segment.get(i + 1);
                if (p1.getX() < 0 && p2.getX() > 0 || p1.getX() > 0 && p2.getX() < 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static final class TestableCustomFunctionGeo extends CustomFunctionGeo {
        private TestableCustomFunctionGeo(String expressionStr) {
            super(expressionStr);
        }

        private void sample(double viewMinX, double viewMaxX,
                            double viewMinY, double viewMaxY,
                            double scale) {
            samplePoints(viewMinX, viewMaxX, viewMinY, viewMaxY, scale);
        }
    }
}
