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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.commons.lang.StringUtils;
import org.pentaho.di.core.util.ExecutorUtil;
import org.pentaho.platform.repository2.unified.webservices.jaxws.IUnifiedRepositoryJaxwsWebService;
import org.pentaho.platform.security.policy.rolebased.ws.IAuthorizationPolicyWebService;
import org.pentaho.platform.security.policy.rolebased.ws.IRoleAuthorizationPolicyRoleBindingDaoWebService;
import org.pentaho.platform.security.userrole.ws.IUserRoleListWebService;
import org.pentaho.platform.security.userroledao.ws.IUserRoleWebService;

import com.pentaho.pdi.ws.IRepositorySyncWebService;
import com.sun.xml.ws.developer.JAXWSProperties;

/**
 * Web service factory. Not a true factory in that the things that this factory can create are not configurable. But it
 * does cache the services.
 * 
 * @author mlowery
 */
public class WebServiceManager implements ServiceManager {

  /**
   * Header name must match that specified in ProxyTrustingFilter. Note that an header has the following form: initial
   * capital letter followed by all lowercase letters.
   */
  private static final String TRUST_USER = "_trust_user_"; //$NON-NLS-1$

  private static final String NAMESPACE_URI = "http://www.pentaho.org/ws/1.0"; //$NON-NLS-1$

  private static final ExecutorService executor = ExecutorUtil.getExecutor();

  private final Map<String, Future<Object>> serviceCache = new HashMap<String, Future<Object>>();

  private final Map<Class<?>, String> serviceNameMap;

  private final String baseUrl;

  private final String lastUsername;

  public WebServiceManager( String baseUrl, String username ) {
    this.baseUrl = baseUrl;
    this.lastUsername = username;
    Map<Class<?>, String> serviceNameMap = new HashMap<Class<?>, String>();
    serviceNameMap.put( IUnifiedRepositoryJaxwsWebService.class, "unifiedRepository" );//$NON-NLS-1$
    serviceNameMap.put( IRepositorySyncWebService.class, "repositorySync" );//$NON-NLS-1$
    serviceNameMap.put( IUserRoleListWebService.class, "userRoleListService" );//$NON-NLS-1$
    serviceNameMap.put( IUserRoleWebService.class, "userRoleService" );//$NON-NLS-1$
    serviceNameMap.put( IRoleAuthorizationPolicyRoleBindingDaoWebService.class, "roleBindingDao" );//$NON-NLS-1$
    serviceNameMap.put( IAuthorizationPolicyWebService.class, "authorizationPolicy" );//$NON-NLS-1$
    this.serviceNameMap = Collections.unmodifiableMap( serviceNameMap );
  }

  @Override
  @SuppressWarnings( "unchecked" )
  public <T> T createService( final String username, final String password, final Class<T> clazz )
    throws MalformedURLException {
    final Future<Object> resultFuture;
    synchronized ( serviceCache ) {
      // if this is true, a coder did not make sure that clearServices was called on disconnect
      if ( lastUsername != null && !lastUsername.equals( username ) ) {
        throw new IllegalStateException();
      }

      final String serviceName = serviceNameMap.get( clazz );
      if ( serviceName == null ) {
        throw new IllegalStateException();
      }

      // build the url handling whether or not baseUrl ends with a slash
      // String baseUrl = repositoryMeta.getRepositoryLocation().getUrl();
      final URL url =
          new URL( baseUrl + ( baseUrl.endsWith( "/" ) ? "" : "/" ) + "webservices/" + serviceName + "?wsdl" ); //$NON-NLS-1$ //$NON-NLS-2$

      String key = url.toString() + '_' + serviceName + '_' + clazz.getName();
      if ( !serviceCache.containsKey( key ) ) {
        resultFuture = executor.submit( new Callable<Object>() {

          @Override
          public Object call() throws Exception {
            Service service = Service.create( url, new QName( NAMESPACE_URI, serviceName ) );
            T port = service.getPort( clazz );
            // add TRUST_USER if necessary
            if ( StringUtils.isNotBlank( System.getProperty( "pentaho.repository.client.attemptTrust" ) ) ) {
              ( (BindingProvider) port ).getRequestContext().put( MessageContext.HTTP_REQUEST_HEADERS,
                  Collections.singletonMap( TRUST_USER, Collections.singletonList( username ) ) );
            } else {
              // http basic authentication
              ( (BindingProvider) port ).getRequestContext().put( BindingProvider.USERNAME_PROPERTY, username );
              ( (BindingProvider) port ).getRequestContext().put( BindingProvider.PASSWORD_PROPERTY, password );
            }
            // accept cookies to maintain session on server
            ( (BindingProvider) port ).getRequestContext().put( BindingProvider.SESSION_MAINTAIN_PROPERTY, true );
            // support streaming binary data
            // TODO mlowery this is not portable between JAX-WS implementations (uses com.sun)
            ( (BindingProvider) port ).getRequestContext().put( JAXWSProperties.HTTP_CLIENT_STREAMING_CHUNK_SIZE, 8192 );
            SOAPBinding binding = (SOAPBinding) ( (BindingProvider) port ).getBinding();
            binding.setMTOMEnabled( true );
            return port;
          }
        } );
        serviceCache.put( key, resultFuture );
      } else {
        resultFuture = serviceCache.get( key );
      }
    }

    try {
      return (T) resultFuture.get();
    } catch ( InterruptedException e ) {
      throw new RuntimeException( e );
    } catch ( ExecutionException e ) {
      Throwable cause = e.getCause();
      if ( cause != null ) {
        if ( cause instanceof RuntimeException ) {
          throw (RuntimeException) cause;
        } else if ( cause instanceof MalformedURLException ) {
          throw (MalformedURLException) cause;
        }
      }
      throw new RuntimeException( e );
    }
  }

  @Override
  public synchronized void close() {
    serviceCache.clear();
  }
}
