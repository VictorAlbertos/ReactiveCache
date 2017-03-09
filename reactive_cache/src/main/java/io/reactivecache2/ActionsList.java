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

import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Provides a set of entries in order to perform write operations on lists with providers in a more
 * easy and safely way.
 */
public class ActionsList<T> {
  protected Single<List<T>> cache;
  protected final Evict<T> evict;

  ActionsList(Evict<T> evict, Single<List<T>> cache) {
    this.evict = evict;
    this.cache = cache;
  }

  /**
   * Static accessor to create an instance of Actions in order follow the "builder" pattern.
   *
   * @param evict The implementation of Evict interface which allow to persists the changes.
   * @param cache An single which is the result of calling the provider without evicting its data.
   * @param <T> the type of the element of the list to be processed.
   * @return an instance of Actions.
   */
  static <T> ActionsList<T> with(Evict<T> evict, Single<List<T>> cache) {
    return new ActionsList<>(evict, cache);
  }

  /**
   * Func2 will be called for every iteration until its condition returns true. When true, the
   * element is added to the cache at the position of the current iteration.
   *
   * @param func2 exposes the position of the current iteration and the count of elements in the
   * cache.
   * @param element the object to addOrUpdate to the cache.
   * @return Completable
   */
  public Completable add(Func2 func2, T element) {
    return addAll(func2, Arrays.asList(element));
  }

  /**
   * Add the object at the first position of the cache.
   *
   * @param element the object to addOrUpdate to the cache.
   * @return Completable
   */
  public Completable addFirst(T element) {
    return addAll((position, count) -> position == 0, Arrays.asList(element));
  }

  /**
   * Add the object at the last position of the cache.
   *
   * @param element the object to addOrUpdate to the cache.
   * @return Completable
   */
  public Completable addLast(T element) {
    return addAll((position, count) -> position == count, Arrays.asList(element));
  }

  /**
   * Add the objects at the first position of the cache.
   *
   * @param elements the objects to addOrUpdate to the cache.
   * @return Completable
   */
  public Completable addAllFirst(List<T> elements) {
    return addAll((position, count) -> position == 0, elements);
  }

  /**
   * Add the objects at the last position of the cache.
   *
   * @param elements the objects to addOrUpdate to the cache.
   * @return Completable
   */
  public Completable addAllLast(List<T> elements) {
    return addAll((position, count) -> position == count, elements);
  }

  /**
   * Func2 will be called for every iteration until its condition returns true. When true, the
   * elements are added to the cache at the position of the current iteration.
   *
   * @param func2 exposes the position of the current iteration and the count of elements in the
   * cache.
   * @param elements the objects to addOrUpdate to the cache.
   * @return Completable
   */
  public Completable addAll(final Func2 func2, final List<T> elements) {
    return evict.call(cache.map(items -> {
      int count = items.size();

      for (int position = 0; position <= count; position++) {
        if (func2.call(position, count)) {
          items.addAll(position, elements);
          break;
        }
      }

      return items;
    })).toCompletable();
  }

  /**
   * Evict object at the first position of the cache
   *
   * @return Completable
   */
  public Completable evictFirst() {
    return evict((position, count, element) -> position == 0);
  }

  /**
   * Evict as much objects as requested by n param starting from the first position.
   *
   * @param n the amount of elements to evict.
   * @return Completable
   */
  public Completable evictFirstN(final int n) {
    return evictFirstN(count -> true, n);
  }

  /**
   * Evict object at the last position of the cache.
   *
   * @return Completable
   */
  public Completable evictLast() {
    return evict((position, count, element) -> position == count - 1);
  }

  /**
   * Evict as much objects as requested by n param starting from the last position.
   *
   * @param n the amount of elements to evict.
   * @return Completable
   */
  public Completable evictLastN(final int n) {
    return evictLastN(count -> true, n);
  }

  /**
   * Evict object at the first position of the cache.
   *
   * @param func1Count exposes the count of elements in the cache.
   * @return Completable
   */
  public Completable evictFirst(final Func1Count func1Count) {
    return evict((position, count, element) -> position == 0 && func1Count.call(count));
  }

  /**
   * Evict as much objects as requested by n param starting from the first position.
   *
   * @param func1Count exposes the count of elements in the cache.
   * @param n the amount of elements to evict.
   * @return Completable
   */
  public Completable evictFirstN(final Func1Count func1Count, final int n) {
    return evictIterable((position, count, element) -> position < n && func1Count.call(count));
  }

  /**
   * Evict object at the last position of the cache.
   *
   * @param func1Count exposes the count of elements in the cache.
   * @return Completable
   */
  public Completable evictLast(final Func1Count func1Count) {
    return evict((position, count, element) -> position == count - 1 && func1Count.call(count));
  }

  private boolean startToEvict;

  /**
   * Evict as much objects as requested by n param starting from the last position.
   *
   * @param func1Count exposes the count of elements in the cache.
   * @param n the amount of elements to evict.
   * @return Completable
   */
  public Completable evictLastN(final Func1Count func1Count, final int n) {
    startToEvict = false;
    return evictIterable((position, count, element) -> {
      if (!startToEvict) startToEvict = count - position == n;

      if (startToEvict) {
        return count - position <= n && func1Count.call(count);
      } else {
        return false;
      }
    });
  }

  /**
   * Func1Element will be called for every iteration until its condition returns true. When true,
   * the element of the current iteration is evicted from the cache.
   *
   * @param func1 exposes the element of the current iteration.
   * @return Completable
   */
  public Completable evict(final Func1<T> func1) {
    return evict((position, count, element) -> func1.call(element));
  }

  /**
   * Func3 will be called for every iteration until its condition returns true. When true, the
   * element of the current iteration is evicted from the cache.
   *
   * @param func3 exposes the position of the current iteration, the count of elements in the cache
   * and the element of the current iteration.
   * @return Completable
   */
  public Completable evict(final Func3<T> func3) {
    return evict.call(cache.map(elements -> {
      int count = elements.size();

      for (int position = 0; position < count; position++) {
        if (func3.call(position, count, elements.get(position))) {
          elements.remove(position);
          break;
        }
      }

      return elements;
    })).toCompletable();
  }

  /**
   * Evict elements from the cache starting from the first position until its count is equal to the
   * value specified in n param.
   *
   * @param n the amount of elements to keep from evict.
   * @return Completable
   */
  public Completable evictAllKeepingFirstN(final int n) {
    return evictIterable((position, count, element) -> {
      int positionToStartEvicting = count - (count - n);
      return position >= positionToStartEvicting;
    });
  }

  /**
   * Evict elements from the cache starting from the last position until its count is equal to the
   * value specified in n param.
   *
   * @param n the amount of elements to keep from evict.
   * @return Completable
   */
  public Completable evictAllKeepingLastN(final int n) {
    return evictIterable((position, count, element) -> {
      int elementsToEvict = count - n;
      return position < elementsToEvict;
    });
  }

  /**
   * Func3 will be called for every iteration. When true, the element of the current iteration is
   * evicted from the cache.
   *
   * @param func3 exposes the position of the current iteration, the count of elements in the cache
   * and the element of the current iteration.
   * @return Completable
   */
  public Completable evictIterable(final Func3<T> func3) {
    return evict.call(cache.map(elements -> {
      int count = elements.size();

      for (int position = 0; position < count; position++) {
        if (func3.call(position, count, elements.get(position))) {
          elements.set(position, null);
        }
      }

      elements.removeAll(Collections.singleton(null));
      return elements;
    })).toCompletable();
  }

  /**
   * Func1Element will be called for every iteration until its condition returns true. When true,
   * the element of the current iteration is updated.
   *
   * @param func1 exposes the element of the current iteration.
   * @param replace exposes the original element and expects back the one modified.
   * @return Completable
   */
  public Completable update(final Func1<T> func1, Replace<T> replace) {
    return update((position, count, element) -> func1.call(element), replace);
  }

  /**
   * Func3 will be called for every iteration until its condition returns true. When true, the
   * element of the current iteration is updated.
   *
   * @param func3 exposes the position of the current iteration, the count of elements in the cache
   * and the element of the current iteration.
   * @param replace exposes the original element and expects back the one modified.
   * @return Completable
   */
  public Completable update(final Func3<T> func3, final Replace<T> replace) {
    return evict.call(cache.map(elements -> {
      int count = elements.size();

      for (int position = 0; position < count; position++) {
        if (func3.call(position, count, elements.get(position))) {
          elements.set(position, replace.call(elements.get(position)));
          break;
        }
      }

      return elements;
    })).toCompletable();
  }

  /**
   * Func1Element will be called for every. When true, the element of the current iteration is
   * updated.
   *
   * @param func1 exposes the element of the current iteration.
   * @param replace exposes the original element and expects back the one modified.
   * @return Completable
   */
  public Completable updateIterable(final Func1<T> func1, Replace<T> replace) {
    return updateIterable((position, count, element) -> func1.call(element), replace);
  }

  /**
   * Func3 will be called for every iteration. When true, the element of the current iteration is
   * updated.
   *
   * @param func3 exposes the position of the current iteration, the count of elements in the cache
   * and the element of the current iteration.
   * @param replace exposes the original element and expects back the one modified.
   * @return Completable
   */
  public Completable updateIterable(final Func3<T> func3, final Replace<T> replace) {
    return evict.call(cache.map(elements -> {
      int count = elements.size();

      for (int position = 0; position < count; position++) {
        if (func3.call(position, count, elements.get(position))) {
          elements.set(position, replace.call(elements.get(position)));
        }
      }

      return elements;
    })).toCompletable();
  }

  public interface Evict<T> {
    Single<List<T>> call(final Single<List<T>> elements);
  }

  public interface Func1Count {
    boolean call(final int count);
  }

  public interface Func1<T> {
    boolean call(final T element);
  }

  public interface Func2 {
    boolean call(final int position, final int count);
  }

  public interface Func3<T> {
    boolean call(final int position, final int count, final T element);
  }

  public interface Replace<T> {
    T call(T element);
  }
}
