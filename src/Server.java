package b.gp;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

/**
 * Server.java
 *
 * Run the server indefinitely and serve pages.
 **/
public class Server extends Thread{
  private int headSize;
  private int maxInput;
  private int maxWait;
  private ServerSocket ss;
  private Socket s;
  private PageBuilder pb;

  /**
   * Server()
   *
   * Initialise the server.
   *
   * @param repos The git repositories of interest.
   * @param config Access to the configuration data.
   **/
  public Server(HashMap<String, Git> repos, JSON config){
    /* Get server settings */
    int port = Integer.parseInt(config.get("server").get("port").value("8080"));
    headSize = Integer.parseInt(config.get("server").get("head-size").value("256"));
    maxInput = Integer.parseInt(config.get("server").get("max-input").value("65536"));
    maxWait = Integer.parseInt(config.get("server").get("max-wait-ms").value("5000"));
    /* Log written values */
    Main.log("Port set to '" + port + "'");
    Main.log("Head size set to '" + headSize + "'");
    Main.log("Max input set to '" + maxInput + "'");
    Main.log("Max wait set to '" + maxWait + "'");
    /* Try to start the server */
    try{
      ss = new ServerSocket(port);
    }catch(IOException e){
      Main.err("Unable to start the server");
      ss = null;
    }
    s = null;
    /* Pre-build PageBuilder instance for the server */
    pb = new PageBuilder(repos, config);
  }

  /**
   * Server()
   *
   * Initialise the server socket handler thread.
   *
   * @param socket The socket to be handled by the thread.
   * @param pageBuilder Access to the page builder object.
   * @param headSize Maximum head size to be processed for URL request String.
   * @param maxInput Maximum input to be read from the client socket.
   * @param maxWait Maximum time to keep the socket around for.
   **/
  public Server(
    Socket socket,
    PageBuilder pageBuilder,
    int headSize,
    int maxInput,
    int maxWait
  ){
    ss = null;
    s = socket;
    pb = pageBuilder;
    this.headSize = headSize;
    this.maxInput = maxInput;
    this.maxWait = maxWait;
  }

  /**
   * loop()
   *
   * Run the main loop for the server.
   **/
  public void loop(){
    /* Check if server was setup correctly */
    if(ss == null){
      Main.log("Server could not be started, stopping thread");
      return;
    }
    /* Infinite loop */
    for(;;){
      try{
        /* Block until a connection is made */
        (new Server(ss.accept(), pb, headSize, maxInput, maxWait)).start();
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
      s.setSoTimeout(maxWait);
      /* Get the streams we'll re-use */
      is = s.getInputStream();
      os = s.getOutputStream();
      /* Get the request */
      byte[] buff = new byte[headSize];
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
      a = a < maxInput ? a : maxInput;
      is.skip(a);
      /* Close the connection */
      os.flush();
      s.close();
    }catch(IOException e){
      /* Do nothing */
    }
  }
}
