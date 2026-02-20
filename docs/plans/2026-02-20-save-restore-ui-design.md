# Save/Restore UI Design

## Summary
Add a File menu bar, keyboard shortcuts, and dialogs for save/load/new-game
with dirty-flag tracking, overwrite confirmation, and save-before-quit prompts.

## State Changes

### App state additions
- `:menu {:open? false}` — File dropdown visibility
- `:dirty false` — unsaved changes flag
- `:save-path nil` — last saved filename (enables "Save" vs "Save As")

### New dialog types (in existing `:dialog` key)
- `{:type :save-file :input ""}` — filename text input for save
- `{:type :load-file :input ""}` — filename text input for load
- `{:type :confirm-save :next-action :quit/:new/:load}` — "Save first?"
- `{:type :confirm-overwrite :path "..."}` — "File exists. Overwrite?"

## File Menu Bar

Persistent thin bar at top of window. Shows "File" label.
Clicking opens dropdown:

```
 File
 +---------------------------+
 | Save           (Ctrl-S)   |
 | Save As...                |
 | Open...        (Ctrl-O)   |
 | New Game       (Ctrl-N)   |
 | ------------------------- |
 | Quit                      |
 +---------------------------+
```

Clicking outside or pressing Esc closes dropdown.
Menu state lives in `:menu` key, separate from `:dialog`.

## Input Flows

### Save (Ctrl-S or menu)
1. If `:save-path` set -> save immediately, set `:dirty false`
2. If no `:save-path` -> open `:save-file` dialog
3. On OK -> if file exists, open `:confirm-overwrite`
4. On confirm -> save, set `:save-path`, set `:dirty false`

### Save As (menu only)
1. Always open `:save-file` dialog
2. Same overwrite/confirm flow as Save step 3+

### Open (Ctrl-O or menu)
1. If `:dirty` -> open `:confirm-save` with `:next-action :load`
2. If not dirty -> open `:load-file` dialog
3. On OK -> load file, set `:save-path`, `:dirty false`, regenerate contracts

### New Game (Ctrl-N or menu)
1. If `:dirty` -> open `:confirm-save` with `:next-action :new`
2. If not dirty -> reset to `initial-state`, clear `:save-path`, `:dirty false`

### Quit (menu or existing Quit button)
1. If `:dirty` -> open `:confirm-save` with `:next-action :quit`
2. If not dirty -> exit

### Confirm-save responses
- Yes -> save (using `:save-path` or prompt), then proceed with `:next-action`
- No -> proceed without saving
- Cancel -> close dialog, return to game

## Dirty Flag

Set `:dirty true` on any state mutation:
- Trade (buy/sell), simulation month, loan borrow/repay
- Hire/fire overseers, plant/spread, contract acceptance

Set `:dirty false` on save or load.

## Error Handling

- Load failure (file not found, parse error) -> display via `:message`
- Save failure (IO error) -> display via `:message`
- Reuse existing message display system, no separate error dialog

## Keyboard Shortcuts

- `Ctrl-S` -> Save
- `Ctrl-O` -> Open
- `Ctrl-N` -> New Game

These are modifier-key combos, handled before the existing `key-actions` map.

## Architecture

- **Approach B: Separate menu state** — `:menu` key independent of `:dialog`
- **In-game text dialogs** for filename input (consistent with existing dialog system)
- **Explicit save only** — no auto-save
- File format remains EDN via existing `persistence.clj`
