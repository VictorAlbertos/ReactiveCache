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
import io.rx_cache.Reply;
import io.rx_cache.internal.Locale;
import io.rx_cache.internal.ProcessorProviders;
import java.util.concurrent.TimeUnit;
import rx.Observable;

/**
 * Entry point to manage cache CRUD operations.
 *
 * @param <T> The type of the data to persist.
 */
public final class Provider<T> {
  private final ProviderBuilder<T> builder;

  Provider(ProviderBuilder<T> builder) {
    this.builder = builder;
  }

  /**
   * Evict all the cached data for this provider.
   */
  public final Observable<Void> evict() {
    return Observable.defer(() ->
        builder.processorProviders
            .<Void>process(getConfigProvider(Observable.<T>just(null),
                new EvictDynamicKey(true), false, false))
            .onErrorResumeNext(this::nullOnRxCacheLoaderError)
    );
  }

  /**
   * Replace the cached data by the element emitted from the observable.
   */
  public final Observable.Transformer<T, T> replace() {
    return loader ->
        loader.flatMap(data -> builder.processorProviders
            .process(getConfigProvider(Observable.just(data),
                new EvictDynamicKey(true), false, null)));
  }

  /**
   * Read from cache and throw if no data is available.
   */
  public final Observable<T> read() {
    return Observable.defer(() -> builder.processorProviders
        .process(getConfigProvider(Observable.<T>just(null),
            new EvictDynamicKey(false), false, null)));
  }

  /**
   * Read from cache and return null if no data is available.
   */
  public final Observable<T> readNullable() {
    return Observable.defer(() -> builder.processorProviders
        .<T>process(getConfigProvider(Observable.<T>just(null),
            new EvictDynamicKey(false), false, null)))
        .onErrorResumeNext(this::nullOnRxCacheLoaderError);
  }

  /**
   * Read from cache but if there is not data available then read from the loader and cache its
   * element.
   */
  public final Observable.Transformer<T, T> readWithLoader() {
    return loader -> builder.processorProviders
        .process(getConfigProvider(loader, new EvictDynamicKey(false), false, null));
  }

  /**
   * Same as {@link Provider#replace()} but wrap the data in a Reply object for debug purposes.
   */
  public final Observable.Transformer<T, Reply<T>> replaceAsReply() {
    return loader ->
        loader.flatMap(data -> builder.processorProviders
            .process(getConfigProvider(Observable.just(data),
                new EvictDynamicKey(true), true, null)));
  }

  /**
   * Same as {@link Provider#readWithLoader()} but wrap the data in a Reply object for debug
   * purposes.
   */
  public final Observable.Transformer<T, Reply<T>> readWithLoaderAsReply() {
    return loader -> builder.processorProviders
        .process(getConfigProvider(loader, new EvictDynamicKey(false), true, null));
  }

  private ConfigProvider getConfigProvider(Observable<T> loader,
      EvictDynamicKey evict, boolean detailResponse, Boolean useExpiredDataIfNotLoaderAvailable) {
    Long lifeTime = builder.timeUnit != null ?
        builder.timeUnit.toMillis(builder.duration) : null;

    return new ConfigProvider(builder.key, useExpiredDataIfNotLoaderAvailable, lifeTime,
        detailResponse,
        builder.expirable, builder.encrypted, builder.key,
        "", loader, evict);
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
     * If called, this provider encrypts its data as long as ReactiveCache has been configured with
     * an encryption key.
     */
    public ProviderBuilder<T> encrypt(boolean encrypt) {
      this.encrypted = encrypt;
      return this;
    }

    /**
     * Make the data associated with this provider eligible to be expired if not enough space
     * remains on disk. By default is true.
     */
    public ProviderBuilder<T> expirable(boolean expirable) {
      this.expirable = expirable;
      return this;
    }

    /**
     * Set the amount of time before the data would be evicted. If life cache is not configured, the
     * data will be never evicted unless it is required explicitly using {@link Provider#evict()} or
     * {@link Provider#replace()}
     */
    public ProviderBuilder<T> lifeCache(long duration, TimeUnit timeUnit) {
      this.duration = duration;
      this.timeUnit = timeUnit;
      return this;
    }

    /**
     * Set the key for the provider.
     */
    public Provider<T> withKey(Object key) {
      this.key = key.toString();
      return new Provider<>(this);
    }
  }
}
