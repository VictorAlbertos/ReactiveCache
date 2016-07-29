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

public final class ProviderGroupTest {
  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();
  private ReactiveCache reactiveCache;
  private ProviderGroup<Mock> cacheProvider;
  private static final String MESSAGE_GROUP = "MESSAGE_GROUP";

  @Before public void setUp() {
    reactiveCache = new ReactiveCache.Builder()
        .using(temporaryFolder.getRoot(), Jolyglot$.newInstance());

    cacheProvider = reactiveCache.<Mock>providerGroup()
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

  @Test public void When_Evict_By_Group_With_No_Cached_Data_Then_Do_Not_Throw() {
    TestSubscriber<Void> subscriber = new TestSubscriber<>();

    cacheProvider.evict(MESSAGE_GROUP)
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertNoErrors();
    subscriber.assertValueCount(1);
    subscriber.assertCompleted();
  }

  @Test public void When_Evict_With_Cached_Data_Then_Evict_Data() {
    saveMock(MESSAGE_GROUP);
    verifyMockCached(MESSAGE_GROUP);

    saveMock(MESSAGE_GROUP+2);
    verifyMockCached(MESSAGE_GROUP+2);

    TestSubscriber<Void> subscriber = new TestSubscriber<>();

    cacheProvider.evict()
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertNoErrors();
    subscriber.assertValueCount(1);
    subscriber.assertCompleted();

    verifyNoMockCached(MESSAGE_GROUP);
    verifyNoMockCached(MESSAGE_GROUP+2);
  }

  @Test public void When_Evict_By_Group_With_Cached_Data_Then_Evict_Data() {
    saveMock(MESSAGE_GROUP);
    verifyMockCached(MESSAGE_GROUP);

    saveMock(MESSAGE_GROUP+2);
    verifyMockCached(MESSAGE_GROUP+2);

    TestSubscriber<Void> subscriber = new TestSubscriber<>();

    cacheProvider.evict(MESSAGE_GROUP)
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertNoErrors();
    subscriber.assertValueCount(1);
    subscriber.assertCompleted();

    verifyNoMockCached(MESSAGE_GROUP);
    verifyMockCached(MESSAGE_GROUP+2);
  }

  @Test public void When_Evict_With_Cached_And_Use_Expired_Data_Then_Evict_Data() {
    reactiveCache = new ReactiveCache.Builder()
        .useExpiredDataWhenNoLoaderAvailable()
        .using(temporaryFolder.getRoot(), Jolyglot$.newInstance());

    cacheProvider = reactiveCache.<Mock>providerGroup()
        .withKey("mock");

    saveMock(MESSAGE_GROUP);
    verifyMockCached(MESSAGE_GROUP);

    saveMock(MESSAGE_GROUP+2);
    verifyMockCached(MESSAGE_GROUP+2);

    TestSubscriber<Void> subscriber = new TestSubscriber<>();

    cacheProvider.evict()
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertNoErrors();
    subscriber.assertValueCount(1);
    subscriber.assertCompleted();

    verifyNoMockCached(MESSAGE_GROUP);
    verifyNoMockCached(MESSAGE_GROUP+2);
  }

  @Test public void When_Evict_By_Group_With_Cached_And_Use_Expired_Data_Then_Evict_Data() {
    reactiveCache = new ReactiveCache.Builder()
        .useExpiredDataWhenNoLoaderAvailable()
        .using(temporaryFolder.getRoot(), Jolyglot$.newInstance());

    cacheProvider = reactiveCache.<Mock>providerGroup()
        .withKey("mock");

    saveMock(MESSAGE_GROUP+2);
    verifyMockCached(MESSAGE_GROUP+2);

    saveMock(MESSAGE_GROUP);
    verifyMockCached(MESSAGE_GROUP);

    TestSubscriber<Void> subscriber = new TestSubscriber<>();

    cacheProvider.evict(MESSAGE_GROUP)
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertNoErrors();
    subscriber.assertValueCount(1);
    subscriber.assertCompleted();

    verifyNoMockCached(MESSAGE_GROUP);
    verifyMockCached(MESSAGE_GROUP+2);
  }

  @Test public void When_Replace_But_Loader_Throws_Then_Do_Not_Replace_Cache() {
    saveMock(MESSAGE_GROUP);
    verifyMockCached(MESSAGE_GROUP);

    TestSubscriber<Mock> subscriber = new TestSubscriber<>();

    Observable.<Mock>error(new RuntimeException())
        .compose(cacheProvider.replace(MESSAGE_GROUP))
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertError(RuntimeException.class);
    subscriber.assertNoValues();

    verifyMockCached(MESSAGE_GROUP);
  }

  @Test public void When_Replace_Then_Replace_Cache() {
    saveMock(MESSAGE_GROUP);
    verifyMockCached(MESSAGE_GROUP);

    TestSubscriber<Mock> subscriber = new TestSubscriber<>();

    Observable.just(new Mock("1"))
        .compose(cacheProvider.replace(MESSAGE_GROUP))
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertValueCount(1);
    subscriber.assertNoErrors();
    subscriber.assertCompleted();

    subscriber = new TestSubscriber<>();

    cacheProvider.read(MESSAGE_GROUP)
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

    cacheProvider.readNullable(MESSAGE_GROUP)
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertValueCount(1);
    subscriber.assertNoErrors();
    subscriber.assertCompleted();
  }

  @Test public void When_Read_Nullable_Then_Return_Data() {
    saveMock(MESSAGE_GROUP);

    TestSubscriber<Mock> subscriber = new TestSubscriber<>();

    cacheProvider.readNullable(MESSAGE_GROUP)
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertValueCount(1);
    subscriber.assertNoErrors();
    subscriber.assertCompleted();
    assertThat(subscriber.getOnNextEvents()
        .get(0).getMessage(), is(MESSAGE_GROUP));
  }

  @Test public void When_Read_With_Nothing_To_Read_Then_Throw() {
    TestSubscriber<Mock> subscriber = new TestSubscriber<>();

    cacheProvider.read(MESSAGE_GROUP)
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertError(RxCacheException.class);
    subscriber.assertNoValues();
  }

  @Test public void When_Read_Then_Return_Data() {
    saveMock(MESSAGE_GROUP);

    TestSubscriber<Mock> subscriber = new TestSubscriber<>();

    cacheProvider.read(MESSAGE_GROUP)
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertValueCount(1);
    subscriber.assertNoErrors();
    subscriber.assertCompleted();
    assertThat(subscriber.getOnNextEvents()
        .get(0).getMessage(), is(MESSAGE_GROUP));
  }

  @Test public void Verify_Read_With_Loader() {
    cacheProvider = reactiveCache.<Mock>providerGroup()
        .lifeCache(100, TimeUnit.MILLISECONDS)
        .withKey("ephemeralMock");

    saveMock(MESSAGE_GROUP);

    TestSubscriber<Mock> subscriber = new TestSubscriber<>();

    Observable.just(new Mock("1"))
        .compose(cacheProvider.readWithLoader(MESSAGE_GROUP))
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertNoErrors();
    subscriber.assertValueCount(1);
    assertThat(subscriber.getOnNextEvents()
        .get(0).getMessage(), is(MESSAGE_GROUP));

    waitTime(200);

    subscriber = new TestSubscriber<>();

    Observable.just(new Mock("1"))
        .compose(cacheProvider.readWithLoader(MESSAGE_GROUP))
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertNoErrors();
    subscriber.assertValueCount(1);
    assertThat(subscriber.getOnNextEvents()
        .get(0).getMessage(), is("1"));

    Observable.just(new Mock("3"))
        .compose(cacheProvider.readWithLoader(MESSAGE_GROUP))
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertNoErrors();
    subscriber.assertValueCount(1);
    assertThat(subscriber.getOnNextEvents()
        .get(0).getMessage(), is("1"));
  }

  @Test public void Verify_Read_As_Reply_With_Loader() {
    cacheProvider = reactiveCache.<Mock>providerGroup()
        .lifeCache(100, TimeUnit.MILLISECONDS)
        .withKey("ephemeralMock");

    saveMock(MESSAGE_GROUP);

    TestSubscriber<Reply<Mock>> subscriber = new TestSubscriber<>();

    Observable.just(new Mock(MESSAGE_GROUP))
        .compose(cacheProvider.readWithLoaderAsReply(MESSAGE_GROUP))
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertNoErrors();
    subscriber.assertValueCount(1);
    assertThat(subscriber.getOnNextEvents()
        .get(0).getSource(), is(Source.MEMORY));

    waitTime(200);

    subscriber = new TestSubscriber<>();

    Observable.just(new Mock(MESSAGE_GROUP))
        .compose(cacheProvider.readWithLoaderAsReply(MESSAGE_GROUP))
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertNoErrors();
    subscriber.assertValueCount(1);
    assertThat(subscriber.getOnNextEvents()
        .get(0).getSource(), is(Source.CLOUD));
  }

  @Test public void Verify_Pagination() {
    ProviderGroup<Mock> mockProvider = reactiveCache.<Mock>providerGroup()
        .withKey("mocks");

    TestSubscriber<Mock> subscriber;

    Mock mockPage1 = new Mock("1");

    subscriber = new TestSubscriber<>();
    Observable.just(mockPage1)
        .compose(mockProvider.replace("1"))
        .subscribe(subscriber);

    subscriber.awaitTerminalEvent();

    Mock mockPage2 = new Mock("2");

    subscriber = new TestSubscriber<>();
    Observable.just(mockPage2)
        .compose(mockProvider.replace("2"))
        .subscribe(subscriber);

    subscriber.awaitTerminalEvent();

    Mock mockPage3 = new Mock("3");

    subscriber = new TestSubscriber<>();
    Observable.just(mockPage3)
        .compose(mockProvider.replace("3"))
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber = new TestSubscriber<>();
    mockProvider.read("1")
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();
    assertThat(subscriber.getOnNextEvents().get(0).getMessage(), is("1"));

    subscriber = new TestSubscriber<>();
    mockProvider.read("2")
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();
    assertThat(subscriber.getOnNextEvents().get(0).getMessage(), is("2"));

    subscriber = new TestSubscriber<>();
    mockProvider.read("3")
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();
    assertThat(subscriber.getOnNextEvents().get(0).getMessage(), is("3"));
  }

  private void saveMock(String messageGroup) {
    TestSubscriber<Mock> subscriber = new TestSubscriber<>();

    Observable.just(new Mock(messageGroup))
        .compose(cacheProvider.replace(messageGroup))
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();
  }

  private void verifyMockCached(String messageGroup) {
    TestSubscriber<Mock> subscriber = new TestSubscriber<>();

    cacheProvider.read(messageGroup)
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();

    subscriber.assertNoErrors();
    subscriber.assertValueCount(1);
    assertThat(subscriber.getOnNextEvents()
        .get(0).getMessage(), is(messageGroup));
  }

  private void verifyNoMockCached(String group) {
    TestSubscriber<Mock> subscriber = new TestSubscriber<>();

    cacheProvider.read(group)
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
