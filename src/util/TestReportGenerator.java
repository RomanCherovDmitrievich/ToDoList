package util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Генератор простого HTML-отчета по JUnit XML.
 */
public final class TestReportGenerator {
    private static final DateTimeFormatter REPORT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TestReportGenerator() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Usage: java util.TestReportGenerator <xml-dir> <html-file> <log-file>");
            System.exit(1);
        }

        Path xmlDir = Path.of(args[0]);
        Path htmlFile = Path.of(args[1]);
        Path logFile = Path.of(args[2]);

        List<TestCaseResult> tests = loadResults(xmlDir);
        String html = buildHtmlReport(tests, logFile);

        if (htmlFile.getParent() != null) {
            Files.createDirectories(htmlFile.getParent());
        }
        Files.writeString(htmlFile, html, StandardCharsets.UTF_8);
    }

    private static List<TestCaseResult> loadResults(Path xmlDir) throws Exception {
        List<TestCaseResult> tests = new ArrayList<>();
        if (!Files.isDirectory(xmlDir)) {
            return tests;
        }

        List<Path> xmlFiles = Files.list(xmlDir)
            .filter(path -> path.getFileName().toString().endsWith(".xml"))
            .sorted(Comparator.comparing(Path::toString))
            .toList();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

        for (Path xmlFile : xmlFiles) {
            Document document = factory.newDocumentBuilder().parse(xmlFile.toFile());
            NodeList nodeList = document.getElementsByTagName("testcase");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element testCase = (Element) nodeList.item(i);
                String className = testCase.getAttribute("classname");
                String name = testCase.getAttribute("name");
                String duration = testCase.getAttribute("time");

                String status = "PASSED";
                String details = "";

                NodeList failures = testCase.getElementsByTagName("failure");
                NodeList errors = testCase.getElementsByTagName("error");
                NodeList skipped = testCase.getElementsByTagName("skipped");

                if (failures.getLength() > 0) {
                    status = "FAILED";
                    details = extractDetails((Element) failures.item(0));
                } else if (errors.getLength() > 0) {
                    status = "ERROR";
                    details = extractDetails((Element) errors.item(0));
                } else if (skipped.getLength() > 0) {
                    status = "SKIPPED";
                    details = "Test skipped";
                }

                tests.add(new TestCaseResult(className, name, duration, status, details));
            }
        }

        return tests;
    }

    private static String extractDetails(Element element) {
        String message = element.getAttribute("message");
        String text = element.getTextContent() == null ? "" : element.getTextContent().trim();
        if (!message.isBlank() && !text.isBlank()) {
            return message + "\n" + text;
        }
        return !message.isBlank() ? message : text;
    }

    private static String buildHtmlReport(List<TestCaseResult> tests, Path logFile) throws IOException {
        int total = tests.size();
        long passed = tests.stream().filter(test -> "PASSED".equals(test.status())).count();
        long failed = tests.stream().filter(test -> "FAILED".equals(test.status()) || "ERROR".equals(test.status())).count();
        long skipped = tests.stream().filter(test -> "SKIPPED".equals(test.status())).count();
        String successRate = total == 0 ? "0%" : Math.round((passed * 100.0) / total) + "%";
        double totalDuration = tests.stream().mapToDouble(TestReportGenerator::parseDuration).sum();
        String logs = Files.exists(logFile) ? Files.readString(logFile, StandardCharsets.UTF_8) : "";

        StringBuilder failureCards = new StringBuilder();
        tests.stream()
            .filter(test -> !"PASSED".equals(test.status()))
            .sorted(Comparator.comparing(TestCaseResult::className).thenComparing(TestCaseResult::name))
            .forEach(test -> failureCards.append("""
                <article class="issue-card %s">
                    <div class="issue-head">
                        <span class="status-pill %s">%s</span>
                        <span class="issue-time">%s s</span>
                    </div>
                    <h3>%s</h3>
                    <p class="issue-suite">%s</p>
                    <pre>%s</pre>
                </article>
                """.formatted(
                test.status().toLowerCase(Locale.ROOT),
                test.status().toLowerCase(Locale.ROOT),
                escapeHtml(test.status()),
                escapeHtml(test.duration()),
                escapeHtml(test.name()),
                escapeHtml(test.className()),
                escapeHtml(test.details().isBlank() ? "Причина не указана." : test.details())
            )));
        if (failureCards.isEmpty()) {
            failureCards.append("""
                <article class="issue-card passed">
                    <div class="issue-head">
                        <span class="status-pill passed">PASSED</span>
                    </div>
                    <h3>Все тесты завершились успешно</h3>
                    <p class="issue-suite">Проблемных сценариев в этой сборке не найдено.</p>
                </article>
                """);
        }

        Map<String, SuiteSummary> suites = new LinkedHashMap<>();
        tests.stream()
            .sorted(Comparator.comparing(TestCaseResult::className).thenComparing(TestCaseResult::name))
            .forEach(test -> suites.computeIfAbsent(test.className(), ignored -> new SuiteSummary()).accept(test));

        StringBuilder suiteCards = new StringBuilder();
        suites.forEach((suiteName, suite) -> suiteCards.append("""
            <article class="suite-card">
                <h3>%s</h3>
                <div class="suite-stats">
                    <span>Всего: %d</span>
                    <span>OK: %d</span>
                    <span>Ошибки: %d</span>
                    <span>Пропуски: %d</span>
                    <span>Время: %.3f s</span>
                </div>
            </article>
            """.formatted(
            escapeHtml(suiteName),
            suite.total,
            suite.passed,
            suite.failed,
            suite.skipped,
            suite.duration
        )));

        StringBuilder rows = new StringBuilder();
        tests.stream()
            .sorted(Comparator.comparing(TestCaseResult::status).thenComparing(TestCaseResult::className).thenComparing(TestCaseResult::name))
            .forEach(test -> {
                String searchableText = (test.className() + " " + test.name() + " " + test.details()).toLowerCase(Locale.ROOT);
            rows.append("""
                <tr data-status="%s" data-search="%s">
                    <td>
                        <div class="test-name">%s</div>
                        <div class="test-suite">%s</div>
                    </td>
                    <td>%s</td>
                    <td><span class="status-pill %s">%s</span></td>
                    <td>%s</td>
                    <td><pre>%s</pre></td>
                </tr>
                """.formatted(
                escapeHtml(test.status().toLowerCase(Locale.ROOT)),
                escapeHtml(searchableText),
                escapeHtml(test.name()),
                escapeHtml(test.className()),
                escapeHtml(describeStatus(test.status())),
                test.status().toLowerCase(),
                escapeHtml(test.status()),
                escapeHtml(test.duration()),
                escapeHtml(test.details())
            ));
        });

        return """
            <!DOCTYPE html>
            <html lang="ru">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Test Report - ToDo List</title>
                <style>
                    :root {
                        color-scheme: light;
                        --bg: #f5efe6;
                        --panel: rgba(255, 255, 255, 0.88);
                        --panel-strong: #fffdf8;
                        --text: #1f2933;
                        --muted: #5b6877;
                        --line: rgba(31, 41, 51, 0.12);
                        --accent: #0f766e;
                        --accent-2: #f97316;
                        --passed-bg: #dcfce7;
                        --passed-text: #166534;
                        --failed-bg: #fee2e2;
                        --failed-text: #991b1b;
                        --skipped-bg: #fef3c7;
                        --skipped-text: #92400e;
                        --shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
                    }
                    * { box-sizing: border-box; }
                    body {
                        margin: 0;
                        font-family: "Segoe UI", "SF Pro Display", Arial, sans-serif;
                        color: var(--text);
                        background:
                            radial-gradient(circle at top left, rgba(15, 118, 110, 0.12), transparent 28%%),
                            radial-gradient(circle at top right, rgba(249, 115, 22, 0.16), transparent 24%%),
                            linear-gradient(180deg, #f7f2e9 0%%, #f5efe6 48%%, #f2ece4 100%%);
                    }
                    .page {
                        max-width: 1360px;
                        margin: 0 auto;
                        padding: 32px 24px 40px;
                    }
                    .hero {
                        background: linear-gradient(135deg, rgba(15, 118, 110, 0.96), rgba(8, 145, 178, 0.9));
                        color: white;
                        border-radius: 28px;
                        padding: 32px;
                        box-shadow: var(--shadow);
                    }
                    .hero h1 {
                        margin: 0 0 8px;
                        font-size: clamp(28px, 4vw, 42px);
                    }
                    .hero p {
                        margin: 0;
                        max-width: 760px;
                        color: rgba(255, 255, 255, 0.88);
                        line-height: 1.5;
                    }
                    .hero-meta {
                        display: flex;
                        gap: 12px;
                        flex-wrap: wrap;
                        margin-top: 18px;
                    }
                    .meta-chip {
                        padding: 10px 14px;
                        border-radius: 999px;
                        background: rgba(255, 255, 255, 0.16);
                        backdrop-filter: blur(10px);
                        font-size: 14px;
                    }
                    .stats {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
                        gap: 16px;
                        margin: 24px 0;
                    }
                    .card {
                        background: var(--panel);
                        backdrop-filter: blur(12px);
                        border: 1px solid rgba(255,255,255,0.55);
                        border-radius: 22px;
                        padding: 20px;
                        box-shadow: var(--shadow);
                    }
                    .card strong {
                        display: block;
                        color: var(--muted);
                        font-size: 13px;
                        text-transform: uppercase;
                        letter-spacing: 0.08em;
                    }
                    .card .value {
                        margin-top: 10px;
                        font-size: 34px;
                        font-weight: 700;
                    }
                    .success-ring {
                        height: 10px;
                        margin-top: 16px;
                        border-radius: 999px;
                        background: rgba(15, 118, 110, 0.12);
                        overflow: hidden;
                    }
                    .success-ring span {
                        display: block;
                        height: 100%%;
                        background: linear-gradient(90deg, var(--accent), #14b8a6);
                        border-radius: inherit;
                    }
                    .section {
                        margin-top: 24px;
                    }
                    .section-header {
                        display: flex;
                        justify-content: space-between;
                        gap: 12px;
                        align-items: end;
                        margin-bottom: 12px;
                    }
                    .section-header h2 {
                        margin: 0;
                        font-size: 24px;
                    }
                    .section-header p {
                        margin: 6px 0 0;
                        color: var(--muted);
                    }
                    .issues-grid, .suite-grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
                        gap: 16px;
                    }
                    .issue-card, .suite-card {
                        background: var(--panel);
                        border: 1px solid rgba(255,255,255,0.55);
                        border-radius: 22px;
                        padding: 18px;
                        box-shadow: var(--shadow);
                    }
                    .issue-card.failed, .issue-card.error {
                        border-color: rgba(239, 68, 68, 0.24);
                    }
                    .issue-head {
                        display: flex;
                        justify-content: space-between;
                        gap: 12px;
                        align-items: center;
                    }
                    .issue-card h3, .suite-card h3 {
                        margin: 14px 0 6px;
                        font-size: 18px;
                    }
                    .issue-suite {
                        margin: 0 0 12px;
                        color: var(--muted);
                    }
                    .suite-stats {
                        display: flex;
                        flex-wrap: wrap;
                        gap: 10px;
                        color: var(--muted);
                    }
                    .toolbar {
                        display: flex;
                        gap: 12px;
                        flex-wrap: wrap;
                        align-items: center;
                        margin: 12px 0 16px;
                    }
                    .search {
                        flex: 1 1 260px;
                        min-width: 240px;
                        padding: 12px 14px;
                        border-radius: 14px;
                        border: 1px solid var(--line);
                        background: rgba(255,255,255,0.84);
                        font: inherit;
                    }
                    .filters {
                        display: flex;
                        gap: 10px;
                        flex-wrap: wrap;
                    }
                    .filter-btn {
                        border: 0;
                        padding: 10px 14px;
                        border-radius: 999px;
                        background: rgba(15, 23, 42, 0.06);
                        color: var(--text);
                        cursor: pointer;
                        font: inherit;
                    }
                    .filter-btn.active {
                        background: var(--accent);
                        color: white;
                    }
                    .table-wrap {
                        overflow: auto;
                        background: var(--panel-strong);
                        border-radius: 24px;
                        box-shadow: var(--shadow);
                        border: 1px solid rgba(255,255,255,0.55);
                    }
                    table {
                        width: 100%%;
                        border-collapse: collapse;
                    }
                    th, td {
                        padding: 14px 16px;
                        border-bottom: 1px solid var(--line);
                        text-align: left;
                        vertical-align: top;
                    }
                    th {
                        position: sticky;
                        top: 0;
                        background: rgba(250, 248, 244, 0.96);
                        z-index: 1;
                    }
                    .test-name {
                        font-weight: 700;
                    }
                    .test-suite {
                        margin-top: 4px;
                        font-size: 13px;
                        color: var(--muted);
                    }
                    .status-pill {
                        display: inline-flex;
                        align-items: center;
                        gap: 8px;
                        padding: 6px 12px;
                        border-radius: 999px;
                        font-weight: 700;
                        font-size: 13px;
                    }
                    .passed { background: var(--passed-bg); color: var(--passed-text); }
                    .failed, .error { background: var(--failed-bg); color: var(--failed-text); }
                    .skipped { background: var(--skipped-bg); color: var(--skipped-text); }
                    pre {
                        white-space: pre-wrap;
                        margin: 0;
                        font: 12px/1.5 "SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace;
                        color: #0f172a;
                    }
                    details.logs {
                        margin-top: 24px;
                        background: var(--panel);
                        border-radius: 22px;
                        padding: 18px 20px;
                        box-shadow: var(--shadow);
                    }
                    details summary {
                        cursor: pointer;
                        font-weight: 700;
                    }
                    .hidden {
                        display: none;
                    }
                    @media (max-width: 780px) {
                        .page { padding: 20px 14px 30px; }
                        .hero { padding: 24px 20px; }
                        th, td { padding: 12px; }
                    }
                </style>
            </head>
            <body>
                <div class="page">
                    <section class="hero">
                        <h1>Отчет по тестам ToDoList</h1>
                        <p>Наглядная сводка по последнему прогону: общая стабильность, проблемные сценарии, детализация по тестовым классам и полный лог запуска в одном HTML-файле.</p>
                        <div class="hero-meta">
                            <span class="meta-chip">Сформирован: %s</span>
                            <span class="meta-chip">Тестов: %d</span>
                            <span class="meta-chip">Успешность: %s</span>
                            <span class="meta-chip">Время: %.3f s</span>
                        </div>
                    </section>

                    <section class="stats">
                        <article class="card">
                            <strong>Всего тестов</strong>
                            <div class="value">%d</div>
                        </article>
                        <article class="card">
                            <strong>Успешно</strong>
                            <div class="value">%d</div>
                        </article>
                        <article class="card">
                            <strong>Ошибки</strong>
                            <div class="value">%d</div>
                        </article>
                        <article class="card">
                            <strong>Пропущено</strong>
                            <div class="value">%d</div>
                        </article>
                        <article class="card">
                            <strong>Успешность</strong>
                            <div class="value">%s</div>
                            <div class="success-ring"><span style="width:%s"></span></div>
                        </article>
                    </section>

                    <section class="section">
                        <div class="section-header">
                            <div>
                                <h2>Проблемные тесты</h2>
                                <p>Сначала показываются сбои и пропуски, чтобы разработчик сразу видел, что требует внимания.</p>
                            </div>
                        </div>
                        <div class="issues-grid">
                            %s
                        </div>
                    </section>

                    <section class="section">
                        <div class="section-header">
                            <div>
                                <h2>Сводка по классам</h2>
                                <p>Быстрый обзор того, какие наборы тестов прошли стабильно, а какие дали сбой.</p>
                            </div>
                        </div>
                        <div class="suite-grid">
                            %s
                        </div>
                    </section>

                    <section class="section">
                        <div class="section-header">
                            <div>
                                <h2>Все тесты</h2>
                                <p>Таблица поддерживает быстрый поиск и фильтрацию по статусу прямо в браузере.</p>
                            </div>
                        </div>
                        <div class="toolbar">
                            <input id="search" class="search" type="search" placeholder="Поиск по имени теста, классу или тексту ошибки">
                            <div class="filters">
                                <button class="filter-btn active" data-filter="all">Все</button>
                                <button class="filter-btn" data-filter="passed">PASSED</button>
                                <button class="filter-btn" data-filter="failed">FAILED</button>
                                <button class="filter-btn" data-filter="error">ERROR</button>
                                <button class="filter-btn" data-filter="skipped">SKIPPED</button>
                            </div>
                        </div>
                        <div class="table-wrap">
                            <table>
                                <thead>
                                    <tr>
                                        <th>Тест</th>
                                        <th>Описание</th>
                                        <th>Результат</th>
                                        <th>Время, с</th>
                                        <th>Причина ошибки</th>
                                    </tr>
                                </thead>
                                <tbody id="test-table-body">
                                    %s
                                </tbody>
                            </table>
                        </div>
                    </section>

                    <details class="logs">
                        <summary>Логи запуска</summary>
                        <pre>%s</pre>
                    </details>
                </div>
                <script>
                    const searchInput = document.getElementById('search');
                    const buttons = Array.from(document.querySelectorAll('.filter-btn'));
                    const rows = Array.from(document.querySelectorAll('#test-table-body tr'));
                    let activeFilter = 'all';

                    function applyFilters() {
                        const query = (searchInput.value || '').toLowerCase().trim();
                        rows.forEach((row) => {
                            const status = row.dataset.status || '';
                            const search = row.dataset.search || '';
                            const matchesFilter = activeFilter === 'all' || status === activeFilter;
                            const matchesQuery = !query || search.includes(query);
                            row.classList.toggle('hidden', !(matchesFilter && matchesQuery));
                        });
                    }

                    buttons.forEach((button) => {
                        button.addEventListener('click', () => {
                            activeFilter = button.dataset.filter;
                            buttons.forEach((candidate) => candidate.classList.toggle('active', candidate === button));
                            applyFilters();
                        });
                    });

                    searchInput.addEventListener('input', applyFilters);
                </script>
            </body>
            </html>
            """.formatted(
            LocalDateTime.now().format(REPORT_TIME),
            total,
            successRate,
            totalDuration,
            total,
            passed,
            failed,
            skipped,
            successRate,
            successRate,
            failureCards,
            suiteCards,
            rows,
            escapeHtml(logs)
        );
    }

    private static String describeStatus(String status) {
        return switch (status) {
            case "PASSED" -> "Тест завершился успешно.";
            case "FAILED" -> "JUnit зафиксировал сбой проверки.";
            case "ERROR" -> "Тест завершился ошибкой выполнения.";
            case "SKIPPED" -> "Сценарий был пропущен.";
            default -> "";
        };
    }

    private static double parseDuration(TestCaseResult test) {
        try {
            return Double.parseDouble(test.duration());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    private record TestCaseResult(String className, String name, String duration, String status, String details) {
    }

    private static final class SuiteSummary {
        private int total;
        private int passed;
        private int failed;
        private int skipped;
        private double duration;

        private void accept(TestCaseResult test) {
            total++;
            duration += parseDuration(test);
            switch (test.status()) {
                case "PASSED" -> passed++;
                case "SKIPPED" -> skipped++;
                case "FAILED", "ERROR" -> failed++;
                default -> {
                }
            }
        }
    }
}
