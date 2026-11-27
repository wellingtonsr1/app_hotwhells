# Specification Quality Checklist: Negociar qualquer exemplar da coleção

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-11-27
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
- [x] Success criteria are technology-agnostic (no implementation details)
- [x] All acceptance scenarios are defined
- [x] Edge cases are identified
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

## Feature Readiness

- [x] All functional requirements have clear acceptance criteria
- [x] User scenarios cover primary flows
- [x] Feature meets measurable outcomes defined in Success Criteria
- [x] No implementation details leak into specification

## Notes

- Items marked incomplete require spec updates before `/speckit-clarify` or `/speckit-plan`

### Validation run 1 — 2026-11-27

| Item | Result | Evidence |
|---|---|---|
| No implementation details | PASS (with a documented exception) | The narrative, user stories and requirements are behavior-only. `## Contexto`, `## Impacto na arquitetura` and `## Plano preliminar` name files, tables and classes **on purpose**: the request's deliverables 1, 2, 8, 9, 10 and 12 demand a diagnosis of the current implementation and a migration assessment. These sections are investigation appendices, not requirements; no FR/NFR/SC mentions technology, framework or API |
| Focused on user value | PASS | Every user story states the collector's problem and the value; scope excludes marketplace, public listing and user communication |
| Non-technical stakeholders | PASS | pt-BR, colecionador terminology, no jargon in stories/requirements |
| Mandatory sections | PASS | User Scenarios, Requirements, Success Criteria, Assumptions present; Edge Cases, Regras de negócio, Fluxos, Rastreabilidade and Riscos added as this project's convention (Specs 001–006) |
| No `[NEEDS CLARIFICATION]` | PASS | Three open points were resolved with recorded decisions: warn both when confirming availability and at negotiation start (D-01/D-02), existing sheet is the confirmation surface (D-03), and the central de negociações is out of scope (S-01). All three would change scope only if the reviewer disagrees; each has a named decision ID so the reviewer can overturn it explicitly |
| Testable and unambiguous | PASS | FR-501…FR-528 each state a verifiable behavior; FR-515 pins the warning to the two intention moments of a single-unit variant — availability confirmation (SALE/TRADE/BOTH) and negotiation start — with explicit non-triggers (UNAVAILABLE, 2+ OWNED, active negotiation) |
| Measurable success criteria | PASS | SC-501…SC-513 use counts (100%, 0%), fixed datasets (1/2/3 exemplares), timings (< 300 ms, < 1 s) and window classes (360/700/1.000 dp) |
| Technology-agnostic success criteria | PASS | SCs speak of screens, states, counts and times; none names Room, Compose or a query |
| Acceptance scenarios | PASS | Five prioritized stories with 3–5 scenarios each, covering the request's ten scenarios plus consistency, archived/sold blocking, accessibility and no-migration |
| Edge cases | PASS | Archived, sold/traded, active negotiation, last-of-two sold, edit while active, save failure, two windows, process death, old backup restore, large lists, enlarged font |
| Scope bounded | PASS | Explicit in/out lists; central de negociações assessed and deferred with reasons; no marketplace |
| Dependencies and assumptions | PASS | No new dependency; depends on existing screens/repositories; assumptions list A1, meaning of "exemplar único", reversibility of the offer and the no-migration assumption |
| FRs have acceptance criteria | PASS | Traceability matrix maps each requested scenario → business rule → FRs → SCs → planned verification |
| Stories cover primary flows | PASS | P1 unique-exemplar availability, P1 negotiation with warning, P2 Duplicados preserved, P2 cross-screen consistency, P3 lifecycle (sale/trade/cancel) |
| Measurable outcomes reached | PASS | SCs are the acceptance bar for implementation; each maps to a test type in the traceability matrix |
| No implementation detail leak | PASS | See the first row: the exception is confined to the investigation appendices required by the request |

### Observations for the reviewer

1. **D-01/D-02 (warning placement)** — resolved as: the warning appears in **both** intention moments. RN-003's "alertar quando o usuário tentar negociar" is read broadly, so FR-515 requires it (a) before saving an availability that makes a unique exemplar available and (b) before opening the negotiation. Marking **Indisponível**, variants with 2+ `OWNED` and edits with an active negotiation do not warn. If a single moment is preferred after review, FR-515 is the one line to narrow.
2. **S-01 (central de negociações)** — assessed and deferred. The evidence is recorded: no screen aggregates exemplars by negotiation state today, and the core goal is met without it. If the reviewer wants it in this feature, it becomes a new P3 story with its own route, which changes SC-512 (no new route).
3. **C-03 (naming debt)** — `duplicate_offer`/`DuplicateOffer` and the route `duplicates/{id}/negotiation` keep their names on purpose (no migration, no route change). The spec registers this instead of renaming silently.
4. **Contradictions C-01, C-02, C-04, C-05** are recorded with file-and-line evidence and are not resolved silently; C-01/C-02/C-05 are resolved by this spec on implementation, C-04 is out of scope.

Result: **16/16 items pass**; spec ready for `/speckit-clarify` (optional, given the three recorded decisions) or `/speckit-plan`.
