/*!
 * PENTAHO CORPORATION PROPRIETARY AND CONFIDENTIAL
 *
 * Copyright 2002 - 2015 Pentaho Corporation (Pentaho). All rights reserved.
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

package com.pentaho.di.trans.dataservice.optimization.cache;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.pentaho.di.trans.dataservice.DataServiceExecutor;
import com.pentaho.di.trans.dataservice.DataServiceMeta;
import com.pentaho.di.trans.dataservice.optimization.OptimizationImpactInfo;
import com.pentaho.di.trans.dataservice.optimization.PushDownOptimizationMeta;
import com.pentaho.di.trans.dataservice.optimization.PushDownType;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.metastore.persist.MetaStoreAttribute;

import javax.cache.Cache;
import java.text.MessageFormat;
import java.util.concurrent.CancellationException;

/**
 * @author nhudak
 */
public class ServiceCache implements PushDownType {
  public static final String NAME = "Service Cache";
  private final ServiceCacheFactory factory;
  public static final String SERVICE_CACHE_TEMPLATE_NAME = "template_name";

  @MetaStoreAttribute( key = SERVICE_CACHE_TEMPLATE_NAME ) private String templateName = "";

  public ServiceCache( ServiceCacheFactory factory ) {
    this.factory = factory;
  }

  @Override public String getTypeName() {
    return NAME;
  }

  @Override public void init( TransMeta transMeta, DataServiceMeta dataService, PushDownOptimizationMeta optMeta ) {
    optMeta.setStepName( dataService.getStepname() );
  }

  @Override public boolean activate( final DataServiceExecutor executor, StepInterface stepInterface ) {
    final LogChannelInterface logChannel = executor.getGenTrans().getLogChannel();
    final CachedServiceLoader.CacheKey cacheKey = CachedServiceLoader.createCacheKey( executor );
    final Cache<CachedServiceLoader.CacheKey, CachedServiceLoader> cache = factory.getCache( this );
    CachedServiceLoader cachedServiceLoader = cache.get( cacheKey );
    if ( cachedServiceLoader == null ) {
      Futures.addCallback( CachedServiceLoader.observe( stepInterface ), new FutureCallback<CachedServiceLoader>() {
            @Override public void onSuccess( CachedServiceLoader result ) {
              if ( cache.putIfAbsent( cacheKey, result ) ) {
                logChannel.logBasic( "Service Transformation results cached" );
              }
            }

            @Override public void onFailure( Throwable t ) {
              if ( t instanceof CancellationException ) {
                logChannel.logBasic( "Service was stopped before results could be cached" );
              } else {
                logChannel.logError( "Cache failed to observe service transformation", t );
              }
            }
          } );
      return false;
    } else {
      Futures.addCallback( cachedServiceLoader.replay( executor ), new FutureCallback<Integer>() {
            @Override public void onSuccess( Integer rowCount ) {
              logChannel.logBasic( "Service Transformation successfully replayed " + rowCount + " rows from cache" );
            }

            @Override public void onFailure( Throwable t ) {
              logChannel.logError( "Cache failed to replay service transformation", t );
            }
          } );
      return true;
    }
  }

  @Override public OptimizationImpactInfo preview( DataServiceExecutor executor, StepInterface stepInterface ) {
    OptimizationImpactInfo info = new OptimizationImpactInfo( stepInterface.getStepname() );
    final CachedServiceLoader.CacheKey cacheKey = CachedServiceLoader.createCacheKey( executor );
    final Cache<CachedServiceLoader.CacheKey, CachedServiceLoader> cache = factory.getCache( this );
    CachedServiceLoader cachedServiceLoader = cache.get( cacheKey );
    if ( cachedServiceLoader == null ) {
      info.setModified( false );
      info.setQueryBeforeOptimization( "Service results are not available. Execute this query to cache results." );
    } else {
      info.setModified( true );
      info.setQueryBeforeOptimization( MessageFormat.format( "Service results for {0} are available.", cacheKey ) );
      info.setQueryAfterOptimization(
          MessageFormat.format( "{0} rows can be read from cache.", cachedServiceLoader.getRowMetaAndData().size() ) );
    }
    return info;
  }

  public String getTemplateName() {
    return templateName;
  }

  public void setTemplateName( String templateName ) {
    this.templateName = templateName;
  }
}
