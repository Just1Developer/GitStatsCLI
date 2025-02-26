package net.justonedev;

import net.justonedev.statswrapper.FileChanges;
import net.justonedev.statswrapper.RepositoryStats;
import net.justonedev.statswrapper.UserChanges;
import net.justonedev.statswrapper.UserStats;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class ExcelExport {
    public static void writeExcel(String fileName, List<RepositoryStats> repositories) {
        try (Workbook workbook = new XSSFWorkbook()) {
            for (RepositoryStats repo : repositories) {
                Sheet sheet = workbook.createSheet(repo.name());

                var commits = UserStats.allChangesPlusTotal(repo.fullBranchStats().getAllChangesSortedByCommit());
                addTable(sheet, 0, true, "Per-user Commits", List.of("Author", "Commits", "Average Additions", "Average Deletions"),
                        commits.stream().map(UserChanges::getAuthor).toList(),
                        commits.stream().map(c -> "%d".formatted(c.getCommits())).toList(),
                        commits.stream().map(c -> "%.2f".formatted((double) c.getAdditions() / c.getCommits())).toList(),
                        commits.stream().map(c -> "%.2f".formatted((double) c.getDeletions() / c.getCommits())).toList()
                );

                var sortedChanges = UserStats.allChangesPlusTotal(repo.fullBranchStats().getAllChangesSorted());
                addTable(sheet, 5, true, "Additions / Deletions - All Branches", List.of("Author", "Additions", "Deletions"),
                        sortedChanges.stream().map(UserChanges::getAuthor).toList(),
                        sortedChanges.stream().map(c -> "%d".formatted(c.getAdditions())).toList(),
                        sortedChanges.stream().map(c -> "%d".formatted(c.getDeletions())).toList()
                );

                var sortedChangesMain = UserStats.allChangesPlusTotal(repo.mainBranchStats().getAllChangesSorted());
                addTable(sheet, 9, true, "Additions / Deletions - Main Branch" + (repo.name().equals(Main.TITLE_ALL_PROJECTS) ? "es" : ""), List.of("Author", "Additions", "Deletions"),
                        sortedChangesMain.stream().map(UserChanges::getAuthor).toList(),
                        sortedChangesMain.stream().map(c -> "%d".formatted(c.getAdditions())).toList(),
                        sortedChangesMain.stream().map(c -> "%d".formatted(c.getDeletions())).toList()
                );

                var codePossession = UserStats.allChangesPlusTotal(repo.contributionStats().getAllChangesSorted());
                var totalCodePossession = codePossession.getLast();
                addTable(sheet, 13, true, "Final Contributions - With Comments - Git Blame", List.of("Author", "Lines written", "Percent (%)"),
                        codePossession.stream().map(UserChanges::getAuthor).toList(),
                        codePossession.stream().map(c -> "%d".formatted(c.getAdditions())).toList(),
                        codePossession.stream().map(c -> "%f".formatted(Math.round(((double) c.getAdditions() / totalCodePossession.getAdditions()) * 10000d) / 100d)).toList()
                );

                addTable(sheet, 17, true, "Final Contributions - No Comments - Git Blame", List.of("Author", "Lines written", "Percent (%)"),
                        codePossession.stream().map(UserChanges::getAuthor).toList(),
                        codePossession.stream().map(c -> "%d".formatted(c.getDeletions())).toList(),
                        codePossession.stream().map(c -> "%f".formatted(Math.round(((double) c.getDeletions() / totalCodePossession.getDeletions()) * 10000d) / 100d)).toList()
                );

                var commentPossession = UserStats.allChangesPlusTotal(repo.contributionsCommentsStats().getAllChangesSortedBy((a, b) -> b.getAdditions() - a.getAdditions()));
                final var totalCommentPossessionAdd = codePossession.getLast();
                addTable(sheet, 21, true, "Comments and Javadoc - Git Blame", List.of("Author", "Lines written", "Percent (%)"),
                        commentPossession.stream().map(UserChanges::getAuthor).toList(),
                        commentPossession.stream().map(c -> "%d".formatted(c.getAdditions())).toList(),
                        commentPossession.stream().map(c -> "%f".formatted(Math.round(((double) c.getAdditions() / totalCommentPossessionAdd.getAdditions()) * 10000d) / 100d)).toList()
                );

                commentPossession = UserStats.allChangesPlusTotal(repo.contributionsCommentsStats().getAllChangesSortedBy((a, b) -> b.getDeletions() - a.getDeletions()));
                final var totalCommentPossessionDel = codePossession.getLast();
                addTable(sheet, 25, true, "Empty Lines - Git Blame", List.of("Author", "Empty Lines written", "Percent (%)"),
                        commentPossession.stream().map(UserChanges::getAuthor).toList(),
                        commentPossession.stream().map(c -> "%d".formatted(c.getDeletions())).toList(),
                        commentPossession.stream().map(c -> "%f".formatted(Math.round(((double) c.getDeletions() / totalCommentPossessionDel.getDeletions()) * 10000d) / 100d)).toList()
                );

                var fileStatsLines = repo.fileStats().getAllChangesSortedBy((a, b) -> Math.toIntExact(b.getLineCount() - a.getLineCount()), a -> a.getLineCount() > 0);
                addTable(sheet, 29, "Files by Line Count", List.of("File", "All Lines", "Lines of Code"),
                        fileStatsLines.stream().map(FileChanges::getFileName).toList(),
                        fileStatsLines.stream().map(FileChanges::getLineCount).filter(d -> d > 0).map("%d"::formatted).toList(),
                        fileStatsLines.stream().map(FileChanges::getLineCountLOC).filter(d -> d > 0).map("%d"::formatted).toList()
                );

                var fileStatsChanges = repo.fileStats().getAllChangesSorted();
                addTable(sheet, 33, "Changes per File", List.of("File", "Total Additions", "Total Deletions", "Status"),
                        fileStatsChanges.stream().map(FileChanges::getFileName).toList(),
                        fileStatsChanges.stream().map(c -> "%d".formatted(c.getAdditions())).toList(),
                        fileStatsChanges.stream().map(c -> "%d".formatted(c.getDeletions())).toList(),
                        fileStatsChanges.stream().map(FileChanges::getLineCount).map(d -> d == 0 ? "gone" : "exists").toList()
                        );

            }
            try (FileOutputStream fileOut = new FileOutputStream(fileName + (fileName.endsWith(".xlsx") ? "" : ".xlsx"))) {
                workbook.write(fileOut);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error writing Excel file", e);
        }
    }


    @SafeVarargs
    private static void addTable(Sheet sheet,
                                 int position,
                                 String title,
                                 List<String> columnTitles,
                                 List<String>... columns) {
        addTable(sheet, position, false, title, columnTitles, columns);
    }

    @SafeVarargs
    private static void addTable(Sheet sheet,
                                 int startColumn,
                                 boolean lastCellBold,
                                 String title,
                                 List<String> columnTitles,
                                 List<String>... columns) {
        Workbook workbook = sheet.getWorkbook();

        Font boldFont = workbook.createFont();
        boldFont.setBold(true);

        // --- Create a CellStyle for numeric cells with exactly two decimals ---
        CellStyle numericStyleDouble = workbook.createCellStyle();
        CellStyle numericStyleInt = workbook.createCellStyle();
        DataFormat dataFormat = workbook.createDataFormat();
        numericStyleDouble.setDataFormat(dataFormat.getFormat("0.00"));
        numericStyleInt.setDataFormat(dataFormat.getFormat("0"));

        CellStyle numericStyleDoubleBold = workbook.createCellStyle();
        CellStyle numericStyleIntBold = workbook.createCellStyle();
        numericStyleDoubleBold.setDataFormat(dataFormat.getFormat("0.00"));
        numericStyleDoubleBold.setFont(boldFont);
        numericStyleIntBold.setDataFormat(dataFormat.getFormat("0"));
        numericStyleIntBold.setFont(boldFont);

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle boldStyle = workbook.createCellStyle();
        boldStyle.setFont(headerFont);

        CellStyle goneStyle = workbook.createCellStyle();
        goneStyle.setAlignment(HorizontalAlignment.RIGHT);
        XSSFColor redColor = new XSSFColor(new java.awt.Color(0xF14646), new DefaultIndexedColorMap());
        goneStyle.setFillForegroundColor(redColor); // Hex: #E63030, red
        goneStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle existsStyle = workbook.createCellStyle();
        existsStyle.setAlignment(HorizontalAlignment.RIGHT);
        XSSFColor greenColor = new XSSFColor(new java.awt.Color(0x5CF345), new DefaultIndexedColorMap());
        existsStyle.setFillForegroundColor(greenColor); // Hex: #3BD424, green
        existsStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeight((short) (titleFont.getFontHeight() * 1.3));
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);

        // Number of columns in the table
        int numColumns = columnTitles.size();

        // --- Title Row (row 0) ---
        Row titleRow = getOrCreateRow(sheet, 0);
        Cell titleCell = titleRow.createCell(startColumn);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(titleStyle);
        // Merge across all columns if more than one column
        if (numColumns > 1) {
            sheet.addMergedRegion(new CellRangeAddress(0, 0, startColumn, startColumn + numColumns - 1));
        }

        // --- Header Row (row 1) with bold style ---
        Row headerRow = getOrCreateRow(sheet, 1);

        for (int i = 0; i < numColumns; i++) {
            Cell headerCell = headerRow.createCell(startColumn + i);
            headerCell.setCellValue(columnTitles.get(i));
            headerCell.setCellStyle(headerStyle);
        }

        // --- Data Rows (starting at row 2) ---
        int dataRows = (columns.length > 0) ? columns[0].size() : 0;
        for (int rowIndex = 0; rowIndex < dataRows; rowIndex++) {
            Row dataRow = getOrCreateRow(sheet, 2 + rowIndex);

            for (int colIndex = 0; colIndex < numColumns; colIndex++) {
                Cell cell = dataRow.createCell(startColumn + colIndex);
                boolean bold = lastCellBold && rowIndex == dataRows - 1;
                String cellValue = columns[colIndex].get(rowIndex);

                // Attempt to parse the cell value as a double
                Double parsed = tryParseDouble(cellValue);
                if (parsed != null) {
                    if (cellValue.matches("^-?\\d+$")) {
                        cell.setCellValue(Integer.parseInt(cellValue));
                        cell.setCellStyle(bold ? numericStyleIntBold : numericStyleInt);
                    } else {
                        cell.setCellValue(parsed);
                        cell.setCellStyle(bold ? numericStyleDoubleBold : numericStyleDouble);
                    }
                } else {
                    // Otherwise, store as text
                    cell.setCellValue(cellValue);
                    if (bold) cell.setCellStyle(boldStyle);
                    else if (cellValue.equals("gone")) {
                        cell.setCellStyle(goneStyle);
                    } else if (cellValue.equals("exists")) {
                        cell.setCellStyle(existsStyle);
                    }
                }
            }
        }

        // --- Auto-size all columns in this table + 20% extra padding ---
        for (int i = 0; i < numColumns; i++) {
            int colIndex = startColumn + i;
            sheet.autoSizeColumn(colIndex);

            // Increase the width by 20% (capped at Excels max width)
            int currentWidth = sheet.getColumnWidth(colIndex);
            int newWidth = (int) (currentWidth * 1.2);
            if (newWidth > 255 * 256) {
                newWidth = 255 * 256; // Excels max column width
            }
            sheet.setColumnWidth(colIndex, newWidth);
        }

        // --- Adjust merged title width ---
        // Compute the total width of the merged columns
        int mergedWidth = 0;
        for (int i = startColumn; i < startColumn + numColumns; i++) {
            mergedWidth += sheet.getColumnWidth(i);
        }
        // Estimate required width: roughly 1.2 * 256 units per character (adjust as needed)
        int requiredWidth = (int) (title.length() * 1.2 * 256);
        if (requiredWidth > mergedWidth) {
            int extraPerColumn = (requiredWidth - mergedWidth) / numColumns;
            for (int i = startColumn; i < startColumn + numColumns; i++) {
                int newColWidth = sheet.getColumnWidth(i) + extraPerColumn;
                if (newColWidth > 255 * 256) {
                    newColWidth = 255 * 256;
                }
                sheet.setColumnWidth(i, newColWidth);
            }
        }
    }

    /**
     * Returns the existing row if it exists, otherwise creates a new one.
     */
    private static Row getOrCreateRow(Sheet sheet, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            row = sheet.createRow(rowIndex);
        }
        return row;
    }

    /**
     * Tries to parse a string as a double, normalizing commas to periods first.
     * Returns null if parsing fails.
     */
    private static Double tryParseDouble(String value) {
        if (value == null) {
            return null;
        }
        // Replace commas with periods in case of localized decimals (e.g. "1,23")
        String normalized = value.trim().replace(',', '.');
        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
