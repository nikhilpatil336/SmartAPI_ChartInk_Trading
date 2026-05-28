Review all changes since last commit.

STEP 1 — Run: git diff HEAD
If nothing staged: run git diff

STEP 2 — For each changed file check:
□ Follows rules in CLAUDE.md?
□ No System.out.println (use log.info / log.warn / log.error)?
□ No new @Value annotations in business logic (use ApplicationProperties)?
□ No @Entity, @Repository, or JPA imports added?
□ No .block() calls inside reactive WebFlux chains?
□ New config fields added to BOTH ApplicationProperties.java AND application.properties?
□ No dead code or commented-out blocks left in changed sections?
□ Error handling present where the code crosses external boundaries?

STEP 3 — Check against @ai-context/decisions.md
Flag anything that violates a recorded decision.

STEP 4 — Report:
### Files reviewed: N
### Issues: [list, or "none ✓"]
### Decisions violated: [list, or "none ✓"]
### Ready to commit? yes / no

STEP 5 — If ready: suggest commit message
Format: type(scope): description
Examples:
  fix(order): use full qty in BuyOpenStrategy instead of partial fill delta
  feat(analytics): add EOD analytics endpoint with Excel output
  refactor(scrip): key scrip master map by ticker not company name