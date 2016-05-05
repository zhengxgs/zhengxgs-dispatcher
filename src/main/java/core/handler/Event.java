package core.handler;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Created by zhengxgs on 2016/4/26.
 */
public class Event implements Serializable {

	private static final long serialVersionUID = 1L;

	private int type;// 类型
	private Object paras;// 参数
	private Object source;// 人物类

	public Event(int eventType, Object paras, Object source) {
		super();
		this.type = eventType;
		this.paras = paras;
		this.source = source;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public Object getParas() {
		return paras;
	}

	public void setParas(Object paras) {
		this.paras = paras;
	}

	public Object getSource() {
		return source;
	}

	public void setSource(Object source) {
		this.source = source;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

}