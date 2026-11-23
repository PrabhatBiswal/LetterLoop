# LetterLoop — Development Guide

This file is the brief for the coding session. Read all of it before writing code.
It turns the assessment PDF ("Android Game Developer Assessment: Word Puzzle Game")
into a concrete design, a build order, and a definition of done.

**Status:** no game code written yet. The project is the untouched Android Studio
"Empty Activity (Compose)" template.

---

## 1. What we are building

A word puzzle game: a crossword-style grid at the top, a circular wheel of 3–5
letters at the bottom. The player drags a finger across wheel letters to spell a
word. Correct words fill into the grid. When the grid is full the game moves to
the next level by itself.

The PDF names Wordscapes as the reference for **how it plays**. We copy the
mechanics only. Name, colours, tile style, backgrounds and all level content are
our own. Do not reuse anything from that game's art or level data.

### Who this is for, and why it changes how you write code

This is a hiring assessment. The author (Prabhat) must explain and defend every
part of it in an interview. So:

- Prefer the plain, well-known Android way over anything clever.
- Every file starts with a 2–4 line comment: what this file is and why it exists.
- Comment the *why* on non-obvious lines (gesture maths, state restoration), not the *what*.
- No library the author would struggle to explain. The allowed list is in section 3.
- When you make a decision that is not already written here, add it to the
  **Decision log** in section 13 (what, why, what you rejected).
  That log is used for the interview walkthrough afterwards.

---

## 2. PDF requirements and where each one is met

Every row must be true at the end. Do not drop or weaken any of them.

| # | PDF requirement | How we meet it | How it is checked |
|---|---|---|---|
| R1 | Crossword grid on top, circular letter wheel (3–5 letters) at the bottom | `CrosswordGrid` + `LetterWheel` on the game screen; levels use 3, 4 or 5 letters only | Level validator test; visual check |
| R2 | Continuous swipe to connect letters, with a line that follows the finger | Custom `pointerInput` gesture + `Canvas` line (section 7) | Manual swipe test, geometry unit test |
| R3 | Clear visual feedback for valid grid word, bonus word, invalid word | Four distinct feedback animations (section 9) | Manual check of each case |
| R4 | Automatic transition to the next level when the grid is filled | ViewModel detects completion, celebrates, loads next level (section 8) | Engine test + manual |
| R5 | Kotlin, MVVM or MVI, Compose or Views with custom Canvas for the wheel | Kotlin, MVVM with one-way data flow, Compose, wheel drawn on `Canvas` | Code review |
| R6 | Flow Home → Level Select → Gameplay → Pause handles system back with no leaks, lost state or duplicate screens | Navigation Compose, single Activity, rules in section 8 | Back-stack checklist in section 11 |
| R7 | Game state survives rotation and app backgrounding | ViewModel + `SavedStateHandle` + DataStore (section 6) | Rotation and process-kill tests in section 11 |
| R8 | 10–20 valid playable levels from a structured source | 15 levels in `assets/levels.json` (section 5) | `LevelsValidationTest` |
| R9 | Smooth screen entry/exit animations | NavHost enter/exit/pop transitions | Visual check |
| R10 | Micro-interactions: letter scaling on touch, effects on word completion | Section 9 | Visual check |
| R11 | Clean typography, cohesive colours, responsive touch | Section 9 theme + gesture rules in section 7 | Visual check on the release build |
| R12 | Deliverables: GitHub repo, playable APK, README explaining architecture, swipe logic, level data | Section 10, milestone 6 | Files exist |
| R13 | Edge cases such as rapid swiping handled | Section 7 "edge cases" | Manual + geometry test |

---

## 3. Project facts and allowed dependencies

Already in place (do not change without reason):

- Path: `/Users/prabhatkumarbiswal/AndroidStudioProjects/LetterLoop`
- Package / applicationId: `com.prabhat.letterloop`
- AGP 9.5.2, Kotlin 2.2.10, Gradle 9.6.0, Compose BOM 2026.02.01, minSdk 24, target/compile SDK 37
- AGP 9 uses built-in Kotlin. **Do not add the `org.jetbrains.kotlin.android` plugin.**
- Version catalog: `gradle/libs.versions.toml`. Add every dependency there, not inline.
- `enableEdgeToEdge()` is on. Every screen must apply safe-drawing insets.

Add these, and only these:

| Dependency | Used for |
|---|---|
| `androidx.navigation:navigation-compose` | Screens and back stack, type-safe routes |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | `viewModel()` in composables |
| `androidx.lifecycle:lifecycle-runtime-compose` | `collectAsStateWithLifecycle()` |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` + plugin `org.jetbrains.kotlin.plugin.serialization` (same version as Kotlin) | Parsing `levels.json`, type-safe nav routes, saved progress |
| `androidx.datastore:datastore-preferences` | Saved progress |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` (test only) | ViewModel unit test |

The template pins old versions of `activity-compose` (1.8.0), `lifecycle` (2.6.1)
and `core-ktx` (1.10.1). Bump them to current stable. **Look up current stable
versions; do not write version numbers from memory.** After changing the
catalog, run a build before writing any feature code.

Not allowed: Hilt/Koin, Room, any MVI framework, `material-icons-extended`,
animation or confetti libraries, networking, ads, analytics, multi-module setup.
For icons use `material-icons-core` or small vector drawables in `res/drawable`.

---

## 4. Architecture

MVVM with one-way data flow. One Activity. Three layers, dependencies point downward only.

```
ui/        Composables. Draw state, forward user actions. No game rules here.
  ↓ observes StateFlow<UiState>, calls ViewModel functions
ViewModel  Holds screen state, calls the engine, saves progress.
  ↓
game/      Pure Kotlin game rules. No Android or Compose imports. Unit tested.
data/      Level loading (assets) and progress saving (DataStore), behind interfaces.
```

Package layout under `com.prabhat.letterloop`:

```
LetterLoopApp.kt            Application class, owns AppContainer
AppContainer.kt             Manual dependency wiring (replaces Hilt)
MainActivity.kt             setContent { theme + NavHost }
game/
  Level.kt                  Level, PlacedWord, Direction, Cell
  GameState.kt              GameState, SubmitResult
  GameEngine.kt             submit(), hint(), completion check
  LevelValidator.kt         Rules a level must satisfy (used by tests)
data/
  LevelRepository.kt        interface + AssetLevelRepository
  ProgressRepository.kt     interface + DataStoreProgressRepository
  LevelProgress.kt          @Serializable saved progress for one level
ui/
  navigation/               Routes.kt, LetterLoopNavHost.kt
  home/                     HomeScreen.kt
  levels/                   LevelSelectScreen.kt, LevelSelectViewModel.kt
  game/                     GameScreen.kt, GameViewModel.kt, GameUiState.kt,
                            CrosswordGrid.kt, LetterWheel.kt, WheelGeometry.kt,
                            WordPreview.kt, Celebration.kt, PauseDialog.kt
  components/               Shared buttons etc. (only if used twice)
  theme/                    Color.kt, Type.kt, Theme.kt
```

Rules:

- Each screen has one immutable `data class ...UiState` exposed as `StateFlow`.
  The UI collects it with `collectAsStateWithLifecycle()`.
- The UI talks to the ViewModel only through named functions
  (`onWordSubmitted`, `onShuffle`, `onHint`, `onFeedbackShown`). No two-way binding.
- ViewModels never hold a `Context`, `Activity`, `NavController` or composable
  lambda. That is the main source of leaks; keep it at zero.
- Repositories are interfaces because the ViewModel test uses fakes. Nothing else
  gets an interface.
- ViewModels are created with `viewModelFactory { initializer { ... } }`, reading
  dependencies from `AppContainer` and `createSavedStateHandle()`.
- Home has no ViewModel unless it needs state.

---

## 5. Level data

File: `app/src/main/assets/levels.json`. 15 levels. All words upper case.
The sample below shows two entries to illustrate the format (a 3-letter and a
4-letter level); the real file has ids 1 to 15 in order.

```json
{
  "levels": [
    {
      "id": 1,
      "letters": "CAT",
      "words": [
        { "word": "CAT", "row": 0, "col": 0, "dir": "ACROSS" },
        { "word": "ACT", "row": 0, "col": 1, "dir": "DOWN" }
      ],
      "bonus": []
    },
    {
      "id": 4,
      "letters": "STOP",
      "words": [
        { "word": "STOP", "row": 0, "col": 0, "dir": "ACROSS" },
        { "word": "POTS", "row": 0, "col": 3, "dir": "DOWN" },
        { "word": "TOPS", "row": 3, "col": 0, "dir": "ACROSS" }
      ],
      "bonus": ["POST", "SPOT", "OPTS", "POT", "TOP", "OPT", "SOP"]
    }
  ]
}
```

- `letters`: the wheel letters. A letter may repeat.
- `words`: the grid words. `row`/`col` is the first letter's cell, zero-based from
  the top-left. `dir` is `ACROSS` or `DOWN`.
- `bonus`: extra real words the same letters can spell that are not in the grid.
  There is no full dictionary in the app; a swipe is valid only if it is a grid
  word or a listed bonus word.
- Grid size is not stored. It is computed from the words.

Rules every level must pass (`LevelValidator`, enforced by `LevelsValidationTest`
reading the real JSON file):

1. `letters` has 3, 4 or 5 upper-case A–Z characters.
2. Every grid word and bonus word has at least 3 letters and can be spelled from
   `letters`, using each wheel letter at most as many times as it appears.
3. Where two words share a cell, they have the same letter there.
4. All grid words are connected into one crossword.
5. Every horizontal or vertical run of 2 or more filled cells is exactly one
   declared word (no accidental side-by-side words).
6. No word appears twice; no word is in both `words` and `bonus`.
7. The grid is at most 7 columns by 7 rows.
8. Ids are 1..15 with no gaps, in file order.

Difficulty ramp: levels 1–3 use 3 letters, 4–8 use 4 letters, 9–15 use 5 letters,
with more grid words as levels go up (2 → about 6). Use common everyday words.
No offensive words. Write the levels by hand and let the validator test catch
mistakes; add an ASCII grid printout to the test's failure message so a broken
level is easy to see.

Loading: `AssetLevelRepository` parses the file once on `Dispatchers.IO` and
caches the result. Screens show their loading state without a blank flash.
Keep the parsing itself in a pure function `parseLevels(json: String): List<Level>`
so it can be unit tested without Android.

---

## 6. Game state and saving

### Engine (pure Kotlin, package `game`)

- `GameState(level, foundWords, foundBonus, hintedCells)` — immutable.
- `GameEngine.submit(state, word): Pair<GameState, SubmitResult>` where
  `SubmitResult` is one of `GridWord`, `BonusWord`, `AlreadyFound`, `Invalid`.
- `GameEngine.hint(state): GameState` reveals the first hidden cell in reading
  order (deterministic, so it is testable).
- A cell is revealed if it belongs to a found word or is hinted.
- A word whose cells are all revealed (through crossings or hints) counts as found.
- The level is complete when every grid cell is revealed.

### What is saved, and where

| State | Where | Survives rotation | Survives process death |
|---|---|---|---|
| Found words, bonus words, hinted cells for a level | DataStore, key `progress_<levelId>`, JSON of `LevelProgress` | yes | yes |
| Highest unlocked level | DataStore, key `highest_unlocked` | yes | yes |
| Level currently being played | `SavedStateHandle["currentLevelId"]`, seeded from the route argument | yes | yes |
| Wheel letter order after shuffle | `SavedStateHandle` | yes | yes |
| Pause dialog open | Navigation back stack (it is a destination) | yes | yes |
| Letters selected mid-swipe, finger position | Local composable state | no — by design | no — by design |

Progress is written to DataStore **immediately after every accepted word or
hint**, not in `onStop`. There is then nothing to lose when the app is
backgrounded or killed.

Saved data is never trusted blindly. If a `progress_<id>` value cannot be parsed,
or names words that are not in that level, treat it as empty progress for that
level instead of crashing. If `highest_unlocked` is out of range, clamp it to
1..16. The value 16 (level count + 1) means every level is finished; without it
level 15 could never show as completed.

The mid-swipe selection is deliberately not saved: a rotation cancels the touch
gesture anyway, so there is nothing meaningful to restore. Note this in the README.

Rules for a finished level:

- On completion, set `highest_unlocked = max(current, levelId + 1)` right away.
- The saved progress of the finished level is cleared when the game moves on
  from it. This makes replaying a finished level from Level Select start fresh,
  and means a level restored after process death during the celebration is still
  shown as complete and still advances.
- A level counts as "completed" on Level Select when `highest_unlocked > id`.

---

## 7. The letter wheel and swipe gesture

This is the most heavily graded part. Build it carefully and test it on the
release build.

### Split of responsibilities

- `WheelGeometry.kt`: pure functions with plain numbers, no Compose runtime.
  Letter centre positions, and "which letters does this finger segment touch, in
  order". Unit tested.
- The selection rule in step 3 below is also a pure function in the same file:
  `nextSelection(selected: List<Int>, hit: Int): List<Int>`. The composable calls
  it; the unit tests call it directly. No selection logic lives inside the
  gesture lambda.
- `LetterWheel.kt`: the composable. Owns the transient selection, draws on
  `Canvas`, reports `onWordSubmitted(word)` when the finger lifts.
- The ViewModel only ever hears about a finished word. It is not updated per
  touch move.

### Geometry

- Letters sit evenly on a circle, first letter at the top (−90°), radius about
  0.62 of the wheel radius.
- Hit radius about 0.25 of the wheel radius. It must stay below half the distance
  between neighbouring letters for 5 letters so two letters can never be hit at
  the same point.
- Selection is by **letter index**, not by character, so repeated letters work.

### Gesture algorithm

Use `pointerInput` with `awaitEachGesture`:

1. `awaitFirstDown()`. Remember that pointer's id and track only that pointer;
   ignore other fingers.
2. For every move event, walk the event's historical positions and then its
   current position. For each consecutive pair of points, test the **line
   segment** between them against every letter's hit circle and handle the hits
   in order along the segment. Testing segments instead of single points is what
   stops a fast swipe from skipping a letter between two touch samples.
3. Handling a hit on letter `i`:
   - not selected → append it, give a light haptic tick;
   - it is the second-to-last selected letter → remove the last one (backtrack);
   - otherwise → ignore.
4. Finger up → if 2 or more letters are selected, call `onWordSubmitted`; then
   clear the selection.
5. Gesture cancelled (system gesture, rotation, dialog) → clear the selection
   and submit nothing.

### Drawing and performance

- The line is a `Path` through the selected letter centres and on to the current
  finger position, round caps and joins.
- Keep the finger position in a state object that is read **only inside the
  Canvas draw lambda**. Then a touch move triggers a redraw, not a recomposition.
  This is what keeps the line glued to the finger.
- The line has no animation on it. It must follow the finger exactly.
- Selected letters scale up with a spring and get a filled accent circle.

### Edge cases that must work (R13)

- Very fast swipes across the wheel select every letter passed over.
- Starting a new swipe while the previous word's feedback animation is still
  playing works normally. Animations never block input.
- The same word submitted twice quickly gives `AlreadyFound` the second time.
- A tap on one letter does nothing.
- Dragging outside the wheel and back in keeps the selection.
- A second finger on the screen does not disturb the swipe.
- Input is disabled while the level-complete celebration is showing.
- Tapping shuffle during a swipe is impossible (one pointer) and shuffle during
  the celebration is disabled.

---

## 8. Navigation and back stack

Single Activity, Navigation Compose, type-safe `@Serializable` routes:

```
Home  →  LevelSelect  →  Game(levelId)  →  Pause (dialog destination)
```

Rules (R6):

- Home "Play" → LevelSelect. LevelSelect level tap → `Game(levelId)`. Locked
  levels do nothing except a small shake.
- Every `navigate` call uses `launchSingleTop = true` **and** is ignored unless
  the current back-stack entry is `RESUMED`. Together these make double-taps and
  taps during a transition harmless. Put this in one small extension function and
  use it everywhere.
- In Game, system back opens Pause (`BackHandler`). It does not leave the level.
- Pause is a `dialog` destination, so system back on it resumes the game with no
  custom code and it survives rotation and process death for free.
- Pause buttons: Resume (`popBackStack()`), Levels (`popBackStack(LevelSelect,
  inclusive = false)`), Home (`popBackStack(Home, inclusive = false)`). These pop
  to existing entries. Never `navigate()` to Home or LevelSelect from Pause; that
  is how duplicate screens are created.
- Back on LevelSelect goes Home. Back on Home leaves the app.
- **Next level does not navigate.** `GameViewModel` swaps the level in place and
  updates `SavedStateHandle["currentLevelId"]`. The back stack stays exactly
  Home → LevelSelect → Game however many levels are played. The screen animates
  the change with `AnimatedContent` keyed on the level id.
- Completion flow, driven by the ViewModel so it survives rotation: grid full →
  phase `Celebrating` → about 1.5 s → clear old progress, load next level, phase
  `Playing`. After level 15 the phase becomes `AllLevelsDone` and the overlay
  offers a button back to Level Select. If a restored level is already complete
  (process death during the celebration), start the same flow from `init`.
- Level Select must refresh its unlocked state when returned to (collect the
  DataStore flow; do not load once).
- The app is not locked to portrait. Game screen layout: portrait = grid above
  wheel; landscape = grid left, wheel right.
- There is no timer, so nothing needs pausing when the app goes to the
  background. State is already saved. Do not auto-open Pause on background.

---

## 9. Look, feel and animation

The PDF asks for "production-ready, not a prototype". Budget real time for this.

### Theme

The visual design is fixed. The mockups are on the design canvas "LetterLoop App
UI": https://claude.ai/artifact/G5H1bVWapPqm17qzFnMp1n (private to the author;
ask them for screenshots if you cannot open it). Everything needed to build it
is written below. Sizes are in dp for a 390dp-wide phone; scale with the screen.

The look: a solid midnight-navy ground, chunky marigold "keycap" tiles and
buttons with a hard darker bottom edge instead of a soft shadow, dark text on
marigold, and faint concentric rings behind the wheel (the "loop").

Colours — define each once in `Color.kt`; no hard-coded colours in screens:

| Token | Hex | Used for |
|---|---|---|
| Ground | `#0E1726` | Screen background |
| Surface | `#18243A` | Wheel disc, cards, round icon buttons, preview pill |
| EmptyTile | `#1F2E48` | Hidden grid cells, secondary buttons, progress track |
| Outline | `#2C3F60` | 1.5dp inner border on surfaces and hidden cells |
| Marigold | `#FFB627` | Revealed tiles, selected letters, swipe line, primary button, progress |
| MarigoldEdge | `#C9820A` | Hard bottom edge under marigold shapes |
| OnMarigold | `#201400` | Text and icons on marigold |
| Teal | `#3DD6C3` | Bonus words: pill, star icon, counter badge (edge `#1E9E8F`, text `#04201C`) |
| Coral | `#FF6F61` | Invalid word pill (edge `#C2463A`, text `#2A0906`) |
| Cream | `#F6F1E7` | Main text and icons |
| Muted | `#A9B8D0` | Secondary text |
| LockedTile | `#152033` | Locked level tile (border `#223149`, content `#8FA0BC`) |

- Dark look only. No dynamic (wallpaper) colour and no light theme; the game
  must look the same on every device.
- Font: **Fredoka** (Google Fonts, Open Font License), bundled in `res/font`,
  weights 400, 500, 600, 700. Record the licence in the README. Tile and wheel
  letters are weight 600 capitals.
- The "keycap" edge is a solid offset shape drawn under the element (3dp under
  grid tiles, 4dp under wheel letters, 5–6dp under buttons), not a blurred
  elevation shadow.
- Background rings: thin circles about 2dp wide every 48dp, white at about 3.5%
  opacity, centred on the wheel (on the logo on Home). Draw them on `Canvas`.
  Level Select has a plain ground.
- Touch targets at least 48dp. Icon buttons have content descriptions.
- Leave the system status bar alone; content starts below the safe-area inset.

### Screens

**Home.** The logo is a small crossword and is left-aligned, not centred:
"LETTER" runs across in six 48dp tiles (5dp gaps, 11dp radius) and "LOOP" runs
down from the shared first L. The four LOOP tiles are marigold keycaps; E, T,
T, E, R are Surface tiles with an Outline border and cream letters (28sp, weight
600). There is no separate text wordmark; give the logo the content description
"LetterLoop". The tagline "Swipe the letters. / Fill the grid." (two lines,
23sp, weight 500, muted) sits to the right of the LOOP column, under LETTER.
Background rings are centred on the L tile. At the bottom: full-width "Play"
primary button (64dp high, 20dp radius, play icon), then a row "Next up: Level
N" / "N of 15 solved" over an 8dp progress bar.

**Level Select.** Header: round back button, "Levels" 28sp, "8/15" muted. No
progress bar here. Levels are grouped by wheel size under left-aligned section
labels "3 letters", "4 letters", "5 letters" (16sp, weight 500, muted); the
groups come from each level's letter count in the data, not from hard-coded
ranges. Each group is a 4-column grid of tiles, 72dp high, 16dp radius, with no
caption on the tiles:
- solved: marigold keycap, level number 30sp, small check mark top-right;
- current: Surface tile with a 3dp marigold ring and a soft outer marigold glow,
  number in cream;
- locked: LockedTile with a lock icon above a small number.
Footer text, left-aligned: "Solve a level to unlock the next one."

**Game (portrait).** Top bar 56dp: round pause button (48dp) left, "Level N"
26sp centre, and on the right a two-line found counter: "2/5" (18sp, weight
600) over the label "words" (13sp, muted). Grid centred in the space below:
cells 44dp with 4dp gaps and 10dp radius, capped at about 56dp on larger
screens. Hidden cells are EmptyTile with an Outline border so the crossword
shape shows from the start; revealed cells are marigold keycaps with a 26sp
dark letter; a hinted cell is EmptyTile with a marigold border and marigold
letter. Above the wheel, the word preview pill (48dp high, fully rounded,
Surface with a 2dp marigold border, 26sp letters spaced 5dp apart). Wheel: a
280dp Surface disc with a 2dp Outline border; letters 36sp weight 600 in cream;
a touched letter becomes a 64dp marigold circle with a 4dp edge and scales to
1.12; the swipe line is marigold, 12dp wide, round caps and joins. Three 52dp
round Surface buttons sit in the corners of the wheel area: hint top-right,
shuffle bottom-left, bonus bottom-right with a teal star and a teal count badge.

**Game (landscape).** Two columns: left has the grid centred, with the top bar
above it at the same width as the grid so the pause button and counter line up
with the grid's edges; right (about 320dp wide) has the preview pill on top, a 240dp wheel,
and the same three buttons in its corners. 44dp side padding for cut-outs.

**Pause.** Dialog over the dimmed game (scrim about 80% of `#060A12`): a Surface
card, 28dp radius, "Paused" 36sp, "Level N · X of Y words found" muted, then
Resume (primary, 60dp), Levels and Home (secondary, 56dp, each with an icon),
and "Your progress is saved." in small muted text.

**Level complete.** The grid stays fully revealed, the wheel and preview pill
fade out completely, confetti in marigold, teal, coral and cream. A Surface
card slides in where the wheel was, with left-aligned content and no icon
badge: "Level N complete" 32sp with "complete" in marigold; "All N words found,
plus M bonus:" muted, followed by the bonus words found as teal-bordered chips
(when no bonus words were found, the line is just "All N words found" and there
are no chips); an 8dp bar that fills during the 1.5 s wait; and "Next up: Level
N+1". After level 15 the same card says all levels are done and
shows a "Back to levels" primary button instead of the bar.

**Bonus words sheet.** Opened by the bonus button: a Surface sheet titled "Bonus
words" with the teal count badge and the found words as chips (EmptyTile with a
teal border).

### Required animations

| Moment | Animation |
|---|---|
| Screen change | Fade + short slide, about 300 ms, with matching pop (back) transitions |
| Pause dialog | Scale + fade in/out |
| Letter touched | Spring scale to 1.12, marigold circle fades in |
| Valid grid word | Its tiles pop in one after another (about 60 ms stagger, slight overshoot); short praise banner; success haptic |
| Bonus word | Preview pill turns teal and flies/shrinks to the bonus button; button bumps; counter ticks up |
| Invalid word | Preview pill turns coral and shakes sideways (about 300 ms); wheel letters settle back |
| Already found | That word's tiles pulse once; pill turns grey (Outline colour) |
| Hint | The revealed tile pops in, drawn in a dimmer "hinted" style |
| Shuffle | Letters glide to their new positions |
| Level complete | Confetti drawn on `Canvas` + "Level complete" card, then the next level slides in |
| Level Select | Level buttons appear with a small stagger; locked ones show a lock; current level highlighted |
| Any button | Scales to about 0.96 while pressed |

### Animation rules

- One-shot feedback is modelled as state: `GameUiState.feedback` carries an
  incrementing id; the screen plays it and calls `onFeedbackShown(id)`, which
  clears it. This stops a rotation from replaying the last animation.
- Tiles that are already revealed when the screen is (re)created must appear
  already revealed. Initialise each tile's animation value from its current
  state, so rotation does not re-run the pop-in for the whole grid.
- No animation may delay game logic. State changes first, animation follows.

Not in scope: sound, coins or any currency, ads, daily puzzles, settings screen.

---

## 10. Build order

Work one milestone at a time, in this order. A milestone is done only when its
checks pass. After each one: run `./gradlew :app:testDebugUnitTest :app:assembleDebug`,
tick the box here, and add any new decisions to the Decision log.

Tests are part of each milestone, not a later step. Section 14 lists every test
that must exist and which milestone it belongs to. Write the tests for a
milestone in that milestone, alongside the code they cover.

- [x] **M0 — Setup.** Add the dependencies from section 3 and bump the stale
  versions. Add `LetterLoopApp` (register it in the manifest) and an empty
  `AppContainer`. Delete the template `Greeting` code and example tests.
  *Check:* project builds and launches to an empty themed screen.

- [x] **M1 — Engine and levels.** `game/` package, `levels.json` with all 15
  levels, `AssetLevelRepository`, `LevelValidator`.
  *Check:* every M1 test in section 14 exists and is green (`GameEngineTest`,
  `LevelValidatorTest`, `LevelsValidationTest`, `LevelParsingTest`).

- [x] **M2 — Playable game screen.** `GameViewModel`, `GameUiState`,
  `CrosswordGrid`, `LetterWheel`, `WheelGeometry`, word preview. Launch straight
  into level 1 for now. No fancy animation yet, but the swipe line must work.
  *Check:* every M2 test in section 14 exists and is green (`WheelGeometryTest`,
  `WheelSelectionTest`, and `LetterWheelGestureTest` on a device). Level 1 can be
  finished by hand.

- [ ] **M3 — Navigation and saving.** Routes, NavHost, Home, Level Select,
  Pause, `DataStoreProgressRepository`, `SavedStateHandle`, next-level flow,
  all-levels-done state, landscape layout.
  *Check:* every M3 test in section 14 exists and is green (`GameViewModelTest`,
  `LevelSelectViewModelTest`, `ProgressRepositoryTest`, and `NavigationTest` and
  `StateRestorationTest` on a device). Every item in section 11
  "Back stack" and "State" passes.

- [ ] **M4 — Polish.** Theme, every animation in section 9, haptics, shuffle,
  hint, bonus list, confetti.
  *Check:* every row of the animation table is visible on a device; every edge
  case in section 7 passes; rotation mid-level does not replay tile animations.
  The whole test suite from section 14 is still green after the polish work, and
  the M4 additions to `GameViewModelTest` (shuffle, hint, bonus list) are in.

- [ ] **M5 — Release build.** Make the `release` build type installable by
  signing it with the debug signing config (state this in the README; it is an
  assessment build, not a store build). Build it, install it, play it. Debug
  builds of Compose apps are visibly less smooth, so the APK handed in must be
  the release one.
  *Check:* `./gradlew :app:assembleRelease` succeeds, the APK installs, level
  loading and saved progress still work with code shrinking on.

- [ ] **M6 — Deliverables.** `README.md` covering: what the game is, how to
  build and install, architecture and why MVVM, how the swipe gesture works, how
  level data is structured and validated, how state survives rotation and process
  death, what was deliberately left out. Copy the release APK to
  `release/LetterLoop.apk`. Run the full section 11 checklist one last time.

Git (decided by the author): run `git init` at the start of M0, then make
**one commit per milestone**, after that milestone's checks pass. Commit message
format: `M2: playable game screen with swipe wheel`. Check `.gitignore` covers
`local.properties`, `.gradle/`, `build/` and `.idea/` before the first commit.
Never push and never add a remote; the author publishes to GitHub.

---

## 11. Verification checklist

Run on an emulator or device. If none is available, say so plainly in the final
report and list exactly which items were not checked. Do not report an item as
passing unless it was actually observed.

Useful commands:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:installDebug
adb shell am kill com.prabhat.letterloop        # process death; press Home first
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1   # landscape; 0 = portrait
```

Back stack (R6):

- [ ] Home → Levels → Game → back opens Pause → back resumes → back opens Pause again
- [ ] Pause → Levels, then back lands on Home (not on a second Level Select or the game)
- [ ] Pause → Home, then back leaves the app
- [ ] Double-tapping Play, a level, or the pause button never opens two screens
- [ ] Finish three levels in a row, then Pause → Levels → back → Home: no stale game screens appear
- [ ] `adb shell dumpsys activity activities` shows a single `MainActivity`

State (R7):

- [ ] Rotate mid-level: found words, bonus count, hinted cells and wheel order are kept; no tile re-animates
- [ ] Rotate with Pause open: Pause is still open, game state intact
- [ ] Press Home, kill the process, reopen from recents: same level, same found words
- [ ] Same test with Pause open, and during the level-complete celebration
- [ ] With developer option "Don't keep activities" on, switching apps loses nothing
- [ ] Force-stop and cold start: unlocked levels and in-progress level are remembered

Gameplay (R1–R4, R13):

- [ ] All 15 levels can be completed and each advances by itself
- [ ] After level 15 the all-done screen shows and leads back to Level Select
- [ ] Grid word, bonus word, already-found word and invalid word each look different
- [ ] Every edge case in section 7 passes
- [ ] Portrait and landscape both fit on a small phone (about 360×640dp) with nothing clipped

Leaks (R6):

- [ ] No ViewModel holds a Context, Activity, NavController or composable lambda
- [ ] No coroutine is launched outside `viewModelScope` or a composable effect scope

---

## 12. When to stop and ask the author

- Anything here that conflicts with the PDF or cannot be done as written.
- Any dependency not listed in section 3.
- Pushing, adding a remote, or anything else involving GitHub.
- Changing the app name, package, or minimum SDK.

Small implementation choices are yours to make. Record them in the Decision log.

---

## 13. Decision log

Filled in during development. One entry per non-obvious choice. This is the
source for the interview walkthrough, so write it for someone who has to
explain the choice out loud.

| # | Decision | Why | Alternative rejected |
|---|---|---|---|
| 1 | Compose with `Canvas` for the wheel | PDF's preferred option; one UI toolkit for everything | Custom `View` with `onDraw`/`onTouchEvent` |
| 2 | MVVM with a single immutable UiState per screen | Asked for by the PDF; one-way flow is easy to reason about and test | Full MVI with intents/reducers: more ceremony than three screens need |
| 3 | Manual dependency wiring (`AppContainer`) | Two repositories and two ViewModels do not need a DI framework | Hilt |
| 4 | Levels in a JSON asset with a validator test | Structured, editable without touching code, and provably playable | Room: no querying or writing of levels is needed |
| 5 | Progress in DataStore, written after every word | Nothing is lost on background or kill; no lifecycle callback to get wrong | Saving in `onStop`; `SavedStateHandle` alone (lost on force-stop) |
| 6 | Swipe selection kept as local composable state | It is short-lived UI state; pushing every touch move through the ViewModel adds lag and recompositions | Selection in the ViewModel's StateFlow |
| 7 | Segment-vs-circle hit testing using historical touch points | Fast swipes move further than one letter between samples | Point-in-circle on the current position only |
| 8 | Pause as a dialog destination | Back press, rotation and process death are handled by Navigation itself | A boolean flag plus custom back handling |
| 9 | Next level swaps in place inside the Game destination | The back stack can never grow or duplicate | Navigating to a new Game destination per level |
| 10 | Bonus words listed per level, no dictionary | Small, deterministic, no large word list in the APK | Bundled dictionary file |
| 11 | Selection rule and hit testing are pure functions outside the composable | The graded gesture logic can be unit tested without a device | Logic written inline in the `pointerInput` lambda |
| 12 | Fakes for repositories in tests, no mocking library | Two small interfaces; a fake is shorter and easier to explain than mock setup | Mockito / MockK |
| 13 | Own visual identity: navy ground, marigold keycap tiles, Fredoka font | The brief asks for cohesive colours and clean type; the reference game is only a guide to mechanics, so the look is original | Photo backgrounds and white-on-colour tiles like the reference; default Material theme |
| 14 | `kotlinx-serialization-json` 1.9.0, not the newest 1.13.0 | The project's Kotlin is 2.2.10 (AGP 9.5.2 resolves KGP 2.2.10). 1.9.0 is the newest release built with Kotlin 2.2; 1.10/1.11 are built with 2.3 and 1.12/1.13 with 2.4, whose metadata a 2.2 compiler cannot read. The plugin stays at the Kotlin version as section 3 says. Compose BOM also stays at 2026.02.01 | Bumping Kotlin to 2.4.21 to use 1.13.0: changes a fixed project fact for no feature gain |
| 15 | Bumped the template's test libraries: espresso 3.5.1 → 3.7.0, `androidx.test.ext:junit` 1.1.5 → 1.3.0 | The test phone runs Android 14 (API 34); espresso 3.5.x fails there (removed `InputManager.getInstance`), fixed from 3.6. Not new dependencies, only version bumps | Keeping the template versions |
| 16 | `enableEdgeToEdge(SystemBarStyle.dark(...))` for both bars | The app is always dark. The default "auto" style follows the phone's light/dark mode and would draw dark status-bar icons on the navy ground in light mode | Default `enableEdgeToEdge()` |
| 17 | Window background set to Ground in `themes.xml` (the one colour duplicated outside `Color.kt`) | The window is drawn before Compose; without it the app flashes white on launch and on recreation | Leaving the template's light platform theme |
| 18 | Fredoka bundled as four static TTFs (400/500/600/700); licence text in `licenses/Fredoka-OFL.txt` | minSdk is 24 and variable-font weight axes only work from API 26, so a single variable file would render every weight the same on older phones | One variable `Fredoka[wdth,wght].ttf`; downloadable Google Fonts (needs Play Services and network) |
| 19 | Colour tokens live in `object LoopColors` | Keeps the exact token names from section 9 (`Surface`, `Outline`) without clashing with Compose's `Surface` composable | Top-level vals with renamed tokens |
| 20 | `Level`, `PlacedWord`, `Direction` carry `@Serializable` directly; no separate JSON classes | kotlinx.serialization is plain Kotlin (no Android), so `game/` stays pure; one model instead of a DTO, a model and a mapper | JSON DTOs in `data/` mapped to game classes |
| 21 | `SubmitResult` is a plain enum | The ViewModel already knows which word it submitted; the engine only has to say what kind of word it was | A sealed class carrying the word and its cells |
| 22 | Words revealed entirely by crossings or hints are added to `foundWords` | The found counter, the saved progress and the `AlreadyFound` check all read one set, so they can never disagree | Working out "fully revealed" again everywhere it is needed |
| 23 | Validator rule 7 also rejects negative coordinates and a grid that does not start at row 0 / column 0 | Grid size is computed as max + 1, so a negative or unused leading row would break the layout | Normalising coordinates when parsing (more code, hides authoring mistakes) |
| 24 | Levels 1 and 4 are the brief's samples verbatim; the other levels store their wheel letters out of word order. Bonus lists are generous | The first wheel should not spell the answer clockwise. With no dictionary, every common word the letters make should count, or real words get rejected | Storing letters in answer order and shuffling at load (non-deterministic first state) |
| 25 | `parseLevels` lets kotlinx's `SerializationException` through, and adds a `require` for an empty list | The library's message already names the offset and the problem; an empty file is the one bad input it would accept silently | Wrapping every error in a custom exception type |
| 26 | `AssetLevelRepository` guards its cache with a coroutine `Mutex` | Level Select and Game can ask for levels at the same moment; the lock makes that one parse, as the brief says | No lock (rare double parse); `lazy` (cannot suspend to switch to `Dispatchers.IO`) |
| 27 | A system cancel is told apart from a real lift by `change.isConsumed` | When Android cancels a touch stream, Compose delivers a synthetic lift that is already consumed; a real lift arrives unconsumed. So "cancel → submit nothing" needs no extra callback. Proven on device: removing the check makes the cancel test fail | Treating every lift as a submit; a timeout heuristic |
| 28 | Wheel and grid letter sizes are dp converted to sp | The letters sit inside fixed-size circles and tiles; with the phone's font size turned up, sp text would spill out of them. Labels, the preview pill and other text still use sp and scale normally | Plain sp everywhere |
| 29 | The wheel's `pointerInput` is keyed on the letters and the enabled flag | A shuffle (possible with a second finger) or the start of the celebration restarts the gesture coroutine, which cancels the swipe; its `finally` clears the selection. No stale indices can survive a letter change | Checking for changed letters inside the gesture loop |
| 30 | Hits along a segment are ordered by where each letter centre projects onto the segment | Hit circles never overlap, so this order equals the order the finger enters them; it is one dot product instead of solving a quadratic for entry points | Exact circle-entry points; point-only hit testing (skips letters on fast swipes) |
| 31 | `WheelGeometry` uses Compose's `Offset` for points | It is a plain x/y value class from `ui-geometry` with no runtime or state, so the functions stay pure and JVM-testable, and the gesture code needs no conversions | A separate `Point` class plus conversions |
| 32 | The in-progress swipe word goes up through `onSelectionChange` into local state in `GameScreen`, for the preview pill | The pill sits outside the wheel; the word is short-lived UI state like the selection itself, so it never reaches the ViewModel | Putting the selection in the ViewModel's StateFlow |
| 33 | The keycap edge is drawn below the element's bounds (into the gap) | Revealed and hidden tiles keep the same face size and line up; the 4dp grid gap holds the 3dp edge | Shrinking the face to make room for the edge inside the bounds |

---

## 14. Test plan

Every test listed here must exist by the milestone shown. Each bullet is at
least one test function, named for the behaviour it proves, for example
`segmentCrossingLetter_withBothEndsOutside_selectsIt`.

### Rules

- JVM unit tests go in `app/src/test`. Device tests go in `app/src/androidTest`.
- Never delete, `@Ignore`, or loosen a failing test to get a green run. Fix the
  code. If the test itself was wrong, fix it and say why in the milestone report.
- No sleeps and no real-time waits. Use `runTest` and virtual time for the
  celebration delay; the ViewModel takes its dispatcher/main dispatcher from the
  test (`Dispatchers.setMain`).
- Use hand-written fakes (`FakeLevelRepository`, `FakeProgressRepository`). No
  mocking library.
- Device tests find things by test tag. Keep all tags in one `TestTags` object.
- Device tests must start from clean saved data: clear progress in `@Before`.
- If no emulator or device is available, still write the device tests and make
  sure they compile (`./gradlew :app:assembleDebugAndroidTest`), then report
  them as "written, not run". Do not claim they pass.

```bash
./gradlew :app:testDebugUnitTest            # JVM tests
./gradlew :app:connectedDebugAndroidTest    # device tests, needs emulator/phone
```

### JVM unit tests

**`GameEngineTest` (M1)**

- A grid word returns `GridWord`, is added to found words, and reveals exactly its cells.
- A bonus word returns `BonusWord`, is added to found bonus words, reveals no cells.
- The same grid word a second time returns `AlreadyFound` and changes nothing.
- The same bonus word a second time returns `AlreadyFound` and changes nothing.
- A word not in the level returns `Invalid` and changes nothing.
- A word that the wheel letters can spell but the level does not list returns `Invalid`.
- An empty string and a single letter return `Invalid` and do not crash.
- Two crossing words reveal their shared cell once (revealed cell count is correct).
- A word whose cells were all revealed by crossing words counts as found;
  submitting it afterwards returns `AlreadyFound`.
- The level is not complete with one word missing, and is complete when the last word is found.
- Bonus words never make a level complete.
- Hint reveals the first hidden cell in reading order; repeated hints reveal the
  next ones in order.
- A word finished by hints counts as found.
- Hints alone can complete a level.
- Hint on a complete level changes nothing.
- `submit` and `hint` return a new state and leave the original untouched.

**`LevelValidatorTest` (M1)** — tests the validator itself, so a validator that
accepts everything cannot pass.

- A correct level is accepted.
- One deliberately broken level for each of the 8 rules in section 5 is
  rejected, and the error names the rule that failed.
- A word needing a letter twice is rejected when the wheel has it once, and
  accepted when the wheel has it twice.

**`LevelsValidationTest` (M1)** — runs against the real `levels.json`.

- The file has exactly 15 levels and every one passes the validator.
- Every level is solvable: submitting all its grid words through `GameEngine`
  completes it.
- Every bonus word of every level returns `BonusWord`.
- Letter counts follow the ramp: levels 1–3 have 3, levels 4–8 have 4, levels 9–15 have 5.
- A failing level prints its grid as ASCII in the failure message.

**`LevelParsingTest` (M1)**

- A small sample JSON parses into the expected `Level` objects.
- Malformed JSON throws a clear exception; it never returns an empty list silently.
- An unknown `dir` value fails.
- An unknown extra field is ignored.

**`WheelGeometryTest` (M2)**

- For 3, 4 and 5 letters: the first letter is at the top centre, all centres are
  the same distance from the wheel centre, and the angles between neighbours are equal.
- For 3, 4 and 5 letters: neighbouring hit circles never overlap.
- A point inside a letter's hit circle hits it; a point just outside does not.
- A segment through two letters returns both in travel order; reversed, it
  returns them in the opposite order.
- A segment with both ends outside a letter's circle that passes through it
  still returns that letter. This is the fast-swipe case.
- A segment that starts inside a circle and ends outside returns that letter.
- A segment passing between two letters returns nothing.
- A segment that just misses a circle returns nothing.
- A zero-length segment returns the letter if the point is inside, nothing if outside.
- Results are the same at wheel radius 100 and 500 (no dependence on screen density).

**`WheelSelectionTest` (M2)** — `nextSelection` and word building.

- A hit on an unselected letter appends it.
- A hit on the last selected letter (finger still resting on it) changes nothing.
- A hit on the second-to-last selected letter removes the last one.
- A hit on any earlier selected letter changes nothing.
- Backtracking repeatedly goes down to one letter and never to an empty or invalid selection.
- With all letters selected, a hit on a non-adjacent selected letter changes nothing.
- A wheel with a repeated letter: both copies can be selected independently, by index.
- Indices convert to the right word, including with repeated letters.

**`GameViewModelTest` (M3; the shuffle, hint and bonus-list cases in M4)**

- Starts on the level from the route with that level's wheel letters and an empty grid.
- Saved progress is restored: found words are revealed in the first state.
- Grid word: cells revealed, feedback kind is grid, progress saved exactly once.
- Bonus word: counter goes up, word is listed, progress saved, feedback kind is bonus.
- Invalid word: nothing saved, feedback kind is invalid.
- Already-found word: nothing saved, feedback kind is already-found.
- Feedback ids always increase. `onFeedbackShown(id)` clears that feedback, and
  an old id does not clear a newer feedback.
- The same word submitted twice in quick succession: the second is
  already-found and progress is saved once.
- Last grid word: `highest_unlocked` is raised and the phase becomes `Celebrating`.
- After the celebration delay (virtual time): the next level is loaded, phase is
  `Playing`, `SavedStateHandle["currentLevelId"]` is updated, and the finished
  level's saved progress is cleared.
- Word submissions, hint and shuffle are ignored while `Celebrating`.
- Finishing level 15: phase becomes `AllLevelsDone` and `highest_unlocked` is 16.
- Created for a level whose saved progress is already complete: it celebrates
  and advances (process death during the celebration).
- Recreated with a `SavedStateHandle` level id that differs from the route
  argument: it uses the saved one (process death after advancing).
- Replaying an older finished level never lowers `highest_unlocked`.
- A level id that does not exist does not crash. Pick the behaviour (fall back
  to a valid level or show an error state), test it, and log the decision.
- Shuffle: the same letters in a new order, grid untouched, order written to
  `SavedStateHandle` and restored from it.
- Hint: one cell revealed and saved; a hint that fills the last cell starts the
  completion flow.

**`LevelSelectViewModelTest` (M3)**

- Fresh install: level 1 unlocked, levels 2–15 locked.
- `highest_unlocked` = 5: levels 1–4 completed, 5 current, 6–15 locked.
- `highest_unlocked` = 16: all 15 completed.
- When the progress flow emits a new value the state updates without a new ViewModel.

**`ProgressRepositoryTest` (M3)** — the real `DataStoreProgressRepository` on a
temporary file. DataStore Preferences can be created on the JVM with a temp
file; if that does not work with the installed version, move this class to
`androidTest` and log it.

- Nothing saved: empty progress and `highest_unlocked` = 1.
- Save then read returns the same found words, bonus words and hinted cells.
- Progress saved for one level does not appear in another.
- Clearing one level leaves the others intact.
- An unparseable stored value reads as empty progress with no exception.
- `highest_unlocked` never decreases, and an out-of-range stored value is clamped.
- The progress flow emits when a value changes.

### Device tests

**`LetterWheelGestureTest` (M2)** — the `LetterWheel` composable alone, with
touch positions computed from `WheelGeometry`.

- A slow drag over letters 0, 1, 2 submits that word once.
- A single move event that jumps from before a letter to past it still selects
  that letter.
- A tap on one letter submits nothing.
- Dragging back onto the previous letter and releasing submits the shortened word.
- Dragging outside the wheel and back in keeps the selection.
- A cancelled gesture submits nothing and clears the selection.
- A second finger going down mid-drag does not change the submitted word.
- With the wheel disabled nothing is submitted.
- Twenty swipes in a row, as fast as the test can send them, give exactly twenty
  submissions with the right words. This is the rapid-swiping case.

**`NavigationTest` (M3)** — launches `MainActivity`.

- Home → Play shows Level Select; tapping level 1 shows the game.
- Back in the game shows Pause; back again shows the game with Pause gone.
- Pause → Levels shows Level Select; back then shows Home.
- Pause → Home shows Home; back then finishes the activity.
- Double-clicking Play, a level, or the pause button: one back press returns to
  the previous screen, proving no duplicate was opened.
- Tapping a locked level stays on Level Select.
- After completing level 1 the game shows level 2, and Pause → Levels → back
  still lands on Home (the back stack did not grow).

**`StateRestorationTest` (M3)** — uses `ActivityScenario.recreate()`, which is
what a rotation does.

- Find a word, recreate: the word is still revealed, same level, same bonus count.
- Open Pause, recreate: Pause is still showing.
- Shuffle, recreate: the wheel order is unchanged.

Real process death cannot be produced from inside these tests. It stays a manual
check in section 11 and must be reported as manual.

### Section 7 edge cases and the test that covers each

| Edge case | Covered by |
|---|---|
| Fast swipe does not skip letters | `WheelGeometryTest` (both-ends-outside segment), `LetterWheelGestureTest` (jumping move event) |
| New swipe during feedback animation | `LetterWheelGestureTest` (twenty swipes), `GameViewModelTest` (feedback ids) |
| Same word twice quickly | `GameViewModelTest`, `GameEngineTest` |
| Tap on one letter does nothing | `LetterWheelGestureTest` |
| Drag outside and back in | `LetterWheelGestureTest` |
| Second finger ignored | `LetterWheelGestureTest` |
| Input disabled during celebration | `GameViewModelTest`, `LetterWheelGestureTest` (disabled wheel) |
| Repeated wheel letters | `WheelSelectionTest`, `LevelValidatorTest` |
| Corrupt saved data | `ProgressRepositoryTest` |

### Reporting

Each milestone report states: which test classes were added, how many JVM tests
ran and passed, whether device tests were run or only compiled, and any test
that was changed after first being written and why. The README (M6) gets a short
"Testing" section with the two commands above and the final counts.

