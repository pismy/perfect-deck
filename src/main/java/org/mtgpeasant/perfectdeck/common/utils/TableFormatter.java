package org.mtgpeasant.perfectdeck.common.utils;

import com.google.common.base.Strings;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@Value
@Builder
@Getter
public class TableFormatter {

    @Builder.Default
    private final boolean padding = true;

    @Singular
    private final List<String> columns;

    @Singular
    private final List<List<?>> rows;

    public String render() {
        int[] widths = IntStream.range(0, columns.size()).map(this::width).toArray();
        String separator = separator(widths);

        StringBuilder table = new StringBuilder();

        // header
        table.append(separator);
        for (int col = 0; col < columns.size(); col++) {
            table.append('|');
            if (padding) {
                table.append(' ');
            }
            table.append(Strings.padEnd(columns.get(col), widths[col], ' '));
            if (padding) {
                table.append(' ');
            }
        }
        table.append("|\n");
        table.append(separator);
        // rows
        for (List<?> row : rows) {
            for (int col = 0; col < columns.size(); col++) {
                table.append('|');
                if (padding) {
                    table.append(' ');
                }
                table.append(Strings.padEnd(cell(row, col), widths[col], ' '));
                if (padding) {
                    table.append(' ');
                }
            }
            table.append("|\n");
        }
        table.append(separator);

        return table.toString();
    }

    private String separator(int[] widths) {
        StringBuilder separator = new StringBuilder();
        separator.append('+');
        for (int col = 0; col < columns.size(); col++) {
            separator.append(Strings.repeat("-", width(col) + (padding ? 2 : 0)));
            separator.append('+');
        }
        separator.append('\n');
        return separator.toString();
    }

    private int width(int col) {
        Optional<Integer> maxRowWidth = rows.stream().map(row -> cell(row, col).length()).max(Integer::compareTo);
        return Math.max(columns.get(col).length(), maxRowWidth.orElse(0));
    }

    private String cell(List<?> row, int col) {
        return row == null || col > rows.size() || row.get(col) == null ? "-" : String.valueOf(row.get(col));
    }
}

