/*
 * Copyright 2016 Victor Albertos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.reactivecache2;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleTransformer;
import io.rx_cache2.ConfigProvider;
import io.rx_cache2.EvictDynamicKey;
import io.rx_cache2.EvictDynamicKeyGroup;
import io.rx_cache2.Reply;
import io.rx_cache2.internal.ProcessorProviders;
import java.util.concurrent.TimeUnit;

/**
 * Entry point to manage cache CRUD operations with groups.
 *
 * @param <T> The type of the data to persist.
 */
public class ProviderGroup<T> {
  private final ProviderBuilder<T> builder;
  protected final ExceptionAdapter exceptionAdapter;

  ProviderGroup(ProviderBuilder<T> builder) {
    this.builder = builder;
    this.exceptionAdapter = new ExceptionAdapter();
  }

  /**
   * Evict all the cached data for this provider.
   */
  public final Completable evict() {
    return Completable.defer(() ->
        Completable.fromObservable(builder.processorProviders
            .process(getConfigProvider(Observable.error(new RuntimeException()), "",
                new EvictDynamicKey(true), false, false)))
            .onErrorResumeNext(exceptionAdapter::completeOnRxCacheLoaderError)
    );
  }

  /**
   * Evict the cached data by group.
   */
  public final Completable evict(final Object group) {
    return Completable.defer(() ->
        Completable.fromObservable(builder.processorProviders
            .process(getConfigProvider(Observable.error(new RuntimeException()), group.toString(),
                new EvictDynamicKeyGroup(true), false, false)))
            .onErrorResumeNext(exceptionAdapter::completeOnRxCacheLoaderError)
    );
  }

  /**
   * Replace the cached data by group based on the element emitted from the loader.
   */
  public final SingleTransformer<T, T> replace(final Object group) {
    return loader ->
        loader.flatMap(data -> Single.fromObservable(builder.processorProviders
            .process(getConfigProvider(Observable.just(data), group.toString(),
                new EvictDynamicKeyGroup(true), false, null))));
  }

  /**
   * Read from cache by group and throw if no data is available.
   */
  public final Single<T> read(final Object group) {
    return Single.defer(() ->
        Single.fromObservable(builder.processorProviders
            .<T>process(getConfigProvider(exceptionAdapter.placeholderLoader(), group.toString(),
                new EvictDynamicKeyGroup(false), false, null))))
        .onErrorResumeNext(exceptionAdapter::stripPlaceholderLoaderException);
  }

  /**
   * Read from cache by group but if there is not data available then read from the loader and cache
   * its element.
   */
  public final SingleTransformer<T, T> readWithLoader(final Object group) {
    return loader -> Single.fromObservable(builder.processorProviders
        .process(getConfigProvider(loader.toObservable(), group.toString(),
            new EvictDynamicKeyGroup(false), false, null)));
  }

  /**
   * Same as {@link ProviderGroup#replace(Object)} but wrap the data in a Reply object for debug
   * purposes.
   */
  public final SingleTransformer<T, Reply<T>> replaceAsReply(final Object group) {
    return loader ->
        loader.flatMap(data -> Single.fromObservable(builder.processorProviders
            .process(getConfigProvider(Observable.just(data), group.toString(),
                new EvictDynamicKeyGroup(true), true, null))));
  }

  /**
   * Same as {@link ProviderGroup#readWithLoader(Object)} but wrap the data in a Reply object for
   * debug purposes.
   */
  public final SingleTransformer<T, Reply<T>> readWithLoaderAsReply(final Object group) {
    return loader -> Single.fromObservable(builder.processorProviders
        .process(getConfigProvider(loader.toObservable(), group.toString(), new EvictDynamicKeyGroup(false),
            true, null)));
  }

  private ConfigProvider getConfigProvider(Observable<T> loader, String group,
      EvictDynamicKey evict, boolean detailResponse, Boolean useExpiredDataIfNotLoaderAvailable) {
    Long lifeTime = builder.timeUnit != null ?
        builder.timeUnit.toMillis(builder.duration) : null;

    return new ConfigProvider(builder.key, useExpiredDataIfNotLoaderAvailable, lifeTime,
        detailResponse,
        builder.expirable, builder.encrypted, builder.key,
        group, loader, evict);
  }

  public static class ProviderBuilder<T> {
    protected String key;
    private boolean encrypted, expirable;
    private Long duration;
    private TimeUnit timeUnit;
    private final ProcessorProviders processorProviders;

    ProviderBuilder(ProcessorProviders processorProviders) {
      this.encrypted = false;
      this.expirable = true;
      this.processorProviders = processorProviders;
    }

    /**
     * Same as {@link Provider.ProviderBuilder#encrypt(boolean)}
     */
    public ProviderBuilder<T> encrypt(boolean encrypt) {
      this.encrypted = encrypt;
      return this;
    }

    /**
     * Same as {@link Provider.ProviderBuilder#expirable(boolean)}
     */
    public ProviderBuilder<T> expirable(boolean expirable) {
      this.expirable = expirable;
      return this;
    }

    /**
     * Same as {@link Provider.ProviderBuilder#lifeCache(long, TimeUnit)}
     */
    public ProviderBuilder<T> lifeCache(long duration, TimeUnit timeUnit) {
      this.duration = duration;
      this.timeUnit = timeUnit;
      return this;
    }

    /**
     * Same as {@link Provider.ProviderBuilder#withKey(Object)}
     */
    public <R extends ProviderGroup<T>> R withKey(Object key) {
      this.key = key.toString();
      return (R) new ProviderGroup<>(this);
    }
  }
}