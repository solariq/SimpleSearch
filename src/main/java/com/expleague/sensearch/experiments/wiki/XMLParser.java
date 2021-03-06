package com.expleague.sensearch.experiments.wiki;

import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument.Link;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument.Section;
import com.expleague.sensearch.experiments.wiki.WikiPage.WikiLink;
import com.expleague.sensearch.experiments.wiki.WikiPage.WikiSection;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.stream.XMLStreamReader;
import org.apache.log4j.Logger;

public class XMLParser {

  private static final Logger LOG = Logger.getLogger(XMLParser.class);

  private static final JAXBContext context;

  static {
    try {
      context = JAXBContext.newInstance(XmlPageRootElement.class);
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    }
  }

  public WikiPage parseXML(File file) {
    try (final FileInputStream fis = new FileInputStream(file)) {
      return parseXML(fis);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public WikiPage parseXML(Path file) {
    try (final InputStream fis = Files.newInputStream(file)) {
      return parseXML(fis);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public WikiPage parseXML(XmlPage page) {
    if (page == null) {
      return null;
    }
    return parsePage(page);
  }

  public Object parseXMLToRaw(InputStream file) {
    try {
      Unmarshaller um = context.createUnmarshaller();
      return um.unmarshal(file);
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    }
  }

  public XmlPage parseXMLToRaw(XMLStreamReader reader) {
    try {
      Unmarshaller um = context.createUnmarshaller();
      return (XmlPage) um.unmarshal(reader);
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    }
  }

  public void saveXml(XmlPageRootElement xml, Path path) {
    try {
      try (BufferedWriter w = Files.newBufferedWriter(path)) {
        Marshaller m = context.createMarshaller();
        m.marshal(xml, w);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    }
  }

  public WikiPage parseXML(InputStream file) {
    try {
      Unmarshaller um = context.createUnmarshaller();
      XmlPageRootElement element = (XmlPageRootElement) um.unmarshal(file);
      XmlPage xmlPage = element.page;

      return parsePage(xmlPage);
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    }
  }

  public CrawlerDocument parseXML(XMLStreamReader reader) {
    try {
      Unmarshaller um = context.createUnmarshaller();
      return parsePage(um.unmarshal(reader, XmlPage.class).getValue());
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    }
  }

  private WikiPage parsePage(XmlPage xmlPage) {
    WikiPage page = new WikiPage();

    page.setTitle(xmlPage.title == null ? "" : xmlPage.title);

    URI pageUri = createUriForTitle(page.title());
    page.setUri(pageUri);

    if (xmlPage.categories == null) {
      page.setCategories(new ArrayList<>());
    } else {
      page.setCategories(Arrays.asList(xmlPage.categories.split("@")));
    }

    Set<URI> uriSet = new HashSet<>();

    List<Section> sections;
    sections =
        xmlPage
            .sections
            .stream()
            .map(
                xmlSection -> {
                  List<Link> links = new ArrayList<>();
                  StringBuilder text = new StringBuilder();

                  if (xmlSection.content != null) {
                    for (Serializable serializable : xmlSection.content) {
                      if (serializable instanceof String) {
                        text.append(((String) serializable).trim());
                      } else if (serializable instanceof XmlSectionLink) {
                        XmlSectionLink link = (XmlSectionLink) serializable;

                        if (link.targetId == 0) {
                          link.targetId = -1;
                        }
                        if (link.targetTitle == null) {
                          link.targetTitle = "";
                        }

                        if (text.length() > 0 && text.charAt(text.length() - 1) != ' ') {
                          text.append(" ");
                        }
                        links.add(
                            new WikiLink(
                                link.text,
                                link.targetTitle,
                                link.targetId,
                                text.length(),
                                createUriForTitle(link.targetTitle)));
                        text.append(link.text.trim());
                        text.append(" ");
                      }
                    }
                  }

                  String sectionTitle = xmlSection.title == null ? "" : xmlSection.title;
                  List<CharSequence> titles = Arrays.asList(sectionTitle.split("\\|@\\|"));
                  /*
                  try {
                    subPageURI = URI.create(
                        "https://ru.wikipedia.org/wiki/"
                            + page.title().replace(" ", "_").replace("%", "%25")
                            + "#" + URLEncoder.encode(section));
                  } catch (IllegalArgumentException e) {
                    System.err.println(e.getMessage());
                  }
                  System.err.println(subPageURI.toString());//*/

                  return new WikiSection(text, titles, links, null);
                })
            .collect(Collectors.toList());

    List<Section> newSections = new ArrayList<>();
    if (sections.size() > 0 && sections.get(0).titles().size() > 1) {
      List<CharSequence> title = sections.get(0).titles();
      for (int i = 1; i < title.size(); i++) {
        newSections.add(
            new WikiSection(
                "",
                title.subList(0, i),
                Collections.emptyList(),
                createUriForTitle(pageUri.toString(), title.get(i), uriSet)));
      }
    }
    for (int i = 0; i < sections.size(); i++) {
      Section curSection = sections.get(i);
      newSections.add(
          new WikiSection(
              curSection.text(),
              curSection.titles(),
              curSection.links(),
              createUriForTitle(
                  pageUri.toString(),
                  curSection.titles().get(curSection.titles().size() - 1),
                  uriSet)));

      if (i == sections.size() - 1) {
        continue;
      }
      List<CharSequence> curSectionTitle = curSection.titles();
      List<CharSequence> newSectionTitle = sections.get(i + 1).titles();

      int commonSections = 0;
      for (int j = 0; j < Math.min(curSectionTitle.size(), newSectionTitle.size()); j++) {
        if (curSectionTitle.get(j).equals(newSectionTitle.get(j))) {
          commonSections++;
        } else {
          break;
        }
      }

      for (int j = commonSections; j < newSectionTitle.size() - 1; j++) {
        newSections.add(
            new WikiSection(
                "",
                newSectionTitle.subList(0, j + 1),
                Collections.emptyList(),
                createUriForTitle(pageUri.toString(), newSectionTitle.get(j), uriSet)));
      }
    }

    if (newSections.size() > 0) {
      Section section = newSections.get(0);
      newSections.set(
          0,
          new WikiSection(
              section.text(),
              Collections.singletonList(section.title()),
              section.links(),
              page.uri()));
    }
    page.setSections(newSections);

    return page;
  }

  private URI createUriForTitle(String pageUri, CharSequence sectionTitle, Set<URI> uriSet) {
    URI subPageURI = createUriInternal(pageUri, sectionTitle);
    int uriIdx = 1;
    // Several subsections can have the same title but they URIs must be different
    while (uriSet.contains(subPageURI)) {
      uriIdx++;
      subPageURI = createUriInternal(pageUri, sectionTitle + "_" + uriIdx);
    }
    uriSet.add(subPageURI);

    return subPageURI;
  }

  private URI createUriForTitle(String title) {
    String pageUri = URLEncoder.encode(title.replace(" ", "_").replace("%", "%25"));
    return URI.create("https://ru.wikipedia.org/wiki/" + pageUri);
  }

  private URI createUriInternal(String pageURI, CharSequence sectionTitle) {
    try {
      return URI.create(
          pageURI
              + "#"
              + URLEncoder.encode(
              sectionTitle
                  .toString()
                  .replaceAll("\\p{Zs}", "_")
                  .replaceAll("\\p{javaWhitespace}", "_")
                  .replace("%", "%25"),
              "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      LOG.error(e);
      return null;
    }
  }

  @XmlRootElement(name = "pages")
  public static class XmlPageRootElement {

    @XmlElement(name = "page")
    public XmlPage page;
  }

  @XmlRootElement(name = "page")
  public static class XmlPage {

    @XmlAttribute(name = "id")
    public long id;

    @XmlAttribute(name = "title")
    public String title;

    @XmlAttribute(name = "categories")
    String categories;

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @XmlElementWrapper(name = "sections")
    @XmlElement(name = "section")
    List<XmlSection> sections;
  }

  @XmlRootElement(name = "section")
  private static class XmlSection {

    @XmlAttribute(name = "title")
    String title;

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @XmlElementRef(name = "link", type = XmlSectionLink.class)
    @XmlMixed
    List<Serializable> content;
  }

  @XmlRootElement(name = "link")
  private static class XmlSectionLink implements Serializable {

    @XmlValue
    String text;

    @XmlAttribute(name = "target")
    String targetTitle;

    @XmlAttribute(name = "targetId")
    long targetId;
  }

  /*
  private void writeXML(WikiPage page) {
      String fileName = "/home/artem/JetBrains/WikiDocs/Mini_Wiki/" + page.getID() + ".xml";
      String startElement = "page";

      XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
      try {
          XMLStreamWriter xmlStreamWriter = xmlOutputFactory.
                  createXMLStreamWriter(new FileOutputStream(fileName), "UTF-8");
          xmlStreamWriter.writeStartDocument("UTF-8", "1.0");
          xmlStreamWriter.writeCharacters("\n");

          xmlStreamWriter.writeStartElement("Pages");
          xmlStreamWriter.writeCharacters("\n");

          xmlStreamWriter.writeStartElement(startElement);
          xmlStreamWriter.writeAttribute("id", Long.toString(page.id()));
          xmlStreamWriter.writeAttribute("title", page.title());
          xmlStreamWriter.writeAttribute("revision", page.revision);
          xmlStreamWriter.writeAttribute("type", page.type);
          xmlStreamWriter.writeAttribute("ns-id", page.nsId);
          xmlStreamWriter.writeAttribute("ns-name", "");
          xmlStreamWriter.writeCharacters("\n");
          xmlStreamWriter.writeCharacters(page.content().toString());
          xmlStreamWriter.writeCharacters("\n");
          xmlStreamWriter.writeEndElement();

          xmlStreamWriter.writeCharacters("\n");
          xmlStreamWriter.writeEndElement();
          xmlStreamWriter.writeEndDocument();

          xmlStreamWriter.flush();
          xmlStreamWriter.close();


      } catch (FileNotFoundException | XMLStreamException e) {
          e.printStackTrace();
      }
  }//*/
}
