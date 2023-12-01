package top.zproto.ptpocket.server.persistence.appendfile;

import top.zproto.ptpocket.server.entity.CommandPool;
import top.zproto.ptpocket.server.entity.ObjectPool;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * appendCommand对象池
 */
public class AppendCommandPool implements ObjectPool<AppendCommand> {
    public static final CommandPool instance = new CommandPool();
    private final ConcurrentLinkedQueue<AppendCommand> pool = new ConcurrentLinkedQueue<>();
    private static final int MAX_SIZE = 100;

    private int peakSize = 0; // 峰值大小


    @Override
    public void tryShrink() {
        while (pool.size() > MAX_SIZE)
            pool.poll();
    }

    public int getPeakSize() {
        return peakSize;
    }

    @Override
    public AppendCommand getObject() {
        AppendCommand c = pool.poll();
        if (c != null)
            return c;
        return new AppendCommand();
    }

    @Override
    public void returnObject(AppendCommand item) {
        if (pool.size() < MAX_SIZE) {
            item.clear();
            pool.add(item);
            int size = pool.size();
            if (size > peakSize)
                peakSize = size;
        }
    }
}
