# Multi-Agent Swarm Autonomous Coordination & Mailbox Protocol

## 1. Operating Channel & State Architecture
When multiple AI agents operate concurrently across separate sessions in the same codebase:
1. **Central Board**: `.mailbox/BOARD.md` (tracks active agents, supervisor orders, task claims, locked files, and heartbeat).
2. **Spectator Chronicle**: `.mailbox/SWARM_LOG.md` (append-only human dev-log recording agent dialogues, supervisor audits, and task handoffs).
3. **Direct Signal Mailboxes**: `.mailbox/TO_AGENT_<N>.md` (file existence = unread message; deleted immediately upon reading).
4. **Supervisor Directives**: `.mailbox/TO_AGENT_<N>.md` with `TYPE: SUPERVISOR_DIRECTIVE`.

---

## 2. Agent Hierarchy & Authority Model
```
                  ┌──────────────────────────────┐
                  │        HUMAN (OWNER)         │
                  └──────────────┬───────────────┘
                                 │
                                 ▼
                  ┌──────────────────────────────┐
                  │       SUPERVISOR AGENT       │
                  │   (Monitoring, QC & Audit)   │
                  └──────────────┬───────────────┘
                                 │
         ┌───────────────────────┴───────────────────────┐
         ▼                                               ▼
┌─────────────────┐                             ┌─────────────────┐
│ WORKER AGENT 1  │                             │ WORKER AGENT 2  │
│ (Implementation)│                             │ (Implementation)│
└─────────────────┘                             └─────────────────┘
```
- **Supervisor Authority**: Outranks all worker agents. Directives issued by the Supervisor are mandatory and take immediate precedence over ongoing worker plans.
- **Worker Duty**: Workers must obey Supervisor audit orders, pause commands, and revision requests.

---

## 3. Supervisor Capabilities & Powers

### Power 1: Mid-Task Preemption & Safe Pause
- The Supervisor can order any worker to halt or redirect mid-task by sending a directive:
  `TYPE: SUPERVISOR_PREEMPTION`
- **Safe State Invariant**: A preempted worker must NOT discard current code. Before pausing, the worker must:
  1. Commit or save all working progress to disk.
  2. Record the exact state in `Docs/Progress.md` under `## In Progress` or `## Blocked` with note `[Paused by Supervisor]`.
  3. Update its status in `BOARD.md` to `STATUS: PAUSED_BY_SUPERVISOR`.
  4. Log acknowledgement in `SWARM_LOG.md`.

### Power 2: Mandatory Quality Control (QC Gate)
- Workers cannot mark a task `Verified` in `Anchor.md` or `Completed` in `Progress.md` without passing the Supervisor QC Gate.
- When a worker finishes implementation:
  1. Worker updates `BOARD.md` to `STATUS: AWAITING_QC`.
  2. Supervisor inspects:
     - Compliance with `Rules.md` (manual DI, raw SQLite, offline-first, no magic values).
     - Compliance with `Android.md` / UI skills (ElmsSans typography, MidnightNavy colors, dp-based spacing).
     - Test suite execution (`./gradlew testDebugUnitTest` must be 100% passing).
     - Documentation integrity (`Docs/Phase*.md`, `Progress.md`, `Anchor.md` with evidence).
  3. **Outcome A — QC APPROVED**: Supervisor logs approval in `SWARM_LOG.md`. Worker marks task `[x]` and proceeds.
  4. **Outcome B — QC REJECTED**: Supervisor logs required revisions with exact file paths and lines. Worker MUST execute the revisions before finishing.

### Power 3: Arbitration & Conflict Resolution
- If workers deadlock on file locks or task claiming in `BOARD.md`, the Supervisor renders the final binding ruling.

---

## 4. Continuous Autonomy in [COLABORATIVE] Mode
- When an agent finishes its active task and passes QC, and `[COLABORATIVE]` mode is active:
  - Do NOT stop or stall.
  - Read `Docs/Progress.md` (under `## Pending`) and `.mailbox/BOARD.md`.
  - Automatically identify uncompleted tasks and coordinate with the Supervisor to claim the next item.

---

## 5. Surplus Agent Stand-Down ("Mengalah dan Mati")
- If there are fewer tasks than active agents (e.g., 2 tasks remaining but 4 agents running), or if further splitting creates harmful coordination overhead:
  - The Supervisor (or self-evaluating worker) issues a stand-down directive.
  - Surplus agents must gracefully update `BOARD.md` to `STATUS: STOOD_DOWN`, log a farewell in `SWARM_LOG.md`, and **terminate execution immediately**.
  - The remaining active agents proceed to finish the workload cleanly.

---

## 6. Strict File Locking & Non-Collision Discipline
- **Primary Rule**: Partition tasks so agents touch completely disjoint sets of files.
- **Shared File Rule**: If two agents must touch the same file:
  - Exercise extreme caution.
  - Always re-read the target file from disk immediately prior to writing to catch external edits.
  - Never overwrite blindly without diff verification.

---

## 7. Emergency Circuit Breaker (Solo Agent Panic Fallback)
- If weird code artifacts, syntax corruption, git conflict markers (`<<<<<<< HEAD`), or out-of-control race conditions appear:
  - The Supervisor (or detecting worker) posts `CIRCUIT_BREAKER_TRIGGERED` in `BOARD.md` and alerts `SWARM_LOG.md`.
  - **All secondary agents must immediately yield, stand down, and terminate.**
  - Exactly ONE Primary Agent stays alive to revert broken artifacts, restore stability, and finish all remaining tasks sequentially to completion.

---

## 8. Spectator Chronicle Invariant (`SWARM_LOG.md`)
- Whenever an agent claims a task, locks files, hands off an interface, undergoes QC, stands down, or receives a Supervisor directive:
  - Append a concise, timestamped dialogue entry to `.mailbox/SWARM_LOG.md`:
    `[YYYY-MM-DD HH:MM:SS] [Agent-X ➔ Swarm/Agent-Y] "Message summary"`
  - Keep this file strictly append-only so the human owner can read the ongoing dev-log at any time without disturbing active agents.

---

## 9. Zero-Polling Invariant
- Never loop-poll in empty cycles. Check `.mailbox/` only at 3 lifecycle moments:
  1. Session Boot (register in `BOARD.md`).
  2. Sub-Task Milestone Transition (check inbox, update board).
  3. Pre-Commit / Pre-Completion Verification (request Supervisor QC).
