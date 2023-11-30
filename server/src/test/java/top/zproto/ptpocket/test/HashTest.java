package top.zproto.ptpocket.test;

import top.zproto.ptpocket.server.datestructure.DataObject;
import top.zproto.ptpocket.server.datestructure.Hash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class HashTest {
    static final HashTest tester = new HashTest();

    public static void main(String[] args) {
//        tester.hashFunctionTest();
        tester.hashTest();
//        tester.performanceTest();
    }

    void hashFunctionTest() {
        Random random = new Random();
        DataObject pre = new DataObject(10);
        for (int i = 0; i < 100000; i++) {
            int len = random.nextInt(100);
            DataObject dataObject = new DataObject(len);
            for (int j = 0; j < len; j++) {
                dataObject.write((byte) random.nextInt(100));
            }
            if (dataObject.hashCode() != dataObject.hashCode()) {
                throw new IllegalStateException("hash function return different value for the self object");
            }
            if (pre.equals(dataObject) && pre.hashCode() != dataObject.hashCode()) {
                throw new IllegalStateException("equal object don't have same hash");
            }
            pre = dataObject;
        }
    }

    void hashTest() {
        Hash hash = new Hash(true);
        HashMap<DataObject, DataObject> map = new HashMap<>();
        Random random = new Random();
        int limit = Math.max(1000, random.nextInt(100000));
        for (int i = 0; i < limit; i++) {
            int i1 = random.nextInt(100);
            DataObject d = new DataObject(4);
            d.writeInt(i1);
            map.put(d, d);
            hash.insert(d, d);
            if (random.nextBoolean()) {
                hash.remove(d);
                map.remove(d);
            }
            if (map.size() != hash.getSize())
                throw new IllegalStateException();
        }
        map.forEach((k, v) -> {
            if (hash.get(k) != v)
                throw new IllegalStateException();
        });
    }

    void performanceTest() {
        int times = 100;
        AtomicLong t1 = new AtomicLong();
        AtomicLong t2 = new AtomicLong();
        ArrayList<Long> l1 = new ArrayList<>();
        ArrayList<Long> l2 = new ArrayList<>();
        for (int i = 0; i < times; i++) {
            hashPerformance(l1, l2);
            l1.forEach(t1::addAndGet);
            l2.forEach(t2::addAndGet);
        }
        System.out.println("t1 avg " + (t1.get() / times));
        System.out.println("t2 avg " + (t2.get() / times));
    }

    void hashPerformance(List<Long> timeList0, List<Long> timeList1) {
        Hash hash = new Hash(true);
        HashMap<DataObject, DataObject> map = new HashMap<>(1 << 6);
        Random random = new Random();
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 1000000; i++) {
            list.add(random.nextInt(100000));
        }
        long time = System.currentTimeMillis();
        list.forEach(i -> {
            DataObject dataObject = new DataObject(4);
            dataObject.writeInt(i);
            map.put(dataObject, dataObject);
        });
        list.forEach(i -> {
            DataObject dataObject = new DataObject(4);
            dataObject.writeInt(i);
            map.remove(dataObject);
        });
        timeList0.add(System.currentTimeMillis() - time);
        time = System.currentTimeMillis();
        list.forEach(i -> {
            DataObject dataObject = new DataObject(4);
            dataObject.writeInt(i);
            hash.insert(dataObject, dataObject);
        });
        list.forEach(i -> {
            DataObject dataObject = new DataObject(4);
            dataObject.writeInt(i);
            hash.remove(dataObject);
        });
        timeList1.add(System.currentTimeMillis() - time);
    }
}
