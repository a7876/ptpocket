package top.zproto.ptpocket.client.core;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import top.zproto.ptpocket.client.exception.ConnectFailException;

import java.net.InetSocketAddress;

/**
 * client 主体
 */
public class Client {
    private String ipAddr;
    private int port;
    private NioEventLoopGroup group;

    private static final int THREAD_LIMIT = 10;
    private ChannelFuture future;
    private Channel channel;

    private Client() {
    }

    public static Client getInstance(String ipAddr, int port) {
        return getInstance(ipAddr, port, 1);
    }

    public static Client getInstance(String ipAddr, int port, int thread) {
        if (thread < 1)
            throw new IllegalArgumentException("thread arg is illegal");
        Client client = new Client();
        client.ipAddr = ipAddr;
        client.port = port;
        client.group = new NioEventLoopGroup(thread);
        client.connect();
        return client;
    }


    /*
     * 共享底层netty线程池
     */
    public static Client getInstanceShared(String ipAddr, int port, Client client) {
        if (client == null)
            throw new NullPointerException("client can't be null");
        Client newClient = new Client();
        client.ipAddr = ipAddr;
        client.port = port;
        newClient.group = client.group;
        client.connect();
        return newClient;
    }

    private void connect() {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new ProtocolSplitHandler());
                        ch.pipeline().addLast(ResponseAcceptor.INSTANCE);
                        ch.pipeline().addLast(RequestSender.INSTANCE);
                    }
                }).option(ChannelOption.TCP_NODELAY, true); // 关闭Nagle
        future = bootstrap.connect(new InetSocketAddress(ipAddr, port));
        future.addListener(f -> {
            if (f.isSuccess()) {
                channel = ((ChannelFuture) f).channel();
            } else {
                throw new ConnectFailException("client connect failed", f.cause());
            }
        });
    }

    public Channel getChannel(){
        if (channel != null)
            return channel;
        try {
            future.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return future.channel();
    }
}
