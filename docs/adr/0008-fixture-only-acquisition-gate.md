# ADR-0008: Fixture-only acquisition gate

- **Status:** Accepted
- **Date:** recorded 2026-10-05; decision from `MASTER-ROADMAP.md` Appendix E "Acquisition rule" and "Smoke is sacred"
- **Decided by:** Owner

## Decision

Acquisition features are developed and verified only against fixtures, mocks, and httptest servers (`DOWNLOADER_ENGINE=fixture`, `INDEXER_FIXTURE=1`, `QBIT_FIXTURE`, `SABNZBD_FIXTURE`, `TMDB_FIXTURE`). No task may require browsing or hitting pirate websites, live indexers, real swarms, or paid debrid/Usenet services. "Deployable" claims require `_mvp/smoke.sh` to pass on the fixture path.

## Consequences

- Live acquisition remains an operator choice behind explicit configuration and VPN requirements (RULE-ACQ-7).
- Delegated refinement (2026-10-05): the fixture path must be the **default** of every shipped profile and the smoke must assert fixture search → grab → import, not only scanner import (FR-INS-003, NFR-SEC-010, SM-1). This strengthens, and does not change, the owner decision.
