package core;

import java.util.concurrent.TimeUnit;

import core.handler.Event;
import core.handler.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
public class NettyRemotingClient extends AbstractRemoting {

	private static NettyRemotingClient nettyClient = null;

	private String host;
	private int port;

	public NettyRemotingClient(String host, int port) {
		this.host = host;
		this.port = port;
	}

	/*public static NettyRemotingClient getInstance(String... address) throws Exception {
		if (nettyClient == null) {
			if (StringUtils.isEmpty(address[0])) {
				throw new Exception("请添加服务地址....");
			}
			String port = "9999";
			if (address.length >= 2) {
				port = address[1];
			}
			synchronized (NettyRemotingClient.class) {
				nettyClient = new NettyRemotingClient(address[0], Integer.parseInt(port));
			}
		}
		return nettyClient;
	}*/

	public Channel channel;
	public EventLoopGroup group = new NioEventLoopGroup();
	public Bootstrap bootstrap = getBootstrap();

	public Bootstrap getBootstrap() {
		Bootstrap b = new Bootstrap();
		b.group(group).channel(NioSocketChannel.class);
		b.handler(new ChannelInitializer<Channel>() {
			@Override
			protected void initChannel(Channel ch) throws Exception {
				ChannelPipeline pipeline = ch.pipeline();
				pipeline.addLast("timeout", new IdleStateHandler(60, 45, 120, TimeUnit.SECONDS));
				pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
				pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
				/*
				pipeline.addLast("decoder", new StringDecoder(CharsetUtil.UTF_8));
				pipeline.addLast("encoder", new StringEncoder(CharsetUtil.UTF_8));
				*/
				pipeline.addLast(new ObjectEncoder());
				pipeline.addLast(new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.cacheDisabled(null)));
				pipeline.addLast("handler", new ClientHandler());
			}
		});
		b.option(ChannelOption.SO_KEEPALIVE, true);
		return b;
	}

	class ClientHandler extends SimpleChannelInboundHandler<Event> {

		private Logger logger = LoggerFactory.getLogger(ClientHandler.class);

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, Event msg) throws Exception {
			// System.out.println("客户端处理事件:" + EventUtil.eventTypeToString(msg.getType()));
			processRequestCommand(msg);
		}

		@Override
		public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
			if (evt instanceof IdleStateEvent) {
				IdleStateEvent event = (IdleStateEvent) evt;
				if (event.state().equals(IdleState.READER_IDLE)) {
					System.out.println("服务端无响应，关闭连接...");
					ctx.close();
				} else if (event.state().equals(IdleState.WRITER_IDLE)) {
					Event ping = new Event(EventType.W_PING, null, null);
					ctx.channel().writeAndFlush(ping);
				}
			}
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			super.channelInactive(ctx);
			System.out.println("链接断开重连...");
			run();
		}
	}

	public Channel doConnect() throws Exception {
		channel = bootstrap.connect(host, port).sync().channel();
		return channel;
	}

	@Override
	public void run() {
		boolean connectSuccess = false;
		while (!connectSuccess) {
			try {
				doConnect();
				connectSuccess = true;
			} catch (Exception e) {
				System.out.println("链接服务器失败，等待3秒后重试...");
				try {
					Thread.sleep(3 * 1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
		System.out.println("客户端启动完成...");
	}

	@Override
	public void addRequestState(Channel channel) {
		// none
	}

	public Channel getChannel() {
		return channel;
	}
}
