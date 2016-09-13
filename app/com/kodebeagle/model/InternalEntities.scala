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

package com.kodebeagle.model

import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder
import org.elasticsearch.search.sort.SortOrder
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder

case class TypeNameWithBool(name: String, isMust: Boolean)

case class Type(nameWithBool: TypeNameWithBool, props: List[String])

trait MetadataReq {
  def text: String
}

case class FileName(text: String) extends MetadataReq

case class TypeName(text: String) extends MetadataReq

case class EsSearchRequest(indices: List[String], types: List[String], query: QueryBuilder,
                           includeFields: Array[String] = Array("*"),
                           from: Int = 0, size: Int = 10,
                           sort: Sort = Sort("_score", SortOrder.DESC),
                           aggregation: Option[AbstractAggregationBuilder] = None)

case class EsSuggestRequest(index: String, suggestText: String,
                            completionBuilders: List[CompletionSuggestionBuilder])

case class Sort(field: String, sortOrder: SortOrder)
