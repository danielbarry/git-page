package b.gp;

/**
 * Maintain.java
 *
 * Maintain the repositories handled by this server by fetching and pulling in
 * the latest changes.
 **/
public class Maintain extends Thread{
  /**
   * Maintain()
   *
   * Initialise the variables for maintenance.
   *
   * @param repos The directories of the repositories of interest.
   **/
  public Maintain(String[] repos){
    /* TODO: Do something with repositories. */
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
