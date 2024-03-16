package be.sddevelopment.conversions;

import java.util.ArrayList;
import java.util.List;

import static java.lang.System.lineSeparator;

public record ParsedChapter(List<Section> sections, List<String> footnotes) {
    public List<String> toMarkdown(int number) {
        List<String> lines = new ArrayList<>();
        lines.add("# Chapter " + number);
        lines.add(lineSeparator());

        sections.forEach(section -> {
            lines.addAll(section.lines());
            lines.add(lineSeparator());
            lines.addAll(section.references());
        });
        lines.add(lineSeparator());
        footnotes.stream()
                .map(footnote -> "[^%d]: %s".formatted(footnotes.indexOf(footnote), footnote))
                .map(footnote -> footnote + lineSeparator())
                .forEach(lines::add);
        return lines;
    }
}
