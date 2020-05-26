package b.gp;

import java.util.ArrayList;

/**
 * JSON.java
 *
 * A single file JSON parser.
 **/
public class JSON{
  private static final int TYPE_OBJ = 1;
  private static final int TYPE_ARR = 2;
  private static final int TYPE_STR = 3;

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
   **/
  public JSON(String json){
    /* Setup internal variables */
    type = 0;
    key = null;
    val = null;
    childs = null;
    /* Find the type of this object */
    int x = 0;
    for(; type <= 0 && x < json.length(); x++){
      char c = json.charAt(x);
      /* Find an indicator character */
      switch(c){
        case '{' :
          type = TYPE_OBJ;
          break;
        case '[' :
          type = TYPE_ARR;
          break;
        case '"' :
          type = TYPE_STR;
          break;
      }
    }
    /* Perform parsing */
    boolean keyFill = true;
    boolean escape = false;
    boolean string = false;
    for(; x < json.length(); x++){
      char c = json.charAt(x);
      /* Check for previous escape */
      if(escape){
        escape = false;
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
            /* NOTE: Special case for double slash. */
            escape = true;
            break;
          default :
            /* TODO: Invalid escape sequence. */
            break;
        }
      }
      /* Parse object indicators only if not processing a string */
      if(!string){
        switch(c){
          case '{' :
            /* TODO: Child object found, add and skip the size of it. */
            break;
          case '}' :
            /* TODO: End of our object found (if we're an object). */
            break;
          case '[' :
            /* TODO: Child array found, add and skip the size of it. */
            break;
          case ']' :
            /* TODO: End of our array (if we're an array). */
            break;
        }
      /* Parse string internals */
      }else{
        switch(c){
          case '"' :
            /* Invert in-string status */
            string = !string;
            /* Switch filling case if end of string found */
            if(!string){
              keyFill = !keyFill;
            }
            break;
          case '\\' :
            /* NOTE: If not already in an escape special case. */
            if(!escape){
              escape = true;
              break;
            }else{
              /* Turn off escape and fall through to plain text */
              escape = false;
            }
          default :
            /* Handle in-string */
            if(string){
              /* Check if filling key */
              if(keyFill){
                key += c;
              /* Check if filling value */
              }else{
                val += c;
              }
            }
            break;
        }
      }
    }
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
   * getKey()
   *
   * Get the key for this JSON object. NOTE: Array elements may not have a key.
   *
   * @return The key, otherwise NULL.
   **/
  public String getKey(){
    return key;
  }

  /**
   * getValue()
   *
   * Get the value for this JSON object. NOTE: Only strings will have keys.
   *
   * @return The value, otherwise NULL.
   **/
  public String getValue(){
    return val;
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
   * @return The JSON object at the given location, otherwise NULL.
   **/
  public JSON get(int x){
    if(childs != null && x >= 0 && x < childs.size()){
      return childs.get(x);
    }else{
      return null;
    }
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
        String o = "{";
        if(childs != null){
          for(int x = 0; x < childs.size(); x++){
            o += childs.get(x).toString();
            if(x < childs.size() - 1){
              o += ',';
            }
          }
        }
        return o + '}';
      case TYPE_ARR :
        String a = "[";
        if(childs != null){
          for(int x = 0; x < childs.size(); x++){
            a += childs.get(x).toString();
            if(x < childs.size() - 1){
              a += ',';
            }
          }
        }
        return a + ']';
      case TYPE_STR :
        if(key != null && val != null){
          return '\"' + key + "\":\"" + val + '\"';
        }else if(val == null){
          return '\"' + key + '\"';
        }else if(key == null){
          return '\"' + val + '\"';
        }else{
          return "";
        }
      default :
        return "";
    }
  }
}
