package b.gp;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

/**
 * PageBuilder.java
 *
 * Build the output page to be displayed by the client.
 **/
public class PageBuilder{
  private HashMap<String, File> repos;

  /**
   * PageBuilder()
   *
   * Load all of the repositories that are to be displayed by the web server.
   *
   * @param repos The repositories to be managed.
   **/
  public PageBuilder(String[] repos){
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
   * generate()
   *
   * Generate a page based on the request.
   *
   * @param os The output stream to write the page to.
   * @param req The request being made of the page builder.
   **/
  public void generate(OutputStream os, String req) throws IOException{
    /* Handle different cases */
    switch(req){
      case "?" :
        os.write("<tt><h1>Bad Request</h1></tt>".getBytes());
        break;
      default :
        String[] paths = req.split("/");
        switch(paths.length){
          case 0 :
          case 1 :
            genHeader(os, null);
            genRoot(os);
            genFooter(os);
            break;
          case 2 :
            genHeader(os, paths[1]);
            /* TODO: Should probably show readme. */
            genPage(os, paths[1], 0);
            genFooter(os);
            break;
          case 3 :
            genHeader(os, paths[1]);
            switch(paths[2]){
              case "commit" :
              case "page" :
                genPage(os, paths[1], 0);
                break;
              default :
                os.write("<tt><h1>Bad Request</h1></tt>".getBytes());
                break;
            }
            genFooter(os);
            break;
          case 4 :
            genHeader(os, paths[1]);
            switch(paths[2]){
              case "commit" :
                genCommit(os, paths[1], paths[3]);
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
                os.write("<tt><h1>Bad Request</h1></tt>".getBytes());
                break;
            }
            genFooter(os);
            break;
          default :
            os.write("<tt><h1>Bad Request</h1></tt>".getBytes());
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
    /* Header and core formatting */
    os.write("<tt><h1>Git Page</h1>".getBytes());
    /* Core navigation */
    os.write("<a href=\"/\">Home</a>".getBytes());
    os.write("<hr>".getBytes());
    /* Project navigation if required */
    if(proj != null && repos.containsKey(proj)){
      os.write(("<a href=\"/" + proj + "\">" + proj + "</a>").getBytes());
      os.write("<hr>".getBytes());
    }
  }

  /**
   * genFooter()
   *
   * Generate a footer for the page.
   *
   * @param os The output stream to be written to.
   **/
  private void genFooter(OutputStream os) throws IOException{
    os.write("</tt>".getBytes());
  }

  /**
   * genRoot()
   *
   * Generate a root list of projects.
   *
   * @param os The output stream to be written to.
   **/
  private void genRoot(OutputStream os) throws IOException{
    os.write("<ul>".getBytes());
    for(String key : repos.keySet()){
      os.write(("<li><a href=\"/" + key + "\">" + key + "</a></li>").getBytes());
    }
    os.write("</ul>".getBytes());
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
      os.write("<tt><h1>Bad Request</h1></tt>".getBytes());
      return;
    }
    /* Generate pages navigation */
    if(page > 0){
      os.write(("<a href=\"/" + proj + "/page/" + (page - 1) + "\">Prev</a> < ").getBytes());
    }
    os.write(("<a href=\"/" + proj + "/page/" + page + "\">" + page + "</a> > ").getBytes());
    os.write(("<a href=\"/" + proj + "/page/" + (page + 1) + "\">Next</a>").getBytes());
    os.write("<hr>".getBytes());
    /* Fill out table */
    /* TODO: Escape output from Git for HTML. */
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
            "<td><a href=\"/" + proj + "/commit/" + log[0] + "\">" + sanitize(log[0]) + "</td>" +
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
      os.write("<tt><h1>Bad Request</h1></tt>".getBytes());
      return;
    }
    /* Generate pages navigation */
    os.write(("<a href=\"/" + proj + "/commit/" + commit + "\">Summary</a>").getBytes());
    /* Generate details */
    String[] details = Git.gitCommit(repos.get(proj), commit);
    /* Make sure they were generated! */
    if(details.length != 11){
      os.write("<tt><h1>Bad Request</h1></tt>".getBytes());
      return;
    }
    /* TODO: Escape output from Git for HTML. */
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
}
