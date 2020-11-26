package me.volart;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EntityLockerImpl<T extends Comparable<T>, R extends Runnable> implements EntityLocker<T, R> {
  
  private static final int GLOBAL_THRESHOLD = 10;
  private static final Logger LOG = Logger.getLogger(EntityLockerImpl.class.getName());
  
  private final ExecutorService executorService = Executors.newCachedThreadPool();
  private final ThreadLocal<T> lockedId = new ThreadLocal<>();
  private final ThreadLocal<Boolean> globalLocked = ThreadLocal.withInitial(() -> Boolean.FALSE);
  private final ReentrantLock globalLock = new ReentrantLock();
  
  private Map<T, EntityLock> locks = new ConcurrentHashMap<>();
  
  @Override
  public void lockAndExecute(T id, R protectedCode) {
    tryLockAndExecute(id, protectedCode, 0, TimeUnit.SECONDS);
  }
  
  @Override
  public void tryLockAndExecute(T id, R protectedCode, long timeout, TimeUnit unit) {
    T lockedBefore = lockedId.get();
    
    if (lockedBefore != null && lockedBefore.compareTo(id) > 0) {
      try {
        Thread.currentThread().wait(1000);
        tryLockAndExecute(id, protectedCode, timeout, unit);
        return;
      } catch (InterruptedException e) {
        LOG.log(Level.WARNING, "InterruptedException on trying to acquire a lock", e);
        Thread.currentThread().interrupt();
      }
    }
    
    EntityLock entityLock = locks.computeIfAbsent(id, k -> new EntityLock());
    if (entityLock.getLockingCount() >= GLOBAL_THRESHOLD) {
      globalLock.lock();
      globalLocked.set(Boolean.TRUE);
    }
    
    try {
      boolean locked = true;
      if (timeout > 0) {
        locked = entityLock.tryLock(timeout, unit);
      } else {
        entityLock.lock();
      }
      if (locked) {
        lockedId.set(id);
        try {
          executorService.execute(protectedCode);
        } finally {
          if (entityLock.getLockingCount() == 1) {
            locks.remove(id);
          }
          entityLock.unlock();
          lockedId.set(lockedBefore);
        }
      }
    } finally {
      if (globalLocked.get()) {
        globalLock.unlock();
        globalLocked.set(Boolean.FALSE);
      }
    }
  }
  
}