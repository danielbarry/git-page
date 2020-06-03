package b.gp;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;

/**
 * PageBuilder.java
 *
 * Build the output page to be displayed by the client.
 **/
public class PageBuilder{
  /**
   * Cache.PageBuilder.java
   *
   * Store a cache item and the relevant data to determine if it's still
   * up-to-date;
   **/
  private class Cache{
    public String index;
    public long timestamp;
    public long timeout;
    public Git repo;
    public byte[] payload;
    public boolean footer;
  }

  /**
   * MarkState.PageBuilder.java
   *
   * Track the state of the simple markdown parser.
   **/
  private class MarkState{
    public String line;
    public boolean code = false;
  }

  private static final long TIME_DAY_MS = 24 * 60 * 60 * 1000;
  private static final int CACHE_MAX = 256 * 256;
  private static final String[] INDEX_NAMES = new String[]{
    "readme",
    "index"
  };
  private static final String[] INDEX_EXTS = new String[]{
    "md",
    "markdown",
    "txt",
    "htm",
    "html"
  };
  private static final String HTTP_HEAD =
    "HTTP/1.1 200 OK\r\n" +
    "Content-Type: text/html\r\n" +
    "\r\n";
  private static final String XML_HEAD =
    "HTTP/1.1 200 OK\r\n" +
    "Content-Type: application/xml\r\n" +
    "\r\n";

  private String indexBad;
  private String reqPre;
  private String url;
  private HashMap<String, Git> repos;
  private String pageHeader;
  private HashMap<String, Cache> cache;

  /**
   * PageBuilder()
   *
   * Load all of the repositories that are to be displayed by the web server.
   *
   * @param repos The git repositories of interest.
   * @param config Access to the configuration data.
   **/
  public PageBuilder(HashMap<String, Git> repos, JSON config){
    /* Set sane defaults */
    String css = "";
    String title = "Git Page";
    String logo = "";
    indexBad = "Error";
    reqPre = "";
    url = "127.0.0.1";
    /* Make sure the configuration structure exists */
    if(config.get("repos") == null){
      Main.warn("No repository configuration provided");
      return;
    }
    /* Add repos to be monitored */
    this.repos = repos;
    /* Try to get page settings */
    if(config.get("page") != null){
      JSON sConfig = config.get("page");
      /* Try to set index bad */
      if(sConfig.get("error") != null && sConfig.get("error").value() != null){
        indexBad = sConfig.get("error").value();
        Main.log("Error set");
      }
      /* Try to set CSS */
      if(sConfig.get("css") != null && sConfig.get("css").length() > 0){
        css = "";
        JSON cssConfig = sConfig.get("css");
        for(int x = 0; x < cssConfig.length(); x++){
          css += cssConfig.get(x).value();
        }
        Main.log("CSS set");
      }
      /* Try to set request title */
      if(sConfig.get("title") != null && sConfig.get("title").value() != null){
        title = sConfig.get("title").value();
        Main.log("Title set to '" + title + "'");
      }
      /* Try to set request logo */
      if(sConfig.get("logo") != null && sConfig.get("logo").value() != null){
        logo = sConfig.get("logo").value();
        Main.log("Logo set");
      }
    }else{
      Main.warn("No configuration found for server");
    }
    /* Try to get server settings */
    if(config.get("server") != null){
      JSON sConfig = config.get("server");
      /* Try to set request pre-string */
      if(sConfig.get("url-sub") != null && sConfig.get("url-sub").value() != null){
        reqPre = sConfig.get("url-sub").value();
        Main.log("Request pre-string set to '" + reqPre + "'");
      }
      /* Try to set URL */
      if(sConfig.get("url") != null && sConfig.get("url").value() != null){
        url = sConfig.get("url").value();
        Main.log("Request URL set to '" + url + "'");
      }
    }else{
      Main.warn("No configuration found for server");
    }
    /* Pre-process the page header */
    pageHeader =
      /* Tell the browser what we are */
      "<!DOCTYPE html><html>" +
      /* Define the page title */
      "<head><title>" + title + "</title></head>" +
      /* Small amount of CSS */
      "<style type=\"text/css\">" + css + "</style>" +
      /* Start body */
      "<body>" +
      /* Header and core formatting */
      "<table><tr>" +
        /* Display SVG logo */
        "<td style=\"width:0px\">" + logo + "</td>" +
        /* Core navigation */
        "<td><nav>" +
        "<b>" + title + "</b> > " +
        "<a href=\"" + reqPre + "/\">Home</a>" +
        "</nav></td>" +
      "</tr></table>";
    /* Setup page cache */
    cache = new HashMap<String, Cache>();
  }

  /**
   * generate()
   *
   * Generate a page based on the request.
   *
   * @param os The output stream to write the page to.
   * @param req The request being made of the page builder.
   **/
  public void generate(OutputStream os, String req) throws IOException{
    /* Store entry timestamp */
    long start = System.nanoTime();
    long startMs = System.currentTimeMillis();
    /* Check if we can potentially serve out of cache */
    Cache c = cache.get(req);
    if(c != null && c.index.equals(req)){
      /* If there is an associated repo, make sure it's still valid */
      if(
        ((c.repo != null && c.timestamp == c.repo.lastUpdate()) ||
         (c.repo == null                                      )) &&
        ((c.timeout != 0 && c.timeout > startMs               ) ||
         (c.timeout == 0                                      ))
      ){
        os.write(c.payload);
        /* Should we also output a footer? */
        if(c.footer){
          os.write(genFooter(start).getBytes());
        }
        return;
      }
    }
    /* Allow cache garbage collection if needed */
    gcCache();
    /* Store pre-string for this request */
    String pre = reqPre;
    /* Handle different cases */
    switch(req){
      case "?" :
        os.write((
          genHeader(pre, null) +
          indexBad +
          genFooter(start)
        ).getBytes());
        break;
      default :
        /* Check for special string */
        String reqSub = req;
        if(req.startsWith(pre)){
          reqSub = reqSub.substring(pre.length());
        }
        String[] paths = reqSub.split("/");
        /* Process the request */
        switch(paths.length){
          case 0 :
          case 1 :
            os.write(updateCache(
              req,
              null,
              true,
              0,
              genHeader(pre, null) +
              genRoot(pre)
            ));
            os.write(genFooter(start).getBytes());
            break;
          case 2 :
            os.write(updateCache(
              req,
              paths[1],
              true,
              TIME_DAY_MS,
              genHeader(pre, paths[1]) +
              genOverview(pre, paths[1])
            ));
            os.write(genFooter(start).getBytes());
            break;
          case 3 :
            switch(paths[2]){
              case "commit" :
              case "diff" :
              case "page" :
                os.write(updateCache(
                  req,
                  paths[1],
                  true,
                  0,
                  genHeader(pre, paths[1]) +
                  genPage(pre, paths[1], 0)
                ));
                os.write(genFooter(start).getBytes());
                break;
              case "rss" :
                os.write(updateCache(
                  req,
                  paths[1],
                  false,
                  0,
                  genRSS(pre, paths[1])
                ));
                break;
              default :
                os.write((
                  genHeader(pre, paths[1]) +
                  indexBad +
                  genFooter(start)
                ).getBytes());
                break;
            }
            break;
          case 4 :
            switch(paths[2]){
              case "commit" :
                os.write(updateCache(
                  req,
                  paths[1],
                  true,
                  0,
                  genHeader(pre, paths[1]) +
                  genCommit(pre, paths[1], paths[3])
                ));
                os.write(genFooter(start).getBytes());
                break;
              case "diff" :
                os.write(updateCache(
                  req,
                  paths[1],
                  true,
                  0,
                  genHeader(pre, paths[1]) +
                  genDiff(pre, paths[1], paths[3])
                ));
                os.write(genFooter(start).getBytes());
                break;
              case "page" :
                int page = 0;
                try{
                  page = Integer.parseInt(paths[3]);
                }catch(NumberFormatException e){
                  /* Fail silently */
                  page = 0;
                }
                os.write(updateCache(
                  req,
                  paths[1],
                  true,
                  0,
                  genHeader(pre, paths[1]) +
                  genPage(pre, paths[1], page)
                ));
                os.write(genFooter(start).getBytes());
                break;
              default :
                os.write((
                  genHeader(pre, paths[1]) +
                  indexBad +
                  genFooter(start)
                ).getBytes());
                break;
            }
            break;
          default :
            os.write((
              genHeader(pre, paths[1]) +
              indexBad +
              genFooter(start)
            ).getBytes());
            break;
        }
        break;
    }
  }

  /**
   * updateCache()
   *
   * Update the server cache and return the payload to be output.
   *
   * @param hash The hash to associated with the payload.
   * @param repo The repository to associate with the content to be served.
   * @param footer Allow a footer to be generated after cache served.
   * @param timeout When to re-process this cache entry, is set to zero ignore.
   * @param payload The entire payload to be served up to the user.
   * @return The processed payload.
   **/
  private byte[] updateCache(
    String hash,
    String repo,
    boolean footer,
    long timeout,
    String payload
  ){
    Cache c = new Cache();
    if(repo != null){
      c.repo = repos.get(repo);
    }
    if(c.repo != null){
      c.timestamp = c.repo.lastUpdate();
    }else{
      c.timestamp = System.currentTimeMillis();
    }
    c.index = hash;
    if(timeout != 0){
      c.timeout = timeout + c.timestamp;
    }else{
      c.timeout = timeout;
    }
    c.footer = footer;
    c.payload = payload.getBytes();
    cache.put(hash, c);
    return c.payload;
  }

  /**
   * gcCache()
   *
   * Randomly delete items from the cash if the maximum cache size is breached.
   **/
  private void gcCache(){
    if(cache.size() > CACHE_MAX){
      Main.log("Garbage collecting cache");
      int r = (new Random()).nextInt(256);
      String[] keys = cache.keySet().toArray(new String[0]);
      for(int x = r; x < keys.length; x += r){
        cache.remove(keys[x]);
      }
      Main.log("Cache garbage collection complete");
    }
  }

  /**
   * genHeader()
   *
   * Generate a header for the page.
   *
   * @param pre Set the pre-string for any links.
   * @param proj The project name to be acted upon. If NULL, no project
   * navigation is displayed.
   * @return The content.
   **/
  private String genHeader(String pre, String proj) throws IOException{
    StringBuilder header = new StringBuilder();
    /* Send the header early */
    header.append(HTTP_HEAD);
    /* Spit out pre-processed header */
    header.append(pageHeader);
    /* Project navigation if required */
    if(proj != null && repos.containsKey(proj)){
      String url = pre + "/" + proj;
      header.append("<nav>");
      header.append(  "<a href=\"");
      header.append(    url);
      header.append(    "\">");
      header.append(    proj);
      header.append(  "</a> ");
      header.append(  "<a href=\"");
      header.append(    url);
      header.append(    "/commit\">Commits</a> ");
      header.append(  "<a href=\"");
      header.append(    url);
      header.append(    "/rss\">RSS</a>");
      header.append("</nav>");
    }
    return header.toString();
  }

  /**
   * genFooter()
   *
   * Generate a footer for the page.
   *
   * @param ts The timestamp processing began.
   * @return The content.
   **/
  private String genFooter(long ts) throws IOException{
    return
      "<hr>Generated in " +
      ((System.nanoTime() - ts) / 1000000) +
      "ms" +
      "</body></html>";
  }

  /**
   * genRoot()
   *
   * Generate a root list of projects.
   *
   * @param pre Set the pre-string for any links.
   * @return The content.
   **/
  private String genRoot(String pre) throws IOException{
    StringBuilder rootHTML = new StringBuilder();
    rootHTML.append("<table>");
    for(String key : repos.keySet()){
      rootHTML.append("<tr><td><a href=\"");
      rootHTML.append(  pre);
      rootHTML.append(  "/");
      rootHTML.append(  key);
      rootHTML.append(  "\">");
      rootHTML.append(  key);
      rootHTML.append(  "</a></td></tr>");
    }
    rootHTML.append("</table>");
    return rootHTML.toString();
  }

  /**
   * genOverview()
   *
   * Generate the repository overview page.
   *
   * @param pre Set the pre-string for any links.
   * @param proj The project name to be acted upon.
   * @return The content.
   **/
  private String genOverview(String pre, String proj) throws IOException{
    /* Make sure the request params are valid */
    if(proj == null || !repos.containsKey(proj)){
      return indexBad;
    }
    /* Find the overview page */
    File file = null;
    int ext = 0;
    String[] files = repos.get(proj).entries(true);
    for(int x = 0; x < files.length && file == null; x++){
      File test = new File(files[x]);
      /* Loop index names */
      for(int i = 0; i < INDEX_NAMES.length && file == null; i++){
        /* Loop extensions */
        for(int e = 0; e < INDEX_EXTS.length && file == null; e++){
          String f = INDEX_NAMES[i] + "." + INDEX_EXTS[e];
          if(test.getName().toLowerCase().equals(f)){
            file = test;
            ext = e;
            break;
          }
        }
      }
    }
    if(file != null){
      StringBuilder overviewHTML = new StringBuilder();
      /* Display repository stats */
      overviewHTML.append("<nav class=\"sub\">");
      overviewHTML.append(  "Commits: ");
      overviewHTML.append(  Integer.toString(repos.get(proj).numCommits()));
      Git.Commit c = repos.get(proj).getHead();
      if(c != null){
        overviewHTML.append(" | Latest: ");
        overviewHTML.append("<a href=\"");
        overviewHTML.append(  pre);
        overviewHTML.append(  "/");
        overviewHTML.append(  proj);
        overviewHTML.append(  "/commit/");
        overviewHTML.append(  c.hash);
        overviewHTML.append(  "\">");
        overviewHTML.append(    c.hash.substring(0, 7));
        overviewHTML.append("</a>");
        overviewHTML.append(" committed by ");
        overviewHTML.append(c.author);
        overviewHTML.append(", ");
        overviewHTML.append(
          TimeUnit.DAYS.convert(
            System.currentTimeMillis() - c.author_date.getTime(),
            TimeUnit.MILLISECONDS
          )
        );
        overviewHTML.append(" days ago");
      }
      overviewHTML.append("</nav>");
      /* Pre-markup for text file */
      if(ext == 2){
        overviewHTML.append("<pre><code>");
      }else{
        /* Otherwise lets make sure all JS is disabled */
        overviewHTML.append("<script>");
        overviewHTML.append(  "throw new Error(\"Disabled\");");
        overviewHTML.append(  "return false;");
        overviewHTML.append(  "die();");
        overviewHTML.append(  "debugger;");
        overviewHTML.append("</script>");
      }
      MarkState ms = new MarkState();
      /* Load the file */
      Scanner s = new Scanner(file);
      while(s.hasNextLine()){
        switch(ext){
          /* Markdown */
          case 0 :
          case 1 :
            overviewHTML.append(markup(s.nextLine(), ms));
            break;
          /* Plain text */
          case 2 :
            overviewHTML.append(sanitize(s.nextLine()));
            overviewHTML.append("\n");
            break;
          /* HTML */
          case 3 :
          case 4 :
            overviewHTML.append(s.nextLine());
            break;
          default :
            Main.warn("Unsupported extension");
            break;
        }
      }
      s.close();
      /* Post-markup for text file */
      if(ext == 2){
        overviewHTML.append("</code></pre>");
      }
      return overviewHTML.toString();
    }else{
      return "No recognized overview found.";
    }
  }

  /**
   * genPage()
   *
   * Generate a given page for a given project, otherwise display an error.
   *
   * @param pre Set the pre-string for any links.
   * @param proj The project name to be acted upon.
   * @param page The page number of commits to display.
   * @return The content.
   **/
  private String genPage(String pre, String proj, int page) throws IOException{
    /* Make sure the request params are valid */
    if(proj == null || !repos.containsKey(proj) || page < 0){
      return indexBad;
    }
    /* Generate pages navigation */
    StringBuilder pageHTML = new StringBuilder();
    pageHTML.append("<nav class=\"sub\">");
    if(page > 0){
      pageHTML.append("<a href=\"");
      pageHTML.append(  pre);
      pageHTML.append(  "/");
      pageHTML.append(  proj);
      pageHTML.append(  "/page/");
      pageHTML.append(  (page - 1));
      pageHTML.append(  "\">Prev</a> < ");
    }
    pageHTML.append("<a href=\"");
    pageHTML.append(  pre);
    pageHTML.append(  "/");
    pageHTML.append(  proj);
    pageHTML.append(  "/page/");
    pageHTML.append(  page);
    pageHTML.append(  "\">");
    pageHTML.append(  page);
    pageHTML.append(  "</a> > ");
    pageHTML.append("<a href=\"");
    pageHTML.append(  pre);
    pageHTML.append(  "/");
    pageHTML.append(  proj);
    pageHTML.append(  "/page/");
    pageHTML.append(  (page + 1));
    pageHTML.append(  "\">Next</a>");
    pageHTML.append("</nav>");
    /* Fill out table */
    Git.Commit[] logs = repos.get(proj).log(page);
    pageHTML.append("<table>");
    for(int x = 0; x < logs.length; x++){
      if(logs[x] != null){
        /* Reduce length of commit message */
        String subject = logs[x].subject;
        if(subject != null && subject.length() > 32){
          subject = subject.substring(0, 30) + "..";
        }
        /* Write the entry */
        pageHTML.append("<tr>");
        pageHTML.append(  "<td><a href=\"");
        pageHTML.append(    pre);
        pageHTML.append(    "/");
        pageHTML.append(    proj);
        pageHTML.append(    "/commit/");
        pageHTML.append(    logs[x].hash);
        pageHTML.append(    "\">");
        pageHTML.append(      logs[x].hash.substring(0, 7));
        pageHTML.append(  "</a></td>");
        pageHTML.append(  "<td>");
        pageHTML.append(    logs[x].author_date.toString());
        pageHTML.append(  "</td>");
        pageHTML.append(  "<td>");
        pageHTML.append(    logs[x].author);
        pageHTML.append(  "</td>");
        pageHTML.append(  "<td>");
        pageHTML.append(    subject);
        pageHTML.append(  "</td>");
        pageHTML.append("</tr>");
      }
    }
    pageHTML.append("</table>");
    return pageHTML.toString();
  }

  /**
   * genCommit()
   *
   * Generate a given commit summary for a given project, otherwise display an
   * error.
   *
   * @param pre Set the pre-string for any links.
   * @param proj The project name to be acted upon.
   * @param hash The commit hash to display a summary for.
   * @return The content.
   **/
  private String genCommit(String pre, String proj, String hash) throws IOException{
    /* Make sure the request params are valid */
    if(
      proj == null             ||
      !repos.containsKey(proj) ||
      hash == null           ||
      !Git.validCommit(hash)
    ){
      return indexBad;
    }
    /* Generate pages navigation */
    StringBuilder commitHTML = new StringBuilder();
    commitHTML.append("<nav class=\"sub\">");
    commitHTML.append("<a href=\"");
    commitHTML.append(  pre);
    commitHTML.append(  "/");
    commitHTML.append(  proj);
    commitHTML.append(  "/commit/");
    commitHTML.append(  hash);
    commitHTML.append(  "\">Summary</a> ");
    commitHTML.append("<a href=\"");
    commitHTML.append(  pre);
    commitHTML.append(  "/");
    commitHTML.append(  proj);
    commitHTML.append(  "/diff/");
    commitHTML.append(  hash);
    commitHTML.append(  "\">Diff</a>");
    commitHTML.append("</nav>");
    /* Generate details */
    Git.Commit commit = repos.get(proj).commit(hash);
    /* Make sure it exists */
    if(commit == null){
      return indexBad;
    }
    commitHTML.append("<table>");
    commitHTML.append(  "<tr><td>Hash</td><td><a href=\"");
    commitHTML.append(    pre);
    commitHTML.append(    "/");
    commitHTML.append(    proj);
    commitHTML.append(    "/commit/");
    commitHTML.append(    commit.hash);
    commitHTML.append(    "\">");
    commitHTML.append(    commit.hash);
    commitHTML.append(  "</a></td></tr>");
    commitHTML.append(  "<tr><td>Tree</td><td>");
    commitHTML.append(    commit.tree);
    commitHTML.append(  "</a></td></tr>");
    commitHTML.append(  "<tr><td>Parents</td><td><a href=\"");
    commitHTML.append(    pre);
    commitHTML.append(    "/");
    commitHTML.append(    proj);
    commitHTML.append(    "/commit/");
    commitHTML.append(    commit.parent);
    commitHTML.append(    "\">");
    commitHTML.append(    commit.parent);
    commitHTML.append(  "</a></td></tr>");
    commitHTML.append(  "<tr><td>Author Name</td><td>");
    commitHTML.append(    commit.author);
    commitHTML.append(  "</td></tr>");
    commitHTML.append(  "<tr><td>Author Email</td><td>");
    commitHTML.append(    commit.author_email);
    commitHTML.append(  "</td></tr>");
    commitHTML.append(  "<tr><td>Author Date</td><td>");
    commitHTML.append(    commit.author_date.toString());
    commitHTML.append(  "</td></tr>");
    commitHTML.append(  "<tr><td>Committer Name</td><td>");
    commitHTML.append(    commit.commit);
    commitHTML.append(  "</td></tr>");
    commitHTML.append(  "<tr><td>Committer Email</td><td>");
    commitHTML.append(    commit.commit_email);
    commitHTML.append(  "</td></tr>");
    commitHTML.append(  "<tr><td>Committer Date</td><td>");
    commitHTML.append(    commit.commit_date.toString());
    commitHTML.append(  "</td></tr>");
    commitHTML.append(  "<tr><td>Subject</td><td>");
    commitHTML.append(    commit.subject);
    commitHTML.append(  "</td></tr>");
    commitHTML.append("</table>");
    return commitHTML.toString();
  }

  /**
   * genDiff()
   *
   * Generate the code difference for a given commit.
   *
   * @param pre Set the pre-string for any links.
   * @param proj The project name to be acted upon.
   * @param commit The commit to display a summary for.
   * @return The content.
   **/
  private String genDiff(String pre, String proj, String commit) throws IOException{
    /* Make sure the request params are valid */
    if(
      proj == null             ||
      !repos.containsKey(proj) ||
      commit == null           ||
      !Git.validCommit(commit)
    ){
      return indexBad;
    }
    /* Generate pages navigation */
    StringBuilder diffHTML = new StringBuilder();
    diffHTML.append("<nav class=\"sub\">");
    diffHTML.append("<a href=\"");
    diffHTML.append(  pre);
    diffHTML.append(  "/");
    diffHTML.append(  proj);
    diffHTML.append(  "/commit/");
    diffHTML.append(  commit);
    diffHTML.append(  "\">Summary</a> ");
    diffHTML.append("<a href=\"");
    diffHTML.append(  pre);
    diffHTML.append(  "/");
    diffHTML.append(  proj);
    diffHTML.append(  "/diff/");
    diffHTML.append(  commit);
    diffHTML.append(  "\">Diff</a>");
    diffHTML.append("</nav>");
    /* Generate details */
    String diff = repos.get(proj).diff(commit);
    diffHTML.append("<pre><code>");
    diffHTML.append(  sanitize(diff));
    diffHTML.append("</code></pre>");
    return diffHTML.toString();
  }

  /**
   * sanitize()
   *
   * Ensure that we sanitize any Strings from Git to not contain HTML.
   *
   * @param s The string to be sanitized.
   * @return The sanitized string.
   **/
  public static String sanitize(String s){
    StringBuilder res = new StringBuilder();
    int x = 0;
    int i = x;
    /* Search and replace special characters */
    for(; x < s.length(); x++){
      switch(s.charAt(x)){
        case '&' :
          res.append(s.substring(i, x));
          res.append("&amp;");;
          i = x + 1;
          break;
        case '<' :
          res.append(s.substring(i, x));
          res.append("&lt;");;
          i = x + 1;
          break;
        case '>' :
          res.append(s.substring(i, x));
          res.append("&gt;");;
          i = x + 1;
          break;
      }
    }
    /* Compose remaining parts of string */
    if(x != i){
      res.append(s.substring(i, x));
    }
    return res.toString();
  }

  /**
   * markup()
   *
   * Provide very basic markup for markdown files.
   *
   * @param s The input line and ignore the global context.
   * @param ms The current markdown parser state.
   * @return The marked up line.
   **/
  private static String markup(String s, MarkState ms){
    /* Check line is worth parsing */
    if(s == null){
      return s;
    }
    /* If line is blank, throw a new line in */
    if(s.length() <= 0){
      return "<br>";
    }
    /* Check pre depth 3 */
    if(s.length() >= 3){
      /* Pre: Check for code */
      if(s.charAt(0) == '`' && s.charAt(1) == '`' && s.charAt(2) == '`'){
        if(!ms.code){
          ms.code = true;
          return "<pre><code>";
        }else{
          ms.code = false;
          return "</code></pre>";
        }
      }
    }
    /* If we're processing a code block we know what to do */
    if(ms.code){
      return sanitize(s) + "\n";
    }
    /* Check pre depth 2 */
    if(s.length() >= 2){
      /* Pre: Check for headers */
      if(s.charAt(0) == '#'){
        /* Check for H2 */
        if(s.charAt(1) != '#'){
          s = "<h1>" + s + "</h1>";
        }else{
          s = "<h2>" + s + "</h2>";
        }
      }
    }
    /* Check pre depth 4 */
    if(s.length() >= 4){
      /* Pre: Check for code */
      if(s.charAt(0) == s.charAt(1) && s.charAt(2) == s.charAt(3) && s.charAt(0) == ' '){
        return "<pre><code>" + sanitize(s) + "</code></pre><br>";
      }
    }
    /* Return whatever we have left */
    return s + "<br>";
  }

  /**
   * genRSS()
   *
   * Generate an RSS feed for a given project.
   *
   * @param pre Set the pre-string for any links.
   * @param proj The project name to be acted upon.
   * @return The content.
   **/
  private String genRSS(String pre, String proj) throws IOException{
    /* Make sure the request params are valid */
    if(proj == null || !repos.containsKey(proj)){
      return "";
    }
    StringBuilder xml = new StringBuilder();
    /* Send the header early */
    xml.append(XML_HEAD);
    /* Generate RSS headers */
    xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?><rss version=\"2.0\"><channel>");
    xml.append("<title>");
    xml.append(  proj);
    xml.append("</title>");
    xml.append("<description>RSS feed for commits to ");
    xml.append(  proj);
    xml.append(".</description>");
    xml.append("<link>");
      xml.append(url);
      xml.append(pre);
      xml.append("/");
      xml.append(proj);
    xml.append("</link>");
    Git.Commit[] logs = repos.get(proj).log(0);
    for(int x = logs.length - 1; x >= 0; x--){
      if(logs[x] != null){
        /* Reduce length of commit message */
        String subject = logs[x].subject;
        if(subject != null && subject.length() > 32){
          subject = subject.substring(0, 30) + "..";
        }
        /* Write the item entry */
        xml.append("<item>");
        xml.append(  "<title>");
        xml.append(    subject);
        xml.append(  "</title>");
        xml.append(  "<author>");
        xml.append(    logs[x].author);
        xml.append(  "</author>");
        xml.append(  "<pubDate>");
        xml.append(    intToDay(logs[x].author_date.getDay()));
        xml.append(    ", ");
        xml.append(    logs[x].author_date.toGMTString());
        xml.append(  "</pubDate>");
        xml.append(  "<description>");
        xml.append(    logs[x].subject);
        xml.append(  "</description>");
        xml.append(  "<link>");
        xml.append(    url);
        xml.append(    pre);
        xml.append(    "/");
        xml.append(    proj);
        xml.append(    "/commit/");
        xml.append(    logs[x].hash);
        xml.append(  "</link>");
        xml.append("</item>");
      }
    }
    /* Generate RSS footers */
    xml.append("</channel></rss>");
    return xml.toString();
  }

  /**
   * intToDay()
   *
   * Helper functions for the RSS feed.
   **/
  private static String intToDay(int day){
    switch(day){
      case 0 :
        return "Sun";
      case 1 :
        return "Mon";
      case 2 :
        return "Tue";
      case 3 :
        return "Wed";
      case 4 :
        return "Thu";
      case 5 :
        return "Fri";
      case 6 :
        return "Sat";
      case 7 :
        return "Sun";
      default :
        return "";
    }
  }
}
