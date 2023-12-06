package top.zproto.ptpocket.server.core;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import top.zproto.ptpocket.server.entity.Command;
import top.zproto.ptpocket.server.log.Logger;
import top.zproto.ptpocket.server.persistence.appendfile.AppendFilePersistence;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * 服务器主类入口
 * 整个服务器会一直在mainLoop主循环中执行，直到停止
 */
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
        if (!mainInstance.checkReload(config)) {
            return;
        }
        mainInstance.commandBatch = 1000 / config.frequencyOfServerCron; // 按照一毫秒处理一个命令来算
        if (mainInstance.commandBatch < 10) // 最低是10
            mainInstance.commandBatch = 10;
        mainInstance.commands = new Command[mainInstance.commandBatch];
        mainInstance.serverReady(config);
        mainInstance.beforeMainLoop();
        mainInstance.mainLoop();
    }

    private int commandBatch; // 一次性最多处理多少个请求
    private Command[] commands;

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
                    while (count != commandBatch && (c = requests.poll()) != null)
                        commands[count++] = c;
                    processCommand(count);
                }
                processTimeEvent();
            }
        } catch (Throwable ex) {
            logger.panic(ex.toString());
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
                        ch.pipeline().addLast(new RequestHandler()); // 请求处理器
                        ch.pipeline().addLast(new ResponseHandler()); // 响应处理器
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
    private void processTimeEvent() { // 必须一对一地执行此命令之前执行一次timeCanWait
        server.processTimeEvent();
    }

    /**
     * 初始化DB
     */
    private void initDb(ServerConfiguration config) {
        server = ServerHolder.INSTANCE;
        server.init(config);
    }

    private void beforeMainLoop() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (server.afp != null)
                    server.afp.close(); // try to make sure the append file safe
            } catch (IOException ignored) {
            }
        }));
    }

    /**
     * 打印启动提示
     */
    private void serverReady(ServerConfiguration config) {
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

    /**
     * 服务器退出前的收尾工作
     */
    private void beforeExit() {
        bootstrap.config().group().shutdownGracefully();
        bootstrap.config().childGroup().shutdownGracefully();
        if (server.afp != null) {
            try {
                server.afp.close(); // 触发后台Append File线程退出
            } catch (IOException ignored) {
            }
        }
        logger.info("server going to exit!");
    }

    /**
     * 向主线程提交Command
     */
    public static void submitCommand(Command c) {
        mainInstance.requests.add(c);
    }

    /**
     * 持久化文件载入
     */
    private boolean checkReload(ServerConfiguration configuration) {
        if (configuration.strongPersistenceSecurityRequired && !configuration.useAppendFile) {
            logger.panic("strong persistence security is required but persistence is close");
            return false;
        }
        if (configuration.useAppendFile) {
            server.afp = new AppendFilePersistence(configuration);
            try {
                server.afp.startReload(configuration.useAppendFile);
            } catch (IOException e) {
                logger.warn("some error occurred in reload data from disk");
                logger.warn(e.getMessage());
                beforeExit();
                return false; // 停止
            }
        }
        return true; // 继续
    }
}
