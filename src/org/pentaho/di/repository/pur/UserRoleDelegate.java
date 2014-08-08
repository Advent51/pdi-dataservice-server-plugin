/*!
 * PENTAHO CORPORATION PROPRIETARY AND CONFIDENTIAL
 *
 * Copyright 2002 - 2014 Pentaho Corporation (Pentaho). All rights reserved.
 *
 * NOTICE: All information including source code contained herein is, and
 * remains the sole property of Pentaho and its licensors. The intellectual
 * and technical concepts contained herein are proprietary and confidential
 * to, and are trade secrets of Pentaho and may be covered by U.S. and foreign
 * patents, or patents in process, and are protected by trade secret and
 * copyright laws. The receipt or possession of this source code and/or related
 * information does not convey or imply any rights to reproduce, disclose or
 * distribute its contents, or to manufacture, use, or sell anything that it
 * may describe, in whole or in part. Any reproduction, modification, distribution,
 * or public display of this information without the express written authorization
 * from Pentaho is strictly prohibited and in violation of applicable laws and
 * international treaties. Access to the source code contained herein is strictly
 * prohibited to anyone except those individuals and entities who have executed
 * confidentiality and non-disclosure agreements or other agreements with Pentaho,
 * explicitly covering such access.
 */

package org.pentaho.di.repository.pur;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.IUser;
import org.pentaho.di.repository.pur.model.IEEUser;
import org.pentaho.di.repository.pur.model.IRole;
import org.pentaho.di.ui.repository.pur.services.IRoleSupportSecurityManager;
import org.pentaho.platform.api.engine.security.userroledao.UserRoleInfo;
import org.pentaho.platform.security.userrole.ws.IUserRoleListWebService;
import org.pentaho.platform.security.userroledao.ws.IUserRoleWebService;
import org.pentaho.platform.security.userroledao.ws.ProxyPentahoRole;
import org.pentaho.platform.security.userroledao.ws.ProxyPentahoUser;
import org.pentaho.platform.security.userroledao.ws.UserRoleException;
import org.pentaho.platform.security.userroledao.ws.UserRoleSecurityInfo;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

public class UserRoleDelegate implements java.io.Serializable {

  private static final long serialVersionUID = 1295309456550391059L; /* EESOURCE: UPDATE SERIALVERUID */
  private UserRoleListChangeListenerCollection userRoleListChangeListeners;

  IUserRoleWebService userRoleWebService;

  IUserRoleListWebService userDetailsRoleListWebService;

  IRoleSupportSecurityManager rsm;

  Log logger;

  UserRoleLookupCache lookupCache;

  UserRoleSecurityInfo userRoleSecurityInfo;

  UserRoleInfo userRoleInfo;

  boolean hasNecessaryPermissions = false;
  boolean managed = true;

  public UserRoleDelegate(IRoleSupportSecurityManager rsm, PurRepositoryMeta repositoryMeta, IUser userInfo, Log logger, ServiceManager serviceManager) {
    try {
      this.logger = logger;
      userDetailsRoleListWebService =
          serviceManager.createService( userInfo.getLogin(), userInfo.getPassword(), IUserRoleListWebService.class );
      userRoleWebService =
          serviceManager.createService( userInfo.getLogin(), userInfo.getPassword(), IUserRoleWebService.class );
      this.rsm = rsm;
      initManaged( repositoryMeta, userInfo );
      updateUserRoleInfo();
    } catch (Exception e) {
      this.logger.error(BaseMessages.getString(UserRoleDelegate.class,
          "UserRoleDelegate.ERROR_0001_UNABLE_TO_INITIALIZE_USER_ROLE_WEBSVC"), e); //$NON-NLS-1$
    }
  }
  
  private void initManaged(PurRepositoryMeta repositoryMeta, IUser userInfo) throws JSONException {
    String baseUrl = repositoryMeta.getRepositoryLocation().getUrl();
    String webService = baseUrl + (baseUrl.endsWith("/")?"":"/")+ "api/system/authentication-provider";
    HTTPBasicAuthFilter authFilter = new HTTPBasicAuthFilter(userInfo.getLogin(), userInfo.getPassword());
    Client client = new Client();
    client.addFilter(authFilter);
    WebResource resource = client.resource(webService);
    String response = resource.accept(MediaType.APPLICATION_JSON_TYPE).get(String.class);
    String provider = new JSONObject(response).getString("authenticationType");
    managed = "jackrabbit".equals(provider);
  }

  public void updateUserRoleInfo() throws UserRoleException {
      if (isManaged()) {
          userRoleSecurityInfo = userRoleWebService.getUserRoleSecurityInfo();
          lookupCache = new UserRoleLookupCache(userRoleSecurityInfo, rsm);
          hasNecessaryPermissions = true;
      } else {
          userRoleInfo = userDetailsRoleListWebService.getUserRoleInfo();
          hasNecessaryPermissions = false;
      }
  }
  public boolean isManaged() {
    return managed;
  }
  public void createUser(IUser newUser) throws KettleException {
    if (hasNecessaryPermissions) {
      try {
        ProxyPentahoUser user = UserRoleHelper.convertToPentahoProxyUser(newUser);
        userRoleWebService.createUser(user);
        if (newUser instanceof IEEUser) {
          userRoleWebService.setRoles(user, UserRoleHelper.convertToPentahoProxyRoles(((IEEUser) newUser).getRoles()));
        }
        lookupCache.insertUserToLookupSet(newUser);
        fireUserRoleListChange();
      } catch (Exception e) {
          throw new KettleException(BaseMessages.getString(UserRoleDelegate.class,
          "UserRoleDelegate.ERROR_0002_UNABLE_TO_CREATE_USER", newUser.getLogin()), e); //$NON-NLS-1$
      }
    } else {
      throw new KettleException(BaseMessages.getString(UserRoleDelegate.class,
          "UserRoleDelegate.ERROR_0014_INSUFFICIENT_PRIVILEGES")); //$NON-NLS-1$
    }

  }

  public void deleteUsers(List<IUser> users) throws KettleException {
    if (hasNecessaryPermissions) {
      try {
        userRoleWebService.deleteUsers(UserRoleHelper.convertToPentahoProxyUsers(users));
        lookupCache.removeUsersFromLookupSet(users);
        fireUserRoleListChange();
      } catch (Exception e) {
        throw new KettleException(BaseMessages.getString(UserRoleDelegate.class,
            "UserRoleDelegate.ERROR_0003_UNABLE_TO_DELETE_USERS", e.getLocalizedMessage()), e); //$NON-NLS-1$
      }
    } else {
      throw new KettleException(BaseMessages.getString(UserRoleDelegate.class,
          "UserRoleDelegate.ERROR_0014_INSUFFICIENT_PRIVILEGES")); //$NON-NLS-1$
    }
  }

  public void deleteUser(String name) throws KettleException {
    if (hasNecessaryPermissions) {
      try {
        ProxyPentahoUser user = userRoleWebService.getUser(name);
        if (user != null) {
          ProxyPentahoUser[] users = new ProxyPentahoUser[1];
          users[0] = user;
          userRoleWebService.deleteUsers(users);
          fireUserRoleListChange();
        } else {
          throw new KettleException(BaseMessages.getString(UserRoleDelegate.class,
              "UserRoleDelegate.ERROR_0004_UNABLE_TO_DELETE_USER", name)); //$NON-NLS-1$       
        }
      } catch (Exception e) {
        throw new KettleException(BaseMessages.getString(UserRoleDelegate.class,
            "UserRoleDelegate.ERROR_0004_UNABLE_TO_DELETE_USER", name), e); //$NON-NLS-1$
      }
    } else {
      throw new KettleException(BaseMessages.getString(UserRoleDelegate.class,
          "UserRoleDelegate.ERROR_0014_INSUFFICIENT_PRIVILEGES")); //$NON-NLS-1$
    }
  }

  public void setUsers(List<IUser> users) throws KettleException {
    // TODO Figure out what to do here
  }

  public IUser getUser(String name, String password) throws KettleException {
    if (hasNecessaryPermissions) {
      IUser userInfo = null;
      try {
        ProxyPentahoUser user = userRoleWebService.getUser(name);
        if (user != null && user.getName().equals(name) && user.getPassword().equals(password)) {
          userInfo = UserRoleHelper.convertToUserInfo(user, userRoleWebService.getRolesForUser(user), rsm);
        }
      } catch (Exception e) {
        throw new KettleException(BaseMessages.getString(UserRoleDelegate.class,
            "UserRoleDelegate.ERROR_0005_UNABLE_TO_GET_USER", name), e); //$NON-NLS-1$
      }
      return userInfo;
    } else {
      throw new KettleException(BaseMessages.getString(UserRoleDelegate.class,
          "UserRoleDelegate.ERROR_0014_INSUFFICIENT_PRIVILEGES")); //$NON-NLS-1$
    }
  }

  public IUser getUser(String name) throws KettleException {
    if (hasNecessaryPermissions) {
      IUser userInfo = null;
      try {
        ProxyPentahoUser user = userRoleWebService.getUser(name);
        if (user != null && user.getName().equals(name)) {
          userInfo = UserRoleHelper.convertToUserInfo(user, userRoleWebService.getRolesForUser(user), rsm);
        }
      } catch (Exception e) {
        throw new KettleException(BaseMessages.getString(UserRoleDelegate.class,
            "UserRoleDelegate.ERROR_0005_UNABLE_TO_GET_USER", name), e); //$NON-NLS-1$
      }
      return userInfo;
    } else {
      throw new KettleException(BaseMessages.getString(UserRoleDelegate.class,
          "UserRoleDelegate.ERROR_0014_INSUFFICIENT_PRIVILEGES")); //$NON-NLS-1$
    }
  }

  public List<IUser> getUsers() throws KettleException {
    try {
      if (hasNecessaryPermissions) {
        return UserRoleHelper.convertFromProxyPentahoUsers(userRoleSecurityInfo, rsm);
      } else {
        return UserRoleHelper.convertFromNonPentahoUsers(userRoleInfo, rsm);
      }
    } catch (Exception e) {
      throw new KettleException(BaseMessages.getString(UserRoleDelegate.class,
          "UserRoleDelegate.ERROR_0006_UNABLE_TO_GET_USERS"), e); //$NON-NLS-1$
    }
  }

  public void updateUser(IUser user) throws KettleException {
    if (hasNecessaryPermissions) {
      try {
        ProxyPentahoUser proxyUser = UserRoleHelper.convertToPentahoProxyUser(user);
        userRoleWebService.updateUser(proxyUser);
        if (user instanceof IEEUser) {
          userRoleWebService
              .setRoles(proxyUser, UserRoleHelper.convertToPentahoProxyRoles(((IEEUser) user).getRoles()));
        }
        lookupCache.updateUserInLookupSet(user);
        fireUserRoleListChange();
      } catch (Exception e) {
        throw new KettleException(BaseMessages.getString(UserRoleDelegate.class,
            "UserRoleDelegate.ERROR_0007_UNABLE_TO_UPDATE_USER", user.getLogin()), e); //$NON-NLS-1$
      }
    } else {
      throw new KettleException(BaseMessages.getString(UserRoleDelegate.class,
          "UserRoleDelegate.ERROR_0014_INSUFFICIENT_PRIVILEGES")); //$NON-NLS-1$
    }
  }

  public void createRole(IRole newRole) throws KettleException {
    if (hasNecessaryPermissions) {
      try {
        ProxyPentahoRole role = UserRoleHelper.convertToPentahoProxyRole(newRole);
        userRoleWebService.createRole(role);
        userRoleWebService.setUsers(role, UserRoleHelper.convertToPentahoProxyUsers(newRole.getUsers()));
        lookupCache.insertRoleToLookupSet(newRole);
        fireUserRoleListChange();
      } catch (UserRoleException e) {
          throw new KettleException(BaseMessages.getString(UserRoleDelegate.class,
              "UserRoleDelegate.ERROR_0008_UNABLE_TO_CREATE_ROLE", newRole.getName()), e); //$NON-NLS-1$
      }
    } else {
      throw new KettleException(BaseMessages.getString(UserRoleDelegate.class,
          "UserRoleDelegate.ERROR_0014_INSUFFICIENT_PRIVILEGES")); //$NON-NLS-1$
    }
  }

  public void deleteRoles(List<IRole> roles) throws KettleException {
    if (hasNecessaryPermissions) {
      try {
        userRoleWebService.deleteRoles(UserRoleHelper.convertToPentahoProxyRoles(roles));
        lookupCache.removeRolesFromLookupSet(roles);
        fireUserRoleListChange();
      } catch (Exception e) {
        throw new KettleException(BaseMessages.getString(UserRoleDelegate.class,
            "UserRoleDelegate.ERROR_0009_UNABLE_TO_DELETE_ROLES"), e); //$NON-NLS-1$
      }
    } else {
      throw new KettleException(BaseMessages.getString(UserRoleDelegate.class,
          "UserRoleDelegate.ERROR_0014_INSUFFICIENT_PRIVILEGES")); //$NON-NLS-1$
    }
  }

  public IRole getRole(String name) throws KettleException {
    if (hasNecessaryPermissions) {
      try {
        return UserRoleHelper.convertFromProxyPentahoRole(userRoleWebService, UserRoleHelper.getProxyPentahoRole(
            userRoleWebService, name), lookupCache, rsm);
      } catch (Exception e) {
        throw new KettleException(BaseMessages.getString(UserRoleDelegate.class,
            "UserRoleDelegate.ERROR_0010_UNABLE_TO_GET_ROLE", name), e); //$NON-NLS-1$
      }
    } else {
      throw new KettleException(BaseMessages.getString(UserRoleDelegate.class,
          "UserRoleDelegate.ERROR_0014_INSUFFICIENT_PRIVILEGES")); //$NON-NLS-1$
    }
  }

  public List<IRole> getRoles() throws KettleException {
    try {
      if (hasNecessaryPermissions) {
          return UserRoleHelper.convertToListFromProxyPentahoRoles(userRoleSecurityInfo, rsm);
      } else {
        return UserRoleHelper.convertToListFromNonPentahoRoles(userRoleInfo, rsm);
      }
    } catch (Exception e) {
      throw new KettleException(BaseMessages.getString(UserRoleDelegate.class,
          "UserRoleDelegate.ERROR_0011_UNABLE_TO_GET_ROLES"), e); //$NON-NLS-1$
    }
  }

  public List<IRole> getDefaultRoles() throws KettleException {
    if (hasNecessaryPermissions) {
      try {
        return UserRoleHelper.convertToListFromProxyPentahoDefaultRoles(userRoleSecurityInfo, rsm);
      } catch (Exception e) {
        throw new KettleException(BaseMessages.getString(UserRoleDelegate.class,
            "UserRoleDelegate.ERROR_0011_UNABLE_TO_GET_ROLES"), e); //$NON-NLS-1$
      }
    } else {
      throw new KettleException(BaseMessages.getString(UserRoleDelegate.class,
          "UserRoleDelegate.ERROR_0014_INSUFFICIENT_PRIVILEGES")); //$NON-NLS-1$
    }
  }

  public void updateRole(IRole role) throws KettleException {
    if (hasNecessaryPermissions) {
      try {
        List<String> users = new ArrayList<String>();
        for (IUser user : role.getUsers()) {
          users.add(user.getLogin());
        }
        userRoleWebService.updateRole(role.getName(), role.getDescription(), users);
        lookupCache.updateRoleInLookupSet(role);
        fireUserRoleListChange();
      } catch (Exception e) {
        throw new KettleException(BaseMessages.getString(UserRoleDelegate.class,
            "UserRoleDelegate.ERROR_0012_UNABLE_TO_UPDATE_ROLE", role.getName()), e); //$NON-NLS-1$
      }
    } else {
      throw new KettleException(BaseMessages.getString(UserRoleDelegate.class,
          "UserRoleDelegate.ERROR_0014_INSUFFICIENT_PRIVILEGES")); //$NON-NLS-1$
    }
  }

  public void deleteRole(String name) throws KettleException {
    if (hasNecessaryPermissions) {
      try {
        ProxyPentahoRole roleToDelete = UserRoleHelper.getProxyPentahoRole(userRoleWebService, name);
        if (roleToDelete != null) {
          ProxyPentahoRole[] roleArray = new ProxyPentahoRole[1];
          roleArray[0] = roleToDelete;
          userRoleWebService.deleteRoles(roleArray);
          fireUserRoleListChange();
        } else {
          throw new KettleException(BaseMessages.getString(UserRoleDelegate.class,
              "UserRoleDelegate.ERROR_0013_UNABLE_TO_DELETE_ROLE", name)); //$NON-NLS-1$
        }
      } catch (Exception e) {
        throw new KettleException(BaseMessages.getString(UserRoleDelegate.class,
            "UserRoleDelegate.ERROR_0013_UNABLE_TO_DELETE_ROLE", name), e); //$NON-NLS-1$
      }
    } else {
      throw new KettleException(BaseMessages.getString(UserRoleDelegate.class,
          "UserRoleDelegate.ERROR_0014_INSUFFICIENT_PRIVILEGES")); //$NON-NLS-1$
    }

  }

  public void setRoles(List<IRole> roles) throws KettleException {
    // TODO Figure out what to do here
  }

  public void addUserRoleListChangeListener(IUserRoleListChangeListener listener) {
    if (userRoleListChangeListeners == null) {
      userRoleListChangeListeners = new UserRoleListChangeListenerCollection();
    }
    userRoleListChangeListeners.add(listener);
  }

  public void removeUserRoleListChangeListener(IUserRoleListChangeListener listener) {
    if (userRoleListChangeListeners != null) {
      userRoleListChangeListeners.remove(listener);
    }
  }

  /**
   * Fire all current {@link IUserRoleListChangeListener}.
   */
  void fireUserRoleListChange() {

    if (userRoleListChangeListeners != null) {
      userRoleListChangeListeners.fireOnChange();
    }
  }
}
