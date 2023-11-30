package top.zproto.ptpocket.server.core;

import java.util.HashSet;
import java.util.Set;

public class ServerHolder {
    // 服务器信息对象

    Database[] dbs; // 数据库
    Set<Client> clients; // 连接的客户

    boolean shutdown = false;

    long startTime;
    long totalCommandCount = 0; // 运行到如今执行过的指令数

    // 统计运行至今一秒最多可以执行多少条命令
    int commandProcessedEachSecondHeapValue = 0;

    static class TimeEventHolder { // 时间事件类
        long triggerTime;
        TimeEvent.TimeEventType type;
        TimeEvent timeEvent;
        TimeEventHolder next;
        TimeEventHolder pre;
        long interval;
    }

    TimeEventHolder timeEvents;
    TimeEventHolder closestTimeEvent;

    public static final ServerHolder INSTANCE = new ServerHolder();

    private ServerHolder() {
    }

    public void init(ServerConfiguration config) {
        dbs = new Database[config.dbNums];
        for (int i = 0; i < config.dbNums; i++) {
            dbs[i] = new Database(i);
        }
        startTime = System.currentTimeMillis();
        clients = new HashSet<>();
        registerTimeEvent(new ServerCron(config), TimeEvent.TimeEventType.INTERVAL,
                1000 / config.frequencyOfServerCron);
    }

    public void registerTimeEvent(TimeEvent timeEvent, TimeEvent.TimeEventType type, long timeOrInterval) {
        TimeEventHolder timeEventHolder = new TimeEventHolder();
        timeEventHolder.timeEvent = timeEvent;
        timeEventHolder.type = type;
        if (type == TimeEvent.TimeEventType.ONCE) {
            timeEventHolder.triggerTime = timeOrInterval;
        } else {
            timeEventHolder.triggerTime = System.currentTimeMillis() + timeOrInterval;
            timeEventHolder.interval = timeOrInterval;
        }
        if (timeEvents != null) {
            timeEventHolder.next = timeEvents;
            timeEvents.pre = timeEventHolder;
        }
        timeEvents = timeEventHolder;
    }

    long getTriggerTimeOfClosestTimeEvent() {
        TimeEventHolder holder = timeEvents;
        if (holder == null)
            return -1;
        long closest = holder.triggerTime;
        TimeEventHolder closestEvent = holder;
        while (holder.next != null) {
            holder = holder.next;
            if (holder.triggerTime < closest) {
                closest = holder.triggerTime;
                closestEvent = holder;
            }
        }
        closestTimeEvent = closestEvent;
        return closest;
    }

    public void processTimeEvent() { // 必须先调用getTriggerTimeOfClosestTimeEvent再调用此
        if (!checkClosestTimeEventIfReach()) // 如果时间还未到没有必要执行
            return;
        closestTimeEvent.timeEvent.processTimeEvent();
        if (closestTimeEvent.type == TimeEvent.TimeEventType.ONCE) { // 摘去
            if (closestTimeEvent.pre != null) {
                closestTimeEvent.pre.next = closestTimeEvent.next;
            } else { // 是队头
                timeEvents = closestTimeEvent.next;
            }
            if (closestTimeEvent.next != null)
                closestTimeEvent.next.pre = closestTimeEvent.pre;
        } else { // 周期事件
            closestTimeEvent.triggerTime = System.currentTimeMillis() + closestTimeEvent.interval;
        }
        closestTimeEvent = null;
    }

    private boolean checkClosestTimeEventIfReach() {
        return System.currentTimeMillis() - closestTimeEvent.triggerTime >= 0;
    }

    public int getDbNumbs() {
        return dbs.length;
    }
}
