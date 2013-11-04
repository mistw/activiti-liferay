package gds.activiti.sr;

import java.util.Set;

import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.activiti.engine.task.IdentityLink;
import org.springframework.stereotype.Service;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
@Service
public class UserTaskListener implements TaskListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static Log _log = LogFactoryUtil.getLog(UserTaskListener.class);

	@Override
	public void notify(DelegateTask delegateTask) {
		_log.info("进入任务创建侦听器");
		Set<IdentityLink> identityLinks= delegateTask.getCandidates();
		if(identityLinks.size()==1){
			_log.info("只有一个候选用户，直接分配任务给他。");
			delegateTask.setAssignee(((IdentityLink)identityLinks.toArray()[0]).getUserId());
		}
	}

}
