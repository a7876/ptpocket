package top.zproto.ptpocket.test;

import top.zproto.ptpocket.server.datestructure.DataObject;
import top.zproto.ptpocket.server.datestructure.SortedSet;

import java.util.*;
import java.util.stream.Collectors;

public class SkipListTest {
    static SkipListTest instance = new SkipListTest();

    public static void main(String[] args) {
        instance.insertTest();
        instance.skipListMixTest();
    }

    void insertTest() {
        DataObject d1 = new DataObject(4);
        DataObject d2 = new DataObject(4);
        DataObject d3 = new DataObject(4);
        SortedSet sortedSet = new SortedSet();
        sortedSet.insert(1, d1);
        sortedSet.insert(2, d2);
        sortedSet.insert(3, d3);
        if (sortedSet.getScore(d1) != 3)
            throw new IllegalStateException();
        if (sortedSet.rank(d1) != 0)
            throw new IllegalStateException();
        if (sortedSet.reverseRank(d1) != 0)
            throw new IllegalStateException();
        sortedSet.remove(d1);
        if (sortedSet.getScore(d1) != null)
            throw new IllegalStateException();
        if (sortedSet.getSize() != 0)
            throw new IllegalStateException();
    }

    void skipListMixTest() {
        Random random = new Random();
        Set<Integer> set = new HashSet<>();
        int lastNum = 0;
        for (int i = 0; i < 10000; i++) { // 生成数据
            set.add((lastNum = random.nextInt(10000)));
        }
        SortedSet sortedSet = new SortedSet();
        Iterator<Integer> iterator = set.iterator();
        while (iterator.hasNext()){ // 插入sortedset
            Integer item = iterator.next();
            DataObject dataObject = new DataObject(4);
            dataObject.writeInt(item);
            sortedSet.insert(item, dataObject);
            if (random.nextBoolean() && item != lastNum){ // 随机删除
                iterator.remove();
                sortedSet.remove(dataObject);
            }
        }
        if (sortedSet.getSize() != set.size())
            throw new IllegalStateException();
        set.forEach(item -> { // 检查元素是否相等
            DataObject dataObject = new DataObject(4);
            dataObject.writeInt(item);
            if (sortedSet.getScore(dataObject) == null)
                throw new IllegalStateException();
        });

        // getRangeByScore方法检测
        {
            List<Integer> list = new ArrayList<>(set);
            list.sort(Comparator.comparingInt(i -> i));
            int size = Math.min(15, set.size());
            List<Integer> range = list.stream().limit(size).collect(Collectors.toList());
            int first = range.get(0);
            int last = range.get(size - 1);
            List<DataObject> rangeByScore = sortedSet.getRangeByScore(first, last);
            if (range.size() != rangeByScore.size())
                throw new IllegalStateException();
            for (int i = 0; i < size; i++) {
                DataObject dataObject = new DataObject(4);
                dataObject.writeInt(range.get(i));
                if (!dataObject.equals(rangeByScore.get(i)))
                    throw new IllegalStateException();
            }
        }

        // getRange range方法检测
        {
            List<Integer> list = new ArrayList<>(set);
            list.sort(Comparator.comparingInt(i -> i));
            int size = Math.min(15, set.size());
            List<Integer> range = list.stream().limit(size).collect(Collectors.toList());
            List<DataObject> srange = sortedSet.getRange(size);
            if (range.size() != srange.size())
                throw new IllegalStateException();
            for (int i = 0; i < size; i++) {
                DataObject dataObject = new DataObject(4);
                dataObject.writeInt(range.get(i));
                if (!dataObject.equals(srange.get(i)))
                    throw new IllegalStateException();
            }
            range.clear();
            int count = 0;
            while (count < size){
                range.add(list.get(list.size() - count - 1));
                count++;
            }
            srange = sortedSet.getReverseRange(size);
            if (range.size() != srange.size())
                throw new IllegalStateException();
            for (int i = 0; i < size; i++) {
                DataObject dataObject = new DataObject(4);
                dataObject.writeInt(range.get(i));
                if (!dataObject.equals(srange.get(i)))
                    throw new IllegalStateException();
            }
        }


        // rank方法测试
        {
            List<Integer> list = new ArrayList<>(set);
            list.sort(Comparator.comparingInt(i -> i));
            DataObject dataObject = new DataObject(4);
            dataObject.writeInt(lastNum);
            int rank = sortedSet.rank(dataObject);
            int lr = 0;
            for (int i : list)
                if (i < lastNum)
                    lr++;
            if (rank != lr)
                throw new IllegalStateException();
            int reverseRank = sortedSet.reverseRank(dataObject);
            lr = 0;
            list.sort(Comparator.reverseOrder());
            for (int i : list)
                if (i > lastNum)
                    lr++;
            if (reverseRank != lr)
                throw new IllegalStateException();
        }
        set.forEach(item -> {
            DataObject dataObject = new DataObject(4);
            dataObject.writeInt(item);
            sortedSet.remove(dataObject);
        });
        if (sortedSet.getSize() != 0)
            throw new IllegalStateException();
    }
}
