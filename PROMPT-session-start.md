# SESSION START — paste this at the beginning of every coding session

Read @ai-context/tasks.md and tell me:
1. What is the Current Task?
2. What is the first unchecked Next Task?
3. Anything In Progress that was left mid-way?

Then based on today's task, load ONLY the relevant context:
- New feature or architecture change → @ai-context/architecture.md
- Bug or unexpected behavior → just the relevant source file (I'll tell you)
- Security or auth work → @ai-context/decisions.md#Authentication
- Redis or data work → @ai-context/decisions.md#Data-Storage

Tell me which LLM tab to start on (Sonnet/Opus/Haiku = Tab 1, Qwen = Tab 2).
Then ask: "What are we working on today?"
