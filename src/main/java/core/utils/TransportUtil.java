package core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import core.*;
import core.handler.Event;
import io.netty.channel.Channel;

public class TransportUtil {

	private static Logger logger = LoggerFactory.getLogger("es.log");

	public static boolean toClient(String remotingAddress, Event event) {
		boolean result = false;
		try {
			ClientState clientState = NettyServer.getInstance().getClientState(remotingAddress);
			if (clientState == null) {
				LoggerUtil.info(logger, "ip:{} 客户端为空", remotingAddress);
			} else {
				clientState.getChannel().writeAndFlush(event).sync();
				LoggerUtil.info(logger, "服务器发向{} 发送事件:{}", remotingAddress, event);
				result = true;
			}
		} catch (Exception e) {
			LoggerUtil.error(logger, "数据发送异常", e);
		}
		return result;
	}

	public static boolean toServer(Event event) {
		boolean result = false;
		try {
			DistributionSupport source = (DistributionSupport) event.getSource();
			NettyRemotingClient remotingClient = NettyClient.getInstance().getRemotingClient(source.getNodeName());
			if (remotingClient != null && remotingClient.getChannel() != null) {
				Channel channel = remotingClient.getChannel();
				synchronized (channel) {
					channel.writeAndFlush(event).sync();
				}
				result = true;
			} else {
				LoggerUtil.error(logger, "uuid:{} server:{} 没有找到对应通信通道,暂作丢弃处理", source.getUuid(), source.getNodeName());
				/**
				 * 后续可判断返回类型是成功还是失败，失败时随机找一个节点继续分发任务执行
				 * 以及清空NettyClient维护的remotingClients无效对象(source.getNodeName())，
				 *
				 * 目前在服务端会对超时(1小时)未返回的任务进行重执行处理.
				 */
			}
		} catch (Exception e) {
			// java.nio.channels.ClosedChannelException
			LoggerUtil.error(logger, "数据发送异常", e);
		}
		return result;
	}
}
