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

import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.rx_cache2.Reply;
import io.rx_cache2.RxCacheException;
import io.rx_cache2.Source;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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
    TestObserver<Void> observer = cacheProvider.evict().test();
    observer.awaitTerminalEvent();

    observer.assertNoErrors();
    observer.assertNoValues();
    observer.assertComplete();
  }

  @Test public void When_Evict_By_Group_With_No_Cached_Data_Then_Do_Not_Throw() {
    TestObserver<Void> observer = cacheProvider.evict(MESSAGE_GROUP).test();
    observer.awaitTerminalEvent();

    observer.assertNoErrors();
    observer.assertNoValues();
    observer.assertComplete();
  }

  @Test public void When_Evict_With_Cached_Data_Then_Evict_Data() {
    saveMock(MESSAGE_GROUP);
    verifyMockCached(MESSAGE_GROUP);

    saveMock(MESSAGE_GROUP+2);
    verifyMockCached(MESSAGE_GROUP+2);

    TestObserver<Void> observer = cacheProvider.evict().test();
    observer.awaitTerminalEvent();

    observer.assertNoErrors();
    observer.assertNoValues();
    observer.assertComplete();

    verifyNoMockCached(MESSAGE_GROUP);
    verifyNoMockCached(MESSAGE_GROUP+2);
  }

  @Test public void When_Evict_By_Group_With_Cached_Data_Then_Evict_Data() {
    saveMock(MESSAGE_GROUP);
    verifyMockCached(MESSAGE_GROUP);

    saveMock(MESSAGE_GROUP+2);
    verifyMockCached(MESSAGE_GROUP+2);

    TestObserver<Void> observer = cacheProvider.evict(MESSAGE_GROUP).test();
    observer.awaitTerminalEvent();

    observer.assertNoErrors();
    observer.assertNoValues();
    observer.assertComplete();

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

    TestObserver<Void> observer = cacheProvider.evict().test();
    observer.awaitTerminalEvent();

    observer.assertNoErrors();
    observer.assertNoValues();
    observer.assertComplete();

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

    TestObserver<Void> observer = cacheProvider.evict(MESSAGE_GROUP).test();
    observer.awaitTerminalEvent();

    observer.assertNoErrors();
    observer.assertNoValues();
    observer.assertComplete();

    verifyNoMockCached(MESSAGE_GROUP);
    verifyMockCached(MESSAGE_GROUP+2);
  }

  @Test public void When_Replace_But_Loader_Throws_Then_Do_Not_Replace_Cache() {
    saveMock(MESSAGE_GROUP);
    verifyMockCached(MESSAGE_GROUP);

    TestObserver<Mock> observer = Single.<Mock>error(new RuntimeException())
        .compose(cacheProvider.replace(MESSAGE_GROUP))
        .test();
    observer.awaitTerminalEvent();

    observer.assertError(RuntimeException.class);
    observer.assertNoValues();

    verifyMockCached(MESSAGE_GROUP);
  }

  @Test public void When_Replace_Then_Replace_Cache() {
    saveMock(MESSAGE_GROUP);
    verifyMockCached(MESSAGE_GROUP);

    TestObserver<Mock> observer = Single.just(new Mock("1"))
        .compose(cacheProvider.replace(MESSAGE_GROUP))
        .test();
    observer.awaitTerminalEvent();

    observer.assertValueCount(1);
    observer.assertNoErrors();
    observer.assertComplete();

    observer = cacheProvider.read(MESSAGE_GROUP).test();
    observer.awaitTerminalEvent();

    observer.assertValueCount(1);
    observer.assertNoErrors();
    observer.assertComplete();
    assertThat(observer.values()
        .get(0).getMessage(), is("1"));
  }

  @Test public void When_Read_With_Nothing_To_Read_Then_Throw() {
    TestObserver<Mock> observer = cacheProvider.read(MESSAGE_GROUP).test();
    observer.awaitTerminalEvent();

    observer.assertError(RxCacheException.class);
    observer.assertNoValues();
  }

  @Test public void When_Read_Then_Return_Data() {
    saveMock(MESSAGE_GROUP);

    TestObserver<Mock> observer = cacheProvider.read(MESSAGE_GROUP).test();
    observer.awaitTerminalEvent();

    observer.assertValueCount(1);
    observer.assertNoErrors();
    observer.assertComplete();
    assertThat(observer.values()
        .get(0).getMessage(), is(MESSAGE_GROUP));
  }

  @Test public void Verify_Read_With_Loader() {
    cacheProvider = reactiveCache.<Mock>providerGroup()
        .lifeCache(100, TimeUnit.MILLISECONDS)
        .withKey("ephemeralMock");

    saveMock(MESSAGE_GROUP);

    TestObserver<Mock> observer = Single.just(new Mock("1"))
        .compose(cacheProvider.readWithLoader(MESSAGE_GROUP))
        .test();
    observer.awaitTerminalEvent();

    observer.assertNoErrors();
    observer.assertValueCount(1);
    assertThat(observer.values()
        .get(0).getMessage(), is(MESSAGE_GROUP));

    waitTime(200);

    observer = Single.just(new Mock("1"))
        .compose(cacheProvider.readWithLoader(MESSAGE_GROUP))
        .test();
    observer.awaitTerminalEvent();

    observer.assertNoErrors();
    observer.assertValueCount(1);
    assertThat(observer.values()
        .get(0).getMessage(), is("1"));

    observer = Single.just(new Mock("3"))
        .compose(cacheProvider.readWithLoader(MESSAGE_GROUP))
        .test();
    observer.awaitTerminalEvent();

    observer.assertNoErrors();
    observer.assertValueCount(1);
    assertThat(observer.values()
        .get(0).getMessage(), is("1"));
  }

  @Test public void Verify_Read_As_Reply_With_Loader() {
    cacheProvider = reactiveCache.<Mock>providerGroup()
        .lifeCache(100, TimeUnit.MILLISECONDS)
        .withKey("ephemeralMock");

    saveMock(MESSAGE_GROUP);

    TestObserver<Reply<Mock>> observer = Single.just(new Mock(MESSAGE_GROUP))
        .compose(cacheProvider.readWithLoaderAsReply(MESSAGE_GROUP))
        .test();
    observer.awaitTerminalEvent();

    observer.assertNoErrors();
    observer.assertValueCount(1);
    assertThat(observer.values()
        .get(0).getSource(), is(Source.MEMORY));

    waitTime(200);

    observer = Single.just(new Mock(MESSAGE_GROUP))
        .compose(cacheProvider.readWithLoaderAsReply(MESSAGE_GROUP))
        .test();
    observer.awaitTerminalEvent();

    observer.assertNoErrors();
    observer.assertValueCount(1);
    assertThat(observer.values()
        .get(0).getSource(), is(Source.CLOUD));
  }

  @Test public void Verify_Pagination() {
    ProviderGroup<Mock> mockProvider = reactiveCache.<Mock>providerGroup()
        .withKey("mocks");

    TestObserver<Mock> observer;

    Mock mockPage1 = new Mock("1");

    observer = Single.just(mockPage1)
        .compose(mockProvider.replace("1"))
        .test();

    observer.awaitTerminalEvent();

    Mock mockPage2 = new Mock("2");

    observer = Single.just(mockPage2)
        .compose(mockProvider.replace("2"))
        .test();

    observer.awaitTerminalEvent();

    Mock mockPage3 = new Mock("3");

    observer = Single.just(mockPage3)
        .compose(mockProvider.replace("3"))
        .test();
    observer.awaitTerminalEvent();

    observer = mockProvider.read("1").test();
    observer.awaitTerminalEvent();
    assertThat(observer.values().get(0).getMessage(), is("1"));

    observer = mockProvider.read("2").test();
    observer.awaitTerminalEvent();
    assertThat(observer.values().get(0).getMessage(), is("2"));

    observer = mockProvider.read("3").test();
    observer.awaitTerminalEvent();
    assertThat(observer.values().get(0).getMessage(), is("3"));
  }

  private void saveMock(String messageGroup) {
    Single.just(new Mock(messageGroup))
        .compose(cacheProvider.replace(messageGroup))
        .test()
        .awaitTerminalEvent();
  }

  private void verifyMockCached(String messageGroup) {
    TestObserver<Mock> observer = cacheProvider.read(messageGroup).test();
    observer.awaitTerminalEvent();

    observer.assertNoErrors();
    observer.assertValueCount(1);
    assertThat(observer.values()
        .get(0).getMessage(), is(messageGroup));
  }

  private void verifyNoMockCached(String group) {
    TestObserver<Mock> observer = cacheProvider.read(group).test();
    observer.awaitTerminalEvent();

    observer.assertError(RxCacheException.class);
    observer.assertNoValues();
    observer.onComplete();
  }

  private void waitTime(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }


}
