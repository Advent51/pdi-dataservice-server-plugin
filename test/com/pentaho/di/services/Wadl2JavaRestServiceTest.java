package com.pentaho.di.services;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import org.pentaho.di.repository.pur.PurRepositoryRestService;
import org.pentaho.di.repository.pur.WebServiceManager;
import org.pentaho.platform.plugin.services.importexport.InitializationException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
/**
 * This class is an integrated test, not a unit test.  Assuming there is a running pdi server on localhost, this
 * class will test rest service connectivity.   The first test will see if the generated code from Wadl2java can
 * run a web service.  The second test will test if our WebServiceManager can do the same. 
 * 
 * @author tkafalas
 *
 */
public class Wadl2JavaRestServiceTest {
  private Client client = null;

  public static void main( String[] args ) {
    Wadl2JavaRestServiceTest test = new Wadl2JavaRestServiceTest();
    try {
      test.initRestService1();
      test.initRestService2();
    } catch ( InitializationException e ) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /**
   * Test stand alone generated code web service connectivity
   */
  private void initRestService1() throws InitializationException {
    // get information about the remote connection
    String username = "admin";
    String password = "password";

    ClientConfig clientConfig = new DefaultClientConfig();
    client = Client.create( clientConfig );
    client.addFilter( new HTTPBasicAuthFilter( username, password ) );

    URI baseUri = null;
    try {
      baseUri = new URI( "http://localHost:9080/pentaho-di/plugin/" );
    } catch ( URISyntaxException e ) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    PentahoDiPlugin.PurRepositoryPluginApiRevision revisionService =
        PentahoDiPlugin.purRepositoryPluginApiRevision( client, baseUri );

    PentahoDiPlugin.PurRepositoryPluginApiRevision.VersioningEnabled versioningEnabledMethod =
        revisionService.versioningEnabled();

    String result = versioningEnabledMethod.getAs( String.class );
    System.out.println( "Test1: " + result );
  }

  /**
   * Test web service connectivity using the the WebServiceManager
   * 
   * @throws InitializationException
   */
  private void initRestService2() throws InitializationException {
    // simulate the registration of web service classes
    String username = "admin";
    String password = "password";
    WebServiceManager webServiceManager = new WebServiceManager( "http://localHost:9080/pentaho-di", username );
    
    // Now get a class representing the services in the class
    //PentahoDiPlugin
    PurRepositoryRestService.PurRepositoryPluginApiRevision servicePort = null;
    try {
      servicePort = webServiceManager.createService( username, password, PurRepositoryRestService.PurRepositoryPluginApiRevision.class );
    } catch ( MalformedURLException e ) {
      // Should never happen
      e.printStackTrace();
    }
    
    //Call any of the web services there
    String commentsEnabled = servicePort.versionCommentsEnabled().getAs( String.class );
    String versionsEnabled = servicePort.versioningEnabled().getAs( String.class );
    System.out.println( "Test2: " + commentsEnabled + "/" + versionsEnabled );
    
    webServiceManager.close();
  }
}
