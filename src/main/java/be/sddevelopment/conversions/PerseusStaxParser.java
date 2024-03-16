package be.sddevelopment.conversions;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.lineSeparator;
import static java.nio.file.Files.write;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.strip;

public class PerseusStaxParser {

    public static void main(String[] args) throws URISyntaxException, IOException {
        var resource = PerseusStaxParser.class.getClassLoader().getResource("raw_export/book1/books1chapter1.xml");
        Path inputFile = Paths.get(resource.toURI());
        var result = new PerseusStaxParser().parse(inputFile);

        var outputFileName = inputFile.toFile().getName().replace(".xml", "") + "_parsed.md";
        Path outputFile = Paths.get("/media/stijnd/DATA/development/projects/ars-rethorica/manuscript").resolve(outputFileName);
        for (ParsedChapter chapter : result.chapters()) {
            write(outputFile, chapter.toMarkdown(result.chapters().indexOf(chapter) + 1));
        }
    }

    public ParsedBook parse(Path fileToParse) {
        try (var filestream = new FileInputStream(fileToParse.toFile())) {
            var factory = XMLInputFactory.newInstance();
            var reader = factory.createXMLEventReader(filestream);
            List<ParsedChapter> chapters = new ArrayList<>();
            List<Section> currentSections = new ArrayList<>();
            List<String> notes = new ArrayList<>();
            String currentSectionContent = "";
            String currentFootNote = "";

            boolean readingNote = false;
            while (reader.hasNext()) {
                var event = reader.nextEvent();
                if (event.isStartElement()) {
                    var startElement = event.asStartElement();
                    var name = startElement.getName().getLocalPart();
                    switch (name) {
                        case "milestone" -> {
                            var mileStoneUnit = startElement.getAttributeByName(new QName("unit"));
                            if ("chapter".equals(mileStoneUnit.getValue()) && !currentSections.isEmpty()) {
                                chapters.add(new ParsedChapter(currentSections, notes));
                                currentSections.clear();
                                notes.clear();
                            }
                        }
                        case "note" -> {
                            readingNote = true;
                            event = reader.nextEvent();
                            if (event.isCharacters()) {
                                currentFootNote += (cleanRawText(event.asCharacters().getData()));
                            }
                        }
                        case "p" -> {
                            event = reader.nextEvent();
                            if (event.isCharacters()) {
                                var contentLine = event.asCharacters().getData();
                                if (isNotBlank(contentLine)) {
                                    currentSectionContent += contentLine;
                                }
                            }
                        }
                    }
                }
                if (event.isEndElement()) {
                    var endElement = event.asEndElement();
                    var name = endElement.getName().getLocalPart();
                    if (name.equals("p")) {
                        currentSections.add(new Section(List.of(currentSectionContent), emptyList()));
                        currentSectionContent = "";
                    }
                    if (name.equals("note")) {
                        notes.add(currentFootNote);
                        readingNote = false;
                        currentFootNote = "";
                        currentSectionContent += "[^^" + notes.size() + "] ";
                    }
                }

                if (event.isCharacters()) {
                    var rawLine = event.asCharacters().getData();
                    var cleaned = cleanRawText(rawLine);
                    if (isNotBlank(cleaned)) {
                        if (readingNote) {
                            currentFootNote += cleaned + " ";
                        } else {
                            currentSectionContent += cleaned + " ";
                        }

                    }
                }
            }

            if (!currentSections.isEmpty()) {
                chapters.add(new ParsedChapter(new ArrayList<>(currentSections), new ArrayList<>(notes)));
                currentSections.clear();
                notes.clear();
            }

            return new ParsedBook(chapters);
        } catch (IOException | XMLStreamException e) {
            throw new RuntimeException(e);
        }


    }

    private static String cleanRawText(String rawLine) {
        return strip(rawLine.replace(lineSeparator(), "")).replaceAll("\\s+", " ").trim();
    }
}
