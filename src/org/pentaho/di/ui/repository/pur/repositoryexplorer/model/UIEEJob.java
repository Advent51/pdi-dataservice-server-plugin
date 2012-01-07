/**
 * The Pentaho proprietary code is licensed under the terms and conditions
 * of the software license agreement entered into between the entity licensing
 * such code and Pentaho Corporation. 
 */
package org.pentaho.di.ui.repository.pur.repositoryexplorer.model;

import java.util.List;

import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.repository.ObjectRevision;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.repository.RepositoryElementMetaInterface;
import org.pentaho.di.repository.pur.model.EERepositoryObject;
import org.pentaho.di.repository.pur.model.ObjectAcl;
import org.pentaho.di.repository.pur.model.RepositoryLock;
import org.pentaho.di.ui.repository.pur.repositoryexplorer.IAclObject;
import org.pentaho.di.ui.repository.pur.repositoryexplorer.ILockObject;
import org.pentaho.di.ui.repository.pur.repositoryexplorer.IRevisionObject;
import org.pentaho.di.ui.repository.pur.services.IAclService;
import org.pentaho.di.ui.repository.pur.services.ILockService;
import org.pentaho.di.ui.repository.pur.services.IRevisionService;
import org.pentaho.di.ui.repository.repositoryexplorer.AccessDeniedException;
import org.pentaho.di.ui.repository.repositoryexplorer.model.UIJob;
import org.pentaho.di.ui.repository.repositoryexplorer.model.UIRepositoryDirectory;

public class UIEEJob extends UIJob implements ILockObject, IRevisionObject, IAclObject, java.io.Serializable {

  private static final long serialVersionUID = 1137552553918382891L; /* EESOURCE: UPDATE SERIALVERUID */
  private ILockService lockService;
  private IRevisionService revisionService;
  private IAclService aclService;
  private UIRepositoryObjectRevisions revisions;
  private EERepositoryObject repObj;
  private ObjectAcl acl;

  public UIEEJob(RepositoryElementMetaInterface rc, UIRepositoryDirectory parent, Repository rep) {
    super(rc, parent, rep);
    if (!(rc instanceof EERepositoryObject)) {
      throw new IllegalArgumentException();
    }
    repObj = (EERepositoryObject) rc;
    try {
      if (rep.hasService(ILockService.class)) {
        lockService = (ILockService) rep.getService(ILockService.class);
      } else {
        throw new IllegalStateException();
      }
      if (rep.hasService(IRevisionService.class)) {
        revisionService = (IRevisionService) rep.getService(IRevisionService.class);
      } else {
        throw new IllegalStateException();
      }
      if (rep.hasService(IAclService.class)) {
        aclService = (IAclService) rep.getService(IAclService.class);
      } else {
        throw new IllegalStateException();
      }      
    } catch (KettleException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getImage() {
    try {
      if(isLocked()) {
        return "images/lock.png"; //$NON-NLS-1$
      }
    } catch (KettleException e) {
      throw new RuntimeException(e);
    }
    return "images/job.png"; //$NON-NLS-1$
  }

  public String getLockMessage() throws KettleException {
    return repObj.getLockMessage();
  }

  public void lock(String lockNote) throws KettleException {
    RepositoryLock lock = lockService.lockJob(getObjectId(), lockNote);
    repObj.setLock(lock);
    uiParent.fireCollectionChanged();
  }

  public void unlock() throws KettleException {
    lockService.unlockJob(getObjectId());
    repObj.setLock(null);
    uiParent.fireCollectionChanged();
  }
  
  public boolean isLocked() throws KettleException {
    return (getRepositoryLock() != null);
  }
  
  public RepositoryLock getRepositoryLock() throws KettleException {
    return repObj.getLock();
  }
  
  public UIRepositoryObjectRevisions getRevisions() throws KettleException {
    if (revisions != null){
      return revisions;
    }
    
    revisions = new UIRepositoryObjectRevisions();
    
    List <ObjectRevision> or = revisionService.getRevisions(getObjectId());

    for (ObjectRevision rev : or) {
      revisions.add(new UIRepositoryObjectRevision(rev));
    }
    return revisions;
  }
  
  protected void refreshRevisions() throws KettleException {
    revisions = null;
    getRevisions();
  }
  
  
  public void restoreRevision(UIRepositoryObjectRevision revision, String commitMessage) throws KettleException {
    if(revisionService != null) {
      revisionService.restoreJob(this.getObjectId(), revision.getName(), commitMessage);
      refreshRevisions();
      uiParent.fireCollectionChanged();
    }
  }
  
  public void getAcls(UIRepositoryObjectAcls acls, boolean forceParentInheriting) throws AccessDeniedException{
    if (acl == null) {
      try {
        acl = aclService.getAcl(getObjectId(), forceParentInheriting);
      } catch(KettleException ke) {
        throw new AccessDeniedException(ke);
      }
    }
    acls.setObjectAcl(acl);
  }

  public void getAcls(UIRepositoryObjectAcls acls) throws AccessDeniedException{
    getAcls(acls, false);
  }

  public void setAcls(UIRepositoryObjectAcls security) throws AccessDeniedException{
    try {
      aclService.setAcl(getObjectId(), security.getObjectAcl());
    } catch (KettleException e) {
      throw new AccessDeniedException(e);
    }
  }

  @Override
  public void clearAcl() {
    acl = null;
  }
}
