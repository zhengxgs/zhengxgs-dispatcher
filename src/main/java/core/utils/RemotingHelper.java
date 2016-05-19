package core.utils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

/**
 * copy lts 工具，使用时不要混用了。lts使用的是自己封装的Channel对象，这里使用netty的Channel
 * Created by zhengxgs on 2016/4/29.
 */
public class RemotingHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemotingHelper.class);
	public static final String RemotingLogName = "LtsRemoting";

	public RemotingHelper() {
	}

	public static SocketAddress string2SocketAddress(String addr) {
		String[] s = addr.split(":");
		return new InetSocketAddress(s[0], Integer.valueOf(s[1]).intValue());
	}

	public static String parseChannelRemoteAddr(Channel channel) {
		if (null == channel) {
			return "";
		} else {
			SocketAddress remote = channel.remoteAddress();
			String addr = remote != null ? remote.toString() : "";
			if (addr.length() > 0) {
				int index = addr.lastIndexOf("/");
				return index >= 0 ? addr.substring(index + 1) : addr;
			} else {
				return "";
			}
		}
	}

	public static void closeChannel(Channel channel) {
		final String addrRemote = parseChannelRemoteAddr(channel);
		channel.close().addListener(new ChannelFutureListener() {
			public void operationComplete(ChannelFuture future) throws Exception {
				RemotingHelper.LOGGER.info("closeChannel: close the connection to remote address[{}] result: {}",
						new Object[] { addrRemote, Boolean.valueOf(future.isSuccess()) });
			}
		});
	}
}