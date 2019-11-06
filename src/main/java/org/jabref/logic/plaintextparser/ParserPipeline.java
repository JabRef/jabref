package org.jabref.logic.plaintextparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;


import com.microsoft.applicationinsights.core.dependencies.apachecommons.io.IOUtils;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

/**
 * This class is used to help making new entries faster by parsing a string.
 * An external parser called anystyle is being used for that.
 */
public class ParserPipeline {

  private static final String ANYSTYLE_SCRIPT_PATH = "src/main/resources/anystyle-reference-parser/lib/";
  private static final String GEM_FILES_PATH = "src/main/resources/gems/";
  //private static File anystyleScriptFile = Paths.get(ANYSTYLE_SCRIPT_PATH).toFile();
  //private static ScriptEngine rubyScriptEngine = new ScriptEngineManager().getEngineByName("jruby");
  //private static ScriptingContainer ruby = new ScriptingContainer(LocalVariableBehavior.PERSISTENT);


  //private BibEntry bibEntry;
  //private BibDatabase bibDatabase;
  private static String referenceText;
  private static String parserResults;
  private static StandardField[] fields; // contains the fields which the parser filtered.

  /**
   * Takes a whole String and filters the specific fields for the entry which is done
   * by an external parser.
   * @param plainText Reference text to be parsed.
   */
  public static void parseRefText(String plainText) {
    //Object parserResult = ruby.runScriptlet(PathType.ABSOLUTE, ANYSTYLE_SCRIPT_PATH);
    /*
    TODO: IMPLEMENT PARSER METHOD // IMPORT ANYSTYLE PARSER
     */
  }

  /**
   * Creates a new entry and fills the fields with the results of the parser, but only
   * there where the parser has found something. If the entry already exists then
   * nothing the function will immediately return.
   * @param entry New entry.
   */
  public static void createNewEntry(String bibtexString) {
    /*
    try {
      System.out.println("ABSDREIFJOERFJO");
      System.out.println(new BufferedReader(new FileReader(ANYSTYLE_SCRIPT_PATH+"anystyle.rb")).readLine());
      ArrayList<String> arrayList = new ArrayList<>();
      arrayList.add(ANYSTYLE_SCRIPT_PATH);

      //String everything;
    //try(FileInputStream inputStream = new FileInputStream(ANYSTYLE_SCRIPT_PATH+"anystyle.rb")) {
      //everything = IOUtils.toString(inputStream);
      //System.out.println(everything);
    //}

      ScriptContext context = rubyScriptEngine.getContext();
      try {
        rubyScriptEngine.eval("$LOAD_PATH << 'file:" + GEM_FILES_PATH + "anystyle-1.3.5/lib'", context);
        rubyScriptEngine.eval("$LOAD_PATH << 'file:" + GEM_FILES_PATH + "anystyle-data-1.2.0/lib'", context);
        rubyScriptEngine.eval("$LOAD_PATH << 'file:" + GEM_FILES_PATH + "bibtex-ruby-5.0.0/lib'", context);
        rubyScriptEngine.eval("$LOAD_PATH << 'file:" + GEM_FILES_PATH + "builder-3.2.3/lib'", context);
        rubyScriptEngine.eval("$LOAD_PATH << 'file:" + GEM_FILES_PATH + "latex-decode-0.3.1/lib'", context);
        rubyScriptEngine.eval("$LOAD_PATH << 'file:" + GEM_FILES_PATH + "namae-1.0.1/lib'", context);
        rubyScriptEngine.eval("$LOAD_PATH << 'file:" + GEM_FILES_PATH + "wapiti-1.0.5/lib'", context);
        rubyScriptEngine.eval("$LOAD_PATH << 'file:" + GEM_FILES_PATH + "wapiti-1.0.5/lib/wapiti'", context);


        context.setAttribute("bibtexString", bibtexString, ScriptContext.ENGINE_SCOPE);

        String parserOutput = (String) rubyScriptEngine.eval("'weiofweuf'", context);
        System.out.println(parserOutput);

        parserOutput = (String) rubyScriptEngine.eval("bibtexString", context);
        System.out.println(parserOutput);

        rubyScriptEngine.eval("require 'anystyle'", context);
        parserOutput = (String) rubyScriptEngine.eval("Anystyle.parse bibtexString", context);
        System.out.println(parserOutput);
      } catch (ScriptException e) {
        System.out.println(e.getMessage());
      }

      Ruby runtime = JavaEmbedUtils.initialize(arrayList);
      RubyRuntimeAdapter evaler = JavaEmbedUtils.newRuntimeAdapter();
      //evaler.eval(runtime, "gem install anystyle");
      //evaler.eval(runtime, everything);

      //rubyScriptEngine.eval(new BufferedReader(new FileReader(ANYSTYLE_SCRIPT_PATH)));
      //rubyScriptEngine.put("bibtexString", bibtexString);
      //String rubyret = (String) rubyScriptEngine.eval("put 'ABCD'");
      //System.out.println(rubyret);
    } catch (IOException e) {
      //TODO
    //} catch (ScriptException e) {
      //TODO
    }
    */
    /*
    if(bibDatabase.containsEntryWithId(entry.getId())) {
    return;
    }
    else{
    for (StandardField field : fields) {
      entry.setField(field, parserResults);
    }
    bibDatabase.insertEntry(entry);
    }
     */
    //TODO: Finish it!

  }





}
