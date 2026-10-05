import { test } from "node:test";
import assert from "node:assert/strict";
import { mkdtempSync, mkdirSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { slugify, anchorsOf, definedRequirementIds, check } from "./check-docs.mjs";

test("slugify follows GitHub heading rules", () => {
  assert.equal(slugify("1. Product in one page"), "1-product-in-one-page");
  assert.equal(slugify("WF-04 Request lifecycle (J-03)"), "wf-04-request-lifecycle-j-03");
  assert.equal(slugify("Key hosts (vault & desk)"), "key-hosts-vault--desk");
});

test("anchorsOf numbers duplicate headings and ignores fences", () => {
  const a = anchorsOf("# A\n```\n# B\n```\n## A\n");
  assert.deepEqual([...a], ["a", "a-1"]);
});

test("definedRequirementIds reads SRS table rows", () => {
  const ids = definedRequirementIds("| FR-INS-001 | x |\n| NFR-SEC-002 | y |\ntext FR-X-003\n");
  assert.deepEqual([...ids].sort(), ["FR-INS-001", "NFR-SEC-002"]);
});

test("check reports broken links, anchors, unknown IDs and ADRs", () => {
  const root = mkdtempSync(join(tmpdir(), "check-docs-"));
  mkdirSync(join(root, "docs/specs"), { recursive: true });
  mkdirSync(join(root, "docs/adr"), { recursive: true });
  writeFileSync(join(root, "docs/specs/SRS.md"), "# SRS\n| FR-A-001 | ok |\n");
  writeFileSync(join(root, "docs/adr/0001-x.md"), "# x\n");
  writeFileSync(
    join(root, "README.md"),
    "[ok](docs/specs/SRS.md#srs) [bad](nope.md) [anc](docs/specs/SRS.md#zzz) FR-A-001 FR-B-002 ADR-0001 ADR-0009\n",
  );
  const errors = check(root);
  assert.equal(errors.length, 4, errors.join("\n"));
});

test("check skips links into submodules that are not checked out", () => {
  const root = mkdtempSync(join(tmpdir(), "check-docs-sub-"));
  mkdirSync(join(root, "docs/specs"), { recursive: true });
  writeFileSync(join(root, "docs/specs/SRS.md"), "# SRS\n");
  writeFileSync(join(root, ".gitmodules"), '[submodule "mod"]\n\tpath = mod\n\turl = x\n');
  mkdirSync(join(root, "mod"));
  writeFileSync(join(root, "README.md"), "[a](mod/README.md) [b](missing.md)\n");
  const errors = check(root);
  assert.deepEqual(errors, ["README.md: broken link missing.md"]);
});
