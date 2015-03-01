package org.bugogre.crawler.parser

import akka.actor.{Actor, Props}
import org.bugogre.crawler.fetcher.PageFetcher
import org.bugogre.crawler.html.Page
import org.bugogre.crawler.httpclient.Web
import org.bugogre.crawler.indexer.{FieldSelector, IndexField, PageIndexer}
import org.bugogre.crawler.url.FetchItem
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

/**
 * Created by xiachen on 12/16/14.
 */
class PageParser extends Actor{
  lazy val LOG = LoggerFactory.getLogger(getClass.getName)
  val pageIndexer = context.actorOf(Props[PageIndexer], "PageIndexer")
  val webFetcher = context.actorOf(Props[PageFetcher], "WebFetcher")
  lazy val m = java.security.MessageDigest.getInstance("MD5")

  def hash(s: String): String = {
    val b = s.getBytes("UTF-8")
    m.update(b, 0, b.length)
    new java.math.BigInteger(1, m.digest()).toString(16)
  }

  def parse(html: String ): Page = parse(html, null)

  def hrefs(doc: Document): Unit = {
    doc.select("a").asScala.map(u => println(u.attr("href")))
  }

  def parse(html: String, item: FetchItem): Page = {
    val doc = Jsoup.parse(html)
    //fetcher(doc)
    Page(doc, item, hash(html), parseBySelector(doc, item.selectors))
  }

  def selectBySelector(doc: Document, selector: String): String = {
    doc.select(selector).first().html()
  }

  def parseBySelector(doc: Document, fieldSelectors: List[FieldSelector]): List[IndexField] = {
    for (fieldSelector <- fieldSelectors) yield IndexField(fieldSelector.field, selectBySelector(doc, fieldSelector.selector))
  }

  def parse(web: Web[FetchItem]): Page = parse(web.html, web.fetchItem)

  def receive = {
    case str: String => {
    }
    case web: Web[FetchItem] => {
      LOG.info("Parse Url: " + web.fetchItem.url)
      val page = parse(web)
      pageIndexer ! page
      sender() ! web.fetchItem.url + "parse finished."
    }
  }
}