package core.utils;

import core.DistributionSupport;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by zhengxgs on 2016/4/29.
 */
public class FragmentUtil {


	public static final List<DistributionSupport> getFragment(DistributionSupport work, int nodeSize) {
		System.out.println("暂不支持的分片任务");
		return null;
	}

	/**
	 * 分片
	 * @param ids
	 * @param size
	 * @return
	 */
	private static List<List<Integer>> fragment(List<Integer> ids, Integer size) {
		int serviceNodeSize = size;
		List<List<Integer>> lists = new ArrayList<>(serviceNodeSize);

		List<Integer> nIds = new ArrayList<>(new HashSet<>(ids));
		int nodeSize = nIds.size() / serviceNodeSize;
		int index = 0;
		List<Integer> sub = null;
		for (int i = 1; i <= serviceNodeSize; i++) {
			sub = nIds.subList(index, (i == serviceNodeSize ? nIds.size() : nodeSize * i));
			index += nodeSize;
			// ArrayList$subList no serializable
			lists.add(new ArrayList<>(sub));
		}
		return lists;
	}
}
