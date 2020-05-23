package b.gp;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;

/**
 * Git.java
 *
 * Wrapper for the git command line.
 **/
public class Git{
  private static final int GIT_MAX_INPUT = 65536;

  /**
   * gitLog()
   *
   * Get a list of commits, otherwise an empty list.
   *
   * @param dir The directory of the repository.
   * @param start The start of the commits to be displayed.
   * @param count The number of commits to be displayed.
   * @param sep The separator to be used.
   **/
  public static String[] gitLog(File dir, int start, int count, String sep){
    byte[] buff = exec(
      dir,
      new String[]{
        "git",
        "log",
        "--all",
        "--skip=" + start,
        "--max-count=" + count,
        "--pretty=format:%h" + sep + "%D" + sep + "%cI" + sep + "%cl" + sep + "%s"
      }
    );
    if(buff != null){
      return (new String(buff)).split("\n");
    }else{
      Main.warn("Command failed to run");
      return new String[]{};
    }
  }

  /**
   * exec()
   *
   * Execute a give command and return the output. Note that this command will
   * block until complete.
   *
   * @param dir The working directory to execute the command.
   * @param cmd The command and parameters to be run.
   * @return The result of the command.
   **/
  private static byte[] exec(File dir, String[] cmd){
    try{
      ProcessBuilder pb = new ProcessBuilder(cmd);
      pb.directory(dir);
      Process p = pb.start();
      p.waitFor();
      InputStream is = p.getInputStream();
      int a = is.available();
      byte[] buff = new byte[a < GIT_MAX_INPUT ? a : GIT_MAX_INPUT];
      is.read(buff);
      is.skip(is.available());
      return buff;
    }catch(InterruptedException e){
    }catch(IOException e){
      Main.warn("Failed to run command '" + cmd[0] + "'");
    }
    return null;
  }
}
