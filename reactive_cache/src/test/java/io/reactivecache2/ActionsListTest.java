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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public abstract class ActionsListTest {

  @Test public void Add_All() {
    checkInitialState();
    addAll(10);
  }

  @Test public void Add_All_First() {
    checkInitialState();
    addAll(10);

    actions()
        .addAllFirst(Arrays.asList(new Mock("11"), new Mock("12")))
        .test()
        .awaitTerminalEvent();

    List<Mock> mocks = cache().test().values().get(0);
    assertThat(mocks.size(), is(12));
    assertThat(mocks.get(0).getMessage(), is("11"));
    assertThat(mocks.get(1).getMessage(), is("12"));
  }

  @Test public void Add_All_Last() {
    checkInitialState();
    addAll(10);

    actions()
        .addAllLast(Arrays.asList(new Mock("11"), new Mock("12")))
        .test()
        .awaitTerminalEvent();

    List<Mock> mocks = cache().test().values().get(0);
    assertThat(mocks.size(), is(12));
    assertThat(mocks.get(10).getMessage(), is("11"));
    assertThat(mocks.get(11).getMessage(), is("12"));
  }

  @Test public void Add_First() {
    checkInitialState();

    actions()
        .addFirst(new Mock("1"))
        .test()
        .awaitTerminalEvent();

    List<Mock> mocks = cache().test().values().get(0);
    assertThat(mocks.size(), is(1));
    assertThat(mocks.get(0).getMessage(), is("1"));
  }

  @Test public void Add_Last() {
    checkInitialState();
    addAll(10);

    actions()
        .addLast(new Mock("11"))
        .test()
        .awaitTerminalEvent();

    List<Mock> mocks = cache().test().values().get(0);
    assertThat(mocks.size(), is(11));
    assertThat(mocks.get(10).getMessage(), is("11"));
  }

  @Test public void Add() {
    checkInitialState();
    addAll(10);

    actions()
        .add((position, count) -> position == 5, new Mock("6_added"))
        .test()
        .awaitTerminalEvent();

    List<Mock> mocks = cache().test().values().get(0);
    assertThat(mocks.size(), is(11));
    assertThat(mocks.get(5).getMessage(), is("6_added"));
  }

  @Test public void EvictFirst() {
    checkInitialState();
    addAll(10);

    actions()
        .evictFirst()
        .test()
        .awaitTerminalEvent();

    List<Mock> mocks = cache().test().values().get(0);
    assertThat(mocks.size(), is(9));
    assertThat(mocks.get(0).getMessage(), is("1"));
  }

  @Test public void EvictFirstN() {
    checkInitialState();
    addAll(10);

    actions()
        .evictFirstN(4)
        .test()
        .awaitTerminalEvent();

    List<Mock> mocks = cache().test().values().get(0);
    assertThat(mocks.size(), is(6));
    assertThat(mocks.get(0).getMessage(), is("4"));
  }

  @Test public void EvictFirstExposingCount() {
    checkInitialState();
    addAll(10);

    //do not evict
    actions()
        .evictFirst(count -> count > 10)
        .test()
        .awaitTerminalEvent();

    List<Mock> mocks = cache().test().values().get(0);
    assertThat(mocks.size(), is(10));

    //evict
    actions()
        .evictFirst(count -> count > 9)
        .test()
        .awaitTerminalEvent();

    mocks = cache().test().values().get(0);
    assertThat(mocks.size(), is(9));
    assertThat(mocks.get(0).getMessage(), is("1"));
  }

  @Test public void EvictFirstNExposingCount() {
    checkInitialState();
    addAll(10);

    //do not evict
    actions()
        .evictFirstN(count -> count > 10, 5)
        .test()
        .awaitTerminalEvent();

    List<Mock> mocks = cache().test().values().get(0);
    assertThat(mocks.size(), is(10));

    //evict
    actions()
        .evictFirstN(count -> count > 9, 5)
        .test()
        .awaitTerminalEvent();

    mocks = cache().test().values().get(0);
    assertThat(mocks.size(), is(5));
    assertThat(mocks.get(0).getMessage(), is("5"));
    assertThat(mocks.get(1).getMessage(), is("6"));
  }

  @Test public void EvictLast() {
    checkInitialState();
    addAll(10);

    actions()
        .evictLast()
        .test()
        .awaitTerminalEvent();

    List<Mock> mocks = cache().test().values().get(0);
    assertThat(mocks.size(), is(9));
    assertThat(mocks.get(8).getMessage(), is("8"));
  }

  @Test public void EvictLastN() {
    checkInitialState();
    addAll(10);

    actions()
        .evictLastN(4)
        .test()
        .awaitTerminalEvent();

    List<Mock> mocks = cache().test().values().get(0);
    assertThat(mocks.size(), is(6));
    assertThat(mocks.get(0).getMessage(), is("0"));
  }

  @Test public void EvictLastExposingCount() {
    checkInitialState();
    addAll(10);

    //do not evict
    actions()
        .evictLast(count -> count > 10)
        .test()
        .awaitTerminalEvent();

    List<Mock> mocks = cache().test().values().get(0);
    assertThat(mocks.size(), is(10));

    //evict
    actions()
        .evictLast(count -> count > 9)
        .test()
        .awaitTerminalEvent();

    mocks = cache().test().values().get(0);
    assertThat(mocks.size(), is(9));
    assertThat(mocks.get(8).getMessage(), is("8"));
  }

  @Test public void EvictLastNExposingCount() {
    checkInitialState();
    addAll(10);

    //do not evict
    actions()
        .evictLastN(count -> count > 10, 5)
        .test()
        .awaitTerminalEvent();

    List<Mock> mocks = cache().test().values().get(0);
    assertThat(mocks.size(), is(10));

    //evict
    actions()
        .evictLastN(count -> count > 9, 5)
        .test()
        .awaitTerminalEvent();

    mocks = cache().test().values().get(0);
    assertThat(mocks.size(), is(5));
    assertThat(mocks.get(0).getMessage(), is("0"));
    assertThat(mocks.get(1).getMessage(), is("1"));
  }

  @Test public void EvictExposingElementCurrentIteration() {
    checkInitialState();
    addAll(10);

    actions()
        .evict(element -> element.getMessage().equals("3"))
        .test()
        .awaitTerminalEvent();

    List<Mock> mocks = cache().test().values().get(0);
    assertThat(mocks.size(), is(9));
    assertThat(mocks.get(3).getMessage(), is("4"));
  }

  @Test public void EvictExposingCountAndPositionAndElementCurrentIteration() {
    checkInitialState();
    addAll(10);

    //do not evict
    actions()
        .evict((position, count, element) -> count > 10 && element.getMessage().equals("3"))
        .test()
        .awaitTerminalEvent();

    List<Mock> mocks = cache().test().values().get(0);
    assertThat(mocks.size(), is(10));
    assertThat(mocks.get(3).getMessage(), is("3"));

    //evict
    actions()
        .evict((position, count, element) -> count > 9 && element.getMessage().equals("3"))
        .test()
        .awaitTerminalEvent();

    mocks = cache().test().values().get(0);
    assertThat(mocks.size(), is(9));
    assertThat(mocks.get(3).getMessage(), is("4"));
  }

  @Test public void EvictIterable() {
    checkInitialState();
    addAll(10);

    actions()
        .evictIterable(
            (position, count, element) -> element.getMessage().equals("2") || element.getMessage()
                .equals("3"))
        .test()
        .awaitTerminalEvent();

    List<Mock> mocks = cache().test().values().get(0);
    assertThat(mocks.size(), is(8));
    assertThat(mocks.get(2).getMessage(), is("4"));
    assertThat(mocks.get(3).getMessage(), is("5"));
  }

  @Test public void EvictAllKeepingFirstN() {
    checkInitialState();
    addAll(10);

    actions()
        .evictAllKeepingFirstN(3)
        .test()
        .awaitTerminalEvent();

    List<Mock> mocks = cache().test().values().get(0);
    assertThat(mocks.size(), is(3));
    assertThat(mocks.get(0).getMessage(), is("0"));
  }

  @Test public void EvictAllKeepingLastN() {
    checkInitialState();
    addAll(10);

    actions()
        .evictAllKeepingLastN(7)
        .test()
        .awaitTerminalEvent();

    List<Mock> mocks = cache().test().values().get(0);
    assertThat(mocks.size(), is(7));
    assertThat(mocks.get(0).getMessage(), is("3"));
  }

  @Test public void UpdateExposingElementCurrentIteration() {
    checkInitialState();
    addAll(10);

    actions()
        .update(element -> element.getMessage().equals("5"), element -> {
          element.setMessage("5_updated");
          return element;
        })
        .test()
        .awaitTerminalEvent();

    List<Mock> mocks = cache().test().values().get(0);
    assertThat(mocks.size(), is(10));
    assertThat(mocks.get(5).getMessage(), is("5_updated"));
  }

  @Test public void UpdateExposingCountAndPositionAndElementCurrentIteration() {
    checkInitialState();
    addAll(10);

    //do not evict
    actions()
        .update((position, count, element) -> count > 10 && element.getMessage().equals("5"),
            element -> {
              element.setMessage("5_updated");
              return element;
            })
        .test()
        .awaitTerminalEvent();

    List<Mock> mocks = cache().test().values().get(0);
    assertThat(mocks.size(), is(10));
    assertThat(mocks.get(5).getMessage(), is("5"));

    //evict
    actions()
        .update((position, count, element) -> count > 9 && element.getMessage().equals("5"),
            element -> {
              element.setMessage("5_updated");
              return element;
            })
        .test()
        .awaitTerminalEvent();

    mocks = cache().test().values().get(0);
    assertThat(mocks.size(), is(10));
    assertThat(mocks.get(5).getMessage(), is("5_updated"));
  }

  @Test public void UpdateIterableExposingElementCurrentIteration() {
    checkInitialState();
    addAll(10);

    actions()
        .updateIterable(
            element -> element.getMessage().equals("5") || element.getMessage().equals("6"),
            element -> {
              element.setMessage("5_or_6_updated");
              return element;
            })
        .test()
        .awaitTerminalEvent();

    List<Mock> mocks = cache().test().values().get(0);
    assertThat(mocks.size(), is(10));
    assertThat(mocks.get(5).getMessage(), is("5_or_6_updated"));
    assertThat(mocks.get(6).getMessage(), is("5_or_6_updated"));
  }

  @Test public void UpdateIterableExposingCountAndPositionAndElementCurrentIteration() {
    checkInitialState();
    addAll(10);

    //do not evict
    actions()
        .updateIterable(
            (position, count, element) -> count > 10 && (element.getMessage().equals("5") || element.getMessage()
                .equals("6")), element -> {
                  element.setMessage("5_or_6_updated");
                  return element;
                })
        .test()
        .awaitTerminalEvent();

    List<Mock> mocks = cache().test().values().get(0);
    assertThat(mocks.size(), is(10));
    assertThat(mocks.get(5).getMessage(), is("5"));
    assertThat(mocks.get(6).getMessage(), is("6"));

    //evict
    actions()
        .updateIterable(
            (position, count, element) -> count > 9 && (element.getMessage().equals("5") || element.getMessage()
                .equals("6")), element -> {
                  element.setMessage("5_or_6_updated");
                  return element;
                })
        .test()
        .awaitTerminalEvent();

    mocks = cache().test().values().get(0);
    assertThat(mocks.size(), is(10));
    assertThat(mocks.get(5).getMessage(), is("5_or_6_updated"));
    assertThat(mocks.get(6).getMessage(), is("5_or_6_updated"));
  }

  private void checkInitialState() {
    TestObserver<List<Mock>> testObserver = cache().test();
    testObserver.awaitTerminalEvent();

    if (testObserver.values().isEmpty()) return;

    List<Mock> mocks = testObserver.values().get(0);
    assertThat(mocks.size(), is(0));
  }

  private void addAll(int count) {
    List<Mock> mocks = new ArrayList<>();

    for (int i = 0; i < count; i++) {
      mocks.add(new Mock(String.valueOf(i)));
    }

    actions()
        .addAll((position, count1) -> true, mocks)
        .test()
        .assertNoErrors()
        .awaitTerminalEvent();

    assertThat(cache().test().values().get(0).size(),
        is(count));
  }

  protected abstract ActionsList<Mock> actions();

  protected abstract Single<List<Mock>> cache();
}
