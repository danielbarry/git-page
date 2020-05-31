package b.gp;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

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
  public class IndexEntry{
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

  /**
   * TreeEntry.Git.java
   *
   * A data structure for the tree entries.
   **/
  public class TreeEntry{
    public int mode;
    public String name;
    public String hash;
  }

  /**
   * Tree.Git.java
   *
   * A data structure for the trees.
   **/
  public class Tree{
    public String hash;
    public TreeEntry[] entries;
  }

  /**
   * Commit.Git.java
   *
   * A data structure for the commits.
   **/
  public class Commit{
    public String hash;
    public String tree;
    public String parent;
    public String author;
    public String author_email;
    public Date author_date;
    public String commit;
    public String commit_email;
    public Date commit_date;
    public String subject;
  }

  /**
   * Blob.Git.java
   *
   * A data structure for the blobs.
   **/
  public class Blob{
    /* TODO: Figure out valid blob structure. */
  }

  private static final int GIT_MAX_INPUT = 256 * 256;
  private static final int GIT_HASH_DIGEST_RAW = 20;
  private static final int GIT_HASH_DIGEST_STR = 40;
  private static final int GIT_INDEX_VAR_LEN = 4;
  private static final int GIT_INDEX_INT_LEN = 2;
  private static final int GIT_INDEX_ENTRY_LEN = (GIT_INDEX_VAR_LEN * 10) +
                                                  GIT_HASH_DIGEST_RAW     +
                                                  GIT_INDEX_VAR_LEN;
  private static final int GIT_PAGE_SIZE = 16;
  private static final int GIT_PAGE_MAX = 256 * 256;

  private File dir;
  private boolean pull;
  private long lastUpdate;
  private IndexEntry[] entries;
  private HashMap<String, String> refs;
  private HashMap<String, Tree> trees;
  private HashMap<String, Commit> commits;
  private HashMap<String, Blob> blobs;
  private ArrayList<Commit> pages;

  /**
   * Git()
   *
   * Initialize the Git class and setup Git for read-only operations.
   *
   * @param dir The directory of the Git repository.
   * @param pull Whether this Git repository should pull when requested to do
   * so.
   **/
  public Git(File dir, boolean pull){
    /* Store variables internally */
    this.dir = dir;
    this.pull = pull;
    /* Initialize once */
    this.lastUpdate = System.currentTimeMillis();
    this.entries = null;
    this.refs = new HashMap<String, String>();
    this.trees = new HashMap<String, Tree>();
    this.commits = new HashMap<String, Commit>();
    this.blobs = new HashMap<String, Blob>();
    this.pages = null;
    unpack();
    update();
  }

  /**
   * update()
   *
   * Update the RAM state of the repository. This is an expensive operation and
   * should only be called after a pull.
   **/
  public void update(){
    readIndex();
    readRefs(new File(dir.getAbsolutePath() + "/.git/refs"), "");
    readObjects();
    readPages(commits.get(refs.get("heads_master")));
    lastUpdate = System.currentTimeMillis();
  }

  /**
   * lastUpdate()
   *
   * Return the time of the last update in milliseconds.
   *
   * @return The time of the last update to the repository in milliseconds.
   **/
  public long lastUpdate(){
    return lastUpdate;
  }

  /**
   * unpack()
   *
   * Find and unpack any packed git objects manually. Git likes to pack objects
   * to save space, but this really slows down searching.
   **/
  private void unpack(){
    File d = new File(dir.getAbsolutePath() + "/.git/objects/pack");
    /* Make sure it's readable */
    if(d.exists() && d.isDirectory() && d.canRead()){
      Main.log("Unpacking required for '" + dir + "'");
      File[] packs = d.listFiles();
      /* Filter and loop over packs */
      for(int x = 0; x < packs.length; x++){
        /* If it's not a pack file, skip */
        if(!packs[x].getName().endsWith(".pack")){
          continue;
        }
        Main.log("Unpacking '" + packs[x].getName() + "' in '" + dir + "'");
        File pack = new File(dir.getAbsolutePath() + "/.pack");
        /* Move the pack to the root */
        packs[x].renameTo(pack);
        /* Unpack */
        /* TODO: This locks us specifically to Unix based systems. */
        exec(new String[]{"/bin/sh", "-c", "git unpack-objects < " + pack.getName()});
        /* Remove */
        pack.delete();
      }
    }else{
      Main.log("Didn't find any objects to unpack");
    }
    /* Refs can only be unpacked with help from remote */
    if(pull){
      Main.log("Unpacking refs for '" + dir + "'");
      /* Get a list of tags */
      String[] tags = new String(exec(new String[]{"git", "tag", "-l"})).split("\n");
      /* Loop over tags and delete them */
      for(int x = 0; x < tags.length; x++){
        exec(new String[]{"git", "tag", "-d", tags[x]});
      }
      /* Pull tags from remote */
      exec(new String[]{"git", "fetch", "--tags"});
    }else{
      Main.log("Unable to unpack refs for '" + dir + "'");
    }
  }

  /**
   * readIndex()
   *
   * Read the Git index and update the entries list.
   **/
  private void readIndex(){
    entries = null;
    /* Attempt to read the index */
    int dataPtr = 0;
    byte[] data = readFile(new File(dir.getAbsolutePath() + "/.git/index"), -1);
    /* Make sure we read something and it seems valid */
    if(data == null || data.length < GIT_INDEX_VAR_LEN * 3){
      Main.warn("Unable to load git index");
      return;
    }
    /* Get header variable signature */
    String sig = new String(data, dataPtr, GIT_INDEX_VAR_LEN);
    dataPtr += GIT_INDEX_VAR_LEN;
    /* Get header variable version */
    long ver = getLong(data, dataPtr);
    dataPtr += GIT_INDEX_VAR_LEN;
    /* Get header variable entry number */
    long num = getLong(data, dataPtr);
    dataPtr += GIT_INDEX_VAR_LEN;
    /* Check the header data */
    if(!sig.equals("DIRC") || ver != 2){
      Main.warn("Bad index signature or version");
      return;
    }
    /* Search for the entries */
    entries = new IndexEntry[(int)num];
    int ePtr = 0;
    while(
      dataPtr + GIT_INDEX_ENTRY_LEN < data.length - GIT_HASH_DIGEST_RAW &&
      ePtr < num
    ){
      entries[ePtr] = new IndexEntry();
      int entryStart = dataPtr;
      /* Read standard entry header */
      entries[ePtr].ctime_s = getLong(data, dataPtr);
      dataPtr += GIT_INDEX_VAR_LEN;
      entries[ePtr].ctime_n = getLong(data, dataPtr);
      dataPtr += GIT_INDEX_VAR_LEN;
      entries[ePtr].mtime_s = getLong(data, dataPtr);
      dataPtr += GIT_INDEX_VAR_LEN;
      entries[ePtr].mtime_n = getLong(data, dataPtr);
      dataPtr += GIT_INDEX_VAR_LEN;
      entries[ePtr].dev = getLong(data, dataPtr);
      dataPtr += GIT_INDEX_VAR_LEN;
      entries[ePtr].ino = getLong(data, dataPtr);
      dataPtr += GIT_INDEX_VAR_LEN;
      entries[ePtr].mode = getLong(data, dataPtr);
      dataPtr += GIT_INDEX_VAR_LEN;
      entries[ePtr].uid = getLong(data, dataPtr);
      dataPtr += GIT_INDEX_VAR_LEN;
      entries[ePtr].gid = getLong(data, dataPtr);
      dataPtr += GIT_INDEX_VAR_LEN;
      entries[ePtr].size = getLong(data, dataPtr);
      dataPtr += GIT_INDEX_VAR_LEN;
      entries[ePtr].hash = getHashRaw(data, dataPtr);
      dataPtr += GIT_HASH_DIGEST_RAW;
      entries[ePtr].flags = getShort(data, dataPtr);
      dataPtr += GIT_INDEX_INT_LEN;
      entries[ePtr].path = PageBuilder.sanitize(getString(data, dataPtr, '\0'));
      dataPtr += entries[ePtr].path.length();
      /* Finally, increase the counter */
      ++ePtr;
      int entryLen = dataPtr - entryStart;
      dataPtr += (((entryLen + 8) >> 3) << 3) - entryLen;
    }
    /* Check that we loaded all the entries correctly */
    if(ePtr != num){
      Main.warn("Bad number of entries expected: " + num + ", got: " + ePtr);
    }
    /* Get digest */
    String dig = getHashRaw(data, (data.length - 1) - GIT_HASH_DIGEST_RAW);
  }

  /**
   * readRefs()
   *
   * Recursively read the references and store in a mapping.
   *
   * @param d The directory to read for references.
   * @param pre The string prefix for the reference.
   **/
  private void readRefs(File d, String pre){
    /* Make sure it's readable */
    if(d.exists() && d.canRead()){
      /* Do we need to keep searching? */
      if(d.isDirectory()){
        File[] childs = d.listFiles();
        for(int x = 0; x < childs.length; x++){
          readRefs(childs[x], d.getName() + '_');
        }
      /* Found a reference, add it */
      }else{
        byte data[] = readFile(d, -1);
        refs.put(pre + d.getName(), getHash(data, 0));
      }
    }else{
      Main.warn("Unable to read reference '" + d.toString() + "'");
    }
  }


  /**
   * readObjects()
   *
   * Read the objects and update their respective lists.
   **/
  private void readObjects(){
    /* Setup common variables */
    byte[] buff = new byte[GIT_MAX_INPUT];
    /* Get list of objects and loop over them */
    File[] objectsPre = new File(dir.getAbsolutePath() + "/.git/objects").listFiles();
    for(int x = 0; x < objectsPre.length; x++){
      File d = objectsPre[x];
      /* Make sure we have something valid */
      if(d.exists() && d.isDirectory() && d.canRead() && d.getName().length() == 2){
        String pre = d.getName();
        /* Now loop over actual objects */
        File[] objectsPost = d.listFiles();
        for(int y = 0; y < objectsPost.length; y++){
          File o = objectsPost[y];
          /* Make sure we have a valid file now */
          if(o.exists() && o.isFile() && o.canRead()){
            String objectHash = pre + o.getName();
            byte[] data = readFile(o, -1);
            /* Decompress the object */
            Inflater decomp = new Inflater();
            decomp.setInput(data);
            int len = 0;
            try{
              len = decomp.inflate(buff);
            }catch(DataFormatException e){
              Main.warn("Error whilst decompressing data");
            }
            /* Check if we finished */
            if(!decomp.finished()){
              Main.warn("Decompression failed, object too long");
              Main.log("Bad hash '" + objectHash + "'");
            }
            /* Parse the object */
            int buffPtr = 0;
            String type = getString(buff, buffPtr, ' ');
            buffPtr += type.length() + 1;
            String num = getString(buff, buffPtr, '\0');
            buffPtr += num.length() + 1;
            switch(type){
              case "tree" :
                Tree t = new Tree();
                t.hash = objectHash;
                /* Check we have the minimum for another loop */
                ArrayList<TreeEntry> teArr = new ArrayList<TreeEntry>();
                while(buffPtr + GIT_INDEX_INT_LEN < len && buffPtr < buff.length){
                  TreeEntry te = new TreeEntry();
                  /* Read entry */
                  String mode = getString(buff, buffPtr, ' ');
                  buffPtr += mode.length() + 1;
                  te.mode = Integer.parseInt(mode, 8);
                  te.name = PageBuilder.sanitize(getString(buff, buffPtr, '\0'));
                  buffPtr += te.name.length() + 1;
                  te.hash = getHashRaw(buff, buffPtr);
                  buffPtr += GIT_HASH_DIGEST_RAW;
                  /* Store entry */
                  teArr.add(te);
                }
                /* Store tree entries in tree and add tree to map */
                t.entries = teArr.toArray(new TreeEntry[0]);
                trees.put(t.hash, t);
                break;
              case "commit" :
                Commit c = new Commit();
                c.hash = objectHash;
                /* Read header values until blank line */
                while(buffPtr < len && buffPtr < buff.length){
                  /* Read entire line and skip it */
                  String line = getString(buff, buffPtr, '\n');
                  buffPtr += line.length() + 1;
                  /* Check if blank line found, go to next stage */
                  if(line.length() <= 0){
                    break;
                  }
                  /* Get line label */
                  String label = getString(line.getBytes(), 0, ' ');
                  line = line.substring(label.length() + 1);
                  /* Figure out which header value we process */
                  switch(label){
                    case "tree" :
                      c.tree = line;
                      break;
                    case "parent" :
                      c.parent = line;
                      break;
                    case "author" :
                      c.author = PageBuilder.sanitize(
                        line.substring(0, line.indexOf('<') - 1)
                      );
                      c.author_email = PageBuilder.sanitize(line.substring(
                        line.indexOf('<') + 1, line.indexOf('>')
                      ));
                      Calendar aCal = Calendar.getInstance(
                        TimeZone.getTimeZone(line.substring(line.lastIndexOf(' ')))
                      );
                      aCal.setTimeInMillis(
                        Long.parseLong(
                          line.substring(line.indexOf('>') + 2,
                          line.lastIndexOf(' ')
                        )
                      ) * 1000L);
                      c.author_date = aCal.getTime();
                      break;
                    case "committer" :
                      c.commit = PageBuilder.sanitize(
                        line.substring(0, line.indexOf('<') - 1)
                      );
                      c.commit_email = PageBuilder.sanitize(line.substring(
                        line.indexOf('<') + 1, line.indexOf('>')
                      ));
                      Calendar cCal = Calendar.getInstance(
                        TimeZone.getTimeZone(line.substring(line.lastIndexOf(' ')))
                      );
                      cCal.setTimeInMillis(
                        Long.parseLong(
                          line.substring(line.indexOf('>') + 2,
                          line.lastIndexOf(' ')
                        )
                      ) * 1000L);
                      c.commit_date = cCal.getTime();
                      break;
                  }
                }
                /* Set the subject and store the commit */
                c.subject = PageBuilder.sanitize(getString(buff, buffPtr, '\n'));
                commits.put(c.hash, c);
                break;
              case "blob" :
                /* TODO: Implement blobs. */
                break;
              default :
                Main.warn("Unknown object type");
                break;
            }
          }
        }
      }
    }
  }

  /**
   * readPages()
   *
   * Pre-compute the location of pages for this git repository.
   *
   * @param commit The starting commit to begin generating pages from.
   **/
  private void readPages(Commit commit){
    pages = new ArrayList<Commit>();
    /* Keep going until all hashes found or we exhaust resources */
    for(int x = 0; commit != null && x < GIT_PAGE_MAX; x++){
      /* Save this page */
      pages.add(commit);
      /* Jump forward */
      for(int i = 0; commit != null && i < GIT_PAGE_SIZE; i++){
        commit = commits.get(commit.parent);
      }
    }
  }

  /**
   * getString()
   *
   * Get a String value from a raw data stream.
   *
   * @param data The data buffer to be read.
   * @param i The offset into the data stream to be converted.
   * @param d The delimiting character.
   * @return The String value retrieved.
   **/
  private static String getString(byte[] data, int i, char d){
    int e = i;
    while(e < data.length && data[e] != d){
      ++e;
    }
    return new String(data, i, e - i);
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
   * getHashRaw()
   *
   * Get a hash value from a raw data stream.
   *
   * @param data The data buffer to be read.
   * @param i The offset into the data stream to be converted.
   * @return The hash value retrieved.
   **/
  private static String getHashRaw(byte[] data, int i){
    byte[] var = new byte[GIT_HASH_DIGEST_RAW];
    System.arraycopy(data, i, var, 0, GIT_HASH_DIGEST_RAW);
    return new BigInteger(1, var).toString(16);
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
    if(i + GIT_HASH_DIGEST_STR < data.length){
      return new String(data, i, GIT_HASH_DIGEST_STR);
    }else{
      return null;
    }
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
   * entries()
   *
   * Get a list of entries being tracked by this repository.
   *
   * @param rootOnly If true, only root level entries are returned.
   * @return A list of entries tracked by this repository.
   **/
  public String[] entries(boolean rootOnly){
    ArrayList<String> ents = new ArrayList<String>();
    for(int x = 0; x < entries.length; x++){
      if(!rootOnly || (rootOnly && !entries[x].path.contains("/"))){
        ents.add(dir.getAbsolutePath() + "/" + entries[x].path);
      }
    }
    return ents.toArray(new String[0]);
  }

  /**
   * log()
   *
   * Get a list of commits, otherwise an empty list.
   *
   * @param page The start of the commits to be returned..
   * @return Array of commits.
   **/
  public Commit[] log(int page){
    Commit[] res = new Commit[GIT_PAGE_SIZE];
    /* Try to find a page */
    if(page < pages.size() && pages.get(page) != null){
      /* Store start of page */
      Commit c = pages.get(page);
      /* Now find additional commits */
      for(int x = 0; c != null && x < res.length; x++){
        /* Store this result */
        res[x] = c;
        /* Load next result */
        c = commits.get(c.parent);
      }
    }
    return res;
  }

  /**
   * commit()
   *
   * Get specific information about a given commit.
   *
   * @param hash The commit hash.
   * @return The commit object, otherwise NULL.
   **/
  public Commit commit(String hash){
    return commits.get(hash);
  }

  /**
   * diff()
   *
   * Get a code difference for a given commit.
   *
   * @param commit The commit name.
   * @return The raw diff.
   **/
  public String diff(String commit){
    /* Validate the commit to ensure no arbitrary command execution */
    if(!validCommit(commit)){
      Main.warn("Bad commit string");
      return new String();
    }
    byte[] buff = exec(
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
   * fetch()
   *
   * Fetch changes from the default origin repository.
   *
   * @return The raw output lines of the fetch.
   **/
  public String fetch(){
    if(!pull){
      return "";
    }
    byte[] buff = exec(
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
   * pull()
   *
   * Pull changes from the default origin repository.
   *
   * @return The raw output lines of the pull.
   **/
  public String pull(){
    if(!pull){
      return "";
    }
    byte[] buff = exec(
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
   * numCommits()
   *
   * Get the number of commits.
   *
   * @return The number of commits in this repository.
   **/
  public int numCommits(){
    return commits.size();
  }

  /**
   * getHead()
   *
   * Get the head commit.
   *
   * @return The head commit, otherwise NULL.
   **/
  public Commit getHead(){
    return commits.get(refs.get("heads_master"));
  }

  /**
   * exec()
   *
   * Execute a give command and return the output. Note that this command will
   * block until complete.
   *
   * @param cmd The command and parameters to be run.
   * @return The result of the command.
   **/
  private byte[] exec(String[] cmd){
    try{
      ProcessBuilder pb = new ProcessBuilder(cmd);
      pb.directory(dir);
      Process p = pb.start();
      p.waitFor();
      InputStream is = p.getInputStream();
      int a = is.available();
      byte[] buff = null;
      if(a > 0){
        buff = new byte[a < GIT_MAX_INPUT ? a : GIT_MAX_INPUT];
      }else{
        is = p.getErrorStream();
        a = is.available();
        buff = new byte[a < GIT_MAX_INPUT ? a : GIT_MAX_INPUT];
      }
      is.read(buff);
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
    if(commit != null && commit.length() == GIT_HASH_DIGEST_STR){
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
