package core;

import java.io.Serializable;

import core.utils.RemotingHelper;
import io.netty.channel.Channel;

/**
 * 客户端状态
 * Created by zhengxgs on 2016/4/28.
 */
public class ClientState implements Serializable {

	private static final long serialVersionUID = 1L;

	String remoteAddress; //远程ip和端口号等
	Channel channel;

	public ClientState(Channel channel) {
		this.channel = channel;
		remoteAddress = RemotingHelper.parseChannelRemoteAddr(channel);
	}

	public String getRemoteAddress() {
		return remoteAddress;
	}

	public void setRemoteAddress(String remoteAddress) {
		this.remoteAddress = remoteAddress;
	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}
}