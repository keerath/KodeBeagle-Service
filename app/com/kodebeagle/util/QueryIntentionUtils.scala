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

import com.kodebeagle.model._
import com.kodebeagle.util.JsonUtils._
import com.kodebeagle.util.QueryBuilderUtils._
import com.kodebeagle.util.Implicits.SearchResponseHandler

object QueryIntentionUtils {

  val JAVA_INDEX = "java"
  val SEARCH_TYPE = "typereference"
  val METADATA_TYPE = "filemetadata"
  val SOURCE_TYPE = "sourcefile"
  val AGG_TYPE = "aggregation"
  val FILEDETAILS_TYPE = "filedetails"
  val REPODETAILS_TYPE = "repodetails"
  val AGG_SIZE = 3

  /* Converts client queries i.e. only (type) queries,(type & method) name queries
   and (word) query.The word query is converted to a (type & method) query by
   querying against aggregation */

  def resolveTypes(queries: List[Query]): List[Type] = {

    def handleWords(words: Option[String]) = queryIntention(words).map(typeName =>
      Type(TypeNameWithBool(typeName, isMust = false), words.get.split(" ").toList))

    val typeQueries = queries.filter(_.queryType == QUERY_TYPE.TYPE)
      .map(query => Type(TypeNameWithBool(query.text, isMust = true), List()))

    val methodQueries = queries filter (_.queryType == QUERY_TYPE.METHOD) map { query =>
      val (typeName, method) = query.text.splitAt(query.text.lastIndexOf('.'))
      Type(TypeNameWithBool(typeName, isMust = true), List(method.stripPrefix(".")))
    }

    val wordsQuery = handleWords(queries.find(_.queryType == QUERY_TYPE.WORD).map(_.text))

    val typeGroup = (typeQueries ++ methodQueries ++ wordsQuery)
      .groupBy(_.nameWithBool).mapValues(_.flatMap(_.props))

    typeGroup.map {
      case (typNameWithBool, props) => Type(typNameWithBool, props)
    }.filter(_.nameWithBool.name.nonEmpty).toList
  }

  /* Queries for the intention of words against
   aggregation and returns type names */

  private def queryIntention(maybeWords: Option[String]) = {
    maybeWords.map { words =>
      val request = EsSearchRequest(indices = List(JAVA_INDEX), types = List(AGG_TYPE),
        query = intentQuery(words), Array("name"), from = 0, size = AGG_SIZE)

      val response = EsClient.search(request)
      transformIntention(response.getHitsAsStringList)
    }.getOrElse(List[String]())
  }
}
