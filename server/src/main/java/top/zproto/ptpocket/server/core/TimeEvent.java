package top.zproto.ptpocket.server.core;

public interface TimeEvent {
    void processTimeEvent();

    public enum TimeEventType {
        ONCE, INTERVAL;
    }
}
