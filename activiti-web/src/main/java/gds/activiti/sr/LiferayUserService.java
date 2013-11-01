package gds.activiti.sr;

import java.util.Collection;

import net.emforge.activiti.identity.LiferayGroupsUtil;

import org.activiti.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

@Service
public class LiferayUserService {
	 private static Log _log = LogFactoryUtil.getLog(LiferayUserService.class);
	@Autowired
	protected LiferayGroupsUtil liferayGroups;
	public Collection<String> getGroups(DelegateExecution execution, Collection<String> groups) {
		_log.info(execution.getCurrentActivityName()+ "活动流程指定的后选组是："+groups);
		Collection<String> returnValue= liferayGroups.getGroupsFromList(execution, groups);
		_log.info(execution.getCurrentActivityName()+"活动实际指定的候选组是:"+returnValue);
		return returnValue;
	}	
}
