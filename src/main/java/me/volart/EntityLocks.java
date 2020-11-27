package me.volart;

import java.util.HashSet;
import java.util.concurrent.locks.ReentrantLock;

public class EntityLocks {
  
  private final HashSet<ReentrantLock> locks = new HashSet<>();
  private volatile boolean active = false;
  
  public void setActive(boolean active) {
    this.active = active;
  }
  
  public boolean isActive() {
    return active;
  }
  
  public void add(ReentrantLock lock) {
    locks.add(lock);
  }
  
  public void remove(ReentrantLock lock) {
    locks.remove(lock);
  }
  
  public int getLocksCount() {
    return locks.size();
  }
}
