package com.bingbaihanji.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 命令历史管理器
 * <p>
 * 用于管理撤销/恢复功能的命令栈
 *
 * @author bingbaihanji
 * @date 2025-12-23
 */
public class CommandHistory {

    /**
     * 撤销栈
     */
    private final List<Command> undoStack = new ArrayList<>();
    /**
     * 恢复栈
     */
    private final List<Command> redoStack = new ArrayList<>();

    /**
     * 执行命令并记录到撤销栈
     *
     * @param command 要执行的命令
     */
    public void execute(Command command) {
        command.execute();
        undoStack.add(command);
        // 执行新命令时，清空恢复栈
        redoStack.clear();
    }

    /**
     * 只记录命令到撤销栈（不执行，用于已经完成的操作如拖动）
     *
     * @param command 要记录的命令
     */
    public void addCommand(Command command) {
        undoStack.add(command);
        // 记录新命令时，清空恢复栈
        redoStack.clear();
    }

    /**
     * 撤销最后一个命令
     */
    public void undo() {
        if (!undoStack.isEmpty()) {
            Command command = undoStack.remove(undoStack.size() - 1);
            command.undo();
            redoStack.add(command);
        }
    }

    /**
     * 恢复最后一个被撤销的命令
     */
    public void redo() {
        if (!redoStack.isEmpty()) {
            Command command = redoStack.remove(redoStack.size() - 1);
            command.execute();
            undoStack.add(command);
        }
    }

    /**
     * 判断是否可以撤销
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /**
     * 判断是否可以恢复
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * 清空所有历史记录
     */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }

    /**
     * 命令接口
     */
    public interface Command {
        /**
         * 执行命令
         */
        void execute();

        /**
         * 撤销命令
         */
        void undo();
    }
}
