package org.bugogre.crawler.fetcher.impl

import java.net.{NoRouteToHostException, SocketException, UnknownHostException}
import java.util.concurrent.Executors

import akka.actor.ActorRef
import com.secer.elastic.controller.PageController._
import com.secer.elastic.model.FetchItem
import org.apache.http.NoHttpResponseException
import org.apache.http.client.ClientProtocolException
import org.apache.http.conn.HttpHostConnectException
import org.bugogre.crawler.cache.URLCache.FETCH_ITEM_CACHE
import org.bugogre.crawler.config._
import org.bugogre.crawler.httpclient.HttpResponse
import org.slf4j.LoggerFactory

import scala.concurrent._


/**
 * Page Fetcher
 * Created by xiachen on 3/1/15.
 */
class HtmlPageFetcher(pageParser: ActorRef) {
  lazy val LOG = LoggerFactory.getLogger(getClass.getName)

  implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(SecConfig.MAX_THREADS))

  def fetch(fetchItem: FetchItem): String = {
    fetchFilter(fetchItem) match {
      case false =>
        LOG.info(s"${fetchItem.url.toString} item have fetched.")
        s"${fetchItem.url.toString} item have fetched."
      case true =>
        try {
          LOG.info("Fetch Url: " + fetchItem.url.toString)
          LOG.info("Cache Size: " + FETCH_ITEM_CACHE.size)
          FETCH_ITEM_CACHE.put(fetchItem.url.toString, fetchItem)
          val w = HttpResponse ==> fetchItem
          pageParser ! w
          fetchItem.url.toString + " fetch finished."
        } catch {
          case e: ClientProtocolException => "client redirect exception:" + fetchItem.url.toString
          case e: UnknownHostException => "unknown url: " + fetchItem.url.toString
          case e: HttpHostConnectException => "host can't connect exception:" + fetchItem.url.toString
          case e: NoRouteToHostException => "no route to host exception:" + fetchItem.url.toString
          case e: SocketException => "socket exception:" + fetchItem.url.toString
          case e: NoHttpResponseException => "no http response exception: " + fetchItem.url.toString
        }
    }
  }

  def fetchFilter(fetchItem: FetchItem): Boolean = {
    if (FETCH_ITEM_CACHE.containsKey(fetchItem.url.toString))
      return false

    if (pageWhetherExist(fetchItem)) {
      FETCH_ITEM_CACHE.put(fetchItem.url.toString, fetchItem)
      return false
    }
    true
  }

  def asyncFetch(fetchItem: FetchItem): Future[String] = {
    Future {
      blocking {
        fetch(fetchItem)
      }
    }
  }
}
