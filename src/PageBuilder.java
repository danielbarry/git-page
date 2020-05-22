package b.gp;

import java.io.IOException;
import java.io.OutputStream;

/**
 * PageBuilder.java
 *
 * Build the output page to be displayed by the client.
 **/
public class PageBuilder{
  /**
   * PageBuilder()
   *
   * Load all of the repositories that are to be displayed by the web server.
   *
   * @param repos The repositories to be managed.
   **/
  public PageBuilder(String[] repos){
    /* TODO: Do something with repos. */
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
      case "/" :
        /* TODO: Display all tracked repos. */
        break;
      case "?" :
        os.write("<tt><h1>404 :(</h1>Report this!</tt>".getBytes());
        break;
      default :
        String[] paths = req.split("/");
        try{
          os.write("<tt>".getBytes()); // TODO
          os.write(("<h1>Page Builder > " + req + "</h1>").getBytes()); // TODO
          os.write("Content here".getBytes()); // TODO
          os.write("</tt>".getBytes()); // TODO
        }catch(IOException e){
          /* Do nothing */
        }
        break;
    }
  }
}
