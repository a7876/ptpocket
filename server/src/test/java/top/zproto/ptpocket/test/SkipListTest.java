package top.zproto.ptpocket.test;

import top.zproto.ptpocket.server.datestructure.DataObject;
import top.zproto.ptpocket.server.datestructure.SortedSet;

import java.util.*;

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
        for (int i = 0; i < 10000; i++) {
            set.add((lastNum = random.nextInt(10000)));
        }
        SortedSet sortedSet = new SortedSet();
        set.forEach(item -> {
            DataObject dataObject = new DataObject(4);
            dataObject.writeInt(item);
            sortedSet.insert(item, dataObject);
        });
        if (sortedSet.getSize() != set.size())
            throw new IllegalStateException();
        set.forEach(item -> {
            DataObject dataObject = new DataObject(4);
            dataObject.writeInt(item);
            if (sortedSet.getScore(dataObject) == null)
                throw new IllegalStateException();
        });
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
