package org.bugogre.crawler.indexer

import akka.actor.Actor
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.source.StringDocumentSource
import com.typesafe.config.ConfigFactory
import org.bugogre.crawler.html.Page
import org.elasticsearch.common.settings.ImmutableSettings
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.parsing.json.JSONObject
import scala.util.{Failure, Success}


/**
 * Created by xiachen on 1/17/15.
 */
class PageIndexer extends Actor {
  lazy val LOG = LoggerFactory.getLogger(getClass.getName)
  lazy val indexConfig = ConfigFactory.load("indexer.conf").getConfig("index")
  lazy val settings = ImmutableSettings.settingsBuilder().put("cluster.name", indexConfig.getString("cluster.name")).build()
  lazy val client = ElasticClient.remote(settings, (indexConfig.getString("host"), indexConfig.getInt("port")))

  def index4elasticsearch(page: Page): Unit = {
    var indexes = page.indexes.groupBy(k => k.field).map(k => (k._2(0).field, k._2(0).content))
    indexes += ("_md5" -> page.md5)
    indexes += ("_url" -> page.fetchItem.url)
    indexes += ("_date" -> System.currentTimeMillis().toString)

    val state = client execute {
      index into page.fetchItem.indexName -> page.fetchItem.indexType doc StringDocumentSource(JSONObject(indexes).toString())
    }

    state onComplete {
      case Success(t) => LOG.info("Index Url: " + page.fetchItem.url + " Success")
      case Failure(t) => LOG.error("A Index Error Occurrence: " + t.getMessage)
    }
  }

  def receive: Receive = {
    case str: String => {
      str match {
        case "indexUrl" => sender() ! indexConfig.getString("host")
      }
    }
    case page: Page => {
      LOG.info("Index Url: " + page.fetchItem.url)
      index4elasticsearch(page)
      sender() ! "page index"
    }
  }
}
