package me.volart;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;


public class EntityLock {
  
  private static final Logger LOG = Logger.getLogger(EntityLock.class.getName());
  
  private ReentrantLock lock = new ReentrantLock();
  private AtomicInteger lockingCount = new AtomicInteger(0);
  
  public void lock() {
    lockingCount.incrementAndGet();
    lock.lock();
  }
  
  public boolean tryLock(long timeout, TimeUnit unit){
    boolean locked = false;
    lockingCount.incrementAndGet();
    try {
      locked = lock.tryLock(timeout, unit);
      if(!locked) lockingCount.decrementAndGet();
    } catch (InterruptedException e) {
      lockingCount.decrementAndGet();
      LOG.log(Level.WARNING, "InterruptedException on trying to acquire a lock", e);
      Thread.currentThread().interrupt();
    }
    return locked;
  }
  
  public void unlock() {
    lock.unlock();
    lockingCount.decrementAndGet();
  }
  
  public int getLockingCount() {
    return lockingCount.get();
  }
}
