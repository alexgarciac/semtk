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

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class SparqlConnection {
/**
 * SparqlConnection is a "SparqlGraph" connection consisting of TWO Sparql Endpoints
 * 1) Ontology
 * 2) Data
 * 
 * To match files written by historical javascript, this is mostly a JSON wrangler.
 * 
 */
	private final static String QUERY_SERVER = "kdl";
	private final static String FUSEKI_SERVER = "fuseki";
	private final static String VIRTUOSO_SERVER = "virtuoso";
	
	private String name = null;
	private String domain = null;
	private ArrayList<SparqlEndpointInterface> modelInterfaces = null;
	private ArrayList<SparqlEndpointInterface> dataInterfaces = null;
	
	public SparqlConnection () {
		this.name = "";
		this.domain = "";
		this.modelInterfaces = new ArrayList<SparqlEndpointInterface>();
		this.dataInterfaces = new ArrayList<SparqlEndpointInterface>();
	}
	
	public SparqlConnection(String jsonText) throws Exception {
		this();
	    this.fromString(jsonText);
	}
	
	public SparqlConnection(String name, String serverType, String dataServicetURL, String knowledgeServiceURL, String dataset, String domain) throws Exception{
		this();
		this.name = name;
		this.domain = domain;
		this.addDataInterface(serverType, 
				dataServicetURL,
				dataset);
		this.addModelInterface(serverType, 
				dataServicetURL,
				dataset);
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject toJson() {
		JSONObject jObj = new JSONObject();
		jObj.put("name", name);
		jObj.put("domain", domain);
		
		JSONArray model = new JSONArray();
		JSONArray data = new JSONArray();

		for (int i=0; i < this.modelInterfaces.size(); i++) {
			SparqlEndpointInterface mi = this.modelInterfaces.get(i);
			JSONObject inner = new JSONObject();
			inner.put("type", mi.getServerType());
			inner.put("url", mi.getServerAndPort());
			inner.put("dataset", mi.getDataset());
			model.add(inner);
		}
		
		for (int i=0; i < this.dataInterfaces.size(); i++) {
			SparqlEndpointInterface di = this.dataInterfaces.get(i);
			JSONObject inner = new JSONObject();
			inner.put("type", di.getServerType());
			inner.put("url", di.getServerAndPort());
			inner.put("dataset", di.getDataset());
			data.add(inner);
		}
		
		jObj.put("model", model);
		jObj.put("data", data);
		
		return jObj;
	}
	
	public void fromJson(JSONObject jObj) throws Exception {
		
		if(jObj.entrySet().size() == 1 && jObj.containsKey("sparqlConn")){
			throw new Exception("Cannot create SparqlConnection object because the JSON is wrapped in \"sparqlConn\"");
		}
		
		this.name = (String) jObj.get("name");
		this.domain = (String) jObj.get("domain");
		
		this.modelInterfaces = new ArrayList<SparqlEndpointInterface>();
		
		this.dataInterfaces = new ArrayList<SparqlEndpointInterface>();
		
		if (jObj.containsKey("dsURL")) {
			
			// backwards compatible read
		
			// If any field doesn't exist, presume it exists in the other connection
			this.addModelInterface(	(String)(jObj.get("type")), 
								    (String)(jObj.containsKey("onURL") ? jObj.get("onURL") : jObj.get("dsURL")),
								    (String)(jObj.containsKey("onDataset") ? jObj.get("onDataset") : jObj.get("dsDataset"))
							        );
			this.addDataInterface(	(String)(jObj.get("type")), 
									(String)(jObj.containsKey("dsURL") ? jObj.get("dsURL") : jObj.get("onURL")),
									(String)(jObj.containsKey("dsDataset") ? jObj.get("dsDataset")  : jObj.get("onDataset"))
									);
		} else {
			// normal read
			
			// read model interfaces
	    	for (int i=0; i < ((JSONArray)(jObj.get("model"))).size(); i++) {
	    		JSONObject m = (JSONObject)((JSONArray)jObj.get("model")).get(i);
	    		this.addModelInterface((String)(m.get("type")), (String)(m.get("url")), (String)(m.get("dataset")));
	    	}
	    	// read data interfaces
	    	for (int i=0; i < ((JSONArray)(jObj.get("data"))).size(); i++) {
	    		JSONObject d = (JSONObject)((JSONArray)jObj.get("data")).get(i);
	    		this.addDataInterface((String)(d.get("type")), (String)(d.get("url")), (String)(d.get("dataset")));
	    	}
		}
		
		// no deprecated field-handling
	}

	public static SparqlConnection deepCopy(SparqlConnection other) throws Exception {
		return new SparqlConnection(other.toJson().toString());
	}
	
	public void fromString(String jsonText) throws Exception {
		JSONParser parser = new JSONParser();
		JSONObject jObj = (JSONObject) parser.parse(jsonText);
		fromJson(jObj);

	}
	
	public String toString() {
		return toJson().toString();
	}
	
	public boolean equals(SparqlConnection other, boolean ignoreName) {
		String thisStr = this.toString();
		String otherStr = other.toString();
		if (ignoreName) {
			thisStr = thisStr.replaceFirst(this.name, "NAME");
			otherStr = otherStr.replaceFirst(other.name, "NAME");
		}
		return (thisStr.equals(otherStr));
	}
	
	public void setName (String name) {
		this.name = name;
	}
	
	public void setDomain (String domain) {
		this.domain = domain;
	}
	
	public void addModelInterface(String sType, String url, String dataset) throws Exception {
		this.modelInterfaces.add(this.createInterface(sType, url, dataset));
	}
	
	public void addDataInterface(String sType, String url, String dataset) throws Exception {
		this.dataInterfaces.add(this.createInterface(sType, url, dataset));
	}
	public void addModelInterface(String sType, String url, String dataset, String user, String passwd) throws Exception {
		this.modelInterfaces.add(this.createInterface(sType, url, dataset, user, passwd));
	}
	
	public void addDataInterface(String sType, String url, String dataset, String user, String passwd) throws Exception {
		this.dataInterfaces.add(this.createInterface(sType, url, dataset, user, passwd));
	}
	public int getModelInterfaceCount() {
		return this.modelInterfaces.size();
	}
	
	public SparqlEndpointInterface getModelInterface(int i) {
		return this.modelInterfaces.get(i);
	}
	
	public ArrayList<SparqlEndpointInterface> getModelInterfaces() {
		return this.modelInterfaces;
	}
	
	public String getDomain() {
		return this.domain;
	}
	
	public String getName() {
		return this.name;
	}
	
	public int getDataInterfaceCount() {
		return this.dataInterfaces.size();
	}
	
	public SparqlEndpointInterface getDataInterface(int i) {
			return this.dataInterfaces.get(i);
	}
	
	public ArrayList<SparqlEndpointInterface> getDataInterfaces() {
		return this.dataInterfaces;
	}
	
	public SparqlEndpointInterface getDefaultQueryInterface() throws Exception {
		if (this.dataInterfaces.size() > 0) {
			return this.dataInterfaces.get(0);
		} else if (this.modelInterfaces.size() > 0) {
			return this.modelInterfaces.get(0);
		} else {
			throw new Exception("This SparqlConnection has no endpoints.");
		}
	}
	
	public SparqlEndpointInterface getInsertInterface() throws Exception {
		if (this.dataInterfaces.size() == 1) {
			return this.dataInterfaces.get(0);
		} else {
			throw new Exception("Expecting one data endpoint for INSERT.  Found " + this.dataInterfaces.size());
		}
	}
	
	// Is number of endpoint serverURLs == 1
	public boolean isSingleServerURL() {
		String url = "";
		for (int i=0; i < this.modelInterfaces.size(); i++) {
			String e =  this.modelInterfaces.get(i).getServerAndPort();
			if (url.equals("")) {
				url = e;
			} else if (! e.equals(url)) {
				return false;
			}
		}
		
		// add data interfaces
		for (int i=0; i < this.dataInterfaces.size(); i++) {
			String e =  this.dataInterfaces.get(i).getServerAndPort();
			if (url.equals("")) {
				url = e;
			} else if (! e.equals(url)) {
				return false;
			}
		}
		
		// if there are no serverURLs then false
		if (url.equals("")) {
			return false;
		} else {
			return true;
		}
	}
	
	// Is number of endpoint serverURLs == 1
	public boolean isSingleDataServerURL() {
		String url = "";
		
		// check data interfaces
		for (int i=0; i < this.dataInterfaces.size(); i++) {
			String e =  this.dataInterfaces.get(i).getServerAndPort();
			if (url.equals("")) {
				url = e;
			} else if (! e.equals(url)) {
				return false;
			}
		}
		
		// if there are no serverURLs then false
		if (url.equals("")) {
			return false;
		} else {
			return true;
		}
	}
	
	// get list of datasets for a given serverURL
	public ArrayList<String> getAllDatasetsForServer(String serverURL) {
		ArrayList<String> ret = new ArrayList<String>();
		
		ret.addAll(this.getDataDatasetsForServer(serverURL));
		
		// add any models that aren't duplicates of data
		ArrayList<String> modelDatasets = this.getModelDatasetsForServer(serverURL);
		for (String ds : modelDatasets) {
			if (! ret.contains(ds)) {
				ret.add(ds);
			}
		}
		
		return ret;
	}
	
	// get list of DATA datasets for a given serverURL
	public ArrayList<String> getDataDatasetsForServer(String serverURL) {
		ArrayList<String> ret = new ArrayList<String>();
		
		for (int i=0; i < this.dataInterfaces.size(); i++) {
			SparqlEndpointInterface e =  this.dataInterfaces.get(i);
			if (e.getServerAndPort().equals(serverURL)  &&  ret.indexOf(e.getDataset()) == -1) {
				ret.add(e.getDataset());
			}
		}
		
		return ret;
	}
	
	// get list of MODEL datasets for a given serverURL
	public ArrayList<String> getModelDatasetsForServer(String serverURL) {
		ArrayList<String> ret = new ArrayList<String>();
		
		for (int i=0; i < this.modelInterfaces.size(); i++) {
			SparqlEndpointInterface e =  this.modelInterfaces.get(i);
			if (e.getServerAndPort().equals(serverURL) &&  ret.indexOf(e.getDataset()) == -1) {
				ret.add(e.getDataset());
			}
		}
		
		return ret;
	}

	//---------- private function
	private SparqlEndpointInterface createInterface(String stype, String url, String dataset) throws Exception{
		if (stype.equals(SparqlConnection.FUSEKI_SERVER)) {
			return new FusekiSparqlEndpointInterface(url, dataset);
		} else if (stype.equals(SparqlConnection.VIRTUOSO_SERVER)) {
			return new VirtuosoSparqlEndpointInterface(url, dataset);
		} else {
			throw new Error("Unsupported SparqlConnection server type: " + stype);
		}
	}
	
	private SparqlEndpointInterface createInterface(String stype, String url, String dataset, String user, String passwd) throws Exception{
		if (stype.equals(SparqlConnection.FUSEKI_SERVER)) {
			return new FusekiSparqlEndpointInterface(url, dataset, user, passwd);
		} else if (stype.equals(SparqlConnection.VIRTUOSO_SERVER)) {
			return new VirtuosoSparqlEndpointInterface(url, dataset, user, passwd);
		} else {
			throw new Error("Unsupported SparqlConnection server type: " + stype);
		}
	}
}
