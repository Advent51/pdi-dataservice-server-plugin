package org.pentaho.di.ui.repository.pur.repositoryexplorer.abs.model;

import java.util.List;

import org.pentaho.di.repository.RepositorySecurityManager;
import org.pentaho.di.repository.pur.AbsSecurityManager;
import org.pentaho.di.ui.repository.pur.repositoryexplorer.IUIRole;
import org.pentaho.di.ui.repository.pur.repositoryexplorer.abs.IUIAbsRole;
import org.pentaho.di.ui.repository.pur.repositoryexplorer.model.UIEESecurity;

public class UIAbsSecurity extends UIEESecurity {

  public UIAbsSecurity() {
    super();
  }

  public UIAbsSecurity(RepositorySecurityManager rsm) throws Exception {
    super(rsm);
    for (IUIRole systemRole : systemRoleList) {
      if (rsm instanceof AbsSecurityManager) {
        AbsSecurityManager asm = (AbsSecurityManager) rsm;
        List<String> logicalRoles = asm.getLogicalRoles(systemRole.getName());
        if (systemRole instanceof UIAbsRepositoryRole) {
          ((UIAbsRepositoryRole) systemRole).setLogicalRoles(logicalRoles);
        } else {
          throw new IllegalStateException();
        }
      } else {
        throw new IllegalStateException();
      }
    }
  }

  public void addLogicalRole(String logicalRole) {
    IUIRole role = getSelectedRole();
    if (role != null) {
      if(role instanceof IUIAbsRole) {
        ((IUIAbsRole) role).addLogicalRole(logicalRole);
      } else {
        throw new IllegalStateException();
      }
    } else {
      role = getSelectedSystemRole();
      if(role instanceof IUIAbsRole) {
        ((IUIAbsRole) role).addLogicalRole(logicalRole);
      } else {
        throw new IllegalStateException();
      }
    } 
  }

  public void removeLogicalRole(String logicalRole) {
    IUIRole role = getSelectedRole();
    if (role != null) {
      if(role instanceof IUIAbsRole) {
        ((IUIAbsRole) role).removeLogicalRole(logicalRole);
      } else {
        throw new IllegalStateException();
      }
    } else {
      role = getSelectedSystemRole();
      if (role instanceof IUIAbsRole) {
        ((IUIAbsRole) role).removeLogicalRole(logicalRole);
      } else {
        throw new IllegalStateException();
      }
    }
  }
}
