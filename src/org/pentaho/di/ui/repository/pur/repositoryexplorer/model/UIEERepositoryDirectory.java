/**
 * The Pentaho proprietary code is licensed under the terms and conditions
 * of the software license agreement entered into between the entity licensing
 * such code and Pentaho Corporation. 
 */
package org.pentaho.di.ui.repository.pur.repositoryexplorer.model;

import java.util.HashMap;
import java.util.Map;

import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.repository.RepositoryDirectoryInterface;
import org.pentaho.di.repository.pur.PurRepository;
import org.pentaho.di.ui.repository.pur.repositoryexplorer.IAclObject;
import org.pentaho.di.ui.repository.pur.services.IAclService;
import org.pentaho.di.ui.repository.repositoryexplorer.AccessDeniedException;
import org.pentaho.di.ui.repository.repositoryexplorer.model.UIRepositoryDirectory;
import org.pentaho.platform.api.repository2.unified.RepositoryFilePermission;

public class UIEERepositoryDirectory extends UIRepositoryDirectory implements IAclObject, java.io.Serializable {

  private static final long serialVersionUID = -6273975748634580673L; /* EESOURCE: UPDATE SERIALVERUID */

  private IAclService aclService;
  private Map<RepositoryFilePermission,Boolean> hasAccess = null;
  
  public UIEERepositoryDirectory() {
    super();
  }

  public UIEERepositoryDirectory(RepositoryDirectoryInterface rd, UIRepositoryDirectory uiParent, Repository rep) {
    super(rd, uiParent, rep);
    initializeService(rep);
  }

  public void getAcls(UIRepositoryObjectAcls acls, boolean forceParentInheriting) throws AccessDeniedException{
    try {
      acls.setObjectAcl(aclService.getAcl(getObjectId(), forceParentInheriting));
    } catch(KettleException ke) {
      throw new AccessDeniedException(ke);
    }
  }

  public void getAcls(UIRepositoryObjectAcls acls) throws AccessDeniedException{
    try {
      acls.setObjectAcl(aclService.getAcl(getObjectId(), false));
    } catch(KettleException ke) {
      throw new AccessDeniedException(ke);
    }
  }

  public void setAcls(UIRepositoryObjectAcls security) throws AccessDeniedException{
    try {
      aclService.setAcl(getObjectId(), security.getObjectAcl());
    } catch (KettleException e) {
      throw new AccessDeniedException(e);
    }
  }
  
  private void initializeService(Repository rep) {
    try {
      if (rep.hasService(IAclService.class)) {
        aclService = (IAclService) rep.getService(IAclService.class);
      } else {
        throw new IllegalStateException();
      }
    } catch (KettleException e) {
      throw new RuntimeException(e);
    } 

  }
  
  public void delete(boolean deleteHomeDirectories)throws Exception{
    if(rep instanceof PurRepository) {
      ((PurRepository)rep).deleteRepositoryDirectory(getDirectory(), deleteHomeDirectories);
    } else {
      rep.deleteRepositoryDirectory(getDirectory());
    }
    getParent().getChildren().remove(this);
    if(getParent().getRepositoryObjects().contains(this))
      getParent().getRepositoryObjects().remove(this);
    getParent().refresh();
  }
  
  public void setName(String name, boolean renameHomeDirectories)throws Exception{
    if (getDirectory().getName().equalsIgnoreCase(name)){
      return;
    }
    
    if(rep instanceof PurRepository) {
      ((PurRepository)rep).renameRepositoryDirectory(getDirectory().getObjectId(), null, name, renameHomeDirectories);
    } else {
      rep.renameRepositoryDirectory(getDirectory().getObjectId(), null, name);
    }
    
    // Update the object reference so the new name is displayed
    obj = rep.getObjectInformation(getObjectId(), getRepositoryElementType());
    refresh();
  }

  @Override
  public void clearAcl() {
    hasAccess = null;
  }

  @Override
  public boolean hasAccess(RepositoryFilePermission perm) throws KettleException {
    if (hasAccess == null) {
      hasAccess = new HashMap<RepositoryFilePermission, Boolean>();
    }
    if (hasAccess.get(perm) == null) {
      hasAccess.put(perm, new Boolean(aclService.hasAccess(getObjectId(), perm)));
    }
    return hasAccess.get(perm).booleanValue();
  }
}
