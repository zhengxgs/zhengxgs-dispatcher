package core.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import core.DistributionSupport;
import core.task.FixmeWork;

/**
 * Created by zhengxgs on 2016/4/29.
 */
public class FragmentUtil {

	public static final boolean isSupportFragment(DistributionSupport work) {
		boolean support = false;
		if (work instanceof FixmeWork) {
			support = true;
		}
		return support;
	}

	public static final List<DistributionSupport> getFragment(DistributionSupport work, int nodeSize) throws Exception {
		if (work instanceof FixmeWork) {
			return getFragmentByFixmeWork((FixmeWork) work, nodeSize);
		} else {
			throw new Exception("暂不支持的分片任务");
		}
	}

	private static final List<DistributionSupport> getFragmentByFixmeWork(FixmeWork work, int nodeSize) {
		List<DistributionSupport> works = new ArrayList<>(nodeSize);
		List<List<Integer>> idsSegment = fragment(work.getIds(), nodeSize);
		for (int i = 0; i < nodeSize; i++) {
			List<Integer> seg = idsSegment.get(i);
			FixmeWork fixmeWork = new FixmeWork(seg);
			fixmeWork.setIsSegment(false);
			fixmeWork.setNodeName(work.getNodeName());
			works.add(fixmeWork);
		}
		return works;
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
