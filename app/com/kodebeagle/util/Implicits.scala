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

import scala.collection.JavaConversions._
import org.elasticsearch.action.search.{SearchRequestBuilder, SearchResponse}
import org.elasticsearch.action.suggest.SuggestRequestBuilder
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder
import org.elasticsearch.search.aggregations.bucket.significant.{SignificantStringTerms, UnmappedSignificantTerms}
import org.elasticsearch.search.suggest.SuggestBuilder.SuggestionBuilder

object Implicits {

  implicit class FancySearchRequestBuilder(searchRequestBuilder: SearchRequestBuilder) {
    def addAggregationIfDefined(maybeAggregationBuilder:
                                Option[AbstractAggregationBuilder]): SearchRequestBuilder = {
      maybeAggregationBuilder match {
        case Some(aggregationBuilder) => searchRequestBuilder.addAggregation(aggregationBuilder)
        case None =>
      }
      searchRequestBuilder
    }
  }

  implicit class FancySuggestRequestBuilder(suggReqBuilder: SuggestRequestBuilder) {
    def addSuggestions[T](suggestions: List[SuggestionBuilder[T]]): SuggestRequestBuilder = {
      suggestions.foreach(suggReqBuilder.addSuggestion)
      suggReqBuilder
    }
  }

  implicit class SearchResponseHandler(searchResponse: SearchResponse) {
    def getHitsAsStringList: List[String] = searchResponse.getHits.map(_.sourceAsString()).toList

    def getHitsCount: Long = searchResponse.getHits.totalHits()

    def getSignificantStringTerms(aggName: String): List[String] =
      searchResponse.getAggregations.asMap().get(aggName)
        .asInstanceOf[SignificantStringTerms]
        .getBuckets.map(_.getKeyAsString).toList
  }

}
