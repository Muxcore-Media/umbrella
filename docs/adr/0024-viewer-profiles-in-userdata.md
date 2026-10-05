# ADR-0024: Viewer profiles live in the account userdata blob

- **Status:** Accepted
- **Date:** 2026-10-05
- **Decided by:** Delegated
- **Extends:** [ADR-0009](0009-per-module-data-ownership.md)

## Context

FR-AUTH-010 asks for several viewer profiles on one login account, including kids profiles with a PIN, and for each profile to keep its own userdata and parental settings. auth-local already owns login accounts (`users`). userdata-local already owns one blob per `(tenant, user)` for progress, favorites, and prefs. Parental PIN hashes are SHA-256 of `accountUserID + ":" + pin`, stored in that blob.

A new auth-local table would make a viewer profile a second kind of principal and would force every identity consumer to learn it. A new userdata table would be a schema change in a module that already stores arbitrary JSON prefs.

## Decision

1. A viewer profile is not an auth-local user. The login account and its session stay the principal.
2. The profile directory is `prefs.profiles` on the **account** userdata blob. No new table.
3. The primary profile is implicit (`id` `primary`). Its progress, favorites, and parental prefs stay on that account blob, so existing households do not migrate.
4. Each extra profile is a sibling blob whose userdata user id is `accountUserID + "~p~" + profileID`. `profileID` is 16 hex characters. The account id is the PIN salt for every profile on that account.
5. The active profile id is stored on the BFF session and survives a BFF restart. `/api/userdata` and the parental checks read the active profile's blob. Responses still report the account id as `user_id`.
6. Leaving a profile that has a PIN, and creating a profile while one is active, requires that PIN. The hash matches admin-ui `hashParentalPIN`. The hash is not returned by the profile API.

## Alternatives

- **One auth-local user per profile.** Separate passwords and a new principal. Larger than the requirement, and it splits parental data away from the blob that already holds it.
- **A new `profiles` table in userdata-local.** A schema and RPC change for a list that fits in the existing prefs document. Sibling blobs already give each profile its own progress without that table.

## Consequences

Deleting an account (NFR-DATA-003) must also delete blobs whose user id starts with `accountUserID + "~p~"`. userdata-local does that when it consumes `identity.user.deleted`. Request anonymisation, playback rows, and BFF/admin sessions remain open under T-M4-07. Jellyfin userdata push follows the blob's user id, so a non-primary profile does not overwrite the account's Jellyfin user data. Profile delete and a profile picker outside Settings are follow-ups. The owner can supersede this ADR if profiles should be real login accounts.
