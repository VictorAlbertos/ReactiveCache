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

package io.reactivecache2.migration;

import io.reactivecache2.Jolyglot$;
import io.reactivecache2.ReactiveCache;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import io.rx_cache2.MigrationCache;
import io.rx_cache2.RxCacheException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public final class MigrationsTest {
  @ClassRule static public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test public void _1_Populate_Mocks() {
    populateMocks();
  }

  @Test public void _2_When_Migrations_Working_Request_Will_Be_Hold_Until_Finish() {
    int countFiles = temporaryFolder.getRoot().listFiles().length;
    assert countFiles > 0;

    ReactiveCache reactiveCache = new ReactiveCache.Builder()
        .migrations(Arrays.asList(
            new MigrationCache(1, new Class[]{Mock1.class})
        ))
        .using(temporaryFolder.getRoot(), Jolyglot$.newInstance());

    TestObserver<List<Mock1>> testObserver = reactiveCache.<List<Mock1>>provider()
        .withKey("getMocks")
        .read()
        .test();

    testObserver.awaitTerminalEvent();
    testObserver.assertNoValues();
    testObserver.assertError(RxCacheException.class);
  }


  private void populateMocks() {
    ReactiveCache reactiveCache = new ReactiveCache.Builder()
        .using(temporaryFolder.getRoot(), Jolyglot$.newInstance());

    TestObserver<List<Mock1>> testObserver =  getMocks()
        .compose(reactiveCache.<List<Mock1>>provider()
            .withKey("getMocks")
            .readWithLoader())
        .test();

    testObserver.awaitTerminalEvent();
    assertThat(testObserver.values().get(0).size(), is(SIZE_MOCKS));
  }

  private static int SIZE_MOCKS = 1000;
  private Observable<List<Mock1>> getMocks() {
    List<Mock1> mocks = new ArrayList<>();

    for (int i = 0; i < SIZE_MOCKS; i++) {
      mocks.add(new Mock1());
    }

    return Observable.just(mocks);
  }

  public static class Mock1 {
    private final String payload = "Lorem Ipsum is simply dummy text of the printing and " +
        "typesetting industry. Lorem Ipsum has been the industry's standard dummy text " +
        "ever since the 1500s, when an unknown printer took a galley of type and scrambled " +
        "it to make a type specimen book. It has survived not only five centuries";

    public Mock1() {}

    public String getPayload() {
      return payload;
    }
  }
}
