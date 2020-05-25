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

  private static final String[] INDEX_NAMES = new String[]{ "readme", "index" };
  private static final String[] INDEX_EXTS = new String[]{ "md", "markdown", "txt", "htm", "html" };
  private static final byte[] INDEX_BAD = "<h1>Bad Request</h1>".getBytes();

  private HashMap<String, File> repos;
  private String url;
  private byte[] pageHeader;

  /**
   * PageBuilder()
   *
   * Load all of the repositories that are to be displayed by the web server.
   *
   * @param repos The repositories to be managed.
   * @param url The URL to be used for the RSS feed.
   **/
  public PageBuilder(String[] repos, String url){
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
    this.url = url;
    /* Pre-process the page header */
    pageHeader = (
      /* Small amount of CSS */
      "<style>" +
        /* Make the table the width of the display */
        "table{width:100%;}" +
        /* Make rows more easily distinguishable */
        "tr:nth-child(even){background-color: #EEE;}" +
        "nav{" +
          /* Navigation should be easy to recognise */
          "background-color:#CCC;" +
          /* Flow the navigation bar correctly */
          "overflow:hidden;" +
          /* Allow "buttons" some space */
          "padding:8px;" +
        "}" +
        /* Make window specific bar a different colour */
        "nav.sub{background-color:#EEE;}" +
        "nav>a{" +
          /* "Buttons" should take up more space */
          "padding:8px;" +
          /* Remove hyperlink markup */
          "text-decoration:none;" +
        "}" +
        /* "Buttons" should hover to show they are interactive */
        "nav>a:hover{background-color:#AAA;}" +
        "pre{" +
          /* Code block should be visibly different */
          "background:#eee;" +
          /* Add a black bar to the left to really stand out */
          "border-left:4px solid #222;" +
          /* Code text shouldn't sit at the edges */
          "padding:4px;" +
        "}" +
      "</style>" +
      /* Header and core formatting */
      "<h1>Git Page</h1>" +
      /* Core navigation */
      "<nav>" +
      "<a href=\"/\">Home</a>" +
      "</nav>"
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
    /* Handle different cases */
    switch(req){
      case "?" :
        os.write(INDEX_BAD);
        break;
      default :
        String[] paths = req.split("/");
        switch(paths.length){
          case 0 :
          case 1 :
            genHeader(os, null);
            genRoot(os);
            genFooter(os, start);
            break;
          case 2 :
            genHeader(os, paths[1]);
            genOverview(os, paths[1]);
            genFooter(os, start);
            break;
          case 3 :
            switch(paths[2]){
              case "commit" :
              case "diff" :
              case "page" :
                genHeader(os, paths[1]);
                genPage(os, paths[1], 0);
                genFooter(os, start);
                break;
              case "rss" :
                genRSS(os, paths[1]);
                break;
              default :
                genHeader(os, paths[1]);
                os.write(INDEX_BAD);
                genFooter(os, start);
                break;
            }
            break;
          case 4 :
            genHeader(os, paths[1]);
            switch(paths[2]){
              case "commit" :
                genCommit(os, paths[1], paths[3]);
                break;
              case "diff" :
                genDiff(os, paths[1], paths[3]);
                break;
              case "page" :
                int page = 0;
                try{
                  page = Integer.parseInt(paths[3]);
                }catch(NumberFormatException e){
                  /* Fail silently */
                  page = 0;
                }
                genPage(os, paths[1], page);
                break;
              default :
                os.write(INDEX_BAD);
                break;
            }
            genFooter(os, start);
            break;
          default :
            os.write(INDEX_BAD);
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
   * @param proj The project name to be acted upon. If NULL, no project
   * navigation is displayed.
   **/
  private void genHeader(OutputStream os, String proj) throws IOException{
    /* Spit out pre-processed header */
    os.write(pageHeader);
    /* Project navigation if required */
    if(proj != null && repos.containsKey(proj)){
      os.write("<nav>".getBytes());
      os.write(("<a href=\"/" + proj + "\">" + proj + "</a> ").getBytes());
      os.write(("<a href=\"/" + proj + "/commit\">Commits</a> ").getBytes());
      os.write(("<a href=\"/" + proj + "/rss\">RSS</a>").getBytes());
      os.write("</nav>".getBytes());
    }
  }

  /**
   * genFooter()
   *
   * Generate a footer for the page.
   *
   * @param os The output stream to be written to.
   * @param ts The timestamp processing began.
   **/
  private void genFooter(OutputStream os, long ts) throws IOException{
    os.write((
      "<hr>Generated in " +
      ((System.nanoTime() - ts) / 1000000) +
      "ms"
    ).getBytes());
  }

  /**
   * genRoot()
   *
   * Generate a root list of projects.
   *
   * @param os The output stream to be written to.
   **/
  private void genRoot(OutputStream os) throws IOException{
    os.write("<table>".getBytes());
    for(String key : repos.keySet()){
      os.write(("<tr><td><a href=\"/" + key + "\">" + key + "</a></li></td></tr>").getBytes());
    }
    os.write("</table>".getBytes());
  }

  /**
   * genOverview()
   *
   * Generate the repository overview page.
   *
   * @param os The output stream to be written to.
   * @param proj The project name to be acted upon.
   **/
  private void genOverview(OutputStream os, String proj) throws IOException{
    /* Make sure the request params are valid */
    if(proj == null || !repos.containsKey(proj)){
      os.write(INDEX_BAD);
      return;
    }
    /* Find the overview page */
    File file = null;
    int ext = 0;
    File[] files = repos.get(proj).listFiles();
    for(int x = 0; x < files.length && file == null; x++){
      /* See if we get a match */
      if(files[x].isFile()){
        /* Loop index names */
        for(int i = 0; i < INDEX_NAMES.length && file == null; i++){
          /* Loop extensions */
          for(int e = 0; e < INDEX_EXTS.length && file == null; e++){
            String f = INDEX_NAMES[i] + "." + INDEX_EXTS[e];
            if(files[x].getName().toLowerCase().equals(f)){
              file = files[x];
              ext = e;
              break;
            }
          }
        }
      }
    }
    if(file != null){
      /* Pre-markup for text file */
      if(ext == 2){
        os.write("<pre><code>".getBytes());
      }else{
        /* Otherwise lets make sure all JS is disabled */
        os.write((
          "<script>" +
            "throw new Error(\"Disabled\");" +
            "return false;" +
            "die();" +
            "debugger;" +
          "</script>"
        ).getBytes());
      }
      MarkState ms = new MarkState();
      /* Load the file */
      Scanner s = new Scanner(file);
      while(s.hasNextLine()){
        switch(ext){
          /* Markdown */
          case 0 :
          case 1 :
            os.write(markup(s.nextLine(), ms).getBytes());
            break;
          /* Plain text */
          case 2 :
            os.write(sanitize(s.nextLine() + "\n").getBytes());
            break;
          /* HTML */
          case 3 :
          case 4 :
            os.write(s.nextLine().getBytes());
            break;
          default :
            Main.warn("Unsupported extension");
            break;
        }
      }
      s.close();
      /* Post-markup for text file */
      if(ext == 2){
        os.write("</code></pre>".getBytes());
      }
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
   * @param proj The project name to be acted upon.
   * @param page The page number of commits to display.
   **/
  private void genPage(OutputStream os, String proj, int page) throws IOException{
    /* Make sure the request params are valid */
    if(proj == null || !repos.containsKey(proj) || page < 0){
      os.write(INDEX_BAD);
      return;
    }
    /* Generate pages navigation */
    os.write("<nav class=\"sub\">".getBytes());
    if(page > 0){
      os.write(("<a href=\"/" + proj + "/page/" + (page - 1) + "\">Prev</a> < ").getBytes());
    }
    os.write(("<a href=\"/" + proj + "/page/" + page + "\">" + page + "</a> > ").getBytes());
    os.write(("<a href=\"/" + proj + "/page/" + (page + 1) + "\">Next</a>").getBytes());
    os.write("</nav>".getBytes());
    /* Fill out table */
    /* TODO: Not sure if tab character is a safe delimiter. */
    String[] logs = Git.gitLog(repos.get(proj), page * 16, 16, "\t");
    os.write("<table>".getBytes());
    for(int x = 0; x < logs.length; x++){
      String log[] = logs[x].split("\t");
      if(log.length == 5 && Git.validCommit(log[0])){
        /* Reduce length of commit message */
        if(log[4].length() > 32){
          log[4] = log[4].substring(0, 28) + "[..]";
        }
        /* Write the entry */
        os.write((
          "<tr>" +
            "<td><a href=\"/" + proj + "/commit/" + log[0] + "\">" + log[0] + "</td>" +
            "<td>" + sanitize(log[1]) + "</td>" +
            "<td>" + sanitize(log[2]) + "</td>" +
            "<td>" + sanitize(log[3]) + "</td>" +
            "<td>" + sanitize(log[4]) + "</td>" +
          "</tr>"
        ).getBytes());
      }else{
        Main.warn("Malformed commit messaged skipped");
      }
    }
    os.write("</table>".getBytes());
  }

  /**
   * genCommit()
   *
   * Generate a given commit summary for a given project, otherwise display an
   * error.
   *
   * @param os The output stream to be written to.
   * @param proj The project name to be acted upon.
   * @param commit The commit to display a summary for.
   **/
  private void genCommit(OutputStream os, String proj, String commit) throws IOException{
    /* Make sure the request params are valid */
    if(
      proj == null             ||
      !repos.containsKey(proj) ||
      commit == null           ||
      !Git.validCommit(commit)
    ){
      os.write(INDEX_BAD);
      return;
    }
    /* Generate pages navigation */
    os.write("<nav class=\"sub\">".getBytes());
    os.write(("<a href=\"/" + proj + "/commit/" + commit + "\">Summary</a> ").getBytes());
    os.write(("<a href=\"/" + proj + "/diff/" + commit + "\">Diff</a>").getBytes());
    os.write("</nav>".getBytes());
    /* Generate details */
    String[] details = Git.gitCommit(repos.get(proj), commit);
    /* Make sure they were generated! */
    if(details.length != 11){
      os.write(INDEX_BAD);
      return;
    }
    os.write("<table>".getBytes());
    os.write(("<tr><td>Hash</td><td>"            + sanitize(details[ 0]) + "</td></tr>").getBytes());
    os.write(("<tr><td>Tree Hash</td><td>"       + sanitize(details[ 1]) + "</td></tr>").getBytes());
    os.write(("<tr><td>Parent Hashes</td><td>"   + sanitize(details[ 2]) + "</td></tr>").getBytes());
    os.write(("<tr><td>Author Name</td><td>"     + sanitize(details[ 3]) + "</td></tr>").getBytes());
    os.write(("<tr><td>Author Email</td><td>"    + sanitize(details[ 4]) + "</td></tr>").getBytes());
    os.write(("<tr><td>Author Date</td><td>"     + sanitize(details[ 5]) + "</td></tr>").getBytes());
    os.write(("<tr><td>Committer Name</td><td>"  + sanitize(details[ 6]) + "</td></tr>").getBytes());
    os.write(("<tr><td>Committer Email</td><td>" + sanitize(details[ 7]) + "</td></tr>").getBytes());
    os.write(("<tr><td>Committer Date</td><td>"  + sanitize(details[ 8]) + "</td></tr>").getBytes());
    os.write(("<tr><td>Reference Names</td><td>" + sanitize(details[ 9]) + "</td></tr>").getBytes());
    os.write(("<tr><td>Subject</td><td>"         + sanitize(details[10]) + "</td></tr>").getBytes());
    os.write("</table>".getBytes());
  }

  /**
   * genDiff()
   *
   * Generate the code difference for a given commit.
   *
   * @param os The output stream to be written to.
   * @param proj The project name to be acted upon.
   * @param commit The commit to display a summary for.
   **/
  private void genDiff(OutputStream os, String proj, String commit) throws IOException{
    /* Make sure the request params are valid */
    if(
      proj == null             ||
      !repos.containsKey(proj) ||
      commit == null           ||
      !Git.validCommit(commit)
    ){
      os.write(INDEX_BAD);
      return;
    }
    /* Generate pages navigation */
    os.write("<nav class=\"sub\">".getBytes());
    os.write(("<a href=\"/" + proj + "/commit/" + commit + "\">Summary</a> ").getBytes());
    os.write(("<a href=\"/" + proj + "/diff/" + commit + "\">Diff</a>").getBytes());
    os.write("</nav>".getBytes());
    /* Generate details */
    String diff = Git.gitDiff(repos.get(proj), commit);
    os.write("<pre><code>".getBytes());
    os.write(sanitize(diff).getBytes());
    os.write("</code></pre>".getBytes());
  }

  /**
   * sanitize()
   *
   * Ensure that we sanitize any Strings from Git to not contain HTML.
   *
   * @param s The string to be sanitized.
   * @return The sanitized string.
   **/
  private static String sanitize(String s){
    return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
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
   * @param proj The project name to be acted upon.
   **/
  private void genRSS(OutputStream os, String proj) throws IOException{
    /* Make sure the request params are valid */
    if(proj == null || !repos.containsKey(proj)){
      /* TODO: Not clear what to write if feed cannot be generated. */
      return;
    }
    /* Generate RSS headers */
    os.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?><rss version=\"2.0\"><channel>".getBytes());
    os.write(("<title>" + proj + "</title>").getBytes());
    os.write(("<description>RSS feed for commits to " + proj + ".</description>").getBytes());
    os.write(("<link>" + url + "/" + proj + "</link>").getBytes());
    /* TODO: Not sure if tab character is a safe delimiter. */
    String[] logs = Git.gitLog(repos.get(proj), 0, 16, "\t");
    for(int x = 0; x < logs.length; x++){
      String log[] = logs[x].split("\t");
      if(log.length == 5 && Git.validCommit(log[0])){
        /* Reduce length of commit message */
        String title = log[4];
        if(title.length() > 32){
          title = title.substring(0, 28) + "[..]";
        }
        /* Write the item entry */
        os.write((
          "<item>" +
            "<title>" + sanitize(title) + "</title>" +
            "<guid>" + log[0] + "</guid>" +
            "<description>" +
              log[0] + " " +
              sanitize(log[1]) + " " +
              sanitize(log[2]) + " " +
              sanitize(log[3]) + " " +
              sanitize(log[4]) + " " +
            "</description>" +
            "<link>" + url + "/" + proj + "/commit/" + log[0] + "</link>" +
          "</item>"
        ).getBytes());
      }else{
        Main.warn("Malformed commit messaged skipped");
      }
    }
    /* Generate RSS footers */
    os.write("</channel></rss>".getBytes());
  }
}
