package b.gp;

/**
 **/
public class Main{
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
    /* Loop over the arguments */
    for(int x = 0; x < args.length; x++){
      switch(args[x]){
        case "-h" :
        case "--help" :
          x = help(args, x);
          break;
        default :
          err("Unknown argument '" + args[x] + "'");
          break;
      }
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
    System.out.println("    -h  --help  Display this help");
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
