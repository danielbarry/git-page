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
  private HashMap<String, Git> repos;

  /**
   * Maintain()
   *
   * Initialise the variables for maintenance.
   *
   * @param repos The git repositories of interest.
   * @param config Access to the configuration data.
   **/
  public Maintain(HashMap<String, Git> repos, JSON config){
    /* Set sane defaults */
    repoLoopMillis = 1000 * 600;
    /* Get a loop time */
    if(
      config.get("maintain") != null &&
      config.get("maintain").get("loop-wait-s") != null &&
      config.get("maintain").get("loop-wait-s").value() != null
    ){
      repoLoopMillis = 1000 *
        Integer.parseInt(config.get("maintain").get("loop-wait-s").value());
    }
    /* Add repos to be monitored */
    this.repos = repos;
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
          repos.get(key).fetch();
          /* TODO: We should check the output of fetch before pulling! */
          Main.log("Pulling changes for '" + key + "'");
          /* Pull changes if there are some */
          repos.get(key).pull();
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
