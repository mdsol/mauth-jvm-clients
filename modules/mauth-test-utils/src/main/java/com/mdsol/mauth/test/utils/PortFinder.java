package com.mdsol.mauth.test.utils;

import java.io.IOException;
import java.net.ServerSocket;

public class PortFinder {
  /**
   * Provides next free port
   *
   * @return free port
   * @throws IOException - if an I/O error occurs when opening the socket.
   */
  public static int findFreePort() throws IOException {
    int port;

    try (ServerSocket socket = new ServerSocket(0)) {
      socket.setReuseAddress(true);
      port = socket.getLocalPort();
    }
    if (port == -1)
      throw new IllegalStateException("Could not find a free TCP/IP port to start embedded Jetty HTTP Server on");
    return port;
  }
}
