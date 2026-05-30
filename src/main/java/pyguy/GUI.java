package pyguy;

import com.formdev.flatlaf.*;
import com.formdev.flatlaf.extras.*;
import com.formdev.flatlaf.extras.components.FlatButton;
import com.formdev.flatlaf.themes.*;
import com.formdev.flatlaf.ui.FlatButtonBorder;
import com.google.gson.*;
import pyguy.types.*;
import pyguy.util.Utils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.*;

// TODO Help?
public class GUI
{
    private record Header(JPanel header, JLabel titleLabel) {}

    // CONSTANTS
    private static final FileNameExtensionFilter PAIMON_FILE_FILTER = new FileNameExtensionFilter("JSON Files", "json");
    private static final FileNameExtensionFilter VIDEO_FILE_FILTER  = new FileNameExtensionFilter("Video Files", "mp4", "mov", "mkv", "avi"); // These are just the "common" ones but FFmpeg will happily read a car as a video file so IDK if it's necessary.

    private static final KeyStroke ESCAPE_KEY = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

    private static final String ICON = "icon", ICON_DARK = "icon-dark";

    private static final String         HELP_PAGES_FILE = "/help/help.json";
    private static final List<HelpPage> HELP_PAGES_DARK = new ArrayList<>(), HELP_PAGES_LIGHT = new ArrayList<>();

    private static float REM = 1f;

    // STATE
    private static final AtomicReference<File>     saveFile         = new AtomicReference<>(null);
    private static       List<String>              accountNames     = null;
    private static final AtomicInteger             selectedAccount  = new AtomicInteger(-1);
    private static final AtomicReference<Database> database         = new AtomicReference<>(null);
    private static final AtomicInteger             selectedCategory = new AtomicInteger(-1);
    private static final AtomicBoolean             mergeCategory    = new AtomicBoolean(true);
    private static final AtomicReference<File>     videoFile        = new AtomicReference<>(null);
    private static final AtomicReference<File>     outputFile       = new AtomicReference<>(null);

    private static AtomicBoolean scanning = new AtomicBoolean(false); // If false, start/cancel button starts, otherwise cancels.

    private static final Map<JComponent, Boolean> temporaryDisableRestoreStates = new HashMap<>();

    private static final List<Runnable> appModeChangeSubscribeList = new ArrayList<>();

    private static Taskbar taskbar = null;

    private static BufferedImage helpPanelImage = null;

    // ELEMENTS
    private static JFrame window;
    private static JPanel mainPanel;

    // Paimon Panel
    private static JPanel paimonPanel;
    private static JPanel paimonFirstPanel;
    private static JPanel paimonSecondPanel;
    private static JPanel paimonThirdPanel;
    private static JPanel paimonFourthPanel;

    private static JLabel            paimonFileInputHintLabel;
    private static JTextField        paimonFileInputField;
    private static JButton           paimonFileChooserButton;
    private static JLabel            paimonAccountChooserHintLabel;
    private static JComboBox<String> paimonAccountChooser;
    private static JLabel            onlineDatabaseCategoryHintLabel;
    private static JLabel            onlineDatabaseHintLabel;
    private static JLabel            onlineDatabaseStatusLabel;
    private static JButton           onlineDatabaseSyncButton;
    private static JComboBox<String> onlineDatabaseCategoryChooser;
    private static JLabel            saveFileRadioHintLabel;
    private static ButtonGroup       saveFileRadioGroup;
    private static JRadioButton      saveFileMergeRadio, saveFileReplaceRadio;

    // Video Panel
    private static JPanel videoPanel;
    private static JPanel videoTopPanel;

    private static JLabel     videoChooserHintLabel;
    private static JTextField videoChooserInputField;
    private static JButton    videoChooserButton;

    // Output Panel
    private static JPanel outputPanel;
    private static JPanel outputTopPanel;

    private static JLabel     outputChooserHintLabel;
    private static JTextField outputChooserInputField;
    private static JButton    outputChooserButton;

    // Results Panel
    private static JPanel scanResultsPanel;

    private static JList<Achievement> scanResultsList;
    private static JScrollPane        scanResultsScrollPane;

    // Scan Panel
    private static JPanel scanPanel;
    private static JPanel scanHorizontalPanel;

    private static JProgressBar scanProgressBar;
    private static JButton      startCancelScanButton;

    // Extras Panel
    private static JPanel extrasPanel;

    private static JButton settingsButton;
    private static JButton darkLightButton;
    private static JButton helpButton;
    private static JButton exitButton;

    // Settings Popup
    private static JDialog settingsDialog;
    private static JPanel  mainSettingsPanel;

    private static JPanel  languagePickerPanel;
    private static JPanel  threadsSliderPanel;

    private static JLabel                       languagePickerHintLabel;
    private static JComboBox<Settings.Language> languagePicker;
    private static JLabel                       threadsSliderHintLabel;
    private static JSlider                      threadsSlider;

    // Help Popup
    private static JDialog helpDialog;
    private static JPanel  mainHelpPanel;

    private static JPanel      helpImagePanel;
    private static JScrollPane helpTextScrollPane;
    private static JPanel      helpTextPanel;
    private static JPanel      helpImageTextPanel;
    private static JPanel      helpButtonsPanel;

    private static JLabel helpTitleLabel;
    private static JLabel helpImageLabel;

    private static JButton helpLeftButton;
    private static JLabel  helpPageLabel;
    private static JButton helpRightButton;


    // BUILDERS
    private static void ApplyCloseOnEsc(JDialog dialog)
    {
        JRootPane rootPane = dialog.getRootPane();

        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ESCAPE_KEY, "ESC");

        rootPane.getActionMap().put("ESC", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                dialog.dispose();
            }
        });
    }

    private static Header BuildHeader(String title)
    {
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.X_AXIS));
        headerPanel.setAlignmentY(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        Font defaultFont = titleLabel.getFont();
        titleLabel.setFont(defaultFont.deriveFont(Font.PLAIN, defaultFont.getSize() + 4));

        headerPanel.add(titleLabel);

        headerPanel.add(Box.createHorizontalStrut(5));

        JPanel separator = new JPanel();
        separator.setAlignmentY(Component.CENTER_ALIGNMENT);
        separator.setBorder(
            new CompoundBorder(
                new EmptyBorder(4, 0, 0, 0),
                new MatteBorder(0, 0, 1, 0, titleLabel.getForeground())
            )
        );
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));

        appModeChangeSubscribeList.add(() -> separator.setBorder(
            new CompoundBorder(
                new EmptyBorder(4, 0, 0, 0),
                new MatteBorder(0, 0, 1, 0, titleLabel.getForeground())
            ))
        );

        headerPanel.add(separator);

        headerPanel.setMaximumSize(
            new Dimension(
                Integer.MAX_VALUE,
                headerPanel.getPreferredSize().height
            )
        );

        return new Header(headerPanel, titleLabel);
    }

    private static JButton BuildRoundButton(String iconName, String tooltip)
    {
        Color defaultColor = new JLabel().getForeground();

        FlatButtonBorder border = new FlatButtonBorder() {{
            arc = Integer.MAX_VALUE;
        }};

        FlatSVGIcon icon = new FlatSVGIcon(iconName, (int) (REM * 1.5), (int) (REM * 1.5));
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> defaultColor));

        FlatButton button = new FlatButton();

        button.setIcon(icon);
        button.setToolTipText(tooltip);
        button.setBorder(border);
        button.setRequestFocusEnabled(false);

        appModeChangeSubscribeList.add(() -> {
            button.setBorder(border);

            icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> new JLabel().getForeground()));
        });

        return button;
    }

    private static JButton BuildRoundMorphButton(String iconNameLight, String iconNameDark, String tooltip)
    {
        JButton button = BuildRoundButton(Settings.GetAppMode() == Settings.AppMode.DARK ? iconNameDark : iconNameLight, tooltip);

        appModeChangeSubscribeList.add(() -> {
            FlatSVGIcon icon = new FlatSVGIcon(Settings.GetAppMode() == Settings.AppMode.DARK ? iconNameDark : iconNameLight, (int) (REM * 1.5), (int) (REM * 1.5));
            icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> new JLabel().getForeground()));

            button.setIcon(icon);
        });

        return button;
    }

    private static void BuildPaimonPanel()
    {
        paimonPanel = new JPanel();
        paimonPanel.setLayout(new BoxLayout(paimonPanel, BoxLayout.Y_AXIS));

        paimonFirstPanel = new JPanel();
        paimonFirstPanel.setLayout(new BoxLayout(paimonFirstPanel, BoxLayout.X_AXIS));
        paimonFirstPanel.setAlignmentY(Component.CENTER_ALIGNMENT);

        paimonSecondPanel = new JPanel();
        paimonSecondPanel.setLayout(new BoxLayout(paimonSecondPanel, BoxLayout.X_AXIS));
        paimonSecondPanel.setAlignmentY(Component.CENTER_ALIGNMENT);

        paimonThirdPanel = new JPanel();
        paimonThirdPanel.setLayout(new BoxLayout(paimonThirdPanel, BoxLayout.X_AXIS));
        paimonThirdPanel.setAlignmentY(Component.CENTER_ALIGNMENT);

        paimonFourthPanel = new JPanel();
        paimonFourthPanel.setLayout(new BoxLayout(paimonFourthPanel, BoxLayout.X_AXIS));
        paimonFourthPanel.setAlignmentY(Component.CENTER_ALIGNMENT);

        // SAVE FILE LABEL
        paimonFileInputHintLabel = new JLabel(I18N.GetString("save-file"));
        paimonFirstPanel.add(paimonFileInputHintLabel);

        paimonFirstPanel.add(Box.createHorizontalStrut(5));

        // TEXT ""INPUT""
        paimonFileInputField = new JTextField(I18N.GetString("no-file"));
        paimonFileInputField.setEnabled(false);

        paimonFirstPanel.add(paimonFileInputField);

        paimonFirstPanel.add(Box.createHorizontalStrut(5));

        // FILE CHOOSER BUTTON
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(PAIMON_FILE_FILTER);

        paimonFileChooserButton = new JButton(I18N.GetString("select"));
        paimonFileChooserButton.setRequestFocusEnabled(false);
        paimonFileChooserButton.setToolTipText(I18N.GetString("save-file-tooltip"));
        paimonFileChooserButton.setPreferredSize(
            new Dimension(paimonFileChooserButton.getPreferredSize().width,
                paimonFileInputField.getPreferredSize().height
            )
        );

        paimonFirstPanel.add(paimonFileChooserButton);

        // ACCOUNT CHOOSER LABEL
        paimonAccountChooserHintLabel = new JLabel(I18N.GetString("account"));
        paimonAccountChooserHintLabel.setEnabled(false);
        paimonAccountChooserHintLabel.setPreferredSize(paimonFileInputHintLabel.getPreferredSize());
        paimonSecondPanel.add(paimonAccountChooserHintLabel);

        paimonSecondPanel.add(Box.createHorizontalStrut(5));

        // ACCOUNT CHOOSER
        paimonAccountChooser = new JComboBox<>();
        paimonAccountChooser.setRequestFocusEnabled(false);
        paimonAccountChooser.setEnabled(false);
        paimonAccountChooser.addItemListener(e -> selectedAccount.set(paimonAccountChooser.getSelectedIndex()));

        paimonSecondPanel.add(paimonAccountChooser);

        // ONLINE DATABASE LABEL
        onlineDatabaseHintLabel = new JLabel(I18N.GetString("online-database"));
        paimonThirdPanel.add(onlineDatabaseHintLabel);

        paimonThirdPanel.add(Box.createHorizontalStrut(5));

        // ONLINE DATABASE STATUS LABEL
        onlineDatabaseStatusLabel = new JLabel(I18N.GetString("syncing"));
        onlineDatabaseStatusLabel.setEnabled(false);
        paimonThirdPanel.add(onlineDatabaseStatusLabel);

        paimonThirdPanel.add(Box.createHorizontalStrut(5));

        // ONLINE DATABASE SYNC BUTTON
        onlineDatabaseSyncButton = new JButton(I18N.GetString("sync"));
        onlineDatabaseSyncButton.setRequestFocusEnabled(false);
        onlineDatabaseSyncButton.setToolTipText(I18N.GetString("sync-tooltip"));
        onlineDatabaseSyncButton.setPreferredSize(
            new Dimension(onlineDatabaseSyncButton.getPreferredSize().width,
                paimonFileInputField.getPreferredSize().height
            )
        );
        onlineDatabaseSyncButton.addActionListener(e -> SyncDatabase(true, true));
        paimonThirdPanel.add(onlineDatabaseSyncButton);

        paimonThirdPanel.add(Box.createHorizontalStrut(10));

        // ONLINE DATABASE CATEGORY CHOOSER LABEL
        onlineDatabaseCategoryHintLabel = new JLabel(I18N.GetString("category"));
        onlineDatabaseCategoryHintLabel.setEnabled(false);
        paimonThirdPanel.add(onlineDatabaseCategoryHintLabel);

        paimonThirdPanel.add(Box.createHorizontalStrut(5));

        // ONLINE DATABASE CATEGORY CHOOSER
        onlineDatabaseCategoryChooser = new JComboBox<>();
        onlineDatabaseCategoryChooser.setRequestFocusEnabled(false);
        onlineDatabaseCategoryChooser.setEnabled(false);
        onlineDatabaseCategoryChooser.setPreferredSize(
            new Dimension(400,
                onlineDatabaseCategoryChooser.getPreferredSize().height
            )
        );
        onlineDatabaseCategoryChooser.addItemListener(e -> selectedCategory.set(onlineDatabaseCategoryChooser.getSelectedIndex()));

        paimonThirdPanel.add(onlineDatabaseCategoryChooser);

        // SAVE FILE ACHIEVEMENTS RADIO LABEL
        saveFileRadioHintLabel = new JLabel(I18N.GetString("save-file-achievements"));
        paimonFourthPanel.add(saveFileRadioHintLabel);

        paimonFourthPanel.add(Box.createHorizontalStrut(5));

        // SAVE FILE ACHIEVEMENTS RADIO BUTTONS
        saveFileRadioGroup = new ButtonGroup();

        saveFileMergeRadio = new JRadioButton(I18N.GetString("merge"));
        saveFileMergeRadio.setRequestFocusEnabled(false);
        saveFileMergeRadio.addActionListener(l -> mergeCategory.set(true));
        saveFileRadioGroup.add(saveFileMergeRadio);

        saveFileReplaceRadio = new JRadioButton(I18N.GetString("replace"));
        saveFileReplaceRadio.setRequestFocusEnabled(false);
        saveFileReplaceRadio.addActionListener(l -> mergeCategory.set(false));
        saveFileRadioGroup.add(saveFileReplaceRadio);

        saveFileMergeRadio.setSelected(true);

        paimonFourthPanel.add(saveFileMergeRadio);
        paimonFourthPanel.add(Box.createHorizontalStrut(5));
        paimonFourthPanel.add(saveFileReplaceRadio);
        paimonFourthPanel.add(Box.createHorizontalGlue());

        // FILE BUTTON ACTION
        ActionListener fileChooserButtonActionListener = e -> {
            int result = fileChooser.showOpenDialog(window);

            if (result == JFileChooser.APPROVE_OPTION)
            {
                File file = fileChooser.getSelectedFile();

                if (file != null && file.exists())
                {
                    accountNames = SaveFileTools.GetPaimonDatabaseAccounts(file);

                    if (accountNames == null) // Save file invalid
                    {
                        SetSaveFile(null);

                        JOptionPane.showMessageDialog(window, I18N.GetString("save-file-invalid"), I18N.GetString("error"), JOptionPane.ERROR_MESSAGE);
                    }
                    else
                        SetSaveFile(file);
                }
            }
        };
        paimonFileChooserButton.addActionListener(fileChooserButtonActionListener);

        // PANEL
        paimonPanel.add(BuildHeader(I18N.GetString("paimon-section-title")).header);
        paimonPanel.add(Box.createVerticalStrut(10));
        paimonPanel.add(paimonFirstPanel);
        paimonPanel.add(Box.createVerticalStrut(5));
        paimonPanel.add(paimonSecondPanel);
        paimonPanel.add(Box.createVerticalStrut(5));
        paimonPanel.add(paimonThirdPanel);
        paimonPanel.add(Box.createVerticalStrut(5));
        paimonPanel.add(paimonFourthPanel);

        paimonPanel.setMaximumSize(
            new Dimension(
                Integer.MAX_VALUE,
                paimonPanel.getPreferredSize().height
            )
        );
    }

    private static void BuildVideoPanel()
    {
        videoPanel = new JPanel();
        videoPanel.setLayout(new BoxLayout(videoPanel, BoxLayout.Y_AXIS));

        videoTopPanel = new JPanel();
        videoTopPanel.setLayout(new BoxLayout(videoTopPanel, BoxLayout.X_AXIS));
        videoTopPanel.setAlignmentY(Component.CENTER_ALIGNMENT);

        // VIDEO CHOOSER LABEL
        videoChooserHintLabel = new JLabel(I18N.GetString("video-file"));
        videoTopPanel.add(videoChooserHintLabel);

        videoTopPanel.add(Box.createHorizontalStrut(5));

        // TEXT ""INPUT""
        videoChooserInputField = new JTextField(I18N.GetString("no-file"));
        videoChooserInputField.setEnabled(false);

        videoTopPanel.add(videoChooserInputField);

        videoTopPanel.add(Box.createHorizontalStrut(5));

        // FILE CHOOSER BUTTON
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(VIDEO_FILE_FILTER);

        videoChooserButton = new JButton(I18N.GetString("select"));
        videoChooserButton.setRequestFocusEnabled(false);
        videoChooserButton.setToolTipText(I18N.GetString("video-file-tooltip"));
        videoChooserButton.setPreferredSize(new Dimension(videoChooserButton.getPreferredSize().width, videoChooserInputField.getPreferredSize().height));

        videoTopPanel.add(videoChooserButton);

        // FILE BUTTON ACTION
        ActionListener fileChooserButtonActionListener = e -> {
            int result = fileChooser.showOpenDialog(window);

            if (result == JFileChooser.APPROVE_OPTION)
            {
                File file = fileChooser.getSelectedFile();

                if (file != null && file.exists())
                {
                    AchievementScanner.VideoValidity videoValidity = AchievementScanner.IsVideoValid(file);

                    if (videoValidity.valid())
                    {
                        SetVideoFile(file);

                        if (videoValidity.reason() != null)
                            JOptionPane.showMessageDialog(window, videoValidity.reason(), I18N.GetString("warning"), JOptionPane.WARNING_MESSAGE);
                    }
                    else
                    {
                        SetVideoFile(null);

                        JOptionPane.showMessageDialog(window, I18N.GetString("video-file-invalid") + " " + videoValidity.reason(), I18N.GetString("error"), JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        };
        videoChooserButton.addActionListener(fileChooserButtonActionListener);

        // PANEL
        videoPanel.add(BuildHeader(I18N.GetString("video-section-title")).header);
        videoPanel.add(Box.createVerticalStrut(10));
        videoPanel.add(videoTopPanel);

        videoPanel.setMaximumSize(
            new Dimension(
                Integer.MAX_VALUE,
                videoPanel.getPreferredSize().height
            )
        );
    }

    private static void BuildOutputPanel()
    {
        outputPanel = new JPanel();
        outputPanel.setLayout(new BoxLayout(outputPanel, BoxLayout.Y_AXIS));

        outputTopPanel = new JPanel();
        outputTopPanel.setLayout(new BoxLayout(outputTopPanel, BoxLayout.X_AXIS));
        outputTopPanel.setAlignmentY(Component.CENTER_ALIGNMENT);

        // OUTPUT CHOOSER LABEL
        outputChooserHintLabel = new JLabel(I18N.GetString("output-file"));
        outputTopPanel.add(outputChooserHintLabel);

        outputTopPanel.add(Box.createHorizontalStrut(5));

        // TEXT ""INPUT""
        outputChooserInputField = new JTextField(I18N.GetString("no-file"));
        outputChooserInputField.setEnabled(false);

        outputTopPanel.add(outputChooserInputField);

        outputTopPanel.add(Box.createHorizontalStrut(5));

        // FILE CHOOSER BUTTON
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(PAIMON_FILE_FILTER);

        outputChooserButton = new JButton(I18N.GetString("select"));
        outputChooserButton.setRequestFocusEnabled(false);
        outputChooserButton.setToolTipText(I18N.GetString("output-file-tooltip"));
        outputChooserButton.setPreferredSize(
            new Dimension(
                outputChooserButton.getPreferredSize().width,
                outputChooserInputField.getPreferredSize().height
            )
        );

        outputTopPanel.add(outputChooserButton);

        // FILE BUTTON ACTION
        ActionListener fileChooserButtonActionListener = e -> {
            int result = fileChooser.showSaveDialog(window);

            if (result == JFileChooser.APPROVE_OPTION)
            {
                File file = fileChooser.getSelectedFile();

                if (file != null)
                {
                    if (!file.getName().toLowerCase().endsWith(".json"))
                        file = new File(file.getAbsolutePath() + ".json");

                    boolean valid = true;
                    if (file.exists())
                    {
                        valid = false;

                        int choice = JOptionPane.showConfirmDialog(window, I18N.GetString("output-file-exists"), I18N.GetString("warning"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

                        if (choice == JOptionPane.YES_OPTION)
                            valid = true;
                    }

                    if (valid)
                        SetOutputFile(file);
                    else
                        SetOutputFile(null);

                    UpdateProgressBarTextAndStartButton();
                }

                UpdateProgressBarTextAndStartButton();
            }
        };
        outputChooserButton.addActionListener(fileChooserButtonActionListener);

        // PANEL
        outputPanel.add(BuildHeader(I18N.GetString("output-section-title")).header);
        outputPanel.add(Box.createVerticalStrut(10));
        outputPanel.add(outputTopPanel);

        outputPanel.setMaximumSize(
            new Dimension(
                Integer.MAX_VALUE,
                outputPanel.getPreferredSize().height
            )
        );
    }

    private static void BuildResultsPanel()
    {
        scanResultsPanel = new JPanel();
        scanResultsPanel.setLayout(new BoxLayout(scanResultsPanel, BoxLayout.Y_AXIS));

        // ACHIEVEMENT LIST
        scanResultsList = new JList<>(new DefaultListModel<>());
        scanResultsList.setCellRenderer(new DefaultListCellRenderer()
        {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
            {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                Achievement achievement = (Achievement) value;

                label.setText(achievement.achievementIDs().length + "⭐ " + achievement.name());

                return label;
            }
        });
        scanResultsList.setBorder(new EmptyBorder(5, 0, 5, 0));

        scanResultsScrollPane = new JScrollPane(scanResultsList);
        scanResultsScrollPane.setMinimumSize(new Dimension(Integer.MAX_VALUE, 80));

        // PANEL
        scanResultsPanel.add(BuildHeader(I18N.GetString("results-section-title")).header);
        scanResultsPanel.add(Box.createVerticalStrut(10));
        scanResultsPanel.add(scanResultsScrollPane);
    }

    private static void BuildScanPanel()
    {
        scanPanel = new JPanel();
        scanPanel.setLayout(new BoxLayout(scanPanel, BoxLayout.Y_AXIS));

        scanHorizontalPanel = new JPanel();
        scanHorizontalPanel.setLayout(new BoxLayout(scanHorizontalPanel, BoxLayout.X_AXIS));
        scanHorizontalPanel.setAlignmentY(Component.CENTER_ALIGNMENT);

        // PROGRESS BAR
        scanProgressBar = new JProgressBar();
        scanProgressBar.setStringPainted(true);
        scanProgressBar.setString(I18N.GetString("missing-save-file"));
        scanHorizontalPanel.add(scanProgressBar);

        scanHorizontalPanel.add(Box.createHorizontalStrut(5));

        // START/CANCEL BUTTON
        startCancelScanButton = new JButton(I18N.GetString("start"));
        startCancelScanButton.setRequestFocusEnabled(false);
        startCancelScanButton.setEnabled(false);
        startCancelScanButton.addActionListener(l ->
        {
            if (scanning.get())
                StopScan(false);
            else
                StartScan();
        });
        scanHorizontalPanel.add(startCancelScanButton);

        Dimension barMaxSize = new Dimension(Integer.MAX_VALUE, startCancelScanButton.getPreferredSize().height);
        scanProgressBar.setMaximumSize(barMaxSize);
        scanProgressBar.setPreferredSize(new Dimension(scanProgressBar.getPreferredSize().width, barMaxSize.height));

        // PANEL
        scanPanel.add(BuildHeader(I18N.GetString("scan-section-title")).header);
        scanPanel.add(Box.createVerticalStrut(10));
        scanPanel.add(scanHorizontalPanel);

        scanPanel.setMaximumSize(
            new Dimension(
                Integer.MAX_VALUE,
                scanPanel.getPreferredSize().height
            )
        );
    }

    private static void BuildExtrasPanel()
    {
        extrasPanel = new JPanel();
        extrasPanel.setLayout(new BoxLayout(extrasPanel, BoxLayout.X_AXIS));
        extrasPanel.setAlignmentY(Component.CENTER_ALIGNMENT);

        // SETTINGS BUTTON
        settingsButton = BuildRoundButton("svg/gear.svg", I18N.GetString("settings"));
        settingsButton.addActionListener(l -> OpenSettingsGUI());
        extrasPanel.add(settingsButton);

        extrasPanel.add(Box.createHorizontalStrut(5));

        // DARK LIGHT TOGGLE BUTTON
        darkLightButton = BuildRoundMorphButton("svg/sun.svg", "svg/moon.svg", I18N.GetString("light-dark-toggle"));
        darkLightButton.addActionListener(l -> SetAppMode(Settings.GetAppMode() == Settings.AppMode.DARK ? Settings.AppMode.LIGHT : Settings.AppMode.DARK, true));
        extrasPanel.add(darkLightButton);

        extrasPanel.add(Box.createHorizontalStrut(5));

        // HELP BUTTON
        helpButton = BuildRoundButton("svg/question.svg", I18N.GetString("help"));
        helpButton.addActionListener(l -> OpenHelpGUI());
        extrasPanel.add(helpButton);

        extrasPanel.add(Box.createHorizontalGlue());

        // EXIT BUTTON
        exitButton = BuildRoundButton("svg/x.svg", I18N.GetString("exit"));
        exitButton.addActionListener(l -> CloseApplication());
        extrasPanel.add(exitButton);

        // PANEL
        extrasPanel.setMaximumSize(
            new Dimension(
                Integer.MAX_VALUE,
                extrasPanel.getPreferredSize().height
            )
        );
    }

    // SETTERS
    private static void SetSaveFile(File file)
    {
        if (file == null)
        {
            saveFile.set(null);

            DisablePaimonAccountChooser();

            paimonFileInputField.setText(I18N.GetString("no-file"));
        }
        else
        {
            saveFile.set(file);

            paimonFileInputField.setText(file.getAbsolutePath());

            EnablePaimonAccountChooser();
        }

        UpdateProgressBarTextAndStartButton();
    }

    private static void SetDatabase(Database database)
    {
        if (database == null)
        {
            GUI.database.set(null);
            selectedCategory.set(-1);

            onlineDatabaseStatusLabel.setText(I18N.GetString("error-syncing"));
        }
        else
        {
            GUI.database.set(database);

            try
            {
                String fileDate = DatabaseReader.GetDatabaseFileDate();
                onlineDatabaseStatusLabel.setText(I18N.GetString("synced") + " " + fileDate);
            }
            catch (IOException e)
            {
                SetDatabase(null);

                return;
            }

            List<String> categories = new ArrayList<>();
            int i = 0;
            for (Category category : database.categories())
            {
                categories.add(i + " - " + category.name());
                i++;
            }
            onlineDatabaseCategoryChooser.setModel(new DefaultComboBoxModel<>(categories.toArray(new String[0])));

            onlineDatabaseCategoryHintLabel.setEnabled(true);
            onlineDatabaseCategoryChooser.setEnabled(true);

            // Make sure the combo box does not overflow
            onlineDatabaseCategoryChooser.setPrototypeDisplayValue("");

            selectedCategory.set(0);
        }

        UpdateProgressBarTextAndStartButton();
    }

    private static void SetVideoFile(File file)
    {
        if (file == null)
        {
            videoFile.set(null);

            videoChooserInputField.setText(I18N.GetString("no-file"));
        }
        else
        {
            videoFile.set(file);

            videoChooserInputField.setText(file.getAbsolutePath());
        }

        UpdateProgressBarTextAndStartButton();
    }

    private static void SetOutputFile(File file)
    {
        if (file == null)
        {
            outputFile.set(null);

            outputChooserInputField.setText(I18N.GetString("no-file"));
        }
        else
        {
            outputFile.set(file);

            outputChooserInputField.setText(file.getAbsolutePath());
        }

        UpdateProgressBarTextAndStartButton();
    }

    private static void SetStartCancelButton(boolean start)
    {
        scanning.set(!start);
        startCancelScanButton.setText(start ? I18N.GetString("start") : I18N.GetString("cancel"));
    }

    private static void SetAppMode(Settings.AppMode appMode, boolean update)
    {
        try
        {
            if (update)
                FlatAnimatedLafChange.showSnapshot();

            if (System.getProperty("os.name").toLowerCase().contains("mac"))
            {
                if (appMode == Settings.AppMode.DARK)
                    FlatMacDarkLaf.setup();
                else
                    FlatMacLightLaf.setup();
            }
            else
            {
                if (appMode == Settings.AppMode.DARK)
                    FlatDarkLaf.setup();
                else
                    FlatLightLaf.setup();
            }

            Settings.SetAppMode(appMode);

            if (update)
            {
                FlatLaf.updateUI();

                if (window != null)
                    SetAppIcon(appMode == Settings.AppMode.DARK ? ICON_DARK : ICON);

                for (Runnable runnable : appModeChangeSubscribeList)
                {
                    runnable.run();
                }

                FlatAnimatedLafChange.hideSnapshotWithAnimation();
            }

            REM = UIManager.getFont("Label.font").getSize2D();
        } catch (Exception ignored) {}
    }

    private static void SetAppIcon(String name)
    {
        try
        {
            URL icon64 = GUI.class.getResource("/icon/" + name + "64.png");
            URL icon48 = GUI.class.getResource("/icon/" + name + "48.png");
            URL icon32 = GUI.class.getResource("/icon/" + name + "32.png");
            URL icon16 = GUI.class.getResource("/icon/" + name + "16.png");

            if (icon64 != null && icon32 != null && icon48 != null&& icon16 != null)
            {
                List<Image> icons = List.of(
                    Toolkit.getDefaultToolkit().getImage(icon64),
                    Toolkit.getDefaultToolkit().getImage(icon48),
                    Toolkit.getDefaultToolkit().getImage(icon32),
                    Toolkit.getDefaultToolkit().getImage(icon16)
                );

                window.setIconImages(icons);
            }
        } catch (Exception ignored) {}
    }

    private static void CloseApplication()
    {
        if (scanning.get())
            StopScan(true);
        else
        {
            window.dispose();

            System.exit(0);
        }
    }

    private static void LoadHelpPage(List<HelpPage> helpPages, int id)
    {
        if (id < 0 || id >= helpPages.size())
            return;

        HelpPage helpPage = helpPages.get(id);

        helpLeftButton.setEnabled(true);
        helpRightButton.setEnabled(true);

        if (id == 0)
            helpLeftButton.setEnabled(false);

        if (id == helpPages.size() - 1)
            helpRightButton.setEnabled(false);

        helpPageLabel.setText((id + 1) + "/" + helpPages.size());

        helpTitleLabel.setText(I18N.GetString(helpPage.header()));

        Dimension maxTextDimension = new Dimension(
            (int) (REM * 25f),
            Integer.MAX_VALUE
        );

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx   = 0;
        constraints.gridy   = 0;
        constraints.weightx = 1.0;
        constraints.fill    = GridBagConstraints.HORIZONTAL;
        constraints.insets  = new Insets(5, 0, 5, 0);

        helpTextPanel.removeAll();
        for (String string : helpPage.textLines())
        {
            String textString = I18N.GetString(string);

            JTextArea fakeLabel = new JTextArea(textString);

            fakeLabel.setPreferredSize(null);

            fakeLabel.setEditable(false);
            fakeLabel.setLineWrap(true);
            fakeLabel.setWrapStyleWord(true);
            fakeLabel.setOpaque(false);
            fakeLabel.setFocusable(false);
            fakeLabel.setBorder(null);

            helpTextPanel.add(fakeLabel, constraints);
            constraints.gridy++;
        }

        JPanel filler = new JPanel();
        filler.setOpaque(false);

        constraints.gridx = 0;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;

        helpTextPanel.add(filler, constraints);

        helpTextPanel.revalidate();
        helpTextPanel.repaint();

        helpTextScrollPane.setPreferredSize(
            new Dimension(
                maxTextDimension.width,
                helpTextScrollPane.getPreferredSize().height
            )
        );

        try (InputStream imageStream = GUI.class.getResourceAsStream(helpPage.imageName()))
        {
            if (imageStream != null)
            {
                BufferedImage bufferedImage = ImageIO.read(imageStream);

                if (bufferedImage != null)
                {
                    helpPanelImage = bufferedImage;

                    Dimension dimension = helpImageLabel.getPreferredSize();
                    if (dimension.width > 0 && dimension.height > 0)
                        helpImageLabel.setIcon(new ImageIcon(Utils.SizeToFit(bufferedImage, dimension.width, dimension.height)));
                }
            }
        } catch (IOException ignored) {}

        SwingUtilities.invokeLater(() -> helpTextScrollPane.getViewport().setViewPosition(new Point(0, 0)));
    }

    // DISABLE/ENABLE GUI ELEMENTS
    private static void TemporaryDisable(JComponent component)
    {
        temporaryDisableRestoreStates.put(component, component.isEnabled());

        component.setEnabled(false);
    }

    private static void ReEnable(JComponent component)
    {
        Boolean state = temporaryDisableRestoreStates.get(component);

        if (state != null)
        {
            component.setEnabled(state);

            temporaryDisableRestoreStates.remove(component);
        }
    }

    private static void DisableEverything()
    {
        TemporaryDisable(paimonFileInputHintLabel);
        TemporaryDisable(paimonFileInputField);
        TemporaryDisable(paimonFileChooserButton);
        TemporaryDisable(paimonAccountChooserHintLabel);
        TemporaryDisable(paimonAccountChooser);
        TemporaryDisable(onlineDatabaseCategoryHintLabel);
        TemporaryDisable(onlineDatabaseHintLabel);
        TemporaryDisable(onlineDatabaseStatusLabel);
        TemporaryDisable(onlineDatabaseSyncButton);
        TemporaryDisable(onlineDatabaseCategoryChooser);
        TemporaryDisable(saveFileRadioHintLabel);
        TemporaryDisable(saveFileMergeRadio);
        TemporaryDisable(saveFileReplaceRadio);

        TemporaryDisable(videoChooserHintLabel);
        TemporaryDisable(videoChooserInputField);
        TemporaryDisable(videoChooserButton);
    }

    private static void EnableEverything()
    {
        ReEnable(paimonFileInputHintLabel);
        ReEnable(paimonFileInputField);
        ReEnable(paimonFileChooserButton);
        ReEnable(paimonAccountChooserHintLabel);
        ReEnable(paimonAccountChooser);
        ReEnable(onlineDatabaseCategoryHintLabel);
        ReEnable(onlineDatabaseHintLabel);
        ReEnable(onlineDatabaseStatusLabel);
        ReEnable(onlineDatabaseSyncButton);
        ReEnable(onlineDatabaseCategoryChooser);
        ReEnable(saveFileRadioHintLabel);
        ReEnable(saveFileMergeRadio);
        ReEnable(saveFileReplaceRadio);

        ReEnable(videoChooserHintLabel);
        ReEnable(videoChooserInputField);
        ReEnable(videoChooserButton);
    }

    private static void DisablePaimonAccountChooser()
    {
        selectedAccount.set(-1);

        paimonAccountChooser.setEnabled(false);
        paimonAccountChooserHintLabel.setEnabled(false);
        paimonAccountChooser.setModel(new DefaultComboBoxModel<>(new String[0]));
    }

    private static void EnablePaimonAccountChooser()
    {
        selectedAccount.set(0);

        paimonAccountChooser.setModel(new DefaultComboBoxModel<>(accountNames.toArray(new String[0])));
        paimonAccountChooser.setEnabled(true);
        paimonAccountChooserHintLabel.setEnabled(true);
    }

    private static void UpdateProgressBarTextAndStartButton()
    {
        startCancelScanButton.setEnabled(false);

        if (database.get() == null || selectedCategory.get() == -1)
            scanProgressBar.setString(I18N.GetString("missing-database"));
        else if (saveFile.get() == null)
            scanProgressBar.setString(I18N.GetString("missing-save-file"));
        else if (accountNames == null || selectedAccount.get() == -1) // This should never happen but just in case...
            scanProgressBar.setString(I18N.GetString("missing-account"));
        else if (videoFile.get() == null)
            scanProgressBar.setString(I18N.GetString("missing-video-file"));
        else if (outputFile.get() == null)
            scanProgressBar.setString(I18N.GetString("missing-output-file"));
        else
        {
            scanProgressBar.setString(I18N.GetString("ready"));
            startCancelScanButton.setEnabled(true);
        }
    }

    // SCAN
    private static void SyncDatabase(boolean force, boolean showPopup)
    {
        onlineDatabaseStatusLabel.setText(I18N.GetString("syncing"));

        onlineDatabaseSyncButton.setEnabled(false);
        onlineDatabaseCategoryHintLabel.setEnabled(false);
        onlineDatabaseCategoryChooser.setEnabled(false);
        onlineDatabaseCategoryChooser.setModel(new DefaultComboBoxModel<>(new String[0]));

        database.set(null);
        selectedCategory.set(-1);

        UpdateProgressBarTextAndStartButton();

        CompletableFuture.supplyAsync(() -> {
            if (force)
            {
                Database database = DatabaseReader.ForceReload();

                if (database == null)
                    return new Pair<>(DatabaseReader.LoadLocalDatabase(), true); // Try to load local DB if sync failed.

                return new Pair<>(database, false);
            }
            else
                return new Pair<>(DatabaseReader.LoadLocalDatabase(), false);
        }).thenAccept(result -> SwingUtilities.invokeLater(() -> {
            database.set(result.a());

            if (showPopup && result.b())
                JOptionPane.showMessageDialog(window, I18N.GetString("error-syncing-popup"), I18N.GetString("error"), JOptionPane.ERROR_MESSAGE);

            SetDatabase(database.get());

            onlineDatabaseSyncButton.setEnabled(true);
        }));
    }

    private static void StartScan()
    {
        if (saveFile.get() == null || !saveFile.get().exists())
        {
            JOptionPane.showMessageDialog(window, I18N.GetString("save-file-moved"), I18N.GetString("error"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (videoFile.get() == null || !videoFile.get().exists())
        {
            JOptionPane.showMessageDialog(window, I18N.GetString("video-file-moved"), I18N.GetString("error"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (database.get() == null ||
            accountNames == null ||
            accountNames.isEmpty() ||
            selectedAccount.get() < 0 ||
            selectedAccount.get() >= accountNames.size() ||
            selectedCategory.get() < 0
        )
        {
            JOptionPane.showMessageDialog(window, I18N.GetString("internal-error"), I18N.GetString("error"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        SetStartCancelButton(false);
        DisableEverything();
        ((DefaultListModel<Achievement>) scanResultsList.getModel()).clear();

        scanProgressBar.setString("0% (0/0)");
        if (taskbar != null)
            taskbar.setWindowProgressValue(window, 0);

        CompletableFuture.supplyAsync(() -> AchievementScanner.ProcessVideo(videoFile.get(), (current, total) -> SwingUtilities.invokeLater(() -> {
            int progressPercent = (int) (((double) current / total) * 100.0);

            scanProgressBar.setMaximum(total);
            scanProgressBar.setValue(current);
            scanProgressBar.setString(progressPercent + "% (" + current + "/" + total + ")");

            if (taskbar != null) taskbar.setWindowProgressValue(window, progressPercent);
        }))).thenAccept(result -> {
            if (result == null)
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(window, I18N.GetString("internal-error"), I18N.GetString("error"), JOptionPane.ERROR_MESSAGE));
            else
            {
                List<Achievement> obtainedAchievements = SaveFileTools.GetObtainedAchievements(onlineDatabaseCategoryChooser.getSelectedIndex(), database.get(), result);

                SaveFileTools.SaveFileValidity generatedSaveFile = SaveFileTools.GenerateSaveFile(
                    selectedCategory.get(),
                    database.get(),
                    obtainedAchievements,
                    saveFile.get(),
                    mergeCategory.get(),
                    selectedAccount.get()
                );

                if (generatedSaveFile == null)
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(window, I18N.GetString("internal-error"), I18N.GetString("error"), JOptionPane.ERROR_MESSAGE));
                else if (!generatedSaveFile.valid())
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(window, generatedSaveFile.reason(), I18N.GetString("error"), JOptionPane.ERROR_MESSAGE));
                else
                {
                    try (PrintWriter outputFileWriter = new PrintWriter(outputFile.get()))
                    {
                        outputFileWriter.write(generatedSaveFile.result());
                        outputFileWriter.flush();
                    }
                    catch (FileNotFoundException e)
                    {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(window, I18N.GetString("output-file-error"), I18N.GetString("error"), JOptionPane.ERROR_MESSAGE));
                    }
                }

                SwingUtilities.invokeLater(() -> {
                    if (taskbar != null)
                        taskbar.setWindowProgressValue(window, -1);

                    scanProgressBar.setMaximum(1000);
                    scanProgressBar.setValue(0);

                    SetOutputFile(null);

                    ((DefaultListModel<Achievement>) scanResultsList.getModel()).addAll(obtainedAchievements);

                    EnableEverything();
                    SetStartCancelButton(true);

                    UpdateProgressBarTextAndStartButton();

                    JOptionPane.showMessageDialog(window, I18N.GetString("scan-complete"), I18N.GetString("info"), JOptionPane.INFORMATION_MESSAGE);
                });
            }
        });
    }

    private static void StopScan(boolean exit)
    {
        int choice = JOptionPane.showConfirmDialog(window, I18N.GetString("cancel-warning"), I18N.GetString("warning"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (choice == JOptionPane.YES_OPTION)
        {
            if (scanning.get())
            {
                scanProgressBar.setString(I18N.GetString("cancelling"));

                CompletableFuture.supplyAsync(() -> {
                    AchievementScanner.Interrupt();

                    return null;
                }).thenAccept(result -> SwingUtilities.invokeLater(() -> {
                    EnableEverything();

                    UpdateProgressBarTextAndStartButton();

                    if (taskbar != null)
                        taskbar.setWindowProgressValue(window, -1);

                    if (exit)
                    {
                        window.dispose();

                        System.exit(0);
                    }
                }));
            }
            else if (exit)
            {
                window.dispose();

                System.exit(0);
            }
        }
    }

    // POPUPS
    private static void OpenSettingsGUI()
    {
        settingsDialog = new JDialog(window, I18N.GetString("settings"), Dialog.ModalityType.APPLICATION_MODAL);

        mainSettingsPanel = new JPanel();
        mainSettingsPanel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                mainSettingsPanel.requestFocusInWindow();
            }
        });

        mainSettingsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainSettingsPanel.setLayout(new BoxLayout(mainSettingsPanel, BoxLayout.Y_AXIS));

        languagePickerPanel = new JPanel();
        languagePickerPanel.setLayout(new BoxLayout(languagePickerPanel, BoxLayout.X_AXIS));
        languagePickerPanel.setAlignmentY(Component.CENTER_ALIGNMENT);

        threadsSliderPanel = new JPanel();
        threadsSliderPanel.setLayout(new BoxLayout(threadsSliderPanel, BoxLayout.X_AXIS));
        threadsSliderPanel.setAlignmentY(Component.CENTER_ALIGNMENT);

        // LANGUAGE PICKER
        languagePickerHintLabel = new JLabel(I18N.GetString("application-language"));
        languagePickerPanel.add(languagePickerHintLabel);

        languagePickerPanel.add(Box.createHorizontalStrut(5));

        List<Settings.Language> languages = Arrays.asList(Settings.Language.values());
        languagePicker = new JComboBox<>(
            new DefaultComboBoxModel<>(
                languages.toArray(Settings.Language[]::new)
            )
        );
        languagePicker.setRequestFocusEnabled(false);

        languagePicker.setRenderer(new DefaultListCellRenderer()
        {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
            {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                Settings.Language language = (Settings.Language) value;

                label.setText(I18N.GetString(language.name()));

                return label;
            }
        });

        languagePicker.setSelectedIndex(languages.indexOf(Settings.GetLanguage()));
        languagePicker.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED)
            {
                Settings.SetLanguage((Settings.Language) languagePicker.getSelectedItem());

                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(settingsDialog, I18N.GetString("language-reload"), I18N.GetString("warning"), JOptionPane.WARNING_MESSAGE));
            }
        });

        languagePickerPanel.add(languagePicker);
        languagePickerPanel.setMaximumSize(
            new Dimension(
                Integer.MAX_VALUE,
                languagePickerPanel.getPreferredSize().height
            )
        );

        // THREADS SLIDER
        threadsSliderHintLabel = new JLabel(I18N.GetString("max-threads"));
        threadsSliderPanel.add(threadsSliderHintLabel);

        threadsSliderPanel.add(Box.createHorizontalStrut(5));

        int maxCores = Runtime.getRuntime().availableProcessors();
        threadsSlider = new JSlider(1, maxCores);
        threadsSlider.setRequestFocusEnabled(false);
        threadsSlider.setMinorTickSpacing(1);
        threadsSlider.setPaintLabels(true);
        threadsSlider.setPaintTicks(true);

        int increment = maxCores / 4;
        Hashtable<Integer, JComponent> labelTable;
        if (increment == 0 || increment > maxCores)
        {
            labelTable = new Hashtable<>();
            labelTable.put(maxCores, new JLabel(String.valueOf(maxCores)));
        }
        else
            labelTable = threadsSlider.createStandardLabels(increment, increment);

        labelTable.put(1, new JLabel("1"));

        threadsSlider.setLabelTable(labelTable);

        if (Settings.GetMaxThreads() > maxCores)
            Settings.SetMaxThreads(-1);

        if (Settings.GetMaxThreads() == -1)
            threadsSlider.setValue(maxCores);
        else
            threadsSlider.setValue(Settings.GetMaxThreads());

        Timer sliderDebounceTimer = new Timer(500, e -> {
            int value = threadsSlider.getValue();

            Settings.SetMaxThreads(value == threadsSlider.getMaximum() ? -1 : value);
        });
        sliderDebounceTimer.setRepeats(false);

        threadsSlider.addChangeListener(l -> sliderDebounceTimer.restart());

        threadsSliderPanel.add(threadsSlider);

        threadsSliderPanel.setMaximumSize(
            new Dimension(
                Integer.MAX_VALUE,
                threadsSliderPanel.getPreferredSize().height
            )
        );

        // PANEL
        mainSettingsPanel.add(BuildHeader(I18N.GetString("settings")).header);
        mainSettingsPanel.add(Box.createVerticalStrut(10));
        mainSettingsPanel.add(languagePickerPanel);
        mainSettingsPanel.add(Box.createVerticalStrut(5));
        mainSettingsPanel.add(threadsSliderPanel);
        mainSettingsPanel.add(Box.createVerticalGlue());

        settingsDialog.add(mainSettingsPanel);

        settingsDialog.pack();
        settingsDialog.setMinimumSize(settingsDialog.getSize());
        settingsDialog.setResizable(false);

        ApplyCloseOnEsc(settingsDialog);
        settingsDialog.setLocationRelativeTo(window);
        settingsDialog.setVisible(true);

        SwingUtilities.invokeLater(mainSettingsPanel::requestFocusInWindow);
    }

    private static void OpenHelpGUI()
    {
        helpDialog = new JDialog(window, I18N.GetString("help"), Dialog.ModalityType.APPLICATION_MODAL);

        mainHelpPanel = new JPanel();
        mainHelpPanel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                mainHelpPanel.requestFocusInWindow();
            }
        });

        mainHelpPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainHelpPanel.setLayout(new BoxLayout(mainHelpPanel, BoxLayout.Y_AXIS));

        helpImagePanel = new JPanel();
        helpImagePanel.setLayout(new BoxLayout(helpImagePanel, BoxLayout.X_AXIS));
        helpImagePanel.setAlignmentY(Component.TOP_ALIGNMENT);

        helpTextPanel = new JPanel();
        helpTextPanel.setLayout(new GridBagLayout());

        helpTextScrollPane = new JScrollPane(helpTextPanel);
        helpTextScrollPane.setBorder(null);
        helpTextScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        helpTextScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        helpImageTextPanel = new JPanel();
        helpImageTextPanel.setLayout(new BoxLayout(helpImageTextPanel, BoxLayout.X_AXIS));
        helpImageTextPanel.setAlignmentY(Component.CENTER_ALIGNMENT);

        helpButtonsPanel = new JPanel();
        helpButtonsPanel.setLayout(new BoxLayout(helpButtonsPanel, BoxLayout.X_AXIS));
        helpButtonsPanel.setAlignmentY(Component.CENTER_ALIGNMENT);

        // IMAGE LABEL
        helpImageLabel = new JLabel();
        helpImageLabel.setPreferredSize(new Dimension(250, 250));
        helpImageLabel.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                if (helpPanelImage != null)
                {
                    Dimension dimension = helpImageLabel.getSize();
                    if (dimension.width > 0 && dimension.height > 0)
                        helpImageLabel.setIcon(new ImageIcon(Utils.SizeToFit(helpPanelImage, dimension.width, dimension.height)));
                }
            }
        });
        helpImagePanel.add(helpImageLabel);

        // NAV BUTTONS
        int[] pageNumber = new int[]{0};

        helpLeftButton = BuildRoundButton("svg/left.svg", I18N.GetString("previous"));
        helpLeftButton.addActionListener(l -> {
            pageNumber[0]--;
            LoadHelpPage(Settings.GetAppMode() == Settings.AppMode.DARK ? HELP_PAGES_DARK : HELP_PAGES_LIGHT, pageNumber[0]);
        });
        helpLeftButton.setRequestFocusEnabled(false);

        helpButtonsPanel.add(Box.createHorizontalGlue());
        helpButtonsPanel.add(helpLeftButton);

        helpButtonsPanel.add(Box.createHorizontalStrut(5));

        helpPageLabel = new JLabel("0/0");
        helpButtonsPanel.add(helpPageLabel);

        helpButtonsPanel.add(Box.createHorizontalStrut(5));

        helpRightButton = BuildRoundButton("svg/right.svg", I18N.GetString("next"));
        helpRightButton.addActionListener(l -> {
            pageNumber[0]++;
            LoadHelpPage(Settings.GetAppMode() == Settings.AppMode.DARK ? HELP_PAGES_DARK : HELP_PAGES_LIGHT, pageNumber[0]);
        });
        helpRightButton.setRequestFocusEnabled(false);

        helpButtonsPanel.add(helpRightButton);

        // PANEL
        helpImageTextPanel.add(helpImagePanel);
        helpImageTextPanel.add(Box.createHorizontalStrut(10));
        helpImageTextPanel.add(helpTextScrollPane);

        Header header = BuildHeader(I18N.GetString("help"));
        helpTitleLabel = header.titleLabel;

        mainHelpPanel.add(header.header);
        mainHelpPanel.add(Box.createVerticalStrut(10));
        mainHelpPanel.add(helpImageTextPanel);
        mainHelpPanel.add(Box.createVerticalStrut(5));
        mainHelpPanel.add(helpButtonsPanel);

        helpDialog.add(mainHelpPanel);

        LoadHelpPage(Settings.GetAppMode() == Settings.AppMode.DARK ? HELP_PAGES_DARK : HELP_PAGES_LIGHT, pageNumber[0]);

        helpButtonsPanel.setMaximumSize(
            new Dimension(
                Integer.MAX_VALUE,
                helpButtonsPanel.getPreferredSize().height
            )
        );
        helpImageLabel.setMaximumSize(
            new Dimension(
                Integer.MAX_VALUE,
                Integer.MAX_VALUE
            )
        );
        helpImagePanel.setMaximumSize(
            new Dimension(
                Integer.MAX_VALUE,
                Integer.MAX_VALUE
            )
        );
        helpImageTextPanel.setMaximumSize(
            new Dimension(
                Integer.MAX_VALUE,
                Integer.MAX_VALUE
            )
        );

        ApplyCloseOnEsc(helpDialog);

        GraphicsDevice gd = window.getGraphicsConfiguration().getDevice();
        int width = gd.getDisplayMode().getWidth();
        int height = gd.getDisplayMode().getHeight();

        helpDialog.pack();
        helpDialog.setMinimumSize(helpDialog.getSize());

        helpDialog.setSize(new Dimension((int) (width * 0.9f), (int) (height * 0.9f)));
        helpDialog.setLocationRelativeTo(null);

        helpDialog.setVisible(true);

        SwingUtilities.invokeLater(mainHelpPanel::requestFocusInWindow);
    }

    // MAIN
    private static void InitializeHelpPages()
    {
        InputStream helpPageFileStream = GUI.class.getResourceAsStream(HELP_PAGES_FILE);

        if (helpPageFileStream != null)
        {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(helpPageFileStream)))
            {
                JsonElement jsonElement = JsonParser.parseReader(bufferedReader);

                if (jsonElement.isJsonObject())
                {
                    JsonObject object = jsonElement.getAsJsonObject();

                    if (object.has("pages"))
                    {
                        JsonArray pagesArray = object.get("pages").getAsJsonArray();

                        for (JsonElement pageElement : pagesArray)
                        {
                            JsonObject pageObject = pageElement.getAsJsonObject();

                            String   header    = pageObject.get("header").getAsString();
                            String[] textLines = pageObject.get("text-lines")
                                .getAsJsonArray()
                                .asList().stream()
                                .map(JsonElement::getAsString)
                                .toArray(String[]::new);

                            HELP_PAGES_LIGHT.add(
                                new HelpPage(
                                    header,
                                    pageObject.get("image-name-light").getAsString(),
                                    textLines
                                )
                            );

                            HELP_PAGES_DARK.add(
                                new HelpPage(
                                    header,
                                    pageObject.get("image-name-dark").getAsString(),
                                    textLines
                                )
                            );
                        }
                    }
                }
            } catch (IOException ignored) {}
        }
    }

    public static void OpenMainGUI()
    {
        InitializeHelpPages();

        SetAppMode(Settings.GetAppMode(), false);

        if (Taskbar.isTaskbarSupported())
        {
            Taskbar tempTaskbar = Taskbar.getTaskbar();

            if (tempTaskbar.isSupported(Taskbar.Feature.PROGRESS_VALUE_WINDOW))
            {
                taskbar = tempTaskbar;
            }
        }

        window = new JFrame(I18N.GetString("app-title"));

        SetAppIcon(Settings.GetAppMode() == Settings.AppMode.DARK ? ICON_DARK : ICON);

        mainPanel = new JPanel();
        mainPanel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                mainPanel.requestFocusInWindow();
            }
        });

        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        BuildPaimonPanel();
        mainPanel.add(paimonPanel);

        mainPanel.add(Box.createVerticalStrut(20));

        BuildVideoPanel();
        mainPanel.add(videoPanel);

        mainPanel.add(Box.createVerticalStrut(20));

        BuildOutputPanel();
        mainPanel.add(outputPanel);

        mainPanel.add(Box.createVerticalStrut(20));

        BuildResultsPanel();
        mainPanel.add(scanResultsPanel);

        mainPanel.add(Box.createVerticalStrut(20));

        BuildScanPanel();
        mainPanel.add(scanPanel);

        mainPanel.add(Box.createVerticalStrut(10));

        BuildExtrasPanel();
        mainPanel.add(extrasPanel);

        window.add(mainPanel);

        window.pack();

        window.setMinimumSize(window.getSize());

        window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        window.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                CloseApplication();
            }
        });

        window.setLocationRelativeTo(null);
        window.setVisible(true);

        SwingUtilities.invokeLater(mainPanel::requestFocusInWindow);

        SyncDatabase(false, false);
    }
}
