package com.chenfei.exview.internal;

import java.util.concurrent.ThreadFactory;

/**
 * This is intended to only be used with a single thread executor.
 */
final class ExViewSingleThreadFactory implements ThreadFactory {

  private final String threadName;

  ExViewSingleThreadFactory(String threadName) {
    this.threadName = "ExView-" + threadName;
  }

  @Override public Thread newThread(Runnable runnable) {
    return new Thread(runnable, threadName);
  }
}
