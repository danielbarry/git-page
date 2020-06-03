package b.gp;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * JSON.java
 *
 * A single file JSON parser.
 **/
public class JSON{
  private static final int TYPE_OBJ = 1;
  private static final int TYPE_ARR = 2;
  private static final int TYPE_STR = 3;

  private int rawLen;
  private int type;
  private String key;
  private String val;
  private ArrayList<JSON> childs;

  /**
   * JSON()
   *
   * Parse the JSON string and generate the relevant children objects.
   *
   * @param json The valid JSON input String.
   * @param offset The offset to start parsing from in the String.
   **/
  private JSON(String json, int offset) throws Exception{
    /* Setup internal variables */
    rawLen = 0;
    type = TYPE_STR;
    key = null;
    val = null;
    childs = null;
    /* Parse for object type */
    boolean string = false;
    boolean escape = false;
    boolean keyFill = true;
    for(int x = offset; x < json.length(); x++){
      char c = json.charAt(x);
      /* Parse JSON structure */
      if(!string){
        switch(c){
          case '{' :
          case '[' :
            /* Set type if not set */
            if(type == TYPE_STR){
              /* Check special case root object */
              if(offset > 0){
                type = c == '{' ? TYPE_OBJ : TYPE_ARR;
              }else{
                offset = 1;
              }
            /* Handle this child object */
            }else{
              if(childs == null){
                childs = new ArrayList<JSON>();
              }
              JSON child = new JSON(json, x);
              childs.add(child);
              x += child.getRawLen();
            }
            break;
          case '}' :
          case ']' :
          case ',' :
            /* End of this object */
            rawLen = x - offset;
            return;
          case '"' :
            if(type == TYPE_STR){
              string = !string;
            }else{
              if(childs == null){
                childs = new ArrayList<JSON>();
              }
              JSON child = new JSON(json, x);
              childs.add(child);
              x += child.getRawLen();
            }
            break;
          case ':' :
            keyFill = false;
            break;
        }
      /* Parse JSON string */
      }else{
        /* If not an escape character */
        if(c != '\\' || escape){
          /* Flip string state if not escaping */
          if(c == '"' && !escape){
            string = !string;
          }
          /* Pre-escape if needed */
          if(escape){
            switch(c){
              case 'b' :
                c = '\b';
                break;
              case 'f' :
                c = '\f';
                break;
              case 'n' :
                c = '\n';
                break;
              case 'r' :
                c = '\r';
                break;
              case 't' :
                c = '\t';
                break;
              case '"' :
                c = '"';
                break;
              case '\\' :
                c = '\\';
                break;
              default :
                throw new Exception("Invalid escape sequence");
            }
            escape = false;
          }
          /* Are we still in the string? */
          if(string){
            /* Where are we putting this? */
            if(keyFill){
              if(key != null){
                key += c;
              }else{
                key = "" + c;
              }
            }else{
              if(val != null){
                val += c;
              }else{
                val = "" + c;
              }
            }
          }
        /* Handle escape case */
        }else{
          escape = true;
        }
      }
    }
  }

  /**
   * JSON()
   *
   * Parse the JSON string and generate the relevant children objects.
   *
   * @param json The valid JSON input String.
   **/
  public JSON(String json) throws Exception{
    this('{' + json + '}', 0);
  }

  /**
   * build()
   *
   * A factory builder to parse the JSON string from a file and generate the
   * relevant child objects.
   *
   * @param filename A valid file containing the JSON data.
   **/
  public static JSON build(String filename) throws Exception{
    /* Check that the file is valid */
    File file = new File(filename);
    if(file == null || !file.isFile()){
      throw new Exception("Unable to find JSON file");
    }
    /* Load the file */
    String json = "";
    Scanner s = new Scanner(file);
    while(s.hasNextLine()){
      json += s.nextLine();
    }
    s.close();
    /* Parse from root */
    return new JSON(json);
  }

  /**
   * getRawLen()
   *
   * Get the raw parser length of what was parsed. This is intended to be used
   * by the constructor during parsing.
   *
   * @return The raw parser length in number of bytes.
   **/
  public int getRawLen(){
    return rawLen;
  }

  /**
   * isObject()
   *
   * Check whether this is an object.
   *
   * @param True if an object, otherwise false.
   **/
  public boolean isObject(){
    return type == TYPE_OBJ;
  }

  /**
   * isArray()
   *
   * Check whether this is an array.
   *
   * @param True if an array, otherwise false.
   **/
  public boolean isArray(){
    return type == TYPE_ARR;
  }

  /**
   * isString()
   *
   * Check whether this is an string.
   *
   * @param True if an string, otherwise false.
   **/
  public boolean isString(){
    return type == TYPE_STR;
  }

  /**
   * key()
   *
   * Get the key for this JSON object. NOTE: Array elements may not have a key.
   *
   * @param def The default value to return if the key is not set.
   * @return The key, otherwise the default key.
   **/
  public String key(String def){
    return key != null ? key : def;
  }

  /**
   * value()
   *
   * Get the value for this JSON object. NOTE: Only strings will have keys.
   *
   * @param def The default value to return if the value is not set.
   * @return The value, otherwise the default value.
   **/
  public String value(String def){
    return val != null ? val : (key != null ? key : def);
  }

  /**
   * length()
   *
   * The number of children elements this JSON object has.
   *
   * @return The number of child elements, otherwise zero.
   **/
  public int length(){
    if(childs != null){
      return childs.size();
    }else{
      return 0;
    }
  }

  /**
   * get()
   *
   * Get a child element of this JSON object. NOTE: Only objects and arrays can
   * have child elements.
   *
   * @param x The index of the element to retrieve.
   * @return The JSON object at the given location, otherwise this object.
   **/
  public JSON get(int x){
    if(childs != null && x >= 0 && x < childs.size()){
      return childs.get(x);
    }else{
      return this;
    }
  }

  /**
   * get()
   *
   * Get a child element of this JSON object by key. NOTE: Only objects and
   * arrays can have child elements.
   *
   * @param key The key to be used to search for the element.
   * @return The JSON object at the given location, otherwise this object.
   **/
  public JSON get(String key){
    if(childs != null && key != null){
      for(int x = 0; x < childs.size(); x++){
        JSON c = childs.get(x);
        if(c != null && c.key(null) != null && c.key(null).equals(key)){
          return c;
        }
      }
    }
    return this;
  }

  /**
   * exists()
   *
   * Check whether a child element exists.
   *
   * @param key The key to be used to search for the element.
   * @return True if the child exists, otherwise false.
   **/
  public boolean exists(String key){
    if(childs != null && key != null){
      for(int x = 0; x < childs.size(); x++){
        JSON c = childs.get(x);
        if(c.key(null) != null && c.key(null).equals(key)){
          return true;
        }
      }
    }
    return false;
  }

  /**
   * toString()
   *
   * Convert this object and all child objects to a printable String.
   *
   * @return A printable String representing this object and it's child
   * elements.
   **/
  @Override
  public String toString(){
    switch(type){
      case TYPE_OBJ :
        String o = key != null ? "\"" + key + "\":{" : "{";
        if(childs != null){
          for(int x = 0; x < childs.size(); x++){
            JSON c = childs.get(x);
            o += c.toString();
            o += x < childs.size() - 1 ? "," : "";
          }
        }
        return o + '}';
      case TYPE_ARR :
        String a = key != null ? "\"" + key + "\":[" : "[";
        if(childs != null){
          for(int x = 0; x < childs.size(); x++){
            JSON c = childs.get(x);
            a += c.toString();
            a += x < childs.size() - 1 ? "," : "";
          }
        }
        return a + ']';
      case TYPE_STR :
        if(key != null && val != null){
          return '\"' + key + "\":\"" + val + '\"';
        }else if(key != null){
          return '\"' + key + '\"';
        }else if(val != null){
          return '\"' + val + '\"';
        }else{
          return "";
        }
      default :
        return "";
    }
  }

  /**
   * assurt()
   *
   * A very simple assertion method for testing that the JSON parser isn't
   * regressing.
   *
   * @param r The result to test.
   * @return Pass through for the result value.
   **/
  private static boolean assurt(boolean r){
    System.out.println(
      "[" + (r ? "PASS" : "FAIL") + "] " +
      Thread.currentThread().getStackTrace()[2].getClassName() + "->" +
      Thread.currentThread().getStackTrace()[2].getMethodName() + "::" +
      Thread.currentThread().getStackTrace()[2].getLineNumber()
    );
    return r;
  }

  /**
   * test()
   *
   * Test that the parser works as expected. The result of each test and a
   * summary is printed to the standard out, as well as a boolean indicating
   * test success.
   *
   * @return The result of performing the tests, true if success, otherwise
   * false.
   **/
  public static boolean test(){
    /* Setup variables */
    String[] test = new String[]{
      "{}",
      "{\"test\"}",
      "{\"test\":\"123\"}",
      "{\"test\":{}}",
      "{\"test\":[]}",
      "{\"test\":{\"arg-a\",\"arg-b\"}}",
      "{\"test\":[\"arg-a\",\"arg-b\"]}",
      "{\"test\":{\"arg-a\":\"123\",\"arg-b\":\"456\"}}",
      "{\"test\":[\"arg-a\":\"123\",\"arg-b\":\"456\"]}",
      "{\"test\":{{\"arg-a\":\"123\"},{\"arg-b\":\"456\"}}}",
      "{\"test\":[{\"arg-a\":\"123\"},{\"arg-b\":\"456\"}]}",
      "{\"1\":[\"a\"],\"2\":[\"b\"]}"
    };
    boolean r = true;
    /* Run parser tests */
    for(int x = 0; x < test.length; x += 2){
      try{
        boolean a = assurt(new JSON(test[x]).toString().equals(test[x]));
        if(!a){
          System.out.println("  in:  '" + test[x] + "'");
          System.out.println("  got: '" + new JSON(test[x]).toString() + "' !=");
          System.out.println("  exp: '" + test[x] + "'");
        }
        r &= a;
      }catch(Exception e){
        System.out.println(">> Major Screw Up <<");
        e.printStackTrace();
        r = false;
      }
    }
    /* Run getter tests */
    try{
      r &= assurt(new JSON("{\"test\":[{\"arg-a\":\"123\"},{\"arg-b\":\"456\"}]}")
        .get(0).get(0).toString().equals("{\"arg-a\":\"123\"}"));
      r &= assurt(new JSON("{\"test\":[\"arg-a\":\"123\",\"arg-b\":\"456\"]}")
        .get(0).get("arg-b").toString().equals("\"arg-b\":\"456\""));
    }catch(Exception e){
      System.out.println(">> Major Screw Up <<");
      e.printStackTrace();
      r = false;
    }
    /* Print result */
    System.out.println("");
    System.out.println("  Tests " + (r ? "PASSED" : "FAILED"));
    return r;
  }
}
