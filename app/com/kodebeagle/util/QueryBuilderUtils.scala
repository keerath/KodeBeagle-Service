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

import com.kodebeagle.model.Type
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder
import org.elasticsearch.index.query.functionscore.script.ScriptScoreFunctionBuilder
import org.elasticsearch.index.query.{BoolQueryBuilder, IdsQueryBuilder, QueryBuilder, QueryBuilders, TermQueryBuilder, TermsQueryBuilder}
import org.elasticsearch.script.Script
import org.elasticsearch.search.aggregations.bucket.significant.heuristics.GND.GNDBuilder
import org.elasticsearch.search.aggregations.{AbstractAggregationBuilder, AggregationBuilders}
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder

object QueryBuilderUtils {

  def intentQuery(words: String): FunctionScoreQueryBuilder = {
    val matchQuery = QueryBuilders.matchQuery("searchText", words)
    val functionScore = new ScriptScoreFunctionBuilder(
      new Script("""_score * doc["score"].value"""))
    QueryBuilders.functionScoreQuery(matchQuery, functionScore)
  }

  def searchQuery(types: List[Type]): BoolQueryBuilder = {
    val onlyTypes = types.filter(_.props.isEmpty)
    val typeProps = types.filter(_.props.nonEmpty)
    val typeQueries = onlyTypes.map(typeQuery)
    val typePropsQueries = typeProps.map(typePropsQuery)

    boolQueryWithShould(typeQueries, typePropsQueries)
  }

  def typeQuery(`type`: Type): TermQueryBuilder = {
    termQuery("contexts.types.name", `type`.nameWithBool.name.toLowerCase)
  }

  def idsQuery(types: List[String], ids: List[String]): IdsQueryBuilder =
    QueryBuilders.idsQuery(types: _ *).ids(ids: _*)

  def aggQuery(typeNames: List[String]): BoolQueryBuilder = {
    val termQueries = typeNames.map(termQuery("name", _))
    boolQueryWithShould(termQueries)
  }

  def suggestionQuery(name: String, field: String): CompletionSuggestionBuilder = {
    val builder = new CompletionSuggestionBuilder(name)
    builder.field(field)
  }

  def fileMetaQuery(field: String, text: String): BoolQueryBuilder =
    boolQueryWithShould(List(termQuery(field, text)))


  private def typePropsQuery(`type`: Type) = {
    val nameWithBool = `type`.nameWithBool
    val termQuerya = termQuery("contexts.types.name", nameWithBool.name.toLowerCase)
    val termsQuerya = termsQuery("contexts.types.props", `type`.props.map(_.toLowerCase))
    val boolQuery = QueryBuilders.boolQuery()

    if (nameWithBool.isMust) {
      boolQuery.must(termQuerya)
      boolQuery.must(termsQuerya)
    } else {
      boolQuery.must(termQuerya)
      boolQuery.should(termsQuerya)
    }

    QueryBuilders.nestedQuery("contexts.types", boolQuery)
  }

  def significantTermsFromTypesQuery: AbstractAggregationBuilder =
    AggregationBuilders.significantTerms("significantTypes")
      .field("contexts.types.name").significanceHeuristic(new GNDBuilder(true))

  private def boolQueryWithShould[T <: QueryBuilder](queryBuildersList: List[T]*) = {
    val boolQuery = QueryBuilders.boolQuery()
    queryBuildersList.foreach { queryBuilders =>
      queryBuilders.foreach(boolQuery.should)
    }
    boolQuery
  }

  def termQuery(field: String, value: String): TermQueryBuilder =
    QueryBuilders.termQuery(field, value)

  def termsQuery(field: String, values: List[String]): TermsQueryBuilder =
    QueryBuilders.termsQuery(field, values: _*)
}
