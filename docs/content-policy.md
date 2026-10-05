# Content and sourcing policy

Detail extracted from `APPROACH.md`. I am not a lawyer and this is my own reasoning, not legal advice. The design is deliberately conservative.

## Exam objectives

CompTIA publishes the SY0-801 objectives free of charge, but the document is copyrighted. The repo therefore stores **objective identifiers** (`1.2`, `2.4`, …) and **my own concise paraphrase** of each, with a link to CompTIA's official download page. The objectives PDF is not vendored into the repo, and the objective text is not reproduced verbatim in bulk. Identifiers and short factual labels are what the system needs; the full text is not. Paraphrasing each objective myself is also, conveniently, a study pass.

## Practice questions

Every item is authored fresh against the objective it maps to. No question is copied, reworded, or "spun" from another provider's bank, from exam dumps, or from any source claiming to reproduce real exam content. Braindump material is not used at any stage, including as AI input — it is both against CompTIA's candidate agreement and useless for actually learning the material. Each item's YAML records the objective it derives from, so the derivation is auditable.

## Third-party study material

Professor Messer's videos and similar resources are referenced by **title, author, URL, and date only**. Transcripts are not stored, and the videos themselves are not summarized. Where a summary appears in the app, it is an AI-assisted summary of **my own study notes**, written after watching — attributed to me, with the source linked so credit flows back to the original author. If a creator asks to be delinked, the link is removed; `CONTACT.md` says so plainly.

## Licensing

Code under Apache-2.0. My authored content (notes, questions, paraphrased objective labels) under CC BY-SA 4.0. Third-party references stay under whatever their owners chose — the repo links, it does not relicense.

## Enforcement

Every content file must carry `source_type` (`authored` | `reference-link` | `own-notes-summary`) and, for references, an `attribution` block. CI fails on a missing or invalid field, so the policy is enforced mechanically rather than by discipline.
