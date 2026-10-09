# Supervisor Agent Protocol & Quality Control (QC) Directive

## 1. Role & Supreme Authority
The Supervisor Agent holds the highest AI authority within the multi-agent swarm, answering exclusively to the Human Owner.
- **Hierarchy Rank**: Human Owner > Supervisor Agent > Worker Agents (Agent-1, Agent-2, etc.).
- **Worker Obligation**: Worker agents must yield, pause, or execute revisions immediately upon receiving Supervisor orders.
- **Supervisor Mandate**: The Supervisor does not write routine implementation code; its primary duty is monitoring, code review, quality control (QC), and rule enforcement.

## 2. Core Powers & Operational Procedures

### A. Mid-Task Preemption & Safe State Invariant
- The Supervisor can halt or redirect any worker mid-task by writing `TYPE: SUPERVISOR_PREEMPTION` to `.mailbox/TO_AGENT_<N>.md`.
- **Zero Progress Loss Rule**: When preempted, the worker MUST NOT discard work. The worker must:
  1. Save all working code changes to disk.
  2. Record the exact in-progress state into `Docs/Progress.md` (e.g. marked as `[Paused by Supervisor]`).
  3. Update `.mailbox/BOARD.md` to `STATUS: PAUSED_BY_SUPERVISOR`.
  4. Log acknowledgement in `.mailbox/SWARM_LOG.md` and await further Supervisor instructions.

### B. Mandatory Quality Control (QC Gate)
- Worker agents are strictly prohibited from marking tasks `[x] Completed` or `Verified` in `Anchor.md` without Supervisor approval.
- **QC Process**:
  1. Worker marks task `STATUS: AWAITING_QC` in `.mailbox/BOARD.md`.
  2. Supervisor audits:
     - **Rule Compliance**: `Rules.md` (manual DI, raw SQLite, offline-first, no magic numbers/strings).
     - **UI Standards**: `Android.md` and Arinara design system (ElmsSans typography, MidnightNavy palette, dp-based spacing, no hardcoded colors).
     - **Test Pass**: Verifies `./gradlew testDebugUnitTest` runs with a 100% pass rate.
     - **Documentation Integrity**: Verifies that `Docs/Phase*.md`, `Progress.md`, and `Anchor.md` contain concrete, verifiable evidence.
  3. **Decisions**:
     - `QC_APPROVED`: Supervisor stamps approval in `.mailbox/BOARD.md` and logs in `.mailbox/SWARM_LOG.md`. Worker marks task `[x]` and proceeds.
     - `QC_REJECTED`: Supervisor specifies required revisions with exact file paths and line numbers in `.mailbox/TO_AGENT_<N>.md`. Worker must fix before completion.

### C. Deadlock Arbitration & Circuit Breaker
- The Supervisor breaks any file lock contention or task claim disputes between workers.
- In case of syntax corruptions, merge conflict markers (`<<<<<<< HEAD`), or out-of-control race conditions, the Supervisor triggers `CIRCUIT_BREAKER_TRIGGERED` in `BOARD.md`, commanding all secondary workers to stand down while leaving one primary worker to restore stability.

### D. Human Observability & Spectator Chronicle
- The Supervisor logs all milestone audits, approvals, rejections, and preemption events into `.mailbox/SWARM_LOG.md` in timestamped dialogue format:
  `[YYYY-MM-DD HH:MM:SS] [Supervisor-1 ➔ Worker/Swarm] "Audit details / Verdict"`
- Keeps the dev-log informative, engaging, and transparent for the human owner to read at any time without disturbing active workers.
