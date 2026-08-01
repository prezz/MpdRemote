package net.prezz.mpr.ui.state

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner

/**
 * Shared, screen-agnostic in-memory state holder used by most Activities/Fragments to survive
 * configuration changes (rotation). Callers stash values under a String key in `onSaveInstanceState`
 * and read them back in `onCreate` (see e.g. `SearchActivity`, `LibraryActivity`, the filtered/
 * playlists/partitions screens). This is a faithful 1:1 port of V1's design.
 *
 * WHY IT LOOKS LIKE THIS (rationale, so it isn't "fixed" by mistake):
 * - A `ViewModel` is the correct, recommended way to retain state across configuration changes, so
 *   the foundation is sound.
 * - The generic `Map<String, Any?>` deliberately sidesteps the ~1 MB `Bundle`/`TransactionTooLargeException`
 *   limit: screens hold large `Serializable` result arrays (search results, library/playlist entities)
 *   that would be unsafe to put in a real `onSaveInstanceState(Bundle)`. Keeping them in a ViewModel has
 *   no size limit. Do NOT "simplify" this by moving those big arrays into the Bundle.
 *
 * KNOWN LIMITATIONS (intentionally accepted for the port):
 * - Not type-safe — every read is an unchecked cast (`as? Array<...>`).
 * - Does NOT survive process death. The screens override `onSaveInstanceState` but write here (a
 *   config-change-only ViewModel), not to the outState `Bundle`; a background low-memory kill loses this.
 * - The Activity/Fragment still owns the async loading (`ResponseReceiver`/`TaskHandle`), so this class
 *   only caches results and each screen carries manual save/restore boilerplate.
 *
 * MODERNIZATION GUIDANCE (for a future "modernize the UI" pass — humans and AI agents):
 * Replace this shared stringly-typed bag with a PER-SCREEN `ViewModel` that OWNS its data and exposes
 * TYPED state, so the view just observes:
 *   - Hold state in a `StateFlow<T>` (or `LiveData<T>`) of a screen-specific type instead of `Any?`.
 *   - Do the loading INSIDE the ViewModel using `viewModelScope` + a `suspend` wrapper over
 *     `MusicPlayerControl` (the model/mpd layer already runs on a coroutine engine). This removes the
 *     re-fetch-on-rotation dance and the callback plumbing in the Activity.
 *   - Use `SavedStateHandle` for the SMALL keys that should also survive process death (scroll index,
 *     selected filter, current query); let large result lists simply re-load rather than be serialized.
 * Migrate screen-by-screen (each screen is independent) — do not do a big-bang rewrite, and do not
 * delete this class until every caller has moved off it. Related notes live in the project memory file
 * `v2-ui-porting.md` / `v2-async-model.md`.
 */
class DataState : ViewModel() {

    class State {
        val data: MutableMap<String, Any?> = HashMap()
    }

    private val state = MutableLiveData(State())

    fun getData(key: String, defaultValue: Any?): Any? {
        return state.value!!.data.getOrDefault(key, defaultValue)
    }

    fun setData(key: String, value: Any?) {
        state.value!!.data[key] = value
    }

    companion object {
        fun get(owner: ViewModelStoreOwner): DataState {
            return ViewModelProvider(owner)[DataState::class.java]
        }
    }
}
