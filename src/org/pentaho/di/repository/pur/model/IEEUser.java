/*!
* The Pentaho proprietary code is licensed under the terms and conditions
* of the software license agreement entered into between the entity licensing
* such code and Pentaho Corporation.
*
* This software costs money - it is not free
*
* Copyright 2002 - 2013 Pentaho Corporation.  All rights reserved.
*/

package org.pentaho.di.repository.pur.model;

import java.util.Set;

import org.pentaho.di.repository.IUser;

/**
 * Repository User object with role support
 * @author rmansoor
 *
 */
public interface IEEUser extends IUser {
  /**
   * Associate a role to this particular user
   * 
   * @param role to be associate
   * @return return the status whether the role association to this user was successful or not
   */
  public boolean addRole(IRole role);
  /**
   * Remove the association of a role to this particular user
   * 
   * @param role to be un associated
   * @return return the status whether the role un association to this user was successful or not
   */  
  public boolean removeRole(IRole role);
  /**
   * Clear all the role association from this particular user
   * 
   */    
  public void clearRoles();
  /**
   * Associate set of roles to this particular user
   * 
   * @param set of roles
   */  
  public void setRoles(Set<IRole> roles);
  /**
   * Retrieve the set of roles associated to this particular user
   * 
   * @return set of associated roles
   */      
  public Set<IRole> getRoles();
}
