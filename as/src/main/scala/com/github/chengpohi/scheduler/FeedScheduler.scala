/**
  * chengpohi@gmail.com
  */
package com.github.chengpohi.scheduler

import akka.actor.ActorRef
import com.github.chengpohi.config.AppStoreConfig.SELECTORS
import com.github.chengpohi.model.{Feed, FetchItem}
import org.json4s._
import org.json4s.native.JsonMethods._
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory

/**
  * secer
  * Created by chengpohi on 10/13/16.
  */
class FeedScheduler(feeds: List[Feed], crawler: ActorRef) extends Runnable {
  lazy val logger = LoggerFactory.getLogger(getClass.getName)
  implicit val formats = org.json4s.DefaultFormats

  override def run(): Unit = {
    val fetchItems: List[FetchItem] = feeds.flatMap(f => {
      logger.info("get feed type: {}, genre: {}, url: {}", f.feedType, f.genre, f.url)
      val body: String = Jsoup.connect(f.url).timeout(1000 * 20).execute().body()
      for {
        r <- (parse(body) \ "feed" \\ "link" \\ "href").children.map(i => i.extract[String])
      } yield FetchItem(r, "appstore", f.genre, SELECTORS,
        Some("^https:\\/\\/itunes\\.apple\\.com\\/%s\\/app\\/.*".format(f.country)),
        delay = Some(2000))
    })
    fetchItems.foreach(item => {
      crawler ! item
    })
    logger.info("send {} items finished", fetchItems.size)
  }
}