---
name: focus
description: Set the current session focus in CLAUDE.md so Claude stays oriented across the session. Use at the start of every work session.
argument-hint: <what you're working on today>
---

# Set Session Focus

Update the `## Current focus` block in `CLAUDE.md` with $ARGUMENTS.

## Steps

1. Read the current `CLAUDE.md`
2. Find the `## Current focus` section
3. Replace its contents with:
   ```
   ## Current focus
   - Building: $ARGUMENTS
   - Started: <today's date>
   ```
4. Write the file back
5. Confirm: "Session focus set to: $ARGUMENTS"

Keep the rest of CLAUDE.md untouched.
