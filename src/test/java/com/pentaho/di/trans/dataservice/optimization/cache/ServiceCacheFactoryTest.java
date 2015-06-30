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

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.pentaho.caching.api.PentahoCacheManager;
import org.pentaho.caching.api.PentahoCacheTemplateConfiguration;

import javax.cache.Cache;

import java.util.concurrent.ExecutorService;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author nhudak
 */
@RunWith( MockitoJUnitRunner.class )
public class ServiceCacheFactoryTest {

  public static final String TEMPLATE_NAME = "template";
  @Mock PentahoCacheManager cacheManager;
  @Mock ExecutorService executorService;
  @InjectMocks ServiceCacheFactory serviceCacheFactory;
  @Mock Cache<CachedService.CacheKey, CachedService> cache;
  @Mock PentahoCacheTemplateConfiguration template;

  @Test
  public void testGetCache() throws Exception {
    when( cacheManager.getCache( ServiceCacheFactory.CACHE_NAME,
      CachedService.CacheKey.class, CachedService.class ) ).thenReturn( cache );
    ServiceCache serviceCache = serviceCacheFactory.createPushDown();

    assertThat( serviceCacheFactory.getCache( serviceCache ), is( cache ) );
  }

  @Test
  public void testCreateCache() throws Exception {
    when( cacheManager.getTemplates() ).thenReturn( ImmutableMap.of( TEMPLATE_NAME, template ) );
    when( template.createCache(
      ServiceCacheFactory.CACHE_NAME, CachedService.CacheKey.class, CachedService.class ) )
      .thenReturn( cache );

    ServiceCache serviceCache = serviceCacheFactory.createPushDown();

    assertThat( serviceCacheFactory.getTemplates(), contains( TEMPLATE_NAME ) );
    serviceCache.setTemplateName( TEMPLATE_NAME );
    assertThat( serviceCacheFactory.getCache( serviceCache ), is( cache ) );
  }
}
