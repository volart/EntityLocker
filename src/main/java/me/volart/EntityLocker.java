package me.volart;

import java.util.concurrent.TimeUnit;

public interface EntityLocker<T extends Comparable<T>> {
  
  void lock(T id) throws InterruptedException;
  
  void lock(T[] ids) throws InterruptedException;
  
  void tryLock(T id, long timeout, TimeUnit unit) throws InterruptedException;
  
  void tryLock(T[] ids, long timeout, TimeUnit unit) throws InterruptedException;
  
  void unlock(T id);
  
  void unlock(T[] ids);
}
