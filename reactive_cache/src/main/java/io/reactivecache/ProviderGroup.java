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

package io.reactivecache;

import io.rx_cache.ConfigProvider;
import io.rx_cache.EvictDynamicKey;
import io.rx_cache.EvictDynamicKeyGroup;
import io.rx_cache.Reply;
import io.rx_cache.internal.Locale;
import io.rx_cache.internal.ProcessorProviders;
import java.util.concurrent.TimeUnit;
import rx.Observable;

/**
 * Entry point to manage cache CRUD operations with groups.
 *
 * @param <T> The type of the data to persist.
 */
public final class ProviderGroup<T> {
  private final ProviderBuilder<T> builder;

  ProviderGroup(ProviderBuilder<T> builder) {
    this.builder = builder;
  }

  /**
   * Evict all the cached data for this provider.
   */
  public final Observable<Void> evict() {
    return Observable.defer(() -> builder.processorProviders
        .<Void>process(getConfigProvider(Observable.<T>just(null), "",
            new EvictDynamicKey(true), false, false))
        .onErrorResumeNext(this::nullOnRxCacheLoaderError)
    );
  }

  /**
   * Evict the cached data by group.
   */
  public final Observable<Void> evict(final Object group) {
    return Observable.defer(() -> builder.processorProviders
        .<Void>process(getConfigProvider(Observable.<T>just(null), group.toString(),
            new EvictDynamicKeyGroup(true), false, false))
        .onErrorResumeNext(this::nullOnRxCacheLoaderError)
    );
  }

  /**
   * Replace the cached data by group based on the element emitted from the observable.
   */
  public final Observable.Transformer<T, T> replace(final Object group) {
    return loader ->
        loader.flatMap(data -> builder.processorProviders
            .process(getConfigProvider(Observable.just(data), group.toString(),
                new EvictDynamicKeyGroup(true), false, null)));
  }

  /**
   * Read from cache by group and throw if no data is available.
   */
  public final Observable<T> read(final Object group) {
    return Observable.defer(() -> builder.processorProviders
        .process(getConfigProvider(Observable.<T>just(null), group.toString(),
            new EvictDynamicKeyGroup(false), false, null)));
  }

  /**
   * Read from cache by group and return null if no data is available.
   */
  public final Observable<T> readNullable(final Object group) {
    return Observable.defer(() -> builder.processorProviders
        .<T>process(getConfigProvider(Observable.<T>just(null), group.toString(),
            new EvictDynamicKeyGroup(false), false, null)))
        .onErrorResumeNext(this::nullOnRxCacheLoaderError);
  }

  /**
   * Read from cache by group but if the is not data available then read from the loader and cache
   * its element.
   */
  public final Observable.Transformer<T, T> readWithLoader(final Object group) {
    return loader -> builder.processorProviders
        .process(getConfigProvider(loader, group.toString(),
            new EvictDynamicKeyGroup(false), false, null));
  }

  /**
   * Same as {@link ProviderGroup#replace(Object)} but wrap the data in a Reply object for debug
   * purposes.
   */
  public final Observable.Transformer<T, Reply<T>> replaceAsReply(final Object group) {
    return loader ->
        loader.flatMap(data -> builder.processorProviders
            .process(getConfigProvider(Observable.just(data), group.toString(),
                new EvictDynamicKeyGroup(true), true, null)));
  }

  /**
   * Same as {@link ProviderGroup#readWithLoader(Object)} but wrap the data in a Reply object for
   * debug purposes.
   */
  public final Observable.Transformer<T, Reply<T>> readWithLoaderAsReply(final Object group) {
    return loader -> builder.processorProviders
        .process(getConfigProvider(loader, group.toString(), new EvictDynamicKeyGroup(false),
            true, null));
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

  private <E> Observable<E> nullOnRxCacheLoaderError(Throwable error) {
    String expected = Locale.NOT_DATA_RETURN_WHEN_CALLING_OBSERVABLE_LOADER
        + " " + builder.key;
    if (error.getMessage().equals(expected)) {
      return Observable.<E>just(null);
    } else {
      return Observable.error(error);
    }
  }

  public static class ProviderBuilder<T> {
    private String key;
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
    public ProviderGroup<T> withKey(Object key) {
      this.key = key.toString();
      return new ProviderGroup<>(this);
    }
  }
}