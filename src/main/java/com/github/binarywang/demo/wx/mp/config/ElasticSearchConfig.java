package com.github.binarywang.demo.wx.mp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author Binary Wang
 */
@Configuration
public class ElasticSearchConfig {
  @Value("#{esProperties.es_host}")
  private String host;

  @Value("#{esProperties.es_query_count}")
  private String queryCount;

  @Value("#{esProperties.es_query}")
  private String query;

  public String getHost() {
    return this.host;
  }

  public String getQueryCount() {
    return this.queryCount;
  }

  public String getQuery() {
    return this.query;
  }

}
