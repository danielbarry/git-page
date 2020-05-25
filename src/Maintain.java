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
  private static final int REPO_LOOP_MILLIS = 1000 * 600;

  private HashMap<String, File> repos;

  /**
   * Maintain()
   *
   * Initialise the variables for maintenance.
   *
   * @param repos The directories of the repositories of interest.
   **/
  public Maintain(String[] repos){
    this.repos = new HashMap<String, File>();
    for(String r : repos){
      File d = new File(r);
      if(d.exists() && d.isDirectory() && d.canRead()){
        File p = d.getAbsoluteFile().getParentFile();
        if(!d.getName().equals(".")){
          Main.log("Adding repository '" + d.getName() + "'");
          this.repos.put(d.getName(), d.getAbsoluteFile());
        }else{
          Main.log("Adding repository '" + p.getName() + "'");
          this.repos.put(p.getName(), p.getAbsoluteFile());
        }
      }else{
        Main.warn("Unable to use repository '" + r + "'");
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
    /* Infinite loop */
    for(;;){
      Main.log("Checking repos...");
      /* Pre-compute timeout value */
      long loopTimeout = System.currentTimeMillis() + REPO_LOOP_MILLIS;
      /* Check each repository */
      for(String key : repos.keySet()){
        try{
          /* Check the repo for remote changes */
          if(Git.gitFetch(repos.get(key)).length() > 0){
            Main.log("Pulling changes for '" + key + "'");
            /* Pull changes if there are some */
            Git.gitPull(repos.get(key));
          }
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
