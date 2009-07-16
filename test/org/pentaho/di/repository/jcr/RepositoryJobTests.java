package org.pentaho.di.repository.jcr;

import java.util.List;

import junit.framework.TestCase;

import org.pentaho.di.core.KettleEnvironment;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entries.tableexists.JobEntryTableExists;
import org.pentaho.di.job.entry.JobEntryCopy;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.ObjectVersion;
import org.pentaho.di.repository.ProfileMeta;
import org.pentaho.di.repository.RepositoryDirectory;
import org.pentaho.di.repository.RepositoryLock;
import org.pentaho.di.repository.RepositoryObjectType;
import org.pentaho.di.repository.UserInfo;
import org.pentaho.di.repository.ProfileMeta.Permission;

public class RepositoryJobTests extends TestCase {
	
	private JCRRepositoryMeta repositoryMeta;
	private JCRRepository repository;
	private UserInfo userInfo;
	private JobMeta	jobMeta;
	private RepositoryDirectory	directoryTree;
	
	private static final String TEST_DIRECTORY_PATH = "/test-jobs/";
	private static final String TEST_DIRECTORY_PATH_OK = "/test-jobs";
	
	private static final String TEST_JOB = "./testfiles/TestJob.kjb";
	private static final String	JOB_NAME = "Test Job";
	
	private static final String DESCRIPTION_ONE   = "Description 1";
	private static final String DESCRIPTION_TWO   = "Description 2";
	private static final String DESCRIPTION_THREE = "Description 3";
	private static final String DESCRIPTION_FOUR  = "Description 4";
	
	private static final String VERSION_COMMENT_ONE = "This is a first test version comment";
	private static final String VERSION_COMMENT_TWO = "This is a second test version comment";
	private static final String VERSION_COMMENT_THREE = "This is a third test version comment";
	private static final String VERSION_COMMENT_FOUR = "This is a fourth test version comment";
	
	protected void setUp() throws Exception {
		super.setUp();
		
		KettleEnvironment.init();
	
		repositoryMeta = new JCRRepositoryMeta();
		repositoryMeta.setName("JackRabbit");
		repositoryMeta.setDescription("JackRabbit test repository");
		repositoryMeta.setRepositoryLocation(new JCRRepositoryLocation("http://localhost:8181/jackrabbit/rmi"));
		
		ProfileMeta adminProfile = new ProfileMeta("admin", "Administrator");
		adminProfile.addPermission(Permission.ADMIN);
		
		userInfo = new UserInfo("tomcat", "tomcat", "Apache Tomcat", "Apache Tomcat user", true, adminProfile);
		
		repository = new JCRRepository();
		repository.init(repositoryMeta, userInfo);
		
		repository.connect();
		
		directoryTree = repository.loadRepositoryDirectoryTree();
		
		if (jobMeta==null) {
			jobMeta = new JobMeta(TEST_JOB, repository);
			RepositoryDirectory directory = directoryTree.findDirectory(TEST_DIRECTORY_PATH);
			if (directory!=null) {
				jobMeta.setRepositoryDirectory(directory);
			}
			jobMeta.setDescription(DESCRIPTION_ONE);
		}
	}
	
	protected void tearDown() throws Exception {
		repository.disconnect();
		super.tearDown();
	}
	
	public void test01_createDirectory() throws Exception {
		RepositoryDirectory tree = repository.loadRepositoryDirectoryTree();
		RepositoryDirectory fooDirectory = tree.findDirectory(TEST_DIRECTORY_PATH);
		
		if (fooDirectory==null) {
			fooDirectory = repository.createRepositoryDirectory(tree, TEST_DIRECTORY_PATH);
		}
		
		assertNotNull(fooDirectory);
		assertNotNull(fooDirectory.getObjectId());
		assertEquals(fooDirectory.getPath(), TEST_DIRECTORY_PATH_OK);
	}
	
	public void test10_saveJob() throws Exception {
		
		// Save the job first...
		//
		repository.save(jobMeta, VERSION_COMMENT_ONE, null);
		
		assertNotNull("Object ID needs to be set", jobMeta.getObjectId());
		
		ObjectVersion version = jobMeta.getObjectVersion();
		
		assertNotNull("Object version needs to be set", version);
		
		assertEquals(VERSION_COMMENT_ONE, version.getComment());
		
		assertEquals("1.0", version.getName());
	}
	
	public void test15_loadJob() throws Exception {

		RepositoryDirectory fooDirectory = directoryTree.findDirectory(TEST_DIRECTORY_PATH);

		JobMeta jobMeta = repository.loadJob(JOB_NAME, fooDirectory, null, null);  // Load the last version

		assertNotNull(jobMeta);
		
		// Verify general content
		//
		ObjectVersion version = jobMeta.getObjectVersion();
		assertNotNull("Object version needs to be set", version);
		assertEquals(VERSION_COMMENT_ONE, version.getComment());
		assertEquals("1.0", version.getName());
		assertEquals("tomcat", version.getLogin());
		
		// Verify specific content
		//
		assertEquals(6, jobMeta.nrJobEntries());
		assertEquals(6, jobMeta.nrJobHops());

		JobEntryCopy entry = jobMeta.findJobEntry("Table exists 1");
		JobEntryTableExists meta = (JobEntryTableExists) entry.getEntry();
		DatabaseMeta databaseMeta = meta.getDatabase();
		assertNotNull(databaseMeta);
	}
	
	public void test20_createJobVersions() throws Exception {
	
		// Change the description of the job & save it again..
		//
		jobMeta.setDescription(DESCRIPTION_TWO);
		repository.save(jobMeta, VERSION_COMMENT_TWO, null);
		
		String id = jobMeta.getObjectId().getId();
		assertEquals("1.1", jobMeta.getObjectVersion().getName());
	
		// Change the description of the job & save it again..
		//
		jobMeta.setDescription(DESCRIPTION_THREE);
		JobEntryCopy populate = jobMeta.findJobEntry("Populate");
		assertNotNull(populate);
		populate.setName("NEW_NAME");
		repository.save(jobMeta, VERSION_COMMENT_THREE, null);

		assertEquals("1.2", jobMeta.getObjectVersion().getName());
		assertEquals(id, jobMeta.getObjectId().getId());

		// Change the description of the job & save it again..
		//
		jobMeta.setDescription(DESCRIPTION_FOUR);
		jobMeta.removeJobHop(jobMeta.nrJobHops()-1);
		jobMeta.removeJobEntry(jobMeta.nrJobEntries()-1);
		repository.save(jobMeta, VERSION_COMMENT_FOUR, null);
		assertEquals(VERSION_COMMENT_FOUR, jobMeta.getObjectVersion().getComment());
		assertEquals("1.3", jobMeta.getObjectVersion().getName());
		assertEquals(id, jobMeta.getObjectId().getId());
	}
	
	public void test30_getJobVersionHistory() throws Exception {
		
		List<ObjectVersion> versions = repository.getVersions(jobMeta);
		assertEquals(versions.size(), 4);
		
		ObjectVersion v1 = versions.get(0);
		assertEquals("1.0", v1.getName());
		assertEquals(VERSION_COMMENT_ONE, v1.getComment());
		
		ObjectVersion v2 = versions.get(1);
		assertEquals("1.1", v2.getName());
		assertEquals(VERSION_COMMENT_TWO, v2.getComment());

		ObjectVersion v3 = versions.get(2);
		assertEquals("1.2", v3.getName());
		assertEquals(VERSION_COMMENT_THREE, v3.getComment());

		ObjectVersion v4 = versions.get(3);
		assertEquals("1.3", v4.getName());
		assertEquals(VERSION_COMMENT_FOUR, v4.getComment());
	}
	
	public void test40_loadJobVersions() throws Exception {

		RepositoryDirectory fooDirectory = directoryTree.findDirectory(TEST_DIRECTORY_PATH);

		JobMeta jobMeta = repository.loadJob(JOB_NAME, fooDirectory, null, "1.1");  // Load the second version
		ObjectVersion version = jobMeta.getObjectVersion();
		assertEquals("1.1", version.getName());
		assertEquals(VERSION_COMMENT_TWO, version.getComment());
		assertEquals(DESCRIPTION_TWO, jobMeta.getDescription());
		assertEquals(6, jobMeta.nrJobEntries());
		assertEquals(6, jobMeta.nrJobHops());
		
		jobMeta = repository.loadJob(JOB_NAME, fooDirectory, null, "1.0");  // Load the first version
		version = jobMeta.getObjectVersion();
		assertEquals("1.0", version.getName());
		assertEquals(VERSION_COMMENT_ONE, version.getComment());
		assertEquals(DESCRIPTION_ONE, jobMeta.getDescription());
		assertEquals(6, jobMeta.nrJobEntries());
		assertEquals(6, jobMeta.nrJobHops());

		jobMeta = repository.loadJob(JOB_NAME, fooDirectory, null, "1.3");  // Load the fourth version
		version = jobMeta.getObjectVersion();
		assertEquals("1.3", version.getName());
		assertEquals(VERSION_COMMENT_FOUR, version.getComment());
		assertEquals(DESCRIPTION_FOUR, jobMeta.getDescription());
		assertEquals(5, jobMeta.nrJobEntries());
		assertEquals(5, jobMeta.nrJobHops());

		jobMeta = repository.loadJob(JOB_NAME, fooDirectory, null, "1.2");  // Load the third version
		version = jobMeta.getObjectVersion();
		assertEquals("1.2", version.getName());
		assertEquals(VERSION_COMMENT_THREE, version.getComment());
		assertEquals(DESCRIPTION_THREE, jobMeta.getDescription());
		assertEquals(6, jobMeta.nrJobEntries());
		assertEquals(6, jobMeta.nrJobHops());
		assertNotNull(jobMeta.findJobEntry("NEW_NAME"));
	}

	public void test50_loadLastJobVersion() throws Exception {
		
		RepositoryDirectory fooDirectory = directoryTree.findDirectory(TEST_DIRECTORY_PATH);
		jobMeta = repository.loadJob(JOB_NAME, fooDirectory, null, null);  // Load the last version
		
		assertNotNull(jobMeta);
		
		ObjectVersion version = jobMeta.getObjectVersion();
		
		assertEquals(VERSION_COMMENT_FOUR, version.getComment());
		assertEquals("1.3", version.getName());
	}
	
	public void test60_lockJob() throws Exception {
		RepositoryDirectory fooDirectory = directoryTree.findDirectory(TEST_DIRECTORY_PATH);
		jobMeta = repository.loadJob(JOB_NAME, fooDirectory, null, null);  // Load the last version
		repository.lockJob(jobMeta.getObjectId(), "Locked by unit test");
		
		RepositoryLock lock = repository.getJobLock(jobMeta.getObjectId());
		
		assertNotNull(lock);
	}
	
	public void test65_unlockJob() throws Exception {
		RepositoryDirectory fooDirectory = directoryTree.findDirectory(TEST_DIRECTORY_PATH);
		jobMeta = repository.loadJob(JOB_NAME, fooDirectory, null, null);  // Load the last version
		repository.unlockJob(jobMeta.getObjectId());
	}

	public void test70_existsJob() throws Exception {
		RepositoryDirectory fooDirectory = directoryTree.findDirectory(TEST_DIRECTORY_PATH);
		jobMeta = repository.loadJob(JOB_NAME, fooDirectory, null, null);  // Load the last version
		
		boolean exists = repository.exists(JOB_NAME, fooDirectory, RepositoryObjectType.JOB);
		
		assertEquals("Job exists in the repository, test didn't find it", true, exists);
	}

	public void test75_deleteJob() throws Exception {
		RepositoryDirectory fooDirectory = directoryTree.findDirectory(TEST_DIRECTORY_PATH);
		jobMeta = repository.loadJob(JOB_NAME, fooDirectory, null, null);  // Load the last version
		
		repository.deleteJob(jobMeta.getObjectId());

		boolean exists = repository.exists(JOB_NAME, fooDirectory, RepositoryObjectType.JOB);
		assertEquals("Job was not deleted", false, exists);
	}

	public void test77_restoreJob() throws Exception {
		RepositoryDirectory fooDirectory = directoryTree.findDirectory(TEST_DIRECTORY_PATH);
		jobMeta = repository.loadJob(JOB_NAME, fooDirectory, null, null);  // Load the last version
		
		repository.undeleteObject(jobMeta); // un-delete

		boolean exists = repository.exists(JOB_NAME, fooDirectory, RepositoryObjectType.JOB);
		assertEquals("Job was not restored", true, exists);
		
		repository.undeleteObject(jobMeta); // restore the second version...
		
		jobMeta = repository.loadJob(JOB_NAME, fooDirectory, null, null);  // Load the last version
		
		ObjectVersion version = jobMeta.getObjectVersion();
		assertEquals("1.5", version.getName());
	}

	public void test77_renameDatabase() throws Exception {
		RepositoryDirectory fooDirectory = directoryTree.findDirectory(TEST_DIRECTORY_PATH);
		jobMeta = repository.loadJob(JOB_NAME, fooDirectory, null, null);  // Load the last version
		
		// Rename connection "H2 test", used in the job...
		//
		ObjectId id = repository.getDatabaseID("H2 test");
		assertNotNull(id);
		
		repository.renameDatabase(id, "new H2");

		jobMeta = repository.loadJob(JOB_NAME, fooDirectory, null, null);  // Load the last version
		JobEntryCopy copy = jobMeta.findJobEntry("Table exists 1");
		JobEntryTableExists meta = (JobEntryTableExists) copy.getEntry();
		
		DatabaseMeta databaseMeta = meta.getDatabase();
		assertNotNull(databaseMeta);
		assertEquals("new H2", databaseMeta.getName());
		
		DatabaseMeta logDatabase = jobMeta.getLogConnection();
		assertNotNull(logDatabase);
		assertEquals("new H2", logDatabase.getName());
	}
	
	public void test99_deleteDirectory() throws Exception {
		RepositoryDirectory fooDirectory = directoryTree.findDirectory(TEST_DIRECTORY_PATH);
		assertNotNull(fooDirectory);
		assertNotNull(fooDirectory.getObjectId());
		
		repository.deleteRepositoryDirectory(fooDirectory);
		directoryTree = repository.loadRepositoryDirectoryTree();
		fooDirectory = directoryTree.findDirectory(TEST_DIRECTORY_PATH);
		assertNull(fooDirectory);
	}
}
