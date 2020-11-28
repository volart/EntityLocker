package me.volart;

import java.util.concurrent.TimeUnit;

public interface EntityLocker<T extends Comparable<T>> {
  
  void lock(T... ids) throws InterruptedException;
  
  void tryLock(long timeout, TimeUnit unit, T... id) throws InterruptedException;
  
  void unlock(T... id);
}
