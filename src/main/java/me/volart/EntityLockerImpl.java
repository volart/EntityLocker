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
  public void lock(T... id) throws InterruptedException {
    tryLock( 0, TimeUnit.SECONDS, id);
  }
  
  @Override
  public void tryLock(long timeout, TimeUnit unit, T... id) throws InterruptedException {
    Arrays.sort(id);
    for (T i : id) {
      tryLock(0, TimeUnit.SECONDS, i);
    }
  }
  
  @Override
  public void unlock(T... ids) {
    Arrays.sort(ids);
    for (int i = ids.length - 1; i >= 0; i--) {
      unlock(ids[i]);
    }
  }
  
  protected void tryLock(long timeout, TimeUnit unit, T id) throws InterruptedException {
    synchronized (syncRoot) {
      if (globalLock.isLocked() && !globalLock.isHeldByCurrentThread()) {
        syncRoot.wait();
      }
    }
    
    EntityLocks thread = threads.computeIfAbsent(Thread.currentThread().getId(), x -> new EntityLocks());
    // this thread doesn't do a work, we are in process of locking
    thread.setActive(false);
    
    try {
      ReentrantLock entityLock = locks.computeIfAbsent(id, k -> new ReentrantLock());
      boolean locked = lock(entityLock, timeout, unit);
      
      if (locked) {
        thread.add(entityLock);
        lockGlobal(thread);
      }
    } finally {
      thread.setActive(thread.getLocksCount() > 0);
    }
  }
  
  private boolean lock(ReentrantLock entityLock, long timeout, TimeUnit unit) throws InterruptedException {
    if (timeout > 0) {
      return entityLock.tryLock(timeout, unit);
    } else {
      entityLock.lock();
    }
    return true;
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
      notifyAll();
    }
    
    thread.setActive(thread.getLocksCount() > 0);
  }
}