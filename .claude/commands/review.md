Review all changes since last commit.

STEP 1 — Run: git diff HEAD
If nothing: run git diff

STEP 2 — For each changed file check:
□ Follows rules in CLAUDE.md?
□ No System.out.println (use LOGGER)?
□ No hardcoded Redis keys?
□ No @Value or JPA annotations?
□ No dead code or commented-out blocks left?
□ Error handling present?

STEP 3 — Check against @ai-context/decisions.md
Flag anything that violates a decision.

STEP 4 — Report:
### Files reviewed: N
### Issues: [list or "none ✓"]
### Decisions violated: [list or "none ✓"]
### Ready to commit? yes / no

STEP 5 — If ready: suggest commit message
Format: type(scope): description
Example: feat(quickaction): add SVG-only enforcement on upload
