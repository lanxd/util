package com.twitter.jvm;

import org.junit.Test;

public class JvmCompilationTest {

  @Test
  public void testJvm() {
    Jvms.get();
  }


  @Test
  public void testPid() {
    Jvms.processId();
  }
}
