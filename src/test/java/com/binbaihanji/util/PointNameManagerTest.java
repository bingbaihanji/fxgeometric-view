package com.binbaihanji.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PointNameManager 测试类
 *
 * @author bingbaihanji
 * @date 2025-12-24
 */
class PointNameManagerTest {

    private PointNameManager manager;

    @BeforeEach
    void setUp() {
        manager = PointNameManager.getInstance();
        manager.clear(); // 每次测试前清空
    }

    @Test
    void testBasicNaming() {
        // 测试基本命名: A, B, C...
        assertEquals("A", manager.assignName(0, 0));
        assertEquals("B", manager.assignName(1, 1));
        assertEquals("C", manager.assignName(2, 2));
    }

    @Test
    void testSamePointReturnsSameName() {
        // 测试相同点返回相同名称
        String name1 = manager.assignName(1.0, 2.0);
        String name2 = manager.assignName(1.0, 2.0);
        assertEquals(name1, name2);
    }

    @Test
    void testNamingSequence() {
        // 测试命名序列: A-Z, A1-Z1, A2-Z2...
        for (int i = 0; i < 26; i++) {
            char expected = (char) ('A' + i);
            assertEquals(String.valueOf(expected), manager.assignName(i, i));
        }

        // 第27个点应该是 A1
        assertEquals("A1", manager.assignName(26, 26));
        assertEquals("B1", manager.assignName(27, 27));
        
        // 第53个点应该是 A2
        for (int i = 28; i < 52; i++) {
            manager.assignName(i, i);
        }
        assertEquals("A2", manager.assignName(52, 52));
    }

    @Test
    void testClear() {
        // 测试清除功能
        manager.assignName(1, 1);
        manager.assignName(2, 2);
        assertEquals(2, manager.getNamedPointCount());
        
        manager.clear();
        assertEquals(0, manager.getNamedPointCount());
        
        // 清除后重新命名应该从A开始
        assertEquals("A", manager.assignName(3, 3));
    }

    @Test
    void testGetName() {
        // 测试获取名称
        manager.assignName(1.0, 2.0);
        assertEquals("A", manager.getName(1.0, 2.0));
        assertNull(manager.getName(3.0, 4.0));
    }

    @Test
    void testHasName() {
        // 测试是否存在名称
        manager.assignName(1.0, 2.0);
        assertTrue(manager.hasName(1.0, 2.0));
        assertFalse(manager.hasName(3.0, 4.0));
    }

    @Test
    void testRemoveName() {
        // 测试移除名称
        manager.assignName(1.0, 2.0);
        assertTrue(manager.hasName(1.0, 2.0));
        
        manager.removeName(1.0, 2.0);
        assertFalse(manager.hasName(1.0, 2.0));
    }

    @Test
    void testFloatingPointPrecision() {
        // 测试浮点数精度处理
        String name1 = manager.assignName(1.0000001, 2.0000001);
        String name2 = manager.assignName(1.0000002, 2.0000002);
        // 由于使用了精度阈值，这两个点应该被视为同一个点
        assertEquals(name1, name2);
    }
}
