package core;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;

import core.factory.NamedThreadFactory;
import core.handler.Event;
import core.handler.EventType;
import core.utils.LoggerUtil;
import core.utils.RemotingHelper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
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
public class NettyRemotingServer extends AbstractRemotingServer {

	private org.slf4j.Logger logger = LoggerFactory.getLogger("es.log");

	private static final int PORT = 9999;
	/**用于分配处理业务线程的线程组个数 */
	protected static final int BIZGROUPSIZE = 1;
	/** 业务处理线程大小*/
	// protected static final int BIZTHREADSIZE = 4;

	private static final EventLoopGroup bossGroup = new NioEventLoopGroup(BIZGROUPSIZE);
	private static final EventLoopGroup workerGroup = new NioEventLoopGroup();

	public NettyRemotingServer() {
		executorService = new ThreadPoolExecutor(5, 10, 10 * 60 * 60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(1024),
				new NamedThreadFactory("processRequestCommandThread"), new ThreadPoolExecutor.CallerRunsPolicy());
	}

	@Override
	public void run() {
		if (NettyServer.getInstance().running) {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup);
			b.channel(NioServerSocketChannel.class);
			b.childOption(ChannelOption.SO_KEEPALIVE, true);
			b.childOption(ChannelOption.TCP_NODELAY, true);
			b.childHandler(new ChannelInitializer<SocketChannel>() {
				@Override
				public void initChannel(SocketChannel ch) throws Exception {
					ChannelPipeline pipeline = ch.pipeline();
					pipeline.addLast("timeout",
							new IdleStateHandler(DEFAULT_READER_IDLETIME, DEFAULT_WRITER_IDLETIME, DEFAULT_ALL_IDLETIME, DEFAULT_UNIT));
					pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
					pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
					pipeline.addLast(new ObjectEncoder());
					pipeline.addLast(new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.cacheDisabled(null)));
					pipeline.addLast(new ServerHandler());
				}
			});
			try {
				b.bind(PORT).sync();
			} catch (InterruptedException e) {
				shutdown();
				LoggerUtil.error(logger, "netty服务启动异常", e);
			}
		}
	}

	class ServerHandler extends SimpleChannelInboundHandler<Event> {

		@Override
		public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
			super.channelRegistered(ctx);
			addRequestState(ctx.channel());
		}

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, Event msg) throws Exception {
			// System.out.println("服务端处理事件:" + EventUtil.eventTypeToString(msg.getType()));
			processRequestCommand(ctx.channel(), msg);
		}

		@Override
		public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
			super.channelUnregistered(ctx);
			String remoteAddr = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
			NettyServer.getInstance().removeClientState(remoteAddr);
			RemotingHelper.closeChannel(ctx.channel());
			LoggerUtil.warn(logger, "客户端:{} 断开连接,现有客户端数量:{}", remoteAddr, NettyServer.getInstance().getClientCount());
		}

		@Override
		public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
			if (evt instanceof IdleStateEvent) {
				IdleStateEvent event = (IdleStateEvent) evt;
				if (event.state().equals(IdleState.READER_IDLE)) {
					// close channel
					String remoteAddr = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
					NettyServer.getInstance().removeClientState(remoteAddr);
					RemotingHelper.closeChannel(ctx.channel());
				} else if (event.state().equals(IdleState.WRITER_IDLE)) {
					Event ping = new Event(EventType.AB_PING, null, null);
					ctx.channel().writeAndFlush(ping);
				}
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			RemotingHelper.closeChannel(ctx.channel());
		}

	}

	protected static void shutdown() {
		workerGroup.shutdownGracefully();
		bossGroup.shutdownGracefully();
	}

	/**
	 * add Client State
	 * @param channel
	 */
	@Override
	public void addRequestState(Channel channel) {
		ClientState clientState = new ClientState(channel);
		String clientAddress = clientState.getRemoteAddress();
		NettyServer.getInstance().putClientState(clientAddress, clientState);
	}

}