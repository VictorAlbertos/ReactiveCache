package io.reactivecache;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import rx.Observable;
import rx.observers.TestSubscriber;

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

    TestSubscriber<Mock> subscriber = new TestSubscriber<>();
    Observable.just(new Mock())
        .compose(provider1.replace())
        .subscribe(subscriber);
    subscriber.awaitTerminalEvent();


    for (int i = 0; i < 50; i++) {
      subscriber = new TestSubscriber<>();
      Observable.just(new Mock())
          .compose(provider2.replace(i))
          .subscribe(subscriber);
      subscriber.awaitTerminalEvent();
    }

    assertThat(temporaryFolder.getRoot().listFiles().length, is(51));

    TestSubscriber<Void> evictSubscriber = new TestSubscriber<>();
    reactiveCache.evictAll().subscribe(evictSubscriber);
    evictSubscriber.awaitTerminalEvent();

    evictSubscriber.assertCompleted();
    evictSubscriber.assertNoErrors();
    evictSubscriber.assertValueCount(1);

    assertThat(temporaryFolder.getRoot().listFiles().length, is(0));
  }
}
