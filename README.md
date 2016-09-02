[![Build Status](https://travis-ci.org/VictorAlbertos/ReactiveCache.svg?branch=master)](https://travis-ci.org/VictorAlbertos/ReactiveCache)
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-ReactiveCache-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/4002)

# ReactiveCache
The act of caching data with **ReactiveCache** is just another transformation in the `Observable` chain. ReactiveCache's API exposes both `Transformer` and `Observable` **RxJava** types to gracefully merge the caching actions with the data stream.  

## Features
* A **dual cache** based on both memory and disk layers.
* **Automatic deserialization-serialization** for custom `Types`, `List`, `Map` and `Array`.
* **Pagination**
* A **lifetime** system to expire data on specific time lapses. 
* Data **encryption**.
* Customizable disk **cache size limit**.
* **Migrations** to evict data by `Type` between releases.

## SetUp
Add to top level *gradle.build* file

```gradle
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
```

Add to app module *gradle.build* file
```gradle
dependencies {
    compile 'com.github.VictorAlbertos:ReactiveCache:0.0.3'
    compile 'com.github.VictorAlbertos.Jolyglot:gson:0.0.3'
    compile 'io.reactivex:rxjava:1.1.8'
}
```

## Usage

### ReactiveCache
Create a **single instace** of `ReactiveCache` for your entire application. The builder offers some [additional configurations](#config_reactive_cache). 

```java
ReactiveCache reactiveCache = new ReactiveCache.Builder()
        .using(application.getFilesDir(), new GsonSpeaker());
```

**`evictAll()`** returns an `Observable` which evicts the cached data for every provider:

```java
cacheProvider.evictAll()
```


### Provider

Call `reactiveCache#provider()` to create a `Provider` to manage cache operations. The builder offers some [additional configurations](#config_providers). 

```java
Provider<List<Model>> cacheProvider = 
		reactiveCache.<List<Model>>provider()
        .withKey("models");
```

**`replace()`** returns a `Transformer` which replaces the `provider` data with the item emitted from the source `Observable`. If the source `Observable` throws an exception, calling `replace()` doesn't evict the `provider` data.

```java
api.getModels()
	.compose(cacheProvider.replace())
```

**`read()`** returns an `Observable` which emits the `provider` data. If there isn't any data available, throws an exception.

```java
cacheProvider.read()
```

**`readWithLoader()`** returns a `Transformer` which emits the `provider` data. If there isn't any data available, it subscribes to the source `observable` to cache and emit its item.

```java
api.getModels()
     .compose(cacheProvider.readWithLoader())
```

**`evict()`** returns an `Observable` which evicts the `provider` data:

```java
cacheProvider.evict()
```


### ProviderGroup

Call `reactiveCache#providerGroup()` to create a `ProviderGroup` to manage cache operations with pagination support. The builder offers some [additional configurations](#config_providers). 


```java
ProviderGroup<List<Model>> cacheProvider = 
		reactiveCache.<List<Model>>providerGroup()
        .withKey("modelsPaginated");
```

`ProviderGroup` exposes the same methods as `Provider` but requesting a key as an argument. That way the scope of the `provider` data in every operation is constrained to the data associated with the key. 

```java
api.getModels(group)
	.compose(cacheProvider.replace(group))

cacheProvider.read(group)

api.getModels(group)
     .compose(cacheProvider.readWithLoader())	
     
cacheProvider.evict(group)     
```

`evict()` is an overloaded method to evict the `provider` data for the entire collection of groups. 

```java
cacheProvider.evict()
```

## Use cases


Next examples illustrate how to use **ReactiveCache** on the *data layer* for client **Android** applications. They follow the *well-known* [repository pattern](http://fernandocejas.com/2014/09/03/architecting-android-the-clean-way/) in order to deal with data coming from a remote repository *(server)* and a local one *(ReactiveCache)*. 


### Simple user session.
```java
class UserRepository {
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
```

### Adding and removing tasks.
```java
  class TasksRepository {
    private final Provider<List<Task>> cacheProvider;
    private final ApiTasks api;

    TasksRepository(ApiTasks api, ReactiveCache reactiveCache) {
      this.api = api;
      this.cacheProvider = reactiveCache.<List<Task>>provider()
          .withKey("tasks");
    }

    Observable<List<Task>> tasks(boolean pullToRefresh) {
      return pullToRefresh ? api.tasks().compose(cacheProvider.replace())
          : api.tasks().compose(cacheProvider.readWithLoader());
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
```

### Paginated feed of events.
```java
  class EventsRepository {
    private final ProviderGroup<List<Event>> cacheProvider;
    private final ApiEvents apiEvents;

    EventsRepository(ApiEvents apiEvents, ReactiveCache reactiveCache) {
      this.apiEvents = apiEvents;
      this.cacheProvider = reactiveCache.<List<Event>>providerGroup()
          .withKey("events");
    }

    Observable<List<Event>> events(boolean pullToRefresh, int page) {
      if (pullToRefresh) {
        return apiEvents.events(page)
            .flatMap(events -> cacheProvider.evict()
                 .map(ignore -> events))
            .compose(cacheProvider.replace(page));
      }

      return apiEvents.events(page)
          .compose(cacheProvider.readWithLoader(page));
    }
  }
```

## Configuration


### <a name="config_reactive_cache"></a> ReactiveCache

When building `ReactiveCache` the next global configurations are available thought the builder:

* **`diskCacheSize(int)`** sets the max memory in megabytes for all the cached data on disk. *Default value is 100*.

* **`encrypt(String)`** sets the key to be used for encrypting the data on those providers as such configured. 

* **`useExpiredDataWhenNoLoaderAvailable()`** if invoked, ReactiveCache dispatches records already expired instead of throwing. 

* **`migrations(List<MigrationCache>)`** every `MigrationCache` expects a version number and a `Class[]` to check what cached data matches with these classes to evict it from disk. Use `MigrationCache` for those `Type` which have added new fields between app releases.

```java
ReactiveCache reactiveCache = new ReactiveCache.Builder()
        .diskCacheSize(100)
        .encrypt("myStrongKey1234")
        .useExpiredDataWhenNoLoaderAvailable()
        .migrations(Arrays.asList(
            new MigrationCache(1, new Class[] {Model.class}),
            new MigrationCache(1, new Class[] {Model1.class})))
        .using(application.getFilesDir(), new GsonSpeaker());
```

### <a name="config_providers"></a> Provider and ProviderGroup

When building both `Provider` and `ProviderGroup` the next configurations are available thought the builder:

* **`encrypt(boolean)`** when true, the data cached by this `provider` is encrypted using the key specified in `ReactiveCache#encript(key)`. *Default value is false*.

* **`expirable(boolean)`** when false, the data cached by this `provider` is not eligible to be expired if not enough space remains on disk. *Default value is true*.

* **`lifeCache(long, TimeUnit)`** sets the amount of time before the data would be expired. *By default the data has no life time*. 

```java
 Provider<Model> cacheModel = reactiveCache.<Model>provider()
          .encrypt(true)
          .expirable(false)
          .lifeCache(60, TimeUnit.MINUTES)
          .withKey("model");
```

## Author

**Víctor Albertos**

* <https://twitter.com/_victorAlbertos>
* <https://www.linkedin.com/in/victoralbertos>
* <https://github.com/VictorAlbertos>


## Another author's libraries using RxJava:
* [Mockery](https://github.com/VictorAlbertos/Mockery): Android and Java library for mocking and testing networking layers with built-in support for Retrofit.
* [RxCache](https://github.com/VictorAlbertos/RxCache): Reactive caching library for Android and Java. (ReactiveCache uses internally the core from RxCache).
* [RxActivityResult](https://github.com/VictorAlbertos/RxActivityResult): A reactive-tiny-badass-vindictive library to break with the OnActivityResult implementation as it breaks the observables chain. 
* [RxFcm](https://github.com/VictorAlbertos/RxFcm): RxJava extension for Android Firebase Cloud Messaging (aka fcm).
* [RxSocialConnect](https://github.com/VictorAlbertos/RxSocialConnect-Android): OAuth RxJava extension for Android.
