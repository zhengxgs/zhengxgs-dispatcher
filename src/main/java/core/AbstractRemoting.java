package core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import core.handler.Event;
import core.handler.EventHandler;
import core.handler.EventType;
import core.utils.EventUtil;
import core.utils.LoggerUtil;
import core.utils.RemotingHelper;
import io.netty.channel.Channel;

/**
 * Created by zhengxgs on 2016/4/29.
 */
public abstract class AbstractRemoting implements Runnable {

	private Logger logger = LoggerFactory.getLogger("es.log");

	public long DEFAULT_READER_IDLETIME = 60;
	public long DEFAULT_WRITER_IDLETIME = 45;
	public long DEFAULT_ALL_IDLETIME = 120;
	public TimeUnit DEFAULT_UNIT = TimeUnit.SECONDS;
	public ExecutorService executorService;

	/**
	 * request process
	 * @param msg
	 */
	public void processRequestCommand(final Channel channel, final Event msg) {
		if (EventType.AB_PING == msg.getType()) {
			if (LoggerUtil.isDebug) {
				LoggerUtil.info(logger, "{} bi bi bi...", RemotingHelper.parseChannelRemoteAddr(channel));
			}
			return;
		}
		// 不阻塞IO线程，将下面的操作放到本地线程里运行
		Runnable run = new Runnable() {
			@Override
			public void run() {
				try {
					EventHandler handler = EventUtil.getEventHandler(msg.getType());
					handler.handleEvent(msg);
				} catch (Exception e) {
					LoggerUtil.error(logger, "处理({})请求异常, msg:{}", RemotingHelper.parseChannelRemoteAddr(channel), msg, e);
				}
			}
		};
		try {
			executorService.submit(run);
		} catch (RejectedExecutionException e) {
			LoggerUtil.error(logger, "too many requests and system thread pool busy", e);
		}
	}
}
