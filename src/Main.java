package b.gp;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Main.java
 *
 * Decide on the execution of the program.
 **/
public class Main{
  private JSON config;

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
    config = null;
    /* Loop over the arguments */
    for(int x = 0; x < args.length; x++){
      switch(args[x]){
        case "-c" :
        case "--config" :
          x = config(args, x);
          break;
        case "-h" :
        case "--help" :
          x = help(args, x);
          break;
        case "-t" :
        case "--test" :
          x = test(args, x);
          break;
        default :
          err("Unknown argument '" + args[x] + "'");
          break;
      }
    }
    /* if we have a server config, try to start the server */
    if(config != null){
      /* Get the repositories and load their git data */
      HashMap<String, Git> repos = getRepos();
      /* Start threads */
      log("Starting maintenance thread");
      (new Maintain(repos, config)).start();
      log("Starting server thread");
      (new Server(repos, config)).loop();
    }
  }

  /**
   * getRepos()
   *
   * Get the repositories to be monitored.
   *
   * @return A HashMap of repositories found.
   **/
  private HashMap<String, Git> getRepos(){
    /* Add repos to be monitored */
    HashMap<String, Git> repos = new HashMap<String, Git>();
    for(int x = 0; x < config.get("repos").length(); x++){
      JSON entry = config.get("repos").get(x);
      if(
        entry.get("dir").value(null) == null ||
        entry.get("url").value(null) == null
      ){
        Main.log("Skipping repository #" + x);
        break;
      }
      File d = new File(entry.get("dir").value(null));
      if(d.exists() && d.isDirectory() && d.canRead()){
        log("Adding repository '" + d.getAbsolutePath() + "'");
        /* Get if we want the repo to be able to pull */
        boolean pull = entry.get("maintain").value("false").equals("true");
        Git git = new Git(d.getAbsoluteFile(), pull);
        repos.put(entry.get("url").value(null), git);
      }else{
        Main.warn("Can't add repo '" + entry.get("url").value("NULL") + "'");
      }
    }
    return repos;
  }

  /**
   * config()
   *
   * Load a configuration file if possible.
   *
   * @param args The command line arguments.
   * @param x The command line offset.
   * @return The new command line offset.
   **/
  private int config(String[] args, int x){
    if(++x < args.length){
      try{
        config = JSON.build(args[x]);
      }catch(Exception e){
        err("Unable to parse JSON file");
      }
    }else{
      err("Not enough params to set configuration");
    }
    return x;
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
    System.out.println("    -c  --config    Load a configuration file");
    System.out.println("                      <STR> Path to config file");
    System.out.println("    -h  --help      Display this help");
    System.out.println("    -t  --test      Perform internal tests");
    System.exit(0);
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
