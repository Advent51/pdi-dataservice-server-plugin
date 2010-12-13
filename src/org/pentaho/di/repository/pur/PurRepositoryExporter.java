package org.pentaho.di.repository.pur;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.ProgressMonitorListener;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.repository.IRepositoryExporter;
import org.pentaho.di.repository.RepositoryDirectoryInterface;
import org.pentaho.di.repository.RepositoryObjectType;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.repository2.unified.RepositoryFileTree;

import com.pentaho.commons.dsc.PentahoDscContent;
import com.pentaho.commons.dsc.PentahoLicenseVerifier;
import com.pentaho.commons.dsc.params.KParam;

/**
 * Optimized exporter designed to keep the server calls to a minimum.
 * 
 * @author jganoff
 */
public class PurRepositoryExporter implements IRepositoryExporter {
  /**
   * Amount of repository files and content to load from the repository at once.
   */
  private static final int DEFAULT_BATCH_SIZE = 50;
  
  private static final String REPOSITORY_BATCH_SIZE_PROPERTY = "KettleRepositoryExportBatchSize"; //$NON-NLS-1$

  private int batchSize;

  private PurRepository repository;

  private LogChannelInterface log;

  public PurRepositoryExporter(PurRepository repository) {
    this.repository = repository;
    this.log = repository.getLog();
  }

  public synchronized void exportAllObjects(ProgressMonitorListener monitor, String xmlFilename,
      RepositoryDirectoryInterface root, String exportType) throws KettleException {
    initBatchSize();
    // Perform a single license check for entire export process
    PentahoDscContent dscContent = PentahoLicenseVerifier.verify(new KParam(PurRepositoryMeta.BUNDLE_REF_NAME));

    OutputStream os = null;
    OutputStreamWriter writer = null;
    try {
      if (dscContent != null) {
        os = new BufferedOutputStream(KettleVFS.getOutputStream(xmlFilename, false));
        writer = new OutputStreamWriter(os, Const.XML_ENCODING);
      }
      if (monitor != null) {
        monitor.beginTask("Exporting the repository to XML...", 3); //$NON-NLS-1$
      }
      root = root == null ? repository.findDirectory("/") : root; //$NON-NLS-1$
      String path = root.getPath();
      RepositoryFileTree repoTree = repository.loadRepositoryFileTree(path);

      if (dscContent != null) {
        writer.write(XMLHandler.getXMLHeader());
        writer.write("<repository>" + Const.CR + Const.CR); //$NON-NLS-1$
        if (monitor != null) {
          monitor.worked(1);
        }
      }

      if (exportType.equals("all") || exportType.equals("trans")) { //$NON-NLS-1$ //$NON-NLS-2$
        // Dump the transformations...
        writer.write("<transformations>" + Const.CR); //$NON-NLS-1$
        //        exportAllTransformations(monitor, repoTree, repoDirTree, writer);
        export(monitor, repoTree, writer, new TransformationBatchExporter());
        writer.write("</transformations>" + Const.CR); //$NON-NLS-1$
      }

      if (exportType.equals("all") || exportType.equals("jobs")) { //$NON-NLS-1$ //$NON-NLS-2$
        // Now dump the jobs...
        writer.write("<jobs>" + Const.CR); //$NON-NLS-1$
        export(monitor, repoTree, writer, new JobBatchExporter());
        writer.write("</jobs>" + Const.CR); //$NON-NLS-1$
      }

      writer.write("</repository>" + Const.CR + Const.CR); //$NON-NLS-1$

      if (monitor != null) {
        monitor.worked(1);
        monitor.subTask("Saving XML to file [" + xmlFilename + "]"); //$NON-NLS-1$ //$NON-NLS-2$
        monitor.worked(1);
      }
    } catch (IOException e) {
      System.out.println("Couldn't create file [" + xmlFilename + "]"); //$NON-NLS-1$ //$NON-NLS-2$
    } finally {
      try {
        if (writer != null) {
          writer.close();
        }
        if (os != null) {
          os.close();
        }
      } catch (Exception e) {
        System.out.println("Exception closing XML file writer to [" + xmlFilename + "]"); //$NON-NLS-1$ //$NON-NLS-2$
      }
    }

    if (monitor != null) {
      monitor.done();
    }
  }

  /**
   * Set the batch size for fetching objects from the repository.
   */
  private void initBatchSize() {
    batchSize = DEFAULT_BATCH_SIZE;
    String batchProp = Const.getEnvironmentVariable(REPOSITORY_BATCH_SIZE_PROPERTY, null);
    boolean err = false;
    if (!Const.isEmpty(batchProp)) {
      try {
        batchSize = Integer.parseInt(batchProp);
        if (batchSize < 1) {
          err = true;
        }
      } catch (Exception ex) {
        err = true;
      }
    }
    if (err) {
      batchSize = DEFAULT_BATCH_SIZE;
      log.logError("Invalid {0} [{1}].  Reverting to [{2}].", REPOSITORY_BATCH_SIZE_PROPERTY, batchProp, batchSize); //$NON-NLS-1$
    }
    log.logDetailed("Using batch size of {0}", batchSize); //$NON-NLS-1$
  }

  /**
   * Controls what objects are exported in bulk from the repository and how to export them.
   */
  private interface RepositoryFileBatchExporter {
    /**
     * Friendly name for the types this exporter exports.  (Used for logging)
     */
    String getFriendlyTypeName();

    /**
     * Can this file be exported by this batch exporter?
     * 
     * @param file File in question 
     * @return true if this batch exporter can handle this type
     * 
     * @throws KettleException error determining object type of {@code file}
     */
    boolean canExport(final RepositoryFile file) throws KettleException;

    /**
     * Export the files.
     * 
     * @param monitor Progress should be provided using this monitor.
     * @param files Repository files to export
     * @param writer Writer to serialize files to
     * 
     * @throws KettleException error exporting files
     */
    void export(final ProgressMonitorListener monitor, final List<RepositoryFile> files, final OutputStreamWriter writer)
        throws KettleException;
  }

  /**
   * Transformation exporter
   */
  private class TransformationBatchExporter implements RepositoryFileBatchExporter {

    @Override
    public String getFriendlyTypeName() {
      return "transformations"; //$NON-NLS-1$
    }

    @Override
    public boolean canExport(RepositoryFile file) throws KettleException {
      return RepositoryObjectType.TRANSFORMATION.equals(repository.getObjectType(file.getName()));
    }

    @Override
    public void export(ProgressMonitorListener monitor, List<RepositoryFile> files, OutputStreamWriter writer)
        throws KettleException {
      List<TransMeta> transformations = repository.loadTransformations(monitor, log, files, true);
      Iterator<TransMeta> transMetasIter = transformations.iterator();
      Iterator<RepositoryFile> filesIter = files.iterator();
      while ((monitor == null || !monitor.isCanceled()) && transMetasIter.hasNext()) {
        TransMeta trans = transMetasIter.next();
        RepositoryFile file = filesIter.next();
        try {
          writer.write(trans.getXML() + Const.CR);
        } catch (IOException ex) {
          log.logError(
              "An error occured while saving transformation [" + trans.getName() + "] from [" + file.getPath() + "]", ex); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
      }
    }
  }

  private class JobBatchExporter implements RepositoryFileBatchExporter {
    @Override
    public String getFriendlyTypeName() {
      return "jobs"; //$NON-NLS-1$
    }

    @Override
    public boolean canExport(RepositoryFile file) throws KettleException {
      return RepositoryObjectType.JOB.equals(repository.getObjectType(file.getName()));
    }

    @Override
    public void export(ProgressMonitorListener monitor, List<RepositoryFile> files, OutputStreamWriter writer)
        throws KettleException {
      List<JobMeta> jobs = repository.loadJobs(monitor, log, files, true);
      Iterator<JobMeta> jobsMeta = jobs.iterator();
      Iterator<RepositoryFile> filesIter = files.iterator();
      while ((monitor == null || !monitor.isCanceled()) && jobsMeta.hasNext()) {
        JobMeta trans = jobsMeta.next();
        RepositoryFile file = filesIter.next();
        try {
          writer.write(trans.getXML() + Const.CR);
        } catch (IOException ex) {
          log.logError("An error occured while saving job [" + trans.getName() + "] from [" + file.getPath() + "]", ex); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
      }
    }
  }

  /**
   * Export objects by directory, breadth first.
   * 
   * @param monitor Feedback handler
   * @param root Root of repository to export from
   * @param writer Output stream the exporter uses to serialize repository objects
   * @param exporter Processes groups of nodes per directory
   * 
   * @throws KettleException error performing export
   */
  private void export(final ProgressMonitorListener monitor, final RepositoryFileTree root, final OutputStreamWriter writer,
      final RepositoryFileBatchExporter exporter) throws KettleException {
    List<RepositoryFileTree> subdirectories = new ArrayList<RepositoryFileTree>();
    // Assume the repository objects are loaded.  If they're null then there are no repository objects in this directory
    if (root.getChildren() != null && !root.getChildren().isEmpty()) {
      Iterator<RepositoryFileTree> repObjIter = root.getChildren().iterator();
      List<RepositoryFile> files = new ArrayList<RepositoryFile>();
      // Walk the tree collecting subdirectories and objects to export
      while ((monitor == null || !monitor.isCanceled()) && repObjIter.hasNext()) {
        RepositoryFileTree repObj = repObjIter.next();
        if (repObj.getFile().isFolder()) {
          // This is a directory, cache it so we can export it after the current folder's objects
          subdirectories.add(repObj);
          continue;
        } else if (!exporter.canExport(repObj.getFile())) {
          // Cannot export this type
          continue;
        }
        files.add(repObj.getFile());
      }
      if (!files.isEmpty()) {
        log.logBasic(
            "Exporting {0} {1} from directory [{2}]", files.size(), exporter.getFriendlyTypeName(), root.getFile().getPath()); //$NON-NLS-1$
        // Only fetch batchSize transformations at a time
        for (int i = 0; (monitor == null || !monitor.isCanceled()) && i < files.size(); i += batchSize) {
          int start = i;
          int end = Math.min(i + batchSize, files.size());
          List<RepositoryFile> group = files.subList(start, end);
          if (monitor != null) {
            monitor
                .subTask("Loading " + group.size() + " " + exporter.getFriendlyTypeName() + " from " + root.getFile().getPath()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
          }
          try {
            exporter.export(monitor, group, writer);
          } catch (KettleException ex) {
            // TODO i18n
            log.logError(
                "Error exporting " + exporter.getFriendlyTypeName() + " from directory [" + root.getFile().getPath() + "]", ex); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
          }
        }
      }
      // Export subdirectories
      Iterator<RepositoryFileTree> subdirIter = subdirectories.iterator();
      while ((monitor == null || !monitor.isCanceled()) && subdirIter.hasNext()) {
        export(monitor, subdirIter.next(), writer, exporter);
      }
    }
  }
}
