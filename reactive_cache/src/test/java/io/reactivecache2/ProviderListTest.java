/*
 * Copyright 2017 Victor Albertos
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

import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.rx_cache2.RxCacheException;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class ProviderListTest extends ActionsListTest {
  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();
  private ReactiveCache reactiveCache;
  private ProviderList<Mock> cacheProvider;

  @Before public void setUp() {
    reactiveCache = new ReactiveCache.Builder()
        .using(temporaryFolder.getRoot(), Jolyglot$.newInstance());
    cacheProvider = reactiveCache.<Mock>providerList()
        .withKey("mock");
  }

  @Test public void Verify_Inherited_Api() {
    final String message = "1";

    //save
    Single.just(Arrays.asList(new Mock(message)))
        .compose(cacheProvider.replace())
        .test()
        .awaitTerminalEvent();

    TestObserver<List<Mock>> observer = cacheProvider.read()
        .test();

    //read
    observer.awaitTerminalEvent();
    observer
        .assertValueAt(0, mocks -> mocks.get(0).getMessage().equals("1"))
        .assertNoErrors()
        .assertComplete();

    //evict
    cacheProvider.evict().test().awaitTerminalEvent();

    //read throws
    observer = cacheProvider.read().test();
    observer.awaitTerminalEvent();
    observer.assertNoValues()
        .assertError(RxCacheException.class)
        .assertNotComplete();

    //read with loader
    observer = Single.just(Arrays.asList(new Mock(message)))
        .compose(cacheProvider.readWithLoader()).test();
    observer.awaitTerminalEvent();
    observer
        .assertValueAt(0, mocks -> mocks.get(0).getMessage().equals("1"))
        .assertNoErrors()
        .assertComplete();
  }

  @Override protected ActionsList<Mock> actions() {
    return cacheProvider.entries();
  }

  @Override protected Single<List<Mock>> cache() {
    return cacheProvider.read();
  }
}
