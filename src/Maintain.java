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
    /* TODO: Poll the repos. */
  }
}
