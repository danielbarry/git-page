package b.gp;

import java.util.ArrayList;

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
    log("Starting maintenance thread");
    (new Maintain(config)).start();
    log("Starting server thread");
    (new Server(config)).loop();
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
