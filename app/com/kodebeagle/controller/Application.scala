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

package com.kodebeagle.controller

import com.kodebeagle.model.{FileName, TypeName}
import com.kodebeagle.service.EsService._
import com.kodebeagle.util.Logger
import play.api.mvc.{Action, AnyContent, Controller}

object Application extends Controller with Logger {

  type Response = Action[AnyContent]

  def suggest(text: String): Response = Action {
    logger.info(s"GET on /suggest with text $text")
    Ok(queryForSuggestion(text))
  }

  def typeRefs(query: String): Response = Action {
    logger.info(s"GET on /typereference with query $query")
    Ok(queryForTypeRefs(query).toString)
  }

  def source(file: String): Response = Action {
    logger.info(s"GET on /source with file $file")
    val fileContent = queryForSources(List(file)).headOption.map(_._2).getOrElse("")
    Ok(fileContent)
  }

  def search(query: String): Response = Action {
    logger.info(s"GET on /search with query $query")
    Ok(queryTypeRefsAndSources(query))
  }

  def properties(types: String): Response = Action {
    logger.info(s"GET on /properties with type $types")
    Ok(queryForProps(types))
  }

  def metadataWithFile(file: String): Response = Action {
    logger.info(s"GET on /metadata with file $file")
    Ok(queryForMetadata(FileName(file)))
  }

  def metadataWithType(`type`: String): Response = Action {
    logger.info(s"GET on /metadata with type ${`type`}")
    Ok(queryForMetadata(TypeName(`type`)))
  }

  def repoDetails(repo: String): Response = Action {
    logger.info(s"GET on /repodetails with repo $repo")
    Ok(queryForRepoDetails(repo))
  }

  def fileDetails(file: String): Response = Action {
    logger.info(s"GET on /filedetails with file $file")
    Ok(queryForFileDetails(file))
  }

  def matchTypes(repo: String, query: String): Response = Action {
    logger.info(s"GET on /matchtypes with repo $repo and query $query")
    Ok("")
  }
}
