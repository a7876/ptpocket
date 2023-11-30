package top.zproto.ptpocket.client.test;

import top.zproto.ptpocket.client.core.Client;
import top.zproto.ptpocket.client.utils.ObjectDecoder;
import top.zproto.ptpocket.client.utils.ObjectEncoder;
import top.zproto.ptpocket.client.utils.PocketTemplate;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

public class Test {

    // 一秒钟内可以处理两万条命令
    public static void main(String[] args) {
        Test test = new Test();
        test.warmUp();
        test.performanceTest();
    }


    private void warmUp() {
        Client client = Client.getInstance("localhost", 7878);
        PocketTemplate<String> template =
                new PocketTemplate<>(client, new StringEncoder(), new StringDecoder(), (byte) 0);
        Random random = new Random();
        for (int i = 0; i < 100000; i++) {
            int key = random.nextInt(100000);
            template.set(" " + key, " " + key);
        }
        System.out.println("warm up done!");
    }

    private void performanceTest() {
        AtomicLong l = new AtomicLong();
        Runnable r = () -> {
            Client client = Client.getInstance("localhost", 7878);
            PocketTemplate<String> template =
                    new PocketTemplate<>(client, new StringEncoder(), new StringDecoder(), (byte) 0);
            long time = System.currentTimeMillis();
            for (int i = 0; i < 10000; i++) {
                template.set("name", "lili");
                template.get("name");
            }
            l.addAndGet(System.currentTimeMillis() - time);
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
        Client client = Client.getInstance("localhost", 7878);
        PocketTemplate<String> template =
                new PocketTemplate<>(client, new StringEncoder(), new StringDecoder(), (byte) 0);
        System.out.println(template.info());
        System.out.println("total " + l.get());
        System.exit(0);
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
