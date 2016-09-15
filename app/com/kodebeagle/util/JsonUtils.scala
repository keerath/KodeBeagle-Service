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

import com.kodebeagle.model.{ClientQuery, QUERY_TYPE, Query}
import org.elasticsearch.action.search.SearchResponse
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsArray, _}
import com.kodebeagle.util.Implicits.SearchResponseHandler

object JsonUtils {

  private implicit val queryReads: Reads[Query] = (
    (JsPath \ "term").read[String] and
      (JsPath \ "type").read[String].map(toQueryType)
    ) (Query.apply _)

  private implicit val clientQueryReads: Reads[ClientQuery] = (
    (JsPath \ "queries").read[List[Query]] and
      (JsPath \ "from").read[Int] and
      (JsPath \ "size").read[Int]
    ) (ClientQuery.apply _)

  private def toQueryType(queryType: String) = {
    if (queryType == "type") {
      QUERY_TYPE.TYPE
    } else if (queryType == "method") {
      QUERY_TYPE.METHOD
    } else {
      QUERY_TYPE.WORD
    }
  }

  def extractFileNames(payload: JsObject): List[String] =
    ((payload \ "hits") \\ "fileName").map(_.as[String]).toList

  def transformSuggestResponse(response: String): String = {
    val jsObj = Json.parse(response).as[JsObject]

    val transformedFields = jsObj.value.map { case (key, value) =>
      val pickOptions = (__ \ 'options).json.pick
      (key, value.as[JsArray].value.head.as[JsObject].transform(pickOptions).get)
    }.toSeq
    JsObject(transformedFields).toString()
  }

  def makeClientQuery(query: String): ClientQuery = Json.parse(query).as[ClientQuery]

  def transformTypeRefs(response: SearchResponse,
                        queriedTypesAndProps: Map[String, List[String]]): JsObject = {
    val hits = response.getHitsAsStringList
    val hitsCount = response.getHitsCount
    val queriedTypes = queriedTypesAndProps.keys.toList

    val relatedTypes = response.getSignificantStringTerms("significantTypes")
      .diff(queriedTypes.map(_.toLowerCase())).take(3)

    val transformedHits = hits.flatMap { hit =>
      val payload = Json.parse(hit).as[JsObject] \ "payload"
      val file = (payload \ "file").as[JsString]
      val score = (payload \ "score").as[JsNumber]
      val types = (payload \ "types").as[JsArray]

      val filteredTypes = types.value.filter(`type` => {
        queriedTypes.contains((`type`.as[JsObject] \ "name").as[String])
      })

      val filteredTypesAndProps = filteredTypes.map { `type` =>
        val typeObj = `type`.as[JsObject]
        val typeName = (typeObj \ "name").as[String]
        val queriedProps = queriedTypesAndProps(typeName)
        val props = (typeObj \ "props").as[JsArray]

        val filteredProps =  if (queriedProps.nonEmpty) {
          props.value.filter { prop =>
            val propName = (prop \ "name").as[String]
            queriedProps.contains(propName)
          }
        } else {
          props.value
        }

        JsObject(Seq("name" -> JsString(typeName),
          "props" -> JsArray(filteredProps)))
      }

      if (filteredTypesAndProps.nonEmpty) {
        Some(JsObject(Seq(("types", JsArray(filteredTypesAndProps)),
          ("score", score), ("fileName", file))))
      } else {
        None
      }
    }

    JsObject(Seq("hits" -> JsArray(transformedHits), "total_hits" -> JsNumber(hitsCount),
      "related_types" -> JsArray(relatedTypes.map(JsString))))
  }

  def transformMetadata(hits: List[String]): String =
    JsArray(hits.map(hit => Json.parse(hit))).toString()

  def transformIntention(hits: List[String]): List[String] =
    hits.map { hit => (Json.parse(hit) \ "name").as[String] }

  def toListOfTypeNames(typeNames: String): List[String] =
    Json.parse(typeNames).as[List[String]].map(_.toLowerCase)

  def transformProps(hits: List[String]): String = {
    JsArray(hits.map { hit =>
      val obj = Json.parse(hit).as[JsObject]
      val name = obj \ "name"
      val methods = obj \ "methods"
      JsObject(Seq("typeName" -> name, "props" -> methods))
    }).toString
  }

  def transformSrcFile(srcHits: List[String]): Map[String, String] =
    srcHits.map { hit =>
      val srcObj = Json.parse(hit).as[JsObject]
      val fName = (srcObj \ "fileName").as[String]
      val fContent = (srcObj \ "fileContent").as[String]
      (fName, fContent)
    }.toMap


  def toJson(hits: List[String]): String = JsArray(hits.map(Json.parse)).toString()

  def combineTypeRefsAndSources(payload: JsObject, files: Map[String, String]): String = {
    val hits = (payload \ "hits").as[JsArray].value
    val total_hits = payload \ "total_hits"
    val relatedTypes = payload \ "related_types"

    val hitsWithFileContent = hits.map {
      case obj: JsObject =>
        val fileName = (obj \ "fileName").as[String]
        obj + ("fileContent", JsString(files(fileName)))
    }

    JsObject(Seq("hits" -> JsArray(hitsWithFileContent),
      "total_hits" -> total_hits, "related_types" -> relatedTypes)).toString
  }
}
