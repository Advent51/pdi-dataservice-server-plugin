package org.pentaho.di.repository.pur;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.KettleEnvironment;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.ProfileMeta;
import org.pentaho.di.repository.RepositoryDirectory;
import org.pentaho.di.repository.RepositoryObject;
import org.pentaho.di.repository.RepositoryTestBase;
import org.pentaho.di.repository.StringObjectId;
import org.pentaho.di.repository.UserInfo;
import org.pentaho.di.repository.ProfileMeta.Permission;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.repository.IUnifiedRepository;
import org.pentaho.platform.api.repository.RepositoryFile;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.userdetails.User;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.pentaho.commons.dsc.PentahoLicenseVerifier;
import com.pentaho.commons.dsc.util.TestLicenseStream;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/sample-repository.spring.xml",
    "classpath:/sample-repository-test-override.spring.xml" })
public class PurRepositoryTest extends RepositoryTestBase implements ApplicationContextAware {

  private IUnifiedRepository pur;

  @BeforeClass
  public static void setUpClass() throws Exception {
    // folder cannot be deleted at teardown shutdown hooks have not yet necessarily completed
    // parent folder must match jcrRepository.homeDir bean property in repository-test-override.spring.xml
    FileUtils.deleteDirectory(new File("/tmp/jackrabbit-test"));
    PentahoSessionHolder.setStrategyName(PentahoSessionHolder.MODE_GLOBAL);
  }

  @Before
  public void setUp() throws Exception {
    PentahoSessionHolder.removeSession();
    SecurityContextHolder.getContext().setAuthentication(null);

    // tell kettle to look for plugins in this package (because custom plugins are defined in this class)
    System.setProperty(Const.KETTLE_PLUGIN_PACKAGES, RepositoryTestBase.class.getPackage().getName());

    // test calls into local "unified" repository which requires biserver-ee license
    PentahoLicenseVerifier.setStreamOpener(new TestLicenseStream("biserver-ee=true\npdi-ee=true")); //$NON-NLS-1$
    KettleEnvironment.init();

    repositoryMeta = new PurRepositoryMeta();
    repositoryMeta.setName("JackRabbit");
    repositoryMeta.setDescription("JackRabbit test repository");
    ProfileMeta adminProfile = new ProfileMeta("admin", "Administrator");
    adminProfile.addPermission(Permission.ADMIN);
    userInfo = new UserInfo(EXP_LOGIN, "password", EXP_USERNAME, "Apache Tomcat user", true, adminProfile);

    repository = new PurRepository();
    ((PurRepository) repository).setPur(pur);

    repository.init(repositoryMeta, userInfo);
    // connect is not called as it is only applicable in the "real" deployment
    //repository.connect();

    // call must come after connect; PurRepository sets its own value in PentahoSessionHolder; in the real deployment,
    // PurRepository and PUR would be in two different JVMs
    setUpUser();
    
    List<RepositoryFile> files = pur.getChildren(pur.getFile("/pentaho/acme/public").getId());
    assertTrue("files not deleted: " + files.toString(), files.isEmpty());
  }
  
  protected void setUpUser() {
    StandaloneSession pentahoSession = new StandaloneSession(userInfo.getLogin());
    pentahoSession.setAuthenticated(userInfo.getLogin());
    pentahoSession.setAttribute(IPentahoSession.TENANT_ID_KEY, "acme");
    final GrantedAuthority[] authorities = new GrantedAuthority[2];
    authorities[0] = new GrantedAuthorityImpl("Authenticated");
    authorities[1] = new GrantedAuthorityImpl("acme_Authenticated");
    final String password = "ignored"; //$NON-NLS-1$
    UserDetails userDetails = new User(userInfo.getLogin(), password, true, true, true, true, authorities);
    Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, password, authorities);
    // next line is copy of SecurityHelper.setPrincipal
    pentahoSession.setAttribute("SECURITY_PRINCIPAL", authentication);
    PentahoSessionHolder.setSession(pentahoSession);
    SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_GLOBAL);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    pur.getRepositoryLifecycleManager().newTenant();
    pur.getRepositoryLifecycleManager().newUser();
  }

  @After
  public void tearDown() throws Exception {

    try {
      // clean up after test
      // delete in correct order to prevent ref integrity exceptions
      // jobs
      RepositoryFile jobsParentFolder = pur.getFile("/pentaho/acme/public/jobs");
      if (jobsParentFolder != null) {
        List<RepositoryObject> files = repository.getJobObjects(new StringObjectId(jobsParentFolder.getId()
            .toString()), true);
        for (RepositoryObject file : files) {
          pur.deleteFile(file.getObjectId().getId(), true, null);
        }
      }
      // transformations
      RepositoryFile transParentFolder = pur.getFile("/pentaho/acme/public/transformations");
      if (transParentFolder != null) {
        List<RepositoryObject> files = repository.getTransformationObjects(new StringObjectId(transParentFolder.getId()
            .toString()), true);
        for (RepositoryObject file : files) {
          pur.deleteFile(file.getObjectId().getId(), true, null);
        }
      }
      // partition schemas
      ObjectId[] psIds = repository.getPartitionSchemaIDs(true);

      for (ObjectId psId : psIds) {
        pur.deleteFile(psId.getId(), true, null);
      }
      // cluster schemas
      ObjectId[] csIds = repository.getClusterIDs(true);

      for (ObjectId csId : csIds) {
        pur.deleteFile(csId.getId(), true, null);
      }
      // slave servers
      ObjectId[] ssIds = repository.getSlaveIDs(true);

      for (ObjectId ssId : ssIds) {
        pur.deleteFile(ssId.getId(), true, null);
      }
      // databases
      ObjectId[] dbIds = repository.getDatabaseIDs(true);

      for (ObjectId dbId : dbIds) {
        pur.deleteFile(dbId.getId(), true, null);
      }
      // dirs
      List<RepositoryFile> dirs = pur.getChildren(pur.getFile("/pentaho/acme/public").getId());
      for (RepositoryFile file : dirs) {
        pur.deleteFile(file.getId(), true, null);
      }

    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }

    repository.disconnect();

    //    repositoryMeta = null;
    //    repository = null;
    //    userInfo = null;
    //    FileUtils.deleteDirectory(new File("/tmp/pdi_jcr_repo_unit_test"));
  }

  public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
    pur = (IUnifiedRepository) applicationContext.getBean("unifiedRepository");
    pur.getRepositoryLifecycleManager().startup();
  }

  @Override
  protected RepositoryDirectory loadStartDirectory() throws Exception {
    RepositoryDirectory rootDir = repository.loadRepositoryDirectoryTree();
    RepositoryDirectory startDir = rootDir.findDirectory("pentaho/acme/public");
    assertNotNull(startDir);
    return startDir;
  }
}
