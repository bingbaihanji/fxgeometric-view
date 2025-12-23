package com.binbaihanji.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 命令历史管理器测试
 *
 * @author bingbaihanji
 * @date 2025-12-23
 */
public class CommandHistoryTest {

    private CommandHistory history;
    private StringBuilder result;

    @BeforeEach
    public void setUp() {
        history = new CommandHistory();
        result = new StringBuilder();
    }

    @Test
    public void testExecuteAndUndo() {
        // 执行一个命令
        history.execute(new CommandHistory.Command() {
            @Override
            public void execute() {
                result.append("A");
            }

            @Override
            public void undo() {
                result.setLength(result.length() - 1);
            }
        });

        assertEquals("A", result.toString());
        assertTrue(history.canUndo());
        assertFalse(history.canRedo());

        // 撤销
        history.undo();
        assertEquals("", result.toString());
        assertFalse(history.canUndo());
        assertTrue(history.canRedo());
    }

    @Test
    public void testRedo() {
        // 执行命令
        history.execute(new CommandHistory.Command() {
            @Override
            public void execute() {
                result.append("A");
            }

            @Override
            public void undo() {
                result.setLength(result.length() - 1);
            }
        });

        assertEquals("A", result.toString());

        // 撤销
        history.undo();
        assertEquals("", result.toString());

        // 恢复
        history.redo();
        assertEquals("A", result.toString());
        assertTrue(history.canUndo());
        assertFalse(history.canRedo());
    }

    @Test
    public void testMultipleCommands() {
        // 执行多个命令
        history.execute(new CommandHistory.Command() {
            @Override
            public void execute() {
                result.append("A");
            }

            @Override
            public void undo() {
                result.setLength(result.length() - 1);
            }
        });

        history.execute(new CommandHistory.Command() {
            @Override
            public void execute() {
                result.append("B");
            }

            @Override
            public void undo() {
                result.setLength(result.length() - 1);
            }
        });

        assertEquals("AB", result.toString());
        assertTrue(history.canUndo());
        assertFalse(history.canRedo());

        // 撤销第二个命令
        history.undo();
        assertEquals("A", result.toString());
        assertTrue(history.canUndo());
        assertTrue(history.canRedo());

        // 撤销第一个命令
        history.undo();
        assertEquals("", result.toString());
        assertFalse(history.canUndo());
        assertTrue(history.canRedo());

        // 恢复第一个命令
        history.redo();
        assertEquals("A", result.toString());
        assertTrue(history.canUndo());
        assertTrue(history.canRedo());

        // 恢复第二个命令
        history.redo();
        assertEquals("AB", result.toString());
        assertTrue(history.canUndo());
        assertFalse(history.canRedo());
    }

    @Test
    public void testClearHistoryAfterNewCommand() {
        // 执行命令
        history.execute(new CommandHistory.Command() {
            @Override
            public void execute() {
                result.append("A");
            }

            @Override
            public void undo() {
                result.setLength(result.length() - 1);
            }
        });

        // 撤销
        history.undo();
        assertTrue(history.canRedo());

        // 执行新命令应清空恢复栈
        history.execute(new CommandHistory.Command() {
            @Override
            public void execute() {
                result.append("B");
            }

            @Override
            public void undo() {
                result.setLength(result.length() - 1);
            }
        });

        assertEquals("B", result.toString());
        assertTrue(history.canUndo());
        assertFalse(history.canRedo()); // 恢复栈应被清空
    }

    @Test
    public void testClear() {
        // 执行命令
        history.execute(new CommandHistory.Command() {
            @Override
            public void execute() {
                result.append("A");
            }

            @Override
            public void undo() {
                result.setLength(result.length() - 1);
            }
        });

        assertTrue(history.canUndo());

        // 清空历史
        history.clear();
        assertFalse(history.canUndo());
        assertFalse(history.canRedo());
    }
}
