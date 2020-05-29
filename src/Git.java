package b.gp;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.math.BigInteger;

/**
 * Git.java
 *
 * Wrapper for the git command line.
 **/
public class Git{
  /**
   * IndexEntry.Git.java
   *
   * A data structure for the index entries.
   **/
  private class IndexEntry{
    public long ctime_s;
    public long ctime_n;
    public long mtime_s;
    public long mtime_n;
    public long dev;
    public long ino;
    public long mode;
    public long uid;
    public long gid;
    public long size;
    public String hash;
    public int flags;
    public String path;
  }

  private static final int GIT_MAX_INPUT = 65536;
  private static final int GIT_INDEX_VAR_LEN = 4;
  private static final int GIT_INDEX_INT_LEN = 2;
  private static final int GIT_HASH_DIGEST_RAW = 20;
  private static final int GIT_INDEX_ENTRY_LEN = (GIT_INDEX_VAR_LEN * 10) +
                                                  GIT_HASH_DIGEST_RAW     +
                                                  GIT_INDEX_VAR_LEN;

  private File dir;
  private IndexEntry[] entries;

  /**
   * Git()
   *
   * Initialize the Git class and setup Git for read-only operations.
   *
   * @param dir The directory of the Git repository.
   **/
  public Git(File dir){
    this.dir = dir;
    readIndex();
  }

  /**
   * readIndex()
   *
   * Read the Git index and update the entries list.
   **/
  private void readIndex(){
    entries = null;
  }

  /**
   * getString()
   *
   * Get a String value from a raw data stream.
   *
   * @param data The data buffer to be read.
   * @param i The offset into the data stream to be converted.
   * @return The String value retrieved.
   **/
  private static String getString(byte[] data, int i){
    int e = 0;
    while(e < data.length && e != '\0'){
      ++e;
    }
    return new String(data, i, e);
  }

  /**
   * getShort()
   *
   * Get a short value from a raw data stream.
   *
   * @param data The data buffer to be read.
   * @param i The offset into the data stream to be converted.
   * @return The short value retrieved.
   **/
  private static int getShort(byte[] data, int i){
    byte[] var = new byte[GIT_INDEX_INT_LEN];
    System.arraycopy(data, i, var, 0, GIT_INDEX_INT_LEN);
    return new BigInteger(1, var).intValue();
  }

  /**
   * getLong()
   *
   * Get a long value from a raw data stream.
   *
   * @param data The data buffer to be read.
   * @param i The offset into the data stream to be converted.
   * @return The long value retrieved.
   **/
  private static long getLong(byte[] data, int i){
    byte[] var = new byte[GIT_INDEX_VAR_LEN];
    System.arraycopy(data, i, var, 0, GIT_INDEX_VAR_LEN);
    return new BigInteger(1, var).longValue();
  }

  /**
   * getHash()
   *
   * Get a hash value from a raw data stream.
   *
   * @param data The data buffer to be read.
   * @param i The offset into the data stream to be converted.
   * @return The hash value retrieved.
   **/
  private static String getHash(byte[] data, int i){
    byte[] var = new byte[GIT_HASH_DIGEST_RAW];
    System.arraycopy(data, i, var, 0, GIT_HASH_DIGEST_RAW);
    return new BigInteger(1, var).toString(16);
  }

  /**
   * readFile()
   *
   * Read binary file from disk and return binary array.
   *
   * @param file The file to be read.
   * @param len The maximum number of bytes to be read, if set to a number less
   * than zero no limit is set.
   * @return The byte array containing the file, otherwise NULL.
   **/
  private static byte[] readFile(File file, int len){
    byte[] buff = null;
    try{
      FileInputStream fis = new FileInputStream(file);
      /* If we have been told to limit then limit */
      if(len >= 0){
        len = file.length() < len ? (int)(file.length()) : len;
      }else{
        len = (int)(file.length());
      }
      buff = new byte[len];
      fis.read(buff);
      fis.close();
    }catch(IOException e){
      buff = null;
    }
    return buff;
  }

  /**
   * gitLog()
   *
   * Get a list of commits, otherwise an empty list.
   *
   * @param dir The directory of the repository.
   * @param start The start of the commits to be displayed.
   * @param count The number of commits to be displayed.
   * @param sep The separator to be used.
   * @return List of commits, one per line.
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
        "--pretty=format:%h" + sep + "%D" + sep + "%cI" + sep + "%cn" + sep + "%s"
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
   * gitCommit()
   *
   * Get specific information about a given commit.
   *
   * @param dir The directory of the repository.
   * @param commit The commit name.
   * @return The commit information.
   **/
  public static String[] gitCommit(File dir, String commit){
    /* Validate the commit to ensure no arbitrary command execution */
    if(!validCommit(commit)){
      Main.warn("Bad commit string");
      return new String[]{};
    }
    byte[] buff = exec(
      dir,
      new String[]{
        "git",
        "log",
        "--max-count=1",
        "--pretty=format:%H%n%T%n%P%n%an%n%ae%n%aI%n%cn%n%ce%n%cI%n%D%n%s",
        commit
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
   * gitDiff()
   *
   * Get a code difference for a given commit.
   *
   * @param dir The directory of the repository.
   * @param commit The commit name.
   * @return The raw diff.
   **/
  public static String gitDiff(File dir, String commit){
    /* Validate the commit to ensure no arbitrary command execution */
    if(!validCommit(commit)){
      Main.warn("Bad commit string");
      return new String();
    }
    byte[] buff = exec(
      dir,
      new String[]{
        "git",
        "show",
        commit
      }
    );
    if(buff != null){
      if(buff.length >= GIT_MAX_INPUT){
        return new String(buff) + "\n\n[-- DIFF TOO LONG --]";
      }else{
        return new String(buff);
      }
    }else{
      Main.warn("Command failed to run");
      return new String();
    }
  }

  /**
   * gitFetch()
   *
   * Fetch changes from the default origin repository.
   *
   * @param dir The directory of the repository.
   * @return The raw output lines of the fetch.
   **/
  public static String gitFetch(File dir){
    /* TODO: Make sure this operation is only run by authorized classes. */
    byte[] buff = exec(
      dir,
      new String[]{
        "git",
        "fetch"
      }
    );
    if(buff != null){
      return new String(buff);
    }else{
      Main.warn("Command failed to run");
      return new String();
    }
  }

  /**
   * gitPull()
   *
   * Pull changes from the default origin repository.
   *
   * @param dir The directory of the repository.
   * @return The raw output lines of the pull.
   **/
  public static String gitPull(File dir){
    /* TODO: Make sure this operation is only run by authorized classes. */
    byte[] buff = exec(
      dir,
      new String[]{
        "git",
        "pull"
      }
    );
    if(buff != null){
      return new String(buff);
    }else{
      Main.warn("Command failed to run");
      return new String();
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

  /**
   * validCommit()
   *
   * Make sure a given commit is valid.
   *
   * @param commit The commit String to be checked.
   * @return True if value, otherwise false.
   **/
  public static boolean validCommit(String commit){
    if(commit != null && commit.length() == 7){
      for(int x = 0; x < commit.length(); x++){
        char c = commit.charAt(x);
        if(!(c >= '0' && c <= '9') && !(c >= 'a' && c <= 'f')){
          return false;
        }
      }
    }else{
      return false;
    }
    return true;
  }
}
