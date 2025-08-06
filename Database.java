import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

public class Database {
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            boolean shouldExit = false;
            while (!shouldExit) {
                System.out.print(">> ");
                String command = scanner.nextLine();
                if (command.toUpperCase().startsWith("CREATE") && command.toUpperCase().contains("HAVING")) {
                    createTable(command);
                } else if (command.toUpperCase().startsWith("DROP") && command.toUpperCase().contains("TABLE")) {
                    dropTable(command);
                } else if (command.equalsIgnoreCase("SHOW TABLES")) {
                    showTables();
                } else if (command.toUpperCase().startsWith("INSERT IN TABLE") && command.toUpperCase().contains("VALUES")) {
                    insertRow(command);
                } else if (command.toUpperCase().startsWith("SELECT FROM TABLE")) {
                    selectData(command);
                } else if (command.toUpperCase().startsWith("UPDATE") && command.toUpperCase().contains("SET") && command.toUpperCase().contains("WHERE")) {
                    updateRow(command);
                } else if (command.toUpperCase().startsWith("DELETE FROM TABLE") && (command.toUpperCase().contains("HAVING") || command.toUpperCase().contains("SORT BY"))) {
                    deleteRow(command);
                } else if (command.toUpperCase().startsWith("HELP")) {
                    showHelp();
                } else if (command.equalsIgnoreCase("EXIT")) {
                    System.out.println("Exiting the database management system...");
                    shouldExit = true;
                } else {
                    System.out.println("Invalid command. Type HELP for available commands.");
                }
            }
        }
    }

    private static void createTable(String command) {
        int havingIndex = command.toUpperCase().indexOf("HAVING");
        if (havingIndex > 0) {
            String tableName = command.substring(7, havingIndex).trim();
            String columns = command.substring(havingIndex + 6).trim();
            
            if (tableName.isEmpty() || columns.isEmpty() || !isValidIdentifier(tableName)) {
                System.out.println("Invalid table name or columns.");
                return;
            }

            File tableFile = new File(tableName + ".txt");
            if (tableFile.exists()) {
                System.out.println("Table already exists.");
                return;
            }

            try (FileWriter writer = new FileWriter(tableFile)) {
                writer.write(columns.replace(",", "\t"));
                System.out.println("Table " + tableName + " created successfully.");
            } catch (IOException e) {
                System.out.println("Error creating table: " + e.getMessage());
            }
        } else {
            System.out.println("Syntax error: Missing HAVING clause.");
        }
    }

    private static void dropTable(String command) {
        int tableKeywordIndex = command.toUpperCase().indexOf("TABLE");
        if (tableKeywordIndex > 0) {
            String tableName = command.substring(5, tableKeywordIndex).trim();
            if (tableName.isEmpty() || !isValidIdentifier(tableName)) {
                System.out.println("Invalid table name.");
                return;
            }

            File tableFile = new File(tableName + ".txt");
            if (!tableFile.exists()) {
                System.out.println("Table does not exist.");
                return;
            }

            if (tableFile.delete()) {
                System.out.println("Table " + tableName + " deleted successfully.");
            } else {
                System.out.println("Failed to delete table. Please try again.");
            }
        } else {
            System.out.println("Syntax error: Missing TABLE keyword.");
        }
    }

    private static void showTables() {
        File currentDir = new File(".");
        File[] files = currentDir.listFiles((dir, name) -> name.endsWith(".txt"));
        
        if (files == null || files.length == 0) {
            System.out.println("No tables available.");
            return;
        }

        System.out.println("Available tables:");
        for (File file : files) {
            System.out.println(file.getName().replace(".txt", ""));
        }
    }

    private static void insertRow(String command) {
        int tableKeywordIndex = command.toUpperCase().indexOf("TABLE");
        int valuesKeywordIndex = command.toUpperCase().indexOf("VALUES");
        
        if (tableKeywordIndex == -1 || valuesKeywordIndex == -1) {
            System.out.println("Syntax error: Missing TABLE or VALUES keyword.");
            return;
        }

        String tableName = command.substring(tableKeywordIndex + 5, valuesKeywordIndex).trim();
        if (tableName.isEmpty() || !isValidIdentifier(tableName)) {
            System.out.println("Invalid table name.");
            return;
        }

        String valuesString = command.substring(valuesKeywordIndex + 6)
            .replace("(", "")
            .replace(")", "")
            .trim();
        
        if (valuesString.isEmpty()) {
            System.out.println("Missing values.");
            return;
        }

        String[] values = valuesString.split(",");
        for (int i = 0; i < values.length; i++) {
            values[i] = values[i].trim();
            if (!values[i].startsWith("\"") || !values[i].endsWith("\"")) {
                System.out.println("Values must be enclosed in double quotes.");
                return;
            }
        }

        File tableFile = new File(tableName + ".txt");
        if (!tableFile.exists()) {
            System.out.println("Table does not exist.");
            return;
        }

        try (FileWriter writer = new FileWriter(tableFile, true)) {
            writer.write("\n" + String.join("\t", values));
            System.out.println("Data inserted into " + tableName + " successfully.");
        } catch (IOException e) {
            System.out.println("Error inserting data: " + e.getMessage());
        }
    }

    private static void selectData(String command) {
        String tableName = extractTableName(command, "SELECT FROM TABLE");
        if (tableName == null) return;

        File tableFile = new File(tableName + ".txt");
        if (!tableFile.exists()) {
            System.out.println("Table does not exist.");
            return;
        }

        try {
            TableData tableData = readTableData(tableFile);
            if (tableData.isEmpty()) {
                System.out.println("Table is empty.");
                return;
            }

            if (command.toUpperCase().contains("HAVING") && command.contains("=")) {
                String filterColumn = command.substring(command.toUpperCase().indexOf("HAVING") + 6, command.indexOf("=")).trim();
                String filterValue = command.substring(command.indexOf("=") + 1).trim();
                
                if (command.toUpperCase().contains("SORT BY")) {
                    filterValue = filterValue.substring(0, filterValue.toUpperCase().indexOf("SORT BY")).trim();
                    String sortColumn = command.substring(command.toUpperCase().indexOf("SORT BY") + 7).trim();
                    tableData.filterAndSort(filterColumn, filterValue, sortColumn);
                } else {
                    tableData.filterRows(filterColumn, filterValue);
                }
            } else if (command.toUpperCase().contains("SORT BY")) {
                String sortColumn = command.substring(command.toUpperCase().indexOf("SORT BY") + 7).trim();
                tableData.sortRows(sortColumn);
            }

            tableData.print(tableName);
        } catch (IOException e) {
            System.out.println("Error reading table: " + e.getMessage());
        }
    }

    private static void updateRow(String command) {
        String tableName = extractTableName(command, "UPDATE");
        if (tableName == null) return;

        File tableFile = new File(tableName + ".txt");
        if (!tableFile.exists()) {
            System.out.println("Table does not exist.");
            return;
        }

        int setIndex = command.toUpperCase().indexOf("SET");
        int whereIndex = command.toUpperCase().indexOf("WHERE");
        
        if (setIndex == -1 || whereIndex == -1) {
            System.out.println("Syntax error: Missing SET or WHERE clause.");
            return;
        }

        String updateColumn = command.substring(setIndex + 3, command.indexOf("=", setIndex)).trim();
        String newValue = command.substring(command.indexOf("=", setIndex) + 1, whereIndex).trim();
        String filterColumn = command.substring(whereIndex + 5, command.lastIndexOf("=")).trim();
        String filterValue = command.substring(command.lastIndexOf("=") + 1).trim();

        try {
            TableData tableData = readTableData(tableFile);
            int updatedCount = tableData.updateRows(updateColumn, newValue, filterColumn, filterValue);
            
            try (FileWriter writer = new FileWriter(tableFile)) {
                writer.write(tableData.serialize());
            }
            
            System.out.println("Updated " + updatedCount + " rows successfully.");
        } catch (IOException e) {
            System.out.println("Error updating table: " + e.getMessage());
        }
    }

    private static void deleteRow(String command) {
        String tableName = extractTableName(command, "DELETE FROM TABLE");
        if (tableName == null) return;

        File tableFile = new File(tableName + ".txt");
        if (!tableFile.exists()) {
            System.out.println("Table does not exist.");
            return;
        }

        if (!command.toUpperCase().contains("HAVING") || !command.contains("=")) {
            System.out.println("Syntax error: Missing HAVING clause or condition.");
            return;
        }

        String filterColumn = command.substring(command.toUpperCase().indexOf("HAVING") + 6, command.indexOf("=")).trim();
        String filterValue = command.substring(command.indexOf("=") + 1).trim();

        try {
            TableData tableData = readTableData(tableFile);
            int deletedCount = tableData.deleteRows(filterColumn, filterValue);
            
            try (FileWriter writer = new FileWriter(tableFile)) {
                writer.write(tableData.serialize());
            }
            
            System.out.println("Deleted " + deletedCount + " rows successfully.");
        } catch (IOException e) {
            System.out.println("Error deleting rows: " + e.getMessage());
        }
    }

    private static void showHelp() {
        System.out.println("================================ HELP ================================");
        System.out.println("CREATE <table> HAVING <col1>,<col2>,...  - Create new table");
        System.out.println("DROP <table> TABLE                        - Delete table");
        System.out.println("SHOW TABLES                               - List all tables");
        System.out.println("INSERT IN TABLE <table> VALUES (v1,v2,..) - Insert data");
        System.out.println("SELECT FROM TABLE <table> [HAVING col=val] [SORT BY col] - Query data");
        System.out.println("UPDATE <table> SET col=val WHERE col=val  - Modify data");
        System.out.println("DELETE FROM TABLE <table> HAVING col=val  - Remove data");
        System.out.println("EXIT                                     - Exit program");
        System.out.println("=========================================================================");
    }

    private static boolean isValidIdentifier(String name) {
        if (name == null || name.isEmpty()) return false;
        if (!Character.isJavaIdentifierStart(name.charAt(0))) return false;
        for (int i = 1; i < name.length(); i++) {
            if (!Character.isJavaIdentifierPart(name.charAt(i))) return false;
        }
        return true;
    }

    private static String extractTableName(String command, String prefix) {
        int startIndex = command.toUpperCase().indexOf(prefix) + prefix.length();
        int endIndex = command.length();
        
        if (command.toUpperCase().contains("HAVING")) {
            endIndex = command.toUpperCase().indexOf("HAVING");
        } else if (command.toUpperCase().contains("SORT BY")) {
            endIndex = command.toUpperCase().indexOf("SORT BY");
        } else if (command.toUpperCase().contains("SET")) {
            endIndex = command.toUpperCase().indexOf("SET");
        } else if (command.toUpperCase().contains("WHERE")) {
            endIndex = command.toUpperCase().indexOf("WHERE");
        }
        
        String tableName = command.substring(startIndex, endIndex).trim();
        if (tableName.isEmpty() || !isValidIdentifier(tableName)) {
            System.out.println("Invalid table name.");
            return null;
        }
        return tableName;
    }

    private static TableData readTableData(File tableFile) throws IOException {
        TableData tableData = new TableData();
        try (BufferedReader reader = new BufferedReader(new FileReader(tableFile))) {
            String headerLine = reader.readLine();
            if (headerLine != null) {
                tableData.setHeaders(headerLine.split("\t"));
            }
            
            String line;
            while ((line = reader.readLine()) != null) {
                tableData.addRow(line.split("\t"));
            }
        }
        return tableData;
    }

    static class TableData {
        private String[] headers;
        private List<String[]> rows = new ArrayList<>();

        public void setHeaders(String[] headers) {
            this.headers = headers;
        }

        public void addRow(String[] row) {
            rows.add(row);
        }

        public boolean isEmpty() {
            return headers == null || headers.length == 0;
        }

        public void filterRows(String column, String value) {
            int colIndex = getColumnIndex(column);
            if (colIndex == -1) return;
            
            rows.removeIf(row -> !row[colIndex].trim().equals(value.trim()));
        }

        public void sortRows(String column) {
            int colIndex = getColumnIndex(column);
            if (colIndex == -1) return;
            
            Collections.sort(rows, Comparator.comparing(row -> row[colIndex].trim()));
        }

        public void filterAndSort(String filterCol, String filterVal, String sortCol) {
            int filterIndex = getColumnIndex(filterCol);
            int sortIndex = getColumnIndex(sortCol);
            
            if (filterIndex == -1 || sortIndex == -1) return;
            
            rows.removeIf(row -> !row[filterIndex].trim().equals(filterVal.trim()));
            Collections.sort(rows, Comparator.comparing(row -> row[sortIndex].trim()));
        }

        public int updateRows(String updateCol, String newValue, String filterCol, String filterVal) {
            int updateIndex = getColumnIndex(updateCol);
            int filterIndex = getColumnIndex(filterCol);
            
            if (updateIndex == -1 || filterIndex == -1) return 0;
            
            int count = 0;
            for (String[] row : rows) {
                if (row[filterIndex].trim().equals(filterVal.trim())) {
                    row[updateIndex] = newValue;
                    count++;
                }
            }
            return count;
        }

        public int deleteRows(String column, String value) {
            int colIndex = getColumnIndex(column);
            if (colIndex == -1) return 0;
            
            int initialSize = rows.size();
            rows.removeIf(row -> row[colIndex].trim().equals(value.trim()));
            return initialSize - rows.size();
        }

        public void print(String tableName) {
            System.out.println("\nTable: " + tableName);
            System.out.println("=".repeat(50));
            
            // Print headers
            for (String header : headers) {
                System.out.printf("%-15s", header);
            }
            System.out.println();
            
            // Print rows
            for (String[] row : rows) {
                for (String cell : row) {
                    System.out.printf("%-15s", cell.replace("\"", ""));
                }
                System.out.println();
            }
        }

        public String serialize() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.join("\t", headers));
            
            for (String[] row : rows) {
                sb.append("\n").append(String.join("\t", row));
            }
            
            return sb.toString();
        }

        private int getColumnIndex(String columnName) {
            for (int i = 0; i < headers.length; i++) {
                if (headers[i].equals(columnName)) {
                    return i;
                }
            }
            System.out.println("Column '" + columnName + "' not found");
            return -1;
        }
    }
}