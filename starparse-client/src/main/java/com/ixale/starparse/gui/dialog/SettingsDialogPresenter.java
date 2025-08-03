package com.ixale.starparse.gui.dialog;

import com.ixale.starparse.domain.ConfigPopoutDefault;
import com.ixale.starparse.domain.ConfigTimer;
import com.ixale.starparse.domain.ConfigTimer.Condition;
import com.ixale.starparse.domain.ConfigTimers;
import com.ixale.starparse.domain.RaidBoss;
import com.ixale.starparse.domain.ServerName;
import com.ixale.starparse.gui.Config;
import com.ixale.starparse.gui.FlashMessage;
import com.ixale.starparse.gui.Marshaller;
import com.ixale.starparse.gui.SoundManager;
import com.ixale.starparse.gui.StarparseApp;
import com.ixale.starparse.gui.popout.BasePopoutPresenter;
import com.ixale.starparse.gui.popout.BasePopoutPresenter.Mode;
import com.ixale.starparse.gui.popout.TimersCenterPopoutPresenter;
import com.ixale.starparse.parser.Helpers;
import com.ixale.starparse.time.TimeUtils;
import com.ixale.starparse.timer.TimerManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Duration;
import javafx.util.StringConverter;

import javax.swing.*;
import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class SettingsDialogPresenter extends BaseDialogPresenter {

	private final static String TIMERS_MAGIC = "PGNvbS5peGFsZS5zdGFycGFyc2UuZG9tYWluLkNvbmZpZ1RpbWVyPg";

	@FXML
	private TabPane dialogRoot;
	@FXML
	private Tab contentOverlays, contentTimers, contentUpload;

	@FXML
	private TextField logDirectoryField, recentParsedLogsLimitField, recentOpenedLogsLimitField, timeSyncHostField,
			raidPullSec, raidPullHotkey, raidBreakMin,
			raidDamageOpacityText, raidHealingOpacityText, raidThreatOpacityText, raidBossOpacityText,
			raidChallengesOpacityText, timersOpacityText, timersFractions, personalOpacityText, lockOverlaysHotkey,
			guildField, parselyLoginField, parselyPasswordField,
			timerName, timerSourceName, timerTargetName, timerAbilityName, timerEffectName,
			timerDuration, timerRepeat, timerSoundOffset, timerCountdownCount;

	@FXML
	private Slider raidDamageOpacitySlider, raidHealingOpacitySlider, raidThreatOpacitySlider, raidBossOpacitySlider,
			raidChallengesOpacitySlider, timersOpacitySlider, personalOpacitySlider, timerSoundVolume, timerCountdownVolume;

	@FXML
	private Button timersCenterMoveButton, overlaysResetButton,
			timerSaveButton, timerCopyButton, timersImportButton, timerSoundButton, timerCountdownButton;

	@FXML
	private CheckBox timeSyncEnabledButton, serverStoreEnabledButton,
			raidDamageBars, raidHealingBars, raidThreatBars, raidBossBars, raidChallengesBars, timersBars, personalBars, timersCenter, popoutSolid,
			timerDisplay, timerPlaySound, timersIgnoreRepeated, timersShowSource, timerPlayCountdown;



	@FXML
	private ColorPicker popoutBackgroundColor, popoutTextColor,
			popoutDamageColor, popoutHealingColor, popoutThreatColor, popoutFriendlyColor,
			timerColor;

	@FXML
	private ChoiceBox<String> timezoneList, serverList,
			raidHealingMode, personalMode, timerSlot;

	@FXML
	private Text currentTime, serverLabel, timerTriggerTimerLabel, timerEffectLabel, timerBossLabel, timerSoundOffsetLabel;

	@FXML
	private RadioButton timerSourceYou, timerSourceOther, timerSourceCustom,
			timerTargetYou, timerTargetOther, timerTargetCustom;

	@FXML
	private AnchorPane timerSourceContainer, timerTargetContainer, timerAbilityContainer,
			timerIntervalContainer, timerTriggerContainer, timerCancelContainer;

	@FXML
	private TreeView<TimerNode> timersList;

	@FXML
	private ToggleGroup timerSource, timerTarget;

	@FXML
	private ComboBox<String> timerBoss, timerTrigger, timerTriggerTimer, timerCancel, timerSoundFile, timerCountdownVoice;

	private Config config;

	private final GeneralSettings generalSettings = new GeneralSettings();
	private final OverlaysSettings overlaysSettings = new OverlaysSettings();
	private final TimersSettings timersSettings = new TimersSettings();
	private final UploadSettings uploadSettings = new UploadSettings();
	private final BaseSettings[] settings = new BaseSettings[]{
			generalSettings, overlaysSettings, timersSettings, uploadSettings
	};

	private SettingsUpdatedListener listener;
	private Timeline clock;

	interface Validator<T extends Control> {
		boolean isValid(T c);
	}

	public interface SettingsUpdatedListener {

		void onOverlaysSettings(
				Color backgroundColor,
				Color textColor,
				Color damageColor,
				Color healingColor,
				Color threatColor,
				Color friendlyColor,
				//
				double raidDamageOpacity, boolean raidDamageBars,
				double raidHealingOpacity, boolean raidHealingBars, String raidHealingMode,
				double raidThreatOpacity, boolean raidThreatBars,
				double raidBossOpacity, boolean raidBossBars,
				double raidChallengesOpacity, boolean raidChallengesBars,
				double timersOpacity, boolean timersBars,
				double personalOpacity, boolean personalBars, String personalMode,
				//
				boolean timersCenter, Double timersCenterX, Double timersCenterY,
				Integer fractions,
				boolean popoutSolid);

		void onOverlaysReset(String characterName);

		void onTimersUpdated();

		void onUploadUpdated();

		void onHotkeyUpdated(Config.Hotkey hotkey, String oldHotkey, String newHotkey);
	}

	@SuppressWarnings("StringConcatenationInLoop")
	private final Validator<TextField> hotkeyValidator = c -> {
		if (c.getText() == null || c.getText().isEmpty()) {
			return true;
		}
		String v = c.getText();
		if (KeyStroke.getKeyStroke(v) != null) {
			return true;
		}
		// try to capitalize
		final String[] parts = v.split("\\s+");
		v = "";
		for (String p : parts) {
			if (p.matches("(shift|control|ctrl|meta|alt|altGraph|pressed|released)")) {
				v += p + " ";
				continue;
			}
			v += p.toUpperCase() + " ";
		}
		v = v.trim();
		if (KeyStroke.getKeyStroke(v) != null) {
			c.setText(v);
			return true;
		}
		return false;
	};

	public void setConfig(final Config config) {
		this.config = config;
	}

	public void setListener(final SettingsUpdatedListener listener) {
		this.listener = listener;
	}

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
		setContent(dialogRoot, "Settings", null);

		for (final BaseSettings s : settings) {
			s.initialize();
		}

		final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		final Tooltip tooltipCopyTimers = new Tooltip("Copy timers to import in your clipboard");
		final Tooltip tooltipSaveTimers = new Tooltip("Pending imported timers will be saved");
		clock = new Timeline(
				new KeyFrame(Duration.seconds(0),
						actionEvent -> {
							Long time = TimeUtils.getCurrentTime();
							final TimeZone tz;
							if (timezoneList.getValue() != null) {
								// simulate other time zone
								tz = TimeZone.getTimeZone(timezoneList.getValue());
							} else {
								tz = TimeUtils.getCurrentTimezone();
							}
							if (!tz.equals(sdf.getTimeZone())) {
								sdf.setTimeZone(tz);
							}
							currentTime.setText("Time (" + TimeUtils.formatTimezoneOffset(tz.getOffset(time)) + "): " + sdf.format(time));

							if (!timersSettings.pendingImportedTimers.isEmpty()) {
								timersImportButton.setDisable(false);
								timersImportButton.setText("Save imported timers (" + timersSettings.pendingImportedTimers.size() + ")");
								timersImportButton.setTooltip(tooltipSaveTimers);
								timersImportButton.setUserData(null);

							} else {
								final String clipboard = Clipboard.getSystemClipboard().getString();
								if (clipboard != null && clipboard.startsWith(TIMERS_MAGIC)) {
									if (Objects.equals(clipboard.hashCode(), timersImportButton.getUserData())) {
										return;
									}
									timersImportButton.setUserData(clipboard.hashCode());
									try {
										final List<ConfigTimer> timers = getTimersFromClipboard(clipboard);
										final List<ConfigTimer> eligible = new ArrayList<>();
										timers: for (final ConfigTimer timer : timers) {
											for (final ConfigTimer other : config.getConfigTimers().getTimers()) {
												if (other.getName().equals(timer.getName())) {
													continue timers;
												}
											}
											eligible.add(timer);
										}
										if (!eligible.isEmpty()) {
											timersImportButton.setDisable(false);
											timersImportButton.setText("Import timers (" + eligible.size() + ")");
											timersImportButton.setTooltip(new Tooltip(eligible.toString()));
										} else {
											timersImportButton.setDisable(true);
											timersImportButton.setText("No new timers to import");
										}
										return;

									} catch (Exception e) {
										timersImportButton.setText("Invalid data to import");
										timersImportButton.setTooltip(new Tooltip("Error: " + e.getMessage()));
										timersImportButton.setDisable(true);
										return;
									}
								}
								if (timersImportButton.getTooltip() != tooltipCopyTimers) {
									timersImportButton.setText("No timers to import");
									timersImportButton.setTooltip(tooltipCopyTimers);
									timersImportButton.setDisable(true);
								}
							}
						}),
				new KeyFrame(Duration.seconds(1)));
		clock.setCycleCount(Animation.INDEFINITE);
	}

	@Override
	public void show() {
		// load values & resolve differences
		for (final BaseSettings s : settings) {
			s.loadCurrent();
			s.suppressEvents = true;
			loadValues(s.current);
			s.suppressEvents = false;
			if (s == timersSettings) {
				continue;
			}
			s.validateAll();
		}


		dialogRoot.getSelectionModel().select(0);

		clock.play();

		super.show();
	}



	public void selectOverlays() {
		dialogRoot.getSelectionModel().select(contentOverlays);
	}

	public void selectTimers() {
		dialogRoot.getSelectionModel().select(contentTimers);
	}

	public void selectUpload() {
		dialogRoot.getSelectionModel().select(contentUpload);
	}

	@SuppressWarnings("unchecked")
	private void loadValues(final HashMap<Control, Object> values) {

		for (final Control c : values.keySet()) {
			if (c instanceof TextField) {
				if (values.get(c) == null) {
					((TextField) c).setText("");
					continue;
				}
				((TextField) c).setText(String.valueOf(values.get(c) instanceof Double
						? Math.round((double) values.get(c))
						: (values.get(c))));
			} else if (c instanceof CheckBox) {
				((CheckBox) c).setSelected((Boolean) values.get(c));
			} else if (c instanceof ChoiceBox) {
				((ChoiceBox<String>) c).setValue((String) values.get(c));
			} else if (c instanceof Slider) {
				((Slider) c).setValue((Double) values.get(c));
			} else if (c instanceof ColorPicker) {
				((ColorPicker) c).setValue((Color) values.get(c));
				c.fireEvent(new ActionEvent());
			} else if (c instanceof Button) {
				c.setUserData(values.get(c));
			}
		}
	}

	public void handleLogDirectory(final ActionEvent e) {
		generalSettings.handleLogDirectory(e);
	}

	public void handleSettingsDefaults(@SuppressWarnings("unused") final ActionEvent event) {
		loadValues(generalSettings.defaults);
		generalSettings.validateAll();
	}

	public void handleSettingsSave(final ActionEvent event) {
		generalSettings.handleSave(event);
	}





	public void handleOverlaysDefaults(@SuppressWarnings("unused") final ActionEvent event) {
		loadValues(overlaysSettings.defaults);
		overlaysSettings.validateAll();
	}

	public void handleOverlaysReset(@SuppressWarnings("unused") final ActionEvent event) {
		overlaysSettings.resetAllPopouts();
	}

	public void handleOverlaysSave(final ActionEvent event) {
		overlaysSettings.handleSave(event);
	}

	public void handleTimerSave(final ActionEvent event) {
		timersSettings.handleSave(event, false);
	}

	public void handleTimerCopy(final ActionEvent event) {
		timersSettings.handleSave(event, true);
	}

	public void handleTimerReset(@SuppressWarnings("unused") final ActionEvent event) {
		setFlash(null);
		if (timersList.getSelectionModel().getSelectedItem() == null) {
			timersSettings.resetForm(null);
			return;
		}
		timersList.getSelectionModel().clearSelection();
	}

	public void handleTimersImport(@SuppressWarnings("unused") final ActionEvent event) {
		if (!timersSettings.pendingImportedTimers.isEmpty()) {
			int pending = 0;
			for (final ConfigTimer timer : config.getConfigTimers().getTimers()) {
				if (Boolean.TRUE.equals(timer.getIsPreview())) {
					timer.setIsPreview(false);
					pending++;
				}
			}
			if (pending > 0) {
				timersSettings.fireSaveAndRefresh(pending > 1
						? "All pending imported timers saved (" + pending + ")"
						: "Pending imported timer saved");
			}
			timersSettings.pendingImportedTimers.clear();
			return;
		}
		final String clipboard = Clipboard.getSystemClipboard().getString();
		if (clipboard != null && clipboard.startsWith(TIMERS_MAGIC)) {
			try {
				final List<ConfigTimer> timers = getTimersFromClipboard(clipboard);
				List<ConfigTimer> pending = new ArrayList<>();
				timers:
				for (final ConfigTimer timer : timers) {
					for (final ConfigTimer other : config.getConfigTimers().getTimers()) {
						if (other.getName().equals(timer.getName())) {
							continue timers;
						}
					}
					timer.setIsPreview(true);
					config.getConfigTimers().getTimers().add(timer);
					pending.add(timer);
				}
				if (!pending.isEmpty()) {
					timersSettings.loadCurrent();
					timersSettings.pendingImportedTimers.addAll(pending);
				}

			} catch (Exception e) {
				setFlash("Invalid data to import: " + e.getMessage());
			}
		}
	}

	public void handleUploadSave(final ActionEvent event) {
		uploadSettings.handleSave(event);
	}

	@Override
	public void handleClose(final ActionEvent event) {
		overlaysSettings.cleanup();
		clock.stop();
		config.getConfigTimers().getTimers().removeAll(timersSettings.pendingImportedTimers);
		timersSettings.pendingImportedTimers.clear();
		super.handleClose(event);
	}

	abstract private static class BaseSettings {
		protected final HashMap<Control, Validator<? extends Control>> validators = new HashMap<>();
		protected final HashMap<Control, Object> defaults = new HashMap<>();
		protected final HashMap<Control, Object> current = new HashMap<>();

		public boolean isDirty = false, suppressEvents = false;

		abstract public void initialize();

		abstract public void loadCurrent();

		public boolean validateAll() {
			boolean isValid = true;
			for (final Control control : current.keySet()) {
				if (!validate(control)) {
					isValid = false;
				}
			}
			return isValid;
		}

		@SuppressWarnings("unchecked")
		public <T extends Control> boolean validate(final T control) {
			if (!validators.containsKey(control)) {
				return true;
			}
			if (((Validator<T>) validators.get(control)).isValid(control)) {
				control.getStyleClass().remove("input-error");
				control.getStyleClass().remove("input-nondefault");
				control.getStyleClass().remove("input-updated");
				if (defaults.containsKey(control)) {
					if (control instanceof TextField && !((TextField) control).getText().equals(current.get(control))) {
						control.getStyleClass().add("input-updated");

					} else if (control instanceof TextField
							&& !((TextField) control).getText().equals(defaults.get(control))) {
						control.getStyleClass().add("input-nondefault");

					} else if (control instanceof CheckBox
							&& ((CheckBox) control).isSelected() != (boolean) current.get(control)) {
						control.getStyleClass().add("input-updated");

					} else if (control instanceof CheckBox
							&& ((CheckBox) control).isSelected() != (boolean) defaults.get(control)) {
						control.getStyleClass().add("input-nondefault");

					} else if (control instanceof ColorPicker
							&& !((ColorPicker) control).getValue().toString().equals(current.get(control).toString())) {
						control.getStyleClass().add("input-updated");

					} else if (control instanceof ColorPicker
							&& !((ColorPicker) control).getValue().toString().equals(defaults.get(control).toString())) {
						control.getStyleClass().add("input-nondefault");

					} else if (control instanceof ChoiceBox
							&& !((ChoiceBox<String>) control).getValue().equals(current.get(control).toString())) {
						control.getStyleClass().add("input-updated");

					} else if (control instanceof ChoiceBox
							&& !((ChoiceBox<String>) control).getValue().equals(defaults.get(control).toString())) {
						control.getStyleClass().add("input-nondefault");
					}
				}
				return true;

			} else {
				control.getStyleClass().add("input-error");
				return false;
			}
		}

		public boolean validate(final Control... controls) {
			for (final Control control : controls) {
				if (!validate(control)) {
					return false;
				}
			}
			return true;
		}
	}

	private class GeneralSettings extends BaseSettings {

		private DirectoryChooser directoryChooser;

		@Override
		public void initialize() {
			// defaults
			defaults.put(logDirectoryField, Config.DEFAULT_LOG_DIRECTORY);
			defaults.put(recentParsedLogsLimitField, String.valueOf(Config.DEFAULT_RECENT_PARSED_LOGS_LIMIT));
			defaults.put(recentOpenedLogsLimitField, String.valueOf(Config.DEFAULT_RECENT_OPENED_LOGS_LIMIT));
			defaults.put(timeSyncEnabledButton, true);
			defaults.put(timeSyncHostField, Config.DEFAULT_TIME_SYNC_HOST);
			defaults.put(serverStoreEnabledButton, true);

			// initialize values
			final List<String> tzs = new ArrayList<>();
			for (final String tz : TimeZone.getAvailableIDs()) {
				if (!tz.contains("/") && !"UTC".equals(tz)) {
					continue;
				}
				if (!tz.startsWith("Africa")
						&& !tz.startsWith("America")
						&& !tz.startsWith("Australia")
						&& !tz.startsWith("Asia")
						&& !tz.startsWith("Atlantic")
						&& !tz.startsWith("Europe")
						&& !tz.startsWith("Indian")
						&& !tz.startsWith("Pacific")
						&& !tz.startsWith("UTC")) {
					continue;
				}

				tzs.add(tz);
			}
			Collections.sort(tzs);
			timezoneList.setItems(FXCollections.observableList(tzs));
			defaults.put(timezoneList, TimeUtils.getDefaultTimezone().getID());

			// validators
			validators.put(logDirectoryField, (Validator<TextField>) c
					-> c.getText() != null && !c.getText().isEmpty() && new File(c.getText()).isDirectory());

			final Validator<TextField> recentsValidator = c -> {
				if (c.getText() == null || c.getText().isEmpty()) {
					return false;
				}
				try {
					int x = Integer.parseInt(c.getText());
					return x > 0 && x < 20;
				} catch (NumberFormatException e) {
					return false;
				}
			};

			validators.put(recentParsedLogsLimitField, recentsValidator);
			validators.put(recentOpenedLogsLimitField, recentsValidator);

			validators.put(timeSyncHostField, (Validator<TextField>) c
					-> !timeSyncEnabledButton.isSelected()
					|| (c.getText() != null && !c.getText().isEmpty() && c.getText().length() >= 10 && c.getText().length() < 255));

			for (final Control control : validators.keySet()) {
				control.focusedProperty().addListener((val, oldVal, newVal) -> {
					if (!newVal) {
						validate(control);
					}
				});
			}

			final Validator<CheckBox> boolValidator = c -> true;
			validators.put(timeSyncEnabledButton, boolValidator);

			timeSyncEnabledButton.selectedProperty().addListener((arg0, oldVal, newVal) -> {
				validate(timeSyncEnabledButton);
				validate(timeSyncHostField);
			});

			timeSyncHostField.disableProperty().bind(timeSyncEnabledButton.selectedProperty().not());

			validators.put(serverStoreEnabledButton, boolValidator);
		}

		@Override
		public void loadCurrent() {
			current.put(logDirectoryField, config.getLogDirectory());
			current.put(recentParsedLogsLimitField, String.valueOf(config.getRecentParsedLogsLimit()));
			current.put(recentOpenedLogsLimitField, String.valueOf(config.getRecentOpenedLogsLimit()));
			current.put(timeSyncEnabledButton, config.isTimeSyncEnabled());
			current.put(timeSyncHostField, config.getTimeSyncHost());
			current.put(timezoneList, config.getTimezone());
			current.put(serverStoreEnabledButton, config.isStoreDataOnServerEnabled());
		}

		private void handleSave(final ActionEvent event) {
			if (!validate(logDirectoryField, recentParsedLogsLimitField, recentOpenedLogsLimitField, timeSyncEnabledButton,
					timeSyncHostField)) {
				setFlash("Please fix the highlighted fields");
				return;
			}

			config.setLogDirectory(logDirectoryField.getText());
			config.setRecentParsedLogsLimit(Integer.parseInt(recentParsedLogsLimitField.getText()));
			config.setRecentOpenedLogsLimit(Integer.parseInt(recentOpenedLogsLimitField.getText()));

			config.setTimeSyncEnabled(timeSyncEnabledButton.isSelected());
			if (timeSyncEnabledButton.isSelected()) {
				config.setTimeSyncHost(timeSyncHostField.getText());
			}
			config.setTimezone(timezoneList.getValue());
			TimeUtils.setCurrentTimezone(timezoneList.getValue());

			config.setStoreDataOnServerEnabled(serverStoreEnabledButton.isSelected());

			handleClose(event);
		}

		private void handleLogDirectory(@SuppressWarnings("unused") final ActionEvent event) {
			if (directoryChooser == null) {
				directoryChooser = new DirectoryChooser();
				directoryChooser.setTitle("Combat Log Directory");
			}

			File f = new File(logDirectoryField.getText());
			if (f.exists() && f.isDirectory()) {
				directoryChooser.setInitialDirectory(f);
			} else {
				directoryChooser.setInitialDirectory(null);
			}

			final File file = directoryChooser.showDialog(null);

			if (file != null) {
				logDirectoryField.setText(file.getPath());
			}
			validate(logDirectoryField);
		}
	}



	private class OverlaysSettings extends BaseSettings {

		private final List<Mode> raidHealingModes = new ArrayList<>(), personalModes = new ArrayList<>();

		private final Tooltip timersAnchor = new Tooltip();

		@Override
		public void initialize() {
			// defaults
			bindPreview(popoutBackgroundColor, ConfigPopoutDefault.DEFAULT_BACKGROUND);
			bindPreview(popoutTextColor, ConfigPopoutDefault.DEFAULT_TEXT);
			bindPreview(popoutDamageColor, ConfigPopoutDefault.DEFAULT_DAMAGE);
			bindPreview(popoutHealingColor, ConfigPopoutDefault.DEFAULT_HEALING);
			bindPreview(popoutThreatColor, ConfigPopoutDefault.DEFAULT_THREAT);
			bindPreview(popoutFriendlyColor, ConfigPopoutDefault.DEFAULT_FRIENDLY);

			bindSlider(raidDamageOpacitySlider, raidDamageOpacityText);
			bindSlider(raidHealingOpacitySlider, raidHealingOpacityText);
			bindSlider(raidThreatOpacitySlider, raidThreatOpacityText);
			bindSlider(raidBossOpacitySlider, raidBossOpacityText);
			bindSlider(raidChallengesOpacitySlider, raidChallengesOpacityText);
			bindSlider(timersOpacitySlider, timersOpacityText);
			bindSlider(personalOpacitySlider, personalOpacityText);

			bindPreview(raidDamageBars);
			bindPreview(raidHealingBars);
			bindPreview(raidThreatBars);
			bindPreview(raidBossBars);
			bindPreview(raidChallengesBars);
			bindPreview(timersBars);
			bindPreview(personalBars);

			bindPreview(raidHealingMode);
			bindPreview(personalMode);

			bindPreview(popoutSolid);

			defaults.put(timersCenter, false);
			validators.put(timersCenter, (Validator<CheckBox>) c -> true);
			timersCenter.selectedProperty().addListener((obsVal, oldVal, newVal) -> {
				timersCenterMoveButton.setDisable(!newVal);
				fireSettingsUpdated();
			});

			defaults.put(timersFractions, "");
			validators.put(timersFractions, (Validator<TextField>) c -> {
				if (c.getText() == null || c.getText().isEmpty()) {
					return true;
				}
				try {
					int x = Integer.parseInt(c.getText());
					return x >= 1 && x <= 99;
				} catch (NumberFormatException e) {
					return false;
				}
			});
			timersFractions.textProperty().addListener((obsVal, oldVal, newVal) -> {
				if (newVal == null) {
					return;
				}
				try {
					if (!validate(timersFractions)) {
						return;
					}
					fireSettingsUpdated();

				} catch (Exception ignored) {
				}
			});

			timersCenterMoveButton.setDisable(true);
			timersCenterMoveButton.setOnAction(new EventHandler<ActionEvent>() {

				private double initialPosX, initialPosY, initialMouseX, initialMouseY;

				final AnchorPane n = new AnchorPane();

				{
					n.setPrefWidth(150);
					n.setPrefHeight(80);
					n.setStyle("-fx-background-color: #00000066; -fx-border-color: red; -fx-border-width: 1px; -fx-padding: 10px");

					final Text sign = new Text("Move me");
					sign.setStyle("-fx-fill: #ffffffdd; -fx-font: bold 1.5em \"System\"");
					AnchorPane.setTopAnchor(sign, 5.0);
					AnchorPane.setLeftAnchor(sign, 27.0);

					final Button ok = new Button("Ok");
					ok.setCursor(Cursor.HAND);
					ok.setOnAction(new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent arg0) {
							cleanTimersCenterMove();
							timersCenterMoveButton.setUserData(new Double[]{timersAnchor.getX(), timersAnchor.getY()});
							fireSettingsUpdated();
						}
					});

					final Button cancel = new Button("Cancel");
					cancel.setCursor(Cursor.HAND);
					cancel.setOnAction(arg0 -> cleanTimersCenterMove());
					n.getChildren().setAll(sign, ok, cancel);
					n.setId("toolbar");

					AnchorPane.setBottomAnchor(ok, 0.0);
					AnchorPane.setLeftAnchor(ok, 10.0);
					AnchorPane.setBottomAnchor(cancel, 0.0);
					AnchorPane.setRightAnchor(cancel, 10.0);

					n.setCursor(Cursor.MOVE);
					n.setOnMousePressed(new EventHandler<MouseEvent>() {
						@Override
						public void handle(MouseEvent me) {
							if (me.getButton() == MouseButton.MIDDLE) {
								return;
							}
							initialPosX = timersAnchor.getX();
							initialPosY = timersAnchor.getY();
							initialMouseX = me.getScreenX();
							initialMouseY = me.getScreenY();
							me.consume();
						}
					});

					n.setOnMouseDragged(new EventHandler<MouseEvent>() {

						@Override
						public void handle(MouseEvent me) {
							if (me.getButton() == MouseButton.MIDDLE) {
								return;
							}
							timersAnchor.setX(getPosition(initialPosX + me.getScreenX() - initialMouseX, config.getPopoutSnap()));
							timersAnchor.setY(getPosition(initialPosY + me.getScreenY() - initialMouseY, config.getPopoutSnap()));
							me.consume();
						}
					});

					timersAnchor.setGraphic(n);
					timersAnchor.setGraphicTextGap(0);
					timersAnchor.setStyle("-fx-background-color: transparent; -fx-border-width: 0; "
							+ "-fx-padding: 0; -fx-background-radius: 0; "
							+ "-fx-background-insets: 0; -fx-effect: null");
				}

				private int getPosition(double pos, Integer snap) {
					if (snap == null || snap <= 0) {
						return (int) pos;
					}

					return (int) Math.round(pos / snap) * snap;
				}

				@Override
				public void handle(ActionEvent arg0) {
					final double x, y;
					if (timersCenterMoveButton.getUserData() != null && (((Double[]) timersCenterMoveButton.getUserData())[0] != null)) {
						x = ((Double[]) timersCenterMoveButton.getUserData())[0];
					} else {
						x = TimersCenterPopoutPresenter.getDefaultX();
					}
					if (timersCenterMoveButton.getUserData() != null && (((Double[]) timersCenterMoveButton.getUserData())[1] != null)) {
						y = ((Double[]) timersCenterMoveButton.getUserData())[1];
					} else {
						y = TimersCenterPopoutPresenter.getDefaultY();
					}

					timersAnchor.show(timersCenterMoveButton.getScene().getWindow(), x, y);
					timersCenterMoveButton.setDisable(true);
				}
			});

			defaults.put(lockOverlaysHotkey, "");
			validators.put(lockOverlaysHotkey, hotkeyValidator);
		}

		private void bindSlider(final Slider slider, final TextField text) {

			defaults.put(text, String.valueOf((int) (Config.DEFAULT_POPOUT_OPACITY * 100)));

			validators.put(text, (Validator<TextField>) c -> {
				if (c.getText() == null || c.getText().isEmpty()) {
					return false;
				}
				try {
					int x = Integer.parseInt(c.getText());
					return x > 0 && x <= 100;
				} catch (NumberFormatException e) {
					return false;
				}
			});

			slider.valueProperty().addListener((obsVal, oldVal, newVal) -> {
				text.setText(String.valueOf(Math.round((double) newVal)));
				fireSettingsUpdated();
			});

			text.textProperty().addListener((obsVal, oldVal, newVal) -> {
				if (newVal == null) {
					return;
				}
				try {
					if (!validate(text)) {
						return;
					}
					slider.adjustValue(Integer.parseInt(newVal));
					fireSettingsUpdated();

				} catch (Exception ignored) {
				}
			});
		}

		private void bindPreview(final ColorPicker control, final Color color) {

			defaults.put(control, color);

			validators.put(control, (Validator<ColorPicker>) c -> true);

			control.valueProperty().addListener((obsVal, oldVal, newVal) -> {
				if (validate(control)) {
					fireSettingsUpdated();
				}
			});

		}

		private void bindPreview(final CheckBox control) {

			defaults.put(control, control != timersCenter && control != popoutSolid);

			validators.put(control, (Validator<CheckBox>) c -> true);

			control.selectedProperty().addListener((obsVal, oldVal, newVal) -> {
				if (validate(control)) {
					fireSettingsUpdated();
				}
			});
		}

		private void bindPreview(final ChoiceBox<String> combo) {
			combo.valueProperty().addListener((arg0, arg1, arg2) -> fireSettingsUpdated());
		}

		@Override
		public void loadCurrent() {
			suppressEvents = true;
			current.put(popoutBackgroundColor, config.getPopoutDefault().getBackgroundColor());
			current.put(popoutTextColor, config.getPopoutDefault().getTextColor());
			current.put(popoutDamageColor, config.getPopoutDefault().getDamageColor());
			current.put(popoutHealingColor, config.getPopoutDefault().getHealingColor());
			current.put(popoutThreatColor, config.getPopoutDefault().getThreatColor());
			current.put(popoutFriendlyColor, config.getPopoutDefault().getFriendlyColor());

			loadPopoutCurrent("Raid Damage", raidDamageOpacitySlider, raidDamageOpacityText, raidDamageBars, null, null);
			loadPopoutCurrent("Raid Healing", raidHealingOpacitySlider, raidHealingOpacityText, raidHealingBars, raidHealingModes, raidHealingMode);
			loadPopoutCurrent("Raid Threat", raidThreatOpacitySlider, raidThreatOpacityText, raidThreatBars, null, null);
			loadPopoutCurrent("Raid Boss", raidBossOpacitySlider, raidBossOpacityText, raidBossBars, null, null);
			loadPopoutCurrent("Raid Challenges", raidChallengesOpacitySlider, raidChallengesOpacityText, raidChallengesBars, null, null);
			loadPopoutCurrent("Timers", timersOpacitySlider, timersOpacityText, timersBars, null, null);
			loadPopoutCurrent("Personal", personalOpacitySlider, personalOpacityText, personalBars, personalModes, personalMode);

			current.put(timersCenter, config.getCurrentCharacter().getPopout("Timers Center").isEnabled());
			current.put(timersCenterMoveButton, new Double[]{
					config.getCurrentCharacter().getPopout("Timers Center").getPositionX(),
					config.getCurrentCharacter().getPopout("Timers Center").getPositionY()
			});

			if (config.getPopoutDefault().getTimersFractions() != null) {
				current.put(timersFractions, String.valueOf(config.getPopoutDefault().getTimersFractions()));
			} else {
				current.put(timersFractions, "");
			}

			current.put(lockOverlaysHotkey, config.getlockOverlaysHotkey() == null ? "" : config.getlockOverlaysHotkey());

			current.put(popoutSolid, config.getPopoutDefault().getSolid());

			if (!config.getCurrentCharacter().getName().equals(Config.DEFAULT_CHARACTER)) {
				overlaysResetButton.setText("Reset Positions for " + config.getCurrentCharacter().getName());
				overlaysResetButton.setVisible(true);
			} else {
				overlaysResetButton.setVisible(false);
			}
			suppressEvents = false;
		}

		private void loadPopoutCurrent(String name, Slider opacitySlider, TextField opacityText, CheckBox bars, final List<Mode> modes,
				final ChoiceBox<String> mode) {

			Double opacity = config.getDefaultCharacter().getPopout(name).getOpacity();
			if (opacity == null) {
				opacity = Config.DEFAULT_POPOUT_OPACITY;
			}
			current.put(opacitySlider, opacity * 100);
			current.put(opacityText, String.valueOf((int) (opacity * 100)));
			if (bars != null) {
				if (config.getDefaultCharacter().getPopout(name).getBars() != null) {
					current.put(bars, config.getDefaultCharacter().getPopout(name).getBars());
				} else {
					current.put(bars, true);
				}
			}
			if (modes == null) {
				return;
			}

			if (modes.isEmpty()) {
				modes.addAll(BasePopoutPresenter.getPopoutModes(name));
				final List<String> items = new ArrayList<>();
				for (final Mode m : modes) {
					items.add(m.getTitle());
				}
				mode.setItems(FXCollections.observableList(items));
				defaults.put(mode, items.get(0));
			}
			final String c = config.getCurrentCharacter().getPopout(name).getMode();
			boolean found = false;
			if (c != null) {
				for (final Mode m : modes) {
					if (m.getMode().equals(c)) {
						mode.getSelectionModel().select(m.getTitle());
						current.put(mode, m.getTitle());
						found = true;
						break;
					}
				}
			}
			if (!found) {
				mode.getSelectionModel().select(mode.getItems().get(0));
				current.put(mode, mode.getItems().get(0));
			}
		}

		private Integer getSafeInt(final TextField control) {
			if (control.getText() == null || control.getText().isEmpty()) {
				return null;
			}
			if (!validate(control)) {
				return null;
			}
			return Integer.parseInt(control.getText());
		}

		private void handleSave(final ActionEvent event) {

			if (!validate(lockOverlaysHotkey)) {
				setFlash("Please enter valid keystroke ('control shift Z', 'alt 0' etc.)");
				return;
			}

			config.getPopoutDefault().setBackgroundColor(popoutBackgroundColor.getValue());
			config.getPopoutDefault().setTextColor(popoutTextColor.getValue());
			config.getPopoutDefault().setDamageColor(popoutDamageColor.getValue());
			config.getPopoutDefault().setHealingColor(popoutHealingColor.getValue());
			config.getPopoutDefault().setThreatColor(popoutThreatColor.getValue());
			config.getPopoutDefault().setFriendlyColor(popoutFriendlyColor.getValue());

			savePopoutCurrent("Raid Damage", raidDamageOpacitySlider.getValue() / 100, raidDamageBars.isSelected());
			savePopoutCurrent("Raid Healing", raidHealingOpacitySlider.getValue() / 100, raidHealingBars.isSelected());
			savePopoutCurrent("Raid Threat", raidThreatOpacitySlider.getValue() / 100, raidThreatBars.isSelected());
			savePopoutCurrent("Raid Boss", raidBossOpacitySlider.getValue() / 100, raidBossBars.isSelected());
			savePopoutCurrent("Raid Challenges", raidChallengesOpacitySlider.getValue() / 100, raidChallengesBars.isSelected());
			savePopoutCurrent("Timers", timersOpacitySlider.getValue() / 100, timersBars.isSelected());
			savePopoutCurrent("Personal", personalOpacitySlider.getValue() / 100, personalBars.isSelected());

			config.getCurrentCharacter().getPopout("Raid Healing").setMode(getMode(raidHealingMode, raidHealingModes));
			config.getCurrentCharacter().getPopout("Personal").setMode(getMode(personalMode, personalModes));

			config.getCurrentCharacter().getPopout("Timers Center").setEnabled(timersCenter.isSelected());
			if (timersCenterMoveButton.getUserData() != null) {
				config.getCurrentCharacter().getPopout("Timers Center").setPositionX(((Double[]) timersCenterMoveButton.getUserData())[0]);
				config.getCurrentCharacter().getPopout("Timers Center").setPositionY(((Double[]) timersCenterMoveButton.getUserData())[1]);
			}

			final String oldHotkey = config.getlockOverlaysHotkey() == null || config.getlockOverlaysHotkey().isEmpty() ? null : config.getlockOverlaysHotkey();
			final String newHotkey = lockOverlaysHotkey.getText() == null || lockOverlaysHotkey.getText().isEmpty() ? null : lockOverlaysHotkey.getText();
			config.setlockOverlaysHotkey(newHotkey);

			if (listener != null) {
				listener.onHotkeyUpdated(Config.Hotkey.LOCK_OVERLAYS, oldHotkey, newHotkey);
			}

			config.getPopoutDefault().setTimersFractions(getSafeInt(timersFractions));
			config.getPopoutDefault().setSolid(popoutSolid.isSelected());

			isDirty = false;
			handleClose(event);
		}

		private void savePopoutCurrent(String name, double opacity, Boolean bars) {
			config.getDefaultCharacter().getPopout(name).setOpacity(opacity);
			config.getDefaultCharacter().getPopout(name).setBars(bars);
		}

		private void resetAllPopouts() {
			config.getCurrentCharacter().resetAllPopouts();
			current.put(timersCenterMoveButton, null);
			timersCenterMoveButton.setUserData(null);
			if (listener != null) {
				listener.onOverlaysReset(config.getCurrentCharacter().getName());
			}
		}

		private void cleanup() {
			if (isDirty) {
				loadValues(current);
				fireSettingsUpdated();
				isDirty = false;
			}
			cleanTimersCenterMove();
		}

		private void cleanTimersCenterMove() {
			if (timersAnchor.isShowing()) {
				timersAnchor.hide();
			}
			timersCenterMoveButton.setDisable(!timersCenter.isSelected());
		}

		private void fireSettingsUpdated() {
			if (!suppressEvents && listener != null) {
				listener.onOverlaysSettings(
						popoutBackgroundColor.getValue(),
						popoutTextColor.getValue(),
						popoutDamageColor.getValue(),
						popoutHealingColor.getValue(),
						popoutThreatColor.getValue(),
						popoutFriendlyColor.getValue(),
						//
						raidDamageOpacitySlider.getValue() / 100, raidDamageBars.isSelected(),
						raidHealingOpacitySlider.getValue() / 100, raidHealingBars.isSelected(), getMode(raidHealingMode, raidHealingModes),
						raidThreatOpacitySlider.getValue() / 100, raidThreatBars.isSelected(),
						raidBossOpacitySlider.getValue() / 100, raidBossBars.isSelected(),
						raidChallengesOpacitySlider.getValue() / 100, raidChallengesBars.isSelected(),
						timersOpacitySlider.getValue() / 100, timersBars.isSelected(),
						personalOpacitySlider.getValue() / 100, personalBars.isSelected(), getMode(personalMode, personalModes),
						//
						timersCenter.isSelected(),
						timersCenterMoveButton.getUserData() != null ? ((Double[]) timersCenterMoveButton.getUserData())[0] : null,
						timersCenterMoveButton.getUserData() != null ? ((Double[]) timersCenterMoveButton.getUserData())[1] : null,
						getSafeInt(timersFractions),
						popoutSolid.isSelected());
			}
			isDirty = true;
		}

		private String getMode(ChoiceBox<String> combo, List<Mode> modes) {
			for (final Mode m : modes) {
				if (m.getTitle().equals(combo.getValue())) {
					return m.getMode();
				}
			}
			return null;
		}
	}

	private class UploadSettings extends BaseSettings {

		@Override
		public void initialize() {
			final List<String> servers = new ArrayList<>();
			for (final ServerName sn : ServerName.values()) {
				if (sn.getActive().equals(sn)) {
					servers.add(sn.getName());
				}
			}
			Collections.sort(servers);
			serverList.setItems(FXCollections.observableList(servers));
		}

		@Override
		public void loadCurrent() {
			current.put(serverList, config.getCurrentCharacter().getServer());
			current.put(guildField, config.getCurrentCharacter().getGuild());

			current.put(parselyLoginField, config.getParselyLogin());
			current.put(parselyPasswordField, config.getParselyPassword());

			if (!config.getCurrentCharacter().getName().equals(Config.DEFAULT_CHARACTER)) {
				serverLabel.setText(config.getCurrentCharacter().getName() + " Server");
			} else {
				serverLabel.setText("Default Server");
			}
		}

		private void handleSave(final ActionEvent event) {

			config.getCurrentCharacter().setServer(serverList.getValue());
			config.getCurrentCharacter().setGuild(guildField.getText());

			config.setParselyLogin(parselyLoginField.getText());
			config.setParselyPassword(parselyPasswordField.getText());

			if (listener != null) {
				listener.onUploadUpdated();
			}

			handleClose(event);
		}
	}

	private class TimersSettings extends BaseSettings {

		private static final String EMPTY_VALUE = "-", SOUND_FILE = "Pick custom file";

		private boolean suppressEvents = false;

		private FileChooser fileChooser;

		private ConfigTimer currentTimer;

		private final TreeItem<TimerNode> rootItem = new TreeItem<>(new TimerNode("Timers"));

		private final List<ConfigTimer> pendingImportedTimers = new ArrayList<>();

		private final Map<Condition.Type, List<? extends Node>> controls = new HashMap<>();

		@Override
		public void initialize() {
			// setup tree
			final ContextMenu treeMenu = new ContextMenu();
			final MenuItem addItem = new MenuItem("Add new Folder");
			treeMenu.getItems().add(addItem);
			addItem.setOnAction(arg0 -> {
				timersList.getSelectionModel().clearSelection();
				final TreeItem<TimerNode> newFolder = new TreeItem<>(new TimerNode("New folder"));
				rootItem.getChildren().add(newFolder);
				timersList.setRoot(rootItem);
				sortAllItems();
				timersList.getSelectionModel().select(newFolder);
			});
			timersList.setContextMenu(treeMenu);

			timersList.setCellFactory(TimerCell::new);
			timersList.getSelectionModel().selectedItemProperty().addListener((arg0, arg1, newNode) -> {
				if (newNode == null || newNode.getValue().isFolder()) {
					resetForm(null);
					return;
				}
				if (newNode.getValue().timer != null && !newNode.getValue().timer.equals(currentTimer)) {
					resetForm(newNode.getValue().timer);
				}
			});

			timersList.setRoot(rootItem);
			timersList.setEditable(true);
			rootItem.setExpanded(true);

			// setup relevant controls
			controls.put(Condition.Type.ABILITY_ACTIVATED, Arrays.asList(timerSourceContainer, timerAbilityContainer));
			controls.put(Condition.Type.COMBAT_START, null);
			// controls.put(Condition.Type.COMBAT_END, null);
			controls.put(Condition.Type.DAMAGE, Arrays.asList(timerSourceContainer, timerTargetContainer, timerAbilityContainer));
			controls.put(Condition.Type.HEAL, controls.get(Condition.Type.DAMAGE));
			controls.put(Condition.Type.EFFECT_GAINED, Arrays.asList(timerSourceContainer, timerTargetContainer, timerAbilityContainer, timerEffectLabel, timerEffectName));
			controls.put(Condition.Type.EFFECT_LOST, controls.get(Condition.Type.EFFECT_GAINED));
			controls.put(Condition.Type.TIMER_STARTED, Arrays.asList(timerTriggerTimerLabel, timerTriggerTimer));
			controls.put(Condition.Type.TIMER_FINISHED, controls.get(Condition.Type.TIMER_STARTED));

			// initialize values
			final List<String> triggers = new ArrayList<>();
			for (final Condition.Type type : controls.keySet()) {
				triggers.add(type.getLabel());
			}
			Collections.sort(triggers);
			timerTrigger.setItems(FXCollections.observableList(triggers));
			timerTrigger.valueProperty().addListener((arg0, oldVal, newVal) -> {
				if (newVal != null) {
					final Condition.Type t = Condition.Type.parse(newVal);
					if (t != null) {
						resetCondition(t);
						return;
					}
				}
				resetCondition(null);
			});

			final List<String> bosses = new ArrayList<>();
			bosses.add(EMPTY_VALUE);
			for (final RaidBoss boss : Helpers.getRaidBosses()) {
				if (bosses.contains(boss.getName())) {
					continue;
				}
				bosses.add(boss.getName());
			}
			Collections.sort(bosses);
			timerBoss.setItems(FXCollections.observableList(bosses));
			timerSlot.setItems(FXCollections.observableList(Arrays.stream(ConfigTimer.Slot.values()).map(Enum::name).collect(Collectors.toList())));

			final List<String> sounds = new ArrayList<>();
			sounds.add(SOUND_FILE);
			for (final File sound : SoundManager.getDefaultSounds(null)) {
				sounds.add(sound.getName());
			}
			timerSoundFile.setItems(FXCollections.observableList(sounds));
			timerSoundFile.valueProperty().addListener((arg0, oldVal, newVal) -> {
				if (suppressEvents) {
					return;
				}
				if (SOUND_FILE.equals(newVal)) {
					handleCustomSoundFile();

				} else if (newVal != null && (currentTimer == null || oldVal != null || !newVal.equals(currentTimer.getAudio()))) {
					stopAllSounds();
					SoundManager.play(newVal, timerSoundVolume.getValue());
				}
			});

			timerPlaySound.selectedProperty().addListener((arg0, arg1, newVal) -> {
				timerSoundFile.setDisable(!newVal);
				timerSoundButton.setDisable(!newVal);
				timerSoundVolume.setDisable(!newVal);
				timerSoundOffset.setDisable(!newVal);
				timerSoundOffsetLabel.getStyleClass().clear();
				if (!newVal) {
					timerSoundOffsetLabel.getStyleClass().add("disabled");
				}
			});

			timerSoundButton.setOnMouseClicked(arg0 -> {
				if (suppressEvents) {
					return;
				}
				if (timerSoundFile.getValue() != null && !SOUND_FILE.equals(timerSoundFile.getValue())) {
					stopAllSounds();
					SoundManager.play(timerSoundFile.getValue(), timerSoundVolume.getValue());
				}
			});

			final List<String> voices = new ArrayList<>(SoundManager.getDefaultVoices());
			timerCountdownVoice.setItems(FXCollections.observableList(voices));

			timerPlayCountdown.selectedProperty().addListener((arg0, arg1, newVal) -> {
				timerCountdownVoice.setDisable(!newVal);
				timerCountdownButton.setDisable(!newVal);
				timerCountdownVolume.setDisable(!newVal);
				timerCountdownCount.setDisable(!newVal);
			});

			timerCountdownButton.setOnMouseClicked(arg0 -> {
				if (timerCountdownVoice.getValue() != null && !SOUND_FILE.equals(timerCountdownVoice.getValue())) {
					stopAllSounds();
					playCountdown(timerCountdownVoice.getValue());
				}
			});

			timerDisplay.selectedProperty().addListener((arg0, arg1, newVal) -> {
				timerColor.setDisable(!newVal);
				timerSlot.setDisable(!newVal);
			});

			timersIgnoreRepeated.setTooltip(new Tooltip("If selected, timer will NOT be restarted every time a trigger occurs"));
			timersShowSource.setTooltip(new Tooltip("If selected, timer name will display the caster"));

			// validators
			validators.put(timerName, (Validator<TextField>) c -> {
				if (c.getText() == null || c.getText().isEmpty()) {
					return false;
				}
				for (final ConfigTimer other : config.getConfigTimers().getTimers()) {
					if (other.getName().equals(timerName.getText()) && !other.equals(currentTimer)) {
						return false;
					}
				}
				return true;
			});
			validators.put(timerTriggerTimer, (Validator<ComboBox<String>>) c -> {
				if (!timerTriggerContainer.isVisible()) {
					return true;
				}
				return !(Condition.Type.TIMER_STARTED.getLabel().equals(timerTrigger.getValue())
						|| Condition.Type.TIMER_FINISHED.getLabel().equals(timerTrigger.getValue()))
						|| (timerTriggerTimer.getValue() != null && !timerTriggerTimer.getValue().equals(EMPTY_VALUE));
			});
			validators.put(timerSourceName, (Validator<TextField>) c
					-> !timerSourceCustom.isSelected()
					|| (c.getText() != null && !c.getText().isEmpty() && c.getText().length() < 255));
			validators.put(timerTargetName, (Validator<TextField>) c
					-> !timerTargetCustom.isSelected()
					|| (c.getText() != null && !c.getText().isEmpty() && c.getText().length() < 255));
			validators.put(timerAbilityName, (Validator<TextField>) c
					-> !Condition.Type.ABILITY_ACTIVATED.getLabel().equals(timerTrigger.getValue())
					|| (c.getText() != null && !c.getText().isEmpty() && c.getText().length() < 255));
			validators.put(timerEffectName, (Validator<TextField>) c
					-> !(Condition.Type.EFFECT_GAINED.getLabel().equals(timerTrigger.getValue())
					|| Condition.Type.EFFECT_LOST.getLabel().equals(timerTrigger.getValue()))
					|| (c.getText() != null && !c.getText().isEmpty() && c.getText().length() < 255));

			validators.put(timerDuration, (Validator<TextField>) c -> {
				if (c.getText() == null || c.getText().isEmpty()) {
					return true;
				}
				try {
					double x = safeDouble(c.getText());
					return x >= 0 && x <= 1000;
				} catch (NumberFormatException e) {
					return false;
				}
			});
			validators.put(timerRepeat, (Validator<TextField>) c -> {
				if (c.getText() == null || c.getText().isEmpty()) {
					return true;
				}
				try {
					int x = Integer.parseInt(c.getText());
					return x >= 0 && x <= 999;
				} catch (NumberFormatException e) {
					return false;
				}
			});
			validators.put(timerSoundOffset, (Validator<TextField>) c -> {
				if (c.getText() == null || c.getText().isEmpty()) {
					return true;
				}
				try {
					int x = Integer.parseInt(c.getText());
					if (x == 0) {
						c.setText(null);
						return true;
					}
					if (x < 0) {
						x = -x;
						c.setText(String.valueOf(x));
					}
					return x >= 1 && x <= 1000;
				} catch (NumberFormatException e) {
					return false;
				}
			});
			validators.put(timerCountdownCount, (Validator<TextField>) c -> {
				if (c.getText() == null || c.getText().isEmpty()) {
					return true;
				}
				try {
					int x = Integer.parseInt(c.getText());
					return x >= 1 && x <= 10;
				} catch (NumberFormatException e) {
					return false;
				}
			});
		}

		@Override
		public void loadCurrent() {
			suppressEvents = true;
			// load current timers
			rootItem.getChildren().clear();

			for (final ConfigTimer timer : config.getConfigTimers().getTimers()) {
				TreeItem<TimerNode> appendTo = null;
				if (timer.getFolder() != null && !timer.getFolder().isEmpty()) {
					for (TreeItem<TimerNode> item : rootItem.getChildren()) {
						if (item.getValue().isFolder()
								&& item.getValue().folderName != null
								&& item.getValue().folderName.equals(timer.getFolder())) {
							// found
							appendTo = item;
							break;
						}
					}
					if (appendTo == null) {
						// folder does not exist
						appendTo = new TreeItem<>(new TimerNode(timer.getFolder()));
						appendTo.setExpanded(!timer.isSystem());
						rootItem.getChildren().add(appendTo);
					}

				} else {
					// no folder, straight to root
					appendTo = rootItem;
				}
				appendTo.getChildren().add(new TreeItem<>(new TimerNode(timer)));
			}
			// sort them
			sortAllItems();

			resetForm(null);
		}

		private void sortAllItems() {
			for (final TreeItem<TimerNode> item : rootItem.getChildren()) {
				if (item.isLeaf()) {
					continue;
				}
				sortItems(item);
			}
			sortItems(rootItem);
		}

		private void resetCondition(Condition.Type selectedType) {
			// hide all conditional items
			for (final Condition.Type type : controls.keySet()) {
				if (controls.get(type) == null) {
					continue;
				}
				for (final Node node : controls.get(type)) {
					node.setVisible(false);
					if (node instanceof AnchorPane) {
						((AnchorPane) node).setPrefHeight(0);
					}
				}
			}
			if (selectedType == null) {
				return;
			}

			// and show those relevant
			for (final Condition.Type type : controls.keySet()) {
				if (controls.get(type) == null) {
					continue;
				}
				for (final Node node : controls.get(type)) {
					if (type.equals(selectedType)) {
						node.setVisible(true);
						if (node instanceof AnchorPane) {
							((AnchorPane) node).setPrefHeight(30);
						}
					}
				}
			}
		}

		private List<String> getAvailableTimers(final ConfigTimer currentTimer) {
			final List<String> timers = new ArrayList<>();
			for (final ConfigTimer timer : config.getConfigTimers().getTimers()) {
				if (timer.isSystem()) {
					continue;
				}
				if (timer.equals(currentTimer)) {
					continue;
				}
				timers.add(timer.getName());
			}
			timers.sort(String::compareTo);
			return timers;
		}

		private void resetForm(final ConfigTimer timer) {
			suppressEvents = true;
			resetCondition(null);

			timerName.setText(null);
			timerName.setPrefWidth(120.0);
			timerName.setDisable(false);
			timerBoss.setVisible(true);
			timerBossLabel.setVisible(true);
			timerBoss.getSelectionModel().clearSelection();
			timerTriggerContainer.setVisible(true);
			timerTriggerContainer.setPrefHeight(30);
			timerTrigger.setVisible(true);
			timerTrigger.getSelectionModel().clearSelection();

			timerTriggerTimer.getSelectionModel().clearSelection();
			final List<String> trigger = new ArrayList<>();
			trigger.add(EMPTY_VALUE);
			trigger.addAll(getAvailableTimers(timer));
			timerTriggerTimer.setItems(FXCollections.observableList(trigger));

			timerSource.selectToggle(null);
			timerSourceName.setText(null);
			timerTarget.selectToggle(null);
			timerTargetName.setText(null);

			timerAbilityName.setText(null);
			timerEffectName.setText(null);

			timerIntervalContainer.setVisible(true);
			timerIntervalContainer.setPrefHeight(30);
			timerDuration.setVisible(true);
			timerDuration.setText(null);
			timerRepeat.setVisible(true);
			timerRepeat.setText(null);

			timerDisplay.setSelected(true);
			timerDisplay.setSelected(false);
			timerColor.setValue(Color.WHEAT);
			timerColor.fireEvent(new ActionEvent());
			timerSlot.setValue(ConfigTimer.Slot.A.name());

			timerPlaySound.setSelected(true);
			timerPlaySound.setSelected(false);
			timerSoundOffset.setText(null);
			timerSoundFile.getSelectionModel().clearSelection();
			timerSoundVolume.setValue(80);

			timerPlayCountdown.setSelected(true);
			timerPlayCountdown.setSelected(false);
			timerCountdownVoice.getSelectionModel().clearSelection();
			timerCountdownVolume.setValue(80);
			timerCountdownCount.setText("5");

			timerCancel.getSelectionModel().clearSelection();
			final List<String> cancel = new ArrayList<>();
			cancel.add(EMPTY_VALUE);
			cancel.add(Condition.Type.COMBAT_END.getLabel());
			cancel.addAll(getAvailableTimers(timer));
			timerCancel.setItems(FXCollections.observableList(cancel));
			timerCancelContainer.setVisible(true);
			timerCancelContainer.setPrefHeight(30);
			timersIgnoreRepeated.setSelected(false);
			timersShowSource.setSelected(false);

			if (timer != null) {
				loadTimer(timer);
				validateAll();
				timerSaveButton.setText("Save changes");
				if (!timer.isSystem() || timer.isSystemModified()) {
					timerCopyButton.setVisible(true);
				} else {
					timerCopyButton.setVisible(false);
				}
				if (timer.isSystem()) {
					timerCopyButton.setText("Reset timer");
				} else {
					timerCopyButton.setText("Save as copy");
				}

			} else {
				currentTimer = null;
				timerSaveButton.setText("Save as new");
				timerCopyButton.setVisible(false);
				validateAll();
				timerName.getStyleClass().remove("input-error");
				timerTrigger.getStyleClass().remove("input-error");
			}
			setFlash(null);
			suppressEvents = false;
		}

		private void loadTimer(final ConfigTimer timer) {
			currentTimer = timer;

			timerName.setText(timer.getName());

			if (timer.isSystem()) {
				timerTriggerContainer.setVisible(false);
				timerTriggerContainer.setPrefHeight(0.0);
				timerName.setPrefWidth(220.0);
				timerName.setDisable(true);
				timerBoss.setVisible(false);
				timerBossLabel.setVisible(false);
				timersShowSource.setVisible(false);
			} else if (timer.getTrigger() != null && timer.getTrigger().getType() != null) {
				timerTrigger.getSelectionModel().select(timer.getTrigger().getType().getLabel());
				timersShowSource.setVisible(true);
			}

			if (timer.getTrigger() != null) {
				if (timer.getTrigger().getBoss() != null) {
					timerBoss.getSelectionModel().select(timer.getTrigger().getBoss());
				}

				if (timer.getTrigger().getTimer() != null) {
					timerTriggerTimer.getSelectionModel().select(timer.getTrigger().getTimer());
				}

				if (timer.getTrigger().getSourceGuid() != null) {
					timerSource.selectToggle(timerSourceCustom);
					timerSourceName.setText(String.valueOf(timer.getTrigger().getSourceGuid()));

				} else if (timer.getTrigger().getSource() != null) {
					if (Condition.OTHER.equals(timer.getTrigger().getSource())) {
						timerSource.selectToggle(timerSourceOther);

					} else if (Condition.SELF.equals(timer.getTrigger().getSource())) {
						timerSource.selectToggle(timerSourceYou);

					} else {
						timerSource.selectToggle(timerSourceCustom);
						timerSourceName.setText(timer.getTrigger().getSource());
					}
				}

				if (timer.getTrigger().getTargetGuid() != null) {
					timerTarget.selectToggle(timerTargetCustom);
					timerTargetName.setText(String.valueOf(timer.getTrigger().getTargetGuid()));

				} else if (timer.getTrigger().getTarget() != null) {
					if (Condition.OTHER.equals(timer.getTrigger().getTarget())) {
						timerTarget.selectToggle(timerTargetOther);

					} else if (Condition.SELF.equals(timer.getTrigger().getTarget())) {
						timerTarget.selectToggle(timerTargetYou);

					} else {
						timerTarget.selectToggle(timerTargetCustom);
						timerTargetName.setText(timer.getTrigger().getTarget());
					}
				}

				if (timer.getTrigger().getAbilityGuid() != null) {
					timerAbilityName.setText(String.valueOf(timer.getTrigger().getAbilityGuid()));

				} else if (timer.getTrigger().getAbility() != null) {
					timerAbilityName.setText(timer.getTrigger().getAbility());
				}

				if (timer.getTrigger().getEffectGuid() != null) {
					timerEffectName.setText(String.valueOf(timer.getTrigger().getEffectGuid()));

				} else if (timer.getTrigger().getEffect() != null) {
					timerEffectName.setText(timer.getTrigger().getEffect());
				}
			}

			if (timer.isSystem()) {
				timerIntervalContainer.setVisible(false);
				timerIntervalContainer.setPrefHeight(0.0);
			} else if (timer.getInterval() != null) {
				final String n = String.valueOf(timer.getInterval());
				timerDuration.setText(n.endsWith(".0") || n.endsWith(",0") ? n.substring(0, n.length() - 2) : n);
			}

			if (timer.getRepeat() != null) {
				timerRepeat.setText(String.valueOf(timer.getRepeat()));
			}

			if (timer.getColor() != null) {
				timerDisplay.setSelected(true);
				timerColor.setValue(timer.getColor());
				timerColor.fireEvent(new ActionEvent());
			}

			timerSlot.setValue((timer.getSlot() == null ? ConfigTimer.Slot.A : timer.getSlot()).name());

			if (timer.getAudio() != null) {
				timerPlaySound.setSelected(true);
				timerSoundFile.getSelectionModel().select(timer.getAudio());
				timerSoundOffset.setText(timer.getSoundOffset() == null ? null : String.valueOf(timer.getSoundOffset()));

				if (timer.getVolume() != null) {
					timerSoundVolume.setValue(timer.getVolume());
				}
			}

			if (timer.getCountdownVoice() != null) {
				timerPlayCountdown.setSelected(true);
				timerCountdownVoice.getSelectionModel().select(timer.getCountdownVoice());

				if (timer.getCountdownVolume() != null) {
					timerCountdownVolume.setValue(timer.getCountdownVolume());
				}
				if (timer.getCountdownCount() != null) {
					timerCountdownCount.setText(String.valueOf(timer.getCountdownCount()));
				}
			}

			if (timer.isSystem()) {
				timerCancelContainer.setVisible(false);
				timerCancelContainer.setPrefHeight(0);
			} else {
				timersIgnoreRepeated.setSelected(timer.isIgnoreRepeated());
				timersShowSource.setSelected(timer.isShowSource());
			}

			if (timer.getCancel() != null) {
				switch (timer.getCancel().getType()) {
					case COMBAT_END:
						timerCancel.getSelectionModel().select(Condition.Type.COMBAT_END.getLabel());
						break;
					case TIMER_STARTED:
					case TIMER_FINISHED:
						if (timer.getCancel().getTimer() != null) {
							timerCancel.getSelectionModel().select(timer.getCancel().getTimer());
						}
					default:
				}
			}
		}

		@Override
		public boolean validateAll() {
			return validate(timerName, timerTriggerTimer,
					timerSourceName, timerTargetName,
					timerAbilityName, timerEffectName,
					timerDuration, timerRepeat,
					timerSoundOffset, timerCountdownCount);
		}

		private void handleSave(@SuppressWarnings("unused") final ActionEvent event, boolean asCopy) {
			if (currentTimer != null && asCopy && currentTimer.isSystem()) {
				// reset and save
				Objects.requireNonNull(TimerManager.getSystemTimer(currentTimer)).fillConfig(currentTimer);

				fireSaveAndRefresh("Timer " + currentTimer.getName() + " reset");
				return;
			}
			if (!validateAll()) {
				if (!validate(timerName) && timerName.getText() != null && !timerName.getText().isEmpty()) {
					setFlash("Please enter a unique name");
					return;
				}
				setFlash("Please fix the highlighted fields");
				return;
			}
			if (asCopy) {
				for (final ConfigTimer other : config.getConfigTimers().getTimers()) {
					if (other.getName().equals(timerName.getText())) {
						timerName.setText(timerName.getText() + " (2)");
					}
				}
			}

			if (currentTimer == null || asCopy) {
				// creating new
				final String folder;
				final TreeItem<TimerNode> selectedFolder = timersList.getSelectionModel().getSelectedItem();
				if (selectedFolder != null && selectedFolder.getValue().isFolder() && !selectedFolder.getValue().isSystemFolder()) {
					folder = selectedFolder.getValue().folderName;
				} else if (asCopy && currentTimer != null) {
					folder = currentTimer.getFolder();
				} else {
					folder = null;
				}
				currentTimer = new ConfigTimer();
				currentTimer.setFolder(folder);
				currentTimer.setEnabled(true);
				config.getConfigTimers().getTimers().add(currentTimer);
			}
			currentTimer.setName(timerName.getText());

			if (currentTimer.isSystem()) {
				// do nothing
			} else {
				if (!handleCustomTimer()) {
					return;
				}
			}

			if (timerDisplay.isSelected() && timerColor.getValue() != null) {
				currentTimer.setColor(timerColor.getValue());
			} else {
				currentTimer.setColor(null);
			}

			currentTimer.setSlot(timerSlot.getValue() == null ? null : ConfigTimer.Slot.valueOf(timerSlot.getValue()));

			if (timerPlaySound.isSelected() && timerSoundFile.getValue() != null) {
				currentTimer.setAudio(timerSoundFile.getValue());
				currentTimer.setVolume((int) (timerSoundVolume.getValue()));
				if (timerSoundOffset.getText() != null && !timerSoundOffset.getText().isEmpty()) {
					currentTimer.setSoundOffset(Integer.parseInt(timerSoundOffset.getText()));
				} else {
					currentTimer.setSoundOffset(null);
				}
			} else {
				currentTimer.setAudio(null);
				currentTimer.setSoundOffset(null);
				currentTimer.setVolume(null);
			}

			if (timerPlayCountdown.isSelected() && timerCountdownVoice.getValue() != null) {
				currentTimer.setCountdownVoice(timerCountdownVoice.getValue());
				if (!timerCountdownCount.getText().isEmpty()) {
					currentTimer.setCountdownCount(Integer.parseInt(timerCountdownCount.getText()));
				} else {
					currentTimer.setCountdownCount(5);
				}
				currentTimer.setCountdownVolume((int) (timerCountdownVolume.getValue()));
			} else {
				currentTimer.setCountdownVoice(null);
				currentTimer.setCountdownCount(null);
				currentTimer.setCountdownVolume(null);
			}
			if (Boolean.TRUE.equals(currentTimer.getIsPreview())) {
				currentTimer.setIsPreview(false);
				pendingImportedTimers.remove(currentTimer);
			}

			// reload
			fireSaveAndRefresh("Timer " + currentTimer.getName() + " saved");
		}

		private void fireSaveAndRefresh(String message) {
			// store as it will disappear
			final String newName = currentTimer == null ? null : currentTimer.getName();
			loadCurrent();
			// will fire reset
			if (newName != null) {
				timersList.getSelectionModel().select(searchTimerNode(rootItem, newName));
			}
			setFlash(message, FlashMessage.Type.SUCCESS);

			if (listener != null) {
				listener.onTimersUpdated();
			}

			// save now
			Marshaller.storeToFile(config.getConfigTimers(), StarparseApp.CONFIG_TIMERS_FILE);
			timersImportButton.setUserData(null); // force new eval
		}

		private boolean handleCustomTimer() {
			final Condition.Type type = Condition.Type.parse(timerTrigger.getValue());
			if (type == null) {
				setFlash("Please choose trigger type");
				return false;
			}

			final Condition trigger = new Condition();
			if (timerBoss.getValue() != null && !EMPTY_VALUE.equals(timerBoss.getValue())) {
				trigger.setBoss(timerBoss.getValue());
			}
			trigger.setType(type);

			switch (type) {
				case TIMER_FINISHED:
				case TIMER_STARTED:
					trigger.setTimer(timerTriggerTimer.getValue());
					break;
				case COMBAT_END:
				case COMBAT_START:
					break;
				case HOTKEY:
					// TODO
					break;
				default:
					if (!timerSourceContainer.isVisible()) {
						// nothing

					} else if (timerSourceYou.isSelected()) {
						trigger.setSource(Condition.SELF);

					} else if (timerSourceOther.isSelected()) {
						trigger.setSource(Condition.OTHER);

					} else if (timerSourceCustom.isSelected() && timerSourceCustom.getText() != null && !timerSourceCustom.getText().isEmpty()) {
						try {
							trigger.setSourceGuid(Long.valueOf(timerSourceName.getText()));
						} catch (Exception e) {
							trigger.setSource(timerSourceName.getText());
						}
					}

					if (!timerTargetContainer.isVisible()) {
						// nothing

					} else if (timerTargetYou.isSelected()) {
						trigger.setTarget(Condition.SELF);

					} else if (timerTargetOther.isSelected()) {
						trigger.setTarget(Condition.OTHER);

					} else if (timerTargetCustom.isSelected() && timerTargetCustom.getText() != null && !timerTargetCustom.getText().isEmpty()) {
						try {
							trigger.setTargetGuid(Long.valueOf(timerTargetName.getText()));
						} catch (Exception e) {
							trigger.setTarget(timerTargetName.getText());
						}
					}

					if (timerAbilityContainer.isVisible() && timerAbilityName.getText() != null && !timerAbilityName.getText().isEmpty()) {
						try {
							trigger.setAbilityGuid(Long.valueOf(timerAbilityName.getText()));
						} catch (Exception e) {
							trigger.setAbility(timerAbilityName.getText());
						}
					}

					if (timerEffectName.isVisible() && timerEffectName.getText() != null && !timerEffectName.getText().isEmpty()) {
						try {
							trigger.setEffectGuid(Long.valueOf(timerEffectName.getText()));
						} catch (Exception e) {
							trigger.setEffect(timerEffectName.getText());
						}
					}
			}
			currentTimer.setTrigger(trigger);

			if (timerDuration.getText() != null && !timerDuration.getText().isEmpty()) {
				currentTimer.setInterval(safeDouble(timerDuration.getText()));
			} else {
				currentTimer.setInterval(0.0);
			}

			if (timerRepeat.getText() != null && !timerRepeat.getText().isEmpty()) {
				currentTimer.setRepeat(Integer.parseInt(timerRepeat.getText()));
			} else {
				currentTimer.setRepeat(null);
			}

			final Condition cancel;
			if (timerCancel.getValue() != null && !timerCancel.getValue().isEmpty() && !EMPTY_VALUE.equals(timerCancel.getValue())) {
				cancel = new Condition();
				if (Condition.Type.COMBAT_END.getLabel().equals(timerCancel.getValue())) {
					cancel.setType(Condition.Type.COMBAT_END);
				} else {
					cancel.setType(Condition.Type.TIMER_STARTED);
					cancel.setTimer(timerCancel.getValue());
				}
			} else {
				cancel = null;
			}
			currentTimer.setCancel(cancel);
			currentTimer.setIgnoreRepeated(timersIgnoreRepeated.isSelected());
			currentTimer.setShowSource(timersShowSource.isSelected());
			return true;
		}

		private void handleCustomSoundFile() {
			if (fileChooser == null) {
				fileChooser = new FileChooser();
				fileChooser.setTitle("Custom Sound File");
				fileChooser.getExtensionFilters().add(new ExtensionFilter("SoundFiles", Arrays.asList("*.wav", "*.mp3", "*.mp4")));
			}

			final File file = fileChooser.showOpenDialog(null);
			if (file != null) {
				// avoid race condition with change handler
				Platform.runLater(() -> timerSoundFile.setValue(file.getAbsolutePath()));
			}
		}

		private Thread player;

		private void playCountdown(final String voice) {
			Integer s = null;
			try {
				s = Integer.parseInt(timerCountdownCount.getText());
			} catch (Exception ignored) {
			}
			if (s == null || s < 1 || s > 10) {
				s = 5;
			}
			final int samples = s;
			player = new Thread() {
				public void run() {
					setName("Countdown " + voice + " " + samples);
					int i = samples;
					do {
						try {
							SoundManager.play(i, voice, timerCountdownVolume.getValue());
							Thread.sleep(1000);
						} catch (Exception e) {
							break;
						}

					} while (--i > 0);
				}
			};
			player.start();
		}

		private void stopAllSounds() {
			if (player != null && player.isAlive()) {
				player.interrupt();
				player = null;
			}
			SoundManager.stopAll();
		}
	}

	private static class TimerNode {

		enum Type {
			FOLDER, TIMER
		}

		private final String folderName;
		private final ConfigTimer timer;
		private final Type type;

		public TimerNode(String folderName) {
			this.folderName = folderName;
			this.type = Type.FOLDER;
			this.timer = null;
		}

		public TimerNode(final ConfigTimer timer) {
			this.folderName = null;
			this.type = Type.TIMER;
			this.timer = timer;
		}

		public String toString() {
			if (timer == null || type.equals(Type.FOLDER)) {
				return folderName;
			}
			return timer.getName();
		}

		public boolean isFolder() {
			return Type.FOLDER.equals(type);
		}

		public boolean isSystemFolder() {
			return Type.FOLDER.equals(type) && folderName != null && folderName.startsWith(ConfigTimer.SYSTEM_FOLDER);
		}
	}

	public class TimerCell extends TextFieldTreeCell<TimerNode> {

		private final ContextMenu folderMenu = new ContextMenu();
		private final ContextMenu timerMenu = new ContextMenu();

		public TimerCell(final TreeView<TimerNode> parentTree) {
			super(new StringConverter<TimerNode>() {
				@Override
				public TimerNode fromString(String folderName) {
					return new TimerNode(folderName);
				}

				@Override
				public String toString(TimerNode node) {
					return node.folderName;
				}
			});

			final MenuItem renameItem = new MenuItem("Rename Folder");
			folderMenu.getItems().add(renameItem);
			renameItem.setOnAction(arg0 -> startEdit());

			final MenuItem exportFolderItem = new MenuItem("Export Folder");
			folderMenu.getItems().add(exportFolderItem);
			exportFolderItem.setOnAction(arg0 -> {
				final List<ConfigTimer> timers = new ArrayList<>();
				for (final ConfigTimer timer : config.getConfigTimers().getTimers()) {
					if (Objects.equals(getItem().folderName, timer.getFolder())) {
						timers.add(timer);
					}
				}
				if (timers.size() > 0) {
					copyTimersToClipboard(timers);
					setFlash("Timers exported into your clipboard (" + timers.size() + ")", FlashMessage.Type.INFO);
				} else {
					setFlash("Folder is empty", FlashMessage.Type.INFO);
				}
			});

			final MenuItem deleteItem = new MenuItem("Remove Timer");
			timerMenu.getItems().add(deleteItem);
			deleteItem.setOnAction(arg0 -> {
				final ConfigTimer timer = getItem().timer;
				config.getConfigTimers().getTimers().remove(timer);
				timersSettings.pendingImportedTimers.remove(timer);
				timersSettings.loadCurrent();
				if (listener != null) {
					listener.onTimersUpdated();
				}
			});

			final MenuItem exportTimerItem = new MenuItem("Export Timer");
			timerMenu.getItems().add(exportTimerItem);
			exportTimerItem.setOnAction(arg0 -> {
				final ConfigTimer timer = getItem().timer;
				copyTimersToClipboard(Collections.singletonList(timer));
				setFlash("Timer " + timer + " exported into your clipboard", FlashMessage.Type.INFO);
			});

			// source node
			setOnDragDetected(event -> {
				if (getItem() == null || getItem().isFolder() || getItem().timer == null || getItem().timer.isSystem()) {
					return;
				}
				final Dragboard dragBoard = startDragAndDrop(TransferMode.MOVE);
				final ClipboardContent content = new ClipboardContent();
				content.put(DataFormat.PLAIN_TEXT, getItem().timer.getName());
				dragBoard.setContent(content);
				event.consume();
			});
			setOnDragDone(Event::consume);

			// target node
			setOnDragEntered(Event::consume);
			setOnDragOver(dragEvent -> {
				if (getItem() == null) {
					return;
				}
				// drop to folders or root
				if ((getItem().isFolder() && !getItem().isSystemFolder()) || getItem().folderName == null) {
					if (dragEvent.getDragboard().hasString()) {
						dragEvent.acceptTransferModes(TransferMode.MOVE);
					}
				}
				dragEvent.consume();
			});
			setOnDragExited(Event::consume);
			setOnDragDropped(dragEvent -> {
				final String valueToMove = dragEvent.getDragboard().getString();
				final TreeItem<TimerNode> itemToMove = searchTimerNode(parentTree.getRoot(), valueToMove);
				final TreeItem<TimerNode> newParent;
				if (getItem().isFolder() && !getItem().isSystemFolder()
						&& !getItem().folderName.equals(itemToMove.getValue().timer.getFolder())) {
					// dropping to new folder
					newParent = getTreeItem();
					itemToMove.getValue().timer.setFolder(getItem().folderName);
				} else {
					// dropping to root
					newParent = getTreeItem().getParent();
					itemToMove.getValue().timer.setFolder(null);
				}
				// Remove from former parent.
				getTreeView().getSelectionModel().clearSelection();
				itemToMove.getParent().getChildren().remove(itemToMove);
				// Add to new parent.
				newParent.getChildren().add(itemToMove);
				sortItems(newParent);
				getTreeView().getSelectionModel().select(itemToMove);
				dragEvent.consume();

				if (listener != null) {
					listener.onTimersUpdated();
				}
			});
		}

		@Override
		public void commitEdit(TimerNode node) {
			super.commitEdit(node);

			for (final TreeItem<TimerNode> item : getTreeItem().getChildren()) {
				item.getValue().timer.setFolder(node.folderName);
			}

			sortItems(this.getTreeItem().getParent());
			if (listener != null) {
				listener.onTimersUpdated();
			}
		}

		@Override
		public void updateItem(TimerNode item, boolean empty) {
			super.updateItem(item, empty);

			setEditable(item != null && item.isFolder() && !item.isSystemFolder());
			String text = (item == null) ? null : item.toString();
			setText(text);
			if (item != null && item.timer != null && Boolean.TRUE.equals(item.timer.getIsPreview())) {
				this.getStyleClass().add("timer-preview");
			} else {
				this.getStyleClass().remove("timer-preview");
			}

			if (!isEditing() && isEditable()) {
				setContextMenu(folderMenu);
			} else if (item != null && !item.isFolder() && !(item.timer != null && item.timer.isSystem())) {
				setContextMenu(timerMenu);
			} else {
				setContextMenu(null);
			}
		}
	}

	private TreeItem<TimerNode> searchTimerNode(final TreeItem<TimerNode> currentNode, final String valueToSearch) {
		TreeItem<TimerNode> result = null;
		if (!currentNode.getValue().isFolder()
				&& currentNode.getValue().timer != null
				&& currentNode.getValue().timer.getName().equals(valueToSearch)) {
			result = currentNode;

		} else if (!currentNode.isLeaf()) {
			for (final TreeItem<TimerNode> child : currentNode.getChildren()) {
				result = searchTimerNode(child, valueToSearch);
				if (result != null) {
					break;
				}
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private void sortItems(TreeItem<TimerNode> item) {
		final List<?> items = Arrays.asList(item.getChildren().toArray());
		//noinspection Java8ListSort
		Collections.sort((List<TreeItem<TimerNode>>) items, (o1, o2) -> {
			if (o1.getValue().isFolder() && !o2.getValue().isFolder()) {
				return 1;
			}
			if (o1.getValue().isSystemFolder()) {
				return 1;
			}
			if (o2.getValue().isFolder() && !o1.getValue().isFolder()) {
				return -1;
			}
			if (o2.getValue().isSystemFolder()) {
				return -1;
			}
			return o1.toString().compareTo(o2.toString());
		});
		item.getChildren().setAll((List<TreeItem<TimerNode>>) items);
	}

	private Double safeDouble(final String text) {
		if (text == null || text.trim().isEmpty()) {
			return 0.0;
		}
		try {
			return Double.parseDouble(text.trim());
		} catch (NumberFormatException e) {
			return Double.parseDouble(text.trim().replace(',', '.'));
		}
	}

	private void copyTimersToClipboard(final List<ConfigTimer> timers) {
		try {
			final StringBuilder sb = new StringBuilder();
			for (final ConfigTimer timer : timers) {
				sb.append(Marshaller.storeToString(timer));
			}
			final ClipboardContent content = new ClipboardContent();
			content.put(DataFormat.PLAIN_TEXT, new String(Base64.getEncoder().encode(sb.toString().getBytes(StandardCharsets.UTF_8))));
			Clipboard.getSystemClipboard().setContent(content);

		} catch (Exception e) {
			setFlash("Unable to export timer: " + e.getMessage());
		}
	}

	private List<ConfigTimer> getTimersFromClipboard(final String clipboard) {
		final String xml = "<com.ixale.starparse.domain.ConfigTimers><timers>"
				+ new String(Base64.getDecoder().decode(clipboard), StandardCharsets.UTF_8)
				+ "</timers></com.ixale.starparse.domain.ConfigTimers>";
		final ConfigTimers imported = Marshaller.loadFromString(xml);
		if (imported == null) {
			throw new IllegalArgumentException("Invalid data");
		}
		if (imported.getTimers() != null) {
			return imported.getTimers();
		}
		return new ArrayList<>();
	}
}
