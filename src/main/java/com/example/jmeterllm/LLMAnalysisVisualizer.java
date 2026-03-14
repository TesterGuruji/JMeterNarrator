package com.example.jmeterllm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.visualizers.gui.AbstractVisualizer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * JMeter visualizer that aggregates results and calls Gemini for analysis.
 */
public class LLMAnalysisVisualizer extends AbstractVisualizer {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ResultsAggregator aggregator = new ResultsAggregator();

    private JTextArea analysisArea;
    private JTextField modelField;
    private JTextField jtlPathField;
    private JCheckBox useJtlCheckbox;

    public LLMAnalysisVisualizer() {
        super();
        init();
    }

    @Override
    public String getLabelResource() {
        // No resource bundle, use static label instead.
        return null;
    }

    public String getStaticLabel() {
        return "LLM Performance Analysis (Gemini)";
    }

    @Override
    public void add(SampleResult sample) {
        aggregator.add(sample);
    }

    @Override
    public void clearData() {
        aggregator.clear();
        if (analysisArea != null) {
            analysisArea.setText("");
        }
    }

    @Override
    public TestElement createTestElement() {
        TestElement el = super.createTestElement();
        modifyTestElement(el);
        return el;
    }

    @Override
    public void modifyTestElement(TestElement te) {
        super.configureTestElement(te);
    }

    private void init() {
        setLayout(new BorderLayout());
        setBorder(makeBorder());
        add(makeTitlePanel(), BorderLayout.NORTH);
        add(buildMainPanel(), BorderLayout.CENTER);
    }

    private JPanel buildMainPanel() {
        JPanel main = new VerticalPanel();

        // Configuration panel
        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        configPanel.setBorder(BorderFactory.createTitledBorder("LLM Configuration"));

        configPanel.add(new JLabel("Model:"));
        modelField = new JTextField(18);
        String defaultModel = JMeterUtils.getPropDefault("gemini.model", "gemini-1.5-flash");
        modelField.setText(defaultModel);
        configPanel.add(modelField);

        JButton analyzeButton = new JButton("Run Analysis Now");
        analyzeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runAnalysis();
            }
        });
        configPanel.add(analyzeButton);

        main.add(configPanel);

        // JTL input panel
        JPanel jtlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        jtlPanel.setBorder(BorderFactory.createTitledBorder("JTL Input (optional)"));

        useJtlCheckbox = new JCheckBox("Use JTL file for analysis");
        jtlPanel.add(useJtlCheckbox);

        jtlPanel.add(new JLabel("JTL file path:"));
        jtlPathField = new JTextField(30);
        jtlPanel.add(jtlPathField);

        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                int result = chooser.showOpenDialog(LLMAnalysisVisualizer.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    jtlPathField.setText(chooser.getSelectedFile().getAbsolutePath());
                }
            }
        });
        jtlPanel.add(browseButton);

        main.add(jtlPanel);

        // Output panel
        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBorder(BorderFactory.createTitledBorder("LLM Analysis Output (Markdown)"));

        analysisArea = new JTextArea();
        analysisArea.setEditable(false);
        analysisArea.setLineWrap(true);
        analysisArea.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(analysisArea);
        scroll.setPreferredSize(new Dimension(800, 400));
        outputPanel.add(scroll, BorderLayout.CENTER);

        main.add(outputPanel);

        return main;
    }

    private void runAnalysis() {
        try {
            ResultsAggregator sourceAgg;

            if (useJtlCheckbox != null && useJtlCheckbox.isSelected()) {
                String path = jtlPathField != null ? jtlPathField.getText().trim() : "";
                if (path.isEmpty()) {
                    throw new IllegalArgumentException("Please select a JTL file path or uncheck 'Use JTL file for analysis'.");
                }
                sourceAgg = JtlCsvLoader.load(new java.io.File(path));
                sourceAgg.setTestName("JTL File: " + path);
            } else {
                aggregator.setTestName(getName());
                sourceAgg = aggregator;
            }

            MetricsSummary summary = MetricsSummary.fromAggregator(sourceAgg);
            String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(summary);

            String apiKey = GeminiClient.resolveApiKey();
            String model = modelField.getText().trim().isEmpty()
                    ? "gemini-1.5-flash"
                    : modelField.getText().trim();

            GeminiClient client = new GeminiClient(apiKey, model);

            analysisArea.setText("Calling Gemini model '" + model + "'...\n\n");
            String analysis = client.analyze(json);
            analysisArea.setText(analysis);
            analysisArea.setCaretPosition(0);
        } catch (Exception ex) {
            analysisArea.setText("Error during analysis:\n" + ex.getMessage());
        }
    }
}

