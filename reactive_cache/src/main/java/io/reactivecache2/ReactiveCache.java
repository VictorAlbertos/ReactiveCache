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
import io.rx_cache2.MigrationCache;
import io.rx_cache2.internal.DaggerRxCacheComponent;
import io.rx_cache2.internal.Locale;
import io.rx_cache2.internal.ProcessorProviders;
import io.rx_cache2.internal.RxCacheModule;
import io.victoralbertos.jolyglot.JolyglotGenerics;
import java.io.File;
import java.security.InvalidParameterException;
import java.util.List;

public final class ReactiveCache {
  private final ProcessorProviders processorProviders;

  private ReactiveCache(Builder builder) {
    this.processorProviders = DaggerRxCacheComponent.builder()
        .rxCacheModule(new RxCacheModule(builder.cacheDirectory,
            builder.useExpiredDataIfLoaderNotAvailable, builder.diskCacheSize,
            builder.encryptKey, builder.migrationsCache,
            builder.jolyglot))
        .build().providers();
  }

  /**
   * Return a {@link Provider.ProviderBuilder} to build the provider.
   *
   * @param <T> the type of data to be cached.
   */
  public <T> Provider.ProviderBuilder<T> provider() {
    return new Provider.ProviderBuilder<>(processorProviders);
  }

  /**
   * Return a {@link ProviderList.ProviderBuilderList} to build the provider.
   *
   * @param <T> the type of data to be cached.
   */
  public <T> ProviderList.ProviderBuilderList<T> providerList() {
    return new ProviderList.ProviderBuilderList<>(processorProviders);
  }

  /**
   * Return a {@link ProviderGroup.ProviderBuilder} to build the provider group.
   *
   * @param <T> the type of data to be cached.
   */
  public <T> ProviderGroup.ProviderBuilder<T> providerGroup() {
    return new ProviderGroup.ProviderBuilder<>(processorProviders);
  }

  /**
   * Return a {@link ProviderGroupList.ProviderBuilder} to build the provider.
   *
   * @param <T> the type of data to be cached.
   */
  public <T> ProviderGroupList.ProviderBuilderList<T> providerGroupList() {
    return new ProviderGroupList.ProviderBuilderList<>(processorProviders);
  }

  /**
   * Evict all the cached data.
   */
  public Completable evictAll() {
    return Completable.fromObservable(processorProviders.evictAll());
  }

  /**
   * Builder for building an specific ReactiveCache instance
   */
  public static class Builder {
    private boolean useExpiredDataIfLoaderNotAvailable;
    private Integer diskCacheSize;
    private String encryptKey;
    private List<MigrationCache> migrationsCache;
    private File cacheDirectory;
    private JolyglotGenerics jolyglot;

    /**
     * if called ReactiveCache dispatches records already expired instead of throwing an exception.
     */
    public Builder useExpiredDataWhenNoLoaderAvailable() {
      this.useExpiredDataIfLoaderNotAvailable = true;
      return this;
    }

    /**
     * Sets the max memory in megabytes for all the cached data on disk If not supplied, 100
     * megabytes will be the default value.
     */
    public Builder diskCacheSize(Integer megabytes) {
      this.diskCacheSize = megabytes;
      return this;
    }

    /**
     * Set the key to encrypt data for specifics providers.
     */
    public Builder encrypt(String key) {
      this.encryptKey = key;
      return this;
    }

    /**
     * Set the migrations to run between releases.
     */
    public Builder migrations(List<MigrationCache> migrationsCache) {
      this.migrationsCache = migrationsCache;
      return this;
    }

    /**
     * Sets the File cache system and the implementation of {@link JolyglotGenerics} to serialise
     * and deserialize objects
     *
     * @param cacheDirectory The File system used by the persistence layer
     * @param jolyglot A concrete implementation of {@link JolyglotGenerics}
     */
    public ReactiveCache using(File cacheDirectory, JolyglotGenerics jolyglot) {
      if (cacheDirectory == null) {
        throw new InvalidParameterException(Locale.REPOSITORY_DISK_ADAPTER_CAN_NOT_BE_NULL);
      }
      if (!cacheDirectory.exists()) {
        throw new InvalidParameterException(Locale.REPOSITORY_DISK_ADAPTER_DOES_NOT_EXIST);
      }
      if (!cacheDirectory.canWrite()) {
        throw new InvalidParameterException(Locale.REPOSITORY_DISK_ADAPTER_IS_NOT_WRITABLE);
      }

      if (jolyglot == null) {
        throw new InvalidParameterException(Locale.JSON_CONVERTER_CAN_NOT_BE_NULL);
      }

      this.cacheDirectory = cacheDirectory;
      this.jolyglot = jolyglot;

      return new ReactiveCache(this);
    }
  }
}
