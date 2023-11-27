package top.zproto.ptpocket.server.datestructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 有序列表实现，底层基于SkipList
 */
public class SortedSet {
    private final Hash hash = new Hash(false);
    private final SkipList skipList = new SkipList();

    public DataObject insert(double num, DataObject dataObject) {
        Double old = (Double) hash.get(dataObject);
        if (old != null) { // 已经存在
            skipList.remove(old, dataObject); // 先删除
        }
        skipList.insert(num, dataObject); // 插入
        hash.insert(dataObject, num); // 插入或修改
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

    public List<DataObject> getRange(int nums) {
        return skipList.getRange(nums);
    }

    public List<DataObject> getReverseRange(int nums) {
        return skipList.getReverseRange(nums);
    }

    public int getSize() {
        return skipList.size;
    }

    private static class SkipList {
        // 根据论文 max_level = log 1/p (N) N 为期待的最大元素，这里取 2 ^ 20 1024 * 1024
        private static final int MAX_LEVEL = 10;
        private static final double P = 0.25;
        int size = 0;

        private static class Node {
            double key;
            DataObject val;
            Node[] forward;
            Node backward; // 从后往前的指针

            public Node(double key, DataObject val, Node[] forward) {
                this.key = key;
                this.val = val;
                this.forward = forward;
            }
        }

        private int currentLevel = 1;
        private final Node head;
        private final Node tail;

        SkipList() {
            this.head = new Node(0, null, new Node[MAX_LEVEL]);
            this.tail = new Node(0, null, new Node[MAX_LEVEL]);
            for (int i = 0; i < MAX_LEVEL; i++) {
                head.forward[i] = tail;
            }
            tail.backward = head;
        }

        Node get(double key, DataObject dataObject) {
            Node node = head;
            for (int i = currentLevel - 1; i >= 0; i--) { // 这里基于一个事实，有i层高的必定又i-1层高
                Node tmp;
                while ((tmp = node.forward[i]) != tail && (key > tmp.key || key == tmp.key && !tmp.val.equals(dataObject))) // 不断前进
                    node = tmp;
                if (tmp != tail && key == tmp.key && tmp.val.equals(dataObject))
                    return tmp;
            }
            return null;
        }

        void insert(double key, DataObject value) {
            Node node = head;
            Node[] tmpArray = new Node[MAX_LEVEL]; // 缓存插入节点前经过的最近节点信息
            for (int i = currentLevel - 1; i >= 0; i--) {
                Node tmp;
                while ((tmp = node.forward[i]) != tail && key > tmp.key) // 不断前进
                    node = tmp;
                tmpArray[i] = node; // 次层的前置节点
            }
            // 插入
            int tmpLevel = getRandomLevel();
            if (tmpLevel > currentLevel) { // 超高了
                for (int i = currentLevel; i < tmpLevel; i++)
                    tmpArray[i] = head;
                currentLevel = tmpLevel;
            }
            node = new Node(key, value, new Node[tmpLevel]);
            Node next = tmpArray[0].forward[0];
            for (int i = 0; i < tmpLevel; i++) { // 调整指针
                node.forward[i] = tmpArray[i].forward[i];
                tmpArray[i].forward[i] = node;
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
                while ((tmp = node.forward[i]) != tail && (key > tmp.key || key == tmp.key && !tmp.val.equals(dataObject))) // 不断前进
                    node = tmp;
                tmpArray[i] = node; // 次层的前置节点
            }
            node = node.forward[0]; // 找到被删除节点
            if (node != null) {
                Double oldKey = node.key;
                for (int i = 0; i < currentLevel; i++) {
                    if (tmpArray[i].forward[i] != node)
                        break; // 没有和当前节点直接连接
                    tmpArray[i].forward[i] = node.forward[i];
                }
                Node next = node.forward[0];
                next.backward = tmpArray[0];
                while (currentLevel > 0 && head.forward[currentLevel - 1] == tail) currentLevel--; // 缩减高度
                size--;
                return oldKey;
            }
            // 没有找到
            return null;
        }

        Integer getRank(double score, DataObject dataObject) {
            Node node = get(score, dataObject);
            if (node == null)
                return null;
            int count = 0;
            while (node.backward != head) {
                count++;
                node = node.backward;
            }
            return count;
        }

        Integer getReverseRank(double score, DataObject dataObject) {
            Node node = get(score, dataObject);
            if (node == null)
                return null;
            int count = 0;
            Node next;
            while ((next = node.forward[0]) != tail) {
                count++;
                node = next;
            }
            return count;
        }

        List<DataObject> getRangeByScore(double start, double end) {
            Node node = head, tmp = null;
            for (int i = currentLevel - 1; i >= 0; i--) { // 这里基于一个事实，有i层高的必定又i-1层高
                while ((tmp = node.forward[i]) != tail && (start > tmp.key)) // 不断前进
                    node = tmp;
            }
            if (tmp == null)
                return Collections.emptyList();
            ArrayList<DataObject> res = new ArrayList<>();
            while (tmp != null && tmp.key <= end) {
                res.add(tmp.val);
                tmp = tmp.forward[0];
            }
            return res;
        }

        List<DataObject> getRange(int nums) {
            ArrayList<DataObject> res = new ArrayList<>();
            Node node = head.forward[0];
            int count = 0;
            while (node != tail && count < nums) {
                res.add(node.val);
                node = node.forward[0];
                count++;
            }
            return res;
        }

        List<DataObject> getReverseRange(int nums) {
            ArrayList<DataObject> res = new ArrayList<>();
            Node node = tail.backward;
            int count = 0;
            while (node != head && count < nums) {
                res.add(node.val);
                node = node.backward;
                count++;
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
    }
}
