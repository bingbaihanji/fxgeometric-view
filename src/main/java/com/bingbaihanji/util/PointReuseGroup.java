package com.bingbaihanji.util;

import com.bingbaihanji.view.layout.draw.geometry.impl.PointGeo;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 点复用组
 * <p>
 * 管理重合点的复用关系。当多个点启用复用后,它们会形成一个复用组,
 * 移动组内任意一个点时,其他点会同步移动。
 * <p>
 * 使用场景：
 * - 两个圆的圆心重合后,开启复用,移动时两个圆同步移动
 * - 线段端点与另一个点重合后,开启复用,形成联动关系
 *
 * @author bingbaihanji
 * @date 2026-01-05
 */
public class PointReuseGroup {

    /**
     * 全局复用组管理器(单例)
     */
    private static final PointReuseGroupManager MANAGER = new PointReuseGroupManager();

    /**
     * 复用组ID
     */
    private final String groupId;

    /**
     * 组内的点集合(使用线程安全的集合)
     */
    private final Set<PointGeo> members = new CopyOnWriteArraySet<>();

    /**
     * 复用是否启用
     */
    private boolean enabled = true;

    /**
     * 主点(组内的"代表"点,通常是最先加入的点)
     */
    private PointGeo masterPoint;

    /**
     * 私有构造函数,通过静态方法创建
     */
    private PointReuseGroup(String groupId) {
        this.groupId = groupId;
    }

    /**
     * 获取复用组管理器
     */
    public static PointReuseGroupManager getManager() {
        return MANAGER;
    }

    /**
     * 获取组ID
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * 添加点到复用组
     */
    public void addMember(PointGeo point) {
        if (point == null) return;

        if (members.isEmpty()) {
            masterPoint = point;
        }
        members.add(point);
        point.setReuseGroup(this);
    }

    /**
     * 从复用组移除点
     */
    public void removeMember(PointGeo point) {
        if (point == null) return;

        members.remove(point);
        point.setReuseGroup(null);

        // 如果移除的是主点,选择新的主点
        if (point == masterPoint && !members.isEmpty()) {
            masterPoint = members.iterator().next();
        }

        // 如果组内只剩一个点,解散复用组
        if (members.size() <= 1) {
            dissolve();
        }
    }

    /**
     * 获取组内所有成员
     */
    public Set<PointGeo> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    /**
     * 获取组内成员数量
     */
    public int getMemberCount() {
        return members.size();
    }

    /**
     * 获取主点
     */
    public PointGeo getMasterPoint() {
        return masterPoint;
    }

    /**
     * 设置主点
     */
    public void setMasterPoint(PointGeo point) {
        if (members.contains(point)) {
            this.masterPoint = point;
        }
    }

    /**
     * 复用是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置复用启用状态
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 同步移动组内所有点到指定位置
     *
     * @param targetX     目标X坐标
     * @param targetY     目标Y坐标
     * @param sourcePoint 触发移动的点(不重复更新此点)
     */
    public void syncMove(double targetX, double targetY, PointGeo sourcePoint) {
        if (!enabled) return;

        // 优化：批量更新,减少重复计算
        for (PointGeo member : members) {
            if (member != sourcePoint && !member.isSyncingPosition()) {
                // 直接设置坐标,避免递归调用
                member.setPositionDirectly(targetX, targetY);
            }
        }
    }

    /**
     * 解散复用组
     */
    public void dissolve() {
        // 使用副本避免 ConcurrentModificationException
        List<PointGeo> membersCopy = new ArrayList<>(members);
        for (PointGeo member : membersCopy) {
            member.setReuseGroup(null);
        }
        members.clear();
        masterPoint = null;
        MANAGER.removeGroup(this);
    }

    /**
     * 获取组内所有成员的名称列表(用于显示)
     */
    public String getMembersInfo() {
        if (members.isEmpty()) {
            return "空组";
        }

        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (PointGeo member : members) {
            if (count > 0) sb.append(", ");
            String name = member.getName();
            if (name != null && !name.isEmpty()) {
                sb.append(name);
            } else {
                sb.append(String.format("点(%.1f,%.1f)", member.getX(), member.getY()));
            }
            count++;
            // 最多显示5个点
            if (count >= 5) {
                if (members.size() > 5) {
                    sb.append(", ...");
                }
                break;
            }
        }
        return sb.toString();
    }

    /**
     * 检查点是否在此组内
     */
    public boolean containsMember(PointGeo point) {
        return members.contains(point);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PointReuseGroup that = (PointReuseGroup) o;
        return Objects.equals(groupId, that.groupId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId);
    }

    /**
     * 复用组管理器
     * <p>
     * 负责创建、查找和管理所有复用组
     */
    public static class PointReuseGroupManager {

        /**
         * 所有复用组
         */
        private final Map<String, PointReuseGroup> groups = new HashMap<>();

        /**
         * 组ID计数器
         */
        private int groupIdCounter = 0;

        /**
         * 创建新的复用组
         */
        public PointReuseGroup createGroup() {
            String groupId = "RG_" + (++groupIdCounter);
            PointReuseGroup group = new PointReuseGroup(groupId);
            groups.put(groupId, group);
            return group;
        }

        /**
         * 创建复用组并添加两个点
         */
        public PointReuseGroup createGroup(PointGeo point1, PointGeo point2) {
            // 如果两个点已经在同一个组,直接返回该组
            if (point1.getReuseGroup() != null && point1.getReuseGroup() == point2.getReuseGroup()) {
                return point1.getReuseGroup();
            }

            // 如果其中一个点已有复用组,将另一个点加入该组
            if (point1.getReuseGroup() != null) {
                point1.getReuseGroup().addMember(point2);
                return point1.getReuseGroup();
            }
            if (point2.getReuseGroup() != null) {
                point2.getReuseGroup().addMember(point1);
                return point2.getReuseGroup();
            }

            // 创建新组
            PointReuseGroup group = createGroup();
            group.addMember(point1);
            group.addMember(point2);
            return group;
        }

        /**
         * 根据ID获取复用组
         */
        public PointReuseGroup getGroup(String groupId) {
            return groups.get(groupId);
        }

        /**
         * 移除复用组
         */
        public void removeGroup(PointReuseGroup group) {
            if (group != null) {
                groups.remove(group.getGroupId());
            }
        }

        /**
         * 获取所有复用组
         */
        public Collection<PointReuseGroup> getAllGroups() {
            return Collections.unmodifiableCollection(groups.values());
        }

        /**
         * 清空所有复用组
         */
        public void clear() {
            for (PointReuseGroup group : new ArrayList<>(groups.values())) {
                group.dissolve();
            }
            groups.clear();
            groupIdCounter = 0;
        }

        /**
         * 获取复用组数量
         */
        public int getGroupCount() {
            return groups.size();
        }
    }
}
