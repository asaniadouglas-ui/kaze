# Test Coverage Analysis

## Summary

The codebase is an Android terminal emulator with three modules:

| Module | Source Files | Test Classes | Coverage | Status |
|---|---|---|---|---|
| `terminal-emulator` | 14 | 19 | ~85% | Good |
| `terminal-view` | 8 | 0 | 0% | Critical gap |
| `app` | 6 | 2 (boilerplate) | ~0% | Critical gap |

---

## Module 1: `terminal-emulator` — Well Covered

The core emulator library has solid test coverage via JUnit 4 with a shared `TerminalTestCase` base class that provides a fluent DSL for writing terminal state assertions.

### What's tested
- Escape sequences: CSI, OSC, DCS, APC via dedicated test classes
- Cursor movement and screen operations (`CursorAndScreenTest`, `TerminalTest`)
- Scroll regions and history (`ScrollRegionTest`, `HistoryTest`)
- Terminal resize (`ResizeTest`)
- Unicode and wide characters (`UnicodeInputTest`, `WcWidthTest`)
- Text styling and 24-bit color (`TextStyleTest`)
- Key handler input encoding (`KeyHandlerTest`)
- Buffer and row internals (`ScreenBufferTest`, `TerminalRowTest`, `ByteQueueTest`)
- DEC private modes (`DecSetTest`)
- Rectangular area operations (`RectangularAreasTest`)

### Gaps to address

**1. `TerminalSession` is not directly tested**

`TerminalSession.java` manages the subprocess lifecycle, I/O threads, and bridges the emulator to the OS shell. It is exercised indirectly through `TerminalTestCase`, but there are no dedicated tests for:
- Session creation and destruction (`finishIfRunning()`)
- The `write()` and `read()` paths under normal and error conditions
- Callback dispatch (`onTextChanged`, `onTitleChanged`, `onSessionFinished`)
- Session state transitions (running → finished)

**Recommendation:** Add `TerminalSessionTest` covering session lifecycle, the callback interface contract, and edge cases like writing to a finished session.

**2. `TerminalColors` / `TerminalColorScheme` color resolution is untested**

`TerminalColors.java` maps 256-color and 24-bit RGB values to ARGB integers. The existing `TextStyleTest` covers encoding of color indices into style longs, but no test verifies that `TerminalColors.parse()` or `mCurrentColors[]` slots resolve correctly end-to-end, including:
- Default palette values at indices 0–255
- Override via OSC 10/11/12 (fg/bg/cursor color change)
- Color scheme reset

**Recommendation:** Add `TerminalColorsTest` that constructs a `TerminalColors` instance, confirms default palette entries, exercises 256-color lookups, and checks that OSC color changes propagate correctly.

**3. `KeyHandler` coverage is narrow**

`KeyHandlerTest` exists but covers only a subset of key combinations. Missing coverage includes:
- Modified keys with Alt, Shift, Fn modifiers
- Application cursor key mode vs. normal mode output differences
- Keypad application mode sequences
- Numpad keys

**Recommendation:** Extend `KeyHandlerTest` to cover all modifier combinations and mode-dependent key sequences.

---

## Module 2: `terminal-view` — No Tests (Critical)

The `terminal-view` module has **zero tests**. It contains the Android `View` responsible for rendering the terminal and handling all touch input. This is the most user-facing code and the hardest to manually verify.

### Files with no test coverage

| File | Responsibility |
|---|---|
| `TerminalView.java` | Main view: rendering, keyboard, touch dispatch |
| `TerminalRenderer.java` | Canvas drawing: text, cursor, colors, selection highlight |
| `GestureAndScaleRecognizer.java` | Touch gesture detection, pinch-to-zoom |
| `TerminalViewClient.java` | Client interface (no logic, but contract is untested) |
| `textselection/TextSelectionCursorController.java` | Text selection state machine |
| `textselection/TextSelectionHandleView.java` | Visual drag handle for selection |
| `textselection/CursorController.java` | Base cursor controller |

### Recommended tests

**4. `GestureAndScaleRecognizer` — unit testable today**

This class uses standard `MotionEvent` objects and has no Android framework dependencies beyond `GestureDetector` and `ScaleGestureDetector`. It can be unit tested with Robolectric or by injecting fake `MotionEvent`s:
- Single tap → `onSingleTapUp` callback fires
- Long press → `onLongPress` callback fires
- Two-finger pinch → `onScale` called with correct scale factor
- Scroll → `onScroll` called with correct distance delta
- Fling → `onFling` called with correct velocity

**5. `TextSelectionCursorController` — selection state logic**

The selection start/end tracking logic (`selectors`, anchor, dragging state) is pure state management that can be tested independently of rendering:
- Selection starts on long press
- Dragging a handle updates the selection bounds
- `resetSelectionIfOutsideScroll()` clears selection at correct boundaries
- Selection is cleared on new key input

**6. `TerminalRenderer` — rendering output**

This is harder to test without a `Canvas`, but with Robolectric or a shadow canvas approach, you can verify:
- Correct number of draw calls per screen row
- Cursor is drawn at the correct (column, row) position
- Background color fills use the right color value from the color palette
- Wide characters occupy two columns

---

## Module 3: `app` — No Meaningful Tests (Critical)

The `app` module contains only two boilerplate test files generated by Android Studio (`ExampleUnitTest` tests `2 + 2 == 4`, `ExampleInstrumentedTest` checks the package name). All application logic is untested.

### Files with no test coverage

| File | Responsibility |
|---|---|
| `MainActivity.kt` | Service binding, lifecycle, key dispatch |
| `TerminalService.kt` | Tab management, session creation, notification |
| `TerminalTab.kt` | Data model |
| `MainActivity.kt` (top-level functions) | `applyTheme()`, `color()`, theme definitions |

### Recommended tests

**7. `TerminalService` tab management — unit testable with mocks**

`TerminalService.createTab()` (lines 43–85) computes the new tab ID, constructs a `TerminalSession`, registers a view callback, and updates the notification. Most of this logic can be tested without a running service by extracting or mocking the Android dependencies:
- `createTab()` assigns IDs sequentially (1, 2, 3 …)
- `createTab()` after removing a tab does not reuse the old ID
- `onTextChanged` callback fires the corresponding `viewCallbacks` entry
- `onDestroy()` calls `finishIfRunning()` on all tabs

**8. `applyTheme()` color application — pure logic test**

`applyTheme()` in `MainActivity.kt` (lines 160–169) writes color values into `mCurrentColors[]` and calls `setBackgroundColor`. With a mock `TerminalSession`/`TerminalView` this is straightforward to verify:
- All 16 ANSI colors are written to indices 0–15
- Foreground written to index 256, background to 257, cursor to 258
- Background color on the view matches `theme.background`

**9. `TerminalTab` data class**

`TerminalTab` is a Kotlin data class. It deserves basic coverage for:
- `copy(name = …)` produces a new instance with the updated name and the same `id`/`session`
- Equality is based on `id` (not session reference)

**10. `KeyboardRow` CTRL-key logic**

The CTRL modifier in `KeyboardRow` (lines 265–270) computes `char.code AND 0x1F` to produce a control character. This logic lives inside a `clickable` lambda today, making it hard to test. Extracting it to a pure function `ctrlChar(label: String): String` would allow straightforward unit testing:
- `ctrlChar("C")` → `"\u0003"`
- `ctrlChar("D")` → `"\u0004"`
- Non-letter keys are passed through unchanged

---

## Prioritized Action Plan

| Priority | Area | Effort | Value |
|---|---|---|---|
| P0 | `GestureAndScaleRecognizer` unit tests | Low | High — pure logic, no rendering needed |
| P0 | `TerminalService` tab management tests | Low | High — core session lifecycle |
| P1 | `TerminalSession` lifecycle tests | Medium | High — subprocess and I/O paths |
| P1 | `TextSelectionCursorController` state tests | Medium | High — selection bugs are user-visible |
| P1 | `applyTheme()` + `TerminalColors` tests | Low | Medium — color correctness |
| P2 | `TerminalRenderer` rendering tests (Robolectric) | High | Medium — requires test infrastructure |
| P2 | `KeyHandler` full modifier coverage | Medium | Medium — extends existing tests |
| P3 | `KeyboardRow` CTRL logic extraction + test | Low | Low — small isolated fix |

## Tooling Recommendation

Add **JaCoCo** coverage reporting to the Gradle build to make coverage regressions visible in CI:

```kotlin
// terminal-emulator/build.gradle.kts
tasks.withType<Test> {
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
    }
}
```

This will generate HTML reports showing exactly which lines and branches are not exercised, making it easier to target future test work.
