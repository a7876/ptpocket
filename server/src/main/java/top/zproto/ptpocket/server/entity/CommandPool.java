package top.zproto.ptpocket.server.entity;

import java.util.concurrent.ConcurrentLinkedQueue;

public class CommandPool implements ObjectPool<Command, CommandPool> {
    // 对象池，获取命令对象
    public static final CommandPool instance = new CommandPool();
    private final ConcurrentLinkedQueue<Command> pool = new ConcurrentLinkedQueue<>();
    private static final int MAX_SIZE = 100;

    @Override
    public Command getObject() {
        Command c = pool.poll();
        if (c != null)
            return c;
        return new Command();
    }

    @Override
    public void returnObject(Command command) {
        if (pool.size() < MAX_SIZE)
            pool.add(command);
    }

    @Override
    public void tryShrink() {
        while (pool.size() > MAX_SIZE)
            pool.poll();
    }
}
