package top.zproto.ptpocket.server.core;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import top.zproto.ptpocket.server.entity.Command;
import top.zproto.ptpocket.server.log.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class Main {
    public static final Main mainInstance = new Main();

    // 串行化命令执行
    // 任务队列
    private final BlockingQueue<Command> requests = new LinkedBlockingDeque<>();
    // 日志logger
    private final Logger logger = Logger.DEFAULT;

    // 整个服务器的信息数据结构
    ServerHolder server = null;

    // 入口
    public static void main(String[] args) {
        ServerConfiguration config = ServerConfiguration.getConfig(args);
        mainInstance.initNet(config);
        mainInstance.initDb(config);
        mainInstance.serverReady(config);
        mainInstance.mainLoop();
    }

    private final int BATCH = 10; // 一次性最多处理多少个请求
    private final Command[] commands = new Command[BATCH];

    /**
     * 主循环
     */
    private void mainLoop() {
        try {
            while (!server.shutdown) {
                long time = timeCanWait();
                int count = 0;
                Command c = requests.poll(time, TimeUnit.MILLISECONDS); // 允许阻塞一定时间
                if (c != null) {
                    commands[count++] = c;
                    while (count != BATCH && (c = requests.poll()) != null)
                        commands[count++] = c;
                    processCommand(count);
                }
                processTimeEvent();
            }
        } catch (Throwable ignore) {
            logger.panic("server exception occurred");
        } finally {
            beforeExit();
        }
    }

    private ServerBootstrap bootstrap;

    /**
     * 初始化网络监听
     */
    private void initNet(ServerConfiguration config) {
        NioEventLoopGroup boosGroup = new NioEventLoopGroup(config.bossGroupThread);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(config.IOThreads);
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(boosGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.TCP_NODELAY, true) // 关闭Nagle算法
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new TimeOutHandler(config)); // 超时处理
                        ch.pipeline().addLast(new PacketSplitHandler()); // 粘包处理
                        ch.pipeline().addLast(RequestHandler.instance); // 请求处理器
                        ch.pipeline().addLast(ResponseHandler.instance); // 响应处理器
                    }
                });
        bootstrap.bind(config.addr, config.port).addListener(f -> {
            if (!f.isSuccess()) {
                logger.panic("server net start failed!");
                logger.panic(f.cause().toString());
                System.exit(1);
            }
        });
        this.bootstrap = bootstrap;
    }

    /**
     * 计算可以阻塞的时间
     */
    private long timeCanWait() {
        long canWait = server.getTriggerTimeOfClosestTimeEvent() - System.currentTimeMillis();
        return canWait > 0 ? canWait : 0;
    }

    /**
     * 执行命令
     */
    private void processCommand(int count) {
        for (int i = 0; i < count; i++) {
            Command command = commands[i];
            command.process();
            command.returnObject();
            server.totalCommandCount++;
        }
    }

    /**
     * 处理时间事件
     */
    private void processTimeEvent() {
        server.processTimeEvent();
    }

    private void initDb(ServerConfiguration config) { // 初始化db
        server = ServerHolder.INSTANCE;
        server.init(config);
    }

    private void serverReady(ServerConfiguration config) { // 打印提示
        String logo = "       _                    _        _   \n" +
                " _ __ | |_ _ __   ___   ___| | _____| |_ \n" +
                "| '_ \\| __| '_ \\ / _ \\ / __| |/ / _ \\ __|\n" +
                "| |_) | |_| |_) | (_) | (__|   <  __/ |_ \n" +
                "| .__/ \\__| .__/ \\___/ \\___|_|\\_\\___|\\__|\n" +
                "|_|       |_|                            \n";
        logger.print(logo);
        String ip = config.addr.equals(ServerConfiguration.LOCAL_HOST) ? "127.0.0.1" : config.addr;
        logger.info(String.format("server running in ip : %s, port : %d", ip, config.port));
        logger.info("server is ready to process command");
    }

    private void beforeExit() { // 退出前处理
        bootstrap.config().group().shutdownGracefully();
        bootstrap.config().childGroup().shutdownGracefully();
        logger.info("server going to exit!");
    }

    public static void submitCommand(Command c) { // 提交command
        mainInstance.requests.add(c);
    }
}
