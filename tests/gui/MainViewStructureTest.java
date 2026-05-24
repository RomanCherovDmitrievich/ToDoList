package gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Проверка структуры MainView.fxml")
class MainViewStructureTest {

    @Test
    @DisplayName("Главное окно содержит новые элементы календаря и аккаунта")
    void testCalendarTabContainsExpectedControls() throws Exception {
        Document document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(Path.of("src", "view", "MainView.fxml").toFile());

        assertTrue(hasFxId(document, "calendarPicker"), "В календарной вкладке должен быть DatePicker");
        assertTrue(hasFxId(document, "calendarMonthGrid"), "В календарной вкладке должна быть месячная сетка");
        assertTrue(hasFxId(document, "calendarTaskList"), "В календарной вкладке должен быть список задач");
        assertTrue(hasFxId(document, "calendarSelectedDateLabel"), "Должен быть заголовок выбранного дня");
        assertTrue(hasFxId(document, "calendarExportButton"), "В календарной вкладке должна быть кнопка экспорта");
        assertTrue(hasFxId(document, "switchUserButton"), "Должна быть кнопка смены пользователя");
        assertTrue(hasFxId(document, "currentUserLabel"), "Должен быть индикатор текущего пользователя");
    }

    private boolean hasFxId(Document document, String fxId) {
        NodeList allNodes = document.getElementsByTagName("*");
        for (int i = 0; i < allNodes.getLength(); i++) {
            String currentFxId = allNodes.item(i).getAttributes() == null
                ? null
                : allNodes.item(i).getAttributes().getNamedItem("fx:id") == null
                    ? null
                    : allNodes.item(i).getAttributes().getNamedItem("fx:id").getNodeValue();
            if (fxId.equals(currentFxId)) {
                return true;
            }
        }
        return false;
    }
}
