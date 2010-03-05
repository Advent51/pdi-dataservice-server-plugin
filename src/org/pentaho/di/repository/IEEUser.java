package org.pentaho.di.repository;

import java.util.Set;

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
