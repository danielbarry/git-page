package b.gp;

import java.io.File;
import java.util.HashMap;

/**
 * Maintain.java
 *
 * Maintain the repositories handled by this server by fetching and pulling in
 * the latest changes.
 **/
public class Maintain extends Thread{
  private int repoLoopMillis;
  private HashMap<String, File> repos;

  /**
   * Maintain()
   *
   * Initialise the variables for maintenance.
   *
   * @param config Access to the configuration data.
   **/
  public Maintain(JSON config){
    /* Make sure the configuration structure exists */
    if(config.get("repos") == null){
      Main.warn("No repository configuration provided");
      return;
    }
    /* Get a loop time */
    if(
      config.get("maintain") != null &&
      config.get("maintain").get("loop-wait-s") != null &&
      config.get("maintain").get("loop-wait-s").value() != null
    ){
      try{
        repoLoopMillis = 1000 *
          Integer.parseInt(config.get("maintain").get("loop-wait-s").value());
      }catch(NumberFormatException e){
        repoLoopMillis = 1000 * 600;
        Main.log("Invalid number, setting default loop wait");
      }
    }else{
      repoLoopMillis = 1000 * 600;
      Main.log("No value available, setting default loop wait");
    }
    /* Add repos to be monitored */
    repos = new HashMap<String, File>();
    for(int x = 0; x < config.get("repos").length(); x++){
      JSON entry = config.get("repos").get(x);
      if(
        entry == null                                 ||
        entry.get("dir") == null                      ||
        entry.get("dir").value() == null              ||
        entry.get("maintain") == null                 ||
        entry.get("maintain").value() == null         ||
        !entry.get("maintain").value().equals("true") ||
        entry.get("url") == null                      ||
        entry.get("url").value() == null
      ){
        Main.log("Skipping repository #" + x);
        break;
      }
      File d = new File(entry.get("dir").value());
      if(d.exists() && d.isDirectory() && d.canRead()){
        repos.put(entry.get("url").value(), d.getAbsoluteFile());
      }else{
        Main.warn("Unable to use repository '" + entry.get("url").value() + "'");
      }
    }
  }

  /**
   * run()
   *
   * Check each of the repositories, polling them and acting accordingly.
   **/
  @Override
  public void run(){
    /* Make sure there is something to maintain */
    if(repos.size() <= 0){
      Main.log("No repos to maintain, stopping thread");
      return;
    }
    /* Infinite loop */
    for(;;){
      Main.log("Checking repos...");
      /* Pre-compute timeout value */
      long loopTimeout = System.currentTimeMillis() + repoLoopMillis;
      /* Check each repository */
      for(String key : repos.keySet()){
        try{
          /* Check the repo for remote changes */
          Git.gitFetch(repos.get(key)); // TODO
//          if(Git.gitFetch(repos.get(key)).length() > 0){
            Main.log("Pulling changes for '" + key + "'");
            /* Pull changes if there are some */
            Git.gitPull(repos.get(key));
//          }
        }catch(Exception e){
          Main.warn("Error checking repository '" + key + "'");
        }
      }
      /* Wait until we can go again */
      long loopRemain = loopTimeout - System.currentTimeMillis();
      if(loopRemain > 0){
        try{
          Main.log("Waiting " + (loopRemain / 1000) + "s...");
          Thread.sleep(loopRemain);
        }catch(InterruptedException e){
          /* Do nothing */
        }
      }
    }
  }
}
