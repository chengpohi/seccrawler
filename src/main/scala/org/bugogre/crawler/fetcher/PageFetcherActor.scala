package org.bugogre.crawler.fetcher


import akka.actor.{Actor, ActorSystem, Props}
import com.secer.elastic.model.FetchItem
import com.typesafe.config.ConfigFactory
import org.bugogre.crawler.fetcher.impl.HtmlPageFetcher
import org.bugogre.crawler.parser.PageParser
import org.slf4j.LoggerFactory

object PageFetcherActor {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("Crawler", ConfigFactory.load("fetcher"))
    system.actorOf(Props[PageFetcherActor], "pagefetcher")
  }
}

class PageFetcherActor extends Actor {
  lazy val LOG = LoggerFactory.getLogger(getClass.getName)

  val pageParser = context.actorOf(Props[PageParser], "htmlParser")

  val htmlPageFetcher = new HtmlPageFetcher(pageParser)

  override def preStart(): Unit = {
  }


  def receive = {
    case str: String =>
      pageParser ! str
    case fetchItem: FetchItem => {
      htmlPageFetcher.asyncFetch(fetchItem)
      sender() ! s"${fetchItem.url.toString} async fetching."
    }
    case fetchItems: List[_] =>
      fetchItems.asInstanceOf[List[FetchItem]]
        .filter(_.url.toString.length != 0)
        .foreach(fetchItem => htmlPageFetcher.asyncFetch(fetchItem))
  }
}
