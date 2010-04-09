package org.pentaho.di.ui.repository.pur.repositoryexplorer;

import java.util.Set;

import org.pentaho.di.ui.repository.repositoryexplorer.model.IUIUser;

public interface IUIEEUser extends IUIUser{

  public boolean addRole(IUIRole role);
  public boolean removeRole(IUIRole role);
  public void clearRoles();
  public void setRoles(Set<IUIRole> roles);
  public Set<IUIRole> getRoles();
}
