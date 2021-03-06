package org.cnag.rdconnect.beacon.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cnag.rdconnect.beacon.model.Chromosome;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

/**
 * CNAG implementation of a RD-Connect beacon service.
 * ElasticSearch DAO to query into rdconnect dataset
 * @author Joan Protasio (joan.protasio@cnag.crg.eu)
 * @version 1.0
 */

public class DAOElasticSearch {	
	
	private static Log log = LogFactory.getLog(DAOElasticSearch.class);
	static Properties prop = new Properties();
	static InputStream input = null;
	static String elasticSearchInstance;
	static String elasticSearchIndex;
	static String elasticSearchType;
	static int elasticSearchPort;
	
	
	public static void loadConf() {
		
		// loading properties file
		try {
    		input = DAOElasticSearch.class.getClassLoader().getResourceAsStream("beacon.properties");    		
			prop.load(input);
			elasticSearchInstance = prop.getProperty("elasticsearch.server");
			elasticSearchIndex    = prop.getProperty("elasticsearch.index");
			elasticSearchType     = prop.getProperty("elasticsearch.type");
			elasticSearchPort	  = Integer.parseInt(prop.getProperty("elasticsearch.port"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	

	
	public boolean searchVariant(String chrom, Long pos, String allele, String ref, String dataset) throws UnknownHostException {		
		
		boolean foundVariant = false;
		
		loadConf();
		log.info("# es instance: " + elasticSearchInstance);
		log.info("# es port: " + elasticSearchPort);		
		
		// for internal details of elasticsearch schema
		Chromosome chromosome = Chromosome.fromString(chrom);
		
		switch(chromosome){			
			case CHRMT:
				chrom = Integer.toString(23);
				break;
			case CHRX:
				chrom = Integer.toString(24);
				break;
			case CHRY:
				chrom = Integer.toString(25);
				break;
			default:
				break;			
		}
				
		@SuppressWarnings("resource")
		//Client client = new TransportClient().addTransportAddress(new InetSocketTransportAddress(elasticSearchInstance, elasticSearchPort));                
		TransportClient client = TransportClient.builder().build().addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(elasticSearchInstance), elasticSearchPort));		
		QueryBuilder filter = QueryBuilders.andQuery(QueryBuilders.termQuery("chrom", chrom), QueryBuilders.termQuery("pos", pos));	

		//FilterBuilder filter = FilterBuilders.andFilter(FilterBuilders.termFilter("chrom", chrom), FilterBuilders.termFilter("pos", pos));		
		SearchResponse response = client.prepareSearch(elasticSearchIndex).setTypes(elasticSearchType).setPostFilter(filter).execute().actionGet();
			
		for (  SearchHit hit : response.getHits()) {
			Map<String,Object> element = hit.getSource();
			
			// for testing purpose
			//log.info("es response: " + response.toString());
			
			if (allele.equalsIgnoreCase(element.get("alt").toString()))
				return true;						   
		  }
		
		client.close();		
		return foundVariant;
	}   
}