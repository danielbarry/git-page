package b.gp;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Scanner;

/**
 * PageBuilder.java
 *
 * Build the output page to be displayed by the client.
 **/
public class PageBuilder{
  /**
   * MarkState.PageBuilder.java
   *
   * Track the state of the simple markdown parser.
   **/
  private class MarkState{
    public String line;
    public boolean code = false;
  }

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
  private static final byte[] HTTP_HEAD = (
    "HTTP/1.1 200 OK\r\n" +
    "Content-Type: text/html\r\n" +
    "\r\n"
  ).getBytes();
  private static final byte[] XML_HEAD = (
    "HTTP/1.1 200 OK\r\n" +
    "Content-Type: application/xml\r\n" +
    "\r\n"
  ).getBytes();

  private byte[] indexBad;
  private String reqPre;
  private String url;
  private HashMap<String, Git> repos;
  private byte[] pageHeader;

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
    indexBad = "Error".getBytes();
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
        indexBad = sConfig.get("error").value().getBytes();
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
    pageHeader = (
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
      "</tr></table>"
    ).getBytes();
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
    /* Store pre-string for this request */
    String pre = reqPre;
    /* Handle different cases */
    switch(req){
      case "?" :
        os.write(indexBad);
        break;
      default :
        /* Check for special string */
        if(req.startsWith(pre)){
          req = req.substring(pre.length());
        }
        String[] paths = req.split("/");
        /* Process the request */
        switch(paths.length){
          case 0 :
          case 1 :
            genHeader(os, pre, null);
            genRoot(os, pre);
            genFooter(os, pre, start);
            break;
          case 2 :
            genHeader(os, pre, paths[1]);
            genOverview(os, pre, paths[1]);
            genFooter(os, pre, start);
            break;
          case 3 :
            switch(paths[2]){
              case "commit" :
              case "diff" :
              case "page" :
                genHeader(os, pre, paths[1]);
                genPage(os, pre, paths[1], 0);
                genFooter(os, pre, start);
                break;
              case "rss" :
                genRSS(os, pre, paths[1]);
                break;
              default :
                genHeader(os, pre, paths[1]);
                os.write(indexBad);
                genFooter(os, pre, start);
                break;
            }
            break;
          case 4 :
            genHeader(os, pre, paths[1]);
            switch(paths[2]){
              case "commit" :
                genCommit(os, pre, paths[1], paths[3]);
                break;
              case "diff" :
                genDiff(os, pre, paths[1], paths[3]);
                break;
              case "page" :
                int page = 0;
                try{
                  page = Integer.parseInt(paths[3]);
                }catch(NumberFormatException e){
                  /* Fail silently */
                  page = 0;
                }
                genPage(os, pre, paths[1], page);
                break;
              default :
                os.write(indexBad);
                break;
            }
            genFooter(os, pre, start);
            break;
          default :
            genHeader(os, pre, paths[1]);
            os.write(indexBad);
            genFooter(os, pre, start);
            break;
        }
        break;
    }
  }

  /**
   * genHeader()
   *
   * Generate a header for the page.
   *
   * @param os The output stream to be written to.
   * @param pre Set the pre-string for any links.
   * @param proj The project name to be acted upon. If NULL, no project
   * navigation is displayed.
   **/
  private void genHeader(OutputStream os, String pre, String proj) throws IOException{
    /* Send the header early */
    os.write(HTTP_HEAD);
    /* Spit out pre-processed header */
    os.write(pageHeader);
    /* Project navigation if required */
    if(proj != null && repos.containsKey(proj)){
      String url = pre + "/" + proj;
      os.write((
        "<nav>" +
          "<a href=\"" + url + "\">" + proj + "</a> " +
          "<a href=\"" + url + "/commit\">Commits</a> " +
          "<a href=\"" + url + "/rss\">RSS</a>" +
        "</nav>"
      ).getBytes());
    }
  }

  /**
   * genFooter()
   *
   * Generate a footer for the page.
   *
   * @param os The output stream to be written to.
   * @param pre Set the pre-string for any links.
   * @param ts The timestamp processing began.
   **/
  private void genFooter(OutputStream os, String pre, long ts) throws IOException{
    os.write((
      "<hr>Generated in " +
      ((System.nanoTime() - ts) / 1000000) +
      "ms" +
      "</body></html>"
    ).getBytes());
  }

  /**
   * genRoot()
   *
   * Generate a root list of projects.
   *
   * @param os The output stream to be written to.
   * @param pre Set the pre-string for any links.
   **/
  private void genRoot(OutputStream os, String pre) throws IOException{
    String rootHTML = "<table>";
    for(String key : repos.keySet()){
      rootHTML += "<tr><td><a href=\"" + pre + "/" + key + "\">" + key + "</a></td></tr>";
    }
    rootHTML += "</table>";
    os.write(rootHTML.getBytes());
  }

  /**
   * genOverview()
   *
   * Generate the repository overview page.
   *
   * @param os The output stream to be written to.
   * @param pre Set the pre-string for any links.
   * @param proj The project name to be acted upon.
   **/
  private void genOverview(OutputStream os, String pre, String proj) throws IOException{
    /* Make sure the request params are valid */
    if(proj == null || !repos.containsKey(proj)){
      os.write(indexBad);
      return;
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
      os.write(overviewHTML.toString().getBytes());
    }else{
      os.write("No recognized overview found.".getBytes());
    }
  }

  /**
   * genPage()
   *
   * Generate a given page for a given project, otherwise display an error.
   *
   * @param os The output stream to be written to.
   * @param pre Set the pre-string for any links.
   * @param proj The project name to be acted upon.
   * @param page The page number of commits to display.
   **/
  private void genPage(OutputStream os, String pre, String proj, int page) throws IOException{
    /* Make sure the request params are valid */
    if(proj == null || !repos.containsKey(proj) || page < 0){
      os.write(indexBad);
      return;
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
    os.write(pageHTML.toString().getBytes());
  }

  /**
   * genCommit()
   *
   * Generate a given commit summary for a given project, otherwise display an
   * error.
   *
   * @param os The output stream to be written to.
   * @param pre Set the pre-string for any links.
   * @param proj The project name to be acted upon.
   * @param hash The commit hash to display a summary for.
   **/
  private void genCommit(OutputStream os, String pre, String proj, String hash) throws IOException{
    /* Make sure the request params are valid */
    if(
      proj == null             ||
      !repos.containsKey(proj) ||
      hash == null           ||
      !Git.validCommit(hash)
    ){
      os.write(indexBad);
      return;
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
      os.write(indexBad);
      return;
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
    os.write(commitHTML.toString().getBytes());
  }

  /**
   * genDiff()
   *
   * Generate the code difference for a given commit.
   *
   * @param os The output stream to be written to.
   * @param pre Set the pre-string for any links.
   * @param proj The project name to be acted upon.
   * @param commit The commit to display a summary for.
   **/
  private void genDiff(OutputStream os, String pre, String proj, String commit) throws IOException{
    /* Make sure the request params are valid */
    if(
      proj == null             ||
      !repos.containsKey(proj) ||
      commit == null           ||
      !Git.validCommit(commit)
    ){
      os.write(indexBad);
      return;
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
    os.write(diffHTML.toString().getBytes());
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
   * @param os The output stream to be written to.
   * @param pre Set the pre-string for any links.
   * @param proj The project name to be acted upon.
   **/
  private void genRSS(OutputStream os, String pre, String proj) throws IOException{
    /* Make sure the request params are valid */
    if(proj == null || !repos.containsKey(proj)){
      /* TODO: Not clear what to write if feed cannot be generated. */
      return;
    }
    /* Send the header early */
    os.write(XML_HEAD);
    /* Generate RSS headers */
    StringBuilder xml = new StringBuilder();
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
    os.write(xml.toString().getBytes());
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
