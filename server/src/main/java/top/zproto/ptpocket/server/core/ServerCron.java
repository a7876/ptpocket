package top.zproto.ptpocket.server.core;

import top.zproto.ptpocket.server.entity.CommandPool;
import top.zproto.ptpocket.server.entity.ResponsePool;
import top.zproto.ptpocket.server.log.Logger;

public class ServerCron implements TimeEvent {
    private final ServerHolder server = ServerHolder.INSTANCE;
    private final Logger logger = Logger.DEFAULT;

    @Override
    public void processTimeEvent() {
        keySpaceRehashCheck(ONCE_CHECK);
        memoryWatch();
        checkExpireKey();
        checkObjectPool();
        logger.info("time event!");
    }

    // 上次检查到的数据库序号
    private int lastTimeCheck = 0;
    // 一次最多检查多少个库
    private static final int ONCE_CHECK = 10;

    // 定期检查所有库是否需要rehash
    private void keySpaceRehashCheck(int limit) {
        Database[] dbs = server.dbs;
        for (int i = 0; i < limit; i++) {
            lastTimeCheck %= dbs.length;
            dbs[lastTimeCheck].keyspace.cronCheckRehash();
            dbs[lastTimeCheck].expire.cronCheckRehash();
            lastTimeCheck++;
        }
    }

    // 内存检查
    private void memoryWatch() {
    }

    private static final int ONCE_CHECK_EXPIRE = 100;
    private int lastCheckExpire = 0;

    // 主动检查过期键
    private void checkExpireKey() {
        Database[] dbs = server.dbs;
        int count = 0;
        int alreadyCheckDb = 0;
        int eachDb = ONCE_CHECK_EXPIRE / 5;
        while (count < ONCE_CHECK_EXPIRE && alreadyCheckDb < dbs.length) {
            lastCheckExpire %= dbs.length;
            count += dbs[lastCheckExpire].expire.checkExpire(eachDb, dbs[lastCheckExpire].keyspace);
            lastCheckExpire++;
            alreadyCheckDb++;
        }
        if (count < ONCE_CHECK_EXPIRE / 2) { // 不够,有可能是有库在rehash
            keySpaceRehashCheck(ONCE_CHECK / 2); // 再尝试推进rehash
        }
    }

    private void checkObjectPool() {
        CommandPool.instance.tryShrink();
        ResponsePool.instance.tryShrink();
    }
}
