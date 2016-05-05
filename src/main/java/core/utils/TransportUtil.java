package core.utils;

import core.*;
import core.handler.Event;
import io.netty.channel.Channel;

public class TransportUtil {

	public static boolean toClient(String remotingAddress, Event event) {
		boolean result = false;
		try {
			ClientState clientState = NettyServer.getClientState(remotingAddress);
			if (clientState == null) {
				System.out.println(remotingAddress + " 客户端为空");
			} else {
				clientState.getChannel().writeAndFlush(event).sync();
				System.out.println("服务器发向" + remotingAddress + "发送事件：" + event);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public static boolean toServer(Event event) {
		boolean result = false;
		try {
			DistributionSupport source = (DistributionSupport) event.getSource();
			NettyRemotingClient remotingClient = NettyClient.getInstance().getRemotingClient(source.getNodeName());
			if (remotingClient != null) {
				Channel channel = remotingClient.getChannel();
				synchronized (channel) {
					channel.writeAndFlush(event).sync();
				}
				result = true;
			} else {
				System.out.println("没有找到对应通信通道,暂作丢弃处理");
				// TODO 丢弃...后续可判断返回类型是成功还是失败，失败随机找一个节点继续分发任务执行
				// int workStateType = Integer.parseInt(event.getParas().toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
}
