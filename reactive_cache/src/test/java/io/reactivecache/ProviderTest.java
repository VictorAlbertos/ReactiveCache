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

import io.rx_cache.Reply;
import io.rx_cache.RxCacheException;
import io.rx_cache.Source;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import rx.Observable;
import rx.observers.TestSubscriber;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public final class ProviderTest {
  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();
  private ReactiveCache reactiveCache;
  private Provider<Mock> cacheProvider;
  private static final String MESSAGE = "0";

  @Before public void setUp() {
    reactiveCache = new ReactiveCache.Builder()
        .using(temporaryFolder.getRoot(), Jolyglot$.newInstance());

    cacheProvider = reactiveCache.<Mock>provider()
        .withKey("mock");
  }

  @Test public void When_Evict_With_No_Cached_Data_Then_Do_Not_Throw() {
    TestSubscriber<Void> subscriber = new TestSubscriber<>();

    cacheProvider.evict()
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertNoErrors();
    subscriber.assertValueCount(1);
    subscriber.assertCompleted();
  }

  @Test public void When_Evict_With_Cached_Data_Then_Evict_Data() {
    saveMock();
    verifyMockCached();

    TestSubscriber<Void> subscriber = new TestSubscriber<>();

    cacheProvider.evict()
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertNoErrors();
    subscriber.assertValueCount(1);
    subscriber.assertCompleted();

    verifyNoMockCached();
  }

  @Test public void When_Evict_With_Cached_And_Use_Expired_Data_Then_Evict_Data() {
    reactiveCache = new ReactiveCache.Builder()
        .useExpiredDataWhenNoLoaderAvailable()
        .using(temporaryFolder.getRoot(), Jolyglot$.newInstance());

    cacheProvider = reactiveCache.<Mock>provider()
        .withKey("mock");

    saveMock();
    verifyMockCached();

    TestSubscriber<Void> subscriber = new TestSubscriber<>();

    cacheProvider.evict()
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertNoErrors();
    subscriber.assertValueCount(1);
    subscriber.assertCompleted();

    verifyNoMockCached();
  }

  @Test public void When_Replace_But_Loader_Throws_Then_Do_Not_Replace_Cache() {
    saveMock();
    verifyMockCached();

    TestSubscriber<Mock> subscriber = new TestSubscriber<>();

    Observable.<Mock>error(new RuntimeException())
        .compose(cacheProvider.replace())
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertError(RuntimeException.class);
    subscriber.assertNoValues();

    verifyMockCached();
  }

  @Test public void When_Replace_Then_Replace_Cache() {
    saveMock();
    verifyMockCached();

    TestSubscriber<Mock> subscriber = new TestSubscriber<>();

    Observable.just(new Mock("1"))
        .compose(cacheProvider.replace())
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertValueCount(1);
    subscriber.assertNoErrors();
    subscriber.assertCompleted();

    subscriber = new TestSubscriber<>();

    cacheProvider.read()
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertValueCount(1);
    subscriber.assertNoErrors();
    subscriber.assertCompleted();
    assertThat(subscriber.getOnNextEvents()
        .get(0).getMessage(), is("1"));
  }

  @Test public void When_Read_Nullable_With_Nothing_To_Read_Then_Return_Null() {
    TestSubscriber<Mock> subscriber = new TestSubscriber<>();

    cacheProvider.readNullable()
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertValueCount(1);
    subscriber.assertNoErrors();
    subscriber.assertCompleted();
  }

  @Test public void When_Read_Nullable_Then_Return_Data() {
    saveMock();

    TestSubscriber<Mock> subscriber = new TestSubscriber<>();

    cacheProvider.readNullable()
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertValueCount(1);
    subscriber.assertNoErrors();
    subscriber.assertCompleted();
    assertThat(subscriber.getOnNextEvents()
        .get(0).getMessage(), is(MESSAGE));
  }

  @Test public void When_Read_With_Nothing_To_Read_Then_Throw() {
    TestSubscriber<Mock> subscriber = new TestSubscriber<>();

    cacheProvider.read()
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertError(RxCacheException.class);
    subscriber.assertNoValues();
  }

  @Test public void When_Read_Then_Return_Data() {
    saveMock();

    TestSubscriber<Mock> subscriber = new TestSubscriber<>();

    cacheProvider.read()
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertValueCount(1);
    subscriber.assertNoErrors();
    subscriber.assertCompleted();
    assertThat(subscriber.getOnNextEvents()
        .get(0).getMessage(), is(MESSAGE));
  }

  @Test public void Verify_Read_With_Loader() {
    cacheProvider = reactiveCache.<Mock>provider()
        .lifeCache(100, TimeUnit.MILLISECONDS)
        .withKey("ephemeralMock");

    saveMock();

    TestSubscriber<Mock> subscriber = new TestSubscriber<>();

    Observable.just(new Mock("1"))
        .compose(cacheProvider.readWithLoader())
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertNoErrors();
    subscriber.assertValueCount(1);
    assertThat(subscriber.getOnNextEvents()
        .get(0).getMessage(), is(MESSAGE));

    waitTime(200);

    subscriber = new TestSubscriber<>();

    Observable.just(new Mock("1"))
        .compose(cacheProvider.readWithLoader())
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertNoErrors();
    subscriber.assertValueCount(1);
    assertThat(subscriber.getOnNextEvents()
        .get(0).getMessage(), is("1"));

    Observable.just(new Mock("3"))
        .compose(cacheProvider.readWithLoader())
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertNoErrors();
    subscriber.assertValueCount(1);
    assertThat(subscriber.getOnNextEvents()
        .get(0).getMessage(), is("1"));
  }

  @Test public void Verify_Read_As_Reply_With_Loader() {
    cacheProvider = reactiveCache.<Mock>provider()
        .lifeCache(100, TimeUnit.MILLISECONDS)
        .withKey("ephemeralMock");

    saveMock();

    TestSubscriber<Reply<Mock>> subscriber = new TestSubscriber<>();

    Observable.just(new Mock(MESSAGE))
        .compose(cacheProvider.readWithLoaderAsReply())
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertNoErrors();
    subscriber.assertValueCount(1);
    assertThat(subscriber.getOnNextEvents()
        .get(0).getSource(), is(Source.MEMORY));

    waitTime(200);

    subscriber = new TestSubscriber<>();

    Observable.just(new Mock(MESSAGE))
        .compose(cacheProvider.readWithLoaderAsReply())
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertNoErrors();
    subscriber.assertValueCount(1);
    assertThat(subscriber.getOnNextEvents()
        .get(0).getSource(), is(Source.CLOUD));
  }

  private void saveMock() {
    TestSubscriber<Mock> subscriber = new TestSubscriber<>();

    Observable.just(new Mock(MESSAGE))
        .compose(cacheProvider.replace())
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();
  }

  private void verifyMockCached() {
    TestSubscriber<Mock> subscriber = new TestSubscriber<>();

    cacheProvider.read()
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertNoErrors();
    subscriber.assertValueCount(1);
    assertThat(subscriber.getOnNextEvents()
        .get(0).getMessage(), is(MESSAGE));
  }

  private void verifyNoMockCached() {
    TestSubscriber<Mock> subscriber = new TestSubscriber<>();

    cacheProvider.read()
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertError(RxCacheException.class);
    subscriber.assertNoValues();
    subscriber.onCompleted();
  }

  private void waitTime(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }


}
