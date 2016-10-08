
package com.github.chengpohi.parser.html

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element, TextNode}

import scala.collection.JavaConverters._

/**
  * Created by xiachen on 3/26/15.
  */
class HtmlToMarkdown {
  val HR: String = "hr"
  val HEADER = List("h1", "h2", "h3", "h4", "h5", "h6")
  val NEW_LINE: String = "###"
  val CONTENT_NEW_LINE: String = "!@#"
  val TABLE_NEW_LINE: String = "%%%"
  val NONE_TAG_NAME: String = "NONE"
  val TABLE_SPLIT_LINE: String = " | "
  val CODE_LINE: String = "~~~"
  val STRONG_SYMBOL: String = "**"
  val STRONG_ITALIC: String = "***"
  val BLOCK_QUOTE: String = ">"
  val THEAD_LINE: String = "-----"

  def parse(html: String): String = {
    html match {
      case "" => ""
      case _ =>
        val doc = Jsoup.parse(html)

        parseElement(doc)
        parseStrong(doc)
        parseHeader(doc)
        parseHR(doc)
        parseHref(doc)
        parseParagraph(doc)
        parseBlockQuotes(doc)
        parseUL(doc)
        parseTable(doc)
        parseCode(doc)
        doc.body().html()
          .replaceAll(NEW_LINE, "\r\n")
          .replaceAll(TABLE_NEW_LINE, "\r\n")
          .replaceAll(CONTENT_NEW_LINE, " ")
          .replaceAll("\\<[^>]*>", "")
    }

  }

  private def parseElement(doc: Document) = doc.select("*").asScala.map(element => {
    if (!element.hasText && element.isBlock) {
      element.remove()
    }
    element.attributes().asScala.map(attr => {
      element.removeAttr(attr.getKey)
    })
  })

  private def parseCode(doc: Document) = doc.getElementsByTag("code").asScala.map(h => {
    val content = CODE_LINE + TABLE_NEW_LINE + h.text() + TABLE_NEW_LINE + CODE_LINE + NEW_LINE
    updateElement(h, content)
  })

  private def parseStrong(doc: Document) = {
    doc.getElementsByTag("strong").asScala.map(h => {
      val content = STRONG_SYMBOL + filterSpace(h.text()) + STRONG_SYMBOL
      updateElement(h, content)
    })

    doc.getElementsByTag("strong").asScala.map(h => {
      h.getElementsByTag("em").asScala.map(e => {
        val content = STRONG_ITALIC + filterSpace(e.text()) + STRONG_ITALIC
        updateElement(h, content)
      })
    })
  }

  private def parseBlockQuotes(doc: Document) = doc.getElementsByTag("blockquote").asScala.map(h => {
    val content = NEW_LINE + BLOCK_QUOTE + CONTENT_NEW_LINE + filterSpace(h.text()) + NEW_LINE
    updateElement(h, content)
  })

  private def parseDiv(doc: Document) = doc.getElementsByTag("div").asScala.map(h => {
    updateElement(h, NEW_LINE + filterSpace(h.text()) + NEW_LINE)
  })

  private def parseTable(doc: Document) = doc.getElementsByTag("table").asScala.filter(u => u.getElementsByTag("thead").isEmpty)
    .map(h => {
      val tr = h.getElementsByTag("thead").first().getElementsByTag("tr")
      val thead = tr.first()
        .getElementsByTag("th")

      val header = {
        val h = for (t <- thead.asScala) yield filterSpace(t.text())
        val split = for (s <- h) yield THEAD_LINE
        filterSpace(h.mkString(TABLE_SPLIT_LINE)) + TABLE_NEW_LINE + filterSpace(split.mkString(TABLE_SPLIT_LINE)) + TABLE_NEW_LINE
      }

      val content = for (tr <- h.getElementsByTag("tbody").first().getElementsByTag("tr").asScala) yield {
        val t = for (t <- tr.getElementsByTag("td").asScala) yield filterSpace(t.text())
        filterSpace(t.mkString(TABLE_SPLIT_LINE))
      }

      updateElement(h, header + content.mkString(TABLE_NEW_LINE) + NEW_LINE)
    })

  private def parseUL(doc: Document) = doc.getElementsByTag("ul").asScala.map(h => {
    h.getElementsByTag("li").asScala.map(l => {
      val content = "-" + CONTENT_NEW_LINE + filterSpace(l.text())
      updateElement(l, content)
    })
  })

  private def parseParagraph(doc: Document) = doc.getElementsByTag("p").asScala.map(h => {
    h.getElementsByTag("code").asScala.map(c => {
      c.html("`" + c.text() + "`")
    })
    val content = NEW_LINE + filterSpace(h.text()) + NEW_LINE
    updateElement(h, content)
  })

  private def parseImage(doc: Document) = doc.getElementsByTag("img").asScala.map(h => {
    val content = "![" + h.attr("alt") + "]" + "(" + h.attr("src") + ")"
    updateElement(h, content)
  })

  private def parseHref(doc: Document) = doc.getElementsByTag("a").asScala.map(h => {
    val content = "[" + filterSpace(h.text()) + "](" + h.attr("href") + ")"
    updateElement(h, content)
  })

  private def parseHR(doc: Document) = doc.getElementsByTag(HR).asScala.map(h => {
    updateElement(h, getHr)
  })

  private def parseHeader(doc: Document) = HEADER.map(t =>
    doc.getElementsByTag(t).asScala.map(h => {
      val content = NEW_LINE + getTitle(t) + filterSpace(h.text()) + NEW_LINE
      updateElement(h, content)
    })
  )

  private def filterSpace(str: String): String = str.replaceAll(NEW_LINE, CONTENT_NEW_LINE)

  private def getHr: String = NEW_LINE + "-----" + NEW_LINE

  private def updateElement(el: Element, content: String) = el.replaceWith(new TextNode(content, ""))

  def getTitle(title: String): String = title match {
    case "h1" => "#"
    case "h2" => "##"
    case "h3" => "###"
    case "h4" => "####"
    case "h5" => "#####"
    case "h6" => "######"
    case _ => "#"
  }
}

