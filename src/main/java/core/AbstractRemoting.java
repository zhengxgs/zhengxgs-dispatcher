package core;


import core.handler.Event;
import core.handler.EventHandler;
import core.utils.EventUtil;
import io.netty.channel.Channel;

/**
 * Created by zhengxgs on 2016/4/29.
 */
public abstract class AbstractRemoting implements Runnable {

    /**
     * request process
     * @param msg
     */
    public void processRequestCommand(Event msg) {
        EventHandler handler = EventUtil.getEventHandler(msg.getType());
        try {
            handler.handleEvent(msg);
        } catch (Exception e) {
            System.out.println(msg);
            e.printStackTrace();
        }
    }

    /**
     * 通道处理
     * @param channel
     */
    public abstract void addRequestState(Channel channel);
}
