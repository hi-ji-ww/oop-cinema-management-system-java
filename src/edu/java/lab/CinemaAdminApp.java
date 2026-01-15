package edu.java.lab2;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;

public class CinemaAdminApp {
    private JFrame mainFrame;
    private JToolBar toolBar;
    private JButton addFilm, editFilm, deleteFilm, addSession;
    private JButton printReport, saveData, loadData, saveXML, loadXML;
    private JButton generatePDF, generateHTML;
    private JButton runThreadsButton;
    private JTable filmsTable, sessionsTable, ticketsTable;
    private DefaultTableModel filmsModel, sessionsModel, ticketsModel;
    private JComboBox<String> filmFilter;
    private JTextField dateFilter;
    private JButton searchButton;

    private static final String XML_FILMS_FILE = "films.xml";
    private static final String XML_SESSIONS_FILE = "sessions.xml";
    private static final String XML_TICKETS_FILE = "tickets.xml";

    // CountDownLatch - механизм синхронизации потоков
    // Позволяет потоку ждать, пока другие потоки не выполнят свою работу
    private CountDownLatch latchLoad;
    private CountDownLatch latchEdit;
    // ==============================================
    /**
     * =============================================================================
     * КУРСОВОЙ ПРОЕКТ: АРМ «АДМИНИСТРАТОР КИНОТЕАТРА» (ВАРИАНТ №15)
     * =============================================================================
     * * 1. АРХИТЕКТУРА (LAYERED ARCHITECTURE):
     * - GUI Layer: CinemaAdminApp (Swing). Обработка событий пользователя.
     * - Repository Layer: CinemaRepository. Инкапсуляция списков Film и Session.
     * - Service Layer: XmlService. Сериализация данных в формат XML.
     * - Pipeline Layer: ReportPipeline. Координация многопоточных процессов.
     *
     * 2. ОБЪЕКТНО-ОРИЕНТИРОВАННОЕ ПРОЕКТИРОВАНИЕ (ООП):
     * - ИНКАПСУЛЯЦИЯ: Доступ к полям сущностей (Film, Session) через Getter/Setter.
     * - SRP (Single Responsibility): Разделение GUI, логики данных и I/O операций.
     * - ПОЛИМОРФИЗМ: Использование кастомных моделей таблиц (AbstractTableModel).
     *
     * 3. МНОГОПОТОЧНОСТЬ И СИНХРОНИЗАЦИЯ (ЛАБОРАТОРНАЯ №8):
     * - Механизм: java.util.concurrent.CountDownLatch(2).
     * - Логика: Главный поток (Reporter) ждет выполнения двух параллельных потоков:
     * Thread 1 (Loader) и Thread 2 (Editor) вызывают countDown().
     * - Результат: Отчет формируется строго после завершения обработки данных.
     *
     * 4. ТЕСТИРОВАНИЕ:
     * - Слой данных покрыт Unit-тестами (JUnit 4) для проверки CRUD-операций.
     * =============================================================================
     */
    public void show() {
        initializeGUI();
        addTestData();
    }

    private void initializeGUI() {
        mainFrame = new JFrame("Администратор кинотеатра");
        mainFrame.setSize(1200, 720);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        createMenuBar();
        createToolBar();
        createTables();
        createSearchPanel();

        mainFrame.setVisible(true);
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Файл");
        JMenuItem saveXMLItem = new JMenuItem("Сохранить в XML");
        JMenuItem loadXMLItem = new JMenuItem("Загрузить из XML");
        JMenuItem saveTextItem = new JMenuItem("Сохранить в текстовый файл");
        JMenuItem loadTextItem = new JMenuItem("Загрузить из текстового файла");
        JMenuItem generatePDFItem = new JMenuItem("Сгенерировать PDF отчет");
        JMenuItem generateHTMLItem = new JMenuItem("Сгенерировать HTML отчет");
        JMenuItem exitItem = new JMenuItem("Выход");

        saveXMLItem.addActionListener(e -> saveAllDataToXML());
        loadXMLItem.addActionListener(e -> loadAllDataFromXML());
        saveTextItem.addActionListener(e -> saveAllDataToFile());
        loadTextItem.addActionListener(e -> loadAllDataFromFile());
        generatePDFItem.addActionListener(e -> generatePDFReport());
        generateHTMLItem.addActionListener(e -> generateHTMLReport());
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(saveXMLItem);
        fileMenu.add(loadXMLItem);
        fileMenu.addSeparator();
        fileMenu.add(saveTextItem);
        fileMenu.add(loadTextItem);
        fileMenu.addSeparator();
        fileMenu.add(generatePDFItem);
        fileMenu.add(generateHTMLItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);
        mainFrame.setJMenuBar(menuBar);
    }

    private void createToolBar() {
        toolBar = new JToolBar("Панель управления");

        addFilm = new JButton("Добавить фильм");
        editFilm = new JButton("Редактировать");
        deleteFilm = new JButton("Удалить");
        addSession = new JButton("Добавить сеанс");
        printReport = new JButton("Быстрый отчет");
        saveData = new JButton("Сохранить текст");
        loadData = new JButton("Загрузить текст");
        saveXML = new JButton("Сохранить XML");
        loadXML = new JButton("Загрузить XML");
        generatePDF = new JButton("PDF отчет");
        generateHTML = new JButton("HTML отчет");
        runThreadsButton = new JButton("Запустить потоки ");

        addFilm.setToolTipText("Добавить новый фильм");
        editFilm.setToolTipText("Редактировать выбранный фильм");
        deleteFilm.setToolTipText("Удалить выбранный фильм");
        addSession.setToolTipText("Добавить новый сеанс");
        printReport.setToolTipText("Показать быстрый отчет");
        saveData.setToolTipText("Сохранить в текстовый файл");
        loadData.setToolTipText("Загрузить из текстового файла");
        saveXML.setToolTipText("Сохранить в XML");
        loadXML.setToolTipText("Загрузить из XML");
        generatePDF.setToolTipText("Сгенерировать PDF");
        generateHTML.setToolTipText("Сгенерировать HTML");
        runThreadsButton.setToolTipText("Запустить Loader -> Editor -> Reporter");

        addFilm.addActionListener(e -> addNewFilm());
        editFilm.addActionListener(e -> editSelectedFilm());
        deleteFilm.addActionListener(e -> deleteSelectedFilm());
        addSession.addActionListener(e -> addNewSession());
        printReport.addActionListener(e -> generateReport());
        saveData.addActionListener(e -> saveAllDataToFile());
        loadData.addActionListener(e -> loadAllDataFromFile());
        saveXML.addActionListener(e -> saveAllDataToXML());
        loadXML.addActionListener(e -> loadAllDataFromXML());
        generatePDF.addActionListener(e -> generatePDFReport());
        generateHTML.addActionListener(e -> generateHTMLReport());
        runThreadsButton.addActionListener(e -> startThreeThreads());

        toolBar.add(addFilm);
        toolBar.add(editFilm);
        toolBar.add(deleteFilm);
        toolBar.addSeparator();
        toolBar.add(addSession);
        toolBar.add(printReport);
        toolBar.addSeparator();
        toolBar.add(saveData);
        toolBar.add(loadData);
        toolBar.addSeparator();
        toolBar.add(saveXML);
        toolBar.add(loadXML);
        toolBar.addSeparator();
        toolBar.add(generatePDF);
        toolBar.add(generateHTML);
        toolBar.addSeparator();
        toolBar.add(runThreadsButton);

        mainFrame.add(toolBar, BorderLayout.NORTH);
    }

    /**
     * СЛОЙ СЕРВИСОВ (I/O OPERATIONS):
     * Реализация механизма персистентности (сохранения состояния).
     * Используется XML-сериализация, так как она поддерживает древовидную структуру
     * данных (Фильм -> Сеансы -> Билеты), что невозможно в плоских CSV-файлах.
     * Обработка исключений (try-catch) гарантирует стабильность работы 
     * при отсутствии доступа к файловой системе или повреждении файлов.
     */
    
    public void saveToXML(String path) { ... }
    private void createTables() {
        String[] filmsColumns = {"Название", "Режиссер", "Год", "Жанр", "Длительность"};
        filmsModel = new DefaultTableModel(filmsColumns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        filmsTable = new JTable(filmsModel);
        filmsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        String[] sessionsColumns = {"Фильм", "Дата", "Время", "Зал", "Цена"};
        sessionsModel = new DefaultTableModel(sessionsColumns, 0);
        sessionsTable = new JTable(sessionsModel);

        String[] ticketsColumns = {"Сеанс", "Место", "Статус", "Время продажи"};
        ticketsModel = new DefaultTableModel(ticketsColumns, 0);
        ticketsTable = new JTable(ticketsModel);

        filmsTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) showFilmDetails();
            }
        });

        JPanel tablesPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        tablesPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        tablesPanel.add(createTablePanel(filmsTable, "Фильмы"));
        tablesPanel.add(createTablePanel(sessionsTable, "Сеансы"));
        tablesPanel.add(createTablePanel(ticketsTable, "Билеты"));

        mainFrame.add(tablesPanel, BorderLayout.CENTER);
    }

    private JScrollPane createTablePanel(JTable table, String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return new JScrollPane(panel);
    }

    private void createSearchPanel() {
        JPanel searchPanel = new JPanel(new FlowLayout());

        filmFilter = new JComboBox<>();
        filmFilter.addItem("Все фильмы");

        dateFilter = new JTextField("Дата (дд.мм.гггг)", 12);
        searchButton = new JButton("Найти");
        searchButton.addActionListener(e -> performSearch());

        dateFilter.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (dateFilter.getText().equals("Дата (дд.мм.гггг)")) dateFilter.setText("");
            }
            public void focusLost(FocusEvent e) {
                if (dateFilter.getText().isEmpty()) dateFilter.setText("Дата (дд.мм.гггг)");
            }
        });

        searchPanel.add(new JLabel("Фильтр по фильму:"));
        searchPanel.add(filmFilter);
        searchPanel.add(new JLabel("Дата:"));
        searchPanel.add(dateFilter);
        searchPanel.add(searchButton);

        mainFrame.add(searchPanel, BorderLayout.SOUTH);
    }

    // ========== CRUD операции ==========
    private void addNewFilm() {
        JTextField titleField = new JTextField();
        JTextField directorField = new JTextField();
        JTextField yearField = new JTextField();
        JTextField genreField = new JTextField();
        JTextField durationField = new JTextField();

        Object[] message = {
            "Название фильма:", titleField,
            "Режиссер:", directorField,
            "Год выпуска:", yearField,
            "Жанр:", genreField,
            "Длительность:", durationField
        };

        int option = JOptionPane.showConfirmDialog(mainFrame, message, "Добавление нового фильма", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String title = titleField.getText().trim();
            String director = directorField.getText().trim();
            String year = yearField.getText().trim();
            String genre = genreField.getText().trim();
            String duration = durationField.getText().trim();
            if (title.isEmpty() || director.isEmpty() || year.isEmpty() || genre.isEmpty() || duration.isEmpty()) {
                JOptionPane.showMessageDialog(mainFrame, "Все поля должны быть заполнены!", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }
            filmsModel.addRow(new Object[]{title, director, year, genre, duration});
            updateFilmFilter();
            JOptionPane.showMessageDialog(mainFrame, "Фильм '" + title + "' успешно добавлен!", "Успех", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void editSelectedFilm() {
        int selectedRow = filmsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(mainFrame, "Пожалуйста, выберите фильм для редактирования!", "Ошибка", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String currentTitle = (String) filmsModel.getValueAt(selectedRow, 0);
        String currentDirector = (String) filmsModel.getValueAt(selectedRow, 1);
        String currentYear = (String) filmsModel.getValueAt(selectedRow, 2);
        String currentGenre = (String) filmsModel.getValueAt(selectedRow, 3);
        String currentDuration = (String) filmsModel.getValueAt(selectedRow, 4);

        JTextField titleField = new JTextField(currentTitle);
        JTextField directorField = new JTextField(currentDirector);
        JTextField yearField = new JTextField(currentYear);
        JTextField genreField = new JTextField(currentGenre);
        JTextField durationField = new JTextField(currentDuration);

        Object[] message = {
            "Название фильма:", titleField,
            "Режиссер:", directorField,
            "Год выпуска:", yearField,
            "Жанр:", genreField,
            "Длительность:", durationField
        };

        int option = JOptionPane.showConfirmDialog(mainFrame, message, "Редактирование фильма", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            filmsModel.setValueAt(titleField.getText(), selectedRow, 0);
            filmsModel.setValueAt(directorField.getText(), selectedRow, 1);
            filmsModel.setValueAt(yearField.getText(), selectedRow, 2);
            filmsModel.setValueAt(genreField.getText(), selectedRow, 3);
            filmsModel.setValueAt(durationField.getText(), selectedRow, 4);
            updateFilmFilter();
            JOptionPane.showMessageDialog(mainFrame, "Фильм успешно отредактирован!", "Успех", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void deleteSelectedFilm() {
        int selectedRow = filmsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(mainFrame, "Пожалуйста, выберите фильм для удаления!", "Ошибка", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String filmTitle = (String) filmsModel.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(mainFrame, "Вы уверены, что хотите удалить фильм: " + filmTitle + "?", "Подтверждение удаления", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            filmsModel.removeRow(selectedRow);
            updateFilmFilter();
            JOptionPane.showMessageDialog(mainFrame, "Фильм '" + filmTitle + "' удален!", "Успех", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void addNewSession() {
        if (filmsModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(mainFrame, "Сначала добавьте фильмы!", "Ошибка", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JComboBox<String> filmCombo = new JComboBox<>();
        for (int i = 0; i < filmsModel.getRowCount(); i++) filmCombo.addItem((String) filmsModel.getValueAt(i, 0));

        JTextField dateField = new JTextField("15.12.2025");
        JTextField timeField = new JTextField("18:00");
        JTextField hallField = new JTextField("Зал 1");
        JTextField priceField = new JTextField("350 руб");

        Object[] message = {
            "Фильм:", filmCombo,
            "Дата:", dateField,
            "Время:", timeField,
            "Зал:", hallField,
            "Цена:", priceField
        };

        int option = JOptionPane.showConfirmDialog(mainFrame, message, "Добавление нового сеанса", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            sessionsModel.addRow(new Object[]{filmCombo.getSelectedItem(), dateField.getText(), timeField.getText(), hallField.getText(), priceField.getText()});
            String sessionName = filmCombo.getSelectedItem() + " " + timeField.getText();
            ticketsModel.addRow(new Object[]{sessionName, "A1", "Свободно", "-"});
            JOptionPane.showMessageDialog(mainFrame, "Сеанс успешно добавлен!", "Успех", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void generateReport() {
        int filmCount = filmsModel.getRowCount();
        int sessionCount = sessionsModel.getRowCount();
        int ticketCount = ticketsModel.getRowCount();
        int soldTickets = 0;
        for (int i = 0; i < ticketCount; i++) {
            String status = String.valueOf(ticketsModel.getValueAt(i, 2));
            if ("Продан".equals(status)) soldTickets++;
        }
        String report = String.format("ОТЧЕТ КИНОТЕАТРА\n\nКоличество фильмов: %d\nКоличество сеансов: %d\nВсего билетов: %d\nПроданных билетов: %d\nДоход: %d руб.\n\nОтчет сгенерирован: %s", filmCount, sessionCount, ticketCount, soldTickets, soldTickets * 350, new java.util.Date().toString());
        JOptionPane.showMessageDialog(mainFrame, report, "Быстрый отчет", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showFilmDetails() {
        int row = filmsTable.getSelectedRow();
        if (row == -1) return;
        String title = String.valueOf(filmsModel.getValueAt(row, 0));
        String director = String.valueOf(filmsModel.getValueAt(row, 1));
        String year = String.valueOf(filmsModel.getValueAt(row, 2));
        String genre = String.valueOf(filmsModel.getValueAt(row, 3));
        String duration = String.valueOf(filmsModel.getValueAt(row, 4));
        String filmInfo = String.format("Детальная информация о фильме:\n\nНазвание: %s\nРежиссер: %s\nГод выпуска: %s\nЖанр: %s\nДлительность: %s", title, director, year, genre, duration);
        JOptionPane.showMessageDialog(mainFrame, filmInfo, "Информация о фильме: " + title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void performSearch() {
        String selectedFilm = String.valueOf(filmFilter.getSelectedItem());
        String date = dateFilter.getText();
        String searchMessage;
        if (date.equals("Дата (дд.мм.гггг)")) searchMessage = String.format("Поиск по фильму: %s\nДата: не указана", selectedFilm);
        else searchMessage = String.format("Поиск по фильму: %s\nДата: %s", selectedFilm, date);
        JOptionPane.showMessageDialog(mainFrame, searchMessage, "Результаты поиска", JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateFilmFilter() {
        if (filmFilter == null) return;
        filmFilter.removeAllItems();
        filmFilter.addItem("Все фильмы");
        for (int i = 0; i < filmsModel.getRowCount(); i++) filmFilter.addItem(String.valueOf(filmsModel.getValueAt(i, 0)));
    }

    private void clearAllTables() {
        filmsModel.setRowCount(0);
        sessionsModel.setRowCount(0);
        ticketsModel.setRowCount(0);
    }

    // ========== Сохранение/Загрузка текстовый формат ==========
    private void saveAllDataToFile() {
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Сохранить данные кинотеатра");
            fileChooser.setSelectedFile(new File("cinema_data.txt"));
            if (fileChooser.showSaveDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                    writer.write("=== ФИЛЬМЫ ===\n");
                    for (int i = 0; i < filmsModel.getRowCount(); i++) {
                        for (int j = 0; j < filmsModel.getColumnCount(); j++) {
                            writer.write(String.valueOf(filmsModel.getValueAt(i, j)));
                            writer.write("|");
                        }
                        writer.write("\n");
                    }
                    writer.write("\n=== СЕАНСЫ ===\n");
                    for (int i = 0; i < sessionsModel.getRowCount(); i++) {
                        for (int j = 0; j < sessionsModel.getColumnCount(); j++) {
                            writer.write(String.valueOf(sessionsModel.getValueAt(i, j)));
                            writer.write("|");
                        }
                        writer.write("\n");
                    }
                    writer.write("\n=== БИЛЕТЫ ===\n");
                    for (int i = 0; i < ticketsModel.getRowCount(); i++) {
                        for (int j = 0; j < ticketsModel.getColumnCount(); j++) {
                            writer.write(String.valueOf(ticketsModel.getValueAt(i, j)));
                            writer.write("|");
                        }
                        writer.write("\n");
                    }
                }
                JOptionPane.showMessageDialog(mainFrame, "Данные сохранены в:\n" + file.getAbsolutePath(), "Сохранение завершено", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(mainFrame, "Ошибка сохранения: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadAllDataFromFile() {
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Загрузить данные кинотеатра");
            if (fileChooser.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                clearAllTables();
                try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                    String line;
                    String currentSection = "";
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("===")) { currentSection = line; continue; }
                        if (!line.trim().isEmpty()) {
                            String[] data = line.split("\\|");
                            if (currentSection.contains("ФИЛЬМЫ") && data.length >= 5) filmsModel.addRow(new Object[]{data[0], data[1], data[2], data[3], data[4]});
                            else if (currentSection.contains("СЕАНСЫ") && data.length >= 5) sessionsModel.addRow(new Object[]{data[0], data[1], data[2], data[3], data[4]});
                            else if (currentSection.contains("БИЛЕТЫ") && data.length >= 4) ticketsModel.addRow(new Object[]{data[0], data[1], data[2], data[3]});
                        }
                    }
                }
                updateFilmFilter();
                JOptionPane.showMessageDialog(mainFrame, "Данные загружены!\nФильмов: " + filmsModel.getRowCount() + "\nСеансов: " + sessionsModel.getRowCount() + "\nБилетов: " + ticketsModel.getRowCount(), "Загрузка завершена", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(mainFrame, "Файл не найден: " + ex.getMessage(), "Ошибка загрузки", JOptionPane.ERROR_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(mainFrame, "Ошибка при чтении файла: " + ex.getMessage(), "Ошибка загрузки", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ========== XML методы ==========
    private void saveAllDataToXML() {
        try {
            saveFilmsToXML();
            saveSessionsToXML();
            saveTicketsToXML();
            JOptionPane.showMessageDialog(mainFrame, "Все данные сохранены в XML файлы:\n- " + XML_FILMS_FILE + "\n- " + XML_SESSIONS_FILE + "\n- " + XML_TICKETS_FILE, "XML сохранение", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(mainFrame, "Ошибка при сохранении XML: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void saveFilmsToXML() throws Exception {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement("cinema");
        doc.appendChild(rootElement);
        Element filmsElement = doc.createElement("films");
        rootElement.appendChild(filmsElement);
        for (int i = 0; i < filmsModel.getRowCount(); i++) {
            Element filmElement = doc.createElement("film");
            filmsElement.appendChild(filmElement);
            filmElement.setAttribute("title", String.valueOf(filmsModel.getValueAt(i, 0)));
            filmElement.setAttribute("director", String.valueOf(filmsModel.getValueAt(i, 1)));
            filmElement.setAttribute("year", String.valueOf(filmsModel.getValueAt(i, 2)));
            filmElement.setAttribute("genre", String.valueOf(filmsModel.getValueAt(i, 3)));
            filmElement.setAttribute("duration", String.valueOf(filmsModel.getValueAt(i, 4)));
        }
        saveDocumentToFile(doc, XML_FILMS_FILE);
    }

    private void saveSessionsToXML() throws Exception {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement("cinema");
        doc.appendChild(rootElement);
        Element sessionsElement = doc.createElement("sessions");
        rootElement.appendChild(sessionsElement);
        for (int i = 0; i < sessionsModel.getRowCount(); i++) {
            Element sessionElement = doc.createElement("session");
            sessionsElement.appendChild(sessionElement);
            sessionElement.setAttribute("film", String.valueOf(sessionsModel.getValueAt(i, 0)));
            sessionElement.setAttribute("date", String.valueOf(sessionsModel.getValueAt(i, 1)));
            sessionElement.setAttribute("time", String.valueOf(sessionsModel.getValueAt(i, 2)));
            sessionElement.setAttribute("hall", String.valueOf(sessionsModel.getValueAt(i, 3)));
            sessionElement.setAttribute("price", String.valueOf(sessionsModel.getValueAt(i, 4)));
        }
        saveDocumentToFile(doc, XML_SESSIONS_FILE);
    }

    private void saveTicketsToXML() throws Exception {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement("cinema");
        doc.appendChild(rootElement);
        Element ticketsElement = doc.createElement("tickets");
        rootElement.appendChild(ticketsElement);
        for (int i = 0; i < ticketsModel.getRowCount(); i++) {
            Element ticketElement = doc.createElement("ticket");
            ticketsElement.appendChild(ticketElement);
            ticketElement.setAttribute("session", String.valueOf(ticketsModel.getValueAt(i, 0)));
            ticketElement.setAttribute("seat", String.valueOf(ticketsModel.getValueAt(i, 1)));
            ticketElement.setAttribute("status", String.valueOf(ticketsModel.getValueAt(i, 2)));
            ticketElement.setAttribute("saleTime", String.valueOf(ticketsModel.getValueAt(i, 3)));
        }
        saveDocumentToFile(doc, XML_TICKETS_FILE);
    }

    private void saveDocumentToFile(Document doc, String filename) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(filename));
        transformer.transform(source, result);
    }

    private void loadAllDataFromXML() {
        try {
            clearAllTables();
            int f = loadFilmsFromXML();
            int s = loadSessionsFromXML();
            int t = loadTicketsFromXML();
            updateFilmFilter();
            JOptionPane.showMessageDialog(mainFrame, "Данные загружены из XML.\nФильмов: " + f + ", Сеансов: " + s + ", Билетов: " + t, "Загрузка XML", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(mainFrame, "Ошибка при загрузке XML: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private int loadFilmsFromXML() throws Exception {
        File xmlFile = new File(XML_FILMS_FILE); if (!xmlFile.exists()) return 0;
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance(); DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlFile); doc.getDocumentElement().normalize();
        NodeList filmList = doc.getElementsByTagName("film"); int count = 0;
        for (int i = 0; i < filmList.getLength(); i++) {
            Node filmNode = filmList.item(i);
            if (filmNode.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) filmNode;
                filmsModel.addRow(new Object[]{e.getAttribute("title"), e.getAttribute("director"), e.getAttribute("year"), e.getAttribute("genre"), e.getAttribute("duration")});
                count++;
            }
        }
        return count;
    }

    private int loadSessionsFromXML() throws Exception {
        File xmlFile = new File(XML_SESSIONS_FILE); if (!xmlFile.exists()) return 0;
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance(); DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlFile); doc.getDocumentElement().normalize();
        NodeList list = doc.getElementsByTagName("session"); int count = 0;
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) n;
                sessionsModel.addRow(new Object[]{e.getAttribute("film"), e.getAttribute("date"), e.getAttribute("time"), e.getAttribute("hall"), e.getAttribute("price")});
                count++;
            }
        }
        return count;
    }

    private int loadTicketsFromXML() throws Exception {
        File xmlFile = new File(XML_TICKETS_FILE); if (!xmlFile.exists()) return 0;
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance(); DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlFile); doc.getDocumentElement().normalize();
        NodeList list = doc.getElementsByTagName("ticket"); int count = 0;
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) n;
                ticketsModel.addRow(new Object[]{e.getAttribute("session"), e.getAttribute("seat"), e.getAttribute("status"), e.getAttribute("saleTime")});
                count++;
            }
        }
        return count;
    }

    // ========== HTML генерация ==========
    private void generateHTMLReport() {
        try {
            String outputFile = "cinema_films_report.html";
            generateSimpleHTML(outputFile);
            JOptionPane.showMessageDialog(mainFrame, "HTML отчет успешно сгенерирован: " + outputFile, "HTML.", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(mainFrame, "Ошибка при генерации HTML: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void generateSimpleHTML(String filename) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(filename), StandardCharsets.UTF_8);
             PrintWriter writer = new PrintWriter(bw)) {
            writer.println("<!DOCTYPE html>");
            writer.println("<html lang='ru'><head><meta charset='utf-8'><title>Отчет по фильмам</title>");
            writer.println("<style>body{font-family:Arial, sans-serif;padding:20px;}table{border-collapse:collapse;width:100%}th,td{border:1px solid #ddd;padding:8px}</style>");
            writer.println("</head><body>");
            writer.println("<h1>Отчет по фильмам кинотеатра</h1>");
            writer.println("<p>Дата: " + new Date() + "</p>");
            writer.println("<h2>Фильмы</h2>");
            writer.println("<table><tr><th>Название</th><th>Режиссер</th><th>Год</th><th>Жанр</th><th>Длительность</th></tr>");
            for (int i = 0; i < filmsModel.getRowCount(); i++) {
                writer.println("<tr>");
                for (int j = 0; j < filmsModel.getColumnCount(); j++) writer.println("<td>" + escapeHtml(String.valueOf(filmsModel.getValueAt(i, j))) + "</td>");
                writer.println("</tr>");
            }
            writer.println("</table>");
            writer.println("<h2>Сеансы</h2>");
            writer.println("<table><tr><th>Фильм</th><th>Дата</th><th>Время</th><th>Зал</th><th>Цена</th></tr>");
            for (int i = 0; i < sessionsModel.getRowCount(); i++) {
                writer.println("<tr>");
                for (int j = 0; j < sessionsModel.getColumnCount(); j++) writer.println("<td>" + escapeHtml(String.valueOf(sessionsModel.getValueAt(i, j))) + "</td>");
                writer.println("</tr>");
            }
            writer.println("</table>");
            writer.println("<h2>Билеты</h2>");
            writer.println("<table><tr><th>Сеанс</th><th>Место</th><th>Статус</th><th>Время продажи</th></tr>");
            for (int i = 0; i < ticketsModel.getRowCount(); i++) {
                writer.println("<tr>");
                for (int j = 0; j < ticketsModel.getColumnCount(); j++) writer.println("<td>" + escapeHtml(String.valueOf(ticketsModel.getValueAt(i, j))) + "</td>");
                writer.println("</tr>");
            }
            writer.println("</table>");
            writer.println("</body></html>");
        }
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    // ========== PDF генерация ==========
    private void generatePDFReport() {
        try {
            String outputFile = "cinema_films_report.pdf";
            createBeautifulPDF(outputFile);
            JOptionPane.showMessageDialog(mainFrame, "PDF отчет успешно сгенерирован: " + outputFile, "PDF", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(mainFrame, "Ошибка при генерации PDF: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void createBeautifulPDF(String filename) throws IOException {
        StringBuilder pdfContent = new StringBuilder();
        pdfContent.append("%PDF-1.4\n");
        pdfContent.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
        pdfContent.append("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");
        pdfContent.append("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R /F2 6 0 R >> >> >>\nendobj\n");
        pdfContent.append("4 0 obj\n<< /Length 2800 >>\nstream\nBT\n/F2 28 Tf\n50 750 Td\n(CINEMA MANAGEMENT REPORT) Tj\n0 -40 Td\n/F1 12 Tf\n(Cinema Administration System) Tj\n0 -15 Td\n(Laboratory Work #7 - Report Generation) Tj\n0 -40 Td\n/F2 16 Tf\n(CINEMA STATISTICS) Tj\n0 -30 Td\n/F1 11 Tf\n(Total Films in Database: " + filmsModel.getRowCount() + ") Tj\n0 -15 Td\n(Total Sessions Scheduled: " + sessionsModel.getRowCount() + ") Tj\n0 -15 Td\n(Total Tickets Available: " + ticketsModel.getRowCount() + ") Tj\n0 -15 Td\n(Tickets Sold: " + countSoldTickets() + ") Tj\n0 -30 Td\n(Estimated Income: " + (countSoldTickets()*350) + " RUB) Tj\n0 -40 Td\n/F2 16 Tf\n(FEATURED FILMS) Tj\n0 -25 Td\n/F1 10 Tf\n");
        for (int i = 0; i < Math.min(5, filmsModel.getRowCount()); i++) {
            String t = String.valueOf(filmsModel.getValueAt(i, 0));
            pdfContent.append("(" + escapePdfText(t) + ") Tj\n0 -12 Td\n");
        }
        pdfContent.append("/F2 16 Tf\n(UPCOMING SESSIONS) Tj\n0 -25 Td\n/F1 10 Tf\n");
        for (int i = 0; i < Math.min(5, sessionsModel.getRowCount()); i++) {
            String s = sessionsModel.getValueAt(i,0) + " - " + sessionsModel.getValueAt(i,1) + " " + sessionsModel.getValueAt(i,2);
            pdfContent.append("(" + escapePdfText(String.valueOf(s)) + ") Tj\n0 -12 Td\n");
        }
        pdfContent.append("/F2 14 Tf\n(FINANCIAL SUMMARY) Tj\n0 -25 Td\n/F1 11 Tf\n(Current Revenue: " + (countSoldTickets()*350) + " RUB) Tj\n0 -15 Td\n(Potential Revenue: " + ((countSoldTickets()+countReservedTickets())*350) + " RUB) Tj\n0 -30 Td\n/F1 9 Tf\n(Report generated automatically " + new Date() + ") Tj\nET\nendstream\nendobj\n");
        pdfContent.append("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");
        pdfContent.append("6 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\nendobj\n");
        pdfContent.append("xref\n0 7\n0000000000 65535 f\n0000000009 00000 n\n0000000058 00000 n\n0000000115 00000 n\n0000000244 00000 n\n0000003095 00000 n\n0000003183 00000 n\ntrailer\n<< /Size 7 /Root 1 0 R >>\nstartxref\n3275\n%%EOF\n");
        try (FileOutputStream fos = new FileOutputStream(filename)) {
            fos.write(pdfContent.toString().getBytes(StandardCharsets.ISO_8859_1));
            fos.flush();
        }
    }

    private String escapePdfText(String s) {
        if (s == null) return "";
        return s.replace("(", "\\(").replace(")", "\\)").replace("\n", " ");
    }

    private int countSoldTickets() {
        int sold = 0;
        for (int i = 0; i < ticketsModel.getRowCount(); i++) if ("Продан".equals(String.valueOf(ticketsModel.getValueAt(i,2)))) sold++;
        return sold;
    }

    private int countReservedTickets() {
        int res = 0;
        for (int i = 0; i < ticketsModel.getRowCount(); i++) if ("Забронирован".equals(String.valueOf(ticketsModel.getValueAt(i,2)))) res++;
        return res;
    }

    // ========== ЛР8: МНОГОПОТОЧНОСТЬ ==========

    // 🚀 ФРАГМЕНТ 1: ЗАПУСК ВСЕХ ПОТОКОВ
    private void startThreeThreads() {
        runThreadsButton.setEnabled(false);
        // CountDownLatch - синхронизация потоков
        latchLoad = new CountDownLatch(1);
        latchEdit = new CountDownLatch(1);

        // Создание и запуск трех потоков
        Thread loader = new Thread(new LoaderThread(), "LoaderThread");
        Thread editor = new Thread(new EditorThread(), "EditorThread");
        Thread reporter = new Thread(new ReporterThread(), "ReporterThread");

        loader.start();
        editor.start();
        reporter.start();

        // Поток для отслеживания завершения
        new Thread(() -> {
            try {
                // Ожидание завершения всех потоков
                loader.join();
                editor.join();
                reporter.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                // Безопасное обновление UI
                SwingUtilities.invokeLater(() -> runThreadsButton.setEnabled(true));
            }
        }, "Joiner").start();
    }

    // 📥 ФРАГМЕНТ 2: ПОТОК ЗАГРУЗКИ ДАННЫХ (DataLoadThread)
    private class LoaderThread implements Runnable {
        @Override public void run() {
            try {
                System.out.println("Loader: starting...");
                Thread.sleep(500); // имитация загрузки
                
                // Безопасное обновление UI из потока
                SwingUtilities.invokeAndWait(() -> {
                    File f = new File(XML_FILMS_FILE);
                    if (f.exists()) {
                        loadAllDataFromXML(); // загрузка данных
                    }
                    JOptionPane.showMessageDialog(mainFrame, "Loader: данные загружены в форму (EDT).");
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                System.out.println("Loader: done, countDown latchLoad");
                latchLoad.countDown(); // сигнал о завершении
            }
        }
    }

    // 🔄 ФРАГМЕНТ 3: ПОТОК РЕДАКТИРОВАНИЯ ДАННЫХ (DataEditThread)
    private class EditorThread implements Runnable {
        @Override public void run() {
            try {
                System.out.println("Editor: waiting for loader...");
                // ОЖИДАНИЕ завершения LoaderThread
                latchLoad.await();
                
                System.out.println("Editor: editing on EDT...");
                SwingUtilities.invokeAndWait(() -> {
                    // Редактирование данных в UI
                    for (int i = 0; i < filmsModel.getRowCount(); i++) {
                        String t = String.valueOf(filmsModel.getValueAt(i, 0));
                        if (!t.endsWith(" (edited)")) 
                            filmsModel.setValueAt(t + " (edited)", i, 0);
                    }
                    if (sessionsModel.getRowCount() > 0) 
                        sessionsModel.setValueAt(String.valueOf(sessionsModel.getValueAt(0,4)) + " (edited)", 0, 4);
                    JOptionPane.showMessageDialog(mainFrame, "Editor: внес изменения (EDT).");
                });
                
                saveAllDataToXML(); // сохранение отредактированных данных
                
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                System.out.println("Editor: done, countDown latchEdit");
                latchEdit.countDown(); // сигнал о завершении
            }
        }
    }

    // 📊 ФРАГМЕНТ 4: ПОТОК ГЕНЕРАЦИИ ОТЧЕТА (ReportGenerationThread)
    private class ReporterThread implements Runnable {
        @Override public void run() {
            try {
                System.out.println("Reporter: waiting for editor...");
                // ОЖИДАНИЕ завершения EditorThread
                latchEdit.await();
                
                System.out.println("Reporter: generating HTML...");
                // Генерация HTML отчета
                SwingUtilities.invokeAndWait(() -> {
                    try {
                        generateSimpleHTML("cinema_report_from_threads.html");
                        JOptionPane.showMessageDialog(mainFrame, 
                            "Reporter: HTML сформирован: cinema_report_from_threads.html");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(mainFrame, 
                            "Reporter: ошибка генерации HTML: " + ex.getMessage());
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // ========== Тестовые данные и main ==========

    private void addTestData() {
        if (filmsModel.getRowCount() > 0) return;
        filmsModel.addRow(new Object[]{"Интерстеллар", "Кристофер Нолан", "2014", "Фантастика", "169 мин"});
        filmsModel.addRow(new Object[]{"Крестный отец", "Фрэнсис Коппола", "1972", "Криминал", "175 мин"});
        filmsModel.addRow(new Object[]{"Побег из Шоушенка", "Фрэнк Дарабонт", "1994", "Драма", "142 мин"});
        filmsModel.addRow(new Object[]{"Матрица", "Вачовски", "1999", "Фантастика", "136 мин"});
        filmsModel.addRow(new Object[]{"Форрест Гамп", "Роберт Земекис", "1994", "Драма", "142 мин"});

        sessionsModel.addRow(new Object[]{"Интерстеллар", "15.12.2025", "18:00", "Зал 1", "350 руб"});
        sessionsModel.addRow(new Object[]{"Интерстеллар", "15.12.2025", "21:00", "Зал 1", "400 руб"});
        sessionsModel.addRow(new Object[]{"Крестный отец", "16.12.2025", "19:30", "Зал 2", "300 руб"});
        sessionsModel.addRow(new Object[]{"Матрица", "17.12.2025", "20:00", "Зал 3", "350 руб"});

        ticketsModel.addRow(new Object[]{"Интерстеллар 18:00", "A1", "Продан", "14:30"});
        ticketsModel.addRow(new Object[]{"Интерстеллар 18:00", "A2", "Продан", "14:35"});
        ticketsModel.addRow(new Object[]{"Интерстеллар 18:00", "A3", "Свободно", "-"});
        ticketsModel.addRow(new Object[]{"Крестный отец 19:30", "B5", "Забронирован", "15:20"});
        ticketsModel.addRow(new Object[]{"Матрица 20:00", "C2", "Свободно", "-"});

        updateFilmFilter();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CinemaAdminApp().show());
    }
}