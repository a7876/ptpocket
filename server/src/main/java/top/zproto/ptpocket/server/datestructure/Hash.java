package top.zproto.ptpocket.server.datestructure;

import java.util.function.Consumer;

/**
 * 哈希表实现
 * 支持渐进rehash
 */
public class Hash implements DataStructure {
    // 主库和副库，副库再rehash时和主库一起使用
    private HashImpl main, sub;
    // rehash进度
    private int currentBucket = -1; // 初始化为-1

    public Hash(boolean bigInit) { // 键空间使用大表初始化
        if (bigInit) {
            main = HashImpl.getBigInit();
        } else {
            main = HashImpl.getInstance();
        }
    }

    /**
     * 代理获取方法
     */
    public Object get(DataObject key) {
        if (currentBucket == -1) // 非rehash阶段
            return main.get(key);
        Object value = sub.get(key); // rehash中则先查子库
        if (value != null)
            return value;
        return main.get(key);
    }

    /**
     * 代理删除方法
     */
    public Object remove(DataObject key) {
        Object returnValue = main.remove(key); // 不管如何都要从main中删
        if (returnValue == null && currentBucket != -1) { // rehash阶段
            returnValue = sub.remove(key); // 看看sub中有没有
        }
        checkShrink();
        return returnValue;
    }

    /**
     * 代理插入
     * 这个插入实现保证同一时间Hash中不会有相同的key
     */
    public Object insert(DataObject key, Object value) {
        Object old;
        if (currentBucket == -1) { // 没有在rehash插入main
            old = main.insert(key, value);
        } else { // 在rehash阶段
            old = main.remove(key); // 尝试删除main中的旧值
            Object insert = sub.insert(key, value); // 插入新的表
            if (insert != null)
                old = insert;
        }
        checkExpand();
        return old;
    }

    /**
     * rehash阈值
     */
    static final float LOAD_FACTOR = 0.9f;

    /**
     * 检查rehash增大
     */
    private void checkExpand() {
        if (currentBucket != -1) { // 正在rehash
            advanceRehash();
            return;
        }
        final int size = main.size;
        final int capacity = main.table.length;
        if (size >= capacity * LOAD_FACTOR) { // 判断是否需要增大
            int newSize = main.getNextAvailableHashExpandSize();
            if (newSize == 0) // 不能再rehash
                return;
            rehashStart(newSize);
            advanceRehash();
        }
    }

    /**
     * 检查rehash缩小
     */
    private void checkShrink() {
        if (currentBucket != -1) {
            advanceRehash();
            return;
        }
        final int size = main.size;
        final int capacity = main.table.length;
        if (size < capacity * LOAD_FACTOR / 4) { // 判断是否需要减小
            int newSize = main.getNextAvailableHashShrinkSize();
            if (newSize == 0) // 不能再rehash
                return;
            rehashStart(newSize);
            advanceRehash();
        }
    }

    // rehash一次推进元素量
    private static final int REHASH_BATCH = 100;

    /**
     * 推进rehash
     */
    private void advanceRehash() {
        int index = currentBucket;
        int count = 0; // 当前表rehash计数
        HashImpl.HashNode[] table = main.table;
        final int size = table.length;

        while (count < REHASH_BATCH) {
            HashImpl.HashNode node = table[index];
            while (node != null) {
                sub.insert(node.key, node.value); // 使用专门的插入方法
                node = node.next;
                count++;
            }
            table[index] = null; // 将当前列置空
            index++;
            if (index == size) { // rehash完毕
                rehashFinish();
                return; // 完毕直接返回
            }
        }
        currentBucket = index; // 更新即将rehash到的桶
        main.size -= count;
    }

    private void rehashFinish() {
        main = sub;
        sub = null;
        currentBucket = -1;
    }

    private void rehashStart(int newSize) {
        sub = new HashImpl(newSize);
        currentBucket = 0;
    }

    /**
     * 专门为定期rehash检查实现的方法
     */
    public void cronCheckRehash() {
        if (currentBucket != -1)
            advanceRehash();
    }

    /**
     * 专门为检查过期键实现的方法
     */
    public int checkExpire(int limit, Hash partnerHash, Consumer<DataObject> forExpiredKey) {
        if (currentBucket != -1) // rehash中不执行过期检查
            return 0;
        int count = 0;
        long current = System.currentTimeMillis();
        HashImpl.HashNode[] table = main.table;
        for (int i = 0; i < table.length && count < limit; i++) { // 一个个桶检查
            HashImpl.HashNode node = table[i];
            if (node == null)
                continue;
            while (node != null && ((Long) node.value) <= current) { // 处理队头
                partnerHash.remove(node.key); // 删除对应的键
                forExpiredKey.accept(node.key); // 调用钩子
                table[i] = node.next;
                node = node.next;
                count++;
            }
            if (node == null)
                continue;
            while (node.next != null) {
                if (((Long) node.next.value) <= current) {
                    partnerHash.remove(node.next.key); // 删除对应的键
                    forExpiredKey.accept(node.next.key); // 调用钩子
                    node.next = node.next.next; // 删除节点
                    count++;
                } else {
                    node = node.next; // 推进一次
                }
            }
        }
        return count;
    }

    public int getSize() {
        int size = main.size;
        if (currentBucket != -1)
            size += sub.size;
        return size;
    }

    /**
     * 内部hash表实现
     */
    public static class HashImpl {
        static final int MAX_SIZE = 1 << 30;
        static final int INIT = 1 << 4; // default 16
        static final int BIG_INIT = 1 << 6;
        HashNode[] table;
        int size = 0;

        private int hash(DataObject dataObject) {
            return table.length - 1 & dataObject.hashCode();
        }

        /**
         * 获取元素
         */
        Object get(DataObject key) {
            HashNode node = table[hash(key)];
            while (node != null) {
                if (node.key.equals(key))
                    return node.value;
                node = node.next;
            }
            return null;
        }

        /**
         * 插入元素
         */
        Object insert(DataObject key, Object value) {
            HashNode newNode = new HashNode(key, value);
            HashNode node = table[hash(key)];
            while (node != null) {
                if (node.key.equals(key)) {
                    Object old = node.value;
                    node.value = value;
                    return old;
                }
                node = node.next;
            }
            table[hash(key)] = newNode.setNext(table[hash(key)]); // 总是头插入
            size++;
            return null;
        }

        /**
         * 删除元素
         */
        Object remove(DataObject key) {
            HashNode node = table[hash(key)];
            if (node == null)
                return null;
            if (node.key.equals(key)) {
                Object old = node.value;
                table[hash(key)] = node.next;
                size--;
                return old;
            }
            while (node.next != null) {
                if (node.next.key.equals(key)) {
                    Object old = node.next.value;
                    node.next = node.next.next;
                    size--;
                    return old;
                }
                node = node.next;
            }
            return null;
        }

        /**
         * 获取下次的增长大小
         */
        int getNextAvailableHashExpandSize() {
            int current = this.table.length;
            if (current == MAX_SIZE)
                return 0; // 不能再扩容
            return current << 1;
        }

        /**
         * 获取缩容大小
         */
        int getNextAvailableHashShrinkSize() {
            int current = this.table.length;
            if (current <= BIG_INIT)
                return 0; // 不能缩容
            return current >>> 1;
        }


        static class HashNode {
            DataObject key;
            Object value;
            HashNode next;

            public HashNode(DataObject key, Object value) {
                this.key = key;
                this.value = value;
            }

            HashNode setNext(HashNode next) {
                this.next = next;
                return this;
            }
        }

        //
        //  获取对象
        //
        static HashImpl getBigInit() { // 大初始值的Hash表
            return new HashImpl(BIG_INIT);
        }

        static HashImpl getInstance() { // 默认始值的Hash表
            return new HashImpl(INIT);
        }

        private HashImpl(int capacity) {
            table = new HashNode[capacity];
        }
    }
}
