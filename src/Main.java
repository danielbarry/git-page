package b.gp;

import java.util.ArrayList;

/**
 * Main.java
 *
 * Decide on the execution of the program.
 **/
public class Main{
  private boolean maintain;
  private ArrayList<String> repos;
  private int port;
  private String url;

  /**
   * main()
   *
   * The main entry point into the program.
   *
   * @param args The command line arguments.
   **/
  public static void main(String[] args){
    new Main(args);
  }

  /**
   * Main()
   *
   * Parse the command line arguments and decide on the execution of the
   * program.
   *
   * @param args The command line arguments.
   **/
  public Main(String[] args){
    /* Initialise local variables */
    maintain = false;
    repos = new ArrayList<String>();
    port = -1;
    url = "http://127.0.0.1";
    /* Loop over the arguments */
    for(int x = 0; x < args.length; x++){
      switch(args[x]){
        case "-h" :
        case "--help" :
          x = help(args, x);
          break;
        case "-m" :
        case "--maintain" :
          x = maintain(args, x);
          break;
        case "-r" :
        case "--repo" :
          x = repo(args, x);
          break;
        case "-s" :
        case "--server" :
          x = server(args, x);
          break;
        case "-t" :
        case "--test" :
          x = test(args, x);
          break;
        case "-u" :
        case "--url" :
          x = url(args, x);
          break;
        default :
          err("Unknown argument '" + args[x] + "'");
          break;
      }
    }
    /* Check if we should run a server */
    if(port >= 0){
      /* Check if maintenance needed */
      if(maintain){
        log("Starting maintenance loop");
        (new Maintain(repos.toArray(new String[repos.size()]))).start();
      }
      log("Starting the server");
      (new Server(port, url, repos.toArray(new String[repos.size()]))).loop();
    }else{
      log("No action requested");
    }
  }

  /**
   * help()
   *
   * Display the help and then exit.
   *
   * @param args The command line arguments.
   * @param x The command line offset.
   * @return The new command line offset.
   **/
  private int help(String[] args, int x){
    System.out.println("gitpage [OPT]");
    System.out.println("");
    System.out.println("  OPTions");
    System.out.println("");
    System.out.println("    -h  --help      Display this help");
    System.out.println("    -m  --maintain  Poll repositories for changes");
    System.out.println("    -r  --repo      Set a repo to be managed");
    System.out.println("                      <STR> Path of repository");
    System.out.println("    -s  --server    Run the server");
    System.out.println("                      <INT> The port number");
    System.out.println("    -t  --test      Perform internal tests");
    System.out.println("    -u  --url       The URL to be used in the RSS");
    System.exit(0);
    return x;
  }

  /**
   * maintain()
   *
   * Flag to maintain all of the repositories referenced.
   *
   * @param args The command line arguments.
   * @param x The command line offset.
   * @return The new command line offset.
   **/
  private int maintain(String[] args, int x){
    maintain = true;
    return x;
  }

  /**
   * repo()
   *
   * Set a repository to be monitored.
   *
   * @param args The command line arguments.
   * @param x The command line offset.
   * @return The new command line offset.
   **/
  private int repo(String[] args, int x){
    if(++x < args.length){
      repos.add(args[x]);
    }else{
      err("Not enough params to set repository");
    }
    return x;
  }

  /**
   * server()
   *
   * Get the server settings to be run.
   *
   * @param args The command line arguments.
   * @param x The command line offset.
   * @return The new command line offset.
   **/
  private int server(String[] args, int x){
    if(++x < args.length){
      try{
        port = Integer.parseInt(args[x]);
      }catch(NumberFormatException e){
        err("Not a valid port number '" + args[x] + "'");
      }
    }else{
      err("Not enough params to set server port");
    }
    return x;
  }

  /**
   * test()
   *
   * Perform tests and then exit.
   *
   * @param args The command line arguments.
   * @param x The command line offset.
   * @return The new command line offset.
   **/
  private int test(String[] args, int x){
    JSON.test();
    System.exit(0);
    return x;
  }

  /**
   * url()
   *
   * Set the URL to be used for the RSS feed.
   *
   * @param args The command line arguments.
   * @param x The command line offset.
   * @return The new command line offset.
   **/
  private int url(String[] args, int x){
    if(++x < args.length){
      url = args[x];
    }else{
      err("Not enough params to set server port");
    }
    return x;
  }

  /**
   * print()
   *
   * Print a formatted message to the terminal.
   *
   * @param msg The message to be displayed.
   **/
  private static void print(String msg){
    System.out.println(
      "[" + System.currentTimeMillis() + "] " +
      Thread.currentThread().getStackTrace()[3].getClassName() + "->" +
      Thread.currentThread().getStackTrace()[3].getMethodName() + "::" +
      Thread.currentThread().getStackTrace()[3].getLineNumber() + " " +
      msg
    );
  }

  /**
   * log()
   *
   * Write the log.
   *
   * @param msg The message to be displayed.
   **/
  public static void log(String msg){
    print("[>>] " + msg);
  }

  /**
   * warn()
   *
   * Write the warning.
   *
   * @param msg The message to be displayed.
   **/
  public static void warn(String msg){
    print("[@@] " + msg);
  }

  /**
   * err()
   *
   * Display the error and then exit.
   *
   * @param msg The message to be displayed.
   **/
  public static void err(String msg){
    print("[!!] " + msg);
    System.exit(1);
  }
}
