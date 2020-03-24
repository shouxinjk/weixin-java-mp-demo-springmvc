/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ilife.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

//import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.BaseDocument;

public class ArangoDbClient implements Serializable {
	private static final long serialVersionUID = 6865168830661725082L;
	private static final Logger LOG = LoggerFactory.getLogger(ArangoDbClient.class);
	private ArangoDB arango;
	private ArangoDatabase arangoClient;
	
	private String HOST = "localhost";
	private int PORT = 8529;
	private String USERNAME = "username";
	private String PASSWORD = "password";

    /**
     * The ArangoDbClient constructor.
     * @param conf The Arango server config
     * @param database The Arango database to read/write data
     */
    public ArangoDbClient(String host,String port,String username,String password, String database) {
        this.HOST = host;
        this.PORT= Integer.parseInt(port);
        this.USERNAME = username;
        this.PASSWORD = password;
		this.arango = new ArangoDB.Builder().host(HOST, PORT).user(USERNAME).password(PASSWORD).build();
		this.arangoClient = this.arango.db(database);
    }

    /**
     * Inserts one or more documents.
     * @param documents documents
     */
    public void insert(String collection, List<BaseDocument> documents) {
    		for(BaseDocument doc:documents) {
    			this.arangoClient.collection(collection).insertDocument(doc);
    		}
    }
    
    /**
     * Insert a single document.
     * @param document document
     */
    public void insert(String collection, BaseDocument document) {
    		this.arangoClient.collection(collection).insertDocument(document);
    }

    /**
     * Update a single document.
     */
    public void update(String collection, String key, BaseDocument document) {
        this.arangoClient.collection(collection).updateDocument(key, document);
    }

    /**
     * Find a single document in the collection according to the specified key.
     *
     * @param key document key
     */
    public BaseDocument find(String collection, String key) {
    		return this.arangoClient.collection(collection).getDocument(key,BaseDocument.class);
    }

    public void delete(String collection, String key) {
	    	// delete a document
		try {
			this.arangoClient.collection(collection).deleteDocument(key);
		} catch (final ArangoDBException e) {
			LOG.error("Failed to delete document. " + e.getMessage());
		}
    }
    
    /**
     * get documents by AQL query
     * 注意：当前需要返回类型。即在AQL后增加 RETURN ...表达式
     * @param <T>
     * @param <T>
     * @param query "FOR t IN firstCollection FILTER t.name == @name RETURN t"
     * @param bindVars new MapBuilder().put("name", "Homer").get()
     * @return documents
     */
    public <T> List<T> query(String query , Map<String,Object> bindVars, Class<T> type){
    		List<T> result = new ArrayList<T>();
		// execute AQL queries
		try {
//			final String query = "FOR t IN firstCollection FILTER t.name == @name RETURN t";
//			final Map<String, Object> bindVars = new MapBuilder().put("name", "Homer").get();
			final ArangoCursor<T> cursor = arangoClient.query(query, bindVars, null,
				type);
			for (; cursor.hasNext();) {
				result.add(cursor.next());
			}
		} catch (final ArangoDBException e) {
			LOG.error("Failed to execute query. " + e.getMessage());
		}
		return result;
    }
    
    /**
     * Closes all resources associated with this instance.
     */
    public void close() {
    		this.arangoClient = null;
        this.arango.shutdown();
    }

}
