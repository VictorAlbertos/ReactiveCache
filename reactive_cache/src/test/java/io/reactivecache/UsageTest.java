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
import io.rx_cache.Source;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import rx.Observable;
import rx.observers.TestSubscriber;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public final class UsageTest {
  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();
  private ReactiveCache reactiveCache;

  @Before public void setUp() {
    reactiveCache = new ReactiveCache.Builder()
        .using(temporaryFolder.getRoot(), Jolyglot$.newInstance());
  }

  @Test public void Verify_User() {
    UserRepository userRepository = new UserRepository(new ApiUser(), reactiveCache);

    TestSubscriber<Boolean> subscriberIsLogged = new TestSubscriber<>();
    userRepository.isLogged().subscribe(subscriberIsLogged);
    subscriberIsLogged.awaitTerminalEvent();

    assertThat(subscriberIsLogged.getOnNextEvents().get(0),
        is(false));
    subscriberIsLogged.assertValueCount(1);
    subscriberIsLogged.assertNoErrors();
    subscriberIsLogged.assertCompleted();

    TestSubscriber<User> subscriberLogin = new TestSubscriber<>();
    userRepository.login("")
        .subscribe(subscriberLogin);
    subscriberLogin.awaitTerminalEvent();

    subscriberIsLogged = new TestSubscriber<>();
    userRepository.isLogged()
        .subscribe(subscriberIsLogged);
    subscriberIsLogged.awaitTerminalEvent();

    assertThat(subscriberIsLogged.getOnNextEvents().get(0),
        is(true));

    TestSubscriber<User> subscriberProfile = new TestSubscriber<>();
    userRepository.profile()
        .subscribe(subscriberProfile);
    subscriberProfile.awaitTerminalEvent();

    subscriberProfile.assertNoErrors();
    subscriberProfile.assertValueCount(1);
    subscriberProfile.assertCompleted();
    assertNotNull(subscriberProfile.getOnNextEvents().get(0));

    TestSubscriber<User> subscriberUpdate = new TestSubscriber<>();
    userRepository.updateUserName("aNewName")
        .subscribe(subscriberUpdate);
    subscriberUpdate.awaitTerminalEvent();

    subscriberUpdate.assertCompleted();
    subscriberUpdate.assertNoErrors();
    subscriberUpdate.assertValueCount(1);

    subscriberProfile = new TestSubscriber<>();
    userRepository.profile()
        .subscribe(subscriberProfile);
    subscriberProfile.awaitTerminalEvent();

    subscriberProfile.assertNoErrors();
    subscriberProfile.assertValueCount(1);
    subscriberProfile.assertCompleted();
    assertNotNull(subscriberProfile.getOnNextEvents().get(0).getName(),
        is("aNewName"));

    TestSubscriber<Void> subscriberLogout = new TestSubscriber<>();
    userRepository.logout()
        .subscribe(subscriberLogout);
    subscriberLogout.awaitTerminalEvent();

    subscriberLogout.assertNoErrors();
    subscriberLogout.assertValueCount(1);
    subscriberLogout.assertCompleted();

    subscriberIsLogged = new TestSubscriber<>();
    userRepository.isLogged().subscribe(subscriberIsLogged);
    subscriberIsLogged.awaitTerminalEvent();

    assertThat(subscriberIsLogged.getOnNextEvents().get(0),
        is(false));
  }

  @Test public void Verify_Tasks() {
    TasksRepository tasksRepository = new TasksRepository(new ApiTasks(), reactiveCache);

    TestSubscriber<Reply<List<Task>>> subscriberTasks = new TestSubscriber<>();
    tasksRepository.tasks(false)
      .subscribe(subscriberTasks);
    subscriberTasks.awaitTerminalEvent();

    subscriberTasks.assertCompleted();
    subscriberTasks.assertNoErrors();
    subscriberTasks.assertValueCount(1);
    assertThat(subscriberTasks.getOnNextEvents().get(0).getSource(),
        is(Source.CLOUD));

    subscriberTasks = new TestSubscriber<>();
    tasksRepository.tasks(false)
        .subscribe(subscriberTasks);
    subscriberTasks.awaitTerminalEvent();

    subscriberTasks.assertCompleted();
    subscriberTasks.assertNoErrors();
    subscriberTasks.assertValueCount(1);
    assertThat(subscriberTasks.getOnNextEvents().get(0).getSource(),
        is(Source.MEMORY));

    subscriberTasks = new TestSubscriber<>();
    tasksRepository.tasks(true)
        .subscribe(subscriberTasks);
    subscriberTasks.awaitTerminalEvent();

    subscriberTasks.assertCompleted();
    subscriberTasks.assertNoErrors();
    subscriberTasks.assertValueCount(1);
    assertThat(subscriberTasks.getOnNextEvents().get(0).getSource(),
        is(Source.CLOUD));
    assertThat(subscriberTasks.getOnNextEvents().get(0).getData().size(),
        is(0));

    TestSubscriber<Void> subscriberAddTask = new TestSubscriber<>();

    tasksRepository.addTask("", "")
        .subscribe(subscriberAddTask);
    subscriberAddTask.awaitTerminalEvent();

    subscriberAddTask.assertCompleted();
    subscriberAddTask.assertNoErrors();
    subscriberAddTask.assertValueCount(1);

    subscriberTasks = new TestSubscriber<>();
    tasksRepository.tasks(false)
        .subscribe(subscriberTasks);
    subscriberTasks.awaitTerminalEvent();

    subscriberTasks.assertCompleted();
    subscriberTasks.assertNoErrors();
    subscriberTasks.assertValueCount(1);
    assertThat(subscriberTasks.getOnNextEvents().get(0).getSource(),
        is(Source.MEMORY));
    assertThat(subscriberTasks.getOnNextEvents().get(0).getData().size(),
        is(1));

    TestSubscriber<Void> subscriberRemoveTask = new TestSubscriber<>();
    tasksRepository.removeTask(1)
        .subscribe(subscriberRemoveTask);

    subscriberRemoveTask.assertCompleted();
    subscriberRemoveTask.assertNoErrors();
    subscriberRemoveTask.assertValueCount(1);

    subscriberTasks = new TestSubscriber<>();
    tasksRepository.tasks(false)
        .subscribe(subscriberTasks);
    subscriberTasks.awaitTerminalEvent();

    subscriberTasks.assertCompleted();
    subscriberTasks.assertNoErrors();
    subscriberTasks.assertValueCount(1);
    assertThat(subscriberTasks.getOnNextEvents().get(0).getSource(),
        is(Source.MEMORY));
    assertThat(subscriberTasks.getOnNextEvents().get(0).getData().size(),
        is(0));

    subscriberTasks = new TestSubscriber<>();
    tasksRepository.tasks(true)
        .subscribe(subscriberTasks);
    subscriberTasks.awaitTerminalEvent();

    subscriberTasks.assertCompleted();
    subscriberTasks.assertNoErrors();
    subscriberTasks.assertValueCount(1);
    assertThat(subscriberTasks.getOnNextEvents().get(0).getSource(),
        is(Source.CLOUD));
    assertThat(subscriberTasks.getOnNextEvents().get(0).getData().size(),
        is(0));
  }

  @Test public void Verify_Events() {
    EventsRepository eventsRepository = new EventsRepository(new ApiEvents(), reactiveCache);
    TestSubscriber<Reply<List<Event>>> subscriberEvents = new TestSubscriber<>();

    eventsRepository.events(false, 1)
        .subscribe(subscriberEvents);
    subscriberEvents.awaitTerminalEvent();

    subscriberEvents.assertCompleted();
    subscriberEvents.assertNoErrors();
    subscriberEvents.assertValueCount(1);

    assertThat(subscriberEvents.getOnNextEvents().get(0).getData().size(),
        is(1));
    assertThat(subscriberEvents.getOnNextEvents().get(0).getSource(),
        is(Source.CLOUD));

    subscriberEvents = new TestSubscriber<>();

    eventsRepository.events(false, 2)
        .subscribe(subscriberEvents);
    subscriberEvents.awaitTerminalEvent();

    subscriberEvents.assertCompleted();
    subscriberEvents.assertNoErrors();
    subscriberEvents.assertValueCount(1);

    assertThat(subscriberEvents.getOnNextEvents().get(0).getData().size(),
        is(2));
    assertThat(subscriberEvents.getOnNextEvents().get(0).getSource(),
        is(Source.CLOUD));

    subscriberEvents = new TestSubscriber<>();

    eventsRepository.events(false, 1)
        .subscribe(subscriberEvents);
    subscriberEvents.awaitTerminalEvent();

    subscriberEvents.assertCompleted();
    subscriberEvents.assertNoErrors();
    subscriberEvents.assertValueCount(1);

    assertThat(subscriberEvents.getOnNextEvents().get(0).getData().size(),
        is(1));
    assertThat(subscriberEvents.getOnNextEvents().get(0).getSource(),
        is(Source.MEMORY));

    subscriberEvents = new TestSubscriber<>();

    eventsRepository.events(false, 2)
        .subscribe(subscriberEvents);
    subscriberEvents.awaitTerminalEvent();

    subscriberEvents.assertCompleted();
    subscriberEvents.assertNoErrors();
    subscriberEvents.assertValueCount(1);

    assertThat(subscriberEvents.getOnNextEvents().get(0).getData().size(),
        is(2));
    assertThat(subscriberEvents.getOnNextEvents().get(0).getSource(),
        is(Source.MEMORY));
  }

  /**
   * Managing user session.
   */
  static class UserRepository {
    private final Provider<User> cacheProvider;
    private final ApiUser api;

    UserRepository(ApiUser api, ReactiveCache reactiveCache) {
      this.api = api;
      this.cacheProvider = reactiveCache.<User>provider()
          .withKey("user");
    }

    Observable<User> login(String email) {
      return api.loginUser(email)
          .compose(cacheProvider.replace());
    }

    Observable<Boolean> isLogged() {
      return cacheProvider.readNullable()
          .map(user -> user != null);
    }

    Observable<User> profile() {
      return cacheProvider.read();
    }

    Observable<User> updateUserName(String name) {
      return cacheProvider.read()
          .doOnNext(user -> user.setName(name))
          .compose(cacheProvider.replace());
    }

    Observable<Void> logout() {
      return api.logout()
          .flatMap(ignore -> cacheProvider.evict());
    }
  }

  /**
   * Managing tasks.
   */
  class TasksRepository {
    private final Provider<List<Task>> cacheProvider;
    private final ApiTasks api;

    TasksRepository(ApiTasks api, ReactiveCache reactiveCache) {
      this.api = api;
      this.cacheProvider = reactiveCache.<List<Task>>provider()
          .withKey("tasks");
    }

    /**
     * If refresh true, call to the server and replace the cache with the data emitted by the observable.
     * If refresh false, use the cache if data is available otherwise call the server and cached the
     * new emitted data.
     */
    Observable<Reply<List<Task>>> tasks(boolean refresh) {
      return refresh ? api.tasks().compose(cacheProvider.replaceAsReply())
          : api.tasks().compose(cacheProvider.readWithLoaderAsReply());
    }

    Observable<Void> addTask(String name, String desc) {
      return api.addTask(name, desc)
          .flatMap(newTask ->
              cacheProvider.read()
                  .doOnNext(tasks -> tasks.add(newTask)))
          .compose(cacheProvider.replace())
          .map(ignore -> null);
    }

    Observable<Void> removeTask(int id) {
      return api.removeTask(id)
          .flatMap(ignore -> cacheProvider.read())
          .flatMapIterable(tasks -> tasks)
          .filter(task -> task.getId() != id)
          .toList()
          .compose(cacheProvider.replace())
          .map(ignore -> null);
    }
  }

  /**
   * Managing events feed with pagination.
   */
  class EventsRepository {
    private final ProviderGroup<List<Event>> cacheProvider;
    private final ApiEvents apiEvents;

    EventsRepository(ApiEvents apiEvents, ReactiveCache reactiveCache) {
      this.apiEvents = apiEvents;
      this.cacheProvider = reactiveCache.<List<Event>>providerGroup()
          .withKey("events");
    }

    Observable<Reply<List<Event>>> events(boolean refresh, int page) {
      if (refresh) {
        return apiEvents.events(page)
            .compose(cacheProvider.replaceAsReply(page));
      }

      return apiEvents.events(page)
          .compose(cacheProvider.readWithLoaderAsReply(page));
    }
  }

  private static class User {
    String name;

    void setName(String name) {
      this.name = name;
    }

    String getName() {
      return name;
    }
  }

  private static class Task {
    private static int COUNTER;
    private final int id;

    public Task() {
      this.id = ++COUNTER;
    }

    public int getId() {
      return id;
    }
  }

  private static class Event {

  }

  private static class ApiUser {

    public Observable<User> loginUser(String email) {
      return Observable.just(new User());
    }

    public Observable<Void> logout() {
      return Observable.just(null);
    }
  }

  private static class ApiTasks {
    private final List<Task> tasks;

    public ApiTasks() {
      this.tasks = new ArrayList<>();
    }

    public Observable<List<Task>> tasks() {
      return Observable.just(new ArrayList<>(tasks));
    }

    public Observable<Task> addTask(String name, String desc) {
      Task task = new Task();
      tasks.add(task);
      return Observable.just(task);
    }

    public Observable<Void> removeTask(int id) {
      Task candidate = null;
      for (Task task : tasks) {
        if (task.getId() == id) candidate = task;
      }
      tasks.remove(candidate);
      return Observable.just(null);
    }
  }

  private static class ApiEvents {
    private final HashMap<Integer, List<Event>> events = new HashMap<Integer, List<Event>>() {{
      put(1, Arrays.asList(new Event()));
      put(2, Arrays.asList(new Event(), new Event()));
    }};


    public Observable<List<Event>> events(int page) {
      return Observable.just(events.get(page));
    }
  }
}
