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

package com.kodebeagle.service

import com.kodebeagle.model.{EsSearchRequest, EsSuggestRequest, FileName, MetadataReq, Sort, TypeName}
import com.kodebeagle.util.Implicits.SearchResponseHandler
import com.kodebeagle.util.JsonUtils._
import com.kodebeagle.util.QueryBuilderUtils._
import com.kodebeagle.util.QueryIntentionUtils._
import com.kodebeagle.util.{EsClient, Logger}
import org.elasticsearch.search.sort.SortOrder
import play.api.libs.json.JsObject

object EsService extends App with Logger {

  /* Queries types i.e. (name and props) against java/typerefs */

  def queryForTypeRefs(queryString: String): JsObject = {
    val query = makeClientQuery(queryString)
    val types =  resolveTypes(query.queries)
    val fromSize = (query.from, query.size)

    val searchRequest = EsSearchRequest(indices = List(JAVA_INDEX),
      types = List(SEARCH_TYPE), query = searchQuery(types),
      Array("payload"), from = fromSize._1, size = fromSize._2,
      aggregation = Some(significantTermsFromTypesQuery))

    val response = EsClient.search(searchRequest)
    val typeAndProps = types.map(t => (t.nameWithBool.name, t.props))

    transformTypeRefs(response, typeAndProps.toMap)
  }

  /* Queries for props against java/aggregation with types names */

  def queryForProps(typeNamesJson: String): String = {
    val typeNames = toListOfTypeNames(typeNamesJson)
    val request = EsSearchRequest(indices = List(JAVA_INDEX), types = List(AGG_TYPE),
      query = aggQuery(typeNames),
      includeFields = Array("name", "methods.name", "methods.count"), from = 0,
      size = typeNames.size, Sort("score", SortOrder.DESC))
    transformProps(EsClient.search(request).getHitsAsStringList)
  }

  /* Queries for metadata against java/filemetadata with either fileName or fileType */

  def queryForMetadata(metaReq: MetadataReq): String = {
    val metaQuery = metaReq match {
      case FileName(name) => fileMetaQuery("fileName", name)
      case TypeName(name) => fileMetaQuery("fileTypes.fileType", name.toLowerCase)
    }

    val request = EsSearchRequest(indices = List(JAVA_INDEX),
      types = List(METADATA_TYPE), query = metaQuery)
    val response = EsClient.search(request)
    transformMetadata(response.getHitsAsStringList)
  }

  /* Queries for type and method names against java/_suggest with text */

  def queryForSuggestion(suggestText: String): String = {
    val typeSuggester = suggestionQuery("types", "typeSuggest")
    val methodSuggester = suggestionQuery("props", "methodSuggest")
    val request = EsSuggestRequest(JAVA_INDEX, suggestText, List(typeSuggester, methodSuggester))

    val response = EsClient.suggest(request)
    transformSuggestResponse(response)
  }

  /* Queries for file content against java/sourcefile with file names */

  def queryForSources(fileNames: List[String]): Map[String, String] = {
    val searchRequest = EsSearchRequest(indices = List(JAVA_INDEX),
      types = List(SOURCE_TYPE), query = idsQuery(List("sourcefile"), fileNames),
      includeFields = Array("fileName", "fileContent"))

    val response = EsClient.search(searchRequest)
    transformSrcFile(response.getHitsAsStringList)
  }

  /* Queries for types against java/typereference for file names and
     then queries these file names against java/sourcefile for file
     content and stitches their response */

  def queryTypeRefsAndSources(queryString: String): String = {
    val payload = queryForTypeRefs(queryString)
    val fileNames = extractFileNames(payload)
    val sources = queryForSources(fileNames)
    combineTypeRefsAndSources(payload, sources)
  }

  /* Queries for file details against java/filedetails */

  def queryForFileDetails(fileName: String): String = {
    val searchRequest = EsSearchRequest(
      indices = List(JAVA_INDEX), types = List(FILEDETAILS_TYPE),
      query = idsQuery(List(FILEDETAILS_TYPE), List(fileName)),
      from = 0, size = 1)

    val response = EsClient.search(searchRequest)
    toJson(response.getHitsAsStringList)
  }

  /* Queries for repo details against java/repodetails */

  def queryForRepoDetails(repoName: String): String = {
    val searchRequest = EsSearchRequest(
      indices = List(JAVA_INDEX), types = List(REPODETAILS_TYPE),
      query = idsQuery(List(REPODETAILS_TYPE), List(repoName)),
      includeFields = Array("gitHubInfo", "gitHistory.mostChanged"),
      from = 0, size = 1)

    val response = EsClient.search(searchRequest)
    toJson(response.getHitsAsStringList)
  }
}
