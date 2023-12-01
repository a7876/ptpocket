package top.zproto.ptpocket.server.datestructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 有序集合实现，底层基于SkipList
 * 支持O1查找对象score
 * 支持Ologn删除、查找排名
 */
public class SortedSet {
    private final Hash hash = new Hash(false);
    private final SkipList skipList = new SkipList();

    public DataObject insert(double score, DataObject dataObject) {
        Double old = (Double) hash.get(dataObject);
        if (old != null) { // 已经存在
            skipList.remove(old, dataObject); // 先删除
        }
        skipList.insert(score, dataObject); // 插入
        hash.insert(dataObject, score); // 插入或修改
        return null;
    }

    public Double remove(DataObject dataObject) {
        Double removed = (Double) hash.remove(dataObject);
        if (removed == null)
            return null;
        skipList.remove(removed, dataObject);
        return removed;
    }

    public Double getScore(DataObject dataObject) {
        return (Double) hash.get(dataObject);
    }

    public List<DataObject> getRangeByScore(double start, double end) {
        return skipList.getRangeByScore(start, end);
    }

    public Integer rank(DataObject dataObject) {
        Double key = (Double) hash.get(dataObject);
        if (key == null)
            return null;
        return skipList.getRank(key, dataObject);
    }

    public Integer reverseRank(DataObject dataObject) {
        Double key = (Double) hash.get(dataObject);
        if (key == null)
            return null;
        return skipList.getReverseRank(key, dataObject);
    }

    public List<DataObject> getRange(int offset, int nums) {
        return skipList.getRange(offset, nums);
    }

    public List<DataObject> getReverseRange(int offset, int nums) {
        return skipList.getReverseRange(offset, nums);
    }

    public int getSize() {
        return skipList.size;
    }

    private static class SkipList {
        // 注意这里是内部使用的SkipList，已经减去了很多不必要的检查同时也假设一些情况已经经过外层Hash表检查
        // 根据论文 max_level = log 1/p (N) N 为期待的最大元素，这里取 2 ^ 20 1024 * 1024
        private static final int MAX_LEVEL = 10;
        private static final double P = 0.25;
        int size = 0;

        private static class Node {
            double key;
            DataObject val;
            NodeAndSpan[] level;
            Node backward; // 从后往前的指针

            public Node(double key, DataObject val, NodeAndSpan[] level) {
                this.key = key;
                this.val = val;
                this.level = level;
            }
        }

        private static class NodeAndSpan {
            Node forward;
            int span; // 注意span应该是从当前到下一个节点中间横跨的距离
        }

        private int currentLevel = 1;
        private final Node head;
        private final Node tail;

        SkipList() {
            this.head = new Node(0, null, getNewLevel(MAX_LEVEL));
            this.tail = new Node(0, null, getNewLevel(MAX_LEVEL));
            for (int i = 0; i < MAX_LEVEL; i++) {
                head.level[i].forward = tail;
                head.level[i].span = 0;
            }
            tail.backward = head;
        }

        Node get(double key, DataObject dataObject) {
            Node node = head;
            for (int i = currentLevel - 1; i >= 0; i--) {
                Node tmp;
                while ((tmp = node.level[i].forward) != tail && (key > tmp.key || key == tmp.key && !tmp.val.equals(dataObject))) // 不断前进
                    node = tmp;
                if (tmp != tail && key == tmp.key && tmp.val.equals(dataObject))
                    return tmp;
            }
            return null;
        }

        void insert(double key, DataObject value) {
            Node node = head;
            Node[] tmpArray = new Node[MAX_LEVEL]; // 缓存插入节点前经过的最近节点信息
            int[] rank = new int[MAX_LEVEL];
            for (int i = currentLevel - 1; i >= 0; i--) {
                rank[i] = i == currentLevel - 1 ? 0 : rank[i + 1]; // 取得当前位置的累计
                Node tmp;
                while ((tmp = node.level[i].forward) != tail && key > tmp.key) { // 不断前进
                    rank[i] += node.level[i].span; // 累加距离
                    node = tmp;
                }
                tmpArray[i] = node; // 次层的前置节点
            }
            // 插入
            int tmpLevel = getRandomLevel();
            if (tmpLevel > currentLevel) { // 超高了
                for (int i = currentLevel; i < tmpLevel; i++) {
                    tmpArray[i] = head;
                    head.level[i].span = size; // 对于head这些新层应该还是横跨整个跳表
                }
                currentLevel = tmpLevel;
            }
            node = new Node(key, value, getNewLevel(tmpLevel));
            Node next = tmpArray[0].level[0].forward;
            for (int i = 0; i < tmpLevel; i++) { // 调整指针
                node.level[i].forward = tmpArray[i].level[i].forward;
                tmpArray[i].level[i].forward = node;
                node.level[i].span = tmpArray[i].level[i].span - (rank[0] - rank[i]); // 设置当前节点当前层的span
                tmpArray[i].level[i].span = rank[0] - rank[i] + 1; // 更新前置节点的span
            }
            // 这时候要注意可能当前层不够高有一些节点没有触及，此时这种也要加一
            for (int i = tmpLevel; i < currentLevel; i++) {
                tmpArray[i].level[i].span++;
            }
            node.backward = tmpArray[0]; // 记录前驱节点
            next.backward = node;
            size++;
        }

        Double remove(double key, DataObject dataObject) {
            Node node = head;
            Node[] tmpArray = new Node[currentLevel];
            for (int i = currentLevel - 1; i >= 0; i--) {
                Node tmp;
                while ((tmp = node.level[i].forward) != tail && (key > tmp.key || key == tmp.key && !tmp.val.equals(dataObject))) // 不断前进
                    node = tmp;
                tmpArray[i] = node; // 次层的前置节点
            }
            node = node.level[0].forward; // 找到被删除节点
            if (node != tail && node.val.equals(dataObject)) {
                Double oldKey = node.key;
                for (int i = 0; i < currentLevel; i++) {
                    if (tmpArray[i].level[i].forward != node) { // 没有连接到当前的点直接减少span即可
                        tmpArray[i].level[i].span--;
                    } else {
                        tmpArray[i].level[i].forward = node.level[i].forward;
                        tmpArray[i].level[i].span += node.level[i].span - 1; // 更新span
                    }
                }
                Node next = node.level[0].forward;
                next.backward = tmpArray[0];
                while (currentLevel > 0 && head.level[currentLevel - 1].forward == tail) currentLevel--; // 缩减高度
                size--;
                return oldKey;
            }
            // 没有找到
            return null;
        }

        int getRank(double score, DataObject dataObject) {
            Node node = head;
            int rank = 0;
            for (int i = currentLevel - 1; i >= 0; i--) {
                Node tmp;
                while ((tmp = node.level[i].forward) != tail && (score > tmp.key || score == tmp.key && !tmp.val.equals(dataObject))) { // 不断前进
                    rank += node.level[i].span;
                    node = tmp;
                }
            }
            return dataObject.equals(node.level[0].forward.val) ? rank + 1 : -1;
        }

        int getReverseRank(double score, DataObject dataObject) {
            int rank = getRank(score, dataObject);
            return size - rank + 1;
        }

        List<DataObject> getRangeByScore(double start, double end) {
            Node node = head, tmp = null;
            for (int i = currentLevel - 1; i >= 0; i--) {
                while ((tmp = node.level[i].forward) != tail && (start > tmp.key)) // 不断前进
                    node = tmp;
            }
            if (tmp == null || tmp == tail)
                return Collections.emptyList();
            ArrayList<DataObject> res = new ArrayList<>();
            while (tmp != tail && tmp.key <= end) {
                res.add(tmp.val);
                tmp = tmp.level[0].forward;
            }
            return res;
        }

        List<DataObject> getRange(int offset, int nums) {
            ArrayList<DataObject> res = new ArrayList<>();
            if (offset >= size)
                return res;
            Node node = head;
            int rank = 0;
            for (int i = currentLevel - 1; i >= 0; i--) {
                Node tmp;
                while ((tmp = node.level[i].forward) != tail) { // 不断前进
                    if (rank + node.level[i].span > offset) {
                        break;
                    }
                    rank += node.level[i].span;
                    node = tmp;
                }
            }
            offset -= rank;
            for (int i = 0; i < offset; i++) {
                node = node.level[0].forward;
            }
            for (int i = 0; i < nums; i++) {
                node = node.level[0].forward;
                if (node == tail) {
                    break;
                }
                res.add(node.val);
            }
            return res;
        }

        List<DataObject> getReverseRange(int offset, int nums) {
            ArrayList<DataObject> res = new ArrayList<>();
            if (offset >= size)
                return res;
            Node node = head;
            offset = size - offset;
            int rank = 0;
            for (int i = currentLevel - 1; i >= 0; i--) {
                Node tmp;
                while ((tmp = node.level[i].forward) != tail) { // 不断前进
                    if (rank + node.level[i].span > offset) {
                        break;
                    }
                    rank += node.level[i].span;
                    node = tmp;
                }
            }
            offset -= rank;
            for (int i = 0; i < offset; i++) {
                node = node.level[0].forward;
            }
            for (int i = 0; i < nums; i++) {
                if (node == head) {
                    break;
                }
                res.add(node.val);
                node = node.backward;
            }
            return res;
        }

        // get random level
        private int getRandomLevel() {
            int level = 1;
            while (Math.random() < P && level < MAX_LEVEL)
                level++;
            return level;
        }

        private NodeAndSpan[] getNewLevel(int level) {
            NodeAndSpan[] nodeAndSpans = new NodeAndSpan[level];
            for (int i = 0; i < level; i++) {
                nodeAndSpans[i] = new NodeAndSpan();
            }
            return nodeAndSpans;
        }
    }
}
