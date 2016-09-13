/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kodebeagle.util

import java.net.InetAddress

import com.kodebeagle.config.EsConfig
import com.kodebeagle.model.{EsSearchRequest, EsSuggestRequest}
import com.kodebeagle.util.Implicits._
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress

object EsClient {

  private val esClient = buildClient

  private def buildClient = {
    val settings = Settings.settingsBuilder()
      .put("cluster.name", EsConfig.esClusterName)
      .put("client.transport.sniff", true).build()

    TransportClient.builder().settings(settings).build
      .addTransportAddress(new InetSocketTransportAddress(
        InetAddress.getByName(EsConfig.esHost), EsConfig.esPort))
  }

  def search(searchRequest: EsSearchRequest): SearchResponse = {
    esClient.prepareSearch(searchRequest.indices: _ *)
      .setTypes(searchRequest.types: _*).setQuery(searchRequest.query)
      .setFetchSource(searchRequest.includeFields, Array[String]())
      .setFrom(searchRequest.from).setSize(searchRequest.size)
      .addAggregationIfDefined(searchRequest.aggregation)
      .addSort(searchRequest.sort.field, searchRequest.sort.sortOrder).execute().get()
  }

  def suggest(suggestRequest: EsSuggestRequest): String = {
    esClient.prepareSuggest(suggestRequest.index).setSuggestText(suggestRequest.suggestText)
      .addSuggestions(suggestRequest.completionBuilders).execute().get().getSuggest.toString
  }
}




