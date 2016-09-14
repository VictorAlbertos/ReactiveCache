package io.reactivecache2;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public final class ReactiveCacheTest {
  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();
  private ReactiveCache reactiveCache;

  @Before public void setUp() {
    reactiveCache = new ReactiveCache.Builder()
        .using(temporaryFolder.getRoot(), Jolyglot$.newInstance());
  }

  @Test public void Verify_Evict_All() {
    assertThat(temporaryFolder.getRoot().listFiles().length, is(0));

    Provider<Mock> provider1 = reactiveCache.<Mock>provider()
        .withKey("1");

    ProviderGroup<Mock> provider2 = reactiveCache.<Mock>providerGroup()
        .withKey("2");

    Observable.just(new Mock())
        .compose(provider1.replace())
        .test()
        .awaitTerminalEvent();

    for (int i = 0; i < 50; i++) {
      Observable.just(new Mock())
          .compose(provider2.replace(i))
          .test()
          .awaitTerminalEvent();
    }

    assertThat(temporaryFolder.getRoot().listFiles().length, is(51));

    TestObserver<Void> observer = reactiveCache.evictAll().test();
    observer.awaitTerminalEvent();

    observer.assertComplete();
    observer.assertNoErrors();
    observer.assertNoValues();

    assertThat(temporaryFolder.getRoot().listFiles().length, is(0));
  }
}
