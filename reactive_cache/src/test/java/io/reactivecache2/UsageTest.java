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

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.rx_cache2.Reply;
import io.rx_cache2.Source;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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

    TestObserver<Boolean> observerIsLogged = userRepository.isLogged().test();
    observerIsLogged.awaitTerminalEvent();

    assertThat(observerIsLogged.values().get(0), is(false));
    observerIsLogged.assertValueCount(1);
    observerIsLogged.assertNoErrors();
    observerIsLogged.assertComplete();

    userRepository.login("")
        .test()
        .awaitTerminalEvent();

    observerIsLogged = userRepository.isLogged().test();
    observerIsLogged.awaitTerminalEvent();

    assertThat(observerIsLogged.values().get(0),
        is(true));

    TestObserver<User> observerProfile = userRepository.profile().test();
    observerProfile.awaitTerminalEvent();

    observerProfile.assertNoErrors();
    observerProfile.assertValueCount(1);
    observerProfile.assertComplete();
    assertNotNull(observerProfile.values().get(0));

    TestObserver<Void> observerUpdate = userRepository.updateUserName("aNewName").test();
    observerUpdate.awaitTerminalEvent();

    observerUpdate.assertComplete();
    observerUpdate.assertNoErrors();
    observerUpdate.assertNoValues();

    observerProfile = userRepository.profile().test();
    observerProfile.awaitTerminalEvent();

    observerProfile.assertNoErrors();
    observerProfile.assertValueCount(1);
    observerProfile.assertComplete();
    assertNotNull(observerProfile.values().get(0).getName(),
        is("aNewName"));

    TestObserver<Void> observerLogout = userRepository.logout().test();
    observerLogout.awaitTerminalEvent();

    observerLogout.assertNoErrors();
    observerLogout.assertNoValues();
    observerLogout.assertComplete();

    observerIsLogged = userRepository.isLogged().test();
    observerIsLogged.awaitTerminalEvent();

    assertThat(observerIsLogged.values().get(0), is(false));
  }

  @Test public void Verify_Tasks() {
    TasksRepository tasksRepository = new TasksRepository(new ApiTasks(), reactiveCache);

    TestObserver<Reply<List<Task>>> observerTasks = tasksRepository.tasks(false).test();
    observerTasks.awaitTerminalEvent();

    observerTasks.assertComplete();
    observerTasks.assertNoErrors();
    observerTasks.assertValueCount(1);
    assertThat(observerTasks.values().get(0).getSource(),
        is(Source.CLOUD));

    observerTasks = tasksRepository.tasks(false).test();
    observerTasks.awaitTerminalEvent();

    observerTasks.assertComplete();
    observerTasks.assertNoErrors();
    observerTasks.assertValueCount(1);
    assertThat(observerTasks.values().get(0).getSource(),
        is(Source.MEMORY));

    observerTasks = tasksRepository.tasks(true).test();
    observerTasks.awaitTerminalEvent();

    observerTasks.assertComplete();
    observerTasks.assertNoErrors();
    observerTasks.assertValueCount(1);
    assertThat(observerTasks.values().get(0).getSource(),
        is(Source.CLOUD));
    assertThat(observerTasks.values().get(0).getData().size(),
        is(0));

    TestObserver<Void> observerAddTask = tasksRepository.addTask("", "").test();
    observerAddTask.awaitTerminalEvent();

    observerAddTask.assertComplete();
    observerAddTask.assertNoErrors();
    observerAddTask.assertNoValues();

    observerTasks = tasksRepository.tasks(false).test();
    observerTasks.awaitTerminalEvent();

    observerTasks.assertComplete();
    observerTasks.assertNoErrors();
    observerTasks.assertValueCount(1);
    assertThat(observerTasks.values().get(0).getSource(), is(Source.MEMORY));
    assertThat(observerTasks.values().get(0).getData().size(), is(1));

    TestObserver<Void> observerRemoveTask = tasksRepository.removeTask(1).test();

    observerRemoveTask.assertComplete();
    observerRemoveTask.assertNoErrors();
    observerRemoveTask.assertNoValues();

    observerTasks = tasksRepository.tasks(false).test();
    observerTasks.awaitTerminalEvent();

    observerTasks.assertComplete();
    observerTasks.assertNoErrors();
    observerTasks.assertValueCount(1);
    assertThat(observerTasks.values().get(0).getSource(), is(Source.MEMORY));
    assertThat(observerTasks.values().get(0).getData().size(), is(0));

    observerTasks = tasksRepository.tasks(true).test();
    observerTasks.awaitTerminalEvent();

    observerTasks.assertComplete();
    observerTasks.assertNoErrors();
    observerTasks.assertValueCount(1);
    assertThat(observerTasks.values().get(0).getSource(), is(Source.CLOUD));
    assertThat(observerTasks.values().get(0).getData().size(), is(0));
  }

  @Test public void Verify_Events() {
    EventsRepository eventsRepository = new EventsRepository(new ApiEvents(), reactiveCache);
    TestObserver<Reply<List<Event>>> observerEvents = eventsRepository.events(false, 1).test();
    observerEvents.awaitTerminalEvent();

    observerEvents.assertComplete();
    observerEvents.assertNoErrors();
    observerEvents.assertValueCount(1);

    assertThat(observerEvents.values().get(0).getData().size(), is(1));
    assertThat(observerEvents.values().get(0).getSource(), is(Source.CLOUD));

    observerEvents = eventsRepository.events(false, 2).test();
    observerEvents.awaitTerminalEvent();

    observerEvents.assertComplete();
    observerEvents.assertNoErrors();
    observerEvents.assertValueCount(1);

    assertThat(observerEvents.values().get(0).getData().size(), is(2));
    assertThat(observerEvents.values().get(0).getSource(), is(Source.CLOUD));

    observerEvents = eventsRepository.events(false, 1).test();
    observerEvents.awaitTerminalEvent();

    observerEvents.assertComplete();
    observerEvents.assertNoErrors();
    observerEvents.assertValueCount(1);

    assertThat(observerEvents.values().get(0).getData().size(), is(1));
    assertThat(observerEvents.values().get(0).getSource(), is(Source.MEMORY));

    observerEvents = eventsRepository.events(false, 2).test();
    observerEvents.awaitTerminalEvent();

    observerEvents.assertComplete();
    observerEvents.assertNoErrors();
    observerEvents.assertValueCount(1);

    assertThat(observerEvents.values().get(0).getData().size(), is(2));
    assertThat(observerEvents.values().get(0).getSource(), is(Source.MEMORY));
  }

  /**
   * Managing user session.
   */
  private static class UserRepository {
    private final Provider<User> cacheProvider;
    private final ApiUser api;

    private UserRepository(ApiUser api, ReactiveCache reactiveCache) {
      this.api = api;
      this.cacheProvider = reactiveCache.<User>provider()
          .withKey("user");
    }

    Single<User> login(String email) {
      return api.loginUser(email)
          .compose(cacheProvider.replace());
    }

    Single<Boolean> isLogged() {
      return cacheProvider.read()
          .map(user -> true)
          .onErrorReturn(observer -> false);
    }

    Single<User> profile() {
      return cacheProvider.read();
    }

    Completable updateUserName(String name) {
      return cacheProvider.read()
          .map(user -> {
            user.setName(name);
            return user;
          })
          .compose(cacheProvider.replace())
          .toCompletable();
    }

    Completable logout() {
      return api.logout().andThen(cacheProvider.evict());
    }
  }

  /**
   * Managing tasks.
   */
  private static class TasksRepository {
    private final ProviderList<Task> cacheProvider;
    private final ApiTasks api;

    private TasksRepository(ApiTasks api, ReactiveCache reactiveCache) {
      this.api = api;
      this.cacheProvider = reactiveCache.<Task>providerList()
          .withKey("tasks");
    }

    Single<Reply<List<Task>>> tasks(boolean refresh) {
      return refresh ? api.tasks().compose(cacheProvider.replaceAsReply())
          : api.tasks().compose(cacheProvider.readWithLoaderAsReply());
    }

    Completable addTask(String name, String desc) {
      return api.addTask(1, name, desc)
          .andThen(cacheProvider.entries()
              .addFirst(new Task(1)));
    }

    Completable removeTask(int id) {
      return api.removeTask(id)
          .andThen(cacheProvider.entries()
              .evict((position, count, element) -> element.getId() == id));
    }
  }

  /**
   * Managing events feed with pagination.
   */
  private static class EventsRepository {
    private final ProviderGroup<List<Event>> cacheProvider;
    private final ApiEvents apiEvents;

    private EventsRepository(ApiEvents apiEvents, ReactiveCache reactiveCache) {
      this.apiEvents = apiEvents;
      this.cacheProvider = reactiveCache.<List<Event>>providerGroup()
          .withKey("events");
    }

    Single<Reply<List<Event>>> events(boolean refresh, int page) {
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
    private final int id;

    public Task(int id) {
      this.id = id;
    }

    public int getId() {
      return id;
    }
  }

  private static class Event {

  }

  private static class ApiUser {

    public Single<User> loginUser(String email) {
      return Single.just(new User());
    }

    public Completable logout() {
      return Completable.complete();
    }
  }

  private static class ApiTasks {
    private final List<Task> tasks;

    public ApiTasks() {
      this.tasks = new ArrayList<>();
    }

    public Single<List<Task>> tasks() {
      return Single.just(new ArrayList<>(tasks));
    }

    public Completable addTask(int id, String name, String desc) {
      Task task = new Task(id);
      tasks.add(task);
      return Completable.complete();
    }

    public Completable removeTask(int id) {
      Task candidate = null;
      for (Task task : tasks) {
        if (task.getId() == id) candidate = task;
      }
      tasks.remove(candidate);
      return Completable.complete();
    }
  }

  private static class ApiEvents {
    private final HashMap<Integer, List<Event>> events = new HashMap<Integer, List<Event>>() {{
      put(1, Arrays.asList(new Event()));
      put(2, Arrays.asList(new Event(), new Event()));
    }};

    public Single<List<Event>> events(int page) {
      return Single.just(events.get(page));
    }
  }
}
