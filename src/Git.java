package b.gp;

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
   * exec()
   *
   * Execute a give command and return the output. Note that this command will
   * block until complete.
   *
   * @param cmd The command and parameters to be run.
   * @return The result of the command.
   **/
  private static byte[] exec(String[] cmd){
    try{
      Process p = (new ProcessBuilder(cmd)).start();
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
