# Zero-Polling Signal Mailbox & Autonomous Swarm Protocol

## 1. Overview
This directory acts as an asynchronous, non-blocking coordination and communication hub between parallel AI agents working in separate sessions or workspaces within the same project tree.

## 2. Directory Structure
- `BOARD.md`: Central coordination board tracking active agents, file locks, and claimed tasks.
- `SWARM_LOG.md`: Human spectator chronicle (append-only log of agent dialogues, handoffs, and negotiations).
- `TEMPLATE_MESSAGE.md`: Standardized message envelope.
- `TO_AGENT_<N>.md`: Transient direct message signals (existence = unread message; deleted immediately upon reading).

## 3. Core Swarm Rules

### Rule 1: Continuous Autonomy in [COLABORATIVE] Mode
When a task finishes and `[COLABORATIVE]` mode is active, agents do not stop. They inspect `Docs/Progress.md` and `BOARD.md` to claim remaining pending tasks.

### Rule 2: Non-Greed & Graceful Stand-Down ("Mengalah & Mati")
- Agents must never race or fight over the same task or files.
- If there are fewer tasks than active agents (e.g. 2 tasks for 4 agents), surplus agents must gracefully post `STATUS: STOOD_DOWN` in `BOARD.md`, log their farewell in `SWARM_LOG.md`, and terminate immediately.

### Rule 3: Strict File Locking
Before modifying files, agents declare locks in `BOARD.md`. No agent may edit a file locked by another agent.

### Rule 4: Emergency Circuit Breaker (Solo Agent Fallback)
If conflict markers (`<<<<<<< HEAD`), parse errors, or out-of-control race conditions occur:
1. Set `CIRCUIT_BREAKER_TRIGGERED` in `BOARD.md`.
2. All secondary agents immediately yield and terminate.
3. The Primary Agent (Agent-1) alone stabilizes the codebase, reverts broken artifacts, and finishes all remaining tasks sequentially.

### Rule 5: Spectator Chronicle (`SWARM_LOG.md`)
Whenever an agent claims a task, locks files, hands off an interface, or stands down, they append a timestamped dialogue entry to `SWARM_LOG.md` so the human owner can read the full journey.

### Rule 6: Zero-Polling Invariant
Never loop-poll. Check `.mailbox/` only at 3 lifecycle moments:
1. Session Boot (register in `BOARD.md`).
2. Sub-Task Milestone Transition (check inbox, update board).
3. Pre-Commit / Pre-Completion Verification.
