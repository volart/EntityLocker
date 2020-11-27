package me.volart;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;

public class EntityLockerImplTest {
  
  private EntityLocker<Integer> entityLocker;
  private ExecutorService executorService;
  
  @Before
  public void setUp() {
    entityLocker = new EntityLockerImpl<>();
    executorService = Executors.newCachedThreadPool();
  }
  
  @Test
  public void testLockingStress_1000Threads() {
    AtomicInteger actualRuns = new AtomicInteger(0);
    int theadCount = 1000;
    
    for (int i = 0; i < theadCount; i++) {
      executorService.execute(getRunnable(actualRuns));
    }
  
    await().atMost(100, TimeUnit.SECONDS).until(() -> actualRuns.get() == theadCount);
  }
  
  @Test
  public void testBunchLockingStress_100Threads() {
    AtomicInteger actualRuns = new AtomicInteger(0);
    int theadCount = 100;
    
    for (int i = 0; i < theadCount; i++) {
      executorService.execute(getBunchRunnable(5, actualRuns));
    }
    
    await().atMost(100, TimeUnit.SECONDS).until(() -> actualRuns.get() == theadCount);
  }
  
  private Runnable getRunnable(AtomicInteger actualRuns) {
    return () -> {
      try {
        Thread.sleep(ThreadLocalRandom.current().nextInt(10));
        int id = ThreadLocalRandom.current().nextInt(10);
        entityLocker.lock(id);
        actualRuns.incrementAndGet();
        Thread.sleep(ThreadLocalRandom.current().nextInt(100));
        entityLocker.unlock(id);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    };
  }
  
  private Runnable getBunchRunnable(int size, AtomicInteger actualRuns) {
    return () -> {
      try {
        Thread.sleep(ThreadLocalRandom.current().nextInt(10));
        Integer[] randomArray = getRandomArray(size, 5);
        entityLocker.lock(randomArray);
        actualRuns.incrementAndGet();
        Thread.sleep(ThreadLocalRandom.current().nextInt(100));
        entityLocker.unlock(randomArray);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    };
  }
  
  private Integer[] getRandomArray(int size, int bound) {
    Integer[] array = new Integer[size];
    for (int i = 0; i < size; i++) {
      array[i] = ThreadLocalRandom.current().nextInt(bound);
    }
    return array;
  }
}