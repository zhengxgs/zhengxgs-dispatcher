package core;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import core.factory.NamedThreadFactory;
import core.handler.Event;
import core.handler.EventType;
import core.utils.LoggerUtil;
import core.utils.RemotingHelper;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * Created by zhengxgs on 2016/4/28.
 */
public class NettyRemotingClient extends AbstractRemotingClient {

	private Logger logger = LoggerFactory.getLogger("es.log");

	private String host;
	private int port;

	public NettyRemotingClient(String host, int port) {
		this.host = host;
		this.port = port;
		executorService = new ThreadPoolExecutor(10, 10, 10 * 60 * 60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(128),
				new NamedThreadFactory("processRequestCommandThread"), new ThreadPoolExecutor.CallerRunsPolicy());
	}

	public Channel channel;
	public EventLoopGroup group = new NioEventLoopGroup();
	public Bootstrap bootstrap = getBootstrap();
	public AtomicBoolean isReConnectRunning = new AtomicBoolean(false);

	public Bootstrap getBootstrap() {
		Bootstrap b = new Bootstrap();
		b.group(group).channel(NioSocketChannel.class);
		b.option(ChannelOption.SO_KEEPALIVE, true);
		b.option(ChannelOption.TCP_NODELAY, true);
		b.handler(new ChannelInitializer<Channel>() {
			@Override
			protected void initChannel(Channel ch) throws Exception {
				ChannelPipeline pipeline = ch.pipeline();
				pipeline.addLast("timeout",
						new IdleStateHandler(DEFAULT_READER_IDLETIME, DEFAULT_WRITER_IDLETIME, DEFAULT_ALL_IDLETIME, DEFAULT_UNIT));
				pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
				pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
				pipeline.addLast(new ObjectEncoder());
				pipeline.addLast(new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.cacheDisabled(null)));
				pipeline.addLast("handler", new ClientHandler());
			}
		});
		return b;
	}

	@Override
	public void clientShutdown() {
		group.shutdownGracefully();
	}

	class ClientHandler extends SimpleChannelInboundHandler<Event> {

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, Event msg) throws Exception {
			// System.out.println("客户端处理事件:" + EventUtil.eventTypeToString(msg.getType()));
			processRequestCommand(ctx.channel(), msg);
		}

		@Override
		public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
			if (evt instanceof IdleStateEvent) {
				IdleStateEvent event = (IdleStateEvent) evt;
				if (event.state().equals(IdleState.READER_IDLE)) {
					LoggerUtil.warn(logger, "服务端无响应，关闭连接...");
					RemotingHelper.closeChannel(ctx.channel());
				} else if (event.state().equals(IdleState.WRITER_IDLE)) {
					Event ping = new Event(EventType.AB_PING, null, null);
					ctx.channel().writeAndFlush(ping);
				}
			}
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			super.channelInactive(ctx);
			LoggerUtil.warn(logger, "链接断开重连...");
			isReConnectRunning.compareAndSet(false, true);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			RemotingHelper.closeChannel(ctx.channel());
		}
	}

	public void retryConnect() {
		if (isReConnectRunning.compareAndSet(true, false)) {
			connect();
			isReConnectRunning.compareAndSet(false, true);
		}
	}

	public void connect() {
		boolean connectSuccess = false;
		while (!connectSuccess) {
			try {
				channel = bootstrap.connect(host, port).sync().channel();
				connectSuccess = true;
			} catch (Exception e) {
				LoggerUtil.warn(logger, "IP:{} 链接服务器失败，等待5秒后重试...", host);
				try {
					Thread.sleep(5 * 1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
		LoggerUtil.info(logger, "客户端链接IP:{} 完成...", host);
	}

	@Override
	public void run() {
		connect();
	}

	public Channel getChannel() {
		return channel;
	}
}
