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

package com.ge.research.semtk.test;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.load.DataLoader;
import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.resultSet.GeneralResultSet;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.sparqlX.VirtuosoSparqlEndpointInterface;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

/**
 * A utility class to load data to a semantic graph.  Intended for use in tests.
 * 
 * NOTE: This class cannot be put in under src/test/java because it must remain accessible to other projects.
 */
public class TestGraph {

	// PEC TODO: if we want the option of sharing or splitting model from data in different graphs then static functions will not do.
	//           It will have to be an object instantiated by a json nodegroup (shows whether they're split) or NULL (pick a default)
	public TestGraph() {
	}
	
	// PEC TODO:  specify model or data graph
	public static SparqlEndpointInterface getSei() throws Exception {
		SparqlEndpointInterface sei = new VirtuosoSparqlEndpointInterface(getSparqlServer(), generateDatasetName("both"), getUsername(), getPassword());
		try{
			sei.executeTestQuery();
		}catch(Exception e){
			LocalLogger.logToStdOut("***** Cannot connect to " + getSparqlServerType() + " server at " + getSparqlServer() + " with the given credentials for '" + getUsername() + "'.  Set up this server or change settings in TestGraph. *****");
			throw e;
		}
		return sei;
	}
	
	public static SparqlConnection getSparqlConn(String domain) throws Exception {
		SparqlConnection conn = new SparqlConnection();
		conn.setName("JUnitTest");
		conn.setDomain(domain);
		
		SparqlEndpointInterface sei = getSei();
		conn.addDataInterface( sei.getServerType(), sei.getServerAndPort(), sei.getDataset());
		conn.addModelInterface(sei.getServerType(), sei.getServerAndPort(), sei.getDataset());
		return conn;
	}
	
	/**
	 * Get the URL of the SPARQL server.
	 * @throws Exception 
	 */
	public static String getSparqlServer() throws Exception{
		return IntegrationTestUtility.getSparqlServer();
	}
	
	/**
	 * Get the test dataset name.
	 */
	public static String getDataset() throws Exception {
		return getSei().getDataset();
	}
	
	/**
	 * Get the SPARQL server type.
	 * @throws Exception 
	 */
	public static String getSparqlServerType() throws Exception{
		return IntegrationTestUtility.getSparqlServerType();
	}
	
	/**
	 * Get the SPARQL server username.
	 * @throws Exception 
	 */
	public static String getUsername() throws Exception{
		return IntegrationTestUtility.getSparqlServerUsername();
	}
	
	/**
	 * Get the SPARQL server password.
	 * @throws Exception 
	 */
	public static String getPassword() throws Exception{
		return IntegrationTestUtility.getSparqlServerPassword();
	}
	
	/**
	 * Clear the test graph
	 */
	// PEC TODO:  clear model and data graph
	public static void clearGraph() throws Exception {
		SparqlEndpointInterface sei = getSei();
		GeneralResultSet resultSet = sei.executeQueryAndBuildResultSet("clear all", SparqlResultTypes.CONFIRM);
		if (!resultSet.getSuccess()) {
			throw new Exception(resultSet.getRationaleAsString(" "));
		}
	}
	
	/**
	 * Drop the test graph (DROP lets the graph be CREATEd again, whereas CLEAR does not)
	 */
	public static void dropGraph() throws Exception {
		SparqlEndpointInterface sei = getSei();
		GeneralResultSet resultSet = sei.executeQueryAndBuildResultSet("drop graph <" + TestGraph.getDataset() + ">", SparqlResultTypes.CONFIRM);
		if (!resultSet.getSuccess()) {
			throw new Exception(resultSet.getRationaleAsString(" "));
		}
	}
	
	public static void execDeletionQuery(String query) throws Exception{
		SparqlEndpointInterface sei = getSei();
		GeneralResultSet resultSet = sei.executeQueryAndBuildResultSet(query, SparqlResultTypes.CONFIRM);
		if (!resultSet.getSuccess()) {
			throw new Exception(resultSet.getRationaleAsString(" "));
		}		
	}
	
	public static Table execTableSelect(String query) throws Exception {
		// execute a select query
		// exception if there's any problem
		// return the table
		SparqlEndpointInterface sei = getSei();
		TableResultSet res = (TableResultSet) sei.executeQueryAndBuildResultSet(query, SparqlResultTypes.TABLE);
		if (! res.getSuccess()) {
			throw new Exception("Sparql query failed: " + res.getRationaleAsString("\n"));
		}
		
		return res.getResults();
	}
	
	/**
	 * Get the number of triples in the test graph.
	 */
	public static int getNumTriples() throws Exception {
		Table table = TestGraph.execTableSelect(Utility.SPARQL_QUERY_TRIPLE_COUNT); 
		return (new Integer(table.getCell(0, 0))).intValue(); // this cell contains the count
	}
	
	/**
	 * Upload owl file to the test graph
	 * @param owlFilename  "src/test/resources/file.owl"
	 * @throws Exception
	 */
	// PEC TODO:  specify model or data graph
	public static void uploadOwl(String owlFilename) throws Exception {
		
		SparqlEndpointInterface sei = getSei();
		
		Path path = Paths.get(owlFilename);
		byte[] owl = Files.readAllBytes(path);
		SimpleResultSet resultSet = SimpleResultSet.fromJson(sei.executeAuthUploadOwl(owl));
		if (!resultSet.getSuccess()) {
			throw new Exception(resultSet.getRationaleAsString(" "));
		}
	}
	
	/**
	 * Upload an owl string to the test graph
	 * @param owl
	 * @throws Exception
	 */
	public static void uploadOwlString(String owl) throws Exception {
		
		SparqlEndpointInterface sei = getSei();
		
		SimpleResultSet resultSet = SimpleResultSet.fromJson(sei.executeAuthUploadOwl(owl.getBytes()));
		if (!resultSet.getSuccess()) {
			throw new Exception(resultSet.getRationaleAsString(" "));
		}
	}
	
	/**
	 * 
	 * @param jsonFilename e.g. "src/test/resources/nodegroup.json"
	 * @return
	 * @throws Exception
	 */
	public static NodeGroup getNodeGroup(String jsonFilename) throws Exception {
		
		return getSparqlGraphJsonFromFile(jsonFilename).getNodeGroup();
	}
	
	/**
	 * Get SparqlGraphJson modified with Test connection
	 * @param jsonFilename
	 */
	public static SparqlGraphJson getSparqlGraphJsonFromFile(String jsonFilename) throws Exception {		
		InputStreamReader reader = new InputStreamReader(new FileInputStream(jsonFilename));
		JSONObject jObj = (JSONObject) (new JSONParser()).parse(reader);		
		return getSparqlGraphJsonFromJson(jObj);
	}	
	
	/**
	 * Get SparqlGraphJson modified with Test connection
	 * @param jsonString
	 */
	public static SparqlGraphJson getSparqlGraphJsonFromString(String jsonString) throws Exception {		
		JSONObject jObj = (JSONObject) (new JSONParser()).parse(jsonString);		
		return getSparqlGraphJsonFromJson(jObj);
	}	
	
	/**
	 * Get SparqlGraphJson modified with Test connection
	 * @param jsonObject
	 */	
	@SuppressWarnings("unchecked")
	public static SparqlGraphJson getSparqlGraphJsonFromJson(JSONObject jObj) throws Exception{
		
		/*
		if (  ((JSONObject)jObj.get("sparqlConn")).containsKey("onDataset")) {
			((JSONObject)jObj.get("sparqlConn")).put("onDataset", generateDatasetName("both"));    // "model"
			((JSONObject)jObj.get("sparqlConn")).put("onURL", getSparqlServer()); 
			((JSONObject)jObj.get("sparqlConn")).put("onKsURL", getSparqlServer()); 
			
			((JSONObject)jObj.get("sparqlConn")).put("dsDataset", generateDatasetName("both"));   // "data"
		} else {
			((JSONObject)jObj.get("sparqlConn")).put("dsDataset", generateDatasetName("both"));   // "both"
		}
		
		// modify dataset and URL
		((JSONObject)jObj.get("sparqlConn")).put("dsURL", getSparqlServer()); 
		((JSONObject)jObj.get("sparqlConn")).put("dsKsURL", getSparqlServer()); 
		*/
		
		SparqlGraphJson s = new SparqlGraphJson(jObj);
		SparqlConnection conn = s.getSparqlConn();
		for (int i=0; i < conn.getDataInterfaceCount(); i++) {
			conn.getDataInterface(i).setDataset(generateDatasetName("both"));
			conn.getDataInterface(i).setServerAndPort(getSparqlServer());
		}
		for (int i=0; i < conn.getModelInterfaceCount(); i++) {
			conn.getModelInterface(i).setDataset(generateDatasetName("both"));
			conn.getModelInterface(i).setServerAndPort(getSparqlServer());
		}
		s.setSparqlConn(conn);
		
		return s;
	}
	
	/**
	 * Generate a dataset name (unique per user)
	 */
	public static String generateDatasetName(String sub) {
		return String.format("http://%s/junit/%s", System.getProperty("user.name"), sub);
	}	
	
	/**
	 * Clear graph, 
	 * In src/test/resources: loads baseName.owl, gets sgJson from baseName.json, and loads baseName.csv
	 * @param baseName
	 * @return SparqlGraphJson
	 * @throws Exception
	 */
	public static SparqlGraphJson initGraphWithData(String baseName) throws Exception {
		String jsonPath = String.format("src/test/resources/%s.json", baseName);
		String owlPath = String.format("src/test/resources/%s.owl", baseName);
		String csvPath = String.format("src/test/resources/%s.csv", baseName);

		// load the model
		TestGraph.clearGraph();
		TestGraph.uploadOwl(owlPath);
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile(jsonPath); 
		
		// load the data
		Dataset ds = new CSVDataset(csvPath, false);
		DataLoader dl = new DataLoader(sgJson, 2, ds, getUsername(), getPassword());
		dl.importData(true);
		
		return sgJson;
	}
}
