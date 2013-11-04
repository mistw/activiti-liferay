package gds.activiti.sr;

import gds.activiti.constants.BusinessConstant;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.impl.el.Expression;
import org.apache.commons.lang.StringUtils;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.model.Organization;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.User;
import com.liferay.portal.model.UserGroupRole;
import com.liferay.portal.service.OrganizationLocalServiceUtil;
import com.liferay.portal.service.RoleLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.UserGroupRoleLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;

/**
 * 该类通常用在流程定义文件中，用来根据内容类型来选择候选用户组
 * 
 * @author wangf
 */
public class GetFirstApproveCandidateUsersDelegate implements JavaDelegate {

	private static Log _log = LogFactoryUtil
			.getLog(GetFirstApproveCandidateUsersDelegate.class);

	private String roleNameString;
	/**
	 * 流程注入，用来确定审核人的角色，缺省是科研助理
	 */
	private Expression roleName; 
	private Expression orgUnitFieldName;

	@Override
	public void execute(DelegateExecution execution) throws Exception {
		// 设置流程变量，流程中的UserTask中使用该流程变量来安排审核人
		// 获取与标签相对应的审核候选组
		roleNameString = roleName == null ? "科研助理" : (String) roleName
				.getValue(execution);
		Collection<String> cadidateUsers = getCadidateUsers(execution);
		_log.info("得到审核人为：" + StringUtils.join(cadidateUsers, ","));
		execution.setVariable("firstCandidateUserList",
				StringUtils.join(cadidateUsers, ","));
	}

	/**
	 * 该方法根据业务表单所属单位获取能够审核该单位业务（拥有roleName流程注入字段中指定的角色名字）的候选用户集合
	 * 
	 * @param execution
	 * @return
	 * @throws SystemException
	 * @throws PortalException
	 * @throws Exception
	 */
	private Collection<String> getCadidateUsers(DelegateExecution execution)
			throws PortalException, SystemException {

		List<String> candidateUserList = new ArrayList<String>();
		ServiceContext sc = (ServiceContext) execution
				.getVariable(WorkflowConstants.CONTEXT_SERVICE_CONTEXT);
		long companyId = GetterUtil.getLong((Serializable) execution
				.getVariable(WorkflowConstants.CONTEXT_COMPANY_ID));
		long groupId = GetterUtil.getLong((Serializable) execution
				.getVariable(WorkflowConstants.CONTEXT_GROUP_ID));
		long applyUserId = GetterUtil.getLong((Serializable) execution
				.getVariable(WorkflowConstants.CONTEXT_USER_ID));

		
		//1、获取角色
		_log.info("期望的流程组织角色为：" + roleNameString);
		Role role = RoleLocalServiceUtil.getRole(companyId, roleNameString);
		//2、获取业务单位
		long orgUnitId = 0;
		if(orgUnitFieldName == null){
			_log.warn("流程中未注入名字为orgUnitFieldName的业务单位字段名，系统默认为："+BusinessConstant.CONTEXT_ORGUNIT_ID);
			orgUnitId = GetterUtil.getLong((Serializable) execution.getVariable(BusinessConstant.CONTEXT_ORGUNIT_ID));
		}else{
			String _orgUnitFieldName=(String) orgUnitFieldName.getValue(execution);
			_log.warn("流程中注入名字为orgUnitFieldName的业务单位字段名为："+_orgUnitFieldName);
			orgUnitId = GetterUtil.getLong((Serializable) execution.getVariable(_orgUnitFieldName));
		}

		List<Organization> orgs = null;
		_log.info("当前用户:" + applyUserId);
		if (orgUnitId == 0) {
			_log.warn("该业务未指定业务单位，系统自动根据申请用户的所在单位进行推算.");
			orgs = UserLocalServiceUtil.getUserById(applyUserId)
					.getOrganizations();
		} else {
			orgs = new ArrayList<Organization>();
			orgs.add(OrganizationLocalServiceUtil.getOrganization(orgUnitId));
		}
		//3、得到业务单位或其上级单位中被赋予指定角色的用户
		for (Organization organization : orgs) {
			List<String> orgUsersWithRole = getRoleUsersByOrganization(
					organization.getOrganizationId(), role.getRoleId(), groupId);
			if (orgUsersWithRole.isEmpty()) {
				if (organization.isRoot()) { // 如果没找到，继续在表单所属组织的其它组织层次中找（一个人又多个组织的情况）
					continue;
				} else { // 还未到根组织,递归上溯找
					Organization parentOrganization = organization
							.getParentOrganization();
					while (parentOrganization != null) {
						orgUsersWithRole = getRoleUsersByOrganization(
								parentOrganization.getOrganizationId(),
								role.getRoleId(), groupId);
						if (orgUsersWithRole.isEmpty()) {
							if (parentOrganization.isRoot()) {
								break;
							} else {
								parentOrganization = parentOrganization
										.getParentOrganization();
							}
						} else {
							break; // 一旦在组织层次上找到拥有指定角色的用户，不再继续
						}
					}
				}
			}
			for (String userId : orgUsersWithRole) {
				if(!candidateUserList.contains(userId)){
					candidateUserList.add(userId);
				}
			}
		}

		if (candidateUserList.isEmpty()) {
			throw new PortalException("你所隶属的组织中找不到被授予【" + roleNameString
					+ "】组织角色的用户，请联系管理员授权后再提交。");
		}
		return candidateUserList;
	}

	private List<String> getRoleUsersByOrganization(long organizationId,
			long roleId, long groupId) throws SystemException, PortalException {
		List<String> candidateUserList = new ArrayList<String>();
		List<User> users = UserLocalServiceUtil
				.getOrganizationUsers(organizationId);
		for (User user : users) {
			List<UserGroupRole> ugrList = UserGroupRoleLocalServiceUtil
					.getUserGroupRoles(user.getUserId());
			for (UserGroupRole userGroupRole : ugrList) {
				if (userGroupRole.getRoleId() == roleId) {
					_log.info("添加了用户:" + user.getUserId() + "----"
							+ user.getScreenName() + "--" + user.getFullName());
					candidateUserList.add("" + user.getUserId());
				}
			}
		}
		return candidateUserList;
	}
}
