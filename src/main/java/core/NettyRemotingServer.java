package core;

import java.util.concurrent.TimeUnit;

import core.handler.Event;
import core.handler.EventType;
import core.utils.RemotingHelper;
import org.apache.log4j.Logger;


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
public class NettyRemotingServer extends AbstractRemoting {

	public Logger log = Logger.getLogger(NettyRemotingServer.class);

	private static final int PORT = 9999;
	/**用于分配处理业务线程的线程组个数 */
	protected static final int BIZGROUPSIZE = 1;
	/** 业务处理线程大小*/
	protected static final int BIZTHREADSIZE = 4;

	private static final EventLoopGroup bossGroup = new NioEventLoopGroup(BIZGROUPSIZE);
	private static final EventLoopGroup workerGroup = new NioEventLoopGroup(BIZTHREADSIZE);

	@Override
	public void run() {
		if (NettyServer.getInstance().running) {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup);
			b.channel(NioServerSocketChannel.class);
			// b.childOption(ChannelOption.SO_RCVBUF, 1024);
			b.childHandler(new ChannelInitializer<SocketChannel>() {
				@Override
				public void initChannel(SocketChannel ch) throws Exception {
					ChannelPipeline pipeline = ch.pipeline();
					pipeline.addLast("timeout", new IdleStateHandler(60, 45, 120, TimeUnit.SECONDS));
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
				e.printStackTrace();
				shutdown();
			}
			System.out.println("netty服务启动...");
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
			processRequestCommand(msg);
		}

		@Override
		public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
			super.channelUnregistered(ctx);
			String remoteAddr = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
			NettyServer.removeClientState(remoteAddr);
			System.out.println("客户端(" + remoteAddr + ")断开连接." + ",现有客户端数量" + NettyServer.getClientCount());
		}

		@Override
		public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
			if (evt instanceof IdleStateEvent) {
				IdleStateEvent event = (IdleStateEvent) evt;
				if (event.state().equals(IdleState.READER_IDLE)) {
					// close channel
					String remoteAddr = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
					NettyServer.removeClientState(remoteAddr);
					// NbConext.removeChannel(nodeIp);
					RemotingHelper.closeChannel(ctx.channel());
				} else if (event.state().equals(IdleState.WRITER_IDLE)) {
					Event ping = new Event(EventType.S_PING, null, null);
					ctx.channel().writeAndFlush(ping);
				}
			}
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
		NettyServer.putClientState(clientAddress, clientState);
	}

}