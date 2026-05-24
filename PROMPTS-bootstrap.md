# Bootstrap Prompts — Run Once Per Project
# Run ONE session at a time. /clear between each.
# Copy-paste the prompt for that session only.

# ════════════════════════════════════════════════
# SESSION 1 — Creates: ai-context/project-overview.md
#           — Also fills: CLAUDE.md (stack, commands, rules)
# ════════════════════════════════════════════════

Read the entire project: all source files, pom.xml / package.json /
build.gradle, all config files, and README if present.

Then do these two things:

─── TASK 1: Create ai-context/project-overview.md ───

Write it with exactly these sections:

## Summary
One paragraph — what this project does and who uses it.

## Tech Stack
Table: Layer | Technology | Version (if known)

## Key Features
Numbered list. Each: feature name + one sentence what it does.

## Roles & Access
What roles exist and what each can do.
(If no roles: write "No role-based access")

## Key commands
```bash
command   # what it does
```
Include: how to run locally, how to build, how to run tests.

Keep under 80 lines. No filler.

─── TASK 2: Fill in CLAUDE.md ───

Open CLAUDE.md (project root). It has placeholder comments.
Fill in these sections based on what you read:

1. ## Stack — fill with actual tech stack found in the project
2. ## Key commands — fill with actual commands (run/build/test)
3. ## Project structure — write a 5-8 line annotated folder map
4. ## Project-specific rules — write 4-8 rules that are TRUE for
   THIS project only. These must be things Claude would get WRONG
   without being told. Examples of good rules:
   - "Config comes only from X class — never use @Value"
   - "JPA is disabled — never add @Entity annotations"
   - "All DB access via X — never call DB from controllers"
   - "Y feature is commented out — do not uncomment or reference it"
   Bad rules (don't write these — they're obvious):
   - "Write clean code"
   - "Use meaningful names"
   Only write rules that are specific to THIS codebase's actual
   constraints, disabled features, or non-obvious patterns.

Do NOT change the "Behavior rules", "LLM routing", or "Context files"
sections in CLAUDE.md — those are generic and stay the same.

When both tasks are done, print:
"Session 1 done. Run /wrap-up then /clear before Session 2."

# ════════════════════════════════════════════════
# SESSION 2 — Creates: ai-context/architecture.md
# Run AFTER Session 1 + /clear
# ════════════════════════════════════════════════

Read @ai-context/project-overview.md and the full source folder.

Create ai-context/architecture.md with exactly these sections:

## Folder structure
Annotated tree. Every folder gets a one-line comment on its purpose.
Only go 2-3 levels deep. Skip test fixtures and generated files.

## System design
ASCII diagram showing main components and connections.
Pattern: Browser → Controller → Service → Repository → Infrastructure
Adjust layers to match the actual project.

## Key data flows
For each major feature (2-4 flows max):
```
Input → step → step → Output
```
Keep each flow to 5-8 lines.

## Cache / store key structure (only if project uses Redis, memcache, etc.)
Table: Key | Field | Value shape
If no cache: skip this section entirely.

## Config management
Where secrets live. How they are loaded. Any encryption used.

Keep under 120 lines. Use ASCII diagrams, not prose paragraphs.

When done, print:
"Session 2 done. Run /wrap-up then /clear before Session 3."

# ════════════════════════════════════════════════
# SESSION 3 — Creates: ai-context/decisions.md
# Run AFTER Session 2 + /clear
# ════════════════════════════════════════════════

Read @ai-context/project-overview.md and the codebase.
Focus on: config files, security setup, data layer, non-obvious patterns,
disabled code, commented-out features.

Create ai-context/decisions.md with this structure:

For each decision use this format:
## [Category]
- **[What was chosen]** — [Why. What was the alternative.]

Suggested categories (only include ones that apply):
- Authentication & Sessions
- Data Storage
- File Handling
- Configuration & Secrets
- Security
- Code Structure & Patterns
- Disabled / Planned (not yet implemented)

Rules:
- Every bullet must have BOTH what AND why
- If something is commented out or disabled, document why
- If a pattern seems unusual, document the reasoning
- 1-2 lines per bullet max
- Do not add a category if there is nothing to put in it

When done, print:
"Session 3 done. Run /wrap-up then /clear before Session 4."

# ════════════════════════════════════════════════
# SESSION 4 — Creates: ai-context/tasks.md
# Run AFTER Session 3 + /clear
# ════════════════════════════════════════════════

Read @ai-context/decisions.md and scan the codebase for:
TODO comments, commented-out features, partially implemented code,
known issues, and any feature stubs with no implementation.

Create ai-context/tasks.md with this structure:

## Current Task
> [The task being worked on right now — or "None — starting fresh"]
> Started: [today's date]

## In Progress
<!-- Any partially done work — note exactly where it was left -->
(Leave empty if nothing is mid-way)

## Next Tasks
<!-- Ordered by priority. Each must say WHAT and WHY. -->
- [ ] **[Task name]** — [what to do + why it's needed]

(List everything you found: TODOs, commented features, known issues,
 anything that looks incomplete. Minimum 3, maximum 15.)

## Completed Tasks
- [x] [Things that are already fully implemented]

When done, print:
"Session 4 done — bootstrap complete! Run /wrap-up then /clear.
 Your ai-context/ folder is ready. From now on use PROMPT-session-start.md."
