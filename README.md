# EntityLocker

EntityLocker is a reusable utility class provides synchronization mechanism similar to row-level DB locking.

EntityLocker is used by the components that are responsible for managing storage 
and caching of different type of entities in an application. 
EntityLocker itself does not deal with the entities, only with the IDs (primary keys) of the entities.

Requirements:

1. EntityLocker should support different types of entity IDs.

2. EntityLocker’s interface should allow the caller to specify which entity does it want to work with (using entity ID) 
and designate the boundaries of the code that should have exclusive access to the entity (called “protected code”).

3. For any given entity, EntityLocker should guarantee that at most one thread executes protected code on that entity. 
If there’s a concurrent request to lock the same entity, the other thread should wait until the entity becomes available.

4. EntityLocker should allow concurrent execution of protected code on different entities.


Bonus requirements (optional):

I. Allow reentrant locking.

II. Allow the caller to specify a timeout for locking an entity.

III. Implement protection from deadlocks (but not taking into account possible locks outside EntityLocker).

IV. Implement global lock. Protected code that executes under a global lock must not execute concurrently with any other protected code.

V. Implement lock escalation. If a single thread has locked too many entities, escalate its lock to be a global lock.

