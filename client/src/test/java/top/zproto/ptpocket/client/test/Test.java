package top.zproto.ptpocket.client.test;

import top.zproto.ptpocket.client.core.Client;
import top.zproto.ptpocket.client.utils.ObjectDecoder;
import top.zproto.ptpocket.client.utils.ObjectEncoder;
import top.zproto.ptpocket.client.utils.PocketTemplate;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

public class Test {

    // 一秒钟内可以处理两万条命令
    public static void main(String[] args) throws IOException, InterruptedException {
        Test test = new Test();
        test.getServerInfo();
        test.warmUp();
        test.performanceTest();
        test.hashTest();
        test.innerHashTest();
        test.sortedSetTest();
        test.otherCommand();
    }


    private void warmUp() throws IOException {
        PocketTemplate<String> template = getTemplate();
        Random random = new Random();
        for (int i = 0; i < 100000; i++) {
            int key = random.nextInt(100000);
            template.set(" " + key, " " + key);
        }
        System.out.println("warm up done!");
        template.close();
    }

    private void otherCommand() throws InterruptedException, IOException {
        // expire
        PocketTemplate<String> template = getTemplate();
        String key = "name", value = "liming";
        template.set(key, value);
        if (!template.get(key).equals(value))
            throw new IllegalStateException();
        template.expire(key, 1);
        Thread.sleep(1000);
        if (template.get(key) != null)
            throw new IllegalStateException();

        // expireMill
        template.set(key, value);
        if (!template.get(key).equals(value))
            throw new IllegalStateException();
        template.expireMill(key, 1000);
        Thread.sleep(1000);
        if (template.get(key) != null)
            throw new IllegalStateException();

        // persist
        template.set(key, value);
        if (!template.get(key).equals(value))
            throw new IllegalStateException();
        template.expireMill(key, 1000);
        template.persist(key);
        Thread.sleep(1000);
        if (template.get(key) == null)
            throw new IllegalStateException();

        // select
        template.select((byte) 2);
        template.set(key, value);
        template.select((byte) 3);
        if (template.get(key) != null)
            throw new IllegalStateException();
        template.del(key);
        template.select((byte) 2);
        if (!value.equals(template.get(key)))
            throw new IllegalStateException();
        template.del(key);

//        System.out.println(template.stop());

        System.out.println("other command test finish");
        template.close();
    }

    private void getServerInfo() throws IOException {
        PocketTemplate<String> template = getTemplate();
        System.out.println(template.info());
        template.close();
    }

    private void hashTest() throws IOException {
        PocketTemplate<String> template = getTemplate();
        Random random = new Random();
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < 10000; i++) {
            String s = "" + random.nextInt(100000);
            map.put(s, s);
            template.set(s, s);
            if (random.nextBoolean()) {
                map.remove(s);
                template.del(s);
            }
        }
        map.keySet().forEach(k -> {
            String s = template.get(k);
            if (!k.equals(s))
                throw new IllegalStateException();
        });
        System.out.println("hashTestCompleted total size " + map.size());
        template.close();
    }

    private void innerHashTest() throws IOException {
        PocketTemplate<String> template = getTemplate();
        Random random = new Random();
        Map<String, String> map = new HashMap<>();
        String key = "inner Test";
        for (int i = 0; i < 10000; i++) {
            String s = "" + random.nextInt(100000);
            map.put(s, s);
            template.hSet(key, s, s);
            if (random.nextBoolean()) {
                map.remove(s);
                template.hDel(key, s);
            }
        }
        map.keySet().forEach(k -> {
            String s = template.hGet(key, k);
            if (!k.equals(s))
                throw new IllegalStateException();
        });
        template.del(key);
        System.out.println("innerHashTestCompleted total size " + map.size());
        template.close();
    }

    private void sortedSetTest() throws IOException {
        PocketTemplate<String> template = getTemplate();
        Random random = new Random();
        Map<Integer, Integer> map = new HashMap<>();
        String key = "sorted set Test";
        template.del(key);
        int min = 100000, max = -1;
        for (int i = 0; i < 10000; i++) {
            int num = random.nextInt(100000);
            map.put(num, num);
            String s = "" + num;
            template.zAdd(key, num, s);
            boolean isDeleted = false;
            if (random.nextBoolean()) {
                isDeleted = true;
                map.remove(num);
                template.zDel(key, s);
            }
            if (!isDeleted) {
                min = Math.min(min, num);
                max = Math.max(max, num);
            }
        }
        map.keySet().forEach(k -> {
            String value = "" + k;
            Integer num = (int) template.zScore(key, value);
            if (!k.equals(num))
                throw new IllegalStateException();
        });
        ArrayList<Integer> nums = new ArrayList<>(map.values());

        nums.sort(Comparator.comparingInt(i -> i));
        // rangeByScore
        List<String> byScore = template.zRangeScore(key, min, max);
        for (int i = 0; i < nums.size(); i++) {
            String value = "" + nums.get(i);
            if (!value.equals(byScore.get(i))) {
                throw new IllegalStateException();
            }
        }

        // rank
        for (int i = 0; i < nums.size(); i++) {
            String value = "" + nums.get(i);
            int rank = template.zRank(key, value);
            if (rank != i + 1) {
                System.out.println(rank);
                throw new IllegalStateException();
            }
        }
        // range
        int rangeTO = random.nextInt(nums.size());
        List<String> strings = template.zRange(key, 0, rangeTO);
        for (int i = 0; i < rangeTO; i++) {
            String value = "" + nums.get(i);
            if (!value.equals(strings.get(i))) {
                throw new IllegalStateException();
            }
        }

        // reverseRank
        nums.sort(Comparator.reverseOrder());
        for (int i = 0; i < nums.size(); i++) {
            String value = "" + nums.get(i);
            int rank = template.zReverseRank(key, value);
            if (rank != i + 1) {
                System.out.println(rank);
                throw new IllegalStateException();
            }
        }

        //reverse range
        rangeTO = random.nextInt(nums.size());
        strings = template.zReverseRange(key, 0, rangeTO);
        for (int i = 0; i < rangeTO; i++) {
            String value = "" + nums.get(i);
            if (!value.equals(strings.get(i))) {
                throw new IllegalStateException();
            }
        }

        template.del(key);
        System.out.println("innerSortedSetTestCompleted total size " + map.size());
        template.close();
    }

    private PocketTemplate<String> getTemplate() {
        Client client = Client.getInstance("localhost", 7878);
        return new PocketTemplate<>(client, new StringEncoder(), new StringDecoder(), (byte) 0);
    }

    private void performanceTest() throws IOException { // 性能测试
        AtomicLong l = new AtomicLong();
        Runnable r = () -> {
            PocketTemplate<String> template = getTemplate();
            long time = System.currentTimeMillis();
            for (int i = 0; i < 10000; i++) {
                template.set("name", "lili");
                template.get("name");
            }
            l.addAndGet(System.currentTimeMillis() - time);
            try {
                template.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        ArrayList<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            futures.add(executorService.submit(r));
        }
        futures.forEach(f -> {
            try {
                f.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        PocketTemplate<String> template = getTemplate();
        System.out.println(template.info());
        System.out.println("total " + l.get());
        executorService.shutdown();
        template.close();
    }

    private static class StringDecoder implements ObjectDecoder<String> {
        @Override
        public String decode(byte[] bytes) {
            if (bytes == null)
                return null;
            return new String(bytes);
        }
    }

    private static class StringEncoder implements ObjectEncoder<String> {
        @Override
        public byte[] encode(String o) {
            if (o == null)
                return null;
            return o.getBytes();
        }
    }
}
