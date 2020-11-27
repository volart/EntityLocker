package me.volart;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class EntityLockerImpl<T extends Comparable<T>> implements EntityLocker<T> {
  
  private static final int GLOBAL_THRESHOLD = 10;
  
  private final ReentrantLock globalLock = new ReentrantLock();
  
  private Map<T, ReentrantLock> locks = new ConcurrentHashMap<>();
  private Map<Long, EntityLocks> threads = new ConcurrentHashMap<>();
  
  private final Object syncRoot = new Object();
  
  @Override
  public void lock(T id) throws InterruptedException {
    tryLock(id, 0, TimeUnit.SECONDS);
  }
  
  @Override
  public void lock(T[] ids) throws InterruptedException {
    tryLock(ids, 0, TimeUnit.SECONDS);
  }
  
  @Override
  public void tryLock(T id, long timeout, TimeUnit unit) throws InterruptedException {
    
    EntityLocks thread = threads.computeIfAbsent(Thread.currentThread().getId(), x -> new EntityLocks());
    // this thread doesn't do a work, we are in process of locking
    thread.setActive(false);
    
    try {
      ReentrantLock entityLock = locks.computeIfAbsent(id, k -> new ReentrantLock());
      boolean locked = true;
      if (timeout > 0) {
        locked = entityLock.tryLock(timeout, unit);
      } else {
        entityLock.lock();
      }
      
      if (locked) {
        thread.add(entityLock);
        lockGlobal(thread);
      }
    } finally {
      thread.setActive(thread.getLocksCount() > 0);
    }
  }
  
  @Override
  public void tryLock(T[] ids, long timeout, TimeUnit unit) throws InterruptedException {
    Arrays.sort(ids);
    for (T id : ids) {
      tryLock(id, 0, TimeUnit.SECONDS);
    }
  }
  
  @Override
  public void unlock(T id) {
    ReentrantLock entityLock = locks.get(id);
    if (entityLock == null)
      throw new IllegalArgumentException("There is no locker for the specified id = " + id);
    
    long threadId = Thread.currentThread().getId();
    EntityLocks thread = threads.get(threadId);
    if (thread == null)
      throw new IllegalStateException("There are no locks for this thread id =  " + threadId);
    
    entityLock.unlock();
    thread.remove(entityLock);
    
    if (globalLock.isHeldByCurrentThread() && thread.getLocksCount() < GLOBAL_THRESHOLD) {
      globalLock.unlock();
    }
    
    thread.setActive(thread.getLocksCount() > 0);
  }
  
  @Override
  public void unlock(T[] ids) {
    Arrays.sort(ids);
    for (int i = ids.length - 1; i >= 0; i--) {
      unlock(ids[i]);
    }
  }
  
  private void lockGlobal(EntityLocks thread) throws InterruptedException {
    synchronized (syncRoot) {
      if (thread.getLocksCount() >= GLOBAL_THRESHOLD && !globalLock.isHeldByCurrentThread()) {
        while (true) {
          boolean hasActiveThread = false;
          for (EntityLocks element : threads.values()) {
            hasActiveThread |= element.isActive();
          }
          if (hasActiveThread) {
            // Make a pause to prevent holding one core for all time
            syncRoot.wait(3);
          } else {
            break;
          }
        }
        globalLock.lock();
      }
    }
  }
}