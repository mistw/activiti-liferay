package net.emforge.activiti;

import java.util.Map;

import net.emforge.activiti.identity.LiferayGroupManagerSessionFactory;
import net.emforge.activiti.identity.LiferayUserManagerSessionFactory;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.impl.history.HistoryLevel;
import org.activiti.engine.impl.interceptor.SessionFactory;
import org.activiti.engine.impl.persistence.entity.GroupManager;
import org.activiti.engine.impl.persistence.entity.UserManager;
import org.activiti.spring.ProcessEngineFactoryBean;
import org.codehaus.groovy.jsr223.GroovyScriptEngineFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

/** Custom implementation of ProcessEngineFactoryBean to set own IdentitySession
 * and customize other things
 * 
 * @author akakunin
 *
 */
public class LiferayProcessEngineFactoryBean extends ProcessEngineFactoryBean {
	private static Log _log = LogFactoryUtil.getLog(LiferayProcessEngineFactoryBean.class);
	
	@Autowired
	LiferayGroupManagerSessionFactory liferayGroupManagerSessionFactory;
	
	@Override
	public ProcessEngine getObject() throws Exception {
		// set history level
		processEngineConfiguration.setHistoryLevel(HistoryLevel.FULL);
		
		ProcessEngine processEngine = super.getObject();
		
		// preconfigure process engine to use our identity session
		Map<Class<?>, SessionFactory> sessionFactories = processEngineConfiguration.getSessionFactories();
		sessionFactories.put(GroupManager.class, liferayGroupManagerSessionFactory);
		sessionFactories.put(UserManager.class, new LiferayUserManagerSessionFactory());
		
		// Add Liferay Script Engine Factory
		processEngineConfiguration.getScriptingEngines().addScriptEngineFactory(new LiferayScriptEngineFactory("LiferayJavaScript", "javascript"));
		processEngineConfiguration.getScriptingEngines().addScriptEngineFactory(new LiferayScriptEngineFactory("LiferayGroovy", "groovy"));
		
		// Add Groovy Script Engine Factory
		processEngineConfiguration.getScriptingEngines().addScriptEngineFactory(new GroovyScriptEngineFactory());
		
		return processEngine;
	}
}