package top.zproto.ptpocket.server.core;

import top.zproto.ptpocket.server.datestructure.DataObject;
import top.zproto.ptpocket.server.entity.Command;
import top.zproto.ptpocket.server.entity.CommandPool;
import top.zproto.ptpocket.server.entity.ResponsePool;
import top.zproto.ptpocket.server.log.Logger;
import top.zproto.ptpocket.server.persistence.appendfile.AppendCommand;
import top.zproto.ptpocket.server.persistence.appendfile.AppendCommandPool;

import java.io.IOException;

public class ServerCron implements TimeEvent {
    private final ServerHolder server = ServerHolder.INSTANCE;
    private final Logger logger = Logger.DEFAULT;

    private long lastTimeAlreadyProcess = 0;
    private long lastCronTime = System.currentTimeMillis();
    private long currentTime; // 每次进入时间事件时的时间，减少对时间精确度要求不高的场景获取时间的开支
    private final ServerConfiguration configuration;

    ServerCron(ServerConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void processTimeEvent() {
        currentTime = System.currentTimeMillis();
        keySpaceRehashCheck(ONCE_CHECK);
        memoryWatch();
        checkExpireKey();
        checkObjectPool();
        calcEachSecondStatistic();
        checkAppendFileState();
        checkAppendFileFsync();
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

    private final Client cronFakeClient = new Client(); // 假客户端，用于append file 生成命令
    private final CommandPool commandPool = CommandPool.instance;
    private final AppendCommandPool appendCommandPool = AppendCommandPool.instance;

    // 主动检查过期键
    private void checkExpireKey() {
        Database[] dbs = server.dbs;
        int count = 0;
        int alreadyCheckDb = 0;
        int eachDb = ONCE_CHECK_EXPIRE / 5;
        while (count < ONCE_CHECK_EXPIRE && alreadyCheckDb < dbs.length) {
            lastCheckExpire %= dbs.length;
            cronFakeClient.setUsedDb((byte) lastCheckExpire); // 设置当前数据库
            count += dbs[lastCheckExpire].expire.checkExpire(eachDb, dbs[lastCheckExpire].keyspace,
                    dataObject -> { // 设置钩子函数
                        if (server.afp != null) {
                            Command command = commandPool.getObject();
                            command.setCommandType(ServerCommandType.DEL);
                            command.setDataObjects(new DataObject[]{dataObject});
                            command.setClient(cronFakeClient);
                            AppendCommand ac = appendCommandPool.getObject();
                            ac.setCommand(command);
                            server.afp.deliver(ac);
                        }
                    });
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
        AppendCommandPool.instance.tryShrink();
    }

    private void calcEachSecondStatistic() {
        // 更新命令处理数量
        long totalCommandCount = server.totalCommandCount;
        int diff = (int) (totalCommandCount - lastTimeAlreadyProcess);
        lastTimeAlreadyProcess = totalCommandCount;
        // 更新时间
        long lastCronTime = this.lastCronTime;
        this.lastCronTime = currentTime;
        int forEachSecond = diff * (int) (1000 / (currentTime - lastCronTime));
        if (server.commandProcessedEachSecondPeakValue < forEachSecond)
            server.commandProcessedEachSecondPeakValue = forEachSecond;
    }

    private long lastTimeFsync;
    private static final long FSYNC_INTERVAL = 1000; // 一秒一次fsync

    private void checkAppendFileFsync() {
        if (server.afp == null || server.afp.failedForPanic()) // 失败中也不发送fsync以免过多堆积
            return;
        if (currentTime - lastTimeFsync >= FSYNC_INTERVAL) {
            server.afp.deliver(AppendCommand.FORCE_FLUSH);
            lastTimeFsync = currentTime; // 更新处理时间
        }
    }

    private boolean noLongerCheck = false; // 不再检查标记
    private int retryError = 0; // 重试中出错
    private final static int RETRY_IN_RETRY_ERROR = 3; // 重试中触发异常的上限

    private void checkAppendFileState() {
        if (!noLongerCheck && server.afp != null && server.afp.failedForPanic()) { // 开启append file 且失败了
            try {
                server.afp.retryBackGroundTask();
                retryError = 0; // 清零重试异常次数
            } catch (IOException e) {
                logger.warn("background append file task retry failed !");
                logger.warn(e.toString());
                retryError++;
                if (retryError > RETRY_IN_RETRY_ERROR) { // 重试一直失败，无法继续
                    logger.panic("restart background append file task error");
                    if (configuration.strongPersistenceSecurityRequired) {
                        logger.panic("strong persistence security is required, server exit");
                        server.shutdown = true; // 无法恢复，停止服务
                    } else {
                        noLongerCheck = true; // 不再检查，停止持久化
                        logger.warn("continue run server without append file support");
                    }
                }
            }
        }
    }
}
