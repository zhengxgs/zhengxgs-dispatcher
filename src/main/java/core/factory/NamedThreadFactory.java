package core.factory;

import java.util.concurrent.ThreadFactory;

/**
 * Created by zhengxgs on 2016/5/5.
 */
public class NamedThreadFactory implements ThreadFactory {
	private String name;

	public NamedThreadFactory(String name) {
		this.name = name;
	}

	@Override
	public Thread newThread(Runnable runnable) {
		return new Thread(runnable, name);
	}
}
