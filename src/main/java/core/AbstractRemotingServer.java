package core;

import io.netty.channel.Channel;

/**
 * Created by zhengxgs on 2016/5/5.
 */
public abstract class AbstractRemotingServer extends AbstractRemoting {

	/**
	 * 通道处理
	 * @param channel
	 */
	public abstract void addRequestState(Channel channel);
}
