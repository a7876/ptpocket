package top.zproto.ptpocket.server.core;

/**
 * 时间事件接口
 */
public interface TimeEvent {
    void processTimeEvent();

    enum TimeEventType {
        ONCE, INTERVAL
    }
}
