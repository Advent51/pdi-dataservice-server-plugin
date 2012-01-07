/**
 * The Pentaho proprietary code is licensed under the terms and conditions
 * of the software license agreement entered into between the entity licensing
 * such code and Pentaho Corporation. 
 */
package org.pentaho.di.ui.repository.pur.repositoryexplorer.model;

import java.util.EnumSet;

import org.pentaho.di.repository.ObjectRecipient;
import org.pentaho.di.repository.pur.model.ObjectAce;
import org.pentaho.di.repository.pur.model.ObjectPermission;
import org.pentaho.ui.xul.XulEventSourceAdapter;

/**
 * TODO mlowery This class represents an ACE, not an ACL.
 */
public class UIRepositoryObjectAcl extends XulEventSourceAdapter implements java.io.Serializable {

  private static final long serialVersionUID = 8320176731576605496L; /* EESOURCE: UPDATE SERIALVERUID */
	
	@Override
  public boolean equals(Object obj) {
	  if(obj == null) {
	    return false;
	  }
	  UIRepositoryObjectAcl acl = (UIRepositoryObjectAcl) obj;
    return ace.equals(acl.getAce());
  }
  protected ObjectAce ace;
	
	public ObjectAce getAce() {
    return ace;
  }
  public UIRepositoryObjectAcl(ObjectAce ace) {
		this.ace = ace;
	}
	public String getRecipientName() {
		return ace.getRecipient().getName();
	}
	public void setRecipientName(String recipientName) {
		ace.getRecipient().setName(recipientName);		
		this.firePropertyChange("recipientName", null, recipientName); //$NON-NLS-1$
	}
	public ObjectRecipient.Type getRecipientType() {
		return ace.getRecipient().getType();
	}
	public void setRecipientType(ObjectRecipient.Type recipientType) {
		ace.getRecipient().setType(recipientType);
		this.firePropertyChange("recipientType", null, recipientType); //$NON-NLS-1$		
	}
	public EnumSet<ObjectPermission> getPermissionSet() {
		return ace.getPermissions();
	}
	public void setPermissionSet(ObjectPermission first, ObjectPermission... rest) {
		ace.setPermissions(first, rest);
		this.firePropertyChange("permissions", null, ace.getPermissions()); //$NON-NLS-1$
	}
	
	public void setPermissionSet(EnumSet<ObjectPermission> permissionSet) {
		EnumSet<ObjectPermission> previousVal = ace.getPermissions(); 
		ace.setPermissions(permissionSet);
		this.firePropertyChange("permissions", previousVal, ace.getPermissions()); //$NON-NLS-1$
	}
	
	public void addPermission(ObjectPermission permissionToAdd) {
		ace.getPermissions().add(permissionToAdd);
	}
	public void removePermission(ObjectPermission permissionToRemove) {
		ace.getPermissions().remove(permissionToRemove);;
	}
}
