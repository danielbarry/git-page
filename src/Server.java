package b.gp;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Server.java
 *
 * Run the server indefinitely and serve pages.
 **/
public class Server extends Thread{
  private static final byte[] HTTP_HEAD = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n".getBytes();

  private ServerSocket ss;
  private Socket s;
  private PageBuilder pb;

  /**
   * Server()
   *
   * Initialise the server.
   *
   * @param port The port number to start the server on.
   * @param repos The directories of the repositories of interest.
   **/
  public Server(int port, String[] repos){
    try{
      ss = new ServerSocket(port);
    }catch(IOException e){
      Main.err("Unable to start the server");
      ss = null;
    }
    s = null;
    pb = new PageBuilder(repos);
  }

  /**
   * Server()
   *
   * Initialise the server socket handler thread.
   *
   * @param socket The socket to be handled by the thread.
   * @param pageBuilder Access to the page builder object.
   **/
  public Server(Socket socket, PageBuilder pageBuilder){
    ss = null;
    s = socket;
    pb = pageBuilder;
  }

  /**
   * loop()
   *
   * Run the main loop for the server.
   **/
  public void loop(){
    /* Infinite loop */
    for(;;){
      try{
        /* Block until a connection is made */
        (new Server(ss.accept(), pb)).start();
      }catch(Exception e){
        Main.warn("Server main loop crashed, but recovered");
      }
    }
  }

  /**
   * run()
   *
   * Handle the socket on it's own thread.
   **/
  @Override
  public void run(){
    /* Setup the connection */
    InputStream is = null;
    OutputStream os = null;
    try{
      /* Make sure we don't get slow loris'd */
      /* TODO: Pull the value out of a config file. */
      s.setSoTimeout(1000);
      /* Get the streams we'll re-use */
      is = s.getInputStream();
      os = s.getOutputStream();
      /* Send the header early */
      os.write(HTTP_HEAD);
      /* Get the request */
      byte[] buff = new byte[256];
      is.read(buff);
      String req = new String(buff);
      int reqA = req.indexOf(' ') + 1;
      int reqB = req.indexOf(' ', reqA);
      if(reqA != reqB && reqB > 0){
        req = req.substring(reqA, reqB);
      }else{
        req = "?";
      }
      /* Generate a new page */
      pb.generate(os, req);
      /* Skip rest of input (up to a maximum) */
      int a = is.available();
      a = a < 65535 ? a : 65535;
      is.skip(a);
      /* Close the connection */
      os.flush();
      s.close();
    }catch(IOException e){
      /* Do nothing */
    }
  }
}
