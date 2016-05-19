package core.utils;

import org.slf4j.Logger;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/**
 * 为了方便本地调试...
 * Created by zhengxgs on 2016/5/4.
 */
public class LoggerUtil {

	public static final boolean isDebug = true;

	public static void info(Logger logger, String msg, Object... objects) {
		if (isDebug) {
			FormattingTuple ft = MessageFormatter.arrayFormat(msg, objects);
			System.out.println(ft.getMessage());
			if (ft.getThrowable() != null) {
				ft.getThrowable().printStackTrace();
			}
		} else {
			logger.info(msg, objects);
		}
	}

	public static void error(Logger logger, String msg, Object... objects) {
		if (isDebug) {
			FormattingTuple ft = MessageFormatter.arrayFormat(msg, objects);
			System.out.println(ft.getMessage());
			if (ft.getThrowable() != null) {
				ft.getThrowable().printStackTrace();
			}
		} else {
			FormattingTuple ft = MessageFormatter.arrayFormat(msg, objects);
			logger.error(msg, objects);
		}
	}

	public static void warn(Logger logger, String msg, Object... objects) {
		if (isDebug) {
			FormattingTuple ft = MessageFormatter.arrayFormat(msg, objects);
			System.out.println(ft.getMessage());
			if (ft.getThrowable() != null) {
				ft.getThrowable().printStackTrace();
			}
		} else {
			logger.warn(msg, objects);
		}
	}
}
