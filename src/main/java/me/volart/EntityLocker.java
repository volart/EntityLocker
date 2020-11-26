package me.volart;

import java.util.concurrent.TimeUnit;

public interface EntityLocker<T extends Comparable<T>, R extends Runnable> {
  
  void lockAndExecute(T id, R protectedCode);
  
  void tryLockAndExecute(T id, R protectedCode, long timeout, TimeUnit unit);
}
