/**
 ** Copyright 2016 General Electric Company
 **
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 ** 
 **     http://www.apache.org/licenses/LICENSE-2.0
 ** 
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */


package com.ge.research.semtk.sparqlX;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.utility.LocalLogger;

public class SparqlToXUtils {
	static final Pattern PATTERN_BAD_FIRST_CHAR = Pattern.compile("#[^a-zA-Z0-9]");
	public static final String BLANK_NODE_PREFIX = "nodeID://";
	
	// check that the passed Sparql query references the appropriate manadate columns in the return.
	public static void validateSparqlQuery(String query, String[] requiredCols) throws IOException {
		
		for (String i : requiredCols){
			if (!query.contains('?'+i)){
				// throw an exception
				throw new IOException("Incoming query missing required field " + i);
			}
		}
	}
	
	// check that the passed SPARQL query does not contain blocked words (e.g. to disallow freeform DELETE queries)
	public static void validateSparqlInsertQuery(String query, String[] blockedWords) throws IOException{
		for (String i : blockedWords){
			if (query.contains(i)){
				// throw an exception
				throw new IOException("Incoming query contains blocked word " + i);
			}
		}
	}
	
  /**
   * Check to see if a URI is of a given type.  True if matches, false if does not
   * @throws Exception 
   */
  public static boolean checkURIType(String uri, String type, String sparqlServerUrl, String sparqlDataset, String serverTypeString, String user, String pass) throws Exception{
    String checkTypeQuery = "select * where { " +  
        "VALUES ?x {<" + uri + ">} . " +
        "?x rdf:type <" + type + ">" +
        "}";
    // execute query
    SparqlEndpointInterface endpoint = SparqlEndpointInterface.executeQuery(sparqlServerUrl, sparqlDataset, serverTypeString, checkTypeQuery, user, pass, SparqlResultTypes.TABLE);
    String[] results = endpoint.getStringResultsColumn("x");
    
    if(results != null && results.length > 0){
      return true; 
    }
    return false;
  } 	
	
	/**
	 * Generate a DELETE query for a specific URI.
	 * To prevent a catastrophic delete, disallows a URI starting with ?
	 * @throws IOException
	 */
  public static String generateDeleteURIQuery(String uri) throws IOException{

    // check that URI does not start with ?   
    if(uri.startsWith("?")){
      LocalLogger.logToStdOut("URI to delete may not start with '?'...not deleting");
      throw new IOException("URI to delete may not start with '?'");
    }
    
    // delete anywhere where the URI is a subject or object
    String query = generateDeleteURISubjectQuery(uri) + generateDeleteURIObjectQuery(uri);
    return query;

  }	
  
  /**
   * Generate a DELETE query where the subject is a specific URI.
   * @throws IOException
   */
  public static String generateDeleteURISubjectQuery(String uri) throws IOException{
    return generateDeleteURISubjectQuery(uri, null);
  }
  
  /**
   * Generate a DELETE query where the subject is a specific URI, and the predicate is given.
   * Expects that predicate will NOT be enclosed by < >
   * @throws IOException
   */
  public static String generateDeleteURISubjectQuery(String uri, String predicate) throws IOException {
   
    // avoid catastrophic delete by checking that URI does not start with ?  
    if(uri.startsWith("?")){
      LocalLogger.logToStdOut("URI to delete may not start with '?'...not deleting");
      throw new IOException("URI to delete may not start with '?'");
    }else if(uri.trim().equals("")){
      LocalLogger.logToStdOut("URI to delete may not be empty...not deleting");
      throw new IOException("URI to delete may not be empty");
    }
    
    // predicate must be null or a valid string - if it's empty then something is wrong
    if(predicate != null && predicate.trim().equals("")){
      LocalLogger.logToStdOut("Predicate to delete may not be empty...not deleting");
      throw new IOException("Predicate to delete may not be empty");
    }
    
    if(predicate == null){
      predicate = "?y1";  // no predicate specified - do not restrict it
    }else{
      predicate = "<" + predicate + ">";  // predicate specified - use it
    }
    
    String query = "DELETE where { <" +  uri + "> " + predicate + " ?z1 . } ";  // uri is subject
    return query;
  }   
  
  /**
   * Generate a DELETE query where the object is a specific URI.
   * @throws IOException
   */
  public static String generateDeleteURIObjectQuery(String uri) throws IOException{

    // avoid catastrophic delete by checking that URI does not start with ?   
    if(uri.startsWith("?")){
      LocalLogger.logToStdOut("URI to delete may not start with '?'...not deleting");
      throw new IOException("URI to delete may not start with '?'");
    }else if(uri.trim().equals("")){
      LocalLogger.logToStdOut("URI to delete may not be empty...not deleting");
      throw new IOException("URI to delete may not be empty");
    }
    
    String query = "DELETE where { ?x2 ?y2 <" + uri + "> . } ";  // uri is object
    return query;

  }  
  
  public static String generateDeletePrefixQuery(String prefix) {
	  // delete all triples containing any trace of the given prefix
	  String sparql = String.format(
				"delete {" +
				"?x ?y ?z." +
				"}" +
				"where {" +
				" ?x ?y ?z  FILTER ( strstarts(str(?x), \"%s\") || strstarts(str(?y), \"%s\") || strstarts(str(?z), \"%s\") )." +
				"}", 
				prefix, prefix, prefix);
	  
	  return sparql;
  }
  
  public static String generateDeleteBySubjectPrefixQuery(String prefix) {
	  // delete all triples containing any trace of the given prefix
	  String sparql = String.format(
				"delete {" +
				"?x ?y ?z." +
				"}" +
				"where {" +
				" ?x ?y ?z FILTER strstarts(str(?x), \"%s\")." +
				"}", 
				prefix);
	  
	  return sparql;
  }
  
  public static String generateDeleteBySubjectRegexQuery(String regex) {
	  // delete all triples containing any trace of the given prefix
	  String sparql = String.format(
				"delete {" +
				"?x ?y ?z." +
				"}" +
				"where {" +
				" ?x ?y ?z FILTER regex(str(?x), \"%s\")." +
				"}", 
				regex);
	  
	  return sparql;
  }
  
  /**
   * Delete all model triples given a list of prefixes.   Also deletes blank nodes.
   * @param prefixes
   * @return
   */
  public static String generateDeleteModelTriplesQuery(ArrayList<String> prefixes, Boolean deleteBlankNodes) {
	  // init regex
	  StringBuilder regex = new StringBuilder("^(");
	  
	  // add all prefixes
	  for (String p : prefixes) {
		  if (regex.toString().length() > 2) {
			  regex.append("|");
		  }
		  regex.append(p);
	  }
	  
	  // add blank node prefix
	  if (deleteBlankNodes) {
		  regex.append("|" + SparqlToXUtils.BLANK_NODE_PREFIX);
	  }
	  regex.append(")");
	  
	  // delete all triples w
	  return SparqlToXUtils.generateDeleteBySubjectRegexQuery(regex.toString());
	  
  }
  
  
  public static String genereateClearAllQuery() {
	  return "clear all";
  }
	
	// check that all required columns are found in the return values from the Sparql endpoint. 
	public static void validateSparqlResults(SparqlEndpointInterface endpoint, String[] requiredCols) throws IOException {
		for (String col : requiredCols){
			try {
				endpoint.checkResultsCol(col);
			} catch (Exception e) {
				throw new IOException("Semantic query did not return column: " + col);
			}
		}
	}
	// check to see if a required parameter is null/missing
	public static void genExceptionIfNull(Object o, String msg, String where) {
		if (o == null) {
		    if (where != null) 
		    	msg = msg + " at " + where;
			throw new IllegalArgumentException(msg);
		}
	}
	

	
	public static String safeSparqlString(String s) {
		
		StringBuilder out = new StringBuilder();
	    for (int i = 0; i < s.length(); i++) {
	        char c = s.charAt(i);
	        
	        // lowercase
	        if      (c == '\"')             { out.append("\\\""); }   // replaces " with \"  
	        else if (c == '\'')             { out.append("\\'"); }    // replaces ' with \'
	        else if (c == '\n')             { out.append("\\n"); } 
	        else if (c == '\r')             { out.append("\\r"); } 
	        else if (c == '\\') {
	        	if (i+1 < s.length()) {                              // backslash requires look-ahead
	        		char c2 = s.charAt(i+1);
	        		if (c2 == 'n' || c2 == 't') {                    // preserve backslash if followed by these
	        			out.append(c);
	        		}
	        		else {
	        			out.append("\\\\");                          // unknown next char: change \ to \\
	        		}
	        	} else {
	        		out.append("\\\\");                              // last char in string is \: change to \\
	        	}
	        }  

	        // rest
	        else                          { out.append(c); }
	    }
	    return out.toString();
		
	}
	
	public static boolean isLegalURI(String uri) {
		
		// special case: virtuoso silently chokes on these
		Matcher m = SparqlToXUtils.PATTERN_BAD_FIRST_CHAR.matcher(uri);
		if (m.find()) {
			return false;
		}
		
		try	{
			new URI(uri);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
}
