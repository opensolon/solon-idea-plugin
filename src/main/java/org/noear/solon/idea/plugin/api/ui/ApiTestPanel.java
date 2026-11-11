package org.noear.solon.idea.plugin.api.ui;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.noear.solon.idea.plugin.api.HttpRequestService;
import org.noear.solon.idea.plugin.api.ServerConfigResolver;
import org.noear.solon.idea.plugin.api.model.ApiEndpoint;
import org.noear.solon.idea.plugin.api.model.ApiParameter;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 接口快速调用侧边栏面板：自动注入 host / port / path / 参数 / 请求体，发送后展示响应。
 * 整体纵向可滚动；响应体与响应头均支持「格式化 / 源码」两种查看方式。
 */
public class ApiTestPanel extends JPanel implements Disposable {

    private static final String KEY_HOST = "solon.apiTest.host";
    private static final String KEY_PORT = "solon.apiTest.port";
    private static final String KEY_CONTEXT_PATH = "solon.apiTest.contextPath";
    private static final String KEY_TOKEN = "solon.apiTest.token";

    private static final String[] PARAM_COLUMNS = {"Name", "In", "Type", "Required", "Value"};
    private static final String[] HEADER_COLUMNS = {"Name", "Value"};
    private static final String[] RESPONSE_HEADER_COLUMNS = {"Name", "Value"};

    private static final String VIEW_FORMATTED = "formatted";
    private static final String VIEW_SOURCE = "source";

    private final Project project;

    private final JComboBox<ApiEndpoint> endpointCombo = new JComboBox<>();
    private final JComboBox<String> methodCombo = new JComboBox<>(new DefaultComboBoxModel<>(
            new String[]{"GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"}));
    private final JBTextField hostField = new JBTextField();
    private final JBTextField portField = new JBTextField();
    private final JBTextField contextPathField = new JBTextField();
    private final JBTextField pathField = new JBTextField();
    private final JBTextField tokenField = new JBTextField();

    private final DefaultTableModel paramsModel;
    private final JBTable paramsTable;
    private final DefaultTableModel headersModel;
    private final JBTable headersTable;

    private final JBTextArea bodyArea = new JBTextArea();

    private final JBTextArea formattedBodyArea = new JBTextArea();
    private final JBTextArea sourceBodyArea = new JBTextArea();
    private final CardLayout bodyCardLayout = new CardLayout();
    private final JPanel bodyCards = new JPanel(bodyCardLayout);

    private final DefaultTableModel responseHeadersModel =
            new DefaultTableModel(RESPONSE_HEADER_COLUMNS, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
    private final JBTable responseHeadersTable = new JBTable(responseHeadersModel);
    private final JBTextArea sourceHeadersArea = new JBTextArea();
    private final CardLayout headerCardLayout = new CardLayout();
    private final JPanel headerCards = new JPanel(headerCardLayout);

    private final JToggleButton formattedViewButton = new JToggleButton("Formatted", true);
    private final JToggleButton sourceViewButton = new JToggleButton("Source");

    private final JBLabel statusLabel = new JBLabel("Ready.");
    private final JButton sendButton = new JButton("Send");

    private boolean updatingCombo;
    private boolean sending;

    public ApiTestPanel(@NotNull Project project) {
        super(new BorderLayout(0, JBUI.scale(6)));
        this.project = project;
        this.paramsModel = new DefaultTableModel(PARAM_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0 || column == 4;
            }
        };
        this.paramsTable = new JBTable(paramsModel);
        this.headersModel = new DefaultTableModel(HEADER_COLUMNS, 0);
        this.headersTable = new JBTable(headersModel);

        initFields();
        buildUi();

        endpointCombo.addActionListener(event -> {
            if (updatingCombo) {
                return;
            }
            Object selected = endpointCombo.getSelectedItem();
            if (selected instanceof ApiEndpoint endpoint) {
                applyEndpoint(endpoint);
            }
        });
        sendButton.addActionListener(event -> sendRequest());
    }

    /**
     * 展示指定 Controller 的接口列表，并选中 initialIndex 对应的接口。
     */
    public void setEndpoints(@NotNull List<ApiEndpoint> endpoints, int initialIndex) {
        updatingCombo = true;
        endpointCombo.setModel(new DefaultComboBoxModel<>(endpoints.toArray(new ApiEndpoint[0])));
        int index = Math.max(0, Math.min(initialIndex, endpoints.size() - 1));
        endpointCombo.setSelectedIndex(index);
        updatingCombo = false;

        if (!endpoints.isEmpty()) {
            applyEndpoint(endpoints.get(index));
        }
    }

    private void initFields() {
        PropertiesComponent properties = PropertiesComponent.getInstance(project);
        ServerConfigResolver.ServerConfig config = ServerConfigResolver.resolve(project);

        hostField.setText(properties.getValue(KEY_HOST, "localhost"));
        portField.setText(properties.getValue(KEY_PORT, String.valueOf(config.port())));
        contextPathField.setText(properties.getValue(KEY_CONTEXT_PATH, config.contextPath()));
        tokenField.setText(properties.getValue(KEY_TOKEN, ""));

        Font mono = new Font(Font.MONOSPACED, Font.PLAIN, 12);
        bodyArea.setFont(mono);
        bodyArea.setLineWrap(true);
        bodyArea.setWrapStyleWord(true);

        formattedBodyArea.setFont(mono);
        formattedBodyArea.setLineWrap(true);
        formattedBodyArea.setWrapStyleWord(true);
        formattedBodyArea.setEditable(false);

        sourceBodyArea.setFont(mono);
        sourceBodyArea.setLineWrap(false);
        sourceBodyArea.setEditable(false);

        sourceHeadersArea.setFont(mono);
        sourceHeadersArea.setLineWrap(false);
        sourceHeadersArea.setEditable(false);

        responseHeadersTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        bodyCards.add(new JBScrollPane(formattedBodyArea), VIEW_FORMATTED);
        bodyCards.add(new JBScrollPane(sourceBodyArea), VIEW_SOURCE);
        headerCards.add(new JBScrollPane(responseHeadersTable), VIEW_FORMATTED);
        headerCards.add(new JBScrollPane(sourceHeadersArea), VIEW_SOURCE);

        paramsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        headersTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        portField.setPreferredSize(new Dimension(JBUI.scale(80), portField.getPreferredSize().height));
        methodCombo.setPreferredSize(new Dimension(JBUI.scale(96), methodCombo.getPreferredSize().height));
    }

    private void buildUi() {
        JPanel content = new ScrollableColumn(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(JBUI.scale(8), JBUI.scale(8),
                JBUI.scale(8), JBUI.scale(8)));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, JBUI.scale(8), 0);

        constraints.gridy = 0;
        constraints.weighty = 0;
        content.add(createTargetPanel(), constraints);

        constraints.gridy = 1;
        constraints.weighty = 0;
        JComponent requestPanel = createRequestPanel();
        requestPanel.setPreferredSize(new Dimension(JBUI.scale(420), JBUI.scale(220)));
        content.add(requestPanel, constraints);

        constraints.gridy = 2;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 0, 0, 0);
        JComponent responsePanel = createResponsePanel();
        responsePanel.setPreferredSize(new Dimension(JBUI.scale(420), JBUI.scale(320)));
        content.add(responsePanel, constraints);

        JBScrollPane scrollPane = new JBScrollPane(content,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(JBUI.scale(16));
        add(scrollPane, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, JBUI.scale(4), 0));
        actions.add(sendButton);
        add(actions, BorderLayout.SOUTH);
    }

    private @NotNull JComponent createTargetPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, JBUI.scale(4), 0);

        JPanel pathRow = new JPanel(new BorderLayout(JBUI.scale(6), 0));
        pathRow.add(methodCombo, BorderLayout.WEST);
        pathRow.add(pathField, BorderLayout.CENTER);

        JPanel serverRow = new JPanel(new BorderLayout(JBUI.scale(6), 0));
        serverRow.add(hostField, BorderLayout.CENTER);
        serverRow.add(portField, BorderLayout.EAST);

        int row = 0;
        constraints.gridy = row++;
        panel.add(labeled("Endpoint", endpointCombo), constraints);
        constraints.gridy = row++;
        panel.add(labeled("Method / Path", pathRow), constraints);
        constraints.gridy = row++;
        panel.add(labeled("Host / Port", serverRow), constraints);
        constraints.gridy = row++;
        panel.add(labeled("Context path", contextPathField), constraints);
        constraints.gridy = row;
        panel.add(labeled("Authorization (Bearer token)", tokenField), constraints);
        return panel;
    }

    private @NotNull JComponent labeled(@NotNull String title, @NotNull JComponent component) {
        JPanel panel = new JPanel(new BorderLayout(0, JBUI.scale(2)));
        panel.add(new JBLabel(title), BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    private @NotNull JComponent createRequestPanel() {
        JBTabbedPane tabs = new JBTabbedPane();
        tabs.addTab("Params", new JBScrollPane(paramsTable));
        tabs.addTab("Body", new JBScrollPane(bodyArea));
        tabs.addTab("Headers", createHeadersPanel());
        return tabs;
    }

    private @NotNull JComponent createHeadersPanel() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(4), JBUI.scale(2)));
        JButton addButton = new JButton("Add");
        JButton removeButton = new JButton("Remove");
        addButton.addActionListener(event -> headersModel.addRow(new Object[]{"", ""}));
        removeButton.addActionListener(event -> {
            int row = headersTable.getSelectedRow();
            if (row >= 0) {
                headersModel.removeRow(row);
            }
        });
        toolbar.add(addButton);
        toolbar.add(removeButton);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(new JBScrollPane(headersTable), BorderLayout.CENTER);
        return panel;
    }

    private @NotNull JComponent createResponsePanel() {
        JBTabbedPane tabs = new JBTabbedPane();
        tabs.addTab("Response body", bodyCards);
        tabs.addTab("Response headers", headerCards);

        JPanel viewBar = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(4), 0));
        ButtonGroup viewGroup = new ButtonGroup();
        viewGroup.add(formattedViewButton);
        viewGroup.add(sourceViewButton);
        initViewButtons();
        viewBar.add(formattedViewButton);
        viewBar.add(sourceViewButton);

        JPanel header = new JPanel(new BorderLayout(0, JBUI.scale(2)));
        header.add(statusLabel, BorderLayout.NORTH);
        header.add(viewBar, BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout(0, JBUI.scale(4)));
        panel.add(header, BorderLayout.NORTH);
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    private void initViewButtons() {
        formattedViewButton.addActionListener(event -> showView(VIEW_FORMATTED));
        sourceViewButton.addActionListener(event -> showView(VIEW_SOURCE));
    }

    private void showView(@NotNull String card) {
        bodyCardLayout.show(bodyCards, card);
        headerCardLayout.show(headerCards, card);
        if (VIEW_FORMATTED.equals(card)) {
            formattedBodyArea.setCaretPosition(0);
            responseHeadersTable.scrollRectToVisible(new Rectangle(0, 0, 1, 1));
        } else {
            sourceBodyArea.setCaretPosition(0);
            sourceHeadersArea.setCaretPosition(0);
        }
    }

    private void applyEndpoint(@NotNull ApiEndpoint endpoint) {
        methodCombo.setSelectedItem(endpoint.getHttpMethod());
        pathField.setText(endpoint.getPath());

        paramsModel.setRowCount(0);
        for (ApiParameter parameter : endpoint.getParameters()) {
            paramsModel.addRow(new Object[]{
                    parameter.name(),
                    parameter.location().name(),
                    parameter.typeName(),
                    parameter.required() ? "yes" : "no",
                    parameter.defaultValue() == null ? "" : parameter.defaultValue()
            });
        }

        headersModel.setRowCount(0);
        for (ApiParameter parameter : endpoint.getParameters()) {
            if (parameter.location() == ApiParameter.Location.HEADER) {
                headersModel.addRow(new Object[]{
                        parameter.name(),
                        parameter.defaultValue() == null ? "" : parameter.defaultValue()
                });
            }
        }

        bodyArea.setText(endpoint.getBodyTemplate());
        bodyArea.setCaretPosition(0);

        showStatus(endpoint.getMethodName() + " - " + endpoint.getPath(), false);
        resizeParamColumns();
    }

    private void resizeParamColumns() {
        if (paramsTable.getColumnModel().getColumnCount() < PARAM_COLUMNS.length) {
            return;
        }
        paramsTable.getColumnModel().getColumn(0).setPreferredWidth(JBUI.scale(140));
        paramsTable.getColumnModel().getColumn(1).setPreferredWidth(JBUI.scale(60));
        paramsTable.getColumnModel().getColumn(2).setPreferredWidth(JBUI.scale(110));
        paramsTable.getColumnModel().getColumn(3).setPreferredWidth(JBUI.scale(60));
        paramsTable.getColumnModel().getColumn(4).setPreferredWidth(JBUI.scale(160));
    }

    private void sendRequest() {
        if (sending) {
            return;
        }
        if (paramsTable.isEditing()) {
            paramsTable.getCellEditor().stopCellEditing();
        }
        if (headersTable.isEditing()) {
            headersTable.getCellEditor().stopCellEditing();
        }
        persistFields();

        String method = String.valueOf(methodCombo.getSelectedItem());
        String path = text(pathField);
        if (path.isEmpty()) {
            showStatus("Path is empty.", true);
            return;
        }

        Map<String, String> pathValues = new LinkedHashMap<>();
        Map<String, String> queryParams = new LinkedHashMap<>();
        Map<String, String> headerValues = new LinkedHashMap<>();
        List<String> cookies = new ArrayList<>();

        for (int row = 0; row < paramsModel.getRowCount(); row++) {
            String name = cell(paramsModel, row, 0);
            String location = cell(paramsModel, row, 1);
            String value = cell(paramsModel, row, 4);
            if (name.isEmpty()) {
                continue;
            }
            switch (location) {
                case "PATH" -> pathValues.put(name, value);
                case "QUERY" -> {
                    if (!value.isEmpty()) {
                        queryParams.put(name, value);
                    }
                }
                case "HEADER" -> {
                    if (!value.isEmpty()) {
                        headerValues.put(name, value);
                    }
                }
                case "COOKIE" -> {
                    if (!value.isEmpty()) {
                        cookies.add(name + "=" + value);
                    }
                }
                default -> {
                    // 未知位置忽略
                }
            }
        }

        for (Map.Entry<String, String> entry : pathValues.entrySet()) {
            if (entry.getValue().isEmpty()) {
                showStatus("Path parameter '" + entry.getKey() + "' is empty.", true);
                return;
            }
            path = path.replace("{" + entry.getKey() + "}", encode(entry.getValue()));
        }
        if (path.contains("{")) {
            showStatus("Unresolved path parameter in: " + path, true);
            return;
        }

        String url = buildBaseUrl() + (path.startsWith("/") ? path : "/" + path);
        if (!queryParams.isEmpty()) {
            List<String> parts = new ArrayList<>();
            queryParams.forEach((name, value) -> parts.add(encode(name) + "=" + encode(value)));
            url = url + "?" + String.join("&", parts);
        }

        Map<String, String> headers = new LinkedHashMap<>(headerValues);
        for (int row = 0; row < headersModel.getRowCount(); row++) {
            String name = cell(headersModel, row, 0);
            String value = cell(headersModel, row, 1);
            if (!name.isEmpty()) {
                headers.put(name, value);
            }
        }
        if (!cookies.isEmpty()) {
            putIfAbsent(headers, "Cookie", String.join("; ", cookies));
        }

        String token = text(tokenField);
        if (!token.isEmpty() && !containsHeader(headers, "Authorization")) {
            headers.put("Authorization", "Bearer " + token);
        }

        String body = bodyArea.getText();
        if (!"POST".equals(method) && !"PUT".equals(method) && !"PATCH".equals(method)) {
            body = "";
        }
        if (body != null && !body.isBlank() && !containsHeader(headers, "Content-Type")) {
            headers.put("Content-Type", "application/json; charset=UTF-8");
        }

        String requestUrl = url;
        sending = true;
        sendButton.setEnabled(false);
        showStatus("Sending " + method + " " + requestUrl + " ...", false);

        HttpRequestService.send(new HttpRequestService.Request(method, requestUrl, headers, body))
                .thenAccept(response -> ApplicationManager.getApplication().invokeLater(() -> {
                    sending = false;
                    sendButton.setEnabled(true);
                    if (!isShowing()) {
                        return;
                    }
                    showResponse(requestUrl, response);
                }));
    }

    private void showResponse(@NotNull String url, @NotNull HttpRequestService.Response response) {
        if (response.error() != null) {
            showStatus("Request failed: " + response.error(), true);
            formattedBodyArea.setText("");
            sourceBodyArea.setText("");
            responseHeadersModel.setRowCount(0);
            sourceHeadersArea.setText("");
            return;
        }

        String reason = response.reasonPhrase();
        String status = "HTTP " + response.statusCode()
                + (reason == null || reason.isBlank() ? "" : " " + reason)
                + "  |  " + response.elapsedMillis() + " ms  |  " + url;
        showStatus(status, response.statusCode() >= 400);

        String rawBody = response.body() == null ? "" : response.body();
        formattedBodyArea.setText(HttpRequestService.prettyPrint(rawBody));
        formattedBodyArea.setCaretPosition(0);
        sourceBodyArea.setText(rawBody);
        sourceBodyArea.setCaretPosition(0);

        responseHeadersModel.setRowCount(0);
        StringBuilder rawHeaders = new StringBuilder();
        response.headers().forEach((name, values) -> {
            for (String value : values) {
                responseHeadersModel.addRow(new Object[]{name, value});
                rawHeaders.append(name).append(": ").append(value).append('\n');
            }
        });
        sourceHeadersArea.setText(rawHeaders.toString());
        sourceHeadersArea.setCaretPosition(0);
        resizeResponseHeaderColumns();
    }

    private void resizeResponseHeaderColumns() {
        if (responseHeadersTable.getColumnModel().getColumnCount() < RESPONSE_HEADER_COLUMNS.length) {
            return;
        }
        responseHeadersTable.getColumnModel().getColumn(0).setPreferredWidth(JBUI.scale(150));
        responseHeadersTable.getColumnModel().getColumn(1).setPreferredWidth(JBUI.scale(250));
    }

    private @NotNull String buildBaseUrl() {
        String host = text(hostField);
        String port = text(portField);
        String contextPath = text(contextPathField);
        if (host.isEmpty()) {
            host = "localhost";
        }

        String scheme = "http://";
        if (host.startsWith("http://")) {
            host = host.substring("http://".length());
        } else if (host.startsWith("https://")) {
            host = host.substring("https://".length());
            scheme = "https://";
        }
        while (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }

        String base = scheme + host + (port.isEmpty() ? "" : ":" + port);
        if (!contextPath.isEmpty() && !"/".equals(contextPath)) {
            String normalized = contextPath.startsWith("/") ? contextPath : "/" + contextPath;
            while (normalized.endsWith("/")) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            base = base + normalized;
        }
        return base;
    }

    private void persistFields() {
        if (project.isDisposed()) {
            return;
        }
        PropertiesComponent properties = PropertiesComponent.getInstance(project);
        properties.setValue(KEY_HOST, text(hostField));
        properties.setValue(KEY_PORT, text(portField));
        properties.setValue(KEY_CONTEXT_PATH, text(contextPathField));
        properties.setValue(KEY_TOKEN, text(tokenField));
    }

    private void showStatus(@NotNull String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.setToolTipText(message);
        statusLabel.setForeground(error ? JBColor.RED : null);
    }

    @Override
    public void dispose() {
        persistFields();
    }

    private static @NotNull String cell(@NotNull DefaultTableModel model, int row, int column) {
        Object value = model.getValueAt(row, column);
        return value == null ? "" : value.toString().trim();
    }

    private static boolean containsHeader(@NotNull Map<String, String> headers, @NotNull String name) {
        for (String key : headers.keySet()) {
            if (key.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private static void putIfAbsent(@NotNull Map<String, String> headers,
                                    @NotNull String name,
                                    @NotNull String value) {
        if (!containsHeader(headers, name)) {
            headers.put(name, value);
        }
    }

    private static @NotNull String encode(@NotNull String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static @NotNull String text(@NotNull JTextField field) {
        String value = field.getText();
        return value == null ? "" : value.trim();
    }

    /**
     * 宽度跟随视口、高度按内容自适应，使整个面板可以纵向滚动。
     */
    private static final class ScrollableColumn extends JPanel implements Scrollable {

        private ScrollableColumn(@NotNull LayoutManager layout) {
            super(layout);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(@NotNull Rectangle visibleRect, int orientation, int direction) {
            return JBUI.scale(16);
        }

        @Override
        public int getScrollableBlockIncrement(@NotNull Rectangle visibleRect, int orientation, int direction) {
            return Math.max(JBUI.scale(16), visibleRect.height - JBUI.scale(16));
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
