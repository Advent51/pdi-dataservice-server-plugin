/**
 * The Pentaho proprietary code is licensed under the terms and conditions
 * of the software license agreement entered into between the entity licensing
 * such code and Pentaho Corporation. 
 */
package org.pentaho.di.ui.repository.pur.services;

import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.repository.IRepositoryService;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.pur.model.ObjectAcl;
/**
 * Repository service which adds ACL feature to the repository. Using this feature, the user
 * of the repository can retrieve and update ACL for a particular object in the repository
 * @author rmansoor
 *
 */
public interface IAclService extends IRepositoryService{

  /**
   * Get the Permissions of a repository object.
   * 
   * @param Object Id of the repository object
   * @param forceParentInheriting retrieve the effective ACLs as if 'inherit from parent' were true
   * 
   * @return The permissions.
   * @throws KettleException in case something goes horribly wrong
   */
  public ObjectAcl getAcl(ObjectId id, boolean forceParentInheriting) throws KettleException;

  /**
   * Set the Permissions of a repository element.
   * 
   * @param Acl object that needs to be set.
   * @param Object Id of a file for which the acl are being set.
   * 
   * @throws KettleException in case something goes horribly wrong
   */
  public  void setAcl(ObjectId id, ObjectAcl aclObject) throws KettleException;
}
