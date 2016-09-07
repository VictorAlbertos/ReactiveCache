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

import io.reactivex.exceptions.CompositeException;
import io.rx_cache.RxCacheException;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public final class ExceptionAdapterTest {
  private ExceptionAdapter exceptionAdapter;

  @Before public void setUp() {
    exceptionAdapter = new ExceptionAdapter();
  }

  @Test public void When_Exception_Is_Not_RxCache_Exception_Then_Return_Observable_Error() {
    exceptionAdapter.completeOnRxCacheLoaderError(new RuntimeException())
        .test()
        .assertNoValues()
        .assertNotComplete()
        .assertError(RuntimeException.class);
  }

  @Test
  public void When_CompositeException_Is_Not_RxCache_Exception_Then_Return_Observable_Error() {
    CompositeException compositeException = new CompositeException(new RuntimeException());
    exceptionAdapter.completeOnRxCacheLoaderError(compositeException)
        .test()
        .assertNoValues()
        .assertNotComplete()
        .assertError(RuntimeException.class);
  }

  @Test public void When_Exception_Is_RxCache_Exception_Then_Return_Completable() {
    exceptionAdapter.completeOnRxCacheLoaderError(new RxCacheException(""))
        .test()
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();
  }

  @Test public void When_CompositeException_Is_RxCache_Exception_Then_Return_Completable() {
    CompositeException compositeException = new CompositeException(new RuntimeException(),
        new RxCacheException(""), new RuntimeException());

    exceptionAdapter.completeOnRxCacheLoaderError(compositeException)
        .test()
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();
  }

  @Test public void When_StripPlaceholderLoaderException_Then_Strip_It() {
    exceptionAdapter.stripPlaceholderLoaderException(new RuntimeException())
        .test()
        .assertNoValues()
        .assertNotComplete()
        .assertError(RuntimeException.class);
  }

  @Test public void When_StripPlaceholderLoaderException_Composite_Then_Strip_It() {
    CompositeException compositeException = new CompositeException(new RuntimeException(),
        new ExceptionAdapter.PlaceHolderLoader(), new RuntimeException());

    List<Throwable> errors = exceptionAdapter.stripPlaceholderLoaderException(compositeException)
        .test()
        .assertNoValues()
        .assertNotComplete()
        .errors();
    assertThat(1, is(errors.size()));

    List<Throwable> compositeErrors = ((CompositeException) errors.get(0)).getExceptions();
    assertThat(2, is(compositeErrors.size()));
    assertThat(RuntimeException.class, is(equalTo(compositeErrors.get(0).getClass())));
    assertThat(RuntimeException.class, is(equalTo(compositeErrors.get(1).getClass())));
  }
}
