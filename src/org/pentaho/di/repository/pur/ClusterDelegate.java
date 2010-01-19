package org.pentaho.di.repository.pur;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.version.Version;

import org.pentaho.di.cluster.ClusterSchema;
import org.pentaho.di.cluster.SlaveServer;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.repository.RepositoryElementInterface;
import org.pentaho.di.repository.StringObjectId;
import org.pentaho.di.repository.jcr.util.JCRObjectRevision;
import org.pentaho.platform.repository.pcr.data.node.DataNode;
import org.pentaho.platform.repository.pcr.data.node.DataNodeRef;

import com.pentaho.commons.dsc.PentahoDscContent;
import com.pentaho.commons.dsc.PentahoLicenseVerifier;
import com.pentaho.commons.dsc.params.KParam;

public class ClusterDelegate extends AbstractDelegate implements ITransformer {

  private static final String NODE_ROOT = "Slave"; //$NON-NLS-1$

  private static final String PROP_BASE_PORT = "BASE_PORT"; //$NON-NLS-1$

  private static final String PROP_SOCKETS_BUFFER_SIZE = "SOCKETS_BUFFER_SIZE"; //$NON-NLS-1$

  private static final String PROP_SOCKETS_FLUSH_INTERVAL = "SOCKETS_FLUSH_INTERVAL"; //$NON-NLS-1$

  private static final String PROP_SOCKETS_COMPRESSED = "SOCKETS_COMPRESSED"; //$NON-NLS-1$

  private static final String PROP_DYNAMIC = "DYNAMIC"; //$NON-NLS-1$

  private static final String NODE_ATTRIBUTES = "attributes"; //$NON-NLS-1$

  private static final String PROP_NB_SLAVE_SERVERS = "NB_SLAVE_SERVERS"; //$NON-NLS-1$

  // ~ Instance fields =================================================================================================

  private Repository repo;

  // ~ Constructors ====================================================================================================

  public ClusterDelegate(final Repository repo) {
    super();
    this.repo = repo;
  }

  public RepositoryElementInterface dataNodeToElement(DataNode rootNode) throws KettleException {
    ClusterSchema clusterSchema = new ClusterSchema();
    dataNodeToElement(rootNode, clusterSchema);
    return clusterSchema;
  }

  public void dataNodeToElement(DataNode rootNode, RepositoryElementInterface element) throws KettleException {
    PentahoDscContent dscContent = PentahoLicenseVerifier.verify(new KParam());
    ClusterSchema clusterSchema = (ClusterSchema) element;
    if (dscContent.getExtra() != null) {
      clusterSchema.setName(rootNode.getProperty(PROP_NAME).getString());
      clusterSchema.setDescription(rootNode.getProperty(PROP_DESCRIPTION).getString());
    }
    // The metadata...
    clusterSchema.setBasePort(rootNode.getProperty(PROP_BASE_PORT).getString());
    clusterSchema.setSocketsBufferSize(rootNode.getProperty(PROP_SOCKETS_BUFFER_SIZE).getString());
    clusterSchema.setSocketsFlushInterval(rootNode.getProperty(PROP_SOCKETS_FLUSH_INTERVAL).getString());
    clusterSchema.setSocketsCompressed(rootNode.getProperty(PROP_SOCKETS_COMPRESSED).getBoolean());
    clusterSchema.setDynamic(rootNode.getProperty(PROP_DYNAMIC).getBoolean());
    DataNode attrNode = rootNode.getNode(NODE_ATTRIBUTES);
    // The slaves...
    long nrSlaves = attrNode.getProperty(PROP_NB_SLAVE_SERVERS).getLong();
    for (int i = 0; i < nrSlaves; i++) {
      DataNodeRef slaveNodeRef = attrNode.getProperty(String.valueOf(i)).getRef();
      clusterSchema.getSlaveServers().add(findSlaveServer(new StringObjectId(slaveNodeRef.toString())));
    }
  }

  public DataNode elementToDataNode(RepositoryElementInterface element) throws KettleException {
    ClusterSchema clusterSchema = (ClusterSchema) element;
    DataNode rootNode = new DataNode(NODE_ROOT);
    PentahoDscContent dscContent = PentahoLicenseVerifier.verify(new KParam());

    // Check for naming collision
    ObjectId clusterId = repo.getClusterID(clusterSchema.getName());
    if (clusterId != null && !clusterSchema.getObjectId().equals(clusterId)) {
      // We have a naming collision, abort the save
      throw new KettleException("Failed to save object to repository. Object [" + clusterSchema.getName()
          + "] already exists.");
    }
    // save the properties...
    rootNode.setProperty(PROP_NAME, clusterSchema.getName());
    rootNode.setProperty(PROP_DESCRIPTION, clusterSchema.getDescription());
    rootNode.setProperty(PROP_BASE_PORT, clusterSchema.getBasePort());
    rootNode.setProperty(PROP_SOCKETS_BUFFER_SIZE, clusterSchema.getSocketsBufferSize());
    rootNode.setProperty(PROP_SOCKETS_FLUSH_INTERVAL, clusterSchema.getSocketsFlushInterval());
    rootNode.setProperty(PROP_SOCKETS_COMPRESSED, clusterSchema.isSocketsCompressed());
    rootNode.setProperty(PROP_DYNAMIC, clusterSchema.isDynamic());

    DataNode attrNode = rootNode.addNode(NODE_ATTRIBUTES);

    if (dscContent.getSubject() != null) {
      // Also save the used slave server references.

      attrNode.setProperty(PROP_NB_SLAVE_SERVERS, clusterSchema.getSlaveServers().size());
      for (int i = 0; i < clusterSchema.getSlaveServers().size(); i++) {
        SlaveServer slaveServer = clusterSchema.getSlaveServers().get(i);
        DataNodeRef slaveNodeRef = new DataNodeRef(slaveServer.getObjectId().getId());
        // Save the slave server by reference, this way it becomes impossible to delete the slave by accident when still in use.
        attrNode.setProperty(String.valueOf(i), slaveNodeRef);
      }
    }
    return rootNode;
  }

  private SlaveServer findSlaveServer(ObjectId slaveServerId) {
    List<SlaveServer> slaveServers;
    try {
      slaveServers = repo.getSlaveServers();
      for (SlaveServer slaveServer : slaveServers) {
        if (slaveServer.getObjectId().equals(slaveServerId)) {
          return slaveServer;
        }
      }
    } catch (KettleException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }
}
