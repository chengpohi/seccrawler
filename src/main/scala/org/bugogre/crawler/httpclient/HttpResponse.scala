package org.bugogre.crawler.httpclient

import com.secer.elastic.model.FetchItem
import org.apache.http.HttpEntity
import org.bugogre.crawler.filter.PageFilter
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory

case class Web(fetchItem: FetchItem, doc: Document) {
  def filter(): Boolean = {
    PageFilter.filterContent(fetchItem, doc.html())
  }
}

object HttpResponse {
  val LOG = LoggerFactory.getLogger(getClass.getName)

  def getEntity(url: String): HttpEntity = {
    null
  }

  def getEntityToStr(item: FetchItem): Web = {
    val doc = Jsoup.connect(item.url)
      .timeout(3000)
      .get()

    Web(item, doc)
  }

  def ==>(item: FetchItem): Web = {
    getEntityToStr(item)
  }
}
