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

package io.reactivecache2.encript;

import io.reactivecache2.Jolyglot$;
import io.reactivecache2.Mock;
import io.reactivecache2.ProviderGroup;
import io.reactivecache2.ReactiveCache;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.rx_cache2.Reply;
import io.rx_cache2.Source;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.MethodSorters;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class EncryptGroupTest {
  private final static int SIZE = 1000;
  @ClassRule public static TemporaryFolder temporaryFolder = new TemporaryFolder();
  private ProviderGroup<List<Mock>> mocksEncryptedProvider;
  private ProviderGroup<List<Mock>> mocksNoEncryptedProvider;
  private final static String GROUP = "GROUP";

  @Before public void init() {
    ReactiveCache reactiveCache = new ReactiveCache.Builder()
        .encrypt("myStrongKey-1234")
        .using(temporaryFolder.getRoot(), Jolyglot$.newInstance());

    mocksEncryptedProvider = reactiveCache.<List<Mock>>providerGroup()
        .encrypt(true)
        .withKey("mocksEncryptedProvider");
    mocksNoEncryptedProvider = reactiveCache.<List<Mock>>providerGroup()
        .withKey("mocksNoEncryptedProvider");
  }

  @Test public void _00_Save_Record_On_Disk_In_Order_To_Test_Following_Tests() {
    TestObserver<Reply<List<Mock>>> observer = createSingleMocks(SIZE)
        .compose(mocksEncryptedProvider.readWithLoaderAsReply(GROUP))
        .test();
    observer.awaitTerminalEvent();

    observer = createSingleMocks(SIZE)
        .compose(mocksNoEncryptedProvider.readWithLoaderAsReply(GROUP))
        .test();

    observer.awaitTerminalEvent();
  }

  @Test public void _01_When_Encrypted_Record_Has_Been_Persisted_And_Memory_Has_Been_Destroyed_Then_Retrieve_From_Disk() {
    TestObserver<Reply<List<Mock>>> observer = Single.<List<Mock>>just(new ArrayList<>())
        .compose(mocksEncryptedProvider.readWithLoaderAsReply(GROUP))
        .test();
    observer.awaitTerminalEvent();

    Reply<List<Mock>> reply = observer.values().get(0);
    assertThat(reply.getSource(), is(Source.PERSISTENCE));
    assertThat(reply.isEncrypted(), is(true));
  }

  @Test public void _02_Verify_Encrypted_Does_Not_Propagate_To_Other_Providers() {
    TestObserver<Reply<List<Mock>>> observer = createSingleMocks(SIZE)
        .compose(mocksEncryptedProvider.readWithLoaderAsReply(GROUP))
        .test();

    observer.awaitTerminalEvent();

    Reply<List<Mock>> reply = observer.values().get(0);
    assertThat(reply.getSource(), is(Source.PERSISTENCE));
    assertThat(reply.isEncrypted(), is(true));

    observer =  createSingleMocks(SIZE)
        .compose(mocksNoEncryptedProvider.readWithLoaderAsReply(GROUP))
        .test();

    observer.awaitTerminalEvent();

    reply = observer.values().get(0);
    assertThat(reply.getSource(), is(Source.PERSISTENCE));
    assertThat(reply.isEncrypted(), is(false));
  }


  private Single<List<Mock>> createSingleMocks(int size) {
    long currentTime = System.currentTimeMillis();

    List<Mock> mocks = new ArrayList(size);
    for (int i = 0; i < size; i++) {
      mocks.add(new Mock("mock"+currentTime));
    }

    return Single.just(mocks);
  }
}
