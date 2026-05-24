package gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Проверка структуры NewTaskDialog.fxml")
class NewTaskDialogGuiTest {

    @Test
    @DisplayName("Диалог создания задачи содержит основные поля формы")
    void testDialogContainsCoreControls() throws Exception {
        Document document = loadDocument();

        assertNotNull(findNodeByFxId(document, "titleField"), "Должно быть поле заголовка");
        assertNotNull(findNodeByFxId(document, "startDatePicker"), "Должен быть DatePicker начала");
        assertNotNull(findNodeByFxId(document, "endDatePicker"), "Должен быть DatePicker окончания");
        assertNotNull(findNodeByFxId(document, "priorityCombo"), "Должен быть список приоритета");
        assertNotNull(findNodeByFxId(document, "categoryCombo"), "Должен быть список категории");
        assertNotNull(findNodeByFxId(document, "createButton"), "Должна быть кнопка создания");
    }

    @Test
    @DisplayName("Кнопки диалога привязаны к ожидаемым обработчикам")
    void testDialogButtonsHaveExpectedActions() throws Exception {
        Document document = loadDocument();

        Node createButton = findNodeByFxId(document, "createButton");
        Node cancelButton = findNodeByFxId(document, "cancelButton");

        assertNotNull(createButton);
        assertNotNull(cancelButton);
        assertEquals("#handleCreateTask", getAttribute(createButton, "onAction"));
        assertEquals("#handleCancel", getAttribute(cancelButton, "onAction"));
        assertEquals("Создать", getAttribute(createButton, "text"));
        assertEquals("Отмена", getAttribute(cancelButton, "text"));
    }

    private Document loadDocument() throws Exception {
        return DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(Path.of("src", "view", "NewTaskDialog.fxml").toFile());
    }

    private Node findNodeByFxId(Document document, String fxId) {
        NodeList allNodes = document.getElementsByTagName("*");
        for (int i = 0; i < allNodes.getLength(); i++) {
            Node node = allNodes.item(i);
            if (fxId.equals(getAttribute(node, "fx:id"))) {
                return node;
            }
        }
        return null;
    }

    private String getAttribute(Node node, String attributeName) {
        if (node == null) {
            return null;
        }
        NamedNodeMap attributes = node.getAttributes();
        if (attributes == null) {
            return null;
        }
        Node attribute = attributes.getNamedItem(attributeName);
        return attribute == null ? null : attribute.getNodeValue();
    }
}
