package top.zproto.ptpocket.server.entity;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ResponsePool implements ObjectPool<Response> {
    // 对象池，获取响应对象
    public static final ResponsePool instance = new ResponsePool();
    private final ConcurrentLinkedQueue<Response> pool = new ConcurrentLinkedQueue<>();
    private static final int MAX_SIZE = 50;
    private int heapSize = 0;

    @Override
    public Response getObject() {
        Response r = pool.poll();
        if (r != null)
            return r;
        return new Response();
    }

    @Override
    public void returnObject(Response response) { // 这个方法一定要注意不能对一个对象调用多次
        if (pool.size() < MAX_SIZE) {
            response.clear();
            pool.add(response);
            int size = pool.size();
            if (size > heapSize)
                heapSize = size;
        }
    }

    @Override
    public void tryShrink() {
        while (pool.size() > MAX_SIZE)
            pool.poll();
    }

    public int getHeapSize() {
        return heapSize;
    }
}
