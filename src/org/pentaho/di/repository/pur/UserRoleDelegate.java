package org.pentaho.di.repository.pur;

import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ActionPermission;
import org.pentaho.di.repository.RoleInfo;
import org.pentaho.di.repository.UserInfo;
import org.pentaho.platform.engine.security.userroledao.ws.IUserRoleWebService;
import org.pentaho.platform.engine.security.userroledao.ws.ProxyPentahoRole;
import org.pentaho.platform.engine.security.userroledao.ws.ProxyPentahoUser;

public class UserRoleDelegate {

  IUserRoleWebService userRoleWebService = null;
  private static final Log logger = LogFactory.getLog(UserRoleDelegate.class);
  UserRoleLookupCache lookupCache = null;
  Map<String, EnumSet<ActionPermission>> actionPermissionMap = new HashMap<String, EnumSet<ActionPermission>>();
  public UserRoleDelegate(PurRepositoryMeta repositoryMeta, UserInfo userInfo) {
    try {
      final String url = repositoryMeta.getRepositoryLocation().getUrl() + "/userroleadmin?wsdl"; //$NON-NLS-1$
      Service service = Service.create(new URL(url), new QName("http://www.pentaho.org/ws/1.0", //$NON-NLS-1$
          "UserRoleWebServiceService"));//$NON-NLS-1$
      userRoleWebService = service.getPort(IUserRoleWebService.class);
      ((BindingProvider) userRoleWebService).getRequestContext().put(BindingProvider.USERNAME_PROPERTY,
          userInfo.getLogin());
      ((BindingProvider) userRoleWebService).getRequestContext().put(BindingProvider.PASSWORD_PROPERTY,
          userInfo.getPassword());
      lookupCache = new UserRoleLookupCache(userRoleWebService);
    } catch (Exception e) {
      logger.error(BaseMessages.getString("UserRoleDelegate.ERROR_0001_UNABLE_TO_INITIALIZE_USER_ROLE_WEBSVC"), e); //$NON-NLS-1$
    }
  }

  public void createUser(UserInfo newUser) throws KettleException {
    try {
      ProxyPentahoUser user = UserRoleHelper.convertToPentahoProxyUser(newUser);
      userRoleWebService.createUser(user);
      userRoleWebService.setRoles(user, UserRoleHelper.convertToPentahoProxyRoles(newUser.getRoles()));
    } catch (Exception e) {
      throw new KettleException(BaseMessages.getString("UserRoleDelegate.ERROR_0002_UNABLE_TO_CREATE_USER"), e); //$NON-NLS-1$
    }
  }

  public void deleteUsers(List<UserInfo> users) throws KettleException {
    try {
      userRoleWebService.deleteUsers(UserRoleHelper.convertToPentahoProxyUsers(users));
      lookupCache.removeUsersFromLookupSet(users);
    } catch (Exception e) {
      throw new KettleException(BaseMessages.getString("UserRoleDelegate.ERROR_0003_UNABLE_TO_DELETE_USERS"), e); //$NON-NLS-1$
    }
  }

  public void deleteUser(String name) throws KettleException {
    try {
      ProxyPentahoUser user = userRoleWebService.getUser(name);
      if (user != null) {
        ProxyPentahoUser[] users = new ProxyPentahoUser[1];
        users[0] = user;
        userRoleWebService.deleteUsers(users);
      } else {
        throw new KettleException(BaseMessages.getString("UserRoleDelegate.ERROR_0004_UNABLE_TO_DELETE_USER", name)); //$NON-NLS-1$       
      }
    } catch (Exception e) {
      throw new KettleException(BaseMessages.getString("UserRoleDelegate.ERROR_0004_UNABLE_TO_DELETE_USER", name), e); //$NON-NLS-1$
    }
  }

  public void setUsers(List<UserInfo> users) throws KettleException {
    // TODO Figure out what to do here
  }

  public UserInfo getUser(String name, String password) throws KettleException {
    UserInfo userInfo = null;
    try {
      ProxyPentahoUser user = userRoleWebService.getUser(name);
      if (user != null && user.getName().equals(name) && user.getPassword().equals(password)) {
        userInfo = UserRoleHelper.convertFromProxyPentahoUser(userRoleWebService, user, lookupCache);
      }
    } catch (Exception e) {
      throw new KettleException(BaseMessages.getString("UserRoleDelegate.ERROR_0005_UNABLE_TO_GET_USER", name), e); //$NON-NLS-1$
    }
    return userInfo;
  }

  public UserInfo getUser(String name) throws KettleException {
    UserInfo userInfo = null;
    try {
      ProxyPentahoUser user = userRoleWebService.getUser(name);
      if (user != null && user.getName().equals(name)) {
        userInfo = UserRoleHelper.convertFromProxyPentahoUser(userRoleWebService, user, lookupCache);
      }
    } catch (Exception e) {
      throw new KettleException(BaseMessages.getString("UserRoleDelegate.ERROR_0005_UNABLE_TO_GET_USER", name), e); //$NON-NLS-1$
    }
    return userInfo;
  }

  public List<UserInfo> getUsers() throws KettleException {
    try {
      return UserRoleHelper.convertToListFromProxyPentahoUsers(userRoleWebService.getUsers(), userRoleWebService,
          lookupCache);
    } catch (Exception e) {
      throw new KettleException(BaseMessages.getString("UserRoleDelegate.ERROR_0006_UNABLE_TO_GET_USERS"), e); //$NON-NLS-1$
    }
  }

  public void updateUser(UserInfo user) throws KettleException {
    try {
      ProxyPentahoUser proxyUser = UserRoleHelper.convertToPentahoProxyUser(user);
      userRoleWebService.updateUser(proxyUser);
      userRoleWebService.setRoles(proxyUser, UserRoleHelper.convertToPentahoProxyRoles(user.getRoles()));
      lookupCache.updateUserInLookupSet(user);
    } catch (Exception e) {
      throw new KettleException(BaseMessages.getString("UserRoleDelegate.ERROR_0007_UNABLE_TO_UPDATE_USER", user.getLogin()), e); //$NON-NLS-1$
    }
  }

  public void createRole(RoleInfo newRole) throws KettleException {
    try {
      ProxyPentahoRole role = UserRoleHelper.convertToPentahoProxyRole(newRole);
      userRoleWebService.createRole(role);
      userRoleWebService.setUsers(role, UserRoleHelper.convertToPentahoProxyUsers(newRole.getUsers()));
    } catch (Exception e) {
      throw new KettleException(BaseMessages.getString("UserRoleDelegate.ERROR_0008_UNABLE_TO_CREATE_ROLE", newRole.getName()), e); //$NON-NLS-1$
    }
  }

  public void deleteRoles(List<RoleInfo> roles) throws KettleException {
    try {
      userRoleWebService.deleteRoles(UserRoleHelper.convertToPentahoProxyRoles(roles));
      lookupCache.removeRolesFromLookupSet(roles);
    } catch (Exception e) {
      throw new KettleException(BaseMessages.getString("UserRoleDelegate.ERROR_0009_UNABLE_TO_DELETE_ROLES"), e); //$NON-NLS-1$
    }

  }

  public RoleInfo getRole(String name) throws KettleException {
    try {
      RoleInfo roleInfo = UserRoleHelper.convertFromProxyPentahoRole(userRoleWebService, UserRoleHelper.getProxyPentahoRole(
          userRoleWebService, name), lookupCache);
      roleInfo.setActionPermissions(getActionPermissions(roleInfo.getName()));
      return roleInfo; 
    } catch (Exception e) {
      throw new KettleException(BaseMessages.getString("UserRoleDelegate.ERROR_0010_UNABLE_TO_GET_ROLE", name), e); //$NON-NLS-1$
    }
  }

  public List<RoleInfo> getRoles() throws KettleException {
    try {
      List<RoleInfo> roles = UserRoleHelper.convertToListFromProxyPentahoRoles(userRoleWebService.getRoles(), userRoleWebService,
          lookupCache);
      for(RoleInfo role:roles) {
        role.setActionPermissions(getActionPermissions(role.getName()));
      }
      return roles;
    } catch (Exception e) {
      throw new KettleException(BaseMessages.getString("UserRoleDelegate.ERROR_0011_UNABLE_TO_GET_ROLES"), e); //$NON-NLS-1$
    }
  }

  public void updateRole(RoleInfo role) throws KettleException {
    try {
      List<String> users = new ArrayList<String>();
      for (UserInfo user : role.getUsers()) {
        users.add(user.getLogin());
      }
      userRoleWebService.updateRole(role.getName(), role.getDescription(), users);
      lookupCache.updateRoleInLookupSet(role);
    } catch (Exception e) {
      throw new KettleException(BaseMessages.getString("UserRoleDelegate.ERROR_0012_UNABLE_TO_UPDATE_ROLE", role.getName()), e); //$NON-NLS-1$
    }

  }

  public void deleteRole(String name) throws KettleException {
    try {
      ProxyPentahoRole roleToDelete = UserRoleHelper.getProxyPentahoRole(userRoleWebService, name);
      if (roleToDelete != null) {
        ProxyPentahoRole[] roleArray = new ProxyPentahoRole[1];
        roleArray[0] = roleToDelete;
        userRoleWebService.deleteRoles(roleArray);
      } else {
        throw new KettleException(BaseMessages.getString("UserRoleDelegate.ERROR_0013_UNABLE_TO_DELETE_ROLE", name)); //$NON-NLS-1$
      }
    } catch (Exception e) {
      throw new KettleException(BaseMessages.getString("UserRoleDelegate.ERROR_0013_UNABLE_TO_DELETE_ROLE", name), e); //$NON-NLS-1$
    }
  }

  public void setRoles(List<RoleInfo> roles) throws KettleException {
    // TODO Figure out what to do here
  }
  
  public void setActionPermissions(String rolename, EnumSet<ActionPermission> permissions) {
    this.actionPermissionMap.put(rolename,permissions);
  }
  
  private EnumSet<ActionPermission> getActionPermissions(String rolename) {
    if(this.actionPermissionMap.containsKey(rolename)) {
      return this.actionPermissionMap.get(rolename);      
    } else {
      return EnumSet.noneOf(ActionPermission.class);
    }

  }
}
