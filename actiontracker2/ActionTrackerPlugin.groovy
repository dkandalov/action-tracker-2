package actiontracker2
import com.intellij.ide.actions.ShowFilePathAction
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import groovy.transform.Immutable
import liveplugin.PluginUtil
import liveplugin.implementation.GlobalVar

import static liveplugin.PluginUtil.newGlobalVar
import static liveplugin.PluginUtil.openInEditor

// TODO rename to ActivityTracker
class ActionTrackerPlugin {
	static final pluginId = "ActionTrackerII"

	private final ActionTracker tracker
	private final TrackerLog trackerLog
	private final PropertiesComponent propertiesComponent
	private final Disposable parentDisposable
	private final GlobalVar<State> stateVar
	PluginUI pluginUI


	ActionTrackerPlugin(ActionTracker tracker, TrackerLog trackerLog,
	                    PropertiesComponent propertiesComponent, Disposable parentDisposable) {
		this.tracker = tracker
		this.trackerLog = trackerLog
		this.propertiesComponent = propertiesComponent
		this.parentDisposable = parentDisposable
		this.stateVar = newGlobalVar("${pluginId}.state", null) as GlobalVar<State>
	}

	def init() {
		updateState { State.load(propertiesComponent, pluginId) }
		this
	}

	def setPluginUI(PluginUI pluginUI) {
		this.pluginUI = pluginUI
		pluginUI.update(stateVar.get())
	}

	def toggleTracking() {
		updateState { it.copyWith(isTracking: !it.isTracking) }
	}

	def enablePollIdeState(boolean value) {
		updateState { it.copyWith(pollIdeState: value) }
	}

	def enableTrackIdeActions(boolean value) {
		updateState { it.copyWith(trackIdeActions: value) }
	}

	def enableTrackKeyboard(boolean value) {
		updateState { it.copyWith(trackKeyboard: value) }
	}

	def enableTrackMouse(boolean value) {
		updateState { it.copyWith(trackMouse: value) }
	}

	def openTrackingLogFile(Project project) {
		openInEditor(trackerLog.currentLogFile().absolutePath, project)
	}

	def openTrackingLogFolder() {
		ShowFilePathAction.openFile(trackerLog.currentLogFile().parentFile)
	}

	private updateState(Closure<State> closure) {
		// note that parameter class is commented out because on plugin reload it will
		// be a different type (since it was loaded in different classloader)
		stateVar.set { /*State*/ oldValue ->
			def newValue = closure.call(oldValue)
			onUpdate(oldValue, newValue)
			newValue
		}
	}

	private onUpdate(/*State*/ oldState, State newState) {
		if (oldState == newState) return

		tracker.stopTracking()
		if (newState.isTracking) {
			tracker.startTracking(asTrackerConfig(newState))
		}
		pluginUI?.update(newState)
		newState.save(propertiesComponent, pluginId)
	}

	private static ActionTracker.Config asTrackerConfig(State state) {
		new ActionTracker.Config(
				state.pollIdeState,
				state.trackIdeActions,
				state.trackKeyboard,
				state.trackMouse
		)
	}


	@Immutable(copyWith = true)
	static final class State {
		static final State defaultValue = new State(true, true, true, false, false)
		boolean isTracking
		boolean pollIdeState
		boolean trackIdeActions
		boolean trackKeyboard
		boolean trackMouse

		def save(PropertiesComponent propertiesComponent, String id) {
			propertiesComponent.with {
				setValue("${id}-isTracking", isTracking)
				setValue("${id}-pollIdeState", pollIdeState)
				setValue("${id}-trackIdeActions", trackIdeActions)
				setValue("${id}-trackKeyboard", trackKeyboard)
				setValue("${id}-trackMouse", trackMouse)
			}
			PluginUtil.show("saved")
		}

		static State load(PropertiesComponent propertiesComponent, String id) {
			propertiesComponent.with {
				new State(
					getBoolean("${id}-isTracking", defaultValue.isTracking),
					getBoolean("${id}-pollIdeState", defaultValue.pollIdeState),
					getBoolean("${id}-trackIdeActions", defaultValue.trackIdeActions),
					getBoolean("${id}-trackKeyboard", defaultValue.trackKeyboard),
					getBoolean("${id}-trackMouse", defaultValue.trackMouse)
				)
			}
		}
	}
}