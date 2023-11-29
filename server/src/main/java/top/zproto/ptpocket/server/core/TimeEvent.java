package top.zproto.ptpocket.server.core;

public interface TimeEvent {
    void processTimeEvent();

    enum TimeEventType {
        ONCE, INTERVAL
    }
}
