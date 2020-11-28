# EntityLocker

EntityLocker is a reusable utility class provides synchronization mechanism similar to row-level DB locking.

EntityLocker is used by the components that are responsible for managing storage 
and caching of different type of entities in an application. 
EntityLocker itself does not deal with the entities, only with the IDs (primary keys) of the entities.

### Features:

1. EntityLocker supports different types of entity IDs.

2. EntityLocker’s interface allows the caller to specify which entity does it want to work with (using entity ID) 
and designate the boundaries of the code that should have exclusive access to the entity (called “protected code”).

3. For any given entity, EntityLocker guarantees that at most one thread executes protected code on that entity. 
If there’s a concurrent request to lock the same entity, the other thread should wait until the entity becomes available.

4. EntityLocker allows concurrent execution of protected code on different entities.

5. EntityLocker allows reentrant locking.

6. EntityLocker allows the caller to specify a timeout for locking an entity.

7. EntityLocker implements protection from deadlocks (but not taking into account possible locks outside EntityLocker).

8. EntityLocker implements global lock. Protected code that executes under a global lock must not execute concurrently with any other protected code.

9. EntityLocker implements lock escalation. If a single thread has locked too many entities, escalate its lock to be a global lock.


### Example of usage:

```java
Integer id = 5;
EntityLocker<Integer> entityLocker = new EntityLockerImpl<>();

entityLocker.lock(id);
// critical section 
entityLocker.unlock(id);
```

