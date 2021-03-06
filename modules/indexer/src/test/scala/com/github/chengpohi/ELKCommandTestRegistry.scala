package com.github.chengpohi

import com.github.chengpohi.api.ElasticDSL
import com.github.chengpohi.helper.ResponseGenerator
import com.github.chengpohi.parser.{ELKParser, InterceptFunction, ParserUtils}
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.node.Node

/**
  * elasticshell
  * Created by chengpohi on 4/4/16.
  */

object ELKCommandTestRegistry {
  private[this] val settings: Settings = Settings.builder()
    .put("http.enabled", "false")
    .put("cluster.name", "distribution_run")
    .put("path.repo", "./target/elkrepo")
    .put("action.destructive_requires_name", "false")
    .put("path.home", "./target/elkdata")
    .put("transport.type", "local")
    .build()
  val node = new Node(settings).start()
  val client = node.client()
  val elasticdsl = new ElasticDSL(client)
  val responseGenerator = new ResponseGenerator
  private[this] val elkCommand = new InterceptFunction(elasticdsl)
  private[this] val parserUtils = new ParserUtils
  val elkParser = new ELKParser(elkCommand)

}

