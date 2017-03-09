
## Next table summarizes the available functions on ActionsList

| method | description |
| --- | --- |
| add(func2, element) | Func2 will be called for every iteration until its condition returns true. When true, the element is added to the cache at the position of the current iteration
| addFirst(element) | Add the object at the first position of the cache
| addLast(element) | Add the object at the last position of the cache
| addAllFirst(elements) | Add the objects at the first position of the cache
| addAllLast(elements) | Add the objects at the last position of the cache
| addAll(func2, elements) | Func2 will be called for every iteration until its condition returns true. When true, the elements are added to the cache at the position of the current iteration
| evictFirst() | Evict object at the first position of the cache
| evictFirstN(n) | Evict as much objects as requested by n param starting from the first position
| evictLast() | Evict object at the last position of the cache
| evictLastN(n) | Evict as much objects as requested by n param starting from the last position
| evictLast() | Evict object at the last position of the cache
| evictFirst(func1Count) | Evict object at the first position of the cache. func1Count exposes the count of elements in the cache
| evictFirstN(func1Count, n) | Evict as much objects as requested by n param starting from the first position. func1Count exposes the count of elements in the cache
| evictLast(func1Count) | Evict object at the last position of the cache. func1Count exposes the count of elements in the cache
| evictLastN(func1Count, n) | Evict as much objects as requested by n param starting from the last position. func1Count exposes the count of elements in the cache
| evict(func1) | Func1 will be called for every iteration until its condition returns true. When true, the element of the current iteration is evicted from the cache.
| evict(func3) | Func3 will be called for every iteration until its condition returns true. When true, the element of the current iteration is evicted from the cache. func3 exposes the position of the current iteration, the count of elements in the cache and the element of the current iteration.
| evictAllKeepingFirstN(n) | Evict elements from the cache starting from the first position until its count is equal to the value specified in n param.
| evictAllKeepingLastN(n) | Evict elements from the cache starting from the last position until its count is equal to the value specified in n param.
| evictIterable(func3) | Func3 will be called for every iteration. When true, the element of the current iteration is evicted from the cache. func3 exposes the position of the current iteration, the count of elements in the cache and the element of the current iteration.
| update(func1, replace) | Func1 will be called for every iteration until its condition returns true. When true, the element of the current iteration is updated. func1 exposes the element of the current iteration. replace exposes the original element and expects back the one modified.
| update(func3, replace) | Func3 will be called for every iteration until its condition returns true. When true, the element of the current iteration is updated. func3 exposes the position of the current iteration, the count of elements in the cache and the element of the current iteration. replace exposes the original element and expects back the one modified.
| updateIterable(func1, replace) | Func1 will be called for every. When true, the element of the current iteration is updated. func1 exposes the element of the current iteration. replace exposes the original element and expects back the one modified.
| updateIterable(func3, replace) | Func3 will be called for every iteration. When true, the element of the current iteration is updated. func3 exposes the position of the current iteration, the count of elements in the cache and the element of the current iteration. replace exposes the original element and expects back the one modified.
